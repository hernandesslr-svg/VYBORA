-- Persistência da imagem de capa e categoria própria para arquivos de capa.
ALTER TABLE users ADD COLUMN IF NOT EXISTS cover_url TEXT;
ALTER TABLE user_files DROP CONSTRAINT IF EXISTS user_files_category_check;
ALTER TABLE user_files ADD CONSTRAINT user_files_category_check
  CHECK (category IN ('avatar','cover','post_media','document','attachment'));
