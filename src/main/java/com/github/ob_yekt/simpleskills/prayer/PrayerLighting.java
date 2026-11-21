package com.github.ob_yekt.simpleskills.prayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Luz real das magias do Prayer. Enquanto um efeito está ativo, um bloco de luz temporário
 * (LightBlock) acompanha o centro dele no servidor. Como é estado do mundo, terreno, paredes
 * e mobs são iluminados de verdade, em todos os clientes. Ao fim do efeito, o bloco original volta.
 */
public final class PrayerLighting {
    /** Máximo de luzes ao mesmo tempo, para limitar o recálculo de luz. */
    private static final int MAX_ACTIVE = 8;
    /** Intervalo mínimo entre trocas de bloco de um mesmo efeito (ticks). */
    private static final int UPDATE_EVERY_TICKS = 2;
    /** Nível de luz em repouso, antes da magia e depois que ela se dissipa. */
    private static final int START_LEVEL = 2;

    /** Magias leves: brilho discreto. */
    private static final Set<String> WEAK = Set.of("spark", "ice_shard", "shadow_bolt", "holy_bolt",
            "life_drain", "corruption", "curse_of_weakness", "divine_healing", "sacred_healing",
            "purification", "shadowstep", "light_orb");
    /** Magias fortes: iluminam bastante no centro da explosão. */
    private static final Set<String> STRONG = Set.of("meteor", "thunderstorm", "inferno", "blizzard",
            "absolute_zero", "storm_judgment", "radiant_nova", "divine_smite", "celestial_wrath",
            "apocalypse", "soul_explosion");
    /** Magias que voam até o alvo: a luz acompanha o trajeto. */
    private static final Set<String> PROJECTILES = Set.of("spark", "fireball", "ice_shard",
            "holy_bolt", "shadow_bolt", "meteor");

    private static final class Source {
        final ServerLevel level;
        BlockPos cell;
        BlockState original;
        int lightLevel = -1;
        int lastUpdate;
        Source(ServerLevel level) { this.level = level; }
    }

    private static final Map<UUID, Source> SOURCES = new HashMap<>();

    private PrayerLighting() {}

    /** Mapeia o estilo de VFX para a magia (os estilos 0, 1 e 2 não passam por abilityForStyle). */
    public static PrayerAbility abilityOf(int style) {
        return switch (style) {
            case 0 -> PrayerAbility.LIGHTNING_BOLT;
            case 1 -> PrayerAbility.DIVINE_HEALING;
            case 2 -> PrayerAbility.METEOR;
            default -> PrayerVisualEffects.abilityForStyle(style);
        };
    }

    public static boolean isProjectile(PrayerAbility ability) {
        return PROJECTILES.contains(ability.id());
    }

    /** Nível de luz (0 a 15) neste instante: sobe suave até o impacto e depois se dissipa. */
    public static int levelFor(PrayerAbility ability, int life, int maxLife, int peak) {
        double top = tierTop(ability);
        double t;
        if (life < peak) {
            double p = Math.min(1.0, Math.max(0.0, life / (double) Math.max(1, peak)));
            t = p * p * (3.0 - 2.0 * p);
        } else {
            double decay = Math.max(6.0, maxLife * 0.3);
            t = Math.exp(-(life - peak) / decay);
        }
        double level = START_LEVEL + (top - START_LEVEL) * t;
        return (int) Math.round(Math.min(LightBlock.MAX_LEVEL, Math.max(0.0, level)));
    }

    private static double tierTop(PrayerAbility ability) {
        if (WEAK.contains(ability.id())) return 8.0;
        if (STRONG.contains(ability.id())) return 15.0;
        return 11.0;
    }

    /** Atualiza (ou cria) a luz de um efeito. Chamado a cada tick do servidor pela entidade do VFX. */
    public static void update(UUID id, ServerLevel level, PrayerAbility ability, int life, int maxLife,
                              int peak, Vec3 center) {
        Source s = SOURCES.get(id);
        if (s == null) {
            if (SOURCES.size() >= MAX_ACTIVE) return;
            s = new Source(level);
            SOURCES.put(id, s);
        }
        if (s.cell != null && life - s.lastUpdate < UPDATE_EVERY_TICKS) return;

        int wanted = levelFor(ability, life, maxLife, peak);
        if (wanted < 1) {
            restore(s);
            s.lastUpdate = life;
            return;
        }

        BlockPos cell = usableCell(level, BlockPos.containing(center.x, center.y, center.z), s.cell);
        if (cell == null) return;

        if (s.cell != null && !cell.equals(s.cell)) restore(s);
        if (s.cell == null) {
            s.original = level.getBlockState(cell);
            s.cell = cell;
            s.lightLevel = -1;
        }
        if (wanted != s.lightLevel) {
            level.setBlock(s.cell, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, wanted),
                    Block.UPDATE_CLIENTS);
            s.lightLevel = wanted;
        }
        s.lastUpdate = life;
    }

    /** Remove a luz de um efeito e devolve o bloco original. */
    public static void release(UUID id) {
        Source s = SOURCES.remove(id);
        if (s != null) restore(s);
    }

    /** Remove todas as luzes ativas (parada do servidor). */
    public static void clearAll() {
        for (UUID id : new ArrayList<>(SOURCES.keySet())) release(id);
    }

    private static void restore(Source s) {
        if (s.cell != null && s.level.isLoaded(s.cell) && s.level.getBlockState(s.cell).is(Blocks.LIGHT)) {
            s.level.setBlock(s.cell, s.original, Block.UPDATE_CLIENTS);
        }
        s.cell = null;
        s.original = null;
        s.lightLevel = -1;
    }

    /** Escolhe uma célula de ar perto do centro (ou a própria célula da luz, se ela ainda estiver lá). */
    private static BlockPos usableCell(ServerLevel level, BlockPos pos, BlockPos own) {
        if (accept(level, pos, own)) return pos;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (accept(level, n, own)) return n;
        }
        return null;
    }

    private static boolean accept(ServerLevel level, BlockPos p, BlockPos own) {
        if (!level.isLoaded(p)) return false;
        return p.equals(own) || level.getBlockState(p).isAir();
    }
}
