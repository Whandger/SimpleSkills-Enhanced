package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Holder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Decides whether a spot is a real fishing spot and keeps its fish population healthy.
 *
 * <ul>
 *   <li>A spot needs enough connected water (flood fill that stops as soon as the minimum volume is
 *       reached), enough depth and enough fish.</li>
 *   <li>The fish that already live there are always used first. Only if the population is below the
 *       configured minimum, a few fish are added <em>discreetly</em>: far from the bait and the player,
 *       away from the surface and either outside the player's field of view or deep (&gt;= 10 blocks).</li>
 *   <li>Nothing is ever spawned to make an invalid spot (puddle, tiny hole) work.</li>
 * </ul>
 *
 * Everything here runs once per cast (and once per spawn attempt), never every tick.
 */
public final class FishingEcosystem {
    public enum Result { OK, TOO_SMALL, TOO_SHALLOW, NO_FISH }

    /** Flood fill limits: the volume check never walks further than this from the bait. */
    private static final int FILL_RADIUS_H = 8;
    private static final int FILL_RADIUS_V = 6;
    /** How far under the bait fish are searched for. */
    public static final double SEARCH_DEPTH = 14.0;
    private static final int DEEP_SPAWN_DEPTH = 10;
    private static final long CACHE_TICKS = 400L;
    private static final int CACHE_MAX = 512;

    private record CacheKey(ResourceKey<Level> dimension, int x, int y, int z) {}
    private record CacheValue(boolean enoughVolume, long expiresAt) {}

    private static final Map<CacheKey, CacheValue> VOLUME_CACHE = new HashMap<>();

    private FishingEcosystem() {}

    /** Evaluates the spot of the bait; may discreetly add fish. */
    public static Result evaluate(ServerLevel level, ServerPlayer player, BlockPos bait) {
        return evaluate(level, player, bait, msg -> { });
    }

    /**
     * Same as {@link #evaluate(ServerLevel, ServerPlayer, BlockPos)} but reports every step to
     * {@code trace} (used by the debug mode).
     *
     * <p>A spot is valid when it has enough water, enough depth and at least one fish after the
     * discreet top-up. {@code fishing_ecosystem_min_fish_count} is the population the top-up aims
     * for, it is not a hard requirement: a lake with two fish is still a lake with fish.</p>
     */
    public static Result evaluate(ServerLevel level, ServerPlayer player, BlockPos bait, Consumer<String> trace) {
        if (!isWater(level, bait)) {
            trace.accept("spot: bait block " + bait.toShortString() + " is not water");
            return Result.TOO_SMALL;
        }
        int depth = waterDepthBelow(level, bait, FishingConfig.minWaterDepth());
        if (depth < FishingConfig.minWaterDepth()) {
            trace.accept("spot: depth " + depth + " < min " + FishingConfig.minWaterDepth());
            return Result.TOO_SHALLOW;
        }
        if (!hasEnoughVolume(level, bait)) {
            trace.accept("spot: connected water volume < min " + FishingConfig.minWaterVolume());
            return Result.TOO_SMALL;
        }

        int minFish = FishingConfig.minFishCount();
        int existing = nearbyFish(level, bait).size();
        int count = existing;
        int spawned = 0;
        if (count < minFish && FishingConfig.isSpawnEnabled()) {
            int room = FishingConfig.maxPopulation() - count;
            int want = Math.min(minFish - count, room);
            spawned = replenish(level, player, bait, want);
            count += spawned;
        }
        trace.accept("spot ok: fish nearby=" + existing + ", added discreetly=" + spawned
                + " (target " + minFish + ", range " + FishingConfig.detectionRange() + ")");
        return count >= 1 ? Result.OK : Result.NO_FISH;
    }

    /** Every handled fish near the bait (alive, swimming, vanilla species). */
    public static List<Mob> nearbyFish(ServerLevel level, BlockPos bait) {
        double range = FishingConfig.detectionRange();
        // Wide on the horizontal plane, deep downwards: ocean fish live up to ~13 blocks under the surface.
        AABB box = new AABB(bait).inflate(range, 0.0, range)
                .expandTowards(0.0, -SEARCH_DEPTH, 0.0)
                .expandTowards(0.0, 2.0, 0.0);
        return level.getEntitiesOfClass(Mob.class, box,
                f -> f.isAlive() && f.isInWater() && FishSpecies.of(f) != null);
    }

    // ------------------------------------------------------------------------------------------
    // Water validation
    // ------------------------------------------------------------------------------------------

