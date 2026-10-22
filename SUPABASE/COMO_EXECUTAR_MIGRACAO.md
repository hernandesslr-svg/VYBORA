# Executar a cópia de dados Railway → Supabase

A rotina é manual e tem travas: não apaga registros, recusa qualquer tabela de destino que não esteja vazia e executa a cópia numa transação com verificação de contagens. Ela copia as tabelas do backend legado; não muda a produção nem transfere os objetos do bucket S3.

## 1. Configurar segredos no GitHub

No repositório `hernandesslr-svg/VYBORA`, abra **Settings → Secrets and variables → Actions → New repository secret** e crie:

- `RAILWAY_DATABASE_URL`: URL PostgreSQL do banco Railway de produção, com acesso de rede permitido a partir do GitHub Actions.
- `SUPABASE_DATABASE_URL`: URL de conexão PostgreSQL do projeto Supabase `jqomgrdlocdqaarfqkay`, com SSL.

Use credenciais de banco restritas ao necessário para leitura da origem e escrita no schema de destino, quando possível. Não coloque URLs/senhas em arquivos do repositório, issues ou mensagens. A URL de conexão do Railway precisa ser acessível fora da rede privada; se não for, configure acesso TCP temporário no painel Railway e restrinja-o, ou execute a rotina de dentro da rede autorizada. Não exponha o banco permanentemente.

## 2. Rodar a transferência

Abra **Actions → Prepare VYBORA database migration → Run workflow**, informe exatamente `MIGRATE` e execute. O workflow:
- confere as tabelas esperadas na origem e no destino;
- recusa destino com qualquer uma dessas tabelas já preenchida;
- copia IDs, hashes de senha, datas e relações;
- valida contagens e faz rollback se houver falha.

Leia os logs e só considere a cópia do banco aprovada se o job terminar em `SUCCESS` e as contagens coincidirem.

## 3. Depois da cópia — ainda obrigatório

A cópia do banco não transfere mídia do bucket Railway `vybora-media`. Antes de trocar o backend de produção, copiar e validar os objetos e metadados do armazenamento, conferir URLs assinadas, validar login (senha e Google), feed, perfis/capa, curtidas, comentários, seguidores, mensagens e notificações. Só então planejar uma implantação de corte com rollback.

**Não altere a variável de produção `DATABASE_URL` para o Supabase antes de concluir e validar todas as etapas.** O backend legado usa JWT próprio; esta rotina não migra identidades para Supabase Auth e não deve fazê-lo automaticamente.
