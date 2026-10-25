-- Reverte os dados de demonstração que foram inseridos por engano pela migração 005.
-- Remove exclusivamente as seis contas sintéticas identificadas pelo domínio @vybora.test.
DO $$
DECLARE test_ids UUID[];
BEGIN
  SELECT array_agg(id) INTO test_ids FROM users WHERE email LIKE '%@vybora.test';
  IF test_ids IS NULL OR cardinality(test_ids)=0 THEN
    RAISE NOTICE 'Nenhuma conta sintética @vybora.test encontrada.';
    RETURN;
  END IF;
  DELETE FROM user_files WHERE owner_id = ANY(test_ids);
  DELETE FROM users WHERE id = ANY(test_ids);
  RAISE NOTICE 'Contas sintéticas removidas: %', cardinality(test_ids);
END $$;
