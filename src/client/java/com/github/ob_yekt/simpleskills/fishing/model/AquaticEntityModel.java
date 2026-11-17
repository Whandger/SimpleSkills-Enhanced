package com.github.ob_yekt.simpleskills.fishing.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public final class AquaticEntityModel extends EntityModel<LivingEntityRenderState> {

    public enum Shape {
        TROUT("trout", 64, 32, 21.0F, 0.72F, 0.45F, false),
        BASS("bass", 64, 32, 21.0F, 0.60F, 0.38F, false),
        CATFISH("catfish", 64, 32, 21.0F, 0.60F, 0.38F, false),
        PIKE("pike", 64, 32, 21.0F, 0.72F, 0.45F, false),
        CARP("carp", 64, 32, 21.0F, 0.60F, 0.38F, false),
        TUNA("tuna", 64, 64, 18.0F, 0.50F, 0.32F, false),
        SWORDFISH("swordfish", 64, 64, 18.0F, 0.50F, 0.32F, false),
        MAHI_MAHI("mahi_mahi", 64, 64, 18.0F, 0.50F, 0.32F, false),
        REEF_SHARK("reef_shark", 64, 64, 18.0F, 0.36F, 0.26F, false),
        GREAT_WHITE_SHARK("great_white_shark", 128, 64, 16.0F, 0.36F, 0.26F, false),
        WHALE_SHARK("whale_shark", 128, 128, 14.0F, 0.24F, 0.18F, false),
        HUMPBACK_WHALE("humpback_whale", 128, 128, 13.0F, 0.18F, 0.16F, true),
        ORCA("orca", 128, 128, 15.0F, 0.28F, 0.22F, true);

        public final String id;
        public final int texWidth;
        public final int texHeight;
        public final float yOffset;
        public final float freq;
        public final float amp;
        public final boolean isMammal;
        Shape(String id, int texWidth, int texHeight, float yOffset, float freq, float amp, boolean isMammal) {
            this.id = id;
            this.texWidth = texWidth;
            this.texHeight = texHeight;
            this.yOffset = yOffset;
            this.freq = freq;
            this.amp = amp;
            this.isMammal = isMammal;
        }
    }

    private final Shape shape;
    private final ModelPart front;
    private final ModelPart back;
    private final ModelPart tail;
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public AquaticEntityModel(ModelPart root, Shape shape) {
        super(root);
        this.shape = shape;
        this.front = root.getChild("front");
        this.back = this.front.hasChild("back") ? this.front.getChild("back") : null;
        this.tail = (this.back != null && this.back.hasChild("tail")) ? this.back.getChild("tail") : null;
        this.leftFin = this.front.hasChild("left_fin") ? this.front.getChild("left_fin") : null;
        this.rightFin = this.front.hasChild("right_fin") ? this.front.getChild("right_fin") : null;
    }

    public static LayerDefinition createLayer(Shape shape) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition front = root.addOrReplaceChild(
                "front",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, shape.yOffset, 0.0F)
        );

        switch (shape) {
            case TROUT -> buildTrout(front);
            case BASS -> buildBass(front);
            case CATFISH -> buildCatfish(front);
            case PIKE -> buildPike(front);
            case CARP -> buildCarp(front);
            case TUNA -> buildTuna(front);
            case SWORDFISH -> buildSwordfish(front);
            case MAHI_MAHI -> buildMahiMahi(front);
            case REEF_SHARK -> buildReefShark(front);
            case GREAT_WHITE_SHARK -> buildGreatWhiteShark(front);
            case WHALE_SHARK -> buildWhaleShark(front);
            case HUMPBACK_WHALE -> buildHumpbackWhale(front);
            case ORCA -> buildOrca(front);
        }

        return LayerDefinition.create(mesh, shape.texWidth, shape.texHeight);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float speed = state.isInWater ? 1.0F : 1.75F;
        float t = state.ageInTicks * this.shape.freq * speed;

        // The existing entity renderer handles the out-of-water pose.
        // Keep the model pivot stable here to avoid applying the rotation twice.
        this.front.zRot = 0.0F;
        this.front.y = this.shape.yOffset;

        if (this.shape.isMammal) {
            this.front.xRot = Mth.sin(t) * (this.shape.amp * 0.25F);
            this.front.yRot = 0.0F;
            if (this.back != null) {
                this.back.xRot = Mth.sin(t - 0.80F) * (this.shape.amp * 0.75F);
                this.back.yRot = 0.0F;
            }
            if (this.tail != null) {
                this.tail.xRot = Mth.sin(t - 1.50F) * this.shape.amp;
                this.tail.yRot = 0.0F;
            }
        } else {
            this.front.xRot = 0.0F;
            this.front.yRot = Mth.sin(t) * (this.shape.amp * 0.22F);
            if (this.back != null) {
                this.back.xRot = 0.0F;
                this.back.yRot = Mth.sin(t - 0.75F) * (this.shape.amp * 0.78F);
            }
            if (this.tail != null) {
                this.tail.xRot = 0.0F;
                this.tail.yRot = Mth.sin(t - 1.40F) * this.shape.amp;
            }
        }

        if (this.leftFin != null) {
            this.leftFin.zRot = -0.22F + Mth.cos(t * 0.80F) * 0.12F;
        }
        if (this.rightFin != null) {
            this.rightFin.zRot = 0.22F - Mth.cos(t * 0.80F) * 0.12F;
        }
    }

    private static void box(PartDefinition parent, String name, int u, int v, float x, float y, float z, int w, int h, int d) {
        parent.addOrReplaceChild(
                name,
                CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d),
                PartPose.ZERO
        );
    }

    private static PartDefinition rearSegment(PartDefinition front, int u, int v, int w, int h, int d, float pivotZ) {
        return front.addOrReplaceChild(
                "back",
                CubeListBuilder.create().texOffs(u, v).addBox(-w / 2.0F, -h / 2.0F, 0.0F, w, h, d),
                PartPose.offset(0.0F, 0.0F, pivotZ)
        );
    }

    private static void vFin(PartDefinition parent, String name, int u, int v, float x, float y, float z, int h, int d) {
        parent.addOrReplaceChild(
                name,
                CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, 0, h, d),
                PartPose.ZERO
        );
    }

    private static void caudalPlane(PartDefinition back, int u, int v, float pivotZ, int h, int d) {
        back.addOrReplaceChild(
                "tail",
                CubeListBuilder.create().texOffs(u, v).addBox(0.0F, -h / 2.0F, 0.0F, 0, h, d),
                PartPose.offset(0.0F, 0.0F, pivotZ)
        );
    }

    private static void pectorals(PartDefinition front, int uLeft, int vLeft, int uRight, int vRight,
                                  float sideX, float y, float z, int w, int d) {
        front.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create().texOffs(uLeft, vLeft).addBox(-w, 0.0F, 0.0F, w, 0, d),
                PartPose.offsetAndRotation(-sideX, y, z, 0.0F, 0.0F, -0.22F)
        );
        front.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create().texOffs(uRight, vRight).addBox(0.0F, 0.0F, 0.0F, w, 0, d),
                PartPose.offsetAndRotation(sideX, y, z, 0.0F, 0.0F, 0.22F)
        );
    }

    private static void mammalTail(PartDefinition back, float pivotZ,
                                   int uPed, int vPed, int pedW, int pedH, int pedD,
                                   int uFluke, int vFluke, int flukeW, int flukeD) {
        PartDefinition tail = back.addOrReplaceChild(
                "tail",
                CubeListBuilder.create().texOffs(uPed, vPed).addBox(-pedW / 2.0F, -pedH / 2.0F, 0.0F, pedW, pedH, pedD),
                PartPose.offset(0.0F, 0.0F, pivotZ)
        );
        tail.addOrReplaceChild(
                "flukes",
                CubeListBuilder.create().texOffs(uFluke, vFluke).addBox(-flukeW / 2.0F, 0.0F, 0.0F, flukeW, 0, flukeD),
                PartPose.offset(0.0F, 0.0F, pedD - 1.0F)
        );
    }

    private static void buildTrout(PartDefinition front) {
        box(front, "head", 0, 0, -1.5F, -2.0F, -8.0F, 3, 4, 3);
        box(front, "torso", 13, 0, -1.5F, -2.5F, -5.0F, 3, 5, 7);
        pectorals(front, 32, 0, 37, 0, 1.5F, 1.0F, -4.0F, 2, 2);
        vFin(front, "dorsal_fin", 44, -4, 0.0F, -4.5F, -3.0F, 2, 4);
        PartDefinition back = rearSegment(front, 0, 13, 2, 4, 6, 2.0F);
        vFin(back, "adipose_fin", 17, 11, 0.0F, -3.0F, 4.0F, 1, 2);
        vFin(back, "anal_fin", 20, 10, 0.0F, 2.0F, 2.0F, 2, 3);
        caudalPlane(back, 24, 8, 6.0F, 5, 5);
    }

    private static void buildBass(PartDefinition front) {
        box(front, "head", 0, 0, -2.0F, -2.5F, -9.0F, 4, 5, 4);
        box(front, "torso", 17, 0, -2.0F, -3.0F, -5.0F, 4, 6, 7);
        pectorals(front, 37, 0, 44, 0, 2.0F, 1.5F, -4.0F, 3, 3);
        vFin(front, "dorsal_fin", 54, -5, 0.0F, -6.0F, -4.0F, 3, 5);
        PartDefinition back = rearSegment(front, 0, 14, 3, 5, 5, 2.0F);
        vFin(back, "second_dorsal", 17, 10, 0.0F, -4.5F, 0.0F, 2, 4);
        vFin(back, "anal_fin", 22, 11, 0.0F, 2.5F, 1.0F, 2, 3);
        caudalPlane(back, 26, 9, 5.0F, 6, 5);
    }

    private static void buildCatfish(PartDefinition front) {
        box(front, "head", 0, 0, -2.5F, -2.0F, -9.0F, 5, 4, 4);
        box(front, "torso", 19, 0, -2.0F, -2.5F, -5.0F, 4, 5, 7);
        pectorals(front, 40, 0, 47, 0, 2.0F, 1.5F, -4.0F, 3, 2);
        vFin(front, "dorsal_fin", 56, -3, 0.0F, -5.5F, -3.0F, 3, 3);
        front.addOrReplaceChild("left_barbel", CubeListBuilder.create().texOffs(0, 13).addBox(-0.5F, -0.5F, -5.0F, 1, 1, 5), PartPose.offsetAndRotation(-2.5F, 1.0F, -8.0F, 0.15F, 0.45F, 0.00F));
        front.addOrReplaceChild("right_barbel", CubeListBuilder.create().texOffs(13, 13).addBox(-0.5F, -0.5F, -5.0F, 1, 1, 5), PartPose.offsetAndRotation(2.5F, 1.0F, -8.0F, 0.15F, -0.45F, 0.00F));
        PartDefinition back = rearSegment(front, 26, 13, 3, 4, 6, 2.0F);
        vFin(back, "adipose_fin", 45, 10, 0.0F, -3.0F, 3.0F, 1, 3);
        vFin(back, "anal_fin", 49, 8, 0.0F, 2.0F, 1.0F, 2, 5);
        caudalPlane(back, 55, 8, 6.0F, 5, 5);
    }

    private static void buildPike(PartDefinition front) {
        box(front, "snout", 0, 0, -1.0F, -1.0F, -13.0F, 2, 2, 3);
        box(front, "head", 11, 0, -1.5F, -2.0F, -10.0F, 3, 4, 4);
        box(front, "torso", 26, 0, -1.5F, -2.0F, -6.0F, 3, 4, 9);
        pectorals(front, 49, 0, 56, 0, 1.5F, 1.0F, -5.0F, 3, 2);
        PartDefinition back = rearSegment(front, 0, 14, 2, 4, 7, 3.0F);
        vFin(back, "dorsal_fin", 19, 10, 0.0F, -5.0F, 2.0F, 3, 4);
        vFin(back, "anal_fin", 24, 10, 0.0F, 2.0F, 2.0F, 2, 4);
        caudalPlane(back, 29, 9, 7.0F, 5, 5);
    }

    private static void buildCarp(PartDefinition front) {
        box(front, "head", 0, 0, -2.0F, -2.5F, -8.0F, 4, 5, 3);
        box(front, "torso", 15, 0, -2.0F, -3.5F, -5.0F, 4, 7, 7);
        pectorals(front, 35, 0, 42, 0, 2.0F, 2.0F, -4.0F, 3, 3);
        vFin(front, "dorsal_fin", 52, -7, 0.0F, -6.5F, -3.0F, 3, 7);
        PartDefinition back = rearSegment(front, 0, 15, 3, 5, 5, 2.0F);
        vFin(back, "anal_fin", 17, 12, 0.0F, 2.5F, 1.0F, 2, 3);
        caudalPlane(back, 21, 10, 5.0F, 6, 5);
    }

    private static void buildTuna(PartDefinition front) {
        box(front, "snout", 0, 0, -2.0F, -2.0F, -15.0F, 4, 4, 3);
        box(front, "head", 15, 0, -3.0F, -3.5F, -12.0F, 6, 7, 5);
        box(front, "torso", 0, 13, -3.5F, -4.5F, -7.0F, 7, 9, 10);
        pectorals(front, 32, 13, 45, 13, 3.5F, 1.5F, -6.0F, 6, 3);
        vFin(front, "dorsal_fin", 0, 28, 0.0F, -8.5F, -4.0F, 4, 5);
        PartDefinition back = rearSegment(front, 6, 33, 4, 6, 8, 3.0F);
        vFin(back, "second_dorsal", 31, 30, 0.0F, -6.0F, 0.0F, 3, 3);
        vFin(back, "anal_fin", 35, 30, 0.0F, 3.0F, 0.0F, 3, 3);
        vFin(back, "finlets", 39, 27, 0.0F, -4.0F, 3.0F, 2, 6);
        caudalPlane(back, 46, 27, 8.0F, 11, 6);
    }

    private static void buildSwordfish(PartDefinition front) {
        box(front, "bill", 0, 0, -0.5F, -0.5F, -28.0F, 1, 1, 13);
        box(front, "head", 29, 0, -2.5F, -3.0F, -15.0F, 5, 6, 5);
        box(front, "torso", 0, 15, -3.0F, -4.0F, -10.0F, 6, 8, 11);
        pectorals(front, 32, 15, 45, 15, 3.0F, 2.0F, -9.0F, 6, 3);
        vFin(front, "dorsal_fin", 0, 29, 0.0F, -11.0F, -8.0F, 7, 6);
        PartDefinition back = rearSegment(front, 7, 35, 4, 5, 9, 1.0F);
        vFin(back, "anal_fin", 34, 32, 0.0F, 2.5F, 3.0F, 2, 3);
        caudalPlane(back, 38, 29, 9.0F, 11, 6);
    }

    private static void buildMahiMahi(PartDefinition front) {
        box(front, "head", 0, 0, -2.5F, -4.5F, -13.0F, 5, 9, 5);
        box(front, "torso", 21, 0, -2.5F, -4.5F, -8.0F, 5, 9, 10);
        pectorals(front, 49, 0, -3, 20, 2.5F, 2.0F, -7.0F, 5, 3);
        vFin(front, "dorsal_fin", 11, 9, 0.0F, -8.5F, -9.0F, 4, 11);
        PartDefinition back = rearSegment(front, 23, 20, 3, 6, 8, 2.0F);
        vFin(back, "anal_fin", 46, 15, 0.0F, 3.0F, 1.0F, 2, 5);
        caudalPlane(back, 52, 14, 8.0F, 10, 6);
    }

    private static void buildReefShark(PartDefinition front) {
        box(front, "snout", 0, 0, -2.5F, -2.0F, -19.0F, 5, 4, 5);
        box(front, "head", 21, 0, -3.5F, -3.0F, -14.0F, 7, 6, 6);
        box(front, "torso", 0, 13, -4.0F, -4.0F, -8.0F, 8, 8, 11);
        pectorals(front, 35, 13, -4, 33, 4.0F, 2.0F, -6.0F, 7, 4);
        vFin(front, "dorsal_fin", 15, 28, 0.0F, -10.0F, -4.0F, 6, 5);
        PartDefinition back = rearSegment(front, 21, 33, 5, 5, 9, 3.0F);
        vFin(back, "second_dorsal", 50, 30, 0.0F, -5.5F, 4.0F, 3, 3);
        caudalPlane(back, 54, 25, 9.0F, 11, 8);
    }

    private static void buildGreatWhiteShark(PartDefinition front) {
        box(front, "snout", 0, 0, -3.0F, -2.5F, -23.0F, 6, 5, 5);
        box(front, "head", 23, 0, -4.5F, -4.0F, -18.0F, 9, 8, 7);
        box(front, "torso", 56, 0, -5.5F, -5.5F, -11.0F, 11, 11, 13);
        pectorals(front, 100, 0, -5, 25, 5.5F, 3.0F, -8.0F, 9, 5);
        vFin(front, "dorsal_fin", 19, 19, 0.0F, -13.5F, -6.0F, 8, 6);
        PartDefinition back = rearSegment(front, 26, 25, 6, 7, 10, 2.0F);
        vFin(back, "second_dorsal", 59, 22, 0.0F, -6.5F, 5.0F, 3, 3);
        caudalPlane(back, 63, 16, 10.0F, 14, 9);
    }

    private static void buildWhaleShark(PartDefinition front) {
        box(front, "head", 0, 0, -7.0F, -4.5F, -29.0F, 14, 9, 9);
        box(front, "torso", 47, 0, -8.0F, -6.5F, -20.0F, 16, 13, 18);
        pectorals(front, -7, 32, 20, 32, 8.0F, 3.5F, -15.0F, 13, 7);
        vFin(front, "dorsal_fin", 54, 25, 0.0F, -14.5F, -6.0F, 8, 7);
        PartDefinition back = rearSegment(front, 62, 32, 10, 9, 13, -2.0F);
        vFin(back, "second_dorsal", 109, 28, 0.0F, -8.5F, 7.0F, 4, 4);
        caudalPlane(back, 114, 21, 13.0F, 18, 11);
    }

    private static void buildHumpbackWhale(PartDefinition front) {
        box(front, "rostrum", 0, 0, -6.0F, -3.5F, -36.0F, 12, 7, 9);
        box(front, "head", 43, 0, -7.5F, -5.5F, -27.0F, 15, 11, 10);
        box(front, "torso", 0, 22, -8.5F, -7.0F, -17.0F, 17, 14, 20);
        pectorals(front, 69, 22, -6, 57, 8.5F, 3.0F, -12.0F, 16, 6);
        vFin(front, "dorsal_fin", 33, 51, 0.0F, -11.0F, -1.0F, 4, 6);
        PartDefinition back = rearSegment(front, 40, 57, 10, 9, 14, 3.0F);
        mammalTail(back, 14.0F, 89, 57, 6, 6, 5, -9, 81, 24, 9);
    }

    private static void buildOrca(PartDefinition front) {
        box(front, "rostrum", 0, 0, -4.0F, -3.0F, -28.0F, 8, 6, 5);
        box(front, "head", 27, 0, -5.5F, -4.5F, -23.0F, 11, 9, 8);
        box(front, "torso", 66, 0, -6.5F, -5.5F, -15.0F, 13, 11, 16);
        pectorals(front, -5, 28, 14, 28, 6.5F, 2.5F, -10.0F, 9, 5);
        vFin(front, "dorsal_fin", 38, 22, 0.0F, -15.5F, -7.0F, 10, 6);
        PartDefinition back = rearSegment(front, 45, 28, 8, 8, 12, 1.0F);
        mammalTail(back, 12.0F, 86, 28, 5, 5, 5, -8, 49, 18, 8);
    }
}
