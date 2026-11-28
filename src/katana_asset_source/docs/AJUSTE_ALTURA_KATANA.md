# Ajuste fino final da altura de repouso da katana

A guarda de combate estava aproximadamente 1 px acima da posição visual desejada. O ajuste final desloca **somente `katana.position` em -0.5 unidade no eixo Y**, de `18.2` para **17.7**.

A alteração foi refletida em todos os keyframes da trilha `katana.position` dos 11 clipes globais, preservando as trajetórias, os tempos, as rotações, as escalas e a continuidade entre clipes. Os dois JSONs-fonte/runtime e os dois projetos `.bbmodel` foram sincronizados.

Não foram alterados os ossos `body`, `right_arm`, `left_arm` ou `saya`, nem as texturas, geometria, locators ou código Java por causa desse ajuste visual.

O deslocamento em pixels depende da projeção da câmera e do renderer. A confirmação final deve ser feita dentro do Minecraft 26.3.
