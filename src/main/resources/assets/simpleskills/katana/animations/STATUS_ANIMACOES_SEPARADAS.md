# Status real — animações separadas da katana

## O que está pronto nesta entrega

- Os 7 clipes existentes foram separados em 7 arquivos JSON individuais.
- Os arquivos estão agrupados por estado na pasta `katana_asset_source/animations/`.
- `manifest.json` lista os 7 clipes carregados em runtime.
- `KatanaAnimationLibrary.java` lê o manifest e abre cada JSON individual.
- As cópias de source e runtime foram espelhadas e verificadas.
- Modelos, texturas, ícones, renders e o `.bbmodel` foram preservados.
- Não há dependência de Live Sync nem patch externo.

## O que ainda NÃO está implementado

- `walk`: existe apenas como template vazio em `katana_asset_source/animations/05_movimento/walk.template.json`.
- `lower_guard`: existe apenas como template vazio em `katana_asset_source/animations/06_guarda_relaxada/lower_guard.template.json`.
- O `KatanaStanceManager` atual ainda usa os estados `SHEATHED`, `DRAWING`, `UNSHEATHED` e `SHEATHING`; ainda não contém o estado `LOWER_GUARD` nem a espera adicional de 5 segundos.
- A caminhada ainda não é selecionada/aplicada pelo Java.
- Os três JSONs de ataque existem e são carregados pelo manifest, mas a busca no código atual não encontrou seleção/execução desses IDs pelo sistema de postura. O gatilho de ataque atual atualiza a postura/temporizador, mas não dispara explicitamente `attack_1_horizontal`, `attack_2_rising_diagonal` ou `attack_3_overhead_finisher`.

## Especificação desejada para o próximo trabalho (Gemini)

1. `idle_unsheathed` é a guarda de combate e a pose-base exata.
2. Após o saque, o jogador chega a `idle_unsheathed`. Os três ataques rápidos devem começar nessa pose, transicionar com fluidez e terminar de volta nela.
3. Após 2 segundos sem atacar, transicionar para `lower_guard`; a katana continua fora da bainha e na mão.
4. Nessa fase relaxada, o jogador pode continuar caminhando com a katana na mão; o ciclo `walk` deve misturar suavemente sem retirar a arma.
5. Após mais 5 segundos sem atacar na guarda relaxada, iniciar `sheath_katana`.
6. Se atacar durante `lower_guard` ou `walk`, voltar suavemente à guarda de combate e iniciar o golpe.
7. A primeira pessoa deve projetar o mesmo rig e os mesmos keyframes globais; não criar `firstperson.*`.
8. Implementar a execução dos ataques e caminhada no Java; criar JSON sem conectar os gatilhos não resolve.

Esta entrega reorganiza arquivos e muda o carregamento; não afirma implementar os estados pendentes acima.
