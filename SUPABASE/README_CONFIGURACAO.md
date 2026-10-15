# Configurar Supabase para a VYBORA

Este pacote deixa o projeto preparado para usar PostgreSQL gerenciado pelo Supabase. **Ele não cria uma conta nem um projeto Supabase automaticamente**: isso exige entrar na sua conta e aceitar os termos no painel.

## 1. Criar o banco

1. Abra https://supabase.com/dashboard e entre/crie sua conta.
2. Clique em **New project**, dê o nome `VYBORA` e escolha uma senha forte para o banco.
3. Escolha uma região próxima dos usuários iniciais. Guarde a senha em local seguro.
4. Aguarde o projeto terminar de provisionar.

## 2. Criar as tabelas

1. No projeto, abra **SQL Editor**.
2. Abra o arquivo `SUPABASE/01_schema.sql` deste ZIP.
3. Copie todo o conteúdo para uma nova consulta no SQL Editor e execute.
4. Confirme que as tabelas `users`, `posts`, `post_likes`, `follows`, `comments`, `messages`, `notifications`, `user_files`, `user_private_profiles` e `schema_migrations` foram criadas.

O esquema é para uso pelo backend Node.js. O app Android **não deve** conectar diretamente ao banco nem conter senha de banco ou chave `service_role`.

## 3. Configurar o backend

1. Abra **Project Settings > Database > Connection string** no Supabase.
2. Selecione **Session pooler** para o backend Node.js de longa duração e copie a URI PostgreSQL. Se a hospedagem só tiver IPv4 e a conexão direta não funcionar, use o pooler.
3. Copie `BACKEND/.env.supabase.example` para `BACKEND/.env`.
4. Substitua `DATABASE_URL` pela URI real copiada do painel. Preserve `sslmode=require` se vier na URI; se não vier, mantenha `DATABASE_SSL=true`.
5. Troque `JWT_SECRET` por um segredo forte e único. Gere um com `openssl rand -hex 32` em um ambiente confiável.
6. Mantenha `AUTO_MIGRATE=false`, pois o esquema já foi aplicado no SQL Editor.
7. Nunca envie `BACKEND/.env` para GitHub, nem compartilhe a URI ou a senha do banco.

**Importante:** a senha dentro da URI precisa estar codificada para URL se tiver caracteres especiais. Não cole credenciais em conversas públicas.

## 4. Publicar a API

O Supabase hospeda o banco, mas não publica automaticamente esta API Express. Publique a pasta `BACKEND` em um serviço que execute Node.js 20+ (por exemplo, uma plataforma de hospedagem de aplicações), defina as variáveis do `.env` no painel seguro do provedor e habilite HTTPS.

Configure `CORS_ORIGIN` com a origem exata de cada cliente web que realmente usar a API. O aplicativo Android nativo não depende de CORS; evite `*` se houver clientes web com credenciais.

Depois da publicação, teste `https://SEU-DOMINIO/health`. A resposta esperada inclui `status: "ok"` e `database: "connected"`.

## 5. Conectar o Android

Depois de publicar a API, configure a URL HTTPS pública no `API_BASE_URL` do aplicativo em `app/build.gradle.kts` e gere um novo APK. Não use a URL do Supabase como `API_BASE_URL`: o aplicativo deve falar com a API Express, e a API fala com o PostgreSQL do Supabase.

## 6. Preparar para trocar de fornecedor

O app usa a API VYBORA, não endpoints específicos do Supabase. PostgreSQL pode ser migrado para outro PostgreSQL compatível e arquivos podem ser copiados entre armazenamentos S3-compatíveis mudando as variáveis `S3_*`. Leia `BACKEND/docs/ARQUITETURA_PORTATIL.md`. Faça e teste exportação/backup antes de qualquer migração.

## 7. Segurança antes de abrir ao público

- Configure backup do banco e teste a restauração.
- Configure monitoramento e limites de tráfego.
- Mantenha a senha do banco e `JWT_SECRET` somente no backend.
- Não exponha o banco PostgreSQL diretamente à internet pública.
- A conta atual da API usa JWT próprio; o Supabase Auth não está ativado automaticamente por este pacote.
- Upload de fotos/vídeos, chat em tempo real, push e lives precisam de integrações adicionais; não estão concluídos só por configurar o banco.
