# Guia para editar animações individuais e enviar ao Gemini

## Fonte por clipe

Edite um arquivo por vez dentro de `katana_asset_source/animations/`. A pasta é organizada pelo estado da katana. O nome do clipe e o identificador completo dentro do JSON devem ser preservados.

## Como aplicar uma alteração

1. Abra o JSON do clipe desejado.
2. Edite somente os keyframes/canais daquele movimento.
3. Valide o JSON.
4. Copie a mesma versão para o caminho espelhado em `main/resources/assets/simpleskills/katana/animations/`.
5. Confira se `manifest.json` continua apontando para o arquivo correto.
6. Compile e teste no Minecraft.

## O que enviar ao Gemini

Para alterar um golpe, envie o prompt `katana_asset_source/docs/PROMPT_GEMINI_ANIMACOES_SEPARADAS.md` e o JSON daquele golpe, a pose-base `02_guarda_combate/idle_unsheathed.animation.json`, o `manifest.json`, além das classes Java listadas em `MAPA_DOS_ARQUIVOS.md`. Para caminhada/guarda relaxada, envie também os templates dessas pastas e a especificação de estados.

## Limitação da versão recebida

Há sete clipes com keyframes reais nesta fonte. `walk` e `lower_guard` são templates vazios, não animações prontas. O código Java ainda não seleciona esses estados. A lógica atual de estados foi preservada nesta reorganização para não introduzir comportamento não solicitado silenciosamente.
