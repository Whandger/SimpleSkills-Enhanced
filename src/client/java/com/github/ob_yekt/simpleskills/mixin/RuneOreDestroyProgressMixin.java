
package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.runes.RuneOreBlock;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class RuneOreDestroyProgressMixin {

    @Inject(
        method = "submitBlockDestroyAnimation",
        at = @At("HEAD")
    )
    private void simpleskills$removeRuneOreVanillaCracks(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LevelRenderState levelRenderState,
            CallbackInfo ci
    ) {
        levelRenderState.blockBreakingRenderStates.removeIf(
            breakingState ->
                breakingState.blockState().getBlock() instanceof RuneOreBlock
        );
    }
}
