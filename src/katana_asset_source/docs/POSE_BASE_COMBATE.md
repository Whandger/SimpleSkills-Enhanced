# Pose-base oficial — guarda de batalha

A pose-base é `animation.katana_ultimo_juramento.idle_unsheathed`, no instante `0.0` de `02_guarda_combate/idle_unsheathed.animation.json`. Essa é a postura de batalha após o saque e a origem dos três golpes e da caminhada.

| Osso | Canal | X | Y | Z |
|---|---|---:|---:|---:|
| `saya` | `position` | 5.2 | 11.9 | 0.4 |
| `saya` | `rotation` | 64.0 | 9.0 | -5.0 |
| `saya` | `scale` | 0.76 | 0.76 | 0.76 |
| `katana` | `position` | -1.2 | **17.7** | 8.2 |
| `katana` | `rotation` | -114.0 | 14.0 | -8.0 |
| `katana` | `scale` | 0.76 | 0.76 | 0.76 |
| `body` | `rotation` | 0.0 | 8.0 | 0.0 |
| `right_arm` | `rotation` | -44.0 | 22.0 | 12.0 |
| `left_arm` | `rotation` | -36.0 | -48.0 | -14.0 |

## Continuidade estrutural verificada

- O fim de `draw_katana` corresponde ao início de `idle_unsheathed`.
- `attack_1_horizontal` começa na guarda-base e termina no início de `attack_2_rising_diagonal`.
- `attack_2_rising_diagonal` termina no início de `attack_3_overhead_finisher`.
- `attack_3_overhead_finisher` termina na guarda-base.
- `walk` começa e fecha exatamente na guarda-base.
- `lower_guard` começa na guarda-base.
- `sheath_katana` termina na pose embainhada.
- `special_sheathed_recovery` termina na pose embainhada. A entrada do especial é uma transição de duas mãos para a postura de saya; o cliente faz cross-fade a partir do idle embainhado.

## Ajuste vertical final

O Y-base anterior era 18.2. O ajuste final desloca a katana mais 0.5 unidade para baixo, para **17.7** (aproximadamente 1 px no renderer de calibração). Os JSONs-fonte/runtime e os dois projetos Blockbench estão alinhados. Não foram alterados braços, ombros, corpo, rotação da katana, saya ou geometria.
