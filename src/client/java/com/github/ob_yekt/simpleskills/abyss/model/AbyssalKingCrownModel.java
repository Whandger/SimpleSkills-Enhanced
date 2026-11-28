package com.github.ob_yekt.simpleskills.abyss.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Small independent model layer that follows the skeleton head and supplies the 3D gold crown. */
public final class AbyssalKingCrownModel extends EntityModel<AbyssalFallenKingRenderState> {
    private final ModelPart head;

    public AbyssalKingCrownModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);

        // Thick royal band, slightly wider than the skull so it reads clearly at a distance.
        head.addOrReplaceChild("band",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.5F, -9.0F, -4.5F, 9.0F, 2.0F, 9.0F),
                PartPose.ZERO);

        // Five raised points give the crown a recognisable silhouette instead of a flat helmet rim.
        crownPoint(head, "front_point", -3.0F, -12.0F, -4.1F, 0, 16);
        crownPoint(head, "front_left_point", -4.0F, -11.0F, -2.0F, 10, 16);
        crownPoint(head, "front_right_point", 2.0F, -11.0F, -2.0F, 20, 16);
        crownPoint(head, "back_left_point", -4.0F, -11.0F, 1.8F, 30, 16);
        crownPoint(head, "back_right_point", 2.0F, -11.0F, 1.8F, 40, 16);

        // A red jewel fixed to the front face.
        head.addOrReplaceChild("ruby",
                CubeListBuilder.create().texOffs(52, 16)
                        .addBox(-1.0F, -8.8F, -4.85F, 2.0F, 2.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void crownPoint(PartDefinition head, String name, float x, float y, float z, int u, int v) {
        head.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v)
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(x + 1.0F, y + 1.0F, z + 1.0F));
    }

    public void copyHeadPose(ModelPart sourceHead) {
        this.head.loadPose(sourceHead.storePose());
    }

    @Override
    public void setupAnim(AbyssalFallenKingRenderState state) {
        // The head pose is copied from the animated skeleton model by the crown render layer.
    }
}
