package com.github.ob_yekt.simpleskills.abyss.weapon;

import com.github.ob_yekt.simpleskills.DefenseCombat;
import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import com.github.ob_yekt.simpleskills.crafting.TranscendentAbilities;
import com.github.ob_yekt.simpleskills.prayer.PrayerAbility;
import com.github.ob_yekt.simpleskills.prayer.PrayerVisualEffects;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Prediction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative katana stance, combat combo, and sheathed special-attack controller. */
public final class KatanaStanceManager {
    public static final int SHEATHED = 0;
    public static final int DRAWING = 1;
    public static final int UNSHEATHED = 2;
    public static final int SHEATHING = 3;
    public static final int LOWER_GUARD = 4;
    public static final int ATTACK_1 = 5;
    public static final int ATTACK_2 = 6;
    public static final int ATTACK_3 = 7;
    /** The entire dash, 1-second lock and multi-cut sequence uses this one sheathed two-hand pose. */
    public static final int SPECIAL_DASH = 8;
    public static final int SPECIAL_RECOVERY = 9;
    /** Persistent Lakai idle: sword sheathed in front, both hands holding the saya. */
    public static final int LAKAI_READY = 10;
    /** Automatic sheathing transition entered when Lakai is enabled while the blade is drawn. */
    public static final int LAKAI_SHEATHING = 11;

    public static final int DRAW_TICKS = 25;
    public static final int IDLE_BEFORE_LOWER_GUARD_TICKS = 40;
    public static final int LOWER_GUARD_TRANSITION_TICKS = 7;
    public static final int LOWER_GUARD_HOLD_BEFORE_SHEATH_TICKS = 100;
    public static final int LOWER_GUARD_TOTAL_TICKS =
            LOWER_GUARD_TRANSITION_TICKS + LOWER_GUARD_HOLD_BEFORE_SHEATH_TICKS;
    public static final int SHEATH_TICKS = 27;

    public static final int ATTACK_1_TICKS = 10;
    public static final int ATTACK_2_TICKS = 10;
    public static final int ATTACK_3_TICKS = 14;
    public static final int ATTACK_1_CHAIN_TICKS = 7;
    public static final int ATTACK_2_CHAIN_TICKS = 8;
    public static final int ATTACK_3_CHAIN_TICKS = 11;
    public static final int COMBO_WINDOW_TICKS = 28;
    private static final int ATTACK_1_HIT_TICK = 5;
    private static final int ATTACK_2_HIT_TICK = 5;
    private static final int ATTACK_3_HIT_TICK = 8;

    public static final int SPECIAL_DASH_TICKS = 5;
    public static final int SPECIAL_LOCK_TICKS = 20;
    public static final int SPECIAL_SLASH_COUNT = 6;
    public static final int SPECIAL_SLASH_INTERVAL_TICKS = 2;
    public static final int SPECIAL_SLASH_PHASE_TICKS = SPECIAL_SLASH_COUNT * SPECIAL_SLASH_INTERVAL_TICKS;
    public static final int SPECIAL_TOTAL_TICKS = SPECIAL_DASH_TICKS + SPECIAL_LOCK_TICKS + SPECIAL_SLASH_PHASE_TICKS;
    public static final int SPECIAL_RECOVERY_TICKS = 6;
    public static final double SPECIAL_MAX_DASH_DISTANCE = 10.0D; // +4 blocks
    public static final double SPECIAL_MAX_TARGET_DISTANCE = 9.15D; // +4 blocks; behind offset is added to dash distance
    private static final double SPECIAL_BEHIND_TARGET_OFFSET = 0.85D;
    private static final double NORMAL_ATTACK_REACH = 4.5D;

    private static final int SPECIAL_PHASE_DASH = 0;
    private static final int SPECIAL_PHASE_LOCK = 1;
    private static final int SPECIAL_PHASE_SLASHES = 2;

    private static final Map<UUID, Entry> STATES = new ConcurrentHashMap<>();
    private static long serverTick;

    private KatanaStanceManager() {}

