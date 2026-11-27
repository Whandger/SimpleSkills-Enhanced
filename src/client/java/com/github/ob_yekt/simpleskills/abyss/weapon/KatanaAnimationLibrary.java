package com.github.ob_yekt.simpleskills.abyss.weapon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads the supplied Blockbench/Bedrock keyframes from {@code animations/manifest.json} and
 * evaluates Catmull-Rom curves, additive walk-cycle overlays, and smooth cross-fades in Java.
 */
public final class KatanaAnimationLibrary {
    private static final String RESOURCE_ROOT = "/assets/simpleskills/katana/animations/";
    private static final String MANIFEST_RESOURCE = RESOURCE_ROOT + "manifest.json";
    public static final String COMBAT_GUARD_CLIP = "animation.katana_ultimo_juramento.idle_unsheathed";
    public static final String WALK_CLIP = "animation.katana_ultimo_juramento.walk";
    public static final String LOWER_GUARD_CLIP = "animation.katana_ultimo_juramento.lower_guard";
    private static final Map<String, Clip> CLIPS = load();

    /*
     * First-person is a camera projection of the same player rig, not a second animation.
     * These constants convert the player model's feet-relative Blockbench coordinates into
     * the first-person camera frame used by vanilla's hand renderer.
     */
    private static final float BODY_PIVOT_Y_PIXELS = 24.0F;
    private static final float PLAYER_EYE_HEIGHT_PIXELS = 25.92F; // 1.62 blocks, standing player
    private static final float FIRST_PERSON_FORWARD_OFFSET = -0.45F;

    private KatanaAnimationLibrary() {}

    /** Returns the one authoritative clip for both third-person and first-person rendering. */
    public static String clipFor(int stance) {
        String suffix = switch (stance) {
            case KatanaStanceManager.DRAWING -> "draw_katana";
            case KatanaStanceManager.UNSHEATHED -> "idle_unsheathed";
            case KatanaStanceManager.SHEATHING -> "sheath_katana";
            case KatanaStanceManager.LOWER_GUARD -> "lower_guard";
            case KatanaStanceManager.ATTACK_1 -> "attack_1_horizontal";
            case KatanaStanceManager.ATTACK_2 -> "attack_2_rising_diagonal";
            case KatanaStanceManager.ATTACK_3 -> "attack_3_overhead_finisher";
            case KatanaStanceManager.SPECIAL_DASH -> "special_sheathed";
            case KatanaStanceManager.SPECIAL_RECOVERY -> "special_sheathed_recovery";
            case KatanaStanceManager.LAKAI_READY -> "idle_lakai_sheathed";
            case KatanaStanceManager.LAKAI_SHEATHING -> "sheath_katana";
            default -> "idle_sheathed";
        };
        return "animation.katana_ultimo_juramento." + suffix;
    }

    /**
     * Compatibility overload for callers from older source trees. The firstPerson argument is
     * deliberately ignored: both perspectives must resolve to the same third-person-authored clip.
     */
    @Deprecated
    public static String clipFor(int stance, boolean firstPerson) {
        return clipFor(stance);
    }

    public static float duration(String clipName) {
        Clip clip = CLIPS.get(clipName);
        return clip == null ? 0.0F : clip.length;
    }

    public static float[] sample(String clipName, String bone, String channel, float seconds, float[] fallback) {
        Clip clip = CLIPS.get(clipName);
        if (clip == null) return fallback.clone();
        Track track = clip.track(bone, channel);
        if (track == null) return fallback.clone();
        float time = seconds;
        if (clip.loop && clip.length > 0.0F) {
            time = time % clip.length;
            if (time < 0.0F) time += clip.length;
        } else {
            time = Math.max(0.0F, Math.min(clip.length, time));
        }
        return track.sample(time, fallback.length);
    }

