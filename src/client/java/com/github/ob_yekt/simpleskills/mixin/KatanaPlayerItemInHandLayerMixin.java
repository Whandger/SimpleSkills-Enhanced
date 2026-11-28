package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents vanilla from drawing a duplicate sword in the player's hand. */
@Mixin(PlayerItemInHandLayer.class)
public abstract class KatanaPlayerItemInHandLayerMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void simpleskills$hideVanillaKatana(AvatarRenderState state, ItemStackRenderState itemState,
                                                ItemStack itemStack, HumanoidArm arm, PoseStack poseStack,
                                                SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        if (state != null && state.mainArm == arm && itemStack != null
                && AbyssalKatanaItems.isKatana(itemStack.getItem())) {
            ci.cancel();
        }
    }
}
