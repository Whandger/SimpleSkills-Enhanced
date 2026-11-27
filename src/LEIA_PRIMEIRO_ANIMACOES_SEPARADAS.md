# SimpleSkills Enhanced — src atualizado (caminhada, guarda relaxada e combo)

## Substituição

1. Faça backup da sua pasta `src/` atual.
2. Extraia o ZIP `SimpleSkills_Katana_Caminhada_Combate_src.zip` sobre a raiz do projeto (mesclando/substituindo os arquivos em `src/`).
3. Mantenha os arquivos Gradle e demais classes não modificadas do projeto.
4. Compile pelo Gradle da raiz (`./gradlew build`) e teste no Minecraft 26.3.

## Nota de validação

- Todos os 9 JSONs de animação e ambos os manifestos (`katana_asset_source/animations` e `main/resources/assets/simpleskills/katana/animations`) foram validados sintaticamente, espelhados byte a byte e verificados quanto à continuidade numérica entre poses.
- A raiz Gradle não fazia parte do pacote enviado; portanto, a compilação Gradle e o teste em execução no jogo devem ser executados no seu workspace local.
