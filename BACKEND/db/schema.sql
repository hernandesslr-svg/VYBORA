CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(24) NOT NULL UNIQUE,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  display_name VARCHAR(80) NOT NULL DEFAULT '',
  bio VARCHAR(300) NOT NULL DEFAULT '',
  avatar_url TEXT,
  is_verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (username ~ '^[a-zA-Z0-9_.]{3,24}$')
);
CREATE TABLE IF NOT EXISTS posts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  caption VARCHAR(2200) NOT NULL DEFAULT '',
  media_url TEXT,
  media_type VARCHAR(10) NOT NULL DEFAULT 'text' CHECK (media_type IN ('text','image','video')),
  visibility VARCHAR(10) NOT NULL DEFAULT 'public' CHECK (visibility IN ('public','private')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS posts_created_at_idx ON posts(created_at DESC);
CREATE INDEX IF NOT EXISTS posts_user_id_idx ON posts(user_id, created_at DESC);
CREATE TABLE IF NOT EXISTS post_likes (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY(user_id, post_id)
);
CREATE TABLE IF NOT EXISTS follows (
  follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY(follower_id, following_id),
  CHECK (follower_id <> following_id)
);
CREATE TABLE IF NOT EXISTS comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body VARCHAR(1200) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body VARCHAR(5000) NOT NULL,
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK(sender_id <> recipient_id)
);
CREATE INDEX IF NOT EXISTS messages_pair_idx ON messages(sender_id, recipient_id, created_at DESC);
CREATE TABLE IF NOT EXISTS notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
  kind VARCHAR(30) NOT NULL,
  entity_id UUID,
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- Arquivos privados: o conteúdo fica em armazenamento de objetos S3-compatível;
-- o PostgreSQL guarda somente metadados e a chave opaca do objeto.
CREATE TABLE IF NOT EXISTS user_files (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category VARCHAR(30) NOT NULL DEFAULT 'attachment' CHECK (category IN ('avatar','post_media','document','attachment')),
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  size_bytes BIGINT NOT NULL DEFAULT 0 CHECK (size_bytes >= 0),
  object_key TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS user_files_owner_idx ON user_files(owner_id, created_at DESC) WHERE deleted_at IS NULL;

-- Campos pessoais opcionais e mínimos; documentos sensíveis não devem ser públicos.
CREATE TABLE IF NOT EXISTS user_private_profiles (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  birth_date DATE,
  phone_e164 VARCHAR(20),
  country_code CHAR(2),
  locale VARCHAR(20),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Registo de versões aplicadas ao banco. Migrações novas devem ser numeradas e imutáveis.
CREATE TABLE IF NOT EXISTS schema_migrations (
  version VARCHAR(100) PRIMARY KEY,
  checksum CHAR(64) NOT NULL,
  applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Integrity and recovery metadata for user-uploaded objects.
ALTER TABLE user_files ADD COLUMN IF NOT EXISTS expected_size_bytes BIGINT;
ALTER TABLE user_files ADD COLUMN IF NOT EXISTS checksum_sha256 TEXT;
ALTER TABLE user_files ADD COLUMN IF NOT EXISTS object_etag TEXT;
ALTER TABLE user_files ADD COLUMN IF NOT EXISTS object_version_id TEXT;
ALTER TABLE user_files ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMPTZ;
ALTER TABLE user_files ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ready';
ALTER TABLE user_files DROP CONSTRAINT IF EXISTS user_files_status_check;
ALTER TABLE user_files ADD CONSTRAINT user_files_status_check CHECK (status IN ('pending','ready','quarantined'));
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_files_expected_size_check') THEN
    ALTER TABLE user_files ADD CONSTRAINT user_files_expected_size_check CHECK (expected_size_bytes IS NULL OR expected_size_bytes > 0);
  END IF;
END $$;
UPDATE user_files SET uploaded_at=created_at WHERE status='ready' AND uploaded_at IS NULL;
-- Do not silently delete file metadata when a user row is removed. Account deletion
-- must go through an explicit retention/deletion workflow that also handles objects.
ALTER TABLE user_files DROP CONSTRAINT IF EXISTS user_files_owner_id_fkey;
ALTER TABLE user_files ADD CONSTRAINT user_files_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT;
CREATE INDEX IF NOT EXISTS user_files_status_idx ON user_files(status, created_at);
