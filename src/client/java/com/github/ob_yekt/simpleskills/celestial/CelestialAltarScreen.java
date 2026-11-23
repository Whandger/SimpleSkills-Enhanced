package com.github.ob_yekt.simpleskills.celestial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Custom graphical interface for the Celestial Altar's infusion and salvage tabs. */
public final class CelestialAltarScreen extends AbstractContainerScreen<CelestialAltarMenu> {
    private static final int GOLD = 0xFFE9CF7A;
    private static final int GOLD_DARK = 0xFF80612A;
    private static final int GOLD_EDGE = 0xFFB28A43;
    private static final int PANEL = 0xFF17121F;
    private static final int PANEL_INNER = 0xFF211A2C;
    private static final int PANEL_RAISED = 0xFF30243D;
    private static final int SLOT_BORDER = 0xFF5D4A68;
    private static final int SLOT_FILL = 0xFF100D18;
    private static final int SLOT_HIGHLIGHT = 0xFF8D744D;
    private static final int PURPLE = 0xFFB46CFF;
    private static final int PURPLE_DARK = 0xFF603A91;
    private static final int TEXT = 0xFFF3E8D2;
    private static final int MUTED = 0xFFB7A88D;

    private Button infusionTab;
    private Button salvageTab;
    private Button actionButton;

