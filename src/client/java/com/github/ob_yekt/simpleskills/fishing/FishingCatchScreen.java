
package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RPG style result screen of a confirmed catch: species name, weight, length and the real fish model
 * (including the tropical fish pattern) that can be rotated by dragging the mouse.
 *
 * <p>The screen is purely informational. It only draws the values received from the server, it never
 * generates or changes anything and closing it (X, ESC) has no effect on the rewards.</p>
 */
public final class FishingCatchScreen extends Screen {
    private static final int PANEL_W = 260;
    private static final int PANEL_H = 230;

    private static final int GOLD = 0xFFE9CF7A;
    private static final int GOLD_DARK = 0xFF8A6D2B;
    private static final int PANEL_BG = 0xE61C1410;
    private static final int PANEL_INNER = 0xFF2A1F17;
    private static final int STAT_BG = 0xCC140E0A;
    private static final int TEXT = 0xFFF2E8D0;
    private static final int TEXT_DIM = 0xFFB9A98A;

    /**
     * Negative IDs reserved for client-side preview entities.
     * These entities are never added to the world.
     */
    private static final AtomicInteger NEXT_PREVIEW_ENTITY_ID = new AtomicInteger(-1);

    private final FishingCatchPayload result;
    private @Nullable Mob fish;
    private @Nullable EntityRenderState cachedState;

    /** Horizontal rotation of the model in degrees (dragging with the mouse changes it). */
    private float yaw = -65.0F;
    private boolean dragging;
    private long lastDragNanos;

    private int left;
    private int top;

    public FishingCatchScreen(FishingCatchPayload result) {
        super(Component.translatable(result.nameKey()));
        this.result = result;
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;

        // Close button (X), top-right corner of the panel.
        this.addRenderableWidget(Button.builder(Component.literal("X"), button -> this.onClose())
                .bounds(this.left + PANEL_W - 24, this.top + 4, 20, 20)
                .build());

        this.fish = createFish();
    }

    /**
     * Builds a client-side preview entity without adding it to the world.
     */
    private @Nullable Mob createFish() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        Identifier id = Identifier.tryParse(result.entityType());
        if (id == null) return null;

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        Entity entity = type.create(level, EntitySpawnReason.LOAD);

        if (!(entity instanceof Mob mob)) return null;

        // Preview entities are never added to the world.
        // Assign a unique non-zero ID because the renderer requires one
        // when extracting the entity render state.
        mob.setId(NEXT_PREVIEW_ENTITY_ID.getAndDecrement());

        FishSpecies species = FishSpecies.of(mob);
        if (species != null && species.hasVariant()) {
            species.applyVariant(mob, result.variant());
        }

        return mob;
    }

    // ------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        // Panel with a double gold frame.
        graphics.fill(this.left - 3, this.top - 3, this.left + PANEL_W + 3, this.top + PANEL_H + 3, GOLD_DARK);
        graphics.fill(this.left - 2, this.top - 2, this.left + PANEL_W + 2, this.top + PANEL_H + 2, GOLD);
        graphics.fill(this.left, this.top, this.left + PANEL_W, this.top + PANEL_H, PANEL_BG);
        graphics.fill(this.left + 6, this.top + 6, this.left + PANEL_W - 6, this.top + PANEL_H - 6, PANEL_INNER);

        // Title: "Catch!" and the species name.
        centered(graphics, Component.literal("\u2726 CATCH \u2726"), this.top + 10, TEXT_DIM);
        centered(graphics, Component.translatable(result.nameKey()).withStyle(style -> style.withBold(true)), this.top + 24, GOLD);

        // Model area.
        int mx0 = this.left + 16;
        int my0 = this.top + 42;
        int mx1 = this.left + PANEL_W - 16;
        int my1 = this.top + 150;
        graphics.fill(mx0 - 1, my0 - 1, mx1 + 1, my1 + 1, GOLD_DARK);
        graphics.fill(mx0, my0, mx1, my1, 0xFF3A5560);
        graphics.fill(mx0, my0, mx1, my0 + (my1 - my0) / 2, 0xFF456A78);
        drawFish(graphics, mx0, my0, mx1, my1);

        // Stats plates.
        String weight = String.format(Locale.US, "%.2f kg", result.weightKg());
        String length = String.format(Locale.US, "%.1f cm", result.lengthCm());
        statPlate(graphics, this.left + 16, this.top + 160, (PANEL_W - 40) / 2, "Weight", weight);
        statPlate(graphics, this.left + 24 + (PANEL_W - 40) / 2, this.top + 160, (PANEL_W - 40) / 2, "Length", length);

        centered(
        graphics,
        Component.literal("+" + result.xpGained() + " Fishing XP")
                .withStyle(style -> style.withBold(true)),
        this.top + PANEL_H - 22,
        GOLD
);
    }

    private void drawFish(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        Mob mob = this.fish;
        if (mob == null) {
            graphics.centeredText(this.font, Component.literal("?"), (x0 + x1) / 2, (y0 + y1) / 2 - 4, TEXT_DIM);
            return;
        }

        // Slow idle rotation until the player grabs the fish.
        long now = System.nanoTime();
        if (!dragging && (now - lastDragNanos) > 1_500_000_000L) {
            yaw += 0.35F;
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        @SuppressWarnings("unchecked")
        EntityRenderer<? super Mob, ?> renderer =
                (EntityRenderer<? super Mob, ?>) dispatcher.getRenderer(mob);

        EntityRenderState state = renderer.createRenderState(mob, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.ageInTicks = (float) (now / 50_000_000L);

        if (state instanceof LivingEntityRenderState living) {
            living.isInWater = true;
            living.bodyRot = 180.0F + yaw;
            living.yRot = yaw;
            living.xRot = 0.0F;
            living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
            living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
            living.scale = 1.0F;
        }

        // Fit the model into the area: the longest side of the entity decides the scale.
        float longest = Math.max(0.3F, Math.max(state.boundingBoxWidth, state.boundingBoxHeight));
        float size = (Math.min(x1 - x0, y1 - y0) * 0.62F) / longest;

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);

        graphics.entity(state, size, translation, rotation, null, x0, y0, x1, y1);
        this.cachedState = state;
    }

    private void statPlate(GuiGraphicsExtractor graphics, int x, int y, int w, String label, String value) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + 31, GOLD_DARK);
        graphics.fill(x, y, x + w, y + 30, STAT_BG);
        graphics.centeredText(this.font, Component.literal(label), x + w / 2, y + 4, TEXT_DIM);
        graphics.centeredText(this.font, Component.literal(value).withStyle(style -> style.withBold(true)), x + w / 2, y + 16, TEXT);
    }

    private void centered(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(this.font, text, this.width / 2 - this.font.width(text) / 2, y, color, true);
    }

    // ------------------------------------------------------------------------------------------
    // Input: dragging rotates the fish; ESC / X close (Screen handles ESC)
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;

        if (event.button() == 0 && insideModelArea(event.x(), event.y())) {
            dragging = true;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging && event.button() == 0) {
            yaw += (float) dx * 1.1F;
            lastDragNanos = System.nanoTime();
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && dragging) {
            dragging = false;
            lastDragNanos = System.nanoTime();
            return true;
        }

        return super.mouseReleased(event);
    }

    private boolean insideModelArea(double mouseX, double mouseY) {
        return mouseX >= left + 16
                && mouseX <= left + PANEL_W - 16
                && mouseY >= top + 42
                && mouseY <= top + 150;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
