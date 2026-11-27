# Katana do Último Juramento — animações separadas e máquina de combate

Esta entrega integra os nove clipes globais do Gemini, separados por estado e espelhados entre fonte e runtime. A terceira pessoa é a fonte de keyframes; a primeira pessoa projeta o mesmo rig, sem tracks `firstperson.*`.

## Clipes ativos

- `00_embainhada/idle_sheathed.animation.json` — katana embainhada (loop).
- `01_saque/draw_katana.animation.json` — desembainhar (1,25 s).
- `02_guarda_combate/idle_unsheathed.animation.json` — guarda/postura de batalha (loop, pose-base).
- `03_ataques/attack_1_horizontal.animation.json` — golpe horizontal (0,46 s).
- `03_ataques/attack_2_rising_diagonal.animation.json` — golpe diagonal ascendente (0,50 s).
- `03_ataques/attack_3_overhead_finisher.animation.json` — finalizador descendente (0,66 s).
- `04_embainhar/sheath_katana.animation.json` — embainhar (1,35 s).
- `05_movimento/walk.animation.json` — ciclo de caminhada (0,80 s, loop).
- `06_guarda_relaxada/lower_guard.animation.json` — transição para guarda relaxada, espada ainda na mão (0,35 s, hold).

`manifest.json` é a lista explícita usada pelo carregador Java. Cada JSON contém exatamente um clipe. Os diretórios `katana_asset_source/animations/` e `main/resources/assets/simpleskills/katana/animations/` devem permanecer idênticos.

## Máquina de estados

- Primeiro clique com a katana embainhada: `DRAWING` (25 ticks / 1,25 s).
- Depois do saque: `UNSHEATHED` na guarda de combate.
- Ataques: combo `ATTACK_1` → `ATTACK_2` → `ATTACK_3`, com buffer e janela de encadeamento.
- Após 2 s sem atacar: `LOWER_GUARD` (0,35 s de transição), mantendo a espada desembainhada.
- Na guarda relaxada, pode continuar caminhando com a katana em mãos.
- Após mais 5 s nessa postura: `SHEATHING` (1,35 s), depois `SHEATHED`.

## Altura da katana

O Y-base da katana está em **18.2** após uma correção fina de 1,0 unidade para baixo em relação a 19.2. Consulte `AJUSTE_ALTURA_KATANA.md`. A transição de `lower_guard` para `sheath_katana` foi alinhada para que o primeiro frame de embainhar parta da guarda relaxada final, sem salto.

## Fonte, runtime e Blockbench

- Fonte JSON: `katana_asset_source/animations/`.
- JSON carregado pelo jogo: `main/resources/assets/simpleskills/katana/animations/`.
- Projeto Blockbench editável e atualizado com os nove clipes: `katana_asset_source/models/katana_ultimo_juramento.bbmodel`.
- Cópia de runtime do projeto Blockbench: `main/resources/assets/simpleskills/katana/models/katana_ultimo_juramento.bbmodel`.

## Limites da validação

JSON, manifestos, espelhamento source/runtime, continuidade numérica e estrutura `.bbmodel` foram verificados neste pacote. A compilação Gradle e a execução dentro do Minecraft 26.3 ainda precisam ser feitas no workspace completo, pois o material de entrada contém somente `src/`, sem `gradlew`/`build.gradle` na raiz.
