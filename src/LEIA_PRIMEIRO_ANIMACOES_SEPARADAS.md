# SimpleSkills Enhanced — src atualizado (katana global, combate e especial)

## Substituição

1. Faça backup da pasta `src/` atual.
2. Extraia este pacote e substitua a pasta `src/` inteira.
3. Mantenha os arquivos Gradle e os demais arquivos da raiz do projeto.
4. Compile a partir da raiz completa (`.\gradlew.bat clean build`) e teste em Minecraft 26.3.

## Conteúdo

- Animações globais separadas em 11 JSONs individuais, listados em `manifest.json`.
- Guarda de batalha, caminhada, três ataques, guarda relaxada, saque/embainhar e especial embainhado.
- Java para combo, dano customizado, remoção do indicador/cooldown vanilla, bloqueio da mão secundária e dash especial.
- Modelos, projetos Blockbench, geometria, texturas, ícones, renders e documentação preservados.
- Sem servidor de Live Sync ou patch separado.

## Validação e limitações

Todos os JSONs do `src` foram lidos e validados; os 11 clipes e manifestos de fonte/runtime foram comparados e estão sincronizados. Os projetos Blockbench de fonte/runtime também estão iguais e contêm os 11 clipes globais. A pose-base da katana usa Y=17.7.

A raiz Gradle não foi enviada; portanto, não foi possível executar `clean build` nem validar a execução dentro do Minecraft nesta sessão. Rode o build no workspace completo antes de instalar no jogo.
