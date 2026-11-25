package com.github.ob_yekt.simpleskills.abyss.renderer;

import com.github.ob_yekt.simpleskills.abyss.AbyssalFallenKingEntity;
import com.github.ob_yekt.simpleskills.abyss.model.AbyssalFallenKingModel;
import com.github.ob_yekt.simpleskills.abyss.model.AbyssalFallenKingRenderState;
import com.github.ob_yekt.simpleskills.abyss.model.AbyssalKingCrownModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/** Renderer for the throne king: a twice-sized wither skeleton wearing a custom gold crown. */
public final class AbyssalFallenKingRenderer extends MobRenderer<
        AbyssalFallenKingEntity, AbyssalFallenKingRenderState, AbyssalFallenKingModel> {

    public static final ModelLayerLocation CROWN_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("simpleskills", "abyssal_fallen_king_crown"), "main");

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/entity/skeleton/wither_skeleton.png");

    public AbyssalFallenKingRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalFallenKingModel(context.bakeLayer(ModelLayers.SKELETON)), 0.95F);
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new AbyssalKingCrownLayer(this, new AbyssalKingCrownModel(context.bakeLayer(CROWN_LAYER))));
    }

    @Override
    public AbyssalFallenKingRenderState createRenderState() {
        return new AbyssalFallenKingRenderState();
    }

    @Override
    public Identifier getTextureLocation(AbyssalFallenKingRenderState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(AbyssalFallenKingEntity entity,
                                   AbyssalFallenKingRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTick);
        state.isAggressive = entity.isAggressive();
        state.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
        state.sittingAmount = entity.getKingState() == AbyssalFallenKingEntity.STATE_SITTING
                ? 1.0F : 0.0F;
        state.scale *= 2.0F;
    }
}