    public CelestialAltarScreen(CelestialAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 236, 232);
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 123;
    }

    @Override
    protected void init() {
        super.init();
        infusionTab = addRenderableWidget(Button.builder(
                Component.translatable("gui.simpleskills.celestial_altar.tab.infusion"),
                button -> click(CelestialAltarMenu.BUTTON_INFUSION_TAB))
                .bounds(leftPos + 10, topPos + 25, 104, 20).build());
        salvageTab = addRenderableWidget(Button.builder(
                Component.translatable("gui.simpleskills.celestial_altar.tab.salvage"),
                button -> click(CelestialAltarMenu.BUTTON_SALVAGE_TAB))
                .bounds(leftPos + 122, topPos + 25, 104, 20).build());
        actionButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.simpleskills.celestial_altar.ascend"),
                button -> click(menu.getMode() == CelestialAltarMenu.MODE_INFUSION
                        ? CelestialAltarMenu.BUTTON_ASCEND : CelestialAltarMenu.BUTTON_SALVAGE_CONFIRM))
                .bounds(leftPos + 66, topPos + 105, 104, 20).build());
        refreshButtons();
    }

    private void click(int buttonId) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null) client.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    private void refreshButtons() {
        boolean infusion = menu.getMode() == CelestialAltarMenu.MODE_INFUSION;
        if (infusionTab != null) infusionTab.active = !infusion;
        if (salvageTab != null) salvageTab.active = infusion;
        if (actionButton != null) {
            actionButton.setMessage(Component.translatable(infusion
                    ? "gui.simpleskills.celestial_altar.ascend"
                    : "gui.simpleskills.celestial_altar.salvage_confirm_button"));
            actionButton.active = infusion ? menu.canInfuse() : menu.canSalvage();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The vanilla container background is intentionally covered by the custom frame. Slot
        // backplates are redrawn below so neither the altar inputs nor player inventory disappear.
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        refreshButtons();

        graphics.fill(leftPos - 3, topPos - 3, leftPos + imageWidth + 3, topPos + imageHeight + 3, 0xCC08060D);
        graphics.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, GOLD_DARK);
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, GOLD);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, PANEL_INNER);

        // Header and section separators.
        graphics.fill(leftPos + 8, topPos + 21, leftPos + imageWidth - 8, topPos + 22, GOLD_DARK);
        graphics.centeredText(font, Component.translatable("block.simpleskills.celestial_altar"),
                leftPos + imageWidth / 2, topPos + 8, GOLD);
        graphics.fill(leftPos + 10, topPos + 49, leftPos + imageWidth - 10, topPos + 50, GOLD_DARK);
        graphics.fill(leftPos + 7, topPos + 130, leftPos + imageWidth - 7, topPos + 131, GOLD_DARK);

        // Subtle, recessed tray behind the player's 27 inventory slots and hotbar.
        graphics.fill(leftPos + 6, topPos + 133, leftPos + imageWidth - 6, topPos + 217, 0xB20D0A12);
        drawPlayerInventorySlotFrames(graphics);

        if (menu.getMode() == CelestialAltarMenu.MODE_INFUSION) {
            drawInfusionLayout(graphics);
        } else {
            drawSalvageLayout(graphics);
        }

        graphics.text(font, Component.translatable("container.inventory"),
                leftPos + 8, topPos + 123, TEXT, false);
    }

    private void drawInfusionLayout(GuiGraphicsExtractor graphics) {
        graphics.centeredText(font, Component.translatable("gui.simpleskills.celestial_altar.essences"),
                leftPos + 118, topPos + 14, PURPLE);
        graphics.centeredText(font, Component.translatable("gui.simpleskills.celestial_altar.equipment"),
                leftPos + 119, topPos + 94, TEXT);

        int equipmentX = leftPos + 111;
        int equipmentY = topPos + 73;
        int topEssenceX = leftPos + 111;
        int topEssenceY = topPos + 51;
        int leftEssenceX = leftPos + 71;
        int sideEssenceY = topPos + 73;
        int rightEssenceX = leftPos + 151;

        // Golden/purple connector lines make the three-essence triangle easy to read.
        drawLine(graphics, leftEssenceX + 9, sideEssenceY + 9,
                equipmentX + 9, equipmentY + 9, 0xFF6D4D8F);
        drawLine(graphics, rightEssenceX + 9, sideEssenceY + 9,
                equipmentX + 9, equipmentY + 9, 0xFF6D4D8F);
        drawLine(graphics, topEssenceX + 9, topEssenceY + 17,
                equipmentX + 9, equipmentY + 1, 0xFF8A6631);

        drawSlotFrame(graphics, equipmentX, equipmentY, true);
        drawSlotFrame(graphics, topEssenceX, topEssenceY, false);
        drawSlotFrame(graphics, leftEssenceX, sideEssenceY, false);
        drawSlotFrame(graphics, rightEssenceX, sideEssenceY, false);
        if (menu.getSlot(CelestialAltarBlockEntity.INFUSION_EQUIPMENT).getItem().isEmpty()) {
            drawEquipmentGlyph(graphics, equipmentX, equipmentY);
        }
        if (menu.getSlot(CelestialAltarBlockEntity.ESSENCE_TOP).getItem().isEmpty()) {
            drawEssenceGlyph(graphics, topEssenceX, topEssenceY);
        }
        if (menu.getSlot(CelestialAltarBlockEntity.ESSENCE_LEFT).getItem().isEmpty()) {
            drawEssenceGlyph(graphics, leftEssenceX, sideEssenceY);
        }
        if (menu.getSlot(CelestialAltarBlockEntity.ESSENCE_RIGHT).getItem().isEmpty()) {
            drawEssenceGlyph(graphics, rightEssenceX, sideEssenceY);
        }
    }

    private void drawSalvageLayout(GuiGraphicsExtractor graphics) {
        int inputX = leftPos + 84;
        int outputX = leftPos + 148;
        int slotY = topPos + 72;

        graphics.centeredText(font, Component.translatable("gui.simpleskills.celestial_altar.one_item"),
                leftPos + 92, topPos + 57, TEXT);
        graphics.centeredText(font, Component.translatable("gui.simpleskills.celestial_altar.essence_output"),
                leftPos + 162, topPos + 57, PURPLE);
        graphics.centeredText(font, Component.translatable("gui.simpleskills.celestial_altar.double_confirm"),
                leftPos + imageWidth / 2, topPos + 96, MUTED);

        drawSlotFrame(graphics, inputX, slotY, true);
        drawSlotFrame(graphics, outputX, slotY, false);
        if (menu.getSlot(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT).getItem().isEmpty()) {
            drawEquipmentGlyph(graphics, inputX, slotY);
        }
        if (menu.getSlot(CelestialAltarBlockEntity.SALVAGE_OUTPUT).getItem().isEmpty()) {
            drawEssenceGlyph(graphics, outputX, slotY);
        }
    }

    private void drawPlayerInventorySlotFrames(GuiGraphicsExtractor graphics) {
        // addStandardInventorySlots(inventory, 8, 137): 3x9 storage slots plus a 9-slot hotbar.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotFrame(graphics, leftPos + 8 + column * 18,
                        topPos + 137 + row * 18, false);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlotFrame(graphics, leftPos + 8 + column * 18, topPos + 195, false);
        }
    }

    private static void drawSlotFrame(GuiGraphicsExtractor graphics, int x, int y, boolean important) {
        int border = important ? GOLD_EDGE : SLOT_BORDER;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, border);
        graphics.fill(x, y, x + 16, y + 16, SLOT_FILL);
        graphics.fill(x + 1, y + 1, x + 15, y + 2, important ? 0xFF7E6743 : 0xFF3C3049);
        graphics.fill(x + 1, y + 1, x + 2, y + 15, important ? 0xFF7E6743 : 0xFF3C3049);
        graphics.fill(x + 2, y + 14, x + 15, y + 15, 0xFF08060D);
    }

    private static void drawEssenceGlyph(GuiGraphicsExtractor graphics, int x, int y) {
        // A small purple crystal placeholder; the actual item is rendered above it when inserted.
        graphics.fill(x + 7, y + 3, x + 9, y + 5, 0xFFE9D7FF);
        graphics.fill(x + 5, y + 5, x + 11, y + 8, PURPLE);
        graphics.fill(x + 6, y + 8, x + 10, y + 11, PURPLE_DARK);
        graphics.fill(x + 7, y + 11, x + 9, y + 13, 0xFF38234F);
    }

    private static void drawEquipmentGlyph(GuiGraphicsExtractor graphics, int x, int y) {
        // Neutral golden equipment marker (not an actual item stack or a valid inventory icon).
        graphics.fill(x + 7, y + 3, x + 9, y + 6, 0xFFEBD7A1);
        graphics.fill(x + 5, y + 6, x + 11, y + 9, GOLD);
        graphics.fill(x + 6, y + 9, x + 10, y + 12, 0xFF98713A);
        graphics.fill(x + 7, y + 12, x + 9, y + 13, GOLD_DARK);
    }

    private static void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps <= 0) return;
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // The altar title and inventory label are drawn with the custom panel palette above.
    }
}
