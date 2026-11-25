package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.prayer.PrayerAbility;
import com.github.ob_yekt.simpleskills.prayer.PrayerScaling;
import com.github.ob_yekt.simpleskills.prayer.PrayerVisualEffects;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxEntity;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import com.github.ob_yekt.simpleskills.crafting.CraftingRarity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class AbyssalFallenKingEntity extends Monster {
    public static final EntityType<AbyssalFallenKingEntity> TYPE = registerType();

    private static EntityType<AbyssalFallenKingEntity> registerType() {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyssal_fallen_king");
        EntityType<AbyssalFallenKingEntity> type = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id,
                EntityType.Builder.<AbyssalFallenKingEntity>of(AbyssalFallenKingEntity::new, MobCategory.MONSTER)
                        .sized(1.15F, 3.8F)
                        .clientTrackingRange(16)
                        .updateInterval(2)
                        .noLootTable()
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
        );
        FabricDefaultAttributeRegistry.register(type, createAttributes());
        return type;
    }

    public static void register() {
        // Forces entity type and attributes to register before the registries freeze.
    }

    // 0 = SITTING_IDLE, 1 = AWAKENING, 2 = ACTIVE
    private static final EntityDataAccessor<Integer> DATA_KING_STATE =
            SynchedEntityData.defineId(AbyssalFallenKingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TICK =
            SynchedEntityData.defineId(AbyssalFallenKingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DEATH_TICKS =
            SynchedEntityData.defineId(AbyssalFallenKingEntity.class, EntityDataSerializers.INT);

    public static final int STATE_SITTING = 0;
    public static final int STATE_AWAKENING = 1;
    public static final int STATE_ACTIVE = 2;

    public static final int AWAKENING_DURATION_TICKS = 48; // 2.4s @ 20 TPS
    public static final int ATTACK_DURATION_TICKS = 20;    // 1.0s @ 20 TPS

    // Cinematic 5-phase death timeline (4.5s @ 20 TPS)
    public static final int DEATH_DURATION_TICKS = 90;
    public static final int DEATH_SWORD_IMPACT_TICK = 45;  // 2.25s: sword driven into floor
    public static final int DEATH_CROWN_IMPACT_TICK = 61;  // 3.05s: crown hits ground beside sword
    public static final int DEATH_CRUMBLE_START_TICK = 63; // 3.15s: body begins crumbling

    // World-space anchor offsets (in blocks, matching 1.18x model scale) for future item integration
    public static final double PLANTED_SWORD_FORWARD_OFFSET = 0.85D;
    public static final double FALLEN_CROWN_FORWARD_OFFSET = 0.72D;
    public static final double FALLEN_CROWN_RIGHT_OFFSET = 0.46D;

    private static final double AWAKEN_TRIGGER_RANGE = 10.0D;
    private static final double RANGED_SPELL_MIN_RANGE = 8.0D;
    private static final double RANGED_SPELL_MAX_RANGE = 36.0D;
    // Return to the throne only after 20 full seconds without a valid player in combat range.
    private static final int NO_PLAYERS_RESET_DELAY_TICKS = 20 * 20;
    private static final int BOSS_PRAYER_LEVEL = 35;
    private static final PrayerAbility[] RANGED_SPELL_ROTATION = {
            PrayerAbility.FIREBALL,
            PrayerAbility.SPARK,
            PrayerAbility.SHADOW_BOLT,
            PrayerAbility.ICE_SHARD
    };

    public final AnimationState sittingIdleAnimationState = new AnimationState();
    public final AnimationState awakeningAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.translatable("entity.simpleskills.abyssal_fallen_king"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    private int awakeningTicks = 0;
    private int lastClientAttackTick = -1;
    private int rangedSpellCooldownTicks = 0;
    private int nextRangedSpellIndex = 0;
    private int noPlayersNearbyTicks = 0;
    private boolean deathSequenceStarted = false;
    private boolean kingRelicsDropped = false;

    // Immutable return point captured at the throne before the first awakening.
    private boolean throneAnchorCaptured = false;
    private double throneX;
    private double throneY;
    private double throneZ;
    private float throneYaw;
    private float thronePitch;

    public AbyssalFallenKingEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 250;
        this.bossEvent.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.4D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KING_STATE, STATE_SITTING);
        builder.define(DATA_ATTACK_TICK, 0);
        builder.define(DATA_DEATH_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ThroneSlumberGoal(this));
        this.goalSelector.addGoal(2, new KingMeleeAttackGoal(this, 1.15D, true));
        // While the target is beyond melee range, keep pursuing it with pathfinding
        // instead of standing still to cast ranged spells.
        this.goalSelector.addGoal(3, new KingRangedSpellPursuitGoal(this, 1.05D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int getKingState() {
        return this.entityData.get(DATA_KING_STATE);
    }

    public void setKingState(int state) {
        this.entityData.set(DATA_KING_STATE, state);
        if (!this.level().isClientSide()) {
            this.bossEvent.setVisible(state != STATE_SITTING && !this.isDeadOrDying());
        }
    }

    public boolean isSittingOnThrone() {
        return getKingState() == STATE_SITTING;
    }

    public boolean isAwakening() {
        return getKingState() == STATE_AWAKENING;
    }

    public boolean isActiveKing() {
        return getKingState() == STATE_ACTIVE && !this.isDeadOrDying();
    }

    public int getDeathTicks() {
        return this.entityData.get(DATA_DEATH_TICKS);
    }

    public float getDeathProgress(float partialTick) {
        int ticks = getDeathTicks();
        if (ticks <= 0 && !this.isDeadOrDying()) {
            return 0.0F;
        }
        float effective = Math.max(0.0F, (ticks > 0 ? ticks - 1.0F : 0.0F) + partialTick);
        return Mth.clamp(effective / (float) DEATH_DURATION_TICKS, 0.0F, 1.0F);
    }

    /** World position where the King's sword is planted vertically into the ground. */
    public Vec3 getPlantedSwordAnchor() {
        float yawRad = this.yBodyRot * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        return new Vec3(
                this.getX() + forwardX * PLANTED_SWORD_FORWARD_OFFSET,
                this.getY(),
                this.getZ() + forwardZ * PLANTED_SWORD_FORWARD_OFFSET
        );
    }

    /** World position where the King's crown comes to rest on the ground beside the sword. */
    public Vec3 getFallenCrownAnchor() {
        float yawRad = this.yBodyRot * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        double rightX = -forwardZ;
        double rightZ = forwardX;
        return new Vec3(
                this.getX() + forwardX * FALLEN_CROWN_FORWARD_OFFSET + rightX * FALLEN_CROWN_RIGHT_OFFSET,
                this.getY() + 0.05D,
                this.getZ() + forwardZ * FALLEN_CROWN_FORWARD_OFFSET + rightZ * FALLEN_CROWN_RIGHT_OFFSET
        );
    }

    public void triggerAwakening() {
        if (!this.isDeadOrDying() && getKingState() == STATE_SITTING) {
            this.awakeningTicks = 0;
            this.noPlayersNearbyTicks = 0;
            setKingState(STATE_AWAKENING);
            this.playSound(SoundEvents.WITHER_SPAWN, 1.3F, 0.72F);
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        if (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.COMMAND) {
            setKingState(STATE_ACTIVE);
        } else {
            setKingState(STATE_SITTING);
        }
        return data;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            updateClientAnimations();
        } else if (!this.isDeadOrDying()) {
            updateServerState();
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (this.deathSequenceStarted) {
            return;
        }
        this.deathSequenceStarted = true;
        super.die(damageSource);

        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
        if (!this.level().isClientSide()) {
            this.bossEvent.setProgress(0.0F);
            this.bossEvent.setVisible(false);
            this.entityData.set(DATA_DEATH_TICKS, 1);
        }
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);

        if (this.level() instanceof ServerLevel serverLevel) {
            this.entityData.set(DATA_DEATH_TICKS, this.deathTime);
            emitServerDeathEffects(serverLevel, this.deathTime);

            if (this.deathTime >= DEATH_DURATION_TICKS && !this.isRemoved()) {
                if (!this.kingRelicsDropped) {
                    // Set the guard before spawning either entity: reloads cannot duplicate the rewards.
                    this.kingRelicsDropped = true;
                    spawnKingRelics(serverLevel);
                }
                serverLevel.sendParticles(ParticleTypes.ASH,
                        this.getX(), this.getY() + 0.15D, this.getZ(),
                        24, 0.48D, 0.08D, 0.48D, 0.01D);
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        this.getX(), this.getY() + 0.12D, this.getZ(),
                        12, 0.42D, 0.06D, 0.42D, 0.01D);
                this.remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    private void spawnKingRelics(ServerLevel serverLevel) {
        // Each reward rolls independently: 5% Transcendent, otherwise Mythic.
        CraftingRarity swordRarity = this.getRandom().nextFloat() < 0.05F
                ? CraftingRarity.TRANSCENDENT : CraftingRarity.MYTHIC;
        CraftingRarity crownRarity = this.getRandom().nextFloat() < 0.05F
                ? CraftingRarity.TRANSCENDENT : CraftingRarity.MYTHIC;

        spawnKingRelic(serverLevel, AbyssalKingItems.ABYSSAL_KING_SWORD,
                getPlantedSwordAnchor(), swordRarity);
        spawnKingRelic(serverLevel, AbyssalKingItems.ABYSSAL_KING_CROWN,
                getFallenCrownAnchor(), crownRarity);
    }

    private void spawnKingRelic(ServerLevel serverLevel, Item item, Vec3 anchor, CraftingRarity rarity) {
        if (item == null) return;
        ItemStack stack = new ItemStack(item);
        CraftingEquipmentManager.markAbyssalLootRarity(stack, rarity);

        // Standard vanilla item drop: gravity, normal proximity pickup and no despawn.
        // The rarity marker is materialized by the active ItemEntityMixin before pickup.
        ItemEntity relic = new ItemEntity(serverLevel, anchor.x, anchor.y, anchor.z, stack);
        relic.setUnlimitedLifetime();
        relic.setYRot(this.getYRot());
        serverLevel.addFreshEntity(relic);
    }

    private void emitServerDeathEffects(ServerLevel serverLevel, int tick) {
        // Phase 1 (ticks 1..18): Final resistance — purple aura starts dissipating
        if (tick <= 18 && tick % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + 1.55D, this.getZ(),
                    5, 0.45D, 0.55D, 0.45D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1.65D, this.getZ(),
                    3, 0.40D, 0.45D, 0.40D, 0.015D);
        }

        // Phase 2 (ticks 19..37): Heavy armored kneel onto the floor
        if (tick == 35) {
            this.playSound(SoundEvents.WITHER_SKELETON_HURT, 0.85F, 0.52F);
            serverLevel.sendParticles(ParticleTypes.ASH,
                    this.getX(), this.getY() + 0.12D, this.getZ(),
                    14, 0.40D, 0.06D, 0.40D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.12D, this.getZ(),
                    8, 0.35D, 0.05D, 0.35D, 0.015D);
        }

        // Phase 3 (tick 45): Sword driven vertically into the ground
        if (tick == DEATH_SWORD_IMPACT_TICK) {
            Vec3 swordPos = getPlantedSwordAnchor();
            this.playSound(SoundEvents.ANVIL_LAND, 0.65F, 0.62F);
            this.playSound(SoundEvents.STONE_BREAK, 0.90F, 0.74F);

            serverLevel.sendParticles(ParticleTypes.ASH,
                    swordPos.x, swordPos.y + 0.12D, swordPos.z,
                    22, 0.32D, 0.14D, 0.32D, 0.045D);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    swordPos.x, swordPos.y + 0.10D, swordPos.z,
                    14, 0.28D, 0.08D, 0.28D, 0.03D);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    swordPos.x, swordPos.y + 0.20D, swordPos.z,
                    20, 0.35D, 0.18D, 0.35D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    swordPos.x, swordPos.y + 0.16D, swordPos.z,
                    14, 0.34D, 0.12D, 0.34D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    swordPos.x, swordPos.y + 0.24D, swordPos.z,
                    10, 0.24D, 0.22D, 0.24D, 0.03D);
        } else if (tick > DEATH_SWORD_IMPACT_TICK && tick % 4 == 0) {
            Vec3 swordPos = getPlantedSwordAnchor();
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    swordPos.x, swordPos.y + 0.45D, swordPos.z,
                    2, 0.12D, 0.30D, 0.12D, 0.01D);
        }

        // Phase 4 (tick 61): Crown hits the floor beside the planted sword
        if (tick == DEATH_CROWN_IMPACT_TICK) {
            Vec3 crownPos = getFallenCrownAnchor();
            this.playSound(SoundEvents.ANVIL_LAND, 0.25F, 1.55F);
            serverLevel.sendParticles(ParticleTypes.ASH,
                    crownPos.x, crownPos.y + 0.05D, crownPos.z,
                    8, 0.16D, 0.03D, 0.16D, 0.015D);
        }

        // Phase 5 (ticks 63..90): Progressive armor & body crumbling down to the floor
        if (tick >= DEATH_CRUMBLE_START_TICK) {
            float crumbleT = Mth.clamp(
                    (tick - DEATH_CRUMBLE_START_TICK) / (float) (DEATH_DURATION_TICKS - DEATH_CRUMBLE_START_TICK),
                    0.0F, 1.0F
            );
            double bodyTopY = Mth.lerp(crumbleT, 1.85D, 0.20D);
            int ashCount = crumbleT < 0.85F ? 10 : 5;

            serverLevel.sendParticles(ParticleTypes.ASH,
                    this.getX(), this.getY() + bodyTopY * 0.55D, this.getZ(),
                    ashCount, 0.42D, Math.max(0.08D, bodyTopY * 0.45D), 0.42D, 0.012D);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + bodyTopY * 0.45D, this.getZ(),
                    5, 0.36D, Math.max(0.06D, bodyTopY * 0.35D), 0.36D, 0.01D);

            if (tick % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + bodyTopY * 0.50D, this.getZ(),
                        3, 0.34D, Math.max(0.06D, bodyTopY * 0.35D), 0.34D, 0.008D);
                serverLevel.sendParticles(ParticleTypes.WITCH,
                        this.getX(), this.getY() + 0.18D, this.getZ(),
                        3, 0.38D, 0.10D, 0.38D, 0.01D);
            }
        }
    }

    @Override
    public boolean isPushable() {
        return !this.isDeadOrDying() && super.isPushable();
    }

    private void updateServerState() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int state = getKingState();

        if (!this.throneAnchorCaptured) {
            captureThroneAnchor();
        }

        if (state == STATE_SITTING) {
            this.noPlayersNearbyTicks = 0;
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            Player nearest = findNearestValidPlayer(serverLevel, AWAKEN_TRIGGER_RANGE);
            if (nearest != null && this.hasLineOfSight(nearest)) {
                this.setTarget(nearest);
                triggerAwakening();
            }
        } else if (state == STATE_AWAKENING) {
            this.noPlayersNearbyTicks = 0;
            this.awakeningTicks++;
            this.getNavigation().stop();
            if (this.awakeningTicks % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + 1.2D, this.getZ(),
                        6, 0.6D, 0.8D, 0.6D, 0.03D);
            }
            if (this.awakeningTicks >= AWAKENING_DURATION_TICKS) {
                setKingState(STATE_ACTIVE);
                this.noPlayersNearbyTicks = 0;
            }
        } else if (state == STATE_ACTIVE) {
            // Always retarget the nearest valid player, rather than remaining focused on
            // a player who has died, disconnected, entered spectator mode, or moved away.
            boolean hasNearbyPlayer = updateNearestPlayerTarget(serverLevel);
            if (this.getKingState() == STATE_ACTIVE && hasNearbyPlayer) {
                updateRangedSpellAttack(serverLevel);
            }
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /** Finds the nearest living, non-creative, non-spectator player within the requested radius. */
    @Nullable
    private Player findNearestValidPlayer(ServerLevel serverLevel, double range) {
        double rangeSqr = range * range;
        Player nearest = null;
        double nearestDistanceSqr = rangeSqr;
        AABB searchBox = this.getBoundingBox().inflate(range);

        for (Player candidate : serverLevel.getEntitiesOfClass(Player.class, searchBox,
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
            double distanceSqr = this.distanceToSqr(candidate);
            if (distanceSqr <= rangeSqr && distanceSqr < nearestDistanceSqr) {
                nearest = candidate;
                nearestDistanceSqr = distanceSqr;
            }
        }
        return nearest;
    }

    /**
     * Keeps the King focused on the closest valid player within his 36-block combat range.
     * If nobody remains nearby, he waits in place for 20 seconds before returning to throne stasis.
     */
    private boolean updateNearestPlayerTarget(ServerLevel serverLevel) {
        Player nearest = findNearestValidPlayer(serverLevel, RANGED_SPELL_MAX_RANGE);
        if (nearest != null) {
            this.noPlayersNearbyTicks = 0;
            if (this.getTarget() != nearest) {
                this.setTarget(nearest);
            }
            return true;
        }

        this.setTarget(null);
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.noPlayersNearbyTicks = Math.min(
                NO_PLAYERS_RESET_DELAY_TICKS,
                this.noPlayersNearbyTicks + 1
        );

        if (this.noPlayersNearbyTicks >= NO_PLAYERS_RESET_DELAY_TICKS) {
            returnToThroneStasis();
        }
        return false;
    }

    private void captureThroneAnchor() {
        this.throneX = this.getX();
        this.throneY = this.getY();
        this.throneZ = this.getZ();
        this.throneYaw = this.getYRot();
        this.thronePitch = this.getXRot();
        this.throneAnchorCaptured = true;
    }

    private void updateRangedSpellAttack(ServerLevel serverLevel) {
        if (this.rangedSpellCooldownTicks > 0) {
            this.rangedSpellCooldownTicks--;
        }

        LivingEntity currentTarget = this.getTarget();
        if (!(currentTarget instanceof Player target) || !target.isAlive()
                || target.isCreative() || target.isSpectator()) {
            return;
        }

        double distanceSqr = this.distanceToSqr(target);
        if (distanceSqr <= RANGED_SPELL_MIN_RANGE * RANGED_SPELL_MIN_RANGE
                || distanceSqr > RANGED_SPELL_MAX_RANGE * RANGED_SPELL_MAX_RANGE
                || !this.hasLineOfSight(target)) {
            return;
        }

        // Movement is handled by KingRangedSpellPursuitGoal. Do not stop navigation here:
        // the King should keep closing the distance while his spell cooldown runs.
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.rangedSpellCooldownTicks <= 0) {
            int spellIndex = Math.floorMod(this.nextRangedSpellIndex, RANGED_SPELL_ROTATION.length);
            PrayerAbility ability = RANGED_SPELL_ROTATION[spellIndex];
            this.nextRangedSpellIndex = (spellIndex + 1) % RANGED_SPELL_ROTATION.length;
            castRangedSpell(serverLevel, target, ability);
            this.rangedSpellCooldownTicks = PrayerScaling.cooldownTicks(ability, BOSS_PRAYER_LEVEL);
        }
    }

    private void castRangedSpell(ServerLevel serverLevel, Player target, PrayerAbility ability) {
        Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 look = targetPoint.subtract(this.getEyePosition()).normalize();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() < 1.0E-5D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        Vec3 hand = this.position()
                .add(0.0D, this.getBbHeight() * 0.68D, 0.0D)
                .add(side.scale(0.34D))
                .add(look.scale(0.28D));

        PrayerVfxEntity visual = PrayerVisualEffects.play(
                serverLevel, hand, targetPoint, ability, BOSS_PRAYER_LEVEL, this);
        visual.setKingSpellPayload(this.getUUID(), target.getUUID(), targetPoint,
                ability, BOSS_PRAYER_LEVEL);
    }

    /** Returns the King to his original throne after the arena has been empty for 20 seconds. */
    public void returnToThroneStasis() {
        if (this.level().isClientSide() || !this.isAlive()) {
            return;
        }

        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(Vec3.ZERO);
        this.rangedSpellCooldownTicks = 0;
        this.nextRangedSpellIndex = 0;
        this.awakeningTicks = 0;
        if (this.throneAnchorCaptured) {
            this.snapTo(this.throneX, this.throneY, this.throneZ, this.throneYaw, this.thronePitch);
        }

        this.setHealth(this.getMaxHealth());
        this.setKingState(STATE_SITTING);
    }

    /** Applies one of the existing Prayer abilities to player targets when cast by this boss. */
    public static void executeBossSpellImpact(ServerLevel serverLevel, UUID casterUuid, UUID targetUuid,
                                               Vec3 impactPos, PrayerAbility ability, int prayerLevel,
                                               float hitShare) {
        if (casterUuid == null) {
            return;
        }
        Entity casterEntity = serverLevel.getEntity(casterUuid);
        if (!(casterEntity instanceof AbyssalFallenKingEntity king)
                || !king.isAlive() || king.getKingState() != STATE_ACTIVE
                || targetUuid == null || impactPos == null || ability == null) {
            return;
        }

        if (ability == PrayerAbility.SHADOW_BOLT) {
            AABB blastBox = AABB.ofSize(impactPos, 4.2D, 3.5D, 4.2D);
            for (Player victim : serverLevel.getEntitiesOfClass(Player.class, blastBox,
                    player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
                applyBossSpellDamage(serverLevel, king, victim, ability, prayerLevel, hitShare);
                if (!king.isActiveKing()) {
                    break;
                }
            }
            return;
        }

        Entity targetEntity = serverLevel.getEntity(targetUuid);
        if (targetEntity instanceof Player target && target.isAlive()
                && !target.isCreative() && !target.isSpectator()
                && target.position().distanceToSqr(impactPos) <= 25.0D) {
            applyBossSpellDamage(serverLevel, king, target, ability, prayerLevel, hitShare);
        }
    }

    private static void applyBossSpellDamage(ServerLevel serverLevel, AbyssalFallenKingEntity king,
                                             Player victim, PrayerAbility ability, int prayerLevel,
                                             float hitShare) {
        float scaling = PrayerScaling.damageScale(prayerLevel);
        float damage = switch (ability) {
            // Match the existing Prayer single-target damage formula for elemental projectiles.
            case FIREBALL, SPARK, ICE_SHARD -> (4.0F + prayerLevel * 0.12F) * scaling * hitShare;
            case SHADOW_BOLT -> (4.0F + prayerLevel * 0.13F) * scaling * hitShare;
            default -> 0.0F;
        };
        if (damage <= 0.0F) {
            return;
        }

        boolean hit = victim.hurtServer(serverLevel,
                victim.damageSources().indirectMagic(king, king), damage);
        if (!hit) {
            return;
        }

        switch (ability) {
            case FIREBALL -> {
                victim.igniteForSeconds(3.0F);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                        12, 0.35D, 0.35D, 0.35D, 0.04D);
            }
            case SPARK -> serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                    16, 0.35D, 0.45D, 0.35D, 0.08D);
            case ICE_SHARD -> {
                // Same debuff as the player's Prayer Ice Shard: 50 ticks of Slowness I.
                BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("minecraft:slowness"))
                        .ifPresent(effect -> victim.addEffect(new MobEffectInstance(effect, 50, 1, false, true)));
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                        18, 0.35D, 0.45D, 0.35D, 0.035D);
            }
            case SHADOW_BOLT -> {
                BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("minecraft:slowness"))
                        .ifPresent(effect -> victim.addEffect(new MobEffectInstance(effect, 40, 1, false, true)));
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                        18, 0.45D, 0.5D, 0.45D, 0.06D);
            }
            default -> { }
        }

    }

    private void updateClientAnimations() {
        if (this.isDeadOrDying()) {
            this.sittingIdleAnimationState.stop();
            this.awakeningAnimationState.stop();
            this.idleAnimationState.stop();
            this.walkAnimationState.stop();
            this.attackAnimationState.stop();

            if (!this.deathAnimationState.isStarted()) {
                int serverDeathTicks = this.entityData.get(DATA_DEATH_TICKS);
                int startTick = serverDeathTicks > 1
                        ? Math.max(0, this.tickCount - serverDeathTicks)
                        : this.tickCount;
                this.deathAnimationState.start(startTick);
            }
            return;
        }

        int state = getKingState();
        if (state == STATE_SITTING) {
            this.awakeningAnimationState.stop();
            this.idleAnimationState.stop();
            this.walkAnimationState.stop();
            this.sittingIdleAnimationState.startIfStopped(this.tickCount);
            return;
        }

        if (state == STATE_AWAKENING) {
            this.sittingIdleAnimationState.stop();
            this.idleAnimationState.stop();
            this.walkAnimationState.stop();
            this.awakeningAnimationState.startIfStopped(this.tickCount);
            return;
        }

        // ACTIVE state
        this.sittingIdleAnimationState.stop();
        this.awakeningAnimationState.stop();

        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        if (moving) {
            this.idleAnimationState.stop();
            this.walkAnimationState.startIfStopped(this.tickCount);
        } else {
            this.walkAnimationState.stop();
            this.idleAnimationState.startIfStopped(this.tickCount);
        }

        int attackTick = this.entityData.get(DATA_ATTACK_TICK);
        if (attackTick > 0 && attackTick != this.lastClientAttackTick) {
            this.lastClientAttackTick = attackTick;
            this.attackAnimationState.start(this.tickCount);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (this.isDeadOrDying()) {
            return false;
        }
        this.entityData.set(DATA_ATTACK_TICK, this.tickCount);
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt) {
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.1F, 0.75F);
            level.sendParticles(ParticleTypes.WITCH,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    10, 0.35D, 0.35D, 0.35D, 0.05D);
        }
        return hurt;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isDeadOrDying()) {
            return false;
        }
        if (getKingState() == STATE_SITTING) {
            triggerAwakening();
            return super.hurtServer(level, source, amount * 0.25F);
        }
        if (getKingState() == STATE_AWAKENING) {
            return super.hurtServer(level, source, amount * 0.35F);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isDeadOrDying()) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("KingState", getKingState());
        output.putInt("AwakeningTicks", this.awakeningTicks);
        output.putBoolean("ThroneAnchorCaptured", this.throneAnchorCaptured);
        if (this.throneAnchorCaptured) {
            output.putDouble("ThroneX", this.throneX);
            output.putDouble("ThroneY", this.throneY);
            output.putDouble("ThroneZ", this.throneZ);
            output.putFloat("ThroneYaw", this.throneYaw);
            output.putFloat("ThronePitch", this.thronePitch);
        }
        output.putInt("NextRangedSpell", this.nextRangedSpellIndex);
        output.putInt("NoPlayersNearbyTicks", this.noPlayersNearbyTicks);
        output.putInt("DeathTicks", this.deathTime);
        output.putBoolean("KingRelicsDropped", this.kingRelicsDropped);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int savedState = input.getIntOr("KingState", STATE_SITTING);
        setKingState(Math.max(STATE_SITTING, Math.min(STATE_ACTIVE, savedState)));
        this.awakeningTicks = Math.max(0, Math.min(AWAKENING_DURATION_TICKS,
                input.getIntOr("AwakeningTicks", 0)));
        this.throneAnchorCaptured = input.getBooleanOr("ThroneAnchorCaptured", false);
        if (this.throneAnchorCaptured) {
            this.throneX = input.getDoubleOr("ThroneX", this.getX());
            this.throneY = input.getDoubleOr("ThroneY", this.getY());
            this.throneZ = input.getDoubleOr("ThroneZ", this.getZ());
            this.throneYaw = input.getFloatOr("ThroneYaw", this.getYRot());
            this.thronePitch = input.getFloatOr("ThronePitch", this.getXRot());
        }
        this.nextRangedSpellIndex = Math.floorMod(
                input.getIntOr("NextRangedSpell", 0), RANGED_SPELL_ROTATION.length);
        this.noPlayersNearbyTicks = Math.max(0, Math.min(
                NO_PLAYERS_RESET_DELAY_TICKS, input.getIntOr("NoPlayersNearbyTicks", 0)));

        this.kingRelicsDropped = input.getBooleanOr("KingRelicsDropped", false);
        int savedDeathTicks = Math.max(0, Math.min(DEATH_DURATION_TICKS, input.getIntOr("DeathTicks", 0)));
        if (savedDeathTicks > 0 || this.isDeadOrDying()) {
            this.deathTime = Math.max(1, savedDeathTicks);
            this.deathSequenceStarted = true;
            this.entityData.set(DATA_DEATH_TICKS, this.deathTime);
            this.bossEvent.setVisible(false);
        }

        // Older builds forced NoAI and NoGravity on the throne entity during spawn.
        // Clear those persisted flags: the slumber goal already immobilizes the seated king,
        // while normal gravity lets him settle onto the throne cushion.
        this.setNoAi(false);
        this.setNoGravity(false);

        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    private static class ThroneSlumberGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final AbyssalFallenKingEntity king;

        ThroneSlumberGoal(AbyssalFallenKingEntity king) {
            this.king = king;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !this.king.isDeadOrDying() && !this.king.isActiveKing();
        }

        @Override
        public void tick() {
            this.king.getNavigation().stop();
        }
    }

    /**
     * Reuses MeleeAttackGoal's native path-following and target tracking while the King
     * remains outside melee reach. Spell casting itself is handled independently by
     * updateRangedSpellAttack(), allowing movement and ranged attacks to happen together.
     */
    private static class KingRangedSpellPursuitGoal extends MeleeAttackGoal {
        private final AbyssalFallenKingEntity king;

        KingRangedSpellPursuitGoal(AbyssalFallenKingEntity king, double speedModifier) {
            super(king, speedModifier, true);
            this.king = king;
        }

        private boolean hasRangedTarget() {
            if (this.king.isDeadOrDying()) {
                return false;
            }
            LivingEntity target = this.king.getTarget();
            if (!(target instanceof Player player) || !player.isAlive()
                    || player.isCreative() || player.isSpectator()) {
                return false;
            }

            double distanceSqr = this.king.distanceToSqr(player);
            return this.king.isActiveKing()
                    && distanceSqr > RANGED_SPELL_MIN_RANGE * RANGED_SPELL_MIN_RANGE
                    && distanceSqr <= RANGED_SPELL_MAX_RANGE * RANGED_SPELL_MAX_RANGE;
        }

        @Override
        public boolean canUse() {
            return this.hasRangedTarget() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.hasRangedTarget() && super.canContinueToUse();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            // No melee strike from this goal; the separate melee goal takes over at 8 blocks.
        }
    }

    private static class KingMeleeAttackGoal extends MeleeAttackGoal {
        private final AbyssalFallenKingEntity king;

        KingMeleeAttackGoal(AbyssalFallenKingEntity king, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(king, speedModifier, followingTargetEvenIfNotSeen);
            this.king = king;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.king.getTarget();
            return this.king.isActiveKing() && target != null
                    && this.king.distanceToSqr(target) <= RANGED_SPELL_MIN_RANGE * RANGED_SPELL_MIN_RANGE
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.king.getTarget();
            return this.king.isActiveKing() && target != null
                    && this.king.distanceToSqr(target) <= RANGED_SPELL_MIN_RANGE * RANGED_SPELL_MIN_RANGE
                    && super.canContinueToUse();
        }

        @Override
        public void stop() {
            super.stop();
            this.king.getNavigation().stop();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.king.doHurtTarget(getServerLevel(this.king), target);
            }
        }
    }
}
