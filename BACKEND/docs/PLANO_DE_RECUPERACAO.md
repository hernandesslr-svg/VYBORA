# VYBORA — plano operacional de recuperação

Este documento é um checklist operacional. Os passos dependentes do provedor não ficam ativos automaticamente ao instalar o código.

## Antes de abrir o cadastro ao público

- [ ] Banco PostgreSQL hospedado com TLS e backups automáticos/PITR configurados.
- [ ] Executar `npm run migrate` em janela controlada e registrar versão/checksums aplicados.
- [ ] Bucket privado, criptografia em repouso, bloqueio de acesso público e versionamento habilitados.
- [ ] Replicação dos objetos para uma conta/provedor separado, com credenciais independentes.
- [ ] Segredos de produção guardados no gerenciador de segredos; `JWT_SECRET` forte e exclusivo.
- [ ] CORS limitado aos domínios oficiais e HTTPS obrigatório.
- [ ] Alertas para falhas de backup, espaço, erros 5xx, falhas de upload e banco indisponível.
- [ ] Executar `ops/backup-database.sh`, guardar chave privada `age` fora do servidor e confirmar hash.
- [ ] Restaurar o dump num banco isolado e confirmar que a API consegue ler os dados.
- [ ] Restaurar uma amostra de objetos de arquivo e confirmar que seus metadados correspondem.

## Incidente ou suspeita de perda

1. Suspenda exclusões e migrações; se necessário, pause temporariamente gravações.
2. Preserve logs, IDs de arquivos, horários e cópias atuais; não sobrescreva o backup conhecido como bom.
3. Restaure banco e objetos num ambiente isolado, nunca por cima da única cópia disponível.
4. Compare contagens por tabela, IDs, tamanhos e checksums disponíveis; teste login, feed, mensagens e downloads.
5. Só troque o tráfego depois da validação e mantenha o ambiente original protegido para rollback.
6. Registre causa, janela afetada e ações para impedir recorrência.

## Troca de provedor

1. Escolha destino PostgreSQL compatível e armazenamento privado compatível com S3 ou implemente outro adaptador.
2. Faça cópia inicial do banco e objetos; sincronize alterações recentes ou entre em janela de manutenção para evitar gravações divergentes.
3. Preserve IDs, timestamps, chaves de objetos e relações. Não reescreva IDs sem plano de mapeamento.
4. Valide contagens e checksums, credenciais/login, downloads, uploads e permissões de acesso.
5. Atualize as variáveis de ambiente do backend e faça testes controlados.
6. Mantenha origem e backups antigos até encerrar o período de observação acordado.

## Exclusão e retenção

A exclusão lógica de um arquivo pode ser revertida por `POST /api/files/:id/restore` enquanto o objeto continuar disponível e íntegro. A exclusão definitiva deve seguir uma política documentada, respeitar obrigações legais e não pode apagar cópias de backup antes do fim da retenção aplicável. A exclusão de conta ainda exige fluxo dedicado para tratar conteúdo, objetos e prazos de retenção.
