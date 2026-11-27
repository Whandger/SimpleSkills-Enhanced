# Correção — Corte Abissal e compilação

## Erro de compilação corrigido

Em `KatanaStanceManager.findSpecialTarget`, a condição comparava `Mob` com `ServerPlayer`. Essas classes são tipos incompatíveis para essa comparação. A verificação foi alterada para comparar o `Entity` resolvido com o `ServerPlayer` antes do pattern matching de `Mob`.

## Controle do Corte Abissal

O Corte Abissal agora usa apenas clique esquerdo quando o jogador está no estado `SHEATHED` (modo especial pronto) e mira um `Mob` vivo. Não é necessário segurar Shift.

Para preservar o saque normal: com a katana embainhada, clicar sem um Mob válido sob a mira continua iniciando `DRAWING`. Durante o dash/execução especial, os cliques permanecem bloqueados pelo estado especial.

A validação de alvo, alcance e linha de visão continua no servidor; o cliente apenas escolhe a ação pretendida.

## Arquivos alterados

- `main/java/com/github/ob_yekt/simpleskills/abyss/weapon/KatanaStanceManager.java`
- `client/java/com/github/ob_yekt/simpleskills/mixin/KatanaMinecraftAttackMixin.java`
- `KATANA_COMBATE_ESPECIAL.md`

Nenhum JSON, modelo, textura, dano, duração de animação ou VFX foi alterado nesta correção.
