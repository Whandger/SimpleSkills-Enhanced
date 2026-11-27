# Katana do Último Juramento — animações globais e combate

A terceira pessoa é a fonte única dos keyframes. A primeira pessoa projeta o mesmo rig; não existem tracks `firstperson.*` independentes.

## Clipes registrados no manifest.json (11)

- `00_embainhada/idle_sheathed.animation.json` — postura com katana na bainha (3,20 s, loop).
- `01_saque/draw_katana.animation.json` — saque (1,25 s).
- `02_guarda_combate/idle_unsheathed.animation.json` — guarda de batalha oficial (2,80 s, loop; pose-base de todos os golpes).
- `03_ataques/attack_1_horizontal.animation.json` — ataque horizontal (0,46 s).
- `03_ataques/attack_2_rising_diagonal.animation.json` — ataque diagonal ascendente (0,50 s).
- `03_ataques/attack_3_overhead_finisher.animation.json` — finalizador descendente (0,66 s).
- `04_embainhar/sheath_katana.animation.json` — retorno à bainha (1,35 s).
- `05_movimento/walk.animation.json` — ciclo de caminhada contínuo (0,80 s, loop e aditivo sobre a guarda).
- `06_guarda_relaxada/lower_guard.animation.json` — relaxamento da postura sem embainhar (0,35 s, hold no último frame).
- `07_especial/special_sheathed.animation.json` — técnica especial com a katana embainhada, duas mãos na saya, dash/lock/cortes (1,85 s).
- `07_especial/special_sheathed_recovery.animation.json` — recuperação da técnica especial para a postura embainhada (0,30 s).

Cada JSON contém um único clipe e ambos os manifestos apontam para os mesmos 11 arquivos. Fonte e runtime são espelhados byte a byte.

## Combate normal

- Clique esquerdo com a katana embainhada: apenas inicia `DRAWING`. O clique não causa dano.
- Durante o saque, cliques são ignorados. O jogador precisa concluir a animação e ficar na guarda `UNSHEATHED`; só então um novo clique inicia um golpe.
- O primeiro clique em `LOWER_GUARD` apenas retorna à guarda de batalha. O clique seguinte pode atacar.
- Os três golpes são encadeados em combo com buffer de entrada.
- O dano normal é calculado pelo atributo `ATTACK_DAMAGE`, sem depender da força/cooldown vanilla. A chamada vanilla `Player.attack` é cancelada para a katana.
- Os indicadores vanilla de cooldown são ocultados enquanto a katana está na mão principal.

## Estados automáticos

- Após 2 s sem atacar na guarda de batalha: entra em `LOWER_GUARD` (0,35 s de transição), com a espada ainda desembainhada.
- Depois de mais 5 s completos nessa guarda relaxada: entra em `SHEATHING` (1,35 s), depois `SHEATHED`.
- A caminhada usa o clipe `walk` como delta aditivo sobre a guarda de combate/relaxada; quando a velocidade zera, o peso de caminhada desaparece suavemente.

## Técnica especial — Corte do Último Juramento

- Ativação: `Shift + clique esquerdo` enquanto a katana está embainhada e um Mob válido está sob a mira.
- A técnica só começa em `SHEATHED`; não é permitido desembainhar a katana durante o especial.
- O personagem mantém o modelo completo embainhado (`sheathed_model`) e as duas mãos seguram a bainha durante dash, travamento, cortes e recuperação.
- O dash segue em linha reta por até 6 blocos e busca terminar atrás do alvo; colisões do cenário interrompem o movimento para evitar atravessar paredes.
- O Mob fica imobilizado por 1 s. Em seguida, recebe seis cortes, espaçados por 2 ticks.
- O dano total do especial é 2× o atributo `ATTACK_DAMAGE`, dividido igualmente entre os seis hits. O cooldown de dano do alvo é zerado antes de cada corte para permitir todos os hits.
- VFX usa partículas e sons já disponíveis no mod.

## Restrição de mão secundária

A katana exige as duas mãos. Com ela equipada na mão principal, o slot secundário não aceita itens. Um item que já estava equipado é movido ao inventário; se não couber, o restante cai no chão e o jogador recebe uma mensagem no chat.

## Altura de repouso da katana

O Y-base atual de `katana.position` na guarda de combate é **17.7 unidades Blockbench**. É 0,5 unidade abaixo da versão anterior (aproximadamente 1 px no renderer usado para calibrar). O mesmo ajuste foi refletido nos JSONs de todos os clipes globais e nos dois projetos `.bbmodel`; a bainha, braços, ombros e corpo não foram alterados por esse ajuste.

## Fonte e runtime

- JSON-fonte: `katana_asset_source/animations/`.
- JSON carregado pelo jogo: `main/resources/assets/simpleskills/katana/animations/`.
- Projeto editável Blockbench: `katana_asset_source/models/katana_ultimo_juramento.bbmodel`.
- Projeto Blockbench espelhado no runtime: `main/resources/assets/simpleskills/katana/models/katana_ultimo_juramento.bbmodel`.

## Limites de validação

Os 11 JSONs, manifestos, espelhamento fonte/runtime, projetos Blockbench e continuidade estrutural foram verificados neste pacote. O build Gradle e o teste visual no Minecraft 26.3 ainda precisam ser executados na raiz completa do projeto; a entrada disponível contém apenas a árvore `src/`.
