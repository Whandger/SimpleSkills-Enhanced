package com.github.ob_yekt.simpleskills.abyss.weapon;

import net.minecraft.world.entity.HumanoidArm;

/**
 * Where the hands grip the katana, in first-person camera space (blocks).
 *
 * <p>The grip points are projected from the same third-person-authored global bones every frame.
 * There are no first-person-only animation tracks.
 */
public final class KatanaGrip {
    /** Main hand, drawn: the grip just below the guard, in bone-local blocks along Y. */
    public static final float MAIN_HAND_ALONG_TSUKA = -0.42F;
    /** Off hand, drawn: lower on the tsuka, close to the pommel. */
    public static final float OFF_HAND_ALONG_TSUKA = -0.47F;
    /**
     * Off hand, sheathed / relaxed: a point on the saya, measured from its koiguchi toward the tip (blocks).
     * Negative values go toward the tip, which is the -Y direction of the saya bone.
     */
    public static final float SAYA_GRIP_ALONG = -0.15F;

    public static final float WRIST_OUT = 0.44F;
    public static final float WRIST_UP = 0.67F;
    public static final float VANILLA_WRIST_DISTANCE = 0.74F;
    public static final float HAND_SCALE_MIN = 0.5F;
    public static final float HAND_SCALE_MAX = 1.0F;

    /** Time for the off hand to slide between the saya and the tsuka. */
    public static final float GRIP_BLEND_SECONDS = 0.35F;
    /** Fast re-engagement blend when attacking from lower_guard. */
    public static final float COMBAT_REENGAGE_SECONDS = 0.10F;
    /** Time for the main hand to grow in and out of view. */
    public static final float HAND_GROW_SECONDS = 0.2F;
    /** Length of the sheath clip. */
    private static final float SHEATH_SECONDS = 1.35F;

    private KatanaGrip() {}

    /** Point on the katana tsuka, {@code along} blocks from the bone origin, in view space. */
    public static float[] point(KatanaClientStanceState.RenderPose pose, float along) {
        return KatanaAnimationLibrary.firstPersonBonePoint(pose, "katana", along);
    }

    /** Point on the saya, {@code along} blocks from its koiguchi, in view space. */
    public static float[] sayaPoint(KatanaClientStanceState.RenderPose pose, float along) {
        return KatanaAnimationLibrary.firstPersonBonePoint(pose, "saya", along);
    }

    public static float[] point(String clip, float seconds, float along) {
        return KatanaAnimationLibrary.firstPersonBonePoint(clip, "katana", seconds, along);
    }

    public static float[] sayaPoint(String clip, float seconds, float along) {
        return KatanaAnimationLibrary.firstPersonBonePoint(clip, "saya", seconds, along);
    }

    public static float[] wristFromArmOrigin(HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        return new float[]{-WRIST_OUT * side, WRIST_UP, 0.0F};
    }

    /**
     * How much the off hand is on the tsuka (1) or on the saya (0).
     * <ul>
     *   <li>{@code DRAWING}: slides from saya (0) to tsuka (1) over {@code GRIP_BLEND_SECONDS}.</li>
     *   <li>{@code UNSHEATHED} / {@code ATTACK_1..3}: on tsuka (1), blending smoothly from 0 if
     *       attacking out of {@code LOWER_GUARD}.</li>
     *   <li>{@code LOWER_GUARD}: slides smoothly back to the saya (1 -> 0 over 0.35s) as the warrior
     *       relaxes into one-handed low guard, keeping the katana in the main hand.</li>
     *   <li>{@code SHEATHING}: stays on the saya (0) to guide the blade back into the scabbard.</li>
     * </ul>
     */
    public static float tsukaWeight(KatanaClientStanceState.RenderPose pose) {
        int stance = pose.stance();
        float seconds = pose.elapsedSeconds();
        switch (stance) {
            case KatanaStanceManager.DRAWING:
                return smooth(seconds / GRIP_BLEND_SECONDS);
            case KatanaStanceManager.UNSHEATHED:
                return 1.0F;
            case KatanaStanceManager.ATTACK_1:
            case KatanaStanceManager.ATTACK_2:
            case KatanaStanceManager.ATTACK_3:
                if (KatanaAnimationLibrary.LOWER_GUARD_CLIP.equals(pose.blendFromClip())
                        || KatanaAnimationLibrary.WALK_CLIP.equals(pose.blendFromClip())) {
                    return smooth(seconds / COMBAT_REENGAGE_SECONDS);
                }
                return 1.0F;
            case KatanaStanceManager.LOWER_GUARD:
                return 1.0F - smooth(seconds / GRIP_BLEND_SECONDS);
            case KatanaStanceManager.SHEATHING:
                return 0.0F;
            default:
                return 0.0F;
        }
    }

    /** Overload kept for single-clip callers. */
    public static float tsukaWeight(int stance, float seconds) {
        return tsukaWeight(KatanaClientStanceState.RenderPose.simple(
                stance, KatanaAnimationLibrary.clipFor(stance), seconds));
    }

    /**
     * Growth factor of the main hand, from 0 (hidden) to 1 (fully shown). Remains 1.0 throughout
     * {@code UNSHEATHED}, {@code ATTACK_1..3}, and {@code LOWER_GUARD} because the katana stays in hand.
     */
    public static float mainHandGrowth(int stance, float seconds) {
        switch (stance) {
            case KatanaStanceManager.DRAWING:
                return smooth(seconds / HAND_GROW_SECONDS);
            case KatanaStanceManager.UNSHEATHED:
            case KatanaStanceManager.LOWER_GUARD:
            case KatanaStanceManager.ATTACK_1:
            case KatanaStanceManager.ATTACK_2:
            case KatanaStanceManager.ATTACK_3:
                return 1.0F;
            case KatanaStanceManager.SHEATHING:
                return 1.0F - smooth((seconds - (SHEATH_SECONDS - HAND_GROW_SECONDS)) / HAND_GROW_SECONDS);
            default:
                return 0.0F;
        }
    }

    public static float handScale(float[] grip) {
        float distance = (float) Math.sqrt(grip[0] * grip[0] + grip[1] * grip[1] + grip[2] * grip[2]);
        float scale = distance / VANILLA_WRIST_DISTANCE;
        return Math.max(HAND_SCALE_MIN, Math.min(HAND_SCALE_MAX, scale));
    }

    public static float[] lerp(float[] a, float[] b, float t) {
        return new float[]{
                a[0] + (b[0] - a[0]) * t,
                a[1] + (b[1] - a[1]) * t,
                a[2] + (b[2] - a[2]) * t};
    }

    private static float smooth(float x) {
        float t = Math.max(0.0F, Math.min(1.0F, x));
        return t * t * (3.0F - 2.0F * t);
    }
}
