# Documentacao Tecnica — Katana do Ultimo Juramento (SimpleSkills Enhanced | Minecraft Fabric 26.3)

## 1. Visao Geral da Arquitetura e Preservação Visual
Esta revisao prepara a **Katana do Ultimo Juramento** para uso real por jogadores em primeira e terceira pessoa no **Minecraft Fabric 26.3**, preservando integralmente sua curvatura (*sori*), o acabamento preto-obsidiana e os ornamentos dourados originais.

---

## 2. Hierarquia de Ossos e Pontos de Encaixe (*Locators*)

```text
root [0.0, 0.0, 0.0]
 ├── saya [0.0, 0.0, 0.0]                       <-- Bainha independente (cintura esquerda)
 │    └── locator_hip_left [0.0, -5.5, 0.0]     <-- Ancora na cintura esquerda do jogador
 ├── katana [0.0, 0.0, 0.0]                     <-- Arma completa independente da bainha
 │    ├── tsuka [0.0, 0.35, 0.0]                <-- Empunhadura (cabo)
 │    │    ├── locator_right_hand [0.0, 3.2, 0.0]  <-- Encaixe da mao direita (proximo a tsuba)
 │    │    └── locator_left_hand [0.0, 7.8, 0.0]   <-- Encaixe da mao esquerda (proximo ao kashira)
 │    ├── tsuba [0.0, 0.175, 0.0]               <-- Guarda dourada octogonal
 │    └── blade [0.0, 0.0, 0.0]                 <-- Lamina curva (13 cubos)
 │         └── locator_blade_tip [0.0, -33.5, -1.1838] <-- Ponta kissaki (particulas e rastros de corte)
 ├── sheathed_model [0.0, 0.0, 0.0]             <-- Composicao visual embainhada (SEM malha interna da lamina)
 └── player_skeleton [0.0, 0.0, 0.0]            <-- Ossos de referencia do esqueleto humanoide padrao
      ├── body [0, 24, 0]
      ├── head [0, 24, 0]
      ├── right_arm [-5, 22, 0]
      └── left_arm [5, 22, 0]
```

### Tabela de *Locators* e Eixos de Rotacao
| Locator | Osso Pai | Coordenada Local (BB units) | Funcao no Jogo |
| :--- | :--- | :--- | :--- |
| `locator_hip_left` | `saya` | `[0.0, -5.5, 0.0]` | Ponto de acoplamento da bainha no quadril esquerdo do jogador (`X=+5.2, Y=12.2, Z=0.5`, rotacao `[68°, 10°, -6°]`, escala `0.76`). |
| `locator_right_hand` | `tsuka` | `[0.0, 3.2, 0.0]` | Encaixe primario da mao direita logo abaixo da guarda (`tsuba`). |
| `locator_left_hand` | `tsuka` | `[0.0, 7.8, 0.0]` | Encaixe secundario da mao esquerda perto do pomo (`kashira`) na postura *Chudan-no-kamae*. |
| `locator_blade_tip` | `blade` | `[0.0, -33.5, -1.1838]` | Emissor de particulas de corte (*slash trail*) e alinhamento de ponta na boca da bainha (`koiguchi`) durante `sheath_katana`. |

---

## 3. Separacao dos Tres Componentes Independentes

1. **`katana` (`models/item/katana_ultimo_juramento.json` + osso `katana`)**:
   - Contem exclusivamente `blade` + `tsuba` + `tsuka`.
   - Quando desembainhada, a lamina e deslocada `+34.4` unidades ao longo do eixo longitudinal da bainha antes de girar para a postura de combate, garantindo zero atravessamento.
2. **`saya` (`models/item/katana_ultimo_juramento_saya.json` + osso `saya`)**:
   - Bainha completa isolada (`koiguchi`, corpo curvo, `kurikata`, `sageo`, `seme_gane`, `kojiri`).
   - Permanece acoplada a cintura esquerda do jogador enquanto ele luta com a katana desembainhada.
3. **`sheathed_model` (`models/item/katana_ultimo_juramento_sheathed.json` + osso `sheathed_model`)**:
   - Composicao visual da arma embainhada (`saya` + `tsuka` + `tsuba`), **sem** os 13 cubos internos da lamina.
   - Elimina completamente qualquer *Z-fighting* ou atravessamento visual enquanto a arma esta embainhada.

---

## 4. Compatibilidade com Minecraft Fabric 26.3 (Sem Dependencia Externa)

O arquivo `animations/katana_ultimo_juramento.animation.json` utiliza o formato padrao JSON de keyframes com interpolacao **Catmull-Rom** (`"lerp_mode": "catmullrom"`).

### Classificacao de Recursos por Motor:
- **100% Nativos / Independentes de Motor (Funcionam direto no Fabric 26.3 puro via Mixin)**:
  - Modelos de item Java (`models/item/katana_ultimo_juramento.json`, `katana_ultimo_juramento_sheathed.json`, `katana_ultimo_juramento_saya.json`).
  - Texturas 64x64 (`katana_ultimo_juramento.png` e `katana_ultimo_juramento_emissive.png`).
  - Dados numericos de rotacao/translacao de `body`, `right_arm`, `left_arm`, `saya` e `katana` em `katana_ultimo_juramento.animation.json` (podem ser lidos diretamente por um avaliador Catmull-Rom em Java aplicando `MatrixStack` e `ModelPart.pitch/yaw/roll` no `PlayerEntityModel`).
- **Opcionais (Apenas se o mod utilizar GeckoLib / AzureLib / PlayerAnimator)**:
  - O formato `.geo.json` + `.animation.json` e diretamente compativel caso o projeto adote GeckoLib/AzureLib no futuro, mas **nao exige** nenhuma biblioteca externa para funcionar no Fabric 26.3.

### Maquina de Estados Recomendada (Saque ao Atacar -> 2s -> Embainhar)
```java
// Estado por jogador (Attached Data / Component no Fabric 26.3):
// SHEATHED -> (ao atacar) -> DRAWING (1.25s = 25 ticks) -> UNSHEATHED -> (40 ticks = 2.0s sem atacar) -> SHEATHING (1.35s = 27 ticks) -> SHEATHED
public enum KatanaStanceState {
    SHEATHED,   // Renderiza apenas `sheathed_model` na cintura esquerda (locator_hip_left)
    DRAWING,    // Executa `draw_katana` (1.25s): mao direita saca axialmente da `saya`
    UNSHEATHED, // Renderiza `katana` nas maos (Chudan-no-kamae) + `saya` vazia na cintura esquerda
    SHEATHING   // Executa `sheath_katana` (1.35s): chiburi + alinhamento da ponta + insercao axial
}
```


## Nota de integração do projeto
O arquivo `.animation.json` contém keyframes; Fabric/Minecraft não os reproduz automaticamente. É necessário um avaliador e renderer Java próprio se o projeto não utilizar uma biblioteca de animação.
