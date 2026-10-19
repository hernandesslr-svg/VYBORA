# VYBORA para iPhone (iOS)

A versão iOS fica isolada em `ios/`. O aplicativo Android original continua em `app/`, sem compartilhar código de interface ou arquivos de compilação. Ambos permanecem na branch `main` do mesmo repositório.

## Estado desta primeira etapa

- Projeto Xcode nativo: `VYBORA.xcodeproj`.
- Interface SwiftUI inicial.
- Cadastro e login por e-mail/senha usando `POST /api/auth/register` e `POST /api/auth/login`.
- Token de sessão armazenado no Keychain do iOS.
- Feed inicial usando `GET /api/posts?limit=30`.
- A base de API está configurada no projeto como `https://vybora-production.up.railway.app`. A disponibilidade desse endereço precisa ser confirmada; o cliente exibe erro de conexão se a API estiver indisponível.
- Busca, publicação de mídia, reprodução de vídeo, mensagens, notificações e câmera ainda não estão integradas nesta versão inicial.

## Abrir e testar no Mac

1. Sincronize a branch `main` mais recente.
2. Abra `ios/VYBORA.xcodeproj` no Xcode.
3. Em **Signing & Capabilities**, selecione sua equipe Apple e mantenha **Automatically manage signing** ativado.
4. Conecte o iPhone ao Mac, desbloqueie-o e aceite **Confiar neste computador**.
5. Escolha o iPhone como destino e pressione **Run**.
6. Se o iOS solicitar, ative o Modo de Desenvolvedor no iPhone.

A Apple documenta o fluxo de compilação e execução em dispositivo físico aqui: https://developer.apple.com/documentation/xcode/running-your-app-on-simulated-or-physical-devices

## Compilar pelo terminal no Mac

Na raiz do repositório, depois de instalar o Xcode e suas ferramentas de linha de comando:

```sh
xcodebuild -project ios/VYBORA.xcodeproj -scheme VYBORA -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

Para instalar em um iPhone físico, use o Xcode com uma equipe de assinatura configurada. O arquivo de projeto usa o identificador inicial `app.vybora.ios`; altere-o no Xcode caso esse identificador já esteja registrado por outra equipe.

## Backend

A versão iOS usa os mesmos endpoints REST do backend VYBORA. Não há credenciais do banco, JWT secret ou chave administrativa embutidas no aplicativo. A API e o armazenamento precisam estar publicados e configurados no ambiente do backend antes dos testes de conta e mídia.
