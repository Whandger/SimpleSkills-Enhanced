# Combo de 3 ataques — integração dos assets (incremento 06)

> **Registro histórico:** este documento descreve a etapa anterior à consolidação do rig global. Na fonte atual, os clipes `firstperson.*` foram removidos do runtime e dos dois projetos `.bbmodel`; os onze clipes globais (incluindo caminhada, guarda relaxada e técnica especial) são a única fonte de animação. Consulte `KATANA_ANIMACAO_GLOBAL.md`.


Esta atualização incorpora os seis clipes fornecidos no material Gemini de 27/11/2026.

## Arquivos atualizados

- `main/resources/assets/simpleskills/katana/animations/katana_ultimo_juramento.animation.json`: 8 clipes originais + 6 clipes novos.
- `katana_asset_source/animations/katana_ultimo_juramento.animation.json`: espelho completo para edição.
- `katana_asset_source/models/katana_ultimo_juramento.bbmodel`: 14 animações no Animation Editor, incluindo os seis novos clipes.
- `katana_asset_source/docs/ATTACK_ANIMATION_TIMINGS.md`: timings, impactos e transições conforme o material recebido.

## Nomes adicionados

- `attack_1_horizontal`
- `attack_2_rising_diagonal`
- `attack_3_overhead_finisher`
- `firstperson.attack_1_horizontal`
- `firstperson.attack_2_rising_diagonal`
- `firstperson.attack_3_overhead_finisher`

## Conversão para Blockbench

Os seis clipes do JSON Bedrock foram convertidos para a estrutura de animação do `.bbmodel`, usando os UUIDs de ossos fornecidos no material. Os UUIDs das novas animações e keyframes foram gerados deterministicamente para evitar colisões com os oito clipes existentes. A geometria, os pivôs, os locators e as texturas não foram modificados. Foi preservada a pose `firstperson.idle_unsheathed` já ajustada no runtime; por isso, somente o primeiro keyframe de `firstperson.attack_1_horizontal` e o último de `firstperson.attack_3_overhead_finisher` para `saya` e `katana` foram alinhados à pose de runtime. As trajetórias internas e as transições entre os três golpes foram mantidas.

## Escopo

Nenhum arquivo Java foi alterado nesta etapa. A sequência de input 1-2-3, janelas de combo, dano, hit detection, cooldown após o terceiro golpe e sincronização multiplayer continuam pendentes de integração Java.

## Validação estrutural

Os dois arquivos JSON foram parseados; os 14 clipes foram confirmados em ambos. Os seis novos clipes usam apenas os ossos permitidos e interpolação Catmull-Rom. As poses finais/iniciais foram comparadas para confirmar continuidade entre ataque 1 → ataque 2 → ataque 3 → idle desembainhada em primeira e terceira pessoa. Isso é validação de dados, não substitui abrir o `.bbmodel` no Blockbench nem testar as animações dentro do Minecraft.
