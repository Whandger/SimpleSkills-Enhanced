package com.github.ob_yekt.simpleskills.abyss.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * Luz real dos ataques do Crimson Colossus. Cada ataque entrega uma lista de pontos com um nível de luz,
 * e cada ponto vira um bloco de luz temporário em ar livre. Como é estado do mundo, terreno, paredes e
 * mobs ficam iluminados em todos os clientes. Blocos que saem da lista voltam ao original.
 * Segue o mesmo padrão do PrayerLighting, generalizado para vários pontos (ex.: o feixe do laser).
 */
public final class CrimsonColossusLighting {
    /** Um ponto de luz: posição no mundo e nível (0 a 15). */
    public record Glow(Vec3 pos, int level) {}

    /** Máximo de blocos de luz por chefe, para limitar o recálculo de luz. */
    private static final int MAX_CELLS = 12;

    private static final class Cell {
        final BlockState original;
        int level = -1;

        Cell(BlockState original) {
            this.original = original;
        }
    }

    private static final class Source {
        final ServerLevel level;
        final Map<BlockPos, Cell> cells = new HashMap<>();

        Source(ServerLevel level) {
            this.level = level;
        }
    }

    private static final Map<UUID, Source> SOURCES = new HashMap<>();

    private CrimsonColossusLighting() {}

    /**
     * Substitui a luz deste chefe pelos pontos dados. Pontos que caem no mesmo bloco usam o maior nível,
     * e pontos sem ar livre por perto são ignorados. Uma lista vazia apaga a luz do chefe.
     */
    public static void update(UUID owner, ServerLevel level, List<Glow> glows) {
        Source s = SOURCES.get(owner);
        if (s == null) {
            if (glows.isEmpty()) return;
            s = new Source(level);
            SOURCES.put(owner, s);
        }

        Map<BlockPos, Integer> wanted = new HashMap<>();
        for (Glow g : glows) {
            if (wanted.size() >= MAX_CELLS) break;
            int lv = Math.max(0, Math.min(LightBlock.MAX_LEVEL, g.level()));
            if (lv < 1) continue;
            BlockPos cell = freeCell(level, BlockPos.containing(g.pos().x, g.pos().y, g.pos().z), s);
            if (cell != null) wanted.merge(cell, lv, Math::max);
        }

        Iterator<Map.Entry<BlockPos, Cell>> it = s.cells.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Cell> e = it.next();
            if (!wanted.containsKey(e.getKey())) {
                restore(s.level, e.getKey(), e.getValue());
                it.remove();
            }
        }

        for (Map.Entry<BlockPos, Integer> e : wanted.entrySet()) {
            BlockPos pos = e.getKey();
            int lv = e.getValue();
            Cell c = s.cells.get(pos);
            if (c == null) {
                c = new Cell(level.getBlockState(pos));
                s.cells.put(pos, c);
            }
            if (lv != c.level) {
                level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lv),
                        Block.UPDATE_CLIENTS);
                c.level = lv;
            }
        }

        if (s.cells.isEmpty()) {
            SOURCES.remove(owner);
        }
    }

    /** Remove toda a luz de um chefe e devolve os blocos originais (morte, descarregamento, fim de ataque). */
    public static void release(UUID owner) {
        Source s = SOURCES.remove(owner);
        if (s == null) return;
        for (Map.Entry<BlockPos, Cell> e : s.cells.entrySet()) {
            restore(s.level, e.getKey(), e.getValue());
        }
        s.cells.clear();
    }

    /** Remove todas as luzes ativas (parada do servidor). */
    public static void clearAll() {
        for (UUID id : new ArrayList<>(SOURCES.keySet())) {
            release(id);
        }
    }

    private static void restore(ServerLevel level, BlockPos pos, Cell c) {
        if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, c.original, Block.UPDATE_CLIENTS);
        }
    }

    /** Escolhe uma célula de ar perto do ponto: a própria, se já for luz deste chefe, ou um vizinho livre. */
    private static BlockPos freeCell(ServerLevel level, BlockPos center, Source s) {
        if (accept(level, center, s)) return center;
        for (Direction d : Direction.values()) {
            BlockPos n = center.relative(d);
            if (accept(level, n, s)) return n;
        }
        return null;
    }

    private static boolean accept(ServerLevel level, BlockPos pos, Source s) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (s.cells.containsKey(pos) && state.is(Blocks.LIGHT)) return true;
        return state.isAir();
    }
}
