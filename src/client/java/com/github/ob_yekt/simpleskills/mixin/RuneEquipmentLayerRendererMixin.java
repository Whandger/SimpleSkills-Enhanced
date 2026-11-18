package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.runes.RuneVisualStyles;
import com.github.ob_yekt.simpleskills.runes.RuneVisualStylesClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies Rune visual styles to the separate renderer used for worn armor. */
@Mixin(EquipmentLayerRenderer.class)
public abstract class RuneEquipmentLayerRendererMixin {
    /**
     * Non-trimmed armor draws its glint as part of the armor cutout pass.
     * Keep the original operation available to other mixins and replace only
     * its RenderType when the equipment stack has a custom Rune style.
     */
    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCullGlint(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType simpleskills$useCustomArmorGlint(
            Identifier armorTexture,
            Operation<RenderType> original,
            @Local(argsOnly = true) ItemStack itemStack
    ) {
        int style = RuneVisualStyles.getStyle(itemStack);
        if (style == RuneVisualStyles.VANILLA) {
            return original.call(armorTexture);
        }

        RenderType custom = RuneVisualStylesClient.armorLayer(style, armorTexture);
        return custom != null ? custom : original.call(armorTexture);
    }

    /**
     * Trimmed armor uses a second, independent overlay pass after the trim.
     * Replace that pass separately so trimmed and non-trimmed armor both use
     * the selected style.
     */
    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;trimmedArmorGlint()Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType simpleskills$useCustomTrimmedArmorGlint(
            Operation<RenderType> original,
            @Local(argsOnly = true) ItemStack itemStack
    ) {
        int style = RuneVisualStyles.getStyle(itemStack);
        if (style == RuneVisualStyles.VANILLA) {
            return original.call();
        }

        RenderType custom = RuneVisualStylesClient.trimmedArmor(style);
        return custom != null ? custom : original.call();
    }
}
