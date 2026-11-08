package com.github.ob_yekt.simpleskills.mixin.FISHING;

import com.github.ob_yekt.simpleskills.fishing.FishingConfig;
import com.github.ob_yekt.simpleskills.fishing.FishingHookDuck;
import com.github.ob_yekt.simpleskills.fishing.FishingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks the fishing minigame into the vanilla fishing bobber.
 *
 * <ul>
 *   <li>{@code catchingFish}: the vanilla "random timer" bite is replaced by real fish (cancelled
 *       while the new system is enabled, so the vanilla code can never hand out loot).</li>
 *   <li>{@code tick}: drives the session of this hook (server side only).</li>
 *   <li>{@code retrieve}: right-click starts / continues the fight instead of reeling in.</li>
 *   <li>{@code remove}: any way a hook can disappear ends its session safely.</li>
 * </ul>
 */
@Mixin(FishingHook.class)
public abstract class FishingHookFightMixin implements FishingHookDuck {

    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_BITING;

    @Inject(method = "catchingFish", at = @At("HEAD"), cancellable = true)
    private void simpleskills$replaceVanillaBite(BlockPos blockPos, CallbackInfo ci) {
        if (FishingConfig.isFightEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simpleskills$tickFishingSession(CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        if (!self.level().isClientSide()) {
            FishingManager.onHookTick(self);
        }
    }

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void simpleskills$retrieveDuringFishing(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook) (Object) this;
        if (!self.level().isClientSide() && FishingManager.onRetrieve(self)) {
            cir.setReturnValue(0); // consumed by the minigame: no loot, no durability, hook stays
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void simpleskills$endSessionOnRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        if (!self.level().isClientSide()) {
            FishingManager.onHookRemoved(self);
        }
    }

    @Override
    public void simpleskills$setBiting(boolean biting) {
        ((FishingHook) (Object) this).getEntityData().set(DATA_BITING, biting);
    }
}
