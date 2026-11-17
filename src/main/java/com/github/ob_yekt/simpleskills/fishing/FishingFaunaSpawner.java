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

/**
 * Adds custom fish and marine mammals to loaded aquatic areas around players.
 * The vanilla four fish retain their vanilla biome spawn rules; this spawner only adds mod species.
 */
public final class FishingFaunaSpawner {
    private static final int SURFACE_SCAN_ABOVE = 10;
    private static final int SURFACE_SCAN_BELOW = 32;
    private static final int FISH_SEARCH_ATTEMPTS = 10;
    private static final int MEGAFAUNA_SEARCH_ATTEMPTS = 8;
    private static final double PLAYER_MIN_DISTANCE_SQR = 14.0 * 14.0;
    private static final double MEGAFAUNA_CHANCE = 0.08;
    private static boolean registered;

    private FishingFaunaSpawner() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FishingConfig.isNaturalSpawningEnabled()) return;
            int interval = FishingConfig.naturalSpawnIntervalTicks();
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getGameTime() % interval != 0L) continue;
                for (ServerPlayer player : level.players()) {
                    if (!player.isAlive() || player.isSpectator()) continue;
                    spawnFishNearPlayer(level, player);
                }
            }
        });
    }

    private static void spawnFishNearPlayer(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < FISH_SEARCH_ATTEMPTS; attempt++) {
            BlockPos surface = findSurfaceWater(level, player, random);
            if (surface == null) continue;
            int depth = waterDepth(level, surface, 40);
            if (depth < 2) continue;

            FishHabitat habitat = FishHabitat.fromBiome(level.getBiome(surface));
            FishSpecies species = FishSpecies.pickWeighted(habitat, depth, random::nextDouble,
                    candidate -> candidate.isAmbientSpawnable()
                            && countSpecies(level, surface, candidate) < FishingConfig.maxPopulationFor(candidate));
            if (species == null) continue;
            if (species.spawnProfile().large() && depth < species.spawnProfile().preferredDepth()) continue;

            int desiredDepth = species.spawnProfile().large()
                    ? species.spawnProfile().preferredDepth()
                    : Math.max(1, species.spawnProfile().preferredDepth() / 2);
            int offset = Math.min(depth - 1, desiredDepth + random.nextInt(3));
            BlockPos spawnPos = surface.below(offset);
            if (!waterAt(level, spawnPos) || !waterAt(level, spawnPos.above())) continue;
            if (player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(spawnPos)) < PLAYER_MIN_DISTANCE_SQR) continue;

            int localPopulation = countSpecies(level, surface, species);
            int populationRoom = Math.max(0, FishingConfig.maxPopulationFor(species) - localPopulation);
            if (populationRoom <= 0) continue;

            var profile = species.spawnProfile();
            int requestedSchoolSize = profile.minSchoolSize()
                    + random.nextInt(profile.maxSchoolSize() - profile.minSchoolSize() + 1);
            requestedSchoolSize = Math.min(requestedSchoolSize, populationRoom);

            int spawned = spawnSchool(level, player, species, spawnPos, requestedSchoolSize, random);
            if (spawned > 0) {
                FishingDebug.info(player, "ambient fish school spawned: " + species.configKey() + " x" + spawned
                        + " | habitat=" + habitat + " | depth=" + depth);
                break;
            }
        }

        // Megafauna is intentionally rare and independent from the catchable-fish population.
        if (FishingConfig.isMegafaunaEnabled() && random.nextDouble() < MEGAFAUNA_CHANCE) {
            spawnMegafaunaNearPlayer(level, player, random);
        }
    }

    /**
     * Spawns a leader and links the remaining fish to it through AbstractSchoolingFish's vanilla
     * follow behaviour. Followers are placed nearby in valid water and the species population cap
     * is applied before this method is called.
     */
    private static int spawnSchool(ServerLevel level, ServerPlayer player, FishSpecies species,
                                   BlockPos center, int requestedSize, RandomSource random) {
        if (requestedSize <= 0) return 0;

        Mob first = species.entityType().spawn(level, center, EntitySpawnReason.NATURAL);
        if (!(first instanceof FishingFishEntity leader)) {
            return first == null ? 0 : 1;
        }

        int spawned = 1;
        int radius = Math.max(2, Mth.ceil(leader.getBbWidth() * 1.6F));
        double minSpacing = Math.max(1.8, leader.getBbWidth() * 1.15);
        double minSpacingSqr = minSpacing * minSpacing;
        int attempts = requestedSize * 14;

        while (spawned < requestedSize && attempts-- > 0
                && spawned < leader.getMaxSchoolSize()) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(3) - 1;
            if (dx == 0 && dy == 0 && dz == 0) continue;

            BlockPos followerPos = center.offset(dx, dy, dz);
            if (!waterAt(level, followerPos) || !waterAt(level, followerPos.above())) continue;
            if (player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(followerPos))
                    < PLAYER_MIN_DISTANCE_SQR) continue;
            if (leader.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(followerPos))
                    < minSpacingSqr) continue;
            if (leader.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(followerPos))
                    > 64.0) continue;

            Mob created = species.entityType().spawn(level, followerPos, EntitySpawnReason.NATURAL);
            if (!(created instanceof FishingFishEntity follower)) continue;

            follower.startFollowing(leader);
            if (follower.isFollower()) {
                spawned++;
            } else {
                // Do not leave an unattached duplicate if the leader has reached its school limit.
                follower.discard();
            }
        }

        return spawned;
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

            int x = surface.getX();
            int z = surface.getZ();
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

    private static int countSpecies(ServerLevel level, BlockPos center, FishSpecies species) {
        AABB box = new AABB(center).inflate(48.0, 24.0, 48.0);
        return level.getEntitiesOfClass(Mob.class, box,
                mob -> mob.isAlive() && mob.getType() == species.entityType()).size();
    }

    private static int countMegafauna(ServerLevel level, BlockPos center) {
        AABB box = new AABB(center).inflate(112.0, 48.0, 112.0);
        return level.getEntitiesOfClass(MarineMammalEntity.class, box, Mob::isAlive).size();
    }
}
