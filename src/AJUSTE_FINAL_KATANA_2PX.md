# Ajuste final de repouso da katana

Foi aplicada apenas uma correção vertical no canal `katana.position`: Y=19.2 passou a Y=18.2 (1 unidade para baixo, aproximadamente 2 px na escala de prévia usada).

O ajuste foi propagado pelos nove clipes globais, com offset total enquanto a katana está desembainhada e interpolação gradual nos clipes de saque/embainhar. Isso mantém as continuidades numéricas entre estados.

Não foram alterados Java, braços, corpo, saya, rotações, escalas, texturas, geometria ou locators.
