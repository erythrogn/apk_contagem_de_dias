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
- Menu inferior: Início, Notas, Filmes, Coisinhas e Mais (as mesmas abas do site).
- Início: contagem de dias juntos a partir de `casais/{id}/data_namoro`.
- Filmes: filmes, séries e jogos em leitura, com filtros Todos, Feitos e Pendentes.
- Coisinhas: lista com marcar como feita (grava `done` no banco do casal).
- Notas: ainda em breve (a senha do bloco é conferida no servidor).
- Mais > Configurações: nome e foto do perfil, data de início, convite da outra pessoa, adicionar foto da página inicial, troca da senha do Bloco e sair. As fotos da página inicial só podem ser adicionadas por enquanto.

O endereço do site fica em `gradle.properties` (chave `siteUrl`). As configurações falam com as rotas `/api` do site, usando o mesmo login (o token do Firebase vai para `/api/auth/session`).

Ainda falta: adicionar e editar itens, buscar capas e dados no TMDB e no RAWG, Notas, missões, calendário, viagens, humor, grupos, perfil e músicas.
