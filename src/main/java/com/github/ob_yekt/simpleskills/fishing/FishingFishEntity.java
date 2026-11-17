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
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Shared base for the added catchable fish species.
 *
 * <p>Extending AbstractSchoolingFish reuses vanilla fish water physics, swim control,
 * navigation, wandering and group-following behaviour instead of approximating a school manually.
 * Bucket interaction is disabled because these species are not designed to be bucket items.</p>
 */
public final class FishingFishEntity extends AbstractSchoolingFish {
    public FishingFishEntity(EntityType<? extends FishingFishEntity> type, Level level) {
        super(type, level);
    }


    /** Maximum number of same-species fish that can remain in this natural school. */
    @Override
    public int getMaxSchoolSize() {
        FishSpecies species = FishSpecies.of(this);
        return species == null ? 4 : species.spawnProfile().maxSchoolSize();
    }

    /** Keep the natural spawn cluster consistent with the school limit. */
    @Override
    public int getMaxSpawnClusterSize() {
        return getMaxSchoolSize();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    private boolean usesSalmonSounds() {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
        return path.equals("trout") || path.equals("tuna")
                || path.equals("swordfish") || path.equals("mahi_mahi");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return usesSalmonSounds() ? SoundEvents.SALMON_AMBIENT : SoundEvents.COD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return usesSalmonSounds() ? SoundEvents.SALMON_HURT : SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return usesSalmonSounds() ? SoundEvents.SALMON_DEATH : SoundEvents.COD_DEATH;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return usesSalmonSounds() ? SoundEvents.SALMON_FLOP : SoundEvents.COD_FLOP;
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

    /** These fish intentionally cannot be picked up with a water bucket. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    /** Keep the rendered fish targetable by vanilla player and projectile hit tests. */
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
