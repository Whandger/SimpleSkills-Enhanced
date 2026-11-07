package com.github.ob_yekt.simpleskills.excavating;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Draws the Excavating investigation progress as a bar that fills up below the crosshair.
 *
 * The server owns the investigation and sends its progress every couple of ticks
 * ({@link InvestigationPayload}); between two packets the bar is interpolated with the system
 * clock so it fills smoothly. The bar is hidden when the server says the investigation ended,
 * when the use key is released, or when the server stops sending updates.
 */
public final class InvestigationHud implements HudElement {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("simpleskills", "investigation_bar");

    private static final long NANOS_PER_TICK = 50_000_000L;
    /** If no update arrives for this long the bar is hidden (server stopped / lag / disconnect). */
    private static final long STALE_NANOS = 600_000_000L;

    private static final int BAR_WIDTH = 41;
    private static final int BAR_HEIGHT = 5;
    /** Distance from the crosshair centre to the top of the bar. */
    private static final int OFFSET_BELOW_CROSSHAIR = 16;

    private static final int BORDER_COLOR = 0xFF000000;
    private static final int BACKGROUND_COLOR = 0xFF3B3B3B;
    private static final int FILL_COLOR = 0xFFE9CF7A;      // sand
    private static final int FILL_HIGHLIGHT_COLOR = 0xFFF8E7A5;

    // Written on the client thread only.
    private static boolean active = false;
    private static int elapsedAtReceive = 0;
    private static int durationTicks = 0;
    private static long receivedAtNanos = 0L;

    private InvestigationHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, HUD_ID, new InvestigationHud());
    }

    /** Called on the client thread when the server sends a progress update. */
    public static void onPayload(InvestigationPayload payload) {
        if (payload.isStop()) {
            reset();
            return;
        }

        active = true;
        elapsedAtReceive = payload.elapsedTicks();
        durationTicks = payload.durationTicks();
        receivedAtNanos = System.nanoTime();
    }

    public static void reset() {
        active = false;
        elapsedAtReceive = 0;
        durationTicks = 0;
        receivedAtNanos = 0L;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker deltaTracker) {
        if (!active || durationTicks <= 0) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        long sinceReceive = System.nanoTime() - receivedAtNanos;

        // Server stopped talking, or the use key was released (the server cancels shortly after).
        if (sinceReceive > STALE_NANOS || !client.options.keyUse.isDown()) {
            return;
        }

        double elapsedNanos = elapsedAtReceive * (double) NANOS_PER_TICK + sinceReceive;
        double progress = Math.max(0.0, Math.min(1.0, elapsedNanos / (durationTicks * (double) NANOS_PER_TICK)));

        int x = context.guiWidth() / 2 - BAR_WIDTH / 2;
        int y = context.guiHeight() / 2 + OFFSET_BELOW_CROSSHAIR;

        // Border + background.
        context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER_COLOR);
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BACKGROUND_COLOR);

        // Fill.
        int filled = (int) Math.round(progress * BAR_WIDTH);
        if (filled > 0) {
            context.fill(x, y, x + filled, y + BAR_HEIGHT, FILL_COLOR);
            context.fill(x, y, x + filled, y + 1, FILL_HIGHLIGHT_COLOR);
        }
    }
}
