# Atualizações do VYBORA para Android

## Objetivo

Evitar gerar APK em todo commit e evitar exigir reinstalação manual a cada pequena mudança.

## Estado técnico confirmado

O código Android que o workflow compila é um aplicativo nativo Kotlin/Jetpack Compose (por exemplo, MainActivity.kt e app/build.gradle.kts). Ele não é um bundle Expo/React Native. Por isso, **EAS Update / expo-updates não pode ser simplesmente habilitado nesse projeto**: esse mecanismo atualiza bundles JavaScript compatíveis, não código Kotlin nativo.

## Estratégia

1. **Backend (Railway):** publicar alterações de API separadamente; mudanças de servidor não exigem novo APK, desde que a API permaneça compatível com versões instaladas.
2. **Conteúdo e configurações remotas:** quando apropriado, carregar dados, textos, feed e configurações do backend, para poder atualizar esses dados sem recompilar o aplicativo. Não usar configuração remota para executar código arbitrário.
3. **APK de versão:** compilar apenas sob demanda (workflow_dispatch) ou ao criar uma tag versionada (v*). Não compilar a cada commit.
4. **Distribuição recomendada:** preparar publicação assinada via Google Play (AAB) para que usuários recebam atualizações normais da loja. Isso ainda exige uma nova versão do aplicativo para alterações nativas, mas evita o ciclo de baixar e instalar manualmente o APK.
5. **Segurança:** não colocar chaves de assinatura, tokens de publicação ou credenciais OAuth diretamente no repositório. Usar GitHub Actions Secrets.
6. **Atualizador próprio:** não implementar instalação silenciosa de APK. O Android exige fluxo de instalação autorizado pelo usuário e uma assinatura compatível; a Play Store é a opção preferida para distribuição pública.

## O que ainda é necessário para automatizar a publicação na Play Store

- Conta e aplicativo configurados no Google Play Console.
- Chave de assinatura/keystore protegida e alinhada com a chave usada nas versões já distribuídas.
- Credencial de serviço do Google Play Console armazenada em GitHub Actions Secrets.
- Pipeline de AAB assinado e teste em faixa interna antes de promover para produção.

## Limite importante

Uma atualização OTA de código nativo Kotlin não é suportada pelo workflow atual. Para obter atualizações de lógica sem APK, a arquitetura precisa ser desenhada explicitamente para isso (por exemplo, conteúdo e regras vindos da API, ou uma camada web compatível). Não afirmar que o app tem OTA até existir implementação e teste reais.
