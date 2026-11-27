package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaAnimationLibrary;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientStanceState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Port of the supplied Abyssal Samurai v2 model to the Minecraft 26.3 render-state API.
 * The geometry uses expanded 256x128 UV atlases so every leg and kasa face stays inside the texture.
 */
public class AbyssalSamuraiModel extends EntityModel<AbyssalSamuraiRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("simpleskills", "abyssal_samurai"), "main");
    public static final ModelLayerLocation SHELL_LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("simpleskills", "abyssal_samurai"), "stone_shell");

    private final ModelPart root;
    private final ModelPart pelvis;
    private final ModelPart torso;
    private final ModelPart chest;
    private final ModelPart head;
    private final ModelPart kasaHat;
    private final ModelPart leftShoulder;
    private final ModelPart rightShoulder;
    private final ModelPart leftThigh;
    private final ModelPart leftShin;
    private final ModelPart rightThigh;
    private final ModelPart rightShin;
    private final ModelPart kusazuriFrontL;
    private final ModelPart kusazuriFrontR;
    private final ModelPart kusazuriSideL;
    private final ModelPart kusazuriSideR;
    private final ModelPart kusazuriBack;

    private final KeyframeAnimation petrifiedPoseAnimation;
    private final KeyframeAnimation awakeningAnimation;
    private final KeyframeAnimation idleAnimation;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation attackAnimation;
    private final KeyframeAnimation deathAnimation;

    public AbyssalSamuraiModel(ModelPart rootPart) {
        super(rootPart);
        this.root = rootPart;
        this.pelvis = rootPart.getChild("pelvis");
        this.torso = this.pelvis.getChild("torso");
        this.chest = this.torso.getChild("chest");
        this.head = this.chest.getChild("head");
        this.kasaHat = this.head.getChild("kasa_hat");
        this.leftShoulder = this.chest.getChild("left_shoulder");
        this.rightShoulder = this.chest.getChild("right_shoulder");
        this.leftThigh = this.pelvis.getChild("left_thigh");
        this.leftShin = this.leftThigh.getChild("left_shin");
        this.rightThigh = this.pelvis.getChild("right_thigh");
        this.rightShin = this.rightThigh.getChild("right_shin");
        this.kusazuriFrontL = this.pelvis.getChild("kusazuri_front_l");
        this.kusazuriFrontR = this.pelvis.getChild("kusazuri_front_r");
        this.kusazuriSideL = this.pelvis.getChild("kusazuri_side_l");
        this.kusazuriSideR = this.pelvis.getChild("kusazuri_side_r");
        this.kusazuriBack = this.pelvis.getChild("kusazuri_back");

        this.petrifiedPoseAnimation = AbyssalSamuraiAnimations.PETRIFIED_STATUE.bake(rootPart);
        this.awakeningAnimation = AbyssalSamuraiAnimations.AWAKENING.bake(rootPart);
        this.idleAnimation = AbyssalSamuraiAnimations.IDLE.bake(rootPart);
        this.walkAnimation = AbyssalSamuraiAnimations.WALK.bake(rootPart);
        this.attackAnimation = AbyssalSamuraiAnimations.SLASH_ATTACK.bake(rootPart);
        this.deathAnimation = AbyssalSamuraiAnimations.DEATH.bake(rootPart);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(createMesh(CubeDeformation.NONE), 256, 128);
    }

    public static LayerDefinition createStoneShellLayer() {
        return LayerDefinition.create(createMesh(new CubeDeformation(0.08F)), 256, 128);
    }

    private static MeshDefinition createMesh(CubeDeformation deform) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition pelvis = root.addOrReplaceChild("pelvis", CubeListBuilder.create()
                .texOffs(20, 62).addBox("pelvis_core", -4.0F, -3.0F, -2.0F, 8.0F, 3.0F, 4.0F, deform)
                .texOffs(44, 62).addBox("obi_sash", -4.5F, -3.5F, -2.5F, 9.0F, 2.0F, 5.0F, deform)
                .texOffs(72, 62).addBox("obi_knot_back", -2.5F, -4.0F, 2.0F, 5.0F, 3.0F, 2.0F, deform)
                .texOffs(86, 62).addBox("belt_gem", -1.5F, -3.5F, -3.0F, 3.0F, 2.0F, 1.0F, deform),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        pelvis.addOrReplaceChild("kusazuri_front_l", CubeListBuilder.create()
                .texOffs(94, 62).addBox("front_plate_l", -0.2F, 0.0F, -0.5F, 4.0F, 5.0F, 1.0F, deform),
                PartPose.offsetAndRotation(0.3F, -1.5F, -2.4F, -0.14F, -0.06F, -0.05F));
        pelvis.addOrReplaceChild("kusazuri_front_r", CubeListBuilder.create()
                .texOffs(104, 62).addBox("front_plate_r", -3.8F, 0.0F, -0.5F, 4.0F, 5.0F, 1.0F, deform),
                PartPose.offsetAndRotation(-0.3F, -1.5F, -2.4F, -0.14F, 0.06F, 0.05F));
        pelvis.addOrReplaceChild("kusazuri_side_l", CubeListBuilder.create()
                .texOffs(0, 69).addBox("side_plate_l", -0.5F, 0.0F, -2.5F, 1.0F, 5.0F, 5.0F, deform),
                PartPose.offsetAndRotation(4.3F, -1.5F, 0.0F, 0.0F, 0.0F, -0.18F));
        pelvis.addOrReplaceChild("kusazuri_side_r", CubeListBuilder.create()
                .texOffs(12, 69).addBox("side_plate_r", -0.5F, 0.0F, -2.5F, 1.0F, 5.0F, 5.0F, deform),
                PartPose.offsetAndRotation(-4.3F, -1.5F, 0.0F, 0.0F, 0.0F, 0.18F));
        pelvis.addOrReplaceChild("kusazuri_back", CubeListBuilder.create()
                .texOffs(24, 69).addBox("back_plate", -4.0F, 0.0F, -0.5F, 8.0F, 5.0F, 1.0F, deform),
                PartPose.offsetAndRotation(0.0F, -1.5F, 2.4F, 0.16F, 0.0F, 0.0F));

        PartDefinition torso = pelvis.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(70, 48).addBox("abdomen", -4.0F, -5.0F, -2.0F, 8.0F, 5.0F, 4.0F, deform)
                .texOffs(94, 48).addBox("abdomen_trim", -4.0F, -5.5F, -2.5F, 8.0F, 1.0F, 5.0F, deform),
                PartPose.offset(0.0F, -3.0F, 0.0F));

        PartDefinition chest = torso.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(80, 32).addBox("do_cuirass", -5.0F, -8.0F, -3.0F, 10.0F, 8.0F, 6.0F, deform)
                .texOffs(112, 32).addBox("munaita_plastron", -3.5F, -7.2F, -3.8F, 7.0F, 5.0F, 1.0F, deform)
                .texOffs(80, 46).addBox("agatsura_ring_l", 2.2F, -8.3F, -3.6F, 1.0F, 2.0F, 1.0F, deform)
                .texOffs(84, 46).addBox("agatsura_ring_r", -3.2F, -8.3F, -3.6F, 1.0F, 2.0F, 1.0F, deform)
                .texOffs(88, 46).addBox("nodowa_gorget", -4.0F, -8.6F, -3.0F, 8.0F, 2.0F, 6.0F, deform)
                .texOffs(0, 54).addBox("back_do_plate", -4.5F, -7.5F, 2.8F, 9.0F, 7.0F, 1.0F, deform)
                .texOffs(20, 54).addBox("sashimono_mount", -1.0F, -6.5F, 3.5F, 2.0F, 4.0F, 1.0F, deform),
                PartPose.offset(0.0F, -5.0F, 0.0F));

        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(112, 20).addBox("head_neck", -1.5F, -2.0F, -1.5F, 3.0F, 2.0F, 3.0F, deform)
                .texOffs(64, 16).addBox("men_core", -4.0F, -9.0F, -4.0F, 8.0F, 8.0F, 8.0F, deform)
                .texOffs(96, 16).addBox("menpo_mask", -3.5F, -4.5F, -4.6F, 7.0F, 3.0F, 2.0F, deform)
                .texOffs(114, 16).addBox("fang_l", 2.2F, -4.8F, -4.9F, 1.0F, 1.0F, 1.0F, deform)
                .texOffs(118, 16).addBox("fang_r", -3.2F, -4.8F, -4.9F, 1.0F, 1.0F, 1.0F, deform),
                PartPose.offset(0.0F, -8.0F, 0.0F));

        head.addOrReplaceChild("shikoro_back", CubeListBuilder.create()
                .texOffs(96, 21).addBox("neckguard_back", -4.5F, 0.0F, -0.5F, 9.0F, 4.0F, 1.0F, deform),
                PartPose.offsetAndRotation(0.0F, -3.5F, 4.1F, 0.24F, 0.0F, 0.0F));
        head.addOrReplaceChild("shikoro_l", CubeListBuilder.create()
                .texOffs(116, 21).addBox("neckguard_l", -0.5F, 0.0F, -2.5F, 1.0F, 4.0F, 5.0F, deform),
                PartPose.offsetAndRotation(4.1F, -3.5F, 0.5F, 0.0F, 0.0F, -0.22F));
        head.addOrReplaceChild("shikoro_r", CubeListBuilder.create()
                .texOffs(64, 32).addBox("neckguard_r", -0.5F, 0.0F, -2.5F, 1.0F, 4.0F, 5.0F, deform),
                PartPose.offsetAndRotation(-4.1F, -3.5F, 0.5F, 0.0F, 0.0F, 0.22F));

        PartDefinition kasaHat = head.addOrReplaceChild("kasa_hat", CubeListBuilder.create()
                .texOffs(0, 0).addBox("kasa_brim", -9.5F, -1.0F, -9.5F, 19.0F, 1.0F, 19.0F, deform)
                .texOffs(76, 0).addBox("kasa_tier_1", -7.5F, -2.8F, -7.5F, 15.0F, 2.0F, 15.0F, deform)
                .texOffs(0, 20).addBox("kasa_tier_2", -5.5F, -4.6F, -5.5F, 11.0F, 2.0F, 11.0F, deform)
                .texOffs(44, 20).addBox("kasa_tier_3", -3.5F, -6.4F, -3.5F, 7.0F, 2.0F, 7.0F, deform)
                .texOffs(44, 29).addBox("kasa_apex", -1.5F, -8.2F, -1.5F, 3.0F, 2.0F, 3.0F, deform),
                PartPose.offsetAndRotation(0.0F, -7.5F, 0.0F, 0.08F, 0.0F, 0.0F));
        kasaHat.addOrReplaceChild("kasa_cord_l", CubeListBuilder.create()
                .texOffs(56, 29).addBox("cord_l", -0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, deform),
                PartPose.offsetAndRotation(3.6F, -0.5F, -1.0F, 0.0F, 0.0F, 0.18F));
        kasaHat.addOrReplaceChild("kasa_cord_r", CubeListBuilder.create()
                .texOffs(60, 29).addBox("cord_r", -0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, deform),
                PartPose.offsetAndRotation(-3.6F, -0.5F, -1.0F, 0.0F, 0.0F, -0.18F));

        PartDefinition leftShoulder = chest.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(0, 77).addBox("osode_l_top", -0.5F, -2.5F, -3.5F, 4.0F, 2.0F, 7.0F, deform)
                .texOffs(22, 77).addBox("osode_l_mid", 0.3F, -0.6F, -3.5F, 3.0F, 3.0F, 7.0F, deform)
                .texOffs(42, 77).addBox("osode_l_low", 1.1F, 2.2F, -3.0F, 2.0F, 3.0F, 6.0F, deform),
                PartPose.offset(5.5F, -6.5F, 0.0F));
        PartDefinition leftArm = leftShoulder.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(82, 69).addBox("kote_upper_l", -1.0F, -2.0F, -1.5F, 3.0F, 5.0F, 3.0F, deform),
                PartPose.ZERO);
        leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(94, 69).addBox("kote_forearm_l", -1.0F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F, deform)
                .texOffs(106, 69).addBox("tekko_handguard_l", -1.2F, 4.2F, -2.0F, 4.0F, 2.0F, 4.0F, deform),
                PartPose.offset(0.0F, 3.0F, 0.0F));

        PartDefinition rightShoulder = chest.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(58, 77).addBox("osode_r_top", -3.5F, -2.5F, -3.5F, 4.0F, 2.0F, 7.0F, deform)
                .texOffs(0, 87).addBox("osode_r_mid", -3.3F, -0.6F, -3.5F, 3.0F, 3.0F, 7.0F, deform)
                .texOffs(20, 87).addBox("osode_r_low", -3.1F, 2.2F, -3.0F, 2.0F, 3.0F, 6.0F, deform),
                PartPose.offset(-5.5F, -6.5F, 0.0F));
        PartDefinition rightArm = rightShoulder.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(58, 77).addBox("kote_upper_r", -2.0F, -2.0F, -1.5F, 3.0F, 5.0F, 3.0F, deform),
                PartPose.ZERO);
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(70, 77).addBox("kote_forearm_r", -2.0F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F, deform)
                .texOffs(82, 77).addBox("tekko_handguard_r", -2.8F, 4.2F, -2.0F, 4.0F, 2.0F, 4.0F, deform),
                PartPose.offset(0.0F, 3.0F, 0.0F));
        PartDefinition leftThigh = pelvis.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(36, 87).addBox("haidate_thigh_l", -1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F, deform),
                PartPose.offset(2.2F, 0.0F, 0.0F));
        leftThigh.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(50, 87).addBox("suneate_greave_l", -1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F, deform)
                .texOffs(64, 87).addBox("kogake_foot_l", -1.5F, 4.2F, -3.0F, 3.0F, 2.0F, 5.0F, deform),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        PartDefinition rightThigh = pelvis.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(80, 87).addBox("haidate_thigh_r", -1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F, deform),
                PartPose.offset(-2.2F, 0.0F, 0.0F));
        rightThigh.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(94, 87).addBox("suneate_greave_r", -1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F, deform)
                .texOffs(108, 87).addBox("kogake_foot_r", -1.5F, 4.2F, -3.0F, 3.0F, 2.0F, 5.0F, deform),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        return mesh;
    }

    @Override
    public void setupAnim(AbyssalSamuraiRenderState state) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        // The complete saya/blade item models are submitted by AbyssalSamuraiKatanaLayer.
        super.setupAnim(state);

        this.petrifiedPoseAnimation.apply(state.petrifiedAnimationState, state.ageInTicks);
        this.awakeningAnimation.apply(state.awakeningAnimationState, state.ageInTicks);
        this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
        this.walkAnimation.apply(state.walkAnimationState, state.ageInTicks);
        this.deathAnimation.apply(state.deathAnimationState, state.ageInTicks);

        if ((state.samuraiState == AbyssalSamuraiEntity.STATE_ACTIVE
                || state.samuraiState == AbyssalSamuraiEntity.STATE_RETURNING) && !state.isDying
                && state.katanaPose != null) {
            applyKatanaAnimation(state.katanaPose);
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.70F;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.55F;
        } else {
            this.attackAnimation.apply(state.attackAnimationState, state.ageInTicks);
        }
    }

    /** Applies the same Blockbench body/arm keyframes used by the player's Katana renderer. */
    private void applyKatanaAnimation(KatanaClientStanceState.RenderPose pose) {
        float[] body = KatanaAnimationLibrary.sampleBlended(pose, "body", "rotation", new float[]{0.0F, 0.0F, 0.0F});
        float[] right = KatanaAnimationLibrary.sampleBlended(pose, "right_arm", "rotation", new float[]{0.0F, 0.0F, 0.0F});
        float[] left = KatanaAnimationLibrary.sampleBlended(pose, "left_arm", "rotation", new float[]{0.0F, 0.0F, 0.0F});

        // Bedrock's animation rig has +Y up and +Z forward. ModelPart uses +Y down
        // and -Z forward, so preserve X and invert the Y/Z rotation channels exactly
        // as KatanaPlayerModelMixin does for the player's arms and body.
        this.torso.xRot += body[0] * Mth.DEG_TO_RAD;
        this.torso.yRot -= body[1] * Mth.DEG_TO_RAD;
        this.torso.zRot -= body[2] * Mth.DEG_TO_RAD;

        this.rightShoulder.xRot = right[0] * Mth.DEG_TO_RAD;
        this.rightShoulder.yRot = -right[1] * Mth.DEG_TO_RAD;
        this.rightShoulder.zRot = -right[2] * Mth.DEG_TO_RAD;
        this.leftShoulder.xRot = left[0] * Mth.DEG_TO_RAD;
        this.leftShoulder.yRot = -left[1] * Mth.DEG_TO_RAD;
        this.leftShoulder.zRot = -left[2] * Mth.DEG_TO_RAD;
    }

    /**
     * Recreates the exact coordinate frame used by KatanaPlayerRenderLayer:
     * body pivot -> feet-relative origin -> Bedrock (+Y up, +Z forward) axes.
     * The Samurai's pelvis/torso pivots total 9 model pixels below its root, so the
     * remaining 15 pixels place the animation origin at the same 24-pixel player origin.
     */
    public void translateToKatanaAnimationFrame(PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.pelvis.translateAndRotate(poseStack);
        this.torso.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 15.0F / 16.0F, 0.0F);
        poseStack.rotateDegrees(Axis.XP, 180.0F);
    }

}
