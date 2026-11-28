package com.github.ob_yekt.simpleskills.abyss;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Solid shard geometry positioned around a generic living entity body. */
public final class CrimsonBlightModel<S extends LivingEntityRenderState> extends EntityModel<S> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("simpleskills", "crimson_blight_spikes"), "main");

    // {x, y, z, pitch, yaw, roll, length, width}; model pixels around the full humanoid body.
    // The Y range follows the vanilla humanoid rig (head near -8, feet near 24),
    // so the crystals surround the whole hitbox instead of clustering only at the torso.
    private static final float[][] SPIKE_SPECS = {
            // Level I: head, shoulders and upper torso.
            {  4.2F, -2.0F,  2.4F,  0.30F,  0.20F,  0.82F,  9.0F, 3.0F },
            { -4.2F, -2.0F,  2.4F,  0.30F, -0.20F, -0.82F,  9.0F, 3.0F },
            { -2.8F,  4.0F, -2.4F, -0.72F,  0.32F, -0.28F,  8.0F, 2.5F },
            {  0.0F, -7.0F,  0.0F,  0.10F,  0.18F,  0.05F,  8.0F, 2.8F },

            // Level II: abdomen, waist and upper legs.
            {  4.6F,  8.0F,  2.4F,  0.34F,  0.10F,  1.12F, 10.0F, 3.0F },
            { -4.6F,  8.0F,  2.4F,  0.34F, -0.10F, -1.12F, 10.0F, 3.0F },
            {  3.2F, 14.0F, -2.0F, -0.62F, -0.30F,  0.42F,  8.0F, 2.6F },
            { -2.3F, 17.0F, -1.0F, -0.22F, -0.10F, -0.62F,  7.0F, 2.4F },

            // Level III: lower legs and additional front/back flanks.
            { -3.4F, 22.0F,  2.0F,  0.28F, -0.12F, -1.38F,  9.0F, 2.8F },
            {  3.4F, 22.0F,  2.0F,  0.28F,  0.12F,  1.38F,  9.0F, 2.8F },
            {  2.5F,  3.0F,  1.0F,  0.38F,  0.45F,  0.74F,  7.0F, 2.3F },
            { -2.5F,  3.0F,  1.0F,  0.38F, -0.45F, -0.74F,  7.0F, 2.3F },
            {  0.0F, 25.0F, -1.8F, -0.82F,  0.12F,  0.16F,  8.0F, 2.6F }
    };

    private final List<ModelPart> spikes = new ArrayList<>();

    public CrimsonBlightModel(ModelPart root) {
        super(root);
        for (int i = 0; i < SPIKE_SPECS.length; i++) {
            this.spikes.add(root.getChild("spike_" + i));
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < SPIKE_SPECS.length; i++) {
            float[] spec = SPIKE_SPECS[i];
            float length = spec[6];
            float width = spec[7];
            float tipWidth = width * 0.58F;

            CubeListBuilder shard = CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-width * 0.5F, -length * 0.72F, -width * 0.5F,
                            width, length * 0.72F, width, CubeDeformation.NONE)
                    .texOffs(12, 0)
                    .addBox(-tipWidth * 0.5F, -length, -tipWidth * 0.5F,
                            tipWidth, length * 0.34F, tipWidth, CubeDeformation.NONE);

            root.addOrReplaceChild("spike_" + i, shard,
                    PartPose.offsetAndRotation(spec[0], spec[1], spec[2],
                            spec[3], spec[4], spec[5]));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    public void setCorruptionLevel(int amplifier) {
        int visible = switch (Math.min(amplifier, 2)) {
            case 0 -> 4;
            case 1 -> 8;
            default -> 13;
        };
        for (int i = 0; i < this.spikes.size(); i++) {
            this.spikes.get(i).visible = i < visible;
        }
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
    }
}
