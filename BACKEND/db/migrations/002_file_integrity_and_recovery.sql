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
