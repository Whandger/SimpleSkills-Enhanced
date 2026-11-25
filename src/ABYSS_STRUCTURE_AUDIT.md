# Revisão de worldgen — estruturas fora do Abismo

## Escopo

O Abismo continua sendo uma camada vertical do Overworld (`Y = -256 .. -80`), não uma dimensão separada. O castelo (`simpleskills:abyssal_castle`) é a única estrutura de construção autorizada a ocupar essa faixa.

## O que foi corrigido

- **Correção do crash de inicialização:** removido o mixin que tentava alterar quatro constantes em `ChunkPyramid.<clinit>`. No Minecraft 26.3, as dependências do pipeline são construídas por `ChunkStep.Builder.addRequirement`, e o inicializador não contém os quatro alvos esperados. O novo `AbyssStructureReferenceChunkStepMixin` intercepta esse método diretamente.

- `AbyssStrongholdMixin`: rejeita um stronghold quando a caixa final de todas as salas/corredores cruza a faixa do Abismo. A checagem usa a caixa do grafo já montado, não apenas a altura do ponto inicial.
- `AbyssStructureStartMixin`: última barreira para qualquer `StructureStart` no Overworld. Se a caixa completa de uma estrutura intersecta `Y=-256..-80`, a estrutura inteira não é colocada. Isso cobre também estruturas registradas por outros mods, sem bloqueá-las fora da faixa vertical do Abismo.
- Exceção explícita: `AbyssalCastleStructure.TYPE` continua permitido.
- Os filtros de mineshafts, Ancient Cities, Ruined Portals e Monster Rooms foram preservados.
- Os filtros de sculk (patches, veins e spread), spawners, lava/aquíferos e spawn natural também foram preservados.
- A ampliação usa dois pontos de extensão compatíveis com a estrutura de classes do Minecraft 26.3: `AbyssStructureReferenceChunkStepMixin` ajusta as dependências reais de `ChunkStep.Builder.addRequirement` (em vez de tentar alterar constantes dentro de `ChunkPyramid.<clinit>`, que não existem ali como esperado), e `AbyssStructureReferenceChunkGeneratorMixin` amplia os cinco limites do laço `createReferences`. Ambos usam a configuração `abyss_city_extended_structure_references` do `MixinPlugin`.

## Auditoria resumida

| Grupo | Regra dentro de Y=-256..-80 |
|---|---|
| Castelo Abissal | Permitido |
| Stronghold | Cancelado antes da colocação quando o grafo intersecta o Abismo |
| Mineshaft | Filtro específico existente + barreira geral |
| Ancient City / outros jigsaws | Filtro específico existente + barreira geral |
| Ruined Portal | Filtro específico existente + barreira geral |
| Vilas, monumentos, templos, mansões, naufrágios, tesouros, trail ruins e outras estruturas vanilla | Barreira geral por caixa da estrutura |
| Estruturas de outros mods | Barreira geral por caixa da estrutura |
| Monster Room / dungeon com spawner | Feature vanilla bloqueada pelo mixin existente |
| Sculk, lava, spawners e spawn natural vanilla | Filtros específicos existentes preservados |
| Minérios e formações do próprio Abismo | Mantidos |

A barreira geral verifica a caixa completa. Portanto, uma estrutura que começa acima de Y=-80, mas desce para dentro do Abismo, também é descartada por inteiro; isso evita construções cortadas na fronteira.

## Custo e configuração

O alcance de referências da cidade usa 18 chunks, em vez dos 8 padrões do Minecraft. O pipeline de dependências de chunks também precisa acompanhar esse alcance. Isso aumenta o trabalho e o uso de memória do worldgen globalmente, não apenas perto do castelo. É configurável em `config/simpleskills/config.json`:

```json
{
  "abyss_city_extended_structure_references": true
}
```

Deixar `true` é necessário para que os distritos externos e o anel geológico do maciço continuem sendo gerados. Com `false`, o Minecraft volta ao alcance padrão e partes da cidade que excedem esse alcance podem ficar sem referência.

## Limitações e teste necessário

- As verificações são aplicadas à geração de chunks novos. Estruturas já gravadas em chunks existentes não são apagadas retroativamente.
- O ZIP de fontes recebido não contém `build.gradle`, `gradle.properties` nem Gradle Wrapper; por isso não foi possível executar um build Fabric completo desta revisão.
- O log do primeiro teste confirmou que `AbyssStructureReferenceChunkPyramidMixin` falhava em `ChunkPyramid.<clinit>` (`0/4` encontrados). Esse mixin foi removido e substituído pelo hook no método real `ChunkStep.Builder.addRequirement`, evitando depender da forma como a JVM distribui lambdas no inicializador estático.
- O novo hook foi conferido contra a API decompilada do Minecraft 26.3. O build completo Fabric ainda precisa ser executado no projeto original com JDK 25 e Loom configurado; o ZIP de fontes não inclui `build.gradle`, `gradle.properties` nem Gradle Wrapper.
- Recomenda-se testar um mundo novo e inspecionar chunks recém-gerados em torno do castelo. Alterações de worldgen não reescrevem chunks existentes.


## Fundação conectada ao piso do Abismo

A versão atual usa um maciço que aumenta de raio conforme desce: aproximadamente 1,12x o raio-base na superfície e até 1,40x próximo do piso. A superfície externa usa uma curva smoothstep de até 43 blocos; o centro do castelo é fixado em Y=-204 para reservar altura suficiente. A base pode chegar a Y=-255, imediatamente acima do fundo do mundo em Y=-256, e a geração para ao encontrar quatro blocos sólidos consecutivos do terreno natural. A caixa horizontal foi ampliada para +/-280 blocos, coberta pelo raio de referência de 18 chunks.
