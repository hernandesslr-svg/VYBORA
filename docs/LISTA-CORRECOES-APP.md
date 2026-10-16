# VYBORA — Lista de correções do aplicativo
Atualizado em 2026-10-16. Trabalhar somente na branch `main`; não criar branches novas.

## Estado validado
- [x] Login Google realizado com sucesso no APK testado.
- [x] Perfil abriu após o login.
- [ ] Não considerar funções sociais concluídas só porque a tela aparece; testar operações reais.

## 1. Login, conta e persistência
- [ ] Preservar o login Google que já funciona.
- [ ] Corrigir perda/reset dos dados da conta após desinstalar/reinstalar ou instalar um APK novo.
- [ ] Investigar o que está sendo “resetado”: perfil, publicações, seguidores, mensagens ou sessão.
- [ ] Garantir que os dados da conta sejam persistidos no backend/banco associados ao ID estável do usuário, não apenas em armazenamento local.
- [ ] Ao reinstalar, permitir entrar com a mesma conta e recuperar os dados do servidor.
- [ ] Não prometer restaurar sessão/token após desinstalação: por segurança, pode ser necessário autenticar de novo; os dados da conta devem continuar no servidor.
- [ ] Testar sessão após fechar/reabrir, sair/entrar e trocar conta.
- [ ] Se voltar `[16] Account reauth failed`, identificar se falha no Credential Manager Android ou se a chamada chega a `POST /api/auth/google`.

## 2. Perfil
- [ ] Adicionar edição separada de nome de exibição.
- [ ] Adicionar edição separada de nome de usuário (@username), com validação de formato e disponibilidade.
- [ ] Salvar foto, nome, usuário e biografia no backend.
- [ ] Carregar dados reais e persistir após novo login.
- [ ] Corrigir contagens e listas de seguidores/seguindo.

## 3. Início e publicações
- [ ] Carregar feed de publicações reais.
- [ ] Publicar texto, imagens e vídeos.
- [ ] Corrigir curtidas, comentários e contagens.
- [ ] Atualizar o feed depois de publicar.
- [ ] Mostrar carregamento, estado vazio, erro e botão para tentar novamente.

## 4. Botão de adicionar/publicar
- [ ] Ao tocar no botão `+`, abrir diretamente a câmera, conforme pedido.
- [ ] Incluir alternativa para escolher foto/vídeo da galeria, se possível, dentro do fluxo de captura/publicação.
- [ ] Solicitar permissão de câmera no momento necessário e tratar permissão negada.
- [ ] Confirmar captura, pré-visualização, legenda e publicação/upload real.

## 5. Pesquisa de usuários
- [ ] Corrigir erro “rota não encontrada” ao pesquisar usuários.
- [ ] Conferir rota de pesquisa existente no backend e alinhar URL/método/parâmetros do app.
- [ ] Pesquisar contas reais e abrir perfil de resultado.
- [ ] Tratar pesquisa vazia, sem resultados, carregamento e falhas da API.

## 6. Seguidores
- [ ] Seguir/deixar de seguir.
- [ ] Listar seguidores e seguindo com dados reais.
- [ ] Atualizar contagens e impedir relações duplicadas ou seguir a si próprio.

## 7. Mensagens
- [ ] Enviar/receber mensagens e carregar histórico real.
- [ ] Atualizar conversas e estados de carregamento/erro.
- [ ] Garantir autorização no servidor para que usuários só acessem conversas permitidas.

## 8. Mídia e armazenamento
- [ ] Corrigir seleção/captura de fotos e vídeos.
- [ ] Confirmar upload para o armazenamento configurado.
- [ ] Mostrar progresso e erros de envio.
- [ ] Confirmar que mídias permanecem acessíveis após novo login/reinstalação.

## 9. Lives e integração de transmissão
- [ ] Investigar mensagem “precisa de integrações no servidor de mídia”.
- [ ] Identificar e documentar o provedor/servidor de mídia necessário; não simular live funcionando sem integração real.
- [ ] Mover configurações e integrações de live para a tela Configurações.
- [ ] Trocar o texto “Configurar transmissão” por “Iniciar transmissão”.
- [ ] Posicionar “Iniciar transmissão” logo acima da barra inferior de navegação.
- [ ] Testar iniciar, visualizar e encerrar live depois que a integração estiver configurada.

## 10. Navegação e interface
- [ ] Remover a engrenagem do topo de Início, Mensagens e Lives.
- [ ] Substituir por ícone de menu de três linhas, igual ao usado no Perfil.
- [ ] Centralizar opções gerais e configurações de lives/integrações na tela Configurações.
- [ ] Testar abas Início, Mensagens, Publicar, Lives e Perfil.
- [ ] Corrigir botões sem ação, telas em branco, travamentos e layout.

## 11. Backend, banco e segurança
- [ ] Confirmar leitura e gravação reais no backend Railway: `https://vybora-production.up.railway.app`.
- [ ] Corrigir rotas inexistentes e manter app e backend usando contratos compatíveis.
- [ ] Verificar que perfil/publicações/seguidores/mensagens são ligados ao usuário correto no banco.
- [ ] Exibir mensagens úteis sem expor detalhes internos.
- [ ] Não armazenar senhas, tokens ou secrets neste documento ou no repositório.

## 12. Builds e regras de trabalho
- [x] Branch principal definida: `main`.
- [x] Login Google passou no teste manual mais recente.
- [ ] Fazer mudanças pequenas e commits claros diretamente na `main`.
- [ ] Compilar e testar o APK após correções; preservar a assinatura/keystore atual.
- [ ] Marcar [x] somente após teste real.
- [ ] Não criar outras branches.

## Configuração de referência (identificadores públicos)
- Pacote Android: `app.vybora.android`
- Android OAuth Client ID: `491553779497-75helub1i21c7tfa9e3v2krpdbj73juv.apps.googleusercontent.com`
- Web OAuth Client ID: `491553779497-llnliort4rsqu1ijna960e417tpt82rv.apps.googleusercontent.com`
- SHA-1 do keystore usado no build validado: `4B:E4:31:84:70:0B:86:CF:3A:32:60:4F:74:09:DE:94:17:2A:CB:3A`
- Build validado: https://github.com/hernandesslr-svg/VYBORA/actions/runs/38866027342
