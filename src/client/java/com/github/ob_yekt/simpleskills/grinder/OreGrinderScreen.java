package com.github.ob_yekt.simpleskills.grinder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact furnace-like GUI with a continuously rotating grinding-wheel indicator. */
public final class OreGrinderScreen extends AbstractContainerScreen<OreGrinderMenu> {
    private static final int PANEL = 0xFF24252C;
    private static final int PANEL_DARK = 0xFF15161C;
    private static final int PANEL_INNER = 0xFF343640;
    private static final int BORDER_LIGHT = 0xFF777B85;
    private static final int BORDER_DARK = 0xFF101116;
    private static final int AMBER = 0xFFE99A32;
    private static final int AMBER_BRIGHT = 0xFFFFD16A;
    private static final int MUTED = 0xFFB9BAC2;
    private static final int TEXT = 0xFFF0F0F2;

    private int clientTicks;

    public OreGrinderScreen(OreGrinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 192);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 103;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        clientTicks++;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL_DARK);
        drawBorder(graphics, x, y, imageWidth, imageHeight, BORDER_LIGHT, BORDER_DARK);

        graphics.centeredText(font, Component.translatable("gui.simpleskills.ore_grinder.title"),
                x + imageWidth / 2, y + 7, TEXT);
        graphics.fill(x + 8, y + 19, x + imageWidth - 8, y + 20, PANEL_INNER);

        // Input and fuel labels
        graphics.centeredText(font, Component.translatable("gui.simpleskills.ore_grinder.input"),
                x + 52, y + 23, MUTED);
        graphics.centeredText(font, Component.translatable("gui.simpleskills.ore_grinder.fuel"),
                x + 52, y + 59, MUTED);
        graphics.centeredText(font, Component.translatable("gui.simpleskills.ore_grinder.output"),
                x + 124, y + 41, MUTED);

        // Recessed slot surrounds
        drawSlotFrame(graphics, x + 43, y + 33);
        drawSlotFrame(graphics, x + 43, y + 69);
        drawSlotFrame(graphics, x + 115, y + 51);

        boolean running = menu.isBurning() && menu.getProgress() > 0;
        drawGrindingWheel(graphics, x + 88, y + 55, clientTicks + partialTick, running);

        // Animated progress bar, separate from the wheel so its state is readable at a glance.
        int barX = x + 74;
        int barY = y + 86;
        int barWidth = 54;
        graphics.fill(barX, barY, barX + barWidth, barY + 5, PANEL_DARK);
        drawBorder(graphics, barX, barY, barWidth, 5, PANEL_INNER, BORDER_DARK);
        int fill = Math.min(barWidth - 2, Math.max(0,
                (menu.getProgress() * (barWidth - 2)) / menu.getTotalProgress()));
        if (fill > 0) graphics.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 4, AMBER);

        // Fuel gauge shows the remaining fraction of the current fuel item.
        int fuelX = x + 67;
        int fuelY = y + 70;
        graphics.fill(fuelX, fuelY, fuelX + 4, fuelY + 16, PANEL_DARK);
        drawBorder(graphics, fuelX, fuelY, 4, 16, PANEL_INNER, BORDER_DARK);
        if (menu.getTotalBurnTime() > 0 && menu.getBurnTime() > 0) {
            int fuelFill = Math.max(1, Math.min(14,
                    (menu.getBurnTime() * 14) / menu.getTotalBurnTime()));
            graphics.fill(fuelX + 1, fuelY + 15 - fuelFill, fuelX + 3, fuelY + 15, AMBER);
        }

        Component status = menu.isBurning()
                ? Component.translatable("gui.simpleskills.ore_grinder.working")
                : (menu.getSlot(0).hasItem()
                    ? Component.translatable("gui.simpleskills.ore_grinder.waiting_fuel")
                    : Component.translatable("gui.simpleskills.ore_grinder.ready"));
        graphics.centeredText(font, status, x + imageWidth / 2, y + 95,
                menu.isBurning() ? AMBER_BRIGHT : MUTED);

        graphics.centeredText(font, Component.translatable("container.inventory"),
                x + 8 + 78, y + 105, TEXT);
    }

    private static void drawGrindingWheel(GuiGraphicsExtractor graphics, int cx, int cy,
                                          float time, boolean running) {
        int inactive = 0xFF555965;
        graphics.fill(cx - 12, cy - 12, cx + 12, cy + 12, PANEL_DARK);
        drawRing(graphics, cx, cy, 11, inactive);
        double angle = running ? time * 0.17D : 0.0D;
        for (int i = 0; i < 8; i++) {
            double a = angle + i * (Math.PI / 4.0D);
            int px = cx + (int) Math.round(Math.cos(a) * 7.0D);
            int py = cy + (int) Math.round(Math.sin(a) * 7.0D);
            int color = running ? (i % 2 == 0 ? AMBER_BRIGHT : AMBER) : inactive;
            graphics.fill(px - 2, py - 1, px + 2, py + 2, color);
        }
        graphics.fill(cx - 3, cy - 3, cx + 4, cy + 4, PANEL_INNER);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, running ? AMBER_BRIGHT : AMBER);
        if (running) {
            int spark = (int) ((time / 2.0F) % 4.0F);
            graphics.fill(cx - 13 + spark, cy - 1, cx - 11 + spark, cy + 1, AMBER_BRIGHT);
            graphics.fill(cx + 11 - spark, cy + 2, cx + 13 - spark, cy + 4, AMBER);
        }
    }

    private static void drawRing(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int x = -radius; x <= radius; x++) {
            int y = (int) Math.round(Math.sqrt(radius * radius - x * x));
            graphics.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
            graphics.fill(cx + x, cy - y, cx + x + 1, cy - y + 1, color);
        }
    }

    private static void drawSlotFrame(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, PANEL_DARK);
        drawBorder(graphics, x, y, 18, 18, BORDER_DARK, BORDER_LIGHT);
    }

    private static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                   int topLeft, int bottomRight) {
        graphics.fill(x, y, x + w, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + h, topLeft);
        graphics.fill(x, y + h - 1, x + w, y + h, bottomRight);
        graphics.fill(x + w - 1, y, x + w, y + h, bottomRight);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Labels are drawn in extractBackground to keep the custom layout aligned.
    }
}
