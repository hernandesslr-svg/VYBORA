-- VYBORA / Supabase PostgreSQL schema
-- Version: 1.0.0
-- IMPORTANT:
-- 1) Review and run only in the intended Supabase project.
-- 2) This is an initial-install script; it does not migrate arbitrary existing schemas.
-- 3) Do not put service_role keys in the Android app.
-- 4) Private documents belong in the private Storage bucket, never in public profile columns.
-- 5) Run in Supabase SQL Editor as project owner.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;

-- ---------------------------------------------------------------------------
-- Helpers
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = ''
AS $fn$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$fn$;

-- ---------------------------------------------------------------------------
-- Profiles: ONLY fields safe to show on a public profile.
-- Sensitive personal data is kept separately in private_user_data.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profiles (
  id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  username text NOT NULL UNIQUE
    DEFAULT ('user_' || replace(gen_random_uuid()::text, '-', '')),
  display_name text NOT NULL DEFAULT '',
  bio text NOT NULL DEFAULT '',
  avatar_path text,
  is_verified boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT profiles_username_format
    CHECK (username ~ '^[a-zA-Z0-9_.]{3,40}$'),
  CONSTRAINT profiles_display_name_length CHECK (char_length(display_name) <= 80),
  CONSTRAINT profiles_bio_length CHECK (char_length(bio) <= 500)
);

DROP TRIGGER IF EXISTS profiles_set_updated_at ON public.profiles;
CREATE TRIGGER profiles_set_updated_at
BEFORE UPDATE ON public.profiles
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- Private personal details. Store only what the product truly needs.
CREATE TABLE IF NOT EXISTS public.private_user_data (
  user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  phone text,
  birth_date date,
  country_code text CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$'),
  language_code text CHECK (language_code IS NULL OR language_code ~ '^[a-z]{2}(-[A-Z]{2})?$'),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS private_user_data_set_updated_at ON public.private_user_data;
CREATE TRIGGER private_user_data_set_updated_at
BEFORE UPDATE ON public.private_user_data
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ---------------------------------------------------------------------------
-- Posts, likes, follows, comments, messages, notifications
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.posts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  caption text NOT NULL DEFAULT '',
  media_path text,
  media_type text NOT NULL DEFAULT 'text'
    CHECK (media_type IN ('text','image','video')),
  visibility text NOT NULL DEFAULT 'public'
    CHECK (visibility IN ('public','private')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT posts_caption_length CHECK (char_length(caption) <= 5000)
);

DROP TRIGGER IF EXISTS posts_set_updated_at ON public.posts;
CREATE TRIGGER posts_set_updated_at
BEFORE UPDATE ON public.posts
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.post_likes (
  user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  post_id uuid NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, post_id)
);

CREATE TABLE IF NOT EXISTS public.follows (
  follower_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  following_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (follower_id, following_id),
  CONSTRAINT follows_no_self CHECK (follower_id <> following_id)
);

CREATE TABLE IF NOT EXISTS public.comments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id uuid NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  body text NOT NULL CHECK (char_length(trim(body)) BETWEEN 1 AND 2000),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.messages (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  recipient_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  body text NOT NULL CHECK (char_length(trim(body)) BETWEEN 1 AND 10000),
  read_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT messages_no_self CHECK (sender_id <> recipient_id)
);

CREATE TABLE IF NOT EXISTS public.notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  actor_id uuid REFERENCES public.profiles(id) ON DELETE SET NULL,
  kind text NOT NULL CHECK (char_length(kind) BETWEEN 1 AND 80),
  entity_id uuid,
  read_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Records for private user documents. Binary files are stored in Supabase Storage.
CREATE TABLE IF NOT EXISTS public.user_documents (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  object_path text NOT NULL UNIQUE,
  original_name text NOT NULL CHECK (char_length(original_name) BETWEEN 1 AND 255),
  mime_type text NOT NULL CHECK (char_length(mime_type) BETWEEN 1 AND 150),
  size_bytes bigint NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 52428800),
  sha256_hex text CHECK (sha256_hex IS NULL OR sha256_hex ~ '^[0-9a-fA-F]{64}$'),
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT user_documents_path_owner CHECK (split_part(object_path, '/', 1) = user_id::text)
);

