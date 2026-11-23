package com.github.ob_yekt.simpleskills.crafting;

import com.github.ob_yekt.simpleskills.VeinMining;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative implementation of abilities exclusive to Transcendent equipment.
 * All targets, damage, cooldowns and movement are calculated on the server.
 */
public final class TranscendentAbilities {
    private static final int VISION_REFRESH_TICKS = 40;
    private static final int VISION_ORE_RADIUS = 16;
    private static final int WORLDBREAKER_ORE_RADIUS = 15;
    private static final int MAX_ORE_MARKERS = 12;
    private static final int MAX_HOSTILE_MARKERS = 20;
    private static final Map<UUID, Long> DIVINE_REFLECTION_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> POSEIDON_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> VISION_WAS_ACTIVE = new ConcurrentHashMap<>();
    /** Per-player opt-outs for each area ability; all are enabled by default. */
    private static final Set<UUID> WORLDBREAKER_DISABLED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> BURIED_TRUTH_DISABLED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> EDEN_DOMINION_DISABLED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Boolean> WORLDBREAKER_PREVIEW_ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> WORLDBREAKER_PREVIEW_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, HomingData> HOMING_ARROWS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingSingularity> SINGULARITY_PROJECTILES = new ConcurrentHashMap<>();
    private static final List<SingularityField> SINGULARITIES = new ArrayList<>();
    private static final ThreadLocal<Boolean> WORLD_BREAKER_ACTIVE = ThreadLocal.withInitial(() -> false);

    private record HomingData(UUID owner, UUID target, ServerLevel level, long expiresAt) {}
    private record PendingSingularity(UUID owner, long expiresAt) {}
    private static final class SingularityField {
        private final ServerLevel level;
        private final Vec3 center;
        private final UUID owner;
        private int age;
        private SingularityField(ServerLevel level, Vec3 center, UUID owner) {
            this.level = level;
            this.center = center;
            this.owner = owner;
        }
    }

    private TranscendentAbilities() {}

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        DIVINE_REFLECTION_COOLDOWNS.entrySet().removeIf(e -> e.getValue() <= now);
        POSEIDON_COOLDOWNS.entrySet().removeIf(e -> e.getValue() <= now);
        SINGULARITY_PROJECTILES.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
        for (Map.Entry<UUID, HomingData> entry : HOMING_ARROWS.entrySet()) {
            HomingData data = entry.getValue();
            if (data.expiresAt() <= now || data.level().isClientSide()) {
                HOMING_ARROWS.remove(entry.getKey(), data);
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            enforceAegisOfPurity(player);
            tickVision(player, now);
            tickWorldbreakerPreview(player);
        }
        tickHomingArrows();
        tickSingularities();
    }

    /** Removes all currently active harmful potion effects while Aegis of Purity is worn. */
    public static void enforceAegisOfPurity(ServerPlayer player) {
        if (!CraftingEquipmentManager.hasEquippedPerkInSlot(player, EquipmentSlot.CHEST, "aegis_of_purity")) return;
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    public static void onProjectileFired(Projectile projectile, ItemStack weapon, String encodedPerks) {
        if (projectile == null || weapon == null || encodedPerks == null || encodedPerks.isBlank()
                || !(projectile.getOwner() instanceof ServerPlayer shooter)
                || !(projectile.level() instanceof ServerLevel level)) return;

        if (contains(encodedPerks, "astral_pursuit") && projectile instanceof AbstractArrow
                && isBow(weapon)) {
            LivingEntity target = findTarget(shooter, 64.0);
            if (target != null) {
                HOMING_ARROWS.put(projectile.getUUID(),
                        new HomingData(shooter.getUUID(), target.getUUID(), level,
                                level.getGameTime() + 20L * 12L));
            }
        }
        if (contains(encodedPerks, "singularity_bolt") && isCrossbow(weapon)) {
            SINGULARITY_PROJECTILES.put(projectile.getUUID(),
                    new PendingSingularity(shooter.getUUID(), level.getGameTime() + 20L * 15L));
        }
    }

    /** Called by the Projectile.onHit mixin after the vanilla impact has been processed. */
    public static void onProjectileImpact(Projectile projectile, HitResult hitResult) {
        if (projectile == null || !(projectile.level() instanceof ServerLevel level)) return;
        PendingSingularity pending = SINGULARITY_PROJECTILES.remove(projectile.getUUID());
        if (pending == null) return;

        Vec3 impact = hitResult == null ? projectile.position() : hitResult.getLocation();
        if (hitResult instanceof net.minecraft.world.phys.EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living
                && isHostile(living, projectile.getOwner())) {
            living.hurt(level.damageSources().magic(), 3.0F);
        }

        SINGULARITIES.add(new SingularityField(level, impact, pending.owner()));
        level.sendParticles(ParticleTypes.PORTAL, impact.x, impact.y, impact.z,
                55, 0.8, 0.8, 0.8, 0.45);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, impact.x, impact.y, impact.z,
                30, 0.55, 0.55, 0.55, 0.18);
        level.playSound(null, BlockPos.containing(impact), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.1F, 0.65F);
    }

