# Estados e arquivos da katana — implementação integrada

| Estado | Constante | Clipe | Arquivo | Situação |
|---|---:|---|---|---|
| Embainhada | `SHEATHED` | `idle_sheathed` | `00_embainhada/idle_sheathed.animation.json` | Ativo |
| Saque | `DRAWING` | `draw_katana` | `01_saque/draw_katana.animation.json` | Ativo |
| Guarda de combate | `UNSHEATHED` | `idle_unsheathed` | `02_guarda_combate/idle_unsheathed.animation.json` | Ativo; pose-base |
| Ataque 1 | `ATTACK_1` | `attack_1_horizontal` | `03_ataques/attack_1_horizontal.animation.json` | Ativo |
| Ataque 2 | `ATTACK_2` | `attack_2_rising_diagonal` | `03_ataques/attack_2_rising_diagonal.animation.json` | Ativo |
| Ataque 3 | `ATTACK_3` | `attack_3_overhead_finisher` | `03_ataques/attack_3_overhead_finisher.animation.json` | Ativo |
| Guarda relaxada | `LOWER_GUARD` | `lower_guard` | `06_guarda_relaxada/lower_guard.animation.json` | Ativo |
| Caminhada | camada aditiva | `walk` | `05_movimento/walk.animation.json` | Ativo |
| Embainhar | `SHEATHING` | `sheath_katana` | `04_embainhar/sheath_katana.animation.json` | Ativo |

A ordem temporal pretendida é: saque → guarda de combate → caminhada/ataques → após 2 s sem atacar, guarda relaxada por 0,35 s → manter a guarda relaxada por mais 5 s → embainhar. Um novo ataque durante `LOWER_GUARD` ou caminhada reengaja o combo com cross-fade.
