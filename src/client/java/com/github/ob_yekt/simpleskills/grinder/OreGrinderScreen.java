package com.github.ob_yekt.simpleskills.grinder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Industrial dark-steel Ore Grinder interface with a live central dismantling chamber,
 * recessed machine controls, fuel gauge, and full 36-slot player inventory.
 */
public final class OreGrinderScreen extends AbstractContainerScreen<OreGrinderMenu> {
    // UI Palette
    private static final int PANEL = 0xFF24252C;
    private static final int PANEL_DARK = 0xFF15161C;
    private static final int PANEL_INNER = 0xFF343640;
    private static final int BORDER_LIGHT = 0xFF777B85;
    private static final int BORDER_DARK = 0xFF101116;
    private static final int AMBER = 0xFFE99A32;
    private static final int AMBER_BRIGHT = 0xFFFFD16A;
    private static final int MUTED = 0xFFB9BAC2;
    private static final int TEXT = 0xFFF0F0F2;

    // Machine Material Palette
    private static final int DARK_IRON = 0xFF1F2028;
    private static final int CASING = 0xFF23252E;
    private static final int STEEL = 0xFF434652;
    private static final int METALLIC_GRAY = 0xFF4C4E5A;
    private static final int METALLIC_HIGHLIGHT = 0xFF686B77;
    private static final int LIGHT_STEEL = 0xFF848792;
    private static final int SILVER_HIGHLIGHT = 0xFF9B9EA8;
    private static final int DARK_COPPER = 0xFF69350F;
    private static final int ORANGE = 0xFFE88E1C;
    private static final int ACTIVE_ORANGE = 0xFF7C370A;
    private static final int ACTIVE_GLOW = 0xFFFFD95D;

    // Layout geometry (204 x 222)
    private static final int CHAMBER_X = 58;
    private static final int CHAMBER_Y = 26;
    private static final int CHAMBER_W = 88;
    private static final int CHAMBER_H = 81;

    private int clientTicks;

    public OreGrinderScreen(OreGrinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 204, 222);
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelX = 22;
        this.inventoryLabelY = 115;
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
        float time = clientTicks + partialTick;

        ItemStack inputStack = menu.getSlot(OreGrinderBlockEntity.INPUT_SLOT).getItem();
        ItemStack fuelStack = menu.getSlot(OreGrinderBlockEntity.FUEL_SLOT).getItem();
        ItemStack outputStack = menu.getSlot(OreGrinderBlockEntity.OUTPUT_SLOT).getItem();

        boolean burning = menu.isBurning();
        boolean running = burning && !inputStack.isEmpty() && menu.getProgress() > 0;
        float progressRatio = running
                ? Mth.clamp((float) menu.getProgress() / (float) menu.getTotalProgress(), 0.0F, 1.0F)
                : 0.0F;

        drawMachineChassis(graphics, x, y, running);
        drawHeaderBanner(graphics, x, y, running);

        // Section A: Central Processing Chamber
        drawProcessingChamber(graphics, x + CHAMBER_X, y + CHAMBER_Y, CHAMBER_W, CHAMBER_H,
                inputStack, outputStack, running, progressRatio, time);

        // Section B: Machine Controls & Recessed Slots
        drawControlColumns(graphics, x, y, inputStack, fuelStack, outputStack, burning, running, time);

