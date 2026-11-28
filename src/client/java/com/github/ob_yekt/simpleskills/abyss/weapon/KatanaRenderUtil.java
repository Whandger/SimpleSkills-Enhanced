package com.github.ob_yekt.simpleskills.abyss.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Bridge that asks the vanilla item renderer to draw the three Blockbench-exported models
 * using the unified blended global pose in both third-person and first-person perspectives.
 */
public final class KatanaRenderUtil {
    private static final Identifier MODEL_SHEATHED = Identifier.fromNamespaceAndPath(
            "simpleskills", "katana_ultimo_juramento_sheathed");
    private static final Identifier MODEL_SAYA = Identifier.fromNamespaceAndPath(
            "simpleskills", "katana_ultimo_juramento_saya");
    private static final Identifier MODEL_BLADE = Identifier.fromNamespaceAndPath(
            "simpleskills", "katana_ultimo_juramento");

    /** The exported item models are about 0.48x the geo bones; this restores the authored size. */
    private static final float ITEM_TO_GEO_SCALE = 2.08F;
    /** Koiguchi (mouth of the saya), in item-model units. It is the origin of the saya bone. */
    private static final float KOIGUCHI_Y = 16.8F;
    /** Centre of the tsuba (guard), in item-model units. It is the origin of the katana bone. */
    private static final float GUARD_Y = 5.196F;

    private KatanaRenderUtil() {}

    public static void submitSheathed(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                      Player player, KatanaClientStanceState.RenderPose pose,
                                      int outlineColor, boolean bedrockFrame) {
        submit(poseStack, collector, light, player, MODEL_SHEATHED, pose, "saya", outlineColor, bedrockFrame);
    }

    public static void submitSaya(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                  Player player, KatanaClientStanceState.RenderPose pose,
                                  int outlineColor, boolean bedrockFrame) {
        submit(poseStack, collector, light, player, MODEL_SAYA, pose, "saya", outlineColor, bedrockFrame);
    }

    public static void submitBlade(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                   Player player, KatanaClientStanceState.RenderPose pose,
                                   int outlineColor, boolean bedrockFrame) {
        submit(poseStack, collector, light, player, MODEL_BLADE, pose, "katana", outlineColor, bedrockFrame);
    }

    public static void submitSheathed(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                      Player player, String clip, float seconds, int outlineColor) {
        submitSheathed(poseStack, collector, light, player, clip, seconds, outlineColor, false);
    }

    public static void submitSaya(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                  Player player, String clip, float seconds, int outlineColor) {
        submitSaya(poseStack, collector, light, player, clip, seconds, outlineColor, false);
    }

    public static void submitBlade(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                   Player player, String clip, float seconds, int outlineColor) {
        submitBlade(poseStack, collector, light, player, clip, seconds, outlineColor, false);
    }

    public static void submitSheathed(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                      Player player, String clip, float seconds, int outlineColor,
                                      boolean bedrockFrame) {
        submit(poseStack, collector, light, player, MODEL_SHEATHED,
                KatanaClientStanceState.RenderPose.simple(0, clip, seconds), "saya", outlineColor, bedrockFrame);
    }

    public static void submitSaya(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                  Player player, String clip, float seconds, int outlineColor,
                                  boolean bedrockFrame) {
        submit(poseStack, collector, light, player, MODEL_SAYA,
                KatanaClientStanceState.RenderPose.simple(0, clip, seconds), "saya", outlineColor, bedrockFrame);
    }

    public static void submitBlade(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                   Player player, String clip, float seconds, int outlineColor,
                                   boolean bedrockFrame) {
        submit(poseStack, collector, light, player, MODEL_BLADE,
                KatanaClientStanceState.RenderPose.simple(0, clip, seconds), "katana", outlineColor, bedrockFrame);
    }

    private static void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, Player player,
                               Identifier modelDefinition, KatanaClientStanceState.RenderPose pose,
                               String bone, int outlineColor, boolean bedrockFrame) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        ItemStack renderStack = player.getMainHandItem().copy();
        renderStack.set(DataComponents.ITEM_MODEL, modelDefinition);
        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(
                renderState, renderStack, ItemDisplayContext.NONE, player.level(), null, player.getId());

        boolean blade = MODEL_BLADE.equals(modelDefinition);
        poseStack.pushPose();

        if (bedrockFrame) {
            KatanaAnimationLibrary.applyBedrock(poseStack, pose, bone);
        } else {
            KatanaAnimationLibrary.applyFirstPerson(poseStack, pose, bone);
        }

        poseStack.scale(ITEM_TO_GEO_SCALE, ITEM_TO_GEO_SCALE, ITEM_TO_GEO_SCALE);
        if (blade) {
            poseStack.translate(0.0F, (GUARD_Y - 8.0F) / 16.0F, 0.0F);
            poseStack.rotateDegrees(Axis.ZP, 180.0F);
        } else {
            poseStack.translate(0.0F, -(KOIGUCHI_Y - 8.0F) / 16.0F, 0.0F);
        }

        renderState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }
}
