# Pose-base oficial — guarda de combate

A pose-base é `animation.katana_ultimo_juramento.idle_unsheathed`, no instante `0.0` do JSON individual `02_guarda_combate/idle_unsheathed.animation.json`. Ela representa a postura com a katana desembainhada e pronta para lutar.

| Osso | Canal | X | Y | Z |
|---|---|---:|---:|---:|
| `saya` | `position` | 5.2 | 11.9 | 0.4 |
| `saya` | `rotation` | 64.0 | 9.0 | -5.0 |
| `saya` | `scale` | 0.76 | 0.76 | 0.76 |
| `katana` | `position` | -1.2 | 18.2 | 8.2 |
| `katana` | `rotation` | -114.0 | 14.0 | -8.0 |
| `katana` | `scale` | 0.76 | 0.76 | 0.76 |
| `body` | `rotation` | 0.0 | 8.0 | 0.0 |
| `right_arm` | `rotation` | -44.0 | 22.0 | 12.0 |
| `left_arm` | `rotation` | -36.0 | -48.0 | -14.0 |

## Continuidade verificada em todos os clipes

- `draw_katana` (`1.25s`) == `idle_unsheathed` (`0.0s`)
- `attack_1_horizontal` (`0.0s`) == `idle_unsheathed` (`0.0s`); `attack_1_horizontal` (`0.46s`) == `attack_2_rising_diagonal` (`0.0s`)
- `attack_2_rising_diagonal` (`0.50s`) == `attack_3_overhead_finisher` (`0.0s`)
- `attack_3_overhead_finisher` (`0.66s`) == `idle_unsheathed` (`0.0s`)
- `walk` (`0.0s`) == `walk` (`0.8s`) == `idle_unsheathed` (`0.0s`)
- `lower_guard` (`0.0s`) == `idle_unsheathed` (`0.0s`)
- `sheath_katana` (`0.0s`) == `lower_guard` (`0.35s`); `sheath_katana` (`1.35s`) == `idle_sheathed` (`0.0s`)


## Ajuste intermediário de altura (integração)

O Y-base da katana foi definido em **19.2 unidades**: o ponto médio entre o valor original `13.2` e a tentativa anterior `25.2` (que ficou alta demais). O mesmo deslocamento `+6.0` foi aplicado de forma consistente aos keyframes ativos de guarda, caminhada, guarda relaxada e três ataques, além do último frame do saque. Os JSONs de fonte e runtime estão sincronizados.


## Correção fina de repouso (27/11/2026)

A katana foi deslocada 1,0 unidade para baixo no eixo Y (aproximadamente 2 px na prévia usada para calibrar). A correção preserva a pose de combate, caminhada, ataques e guarda relaxada. As transições de saque e embainhar recebem compensação gradual para manter os pontos de entrada/saída contínuos. A pose embainhada (`idle_sheathed`) permanece inalterada.
