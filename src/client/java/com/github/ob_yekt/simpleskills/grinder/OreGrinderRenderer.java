package com.github.ob_yekt.simpleskills.grinder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

/**
 * Renders the equipment currently loaded in the Ore Grinder physically resting on top
 * of the machine's grinding surface, with smooth synchronized grinding motion and 3D sparks.
 */
public final class OreGrinderRenderer implements BlockEntityRenderer<OreGrinderBlockEntity, OreGrinderRenderer.GrinderRenderState> {
    private static final int FULL_BRIGHT = 0x00F000F0;

    private final ItemModelResolver itemModelResolver;

    public OreGrinderRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public GrinderRenderState createRenderState() {
        return new GrinderRenderState();
    }

    @Override
    public void extractRenderState(OreGrinderBlockEntity blockEntity, GrinderRenderState state,
                                   float partialTick, Vec3 cameraPosition,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);

        ItemStack input = blockEntity.getItem(OreGrinderBlockEntity.INPUT_SLOT);
        state.hasItem = !input.isEmpty();
        state.partialTick = partialTick;
        state.grindProgress = blockEntity.getGrindProgress();
        state.grindTime = Math.max(1, blockEntity.getGrindTime());

        boolean lit = blockEntity.getBlockState().hasProperty(OreGrinderBlock.LIT)
                && blockEntity.getBlockState().getValue(OreGrinderBlock.LIT);
        state.grinding = state.hasItem && (lit || blockEntity.isBurning()) && state.grindProgress > 0;

        Level level = blockEntity.getLevel();
        state.gameTime = level != null ? level.getGameTime() : 0L;
        if (level != null) {
            BlockPos abovePos = blockEntity.getBlockPos().above();
            state.topLightCoords = LightCoordsUtil.pack(
                    level.getBrightness(LightLayer.BLOCK, abovePos),
                    level.getBrightness(LightLayer.SKY, abovePos)
            );
        } else {
            state.topLightCoords = FULL_BRIGHT;
        }

