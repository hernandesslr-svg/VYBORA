# VYBORA Android + Backend 0.3.0

Projeto Android Kotlin/Jetpack Compose e API REST Node.js/Express com PostgreSQL. Inclui guia e SQL para configurar um banco PostgreSQL gerenciado pelo Supabase.

## Conteúdo

- `app/`: aplicativo Android.
- `BACKEND/`: API Express, schema SQL, Docker e exemplos de ambiente.
- `SUPABASE/01_schema.sql`: esquema a executar no SQL Editor do Supabase.
- `SUPABASE/README_CONFIGURACAO.md`: guia de criação e conexão do projeto Supabase.

## Atenção: não está publicado ainda

Este ZIP prepara os arquivos para conectar ao Supabase, mas não cria o projeto de nuvem, não configura credenciais na sua conta e não publica a API. Isso precisa ser feito com acesso autenticado à sua conta Supabase e a uma hospedagem Node.js.

A API implementa cadastro/login e endpoints de usuários, feed, publicações, curtidas, comentários, seguidores, mensagens e notificações. O Android só está ligado ao backend para cadastro/login; as outras telas ainda precisam ser conectadas. Upload de mídia, chat em tempo real, push e lives não estão concluídos.

## Compilação

Requer JDK 17 e acesso às dependências Gradle/Maven. Execute `./gradlew assembleDebug` nesta pasta em ambiente Android/Gradle configurado. A compilação ainda não foi validada nesta entrega.


## Portabilidade e dados pessoais

Leia `BACKEND/docs/ARQUITETURA_PORTATIL.md`. O Supabase é opcional: o backend usa PostgreSQL padrão, migrações versionadas e storage S3-compatível para arquivos privados. O ZIP não cria serviços externos automaticamente.

## Prioridade: segurança e preservação de dados
Consulte `BACKEND/docs/ARQUITETURA_PORTATIL.md` para a política de backups, recuperação e migração. Não cadastre usuários reais até que backups automáticos do banco, versionamento/replicação do armazenamento e testes de restauração estejam ativos.
