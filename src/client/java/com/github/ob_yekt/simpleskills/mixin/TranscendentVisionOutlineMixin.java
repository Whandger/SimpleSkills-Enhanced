package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.TranscendentVisionHud;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds an outline to mobs identified by Oracle Vision, including entities behind blocks. */
@Mixin(EntityRenderer.class)
public abstract class TranscendentVisionOutlineMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void simpleskills$applyOracleVisionOutline(
            Entity entity,
            EntityRenderState renderState,
            float partialTicks,
            CallbackInfo ci
    ) {
        if (TranscendentVisionHud.isOutlined(entity)) {
            renderState.outlineColor = 0xFF62DFFF;
        }
    }
}
