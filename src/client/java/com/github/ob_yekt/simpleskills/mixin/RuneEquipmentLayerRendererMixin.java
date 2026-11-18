package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.runes.RuneEquipmentVfxRenderer;
import com.github.ob_yekt.simpleskills.runes.RuneVisualStyles;
import com.github.ob_yekt.simpleskills.runes.RuneVisualStylesClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies custom glints and procedural VFX to the independent worn-armor renderer. */
@Mixin(EquipmentLayerRenderer.class)
public abstract class RuneEquipmentLayerRendererMixin {
    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCullGlint(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType simpleskills$useCustomArmorGlint(
            Identifier armorTexture, Operation<RenderType> original, @Local(argsOnly = true) ItemStack itemStack) {
        int style = RuneVisualStyles.getStyle(itemStack);
        if (style == RuneVisualStyles.VANILLA) return original.call(armorTexture);
        RenderType custom = RuneVisualStylesClient.armorLayer(style, armorTexture);
        return custom != null ? custom : original.call(armorTexture);
    }

    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;trimmedArmorGlint()Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType simpleskills$useCustomTrimmedArmorGlint(
            Operation<RenderType> original, @Local(argsOnly = true) ItemStack itemStack) {
        int style = RuneVisualStyles.getStyle(itemStack);
        if (style == RuneVisualStyles.VANILLA) return original.call();
        RenderType custom = RuneVisualStylesClient.trimmedArmor(style);
        return custom != null ? custom : original.call();
    }

    @Inject(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At("TAIL")
    )
    private <S> void simpleskills$submitCustomArmorVfx(
            EquipmentClientInfo.LayerType layerType,
            ResourceKey<EquipmentAsset> equipmentAssetId,
            Model<? super S> model,
            S state,
            ItemStack itemStack,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            Identifier playerTextureOverride,
            int outlineColor,
            int order,
            CallbackInfo ci) {
        RuneEquipmentVfxRenderer.submitArmor(itemStack, model, poseStack, collector);
    }
}
