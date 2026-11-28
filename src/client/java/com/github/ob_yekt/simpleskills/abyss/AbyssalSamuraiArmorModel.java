package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Geometria das peças da armadura do Samurai Abissal (elmo, peitoral, perneiras e botas).
 * Cada peça tem a sua própria camada e o seu próprio atlas (textures/entity/abyssal_samurai_armor_*.png).
 * A pose não é calculada aqui: a AbyssalSamuraiArmorPoser copia a pose do jogador
 * (já com a pose da katana) para as partes que a peça possui.
 */
public final class AbyssalSamuraiArmorModel extends EntityModel<HumanoidRenderState> {

    public enum Piece {
        HELMET("abyssal_samurai_helmet",
                "textures/entity/equipment/humanoid/abyssal_samurai_helmet.png"),
        CHESTPLATE("abyssal_samurai_chestplate",
                "textures/entity/equipment/humanoid/abyssal_samurai_chestplate.png"),
        LEGGINGS("abyssal_samurai_leggings",
                "textures/entity/equipment/humanoid_leggings/abyssal_samurai_leggings.png"),
        BOOTS("abyssal_samurai_boots",
                "textures/entity/equipment/humanoid/abyssal_samurai_boots.png");

        private final String layerName;
        private final String texturePath;

        Piece(String layerName, String texturePath) {
            this.layerName = layerName;
            this.texturePath = texturePath;
        }

        public ModelLayerLocation layerLocation() {
            return new ModelLayerLocation(
                    Identifier.fromNamespaceAndPath("simpleskills", layerName), "main");
        }

        /** Caminho que o equipamento usa; é o mesmo que o jogo procura para a camada da peça. */
        public Identifier texture() {
            return Identifier.fromNamespaceAndPath("simpleskills", texturePath);
        }

        public LayerDefinition createLayer() {
            return switch (this) {
                case HELMET -> createHelmetLayer();
                case CHESTPLATE -> createChestplateLayer();
                case LEGGINGS -> createLeggingsLayer();
                case BOOTS -> createBootsLayer();
            };
        }
    }

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public AbyssalSamuraiArmorModel(ModelPart root) {
        super(root);
        this.head = optional(root, "head");
        this.body = optional(root, "body");
        this.rightArm = optional(root, "right_arm");
        this.leftArm = optional(root, "left_arm");
        this.rightLeg = optional(root, "right_leg");
        this.leftLeg = optional(root, "left_leg");
    }

    private static ModelPart optional(ModelPart root, String name) {
        return root.hasChild(name) ? root.getChild(name) : null;
    }

    /** Copia a pose de um HumanoidModel já posado para as partes desta peça. */
    public void copyPoseFrom(HumanoidModel<?> source) {
        copyTransform(source.head, this.head);
        copyTransform(source.body, this.body);
        copyTransform(source.rightArm, this.rightArm);
        copyTransform(source.leftArm, this.leftArm);
        copyTransform(source.rightLeg, this.rightLeg);
        copyTransform(source.leftLeg, this.leftLeg);
    }

    private static void copyTransform(ModelPart source, ModelPart target) {
        if (source == null || target == null) return;
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        // Pose vem de copyPoseFrom(), chamado pela AbyssalSamuraiArmorPoser.
    }

    // ---------------------------------------------------------------------
    // Geometria das quatro peças. Os UVs de elmo, perneiras e botas foram empacotados junto com as texturas.
    // ---------------------------------------------------------------------

    public static LayerDefinition createHelmetLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation deform = CubeDeformation.NONE;

        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox("kabuto_bowl", -5.0F, -10.5F, -5.0F, 10.0F, 4.0F, 10.0F, deform)
                .texOffs(40, 0).addBox("kabuto_rim", -5.5F, -7.0F, -5.5F, 11.0F, 1.0F, 11.0F, deform)
                .texOffs(20, 14).addBox("maedate_crest", -1.0F, -12.5F, -5.6F, 2.0F, 3.0F, 1.0F, deform)
                .texOffs(112, 0).addBox("horn_l", 4.0F, -14.0F, -1.0F, 1.0F, 5.0F, 2.0F, deform)
                .texOffs(118, 0).addBox("horn_r", -5.0F, -14.0F, -1.0F, 1.0F, 5.0F, 2.0F, deform)
                .texOffs(84, 0).addBox("cheek_l", 4.1F, -6.0F, -3.5F, 1.0F, 6.0F, 6.0F, deform)
                .texOffs(98, 0).addBox("cheek_r", -5.1F, -6.0F, -3.5F, 1.0F, 6.0F, 6.0F, deform)
                .texOffs(0, 14).addBox("shikoro_neck", -4.6F, -4.5F, 3.6F, 9.0F, 5.0F, 1.0F, deform),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition createChestplateLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation deform = CubeDeformation.NONE;

