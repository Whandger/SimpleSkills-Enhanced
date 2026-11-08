package com.github.ob_yekt.simpleskills.fishing;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * HUD of the fishing minigame, drawn under the crosshair.
 *
 * <p>The server owns the minigame and sends snapshots ({@link FishingStatePayload}); this class only
 * draws them: the line tension (green / yellow / red), the fish fatigue, the reeling progress, the
 * direction the fish is pulling to and the species. When snapshots stop arriving the HUD hides itself.</p>
 */
public final class FishingHud implements HudElement {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("simpleskills", "fishing_hud");

    private static final long NANOS_PER_MS = 1_000_000L;
    /** Fight snapshots arrive every 2 ticks; hide when nothing arrived for this long. */
    private static final long STALE_FIGHT_MS = 800L;
    private static final long STALE_BITE_MS = 1000L;
    private static final long RESULT_MS = 2500L;

    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 7;
    private static final int OFFSET_BELOW_CROSSHAIR = 22;

    private static final int BORDER = 0xFF000000;
    private static final int BACKGROUND = 0xFF2B2B2B;
    private static final int GREEN = 0xFF55E05A;
    private static final int YELLOW = 0xFFF2D544;
    private static final int RED = 0xFFE5483C;
    private static final int FATIGUE = 0xFF58B7F0;
    private static final int PROGRESS = 0xFFE9CF7A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFBBBBBB;

    // Written and read on the client thread only.
    private static int state = FishingStatePayload.STATE_STOP;
    private static float tension;
    private static float fatigue;
    private static float progress;
    private static int fishMove;
    private static String species = "";
    private static long receivedAtNanos;

    private FishingHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, HUD_ID, new FishingHud());
    }

    public static void onPayload(FishingStatePayload payload) {
        if (payload.state() == FishingStatePayload.STATE_STOP) {
            reset();
            return;
        }
        state = payload.state();
        tension = payload.tension();
        fatigue = payload.fatigue();
        progress = payload.progress();
        fishMove = payload.fishMove();
        species = payload.species();
        receivedAtNanos = System.nanoTime();
    }

    public static void reset() {
        state = FishingStatePayload.STATE_STOP;
        tension = 0f;
        fatigue = 0f;
        progress = 0f;
        fishMove = 0;
        species = "";
        receivedAtNanos = 0L;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker deltaTracker) {
        if (state == FishingStatePayload.STATE_STOP) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        long ageMs = (System.nanoTime() - receivedAtNanos) / NANOS_PER_MS;
        Font font = client.font;
        int cx = context.guiWidth() / 2;
        int y = context.guiHeight() / 2 + OFFSET_BELOW_CROSSHAIR;
        Component name = species.isEmpty() ? Component.empty() : Component.translatable(species);

        switch (state) {
            case FishingStatePayload.STATE_BITE -> {
                if (ageMs > STALE_BITE_MS) return;
                centered(context, font, Component.literal("!  ").append(name).append(Component.literal("  !")), cx, y, YELLOW);
                centered(context, font, Component.literal("Right-click to hook it"), cx, y + 11, TEXT_DIM);
            }
            case FishingStatePayload.STATE_FIGHT -> {
                if (ageMs > STALE_FIGHT_MS) return;
                drawFight(context, font, cx, y, name);
            }
            case FishingStatePayload.STATE_CAUGHT -> {
                if (ageMs > RESULT_MS) return;
                centered(context, font, Component.literal("Caught: ").append(name), cx, y, GREEN);
            }
            case FishingStatePayload.STATE_LINE_BROKEN -> {
                if (ageMs > RESULT_MS) return;
                centered(context, font, Component.literal("The line snapped!"), cx, y, RED);
            }
            case FishingStatePayload.STATE_LOST -> {
                if (ageMs > RESULT_MS) return;
                centered(context, font, Component.literal("It got away..."), cx, y, TEXT_DIM);
            }
            default -> { }
        }
    }

    private static void drawFight(GuiGraphicsExtractor context, Font font, int cx, int y, Component name) {
        // Species + what the fish is doing. "Look the other way" is the whole game.
        String arrow = switch (fishMove) {
            case -1 -> "<<<  ";
            case 1 -> "  >>>";
            default -> "";
        };
        Component head = fishMove == -1
                ? Component.literal(arrow).append(name)
                : fishMove == 1 ? Component.empty().append(name).append(Component.literal(arrow)) : name;
        centered(context, font, head, cx, y, fishMove == 0 ? TEXT : YELLOW);
        String hint = switch (fishMove) {
            case -1 -> "Look RIGHT";
            case 1 -> "Look LEFT";
            default -> "It rests - reel in";
        };
        centered(context, font, Component.literal(hint), cx, y + 10, fishMove == 0 ? GREEN : TEXT_DIM);

        int barX = cx - BAR_WIDTH / 2;
        int tensionY = y + 22;
        int tensionColor = tension < 0.55f ? GREEN : tension < 0.82f ? YELLOW : RED;
        bar(context, barX, tensionY, BAR_WIDTH, BAR_HEIGHT, tension, tensionColor);
        context.text(font, "Tension", barX - font.width("Tension") - 4, tensionY, tensionColor, false);

        bar(context, barX, tensionY + 10, BAR_WIDTH, 4, fatigue, FATIGUE);
        context.text(font, "Fatigue", barX - font.width("Fatigue") - 4, tensionY + 9, FATIGUE, false);

        bar(context, barX, tensionY + 18, BAR_WIDTH, 4, progress, PROGRESS);
        context.text(font, "Line", barX - font.width("Line") - 4, tensionY + 17, PROGRESS, false);
    }

    private static void bar(GuiGraphicsExtractor context, int x, int y, int width, int height, float value, int color) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, BORDER);
        context.fill(x, y, x + width, y + height, BACKGROUND);
        int filled = Math.round(Math.max(0f, Math.min(1f, value)) * width);
        if (filled > 0) {
            context.fill(x, y, x + filled, y + height, color);
        }
    }

    private static void centered(GuiGraphicsExtractor context, Font font, Component text, int cx, int y, int color) {
        context.text(font, text, cx - font.width(text) / 2, y, color, true);
    }
}
