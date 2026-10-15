# Backend VYBORA — Node.js + PostgreSQL / Supabase

API REST Express para a VYBORA. Pode usar PostgreSQL local via Docker ou PostgreSQL gerenciado pelo Supabase.

## Rotas implementadas

- `GET /health`: verifica API e banco.
- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`.
- `PATCH /api/users/me`, `GET /api/users/:username`, `POST /api/users/:id/follow`.
- `GET /api/posts`, `POST /api/posts`, `POST /api/posts/:id/like`.
- `GET/POST /api/posts/:id/comments`.
- `GET/POST /api/messages/:username`.
- `GET /api/notifications`, `POST /api/notifications/read`.
- `GET/PATCH /api/users/me/private-profile`: campos pessoais opcionais e privados.
- `GET /api/users/me/export`: exporta dados da própria conta em JSON.
- `POST /api/files/upload-url`, `GET /api/files`, `GET /api/files/:id/download-url`, `DELETE /api/files/:id`: arquivos privados em storage S3-compatível.
- `npm run migrate`: aplica migrações numeradas e verifica checksum.

## Portabilidade antes de tudo

O backend usa PostgreSQL padrão e storage S3-compatível. Supabase é opcional. Para detalhes de migração entre fornecedores, arquivos privados e exportação de dados, leia `docs/ARQUITETURA_PORTATIL.md`. As migrações ficam em `db/migrations/` e são aplicadas explicitamente com `npm run migrate`; a API não altera o banco automaticamente ao iniciar.

## Usar Supabase (banco gerenciado)

Siga `../SUPABASE/README_CONFIGURACAO.md`. O resumo é:
1. Crie o projeto Supabase manualmente no dashboard.
2. Para a instalação inicial no Supabase, execute `SUPABASE/01_schema.sql` no SQL Editor. Para instalações controladas pelo backend, prefira `npm run migrate` com a connection string configurada.
3. Copie `.env.supabase.example` para `.env`, preencha a connection string do **Session pooler**, um `JWT_SECRET` forte e `AUTO_MIGRATE=false`.
4. Publique esta API em uma hospedagem Node.js 20+ com HTTPS.
5. Teste `/health` e configure `API_BASE_URL` no Android para o domínio da API, não o domínio do Supabase.

Nunca coloque a senha do banco ou uma chave `service_role` no aplicativo Android.

## Usar localmente com Docker

1. Copie `.env.example` para `.env`.
2. Defina `POSTGRES_PASSWORD` e `JWT_SECRET` com valores fortes.
3. Execute `docker compose up -d --build`.
4. A API fica na porta 8080. Em produção, coloque-a atrás de HTTPS e não exponha PostgreSQL.

## Estado real e limitações

- A API tem autenticação própria com bcrypt e JWT e endpoints para contas, publicações, curtidas, comentários, seguidores, mensagens e notificações.
- O projeto Supabase ainda precisa ser criado na conta do proprietário e a API ainda precisa ser hospedada. Este ZIP não contém credenciais nem um serviço já publicado.
- O app Android está ligado ao backend para cadastro/login. As outras telas não estão todas ligadas aos endpoints.
- Upload de mídia, chat em tempo real, notificações push e lives precisam de integrações adicionais.
- Antes do lançamento, use backups, monitoramento, HTTPS, proteção contra abuso e testes de restauração.

## Variáveis

Veja `.env.example` para Docker local e `.env.supabase.example` para Supabase.

## Backups e integridade de arquivos

- Aplique novas migrações com `npm run migrate`; migrações já aplicadas não devem ser editadas.
- Após aplicar a migração 002, clientes que fazem upload precisam enviar `fileName`, `contentType`, `category` e `sizeBytes`, executar o `PUT` na URL temporária e chamar `POST /api/files/:id/complete` antes de usar o arquivo.
- Configure backup automático do banco e versionamento/replicação no armazenamento. O script `ops/backup-database.sh` produz dump criptografado com `age`; consulte `docs/ARQUITETURA_PORTATIL.md`.
- Não use com usuários reais antes de configurar e testar restauração do banco e dos objetos.
