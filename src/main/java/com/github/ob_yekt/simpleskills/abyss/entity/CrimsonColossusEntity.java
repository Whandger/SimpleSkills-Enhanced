package com.github.ob_yekt.simpleskills.abyss.entity;

import com.github.ob_yekt.simpleskills.SimpleSkillsSounds;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CrimsonColossusEntity extends Monster {
    public static final EntityType<CrimsonColossusEntity> TYPE = registerType();

    // ------------------------------------------------------------------------------------------
    // Timeline in ticks (20 ticks = 1 second). The client poses and VFX read these same constants,
    // so changing a number here keeps damage, animation and effects in sync.
    // ------------------------------------------------------------------------------------------
    public static final int SWIPE_IMPACT_TICK = 6;
    public static final int SWIPE_DURATION = 14;

    public static final int LASER_CHARGE_END = 22;
    public static final int LASER_FIRE_END = 60;
    public static final int LASER_DURATION = 72;

    public static final int SLAM_IMPACT_TICK = 16;
    public static final int SLAM_DURATION = 36;
    public static final double SLAM_REACH = 2.2D;

    public static final int BARRAGE_WAVE_START = 9;
    public static final int BARRAGE_WAVE_END = 24;
    public static final int BARRAGE_DURATION = 32;

    public static final int LEAP_LAUNCH_TICK = 10;
    /** Airtime for LEAP_LAUNCH_VY = 1.0 under Minecraft gravity (0.08/tick) and drag (0.98/tick). */
    public static final int LEAP_STEER_TICKS = 23;
    public static final int LEAP_MAX_AIR_TICKS = 36;
    public static final int LEAP_SPREAD_TICKS = 22;
    public static final int LEAP_DURATION = 80;
    public static final double LEAP_SPREAD_RADIUS = 8.0D;
    public static final double LEAP_LAUNCH_VY = 1.0D;
    public static final double LEAP_MAX_HSPEED = 1.25D;

    /** Mouth position relative to the Colossus base. The client layer uses the same numbers. */
    public static final double MOUTH_FORWARD = 2.2D;
    public static final double MOUTH_HEIGHT = 1.75D;

    /** How fast the beam can slide toward the chest, in blocks per tick. Slower than a sideways step. */
    private static final double LASER_TRACK_SPEED = 0.14D;
    private static final double LASER_REACH = 26.0D;
    private static final float LASER_DAMAGE = 3.5F;

    public enum AttackType {
        NONE(0, 0),
        CRYSTAL_LASER(1, LASER_DURATION),
        SEISMIC_SLAM(2, SLAM_DURATION),
        CRYSTAL_BARRAGE(3, BARRAGE_DURATION),
        COLOSSUS_LEAP(4, LEAP_DURATION),
        SWIPE(5, SWIPE_DURATION);

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
    /** Attack tick at which the leap touched the ground, or -1 while still airborne. */
    private static final EntityDataAccessor<Integer> DATA_IMPACT_TICK =
            SynchedEntityData.defineId(CrimsonColossusEntity.class, EntityDataSerializers.INT);

    private static final DustParticleOptions CRIMSON_ENERGY =
            new DustParticleOptions(0xF6384C, 1.45F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.translatable("entity.simpleskills.crimson_colossus"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    private int globalAttackCooldown = 30;
    private int laserCooldown = 0;
    private int slamCooldown = 0;
    private int barrageCooldown = 0;
    private int leapCooldown = 0;
    private int swipeCooldown = 0;

    private AttackType lastAttack = AttackType.NONE;
    /** Entity ids already struck by the current leap shock, so each target is hit once per landing. */
    private final Set<Integer> hitThisAttack = new HashSet<>();
    private boolean leapLaunched = false;
    private boolean leapLanded = false;
    private int leapLandedTick = -1;
    private Vec3 lockedTargetPos = Vec3.ZERO;

    public CrimsonColossusEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 350;
    }

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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_TYPE, AttackType.NONE.id);
        builder.define(DATA_ATTACK_TICK, 0);
        builder.define(DATA_TARGET_X, 0.0F);
        builder.define(DATA_TARGET_Y, 0.0F);
        builder.define(DATA_TARGET_Z, 0.0F);
        builder.define(DATA_IMPACT_TICK, -1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
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
        // Stalking replaces the old melee chase: the boss keeps a range of about six blocks,
        // circles the target and only closes in when it is too far away.
        this.goalSelector.addGoal(1, new StalkGoal());
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));

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
        tickCooldowns();

        // Below half health the boss recovers faster and attacks more often.
        boolean enraged = this.getHealth() <= this.getMaxHealth() * 0.5F;

        AttackType current = getAttackType();
        if (current != AttackType.NONE) {
            int tick = getAttackTick() + 1;
            setAttackTick(tick);
            tickActiveAttack(level, current, tick);
            if (isAttackOver(current, tick)) {
                finishAttack(current, enraged);
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Up close the boss swats first. Otherwise it reads the situation and picks a skill.
        if (flatDistanceTo(target) <= 3.2D && swipeCooldown <= 0 && this.random.nextFloat() < 0.7F) {
            startAttack(AttackType.SWIPE, target.position());
            return;
        }
        if (globalAttackCooldown <= 0 && this.random.nextFloat() < (enraged ? 0.55F : 0.32F)) {
            chooseAttack(target);
        }
    }

    private void tickCooldowns() {
        if (globalAttackCooldown > 0) globalAttackCooldown--;
        if (laserCooldown > 0) laserCooldown--;
        if (slamCooldown > 0) slamCooldown--;
        if (barrageCooldown > 0) barrageCooldown--;
        if (leapCooldown > 0) leapCooldown--;
        if (swipeCooldown > 0) swipeCooldown--;
    }

    /**
     * Weighs every skill that is usable right now against the current situation, then rolls among
     * them. Weights reward the answer to what the player is doing: standing still gets lasered,
     * sprinting gets leapt at or walled with the crystal barrage. Repeating the last skill is penalised
     * and a little noise keeps the choice from looking scripted.
     */
    private void chooseAttack(LivingEntity target) {
        double dist = flatDistanceTo(target);
        Vec3 velocity = target.getDeltaMovement();
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        boolean moving = speed > 0.12D;
        boolean standingStill = speed < 0.05D;
        boolean grounded = this.onGround();
        boolean lineOfSight = this.hasLineOfSight(target);

        AttackType[] options = {
                AttackType.SEISMIC_SLAM, AttackType.COLOSSUS_LEAP,
                AttackType.CRYSTAL_BARRAGE, AttackType.CRYSTAL_LASER
        };
        double[] score = new double[options.length];

        // Slam: the close-range punisher. Hits hardest on targets that stand still.
        if (dist <= 7.0D && slamCooldown <= 0 && grounded) {
            score[0] = 3.0D + (dist <= 3.6D ? 1.4D : 0.0D) + (standingStill ? 1.0D : 0.0D);
        }
        // Leap: closes the gap and homes in, so it answers players who run away.
        if (dist >= 5.5D && dist <= 22.0D && leapCooldown <= 0 && grounded) {
            score[1] = 2.2D + (dist >= 9.0D ? 0.8D : 0.0D) + (moving ? 1.8D : 0.0D);
        }
        // Crystal barrage: a ground wave that works best against someone sprinting across the arena.
        if (dist >= 4.5D && dist <= 24.0D && barrageCooldown <= 0 && grounded) {
            score[2] = 1.9D + (moving ? 1.6D : 0.0D) + (dist >= 8.0D ? 0.5D : 0.0D);
        }
        // Laser: long range and needs line of sight. Punishes players who stop to fight.
        if (dist >= 4.0D && dist <= 26.0D && laserCooldown <= 0 && lineOfSight) {
            score[3] = 2.3D + (standingStill ? 1.8D : 0.0D) + (dist >= 10.0D ? 0.5D : 0.0D);
        }

        double total = 0.0D;
        for (int i = 0; i < options.length; i++) {
            if (score[i] <= 0.0D) continue;
            if (options[i] == lastAttack) score[i] *= 0.2D;
            score[i] *= 0.75D + this.random.nextDouble() * 0.5D;
            total += score[i];
        }
        if (total <= 0.0D) return;

        double roll = this.random.nextDouble() * total;
        for (int i = 0; i < options.length; i++) {
            if (score[i] <= 0.0D) continue;
            roll -= score[i];
            if (roll <= 0.0D) {
                AttackType chosen = options[i];
                startAttack(chosen, chosen == AttackType.CRYSTAL_LASER ? chestPosition(target) : target.position());
                return;
            }
        }
    }

    private void startAttack(AttackType type, Vec3 aimPos) {
        this.getNavigation().stop();
        this.entityData.set(DATA_ATTACK_TYPE, type.id);
        this.entityData.set(DATA_ATTACK_TICK, 0);
        this.entityData.set(DATA_IMPACT_TICK, -1);
        this.lockedTargetPos = aimPos;
        syncTargetPos(aimPos);

        this.hitThisAttack.clear();
        this.leapLaunched = false;
        this.leapLanded = false;
        this.leapLandedTick = -1;
        this.lastAttack = type;

        switch (type) {
            case CRYSTAL_LASER -> this.playSound(SimpleSkillsSounds.COLOSSUS_LASER_CHARGE, 1.6F, 1.0F);
            case SEISMIC_SLAM -> this.playSound(SimpleSkillsSounds.COLOSSUS_AMBIENT, 1.4F, 0.6F);
            default -> {
            }
        }
    }

    private void finishAttack(AttackType finished, boolean enraged) {
        CrimsonColossusLighting.release(this.getUUID());
        this.entityData.set(DATA_ATTACK_TYPE, AttackType.NONE.id);
        this.entityData.set(DATA_ATTACK_TICK, 0);
        this.entityData.set(DATA_IMPACT_TICK, -1);
        this.hitThisAttack.clear();

        float k = enraged ? 0.7F : 1.0F;
        this.globalAttackCooldown = enraged ? 8 : 14;

        switch (finished) {
            case CRYSTAL_LASER -> this.laserCooldown = (int) (110 * k);
            case SEISMIC_SLAM -> this.slamCooldown = (int) (60 * k);
            case CRYSTAL_BARRAGE -> this.barrageCooldown = (int) (70 * k);
            case COLOSSUS_LEAP -> this.leapCooldown = (int) (90 * k);
            case SWIPE -> this.swipeCooldown = enraged ? 16 : 22;
            default -> {
            }
        }
    }

    private boolean isAttackOver(AttackType type, int tick) {
        if (type == AttackType.COLOSSUS_LEAP && this.leapLanded) {
            return tick >= this.leapLandedTick + LEAP_SPREAD_TICKS + 6;
        }
        return tick >= type.duration;
    }

    private void tickActiveAttack(ServerLevel level, AttackType type, int tick) {
        LivingEntity target = this.getTarget();
        updateAttackLight(level, type, tick);
        switch (type) {
            case CRYSTAL_LASER -> tickCrystalLaser(level, target, tick);
            case SEISMIC_SLAM -> tickSeismicSlam(level, target, tick);
            case CRYSTAL_BARRAGE -> tickCrystalBarrage(level, target, tick);
            case COLOSSUS_LEAP -> tickColossusLeap(level, target, tick);
            case SWIPE -> tickSwipe(level, target, tick);
            default -> {
            }
        }
    }

    private void tickCrystalLaser(ServerLevel level, LivingEntity target, int tick) {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);

        if (target != null && target.isAlive()) {
            Vec3 chest = chestPosition(target);
            if (tick < LASER_CHARGE_END) {
                // Telegraph: the aim settles onto the chest slowly, so the player can see where it is heading.
                this.getLookControl().setLookAt(target, 24.0F, 24.0F);
                this.lockedTargetPos = this.lockedTargetPos.lerp(chest, 0.12D);
            } else if (tick < LASER_FIRE_END) {
                // Firing: the aim can only slide LASER_TRACK_SPEED blocks per tick. It chases the player,
                // but a sideways step takes them out of the line.
                this.getLookControl().setLookAt(target, 20.0F, 20.0F);
                Vec3 step = chest.subtract(this.lockedTargetPos);
                double len = step.length();
                if (len > LASER_TRACK_SPEED) {
                    step = step.scale(LASER_TRACK_SPEED / len);
                }
                this.lockedTargetPos = this.lockedTargetPos.add(step);
            }
            syncTargetPos(this.lockedTargetPos);
        }

        if (tick == LASER_CHARGE_END) {
            this.playSound(SimpleSkillsSounds.COLOSSUS_LASER_FIRE, 1.6F, 1.0F);
        }

        if (tick >= LASER_CHARGE_END && tick < LASER_FIRE_END) {
            if ((tick - LASER_CHARGE_END) % 6 == 0) {
                fireLaserSegment(level);
            }
        }
    }

    /** Ponta do feixe: a partir da boca, na direção da mira, com alcance máximo de LASER_REACH. */
    private Vec3 laserBeamEnd(Vec3 mouth) {
        Vec3 toAim = this.lockedTargetPos.subtract(mouth);
        double len = Mth.clamp(toAim.length(), 0.5D, LASER_REACH);
        Vec3 dir = toAim.lengthSqr() > 1.0E-6D
                ? toAim.normalize()
                : Vec3.directionFromRotation(0.0F, this.yBodyRot);
        return mouth.add(dir.scale(len));
    }

    private void fireLaserSegment(ServerLevel level) {
        Vec3 mouth = getMouthWorldPosition();
        Vec3 beamEnd = laserBeamEnd(mouth);
        Vec3 dir = beamEnd.subtract(mouth).normalize();

        AABB searchBox = new AABB(mouth, beamEnd).inflate(1.5D);
        List<LivingEntity> hitEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != this && e.isAlive());
        for (LivingEntity hit : hitEntities) {
            if (distanceToSegmentSq(chestPosition(hit), mouth, beamEnd) <= 1.25D * 1.25D) {
                hit.hurtServer(level, this.damageSources().mobAttack(this), LASER_DAMAGE);
                hit.setRemainingFireTicks(40);
                Vec3 push = new Vec3(dir.x, 0.08D, dir.z);
                push = push.lengthSqr() > 1.0E-6D ? push.normalize().scale(0.28D) : Vec3.ZERO;
                hit.push(push.x, push.y, push.z);
            }
        }
    }

    private void tickSeismicSlam(ServerLevel level, LivingEntity target, int tick) {
        this.getNavigation().stop();
        if (tick < SLAM_IMPACT_TICK && target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (tick != SLAM_IMPACT_TICK) {
            return;
        }

        this.playSound(SimpleSkillsSounds.COLOSSUS_SLAM, 1.6F, 1.0F);

        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        Vec3 slamCenter = this.position().add(forward.scale(SLAM_REACH));
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

    private void tickCrystalBarrage(ServerLevel level, LivingEntity target, int tick) {
        this.getNavigation().stop();
        if (tick < BARRAGE_WAVE_START && target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.lockedTargetPos = target.position();
            syncTargetPos(this.lockedTargetPos);
        }

        if (tick == BARRAGE_WAVE_START) {
            this.playSound(SimpleSkillsSounds.COLOSSUS_BARRAGE, 1.5F, 1.0F);
        }

        if (tick >= BARRAGE_WAVE_START && tick <= BARRAGE_WAVE_END && (tick - BARRAGE_WAVE_START) % 3 == 0) {
            double progress = (tick - BARRAGE_WAVE_START) / (double) (BARRAGE_WAVE_END - BARRAGE_WAVE_START);
            Vec3 wavePoint = this.position().lerp(this.lockedTargetPos, progress);
            double radius = progress > 0.85D ? 3.0D : 1.9D;

            AABB box = new AABB(wavePoint, wavePoint).inflate(radius, 2.5D, radius);
            for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
                hit.hurtServer(level, this.damageSources().mobAttack(this), progress > 0.85D ? 16.0F : 11.0F);
                hit.setDeltaMovement(hit.getDeltaMovement().x, 0.48D, hit.getDeltaMovement().z);
            }
        }
    }

    private void tickColossusLeap(ServerLevel level, LivingEntity target, int tick) {
        if (tick < LEAP_LAUNCH_TICK) {
            this.getNavigation().stop();
            if (target != null && target.isAlive()) {
                this.getLookControl().setLookAt(target, 35.0F, 35.0F);
                this.lockedTargetPos = target.position();
                syncTargetPos(this.lockedTargetPos);
            }
            return;
        }

        if (!this.leapLaunched) {
            this.leapLaunched = true;
            this.setDeltaMovement(0.0D, LEAP_LAUNCH_VY, 0.0D);
            this.playSound(SimpleSkillsSounds.COLOSSUS_LEAP_LAUNCH, 1.6F, 1.0F);
        }

        if (!this.leapLanded) {
            int air = tick - LEAP_LAUNCH_TICK;

            // Homing: each airborne tick the jump re-aims at the player's live position.
            if (target != null && target.isAlive()) {
                this.lockedTargetPos = target.position();
                syncTargetPos(this.lockedTargetPos);
            }
            steerLeap(air);

            if ((air >= 4 && this.onGround()) || air >= LEAP_MAX_AIR_TICKS) {
                landLeap(tick);
            }
        }

        if (this.leapLanded) {
            spreadLeapShock(level, tick);
        }
    }

    /** Sets the horizontal velocity so the boss arrives over the target roughly when the jump ends. */
    private void steerLeap(int air) {
        int remaining = Math.max(4, LEAP_STEER_TICKS - air);
        double vx = (this.lockedTargetPos.x - this.getX()) / remaining;
        double vz = (this.lockedTargetPos.z - this.getZ()) / remaining;
        double speed = Math.sqrt(vx * vx + vz * vz);
        if (speed > LEAP_MAX_HSPEED) {
            vx *= LEAP_MAX_HSPEED / speed;
            vz *= LEAP_MAX_HSPEED / speed;
        }
        this.setDeltaMovement(vx, this.getDeltaMovement().y, vz);
    }

    private void landLeap(int tick) {
        this.leapLanded = true;
        this.leapLandedTick = tick;
        this.entityData.set(DATA_IMPACT_TICK, tick);
        syncTargetPos(this.position());
        this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);

        this.playSound(SimpleSkillsSounds.COLOSSUS_LEAP_LAND, 1.8F, 1.0F);
    }

    /**
     * The shock front spreads outward from the exact point of landing. Anything the front passes is
     * hit once, so a player standing near the landing has to get clear before the front reaches them.
     */
    private void spreadLeapShock(ServerLevel level, int tick) {
        double front = leapFrontRadius(tick - this.leapLandedTick);
        Vec3 center = this.position();
        AABB box = new AABB(center, center).inflate(LEAP_SPREAD_RADIUS + 1.5D, 2.5D, LEAP_SPREAD_RADIUS + 1.5D);

        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
            if (this.hitThisAttack.contains(hit.getId())) continue;

            double dx = hit.getX() - center.x;
            double dz = hit.getZ() - center.z;
            double edge = Math.sqrt(dx * dx + dz * dz) - hit.getBbWidth() * 0.5D;
            if (edge > front + 0.6D) continue;

            this.hitThisAttack.add(hit.getId());
            double falloff = Math.max(0.45D, 1.0D - edge / (LEAP_SPREAD_RADIUS + 1.0D));
            hit.hurtServer(level, this.damageSources().mobAttack(this), (float) (22.0D * falloff));

            Vec3 out = new Vec3(dx, 0.0D, dz);
            out = out.lengthSqr() > 1.0E-4D ? out.normalize() : Vec3.directionFromRotation(0.0F, this.yBodyRot);
            hit.setDeltaMovement(out.x * 1.1D * falloff, 0.55D * falloff, out.z * 1.1D * falloff);
            hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1));
        }
    }

    /**
     * Radius of the leap shock front, in blocks, a given number of ticks after landing.
     * Public so the client VFX grows its rings on exactly the same curve.
     */
    public static double leapFrontRadius(double sinceLandingTicks) {
        if (sinceLandingTicks < 0.0D) return 0.0D;
        double p = Mth.clamp(sinceLandingTicks / LEAP_SPREAD_TICKS, 0.0D, 1.0D);
        return LEAP_SPREAD_RADIUS * (1.0D - (1.0D - p) * (1.0D - p));
    }

    private void tickSwipe(ServerLevel level, LivingEntity target, int tick) {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        if (tick < SWIPE_IMPACT_TICK && target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (tick != SWIPE_IMPACT_TICK) {
            return;
        }

        this.playSound(SimpleSkillsSounds.COLOSSUS_SWIPE, 1.1F, 1.0F);
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        AABB box = new AABB(this.position(), this.position()).inflate(3.4D, 2.0D, 3.4D);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
            Vec3 toHit = new Vec3(hit.getX() - this.getX(), 0.0D, hit.getZ() - this.getZ());
            if (toHit.lengthSqr() < 1.0E-4D) continue;
            Vec3 dir = toHit.normalize();
            if (dir.dot(forward) < 0.15D) continue;

            hit.hurtServer(level, this.damageSources().mobAttack(this), 12.0F);
            hit.push(dir.x * 0.8D, 0.35D, dir.z * 0.8D);
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        if (getAttackType() == AttackType.COLOSSUS_LEAP) {
            return false;
        }
        return super.causeFallDamage(fallDistance, damageMultiplier, source);
    }

    /** World position of the mouth, used by the server laser and mirrored by the client beam. */
    public Vec3 getMouthWorldPosition() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        return this.position().add(mouthOffset(forward));
    }

    /** Mouth offset from the Colossus base for a given body-forward direction. */
    public static Vec3 mouthOffset(Vec3 forward) {
        return forward.scale(MOUTH_FORWARD).add(0.0D, MOUTH_HEIGHT, 0.0D);
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

    /** Tick at which the current leap landed, or -1 while still airborne. */
    public int getImpactTick() {
        return this.entityData.get(DATA_IMPACT_TICK);
    }

    public Vec3 getSyncedTargetPos() {
        return new Vec3(
                this.entityData.get(DATA_TARGET_X),
                this.entityData.get(DATA_TARGET_Y),
                this.entityData.get(DATA_TARGET_Z)
        );
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
    public void remove(Entity.RemovalReason reason) {
        CrimsonColossusLighting.release(this.getUUID());
        super.remove(reason);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SimpleSkillsSounds.COLOSSUS_STEP, 1.2F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SimpleSkillsSounds.COLOSSUS_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SimpleSkillsSounds.COLOSSUS_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SimpleSkillsSounds.COLOSSUS_DEATH;
    }

    public float getCrystalGlowPulse(float partialTick) {
        float t = (this.tickCount + partialTick) * 0.09F;
        if (getAttackType() != AttackType.NONE) {
            return 1.0F;
        }
        return 0.82F + 0.18F * Mth.sin(t);
    }

    private static Vec3 chestPosition(Entity entity) {
        return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.62D, entity.getZ());
    }

    private double flatDistanceTo(Entity other) {
        double dx = other.getX() - this.getX();
        double dz = other.getZ() - this.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double distanceToSegmentSq(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq < 1.0E-6D) return p.distanceToSqr(a);
        double t = Mth.clamp(p.subtract(a).dot(ab) / lenSq, 0.0D, 1.0D);
        Vec3 closest = a.add(ab.scale(t));
        return p.distanceToSqr(closest);
    }

    // ------------------------------------------------------------------------------------------
    // Luz dos ataques. Cada ataque manda pontos para CrimsonColossusLighting, que os transforma em
    // blocos de luz reais. A cada 2 ticks, só para não recalcular a luz do mundo a cada tick.
    // ------------------------------------------------------------------------------------------

    /** Espaçamento entre os blocos de luz ao longo do feixe. Com nível alto, cobre o caminho inteiro. */
    private static final double LIGHT_SPACING = 3.0D;

    private void updateAttackLight(ServerLevel level, AttackType type, int tick) {
        if (tick % 2 != 0) return;
        List<CrimsonColossusLighting.Glow> glows = new ArrayList<>();
        switch (type) {
            case CRYSTAL_LASER -> laserGlows(glows, tick);
            case SEISMIC_SLAM -> slamGlows(glows, tick);
            case CRYSTAL_BARRAGE -> barrageGlows(glows, tick);
            case COLOSSUS_LEAP -> leapGlows(glows, tick);
            case SWIPE -> swipeGlows(glows, tick);
            default -> {
            }
        }
        CrimsonColossusLighting.update(this.getUUID(), level, glows);
    }

    /** Carregamento: sobe até o máximo no disparo. Disparo: brilho cheio com leve oscilação. Fim: apaga. */
    private void laserGlows(List<CrimsonColossusLighting.Glow> out, int tick) {
        int lv;
        if (tick < LASER_CHARGE_END) {
            lv = (int) Math.round(1.0D + 14.0D * (tick + 1) / (double) LASER_CHARGE_END);
        } else if (tick < LASER_FIRE_END) {
            lv = (tick / 6) % 2 == 0 ? 15 : 13;
        } else {
            double left = (LASER_DURATION - tick) / (double) (LASER_DURATION - LASER_FIRE_END);
            lv = (int) Math.round(15.0D * Mth.clamp(left, 0.0D, 1.0D));
        }
        if (lv < 1) return;
        Vec3 mouth = getMouthWorldPosition();
        addAlongSegment(out, mouth, laserBeamEnd(mouth), lv);
    }

    /** Antes do golpe: a luz do punho cresce. Depois: estouro no centro que se apaga, com um anel em volta. */
    private void slamGlows(List<CrimsonColossusLighting.Glow> out, int tick) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        Vec3 center = this.position().add(forward.scale(SLAM_REACH)).add(0.0D, 0.5D, 0.0D);
        if (tick < SLAM_IMPACT_TICK) {
            int lv = (int) Math.round(1.0D + 9.0D * tick / (double) SLAM_IMPACT_TICK);
            out.add(new CrimsonColossusLighting.Glow(center, lv));
            return;
        }
        int lv = (int) Math.round(15.0D * Mth.clamp(1.0D - (tick - SLAM_IMPACT_TICK) / 14.0D, 0.0D, 1.0D));
        if (lv < 1) return;
        out.add(new CrimsonColossusLighting.Glow(center, lv));
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI * 0.5D;
            out.add(new CrimsonColossusLighting.Glow(
                    center.add(Math.cos(a) * 3.5D, 0.0D, Math.sin(a) * 3.5D), Math.max(1, lv - 4)));
        }
    }

    /** Uma luz forte viaja com a onda de cristais, da frente do chefe até o alvo, e se apaga depois. */
    private void barrageGlows(List<CrimsonColossusLighting.Glow> out, int tick) {
        Vec3 base = this.position().add(0.0D, 1.2D, 0.0D);
        Vec3 goal = this.lockedTargetPos.add(0.0D, 1.2D, 0.0D);
        if (tick >= BARRAGE_WAVE_START && tick <= BARRAGE_WAVE_END) {
            double progress = Mth.clamp((tick - BARRAGE_WAVE_START)
                    / (double) (BARRAGE_WAVE_END - BARRAGE_WAVE_START), 0.0D, 1.0D);
            out.add(new CrimsonColossusLighting.Glow(base.lerp(goal, progress), 14));
            out.add(new CrimsonColossusLighting.Glow(base.lerp(goal, Math.max(0.0D, progress - 0.25D)), 8));
        } else if (tick > BARRAGE_WAVE_END) {
            int lv = (int) Math.round(10.0D * Mth.clamp(1.0D - (tick - BARRAGE_WAVE_END) / 8.0D, 0.0D, 1.0D));
            if (lv >= 1) out.add(new CrimsonColossusLighting.Glow(goal, lv));
        }
    }

    /** No ar, o corpo brilha. Ao pousar, um flash forte no ponto de impacto que se apaga com a onda de choque. */
    private void leapGlows(List<CrimsonColossusLighting.Glow> out, int tick) {
        if (!this.leapLanded) {
            if (this.leapLaunched) {
                out.add(new CrimsonColossusLighting.Glow(this.position().add(0.0D, 2.0D, 0.0D), 11));
            }
            return;
        }
        int since = tick - this.leapLandedTick;
        int lv = (int) Math.round(15.0D * Mth.clamp(1.0D - since / (double) (LEAP_SPREAD_TICKS + 6), 0.0D, 1.0D));
        if (lv < 1) return;
        Vec3 c = this.position().add(0.0D, 0.5D, 0.0D);
        out.add(new CrimsonColossusLighting.Glow(c, lv));
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI * 0.5D;
            out.add(new CrimsonColossusLighting.Glow(
                    c.add(Math.cos(a) * 4.0D, 0.0D, Math.sin(a) * 4.0D), Math.max(1, lv - 3)));
        }
    }

    /** Um clarão curto na frente do chefe, no impacto da garra. */
    private void swipeGlows(List<CrimsonColossusLighting.Glow> out, int tick) {
        if (tick < SWIPE_IMPACT_TICK) return;
        int lv = Math.max(0, 12 - (tick - SWIPE_IMPACT_TICK) * 2);
        if (lv < 1) return;
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        out.add(new CrimsonColossusLighting.Glow(
                this.position().add(forward.scale(2.5D)).add(0.0D, 1.6D, 0.0D), lv));
    }

    /** Pontos de luz espaçados ao longo do segmento a-b, incluindo as duas pontas. */
    private static void addAlongSegment(List<CrimsonColossusLighting.Glow> out, Vec3 a, Vec3 b, int lv) {
        int steps = Math.max(1, (int) Math.ceil(a.distanceTo(b) / LIGHT_SPACING));
        for (int i = 0; i <= steps; i++) {
            out.add(new CrimsonColossusLighting.Glow(a.lerp(b, i / (double) steps), lv));
        }
    }

    /**
     * Keeps the boss at a stalking distance of about six blocks. It closes in when the target is far,
     * backs off when it is too close, and otherwise orbits, changing direction now and then.
     */
    private final class StalkGoal extends Goal {
        private int repathTicks;
        private int orbitTicks;
        private double orbitSign = 1.0D;

        StalkGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = CrimsonColossusEntity.this.getTarget();
            return CrimsonColossusEntity.this.getAttackType() == AttackType.NONE && t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.repathTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = CrimsonColossusEntity.this.getTarget();
            if (t == null) return;
            if (--this.repathTicks > 0) return;
            this.repathTicks = 6;

            if (--this.orbitTicks <= 0) {
                this.orbitTicks = 50 + CrimsonColossusEntity.this.random.nextInt(40);
                this.orbitSign = CrimsonColossusEntity.this.random.nextBoolean() ? 1.0D : -1.0D;
            }

            double dist = CrimsonColossusEntity.this.flatDistanceTo(t);
            Vec3 away = new Vec3(
                    CrimsonColossusEntity.this.getX() - t.getX(), 0.0D,
                    CrimsonColossusEntity.this.getZ() - t.getZ());
            away = away.lengthSqr() > 1.0E-4D
                    ? away.normalize()
                    : Vec3.directionFromRotation(0.0F, CrimsonColossusEntity.this.getYRot() + 180.0F);

            Vec3 spot;
            double speed;
            if (dist > 8.0D) {
                spot = t.position();
                speed = 1.2D;
            } else if (dist < 4.5D) {
                spot = t.position().add(away.scale(6.0D));
                speed = 1.0D;
            } else {
                Vec3 tangent = new Vec3(-away.z, 0.0D, away.x).scale(this.orbitSign);
                spot = t.position().add(away.scale(6.0D)).add(tangent.scale(2.5D));
                speed = 0.85D;
            }
            CrimsonColossusEntity.this.getNavigation().moveTo(spot.x, spot.y, spot.z, speed);
        }
    }
}