    /**
     * Samples a bone channel for a {@link KatanaClientStanceState.RenderPose}, combining:
     * 1) Active stance clip,
     * 2) Additive rhythmic {@code walk} cycle delta anchored to {@code idle_unsheathed(0.0)}, and
     * 3) Smooth cross-fade from {@code blendFromClip} when recovering or canceling into combat.
     */
    public static float[] sampleBlended(KatanaClientStanceState.RenderPose pose,
                                        String bone, String channel, float[] fallback) {
        float[] current = sample(pose.clip(), bone, channel, pose.elapsedSeconds(), fallback);
        if (pose.walkWeight() > 0.001F && !"scale".equals(channel)) {
            float[] walkSample = sample(WALK_CLIP, bone, channel, pose.walkPhaseSeconds(), fallback);
            float[] combatBase = sample(COMBAT_GUARD_CLIP, bone, channel, 0.0F, fallback);
            float w = pose.walkWeight();
            for (int i = 0; i < current.length; i++) {
                current[i] += w * (walkSample[i] - combatBase[i]);
            }
        }
        if (pose.blendFromClip() != null && pose.blendWeight() < 0.999F) {
            float[] previous = sample(pose.blendFromClip(), bone, channel, pose.blendFromSeconds(), fallback);
            float t = pose.blendWeight();
            for (int i = 0; i < current.length; i++) {
                current[i] = previous[i] + (current[i] - previous[i]) * t;
            }
        }
        return current;
    }

    /** Applies the blended global Blockbench/Bedrock bone in player-model space (third person). */
    public static void applyBedrock(PoseStack poseStack, KatanaClientStanceState.RenderPose pose, String bone) {
        float[] position = sampleBlended(pose, bone, "position", new float[]{0, 0, 0});
        float[] rotation = sampleBlended(pose, bone, "rotation", new float[]{0, 0, 0});
        float[] scale = sampleBlended(pose, bone, "scale", new float[]{1, 1, 1});
        poseStack.translate(position[0] / 16.0F, position[1] / 16.0F, position[2] / 16.0F);
        poseStack.rotateDegrees(Axis.ZP, rotation[2]);
        poseStack.rotateDegrees(Axis.YP, rotation[1]);
        poseStack.rotateDegrees(Axis.XP, rotation[0]);
        poseStack.scale(scale[0], scale[1], scale[2]);
    }

    /** Overload kept for direct single-clip callers. */
    public static void applyBedrock(PoseStack poseStack, String clipName, String bone, float seconds) {
        applyBedrock(poseStack, KatanaClientStanceState.RenderPose.simple(0, clipName, seconds), bone);
    }

    /**
     * Projects the same blended global Blockbench bone to the first-person camera.
     * No first-person keyframe data is read here.
     */
    public static void applyFirstPerson(PoseStack poseStack, KatanaClientStanceState.RenderPose pose, String bone) {
        float[] position = sampleBlended(pose, bone, "position", new float[]{0, 0, 0});
        float[] rotation = sampleBlended(pose, bone, "rotation", new float[]{0, 0, 0});
        float[] scale = sampleBlended(pose, bone, "scale", new float[]{1, 1, 1});
        float[] bodyRotation = sampleBlended(pose, "body", "rotation", new float[]{0, 0, 0});

        float[] cameraPosition = playerPointToFirstPerson(applyBodyTransform(position, bodyRotation));
        poseStack.translate(cameraPosition[0], cameraPosition[1], cameraPosition[2]);

        poseStack.rotateDegrees(Axis.YP, 180.0F);
        applyBedrockRotation(poseStack, bodyRotation);
        applyBedrockRotation(poseStack, rotation);
        poseStack.scale(scale[0], scale[1], scale[2]);
    }

    /** Overload kept for direct single-clip callers. */
    public static void applyFirstPerson(PoseStack poseStack, String clipName, String bone, float seconds) {
        applyFirstPerson(poseStack, KatanaClientStanceState.RenderPose.simple(0, clipName, seconds), bone);
    }

    /**
     * Returns a point along a blended global animation bone in first-person camera coordinates.
     * Used to place the visible hands on the exact same animated katana/saya grip points as third person.
     */
    public static float[] firstPersonBonePoint(KatanaClientStanceState.RenderPose pose, String bone, float localY) {
        float[] position = sampleBlended(pose, bone, "position", new float[]{0, 0, 0});
        float[] rotation = sampleBlended(pose, bone, "rotation", new float[]{0, 0, 0});
        float[] bodyRotation = sampleBlended(pose, "body", "rotation", new float[]{0, 0, 0});
        float[] local = rotateBedrock(new float[]{0.0F, localY, 0.0F}, rotation);
        float[] playerPoint = new float[]{
                position[0] + local[0],
                position[1] + local[1],
                position[2] + local[2]
        };
        return playerPointToFirstPerson(applyBodyTransform(playerPoint, bodyRotation));
    }

