package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.github.ob_yekt.simpleskills.mixin.FISHING.FishingBobberEntityAccessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative coordinator of the fishing minigame.
 *
 * <h3>Life of a session (one per fishing hook)</h3>
 * <ol>
 *   <li><b>SETTLING</b> - the bobber floats; the spot is validated once ({@link FishingEcosystem}).</li>
 *   <li><b>WAITING</b> - a delay that shrinks with Fish Attraction, then a real fish nearby is picked.</li>
 *   <li><b>APPROACHING</b> - the fish swims towards the bait (its vanilla AI is paused).</li>
 *   <li><b>BITING</b> - the bait is at the fish's mouth; right-click starts the fight in time.</li>
 *   <li><b>FIGHTING</b> - {@link FishingFightModel}: the fish pulls left/right/pauses, the player counters
 *       with the camera and reels with the use key; the tension must never reach the limit.</li>
 * </ol>
 * The session ends with a catch (reward + XP exactly once) or a snapped line, or silently when the
 * hook disappears (reeled in early, rod unequipped, player left/died, ...).
 *
 * <p>Everything runs on the server thread. The state lives in plain maps keyed by the hook's UUID.</p>
 */
public final class FishingManager {

    private enum Phase { SETTLING, INVALID, WAITING, APPROACHING, BITING, FIGHTING }

    private static final class Session {
        final FishingHook hook;
        final ServerPlayer player;
        final Random rng;
        final int fishingLevel;
        final double attraction;
        final double surfaceY;

        Phase phase = Phase.SETTLING;
        long phaseStart;
        long nextSelect;
        int failedSelects;
        boolean sawIneligible;
        boolean messageSent;
        boolean finished;
        String lastSelectReport = "";
        int blockedTicks;

        @Nullable Mob fish;
        @Nullable FishSpecies species;
        /** Stored size of the hooked fish (read once when the fish is picked, never rolled again). */
        @Nullable FishAttributes attributes;

        // approach
        double speed;
        double bestDistance = Double.MAX_VALUE;
        long bestDistanceTick;

        // bite
        int biteTicksLeft;

        // fight
        @Nullable FishingFightModel model;
        long lastReelTick;
        long reelGraceUntil;
        float neutralYaw;
        float lastYaw;
        double yawVelocity;
        double startDistance;

        Session(FishingHook hook, ServerPlayer player, int fishingLevel, double attraction, long now) {
            this.hook = hook;
            this.player = player;
            this.rng = new Random(hook.getUUID().getLeastSignificantBits() ^ now);
            this.fishingLevel = fishingLevel;
            this.attraction = attraction;
            this.surfaceY = hook.getY();
            this.phaseStart = now;
        }
    }

    private static final Map<UUID, Session> BY_HOOK = new HashMap<>();
    /** Fish whose vanilla AI is paused by a session. Read by the Mob mixin (any thread). */
    private static final Set<UUID> CONTROLLED_FISH = ConcurrentHashMap.newKeySet();

    private static final int SEND_EVERY_TICKS = 2;
    private static final int FIGHT_TIMEOUT_TICKS = 20 * 150;
    private static final int REEL_WINDOW_TICKS = 5;
    private static final int APPROACH_STUCK_TICKS = 60;

    private FishingManager() {}

