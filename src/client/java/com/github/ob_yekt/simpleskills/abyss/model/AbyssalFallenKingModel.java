package com.github.ob_yekt.simpleskills.abyss.model;

import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Wither-skeleton silhouette with an authored throne-sitting pose that blends into standing. */
public final class AbyssalFallenKingModel extends SkeletonModel<AbyssalFallenKingRenderState> {
    public AbyssalFallenKingModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(AbyssalFallenKingRenderState state) {
        super.setupAnim(state);
        float sit = Mth.clamp(state.sittingAmount, 0.0F, 1.0F);
        if (sit <= 0.0F) return;

        // Legs fold forward over the seat; arms rest on the throne's gilded armrests.
        this.rightLeg.xRot = Mth.lerp(sit, this.rightLeg.xRot, -1.38F);
        this.leftLeg.xRot = Mth.lerp(sit, this.leftLeg.xRot, -1.38F);
        this.rightLeg.yRot = Mth.lerp(sit, this.rightLeg.yRot, 0.12F);
        this.leftLeg.yRot = Mth.lerp(sit, this.leftLeg.yRot, -0.12F);
        this.rightArm.xRot = Mth.lerp(sit, this.rightArm.xRot, -0.72F);
        this.leftArm.xRot = Mth.lerp(sit, this.leftArm.xRot, -0.72F);
        this.rightArm.zRot = Mth.lerp(sit, this.rightArm.zRot, -0.10F);
        this.leftArm.zRot = Mth.lerp(sit, this.leftArm.zRot, 0.10F);
        this.body.xRot = Mth.lerp(sit, this.body.xRot, 0.04F);
    }
}
