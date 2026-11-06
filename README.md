# SimpleSkills Enhanced

**SimpleSkills Enhanced** is a community-developed fork of the original [SimpleSkills](https://modrinth.com/mod/simpleskills) Fabric mod, introducing additional RPG mechanics, profession bonuses, combat improvements, and interface enhancements.

> **Important Notice — Original Mod Credits**
>
> SimpleSkills Enhanced is a **fork** of the official SimpleSkills mod. The entire foundation of the mod, including its skill system, leaderboards, Ironman mode, configuration, and original concept, belongs to its creator, **obyekt** (Discord: `obyekt`).
>
> All credit for the original work goes to its creator. This fork builds upon his work by introducing additional improvements.
>
> If you enjoy this project, please support the original author and check out the official mod:
>
> - [Modrinth — SimpleSkills](https://modrinth.com/mod/simpleskills)
> - [GitHub — obujekuto/simpleskills](https://github.com/obujekuto/simpleskills)
> - [Buy Me a Coffee — obujekuto](https://www.buymeacoffee.com/obujekuto)

---

## 🏆 Progression Philosophy

The perks in SimpleSkills exist to reward players for the time they invest in their professions.

Reaching level 99 requires significant dedication, so achieving the maximum level should feel worthwhile. Players who reach this milestone earn the full benefits of their profession, with bonuses intentionally tuned to become powerful at higher levels.

## ✨ What SimpleSkills Enhanced Adds

### ⛏️ Mining, 🪓 Woodcutting & 🥄 Excavating

**Gathering Speed**

- **+1% block-breaking speed per level**, reaching **+99% at level 99**.
- Bonuses respect the appropriate tool for each profession:
  - **Mining:** Pickaxe
  - **Woodcutting:** Axe
  - **Excavating:** Shovel
- Bonuses multiply vanilla block-breaking speed instead of replacing the original calculation.
- **Tool Durability Protection:** +1 percentage point per level, up to **99%** at level 99.

### ⛏️ Mining

- **Loot Duplication:** +5 percentage points of duplication chance every 10 levels, reaching **45% at level 90**.
- **Vein Mining:** Unlocked at level 15. Hold Shift to mine connected blocks of the same ore type. Every 15 levels adds 2 extra blocks, reaching 12 additional blocks at level 90.

### 🪓 Woodcutting

- **Loot Duplication:** +5 percentage points of duplication chance every 10 levels, reaching **45% at level 90**.
- **Tree Feller:** Unlocked at level 5. Hold Shift and break a tree trunk with an axe to make the connected tree fall, including its branches and leaves. Logs and leaves fall with animations, and leaves generate their normal drops when they land.

### ⚔️ Slaying

- **Sword Damage:** +0.3% per level, up to **+29.7% at level 99**.
- **Critical Hit Chance:** +0.5 percentage points per level, up to **49.5% at level 99**.
- **Custom Critical Hits:** Deal **1.5× damage**.
- **Vanilla Critical Compatibility:** Vanilla critical hits triggered while falling or jumping remain fully functional. A vanilla critical and a SimpleSkills critical can trigger simultaneously on the same hit.
- **Life Steal I (Level 50):** 30% chance to recover half a heart.
- **Life Steal II (Level 99):** 60% chance to recover half a heart.
- Bonuses apply to **swords** when performing charged attacks.

### 🏹 Ranged

- **Projectile Damage:** +0.5% per level, up to **+49.5% at level 99**.
- **Double Shot (Levels 1–50):** +2 percentage points of duplication chance per level, reaching 100% at level 50.
- **Triple Shot (Levels 51–99):** Once Double Shot is guaranteed, a second extra projectile can be created. Its chance progressively increases from 0% at level 50 to 100% at level 99.
- **Maximum Projectiles:** Up to 3 projectiles per shot at level 99 (the original projectile plus 2 duplicates).
- **Eligible Weapons:** Bows and Crossbows.
- **Arrow Duplication:** Works with both Bows and Crossbows.
- **Firework Rocket Duplication:** Works exclusively with Crossbows.
- **Multishot Compatibility:** Each projectile has its own independent chance to trigger duplication.
- **Free Duplication:** Extra projectiles consume no additional ammunition and grant no additional skill XP.
- **Projectile Trajectory:** Duplicated projectiles follow the original shot direction and spawn at randomized positions around the original projectile.

### 🛡️ Defense

- **Damage Reduction:** +0.3% per level.
- **Armor Durability Protection:** +1 percentage point per level.
- **Knockback Resistance:** +0.5 percentage points per level.

### 🖥️ HUD & Interface

**Skills HUD**
- Refreshed visual appearance.
- Removed the original yellow border.

**Reworked TAB Menu — SkillTabMenu**
- Currently displayed exclusively on the multiplayer player TAB screen.
- Shows the benefits of each profession.
- Displays progression information and per-level bonuses.
- Improved visual organization of skill information.

![SimpleSkills Enhanced — SkillTabMenu](https://media.forgecdn.net/attachments/description/1784164/description_ccb25eef-737e-44f9-96a2-e2b18fb78066.jpg)

---

## 📜 Original SimpleSkills Features

Everything described in this section belongs to the original **SimpleSkills** mod, created by **obyekt**.

SimpleSkills is a **server-side Fabric mod** that transforms Minecraft into an RPG experience with 15 skills, unlockable equipment, leaderboards, and progression up to level 99.

It works in multiplayer without requiring players to install the mod on their clients. For singleplayer, install it on the client.

**Default Controls:**
- `TAB` — Toggle the HUD.
- `J` — Move the HUD.
- Keys can be rebound in Minecraft's Controls menu.

### ⭐ Main Features

- **15 Skills:** From Mining to Prayer, each with its own progression and benefits.
- **Gear Unlocks:** Diamond, Netherite, and other equipment unlocked according to skill levels.
- **Ironman Mode:** Lose items and XP upon death, with reduced HP (-50%) and 0.2× XP gain. Includes a red skull icon and server death announcement. Can be enforced for all players.
- **Leaderboards:** Top 5 rankings for individual skills or total levels, including an Ironman-exclusive leaderboard. XP continues to be tracked after level 99.
- **Prestige System:** Reach maximum level in all skills to reset progression and earn a Prestige counter next to your name.
- **Natural Economy:** High-level players can provide services such as enchanting, cooking, and crafting.
- **Fully Customizable:** JSON configuration with live reloading through `/simpleskills reload`, without restarting the server.
- **Vanilla+ Experience:** Simple, intuitive, and unobtrusive gameplay.

### 🛠️ The 15 Skills

| Skill | Description |
|:--|:--|
| Mining | Break stone and ores to unlock better tools. |
| Woodcutting | Chop trees. |
| Excavating | Dig dirt, sand, and gravel. |
| Farming | Till, compost, shear, feed, breed animals, and harvest crops. |
| Fishing | Fish faster and unlock better loot. |
| Defense | Take armor hits to unlock better equipment. |
| Slaying | Improve melee combat progression. |
| Ranged | Use bows, crossbows, and tridents. |
| Enchanting | Unlock level-restricted enchantments at the anvil. |
| Alchemy | Brew potions with extended durations. |
| Smithing | Repair and upgrade equipment. |
| Cooking | Prepare meals that restore more hunger and saturation. |
| Crafting | Create more durable tools and armor, with a chance to recover materials. |
| Agility | Run, jump, and swim faster. |
| Prayer | Sacrifice rare items to obtain long-lasting buffs. |

### 🔓 Gear Progression

| Armor | Defense Level | Tool | Tool Level |
|:--|--:|:--|--:|
| Leather | 0 | Wood | 0 |
| Gold | 10 | Gold | 5 |
| Copper | 25 | Stone | 10 |
| Turtle Helmet | 30 | Copper | 25 |
| Chainmail | 35 | Iron | 50 |
| Iron | 50 | Diamond | 75 |
| Diamond | 75 | Netherite | 99 |
| Netherite | 99 | — | — |

**Additional Equipment Requirements:**

- **Copper Pickaxe:** Mines the same ores as the Iron Pickaxe for balance reasons.
- **Shields:** Defense level 5 by default.
- **Weapons:**
  - Mace: Level 50.
  - Crossbow: Level 0.
  - Bow: Level 30.
  - Trident: Level 99.
- **Elytra:** Prayer level 50 by default.
- Affected items display their requirements in their item lore (e.g., `Requires level 50 Defense`).
- All requirements can be customized through `tool_requirements.json` and `armor_requirements.json`.

### ⚒️ Original Skill Highlights

#### Mining, Excavating & Woodcutting

- XP is calculated based on block hardness and the required tool.
- Block XP overrides can be configured through `block_mappings.json`.
- Ores mined using Silk Touch grant no XP.

#### 🎣 Fishing

- Fishing speed increases by **+0.5% per level**, up to **+49.5%**.

**Fishing Loot Tables:**

| Tier | Fish | Junk | Treasure |
|:--|--:|--:|--:|
| Novice (1–24) | 70% | 25% | 5% |
| Journeyman (25–49) | 70% | 22% | 8% |
| Artisan (50–74) | 70% | 19% | 11% |
| Expert (75–98) | 70% | 16% | 14% |
| Grandmaster (99) | 70% | 10% | 20% |

> If another mod modifies fishing loot, set `custom_fishing_loot_enabled` to `false` in `config.json`.

#### ✨ Enchanting

Level-restricted enchantments available at the anvil:

| Enchantment | Required Level |
|:--|--:|
| Efficiency IV | 15 |
| Fortune III | 25 |
| Sharpness V | 50 |
| Power V | 50 |
| Efficiency V | 75 |
| Mending | 99 |

- XP per enchantment: `multiplier × level²`.
- XP requirements can be adjusted through `enchantment_requirements.json`.

#### 🍖 Cooking

Hunger and saturation multipliers based on skill tier:

| Tier | Multiplier |
|:--|--:|
| Novice | 0.875× |
| Journeyman | 1× |
| Artisan | 1.125× |
| Expert | 1.25× |
| Grandmaster | 1.5× |

- Customizable XP tables encourage players to prepare a variety of foods, including Rabbit Stew, Golden Apple, Pumpkin Pie, and more.

#### 🧪 Alchemy

Potion duration multipliers:

| Tier | Multiplier |
|:--|--:|
| Novice | 1× |
| Journeyman | 1.25× |
| Artisan | 1.5× |
| Expert | 1.75× |
| Grandmaster | 3× |

#### 🌾 Farming

- XP for breeding, feeding, and shearing animals.
- XP for harvesting mature crops.
- XP configuration through `farming_xp.json`.
- +1% chance of an extra harvest drop per level, up to 99%.
- Unlocks better hoes as players progress.

#### 🧰 Crafting

Durability and ingredient recovery bonuses:

| Tier | Durability Bonus | Ingredient Recovery |
|:--|--:|--:|
| Novice | 0% | 0% |
| Journeyman | 5% | 2.5% |
| Artisan | 10% | 5% |
| Expert | 15% | 7.5% |
| Grandmaster | 25% | 15% |

- Crafted items are signed with the player's name and rank.

#### 🔨 Smithing

- Repair bonuses per tier:

| Tier | Repair Bonus |
|:--|--:|
| Novice | 25% |
| Journeyman | 35% |
| Artisan | 45% |
| Expert | 55% |
| Grandmaster | 100% |

- Anvils no longer take damage when repairing items.
- Diamond-to-Netherite upgrade durability bonuses:

| Tier | Durability Bonus |
|:--|--:|
| Novice | 0% |
| Journeyman | 5% |
| Artisan | 7.5% |
| Expert | 10% |
| Grandmaster | 20% |

- Smithing bonuses stack with Crafting bonuses.

#### 🙏 Prayer

- Sacrifice rare items by right-clicking a lit candle.
- Five buff tiers, lasting from 1 to 8 hours.
- Examples:
  - Rabbit Foot → Luck.
  - Diamond → Speed II.
  - Totem of Undying → Invisibility.
  - Nether Star → Haste II.
- All sacrifices and buffs are fully customizable.

---

## 💻 Commands

### Players

| Command | Description |
|:--|:--|
| `/simpleskills togglehud` | Toggle the skills HUD. |
| `/simpleskills ironman enable` | Enable Ironman mode. |
| `/simpleskills reset` | Reset your skills. |
| `/simpleskills leaderboard <skill\|total>` | View the regular leaderboard. |
| `/simpleskills leaderboardironman <skill\|total>` | View the Ironman leaderboard. |
| `/simpleskills query <username> <skill\|total>` | Query a player's skill progression. |

### Administrators

| Command | Description |
|:--|:--|
| `/simpleskills reload` | Reload configuration files. |
| `/simpleskills reset <username>` | Reset a player's progression. |
| `/simpleskills addxp <username> <skill> <amount>` | Add XP to a player's skill. |
| `/simpleskills setlevel <username> <skill> <level>` | Set a player's skill level. |

---

## ⚙️ Configuration

Configuration files are located in:

```text
server_root/config/simpleskills
```

Customize XP progression, unlock requirements, loot tables, Prayer sacrifices, and Ironman difficulty.

Important Ironman configuration options:

```json
{
  "standard_xp_multiplier": "...",
  "ironman_xp_multiplier": "..."
}
```

Configuration can be reloaded without restarting the server using:

```text
/simpleskills reload
```

---

## 🚀 Installation

1. Install **Fabric Loader** and **Fabric API**.
2. Download the SimpleSkills Enhanced JAR.
3. Place the JAR inside your server's `mods` folder.
4. For singleplayer, place the JAR inside your client's `mods` folder.
5. Customize the configuration files if desired.
6. Start Minecraft and enjoy your RPG progression!

---

## ❤️ Credits & Acknowledgements

### Original SimpleSkills

Created and maintained by **obyekt**.

All credit for the original concept, foundation, skill system, configuration, and original features belongs to the original author.

Please support his work:

- [Modrinth — SimpleSkills](https://modrinth.com/mod/simpleskills)
- [GitHub — obujekuto/simpleskills](https://github.com/obujekuto/simpleskills)
- [Buy Me a Coffee — obujekuto](https://www.buymeacoffee.com/obujekuto)

### SimpleSkills Enhanced

**Fork developed by Whandger.**

Responsible for the additional improvements introduced in this fork, including:

- Mining, Woodcutting, and Excavating gathering bonuses.
- Slaying combat improvements.
- Ranged projectile bonuses and duplication mechanics.
- Defense bonuses.
- HUD and TAB interface improvements.
- Supporting technical implementations and compatibility work.

---

## SimpleSkills Enhanced Team

**Original Creator:** obyekt  
**Fork Developer:** Whandger

*Built upon the original SimpleSkills project with respect for its creator and the community.*

