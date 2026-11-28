# Katana do Último Juramento — rig global, estados e combate

A terceira pessoa é a fonte única dos keyframes. A primeira pessoa projeta o mesmo rig global; não existem tracks `firstperson.*` independentes. Os clipes estão separados em arquivos individuais e são carregados por `animations/manifest.json`.

## Onze clipes ativos

- `00_embainhada/idle_sheathed.animation.json` — repouso embainhado.
- `01_saque/draw_katana.animation.json` — saque.
- `02_guarda_combate/idle_unsheathed.animation.json` — guarda de batalha e pose-base.
- `03_ataques/attack_1_horizontal.animation.json` — golpe horizontal.
- `03_ataques/attack_2_rising_diagonal.animation.json` — golpe diagonal ascendente.
- `03_ataques/attack_3_overhead_finisher.animation.json` — finalizador descendente.
- `04_embainhar/sheath_katana.animation.json` — embainhar.
- `05_movimento/walk.animation.json` — caminhada aditiva e contínua.
- `06_guarda_relaxada/lower_guard.animation.json` — relaxamento mantendo a katana na mão.
- `07_especial/special_sheathed.animation.json` — dash e seis cortes mantendo a katana embainhada e segurada com as duas mãos.
- `07_especial/special_sheathed_recovery.animation.json` — retorno à posição embainhada.

## Regras de combate

- Primeiro clique esquerdo com a katana embainhada apenas inicia o saque (25 ticks / 1,25 s). Não causa dano.
- Cliques durante o saque são ignorados. É necessário terminar o saque e ficar na guarda de batalha; somente um novo clique causa o primeiro golpe.
- Os ataques normais encadeiam os três clipes com buffer de entrada e janelas curtas de combo.
- Em `LOWER_GUARD`, o primeiro clique apenas retorna à guarda de batalha; o seguinte inicia um ataque.
- O dano normal usa o atributo de ataque e não depende da força/cooldown vanilla; o dano vanilla e os indicadores vanilla são desativados para a katana.
- Após 2 s sem atacar na guarda de batalha, começa `lower_guard` (0,35 s). Depois de mais 5 s nessa postura, começa `sheath_katana`.
- A caminhada aplica o delta do ciclo `walk` sobre a guarda ativa e suaviza o peso conforme a velocidade horizontal.
- A mão secundária deve permanecer vazia enquanto a katana está na mão principal. O item é transferido ao inventário ou dropado com aviso quando não há espaço.

## Técnica especial — Corte do Último Juramento

Ativar com `clique esquerdo`, com o modo Lakai ativo, a katana embainhada e um Mob válido sob a mira. O especial mantém o modelo `sheathed_model` e as duas mãos segurando a saya durante toda a técnica; não desembainha a lâmina. O jogador avança em linha reta por até 13,15 blocos de alcance ao alvo (dash máximo de 15,5 blocos incluindo a posição atrás do alvo) para terminar atrás do alvo. O Mob fica atordoado por 1 s e depois recebe seis cortes, espaçados por 2 ticks, cuja soma equivale a 2× `ATTACK_DAMAGE`. O cooldown de dano é zerado antes de cada hit. VFX usa partículas e sons já disponíveis no mod.

## Altura de repouso

O valor atual de `katana.position` na pose-base é Y=**17.7**. Isso representa mais 0,5 unidade para baixo em relação à versão Y=18.2 (aproximadamente 1 px na referência de calibração). JSONs de fonte/runtime e os dois projetos Blockbench estão sincronizados.

## Validação

Os 11 clipes, manifestos, espelhamento fonte/runtime e os dois projetos Blockbench foram validados estruturalmente. O build Gradle e o teste visual em Minecraft 26.3 precisam ser executados na raiz completa do repositório; este pacote contém somente `src/`.
