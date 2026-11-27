# Katana do Último Juramento — timings dos ataques e estados globais

A animação de terceira pessoa é a única fonte de keyframes. A câmera em primeira pessoa projeta os mesmos ossos e blends para o espaço da câmera; não existem clipes `firstperson.*`.

## Clipes e janelas de impacto

| Golpe / Clipe | Estado | Duração | Impacto visual | Janela de encadeamento |
|---|---|---:|---:|---:|
| `attack_1_horizontal` | `ATTACK_1` (`5`) | 0,46 s (`10 ticks`) | 0,23 s | A partir de `0,35 s` (`7 ticks`, com buffer antes disso) |
| `attack_2_rising_diagonal` | `ATTACK_2` (`6`) | 0,50 s (`10 ticks`) | 0,27 s | A partir de `0,40 s` (`8 ticks`, com buffer antes disso) |
| `attack_3_overhead_finisher` | `ATTACK_3` (`7`) | 0,66 s (`14 ticks`) | 0,40 s | Retorna à guarda de combate ao concluir (`0,66 s`) |
| `walk` | Overlay aditivo | 0,80 s (loop) | — | Mistura suave conforme velocidade horizontal |
| `lower_guard` | `LOWER_GUARD` (`4`) | 0,35 s (`7 ticks`) + 5,00 s hold | — | Ativa 2,00 s após o último ataque; embainha 5,00 s após concluir |