        // Section C: Full 36-Slot Player Inventory
        drawPlayerInventorySection(graphics, x, y);
    }

    private void drawMachineChassis(GuiGraphicsExtractor graphics, int x, int y, boolean running) {
        // Outer drop shadow & heavy dark-iron casing
        graphics.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, BORDER_DARK);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, CASING);
        drawBevelBorder(graphics, x, y, imageWidth, imageHeight, BORDER_LIGHT, BORDER_DARK);

        // Inner steel plate
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, PANEL);
        drawBevelBorder(graphics, x + 3, y + 3, imageWidth - 6, imageHeight - 6, METALLIC_HIGHLIGHT, PANEL_DARK);

        // Corner industrial rivets
        int rivetColor = running ? AMBER : LIGHT_STEEL;
        drawRivet(graphics, x + 5, y + 5, rivetColor);
        drawRivet(graphics, x + imageWidth - 8, y + 5, rivetColor);
        drawRivet(graphics, x + 5, y + imageHeight - 8, rivetColor);
        drawRivet(graphics, x + imageWidth - 8, y + imageHeight - 8, rivetColor);
    }

    private void drawHeaderBanner(GuiGraphicsExtractor graphics, int x, int y, boolean running) {
        int bx = x + 10;
        int by = y + 5;
        int bw = imageWidth - 20;
        graphics.fill(bx, by, bx + bw, by + 15, DARK_IRON);
        drawBevelBorder(graphics, bx, by, bw, 15, PANEL_INNER, BORDER_DARK);

        // Side indicator LEDs
        int ledColor = running ? ACTIVE_GLOW : METALLIC_GRAY;
        int ledRim = running ? DARK_COPPER : BORDER_DARK;
        graphics.fill(bx + 5, by + 5, bx + 9, by + 9, ledRim);
        graphics.fill(bx + 6, by + 6, bx + 8, by + 8, ledColor);
        graphics.fill(bx + bw - 9, by + 5, bx + bw - 5, by + 9, ledRim);
        graphics.fill(bx + bw - 8, by + 6, bx + bw - 6, by + 8, ledColor);

        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.ore_grinder.title"),
                x + imageWidth / 2, by + 4, bw - 26, TEXT);
    }

    private void drawControlColumns(GuiGraphicsExtractor graphics, int x, int y,
                                    ItemStack inputStack, ItemStack fuelStack, ItemStack outputStack,
                                    boolean burning, boolean running, float time) {
        // Left housing (Input & Fuel)
        int leftBoxX = x + 7;
        int leftBoxY = y + 26;
        int leftBoxW = 47;
        int leftBoxH = 81;
        graphics.fill(leftBoxX, leftBoxY, leftBoxX + leftBoxW, leftBoxY + leftBoxH, DARK_IRON);
        drawBevelBorder(graphics, leftBoxX, leftBoxY, leftBoxW, leftBoxH, PANEL_INNER, BORDER_DARK);

        // Equipment Input Slot (menu slot 0 at x+23, y+42 -> frame at x+22, y+41)
        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.ore_grinder.input"),
                leftBoxX + leftBoxW / 2, y + 30, leftBoxW - 4, MUTED);
        drawMachineSlotFrame(graphics, x + 22, y + 41, !inputStack.isEmpty(), running);

        // Separator bar between input and fuel
        graphics.fill(leftBoxX + 5, y + 63, leftBoxX + leftBoxW - 5, y + 64, PANEL_INNER);

        // Fuel Input Slot (menu slot 1 at x+23, y+82 -> frame at x+22, y+81)
        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.ore_grinder.fuel"),
                leftBoxX + leftBoxW / 2, y + 68, leftBoxW - 4, MUTED);
        drawMachineSlotFrame(graphics, x + 22, y + 81, !fuelStack.isEmpty(), burning);

        // Segmented vertical fuel gauge right beside fuel slot
        int gaugeX = x + 43;
        int gaugeY = y + 81;
        int gaugeW = 6;
        int gaugeH = 18;
        graphics.fill(gaugeX, gaugeY, gaugeX + gaugeW, gaugeY + gaugeH, PANEL_DARK);
        drawBevelBorder(graphics, gaugeX, gaugeY, gaugeW, gaugeH, BORDER_DARK, METALLIC_HIGHLIGHT);
        if (menu.getTotalBurnTime() > 0 && menu.getBurnTime() > 0) {
            int fillH = Math.max(2, Math.min(gaugeH - 2,
                    (menu.getBurnTime() * (gaugeH - 2)) / menu.getTotalBurnTime()));
            int fillTop = gaugeY + gaugeH - 1 - fillH;
            graphics.fill(gaugeX + 1, fillTop, gaugeX + gaugeW - 1, gaugeY + gaugeH - 1, AMBER);
            graphics.fill(gaugeX + 1, fillTop, gaugeX + gaugeW - 1, Math.min(fillTop + 2, gaugeY + gaugeH - 1), AMBER_BRIGHT);
        }

        // Feed conduits into central processing chamber
        drawHorizontalConduit(graphics, x + 41, y + 49, x + CHAMBER_X, !inputStack.isEmpty(), running, time);
        drawHorizontalConduit(graphics, x + 49, y + 89, x + CHAMBER_X, burning, burning, time);

        // Right housing (Recovered Material Output)
        int rightBoxX = x + 150;
        int rightBoxY = y + 26;
        int rightBoxW = 47;
        int rightBoxH = 81;
        graphics.fill(rightBoxX, rightBoxY, rightBoxX + rightBoxW, rightBoxY + rightBoxH, DARK_IRON);
        drawBevelBorder(graphics, rightBoxX, rightBoxY, rightBoxW, rightBoxH, PANEL_INNER, BORDER_DARK);

        // Output chute conduit from chamber to output slot
        drawHorizontalConduit(graphics, x + CHAMBER_X + CHAMBER_W, y + 69, x + 164,
                !outputStack.isEmpty() || running, running, time);

        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.ore_grinder.output"),
                rightBoxX + rightBoxW / 2, y + 47, rightBoxW - 4, MUTED);
        drawMachineSlotFrame(graphics, x + 164, y + 61, !outputStack.isEmpty(), !outputStack.isEmpty());

        // Output count / salvage indicator badge
        if (!outputStack.isEmpty()) {
            graphics.centeredText(font, Component.literal("+" + outputStack.getCount()),
                    rightBoxX + rightBoxW / 2, y + 84, AMBER_BRIGHT);
        } else if (!inputStack.isEmpty()) {
            ItemStack preview = OreGrinderSalvage.getResult(inputStack);
            if (!preview.isEmpty()) {
                graphics.centeredText(font, Component.literal("≤" + preview.getCount()),
                        rightBoxX + rightBoxW / 2, y + 84, LIGHT_STEEL);
            }
        }
    }

    private void drawProcessingChamber(GuiGraphicsExtractor graphics, int cx, int cy, int cw, int ch,
                                       ItemStack inputStack, ItemStack outputStack,
                                       boolean running, float progressRatio, float time) {
        // Recessed heavy steel housing
        graphics.fill(cx, cy, cx + cw, cy + ch, PANEL_DARK);
        drawBevelBorder(graphics, cx, cy, cw, ch, BORDER_DARK, BORDER_LIGHT);
        graphics.fill(cx + 2, cy + 2, cx + cw - 2, cy + ch - 2, DARK_IRON);
        drawBevelBorder(graphics, cx + 2, cy + 2, cw - 4, ch - 4, BORDER_DARK, STEEL);

        // Subtle industrial grid lines inside chamber backdrop
        for (int gx = cx + 10; gx < cx + cw - 8; gx += 12) {
            graphics.fill(gx, cy + 4, gx + 1, cy + ch - 22, 0xFF191A22);
        }
        for (int gy = cy + 10; gy < cy + ch - 22; gy += 12) {
            graphics.fill(cx + 4, gy, cx + cw - 4, gy + 1, 0xFF191A22);
        }

        // Side guide rails
        graphics.fill(cx + 4, cy + 16, cx + 8, cy + 46, STEEL);
        graphics.fill(cx + cw - 8, cy + 16, cx + cw - 4, cy + 46, STEEL);
        graphics.fill(cx + 5, cy + 17, cx + 7, cy + 45, METALLIC_HIGHLIGHT);
        graphics.fill(cx + cw - 7, cy + 17, cx + cw - 5, cy + 45, METALLIC_HIGHLIGHT);

        int centerX = cx + cw / 2;
        int wheelY = cy + 42;

        // Rotating toothed grinding wheel
        drawMechanicalGrindingWheel(graphics, centerX, wheelY, time, running);

        // Central equipment / output visualization
        if (!inputStack.isEmpty()) {
            float itemX = centerX;
            float itemY = cy + 24.0F;
            float scale = 1.55F;
            float angleRad = 0.0F;

            if (running) {
                // Smoothly rotate, gradually lower toward the wheel, and shrink as grinding progresses
                angleRad = progressRatio * (float) (Math.PI * 3.0D) + Mth.sin(time * 0.45F) * 0.08F;
                float vibrationX = Mth.cos(time * 1.9F) * 0.65F;
                float vibrationY = Mth.sin(time * 2.3F) * 0.65F;
                itemX += vibrationX;
                itemY += progressRatio * 12.0F + vibrationY;
                scale = 1.65F * (1.0F - 0.34F * progressRatio);

                // Friction glow where equipment meets the wheel
                int glowW = 10 + (int) (Mth.sin(time * 0.9F) * 3.0F);
                graphics.fill(centerX - glowW, wheelY - 12, centerX + glowW, wheelY - 9, ACTIVE_ORANGE);
                graphics.fill(centerX - glowW / 2, wheelY - 11, centerX + glowW / 2, wheelY - 10, ACTIVE_GLOW);
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate(itemX, itemY);
            if (angleRad != 0.0F) {
                graphics.pose().rotate(angleRad);
            }
            graphics.pose().scale(scale, scale);
            graphics.item(inputStack, -8, -8);
            graphics.pose().popMatrix();

            if (running) {
                drawGrindingSparksAndFragments(graphics, centerX, wheelY - 10, time);
            }
        } else if (!outputStack.isEmpty()) {
            // Transition state: display recovered mineral in the lower recovery cradle
            graphics.fill(centerX - 13, cy + 16, centerX + 13, cy + 40, PANEL_INNER);
            drawBevelBorder(graphics, centerX - 13, cy + 16, 26, 24, AMBER, BORDER_DARK);

            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX, cy + 28.0F);
            graphics.pose().scale(1.25F, 1.25F);
            graphics.item(outputStack, -8, -8);
            graphics.pose().popMatrix();
        }

        // Progress bar at bottom of chamber
        int barX = cx + 6;
        int barY = cy + ch - 20;
        int barW = cw - 12;
        int barH = 6;
        graphics.fill(barX, barY, barX + barW, barY + barH, PANEL_DARK);
        drawBevelBorder(graphics, barX, barY, barW, barH, BORDER_DARK, METALLIC_HIGHLIGHT);
        int fillW = Math.min(barW - 2, Math.max(0,
                (menu.getProgress() * (barW - 2)) / menu.getTotalProgress()));
        if (fillW > 0) {
            graphics.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, AMBER);
            graphics.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + 2, AMBER_BRIGHT);
        }

        // Status text inside lower chamber plaque
        Component status;
        int statusColor;
        if (running) {
            int pct = Math.round(progressRatio * 100.0F);
            status = Component.translatable("gui.simpleskills.ore_grinder.working")
                    .append(Component.literal(" " + pct + "%"));
            statusColor = AMBER_BRIGHT;
        } else if (!inputStack.isEmpty()) {
            status = Component.translatable("gui.simpleskills.ore_grinder.waiting_fuel");
            statusColor = ORANGE;
        } else if (!outputStack.isEmpty()) {
            status = Component.translatable("gui.simpleskills.ore_grinder.complete");
            statusColor = AMBER;
        } else {
            status = Component.translatable("gui.simpleskills.ore_grinder.ready");
            statusColor = MUTED;
        }
        drawFittedCenteredText(graphics, status, centerX, cy + ch - 11, cw - 8, statusColor);
    }

    private static void drawMechanicalGrindingWheel(GuiGraphicsExtractor graphics, int cx, int cy,
                                                    float time, boolean running) {
        int radius = 14;
        // Wheel housing shadow
        fillCircle(graphics, cx, cy, radius + 2, BORDER_DARK);
        fillCircle(graphics, cx, cy, radius, STEEL);
        fillCircle(graphics, cx, cy, radius - 2, METALLIC_GRAY);

        // Rotating abrasive teeth around rim
        double baseAngle = running ? time * 0.24D : 0.0D;
        int teeth = 12;
        for (int i = 0; i < teeth; i++) {
            double a = baseAngle + i * (Math.PI * 2.0D / teeth);
            int tx = cx + (int) Math.round(Math.cos(a) * (radius - 1));
            int ty = cy + (int) Math.round(Math.sin(a) * (radius - 1));
            int toothColor = running
                    ? (i % 3 == 0 ? AMBER_BRIGHT : SILVER_HIGHLIGHT)
                    : LIGHT_STEEL;
            graphics.fill(tx - 1, ty - 1, tx + 2, ty + 2, toothColor);
        }

        // Inner spokes
        for (int i = 0; i < 4; i++) {
            double a = baseAngle + i * (Math.PI / 2.0D);
            for (int r = 4; r <= radius - 4; r += 2) {
                int sx = cx + (int) Math.round(Math.cos(a) * r);
                int sy = cy + (int) Math.round(Math.sin(a) * r);
                graphics.fill(sx, sy, sx + 1, sy + 1, METALLIC_HIGHLIGHT);
            }
        }

        // Central axle hub
        graphics.fill(cx - 4, cy - 4, cx + 5, cy + 5, DARK_IRON);
        graphics.fill(cx - 3, cy - 3, cx + 4, cy + 4, METALLIC_HIGHLIGHT);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, running ? ACTIVE_GLOW : AMBER);
    }

    private static void drawGrindingSparksAndFragments(GuiGraphicsExtractor graphics, int originX, int originY, float time) {
        // Amber sparks ejecting left and right from the grinding contact point
        for (int i = 0; i < 8; i++) {
            float phase = ((time * 0.16F) + i * 0.125F) % 1.0F;
            float side = (i % 2 == 0) ? -1.0F : 1.0F;
            float spread = 0.65F + (i % 4) * 0.22F;
            int sx = originX + (int) (side * phase * 24.0F * spread);
            int sy = originY - (int) (Math.sin(phase * Math.PI) * 9.0F) + (int) (phase * phase * 12.0F);
            int sparkColor = (phase < 0.45F) ? ACTIVE_GLOW : (phase < 0.78F ? AMBER_BRIGHT : AMBER);
            graphics.fill(sx, sy, sx + (phase < 0.5F ? 2 : 1), sy + 1, sparkColor);
        }

        // Small metal fragments flaking downward from the dismantled equipment
        for (int i = 0; i < 5; i++) {
            float phase = ((time * 0.11F) + i * 0.2F) % 1.0F;
            int fx = originX - 10 + i * 5 + (int) (Math.sin(time * 0.3F + i) * 2.0F);
            int fy = originY + 2 + (int) (phase * 16.0F);
            int fragColor = (i % 2 == 0) ? SILVER_HIGHLIGHT : LIGHT_STEEL;
            graphics.fill(fx, fy, fx + 2, fy + 2, fragColor);
        }
    }

    private void drawPlayerInventorySection(GuiGraphicsExtractor graphics, int x, int y) {
        // Horizontal chassis divider
        graphics.fill(x + 7, y + 111, x + imageWidth - 7, y + 112, BORDER_DARK);
        graphics.fill(x + 7, y + 112, x + imageWidth - 7, y + 113, METALLIC_HIGHLIGHT);

        // Inventory label cleanly positioned above the 9x3 grid
        graphics.text(font, Component.translatable("container.inventory"),
                x + 22, y + 115, TEXT, false);

        // Recessed inventory tray (9 columns = 162px wide at x+22..x+184)
        int trayX = x + 19;
        int trayY = y + 124;
        int trayW = 168;
        int trayH = 82;
        graphics.fill(trayX, trayY, trayX + trayW, trayY + trayH, DARK_IRON);
        drawBevelBorder(graphics, trayX, trayY, trayW, trayH, PANEL_INNER, BORDER_DARK);

        // 27 main inventory slots: menu indices 3..29 -> (x + 22 + col*18, y + 127 + row*18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = 3 + row * 9 + col;
                int sx = x + 21 + col * 18;
                int sy = y + 126 + row * 18;
                boolean hasItem = slotIdx < menu.slots.size() && menu.getSlot(slotIdx).hasItem();
                drawInventorySlotFrame(graphics, sx, sy, hasItem);
            }
        }

        // Steel separator between main inventory and hotbar
        graphics.fill(x + 21, y + 182, x + 183, y + 183, STEEL);

        // 9 hotbar slots: menu indices 30..38 -> (x + 22 + col*18, y + 185)
        for (int col = 0; col < 9; col++) {
            int slotIdx = 30 + col;
            int sx = x + 21 + col * 18;
            int sy = y + 184;
            boolean hasItem = slotIdx < menu.slots.size() && menu.getSlot(slotIdx).hasItem();
            drawInventorySlotFrame(graphics, sx, sy, hasItem);
        }
    }

    private static void drawMachineSlotFrame(GuiGraphicsExtractor graphics, int x, int y,
                                             boolean hasItem, boolean active) {
        graphics.fill(x - 1, y - 1, x + 19, y + 19, STEEL);
        graphics.fill(x, y, x + 18, y + 18, PANEL_DARK);
        int topLeft = active ? DARK_COPPER : BORDER_DARK;
        int bottomRight = active ? AMBER : (hasItem ? LIGHT_STEEL : BORDER_LIGHT);
        drawBevelBorder(graphics, x, y, 18, 18, topLeft, bottomRight);
        if (hasItem) {
            graphics.fill(x + 1, y + 1, x + 3, y + 3, active ? AMBER_BRIGHT : AMBER);
        }
    }

    private static void drawInventorySlotFrame(GuiGraphicsExtractor graphics, int x, int y, boolean hasItem) {
        graphics.fill(x, y, x + 18, y + 18, PANEL_DARK);
        drawBevelBorder(graphics, x, y, 18, 18, BORDER_DARK, hasItem ? METALLIC_HIGHLIGHT : STEEL);
    }

    private static void drawHorizontalConduit(GuiGraphicsExtractor graphics, int x0, int y, int x1,
                                              boolean loaded, boolean flowing, float time) {
        int start = Math.min(x0, x1);
        int end = Math.max(x0, x1);
        graphics.fill(start, y - 2, end, y + 3, BORDER_DARK);
        graphics.fill(start, y - 1, end, y + 2, loaded ? STEEL : PANEL_INNER);
        if (flowing && end > start + 2) {
            int span = end - start - 2;
            int pulsePos = start + 1 + (int) ((time * 1.4F) % span);
            graphics.fill(pulsePos, y - 1, Math.min(end - 1, pulsePos + 3), y + 2, AMBER_BRIGHT);
        }
    }

    private static void drawRivet(GuiGraphicsExtractor graphics, int x, int y, int highlight) {
        graphics.fill(x, y, x + 3, y + 3, BORDER_DARK);
        graphics.fill(x, y, x + 2, y + 2, highlight);
    }

    private static void fillCircle(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.round(Math.sqrt(radius * radius - dy * dy));
            graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private static void drawBevelBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                        int topLeft, int bottomRight) {
        graphics.fill(x, y, x + w, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + h, topLeft);
        graphics.fill(x, y + h - 1, x + w, y + h, bottomRight);
        graphics.fill(x + w - 1, y, x + w, y + h, bottomRight);
    }

    private void drawFittedCenteredText(GuiGraphicsExtractor graphics, Component text,
                                        int centerX, int y, int maxWidth, int color) {
        int rawWidth = font.width(text);
        if (rawWidth <= maxWidth || rawWidth <= 0) {
            graphics.centeredText(font, text, centerX, y, color);
            return;
        }
        float scale = Math.max(0.65F, (float) maxWidth / (float) rawWidth);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y + 4.0F);
        graphics.pose().scale(scale, scale);
        graphics.centeredText(font, text, 0, -4, color);
        graphics.pose().popMatrix();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // All labels are rendered in extractBackground to stay aligned with the custom chassis.
    }
}
