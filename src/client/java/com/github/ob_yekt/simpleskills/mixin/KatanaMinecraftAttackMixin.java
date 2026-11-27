package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientStanceState;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaStanceRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Triggers katana draw or 3-hit combo progression on a real left-click attack input. */
@Mixin(Minecraft.class)
public abstract class KatanaMinecraftAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void simpleskills$drawKatanaOnAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = (Minecraft) (Object) this;
        LocalPlayer player = client.player;
        if (player == null || !player.isAlive() || player.isSpectator() || player.isHandsBusy()
                || client.gui.screen() != null) {
            return;
        }

        var heldItem = player.getMainHandItem();
        if (!AbyssalKatanaItems.isKatana(heldItem.getItem())
                || !ClientPlayNetworking.canSend(KatanaStanceRequestPayload.TYPE)) {
            return;
        }

        KatanaClientStanceState.predictAttack(player.getUUID());
        ClientPlayNetworking.send(new KatanaStanceRequestPayload(KatanaStanceRequestPayload.ATTACK));
    }
}
