package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.runes.RuneEquipmentVfxRenderer;
import com.github.ob_yekt.simpleskills.runes.RuneVisualStyles;
import com.github.ob_yekt.simpleskills.runes.RuneVisualStylesClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies custom Rune glints and procedural 3D VFX to the vanilla ShieldSpecialRenderer. */
@Mixin(ShieldSpecialRenderer.class)
public abstract class RuneShieldSpecialRendererMixin {
    @WrapOperation(
            method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySolidGlint(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            require = 0
    )
    private RenderType simpleskills$useCustomShieldGlint(
            Identifier texture,
            Operation<RenderType> original,
            @Local(argsOnly = true) @Nullable DataComponentMap components) {
        int style = RuneVisualStyles.getStyle(components);
        if (style == RuneVisualStyles.VANILLA) return original.call(texture);
        RenderType custom = RuneVisualStylesClient.shieldEntity(style, texture);
        return custom != null ? custom : original.call(texture);
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;patternedShieldGlint()Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            require = 0
    )
    private RenderType simpleskills$useCustomPatternedShieldGlint(
            Operation<RenderType> original,
            @Local(argsOnly = true) @Nullable DataComponentMap components) {
        int style = RuneVisualStyles.getStyle(components);
        if (style == RuneVisualStyles.VANILLA) return original.call();
        RenderType custom = RuneVisualStylesClient.patternedShield(style);
        return custom != null ? custom : original.call();
    }

    @Inject(
            method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At("TAIL")
    )
    private void simpleskills$submitCustomShieldVfx(
            @Nullable DataComponentMap components,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor,
            CallbackInfo ci) {
        RuneEquipmentVfxRenderer.submitShield(components, poseStack, submitNodeCollector, false);
    }
}