    // ==========================================================================================
    // Registration
    // ==========================================================================================

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(FishingStatePayload.TYPE, FishingStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FishingCatchPayload.TYPE, FishingCatchPayload.CODEC);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FishingPerception::tickServer);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (player != null) {
                endSessionsOf(player);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> clearAll());
        ServerLifecycleEvents.SERVER_STARTING.register(server -> clearAll());
    }

    /** Whether the vanilla AI of this mob is currently paused by a fishing session. */
    public static boolean isControlled(Mob mob) {
        return !CONTROLLED_FISH.isEmpty() && CONTROLLED_FISH.contains(mob.getUUID());
    }

    // ==========================================================================================
    // Entry points called from FishingHookFightMixin
    // ==========================================================================================

    /** Called at the end of every server-side tick of a fishing hook. */
    public static void onHookTick(FishingHook hook) {
        if (!FishingConfig.isFightEnabled()) {
            if (!BY_HOOK.isEmpty()) clearAll();
            return;
        }
        if (hook.isRemoved() || !(hook.level() instanceof ServerLevel level)) return;

        Player owner = hook.getPlayerOwner();
        Session session = BY_HOOK.get(hook.getUUID());

        if (!(owner instanceof ServerPlayer player) || !player.isAlive() || player.level() != level) {
            if (session != null) endSession(session, true);
            return;
        }
        if (hook.getHookedIn() != null) { // vanilla entity hooking wins
            if (session != null) endSession(session, true);
            return;
        }

        long now = level.getGameTime();
        if (session == null) {
            if (!isInWater(hook) || hook.tickCount < 10) return;
            session = startSession(hook, player, now);
        }
        if (session.finished) return;

        switch (session.phase) {
            case SETTLING -> tickSettling(session, level, now);
            case WAITING -> tickWaiting(session, level, now);
            case APPROACHING -> tickApproaching(session, level, now);
            case BITING -> tickBiting(session, level, now);
            case FIGHTING -> tickFighting(session, level, now);
            case INVALID -> { /* no fishing at this spot */ }
        }
    }

    /**
     * Called when the player uses the rod while this hook is out. Returns {@code true} when the
     * click was consumed by the minigame (the vanilla retrieve must not run).
     */
    public static boolean onRetrieve(FishingHook hook) {
        if (!FishingConfig.isFightEnabled()) return false;
        Session session = BY_HOOK.get(hook.getUUID());
        if (session == null || session.finished) return false;
        long now = hook.level().getGameTime();

        if (session.phase == Phase.BITING) {
            startFight(session, now);
            return true;
        }
        if (session.phase == Phase.FIGHTING) {
            session.lastReelTick = now;
            return true;
        }
        return false; // nothing biting yet: vanilla simply reels the bait in
    }

    /** Called when the hook entity is removed for any reason. */
    public static void onHookRemoved(FishingHook hook) {
        Session session = BY_HOOK.get(hook.getUUID());
        if (session != null) {
            endSession(session, false);
        }
    }

    // ==========================================================================================
    // Phases
    // ==========================================================================================

    private static Session startSession(FishingHook hook, ServerPlayer player, long now) {
        // One active session per player: an older hook of the same player is closed first.
        endSessionsOf(player);

        int level = XPManager.getSkillLevel(player.getStringUUID(), Skills.FISHING);
        double attraction = FishingProgression.attractionMultiplier(level, ConfigManager.isFishingSpeedBonusEnabled());
        Session session = new Session(hook, player, level, attraction, now);
        BY_HOOK.put(hook.getUUID(), session);
        FishingDebug.info(player, "session started at " + hook.blockPosition().toShortString()
                + " | fishing level " + level + " | attraction x" + String.format("%.2f", attraction));
        return session;
    }

    private static void tickSettling(Session s, ServerLevel level, long now) {
        FishingEcosystem.Result result = FishingEcosystem.evaluate(level, s.player, s.hook.blockPosition(),
                msg -> FishingDebug.info(s.player, msg));
        if (result != FishingEcosystem.Result.OK) {
            FishingDebug.info(s.player, "spot rejected: " + result);
            s.phase = Phase.INVALID;
            sendOnce(s, switch (result) {
                case TOO_SHALLOW -> "§7This water is too shallow to fish in.";
                case NO_FISH -> "§7There are no fish around here.";
                default -> "§7This water is too small to fish in.";
            });
            return;
        }
        setPhase(s, Phase.WAITING, now);
        int wait = waitTicks(s);
        s.nextSelect = now + wait;
        FishingDebug.info(s.player, "waiting " + wait + " ticks before a fish takes an interest");
    }

    private static void tickWaiting(Session s, ServerLevel level, long now) {
        if (now < s.nextSelect) return;

        Mob fish = selectFish(s, level);
        if (fish == null) {
            s.failedSelects++;
            s.nextSelect = now + 40;
            FishingDebug.info(s.player, "no eligible fish (attempt " + s.failedSelects + "): " + s.lastSelectReport);
            if (s.failedSelects >= 3 && s.sawIneligible) {
                sendOnce(s, "§7The fish around here are too advanced for you.");
            }
            return;
        }

        s.fish = fish;
        s.species = FishSpecies.of(fish);
        s.attributes = FishAttributes.of(fish);
        FishingDebug.info(s.player, "fish picked: " + s.species + (s.attributes == null ? "" : " " + s.attributes.weightKg()
                + " kg / " + s.attributes.lengthCm() + " cm") + " at " + String.format("%.1f", Math.sqrt(fish.distanceToSqr(s.hook)))
                + " blocks | " + s.lastSelectReport);
        CONTROLLED_FISH.add(fish.getUUID());
        fish.getNavigation().stop();
        s.speed = 0.03;
        s.bestDistance = Double.MAX_VALUE;
        s.bestDistanceTick = now;
        setPhase(s, Phase.APPROACHING, now);
    }

    private static void tickApproaching(Session s, ServerLevel level, long now) {
        Mob fish = s.fish;
        if (!fishUsable(s, level)) {
            loseFish(s, now, 40);
            return;
        }

        // Fish give up when something scary happens near them.
        if (now % 10 == 0 && FishingPerception.maxThreat(level, fish, s.player.getUUID()) >= 0.5) {
            FishingDebug.info(s.player, "fish got scared by another player and swam away");
            FishingPerception.spook(fish, s.player.position(), 80, 1.8);
            releaseFish(s);
            setPhase(s, Phase.WAITING, now);
            s.nextSelect = now + 80;
            return;
        }

        Vec3 target = s.hook.position().add(0.0, -0.2, 0.0);
        Vec3 mouth = mouthOf(fish);
        Vec3 toTarget = target.subtract(mouth);
        double distance = toTarget.length();

        if (distance < 0.55) {
            startBite(s, level, now);
            return;
        }

        // Stuck (terrain in the way, ...): let the fish go and try another one later.
        if (distance < s.bestDistance - 0.05) {
            s.bestDistance = distance;
            s.bestDistanceTick = now;
        } else if (now - s.bestDistanceTick > APPROACH_STUCK_TICKS) {
            FishingDebug.info(s.player, "fish got stuck at distance " + String.format("%.1f", distance) + " (blocked ticks "
                    + s.blockedTicks + "), picking another one");
            releaseFish(s);
            setPhase(s, Phase.WAITING, now);
            s.nextSelect = now + 40;
            return;
        }

        double maxSpeed = Math.min(0.32, 0.085 * s.attraction);
        s.speed = Mth.lerp(0.12, s.speed, maxSpeed);
        Vec3 direction = toTarget.normalize();
        // Gentle side-to-side wiggle so the swimming does not look like a rail.
        Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
        double wiggle = Math.sin((now + fish.getId()) * 0.35) * 0.025 * Math.min(1.0, distance / 3.0);
        Vec3 step = direction.scale(Math.min(s.speed, distance)).add(side.scale(wiggle));

        if (moveFish(fish, level, step)) {
            s.blockedTicks = 0;
        } else {
            s.blockedTicks++;
        }
        faceTowards(fish, step, 22.0F);
        if (now % 20 == 0) {
            FishingDebug.trace(s.player, "approaching: distance " + String.format("%.1f", distance)
                    + ", speed " + String.format("%.3f", s.speed) + ", blocked ticks " + s.blockedTicks);
        }

        // The bobber twitches when a fish is close (vanilla "tease" feeling).
        if (distance < 2.5 && now % 12 == 0) {
            s.hook.setDeltaMovement(s.hook.getDeltaMovement().add(0.0, -0.04, 0.0));
            level.sendParticles(ParticleTypes.BUBBLE, fish.getX(), fish.getY() + 0.2, fish.getZ(), 2, 0.1, 0.05, 0.1, 0.0);
        } else if (now % 8 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE, fish.getX(), fish.getY() + 0.2, fish.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private static void startBite(Session s, ServerLevel level, long now) {
        FishingDebug.info(s.player, "BITE! right-click within " + FishingConfig.biteWindowTicks() + " ticks");
        s.biteTicksLeft = FishingConfig.biteWindowTicks();
        setPhase(s, Phase.BITING, now);
        setBiting(s.hook, true);
        level.playSound(null, s.hook.getX(), s.hook.getY(), s.hook.getZ(), SoundEvents.FISHING_BOBBER_SPLASH,
                SoundSource.NEUTRAL, 0.5F, 1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.4F);
        level.sendParticles(ParticleTypes.FISHING, s.hook.getX(), s.hook.getY() + 0.1, s.hook.getZ(), 8, 0.25, 0.0, 0.25, 0.1);
        send(s, FishingStatePayload.STATE_BITE, 0f, 0f, 0f, 0);
    }

    private static void tickBiting(Session s, ServerLevel level, long now) {
        Mob fish = s.fish;
        if (!fishUsable(s, level)) {
            setBiting(s.hook, false);
            loseFish(s, now, 40);
            send(s, FishingStatePayload.STATE_LOST, 0f, 0f, 0f, 0);
            return;
        }

        // Fish tugs at the bait: small shake of the body, the bobber stays in its mouth.
        float shake = (float) Math.sin(now * 1.7) * 6.0F;
        fish.setYRot(fish.getYRot() + shake * 0.2F);
        fish.yBodyRot = fish.getYRot();
        fish.yHeadRot = fish.getYRot();
        snapHookToMouth(s);

        if (now % 10 == 0) {
            send(s, FishingStatePayload.STATE_BITE, 0f, 0f, 0f, 0);
        }
        if (--s.biteTicksLeft <= 0) {
            // Too slow: the fish spits the hook and swims away.
            setBiting(s.hook, false);
            FishingPerception.spook(fish, s.hook.position(), 100, 1.8);
            releaseFish(s);
            setPhase(s, Phase.WAITING, now);
            s.nextSelect = now + 60 + s.rng.nextInt(60);
            send(s, FishingStatePayload.STATE_LOST, 0f, 0f, 0f, 0);
        }
    }

    private static void startFight(Session s, long now) {
        Mob fish = s.fish;
        FishSpecies species = s.species;
        if (fish == null || species == null) return;

        FishingDebug.info(s.player, "fight started against " + species);
        s.model = new FishingFightModel(FishingProgression.fightParams(s.fishingLevel, species, s.attributes), s.rng);
        s.lastReelTick = Long.MIN_VALUE / 2;
        s.reelGraceUntil = now + 12; // the click that started the fight must not also reel
        s.neutralYaw = s.player.getYRot();
        s.lastYaw = s.neutralYaw;
        s.yawVelocity = 0.0;
        double dx = fish.getX() - s.player.getX();
        double dz = fish.getZ() - s.player.getZ();
        s.startDistance = Math.max(4.0, Math.sqrt(dx * dx + dz * dz));
        setPhase(s, Phase.FIGHTING, now);
    }

    private static void tickFighting(Session s, ServerLevel level, long now) {
        Mob fish = s.fish;
        FishingFightModel model = s.model;
        if (model == null || !fishUsable(s, level)) {
            setBiting(s.hook, false);
            loseFish(s, now, 40);
            send(s, FishingStatePayload.STATE_LOST, 0f, 0f, 0f, 0);
            return;
        }
        if (now - s.phaseStart > FIGHT_TIMEOUT_TICKS) {
            escape(s, level, false);
            return;
        }

        // ---- camera: how well is the player countering the pull? ----
        float yaw = s.player.getYRot();
        double yawDelta = Mth.wrapDegrees(yaw - s.lastYaw);
        s.lastYaw = yaw;
        s.yawVelocity = s.yawVelocity * 0.5 + yawDelta * 0.5;

        FishingFightModel.Move move = model.move();
        double offset = Mth.wrapDegrees(yaw - s.neutralYaw);
        double positionCounter = FishingFightModel.counterFromOffset(move.dir, offset);
        double velocityCounter = Mth.clamp((-move.dir * s.yawVelocity - 0.4) / 2.2, 0.0, 1.0) * 0.8;
        double counter = Math.max(positionCounter, velocityCounter);
        if (move == FishingFightModel.Move.PAUSE) {
            // The neutral direction slowly follows the camera while the fish rests.
            s.neutralYaw += (float) (Mth.wrapDegrees(yaw - s.neutralYaw) * 0.05);
        }

        boolean reeling = now >= s.reelGraceUntil && now - s.lastReelTick <= REEL_WINDOW_TICKS;
        FishingFightModel.Outcome outcome = model.tick(reeling, counter);

        moveFishInFight(s, level, model);
        snapHookToMouth(s);

        if (move != FishingFightModel.Move.PAUSE && now % 6 == 0) {
            level.sendParticles(ParticleTypes.SPLASH, fish.getX(), s.surfaceY + 0.1, fish.getZ(), 3, 0.15, 0.0, 0.15, 0.0);
        }
        if (now % SEND_EVERY_TICKS == 0) {
            send(s, FishingStatePayload.STATE_FIGHT, (float) model.tensionRatio(), (float) model.fatigue(),
                    (float) model.progress(), model.move().dir);
        }

        switch (outcome) {
            case CAUGHT -> completeCatch(s, level);
            case LINE_BROKEN -> escape(s, level, true);
            case ONGOING -> { }
        }
    }

    // ==========================================================================================
    // Outcomes
    // ==========================================================================================

    private static void completeCatch(Session s, ServerLevel level) {
        if (s.finished) return; // never reward twice
        s.finished = true;
        FishingDebug.info(s.player, "caught " + s.species);

        Mob fish = s.fish;
        FishSpecies species = s.species;
        ServerPlayer player = s.player;
        if (fish == null || species == null) {
            endSession(s, true);
            return;
        }

        // The stored size of THIS fish drives the XP and the result screen (never rolled again).
        FishAttributes attributes = s.attributes != null ? s.attributes : FishAttributes.of(fish);

        InteractionHand hand = rodHand(player);
        ItemStack rod = hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
        int luck = ((FishingBobberEntityAccessor) s.hook).getLuckBonus();

        // Rewards go straight to the inventory, wherever the fish was when the bar filled up.
        ItemStack main = new ItemStack(species.item());
        List<ItemStack> bonus = FishingLoot.rollBonus(level, player, s.hook, rod, s.fishingLevel, luck);
        FishingLoot.deliver(level, player, s.hook, rod, main, bonus, level.getRandom().nextInt(6) + 1);

        if (hand != null && !rod.isEmpty()) {
            rod.hurtAndBreak(1, player, hand.asEquipmentSlot());
        }

        // XP: only here, once, after the catch is confirmed by the server; bigger fish are worth more.
        int baseXp = ConfigManager.getFishingXP("catch", Skills.FISHING);
        int xp = FishingProgression.catchXp(baseXp, species, attributes);
        if (xp > 0) {
            XPManager.addXPWithNotification(player, Skills.FISHING, xp);
        }
        FishingDebug.info(player, "reward: " + species + (attributes == null ? "" : " " + attributes.weightKg() + " kg / "
                + attributes.lengthCm() + " cm") + ", xp " + xp);

        send(s, FishingStatePayload.STATE_CAUGHT, 0f, 0f, 1f, 0);
        sendCatchResult(player, fish, species, attributes, xp);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.NEUTRAL, 1.0F, 0.9F);

        // The fish leaves the world as the reward (vanilla behaviour: hook disappears too).
        CONTROLLED_FISH.remove(fish.getUUID());
        fish.discard();
        s.fish = null;
        s.hook.discard(); // -> onHookRemoved -> endSession
    }

    /** Shows the confirmed catch on the client (purely informational: the screen can not grant anything). */
    private static void sendCatchResult(ServerPlayer player, Mob fish, FishSpecies species, @Nullable FishAttributes attributes, int xp) {
        if (!FishingConfig.isCatchScreenEnabled() || attributes == null) return;
        if (player.connection == null || !ServerPlayNetworking.canSend(player, FishingCatchPayload.TYPE)) return;
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(species.entityType()).toString();
        ServerPlayNetworking.send(player, new FishingCatchPayload(typeId, species.descriptionId(),
                species.variantOf(fish), attributes.weightKg(), attributes.lengthCm(), xp));
    }

    /** The fish gets away. {@code lineBroke}: the line snapped (durability loss, hook lost). */
    private static void escape(Session s, ServerLevel level, boolean lineBroke) {
        if (s.finished) return;
        s.finished = true;
        FishingDebug.info(s.player, lineBroke ? "line snapped, the fish escaped" : "the fish escaped");

        Mob fish = s.fish;
        ServerPlayer player = s.player;
        Vec3 hookPos = s.hook.position();

        setBiting(s.hook, false);
        releaseFish(s);
        if (fish != null && fish.isAlive()) {
            // The very same entity swims away fast and calms down later; nothing is replaced.
            FishingPerception.spook(fish, hookPos, 140, 2.2);
        }

        if (lineBroke) {
            InteractionHand hand = rodHand(player);
            int loss = FishingConfig.lineBreakDurabilityLoss();
            if (hand != null && loss > 0) {
                player.getItemInHand(hand).hurtAndBreak(loss, player, hand.asEquipmentSlot());
            }
            level.playSound(null, hookPos.x, hookPos.y, hookPos.z, SoundEvents.LEAD_BREAK,
                    SoundSource.NEUTRAL, 1.0F, 0.8F);
        }
        send(s, lineBroke ? FishingStatePayload.STATE_LINE_BROKEN : FishingStatePayload.STATE_LOST, 0f, 0f, 0f, 0);
        s.hook.discard(); // -> onHookRemoved -> endSession (no rewards, no XP)
    }

    // ==========================================================================================
    // Fish helpers
    // ==========================================================================================

    private static @Nullable Mob selectFish(Session s, ServerLevel level) {
        BlockPos bait = s.hook.blockPosition();
        double range = FishingConfig.detectionRange();
        List<Mob> candidates = new ArrayList<>();
        int total = 0, busy = 0, scared = 0, far = 0, lowLevel = 0;
        for (Mob fish : FishingEcosystem.nearbyFish(level, bait)) {
            FishSpecies species = FishSpecies.of(fish);
            if (species == null) continue;
            total++;
            if (CONTROLLED_FISH.contains(fish.getUUID())) { busy++; continue; }
            if (!FishingPerception.isCalm(fish)) { scared++; continue; }
            double dy = s.hook.getY() - fish.getY();
            if (fish.distanceToSqr(s.hook) > (range + 4.0) * (range + 4.0) || dy > FishingEcosystem.SEARCH_DEPTH) { far++; continue; }
            if (s.fishingLevel < FishingConfig.minLevelFor(species)) {
                s.sawIneligible = true;
                lowLevel++;
                continue;
            }
            candidates.add(fish);
        }
        s.lastSelectReport = "nearby=" + total + ", usable=" + candidates.size() + ", busy=" + busy
                + ", scared=" + scared + ", too far=" + far + ", level too low=" + lowLevel;
        if (candidates.isEmpty()) return null;

        candidates.sort(Comparator.comparingDouble(f -> f.distanceToSqr(s.hook)));
        int pool = Math.min(3, candidates.size());
        return candidates.get(s.rng.nextInt(pool));
    }

    private static boolean fishUsable(Session s, ServerLevel level) {
        Mob fish = s.fish;
        return fish != null && fish.isAlive() && !fish.isRemoved() && fish.level() == level && fish.isInWater();
    }

    /** The fish disappeared or died: free the session for the next fish. */
    private static void loseFish(Session s, long now, int retryDelay) {
        releaseFish(s);
        setPhase(s, Phase.WAITING, now);
        s.nextSelect = now + retryDelay;
    }

    private static void releaseFish(Session s) {
        Mob fish = s.fish;
        if (fish != null) {
            CONTROLLED_FISH.remove(fish.getUUID());
            if (fish.isAlive() && fish.level() instanceof ServerLevel) {
                fish.getNavigation().stop();
            }
        }
        s.fish = null;
        s.species = null;
        s.attributes = null;
        s.model = null;
    }

    private static Vec3 mouthOf(Mob fish) {
        float yaw = fish.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        return fish.position().add(forward.scale(fish.getBbWidth() * 0.55)).add(0.0, fish.getBbHeight() * 0.5, 0.0);
    }

    private static void snapHookToMouth(Session s) {
        Mob fish = s.fish;
        if (fish == null) return;
        Vec3 mouth = mouthOf(fish);
        s.hook.setPos(mouth.x, mouth.y, mouth.z);
        s.hook.setDeltaMovement(Vec3.ZERO);
    }

    private static void moveFishInFight(Session s, ServerLevel level, FishingFightModel model) {
        Mob fish = s.fish;
        if (fish == null) return;
        ServerPlayer player = s.player;

        double ax = fish.getX() - player.getX();
        double az = fish.getZ() - player.getZ();
        double dist = Math.sqrt(ax * ax + az * az);
        if (dist < 1.0E-3) {
            float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
            ax = -Mth.sin(yaw);
            az = Mth.cos(yaw);
            dist = 1.0;
        }
        double nx = ax / dist;
        double nz = az / dist;
        // The player's "right" when looking at the fish.
        double rx = -nz;
        double rz = nx;

        double lateral = model.move().dir * (0.045 + 0.075 * model.resistance());
        double targetDistance = Math.max(2.5, s.startDistance * (1.0 - model.progress()));
        double radial = Mth.clamp((targetDistance - dist) * 0.12, -0.14, 0.14);
        double vertical = Mth.clamp((s.surfaceY - 1.0 - fish.getY()) * 0.1, -0.05, 0.05);

        Vec3 step = new Vec3(rx * lateral + nx * radial, vertical, rz * lateral + nz * radial);
        moveFish(fish, level, step);
        faceTowards(fish, step, 25.0F);
    }

    /** Moves the (AI-paused) fish by {@code step}, never letting it leave the water. */
    private static boolean moveFish(Mob fish, ServerLevel level, Vec3 step) {
        Vec3 pos = fish.position();
        Vec3 eye = fish.getEyePosition();
        if (!waterAt(level, pos.add(step)) || !waterAt(level, eye.add(step))) {
            Vec3 flat = new Vec3(step.x, 0.0, step.z);
            Vec3 vertical = new Vec3(0.0, step.y, 0.0);
            if (waterAt(level, pos.add(flat)) && waterAt(level, eye.add(flat))) {
                step = flat;
            } else if (waterAt(level, pos.add(vertical)) && waterAt(level, eye.add(vertical))) {
                step = vertical;
            } else {
                return false;
            }
        }
        fish.move(MoverType.SELF, step);
        fish.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    private static boolean waterAt(ServerLevel level, Vec3 pos) {
        BlockPos block = BlockPos.containing(pos);
        return level.hasChunkAt(block) && level.getFluidState(block).is(FluidTags.WATER);
    }

    private static void faceTowards(Mob fish, Vec3 direction, float maxTurnDegrees) {
        double horizontal = direction.horizontalDistance();
        if (horizontal < 1.0E-3) return;
        float target = (float) (Mth.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
        float turn = Mth.clamp(Mth.wrapDegrees(target - fish.getYRot()), -maxTurnDegrees, maxTurnDegrees);
        float yaw = fish.getYRot() + turn;
        fish.setYRot(yaw);
        fish.yBodyRot = yaw;
        fish.yHeadRot = yaw;
        fish.setXRot(Mth.clamp((float) -(Mth.atan2(direction.y, horizontal) * 180.0 / Math.PI), -30.0F, 30.0F));
    }

    // ==========================================================================================
    // Misc helpers
    // ==========================================================================================

    private static boolean isInWater(FishingHook hook) {
        return hook.level().getFluidState(hook.blockPosition()).is(FluidTags.WATER);
    }

    private static @Nullable InteractionHand rodHand(ServerPlayer player) {
        if (player.getMainHandItem().is(Items.FISHING_ROD)) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().is(Items.FISHING_ROD)) return InteractionHand.OFF_HAND;
        return null;
    }

    private static int waitTicks(Session s) {
        // Base 2-7 seconds, shortened by Fish Attraction (never below 1 second).
        int base = 40 + s.rng.nextInt(100);
        return Math.max(20, (int) Math.round(base / Math.max(0.1, s.attraction)));
    }

    private static void setPhase(Session s, Phase phase, long now) {
        s.phase = phase;
        s.phaseStart = now;
    }

    private static void setBiting(FishingHook hook, boolean biting) {
        if (hook instanceof FishingHookDuck duck && !hook.isRemoved()) {
            duck.simpleskills$setBiting(biting);
        }
    }

    private static void sendOnce(Session s, String message) {
        if (s.messageSent) return;
        s.messageSent = true;
        s.player.sendSystemMessage(Component.literal(message), true);
    }

    private static void send(Session s, int state, float tension, float fatigue, float progress, int move) {
        ServerPlayer player = s.player;
        if (player.connection == null || !ServerPlayNetworking.canSend(player, FishingStatePayload.TYPE)) return;
        FishSpecies species = s.species;
        ServerPlayNetworking.send(player, new FishingStatePayload(state, tension, fatigue, progress, move,
                species == null ? "" : species.descriptionId()));
    }

    // ==========================================================================================
    // Session teardown
    // ==========================================================================================

    private static void endSession(Session s, boolean discardHook) {
        BY_HOOK.remove(s.hook.getUUID());
        // When the session already ended with a result (caught / escaped) the HUD is showing that
        // result message and hides it by itself, so no STOP packet is sent over it.
        boolean wasShowing = !s.finished && (s.phase == Phase.BITING || s.phase == Phase.FIGHTING);
        s.finished = true;
        if (!s.hook.isRemoved()) {
            setBiting(s.hook, false);
        }
        releaseFish(s);
        if (wasShowing && s.player.connection != null && ServerPlayNetworking.canSend(s.player, FishingStatePayload.TYPE)) {
            ServerPlayNetworking.send(s.player, FishingStatePayload.stop());
        }
        if (discardHook && !s.hook.isRemoved()) {
            s.hook.discard();
        }
    }

    private static void endSessionsOf(ServerPlayer player) {
        List<Session> mine = new ArrayList<>();
        for (Session s : BY_HOOK.values()) {
            if (s.player.getUUID().equals(player.getUUID())) mine.add(s);
        }
        for (Session s : mine) {
            endSession(s, false);
        }
    }

    private static void clearAll() {
        for (Session s : new ArrayList<>(BY_HOOK.values())) {
            endSession(s, false);
        }
        BY_HOOK.clear();
        CONTROLLED_FISH.clear();
        FishingPerception.clear();
        FishingEcosystem.clear();
        FishingDebug.clear();
    }

    /** Debug helper for tests. */
    static int activeSessions() {
        return BY_HOOK.size();
    }
}