    /** Overload kept for direct single-clip callers. */
    public static float[] firstPersonBonePoint(String clipName, String bone, float seconds, float localY) {
        return firstPersonBonePoint(
                KatanaClientStanceState.RenderPose.simple(0, clipName, seconds), bone, localY);
    }

    private static void applyBedrockRotation(PoseStack poseStack, float[] rotation) {
        poseStack.rotateDegrees(Axis.ZP, rotation[2]);
        poseStack.rotateDegrees(Axis.YP, rotation[1]);
        poseStack.rotateDegrees(Axis.XP, rotation[0]);
    }

    private static float[] applyBodyTransform(float[] playerPoint, float[] bodyRotation) {
        float[] relativeToBodyPivot = new float[]{
                playerPoint[0],
                playerPoint[1] - BODY_PIVOT_Y_PIXELS,
                playerPoint[2]
        };
        float[] rotated = rotateBedrock(relativeToBodyPivot, bodyRotation);
        return new float[]{
                rotated[0],
                rotated[1] + BODY_PIVOT_Y_PIXELS,
                rotated[2]
        };
    }

    private static float[] playerPointToFirstPerson(float[] playerPointPixels) {
        return new float[]{
                -playerPointPixels[0] / 16.0F,
                (playerPointPixels[1] - PLAYER_EYE_HEIGHT_PIXELS) / 16.0F,
                -playerPointPixels[2] / 16.0F + FIRST_PERSON_FORWARD_OFFSET
        };
    }

    private static float[] rotateBedrock(float[] vector, float[] degrees) {
        float[] rotated = rotateX(vector, degrees[0]);
        rotated = rotateY(rotated, degrees[1]);
        return rotateZ(rotated, degrees[2]);
    }

    private static float[] rotateX(float[] vector, float degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new float[]{vector[0], (float) (cosine * vector[1] - sine * vector[2]),
                (float) (sine * vector[1] + cosine * vector[2])};
    }

    private static float[] rotateY(float[] vector, float degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new float[]{(float) (cosine * vector[0] + sine * vector[2]), vector[1],
                (float) (-sine * vector[0] + cosine * vector[2])};
    }

    private static float[] rotateZ(float[] vector, float degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new float[]{(float) (cosine * vector[0] - sine * vector[1]),
                (float) (sine * vector[0] + cosine * vector[1]), vector[2]};
    }

