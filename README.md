# Contagem de Dias (Android)

App nativo em Kotlin e Jetpack Compose para o Contagem de Dias. Usa o mesmo projeto Firebase do site.

## Antes de compilar

1. Baixe o `google-services.json` do projeto Firebase (Configurações do projeto > Seus apps > Android, pacote `com.tamojunto.app`) e coloque em `app/google-services.json`.
2. No Firebase, em Authentication > Sign-in method, deixe o Google ativado. Em Configurações do projeto > Seus apps > Android, adicione esta impressão digital SHA-1 (da chave de depuração em `app/debug.keystore`): `11:1F:78:D0:65:81:F1:00:37:35:0E:9A:99:B7:02:5E:5C:F0:A4:E4`.
3. As regras do Realtime Database são as mesmas do site (`database.rules.json` do outro repositório).

## Gerar o APK

- Automático, sem instalar nada no computador: cada push roda o workflow "Gerar APK". Abra a aba **Actions** no GitHub, escolha a última execução e baixe o arquivo `tamo-junto-debug` (um .zip com o `app-debug.apk`). Copie o APK para o celular e instale (é preciso permitir instalação de fontes desconhecidas).
- Local, com Android Studio: abra a pasta do projeto e rode o app no celular.

## Estado atual

- Login com Google.
- Menu inferior: Início, Notas, Filmes, Coisinhas e Mais (as mesmas abas do site).
- Início: contagem de dias juntos a partir de `casais/{id}/data_namoro`.
- Filmes: filmes, séries e jogos em leitura, com filtros Todos, Feitos e Pendentes.
- Coisinhas: lista com marcar como feita (grava `done` no banco do casal).
- Notas: ainda em breve (a senha do bloco é conferida no servidor).
- Mais > Configurações: nome e foto do perfil, data de início, convite da outra pessoa, adicionar foto da página inicial, troca da senha do Bloco e sair. As fotos da página inicial só podem ser adicionadas por enquanto.

O endereço do site fica em `gradle.properties` (chave `siteUrl`). As configurações falam com as rotas `/api` do site, usando o mesmo login (o token do Firebase vai para `/api/auth/session`).

Ainda falta: adicionar e editar itens, buscar capas e dados no TMDB e no RAWG, Notas, missões, calendário, viagens, humor, grupos, perfil e músicas.
