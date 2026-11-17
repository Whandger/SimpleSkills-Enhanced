package com.github.ob_yekt.simpleskills.fishing;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/** Periodically adds rare marine mammals; catchable fish use vanilla biome natural spawning. */
public final class FishingFaunaSpawner {
    private static final int SURFACE_SCAN_ABOVE = 10;
    private static final int SURFACE_SCAN_BELOW = 32;
    private static final int MEGAFAUNA_SEARCH_ATTEMPTS = 8;
    private static final double MEGAFAUNA_CHANCE = 0.08;
    private static boolean registered;

    private FishingFaunaSpawner() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FishingConfig.isNaturalSpawningEnabled() || !FishingConfig.isMegafaunaEnabled()) return;
            int interval = FishingConfig.naturalSpawnIntervalTicks();
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getGameTime() % interval != 0L) continue;
                for (ServerPlayer player : level.players()) {
                    if (!player.isAlive() || player.isSpectator()) continue;
                    if (level.getRandom().nextDouble() < MEGAFAUNA_CHANCE) {
                        spawnMegafaunaNearPlayer(level, player, level.getRandom());
                    }
                }
            }
        });
    }

    private static void spawnMegafaunaNearPlayer(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (countMegafauna(level, player.blockPosition()) >= FishingConfig.maxMegafaunaPerArea()) return;
        for (int attempt = 0; attempt < MEGAFAUNA_SEARCH_ATTEMPTS; attempt++) {
            BlockPos surface = findSurfaceWater(level, player, random);
            if (surface == null) continue;
            FishHabitat habitat = FishHabitat.fromBiome(level.getBiome(surface));
            if (!habitat.isOcean()) continue;
            int depth = waterDepth(level, surface, 48);
            if (depth < 14) continue;

            BlockPos spawnPos = surface.below(Math.min(5, depth - 2));
            if (!hasOpenWaterVolume(level, spawnPos, 3, 2)) continue;
            if (player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(spawnPos)) < 32.0 * 32.0) continue;

            boolean orca = habitat != FishHabitat.WARM_OCEAN && random.nextFloat() < 0.25F;
            var type = orca ? FishingEntities.ORCA : FishingEntities.HUMPBACK_WHALE;
            Mob created = type.spawn(level, spawnPos, EntitySpawnReason.NATURAL);
            if (created != null) {
                FishingDebug.info(player, "ambient marine mammal spawned: " + (orca ? "orca" : "humpback_whale")
                        + " | habitat=" + habitat + " | depth=" + depth);
                return;
            }
        }
    }

    private static BlockPos findSurfaceWater(ServerLevel level, ServerPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        int distance = 18 + random.nextInt(25);
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        int top = Math.min(level.getMaxY() - 2, player.blockPosition().getY() + SURFACE_SCAN_ABOVE);
        int bottom = Math.max(level.getMinY() + 2, player.blockPosition().getY() - SURFACE_SCAN_BELOW);

        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (waterAt(level, pos) && !waterAt(level, pos.above()) && waterAt(level, pos.below())) {
                return pos;
            }
        }
        return null;
    }

    private static int waterDepth(ServerLevel level, BlockPos surface, int maxDepth) {
        int depth = 0;
        BlockPos.MutableBlockPos cursor = surface.mutable();
        while (depth < maxDepth && waterAt(level, cursor)) {
            depth++;
            cursor.move(0, -1, 0);
        }
        return depth;
    }

    private static boolean waterAt(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static boolean hasOpenWaterVolume(ServerLevel level, BlockPos center, int horizontal, int vertical) {
        for (int x = -horizontal; x <= horizontal; x += 2) {
            for (int z = -horizontal; z <= horizontal; z += 2) {
                for (int y = -vertical; y <= vertical; y += 2) {
                    if (!waterAt(level, center.offset(x, y, z))) return false;
                }
            }
        }
        return true;
    }

    private static int countMegafauna(ServerLevel level, BlockPos center) {
        AABB box = new AABB(center).inflate(112.0, 48.0, 112.0);
        return level.getEntitiesOfClass(MarineMammalEntity.class, box, Mob::isAlive).size();
    }
}
