package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientStanceState;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaGrip;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaRenderUtil;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaStanceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person katana renderer driven by the same blended global rig as third person.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class KatanaFirstPersonHandsAndItemsRendererMixin {
    private static final float VANILLA_HAND_OUT = 0.56F;
    private static final float VANILLA_HAND_Y = -0.52F;
    private static final float VANILLA_HAND_Z = -0.72F;

    @Invoker("applyItemArmTransform")
    protected abstract void simpleskills$invokeApplyItemArmTransform(
            PoseStack poseStack, HumanoidArm arm, float inverseArmHeight);

    @Invoker("renderPlayerHand")
    protected abstract void simpleskills$invokeRenderPlayerHand(
            PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
            HumanoidArm arm, PlayerRenderState playerState);

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void simpleskills$renderKatanaFirstPerson(PlayerRenderState playerState,
                                                       FirstPersonHandsAndItemsRenderState renderState,
                                                       float partialTicks, float xRot, InteractionHand hand,
                                                       float attack, ItemStack itemStack, float inverseArmHeight,
                                                       PoseStack poseStack, SubmitNodeCollector collector,
                                                       int lightCoords, CallbackInfo ci) {
        if (renderState.isScoping) return;

        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        if (mainHand) {
            if (itemStack == null || itemStack.isEmpty() || !AbyssalKatanaItems.isKatana(itemStack.getItem())) return;
        } else if (itemStack != null && !itemStack.isEmpty()) {
            return;
        }

        AvatarRenderState avatar = playerState.avatarRenderState;
        Player player = Minecraft.getInstance().player;
        if (avatar == null || player == null || !AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) {
            return;
        }

        KatanaClientStanceState.RenderPose pose = KatanaClientStanceState.resolveRenderPose(player);
        int state = pose.stance();
        float seconds = pose.elapsedSeconds();

        HumanoidArm mainArm = avatar.mainArm;
        HumanoidArm handArm = mainHand ? mainArm
                : (mainArm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT);

        if (mainHand) {
            float growth = KatanaGrip.mainHandGrowth(state, seconds);
            if (growth > 0.0F) {
                float[] grip = KatanaStanceManager.isSheathedGripStance(state)
                        ? KatanaGrip.sayaPoint(pose, KatanaGrip.SPECIAL_MAIN_HAND_SAYA_ALONG)
                        : KatanaGrip.point(pose, KatanaGrip.MAIN_HAND_ALONG_TSUKA);
                simpleskills$renderGrippingHand(poseStack, collector, lightCoords, playerState, avatar,
                        handArm, inverseArmHeight, grip, growth);
            }

            if (state == KatanaStanceManager.SHEATHED || KatanaStanceManager.isSheathedGripStance(state)) {
                // The special technique never unsheathes the blade: render the complete saya+tsuka model.
                KatanaRenderUtil.submitSheathed(poseStack, collector, lightCoords, player, pose, 0, false);
            } else {
                KatanaRenderUtil.submitSaya(poseStack, collector, lightCoords, player, pose, 0, false);
                KatanaRenderUtil.submitBlade(poseStack, collector, lightCoords, player, pose, 0, false);
            }
        } else {
            float[] grip;
            if (KatanaStanceManager.isSheathedGripStance(state)) {
                grip = KatanaGrip.sayaPoint(pose, KatanaGrip.SPECIAL_OFF_HAND_SAYA_ALONG);
            } else {
                float[] sayaGrip = KatanaGrip.sayaPoint(pose, KatanaGrip.SAYA_GRIP_ALONG);
                float[] tsukaGrip = KatanaGrip.point(pose, KatanaGrip.OFF_HAND_ALONG_TSUKA);
                grip = KatanaGrip.lerp(sayaGrip, tsukaGrip, KatanaGrip.tsukaWeight(pose));
            }
            simpleskills$renderGrippingHand(poseStack, collector, lightCoords, playerState, avatar,
                    handArm, inverseArmHeight, grip, 1.0F);
        }
        ci.cancel();
    }

    private void simpleskills$renderGrippingHand(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                                 PlayerRenderState playerState, AvatarRenderState avatar,
                                                 HumanoidArm handArm, float inverseArmHeight,
                                                 float[] grip, float growth) {
        float scale = KatanaGrip.handScale(grip) * growth;
        if (scale <= 0.001F || avatar.isInvisible) return;

        float[] wrist = KatanaGrip.wristFromArmOrigin(handArm);
        float vanillaX = handArm == HumanoidArm.RIGHT ? VANILLA_HAND_OUT : -VANILLA_HAND_OUT;

        poseStack.pushPose();
        this.simpleskills$invokeApplyItemArmTransform(poseStack, handArm, inverseArmHeight);
        poseStack.translate(
                grip[0] - scale * wrist[0] - vanillaX,
                grip[1] - scale * wrist[1] - VANILLA_HAND_Y,
                grip[2] - scale * wrist[2] - VANILLA_HAND_Z);
        poseStack.scale(scale, scale, scale);
        this.simpleskills$invokeRenderPlayerHand(poseStack, collector, lightCoords, handArm, playerState);
        poseStack.popPose();
    }
}
