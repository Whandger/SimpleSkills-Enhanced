# Katana do Último Juramento — animações globais e máquina de combate

A terceira pessoa é a fonte de keyframes. A primeira pessoa projeta o mesmo rig global, sem tracks `firstperson.*`. Os clipes são separados em JSONs individuais e registrados por `manifest.json`.

## Clipes ativos (11)

- `00_embainhada/idle_sheathed.animation.json` — katana embainhada (loop).
- `01_saque/draw_katana.animation.json` — desembainhar (1,25 s).
- `02_guarda_combate/idle_unsheathed.animation.json` — guarda de batalha e pose-base (loop).
- `03_ataques/attack_1_horizontal.animation.json` — golpe horizontal (0,46 s).
- `03_ataques/attack_2_rising_diagonal.animation.json` — golpe diagonal ascendente (0,50 s).
- `03_ataques/attack_3_overhead_finisher.animation.json` — finalizador descendente (0,66 s).
- `04_embainhar/sheath_katana.animation.json` — embainhar (1,35 s).
- `05_movimento/walk.animation.json` — ciclo de caminhada (0,80 s, loop).
- `06_guarda_relaxada/lower_guard.animation.json` — transição e postura de guarda relaxada (0,35 s).
- `07_especial/special_sheathed.animation.json` — técnica especial com katana embainhada e as duas mãos na saya (1,85 s).
- `07_especial/special_sheathed_recovery.animation.json` — retorno à pose embainhada (0,30 s).

`manifest.json` é a lista explícita usada pelo carregador Java. Cada JSON contém exatamente um clipe. `katana_asset_source/animations/` e `main/resources/assets/simpleskills/katana/animations/` devem permanecer idênticos.

## Máquina de estados normal

- Primeiro clique com a katana embainhada: `DRAWING` (25 ticks / 1,25 s). Esse clique não causa dano.
- Depois do saque: `UNSHEATHED`, na guarda de batalha.
- Somente `UNSHEATHED` pode iniciar um ataque que causa dano. Em `LOWER_GUARD`, o primeiro clique apenas retorna à guarda de batalha; o próximo inicia o golpe.
- Combo: `ATTACK_1` → `ATTACK_2` → `ATTACK_3`, com buffer e janelas de encadeamento.
- Após 2 s sem atacar: `LOWER_GUARD` (0,35 s de transição), mantendo a espada desembainhada.
- Depois de mais 5 s na guarda relaxada: `SHEATHING` (1,35 s), depois `SHEATHED`.
- A caminhada mistura o ciclo `walk` de forma aditiva sobre a guarda global; as pernas continuam com o ciclo vanilla e os braços ficam alinhados à empunhadura.

O ataque vanilla e os indicadores vanilla de cooldown são desativados enquanto a katana está na mão principal. Os ataques normais usam dano próprio baseado no atributo `ATTACK_DAMAGE`, aplicado no frame de impacto.

## Técnica especial — Corte do Último Juramento

- Ativação: `Shift + clique esquerdo`, somente com a katana embainhada e mirando um Mob vivo em linha de visão.
- Dash em linha reta até uma posição atrás do alvo, com distância máxima de 6 blocos; o dash é interrompido por colisões para não atravessar paredes.
- Durante todo o especial a lâmina permanece embainhada e o personagem segura a saya com as duas mãos.
- Ao chegar atrás do alvo, o Mob fica imobilizado por 20 ticks (1 s).
- Em seguida recebe seis cortes, espaçados por 2 ticks. O dano total é 2× o atributo `ATTACK_DAMAGE`, dividido igualmente entre os seis hits; o cooldown de dano do alvo é zerado antes de cada hit.
- VFX utiliza partículas e sons já existentes no mod.

## Mão secundária

Com a katana na mão principal, o slot secundário deve ficar vazio. Itens existentes são movidos para o inventário; o excedente é dropado no chão e uma mensagem é enviada ao jogador. Novas tentativas de equipar algo na mão secundária são bloqueadas.

## Altura da katana

A pose-base usa `katana.position` Y = **17.7**, cerca de 1 px abaixo da versão anterior. JSONs, manifestos e projetos Blockbench de fonte/runtime estão sincronizados.

## Multiplayer e validação

A lógica e os pacotes de postura/ataque são server-authoritative. O mesmo build do mod deve estar instalado no servidor e nos clientes, pois o payload de solicitação inclui o identificador do alvo.

Os JSONs, os manifestos, as cópias fonte/runtime e os projetos Blockbench foram validados estruturalmente. A compilação Gradle e o teste em Minecraft 26.3 ainda precisam ser executados na raiz completa do repositório; esta entrega contém somente `src/`.
