package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaAnimationLibrary;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientStanceState;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaStanceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the global blended body/arm pose wherever the player model is rendered. */
@Mixin(PlayerModel.class)
public abstract class KatanaPlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void simpleskills$animateKatanaArms(AvatarRenderState state, CallbackInfo ci) {
        if (state == null || Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(state.id);
        if (!(entity instanceof Player player)
                || !AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) return;

        KatanaClientStanceState.RenderPose pose = KatanaClientStanceState.resolveRenderPose(player);
        PlayerModel model = (PlayerModel) (Object) this;
        addRotation(model.body, KatanaAnimationLibrary.sampleBlended(pose, "body", "rotation",
                new float[]{0, 0, 0}));

        ModelPart dominantArm = player.getMainArm() == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart supportArm = player.getMainArm() == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;

        // While the katana is drawn (combat guard, attacks, lower_guard, or walking with the blade),
        // override vanilla's opposite-arm walk swing so both hands remain locked onto the katana grip.
        // Legs keep vanilla's full walking/sprinting animation untouched.
        if (pose.stance() != KatanaStanceManager.SHEATHED) {
            dominantArm.xRot = 0.0F;
            dominantArm.yRot = 0.0F;
            dominantArm.zRot = 0.0F;
            supportArm.xRot = 0.0F;
            supportArm.yRot = 0.0F;
            supportArm.zRot = 0.0F;
        }

        addRotation(dominantArm, KatanaAnimationLibrary.sampleBlended(pose, "right_arm", "rotation",
                new float[]{0, 0, 0}));
        addRotation(supportArm, KatanaAnimationLibrary.sampleBlended(pose, "left_arm", "rotation",
                new float[]{0, 0, 0}));
    }

    /**
     * The keyframes are Bedrock values (Y up, +Z front). ModelPart uses Y down and -Z front, so the
     * conversion is a 180° turn around X: X keeps its sign, while Y and Z change sign.
     */
    private static void addRotation(ModelPart part, float[] bedrockRotation) {
        part.xRot += (float) Math.toRadians(bedrockRotation[0]);
        part.yRot += (float) Math.toRadians(-bedrockRotation[1]);
        part.zRot += (float) Math.toRadians(-bedrockRotation[2]);
    }
}
