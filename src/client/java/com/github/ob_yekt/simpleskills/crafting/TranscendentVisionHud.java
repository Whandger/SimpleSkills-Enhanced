package com.github.ob_yekt.simpleskills.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/** Client-side outlines for the Transcendent helmet and pickaxe scanners. */
public final class TranscendentVisionHud {
    private static final float BLOCK_OUTSET = 0.012F;
    private static final float EDGE_THICKNESS = 0.018F;
    private static final float WORLDBREAKER_OUTSET = 0.006F;
    private static final float WORLDBREAKER_EDGE_THICKNESS = 0.0075F;

    private static final RenderPipeline THROUGH_WALLS_BOXES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "simpleskills", "pipeline/transcendent_vision_boxes"))
                    .withDepthStencilState(Optional.empty())
                    .withCull(false)
                    .build()
    );

    private static final RenderType ORE_OUTLINE_RENDER_TYPE = RenderType.create(
            "simpleskills_transcendent_ore_outline",
            RenderSetup.builder(THROUGH_WALLS_BOXES).createRenderSetup()
    );

    private record OreMarker(String kind, int x, int y, int z) {}
    private record WorldbreakerMarker(int x, int y, int z) {}

    private record VisionState(boolean oracleActive, boolean breakerActive,
                               Set<UUID> outlinedEntities, List<OreMarker> ores) {
        private static VisionState empty() {
            return new VisionState(false, false, Set.of(), List.of());
        }
    }

    // A volatile immutable snapshot keeps the render thread independent from packet updates.
    private static volatile VisionState state = VisionState.empty();
    private static volatile List<WorldbreakerMarker> worldbreakerPreview = List.of();

    private TranscendentVisionHud() {}

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(TranscendentVisionHud::renderOreOutlines);
    }

    public static void accept(TranscendentVisionPayload payload) {
        try {
            JsonObject root = JsonParser.parseString(payload.json()).getAsJsonObject();
            boolean oracle = root.has("oracle") && root.get("oracle").getAsBoolean();
            boolean breaker = root.has("breaker") && root.get("breaker").getAsBoolean();

            Set<UUID> entities = new HashSet<>();
            JsonArray entityArray = root.has("entities") ? root.getAsJsonArray("entities") : new JsonArray();
            if (oracle) {
                for (JsonElement element : entityArray) {
                    entities.add(UUID.fromString(element.getAsString()));
                }
            }

            List<OreMarker> ores = new ArrayList<>();
            JsonArray oreArray = root.has("ores") ? root.getAsJsonArray("ores") : new JsonArray();
            for (JsonElement element : oreArray) {
                JsonObject ore = element.getAsJsonObject();
                ores.add(new OreMarker(
                        ore.get("kind").getAsString(),
                        ore.get("x").getAsInt(),
                        ore.get("y").getAsInt(),
                        ore.get("z").getAsInt()
                ));
            }

            state = new VisionState(oracle, breaker, Set.copyOf(entities), List.copyOf(ores));
        } catch (RuntimeException ignored) {
            state = VisionState.empty();
        }
    }

    public static void acceptWorldbreakerPreview(WorldbreakerPreviewPayload payload) {
        try {
            JsonArray blocks = JsonParser.parseString(payload.json()).getAsJsonArray();
            List<WorldbreakerMarker> markers = new ArrayList<>(blocks.size());
            for (JsonElement element : blocks) {
                JsonObject block = element.getAsJsonObject();
                markers.add(new WorldbreakerMarker(
                        block.get("x").getAsInt(),
                        block.get("y").getAsInt(),
                        block.get("z").getAsInt()
                ));
            }
            worldbreakerPreview = List.copyOf(markers);
        } catch (RuntimeException ignored) {
            worldbreakerPreview = List.of();
        }
    }

    public static void clear() {
        state = VisionState.empty();
        worldbreakerPreview = List.of();
    }

    /** Used by the EntityRenderer mixin to make hostile mobs receive a vanilla-style outline. */
    public static boolean isOutlined(Entity entity) {
        return entity != null && state.oracleActive() && state.outlinedEntities().contains(entity.getUUID());
    }

    private static void renderOreOutlines(LevelRenderContext context) {
        VisionState snapshot = state;
        List<WorldbreakerMarker> preview = worldbreakerPreview;
        boolean hasOreOutlines = (snapshot.oracleActive() || snapshot.breakerActive())
                && !snapshot.ores().isEmpty();
        if (!hasOreOutlines && preview.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        context.submitNodeCollector().submitCustomGeometry(
                context.poseStack(), ORE_OUTLINE_RENDER_TYPE,
                (pose, vertices) -> {
                    Matrix4fc matrix = pose.pose();
                    if (hasOreOutlines) {
                        for (OreMarker ore : snapshot.ores()) {
                            float red;
                            float green;
                            float blue;
                            switch (ore.kind()) {
                                case "diamond" -> {
                                    red = 0.25F;
                                    green = 0.88F;
                                    blue = 1.0F;
                                }
                                case "ancient_debris" -> {
                                    red = 1.0F;
                                    green = 0.62F;
                                    blue = 0.20F;
                                }
                                default -> {
                                    red = 0.78F;
                                    green = 0.42F;
                                    blue = 1.0F;
                                }
                            }

                            float minX = (float) (ore.x() - camera.x) - BLOCK_OUTSET;
                            float minY = (float) (ore.y() - camera.y) - BLOCK_OUTSET;
                            float minZ = (float) (ore.z() - camera.z) - BLOCK_OUTSET;
                            float maxX = minX + 1.0F + BLOCK_OUTSET * 2.0F;
                            float maxY = minY + 1.0F + BLOCK_OUTSET * 2.0F;
                            float maxZ = minZ + 1.0F + BLOCK_OUTSET * 2.0F;

                            drawWireCube(matrix, vertices, minX, minY, minZ, maxX, maxY, maxZ,
                                    red, green, blue, EDGE_THICKNESS);
                        }
                    }

                    // A fine golden frame shows exactly which blocks the next Worldbreaker swing affects.
                    for (WorldbreakerMarker block : preview) {
                        float minX = (float) (block.x() - camera.x) - WORLDBREAKER_OUTSET;
                        float minY = (float) (block.y() - camera.y) - WORLDBREAKER_OUTSET;
                        float minZ = (float) (block.z() - camera.z) - WORLDBREAKER_OUTSET;
                        float maxX = minX + 1.0F + WORLDBREAKER_OUTSET * 2.0F;
                        float maxY = minY + 1.0F + WORLDBREAKER_OUTSET * 2.0F;
                        float maxZ = minZ + 1.0F + WORLDBREAKER_OUTSET * 2.0F;
                        drawWireCube(matrix, vertices, minX, minY, minZ, maxX, maxY, maxZ,
                                1.0F, 0.78F, 0.24F, WORLDBREAKER_EDGE_THICKNESS);
                    }
                }
        );
    }

    private static void drawWireCube(Matrix4fc matrix, VertexConsumer vertices,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ,
                                     float red, float green, float blue, float thickness) {
        float half = thickness;

        // Four X-axis edges.
        for (float y : new float[]{minY, maxY}) {
            for (float z : new float[]{minZ, maxZ}) {
                drawFilledBox(matrix, vertices,
                        minX, y - half, z - half, maxX, y + half, z + half, red, green, blue);
            }
        }

        // Four Y-axis edges.
        for (float x : new float[]{minX, maxX}) {
            for (float z : new float[]{minZ, maxZ}) {
                drawFilledBox(matrix, vertices,
                        x - half, minY, z - half, x + half, maxY, z + half, red, green, blue);
            }
        }

        // Four Z-axis edges.
        for (float x : new float[]{minX, maxX}) {
            for (float y : new float[]{minY, maxY}) {
                drawFilledBox(matrix, vertices,
                        x - half, y - half, minZ, x + half, y + half, maxZ, red, green, blue);
            }
        }
    }

    private static void drawFilledBox(Matrix4fc matrix, VertexConsumer vertices,
                                      float minX, float minY, float minZ,
                                      float maxX, float maxY, float maxZ,
                                      float red, float green, float blue) {
        // Front and back.
        addQuad(matrix, vertices,
                minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ,
                red, green, blue);
        addQuad(matrix, vertices,
                maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ,
                red, green, blue);

        // Left and right.
        addQuad(matrix, vertices,
                minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ,
                red, green, blue);
        addQuad(matrix, vertices,
                maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                red, green, blue);

        // Bottom and top.
        addQuad(matrix, vertices,
                minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ,
                red, green, blue);
        addQuad(matrix, vertices,
                minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                red, green, blue);
    }

    private static void addQuad(Matrix4fc matrix, VertexConsumer vertices,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float red, float green, float blue) {
        vertices.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, 1.0F);
        vertices.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, 1.0F);
        vertices.addVertex(matrix, x3, y3, z3).setColor(red, green, blue, 1.0F);
        vertices.addVertex(matrix, x4, y4, z4).setColor(red, green, blue, 1.0F);
    }
}
