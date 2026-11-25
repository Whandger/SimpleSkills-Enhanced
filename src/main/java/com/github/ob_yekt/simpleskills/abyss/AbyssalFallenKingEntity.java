package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
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

    public static final int STATE_SITTING = 0;
    public static final int STATE_AWAKENING = 1;
    public static final int STATE_ACTIVE = 2;

    public static final int AWAKENING_DURATION_TICKS = 48; // 2.4s @ 20 TPS
    public static final int ATTACK_DURATION_TICKS = 20;    // 1.0s @ 20 TPS
    private static final double AWAKEN_TRIGGER_RANGE = 10.0D;

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
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ThroneSlumberGoal(this));
        this.goalSelector.addGoal(2, new KingMeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
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
            this.bossEvent.setVisible(state != STATE_SITTING);
        }
    }

    public boolean isSittingOnThrone() {
        return getKingState() == STATE_SITTING;
    }

    public boolean isAwakening() {
        return getKingState() == STATE_AWAKENING;
    }

    public boolean isActiveKing() {
        return getKingState() == STATE_ACTIVE;
    }

    public void triggerAwakening() {
        if (getKingState() == STATE_SITTING) {
            this.awakeningTicks = 0;
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
        } else {
            updateServerState();
        }
    }

    private void updateServerState() {
        int state = getKingState();

        if (state == STATE_SITTING) {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            Player nearest = this.level().getNearestPlayer(this, AWAKEN_TRIGGER_RANGE);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator() && this.hasLineOfSight(nearest)) {
                this.setTarget(nearest);
                triggerAwakening();
            }
        } else if (state == STATE_AWAKENING) {
            this.awakeningTicks++;
            this.getNavigation().stop();
            if (this.level() instanceof ServerLevel serverLevel && this.awakeningTicks % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + 1.2D, this.getZ(),
                        6, 0.6D, 0.8D, 0.6D, 0.03D);
            }
            if (this.awakeningTicks >= AWAKENING_DURATION_TICKS) {
                setKingState(STATE_ACTIVE);
            }
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    private void updateClientAnimations() {
        if (this.isDeadOrDying()) {
            this.sittingIdleAnimationState.stop();
            this.awakeningAnimationState.stop();
            this.idleAnimationState.stop();
            this.walkAnimationState.stop();
            this.attackAnimationState.stop();
            this.deathAnimationState.startIfStopped(this.tickCount);
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
        this.bossEvent.addPlayer(player);
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int savedState = input.getIntOr("KingState", STATE_SITTING);
        setKingState(Math.max(STATE_SITTING, Math.min(STATE_ACTIVE, savedState)));
        this.awakeningTicks = Math.max(0, Math.min(AWAKENING_DURATION_TICKS,
                input.getIntOr("AwakeningTicks", 0)));

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
            return !this.king.isActiveKing();
        }

        @Override
        public void tick() {
            this.king.getNavigation().stop();
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
            return this.king.isActiveKing() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.king.isActiveKing() && super.canContinueToUse();
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
