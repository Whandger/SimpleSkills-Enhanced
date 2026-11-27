# Ajuste fino da altura de repouso da katana

A posição Y anterior da guarda de combate era 19.2 e ficou aproximadamente 2 px acima do desejado. A correção final usa **Y = 18.2**, deslocando a katana **1,0 unidade para baixo**.

O ajuste foi aplicado somente ao canal `katana.position` dos clipes globais em que a arma está desembainhada: `idle_unsheathed`, `walk`, `lower_guard` e os três ataques. Os clipes de saque e embainhar recebem uma compensação gradual para chegar à nova guarda de combate e retornar à posição embainhada original sem salto.

`idle_sheathed`, `saya`, rotações, escalas, braços, corpo, geometria, locators, texturas e código Java não foram alterados. Os nove JSONs de fonte/runtime e os dois projetos Blockbench foram sincronizados.

O deslocamento em pixels depende da projeção e do renderer; confirme o ajuste final dentro do Minecraft 26.3.