    public static boolean isAttackStance(int stance) {
        return stance == ATTACK_1 || stance == ATTACK_2 || stance == ATTACK_3;
    }

    public static boolean isSpecialStance(int stance) {
        return stance == SPECIAL_DASH || stance == SPECIAL_RECOVERY;
    }

    /** Stances that keep both first-person hands attached to the sheathed saya. */
    public static boolean isSheathedGripStance(int stance) {
        return stance == LAKAI_READY || isSpecialStance(stance);
    }

    /**
     * Called by the server-authoritative mode toggle. Entering Lakai starts sheathing
     * immediately (or switches straight to the sheathed two-hand ready pose when already
     * sheathed). Returning to Parry leaves the katana sheathed so the next click can draw it.
     */
    public static void onLakaiModeChanged(ServerPlayer player, boolean enabled) {
        if (player == null || !AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        UUID id = player.getUUID();
        long now = serverTick;
        if (!enabled) {
            STATES.remove(id);
            broadcastSheathed(server, id);
            return;
        }

        enforceOffhand(player);
        Entry entry = STATES.computeIfAbsent(id, ignored -> new Entry(SHEATHED, now, now, now));
        boolean alreadySheathed = entry.stance == SHEATHED || entry.stance == LAKAI_READY;
        entry.stance = alreadySheathed ? LAKAI_READY : LAKAI_SHEATHING;
        entry.stateStartedTick = now;
        entry.lastAttackTick = now;
        entry.combatReadyTick = now;
        entry.comboStep = 0;
        entry.queuedAttack = false;
        entry.queuedTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
        entry.attackTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
        entry.attackHitApplied = false;
        entry.specialTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
        entry.specialPhase = SPECIAL_PHASE_DASH;
        broadcast(server, id, entry, now);
    }

    public static int attackStanceForStep(int step) {
        return switch (step) {
            case 1 -> ATTACK_1;
            case 2 -> ATTACK_2;
            case 3 -> ATTACK_3;
            default -> UNSHEATHED;
        };
    }

    public static int comboStepForStance(int stance) {
        return switch (stance) {
            case ATTACK_1 -> 1;
            case ATTACK_2 -> 2;
            case ATTACK_3 -> 3;
            default -> 0;
        };
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(KatanaStanceRequestPayload.TYPE,
                KatanaStanceRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KatanaStancePayload.TYPE, KatanaStancePayload.CODEC);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(KatanaStanceRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleRequest(context.player(), payload.action(),
                        payload.targetEntityId(), context.server())));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverTick = 0L;
            STATES.clear();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> STATES.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            serverTick++;
            tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer joining = handler.getPlayer();
            if (joining != null) {
                server.execute(() -> {
                    for (Map.Entry<UUID, Entry> state : STATES.entrySet()) {
                        ServerPlayer active = server.getPlayerList().getPlayer(state.getKey());
                        if (active != null) send(joining, active.getUUID(), state.getValue(), serverTick);
                    }
                });
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID disconnectedId = handler.getPlayer().getUUID();
            STATES.remove(disconnectedId);
            server.execute(() -> broadcastSheathed(server, disconnectedId));
        });
    }

    private static void handleRequest(ServerPlayer player, int action, int targetEntityId, MinecraftServer server) {
        if (player == null || !player.isAlive() || player.isSpectator()
                || !AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) return;

        enforceOffhand(player);
        long now = serverTick;
        UUID id = player.getUUID();
        Entry entry = STATES.computeIfAbsent(id, ignored -> new Entry(SHEATHED, now, now, now));

        boolean lakaiMode = TranscendentAbilities.isKatanaLakaiEnabled(player);

        // Compatibility for clients that still send the dedicated special action.
        if (action == KatanaStanceRequestPayload.SPECIAL_ATTACK) {
            if (lakaiMode) startSpecial(player, entry, targetEntityId, server, now);
            return;
        }
        if (action != KatanaStanceRequestPayload.ATTACK) return;

        // Lakai is a distinct sheathed stance. A click with a valid Mob target starts the
        // special; clicking empty space does not draw, attack, or change the two-hand pose.
        // The client does not need to find a target merely to enter Lakai mode: the mode
        // toggle itself has already synchronized LAKAI_READY to every client.
        if (lakaiMode) {
            if (entry.stance == SHEATHED) {
                entry.stance = LAKAI_READY;
                entry.stateStartedTick = now;
                entry.lastAttackTick = now;
                entry.combatReadyTick = now;
                broadcast(server, id, entry, now);
            }
            if (entry.stance == LAKAI_READY && isValidSpecialTarget(player, targetEntityId)) {
                startSpecial(player, entry, targetEntityId, server, now);
            }
            return;
        }

        // The first click only draws the blade. Damage is impossible until the complete draw
        // animation has finished and the player is standing in the combat guard.
        if (entry.stance == SHEATHED || entry.stance == SHEATHING) {
            entry.stance = DRAWING;
            entry.stateStartedTick = now;
            entry.lastAttackTick = now;
            entry.combatReadyTick = now;
            entry.comboStep = 0;
            entry.queuedAttack = false;
            entry.attackTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
            broadcast(server, id, entry, now);
            return;
        }
        if (entry.stance == DRAWING || isSpecialStance(entry.stance)) {
            // No attack buffering while drawing or during the sheathed special move.
            return;
        }

        // Lower guard is a relaxed, non-damaging posture. First re-enter the full battle guard;
        // the next click can then begin a damaging attack from the exact idle_unsheathed pose.
        if (entry.stance == LOWER_GUARD) {
            entry.stance = UNSHEATHED;
            entry.stateStartedTick = now;
            entry.lastAttackTick = now;
            entry.combatReadyTick = now;
            entry.comboStep = 0;
            entry.queuedAttack = false;
            broadcast(server, id, entry, now);
            return;
        }

        if (isAttackStance(entry.stance)) {
            long elapsed = now - entry.stateStartedTick;
            entry.lastAttackTick = now;
            if (entry.stance == ATTACK_1) {
                if (elapsed >= ATTACK_1_CHAIN_TICKS) startAttack(entry, 2, now, targetEntityId);
                else queueAttack(entry, targetEntityId);
            } else if (entry.stance == ATTACK_2) {
                if (elapsed >= ATTACK_2_CHAIN_TICKS) startAttack(entry, 3, now, targetEntityId);
                else queueAttack(entry, targetEntityId);
            } else {
                if (elapsed >= ATTACK_3_CHAIN_TICKS) startAttack(entry, 1, now, targetEntityId);
                else if (elapsed >= 8) queueAttack(entry, targetEntityId);
            }
            broadcast(server, id, entry, now);
            return;
        }

        if (entry.stance != UNSHEATHED) return;
        int nextStep = entry.comboStep > 0 && (now - entry.lastAttackTick) <= COMBO_WINDOW_TICKS
                ? (entry.comboStep % 3) + 1 : 1;
        startAttack(entry, nextStep, now, targetEntityId);
        broadcast(server, id, entry, now);
    }

    private static void queueAttack(Entry entry, int targetEntityId) {
        entry.queuedAttack = true;
        entry.queuedTargetEntityId = targetEntityId;
    }

    private static void startAttack(Entry entry, int step, long now, int targetEntityId) {
        entry.comboStep = step;
        entry.stance = attackStanceForStep(step);
        entry.stateStartedTick = now;
        entry.lastAttackTick = now;
        entry.combatReadyTick = now;
        entry.queuedAttack = false;
        entry.queuedTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
        entry.attackTargetEntityId = targetEntityId;
        entry.attackHitApplied = false;
    }

    private static void startSpecial(ServerPlayer player, Entry entry, int targetEntityId,
                                     MinecraftServer server, long now) {
        if (!TranscendentAbilities.isKatanaLakaiEnabled(player)) return;
        if (entry.stance != SHEATHED && entry.stance != LAKAI_READY) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.katana.special_requires_sheathed"), true);
            return;
        }
        LivingEntity target = findSpecialTarget(player, targetEntityId);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.katana.special_target_required"), true);
            return;
        }

        Vec3 towardTarget = new Vec3(target.getX() - player.getX(), 0.0D, target.getZ() - player.getZ());
        if (towardTarget.lengthSqr() < 1.0E-5D) {
            Vec3 look = player.getLookAngle();
            towardTarget = new Vec3(look.x, 0.0D, look.z);
        }
        if (towardTarget.lengthSqr() < 1.0E-5D) return;
        Vec3 direction = towardTarget.normalize();
        double behindOffset = Math.max(SPECIAL_BEHIND_TARGET_OFFSET, target.getBbWidth() * 0.5D + 0.45D);
        Vec3 end = new Vec3(target.getX() + direction.x * behindOffset,
                player.getY(), target.getZ() + direction.z * behindOffset);
        double dashDistance = end.distanceTo(player.position());
        if (dashDistance > SPECIAL_MAX_DASH_DISTANCE || dashDistance < 0.15D) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.katana.special_target_too_far"), true);
            return;
        }

        entry.stance = SPECIAL_DASH;
        entry.stateStartedTick = now;
        entry.lastAttackTick = now;
        entry.combatReadyTick = now;
        entry.specialTargetEntityId = target.getId();
        entry.specialStart = player.position();
        entry.specialEnd = end;
        entry.specialPhase = SPECIAL_PHASE_DASH;
        entry.specialPhaseStartedTick = now;
        entry.specialSlashIndex = 0;
        entry.specialLastSlashTick = now;
        entry.queuedAttack = false;
        entry.attackTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
        player.setDeltaMovement(Vec3.ZERO);

        // Lankai is a celestial lightning dash: reuse the exact procedural SPARK VFX used by
        // the Elementalist ability, stretched along the dash path. The player movement follows
        // this line for the same five ticks, so the teleport reads as a lightning displacement.
        Vec3 dashVfxStart = player.position().add(0.0D, player.getBbHeight() * 0.62D, 0.0D);
        Vec3 dashVfxEnd = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        PrayerVisualEffects.playConnection((ServerLevel) player.level(), dashVfxStart, dashVfxEnd,
                PrayerAbility.SPARK);
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + player.getBbHeight() * 0.55D, player.getZ(),
                18, 0.28D, 0.42D, 0.28D, 0.12D);
        ((ServerLevel) player.level()).playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.65F);
        broadcast(server, player.getUUID(), entry, now);
    }

    private static boolean isValidSpecialTarget(ServerPlayer player, int targetEntityId) {
        return findSpecialTarget(player, targetEntityId) != null;
    }

    private static Mob findSpecialTarget(ServerPlayer player, int targetEntityId) {
        if (targetEntityId < 0) return null;
        Entity entity = player.level().getEntity(targetEntityId);
        // The current stun system immobilizes Mob/boss AI through its existing mixins. Restrict
        // the special target to Mob so the promised one-second lock is real, not just a velocity reset.
        if (entity == null || entity == player || !(entity instanceof Mob target) || !target.isAlive()
                || target.level() != player.level() || !player.hasLineOfSight(target)) return null;
        double distanceSqr = player.distanceToSqr(target);
        if (distanceSqr > SPECIAL_MAX_TARGET_DISTANCE * SPECIAL_MAX_TARGET_DISTANCE) return null;
        return target;
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            boolean holdingKatana = AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem());
            if (holdingKatana) enforceOffhand(player);

            Entry entry = STATES.get(id);
            if (!player.isAlive() || !holdingKatana) {
                if (STATES.remove(id) != null) broadcastSheathed(server, id);
                continue;
            }

            long now = serverTick;
            if (entry == null) {
                // If the player changes hotbar slots and returns to the katana while Lakai is
                // still selected, restore the persistent sheathed two-hand stance immediately.
                if (TranscendentAbilities.isKatanaLakaiEnabled(player)) {
                    Entry ready = new Entry(LAKAI_READY, now, now, now);
                    STATES.put(id, ready);
                    broadcast(server, id, ready, now);
                }
                continue;
            }
            long elapsed = now - entry.stateStartedTick;
            switch (entry.stance) {
                case DRAWING -> {
                    if (elapsed >= DRAW_TICKS) {
                        entry.stance = UNSHEATHED;
                        entry.stateStartedTick = now;
                        entry.combatReadyTick = now;
                        entry.comboStep = 0;
                        entry.queuedAttack = false;
                        broadcast(server, id, entry, now);
                    }
                }
                case ATTACK_1 -> {
                    applyNormalAttackHit(player, entry, elapsed, ATTACK_1_HIT_TICK);
                    if (entry.queuedAttack && elapsed >= ATTACK_1_CHAIN_TICKS) {
                        startAttack(entry, 2, now, entry.queuedTargetEntityId);
                        broadcast(server, id, entry, now);
                    } else if (elapsed >= ATTACK_1_TICKS) {
                        returnToCombatGuard(entry, now);
                        broadcast(server, id, entry, now);
                    }
                }
                case ATTACK_2 -> {
                    applyNormalAttackHit(player, entry, elapsed, ATTACK_2_HIT_TICK);
                    if (entry.queuedAttack && elapsed >= ATTACK_2_CHAIN_TICKS) {
                        startAttack(entry, 3, now, entry.queuedTargetEntityId);
                        broadcast(server, id, entry, now);
                    } else if (elapsed >= ATTACK_2_TICKS) {
                        returnToCombatGuard(entry, now);
                        broadcast(server, id, entry, now);
                    }
                }
                case ATTACK_3 -> {
                    applyNormalAttackHit(player, entry, elapsed, ATTACK_3_HIT_TICK);
                    if (entry.queuedAttack && elapsed >= ATTACK_3_CHAIN_TICKS) {
                        startAttack(entry, 1, now, entry.queuedTargetEntityId);
                        broadcast(server, id, entry, now);
                    } else if (elapsed >= ATTACK_3_TICKS) {
                        entry.stance = UNSHEATHED;
                        entry.stateStartedTick = now;
                        entry.combatReadyTick = now;
                        entry.comboStep = 0;
                        entry.queuedAttack = false;
                        entry.queuedTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
                        broadcast(server, id, entry, now);
                    }
                }
                case UNSHEATHED -> {
                    long idleReference = Math.max(entry.lastAttackTick, entry.combatReadyTick);
                    if (now - idleReference >= IDLE_BEFORE_LOWER_GUARD_TICKS) {
                        entry.stance = LOWER_GUARD;
                        entry.stateStartedTick = now;
                        entry.comboStep = 0;
                        entry.queuedAttack = false;
                        broadcast(server, id, entry, now);
                    }
                }
                case LOWER_GUARD -> {
                    if (elapsed >= LOWER_GUARD_TOTAL_TICKS) {
                        entry.stance = SHEATHING;
                        entry.stateStartedTick = now;
                        entry.comboStep = 0;
                        entry.queuedAttack = false;
                        broadcast(server, id, entry, now);
                    }
                }
                case SHEATHING -> {
                    if (elapsed >= SHEATH_TICKS) {
                        if (TranscendentAbilities.isKatanaLakaiEnabled(player)) {
                            entry.stance = LAKAI_READY;
                            entry.stateStartedTick = now;
                            entry.lastAttackTick = now;
                            entry.combatReadyTick = now;
                            broadcast(server, id, entry, now);
                        } else {
                            STATES.remove(id);
                            broadcastSheathed(server, id);
                        }
                    }
                }
                case LAKAI_SHEATHING -> {
                    if (elapsed >= SHEATH_TICKS) {
                        entry.stance = LAKAI_READY;
                        entry.stateStartedTick = now;
                        entry.lastAttackTick = now;
                        entry.combatReadyTick = now;
                        broadcast(server, id, entry, now);
                    }
                }
                case SPECIAL_DASH -> tickSpecial(player, entry, server, now);
                case SPECIAL_RECOVERY -> {
                    if (elapsed >= SPECIAL_RECOVERY_TICKS) {
                        if (TranscendentAbilities.isKatanaLakaiEnabled(player)) {
                            entry.stance = LAKAI_READY;
                            entry.stateStartedTick = now;
                            entry.lastAttackTick = now;
                            entry.combatReadyTick = now;
                            broadcast(server, id, entry, now);
                        } else {
                            STATES.remove(id);
                            broadcastSheathed(server, id);
                        }
                    }
                }
                case LAKAI_READY -> {
                    // Persistent two-hand sheathed ready pose; only a targeted click starts Lankai.
                }
                default -> {
                    STATES.remove(id);
                    broadcastSheathed(server, id);
                }
            }
        }
        STATES.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private static void returnToCombatGuard(Entry entry, long now) {
        entry.stance = UNSHEATHED;
        entry.stateStartedTick = now;
        entry.combatReadyTick = now;
        entry.queuedAttack = false;
        entry.queuedTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
    }

    private static void applyNormalAttackHit(ServerPlayer player, Entry entry, long elapsed, int hitTick) {
        if (entry.attackHitApplied || elapsed < hitTick) return;
        entry.attackHitApplied = true;
        LivingEntity target = findNormalAttackTarget(player, entry.attackTargetEntityId);
        if (target == null) return;

        float damage = currentAttackDamage(player);
        if (damage <= 0.0F) return;
        ServerLevel level = (ServerLevel) player.level();
        target.damageCooldownTime = 0;
        target.hurtServer(level, player.damageSources().playerAttack(player), damage);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.55F, 1.1F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                1, 0.15D, 0.18D, 0.15D, 0.0D);
    }

    /**
     * Resolve the attack target again at the impact frame. The click-time entity id is preferred,
     * but if vanilla's crosshair entity was not available in the request, use the server-side
     * look segment so a valid attack does not silently become animation-only.
     */
    private static LivingEntity findNormalAttackTarget(ServerPlayer player, int requestedEntityId) {
        Entity requested = requestedEntityId >= 0 ? player.level().getEntity(requestedEntityId) : null;
        if (requested instanceof LivingEntity living && isValidNormalTarget(player, living)) {
            return living;
        }

        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(NORMAL_ATTACK_REACH));
        AABB search = player.getBoundingBox()
                .expandTowards(end.subtract(start))
                .inflate(0.75D);

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> isValidNormalTarget(player, entity))) {
            var intersection = candidate.getBoundingBox().inflate(0.18D).clip(start, end);
            if (intersection.isEmpty()) continue;
            double distance = intersection.get().distanceToSqr(start);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isValidNormalTarget(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && target.level() == player.level()
                && player.distanceToSqr(target) <= NORMAL_ATTACK_REACH * NORMAL_ATTACK_REACH
                && player.hasLineOfSight(target);
    }

    private static float currentAttackDamage(ServerPlayer player) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        return attackDamage == null ? 6.0F : (float) Math.max(1.0D, attackDamage.getValue());
    }

    private static void tickSpecial(ServerPlayer player, Entry entry, MinecraftServer server, long now) {
        Entity found = player.level().getEntity(entry.specialTargetEntityId);
        if (!(found instanceof Mob target) || !target.isAlive() || target.level() != player.level()) {
            beginSpecialRecovery(entry, server, player.getUUID(), now);
            return;
        }
        ServerLevel level = (ServerLevel) player.level();

        if (entry.specialPhase == SPECIAL_PHASE_DASH) {
            long dashElapsed = now - entry.specialPhaseStartedTick;
            double progress = Math.min(1.0D, dashElapsed / (double) SPECIAL_DASH_TICKS);
            Vec3 desired = entry.specialStart.lerp(entry.specialEnd, progress);
            Vec3 delta = desired.subtract(player.position());
            player.setDeltaMovement(Vec3.ZERO);
            player.move(MoverType.SELF, delta);
            player.needsSync = true;
            player.syncVelocity = true;
            if (dashElapsed >= SPECIAL_DASH_TICKS) {
                // The dash animation/movement may be shortened by entity or block collision.
                // Finish with an authoritative server teleport so the player is reliably placed
                // on the far side of the target instead of stopping at the collision boundary.
                Vec3 travel = entry.specialEnd.subtract(entry.specialStart);
                Vec3 direction = new Vec3(travel.x, 0.0D, travel.z).normalize();
                if (direction.lengthSqr() > 1.0E-5D) {
                    Vec3 behind = findSafeBehindPosition(player, target, level, direction, entry.specialStart);
                    player.teleportTo(behind.x, behind.y, behind.z);
                    Vec3 faceTarget = target.position().subtract(player.position());
                    if (faceTarget.horizontalDistanceSqr() > 1.0E-5D) {
                        float yaw = (float) Math.toDegrees(Math.atan2(-faceTarget.x, faceTarget.z));
                        player.setYRot(yaw);
                        player.setYHeadRot(yaw);
                    }
                }
                player.setDeltaMovement(Vec3.ZERO);
                player.needsSync = true;
                player.syncVelocity = true;
                entry.specialPhase = SPECIAL_PHASE_LOCK;
                entry.specialPhaseStartedTick = now;
                DefenseCombat.applyStun(target, SPECIAL_LOCK_TICKS);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                        20, 0.4D, 0.5D, 0.4D, 0.12D);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                        14, 0.35D, 0.45D, 0.35D, 0.08D);
                broadcast(server, player.getUUID(), entry, now);
            }
            return;
        }

        if (entry.specialPhase == SPECIAL_PHASE_LOCK) {
            if (now - entry.specialPhaseStartedTick >= SPECIAL_LOCK_TICKS) {
                entry.specialPhase = SPECIAL_PHASE_SLASHES;
                entry.specialPhaseStartedTick = now;
                entry.specialSlashIndex = 0;
                entry.specialLastSlashTick = now;
                applySpecialSlash(player, target, level, entry.specialSlashIndex++);
                broadcast(server, player.getUUID(), entry, now);
            }
            return;
        }

        if (entry.specialPhase == SPECIAL_PHASE_SLASHES) {
            long slashElapsed = now - entry.specialPhaseStartedTick;
            if (entry.specialSlashIndex < SPECIAL_SLASH_COUNT
                    && now - entry.specialLastSlashTick >= SPECIAL_SLASH_INTERVAL_TICKS) {
                applySpecialSlash(player, target, level, entry.specialSlashIndex++);
                entry.specialLastSlashTick = now;
            }
            if (entry.specialSlashIndex >= SPECIAL_SLASH_COUNT && slashElapsed >= SPECIAL_SLASH_PHASE_TICKS) {
                beginSpecialRecovery(entry, server, player.getUUID(), now);
            }
        }
    }

    private static Vec3 findSafeBehindPosition(ServerPlayer player, Mob target, ServerLevel level,
                                               Vec3 direction, Vec3 dashStart) {
        double requiredOffset = Math.max(SPECIAL_BEHIND_TARGET_OFFSET,
                target.getBbWidth() * 0.5D + player.getBbWidth() * 0.5D + 0.20D);
        Vec3 best = new Vec3(target.getX() + direction.x * requiredOffset,
                player.getY(), target.getZ() + direction.z * requiredOffset);

        // Prefer the nearest unobstructed point directly behind the target. The small outward
        // search avoids clipping into the mob or a wall while preserving the behind-the-target line.
        for (int step = 0; step <= 6; step++) {
            double offset = requiredOffset + step * 0.20D;
            Vec3 candidate = new Vec3(target.getX() + direction.x * offset,
                    player.getY(), target.getZ() + direction.z * offset);
            if (candidate.distanceTo(dashStart) > SPECIAL_MAX_DASH_DISTANCE) break;
            var candidateBox = player.getBoundingBox().move(candidate.subtract(player.position()));
            best = candidate;
            if (level.noCollision(player, candidateBox)) return candidate;
        }
        return best;
    }

    private static void applySpecialSlash(ServerPlayer player, LivingEntity target, ServerLevel level, int slashIndex) {
        float totalSpecialDamage = currentAttackDamage(player) * 2.0F;
        float perHitDamage = totalSpecialDamage / SPECIAL_SLASH_COUNT;
        target.damageCooldownTime = 0;
        boolean hit = target.hurtServer(level, player.damageSources().playerAttack(player), perHitDamage);

        double height = target.getY() + target.getBbHeight() * (0.32D + (slashIndex % 3) * 0.18D);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                target.getX(), height, target.getZ(), 1, 0.28D, 0.20D, 0.28D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                hit ? 10 : 4, 0.35D, 0.45D, 0.35D, 0.12D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                8, 0.30D, 0.42D, 0.30D, 0.09D);

        // Each cut releases a short SPARK branch outward from the enemy. The SPARK renderer
        // already provides a bright core, three forks and ground arcs, so this reuses the mod's
        // established lightning look instead of adding a second generic particle style.
        Vec3 arcOrigin = target.position().add(0.0D, target.getBbHeight() * 0.56D, 0.0D);
        double arcAngle = slashIndex * 2.399963229728653D;
        double arcLength = 0.72D + (slashIndex % 2) * 0.16D;
        Vec3 arcEnd = arcOrigin.add(Math.cos(arcAngle) * arcLength,
                0.18D + Math.sin(slashIndex * 1.17D) * 0.42D,
                Math.sin(arcAngle) * arcLength);
        PrayerVisualEffects.playConnection(level, arcOrigin, arcEnd, PrayerAbility.SPARK);

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.65F,
                0.82F + slashIndex * 0.055F);
    }

    private static void beginSpecialRecovery(Entry entry, MinecraftServer server, UUID playerId, long now) {
        if (entry.stance == SPECIAL_RECOVERY) return;
        entry.stance = SPECIAL_RECOVERY;
        entry.stateStartedTick = now;
        entry.specialPhaseStartedTick = now;
        broadcast(server, playerId, entry, now);
    }

    /** Move an offhand stack into normal inventory; drop and notify only the remainder. */
    public static void enforceOffhand(ServerPlayer player) {
        if (player == null || !AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) return;
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty()) return;

        ItemStack remaining = offhand.copy();
        player.getInventory().setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
        player.getInventory().add(remaining);
        if (!remaining.isEmpty()) {
            ItemStack dropped = remaining.copy();
            player.drop(dropped, false, Prediction.SERVER_ONLY);
            player.sendSystemMessage(Component.translatable(
                    "message.simpleskills.katana.offhand_dropped", dropped.getHoverName()), false);
        }
    }

    private static void broadcast(MinecraftServer server, UUID playerId, Entry entry, long now) {
        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) send(recipient, playerId, entry, now);
    }

    private static void send(ServerPlayer recipient, UUID playerId, Entry entry, long now) {
        if (!ServerPlayNetworking.canSend(recipient, KatanaStancePayload.TYPE)) return;
        int elapsed = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, now - entry.stateStartedTick));
        int sinceAttack = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, now - entry.lastAttackTick));
        ServerPlayNetworking.send(recipient, new KatanaStancePayload(
                playerId.toString(), entry.stance, elapsed, sinceAttack));
    }

    private static void broadcastSheathed(MinecraftServer server, UUID playerId) {
        KatanaStancePayload payload = new KatanaStancePayload(playerId.toString(), SHEATHED, 0, 0);
        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(recipient, KatanaStancePayload.TYPE)) {
                ServerPlayNetworking.send(recipient, payload);
            }
        }
    }

    private static final class Entry {
        private int stance;
        private long stateStartedTick;
        private long lastAttackTick;
        private long combatReadyTick;
        private int comboStep;
        private boolean queuedAttack;
        private int queuedTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
        private int attackTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
        private boolean attackHitApplied;
        private int specialTargetEntityId = KatanaStanceRequestPayload.NO_TARGET;
        private Vec3 specialStart = Vec3.ZERO;
        private Vec3 specialEnd = Vec3.ZERO;
        private int specialPhase;
        private long specialPhaseStartedTick;
        private long specialLastSlashTick;
        private int specialSlashIndex;

        private Entry(int stance, long stateStartedTick, long lastAttackTick, long combatReadyTick) {
            this.stance = stance;
            this.stateStartedTick = stateStartedTick;
            this.lastAttackTick = lastAttackTick;
            this.combatReadyTick = combatReadyTick;
        }
    }
}
