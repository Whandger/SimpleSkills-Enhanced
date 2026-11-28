package com.github.ob_yekt.simpleskills.abyss.client;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
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

public class CrimsonColossusModel extends EntityModel<CrimsonColossusRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "crimson_colossus"),
            "main"
    );

    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart lowerJaw;
    private final ModelPart crystalCrown;

    private final ModelPart frontLeftLeg;
    private final ModelPart frontLeftKnee;
    private final ModelPart frontLeftFoot;

    private final ModelPart frontRightLeg;
    private final ModelPart frontRightKnee;
    private final ModelPart frontRightFoot;

    private final ModelPart backLeftLeg;
    private final ModelPart backLeftKnee;

    private final ModelPart backRightLeg;
    private final ModelPart backRightKnee;

    public CrimsonColossusModel(ModelPart root) {
        super(root);
        this.torso = root.getChild("torso");
        this.head = this.torso.getChild("head");
        this.lowerJaw = this.head.getChild("lower_jaw");
        this.crystalCrown = this.torso.getChild("crystal_crown");

        this.frontLeftLeg = this.torso.getChild("front_left_leg");
        this.frontLeftKnee = this.frontLeftLeg.getChild("front_left_knee");
        this.frontLeftFoot = this.frontLeftKnee.getChild("front_left_foot");

        this.frontRightLeg = this.torso.getChild("front_right_leg");
        this.frontRightKnee = this.frontRightLeg.getChild("front_right_knee");
        this.frontRightFoot = this.frontRightKnee.getChild("front_right_foot");

        this.backLeftLeg = this.torso.getChild("back_left_leg");
        this.backLeftKnee = this.backLeftLeg.getChild("back_left_knee");

        this.backRightLeg = this.torso.getChild("back_right_leg");
        this.backRightKnee = this.backRightLeg.getChild("back_right_knee");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition torso = root.addOrReplaceChild("torso",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-15.0F, -10.0F, -16.0F, 30.0F, 18.0F, 32.0F, CubeDeformation.NONE)
                        .texOffs(0, 50).addBox(-13.0F, -13.0F, -12.0F, 26.0F, 4.0F, 22.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 4.0F, 0.0F)
        );

        PartDefinition head = torso.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 76).addBox(-8.0F, -6.0F, -12.0F, 16.0F, 10.0F, 13.0F, CubeDeformation.NONE)
                        .texOffs(58, 76).addBox(-9.0F, -7.0F, -13.5F, 18.0F, 4.0F, 14.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -2.0F, -16.0F, 0.06F, 0.0F, 0.0F)
        );

        head.addOrReplaceChild("lower_jaw",
                CubeListBuilder.create()
                        .texOffs(58, 94).addBox(-7.0F, 0.0F, -11.5F, 14.0F, 4.0F, 11.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 3.0F, 0.0F)
        );

        PartDefinition crown = torso.addOrReplaceChild("crystal_crown",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, -11.0F, 1.0F)
        );

        crown.addOrReplaceChild("crystal_main",
                CubeListBuilder.create()
                        .texOffs(84, 124).addBox(-3.0F, -20.0F, -3.0F, 6.0F, 20.0F, 6.0F, CubeDeformation.NONE)
                        .texOffs(108, 124).addBox(-2.0F, -25.0F, -2.0F, 4.0F, 6.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 1.5F, -0.12F, 0.08F, 0.02F)
        );

        crown.addOrReplaceChild("crystal_flank_right",
                CubeListBuilder.create()
                        .texOffs(108, 134).addBox(-2.5F, -15.0F, -2.5F, 5.0F, 15.0F, 5.0F, CubeDeformation.NONE)
                        .texOffs(96, 147).addBox(-1.5F, -19.0F, -1.5F, 3.0F, 5.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-4.5F, 0.5F, 0.5F, -0.08F, -0.25F, -0.42F)
        );
        crown.addOrReplaceChild("crystal_flank_left",
                CubeListBuilder.create()
                        .texOffs(108, 134).addBox(-2.5F, -15.0F, -2.5F, 5.0F, 15.0F, 5.0F, CubeDeformation.NONE)
                        .texOffs(96, 147).addBox(-1.5F, -19.0F, -1.5F, 3.0F, 5.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(4.5F, 0.5F, 0.5F, -0.08F, 0.25F, 0.42F)
        );

        crown.addOrReplaceChild("crystal_mid_front_right",
                CubeListBuilder.create()
                        .texOffs(108, 134).addBox(-2.5F, -13.0F, -2.5F, 5.0F, 13.0F, 5.0F, CubeDeformation.NONE)
                        .texOffs(96, 147).addBox(-1.5F, -16.5F, -1.5F, 3.0F, 4.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.5F, 1.0F, -4.5F, -0.36F, 0.20F, -0.48F)
        );
        crown.addOrReplaceChild("crystal_mid_front_left",
                CubeListBuilder.create()
                        .texOffs(108, 134).addBox(-2.5F, -13.0F, -2.5F, 5.0F, 13.0F, 5.0F, CubeDeformation.NONE)
                        .texOffs(96, 147).addBox(-1.5F, -16.5F, -1.5F, 3.0F, 4.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.5F, 1.0F, -4.5F, -0.36F, -0.20F, 0.48F)
        );
        crown.addOrReplaceChild("crystal_mid_back_right",
                CubeListBuilder.create()
                        .texOffs(108, 134).addBox(-2.5F, -13.0F, -2.5F, 5.0F, 13.0F, 5.0F, CubeDeformation.NONE)
                        .texOffs(96, 147).addBox(-1.5F, -16.5F, -1.5F, 3.0F, 4.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.8F, 1.0F, 5.5F, 0.34F, -0.20F, -0.52F)
        );
        crown.addOrReplaceChild("crystal_mid_back_left",
                CubeListBuilder.create()
                        .texOffs(108, 134).addBox(-2.5F, -13.0F, -2.5F, 5.0F, 13.0F, 5.0F, CubeDeformation.NONE)
                        .texOffs(96, 147).addBox(-1.5F, -16.5F, -1.5F, 3.0F, 4.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.8F, 1.0F, 5.5F, 0.34F, 0.20F, 0.52F)
        );

        addShard(crown, "crystal_shard_0", -8.5F, 2.0F, -1.5F, -0.15F,  0.10F, -0.78F);
        addShard(crown, "crystal_shard_1",  8.5F, 2.0F, -1.5F, -0.15F, -0.10F,  0.78F);
        addShard(crown, "crystal_shard_2", -8.2F, 2.0F,  3.5F,  0.22F, -0.10F, -0.82F);
        addShard(crown, "crystal_shard_3",  8.2F, 2.0F,  3.5F,  0.22F,  0.10F,  0.82F);
        addShard(crown, "crystal_shard_4", -2.8F, 1.5F, -7.5F, -0.62F,  0.15F, -0.22F);
        addShard(crown, "crystal_shard_5",  2.8F, 1.5F, -7.5F, -0.62F, -0.15F,  0.22F);

        buildFrontLeg(torso, "front_left", 16.5F, false);
        buildFrontLeg(torso, "front_right", -16.5F, true);
        buildBackLeg(torso, "back_left", 15.5F, false);
        buildBackLeg(torso, "back_right", -15.5F, true);

        return LayerDefinition.create(mesh, 128, 160);
    }

    private static void addShard(PartDefinition parent, String name,
                                 float px, float py, float pz, float rx, float ry, float rz) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(96, 147).addBox(-1.5F, -9.5F, -1.5F, 3.0F, 10.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(px, py, pz, rx, ry, rz)
        );
    }

    private static void buildFrontLeg(PartDefinition torso, String prefix, float offsetX, boolean mirror) {
        float flare = mirror ? 0.07F : -0.07F;
        PartDefinition upper = torso.addOrReplaceChild(prefix + "_leg",
                CubeListBuilder.create().mirror(mirror)
                        .texOffs(0, 100).addBox(-5.5F, -2.0F, -6.0F, 11.0F, 12.0F, 12.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(offsetX, -2.0F, -9.0F, -0.06F, 0.0F, flare)
        );
        PartDefinition knee = upper.addOrReplaceChild(prefix + "_knee",
                CubeListBuilder.create().mirror(mirror)
                        .texOffs(46, 109).addBox(-4.5F, 0.0F, -5.0F, 9.0F, 11.0F, 10.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.06F, 0.0F, -flare)
        );
        knee.addOrReplaceChild(prefix + "_foot",
                CubeListBuilder.create().mirror(mirror)
                        .texOffs(84, 109).addBox(-5.0F, 0.0F, -6.0F, 10.0F, 3.0F, 11.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 10.0F, 0.0F)
        );
    }

    private static void buildBackLeg(PartDefinition torso, String prefix, float offsetX, boolean mirror) {
        float flare = mirror ? 0.05F : -0.05F;
        PartDefinition upper = torso.addOrReplaceChild(prefix + "_leg",
                CubeListBuilder.create().mirror(mirror)
                        .texOffs(0, 125).addBox(-5.0F, -2.0F, -6.0F, 10.0F, 11.0F, 12.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(offsetX, -1.0F, 10.0F, 0.08F, 0.0F, flare)
        );
        upper.addOrReplaceChild(prefix + "_knee",
                CubeListBuilder.create().mirror(mirror)
                        .texOffs(46, 132).addBox(-4.0F, 0.0F, -4.5F, 8.0F, 13.0F, 9.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 8.0F, 1.0F, -0.08F, 0.0F, -flare)
        );
    }

    /** Walk cycle frequency. Higher than the old stride so the hops come quickly. */
    private static final float GAIT_RATE = 1.45F;

    @Override
    public void setupAnim(CrimsonColossusRenderState state) {
        super.setupAnim(state);
        resetBasePose();

        boolean attacking = state.attackType != CrimsonColossusEntity.AttackType.NONE;
        // The walk cycle is muted during attacks so the attack pose reads cleanly.
        float speed = attacking ? 0.0F : Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
        float age = state.ageInTicks;
        float cadence = state.walkAnimationPos * GAIT_RATE;

        applyMonkeyGait(cadence, speed);
        if (!attacking) {
            applyIdleFidget(age, 1.0F - speed);
        }

        this.head.yRot = (state.yRot * Mth.DEG_TO_RAD) * 0.45F + Mth.sin(cadence) * 0.07F * speed;
        this.head.xRot = 0.08F + (state.xRot * Mth.DEG_TO_RAD) * 0.40F
                - Mth.cos(cadence * 2.0F) * 0.07F * speed;

        float t = state.attackTick + state.partialTick;
        switch (state.attackType) {
            case CRYSTAL_LASER -> applyLaserPose(t);
            case SEISMIC_SLAM -> applySeismicSlamPose(t);
            case CRYSTAL_BARRAGE -> applyCrystalBarragePose(t);
            case COLOSSUS_LEAP -> applyColossusLeapPose(t, state.impactTick);
            case SWIPE -> applySwipePose(t);
            default -> {
            }
        }
    }

    /**
     * Monkey gait: quick hops. Each cycle is one hop. The front hands tap down fast, the torso springs
     * up at the top of every bounce, and the back legs push off on the opposite beat.
     */
    private void applyMonkeyGait(float cadence, float speed) {
        float phaseA = cadence;
        float phaseB = cadence + Mth.PI;

        float liftA = snapLift(Mth.sin(phaseA)) * speed;
        float liftB = snapLift(Mth.sin(phaseB)) * speed;
        float swingA = Mth.cos(phaseA) * 0.78F * speed;
        float swingB = Mth.cos(phaseB) * 0.78F * speed;
        float hop = Math.abs(Mth.sin(cadence)) * speed;

        // Front hands: short, snappy taps. Each one lifts, flicks forward and plants.
        this.frontLeftLeg.xRot = -0.10F + swingA - liftA * 0.55F;
        this.frontLeftKnee.xRot = 0.10F + liftA * 1.05F;
        this.frontLeftFoot.xRot = -liftA * 0.45F;

        this.frontRightLeg.xRot = -0.10F + swingB - liftB * 0.55F;
        this.frontRightKnee.xRot = 0.10F + liftB * 1.05F;
        this.frontRightFoot.xRot = -liftB * 0.45F;

        // Back legs push off on the opposite beat, which is what turns each step into a hop.
        this.backRightLeg.xRot = 0.10F + swingA * 0.45F - liftA * 0.30F;
        this.backRightKnee.xRot = -0.10F + liftA * 0.90F;
        this.backLeftLeg.xRot = 0.10F + swingB * 0.45F - liftB * 0.30F;
        this.backLeftKnee.xRot = -0.10F + liftB * 0.90F;

        // The body springs up on every hop and leans forward, hunched like a primate.
        this.torso.y = 4.0F - hop * 2.6F;
        this.torso.xRot = 0.14F * speed + Mth.cos(cadence * 2.0F) * 0.035F * speed;
        this.torso.zRot = Mth.sin(cadence) * 0.05F * speed;

        this.lowerJaw.xRot = hop * 0.05F;
        this.crystalCrown.xRot = -0.03F * speed + Mth.sin(cadence * 2.0F - 0.7F) * 0.05F * speed;
        this.crystalCrown.zRot = -Mth.sin(cadence) * 0.04F * speed;
    }

    /** Standing still, the Colossus fidgets: quick hand flicks and the occasional small hop. */
    private void applyIdleFidget(float age, float idle) {
        float flickA = (float) Math.pow(Math.max(0.0F, Mth.sin(age * 0.23F)), 6.0D) * idle;
        float flickB = (float) Math.pow(Math.max(0.0F, Mth.sin(age * 0.31F + 1.3F)), 6.0D) * idle;
        float hop = (float) Math.pow(Math.max(0.0F, Mth.sin(age * 0.07F)), 14.0D) * idle;

        this.frontLeftLeg.xRot -= flickA * 0.35F;
        this.frontLeftKnee.xRot += flickA * 0.30F;
        this.frontRightLeg.xRot -= flickB * 0.30F;
        this.frontRightKnee.xRot += flickB * 0.25F;
        this.torso.y -= hop * 1.6F + Mth.sin(age * 0.08F) * 0.25F * idle;
    }

    /** Positive half of a wave, pushed toward 1: a fast lift that settles sharply. */
    private static float snapLift(float wave) {
        return (float) Math.pow(Math.max(0.0F, wave), 0.55D);
    }

    private void resetBasePose() {
        this.torso.y = 4.0F;
        this.torso.z = 0.0F;
        this.torso.xRot = 0.0F;
        this.torso.yRot = 0.0F;
        this.torso.zRot = 0.0F;
        this.lowerJaw.xRot = 0.0F;
        this.frontLeftLeg.zRot = -0.07F;
        this.frontRightLeg.zRot = 0.07F;
        this.frontLeftKnee.zRot = 0.07F;
        this.frontRightKnee.zRot = -0.07F;
        this.backLeftLeg.zRot = -0.05F;
        this.backRightLeg.zRot = 0.05F;
        this.backLeftKnee.zRot = 0.05F;
        this.backRightKnee.zRot = -0.05F;
    }

    private void applyLaserPose(float t) {
        float chargeEnd = CrimsonColossusEntity.LASER_CHARGE_END;
        float fireEnd = CrimsonColossusEntity.LASER_FIRE_END;
        float charge = smooth(0.0F, chargeEnd, t);
        float fire = smooth(chargeEnd, chargeEnd + 5.0F, t) * (1.0F - smooth(fireEnd - 6.0F, fireEnd + 6.0F, t));
        float recover = 1.0F - smooth(fireEnd, CrimsonColossusEntity.LASER_DURATION, t);

        this.torso.y = 4.0F + 1.4F * charge * recover;
        this.torso.xRot = 0.08F * charge * recover;

        this.frontLeftLeg.xRot = -0.22F * charge * recover;
        this.frontRightLeg.xRot = -0.22F * charge * recover;
        this.frontLeftKnee.xRot = 0.22F * charge * recover;
        this.frontRightKnee.xRot = 0.22F * charge * recover;

        this.backLeftLeg.xRot = 0.20F * charge * recover;
        this.backRightLeg.xRot = 0.20F * charge * recover;

        float recoil = fire > 0.01F ? Mth.sin(t * 2.6F) * 0.035F * fire : 0.0F;
        this.head.xRot = (-0.12F * charge + recoil) * recover;
        this.lowerJaw.xRot = (0.28F * charge + 0.42F * fire + Math.abs(recoil)) * recover;

        if (charge > 0.1F) {
            this.crystalCrown.zRot = Mth.sin(t * 3.1F) * 0.045F * recover;
        }
    }

    private void applySeismicSlamPose(float t) {
        float impact = CrimsonColossusEntity.SLAM_IMPACT_TICK;
        float rearUp = smooth(0.0F, 12.0F, t);
        float slamDown = smooth(13.0F, impact, t);
        float recover = 1.0F - smooth(impact + 6.0F, CrimsonColossusEntity.SLAM_DURATION, t);

        float standFactor = rearUp * (1.0F - slamDown);
        float impactFactor = slamDown * recover;

        this.torso.y = 4.0F - 7.5F * standFactor + 2.8F * impactFactor;
        this.torso.z = 5.0F * standFactor - 2.0F * impactFactor;
        this.torso.xRot = -0.66F * standFactor + 0.22F * impactFactor;

        this.backLeftLeg.xRot = 0.62F * standFactor - 0.15F * impactFactor;
        this.backRightLeg.xRot = 0.62F * standFactor - 0.15F * impactFactor;
        this.backLeftKnee.xRot = -0.25F * standFactor + 0.20F * impactFactor;
        this.backRightKnee.xRot = -0.25F * standFactor + 0.20F * impactFactor;

        this.frontLeftLeg.xRot = -2.15F * standFactor - 0.42F * impactFactor;
        this.frontRightLeg.xRot = -2.15F * standFactor - 0.42F * impactFactor;
        this.frontLeftLeg.zRot = -0.07F - 0.24F * standFactor;
        this.frontRightLeg.zRot = 0.07F + 0.24F * standFactor;

        this.frontLeftKnee.xRot = -0.45F * standFactor + 0.45F * impactFactor;
        this.frontRightKnee.xRot = -0.45F * standFactor + 0.45F * impactFactor;

        this.head.xRot = -0.25F * standFactor + 0.10F * impactFactor;
        this.lowerJaw.xRot = 0.48F * standFactor + 0.32F * impactFactor;
    }

    private void applyCrystalBarragePose(float t) {
        float raiseHand = smooth(0.0F, 7.0F, t);
        float castSweep = smooth(CrimsonColossusEntity.BARRAGE_WAVE_START, CrimsonColossusEntity.BARRAGE_WAVE_START + 5.0F, t);
        float recover = 1.0F - smooth(CrimsonColossusEntity.BARRAGE_WAVE_END, CrimsonColossusEntity.BARRAGE_DURATION, t);

        float active = raiseHand * recover;
        float cast = castSweep * recover;

        this.torso.y = 4.0F - 1.8F * active + 1.2F * cast;
        this.torso.xRot = -0.18F * active + 0.14F * cast;
        this.torso.yRot = -0.22F * active + 0.28F * cast;

        this.frontRightLeg.xRot = -1.85F * active + 1.10F * cast;
        this.frontRightLeg.zRot = 0.07F + 0.28F * active;
        this.frontRightKnee.xRot = -0.55F * active + 0.45F * cast;

        this.frontLeftLeg.xRot = -0.18F * active;
        this.frontLeftKnee.xRot = 0.22F * active;

        this.head.xRot = -0.10F * active + 0.06F * cast;
        this.lowerJaw.xRot = 0.25F * active;
    }

    /**
     * Leap: crouch, spring up, glide toward the player, then slam into the ground. The airborne pose
     * holds until the impact tick is known, so the legs stay extended for the whole flight.
     */
    private void applyColossusLeapPose(float t, int impactTick) {
        float launch = CrimsonColossusEntity.LEAP_LAUNCH_TICK;
        float crouch = smooth(0.0F, launch - 1.0F, t) * (1.0F - smooth(launch - 1.0F, launch + 2.0F, t));
        float airborne = impactTick < 0 ? smooth(launch, launch + 4.0F, t) : 0.0F;
        float landSlam = 0.0F;
        if (impactTick >= 0) {
            float since = t - impactTick;
            landSlam = smooth(0.0F, 4.0F, since) * (1.0F - smooth(10.0F, 20.0F, since));
        }

        this.torso.y = 4.0F + 4.2F * crouch - 2.5F * airborne + 3.6F * landSlam;
        this.torso.xRot = 0.18F * crouch - 0.24F * airborne + 0.22F * landSlam;

        this.frontLeftLeg.xRot = -0.45F * crouch - 0.95F * airborne - 0.35F * landSlam;
        this.frontRightLeg.xRot = -0.45F * crouch - 0.95F * airborne - 0.35F * landSlam;
        this.frontLeftKnee.xRot = 0.72F * crouch + 0.35F * airborne + 0.55F * landSlam;
        this.frontRightKnee.xRot = 0.72F * crouch + 0.35F * airborne + 0.55F * landSlam;

        this.backLeftLeg.xRot = 0.52F * crouch + 0.85F * airborne + 0.35F * landSlam;
        this.backRightLeg.xRot = 0.52F * crouch + 0.85F * airborne + 0.35F * landSlam;
        this.backLeftKnee.xRot = -0.68F * crouch - 0.25F * airborne - 0.45F * landSlam;
        this.backRightKnee.xRot = -0.68F * crouch - 0.25F * airborne - 0.45F * landSlam;

        this.head.xRot = -0.15F * airborne + 0.12F * landSlam;
        this.lowerJaw.xRot = 0.42F * airborne + 0.30F * landSlam;
    }

    /** Swipe: the right hand rears back, then sweeps across the front while the body turns into it. */
    private void applySwipePose(float t) {
        float impact = CrimsonColossusEntity.SWIPE_IMPACT_TICK;
        float rear = smooth(0.0F, impact - 1.0F, t);
        float strike = smooth(impact - 1.0F, impact + 1.0F, t);
        float recover = 1.0F - smooth(impact + 2.0F, CrimsonColossusEntity.SWIPE_DURATION, t);
        float armBack = rear * (1.0F - strike);

        this.frontRightLeg.xRot = (-0.95F * armBack + 0.75F * strike) * recover;
        this.frontRightKnee.xRot = (-0.55F * armBack + 0.45F * strike) * recover;
        this.frontRightLeg.zRot = 0.07F + 0.25F * armBack * recover;
        this.torso.yRot = (-0.30F * armBack + 0.55F * strike) * recover;
        this.torso.xRot = 0.10F * strike * recover;
        this.lowerJaw.xRot = 0.30F * strike * recover;
    }

    private static float smooth(float start, float end, float value) {
        if (end <= start) return value >= end ? 1.0F : 0.0F;
        float x = Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
