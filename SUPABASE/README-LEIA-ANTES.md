# VYBORA — Supabase seguro (SQL inicial)

## Conteúdo
- `01_vybora_supabase_seguro.sql`: schema inicial, tabelas, RLS, permissões mínimas e buckets do Storage.

## Antes de executar
1. Confirme que está no projeto Supabase correto.
2. Faça backup/exportação se já houver dados importantes.
3. Este arquivo é para instalação inicial. Ele não converte automaticamente uma estrutura antiga ou diferente.
4. Revise o script e execute primeiro em projeto de teste, se possível.
5. Nunca coloque `service_role` no aplicativo Android.

## Dados e arquivos
- `profiles`: somente dados destinados a aparecer no perfil público.
- `private_user_data`: dados pessoais privados opcionais.
- `user_documents`: metadados de documentos; o conteúdo binário fica no bucket privado `private-documents`.
- `post-media`: mídia pública de publicações; os caminhos devem começar pelo UUID do usuário.
- `private-documents`: arquivos privados; os caminhos devem começar pelo UUID do proprietário e só o dono pode ler/escrever.

## Limitações importantes
Este SQL não ativa backups automáticos, não prova restauração e não configura monitoramento, limites de requisição, verificação antimalware nem integração das telas Android. É necessário configurar backups no Supabase/plano escolhido e fazer cópias independentes do Storage, além de testar restauração. Para migrar de fornecedor, exporte banco e objetos, compare contagens e hashes e só então altere a API.

## Testes mínimos antes de usuários reais
- Usuário A não consegue ler dados privados, documentos ou mensagens do usuário B.
- Usuário não consegue mudar `is_verified`, IDs de proprietário, timestamps protegidos ou destinatário de mensagens.
- Anônimo consegue ler somente dados destinados a serem públicos.
- Upload privado exige autenticação e pasta com o UUID do próprio usuário.
- Restauração de backup e recuperação de arquivos são testadas.