    /** Loads one JSON per animation clip using the explicit manifest (jar-safe; no folder scan). */
    private static Map<String, Clip> load() {
        try (InputStream manifestStream = KatanaAnimationLibrary.class.getResourceAsStream(MANIFEST_RESOURCE)) {
            if (manifestStream == null) {
                throw new IllegalStateException("Missing katana animation manifest: " + MANIFEST_RESOURCE);
            }
            JsonObject manifest = JsonParser.parseReader(
                    new InputStreamReader(manifestStream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject clipResources = manifest.getAsJsonObject("clips");
            if (clipResources == null || clipResources.size() == 0) {
                throw new IllegalStateException("Katana animation manifest has no clips: " + MANIFEST_RESOURCE);
            }

            Map<String, Clip> result = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : clipResources.entrySet()) {
                String clipId = entry.getKey();
                if (!entry.getValue().isJsonPrimitive()) {
                    throw new IllegalStateException("Invalid resource path for katana clip " + clipId);
                }
                String relativePath = entry.getValue().getAsString();
                if (relativePath.isBlank() || relativePath.startsWith("/")
                        || relativePath.contains("..") || relativePath.contains("\\")) {
                    throw new IllegalStateException("Unsafe resource path for katana clip " + clipId
                            + ": " + relativePath);
                }
                String resource = RESOURCE_ROOT + relativePath;
                try (InputStream clipStream = KatanaAnimationLibrary.class.getResourceAsStream(resource)) {
                    if (clipStream == null) {
                        throw new IllegalStateException("Missing katana animation clip " + clipId
                                + " at " + resource);
                    }
                    JsonObject root = JsonParser.parseReader(
                            new InputStreamReader(clipStream, StandardCharsets.UTF_8)).getAsJsonObject();
                    JsonObject animations = root.getAsJsonObject("animations");
                    JsonElement animationElement = animations == null ? null : animations.get(clipId);
                    if (animationElement == null || !animationElement.isJsonObject()) {
                        throw new IllegalStateException("File " + resource
                                + " does not contain the declared animation " + clipId);
                    }
                    if (result.put(clipId, parseClip(animationElement.getAsJsonObject())) != null) {
                        throw new IllegalStateException("Duplicate katana animation id in manifest: " + clipId);
                    }
                }
            }
            return Map.copyOf(result);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Clip parseClip(JsonObject animation) {
        float length = animation.has("animation_length")
                ? animation.get("animation_length").getAsFloat() : 0.0F;
        JsonElement loopElement = animation.get("loop");
        boolean loop = loopElement != null && loopElement.isJsonPrimitive()
                && loopElement.getAsJsonPrimitive().isBoolean() && loopElement.getAsBoolean();
        Map<String, Map<String, Track>> bones = new TreeMap<>();
        JsonObject bonesJson = animation.getAsJsonObject("bones");
        if (bonesJson != null) {
            for (Map.Entry<String, JsonElement> boneEntry : bonesJson.entrySet()) {
                JsonObject channelsJson = boneEntry.getValue().getAsJsonObject();
                Map<String, Track> channels = new TreeMap<>();
                for (Map.Entry<String, JsonElement> channelEntry : channelsJson.entrySet()) {
                    if (channelEntry.getValue().isJsonObject()) {
                        channels.put(channelEntry.getKey(), Track.parse(channelEntry.getValue().getAsJsonObject()));
                    }
                }
                bones.put(boneEntry.getKey(), channels);
            }
        }
        return new Clip(length, loop, bones);
    }

    private record Clip(float length, boolean loop, Map<String, Map<String, Track>> bones) {
        private Track track(String bone, String channel) {
            Map<String, Track> channels = bones.get(bone);
            return channels == null ? null : channels.get(channel);
        }
    }

    private record Knot(float time, float[] value) {}

    private static final class Track {
        private final List<Knot> knots;

        private Track(List<Knot> knots) {
            this.knots = knots;
        }

        private static Track parse(JsonObject json) {
            List<Knot> knots = new ArrayList<>();
            for (Map.Entry<String, JsonElement> keyframe : json.entrySet()) {
                float time;
                try {
                    time = Float.parseFloat(keyframe.getKey());
                } catch (NumberFormatException ignored) {
                    continue;
                }
                JsonElement value = keyframe.getValue();
                if (value.isJsonObject()) {
                    JsonObject keyframeObject = value.getAsJsonObject();
                    value = keyframeObject.has("post") ? keyframeObject.get("post") : keyframeObject.get("pre");
                }
                if (value == null || !value.isJsonArray()) continue;
                float[] vector = new float[value.getAsJsonArray().size()];
                for (int i = 0; i < vector.length; i++) vector[i] = value.getAsJsonArray().get(i).getAsFloat();
                knots.add(new Knot(time, vector));
            }
            knots.sort(Comparator.comparingDouble(Knot::time));
            return new Track(List.copyOf(knots));
        }

        private float[] sample(float time, int fallbackSize) {
            if (knots.isEmpty()) return new float[fallbackSize];
            if (knots.size() == 1 || time <= knots.get(0).time) return knots.get(0).value.clone();
            int last = knots.size() - 1;
            if (time >= knots.get(last).time) return knots.get(last).value.clone();

            int index = 0;
            while (index < last - 1 && time > knots.get(index + 1).time) index++;
            Knot p1 = knots.get(index);
            Knot p2 = knots.get(index + 1);
            Knot p0 = knots.get(Math.max(0, index - 1));
            Knot p3 = knots.get(Math.min(last, index + 2));
            float span = Math.max(0.0001F, p2.time - p1.time);
            float t = Math.max(0.0F, Math.min(1.0F, (time - p1.time) / span));
            float[] out = new float[Math.min(p1.value.length, p2.value.length)];
            for (int i = 0; i < out.length; i++) {
                float a = p0.value[Math.min(i, p0.value.length - 1)];
                float b = p1.value[i];
                float c = p2.value[i];
                float d = p3.value[Math.min(i, p3.value.length - 1)];
                float t2 = t * t;
                float t3 = t2 * t;
                out[i] = 0.5F * ((2.0F * b) + (-a + c) * t
                        + (2.0F * a - 5.0F * b + 4.0F * c - d) * t2
                        + (-a + 3.0F * b - 3.0F * c + d) * t3);
            }
            return out;
        }
    }
}
