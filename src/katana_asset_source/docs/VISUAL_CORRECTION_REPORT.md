> **Relatório histórico:** este relatório descreve a etapa de correção visual anterior à implantação dos nove clipes globais. Os dados atuais de animação são os JSONs individuais descritos em `katana_asset_source/animations/manifest.json`; referências antigas a `firstperson.*` não representam o runtime atual.

# Katana do Último Juramento — relatório de correção visual 07

> **Registro histórico:** este relatório descreve um snapshot anterior com 14 animações separadas por perspectiva. A fonte atual foi consolidada em sete clipes globais compartilhados; consulte `KATANA_ANIMACAO_GLOBAL.md` para o estado atual.


## Fonte e escopo

Atualização baseada no texto e no JSON de animação corrigidos enviados em 27/11/2026, e no snapshot do `src` mais recente fornecido pelo usuário (`src(20261127-104500).zip`).

Este incremento altera somente assets de modelo/animação e documentação. Nenhum arquivo Java, mixin, payload, item definition, geometria `.geo.json`, textura ou locator foi alterado.

## Alterações realizadas

1. O JSON corrigido com 14 animações foi instalado em:
   - `katana_asset_source/animations/katana_ultimo_juramento.animation.json`
   - `main/resources/assets/simpleskills/katana/animations/katana_ultimo_juramento.animation.json`

2. O projeto editável de Blockbench (`katana_ultimo_juramento.bbmodel`) teve seus animators/keyframes reconstruídos a partir do JSON corrigido, preservando:
   - elementos e UUIDs de geometria;
   - grupos e UUIDs de ossos;
   - locators e suas posições;
   - UUIDs das 14 animações existentes;
   - escala, pivôs e elementos da katana.

3. A cópia de `.bbmodel` em `main/resources/assets/simpleskills/katana/models/` foi sincronizada com a fonte editável. Antes, essa cópia continha somente 8 clipes; agora também contém os 14 clipes.

4. Os seis ataques continuam com nomes reconhecidos pelo sistema atual:
   - `attack_1_horizontal`
   - `attack_2_rising_diagonal`
   - `attack_3_overhead_finisher`
   - `firstperson.attack_1_horizontal`
   - `firstperson.attack_2_rising_diagonal`
   - `firstperson.attack_3_overhead_finisher`

## Resultado da validação estrutural

- 14 clipes no JSON de runtime.
- 14 clipes no `.bbmodel` da pasta-fonte.
- 14 clipes no `.bbmodel` da pasta de recursos.
- Keyframes numéricos com vetores de três valores.
- Interpolação Catmull-Rom em todas as tracks animadas.
- Tracks de terceira pessoa limitadas a `body`, `right_arm`, `left_arm`, `saya` e `katana`.
- Tracks de primeira pessoa limitadas a `right_arm`, `saya` e `katana`.
- As cinco transições relevantes foram comparadas por posição, rotação e escala e apresentam diferença zero entre último e primeiro keyframe:
  - saque → idle de combate;
  - embainhar → idle embainhada;
  - ataque 1 → ataque 2;
  - ataque 2 → ataque 3;
  - ataque 3 → idle de combate.
- Os mesmos resultados foram verificados nas variantes de primeira pessoa.

## Timings dos ataques

| Ataque | Duração | Impacto visual |
|---|---:|---:|
| Horizontal | 0,46 s | 0,23 s |
| Diagonal ascendente | 0,50 s | 0,27 s |
| Finalizador descendente | 0,66 s | 0,40 s |

Consulte `ATTACK_ANIMATION_TIMINGS.md` para a descrição e o timing equivalente em ticks.

## Pré-visualizações incluídas

As imagens em `renders/visual_correction/` foram geradas tecnicamente a partir dos modelos JSON de item, textura, transforms de pivô/escala descritos no renderer Java e keyframes do JSON corrigido.

- Terceira pessoa: utiliza um boneco humanoide simplificado em blocos como referência de proporção e as meshes dos modelos de item.
- Primeira pessoa: utiliza projeção de câmera das meshes e keyframes dos modelos de item, com retículo central de referência.

**Limitação importante:** estas imagens são pré-visualizações técnicas feitas fora do Blockbench/Minecraft. Não são capturas do jogo nem renders nativos do Blockbench. O boneco de terceira pessoa é um proxy, e a prévia de primeira pessoa não reproduz o método interno completo que desenha a mão vanilla. Elas ajudam a inspecionar trajetória, escala relativa e continuidade dos modelos, mas não substituem teste visual no jogo.

## O que continua pendente

- Abrir o `.bbmodel` no Blockbench para confirmar a apresentação dos 14 clipes no Animation Editor.
- Compilar com a raiz Gradle completa e mappings exatos do workspace.
- Testar em Minecraft 26.3, em Steve e Alex, primeira e terceira pessoa.
- Ajustar qualquer diferença remanescente causada pelo `PoseStack`, modelo real do jogador, armadura ou renderização de mão.
- Integrar o Java para selecionar os três ataques, determinar as janelas de acerto, aplicar dano e cooldown pós-combo.

Nenhuma afirmação de correção visual em runtime é feita neste relatório: esta entrega sincroniza os dados e fornece prévias técnicas; a confirmação final depende do teste no Minecraft.
