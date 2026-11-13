package com.github.ob_yekt.simpleskills.prayer;

import com.github.ob_yekt.simpleskills.Simpleskills;
import java.util.UUID;
import net.minecraft.core.Registry;
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
import net.minecraft.world.level.Level;
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

    public PrayerVfxEntity(EntityType<?> type, Level level) { super(type, level); }

    public PrayerVfxEntity(Level level, int style, Vec3 start, Vec3 end, int owner, int duration) {
        this(TYPE, level);
        setPos(start);
        entityData.set(STYLE, style);
        entityData.set(END, new Vector3f((float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z)));
        entityData.set(OWNER, owner);
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
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STYLE, 0).define(END, new Vector3f()).define(OWNER, -1).define(DURATION, 24);
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

            if (!impactFired && payloadAbility != null && life >= impactTick) {
                impactFired = true;
                PrayerManager.executeDelayedImpact(server, casterUuid, targetUuid, impactPos, payloadAbility, prayerLevel);
            }

            if (life >= maxLife) discard();
        }
    }

    public int style() { return entityData.get(STYLE); }
    public Vec3 endOffset() { Vector3fc v = entityData.get(END); return new Vec3(v.x(), v.y(), v.z()); }
    public int ownerId() { return entityData.get(OWNER); }

    /** Current hand position relative to the fixed cast origin; the aimed endpoint remains world-anchored. */
    public Vec3 handOffset() {
        if (ownerId() < 0) return Vec3.ZERO;
        Entity owner = level().getEntity(ownerId());
        if (!(owner instanceof LivingEntity living)) return Vec3.ZERO;
        Vec3 look = living.getLookAngle().normalize();
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-5) side = new Vec3(1, 0, 0);
        else side = side.normalize();
        double hand = living.getMainArm() == HumanoidArm.LEFT ? -1.0 : 1.0;
        Vec3 position = living.position()
                .add(0, living.getBbHeight() * 0.69, 0)
                .add(side.scale(0.34 * hand))
                .add(look.scale(0.24));
        return position.subtract(this.position());
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