-- ---------------------------------------------------------------------------
-- Create profile automatically for new Supabase Auth users.
-- Username default is generated by the table. Metadata is untrusted user input,
-- so it is length-limited and never used for authorization.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $fn$
BEGIN
  INSERT INTO public.profiles (id, display_name)
  VALUES (
    NEW.id,
    left(coalesce(NEW.raw_user_meta_data ->> 'display_name', ''), 80)
  )
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO public.private_user_data (user_id)
  VALUES (NEW.id)
  ON CONFLICT (user_id) DO NOTHING;

  RETURN NEW;
END;
$fn$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS posts_feed_idx ON public.posts (created_at DESC);
CREATE INDEX IF NOT EXISTS posts_user_idx ON public.posts (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS posts_visibility_feed_idx ON public.posts (visibility, created_at DESC);
CREATE INDEX IF NOT EXISTS comments_post_idx ON public.comments (post_id, created_at);
CREATE INDEX IF NOT EXISTS messages_inbox_idx ON public.messages (recipient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS messages_sender_idx ON public.messages (sender_id, created_at DESC);
CREATE INDEX IF NOT EXISTS notifications_inbox_idx ON public.notifications (recipient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS follows_following_idx ON public.follows (following_id, created_at DESC);
CREATE INDEX IF NOT EXISTS user_documents_owner_idx ON public.user_documents (user_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- RLS enabled everywhere
-- ---------------------------------------------------------------------------
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.private_user_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_documents ENABLE ROW LEVEL SECURITY;

-- Remove old policies of the same names so rerunning the policy section is safe.
DROP POLICY IF EXISTS profiles_read ON public.profiles;
CREATE POLICY profiles_read ON public.profiles
FOR SELECT TO anon, authenticated USING (true);

DROP POLICY IF EXISTS profiles_update_own ON public.profiles;
CREATE POLICY profiles_update_own ON public.profiles
FOR UPDATE TO authenticated
USING (id = (SELECT auth.uid()))
WITH CHECK (id = (SELECT auth.uid()));

DROP POLICY IF EXISTS private_user_data_select_own ON public.private_user_data;
CREATE POLICY private_user_data_select_own ON public.private_user_data
FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS private_user_data_insert_own ON public.private_user_data;
CREATE POLICY private_user_data_insert_own ON public.private_user_data
FOR INSERT TO authenticated WITH CHECK (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS private_user_data_update_own ON public.private_user_data;
CREATE POLICY private_user_data_update_own ON public.private_user_data
FOR UPDATE TO authenticated
USING (user_id = (SELECT auth.uid()))
WITH CHECK (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS posts_read ON public.posts;
CREATE POLICY posts_read ON public.posts FOR SELECT TO anon, authenticated
USING (visibility = 'public' OR user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS posts_insert_own ON public.posts;
CREATE POLICY posts_insert_own ON public.posts FOR INSERT TO authenticated
WITH CHECK (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS posts_update_own ON public.posts;
CREATE POLICY posts_update_own ON public.posts FOR UPDATE TO authenticated
USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS posts_delete_own ON public.posts;
CREATE POLICY posts_delete_own ON public.posts FOR DELETE TO authenticated
USING (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS likes_read ON public.post_likes;
CREATE POLICY likes_read ON public.post_likes FOR SELECT TO anon, authenticated
USING (EXISTS (SELECT 1 FROM public.posts p WHERE p.id = post_id AND p.visibility = 'public'));
DROP POLICY IF EXISTS likes_insert_own ON public.post_likes;
CREATE POLICY likes_insert_own ON public.post_likes FOR INSERT TO authenticated
WITH CHECK (
  user_id = (SELECT auth.uid()) AND
  EXISTS (SELECT 1 FROM public.posts p WHERE p.id = post_id AND p.visibility = 'public')
);
DROP POLICY IF EXISTS likes_delete_own ON public.post_likes;
CREATE POLICY likes_delete_own ON public.post_likes FOR DELETE TO authenticated
USING (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS follows_read ON public.follows;
CREATE POLICY follows_read ON public.follows FOR SELECT TO anon, authenticated USING (true);
DROP POLICY IF EXISTS follows_insert_own ON public.follows;
CREATE POLICY follows_insert_own ON public.follows FOR INSERT TO authenticated
WITH CHECK (follower_id = (SELECT auth.uid()) AND follower_id <> following_id);
DROP POLICY IF EXISTS follows_delete_own ON public.follows;
CREATE POLICY follows_delete_own ON public.follows FOR DELETE TO authenticated
USING (follower_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS comments_read ON public.comments;
CREATE POLICY comments_read ON public.comments FOR SELECT TO anon, authenticated
USING (EXISTS (SELECT 1 FROM public.posts p WHERE p.id = post_id AND p.visibility = 'public'));
DROP POLICY IF EXISTS comments_insert_own ON public.comments;
CREATE POLICY comments_insert_own ON public.comments FOR INSERT TO authenticated
WITH CHECK (
  user_id = (SELECT auth.uid()) AND
  EXISTS (SELECT 1 FROM public.posts p WHERE p.id = post_id AND p.visibility = 'public')
);
DROP POLICY IF EXISTS comments_delete_own ON public.comments;
CREATE POLICY comments_delete_own ON public.comments FOR DELETE TO authenticated
USING (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS messages_read ON public.messages;
CREATE POLICY messages_read ON public.messages FOR SELECT TO authenticated
USING (sender_id = (SELECT auth.uid()) OR recipient_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS messages_send ON public.messages;
CREATE POLICY messages_send ON public.messages FOR INSERT TO authenticated
WITH CHECK (sender_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS messages_read_update ON public.messages;
CREATE POLICY messages_read_update ON public.messages FOR UPDATE TO authenticated
USING (recipient_id = (SELECT auth.uid())) WITH CHECK (recipient_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS notifications_read ON public.notifications;
CREATE POLICY notifications_read ON public.notifications FOR SELECT TO authenticated
USING (recipient_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS notifications_update ON public.notifications;
CREATE POLICY notifications_update ON public.notifications FOR UPDATE TO authenticated
USING (recipient_id = (SELECT auth.uid())) WITH CHECK (recipient_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS user_documents_select_own ON public.user_documents;
CREATE POLICY user_documents_select_own ON public.user_documents FOR SELECT TO authenticated
USING (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS user_documents_insert_own ON public.user_documents;
CREATE POLICY user_documents_insert_own ON public.user_documents FOR INSERT TO authenticated
WITH CHECK (user_id = (SELECT auth.uid()) AND split_part(object_path, '/', 1) = (SELECT auth.uid())::text);
DROP POLICY IF EXISTS user_documents_update_own ON public.user_documents;
CREATE POLICY user_documents_update_own ON public.user_documents FOR UPDATE TO authenticated
USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS user_documents_delete_own ON public.user_documents;
CREATE POLICY user_documents_delete_own ON public.user_documents FOR DELETE TO authenticated
USING (user_id = (SELECT auth.uid()));

-- ---------------------------------------------------------------------------
-- Least-privilege grants. Column-level grants prevent users from changing
-- ownership, verification flags, timestamps, message sender/recipient, etc.
-- ---------------------------------------------------------------------------
GRANT USAGE ON SCHEMA public TO anon, authenticated;

REVOKE ALL ON public.profiles, public.private_user_data, public.posts,
  public.post_likes, public.follows, public.comments, public.messages,
  public.notifications, public.user_documents FROM anon, authenticated;

GRANT SELECT ON public.profiles TO anon, authenticated;
GRANT UPDATE (username, display_name, bio, avatar_path)
  ON public.profiles TO authenticated;

GRANT SELECT ON public.posts TO anon, authenticated;
GRANT INSERT (user_id, caption, media_path, media_type, visibility)
  ON public.posts TO authenticated;
GRANT UPDATE (caption, media_path, media_type, visibility)
  ON public.posts TO authenticated;
GRANT DELETE ON public.posts TO authenticated;

GRANT SELECT ON public.post_likes, public.follows, public.comments TO anon, authenticated;
GRANT INSERT (user_id, post_id) ON public.post_likes TO authenticated;
GRANT DELETE ON public.post_likes TO authenticated;
GRANT INSERT (follower_id, following_id) ON public.follows TO authenticated;
GRANT DELETE ON public.follows TO authenticated;
GRANT INSERT (post_id, user_id, body) ON public.comments TO authenticated;
GRANT DELETE ON public.comments TO authenticated;

GRANT SELECT ON public.messages TO authenticated;
GRANT INSERT (sender_id, recipient_id, body) ON public.messages TO authenticated;
GRANT UPDATE (read_at) ON public.messages TO authenticated;

GRANT SELECT ON public.notifications TO authenticated;
GRANT UPDATE (read_at) ON public.notifications TO authenticated;

GRANT SELECT ON public.private_user_data TO authenticated;
GRANT INSERT (user_id, phone, birth_date, country_code, language_code)
  ON public.private_user_data TO authenticated;
GRANT UPDATE (phone, birth_date, country_code, language_code)
  ON public.private_user_data TO authenticated;

GRANT SELECT ON public.user_documents TO authenticated;
GRANT INSERT (user_id, object_path, original_name, mime_type, size_bytes, sha256_hex)
  ON public.user_documents TO authenticated;
GRANT UPDATE (deleted_at) ON public.user_documents TO authenticated;
GRANT DELETE ON public.user_documents TO authenticated;

-- ---------------------------------------------------------------------------
-- Supabase Storage buckets and policies
-- ---------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES
  ('post-media', 'post-media', true, 104857600, ARRAY['image/jpeg','image/png','image/webp','image/gif','video/mp4','video/quicktime','video/webm']),
  ('private-documents', 'private-documents', false, 52428800, ARRAY['application/pdf','image/jpeg','image/png','image/webp'])
ON CONFLICT (id) DO UPDATE SET
  public = EXCLUDED.public,
  file_size_limit = EXCLUDED.file_size_limit,
  allowed_mime_types = EXCLUDED.allowed_mime_types;

-- Public post media can be read publicly; uploads/updates/deletes only by owner folder.
DROP POLICY IF EXISTS post_media_public_read ON storage.objects;
CREATE POLICY post_media_public_read ON storage.objects
FOR SELECT TO anon, authenticated
USING (bucket_id = 'post-media');

DROP POLICY IF EXISTS post_media_insert_own ON storage.objects;
CREATE POLICY post_media_insert_own ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'post-media' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text);

DROP POLICY IF EXISTS post_media_update_own ON storage.objects;
CREATE POLICY post_media_update_own ON storage.objects
FOR UPDATE TO authenticated
USING (bucket_id = 'post-media' AND owner_id = (SELECT auth.uid())::text)
WITH CHECK (bucket_id = 'post-media' AND owner_id = (SELECT auth.uid())::text);

DROP POLICY IF EXISTS post_media_delete_own ON storage.objects;
CREATE POLICY post_media_delete_own ON storage.objects
FOR DELETE TO authenticated
USING (bucket_id = 'post-media' AND owner_id = (SELECT auth.uid())::text);

-- Private documents are never publicly readable. Object path must start with user UUID.
DROP POLICY IF EXISTS private_documents_select_own ON storage.objects;
CREATE POLICY private_documents_select_own ON storage.objects
FOR SELECT TO authenticated
USING (bucket_id = 'private-documents' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text);

DROP POLICY IF EXISTS private_documents_insert_own ON storage.objects;
CREATE POLICY private_documents_insert_own ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'private-documents' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text);

DROP POLICY IF EXISTS private_documents_update_own ON storage.objects;
CREATE POLICY private_documents_update_own ON storage.objects
FOR UPDATE TO authenticated
USING (bucket_id = 'private-documents' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text)
WITH CHECK (bucket_id = 'private-documents' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text);

DROP POLICY IF EXISTS private_documents_delete_own ON storage.objects;
CREATE POLICY private_documents_delete_own ON storage.objects
FOR DELETE TO authenticated
USING (bucket_id = 'private-documents' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text);

COMMIT;

-- POST-INSTALL NOTES:
-- * Keep the Supabase service_role key ONLY on a trusted server; never in Android.
-- * Public media is intentionally public. Do not upload identity documents to post-media.
-- * Private document object path must be: <auth.uid()>/<random-file-name>
-- * Use signed URLs for private files and keep their expiry short.
-- * This SQL does not configure off-site backups, backup schedules, restore drills,
--   rate limits, malware scanning, or end-to-end encryption. Configure/test those separately.
-- * Before production: test policies with two different user accounts and anon access.
