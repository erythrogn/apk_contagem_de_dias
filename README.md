# tamo junto (Android)

App nativo em Kotlin e Jetpack Compose para o "tamo junto". Usa o mesmo projeto Firebase do site.

## Antes de compilar

1. Baixe o `google-services.json` do projeto Firebase (Configurações do projeto > Seus apps > Android, pacote `com.tamojunto.app`) e coloque em `app/google-services.json`.
2. No Firebase, em Authentication > Sign-in method, deixe o Google ativado. Em Configurações do projeto > Seus apps, adicione a impressão digital SHA-1 da chave de depuração.
3. As regras do Realtime Database são as mesmas do site (`database.rules.json` do outro repositório).

## Gerar o APK

- Automático: cada push roda o workflow "Gerar APK". Baixe `tamo-junto-debug` na aba Actions.
- Local, com Android Studio: abra a pasta do projeto e rode o app no celular.

## Estado atual

- Login com Google.
- Tela inicial que lê o nome e a situação do casal em `usuarios/{uid}`.

Ainda falta portar as demais telas do site (filmes, séries, jogos, músicas, viagens, calendário, humor, grupos e cartinhas).