        if (state.hasItem) {
            this.itemModelResolver.updateForTopItem(
                    state.item,
                    input,
                    ItemDisplayContext.FIXED,
                    level,
                    null,
                    (int) blockEntity.getBlockPos().asLong()
            );
        } else {
            state.item.clear();
        }
    }

    @Override
    public void submit(GrinderRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.hasItem || state.item.isEmpty()) return;

        float animTime = (state.gameTime % 24000L) + state.partialTick;
        float progressRatio = state.grinding
                ? Mth.clamp((state.grindProgress + state.partialTick) / (float) state.grindTime, 0.0F, 1.0F)
                : 0.0F;

        poseStack.pushPose();

        // Center on the top grinding surface (block top is Y = 1.0)
        float baseHeight = 1.024F;
        float sinkOffset = state.grinding ? (-0.014F * progressRatio) : 0.0F;
        float bobOffset = state.grinding ? (Mth.sin(animTime * 1.45F) * 0.0045F) : 0.0F;
        float jitterX = state.grinding ? (Mth.cos(animTime * 2.1F) * 0.004F) : 0.0F;
        float jitterZ = state.grinding ? (Mth.sin(animTime * 2.3F) * 0.004F) : 0.0F;

        poseStack.translate(0.5F + jitterX, baseHeight + sinkOffset + bobOffset, 0.5F + jitterZ);

        // Lay the equipment flat onto the grinding table
        poseStack.rotateDegrees(Axis.XP, 90.0F);

        if (state.grinding) {
            // Smooth continuous rotation around the vertical grinding axis
            poseStack.rotateDegrees(Axis.ZP, animTime * 3.2F);
        }

        float scale = 0.58F * (1.0F - 0.22F * progressRatio);
        poseStack.scale(scale, scale, scale);

        state.item.submit(poseStack, collector, state.topLightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();

        // 3D sparks and metal fragments orbiting the grinding contact zone while active
        if (state.grinding) {
            submitGrindingVfx(poseStack, collector, animTime);
        }
    }

    private static void submitGrindingVfx(PoseStack poseStack, SubmitNodeCollector collector, float animTime) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (matrix, vertices) -> {
            Matrix4fc transform = matrix.pose();

            // 6 arcing amber/bright-amber sparks (#E99A32 / #FFD16A)
            for (int i = 0; i < 6; i++) {
                float phase = ((animTime * 0.085F) + i * (1.0F / 6.0F)) % 1.0F;
                float angle = animTime * 0.22F + i * (float) (Math.PI * 2.0D / 6.0D);
                float radius = 0.14F + phase * 0.24F;
                float height = 1.025F + Mth.sin(phase * (float) Math.PI) * 0.14F;

                float px = 0.5F + Mth.cos(angle) * radius;
                float pz = 0.5F + Mth.sin(angle) * radius;
                float tailRadius = Math.max(0.08F, radius - 0.05F);
                float tx = 0.5F + Mth.cos(angle - 0.18F) * tailRadius;
                float tz = 0.5F + Mth.sin(angle - 0.18F) * tailRadius;
                float tailHeight = Math.max(1.01F, height - 0.025F);

                float alpha = 1.0F - phase * 0.85F;
                float r = 1.0F;
                float g = phase < 0.5F ? 0.82F : 0.60F;
                float b = phase < 0.5F ? 0.42F : 0.20F;

                drawSparkStreak(transform, vertices, tx, tailHeight, tz, px, height, pz, 0.008F, r, g, b, alpha);
            }

            // 4 small dark-steel / silver metal fragments (#848792 / #9B9EA8)
            for (int i = 0; i < 4; i++) {
                float phase = ((animTime * 0.06F) + i * 0.25F) % 1.0F;
                float angle = -animTime * 0.15F + i * (float) (Math.PI * 0.5D);
                float radius = 0.16F + phase * 0.18F;
                float fy = 1.045F - phase * 0.032F;
                float fx = 0.5F + Mth.cos(angle) * radius;
                float fz = 0.5F + Mth.sin(angle) * radius;

                float shade = (i % 2 == 0) ? 0.61F : 0.52F;
                float alpha = 1.0F - phase * 0.9F;
                drawFragmentCube(transform, vertices, fx, fy, fz, 0.011F, shade, shade + 0.02F, shade + 0.05F, alpha);
            }
        });
    }

    private static void drawSparkStreak(Matrix4fc matrix, VertexConsumer vertices,
                                        float x0, float y0, float z0,
                                        float x1, float y1, float z1,
                                        float halfWidth, float r, float g, float b, float a) {
        addQuad(matrix, vertices,
                x0 - halfWidth, y0, z0,
                x0 + halfWidth, y0, z0,
                x1 + halfWidth * 0.5F, y1, z1,
                x1 - halfWidth * 0.5F, y1, z1,
                r, g, b, a);
        addQuad(matrix, vertices,
                x0, y0, z0 - halfWidth,
                x0, y0, z0 + halfWidth,
                x1, y1, z1 + halfWidth * 0.5F,
                x1, y1, z1 - halfWidth * 0.5F,
                r, g, b, a);
    }

    private static void drawFragmentCube(Matrix4fc matrix, VertexConsumer vertices,
                                         float cx, float cy, float cz, float h,
                                         float r, float g, float b, float a) {
        addQuad(matrix, vertices, cx - h, cy + h, cz - h, cx + h, cy + h, cz - h,
                cx + h, cy + h, cz + h, cx - h, cy + h, cz + h, r, g, b, a);
        addQuad(matrix, vertices, cx - h, cy - h, cz - h, cx + h, cy - h, cz - h,
                cx + h, cy + h, cz - h, cx - h, cy + h, cz - h, r, g, b, a);
        addQuad(matrix, vertices, cx + h, cy - h, cz + h, cx - h, cy - h, cz + h,
                cx - h, cy + h, cz + h, cx + h, cy + h, cz + h, r, g, b, a);
    }

    private static void addQuad(Matrix4fc matrix, VertexConsumer vertices,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float r, float g, float b, float a) {
        vertices.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        vertices.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        vertices.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        vertices.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
    }

    public static final class GrinderRenderState extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public boolean hasItem;
        public boolean grinding;
        public int grindProgress;
        public int grindTime = OreGrinderBlockEntity.GRIND_TIME;
        public int topLightCoords = FULL_BRIGHT;
        public float partialTick;
        public long gameTime;
    }
}