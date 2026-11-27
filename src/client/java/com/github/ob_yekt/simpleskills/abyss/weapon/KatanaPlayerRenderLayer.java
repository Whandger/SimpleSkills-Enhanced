package com.github.ob_yekt.simpleskills.abyss.weapon;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Third-person model layer. Renders the saya and blade from the unified blended player animation. */
public final class KatanaPlayerRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** Height of the feet below the body pivot (the neck), in model pixels. */
    private static final float FEET_BELOW_BODY_PIVOT = 24.0F;

    public KatanaPlayerRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float yRot, float xRot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || state.isInvisible) return;
        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player)
                || !AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) return;

        KatanaClientStanceState.RenderPose pose = KatanaClientStanceState.resolveRenderPose(player);

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, FEET_BELOW_BODY_PIVOT / 16.0F, 0.0F);
        poseStack.rotateDegrees(Axis.XP, 180.0F);

        if (pose.stance() == KatanaStanceManager.SHEATHED) {
            KatanaRenderUtil.submitSheathed(poseStack, collector, light, player, pose, state.outlineColor, true);
        } else {
            KatanaRenderUtil.submitSaya(poseStack, collector, light, player, pose, state.outlineColor, true);
            KatanaRenderUtil.submitBlade(poseStack, collector, light, player, pose, state.outlineColor, true);
        }
        poseStack.popPose();
    }
}
