# Lakai — alcance e postura embainhada

## Alcance do Corte Abissal

O alcance de validação do alvo foi aumentado de 5,15 para **9,15 blocos**. Como a posição final fica atrás do alvo (offset mínimo de 0,85 bloco), a distância máxima total de deslocamento foi aumentada de 6 para **10 blocos**. Os dois limites precisam permanecer juntos para que o alcance adicional seja utilizável.

## Idle do Lakai

`idle_lakai_sheathed.animation.json` foi reconstruído a partir do JSON `idle_sheathed` fornecido pelo usuário. Foram preservados os valores de `saya`, `katana`, `body`, `right_arm` e `left_arm`, mudando somente o identificador do clipe para `idle_lakai_sheathed`. A intenção é reutilizar a postura baixa da katana embainhada, sem a pose anterior de braços abertos.

O JSON-fonte e o JSON-runtime são idênticos, e os projetos Blockbench fonte/runtime foram atualizados.
