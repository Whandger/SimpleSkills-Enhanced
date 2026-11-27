package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hides vanilla attack cooldown indicators only while the custom katana is in the main hand. */
@Mixin(Hud.class)
public abstract class KatanaAttackIndicatorHudMixin {
    @ModifyExpressionValue(
            method = "extractCrosshair",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object simpleskills$hideCrosshairAttackCooldown(Object original) {
        return simpleskills$katanaEquipped() ? AttackIndicatorStatus.OFF : original;
    }

    @ModifyExpressionValue(
            method = "extractItemHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object simpleskills$hideHotbarAttackCooldown(Object original) {
        return simpleskills$katanaEquipped() ? AttackIndicatorStatus.OFF : original;
    }

    private static boolean simpleskills$katanaEquipped() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && AbyssalKatanaItems.isKatana(client.player.getMainHandItem().getItem());
    }
}
