package com.github.ob_yekt.simpleskills.prayer;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Compact Prayer cooldown tile beside the Dash cooldown indicator. */
public final class PrayerCooldownHud implements HudElement {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("simpleskills", "prayer_cooldown");
    private static String ability = "";
    private static int duration;
    private static long started;

    private PrayerCooldownHud() {}
    public static void register() { HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ID, new PrayerCooldownHud()); }
    public static void accept(PrayerCooldownPayload payload) {
        ability = payload.abilityId();
        duration = Math.max(1, Math.min(72_000, payload.cooldownTicks()));
        Minecraft client = Minecraft.getInstance();
        started = client.level == null ? 0 : client.level.getGameTime();
    }
    public static void reset() { ability = ""; duration = 0; started = 0; }

    @Override public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || ability.isEmpty()) return;
        int remaining = Math.max(0, duration - (int) (mc.level.getGameTime() - started));
        int x = g.guiWidth() / 2 + 143, y = g.guiHeight() - 27;
        Component title = Component.literal("Prayer: " + ability.replace('_', ' '));
        g.text(mc.font, title, x - mc.font.width(title) / 2 + 10, y - 9, 0xFFF3E8D2, true);
        g.fill(x - 1, y - 1, x + 21, y + 21, 0xFF05070A);
        g.fill(x, y, x + 20, y + 20, 0xD9161C24);
        g.text(mc.font, Component.literal("✦"), x + 4, y + 3, remaining == 0 ? 0xFFFFD978 : 0xFF77808A, true);
        if (remaining > 0) {
            String seconds = Integer.toString((remaining + 19) / 20);
            g.text(mc.font, seconds, x + 10 - mc.font.width(seconds) / 2, y + 12, 0xFFFFFFFF, true);
        }
        int filled = remaining == 0 ? 16 : (int) ((1.0 - remaining / (double) duration) * 16);
        g.fill(x + 2, y + 22, x + 18, y + 24, 0xFF303841);
        if (filled > 0) g.fill(x + 2, y + 22, x + 2 + filled, y + 24, 0xFFFFD978);
    }
}
