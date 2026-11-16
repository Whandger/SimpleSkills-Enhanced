package com.github.ob_yekt.simpleskills.prayer;

import com.github.ob_yekt.simpleskills.Simpleskills;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Server-tracked visual carrier that triggers spell impact effects upon reaching its target. */
public final class PrayerVfxEntity extends Entity {
    public static void register() { /* Trigger class initialization before entity registries freeze. */ }
    public static final EntityType<PrayerVfxEntity> TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "prayer_vfx"),
            EntityType.Builder.<PrayerVfxEntity>of(PrayerVfxEntity::new, MobCategory.MISC).sized(0.05f, 0.05f)
                    .clientTrackingRange(64).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "prayer_vfx"))));
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(PrayerVfxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3fc> END = SynchedEntityData.defineId(PrayerVfxEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(PrayerVfxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(PrayerVfxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(PrayerVfxEntity.class, EntityDataSerializers.INT);

    private int life;
    private int maxLife = 24;

    // Server-side delayed impact payload
    private UUID casterUuid;
    private UUID targetUuid;
    private Vec3 impactPos;
    private PrayerAbility payloadAbility;
    private int prayerLevel;
    private int impactTick = -1;
    private boolean impactFired = false;

    // Client-side cache para manter a última posição caso o alvo morra no meio da animação
    private Vec3 lastKnownHandOffset = null;
    private Vec3 lastKnownEndOffset = null;

    public PrayerVfxEntity(EntityType<?> type, Level level) { super(type, level); }

    public PrayerVfxEntity(Level level, int style, Vec3 start, Vec3 end, int owner, int duration) {
        this(TYPE, level);
        setPos(start);
        entityData.set(STYLE, style);
        entityData.set(END, new Vector3f((float) (end.x - start.x), (float) (end.y - start.y), (float) (end.z - start.z)));
        entityData.set(OWNER, owner);
        entityData.set(TARGET, -1);
        entityData.set(DURATION, duration);
        maxLife = duration;
    }

    public void setSpellPayload(UUID casterUuid, UUID targetUuid, Vec3 impactPos,
                                PrayerAbility ability, int prayerLevel, int impactTick) {
        this.casterUuid = casterUuid;
        this.targetUuid = targetUuid;
        this.impactPos = impactPos;
        this.payloadAbility = ability;
        this.prayerLevel = prayerLevel;
        this.impactTick = impactTick;

        if (level() instanceof ServerLevel server) {
            if (targetUuid != null) {
                Entity target = server.getEntity(targetUuid);
                if (target != null) entityData.set(TARGET, target.getId());
            }
            if (casterUuid != null && ownerId() < 0) {
                Entity caster = server.getEntity(casterUuid);
                if (caster != null) entityData.set(OWNER, caster.getId());
            }
        }
    }

    public void setTargetId(int targetId) {
        entityData.set(TARGET, targetId);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STYLE, 0)
                .define(END, new Vector3f())
                .define(OWNER, -1)
                .define(TARGET, -1)
                .define(DURATION, 24);
    }

    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {}
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {}

    @Override public void tick() {
        super.tick();
        life++;
        maxLife = entityData.get(DURATION);

        if (!level().isClientSide() && level() instanceof ServerLevel server) {
            if (style() == 1 && ownerId() >= 0) {
                var owner = server.getEntity(ownerId());
                if (owner instanceof LivingEntity) setPos(owner.getX(), owner.getY(), owner.getZ());
            }

            // Mantém o alvo sincronizado e atualiza o ponto de impacto no servidor enquanto o projétil/raio voa
            if (targetUuid != null) {
                Entity target = server.getEntity(targetUuid);
                if (target != null && entityData.get(TARGET) != target.getId()) {
                    entityData.set(TARGET, target.getId());
                }
                if (!impactFired && target instanceof LivingEntity living && living.isAlive()) {
                    impactPos = living.position().add(0, living.getBbHeight() * 0.5, 0);
                    entityData.set(END, new Vector3f(
                            (float) (impactPos.x - getX()),
                            (float) (impactPos.y - getY()),
                            (float) (impactPos.z - getZ())
                    ));
                }
            }

            if (payloadAbility == PrayerAbility.SHADOW_BOLT && !impactFired) {
                tickShadowBolt(server);
            }

            if (!impactFired && payloadAbility != null && life >= impactTick) {
                impactFired = true;
                PrayerManager.executeDelayedImpact(server, casterUuid, targetUuid, impactPos, payloadAbility, prayerLevel);
            }

            if (life >= maxLife) discard();
        }
    }

    private void tickShadowBolt(ServerLevel server) {
        final Entity caster = casterUuid != null ? server.getEntity(casterUuid) : null;
        final LivingEntity primaryTarget = (targetUuid != null
                && server.getEntity(targetUuid) instanceof LivingEntity living
                && living.isAlive()) ? living : null;

        // 1. Mantém a orbe e o buraco negro acompanhando o centro do mob atingido
        if (primaryTarget != null) {
            impactPos = primaryTarget.position().add(0, primaryTarget.getBbHeight() * 0.5, 0);
            entityData.set(END, new Vector3f(
                    (float) (impactPos.x - getX()),
                    (float) (impactPos.y - getY()),
                    (float) (impactPos.z - getZ())
            ));
        }

        if (impactPos == null) return;

        // 2. Fase de Buraco Negro (entre 26% da duração e o tick de explosão)
        double progress = (double) life / Math.max(1, maxLife);
        if (progress >= 0.26 && life < impactTick) {
            // Prende o mob atingido no centro da singularidade enquanto não explode
            if (primaryTarget != null) {
                Vec3 targetCenter = primaryTarget.position().add(0, primaryTarget.getBbHeight() * 0.5, 0);
                Vec3 toCore = impactPos.subtract(targetCenter);
                Vec3 pinVel = toCore.lengthSqr() > 0.01 ? toCore.scale(0.45) : Vec3.ZERO;
                primaryTarget.setDeltaMovement(pinVel);
                primaryTarget.fallDistance = 0;
                primaryTarget.needsSync = true;
            }

            // Puxa todos os mobs numa área 4x4 ao redor do alvo atingido
            AABB suctionBox = AABB.ofSize(impactPos, 4.0, 3.5, 4.0);
            List<LivingEntity> nearbyMobs = server.getEntitiesOfClass(LivingEntity.class, suctionBox,
                    e -> e != caster && e != primaryTarget && e.isAlive() && !(e instanceof Player));

            for (LivingEntity mob : nearbyMobs) {
                Vec3 mobCenter = mob.position().add(0, mob.getBbHeight() * 0.5, 0);
                Vec3 pull = impactPos.subtract(mobCenter);
                double dist = pull.length();
                if (dist > 0.28) {
                    double strength = Math.min(0.42, 0.16 + (1.0 - Math.min(1.0, dist / 2.8)) * 0.22);
                    Vec3 dir = pull.normalize().scale(strength);
                    mob.setDeltaMovement(mob.getDeltaMovement().scale(0.35).add(dir.x, Math.max(dir.y, 0.04), dir.z));
                } else {
                    mob.setDeltaMovement(Vec3.ZERO);
                }
                mob.fallDistance = 0;
                mob.needsSync = true;
            }

            if (life % 2 == 0) {
                server.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        impactPos.x, impactPos.y, impactPos.z, 3, 1.4, 0.6, 1.4, 0.08);
            }
        }
    }

    public int style() { return entityData.get(STYLE); }
    public int ownerId() { return entityData.get(OWNER); }
    public int targetId() { return entityData.get(TARGET); }

    public Vec3 endOffset() {
        Vector3fc v = entityData.get(END);
        return new Vec3(v.x(), v.y(), v.z());
    }

    /** Posição interpolada do centro do alvo em tempo real (fallback para o ponto fixo caso não haja entidade). */
    public Vec3 endOffset(float partialTick) {
        int id = targetId();
        if (id >= 0) {
            Entity target = level().getEntity(id);
            if (target != null && target.isAlive()) {
                Vec3 targetCenter = target.getPosition(partialTick)
                        .add(0.0, target.getBbHeight() * 0.5, 0.0);
                lastKnownEndOffset = targetCenter.subtract(this.getPosition(partialTick));
                return lastKnownEndOffset;
            }
        }
        return lastKnownEndOffset != null ? lastKnownEndOffset : endOffset();
    }

    public Vec3 handOffset() {
        return handOffset(1.0f);
    }

    /** Posição interpolada da mão do conjurador frame a frame. */
    public Vec3 handOffset(float partialTick) {
        if (ownerId() >= 0) {
            Entity owner = level().getEntity(ownerId());
            if (owner instanceof LivingEntity living && living.isAlive()) {
                Vec3 look = living.getViewVector(partialTick);
                Vec3 horizLook = new Vec3(look.x, 0.0, look.z);
                Vec3 forward = horizLook.lengthSqr() > 1.0E-5 ? horizLook.normalize() : look.normalize();
                Vec3 side = forward.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() < 1.0E-5) side = new Vec3(1, 0, 0);
                else side = side.normalize();

                double hand = living.getMainArm() == HumanoidArm.LEFT ? -1.0 : 1.0;
                Vec3 position = living.getPosition(partialTick)
                        .add(0, living.getBbHeight() * 0.69 + look.y * 0.22, 0)
                        .add(side.scale(0.34 * hand))
                        .add(forward.scale(0.28));
                lastKnownHandOffset = position.subtract(this.getPosition(partialTick));
                return lastKnownHandOffset;
            }
        }
        return lastKnownHandOffset != null ? lastKnownHandOffset : Vec3.ZERO;
    }

    public int life() { return life; }
    public int maxLife() { return maxLife; }
    @Override public boolean shouldRender(double x, double y, double z) { return true; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeHitByProjectile() { return false; }
    @Override public boolean hurtServer(ServerLevel level,
            net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
}
