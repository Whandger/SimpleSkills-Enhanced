package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.runes.RuneEquipmentVfxRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Submits held-item VFX after the item's real display transform has been applied. */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class RuneItemStackLayerVfxMixin {
    @Inject(method = "submit", at = @At("TAIL"))
    private void simpleskills$submitHeldItemVfx(PoseStack poseStack,
                                                SubmitNodeCollector collector,
                                                int lightCoords,
                                                int overlayCoords,
                                                int outlineColor,
                                                CallbackInfo ci) {
        ItemStackLayerRenderStateAccessor layer = (ItemStackLayerRenderStateAccessor) (Object) this;
        ItemStackRenderState parent = layer.simpleskills$getParentState();
        ItemStackRenderStateAccessor state = (ItemStackRenderStateAccessor) (Object) parent;
        ItemStackRenderState.LayerRenderState[] layers = state.simpleskills$getLayers();
        if (layers.length == 0 || layers[0] != (Object) this) return;

        poseStack.pushPose();
        layer.simpleskills$applyTransform(poseStack.last());
        RuneEquipmentVfxRenderer.submitHeldItem(parent, poseStack, collector);
        poseStack.popPose();
    }
}
