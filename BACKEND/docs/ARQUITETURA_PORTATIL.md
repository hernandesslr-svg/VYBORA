# VYBORA — arquitetura portátil (banco, arquivos e migração)

## Regra estrutural

O Android fala exclusivamente com a API VYBORA. A API fala com PostgreSQL e com armazenamento privado S3-compatível. Não coloque SDK/chaves de fornecedor no APK nem espalhe chamadas específicas do Supabase pelas telas.

- **Banco:** PostgreSQL padrão. O Supabase é apenas um fornecedor PostgreSQL, não uma dependência obrigatória do app.
- **Arquivos:** bucket privado S3-compatível. `S3_ENDPOINT`, região, bucket e credenciais são configuração do servidor; a API gera URLs temporárias e autorizadas.
- **Migrações:** `db/migrations/NNN_nome.sql` em ordem. Não altere migrações já aplicadas; crie uma nova. `npm run migrate` verifica SHA-256 e recusa migração antiga alterada.
- **Portabilidade do usuário:** `GET /api/users/me/export` exporta JSON com dados da conta. Arquivos binários precisam ser copiados pelo serviço de armazenamento e seus metadados exportados junto.

## Rotas de arquivos

- `POST /api/files/upload-url` — autenticado; aceita imagens JPEG/PNG/WebP/GIF, vídeos MP4/WebM e PDF; retorna URL temporária de upload e registro do arquivo.
- `GET /api/files` — lista arquivos da própria conta.
- `GET /api/files/:id/download-url` — gera URL temporária apenas para o dono.
- `DELETE /api/files/:id` — marca para exclusão; não apaga imediatamente o objeto físico.

O cliente envia o arquivo com `PUT` à URL temporária, usando o `Content-Type` devolvido. O upload deve ser concluído pelo Android; a API não verifica automaticamente que o objeto foi enviado nem o tamanho real. Antes de produção, acrescentar verificação de objeto/tamanho, quota por usuário, varredura antimalware quando aplicável e rotina de expurgo dos arquivos marcados.

## Publicação e atualização

1. Faça backup verificável do PostgreSQL e copie objetos do bucket antigo para o novo.
2. Configure `DATABASE_URL` para o novo PostgreSQL e as variáveis `S3_*` para o novo armazenamento.
3. Execute `npm run migrate` no release/deploy; depois publique a API.
4. Teste `/health`, login, upload/download, exportação de dados e restauração do backup.
5. Mantenha o backend com versão compatível com o schema. Faça mudanças aditivas primeiro; só remova colunas em release posterior depois que nenhum código antigo depender delas.

## Segurança e privacidade

- Buckets devem ser privados, sem acesso público anônimo.
- Nunca coloque credenciais PostgreSQL/S3/JWT no APK, no repositório ou em conversas.
- Colete apenas dados pessoais necessários. Data de nascimento/telefone/localidade são opcionais e ficam em `user_private_profiles`, não no perfil público.
- Não use este esquema para armazenar documentos oficiais sensíveis até implementar consentimento/propósito, controles de acesso e auditoria, retenção/exclusão, criptografia gerenciada e revisão LGPD. PDF é permitido como arquivo privado, mas isso não substitui esses controles.
- O endpoint de exportação inclui mensagens entre o usuário e outras pessoas; trate o arquivo exportado como dado altamente privado e adicione reautenticação/limite de frequência antes do lançamento.

## Limitações atuais que não devem ser confundidas com conclusão

O pacote prepara a camada de API e o esquema. Ainda é necessário integrar essas rotas às telas Android, configurar o provedor de armazenamento, aplicar a migração no banco real e testar ponta a ponta. Não existe banco ou bucket criado automaticamente por este ZIP.

## Proteção contra perda de arquivos e recuperação

- O bucket de arquivos deve ser **privado**, com criptografia em repouso, acesso público bloqueado e credenciais exclusivas do backend. Nunca coloque chaves S3 no APK.
- O upload agora exige tamanho declarado, URL temporária de 5 minutos e chamada `POST /api/files/:id/complete`. O backend verifica `HEAD` no objeto e só libera listagem/download quando tamanho e tipo conferem. Limites atuais: 25 MB por padrão e 100 MB para vídeo em `post_media`.
- Arquivos não concluídos ficam em `pending` e não são liberados para download. Configure um trabalho de limpeza com período de carência, depois de verificar objetos e logs; não apague automaticamente sem política definida.
- `DELETE /api/files/:id` é exclusão lógica. O objeto não é apagado imediatamente, permitindo recuperação manual dentro da janela de retenção. É necessário configurar uma rotina de retenção aprovada antes de qualquer exclusão física.
- A migração `002_file_integrity_and_recovery.sql` registra estado, tamanho esperado, data de upload e metadados de integridade. Arquivos existentes são marcados como prontos; faça inventário e confira o bucket antes de migrar dados reais.

## Backups obrigatórios antes de produção

1. Configure backups automáticos gerenciados do PostgreSQL, com PITR/recuperação pontual quando disponível e retenção compatível com os objetivos de recuperação.
2. Ative versionamento de objetos no bucket e, idealmente, replicação para uma conta/provedor separado. Um backup do PostgreSQL **não** copia o conteúdo das fotos e vídeos.
3. Use `ops/backup-database.sh` para gerar dump PostgreSQL criptografado com `age`; requer `pg_dump`, `age`, `DATABASE_URL` e `AGE_RECIPIENT`. `BACKUP_S3_URI` é opcional para uma segunda cópia via AWS CLI. Guarde a chave privada `age` separada dos backups.
4. Restrinja acesso às cópias, use credenciais distintas e proteja-as contra exclusão/alteração. Nunca grave segredos no repositório.
5. Faça teste periódico de restauração em ambiente isolado e compare tabelas, contagens e amostras de arquivos. Backup sem teste de restauração não é considerado validado.
6. Antes de trocar de fornecedor, pause gravações ou use uma estratégia de replicação/sincronização final, copie banco **e objetos**, compare contagens/checksums e teste login, publicações, mensagens e downloads. Só então altere o tráfego. Preserve o ambiente antigo até aprovar a validação.

**Importante:** o código oferece mecanismos, mas não ativa sozinho backups gerenciados, versionamento, replicação, monitoramento ou restauração. Essas configurações dependem do provedor escolhido e precisam ser concluídas antes de receber dados reais.
