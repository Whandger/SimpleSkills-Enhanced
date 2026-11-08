Testes puros (sem Minecraft) do minigame de pesca: tamanho/peso dos peixes, XP, dificuldade da luta e direcao esquerda/direita.
Rodar (JDK 25), na raiz do projeto:
  javac -d /tmp/tout src/main/java/com/github/ob_yekt/simpleskills/fishing/FishSizeModel.java src/main/java/com/github/ob_yekt/simpleskills/fishing/FishingFightModel.java tools/fishing-tests/SizeTest.java
  java -cp /tmp/tout SizeTest
Saida esperada: ultima linha FAILS=0
