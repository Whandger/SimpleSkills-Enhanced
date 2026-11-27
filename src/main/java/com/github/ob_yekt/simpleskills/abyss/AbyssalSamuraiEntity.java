package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.DefenseCombat;
import com.github.ob_yekt.simpleskills.agility.AgilityManager;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaStanceManager;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import com.github.ob_yekt.simpleskills.crafting.CraftingRarity;
import com.github.ob_yekt.simpleskills.prayer.PrayerAbility;
import com.github.ob_yekt.simpleskills.prayer.PrayerVisualEffects;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class AbyssalSamuraiEntity extends Monster {
    public static final EntityType<AbyssalSamuraiEntity> TYPE = registerType();

    private static EntityType<AbyssalSamuraiEntity> registerType() {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyssal_samurai");
        EntityType<AbyssalSamuraiEntity> type = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id,
                EntityType.Builder.<AbyssalSamuraiEntity>of(AbyssalSamuraiEntity::new, MobCategory.MONSTER)
                        .sized(1.30F, 3.50F)
                        .clientTrackingRange(16)
                        .updateInterval(2)
                        .noLootTable()
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
        );
        FabricDefaultAttributeRegistry.register(type, createAttributes());
        return type;
    }

    public static void register() {
        // Forces entity type and attributes to register before registries freeze.
    }

    private static final EntityDataAccessor<Integer> DATA_SAMURAI_STATE =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TICK =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KATANA_ATTACK_TYPE =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_KATANA_ATTACK_FAST =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.BOOLEAN);
    /** Authoritative player-katana stance currently being played by this Samurai. */
    private static final EntityDataAccessor<Integer> DATA_KATANA_STANCE =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.INT);
    /** Elapsed ticks in the active stance, synchronized directly instead of comparing client/server clocks. */
    private static final EntityDataAccessor<Integer> DATA_KATANA_STANCE_AGE_TICKS =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KATANA_BLEND_FROM_STANCE =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_KATANA_BLEND_FROM_SECONDS =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_KATANA_BLEND_AGE_TICKS =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KATANA_BLEND_DURATION_TICKS =
            SynchedEntityData.defineId(AbyssalSamuraiEntity.class, EntityDataSerializers.INT);

    public static final int STATE_PETRIFIED = 0;
    public static final int STATE_AWAKENING = 1;
    public static final int STATE_ACTIVE = 2;

    public static final int AWAKENING_DURATION_TICKS = 40; // 2.0s @ 20 TPS
    private static final double AWAKEN_TRIGGER_RANGE = 8.0D;
    private static final double BASE_MOVEMENT_SPEED = 0.28D;
    private static final double BASE_ATTACK_DAMAGE = 12.0D;

    // The Samurai uses the same six-cut Lankai sequence as the player's katana special.
    private static final int LANKAI_COOLDOWN_TICKS = 20 * 10;
    private static final int LANKAI_DASH_TICKS = 5;
    private static final int LANKAI_LOCK_TICKS = 20;
    private static final int LANKAI_SLASH_COUNT = 6;
    private static final int LANKAI_SLASH_INTERVAL_TICKS = 2;
    private static final int LANKAI_SLASH_PHASE_TICKS = LANKAI_SLASH_COUNT * LANKAI_SLASH_INTERVAL_TICKS;
    private static final int LANKAI_RECOVERY_TICKS = 6;
    private static final double LANKAI_MIN_TARGET_DISTANCE = 3.25D;
    private static final double LANKAI_MAX_TARGET_DISTANCE = 13.15D;
    private static final double LANKAI_MAX_DASH_DISTANCE = 15.5D;
    private static final double LANKAI_BEHIND_TARGET_OFFSET = 0.85D;

    private static final int AGILITY_DASH_COOLDOWN_MIN_TICKS = 100;
    private static final double AGILITY_DASH_MIN_RANGE = 8.5D;
    private static final double AGILITY_DASH_MAX_RANGE = 25.0D;

    private static final int LANKAI_PREP_SHEATH_TICKS = KatanaStanceManager.SHEATH_TICKS;
    /** Brief two-handed hold in the player's dedicated pre-Lankai stance. */
    private static final int LANKAI_PREP_HOLD_TICKS = 20;

    private static final int LANKAI_PHASE_NONE = -1;
    private static final int LANKAI_PHASE_PREP_SHEATH = 0;
    private static final int LANKAI_PHASE_PREP_READY = 1;
    private static final int LANKAI_PHASE_DASH = 2;
    private static final int LANKAI_PHASE_LOCK = 3;
    private static final int LANKAI_PHASE_SLASHES = 4;
    private static final int LANKAI_PHASE_RECOVERY = 5;

    public final AnimationState petrifiedAnimationState = new AnimationState();
    public final AnimationState awakeningAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();

    private int awakeningTicks = 0;
    private int lastClientAttackTick = -1;
    private int lastObservedPetrificationStage = 0;
    private int lankaiCooldownTicks = 0;
    private int agilityDashCooldownTicks = 0;
    private int agilityDashTicksRemaining = 0;
    private int lankaiPhase = LANKAI_PHASE_NONE;
    private int lankaiPhaseStartedTick = 0;
    private int lankaiTargetEntityId = -1;
    private int lankaiSlashIndex = 0;
    private int lankaiLastSlashTick = 0;
    private float lankaiDamagePerSlash = 0.0F;
    private Vec3 lankaiStart = Vec3.ZERO;
    private Vec3 lankaiEnd = Vec3.ZERO;
    private boolean relicsDropped = false;
    private boolean katanaDropped = false;

    public AbyssalSamuraiEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, 1.1D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SAMURAI_STATE, STATE_PETRIFIED);
        builder.define(DATA_ATTACK_TICK, 0);
        builder.define(DATA_KATANA_ATTACK_TYPE, 0);
        builder.define(DATA_KATANA_ATTACK_FAST, false);
        builder.define(DATA_KATANA_STANCE, KatanaStanceManager.SHEATHED);
        builder.define(DATA_KATANA_STANCE_AGE_TICKS, 0);
        builder.define(DATA_KATANA_BLEND_FROM_STANCE, -1);
        builder.define(DATA_KATANA_BLEND_FROM_SECONDS, 0.0F);
        builder.define(DATA_KATANA_BLEND_AGE_TICKS, 0);
        builder.define(DATA_KATANA_BLEND_DURATION_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PetrifiedStasisGoal(this));
        this.goalSelector.addGoal(2, new SamuraiMeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int getSamuraiState() {
        return this.entityData.get(DATA_SAMURAI_STATE);
    }

    public void setSamuraiState(int state) {
        this.entityData.set(DATA_SAMURAI_STATE, state);
    }

    /** Last katana combo animation selected by the server (1-3), synchronized for rendering. */
    public int getKatanaAttackType() {
        return this.entityData.get(DATA_KATANA_ATTACK_TYPE);
    }

    /** Synchronized world game tick at which the current katana attack animation started. */
    public int getKatanaAttackTick() {
        return this.entityData.get(DATA_ATTACK_TICK);
    }

    /** Lankai's six rapid cuts compress the player combo clips into each short slash interval. */
    public boolean isKatanaAttackFast() {
        return this.entityData.get(DATA_KATANA_ATTACK_FAST);
    }

    /** Current shared player-katana stance (draw, guard, attacks, sheathe, or Lankai preparation). */
    public int getKatanaStance() {
        return this.entityData.get(DATA_KATANA_STANCE);
    }

    /** Elapsed ticks in the current player-katana clip; the value is server-synchronized every tick. */
    public int getKatanaStanceAgeTicks() {
        return this.entityData.get(DATA_KATANA_STANCE_AGE_TICKS);
    }

    public int getKatanaBlendFromStance() {
        return this.entityData.get(DATA_KATANA_BLEND_FROM_STANCE);
    }

    public float getKatanaBlendFromSeconds() {
        return this.entityData.get(DATA_KATANA_BLEND_FROM_SECONDS);
    }

    public int getKatanaBlendAgeTicks() {
        return this.entityData.get(DATA_KATANA_BLEND_AGE_TICKS);
    }

    public int getKatanaBlendDurationTicks() {
        return this.entityData.get(DATA_KATANA_BLEND_DURATION_TICKS);
    }

    /**
     * Starts an authoritative stance transition and captures the old clip/time for a smooth cross-fade.
     * The animation clock itself is synchronized as elapsed ticks, so client and server never need
     * to compare their potentially offset world game-time clocks.
     */
    private void setKatanaStance(int stance) {
        int previousStance = getKatanaStance();
        if (previousStance != stance) {
            float previousSeconds = getKatanaStanceAgeTicks() / 20.0F;
            this.entityData.set(DATA_KATANA_BLEND_FROM_STANCE, previousStance);
            this.entityData.set(DATA_KATANA_BLEND_FROM_SECONDS, previousSeconds);
            this.entityData.set(DATA_KATANA_BLEND_AGE_TICKS, 0);
            this.entityData.set(DATA_KATANA_BLEND_DURATION_TICKS,
                    katanaBlendDurationTicks(previousStance, stance));
            this.entityData.set(DATA_KATANA_STANCE, stance);
            this.entityData.set(DATA_KATANA_STANCE_AGE_TICKS, 0);
        }
    }

    /** Advance the synchronized clip clocks once per authoritative server tick. */
    private void advanceKatanaAnimationClocks() {
        this.entityData.set(DATA_KATANA_STANCE_AGE_TICKS,
                Math.min(Integer.MAX_VALUE - 1, getKatanaStanceAgeTicks() + 1));
        int blendDuration = getKatanaBlendDurationTicks();
        if (blendDuration > 0 && getKatanaBlendAgeTicks() < blendDuration) {
            this.entityData.set(DATA_KATANA_BLEND_AGE_TICKS, getKatanaBlendAgeTicks() + 1);
        }
    }

    private static int katanaBlendDurationTicks(int from, int to) {
        if (to == KatanaStanceManager.SPECIAL_DASH
                || to == KatanaStanceManager.SPECIAL_RECOVERY
                || from == KatanaStanceManager.SPECIAL_RECOVERY) {
            return 2; // 80-100 ms special transition.
        }
        if (KatanaStanceManager.isAttackStance(to)) return 2;
        if (KatanaStanceManager.isAttackStance(from)) return 3; // 150 ms recovery blend.
        if (to == KatanaStanceManager.DRAWING
                || to == KatanaStanceManager.SHEATHING
                || to == KatanaStanceManager.LAKAI_SHEATHING
                || to == KatanaStanceManager.LAKAI_READY) {
            return 3;
        }
        return 2;
    }

    /**
     * Progressive petrification stages derived automatically from synchronized health:
     * 0: 100% - 75% HP -> Intact stone statue
     * 1:  75% - 50% HP -> Hairline fractures revealing gold beneath
     * 2:  50% - 25% HP -> Shattering stone plates, ~60% gold exposed
     * 3:  25% -  0% HP -> Stone completely shattered, pure golden samurai armor
     */
    public int getPetrificationStage() {
        float ratio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        if (ratio > 0.75F) return 0;
        if (ratio > 0.50F) return 1;
        if (ratio > 0.25F) return 2;
        return 3;
    }

    public void triggerAwakening() {
        if (!this.isDeadOrDying() && getSamuraiState() == STATE_PETRIFIED) {
            this.awakeningTicks = 0;
            setSamuraiState(STATE_AWAKENING);
            this.playSound(SoundEvents.STONE_BREAK, 1.2F, 0.65F);
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        setSamuraiState(STATE_PETRIFIED);
        ensureKatanaEquipped();
        return data;
    }

    /** Keeps the mod's own katana equipped, including on Samurai entities loaded from older saves. */
    private void ensureKatanaEquipped() {
        Item katana = AbyssalKatanaItems.KATANA_ULTIMO_JURAMENTO;
        if (katana == null || this.katanaDropped || this.isDeadOrDying()) return;
        if (!AbyssalKatanaItems.isKatana(this.getMainHandItem().getItem())) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(katana));
        }
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

    private void updateServerState() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ensureKatanaEquipped();
        advanceKatanaAnimationClocks();

        int currentStage = getPetrificationStage();
        if (currentStage > this.lastObservedPetrificationStage) {
            onStoneShellFractured(serverLevel, currentStage);
            this.lastObservedPetrificationStage = currentStage;
        }

        int state = getSamuraiState();
        if (state == STATE_PETRIFIED) {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            Player nearest = serverLevel.getNearestPlayer(this, AWAKEN_TRIGGER_RANGE);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator() && this.hasLineOfSight(nearest)) {
                this.setTarget(nearest);
                triggerAwakening();
            }
        } else if (state == STATE_AWAKENING) {
            this.awakeningTicks++;
            this.getNavigation().stop();
            if (this.awakeningTicks % 5 == 0) {
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()),
                        this.getX(), this.getY() + 1.2D, this.getZ(),
                        8, 0.45D, 0.65D, 0.45D, 0.04D);
            }
            if (this.awakeningTicks >= AWAKENING_DURATION_TICKS) {
                setSamuraiState(STATE_ACTIVE);
                // The Samurai visibly draws the same katana used by the player when it awakens.
                setKatanaStance(KatanaStanceManager.DRAWING);
            }
        } else if (state == STATE_ACTIVE) {
            updateActiveCombat(serverLevel);
        }
    }

    /** Runs the Samurai's signature katana special and the existing Agility dash while in combat. */
    private void updateActiveCombat(ServerLevel serverLevel) {
        if (this.lankaiCooldownTicks > 0) this.lankaiCooldownTicks--;
        if (this.agilityDashCooldownTicks > 0) this.agilityDashCooldownTicks--;
        if (this.agilityDashTicksRemaining > 0) {
            this.agilityDashTicksRemaining--;
            this.getNavigation().stop();
            return;
        }

        if (this.lankaiPhase != LANKAI_PHASE_NONE) {
            tickLankai(serverLevel);
            return;
        }

        Player target = this.getTarget() instanceof Player player && player.isAlive()
                && !player.isCreative() && !player.isSpectator() && this.hasLineOfSight(player)
                ? player : null;

        // Lankai has priority over ordinary drawing/sheathing transitions. It deliberately
        // returns the blade to the saya, holds the dedicated pre-Lankai pose, then dashes.
        if (target != null) {
            double distance = this.distanceTo(target);
            if (this.lankaiCooldownTicks <= 0
                    && distance >= LANKAI_MIN_TARGET_DISTANCE
                    && distance <= LANKAI_MAX_TARGET_DISTANCE
                    && startLankai(serverLevel, target)) {
                return;
            }
        }

        tickKatanaStance();
        if (target != null) {
            updateAgilityDash(serverLevel, target);
        }
    }

    /**
     * Advances the same inactivity-driven draw -> guard -> lower guard -> sheathe sequence as the player.
     * Unlike a proximity toggle, this lets the Samurai sheathe even while an opponent remains nearby;
     * the next committed melee swing calls doHurtTarget(), which draws the blade again first.
     */
    private void tickKatanaStance() {
        int stance = getKatanaStance();
        int elapsed = getKatanaStanceAgeTicks();

        switch (stance) {
            case KatanaStanceManager.DRAWING -> {
                if (elapsed >= KatanaStanceManager.DRAW_TICKS) {
                    setKatanaStance(KatanaStanceManager.UNSHEATHED);
                }
            }
            case KatanaStanceManager.ATTACK_1 -> {
                if (elapsed >= KatanaStanceManager.ATTACK_1_TICKS) {
                    setKatanaStance(KatanaStanceManager.UNSHEATHED);
                }
            }
            case KatanaStanceManager.ATTACK_2 -> {
                if (elapsed >= KatanaStanceManager.ATTACK_2_TICKS) {
                    setKatanaStance(KatanaStanceManager.UNSHEATHED);
                }
            }
            case KatanaStanceManager.ATTACK_3 -> {
                if (elapsed >= KatanaStanceManager.ATTACK_3_TICKS) {
                    setKatanaStance(KatanaStanceManager.UNSHEATHED);
                }
            }
            case KatanaStanceManager.UNSHEATHED -> {
                if (elapsed >= KatanaStanceManager.IDLE_BEFORE_LOWER_GUARD_TICKS) {
                    setKatanaStance(KatanaStanceManager.LOWER_GUARD);
                }
            }
            case KatanaStanceManager.LOWER_GUARD -> {
                if (elapsed >= KatanaStanceManager.LOWER_GUARD_TOTAL_TICKS) {
                    setKatanaStance(KatanaStanceManager.SHEATHING);
                }
            }
            case KatanaStanceManager.SHEATHING -> {
                if (elapsed >= KatanaStanceManager.SHEATH_TICKS) {
                    setKatanaStance(KatanaStanceManager.SHEATHED);
                }
            }
            case KatanaStanceManager.SHEATHED -> {
                // Remains sheathed until a melee swing or Lankai actually needs the weapon.
            }
            default -> {
                // Lankai-specific stances are advanced only by tickLankai().
            }
        }
    }

    /** Starts the same lightning displacement and six rapid cuts used by the player's Lankai. */
    private boolean startLankai(ServerLevel serverLevel, Player target) {
        Vec3 towardTarget = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        if (towardTarget.lengthSqr() < 1.0E-5D) return false;
        Vec3 direction = towardTarget.normalize();

        double behindOffset = Math.max(LANKAI_BEHIND_TARGET_OFFSET,
                target.getBbWidth() * 0.5D + this.getBbWidth() * 0.5D + 0.20D);
        Vec3 end = new Vec3(target.getX() + direction.x * behindOffset,
                this.getY(), target.getZ() + direction.z * behindOffset);
        if (end.distanceTo(this.position()) > LANKAI_MAX_DASH_DISTANCE) return false;

        this.lankaiStart = this.position();
        this.lankaiEnd = end;
        this.lankaiTargetEntityId = target.getId();
        this.lankaiSlashIndex = 0;
        this.lankaiLastSlashTick = this.tickCount;
        this.lankaiDamagePerSlash = 0.0F;
        this.lankaiCooldownTicks = LANKAI_COOLDOWN_TICKS;

        // Play the player's actual sheath animation first. Once the blade is away, hold
        // idle_lakai_sheathed for a short anticipation window before the special dash begins.
        if (getKatanaStance() == KatanaStanceManager.SHEATHED) {
            this.lankaiPhase = LANKAI_PHASE_PREP_READY;
            setKatanaStance(KatanaStanceManager.LAKAI_READY);
        } else {
            this.lankaiPhase = LANKAI_PHASE_PREP_SHEATH;
            setKatanaStance(KatanaStanceManager.LAKAI_SHEATHING);
        }
        this.lankaiPhaseStartedTick = this.tickCount;

        this.getNavigation().stop();
        this.stopHorizontalMovementPreservingFall();
        Vec3 dashVfxStart = this.position().add(0.0D, this.getBbHeight() * 0.62D, 0.0D);
        Vec3 dashVfxEnd = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        PrayerVisualEffects.playConnection(serverLevel, dashVfxStart, dashVfxEnd, PrayerAbility.SPARK);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(),
                18, 0.28D, 0.42D, 0.28D, 0.12D);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.9F, 1.65F);
        return true;
    }

    private void tickLankai(ServerLevel serverLevel) {
        Entity found = serverLevel.getEntity(this.lankaiTargetEntityId);
        if (!(found instanceof Player target) || !target.isAlive() || target.isSpectator()
                || target.isCreative() || target.level() != this.level()) {
            finishLankai();
            return;
        }

        this.getNavigation().stop();
        int elapsed = this.tickCount - this.lankaiPhaseStartedTick;

        if (this.lankaiPhase == LANKAI_PHASE_PREP_SHEATH) {
            if (elapsed >= LANKAI_PREP_SHEATH_TICKS) {
                this.lankaiPhase = LANKAI_PHASE_PREP_READY;
                this.lankaiPhaseStartedTick = this.tickCount;
                setKatanaStance(KatanaStanceManager.LAKAI_READY);
            }
            return;
        }

        if (this.lankaiPhase == LANKAI_PHASE_PREP_READY) {
            if (elapsed >= LANKAI_PREP_HOLD_TICKS) {
                this.lankaiPhase = LANKAI_PHASE_DASH;
                this.lankaiPhaseStartedTick = this.tickCount;
                setKatanaStance(KatanaStanceManager.SPECIAL_DASH);
                this.stopHorizontalMovementPreservingFall();
            }
            return;
        }

        if (this.lankaiPhase == LANKAI_PHASE_DASH) {
            double progress = Math.min(1.0D, elapsed / (double) LANKAI_DASH_TICKS);
            Vec3 desired = this.lankaiStart.lerp(this.lankaiEnd, progress);
            this.stopHorizontalMovementPreservingFall();
            Vec3 horizontalDelta = desired.subtract(this.position());
            this.move(MoverType.SELF, new Vec3(horizontalDelta.x, 0.0D, horizontalDelta.z));

            if (elapsed >= LANKAI_DASH_TICKS) {
                Vec3 travel = this.lankaiEnd.subtract(this.lankaiStart);
                Vec3 direction = new Vec3(travel.x, 0.0D, travel.z).normalize();
                if (direction.lengthSqr() > 1.0E-5D) {
                    Vec3 behind = findSafeLankaiPosition(serverLevel, target, direction);
                    this.setPos(behind.x, behind.y, behind.z);
                }
                this.stopHorizontalMovementPreservingFall();
                this.getLookControl().setLookAt(target, 45.0F, 45.0F);
                this.lankaiPhase = LANKAI_PHASE_LOCK;
                this.lankaiPhaseStartedTick = this.tickCount;
                DefenseCombat.applyStun(target, LANKAI_LOCK_TICKS);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                        20, 0.4D, 0.5D, 0.4D, 0.12D);
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                        14, 0.35D, 0.45D, 0.35D, 0.08D);
            }
            return;
        }

        if (this.lankaiPhase == LANKAI_PHASE_LOCK) {
            DefenseCombat.applyStun(target, 2);
            if (elapsed >= LANKAI_LOCK_TICKS) {
                this.lankaiPhase = LANKAI_PHASE_SLASHES;
                this.lankaiPhaseStartedTick = this.tickCount;
                this.lankaiSlashIndex = 0;
                this.lankaiLastSlashTick = this.tickCount;
                applyLankaiSlash(serverLevel, target, this.lankaiSlashIndex++);
            }
            return;
        }

        if (this.lankaiPhase == LANKAI_PHASE_SLASHES) {
            int slashElapsed = this.tickCount - this.lankaiPhaseStartedTick;
            if (this.lankaiSlashIndex < LANKAI_SLASH_COUNT
                    && this.tickCount - this.lankaiLastSlashTick >= LANKAI_SLASH_INTERVAL_TICKS) {
                applyLankaiSlash(serverLevel, target, this.lankaiSlashIndex++);
                this.lankaiLastSlashTick = this.tickCount;
            }
            if (this.lankaiSlashIndex >= LANKAI_SLASH_COUNT && slashElapsed >= LANKAI_SLASH_PHASE_TICKS) {
                this.lankaiPhase = LANKAI_PHASE_RECOVERY;
                this.lankaiPhaseStartedTick = this.tickCount;
                setKatanaStance(KatanaStanceManager.SPECIAL_RECOVERY);
            }
            return;
        }

        if (this.lankaiPhase == LANKAI_PHASE_RECOVERY && elapsed >= LANKAI_RECOVERY_TICKS) {
            finishLankai();
        }
    }

    private Vec3 findSafeLankaiPosition(ServerLevel serverLevel, Player target, Vec3 direction) {
        double requiredOffset = Math.max(LANKAI_BEHIND_TARGET_OFFSET,
                target.getBbWidth() * 0.5D + this.getBbWidth() * 0.5D + 0.20D);
        Vec3 best = new Vec3(target.getX() + direction.x * requiredOffset,
                this.getY(), target.getZ() + direction.z * requiredOffset);

        for (int step = 0; step <= 6; step++) {
            double offset = requiredOffset + step * 0.20D;
            Vec3 candidate = new Vec3(target.getX() + direction.x * offset,
                    this.getY(), target.getZ() + direction.z * offset);
            if (candidate.distanceTo(this.lankaiStart) > LANKAI_MAX_DASH_DISTANCE) break;
            AABB candidateBox = this.getBoundingBox().move(candidate.subtract(this.position()));
            best = candidate;
            if (serverLevel.noCollision(this, candidateBox)) return candidate;
        }
        return best;
    }

    private void applyLankaiSlash(ServerLevel serverLevel, Player target, int slashIndex) {
        if (this.lankaiDamagePerSlash <= 0.0F) {
            var attackDamageAttribute = this.getAttribute(Attributes.ATTACK_DAMAGE);
            double attackDamage = attackDamageAttribute == null
                    ? BASE_ATTACK_DAMAGE : attackDamageAttribute.getValue();
            this.lankaiDamagePerSlash = (float) (attackDamage * 2.0D / LANKAI_SLASH_COUNT);
        }

        target.damageCooldownTime = 0;
        boolean hit = target.hurtServer(serverLevel,
                this.damageSources().mobAttack(this), this.lankaiDamagePerSlash);
        this.entityData.set(DATA_ATTACK_TICK, (int) serverLevel.getGameTime());
        this.entityData.set(DATA_KATANA_ATTACK_TYPE, (slashIndex % 3) + 1);
        this.entityData.set(DATA_KATANA_ATTACK_FAST, true);

        double height = target.getY() + target.getBbHeight() * (0.32D + (slashIndex % 3) * 0.18D);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                target.getX(), height, target.getZ(), 1, 0.28D, 0.20D, 0.28D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                hit ? 10 : 4, 0.35D, 0.45D, 0.35D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                8, 0.30D, 0.42D, 0.30D, 0.09D);

        Vec3 arcOrigin = target.position().add(0.0D, target.getBbHeight() * 0.56D, 0.0D);
        double arcAngle = slashIndex * 2.399963229728653D;
        double arcLength = 0.72D + (slashIndex % 2) * 0.16D;
        Vec3 arcEnd = arcOrigin.add(Math.cos(arcAngle) * arcLength,
                0.18D + Math.sin(slashIndex * 1.17D) * 0.42D,
                Math.sin(arcAngle) * arcLength);
        PrayerVisualEffects.playConnection(serverLevel, arcOrigin, arcEnd, PrayerAbility.SPARK);
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.65F,
                0.82F + slashIndex * 0.055F);
    }

    private void finishLankai() {
        this.lankaiPhase = LANKAI_PHASE_NONE;
        this.lankaiTargetEntityId = -1;
        this.lankaiSlashIndex = 0;
        this.lankaiDamagePerSlash = 0.0F;
        this.entityData.set(DATA_KATANA_ATTACK_FAST, false);
        this.stopHorizontalMovementPreservingFall();
        if (!this.isDeadOrDying() && getSamuraiState() == STATE_ACTIVE) {
            setKatanaStance(KatanaStanceManager.DRAWING);
        }
    }

    /**
     * Stops horizontal drift without cancelling gravity. Positive vertical velocity is removed so
     * the Samurai's special moves cannot keep him hovering after an Agility dash or knock-up.
     */
    private void stopHorizontalMovementPreservingFall() {
        Vec3 velocity = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, Math.min(velocity.y, 0.0D), 0.0D);
    }

    /** Reuses the exact level-99 horizontal impulse and cooldown calculation used by Agility Dash. */
    private void updateAgilityDash(ServerLevel serverLevel, Player target) {
        if (this.agilityDashCooldownTicks > 0 || target == null || !target.isAlive()) return;

        double distanceSqr = this.distanceToSqr(target);
        if (distanceSqr < AGILITY_DASH_MIN_RANGE * AGILITY_DASH_MIN_RANGE
                || distanceSqr > AGILITY_DASH_MAX_RANGE * AGILITY_DASH_MAX_RANGE
                || !this.hasLineOfSight(target)) {
            return;
        }

        Vec3 direction = target.position().subtract(this.position());
        direction = new Vec3(direction.x, 0.0D, direction.z);
        if (direction.lengthSqr() < 1.0E-5D) return;
        direction = direction.normalize();

        double impulse = AgilityManager.dashImpulseAtLevel99();
        Vec3 velocity = this.getDeltaMovement();
        this.getNavigation().stop();
        // Agility's dash is a horizontal burst, not a jump. Keep downward motion if airborne,
        // but never add upward velocity; this keeps the boss grounded on flat terrain.
        this.setDeltaMovement(direction.x * impulse,
                Math.min(velocity.y, 0.0D),
                direction.z * impulse);
        this.agilityDashTicksRemaining = 8;
        this.agilityDashCooldownTicks = Math.max(AGILITY_DASH_COOLDOWN_MIN_TICKS,
                AgilityManager.dashCooldownAtLevel99() * 2);
        this.getLookControl().setLookAt(target, 45.0F, 45.0F);

        this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.95F, 1.35F);
        Vec3 trail = this.position().subtract(direction.scale(0.55D)).add(0.0D, 0.35D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                trail.x, trail.y, trail.z, 16, 0.32D, 0.22D, 0.32D, 0.06D);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                trail.x, trail.y, trail.z, 18, 0.28D, 0.30D, 0.28D, 0.04D);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                this.getX(), this.getY() + 0.8D, this.getZ(),
                12, 0.45D, 0.55D, 0.45D, 0.08D);
    }

    /** Bursts stone debris & golden/arcane sparks whenever a combat milestone shatters more stone off the armor. */
    private void onStoneShellFractured(ServerLevel serverLevel, int newStage) {
        this.playSound(SoundEvents.STONE_BREAK, 1.35F, 0.72F + newStage * 0.08F);
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.15F, 0.90F + newStage * 0.12F);

        int blockCount = newStage == 3 ? 45 : 24;
        serverLevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()),
                this.getX(), this.getY() + 1.15D, this.getZ(),
                blockCount, 0.55D, 0.75D, 0.55D, 0.08D);
        serverLevel.sendParticles(ParticleTypes.ASH,
                this.getX(), this.getY() + 1.15D, this.getZ(),
                18, 0.50D, 0.65D, 0.50D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                this.getX(), this.getY() + 1.25D, this.getZ(),
                16, 0.45D, 0.55D, 0.45D, 0.09D);

        // Shedding heavy stone makes the Samurai noticeably faster & deadlier!
        var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(BASE_MOVEMENT_SPEED * (1.0D + newStage * 0.12D));
        }
        var dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            dmgAttr.setBaseValue(BASE_ATTACK_DAMAGE * (1.0D + newStage * 0.10D));
        }
    }

    /**
     * The signature katana is spawned from die() rather than the loot-table callback.
     * This keeps the 100% reward independent of the entity's noLootTable setting and mob-loot table.
     */
    @Override
    public void die(DamageSource damageSource) {
        ItemStack katanaDrop = ItemStack.EMPTY;
        ServerLevel serverLevel = this.level() instanceof ServerLevel level ? level : null;

        if (serverLevel != null && !this.katanaDropped) {
            Item katanaItem = AbyssalKatanaItems.KATANA_ULTIMO_JURAMENTO;
            if (katanaItem != null) {
                this.katanaDropped = true;
                katanaDrop = AbyssalKatanaItems.isKatana(this.getMainHandItem().getItem())
                        ? this.getMainHandItem().copy()
                        : new ItemStack(katanaItem);

                // Clear the equipment before vanilla death handling so it cannot also drop randomly.
                if (AbyssalKatanaItems.isKatana(this.getMainHandItem().getItem())) {
                    this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
            }
        }

        super.die(damageSource);

        if (serverLevel != null && !katanaDrop.isEmpty()) {
            CraftingRarity rarity = this.getRandom().nextFloat() < 0.05F
                    ? CraftingRarity.TRANSCENDENT : CraftingRarity.MYTHIC;
            CraftingEquipmentManager.markAbyssalLootRarity(katanaDrop, rarity);
            ItemEntity drop = new ItemEntity(
                    serverLevel, this.getX() + 0.20D, this.getY() + 0.35D, this.getZ(), katanaDrop);
            drop.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(drop);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(serverLevel, source, causedByPlayer);
        if (this.relicsDropped) {
            return;
        }
        this.relicsDropped = true;

        Item chestplateItem = AbyssalKingItems.ABYSSAL_SAMURAI_CHESTPLATE;
        if (chestplateItem != null) {
            CraftingRarity rarity = this.getRandom().nextFloat() < 0.05F
                    ? CraftingRarity.TRANSCENDENT : CraftingRarity.MYTHIC;
            ItemStack stack = new ItemStack(chestplateItem);
            CraftingEquipmentManager.markAbyssalLootRarity(stack, rarity);
            ItemEntity drop = new ItemEntity(serverLevel, this.getX(), this.getY() + 0.35D, this.getZ(), stack);
            drop.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(drop);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (this.isDeadOrDying() || this.lankaiPhase != LANKAI_PHASE_NONE) {
            return false;
        }
        int stance = getKatanaStance();
        if (stance != KatanaStanceManager.UNSHEATHED) {
            // A normal melee strike cannot begin while the blade is still in its saya,
            // being drawn, or moving through a recovery pose.
            if (stance == KatanaStanceManager.SHEATHED || stance == KatanaStanceManager.SHEATHING
                    || stance == KatanaStanceManager.LOWER_GUARD) {
                setKatanaStance(KatanaStanceManager.DRAWING);
            }
            return false;
        }

        // doHurtTarget is called when the melee goal commits to a swing. Start the
        // exact player combo clip even if the target blocks or otherwise rejects damage.
        int nextAttackType = (this.entityData.get(DATA_KATANA_ATTACK_TYPE) % 3) + 1;
        this.entityData.set(DATA_KATANA_ATTACK_TYPE, nextAttackType);
        this.entityData.set(DATA_ATTACK_TICK, (int) level.getGameTime());
        this.entityData.set(DATA_KATANA_ATTACK_FAST, false);
        setKatanaStance(KatanaStanceManager.attackStanceForStep(nextAttackType));

        boolean hurt = super.doHurtTarget(level, target);
        if (hurt) {
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.95F);
        }
        return hurt;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isDeadOrDying()) {
            return false;
        }
        if (getSamuraiState() == STATE_PETRIFIED) {
            triggerAwakening();
            return super.hurtServer(level, source, amount * 0.50F);
        }
        return super.hurtServer(level, source, amount);
    }

    private void updateClientAnimations() {
        if (this.isDeadOrDying()) {
            this.petrifiedAnimationState.stop();
            this.awakeningAnimationState.stop();
            this.idleAnimationState.stop();
            this.walkAnimationState.stop();
            this.attackAnimationState.stop();
            this.deathAnimationState.startIfStopped(this.tickCount);
            return;
        }

        int state = getSamuraiState();
        if (state == STATE_PETRIFIED) {
            this.awakeningAnimationState.stop();
            this.idleAnimationState.stop();
            this.walkAnimationState.stop();
            this.petrifiedAnimationState.startIfStopped(this.tickCount);
            return;
        }

        if (state == STATE_AWAKENING) {
            this.petrifiedAnimationState.stop();
            this.idleAnimationState.stop();
            this.walkAnimationState.stop();
            this.awakeningAnimationState.startIfStopped(this.tickCount);
            return;
        }

        this.petrifiedAnimationState.stop();
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
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SamuraiState", getSamuraiState());
        output.putInt("AwakeningTicks", this.awakeningTicks);
        output.putInt("LankaiCooldownTicks", this.lankaiCooldownTicks);
        output.putInt("AgilityDashCooldownTicks", this.agilityDashCooldownTicks);
        output.putBoolean("RelicsDropped", this.relicsDropped);
        output.putBoolean("KatanaDropped", this.katanaDropped);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int savedState = input.getIntOr("SamuraiState", STATE_PETRIFIED);
        setSamuraiState(Math.max(STATE_PETRIFIED, Math.min(STATE_ACTIVE, savedState)));
        this.awakeningTicks = Math.max(0, Math.min(AWAKENING_DURATION_TICKS,
                input.getIntOr("AwakeningTicks", 0)));
        this.lankaiCooldownTicks = Math.max(0, input.getIntOr("LankaiCooldownTicks", 0));
        this.agilityDashCooldownTicks = Math.max(0, input.getIntOr("AgilityDashCooldownTicks", 0));
        this.agilityDashTicksRemaining = 0;
        this.lankaiPhase = LANKAI_PHASE_NONE;
        this.lankaiTargetEntityId = -1;
        this.lankaiDamagePerSlash = 0.0F;
        this.relicsDropped = input.getBooleanOr("RelicsDropped", false);
        this.katanaDropped = input.getBooleanOr("KatanaDropped", false);
        this.entityData.set(DATA_KATANA_STANCE_AGE_TICKS, 0);
        this.entityData.set(DATA_KATANA_BLEND_AGE_TICKS, 0);
        setKatanaStance(KatanaStanceManager.SHEATHED);
        ensureKatanaEquipped();
        this.lastObservedPetrificationStage = getPetrificationStage();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return getPetrificationStage() < 2 ? SoundEvents.STONE_HIT : SoundEvents.ANVIL_PLACE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    private static class PetrifiedStasisGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final AbyssalSamuraiEntity samurai;

        PetrifiedStasisGoal(AbyssalSamuraiEntity samurai) {
            this.samurai = samurai;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !this.samurai.isDeadOrDying() && this.samurai.getSamuraiState() != STATE_ACTIVE;
        }

        @Override
        public void tick() {
            this.samurai.getNavigation().stop();
        }
    }

    /** Stops ordinary pathfinding attacks from overwriting the Samurai's Lankai and Agility dashes. */
    private static class SamuraiMeleeAttackGoal extends MeleeAttackGoal {
        private final AbyssalSamuraiEntity samurai;

        SamuraiMeleeAttackGoal(AbyssalSamuraiEntity samurai, double speed, boolean followingTargetEvenIfNotSeen) {
            super(samurai, speed, followingTargetEvenIfNotSeen);
            this.samurai = samurai;
        }

        private boolean canUseMelee() {
            return this.samurai.getSamuraiState() == STATE_ACTIVE
                    && this.samurai.lankaiPhase == LANKAI_PHASE_NONE
                    && this.samurai.agilityDashTicksRemaining <= 0;
        }

        @Override
        public boolean canUse() {
            return canUseMelee() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return canUseMelee() && super.canContinueToUse();
        }
    }
}
