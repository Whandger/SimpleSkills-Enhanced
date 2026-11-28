package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** One-page Abyssal chest screen. Double chests expand horizontally to show all 108 slots. */
public final class AbyssalChestScreen extends AbstractContainerScreen<AbyssalChestMenu> {
    private static final int FRAME_OUTER = 0xFF100D15;
    private static final int FRAME_EDGE = 0xFF55405F;
    private static final int CHEST_PANEL = 0xFF211A2A;
    private static final int INVENTORY_PANEL = 0xFF19151F;
    private static final int DIVIDER = 0xFF76588A;

    // Each slot is drawn explicitly. The custom panel used to cover the vanilla slot
    // backgrounds, leaving a large empty-looking panel with no visible grid.
    private static final int SLOT_OUTER = 0xFF09070D;
    private static final int SLOT_INNER_EDGE = 0xFF40354B;
    private static final int SLOT_FILL = 0xFF211B2A;
    private static final int SLOT_HIGHLIGHT = 0xFF675477;
    private static final int SLOT_SHADOW = 0xFF0D0A12;

    public AbyssalChestScreen(AbyssalChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.getImageWidth(), menu.getImageHeight());
        this.titleLabelX = 8;
        this.inventoryLabelY = menu.getRows() * 18 + 18;
        this.inventoryLabelX = menu.getPlayerInventoryX();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int left = leftPos;
        int top = topPos;
        int right = left + imageWidth;
        int bottom = top + imageHeight;
        int chestBottom = top + menu.getRows() * 18 + 28;

        // Dark frame and two distinct panels; this is intentionally kept behind the slots.
        graphics.fill(left - 2, top - 2, right + 2, bottom + 2, FRAME_OUTER);
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, FRAME_EDGE);
        graphics.fill(left, top, right, bottom, FRAME_OUTER);
        graphics.fill(left + 4, top + 4, right - 4, chestBottom, CHEST_PANEL);
        graphics.fill(left + 4, chestBottom + 1, right - 4, bottom - 4, INVENTORY_PANEL);
        graphics.fill(left + 8, chestBottom, right - 8, chestBottom + 1, DIVIDER);

        // Six-row chest grid: 9 columns for a single strongbox, 18 for a joined pair.
        for (int row = 0; row < menu.getRows(); row++) {
            for (int column = 0; column < menu.getColumns(); column++) {
                drawSlot(graphics,
                        left + 8 + column * 18,
                        top + 18 + row * 18);
            }
        }

        // Player inventory: 3 main rows and the hotbar, centered under either chest size.
        int inventoryLeft = left + menu.getPlayerInventoryX();
        int inventoryTop = top + menu.getPlayerInventoryY();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics,
                        inventoryLeft + column * 18,
                        inventoryTop + row * 18);
            }
        }
        int hotbarTop = inventoryTop + 58;
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, inventoryLeft + column * 18, hotbarTop);
        }
    }

    private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        // Outer recessed edge.
        graphics.fill(x, y, x + 18, y + 18, SLOT_OUTER);
        // One-pixel inset and the actual slot face.
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_INNER_EDGE);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, SLOT_FILL);
        // Subtle bevel makes every slot boundary readable without overpowering the UI.
        graphics.fill(x + 1, y + 1, x + 17, y + 2, SLOT_HIGHLIGHT);
        graphics.fill(x + 1, y + 2, x + 2, y + 17, SLOT_HIGHLIGHT);
        graphics.fill(x + 2, y + 16, x + 17, y + 17, SLOT_SHADOW);
        graphics.fill(x + 16, y + 2, x + 17, y + 16, SLOT_SHADOW);
    }
}
