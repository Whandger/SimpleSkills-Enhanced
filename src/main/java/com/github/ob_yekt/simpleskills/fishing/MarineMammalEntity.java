package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Ambient whale/orca entity. It is deliberately not registered in FishSpecies or fish loot tables. */
public final class MarineMammalEntity extends AbstractFish implements AquaticSizeHolder {
    private static final EntityDataAccessor<Float> DATA_AQUATIC_SCALE =
            SynchedEntityData.defineId(MarineMammalEntity.class, EntityDataSerializers.FLOAT);
    private static final FishSizeModel.Profile ORCA_SIZE =
            new FishSizeModel.Profile(450.0, 900.0, 1.6e-5, 3.0);
    private static final FishSizeModel.Profile HUMPBACK_SIZE =
            new FishSizeModel.Profile(1100.0, 1600.0, 1.2e-5, 3.0);

    private final boolean orca;

    public MarineMammalEntity(EntityType<? extends MarineMammalEntity> type, Level level) {
        super(type, level);
        this.orca = "orca".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
    }

    public boolean isOrca() { return orca; }

    public FishSizeModel.Profile sizeProfile() {
        return orca ? ORCA_SIZE : HUMPBACK_SIZE;
    }

    /** Orcas are scaled up substantially; humpbacks remain the largest of the two mammals. */
    public float getBaseVisualScale() {
        return orca ? 1.90F : 2.10F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AQUATIC_SCALE, 1.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_AQUATIC_SCALE.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    @Override
    public float simpleskills$getAquaticScale() {
        return this.entityData.get(DATA_AQUATIC_SCALE);
    }

    @Override
    public void simpleskills$setAquaticScale(float scale) {
        if (this.level().isClientSide()) return;
        float clamped = Math.max(1.25F, Math.min(3.15F, scale));
        if (Math.abs(this.entityData.get(DATA_AQUATIC_SCALE) - clamped) < 0.001F) return;
        this.entityData.set(DATA_AQUATIC_SCALE, clamped);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(this.simpleskills$getAquaticScale());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.16D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.orca && !level().isClientSide() && tickCount % 160 == 0 && level() instanceof ServerLevel server) {
            BlockPos above = blockPosition().above();
            if (!server.getFluidState(above).is(FluidTags.WATER)) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + getBbHeight() * 0.85,
                        getZ(), 5, 0.25, 0.12, 0.25, 0.015);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DOLPHIN_AMBIENT_WATER;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    /** AbstractFish implements Bucketable; these ambient species deliberately have no bucket item. */
    @Override
    public ItemStack getBucketItemStack() {
        return ItemStack.EMPTY;
    }

    /** Explicitly reject bucket pickup even if another interaction path checks Bucketable directly. */
    @Override
    public boolean canBePickedUpWithBucket(ItemStack bucket) {
        return false;
    }

    /** Marine mammals are ambient fauna, not bucket-capturable fish. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return this.isAlive();
    }

    @Override
    public boolean isAttackable() {
        return this.isAlive();
    }

    @Override
    public boolean canBeHitByProjectile() {
        return this.isAlive();
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }
}
