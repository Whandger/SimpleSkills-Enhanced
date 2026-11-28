# Katana — postura, combate e técnica especial

## Controles

- **Clique esquerdo:**
  - Com a katana embainhada e mirando um Mob vivo dentro do alcance, ativa o Corte Abissal sem precisar de Shift.
  - Com a katana embainhada e sem mirar um Mob válido (por exemplo, mirando o vazio), começa o saque; esse clique não causa dano.
  - Durante o saque, cliques adicionais são ignorados.
  - Com a katana totalmente desembainhada e na guarda de batalha, inicia/encadeia os três golpes.
  - Em guarda relaxada, o primeiro clique só retorna à guarda de batalha; o próximo ataca.
- **Corte Abissal:** clique esquerdo simples enquanto a katana está embainhada e um Mob válido está sob a mira.

## Sequência normal

1. `SHEATHED` → `DRAWING` (1,25 s) → `UNSHEATHED`.
2. Combo: `ATTACK_1` → `ATTACK_2` → `ATTACK_3`.
3. Após 2 s sem atacar: `LOWER_GUARD` (transição de 0,35 s).
4. Após mais 5 s na guarda relaxada: `SHEATHING` (1,35 s) → `SHEATHED`.

A caminhada mistura o ciclo `walk` de forma aditiva sobre a guarda global. As pernas continuam com o ciclo vanilla; os braços permanecem presos à empunhadura.

## Técnica especial — Corte do Último Juramento

- Disponível apenas quando `SHEATHED`.
- Ativação: clique esquerdo simples, com a katana embainhada, mirando um Mob vivo em linha de visão e a no máximo 5,15 blocos.
- Dash em linha reta até uma posição atrás do alvo, sem ultrapassar 6 blocos; colisões do cenário podem interromper o dash para evitar atravessar paredes.
- O modelo permanece embainhado (`sheathed_model`) e as duas mãos seguram a saya durante toda a técnica.
- O alvo fica imobilizado por 20 ticks (1 s); depois recebe seis hits, espaçados por 2 ticks.
- Dano total calculado: 2× o atributo `ATTACK_DAMAGE`, dividido igualmente pelos seis hits. O cooldown de dano do alvo é zerado antes de cada hit para que todos sejam aplicados.
- VFX usa partículas e sons já existentes no mod.

## Mão secundária

Com a katana na mão principal, o slot secundário precisa ficar vazio. Itens que já estejam nele são movidos para o inventário; se o inventário não comportar o item inteiro, o restante é dropado no chão e uma mensagem é exibida.

## Multiplayer e build

A lógica e os pacotes de postura/dano são server-authoritative. Instale o mesmo build do mod no servidor e em todos os clientes conectados. Não há patch de Live Sync neste pacote.
