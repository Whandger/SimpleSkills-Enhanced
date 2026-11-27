package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKingItems;
import com.github.ob_yekt.simpleskills.abyss.AbyssalSamuraiArmorModel;
import com.github.ob_yekt.simpleskills.abyss.AbyssalSamuraiArmorModel.Piece;
import com.github.ob_yekt.simpleskills.abyss.AbyssalSamuraiArmorPoser;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Troca as peças da armadura do Samurai pelos modelos próprios, já posados para o jogador. */
@Mixin(EquipmentLayerRenderer.class)
public abstract class AbyssalSamuraiArmorRendererMixin {
    private static final String RENDER_LAYERS =
            "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"
                    + "Lnet/minecraft/resources/ResourceKey;"
                    + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                    + "Lnet/minecraft/world/item/ItemStack;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                    + "ILnet/minecraft/resources/Identifier;II)V";

    @ModifyVariable(method = RENDER_LAYERS, at = @At("HEAD"), argsOnly = true)
    private Model<?> simpleskills$useSamuraiArmorModel(
            Model<?> originalModel,
            @Local(argsOnly = true) EquipmentClientInfo.LayerType layerType,
            @Local(argsOnly = true) ItemStack itemStack,
            @Local(argsOnly = true) Object state) {
        Piece piece = pieceFor(layerType, itemStack);
        if (piece == null) return originalModel;

        AbyssalSamuraiArmorModel samuraiModel = null;
        if (state instanceof AvatarRenderState avatar) {
            samuraiModel = AbyssalSamuraiArmorPoser.forPlayer(piece, avatar);
        } else if (originalModel instanceof HumanoidModel<?> humanoid) {
            samuraiModel = AbyssalSamuraiArmorPoser.forHumanoid(piece, humanoid);
        }
        return samuraiModel != null ? samuraiModel : originalModel;
    }

    @ModifyVariable(method = RENDER_LAYERS, at = @At("HEAD"), argsOnly = true)
    private Identifier simpleskills$useSamuraiArmorTexture(
            Identifier originalTexture,
            @Local(argsOnly = true) EquipmentClientInfo.LayerType layerType,
            @Local(argsOnly = true) ItemStack itemStack) {
        Piece piece = pieceFor(layerType, itemStack);
        return piece != null ? piece.texture() : originalTexture;
    }

    private static Piece pieceFor(EquipmentClientInfo.LayerType layerType, ItemStack stack) {
        if (layerType == EquipmentClientInfo.LayerType.HUMANOID) {
            if (stack.is(AbyssalKingItems.ABYSSAL_SAMURAI_HELMET)) return Piece.HELMET;
            if (stack.is(AbyssalKingItems.ABYSSAL_SAMURAI_CHESTPLATE)) return Piece.CHESTPLATE;
            if (stack.is(AbyssalKingItems.ABYSSAL_SAMURAI_BOOTS)) return Piece.BOOTS;
        } else if (layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
                && stack.is(AbyssalKingItems.ABYSSAL_SAMURAI_LEGGINGS)) {
            return Piece.LEGGINGS;
        }
        return null;
    }
}
