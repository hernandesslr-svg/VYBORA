# Migração gradual do backend para o Supabase

## Estado desta etapa

- O projeto Supabase já tem o schema isolado `vybora_backend`, criado sem apagar nem alterar as tabelas nativas de `public`.
- O backend e o executor de migrations aceitam agora `DB_SCHEMA=vybora_backend`.
- `SKIP_DEMO_DATA=true` impede as migrations de remoção/semeadura de perfis demonstrativos (`004` e `005`) no destino.
- **A produção continua no Railway.** Não altere `DATABASE_URL` da produção antes de transferir e validar os dados.

## Por que o schema é isolado

O Supabase já possui tabelas próprias em `public` (como `profiles`, `posts`, `messages` e `notifications`) com estrutura e chaves estrangeiras diferentes das tabelas que o Express atual usa. Executar as migrations antigas em `public` causaria conflito. A aplicação legada usa `users`, `media_url`, `password_hash` e `cover_url`; o schema nativo do Supabase usa outro modelo e referencia `auth.users`.

## Configuração futura do serviço backend

Somente depois de um backup verificado e de importar os dados, configure no serviço backend:

- `DATABASE_URL`: URI de conexão PostgreSQL do projeto Supabase (guardar como segredo; nunca commitar).
- `DATABASE_SSL=true`: habilitar TLS conforme o certificado CA disponível na configuração do serviço.
- `DB_SCHEMA=vybora_backend`
- `SKIP_DEMO_DATA=true`

Não cole senhas ou chaves no GitHub, neste arquivo, no Android ou em mensagens. Se o certificado CA não estiver disponível no ambiente, configure-o antes da troca; não desative a validação TLS para contornar o problema.

## Ordem obrigatória para concluir

1. Fazer backup consistente do banco Railway e verificar que o arquivo pode ser lido/restaurado.
2. Inventariar contagens e relacionamentos no Railway (usuários, posts, curtidas, follows, comentários, mensagens, notificações, arquivos e metadados privados).
3. Transferir os dados para `vybora_backend`, preservando UUIDs, datas e relações; verificar contagens e amostras antes de apontar o backend para o destino.
4. Transferir os objetos do armazenamento S3 atual para Supabase Storage, mantendo uma tabela de correspondência entre `object_key` e caminho novo; verificar hashes/tamanhos e URLs assinadas.
5. Testar login/cadastro (incluindo Google), perfis/capa, feed, curtidas, comentários, seguidores, mensagens, notificações, upload/download e exclusão/recuperação.
6. Só então alterar as variáveis do backend e executar uma implantação controlada. Manter Railway e backups disponíveis para rollback até a validação final.

## Importante sobre autenticação

Esta etapa **não migra contas para Supabase Auth**. O backend atual verifica o token Google e emite seu próprio JWT, além de autenticar contas por senha usando a tabela `users`. Migrar isso para Supabase Auth exige uma migração de identidade planejada (incluindo usuários com senha), alterações coordenadas no Android e validação do provedor Google. Não se deve simplesmente trocar a URL do banco e esperar que o login Supabase funcione.

## Critério de conclusão

A migração só está concluída quando os dados e arquivos reais estiverem no destino, todos os testes acima passarem e o aplicativo publicado usar a nova configuração. Criar o schema ou conectar o banco vazio não conta como migração concluída.
