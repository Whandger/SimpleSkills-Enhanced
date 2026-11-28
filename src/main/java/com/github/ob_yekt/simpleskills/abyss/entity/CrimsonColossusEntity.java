package com.github.ob_yekt.simpleskills.abyss.entity;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.abyss.AbyssContent;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class CrimsonColossusEntity extends Monster {
    public static final EntityType<CrimsonColossusEntity> TYPE = registerType();

    private static EntityType<CrimsonColossusEntity> registerType() {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "crimson_colossus");
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        EntityType<CrimsonColossusEntity> type = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id,
                EntityType.Builder.<CrimsonColossusEntity>of(CrimsonColossusEntity::new, MobCategory.MONSTER)
                        .sized(3.0F, 3.1F)
                        .clientTrackingRange(16)
                        .updateInterval(2)
                        .build(key)
        );
        FabricDefaultAttributeRegistry.register(type, createAttributes());
        return type;
    }

    /** Forces the Colossus type and attributes to register before registries freeze. */
    public static void register() {
    }
    public enum AttackType {
        NONE(0, 0),
        CRYSTAL_LASER(1, 90),
        SEISMIC_SLAM(2, 54),
        CRYSTAL_BARRAGE(3, 48),
        COLOSSUS_LEAP(4, 62);

        public final int id;
        public final int duration;

        AttackType(int id, int duration) {
            this.id = id;
            this.duration = duration;
        }

        public static AttackType byId(int id) {
            for (AttackType type : values()) {
                if (type.id == id) return type;
            }
            return NONE;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_ATTACK_TYPE =
            SynchedEntityData.defineId(CrimsonColossusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TICK =
            SynchedEntityData.defineId(CrimsonColossusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TARGET_X =
            SynchedEntityData.defineId(CrimsonColossusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
            SynchedEntityData.defineId(CrimsonColossusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z =
            SynchedEntityData.defineId(CrimsonColossusEntity.class, EntityDataSerializers.FLOAT);

    private static final DustParticleOptions CRIMSON_ENERGY =
            new DustParticleOptions(0xF6384C, 1.45F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.translatable("entity.simpleskills.crimson_colossus"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    private int globalAttackCooldown = 45;
    private int laserCooldown = 0;
    private int slamCooldown = 0;
    private int barrageCooldown = 0;
    private int leapCooldown = 0;

    private boolean leapLaunched = false;
    private boolean leapLanded = false;
    private Vec3 lockedTargetPos = Vec3.ZERO;

    public CrimsonColossusEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 350;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_TYPE, AttackType.NONE.id);
        builder.define(DATA_ATTACK_TICK, 0);
        builder.define(DATA_TARGET_X, 0.0F);
        builder.define(DATA_TARGET_Y, 0.0F);
        builder.define(DATA_TARGET_Z, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.2D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 44.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true) {
            @Override
            public boolean canUse() {
                return getAttackType() == AttackType.NONE && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getAttackType() == AttackType.NONE && super.canContinueToUse();
            }
        });
        // Stay on the central arena dais while idle; the melee goal takes over once a player is found.
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide()) {
            AttackType current = getAttackType();
            int particleCount = current != AttackType.NONE ? 3 : 1;
            for (int i = 0; i < particleCount; i++) {
                if (this.random.nextFloat() < 0.55F) {
                    double rx = this.getX() + (this.random.nextDouble() - 0.5D) * 2.0D;
                    double ry = this.getY() + 2.3D + this.random.nextDouble() * 1.2D;
                    double rz = this.getZ() + (this.random.nextDouble() - 0.5D) * 2.0D;
                    this.level().addParticle(CRIMSON_ENERGY, rx, ry, rz, 0.0D, 0.04D, 0.0D);
                }
            }

            if (this.walkAnimation.speed() > 0.15F && this.random.nextFloat() < 0.35F) {
                double fx = this.getX() + (this.random.nextDouble() - 0.5D) * 2.6D;
                double fz = this.getZ() + (this.random.nextDouble() - 0.5D) * 2.6D;
                this.level().addParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, AbyssContent.ABYSSAL_STONE.defaultBlockState()),
                        fx, this.getY() + 0.1D, fz,
                        0.0D, 0.06D, 0.0D
                );
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (globalAttackCooldown > 0) globalAttackCooldown--;
        if (laserCooldown > 0) laserCooldown--;
        if (slamCooldown > 0) slamCooldown--;
        if (barrageCooldown > 0) barrageCooldown--;
        if (leapCooldown > 0) leapCooldown--;

        AttackType current = getAttackType();
        if (current != AttackType.NONE) {
            int tick = getAttackTick() + 1;
            setAttackTick(tick);
            tickActiveAttack(level, current, tick);

            if (tick >= current.duration) {
                finishAttack();
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && globalAttackCooldown <= 0) {
            trySelectAttack(target);
        }
    }

    private void trySelectAttack(LivingEntity target) {
        double dist = Math.sqrt(this.distanceToSqr(target));

        if (dist <= 5.8D && slamCooldown <= 0 && this.random.nextFloat() < 0.45F) {
            startAttack(AttackType.SEISMIC_SLAM, target.position());
            return;
        }

        if (dist >= 7.0D && dist <= 20.0D && leapCooldown <= 0 && this.onGround() && this.random.nextFloat() < 0.38F) {
            startAttack(AttackType.COLOSSUS_LEAP, target.position());
            return;
        }

        if (dist >= 5.0D && dist <= 22.0D && barrageCooldown <= 0 && this.random.nextFloat() < 0.42F) {
            startAttack(AttackType.CRYSTAL_BARRAGE, target.position());
            return;
        }

        if (dist >= 4.0D && dist <= 24.0D && laserCooldown <= 0 && this.hasLineOfSight(target)) {
            startAttack(AttackType.CRYSTAL_LASER, target.getEyePosition());
        }
    }

    private void startAttack(AttackType type, Vec3 targetPos) {
        this.getNavigation().stop();
        this.entityData.set(DATA_ATTACK_TYPE, type.id);
        this.entityData.set(DATA_ATTACK_TICK, 0);
        this.lockedTargetPos = targetPos;
        syncTargetPos(targetPos);

        this.leapLaunched = false;
        this.leapLanded = false;

        switch (type) {
            case CRYSTAL_LASER -> this.playSound(SoundEvents.BEACON_ACTIVATE, 1.6F, 0.55F);
            case SEISMIC_SLAM -> this.playSound(SoundEvents.WARDEN_ROAR, 1.5F, 0.65F);
            case CRYSTAL_BARRAGE -> this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.8F, 0.50F);
            case COLOSSUS_LEAP -> this.playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 0.60F);
            default -> {}
        }
    }

    private void finishAttack() {
        AttackType finished = getAttackType();
        this.entityData.set(DATA_ATTACK_TYPE, AttackType.NONE.id);
        this.entityData.set(DATA_ATTACK_TICK, 0);
        this.globalAttackCooldown = 36;

        switch (finished) {
            case CRYSTAL_LASER -> this.laserCooldown = 160;
            case SEISMIC_SLAM -> this.slamCooldown = 110;
            case CRYSTAL_BARRAGE -> this.barrageCooldown = 95;
            case COLOSSUS_LEAP -> this.leapCooldown = 150;
            default -> {}
        }
    }

    private void tickActiveAttack(ServerLevel level, AttackType type, int tick) {
        LivingEntity target = this.getTarget();
        switch (type) {
            case CRYSTAL_LASER -> tickCrystalLaser(level, target, tick);
            case SEISMIC_SLAM -> tickSeismicSlam(level, target, tick);
            case CRYSTAL_BARRAGE -> tickCrystalBarrage(level, target, tick);
            case COLOSSUS_LEAP -> tickColossusLeap(level, target, tick);
            default -> {}
        }
    }

    private void tickCrystalLaser(ServerLevel level, LivingEntity target, int tick) {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);

        if (target != null && target.isAlive()) {
            // Track the target throughout the charge, then lock to its exact eye position
            // every firing tick. This prevents the beam from trailing behind a moving player.
            this.getLookControl().setLookAt(target, tick < 28 ? 24.0F : 40.0F, 24.0F);
            Vec3 desired = target.getEyePosition();
            if (tick < 28) {
                this.lockedTargetPos = this.lockedTargetPos.lerp(desired, 0.30D);
            } else {
                this.lockedTargetPos = desired;
            }
            syncTargetPos(this.lockedTargetPos);
        }

        if (tick < 28 && tick % 6 == 0) {
            this.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.1F, 0.70F + tick * 0.02F);
        }
        if (tick == 28) {
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.35F, 0.72F);
        }

        if (tick >= 28 && tick <= 78) {
            if (tick % 8 == 0) {
                this.playSound(SoundEvents.BEACON_AMBIENT, 1.5F, 0.55F);
            }
            if ((tick - 28) % 5 == 0) {
                Vec3 mouthOrigin = getMouthWorldPosition();
                Vec3 toEnd = this.lockedTargetPos.subtract(mouthOrigin);
                double len = Math.max(0.5D, Math.min(26.0D, toEnd.length()));
                Vec3 dir = toEnd.normalize();
                Vec3 beamEnd = mouthOrigin.add(dir.scale(len));

                AABB searchBox = new AABB(mouthOrigin, beamEnd).inflate(1.35D);
                List<LivingEntity> hitEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> e != this && e.isAlive());

                for (LivingEntity hit : hitEntities) {
                    if (distanceToSegmentSq(hit.getBoundingBox().getCenter(), mouthOrigin, beamEnd) <= 1.45D) {
                        hit.hurtServer(level, this.damageSources().mobAttack(this), 5.5F);
                        hit.setRemainingFireTicks(40);
                        Vec3 push = new Vec3(dir.x, 0.08D, dir.z).normalize().scale(0.28D);
                        hit.push(push.x, push.y, push.z);
                    }
                }
            }
        }
    }

    private void tickSeismicSlam(ServerLevel level, LivingEntity target, int tick) {
        this.getNavigation().stop();
        if (tick <= 22 && target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (tick == 24) {
            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.65F);
            this.playSound(SoundEvents.ANVIL_LAND, 1.3F, 0.55F);

            Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
            Vec3 slamCenter = this.position().add(forward.scale(2.2D));
            syncTargetPos(slamCenter);

            AABB area = new AABB(slamCenter, slamCenter).inflate(6.5D, 3.0D, 6.5D);
            for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive())) {
                double d = Math.sqrt(hit.distanceToSqr(slamCenter));
                if (d <= 6.5D) {
                    float falloff = (float) Math.max(0.45D, 1.0D - (d / 7.5D));
                    hit.hurtServer(level, this.damageSources().mobAttack(this), 22.0F * falloff);
                    Vec3 away = hit.position().subtract(slamCenter);
                    away = away.lengthSqr() > 1.0E-4D ? away.normalize() : forward;
                    hit.setDeltaMovement(away.x * 1.15D, 0.62D, away.z * 1.15D);
                    hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                }
            }
        }
    }

    private void tickCrystalBarrage(ServerLevel level, LivingEntity target, int tick) {
        this.getNavigation().stop();
        if (tick <= 14 && target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.lockedTargetPos = target.position();
            syncTargetPos(this.lockedTargetPos);
        }

        if (tick == 15) {
            this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.8F, 0.55F);
            this.playSound(SoundEvents.TRIDENT_THUNDER.value(), 1.1F, 0.75F);
        }

        if (tick >= 15 && tick <= 32 && (tick - 15) % 3 == 0) {
            double progress = (tick - 15) / 17.0D;
            Vec3 wavePoint = this.position().lerp(this.lockedTargetPos, progress);
            double radius = progress > 0.85D ? 3.2D : 1.85D;

            AABB box = new AABB(wavePoint, wavePoint).inflate(radius, 2.5D, radius);
            for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
                hit.hurtServer(level, this.damageSources().mobAttack(this), progress > 0.85D ? 16.0F : 11.0F);
                hit.setDeltaMovement(hit.getDeltaMovement().x, 0.48D, hit.getDeltaMovement().z);
            }
        }
    }

    private void tickColossusLeap(ServerLevel level, LivingEntity target, int tick) {
        if (tick < 12) {
            this.getNavigation().stop();
            if (target != null && target.isAlive()) {
                this.getLookControl().setLookAt(target, 35.0F, 35.0F);
                this.lockedTargetPos = target.position();
                syncTargetPos(this.lockedTargetPos);
            }
            return;
        }

        if (tick == 12 && !leapLaunched) {
            leapLaunched = true;
            Vec3 toTarget = this.lockedTargetPos.subtract(this.position());
            Vec3 horiz = new Vec3(toTarget.x, 0.0D, toTarget.z);
            double dist = Math.min(20.0D, horiz.length());
            Vec3 dir = dist > 0.01D ? horiz.normalize() : Vec3.directionFromRotation(0.0F, this.getYRot());

            // vy = 1.38 atinge ~10.2 blocos de altura no ápice
            double flightTicks = 22.0D;
            double vHoriz = dist / flightTicks;
            this.setDeltaMovement(dir.x * vHoriz, 1.38D, dir.z * vHoriz);
            this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.6F, 0.50F);
        }

        if (leapLaunched && !leapLanded && tick >= 20 && (this.onGround() || tick >= 35)) {
            leapLanded = true;
            setAttackTick(35);
            syncTargetPos(this.position());

            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.8F, 0.55F);
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.5F, 0.60F);

            double shockRadius = 8.0D;
            AABB shockBox = this.getBoundingBox().inflate(shockRadius, 3.5D, shockRadius);
            for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, shockBox, e -> e != this && e.isAlive())) {
                double dist = Math.sqrt(hit.distanceToSqr(this.position()));
                if (dist <= shockRadius) {
                    float factor = (float) Math.max(0.35D, 1.0D - (dist / (shockRadius + 1.0D)));
                    hit.hurtServer(level, this.damageSources().mobAttack(this), 26.0F * factor);

                    Vec3 pushDir = hit.position().subtract(this.position());
                    pushDir = pushDir.lengthSqr() > 1.0E-4D
                            ? new Vec3(pushDir.x, 0.0D, pushDir.z).normalize()
                            : Vec3.directionFromRotation(0.0F, this.getYRot());
                    hit.setDeltaMovement(pushDir.x * (1.45D * factor), 0.72D * factor, pushDir.z * (1.45D * factor));
                }
            }
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        if (getAttackType() == AttackType.COLOSSUS_LEAP) {
            return false;
        }
        return super.causeFallDamage(fallDistance, damageMultiplier, source);
    }

    public Vec3 getMouthWorldPosition() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        return this.position().add(forward.x * 1.85D, 1.75D, forward.z * 1.85D);
    }

    private void syncTargetPos(Vec3 pos) {
        this.entityData.set(DATA_TARGET_X, (float) pos.x);
        this.entityData.set(DATA_TARGET_Y, (float) pos.y);
        this.entityData.set(DATA_TARGET_Z, (float) pos.z);
    }

    public AttackType getAttackType() {
        return AttackType.byId(this.entityData.get(DATA_ATTACK_TYPE));
    }

    public int getAttackTick() {
        return this.entityData.get(DATA_ATTACK_TICK);
    }

    private void setAttackTick(int tick) {
        this.entityData.set(DATA_ATTACK_TICK, tick);
    }

    public Vec3 getSyncedTargetPos() {
        return new Vec3(
                this.entityData.get(DATA_TARGET_X),
                this.entityData.get(DATA_TARGET_Y),
                this.entityData.get(DATA_TARGET_Z)
        );
    }

    private static double distanceToSegmentSq(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq < 1.0E-6D) return p.distanceToSqr(a);
        double t = Mth.clamp(p.subtract(a).dot(ab) / lenSq, 0.0D, 1.0D);
        Vec3 closest = a.add(ab.scale(t));
        return p.distanceToSqr(closest);
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.15F, 0.62F);
        this.playSound(SoundEvents.DEEPSLATE_STEP, 0.90F, 0.70F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    public float getCrystalGlowPulse(float partialTick) {
        float t = (this.tickCount + partialTick) * 0.09F;
        if (getAttackType() != AttackType.NONE) {
            return 1.0F;
        }
        return 0.82F + 0.18F * Mth.sin(t);
    }
}
