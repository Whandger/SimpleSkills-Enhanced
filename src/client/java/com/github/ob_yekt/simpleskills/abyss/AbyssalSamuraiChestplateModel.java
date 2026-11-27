package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.client.Minecraft;
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
 * Custom 3D model for the Abyssal Samurai chestplate when equipped by a player.
 * Uses its own 256x128 atlas (abyssal_samurai_armor.png), because its UV layout differs
 * from the boss model's atlas (abyssal_samurai_gold.png). It copies the player's
 * humanoid body/arm transforms so shoulder plates, sleeves, and cuirass follow
 * movement and katana poses.
 */
public final class AbyssalSamuraiChestplateModel extends EntityModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    Identifier.fromNamespaceAndPath("simpleskills", "abyssal_samurai_chestplate"),
                    "main"
            );

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "simpleskills", "textures/entity/abyssal_samurai_armor.png"
    );

    private static AbyssalSamuraiChestplateModel instance;

    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public AbyssalSamuraiChestplateModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    /** Bake the registered layer lazily after the client model set is available. */
    public static AbyssalSamuraiChestplateModel getOrBake() {
        if (instance == null) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getEntityModels() != null) {
                instance = new AbyssalSamuraiChestplateModel(
                        minecraft.getEntityModels().bakeLayer(LAYER_LOCATION)
                );
            }
        }
        return instance;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation deform = CubeDeformation.NONE;

        // Player torso: standard humanoid body pivot scaled around the 8x12x4 torso.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                // Do cuirass, raised Munaita plastron, agatsura rings, and Nodowa gorget.
                .texOffs(0, 37).addBox("do_cuirass", -5.0F, 0.0F, -3.0F, 10.0F, 8.0F, 6.0F, deform)
                .texOffs(104, 32).addBox("munaita_plastron", -3.5F, 0.8F, -3.7F, 7.0F, 5.0F, 1.0F, deform)
                .texOffs(120, 9).addBox("agatsura_ring_l", 2.2F, -0.3F, -3.6F, 1.0F, 2.0F, 1.0F, deform)
                .texOffs(124, 9).addBox("agatsura_ring_r", -3.2F, -0.3F, -3.6F, 1.0F, 2.0F, 1.0F, deform)
                .texOffs(32, 37).addBox("nodowa_gorget", -4.2F, -0.8F, -3.1F, 8.4F, 2.0F, 6.2F, deform)
                // Back Do plate and sashimono mount.
                .texOffs(60, 38).addBox("back_do_plate", -4.5F, 0.5F, 2.6F, 9.0F, 7.0F, 1.0F, deform)
                .texOffs(122, 18).addBox("sashimono_mount", -1.0F, 1.5F, 3.3F, 2.0F, 4.0F, 1.0F, deform)
                // Abdominal lamellae.
                .texOffs(80, 38).addBox("abdomen_lamellae", -4.3F, 7.2F, -2.3F, 8.6F, 3.0F, 4.6F, deform)
                .texOffs(32, 45).addBox("abdomen_trim", -4.4F, 6.9F, -2.5F, 8.8F, 1.0F, 5.0F, deform)
                // Crimson obi sash, rear knot, and amber belt gem.
                .texOffs(58, 47).addBox("obi_sash", -4.6F, 9.8F, -2.6F, 9.2F, 2.2F, 5.2F, deform)
                .texOffs(104, 45).addBox("obi_knot_back", -2.5F, 9.4F, 2.2F, 5.0F, 3.0F, 2.0F, deform)
                .texOffs(104, 28).addBox("belt_buckle_gem", -1.5F, 9.8F, -3.1F, 3.0F, 2.0F, 1.0F, deform),
                PartPose.ZERO);

        // Compact Kusazuri waist plates follow the torso pivot.
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

    /** Copy the already-posed vanilla player model, including custom katana arm poses. */
    public void copyFromHumanoid(HumanoidModel<?> humanoid) {
        copyTransform(humanoid.body, this.body);
        copyTransform(humanoid.rightArm, this.rightArm);
        copyTransform(humanoid.leftArm, this.leftArm);
    }

    private static void copyTransform(ModelPart source, ModelPart target) {
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
        target.visible = source.visible;
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        // Poses are copied from the already animated player model in the equipment renderer mixin.
    }
}