    private static boolean hasEnoughVolume(ServerLevel level, BlockPos bait) {
        int needed = FishingConfig.minWaterVolume();
        long now = level.getGameTime();
        CacheKey key = new CacheKey(level.dimension(), bait.getX() >> 1, bait.getY(), bait.getZ() >> 1);

        CacheValue cached = VOLUME_CACHE.get(key);
        if (cached != null && cached.expiresAt() > now) {
            return cached.enoughVolume();
        }

        boolean result = floodFill(level, bait, needed);
        if (VOLUME_CACHE.size() >= CACHE_MAX) {
            VOLUME_CACHE.values().removeIf(v -> v.expiresAt() <= now);
            if (VOLUME_CACHE.size() >= CACHE_MAX) {
                VOLUME_CACHE.clear();
            }
        }
        VOLUME_CACHE.put(key, new CacheValue(result, now + CACHE_TICKS));
        return result;
    }

    /** Bounded flood fill over water blocks; stops as soon as {@code needed} blocks were counted. */
    private static boolean floodFill(ServerLevel level, BlockPos start, int needed) {
        if (needed <= 1) return true;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        int count = 0;
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (++count >= needed) {
                return true;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (Math.abs(next.getX() - start.getX()) > FILL_RADIUS_H
                        || Math.abs(next.getZ() - start.getZ()) > FILL_RADIUS_H
                        || Math.abs(next.getY() - start.getY()) > FILL_RADIUS_V) {
                    continue;
                }
                if (!seen.add(next)) continue;
                if (isWater(level, next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }

    private static int waterDepthBelow(ServerLevel level, BlockPos from, int limit) {
        int depth = 0;
        BlockPos.MutableBlockPos pos = from.mutable();
        while (depth < limit && isWater(level, pos)) {
            depth++;
            pos.move(Direction.DOWN);
        }
        return depth;
    }

    private static int waterDepthAbove(ServerLevel level, BlockPos from, int limit) {
        int depth = 0;
        BlockPos.MutableBlockPos pos = from.mutable();
        while (depth < limit && isWater(level, pos.move(Direction.UP))) {
            depth++;
        }
        return depth;
    }

    private static boolean isWater(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER);
    }

    // ------------------------------------------------------------------------------------------
    // Discreet population top-up
    // ------------------------------------------------------------------------------------------

    private static int replenish(ServerLevel level, ServerPlayer player, BlockPos bait, int want) {
        if (want <= 0) return 0;
        RandomSource random = level.getRandom();
        double range = FishingConfig.detectionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 baitCenter = Vec3.atCenterOf(bait);

        int spawned = 0;
        int attempts = want * 12;
        while (spawned < want && attempts-- > 0) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 4.5 + random.nextDouble() * Math.max(1.0, range - 4.5);
            int x = Mth.floor(bait.getX() + 0.5 + Math.cos(angle) * distance);
            int z = Mth.floor(bait.getZ() + 0.5 + Math.sin(angle) * distance);
            int y = bait.getY() - (1 + random.nextInt(11));
            BlockPos pos = new BlockPos(x, y, z);

            if (!isWater(level, pos) || !isWater(level, pos.above())) continue;

            int surfaceDepth = waterDepthAbove(level, pos, 14) + 1; // blocks of water over the fish
            if (surfaceDepth < 2) continue; // never in the top layer

            Vec3 center = Vec3.atCenterOf(pos);
            if (center.distanceToSqr(player.position()) < 36.0) continue;       // >= 6 blocks from the player
            if (center.distanceToSqr(baitCenter) < 4.0 * 4.0) continue;         // >= 4 blocks from the bait

            // Must not pop up in front of the player unless it is deep enough to be unnoticeable.
            boolean inView = look.dot(center.subtract(eye).normalize()) > 0.30;
            if (inView && surfaceDepth < DEEP_SPAWN_DEPTH) continue;

            EntityType<? extends Mob> type = pickSpecies(level, pos, random);
            Mob fish = type.spawn(level, pos, EntitySpawnReason.EVENT);
            if (fish != null) {
                spawned++;
            }
        }
        return spawned;
    }

    /** Species that fits the biome: warm waters get tropical fish and pufferfish, the rest cod and salmon. */
    private static EntityType<? extends Mob> pickSpecies(ServerLevel level, BlockPos pos, RandomSource random) {
        Holder<Biome> biome = level.getBiome(pos);
        float temperature = biome.value().getBaseTemperature();
        double roll = random.nextDouble();
        if (biome.is(BiomeTags.IS_RIVER)) {
            return roll < 0.65 ? EntityTypes.SALMON : EntityTypes.COD;
        }
        if (temperature >= 0.95F) {
            return roll < 0.65 ? EntityTypes.TROPICAL_FISH : EntityTypes.PUFFERFISH;
        }
        if (temperature <= 0.3F) {
            return roll < 0.5 ? EntityTypes.SALMON : EntityTypes.COD;
        }
        return roll < 0.65 ? EntityTypes.COD : EntityTypes.SALMON;
    }

    /** Forget cached results (server stop / reload). */
    public static void clear() {
        VOLUME_CACHE.clear();
    }
}
