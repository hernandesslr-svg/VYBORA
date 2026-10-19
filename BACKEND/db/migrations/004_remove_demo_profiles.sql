-- Remove somente contas de demonstração identificáveis; contas reais não são afetadas.
DO $$
DECLARE
  demo_ids UUID[];
BEGIN
  SELECT array_agg(id) INTO demo_ids
  FROM users
  WHERE right(lower(username), 5) = '.demo'
     OR right(lower(username), 6) = '_teste'
     OR lower(username) IN ('ana_rose', 'ana_teste', 'caio_teste', 'marina_teste', 'lucas_teste')
     OR lower(email) LIKE '%@example.invalid';

  IF demo_ids IS NULL OR cardinality(demo_ids) = 0 THEN
    RAISE NOTICE 'Nenhum perfil de teste encontrado.';
    RETURN;
  END IF;

  -- Remove os metadados de arquivos privados associados a essas contas para respeitar a FK RESTRICT.
  DELETE FROM user_files WHERE owner_id = ANY(demo_ids);
  -- As publicações, curtidas, seguidores, comentários e mensagens vinculados usam ON DELETE CASCADE.
  DELETE FROM users WHERE id = ANY(demo_ids);
  RAISE NOTICE 'Perfis de teste removidos: %', cardinality(demo_ids);
END $$;
