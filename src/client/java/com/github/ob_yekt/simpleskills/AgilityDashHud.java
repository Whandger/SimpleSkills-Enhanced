package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.agility.AgilityDashCooldownPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Small Dash icon and cooldown bar shown beside the hotbar. */
public final class AgilityDashHud implements HudElement {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("simpleskills", "agility_dash");
    private static final int READY_COLOR = 0xFF58D6FF;
    private static final int COOLDOWN_COLOR = 0xFF77808A;
    private static final int TILE_COLOR = 0xD9161C24;
    private static final int BORDER_COLOR = 0xFF05070A;
    private static final int BAR_BACKGROUND = 0xFF303841;

    private static int cooldownTicks;
    private static long cooldownStartedAt;

    private AgilityDashHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, HUD_ID, new AgilityDashHud());
    }

    public static void onCooldown(AgilityDashCooldownPayload payload) {
        Minecraft client = Minecraft.getInstance();
        cooldownTicks = Math.max(0, Math.min(72_000, payload.cooldownTicks()));
        cooldownStartedAt = client.level == null ? 0L : client.level.getGameTime();
    }

    public static void reset() {
        cooldownTicks = 0;
        cooldownStartedAt = 0L;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null
                || AgilityClientState.getLevel() < AgilityClientState.getDashUnlockLevel()) return;

        int remaining = Math.max(0, cooldownTicks - (int) (client.level.getGameTime() - cooldownStartedAt));
        int width = context.guiWidth();
        int height = context.guiHeight();
        int x = width / 2 + 97;
        int y = height - 27;

        context.text(client.font, Component.translatable("key.simpleskills.agility_dash"),
                x + 10 - client.font.width(Component.translatable("key.simpleskills.agility_dash")) / 2,
                y - 9, 0xFFE5EDF4, true);

        context.fill(x - 1, y - 1, x + 21, y + 21, BORDER_COLOR);
        context.fill(x, y, x + 20, y + 20, TILE_COLOR);
        int iconColor = remaining == 0 ? READY_COLOR : COOLDOWN_COLOR;
        context.text(client.font, Component.literal("»"), x + 5, y + 2, iconColor, true);

        if (remaining > 0 && cooldownTicks > 0) {
            int seconds = (remaining + 19) / 20;
            String countdown = Integer.toString(seconds);
            context.text(client.font, countdown, x + 10 - client.font.width(countdown) / 2,
                    y + 12, 0xFFFFFFFF, true);

            int barX = x + 2;
            int barY = y + 22;
            int barWidth = 16;
            context.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 3, BORDER_COLOR);
            context.fill(barX, barY, barX + barWidth, barY + 2, BAR_BACKGROUND);
            double progress = 1.0 - remaining / (double) cooldownTicks;
            int filled = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * barWidth);
            if (filled > 0) context.fill(barX, barY, barX + filled, barY + 2, READY_COLOR);
        } else {
            context.fill(x + 2, y + 22, x + 18, y + 24, READY_COLOR);
        }
    }
}
