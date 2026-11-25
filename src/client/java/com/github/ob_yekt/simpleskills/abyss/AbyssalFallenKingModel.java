package com.github.ob_yekt.simpleskills.abyss;

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

public class AbyssalFallenKingModel extends EntityModel<AbyssalFallenKingRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("simpleskills", "abyssal_fallen_king"), "main");

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
        // Rebuild the bind pose first so transforms from the previous frame do not accumulate.
        this.root.getAllParts().forEach(ModelPart::resetPose);
        super.setupAnim(state);

        // In Minecraft 26.3 animation definitions are baked once and applied through KeyframeAnimation.
        this.sittingIdleAnimation.apply(state.sittingIdleAnimationState, state.ageInTicks);
        this.awakeningAnimation.apply(state.awakeningAnimationState, state.ageInTicks);
        this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
        this.walkAnimation.apply(state.walkAnimationState, state.ageInTicks);
        this.attackAnimation.apply(state.attackAnimationState, state.ageInTicks);
        this.deathAnimation.apply(state.deathAnimationState, state.ageInTicks);

        if (state.kingState == AbyssalFallenKingEntity.STATE_ACTIVE && !state.deathAnimationState.isStarted()) {
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.65F;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.55F;
        }
    }
}
