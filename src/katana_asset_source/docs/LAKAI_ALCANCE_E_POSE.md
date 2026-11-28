# Lakai — alcance e postura embainhada

## Alcance do Corte Abissal

O alcance de seleção/validação do alvo agora é de **13,15 blocos**, mais 4 blocos em relação ao limite anterior de 9,15. Como a posição final fica atrás do alvo, a distância máxima total do dash é **15,5 blocos**, incluindo o espaço necessário para terminar atrás do Mob. Cliente e servidor usam o mesmo raio de mira e o servidor confirma novamente o alvo e a linha de visão.

## Idle do Lakai

`idle_lakai_sheathed.animation.json` foi reconstruído a partir do JSON `idle_sheathed` fornecido pelo usuário. Foram preservados os valores de `saya`, `katana`, `body`, `right_arm` e `left_arm`, mudando somente o identificador do clipe para `idle_lakai_sheathed`. A intenção é reutilizar a postura baixa da katana embainhada, sem a pose anterior de braços abertos.

O JSON-fonte e o JSON-runtime são idênticos, e os projetos Blockbench fonte/runtime foram atualizados.

## Postura durante o Corte do Abismo

Os clipes `special_sheathed` e `special_sheathed_recovery` agora usam a mesma postura baixa de `idle_lakai_sheathed`. Os braços não levantam durante o dash, o travamento, os seis cortes ou a recuperação. O clipe de recuperação termina nos mesmos valores de `idle_lakai_sheathed`, e o cliente faz um cross-fade curto de volta para o idle.

O Corte do Abismo é ativado com clique esquerdo quando o modo Lakai está ativo, a katana está embainhada e há um Mob válido na mira; não exige Shift.
