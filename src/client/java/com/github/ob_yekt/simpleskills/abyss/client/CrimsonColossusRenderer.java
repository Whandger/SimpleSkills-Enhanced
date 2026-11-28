package com.github.ob_yekt.simpleskills.abyss.client;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class CrimsonColossusRenderer
        extends MobRenderer<CrimsonColossusEntity, CrimsonColossusRenderState, CrimsonColossusModel> {
    private static final Identifier BASE_TEXTURE = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "textures/entity/boss/crimson_colossus.png");

    public CrimsonColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new CrimsonColossusModel(context.bakeLayer(CrimsonColossusModel.LAYER_LOCATION)), 1.65F);
        this.addLayer(new CrimsonColossusEmissiveLayer(this));
        this.addLayer(new CrimsonColossusAttackLayer(this));
    }

    @Override
    public CrimsonColossusRenderState createRenderState() {
        return new CrimsonColossusRenderState();
    }

    @Override
    public void extractRenderState(CrimsonColossusEntity entity, CrimsonColossusRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.deathTime = 0.0F;
        state.crystalGlowPulse = entity.getCrystalGlowPulse(partialTick);
        state.attackType = entity.getAttackType();
        state.attackTick = entity.getAttackTick();
        state.impactTick = entity.getImpactTick();
        state.partialTick = partialTick;
        state.targetOffset = entity.getSyncedTargetPos().subtract(entity.getPosition(partialTick));
    }

    @Override
    protected void scale(CrimsonColossusRenderState state, PoseStack poseStack) {
        poseStack.scale(1.65F, 1.65F, 1.65F);
    }

    @Override
    public Identifier getTextureLocation(CrimsonColossusRenderState state) {
        return BASE_TEXTURE;
    }
}