        // Torso: cuirass, munaita plastron, agatsura rings, nodowa gorget, back do, sashimono, abdomen, obi.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 37).addBox("do_cuirass", -5.0F, 0.0F, -3.0F, 10.0F, 8.0F, 6.0F, deform)
                .texOffs(104, 32).addBox("munaita_plastron", -3.5F, 0.8F, -3.7F, 7.0F, 5.0F, 1.0F, deform)
                .texOffs(120, 9).addBox("agatsura_ring_l", 2.2F, -0.3F, -3.6F, 1.0F, 2.0F, 1.0F, deform)
                .texOffs(124, 9).addBox("agatsura_ring_r", -3.2F, -0.3F, -3.6F, 1.0F, 2.0F, 1.0F, deform)
                .texOffs(32, 37).addBox("nodowa_gorget", -4.2F, -0.8F, -3.1F, 8.4F, 2.0F, 6.2F, deform)
                .texOffs(60, 38).addBox("back_do_plate", -4.5F, 0.5F, 2.6F, 9.0F, 7.0F, 1.0F, deform)
                .texOffs(122, 18).addBox("sashimono_mount", -1.0F, 1.5F, 3.3F, 2.0F, 4.0F, 1.0F, deform)
                .texOffs(80, 38).addBox("abdomen_lamellae", -4.3F, 7.2F, -2.3F, 8.6F, 3.0F, 4.6F, deform)
                .texOffs(32, 45).addBox("abdomen_trim", -4.4F, 6.9F, -2.5F, 8.8F, 1.0F, 5.0F, deform)
                .texOffs(58, 47).addBox("obi_sash", -4.6F, 9.8F, -2.6F, 9.2F, 2.2F, 5.2F, deform)
                .texOffs(104, 45).addBox("obi_knot_back", -2.5F, 9.4F, 2.2F, 5.0F, 3.0F, 2.0F, deform)
                .texOffs(104, 28).addBox("belt_buckle_gem", -1.5F, 9.8F, -3.1F, 3.0F, 2.0F, 1.0F, deform),
                PartPose.ZERO);

        body.addOrReplaceChild("kusazuri_front_l", CubeListBuilder.create()
                .texOffs(118, 45).addBox("front_plate_l", -0.2F, 0.0F, -0.5F, 4.0F, 3.5F, 1.0F, deform),
                PartPose.offsetAndRotation(0.3F, 11.4F, -2.5F, -0.20F, -0.06F, -0.05F));
        body.addOrReplaceChild("kusazuri_front_r", CubeListBuilder.create()
                .texOffs(86, 47).addBox("front_plate_r", -3.8F, 0.0F, -0.5F, 4.0F, 3.5F, 1.0F, deform),
                PartPose.offsetAndRotation(-0.3F, 11.4F, -2.5F, -0.20F, 0.06F, 0.05F));
        body.addOrReplaceChild("kusazuri_side_l", CubeListBuilder.create()
                .texOffs(96, 50).addBox("side_plate_l", -0.5F, 0.0F, -2.5F, 1.0F, 3.5F, 5.0F, deform),
                PartPose.offsetAndRotation(4.4F, 11.4F, 0.0F, 0.0F, 0.0F, -0.22F));
        body.addOrReplaceChild("kusazuri_side_r", CubeListBuilder.create()
                .texOffs(0, 51).addBox("side_plate_r", -0.5F, 0.0F, -2.5F, 1.0F, 3.5F, 5.0F, deform),
                PartPose.offsetAndRotation(-4.4F, 11.4F, 0.0F, 0.0F, 0.0F, 0.22F));
        body.addOrReplaceChild("kusazuri_back", CubeListBuilder.create()
                .texOffs(12, 51).addBox("back_plate", -4.0F, 0.0F, -0.5F, 8.0F, 3.5F, 1.0F, deform),
                PartPose.offsetAndRotation(0.0F, 11.4F, 2.5F, 0.22F, 0.0F, 0.0F));

        // Right arm pivot matches HumanoidModel (-5, 2, 0): stepped O-Sode, Kote sleeve, Tekko hand guard.
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 61).addBox("osode_r_top", -3.2F, -2.8F, -3.5F, 4.4F, 2.0F, 7.0F, deform)
                .texOffs(58, 63).addBox("osode_r_mid", -3.8F, -0.9F, -3.5F, 3.6F, 2.8F, 7.0F, deform)
                .texOffs(78, 63).addBox("osode_r_low", -4.0F, 1.8F, -3.0F, 3.0F, 2.8F, 6.0F, deform)
                .texOffs(38, 65).addBox("kote_upper_r", -3.25F, -2.1F, -2.25F, 4.5F, 4.8F, 4.5F, deform)
                .texOffs(94, 68).addBox("kote_forearm_r", -3.25F, 2.7F, -2.25F, 4.5F, 5.2F, 4.5F, deform)
                .texOffs(0, 70).addBox("tekko_handguard_r", -3.4F, 7.6F, -2.4F, 4.8F, 2.0F, 4.8F, deform),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        // Left arm pivot matches HumanoidModel (5, 2, 0).
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(58, 54).addBox("osode_l_top", -1.2F, -2.8F, -3.5F, 4.4F, 2.0F, 7.0F, deform)
                .texOffs(108, 54).addBox("osode_l_mid", 0.2F, -0.9F, -3.5F, 3.6F, 2.8F, 7.0F, deform)
                .texOffs(80, 54).addBox("osode_l_low", 1.0F, 1.8F, -3.0F, 3.0F, 2.8F, 6.0F, deform)
                .texOffs(96, 60).addBox("kote_upper_l", -1.25F, -2.1F, -2.25F, 4.5F, 4.8F, 4.5F, deform)
                .texOffs(108, 64).addBox("kote_forearm_l", -1.25F, 2.7F, -2.25F, 4.5F, 5.2F, 4.5F, deform)
                .texOffs(22, 65).addBox("tekko_handguard_l", -1.4F, 7.6F, -2.4F, 4.8F, 2.0F, 4.8F, deform),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 256, 128);
    }

    public static LayerDefinition createLeggingsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation deform = CubeDeformation.NONE;

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(84, 0).addBox("hakama_front", -4.5F, 9.0F, -2.9F, 9.0F, 5.0F, 1.0F, deform)
                .texOffs(104, 0).addBox("hakama_back", -4.5F, 9.0F, 1.9F, 9.0F, 5.0F, 1.0F, deform),
                PartPose.ZERO);

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(60, 0).addBox("thigh_front_r", -2.5F, 0.0F, -2.7F, 5.0F, 6.0F, 1.0F, deform)
                .texOffs(0, 0).addBox("thigh_outer_r", -2.9F, 0.0F, -2.0F, 1.0F, 6.0F, 4.0F, deform)
                .texOffs(20, 0).addBox("shin_shell_r", -2.5F, 6.0F, -2.5F, 5.0F, 3.0F, 5.0F, deform),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(72, 0).addBox("thigh_front_l", -2.5F, 0.0F, -2.7F, 5.0F, 6.0F, 1.0F, deform)
                .texOffs(10, 0).addBox("thigh_outer_l", 1.9F, 0.0F, -2.0F, 1.0F, 6.0F, 4.0F, deform)
                .texOffs(40, 0).addBox("shin_shell_l", -2.5F, 6.0F, -2.5F, 5.0F, 3.0F, 5.0F, deform),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition createBootsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation deform = CubeDeformation.NONE;

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox("boot_shaft_r", -2.5F, 9.0F, -2.5F, 5.0F, 3.0F, 5.0F, deform)
                .texOffs(40, 0).addBox("boot_trim_r", -3.0F, 8.6F, -3.0F, 6.0F, 1.0F, 6.0F, deform),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(20, 0).addBox("boot_shaft_l", -2.5F, 9.0F, -2.5F, 5.0F, 3.0F, 5.0F, deform)
                .texOffs(64, 0).addBox("boot_trim_l", -3.0F, 8.6F, -3.0F, 6.0F, 1.0F, 6.0F, deform),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }
}
