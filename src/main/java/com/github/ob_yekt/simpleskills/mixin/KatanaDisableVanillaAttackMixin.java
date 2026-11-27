package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents any vanilla Player.attack call from bypassing the katana's server-side stance gate. */
@Mixin(Player.class)
public abstract class KatanaDisableVanillaAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void simpleskills$disableVanillaKatanaDamage(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) ci.cancel();
    }
}
