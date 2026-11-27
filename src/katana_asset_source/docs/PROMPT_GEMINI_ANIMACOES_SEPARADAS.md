# Prompt para Gemini — animações da katana separadas por estado

O projeto agora possui um arquivo JSON por clipe dentro de `katana_asset_source/animations/`, organizado por estado. O Java carrega os JSONs por `manifest.json`; ao alterar ou criar um clipe, atualize também o arquivo espelhado em `main/resources/assets/simpleskills/katana/animations/` e o manifest correspondente.

## Regras obrigatórias

1. A pose de combate é o clipe `idle_unsheathed`, em `02_guarda_combate/idle_unsheathed.animation.json`. Ela é a pose-base exata.
2. Os três ataques devem começar e terminar na pose-base, com golpes rápidos, legíveis e fluidos. Preserve seus nomes/identificadores: `attack_1_horizontal`, `attack_2_rising_diagonal`, `attack_3_overhead_finisher`.
3. Implementar `walk` como ciclo global, iniciando e fechando na pose-base, com mistura suave para a pose-base ao parar. Use `05_movimento/walk.template.json` como ponto de partida.
4. Implementar `lower_guard` como transição suave após 2 segundos sem atacar. A espada continua desembainhada e na mão; não é a animação de embainhar. Use `06_guarda_relaxada/lower_guard.template.json`.
5. Durante a guarda relaxada, o jogador pode continuar andando com a katana na mão.
6. Após mais 5 segundos sem atacar na guarda relaxada, iniciar `sheath_katana` e retornar a `idle_sheathed`.
7. Ao atacar novamente enquanto estiver em guarda relaxada ou caminhando, voltar rapidamente à pose-base e executar a sequência de três ataques.
8. Primeira pessoa projeta o mesmo rig e os mesmos keyframes globais. Não criar `firstperson.*`.
9. Não alterar modelos, texturas, locators, nomes de bones ou sistemas não relacionados sem justificar. Não adicionar dependências externas sem provar necessidade/compatibilidade.
10. Implementar os estados/gatilhos Java necessários, não apenas JSONs. Verificar que o ataque realmente inicia quando o jogador clica e que o ciclo de caminhada é aplicado enquanto a entidade anda.

## Entrega

- Devolver a pasta `src/` completa.
- Um JSON por animação, agrupado por estado.
- Manifests de fonte e runtime atualizados.
- Java atualizado para selecionar os novos estados sem misturar clipes.
- Documentar os tempos em ticks/segundos e as transições.
- Validar JSON, referências de manifest, sincronização source/runtime e compilação sempre que a raiz Gradle estiver disponível.
- Não afirmar que testou dentro do Minecraft se isso não ocorreu.
