package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hearing, sight and the alert / flee / recover cycle of the fish.
 *
 * <p>The fish keep their vanilla AI: reacting only means asking the vanilla navigation to stop or to
 * swim somewhere else, so no fish is ever frozen or permanently scared. Only fish that were
 * spooked have an entry in {@link #MINDS}; a fish without entry is {@link Mood#NORMAL}. Entries
 * expire by themselves, which keeps the map tiny.</p>
 *
 * <p>Cost: every player is scanned once every {@value #SCAN_INTERVAL} ticks (staggered by entity id)
 * with a single bounded entity query.</p>
 */
public final class FishingPerception {
    public enum Mood { NORMAL, ALERT, FLEEING, RECOVERING }

    private static final int SCAN_INTERVAL = 10;
    private static final int MAX_FISH_PER_SCAN = 24;
    private static final int ALERT_TICKS = 40;
    private static final int RECOVER_TICKS = 100;

    private static final class Mind {
        Mood mood = Mood.NORMAL;
        long until;
        int alertScans;
    }

    private static final Map<UUID, Mind> MINDS = new HashMap<>();
    /** Horizontal speed (blocks per tick) of every player, measured at each scan. */
    private static final Map<UUID, Double> SPEED = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();

    private FishingPerception() {}

    // ------------------------------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------------------------------

    public static Mood moodOf(AbstractFish fish) {
        Mind mind = MINDS.get(fish.getUUID());
        return mind == null ? Mood.NORMAL : mind.mood;
    }

    /** A fish can only be picked for a fishing session while it is calm. */
    public static boolean isCalm(AbstractFish fish) {
        return moodOf(fish) == Mood.NORMAL;
    }

    /**
     * Highest threat (0..1) any player (except {@code ignoredPlayer}) in the fish's level poses to it right
     * now. Used by fish that are approaching a bait to decide whether to give up.
     */
    public static double maxThreat(ServerLevel level, AbstractFish fish, UUID ignoredPlayer) {
        double flee = FishingConfig.fleeRange();
        double best = 0.0;
        for (ServerPlayer player : level.players()) {
            // The angler is patient: his own presence never scares the fish he is luring.
            if (player.getUUID().equals(ignoredPlayer)) continue;
            if (player.isSpectator() || !player.isAlive()) continue;
            if (player.distanceToSqr(fish) > flee * flee) continue;
            best = Math.max(best, threat(fish, player, SPEED.getOrDefault(player.getUUID(), 0.0)));
        }
        return best;
    }

    /** 0 = no threat, 1 = maximum. Combines hearing (footsteps) and sight. */
    static double threat(AbstractFish fish, ServerPlayer player, double speedPerTick) {
        double flee = FishingConfig.fleeRange();
        double distance = Math.sqrt(player.distanceToSqr(fish));
        double severity = 0.0;

        // --- hearing: footsteps / noise near the water ---
        if (FishingConfig.isHearingEnabled() && speedPerTick > 0.04 && !player.isShiftKeyDown()) {
            double loudness = player.isSprinting() ? 1.0 : 0.6;
            double radius = flee * loudness;
            if (distance < radius) {
                severity = Math.max(severity, 1.0 - distance / radius);
            }
        }

        // --- sight ---
        if (FishingConfig.isVisionEnabled()) {
            double visionRadius = player.isInWater() ? flee * 0.8 : 4.0;
            if (player.isShiftKeyDown()) visionRadius *= 0.5;
            if (distance < visionRadius && fish.hasLineOfSight(player)) {
                severity = Math.max(severity, 0.35 + 0.65 * (1.0 - distance / visionRadius));
            }
            // Walking straight at a fish.
            if (distance < 6.0 && speedPerTick > 0.02) {
                Vec3 toFish = fish.position().subtract(player.getEyePosition()).normalize();
                if (player.getLookAngle().dot(toFish) > 0.985) {
                    severity = Math.max(severity, 0.6);
                }
            }
        }
        return Mth.clamp(severity, 0.0, 1.0);
    }

    // ------------------------------------------------------------------------------------------
    // Reactions
    // ------------------------------------------------------------------------------------------

    /** Makes the fish swim away from {@code from} and ignore the bait until it has calmed down. */
    public static void spook(AbstractFish fish, Vec3 from, int fleeTicks, double speedModifier) {
        if (!(fish.level() instanceof ServerLevel level)) return;
        long now = level.getServer().getTickCount();
        Mind mind = MINDS.computeIfAbsent(fish.getUUID(), id -> new Mind());
        mind.mood = Mood.FLEEING;
        mind.until = now + fleeTicks;
        mind.alertScans = 0;
        swimAwayFrom(fish, level, from, speedModifier);
    }

    private static void swimAwayFrom(AbstractFish fish, ServerLevel level, Vec3 from, double speed) {
        RandomSource random = fish.getRandom();
        Vec3 away = fish.position().subtract(from);
        double len = Math.sqrt(away.x * away.x + away.z * away.z);
        double baseAngle = len < 1.0E-4 ? random.nextDouble() * Math.PI * 2.0 : Math.atan2(away.z, away.x);

        for (int attempt = 0; attempt < 4; attempt++) {
            double angle = baseAngle + (random.nextDouble() - 0.5) * Math.toRadians(100.0);
            double dist = 7.0 + random.nextDouble() * 4.0;
            double x = fish.getX() + Math.cos(angle) * dist;
            double z = fish.getZ() + Math.sin(angle) * dist;
            double y = fish.getY() + (random.nextDouble() - 0.6) * 2.0;
            BlockPos target = BlockPos.containing(x, y, z);
            if (level.hasChunkAt(target) && level.getFluidState(target).is(FluidTags.WATER)) {
                fish.getNavigation().moveTo(x, y, z, speed);
                return;
            }
        }
        // No reachable water point found: at least give the fish a push in the right direction.
        if (len > 1.0E-4) {
            fish.setDeltaMovement(fish.getDeltaMovement().add(away.x / len * 0.2, 0.0, away.z / len * 0.2));
        }
    }

    private static void alert(AbstractFish fish, Mind mind, long now) {
        mind.mood = Mood.ALERT;
        mind.until = now + ALERT_TICKS + fish.getRandom().nextInt(20);
        mind.alertScans = 1;
        fish.getNavigation().stop();
        fish.setDeltaMovement(fish.getDeltaMovement().scale(0.2));
    }

    // ------------------------------------------------------------------------------------------
    // Server driven scan
    // ------------------------------------------------------------------------------------------

    /** Called once per server tick. */
    public static void tickServer(MinecraftServer server) {
        int tick = server.getTickCount();
        if (tick % SCAN_INTERVAL == 0) {
            expireMinds(tick);
        }
        if (tick % 200 == 0) {
            pruneOfflinePlayers(server);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if ((tick + player.getId()) % SCAN_INTERVAL != 0) continue;
            scanPlayer(player, tick);
        }
    }

    private static void scanPlayer(ServerPlayer player, long now) {
        // Measure how fast the player moved since the last scan (horizontal blocks per tick).
        Vec3 pos = player.position();
        Vec3 last = LAST_POS.put(player.getUUID(), pos);
        double speed = 0.0;
        if (last != null) {
            double dx = pos.x - last.x;
            double dz = pos.z - last.z;
            speed = Math.sqrt(dx * dx + dz * dz) / SCAN_INTERVAL;
        }
        SPEED.put(player.getUUID(), speed);

        if (player.isSpectator() || !player.isAlive()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!FishingConfig.isFightEnabled()) return;
        if (!FishingConfig.isHearingEnabled() && !FishingConfig.isVisionEnabled()) return;

        double flee = FishingConfig.fleeRange();
        AABB box = player.getBoundingBox().inflate(flee, flee * 0.6, flee);
        List<AbstractFish> fish = level.getEntitiesOfClass(AbstractFish.class, box,
                f -> f.isAlive() && f.isInWater() && !FishingManager.isControlled(f));
        int handled = 0;
        for (AbstractFish f : fish) {
            if (++handled > MAX_FISH_PER_SCAN) break;
            double severity = threat(f, player, speed);
            if (severity <= 0.0) continue;
            react(f, player, severity, now);
        }
    }

    private static void react(AbstractFish fish, ServerPlayer player, double severity, long now) {
        Mind mind = MINDS.computeIfAbsent(fish.getUUID(), id -> new Mind());
        if (mind.mood == Mood.FLEEING) {
            return; // already running
        }
        boolean escalate = mind.mood == Mood.ALERT && ++mind.alertScans >= 3;
        if (severity >= 0.5 || escalate) {
            spook(fish, player.position(), 60 + fish.getRandom().nextInt(40), 1.8);
        } else if (mind.mood == Mood.NORMAL || mind.mood == Mood.RECOVERING) {
            alert(fish, mind, now);
        }
    }

    private static void expireMinds(long now) {
        Iterator<Map.Entry<UUID, Mind>> it = MINDS.entrySet().iterator();
        while (it.hasNext()) {
            Mind mind = it.next().getValue();
            if (now < mind.until) continue;
            if (mind.mood == Mood.FLEEING) {
                mind.mood = Mood.RECOVERING;
                mind.until = now + RECOVER_TICKS;
            } else {
                it.remove(); // ALERT / RECOVERING -> back to NORMAL
            }
        }
    }

    private static void pruneOfflinePlayers(MinecraftServer server) {
        SPEED.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        LAST_POS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    /** Drops everything (server stop / feature disabled). */
    public static void clear() {
        MINDS.clear();
        SPEED.clear();
        LAST_POS.clear();
    }
}
