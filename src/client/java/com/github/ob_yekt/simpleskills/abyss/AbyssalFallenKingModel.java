package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class AbyssalFallenKingModel extends EntityModel<AbyssalFallenKingRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("simpleskills", "abyssal_fallen_king"), "main");

    // Model-root space anchors (floor is Y = 24.0F) for the planted sword and fallen crown
    public static final float PLANTED_SWORD_MODEL_X = 0.0F;
    public static final float PLANTED_SWORD_MODEL_Y = 5.5F;
    public static final float PLANTED_SWORD_MODEL_Z = -11.5F;

    public static final float FALLEN_CROWN_MODEL_X = -6.2F;
    public static final float FALLEN_CROWN_MODEL_Y = 23.35F;
    public static final float FALLEN_CROWN_MODEL_Z = -9.8F;

    private static final float SWORD_PLANT_NORM_T = 2.25F / 4.50F;  // 0.500
    private static final float CROWN_DETACH_NORM_T = 2.55F / 4.50F; // 0.567
    private static final float CROWN_LAND_NORM_T = 3.25F / 4.50F;   // 0.722
    private static final float CRUMBLE_START_NORM_T = 3.15F / 4.50F; // 0.700

    public final ModelPart root;
    public final ModelPart pelvis;
    public final ModelPart torso;
    public final ModelPart chest;
    public final ModelPart capeUpper;
    public final ModelPart capeLower;
    public final ModelPart neck;
    public final ModelPart head;
    public final ModelPart jaw;
    public final ModelPart crown;
    public final ModelPart leftShoulder;
    public final ModelPart leftArm;
    public final ModelPart leftForearm;
    public final ModelPart rightShoulder;
    public final ModelPart rightArm;
    public final ModelPart rightForearm;
    public final ModelPart swordHilt;
    public final ModelPart swordBlade;
    public final ModelPart leftThigh;
    public final ModelPart leftShin;
    public final ModelPart rightThigh;
    public final ModelPart rightShin;
    public final ModelPart abyssalAuraCrest;

    private final KeyframeAnimation sittingIdleAnimation;
    private final KeyframeAnimation awakeningAnimation;
    private final KeyframeAnimation idleAnimation;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation attackAnimation;
    private final KeyframeAnimation deathAnimation;

    public AbyssalFallenKingModel(ModelPart rootPart) {
        super(rootPart);
        this.root = rootPart.getChild("root");
        this.pelvis = this.root.getChild("pelvis");
        this.torso = this.pelvis.getChild("torso");
        this.chest = this.torso.getChild("chest");
        this.capeUpper = this.chest.getChild("cape_upper");
        this.capeLower = this.capeUpper.getChild("cape_lower");
        this.neck = this.chest.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.crown = this.head.getChild("crown");
        this.leftShoulder = this.chest.getChild("left_shoulder");
        this.leftArm = this.leftShoulder.getChild("left_arm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.rightShoulder = this.chest.getChild("right_shoulder");
        this.rightArm = this.rightShoulder.getChild("right_arm");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.swordHilt = this.rightForearm.getChild("sword_hilt");
        this.swordBlade = this.swordHilt.getChild("sword_blade");
        this.leftThigh = this.pelvis.getChild("left_thigh");
        this.leftShin = this.leftThigh.getChild("left_shin");
        this.rightThigh = this.pelvis.getChild("right_thigh");
        this.rightShin = this.rightThigh.getChild("right_shin");
        this.abyssalAuraCrest = this.chest.getChild("abyssal_aura_crest");

        this.sittingIdleAnimation = AbyssalFallenKingAnimations.SITTING_IDLE.bake(rootPart);
        this.awakeningAnimation = AbyssalFallenKingAnimations.AWAKENING.bake(rootPart);
        this.idleAnimation = AbyssalFallenKingAnimations.IDLE.bake(rootPart);
        this.walkAnimation = AbyssalFallenKingAnimations.WALK.bake(rootPart);
        this.attackAnimation = AbyssalFallenKingAnimations.ATTACK.bake(rootPart);
        this.deathAnimation = AbyssalFallenKingAnimations.DEATH.bake(rootPart);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        PartDefinition rootPart = partDefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0F, 24.0F, 0F));
        PartDefinition pelvisPart = rootPart.addOrReplaceChild("pelvis", CubeListBuilder.create()
                .texOffs(0, 0).addBox("pelvis_core", -4.5F, -1.0F, -2.5F, 9.0F, 3.0F, 5.0F)
                .texOffs(0, 8).addBox("pelvis_hip_l", 2.5F, -0.5F, -2.0F, 2.0F, 3.0F, 4.0F)
                .texOffs(12, 8).addBox("pelvis_hip_r", -4.5F, -0.5F, -2.0F, 2.0F, 3.0F, 4.0F)
                .texOffs(28, 0).addBox("belt_buckle", -1.5F, -0.8F, -3.0F, 3.0F, 2.0F, 1.0F)
                .texOffs(28, 4).addBox("tasset_front_l", 0.8F, 1.5F, -2.8F, 3.0F, 4.0F, 1.0F)
                .texOffs(36, 4).addBox("tasset_front_r", -3.8F, 1.5F, -2.8F, 3.0F, 4.0F, 1.0F),
                PartPose.offset(0F, -16F, 0F));
        PartDefinition torsoPart = pelvisPart.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 16).addBox("spine_lumbar", -1.5F, -5.0F, -1.0F, 3.0F, 5.0F, 3.0F)
                .texOffs(12, 16).addBox("vertebra_1", -2.0F, -1.5F, 1.5F, 4.0F, 1.0F, 1.0F)
                .texOffs(12, 18).addBox("vertebra_2", -2.0F, -3.5F, 1.5F, 4.0F, 1.0F, 1.0F)
                .texOffs(44, 0).addBox("abdomen_plate", -4.0F, -5.0F, -2.5F, 8.0F, 5.0F, 5.0F),
                PartPose.offset(0F, -1F, 0F));
        PartDefinition chestPart = torsoPart.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(70, 0).addBox("chest_cuirass", -5.0F, -8.0F, -3.0F, 10.0F, 8.0F, 6.0F)
                .texOffs(102, 0).addBox("sternum_crest", -2.0F, -6.5F, -3.5F, 4.0F, 5.0F, 1.0F)
                .texOffs(70, 14).addBox("collar_gorget", -4.5F, -9.5F, -3.2F, 9.0F, 2.0F, 6.0F)
                .texOffs(102, 6).addBox("rib_cage_inner", -4.2F, -7.5F, -2.2F, 8.0F, 7.0F, 4.0F)
                .texOffs(102, 17).addBox("back_plate", -4.5F, -7.5F, 2.6F, 9.0F, 7.0F, 1.0F)
                .texOffs(24, 16).addBox("chest_trim_l", 2.2F, -6.0F, -3.4F, 2.0F, 3.0F, 1.0F)
                .texOffs(30, 16).addBox("chest_trim_r", -4.2F, -6.0F, -3.4F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(0F, -5F, 0F));
        PartDefinition cape_upperPart = chestPart.addOrReplaceChild("cape_upper", CubeListBuilder.create()
                .texOffs(0, 26).addBox("cape_mantle_top", -5.5F, -0.5F, -0.4F, 11.0F, 10.0F, 1.0F)
                .texOffs(24, 26).addBox("cape_clasp_l", 3.5F, -1.0F, -5.7F, 2.0F, 2.0F, 6.0F)
                .texOffs(40, 26).addBox("cape_clasp_r", -5.5F, -1.0F, -5.7F, 2.0F, 2.0F, 6.0F)
                .texOffs(24, 34).addBox("cape_trim_upper", -5.0F, 7.5F, 0.1F, 10.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0F, -7.5F, 3.2F, 6F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD));
        PartDefinition cape_lowerPart = cape_upperPart.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(0, 38).addBox("cape_drape_main", -5.5F, -0.0F, -0.4F, 11.0F, 11.0F, 1.0F)
                .texOffs(24, 38).addBox("cape_tatter_l", 2.5F, 11.0F, -0.4F, 3.0F, 4.0F, 1.0F)
                .texOffs(32, 38).addBox("cape_tatter_c", -1.5F, 11.0F, -0.4F, 3.0F, 5.0F, 1.0F)
                .texOffs(40, 38).addBox("cape_tatter_r", -5.5F, 10.5F, -0.4F, 3.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0F, 9.5F, 0.0F, 5F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD));
        PartDefinition neckPart = chestPart.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(48, 38).addBox("neck_cervical", -1.5F, -2.5F, -1.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(60, 38).addBox("neck_tendon_l", 1.0F, -1.7F, -0.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(64, 38).addBox("neck_tendon_r", -2.0F, -1.7F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0F, -8.5F, 0F));
        PartDefinition headPart = neckPart.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 52).addBox("skull_cranium", -4.0F, -8.0F, -4.0F, 8.0F, 6.0F, 8.0F)
                .texOffs(32, 52).addBox("skull_face_upper", -3.5F, -2.0F, -4.0F, 7.0F, 2.0F, 5.0F)
                .texOffs(32, 59).addBox("brow_ridge", -3.8F, -3.5F, -4.4F, 8.0F, 1.0F, 1.0F)
                .texOffs(32, 61).addBox("cheekbone_l", 2.0F, -2.3F, -4.2F, 2.0F, 2.0F, 3.0F)
                .texOffs(42, 61).addBox("cheekbone_r", -4.0F, -2.3F, -4.2F, 2.0F, 2.0F, 3.0F)
                .texOffs(52, 61).addBox("nasal_bridge", -1.0F, -2.7F, -4.3F, 2.0F, 2.0F, 1.0F)
                .texOffs(58, 61).addBox("eye_glow_l", 1.2F, -2.5F, -4.1F, 2.0F, 1.0F, 1.0F)
                .texOffs(64, 61).addBox("eye_glow_r", -3.2F, -2.5F, -4.1F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0F, -2.0F, 0F));
        PartDefinition jawPart = headPart.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(56, 52).addBox("mandible_base", -2.5F, -0.2F, -2.6F, 5.0F, 2.0F, 4.0F)
                .texOffs(56, 58).addBox("teeth_lower", -2.0F, -0.4F, -2.7F, 4.0F, 1.0F, 1.0F)
                .texOffs(66, 58).addBox("chin_guard", -1.5F, 1.0F, -2.9F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0F, -0.5F, -1.0F, 4F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD));
        PartDefinition crownPart = headPart.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(0, 68).addBox("crown_band", -4.4F, -1.5F, -4.4F, 9.0F, 2.0F, 9.0F)
                .texOffs(36, 68).addBox("crown_spike_front", -1.0F, -4.5F, -4.6F, 2.0F, 3.0F, 1.0F)
                .texOffs(42, 68).addBox("crown_spike_fl", 2.6F, -3.5F, -4.5F, 2.0F, 2.0F, 1.0F)
                .texOffs(48, 68).addBox("crown_spike_fr", -4.6F, -3.5F, -4.5F, 2.0F, 2.0F, 1.0F)
                .texOffs(36, 72).addBox("crown_spike_bl", 2.6F, -3.5F, 3.5F, 2.0F, 2.0F, 1.0F)
                .texOffs(42, 72).addBox("crown_spike_br", -4.6F, -3.5F, 3.5F, 2.0F, 2.0F, 1.0F)
                .texOffs(48, 72).addBox("crown_spike_back", -1.0F, -4.5F, 3.6F, 2.0F, 3.0F, 1.0F)
                .texOffs(54, 72).addBox("crown_spike_side_l", 3.6F, -4.5F, -1.0F, 1.0F, 3.0F, 2.0F)
                .texOffs(60, 72).addBox("crown_spike_side_r", -4.6F, -4.5F, -1.0F, 1.0F, 3.0F, 2.0F)
                .texOffs(54, 68).addBox("gem_ruby_center", -1.0F, -2.2F, -4.8F, 2.0F, 2.0F, 1.0F)
                .texOffs(60, 68).addBox("gem_sapphire_l", 3.6F, -2.0F, -1.0F, 1.0F, 2.0F, 2.0F)
                .texOffs(66, 68).addBox("gem_emerald_r", -4.7F, -2.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(0F, -7.5F, 0F));
        PartDefinition left_shoulderPart = chestPart.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(74, 52).addBox("pauldron_l_main", -0.7F, -2.0F, -3.2F, 4.0F, 4.0F, 6.0F)
                .texOffs(74, 62).addBox("pauldron_l_rim", -0.3F, -2.3F, -3.5F, 4.0F, 1.0F, 7.0F)
                .texOffs(96, 52).addBox("pauldron_l_spike", 2.0F, -3.7F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(5.5F, -6.5F, 0F));
        PartDefinition left_armPart = left_shoulderPart.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(96, 56).addBox("humerus_l", -1.0F, 0.5F, -1.5F, 3.0F, 6.0F, 3.0F)
                .texOffs(108, 56).addBox("arm_strap_l", -1.2F, 3.5F, -1.8F, 3.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(1.0F, 1.0F, 0F, 0F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD, -4F * Mth.DEG_TO_RAD));
        PartDefinition left_forearmPart = left_armPart.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(96, 65).addBox("ulna_radius_l", -0.9F, -0.0F, -1.4F, 3.0F, 6.0F, 3.0F)
                .texOffs(108, 65).addBox("gauntlet_l", -1.3F, 3.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(108, 73).addBox("knuckle_guard_l", -1.1F, 6.5F, -2.2F, 3.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 6.5F, 0F, -12F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD));
        PartDefinition right_shoulderPart = chestPart.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(74, 70).addBox("pauldron_r_main", -3.3F, -2.0F, -3.2F, 4.0F, 4.0F, 6.0F)
                .texOffs(74, 80).addBox("pauldron_r_rim", -3.7F, -2.3F, -3.5F, 4.0F, 1.0F, 7.0F)
                .texOffs(96, 74).addBox("pauldron_r_spike", -4.0F, -3.7F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-5.5F, -6.5F, 0F));
        PartDefinition right_armPart = right_shoulderPart.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(96, 78).addBox("humerus_r", -2.0F, 0.5F, -1.5F, 3.0F, 6.0F, 3.0F)
                .texOffs(108, 78).addBox("arm_strap_r", -1.8F, 3.5F, -1.8F, 3.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(-1.0F, 1.0F, 0F, 0F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD, 4F * Mth.DEG_TO_RAD));
        PartDefinition right_forearmPart = right_armPart.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(96, 87).addBox("ulna_radius_r", -2.1F, -0.0F, -1.4F, 3.0F, 6.0F, 3.0F)
                .texOffs(108, 87).addBox("gauntlet_r", -2.7F, 3.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(108, 95).addBox("knuckle_guard_r", -1.9F, 6.5F, -2.2F, 3.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 6.5F, 0F, -18F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD, 0F * Mth.DEG_TO_RAD));
        PartDefinition sword_hiltPart = right_forearmPart.addOrReplaceChild("sword_hilt", CubeListBuilder.create()
                .texOffs(0, 80).addBox("sword_pommel", -1.0F, -1.0F, 3.5F, 2.0F, 2.0F, 2.0F)
                .texOffs(8, 80).addBox("sword_grip", -0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 5.0F)
                .texOffs(20, 80).addBox("sword_crossguard", -3.0F, -1.0F, -3.0F, 6.0F, 2.0F, 2.0F)
                .texOffs(36, 80).addBox("sword_guard_wing_l", 2.5F, -1.3F, -3.7F, 1.0F, 2.0F, 2.0F)
                .texOffs(42, 80).addBox("sword_guard_wing_r", -3.5F, -1.3F, -3.7F, 1.0F, 2.0F, 2.0F)
                .texOffs(48, 80).addBox("sword_guard_gem", -0.5F, -0.5F, -3.3F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-0.5F, 6.5F, -1.5F));
        PartDefinition sword_bladePart = sword_hiltPart.addOrReplaceChild("sword_blade", CubeListBuilder.create()
                .texOffs(0, 86).addBox("blade_ricasso", -1.0F, -0.3F, -5.0F, 2.0F, 1.0F, 5.0F)
                .texOffs(14, 86).addBox("blade_body", -1.5F, -0.5F, -17.0F, 3.0F, 1.0F, 12.0F)
                .texOffs(44, 86).addBox("blade_fuller_rune", -0.5F, -0.7F, -16.0F, 1.0F, 1.0F, 11.0F)
                .texOffs(68, 86).addBox("blade_rune_sigil_1", -0.5F, -0.2F, -9.0F, 1.0F, 1.0F, 2.0F)
                .texOffs(74, 86).addBox("blade_rune_sigil_2", -0.5F, -0.2F, -14.0F, 1.0F, 1.0F, 2.0F)
                .texOffs(0, 92).addBox("blade_tip_mid", -1.0F, -0.5F, -20.0F, 2.0F, 1.0F, 3.0F)
                .texOffs(10, 92).addBox("blade_tip_point", -0.5F, -0.5F, -22.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, -0.0F, -3.0F));
        PartDefinition left_thighPart = pelvisPart.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(0, 100).addBox("femur_l", -1.3F, -0.5F, -1.5F, 3.0F, 7.0F, 3.0F)
                .texOffs(12, 100).addBox("cuisses_l", -1.6F, 0.5F, -1.9F, 3.0F, 4.0F, 4.0F)
                .texOffs(26, 100).addBox("knee_poleyn_l", -1.5F, 5.0F, -2.2F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(2.5F, 2.0F, 0F));
        PartDefinition left_shinPart = left_thighPart.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(36, 100).addBox("tibia_l", -1.2F, -0.0F, -1.4F, 3.0F, 6.0F, 3.0F)
                .texOffs(48, 100).addBox("greave_l", -1.5F, 0.5F, -1.8F, 3.0F, 5.0F, 4.0F)
                .texOffs(62, 100).addBox("sabaton_l", -1.5F, 5.5F, -2.8F, 3.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, 6.5F, 0F));
        PartDefinition right_thighPart = pelvisPart.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(0, 112).addBox("femur_r", -1.7F, -0.5F, -1.5F, 3.0F, 7.0F, 3.0F)
                .texOffs(12, 112).addBox("cuisses_r", -1.4F, 0.5F, -1.9F, 3.0F, 4.0F, 4.0F)
                .texOffs(26, 112).addBox("knee_poleyn_r", -1.5F, 5.0F, -2.2F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(-2.5F, 2.0F, 0F));
        PartDefinition right_shinPart = right_thighPart.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(36, 112).addBox("tibia_r", -1.8F, -0.0F, -1.4F, 3.0F, 6.0F, 3.0F)
                .texOffs(48, 112).addBox("greave_r", -1.5F, 0.5F, -1.8F, 3.0F, 5.0F, 4.0F)
                .texOffs(62, 112).addBox("sabaton_r", -1.5F, 5.5F, -2.8F, 3.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, 6.5F, 0F));
        PartDefinition abyssal_aura_crestPart = chestPart.addOrReplaceChild("abyssal_aura_crest", CubeListBuilder.create()
                .texOffs(80, 100).addBox("halo_arch_top", -3.0F, -11.5F, 0.0F, 6.0F, 1.0F, 1.0F)
                .texOffs(80, 104).addBox("halo_pillar_l", 3.0F, -10.5F, 0.0F, 1.0F, 7.0F, 1.0F)
                .texOffs(84, 104).addBox("halo_pillar_r", -4.0F, -10.5F, 0.0F, 1.0F, 7.0F, 1.0F)
                .texOffs(88, 104).addBox("halo_rune_l", 3.2F, -8.5F, -0.3F, 1.0F, 2.0F, 1.0F)
                .texOffs(92, 104).addBox("halo_rune_r", -4.2F, -8.5F, -0.3F, 1.0F, 2.0F, 1.0F)
                .texOffs(96, 104).addBox("halo_rune_top", -0.5F, -13.2F, -0.3F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0F, -9.0F, 2.5F));

        return LayerDefinition.create(meshDefinition, 128, 128);
    }

    @Override
    public void setupAnim(AbyssalFallenKingRenderState state) {
        // Rebuild the bind pose first so transforms and visibility from the previous frame do not accumulate.
        this.root.getAllParts().forEach(part -> {
            part.resetPose();
            part.visible = true;
        });
        super.setupAnim(state);

        // In Minecraft 26.3 animation definitions are baked once and applied through KeyframeAnimation.
        this.sittingIdleAnimation.apply(state.sittingIdleAnimationState, state.ageInTicks);
        this.awakeningAnimation.apply(state.awakeningAnimationState, state.ageInTicks);
        this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
        this.walkAnimation.apply(state.walkAnimationState, state.ageInTicks);
        this.attackAnimation.apply(state.attackAnimationState, state.ageInTicks);
        this.deathAnimation.apply(state.deathAnimationState, state.ageInTicks);

        if (state.isDying) {
            applyCinematicDeathKinematics(Mth.clamp(state.deathProgress, 0.0F, 1.0F));
            return;
        }

        if (state.kingState == AbyssalFallenKingEntity.STATE_ACTIVE && !state.deathAnimationState.isStarted()) {
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.65F;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.55F;
        }
    }

    /**
     * Handles Phase 4 (crown detachment, tumble, bounce and rest on floor) and Phase 5
     * (progressive top-down armor crumbling) while keeping the planted sword and fallen
     * crown locked at exact ground coordinates unaffected by parent bone collapse.
     */
    private void applyCinematicDeathKinematics(float t) {
        // Capture exact crown world transform at detachment (t = 2.55s) before head moves further
        Matrix4f headMatBeforeCrumble = computeHeadChainMatrix();
        Matrix4f crownAtHeadMat = applyPartTransform(new Matrix4f(headMatBeforeCrumble), this.crown);

        // Phase 5 (3.15s..4.50s): staggered top-down crumbling of the King's armor and skeleton
        if (t > CRUMBLE_START_NORM_T) {
            float c = Mth.clamp((t - CRUMBLE_START_NORM_T) / (1.0F - CRUMBLE_START_NORM_T), 0.0F, 1.0F);

            // Leaf/branch parts without the planted sword or fallen crown can hide completely
            shrinkAndHideLeafPart(this.abyssalAuraCrest, window(c, 0.00F, 0.25F));
            shrinkAndHideLeafPart(this.capeLower, window(c, 0.06F, 0.34F));
            shrinkAndHideLeafPart(this.capeUpper, window(c, 0.14F, 0.44F));
            shrinkAndHideLeafPart(this.leftForearm, window(c, 0.20F, 0.50F));
            shrinkAndHideLeafPart(this.leftArm, window(c, 0.30F, 0.58F));
            shrinkAndHideLeafPart(this.leftShoulder, window(c, 0.38F, 0.64F));
            shrinkAndHideLeafPart(this.jaw, window(c, 0.40F, 0.64F));
            shrinkAndHideLeafPart(this.leftShin, window(c, 0.66F, 0.90F));
            shrinkAndHideLeafPart(this.rightShin, window(c, 0.68F, 0.92F));
            shrinkAndHideLeafPart(this.leftThigh, window(c, 0.74F, 0.96F));
            shrinkAndHideLeafPart(this.rightThigh, window(c, 0.76F, 0.98F));

            // Ancestor bones of crown and sword_hilt shrink down to 0.015F (non-singular)
            // so their children stay full-size and anchored on the ground.
            shrinkAncestorPart(this.rightForearm, window(c, 0.22F, 0.52F));
            shrinkAncestorPart(this.rightArm, window(c, 0.32F, 0.60F));
            shrinkAncestorPart(this.rightShoulder, window(c, 0.40F, 0.66F));
            shrinkAncestorPart(this.head, window(c, 0.45F, 0.70F));
            shrinkAncestorPart(this.neck, window(c, 0.50F, 0.74F));
            shrinkAncestorPart(this.chest, window(c, 0.56F, 0.82F));
            shrinkAncestorPart(this.torso, window(c, 0.64F, 0.88F));
            shrinkAncestorPart(this.pelvis, window(c, 0.78F, 1.00F));
        }

        // Phase 3..5 (2.25s..4.50s): lock sword vertically in the ground
        if (t >= SWORD_PLANT_NORM_T) {
            Matrix4f rightForearmMat = computeRightForearmChainMatrix();
            Matrix4f desiredSwordMat = new Matrix4f()
                    .translate(PLANTED_SWORD_MODEL_X, PLANTED_SWORD_MODEL_Y, PLANTED_SWORD_MODEL_Z)
                    .rotateZYX(0.0F, 0.0F, Mth.HALF_PI);
            setPartFromWorldMatrix(this.swordHilt, rightForearmMat, desiredSwordMat);
        }

        // Phase 4..5 (2.55s..4.50s): detach crown, tumble/spin to floor, and rest beside sword
        if (t >= CROWN_DETACH_NORM_T) {
            Vector3f startPos = crownAtHeadMat.getTranslation(new Vector3f());
            Vector3f startEuler = crownAtHeadMat.getEulerAnglesZYX(new Vector3f());

            float fallSpan = CROWN_LAND_NORM_T - CROWN_DETACH_NORM_T;
            float fallT = Mth.clamp((t - CROWN_DETACH_NORM_T) / fallSpan, 0.0F, 1.0F);

            // Split into main drop (0.0..0.76) and a small grounded bounce (0.76..1.0)
            float curX;
            float curY;
            float curZ;
            float rotX;
            float rotY;
            float rotZ;

            if (fallT < 0.76F) {
                float u = fallT / 0.76F;
                float uEase = u * u; // gravity acceleration
                curX = Mth.lerp(u, startPos.x, FALLEN_CROWN_MODEL_X + 0.45F);
                curZ = Mth.lerp(u, startPos.z, FALLEN_CROWN_MODEL_Z + 0.35F);
                // Slight upward hop off the brow followed by parabolic drop to floor
                float hop = -1.1F * Mth.sin(u * Mth.PI);
                curY = Mth.lerp(uEase, startPos.y, FALLEN_CROWN_MODEL_Y) + hop;

                rotX = Mth.lerp(u, startEuler.x, 22.0F * Mth.DEG_TO_RAD);
                rotY = Mth.lerp(u, startEuler.y, 145.0F * Mth.DEG_TO_RAD);
                rotZ = Mth.lerp(u, startEuler.z, -28.0F * Mth.DEG_TO_RAD);
            } else {
                float b = (fallT - 0.76F) / 0.24F;
                float bounceHop = -0.65F * Mth.sin(b * Mth.PI);
                curX = Mth.lerp(b, FALLEN_CROWN_MODEL_X + 0.45F, FALLEN_CROWN_MODEL_X);
                curZ = Mth.lerp(b, FALLEN_CROWN_MODEL_Z + 0.35F, FALLEN_CROWN_MODEL_Z);
                curY = FALLEN_CROWN_MODEL_Y + bounceHop;

                rotX = Mth.lerp(b, 22.0F * Mth.DEG_TO_RAD, 8.0F * Mth.DEG_TO_RAD);
                rotY = Mth.lerp(b, 145.0F * Mth.DEG_TO_RAD, 162.0F * Mth.DEG_TO_RAD);
                rotZ = Mth.lerp(b, -28.0F * Mth.DEG_TO_RAD, -6.0F * Mth.DEG_TO_RAD);
            }

            Matrix4f headMatNow = computeHeadChainMatrix();
            Matrix4f desiredCrownMat = new Matrix4f()
                    .translate(curX, curY, curZ)
                    .rotateZYX(rotZ, rotY, rotX);
            setPartFromWorldMatrix(this.crown, headMatNow, desiredCrownMat);
        }
    }

    private Matrix4f computeChestChainMatrix() {
        Matrix4f m = new Matrix4f();
        applyPartTransform(m, this.root);
        applyPartTransform(m, this.pelvis);
        applyPartTransform(m, this.torso);
        applyPartTransform(m, this.chest);
        return m;
    }

    private Matrix4f computeHeadChainMatrix() {
        Matrix4f m = computeChestChainMatrix();
        applyPartTransform(m, this.neck);
        applyPartTransform(m, this.head);
        return m;
    }

    private Matrix4f computeRightForearmChainMatrix() {
        Matrix4f m = computeChestChainMatrix();
        applyPartTransform(m, this.rightShoulder);
        applyPartTransform(m, this.rightArm);
        applyPartTransform(m, this.rightForearm);
        return m;
    }

    private static Matrix4f applyPartTransform(Matrix4f mat, ModelPart part) {
        mat.translate(part.x, part.y, part.z);
        if (part.xRot != 0.0F || part.yRot != 0.0F || part.zRot != 0.0F) {
            mat.rotateZYX(part.zRot, part.yRot, part.xRot);
        }
        if (part.xScale != 1.0F || part.yScale != 1.0F || part.zScale != 1.0F) {
            mat.scale(part.xScale, part.yScale, part.zScale);
        }
        return mat;
    }

    private static void setPartFromWorldMatrix(ModelPart part, Matrix4f parentWorldMat, Matrix4f desiredWorldMat) {
        Matrix4f localMat = new Matrix4f(parentWorldMat).invert().mul(desiredWorldMat);
        Vector3f trans = localMat.getTranslation(new Vector3f());
        Vector3f scale = localMat.getScale(new Vector3f());

        Matrix4f rotOnly = new Matrix4f(localMat);
        if (Math.abs(scale.x) > 1.0E-6F) {
            rotOnly.scale(1.0F / scale.x, 1.0F, 1.0F);
        }
        if (Math.abs(scale.y) > 1.0E-6F) {
            rotOnly.scale(1.0F, 1.0F / scale.y, 1.0F);
        }
        if (Math.abs(scale.z) > 1.0E-6F) {
            rotOnly.scale(1.0F, 1.0F, 1.0F / scale.z);
        }
        Vector3f euler = rotOnly.getEulerAnglesZYX(new Vector3f());

        part.x = trans.x;
        part.y = trans.y;
        part.z = trans.z;
        part.xRot = euler.x;
        part.yRot = euler.y;
        part.zRot = euler.z;
        part.xScale = scale.x;
        part.yScale = scale.y;
        part.zScale = scale.z;
    }

    private static void shrinkAndHideLeafPart(ModelPart part, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        if (amount >= 0.995F) {
            part.visible = false;
            return;
        }
        float s = Math.max(0.02F, 1.0F - amount * amount);
        part.xScale = s;
        part.yScale = s;
        part.zScale = s;
    }

    private static void shrinkAncestorPart(ModelPart part, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        // Keep a tiny non-zero scale (0.015F) so matrix inversion for crown/sword_hilt stays exact
        float s = Mth.lerp(amount * amount, 1.0F, 0.015F);
        part.xScale = s;
        part.yScale = s;
        part.zScale = s;
    }

    private static float window(float value, float start, float end) {
        if (end <= start) {
            return value >= end ? 1.0F : 0.0F;
        }
        return Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
    }
}
