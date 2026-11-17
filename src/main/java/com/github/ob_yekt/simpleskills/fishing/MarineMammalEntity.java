package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Ambient whale/orca entity. It is deliberately not registered in FishSpecies or fish loot tables. */
public final class MarineMammalEntity extends AbstractFish {
    private final boolean orca;

    public MarineMammalEntity(EntityType<? extends MarineMammalEntity> type, Level level) {
        super(type, level);
        this.orca = "orca".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
    }

    public boolean isOrca() { return orca; }

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