    private static void tickHomingArrows() {
        // ConcurrentHashMap's iterators are weakly consistent and do not support iterator.remove().
        // Remove entries conditionally through the map so multiple server events remain safe.
        for (Map.Entry<UUID, HomingData> entry : HOMING_ARROWS.entrySet()) {
            UUID projectileId = entry.getKey();
            HomingData data = entry.getValue();
            Entity found = data.level().getEntity(projectileId);
            Entity targetEntity = data.level().getEntity(data.target());
            Entity ownerEntity = data.level().getEntity(data.owner());
            if (!(found instanceof AbstractArrow arrow) || !(targetEntity instanceof LivingEntity target)
                    || !(ownerEntity instanceof ServerPlayer owner) || !arrow.isAlive() || !target.isAlive()
                    || !owner.isAlive() || !owner.hasLineOfSight(target)
                    || arrow.position().distanceToSqr(target.position()) > 64.0 * 64.0) {
                HOMING_ARROWS.remove(projectileId, data);
                continue;
            }

            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
            double speed = Math.max(0.65, arrow.getDeltaMovement().length());
            // Direct homing: the projectile continuously steers to the selected target.
            arrow.setDeltaMovement(toTarget.scale(speed));
            arrow.setYRot((float) (Math.atan2(toTarget.z, toTarget.x) * 180.0 / Math.PI) - 90.0F);
            arrow.setXRot((float) (-Math.atan2(toTarget.y,
                    Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z)) * 180.0 / Math.PI));
        }
    }

    private static void tickSingularities() {
        Iterator<SingularityField> fields = SINGULARITIES.iterator();
        while (fields.hasNext()) {
            SingularityField field = fields.next();
            if (field.level.isClientSide() || ++field.age > 40) {
                fields.remove();
                continue;
            }
            AABB area = new AABB(field.center, field.center).inflate(4.0);
            Entity owner = field.level.getEntity(field.owner);
            for (Mob mob : field.level.getEntitiesOfClass(Mob.class, area,
                    mob -> mob.isAlive() && mob.distanceToSqr(field.center.x, field.center.y, field.center.z) <= 16.0
                            && isHostile(mob, owner))) {
                Vec3 pull = field.center.subtract(mob.getBoundingBox().getCenter());
                double distance = Math.max(0.65, pull.length());
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.35)
                        .add(pull.normalize().scale(0.32 / Math.max(0.55, distance * 0.25))));
                if (field.age % 10 == 0) {
                    mob.hurt(field.level.damageSources().magic(), 1.0F);
                }
            }
            if (field.age % 5 == 0) {
                field.level.sendParticles(ParticleTypes.PORTAL, field.center.x, field.center.y,
                        field.center.z, 8, 0.35, 0.35, 0.35, 0.12);
            }
        }
    }

    /** Poseidon's Wrath is triggered by the trident's real damage event, not by merely throwing it. */
    public static void onWeaponHit(ServerPlayer attacker, LivingEntity victim, String encodedPerks, Entity directEntity) {
        if (!contains(encodedPerks, "poseidons_wrath") || !(attacker.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (POSEIDON_COOLDOWNS.getOrDefault(attacker.getUUID(), 0L) > now) return;
        POSEIDON_COOLDOWNS.put(attacker.getUUID(), now + 120L);

        Vec3 center = victim.position();
        AABB area = victim.getBoundingBox().inflate(10.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != attacker && entity != victim && !(entity instanceof Player)
                        && entity.isAlive() && entity.distanceToSqr(center.x, center.y, center.z) <= 100.0
                        && isHostile(entity, attacker))) {
            target.hurt(level.damageSources().magic(), 4.0F);
            Vec3 knockback = target.position().subtract(center).normalize().scale(1.05);
            target.push(knockback.x, 0.35, knockback.z);
        }
        level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.4, center.z,
                85, 1.8, 0.35, 1.8, 0.2);
        level.sendParticles(ParticleTypes.BUBBLE, center.x, center.y + 0.4, center.z,
                38, 1.6, 0.55, 1.6, 0.08);
        level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_SPLASH,
                SoundSource.PLAYERS, 1.4F, 0.75F);
    }

    /** Called only after DefenseCombat has verified a genuine Perfect Parry. */
    public static void onPerfectParry(ServerPlayer defender, Entity originalAttacker, float incomingDamage) {
        if (!CraftingEquipmentManager.hasActivePerk(defender, "divine_reflection")
                || !(defender.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (DIVINE_REFLECTION_COOLDOWNS.getOrDefault(defender.getUUID(), 0L) > now) return;
        DIVINE_REFLECTION_COOLDOWNS.put(defender.getUUID(), now + 160L);

        int defenseLevel = com.github.ob_yekt.simpleskills.Defense.getSkillLevelFromCache(defender.getUUID());
        float damage = Math.max(3.0F, Math.min(22.0F, 3.0F + defenseLevel * 0.22F
                + Math.max(0.0F, incomingDamage) * 0.45F));
        Vec3 center = defender.position();
        AABB area = defender.getBoundingBox().inflate(10.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != defender && !(entity instanceof Player)
                        && entity.isAlive() && entity.distanceToSqr(center.x, center.y, center.z) <= 100.0
                        && isHostile(entity, defender))) {
            target.hurt(defender.damageSources().magic(), damage);
            addEffect(target, "minecraft:weakness", 0, 60);
            addEffect(target, "minecraft:slowness", 0, 60);
            Vec3 away = target.position().subtract(center).normalize().scale(1.15);
            target.push(away.x, 0.42, away.z);
        }
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.8, center.z,
                80, 2.0, 0.6, 2.0, 0.1);
        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF),
                center.x, center.y + 1.0, center.z, 1,
                0.0, 0.0, 0.0, 0.0);
        level.playSound(null, defender.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.2F, 0.7F);
    }

    /** Area effects for the pickaxe, shovel and hoe have independent toggles and start enabled. */
    public static boolean isWorldbreakerAreaEnabled(ServerPlayer player) {
        return player != null && !WORLDBREAKER_DISABLED.contains(player.getUUID());
    }

    public static boolean isBuriedTruthAreaEnabled(ServerPlayer player) {
        return player != null && !BURIED_TRUTH_DISABLED.contains(player.getUUID());
    }

    public static boolean isEdenDominionAreaEnabled(ServerPlayer player) {
        return player != null && !EDEN_DOMINION_DISABLED.contains(player.getUUID());
    }

    /** Server-authoritative toggle. The held Transcendent tool selects which area ability changes. */
    public static void toggleToolAreaEffect(ServerPlayer player) {
        if (player == null) return;
        ItemStack held = player.getMainHandItem();
        UUID playerId = player.getUUID();
        if (held.is(ItemTags.PICKAXES) && CraftingEquipmentManager.stackHasPerk(held, "worldbreaker")) {
            boolean enabled;
            if (WORLDBREAKER_DISABLED.remove(playerId)) {
                enabled = true;
            } else {
                WORLDBREAKER_DISABLED.add(playerId);
                enabled = false;
                clearWorldbreakerPreview(player);
            }
            player.sendSystemMessage(Component.translatable(enabled
                    ? "message.simpleskills.transcendent_area.worldbreaker_enabled"
                    : "message.simpleskills.transcendent_area.worldbreaker_disabled"));
            return;
        }
        if (held.is(ItemTags.SHOVELS) && CraftingEquipmentManager.stackHasPerk(held, "buried_truth")) {
            boolean enabled = BURIED_TRUTH_DISABLED.remove(playerId);
            if (!enabled) {
                BURIED_TRUTH_DISABLED.add(playerId);
                clearWorldbreakerPreview(player);
            }
            player.sendSystemMessage(Component.translatable(enabled
                    ? "message.simpleskills.transcendent_area.buried_truth_enabled"
                    : "message.simpleskills.transcendent_area.buried_truth_disabled"));
            return;
        }
        if (held.is(ItemTags.HOES) && CraftingEquipmentManager.stackHasPerk(held, "edens_dominion")) {
            boolean enabled = EDEN_DOMINION_DISABLED.remove(playerId);
            if (!enabled) EDEN_DOMINION_DISABLED.add(playerId);
            player.sendSystemMessage(Component.translatable(enabled
                    ? "message.simpleskills.transcendent_area.eden_enabled"
                    : "message.simpleskills.transcendent_area.eden_disabled"));
            return;
        }
        player.sendSystemMessage(Component.translatable(
                "message.simpleskills.transcendent_area.requires_tool"));
    }

    private static void clearWorldbreakerPreview(ServerPlayer player) {
        UUID playerId = player.getUUID();
        boolean wasActive = WORLDBREAKER_PREVIEW_ACTIVE.remove(playerId) != null;
        String previousPreview = WORLDBREAKER_PREVIEW_CACHE.remove(playerId);
        if ((wasActive || (previousPreview != null && !previousPreview.equals("[]")))
                && ServerPlayNetworking.canSend(player, WorldbreakerPreviewPayload.TYPE)) {
            ServerPlayNetworking.send(player, new WorldbreakerPreviewPayload("[]"));
        }
    }

    /**
     * Breaks the surrounding 3x3 for Worldbreaker pickaxes and Buried Truth shovels.
     * Each extra block uses the normal player break pipeline so profession XP, loot,
     * durability and protection checks are processed independently for every block.
     */
    public static void tryWorldbreaker(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState originState) {
        if (WORLD_BREAKER_ACTIVE.get() || player == null || level == null || origin == null || originState == null) return;

        ItemStack held = player.getMainHandItem();
        boolean shovelAbility = CraftingEquipmentManager.stackHasPerk(held, "buried_truth")
                && held.is(ItemTags.SHOVELS) && isBuriedTruthAreaEnabled(player);
        boolean pickaxeAbility = CraftingEquipmentManager.stackHasPerk(held, "worldbreaker")
                && held.is(ItemTags.PICKAXES) && isWorldbreakerAreaEnabled(player);
        if (!shovelAbility && !pickaxeAbility) return;
        if (!canTranscendentAreaBreak(player, level, origin, originState, shovelAbility)) return;

        WORLD_BREAKER_ACTIVE.set(true);
        try {
            for (BlockPos targetPos : getWorldbreakerArea(
                    player, level, origin, player.getLookAngle(), shovelAbility)) {
                if (targetPos.equals(origin)) continue;
                player.gameMode.destroyBlock(targetPos);
            }
        } finally {
            WORLD_BREAKER_ACTIVE.set(false);
        }
    }

    private static boolean isBuriedTruthShovel(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        return held.is(ItemTags.SHOVELS)
                && CraftingEquipmentManager.stackHasPerk(held, "buried_truth");
    }

    /** Refreshes the preview independently from the slower Oracle Vision ore scan. */
    private static void tickWorldbreakerPreview(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ItemStack held = player.getMainHandItem();
        boolean shovelAbility = isBuriedTruthShovel(player) && isBuriedTruthAreaEnabled(player);
        boolean pickaxeAbility = held.is(ItemTags.PICKAXES)
                && CraftingEquipmentManager.stackHasPerk(held, "worldbreaker")
                && isWorldbreakerAreaEnabled(player);
        boolean active = shovelAbility || pickaxeAbility;
        boolean wasActive = WORLDBREAKER_PREVIEW_ACTIVE.getOrDefault(playerId, false);

        if (!active) {
            if (wasActive) {
                WORLDBREAKER_PREVIEW_ACTIVE.remove(playerId);
                String previousPreview = WORLDBREAKER_PREVIEW_CACHE.remove(playerId);
                if (previousPreview != null && !previousPreview.equals("[]")
                        && ServerPlayNetworking.canSend(player, WorldbreakerPreviewPayload.TYPE)) {
                    ServerPlayNetworking.send(player, new WorldbreakerPreviewPayload("[]"));
                }
            }
            return;
        }
        if (player.tickCount % 2 != 0 || !ServerPlayNetworking.canSend(player, WorldbreakerPreviewPayload.TYPE)) return;

        WORLDBREAKER_PREVIEW_ACTIVE.put(playerId, true);
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().normalize().scale(6.0));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));

        JsonArray blocks = new JsonArray();
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos origin = hit.getBlockPos();
            BlockState originState = level.getBlockState(origin);
            if (canTranscendentAreaBreak(player, level, origin, originState, shovelAbility)) {
                for (BlockPos pos : getWorldbreakerArea(
                        player, level, origin, player.getLookAngle(), shovelAbility)) {
                    JsonObject marker = new JsonObject();
                    marker.addProperty("x", pos.getX());
                    marker.addProperty("y", pos.getY());
                    marker.addProperty("z", pos.getZ());
                    blocks.add(marker);
                }
            }
        }
        String encodedPreview = blocks.toString();
        String previousPreview = WORLDBREAKER_PREVIEW_CACHE.put(playerId, encodedPreview);
        if (!encodedPreview.equals(previousPreview)) {
            ServerPlayNetworking.send(player, new WorldbreakerPreviewPayload(encodedPreview));
        }
    }

    /** Returns the valid blocks in the same face-aligned 3x3 plane used by the break action. */
    private static List<BlockPos> getWorldbreakerArea(ServerPlayer player, ServerLevel level,
                                                       BlockPos origin, Vec3 look, boolean shovelAbility) {
        List<BlockPos> positions = new ArrayList<>(9);
        double absX = Math.abs(look.x);
        double absY = Math.abs(look.y);
        double absZ = Math.abs(look.z);
        int normalAxis = absY >= absX && absY >= absZ ? 1 : (absX >= absZ ? 0 : 2);

        for (int first = -1; first <= 1; first++) {
            for (int second = -1; second <= 1; second++) {
                BlockPos pos = switch (normalAxis) {
                    case 0 -> origin.offset(0, first, second); // X-facing wall: Y/Z
                    case 1 -> origin.offset(first, 0, second); // Floor/ceiling: X/Z
                    default -> origin.offset(first, second, 0); // Z-facing wall: X/Y
                };
                if (!level.hasChunkAt(pos)) continue;
                if (canTranscendentAreaBreak(player, level, pos, level.getBlockState(pos), shovelAbility)) {
                    positions.add(pos.immutable());
                }
            }
        }
        return positions;
    }

    private static boolean canTranscendentAreaBreak(ServerPlayer player, ServerLevel level,
                                                       BlockPos pos, BlockState state, boolean shovelAbility) {
        ItemStack held = player.getMainHandItem();
        boolean correctToolType = shovelAbility
                ? held.is(ItemTags.SHOVELS) && state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                : held.is(ItemTags.PICKAXES);
        return level.hasChunkAt(pos)
                && !state.isAir()
                && !isOreBlock(state)
                && correctToolType
                && state.getDestroySpeed(level, pos) >= 0.0F
                && player.hasCorrectToolForDrops(state)
                && player.isWithinBlockInteractionRange(pos, 1.0)
                && level.mayInteract(player, pos)
                && !level.getServer().isUnderSpawnProtection(level, pos, player);
    }

    private static void tickVision(ServerPlayer player, long now) {
        if (player.tickCount % VISION_REFRESH_TICKS != 0 || !ServerPlayNetworking.canSend(player, TranscendentVisionPayload.TYPE)) return;
        boolean oracle = CraftingEquipmentManager.hasEquippedPerkInSlot(
                player, EquipmentSlot.HEAD, "oracle_vision");
        boolean breaker = CraftingEquipmentManager.stackHasPerk(player.getMainHandItem(), "worldbreaker");
        if (!oracle && !breaker) {
            if (VISION_WAS_ACTIVE.remove(player.getUUID()) != null) {
                ServerPlayNetworking.send(player, new TranscendentVisionPayload("{\"oracle\":false,\"breaker\":false,\"entities\":[],\"ores\":[]}"));
            }
            return;
        }
        VISION_WAS_ACTIVE.put(player.getUUID(), true);

        ServerLevel level = player.level();
        JsonObject json = new JsonObject();
        json.addProperty("oracle", oracle);
        json.addProperty("breaker", breaker);
        JsonArray entities = new JsonArray();
        if (oracle) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(24.0),
                    candidate -> candidate != player && candidate.isAlive() && isHostile(candidate, player))) {
                entities.add(entity.getUUID().toString());
                if (entities.size() >= MAX_HOSTILE_MARKERS) break;
            }
        }
        JsonArray ores = new JsonArray();
        int radius = oracle ? VISION_ORE_RADIUS : WORLDBREAKER_ORE_RADIUS;
        BlockPos center = player.blockPosition();
        int radiusSq = radius * radius;
        outer:
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSq) continue;
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    String kind = oreKind(state);
                    if (kind == null) continue;
                    JsonObject marker = new JsonObject();
                    marker.addProperty("kind", kind);
                    marker.addProperty("x", pos.getX());
                    marker.addProperty("y", pos.getY());
                    marker.addProperty("z", pos.getZ());
                    marker.addProperty("distance", (int) Math.round(Vec3.atCenterOf(pos).distanceTo(player.position())));
                    ores.add(marker);
                    if (ores.size() >= MAX_ORE_MARKERS) break outer;
                }
            }
        }
        json.add("entities", entities);
        json.add("ores", ores);
        ServerPlayNetworking.send(player, new TranscendentVisionPayload(json.toString()));
    }

    private static String oreKind(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.equals("diamond_ore") || path.equals("deepslate_diamond_ore")) return "diamond";
        if (path.equals("ancient_debris")) return "ancient_debris";
        if (path.equals("rune_ore") || path.equals("deepslate_rune_ore") || id.endsWith(":rune_ore")) return "rune";
        return null;
    }

    private static boolean isOreBlock(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        // Match Vein Mining's ore tags as well as the explicit Transcendent ores
        // and conventional *_ore identifiers. This gives Vein Mining priority for
        // vanilla and modded ores, preventing Worldbreaker from overlapping it.
        return oreKind(state) != null || VeinMining.isOre(state) || path.endsWith("_ore");
    }

    private static LivingEntity findTarget(ServerPlayer shooter, double maxDistance) {
        Vec3 eye = shooter.getEyePosition();
        Vec3 look = shooter.getLookAngle().normalize();
        return shooter.level().getEntitiesOfClass(LivingEntity.class,
                        shooter.getBoundingBox().inflate(maxDistance),
                        entity -> entity != shooter && !(entity instanceof Player) && entity.isAlive()
                                && isHostile(entity, shooter))
                .stream()
                .filter(target -> target.distanceToSqr(shooter) <= maxDistance * maxDistance)
                .filter(target -> {
                    Vec3 toward = target.getBoundingBox().getCenter().subtract(eye).normalize();
                    return look.dot(toward) >= 0.93 && shooter.hasLineOfSight(target);
                })
                .min(java.util.Comparator.comparingDouble(target -> target.distanceToSqr(shooter)))
                .orElse(null);
    }

    private static boolean isHostile(LivingEntity entity, Entity against) {
        if (entity instanceof Player) return false;
        if (entity instanceof Enemy) return true;
        if (entity instanceof Mob mob && mob.getTarget() != null && mob.getTarget() == against) return true;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return id.equals("ender_dragon") || id.equals("wither") || id.equals("warden");
    }

    private static boolean isBow(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.equals("bow") || id.endsWith("_bow");
    }

    private static boolean isCrossbow(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.equals("crossbow") || id.endsWith("_crossbow");
    }

    private static boolean contains(String encoded, String perk) {
        for (String entry : encoded.split(",")) if (entry.startsWith(perk + ":")) return true;
        return false;
    }

    private static void addEffect(LivingEntity target, String id, int amplifier, int duration) {
        BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(id)).ifPresent(holder ->
                target.addEffect(new MobEffectInstance(holder, duration, amplifier, false, true)));
    }

    public static void clearPlayer(UUID playerId) {
        WORLDBREAKER_DISABLED.remove(playerId);
        BURIED_TRUTH_DISABLED.remove(playerId);
        EDEN_DOMINION_DISABLED.remove(playerId);
        HOMING_ARROWS.entrySet().removeIf(e -> e.getValue().owner().equals(playerId));
        SINGULARITY_PROJECTILES.entrySet().removeIf(e -> e.getValue().owner().equals(playerId));
        SINGULARITIES.removeIf(field -> field.owner.equals(playerId));
        VISION_WAS_ACTIVE.remove(playerId);
        WORLDBREAKER_PREVIEW_ACTIVE.remove(playerId);
        WORLDBREAKER_PREVIEW_CACHE.remove(playerId);
        DIVINE_REFLECTION_COOLDOWNS.remove(playerId);
        POSEIDON_COOLDOWNS.remove(playerId);
    }
}
