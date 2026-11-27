# Status — combate e técnica especial da katana

## Animações

Há **11 clipes globais**, em JSONs separados por estado, espelhados byte a byte entre fonte e runtime. Não há tracks `firstperson.*`; a primeira pessoa projeta o mesmo rig global.

1. `idle_sheathed`
2. `draw_katana`
3. `idle_unsheathed` — guarda de batalha/pose-base
4. `attack_1_horizontal`
5. `attack_2_rising_diagonal`
6. `attack_3_overhead_finisher`
7. `sheath_katana`
8. `walk`
9. `lower_guard`
10. `special_sheathed`
11. `special_sheathed_recovery`

## Lógica Java

- Primeiro clique com katana embainhada apenas executa o saque; dano bloqueado até `UNSHEATHED`.
- O primeiro clique em `LOWER_GUARD` apenas reentra na guarda de batalha.
- Combo de três ataques, com buffer e janelas para encadeamento.
- Sem ataques por 2 s: `LOWER_GUARD` por 0,35 s; depois de mais 5 s, `SHEATHING` e retorno a `SHEATHED`.
- Caminhada usa overlay aditivo global e peso suavizado pela velocidade horizontal.
- Dano vanilla e indicadores vanilla de cooldown são desativados para a katana; dano customizado não usa o cooldown visual padrão.
- A mão secundária fica bloqueada enquanto a katana está na mão principal. Item existente é movido para o inventário ou dropado com aviso se não houver espaço.
- Especial `Shift + clique esquerdo`, somente embainhada e mirando um Mob: dash de até 6 blocos para trás do alvo, 1 s de stun, seis cortes com 2× o dano-base total distribuído entre eles, VFX do mod e a bainha segurada com as duas mãos.

## Altura

A pose-base usa Y=17.7 para `katana.position`, aproximadamente 1 px abaixo da versão anterior. Os projetos Blockbench e os JSONs-fonte/runtime estão alinhados.

## Validação e limitações

Os JSONs, os manifestos, as cópias fonte/runtime e os projetos Blockbench foram validados estruturalmente. A compilação Gradle e o teste em Minecraft 26.3 ainda precisam ser executados na raiz completa do repositório; esta entrega contém somente `src/`.
