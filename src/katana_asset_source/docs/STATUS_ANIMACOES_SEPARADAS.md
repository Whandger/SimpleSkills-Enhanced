# Status real — caminhada, guarda relaxada e combo de combate da katana

## O que está implementado nesta entrega

- **9 clipes globais** separados por estado em `katana_asset_source/animations/` e espelhados byte a byte em `main/resources/assets/simpleskills/katana/animations/`:
  - `00_embainhada/idle_sheathed.animation.json`
  - `01_saque/draw_katana.animation.json`
  - `02_guarda_combate/idle_unsheathed.animation.json` (pose-base de combate)
  - `03_ataques/attack_1_horizontal.animation.json`
  - `03_ataques/attack_2_rising_diagonal.animation.json`
  - `03_ataques/attack_3_overhead_finisher.animation.json`
  - `04_embainhar/sheath_katana.animation.json`
  - `05_movimento/walk.animation.json` (ciclo contínuo de `0,80 s` partindo e fechando na pose-base `idle_unsheathed`)
  - `06_guarda_relaxada/lower_guard.animation.json` (transição de `0,35 s` com `hold_on_last_frame` mantendo a katana desembainhada na mão)
- **Manifestos atualizados** (`katana_asset_source/animations/manifest.json` e `main/resources/assets/simpleskills/katana/animations/manifest.json`) com os 9 clipes validados.
- **Combo real de 3 golpes no Java** (`KatanaStanceManager`, `KatanaClientStanceState`, `KatanaAnimationLibrary`):
  - Sequência encadeada `ATTACK_1` (`0,46 s`) -> `ATTACK_2` (`0,50 s`) -> `ATTACK_3` (`0,66 s`) -> `UNSHEATHED` (`idle_unsheathed`).
  - Buffer de entrada durante a aceleração do golpe para não reiniciar o primeiro ataque nem engolir cliques válidos dentro da janela de combo (`1,40 s` / `28 ticks`).
  - Sincronização multiplayer via `KatanaStancePayload` (usando os estados `ATTACK_1 = 5`, `ATTACK_2 = 6`, `ATTACK_3 = 7` e `LOWER_GUARD = 4` sem quebrar a assinatura do pacote).
- **Sequência de postura 2s + 5s implementada**:
  - Ao concluir `draw_katana` (`1,25 s` / `25 ticks`) ou um golpe, entra em `UNSHEATHED` (`idle_unsheathed`).
  - Após **2,0 s (`40 ticks`)** sem atacar, transiciona para `LOWER_GUARD` (`0,35 s` / `7 ticks`).
  - **Decisão de timing documentada:** o temporizador de **5,0 s (`100 ticks`)** começa **após o término da transição de `0,35 s` (`7 ticks`) para `lower_guard`** (`107 ticks` totais desde a entrada em `LOWER_GUARD`), executando `sheath_katana` (`1,35 s` / `27 ticks`) ao expirar.
- **Caminhada suave e preservação da empunhadura**:
  - `KatanaClientStanceState` suaviza o peso de movimento horizontal (`walkWeight`) e aplica o delta rítmico de `walk.animation.json` sobre `UNSHEATHED` ou `LOWER_GUARD`, retornando suavemente para `lower_guard` (ou `idle_unsheathed`) quando a velocidade horizontal zera.
  - `KatanaPlayerModelMixin` suprime o balanço oposto vanilla dos braços enquanto a katana está desembainhada para que ambas as mãos permaneçam firmes na tsuka em 3ª pessoa, preservando o ciclo vanilla das pernas.
  - Se o jogador atacar durante `LOWER_GUARD` ou `WALKING`, o sistema faz cross-fade rápido (`0,09 s`) para o golpe correspondente sem teleporte visual.


## Integração e ajuste de altura

Esta versão integra os nove clipes e a máquina Java descritos acima. A pose-base da katana usa Y=18.2, o ponto médio entre 13.2 e 25.2. Consulte `AJUSTE_ALTURA_KATANA.md`. A compilação e o teste in-game ainda dependem do workspace Gradle completo.
