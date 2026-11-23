package com.github.ob_yekt.simpleskills.celestial;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.StatUpgradeInfo;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.PerkRerollInfo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom endgame graphical interface for the Celestial Altar.
 * Theme: Obsidian, Runic Amethyst, Forged Gold and Nether Starlight.
 * Screen widgets handle interaction while the server validates every altar action.
 */
public final class CelestialAltarScreen extends AbstractContainerScreen<CelestialAltarMenu> {
    // Obsidian & Abyssal Slate (minecraft:obsidian)
    private static final int OBSIDIAN_VOID = 0xFF07050C;
    private static final int OBSIDIAN_DEEP = 0xFF0D0917;
    private static final int OBSIDIAN_PANEL = 0xFF140F22;
    private static final int OBSIDIAN_CHAMBER = 0xFF0A0713;
    private static final int OBSIDIAN_RAISED = 0xFF1E1633;
    private static final int OBSIDIAN_EDGE = 0xFF332652;

    // SimpleSkills Rune (simpleskills:rune)
    private static final int RUNE_DEEP = 0xFF3D1D6E;
    private static final int RUNE_PURPLE = 0xFF8E44EC;
    private static final int RUNE_BRIGHT = 0xFFC584FF;

    // Forged Gold (minecraft:gold_ingot)
    private static final int GOLD_DARK = 0xFF6B4C1D;
    private static final int GOLD_MID = 0xFFB88A38;
    private static final int GOLD = 0xFFE9C96B;
    private static final int GOLD_BRIGHT = 0xFFFFEEAA;

    // Nether Star Core (minecraft:nether_star)
    private static final int STAR_CORE = 0xFFFFFDF4;
    private static final int STAR_CYAN = 0xFF9CEBFF;

    // Typography & State Accents
    private static final int TEXT = 0xFFF5ECDA;
    private static final int MUTED = 0xFFB5A5C4;
    private static final int DIM = 0xFF675A78;
    private static final int INVALID_RED = 0xFFE45A4A;
    private static final int INVALID_FILL = 0x33E45A4A;
    private static final int SALVAGE_WARN = 0xFFFFB84D;

    // Interactive bounds for the hand-drawn tabs and ritual buttons.
    private static final int TAB_Y = 21;
    private static final int TAB_H = 16;
    private static final int INFUSION_TAB_X = 10;
    private static final int SALVAGE_TAB_X = 109;
    private static final int ENHANCE_TAB_X = 208;
    private static final int TAB_W = 94;

    private static final int ACTION_BTN_X = 81;
    private static final int ACTION_BTN_Y = 105;
    private static final int ACTION_BTN_W = 150;
    private static final int ACTION_BTN_H = 18;

    // Enhancement list: each row is an individual action, so no selection arrows or shared confirm button.
    private static final int ENHANCE_LIST_X = 52;
    private static final int ENHANCE_LIST_Y = 47;
    private static final int ENHANCE_LIST_W = 208;
    private static final int ENHANCE_ROW_H = 12;
    private static final int ENHANCE_VISIBLE_ROWS = 6;

    private int clientTicks;
    private int salvageArmedTick = -100;
    private int enhancementScroll;
    private final List<Button> enhancementRowButtons = new ArrayList<>();
    private Button infusionTabButton;
    private Button salvageTabButton;
    private Button enhanceTabButton;
    private Button ritualActionButton;

    public CelestialAltarScreen(CelestialAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 312, 244);
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 10;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        enhancementRowButtons.clear();

        // Use real Screen widgets for hit-testing. The custom drawing remains underneath,
        // but invisible hitboxes let Minecraft dispatch clicks through its normal widget path.
        infusionTabButton = addInvisibleButton(leftPos + INFUSION_TAB_X, topPos + TAB_Y,
                TAB_W, TAB_H, () -> sendTab(CelestialAltarActionPayload.TAB_INFUSION));
        salvageTabButton = addInvisibleButton(leftPos + SALVAGE_TAB_X, topPos + TAB_Y,
                TAB_W, TAB_H, () -> sendTab(CelestialAltarActionPayload.TAB_SALVAGE));
        enhanceTabButton = addInvisibleButton(leftPos + ENHANCE_TAB_X, topPos + TAB_Y,
                TAB_W, TAB_H, () -> sendTab(CelestialAltarActionPayload.TAB_ENHANCE));

        for (int row = 0; row < ENHANCE_VISIBLE_ROWS; row++) {
            final int visibleRow = row;
            enhancementRowButtons.add(addInvisibleButton(leftPos + ENHANCE_LIST_X,
                    topPos + ENHANCE_LIST_Y + row * ENHANCE_ROW_H,
                    ENHANCE_LIST_W, ENHANCE_ROW_H,
                    () -> clickEnhancementRow(visibleRow)));
        }
        ritualActionButton = addInvisibleButton(leftPos + ACTION_BTN_X, topPos + ACTION_BTN_Y,
                ACTION_BTN_W, ACTION_BTN_H, this::clickRitualAction);
        updateWidgetStates();
    }

    private Button addInvisibleButton(int x, int y, int width, int height, Runnable action) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), b -> action.run())
                .bounds(x, y, width, height).build());
        button.setAlpha(0.0F);
        return button;
    }

    private void sendTab(int action) {
        int targetMode = switch (action) {
            case CelestialAltarActionPayload.TAB_INFUSION -> CelestialAltarMenu.MODE_INFUSION;
            case CelestialAltarActionPayload.TAB_SALVAGE -> CelestialAltarMenu.MODE_SALVAGE;
            default -> CelestialAltarMenu.MODE_ENHANCE;
        };
        if (menu.getMode() == targetMode) return;
        if (action == CelestialAltarActionPayload.TAB_ENHANCE) enhancementScroll = 0;
        salvageArmedTick = -100;
        sendAction(action, "");
    }

    private void clickEnhancementRow(int visibleRow) {
        if (menu.getMode() != CelestialAltarMenu.MODE_ENHANCE || !menu.hasEnhancementRunes()) return;
        List<EnhancementRow> rows = getEnhancementRows();
        int optionIndex = enhancementScroll + visibleRow;
        if (optionIndex < 0 || optionIndex >= rows.size()) return;
        EnhancementRow row = rows.get(optionIndex);
        if (!row.actionable()) return;
        ItemStack equipment = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT).getItem();
        if (equipment.isEmpty()) return;
        sendAction(row.perk() ? CelestialAltarActionPayload.REROLL_PERK
                : CelestialAltarActionPayload.ENHANCE_STAT, row.id());
    }

    private void clickRitualAction() {
        int mode = menu.getMode();
        if (mode == CelestialAltarMenu.MODE_INFUSION) {
            if (menu.canInfuse()) sendAction(CelestialAltarActionPayload.ASCEND, "");
        } else if (mode == CelestialAltarMenu.MODE_SALVAGE && menu.canSalvage()) {
            salvageArmedTick = isSalvageArmed() ? -100 : clientTicks;
            sendAction(CelestialAltarActionPayload.SALVAGE, "");
        }
    }

    private void updateWidgetStates() {
        boolean enhance = menu.getMode() == CelestialAltarMenu.MODE_ENHANCE;
        List<EnhancementRow> rows = getEnhancementRows();
        boolean hasRunes = menu.hasEnhancementRunes();
        for (int i = 0; i < enhancementRowButtons.size(); i++) {
            Button button = enhancementRowButtons.get(i);
            int optionIndex = enhancementScroll + i;
            boolean actionable = enhance && hasRunes && optionIndex < rows.size()
                    && rows.get(optionIndex).actionable();
            button.visible = enhance;
            button.active = actionable;
        }
        if (ritualActionButton != null) {
            boolean infusion = menu.getMode() == CelestialAltarMenu.MODE_INFUSION;
            boolean salvage = menu.getMode() == CelestialAltarMenu.MODE_SALVAGE;
            ritualActionButton.visible = infusion || salvage;
            ritualActionButton.active = infusion ? menu.canInfuse() : (salvage && menu.canSalvage());
        }
        if (infusionTabButton != null) infusionTabButton.active = menu.getMode() != CelestialAltarMenu.MODE_INFUSION;
        if (salvageTabButton != null) salvageTabButton.active = menu.getMode() != CelestialAltarMenu.MODE_SALVAGE;
        if (enhanceTabButton != null) enhanceTabButton.active = menu.getMode() != CelestialAltarMenu.MODE_ENHANCE;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        clientTicks++;
        if (menu.getMode() != CelestialAltarMenu.MODE_SALVAGE || !menu.canSalvage()) {
            salvageArmedTick = -100;
        }
        enhancementScroll = Math.max(0, Math.min(maxEnhancementScroll(), enhancementScroll));
        updateWidgetStates();
    }

    private void sendAction(int action, String targetId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ClientPlayNetworking.canSend(CelestialAltarActionPayload.TYPE)) return;
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        ClientPlayNetworking.send(new CelestialAltarActionPayload(action, targetId == null ? "" : targetId));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.getMode() == CelestialAltarMenu.MODE_ENHANCE
                && isInside(mouseX, mouseY, leftPos + ENHANCE_LIST_X, topPos + ENHANCE_LIST_Y,
                ENHANCE_LIST_W, ENHANCE_ROW_H * ENHANCE_VISIBLE_ROWS)) {
            int previous = enhancementScroll;
            enhancementScroll = Math.max(0, Math.min(maxEnhancementScroll(),
                    enhancementScroll - (int) Math.signum(scrollY)));
            updateWidgetStates();
            return previous != enhancementScroll || maxEnhancementScroll() > 0;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isSalvageArmed() {
        return menu.getMode() == CelestialAltarMenu.MODE_SALVAGE
                && menu.canSalvage()
                && (clientTicks - salvageArmedTick) >= 0
                && (clientTicks - salvageArmedTick) <= 30;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int mode = menu.getMode();
        float time = clientTicks + partialTick;

        drawMainAltarFrame(graphics, time);
        drawHeaderBanner(graphics, time);
        drawModeTabs(graphics, mouseX, mouseY, mode);

        // Ritual sanctuary chamber (full width 8..304)
        int chamberX = leftPos + 8;
        int chamberY = topPos + 38;
        int chamberW = imageWidth - 16;
        int chamberH = 88;
        graphics.fill(chamberX, chamberY, chamberX + chamberW, chamberY + chamberH, OBSIDIAN_CHAMBER);
        drawBevelBorder(graphics, chamberX, chamberY, chamberW, chamberH, OBSIDIAN_VOID, OBSIDIAN_EDGE);
        drawCornerFiligree(graphics, chamberX + 2, chamberY + 2, chamberW - 4, chamberH - 4, GOLD_DARK, RUNE_PURPLE, 5);
        drawAmbientStarlight(graphics, chamberX, chamberY, chamberW, chamberH, time);

        if (mode == CelestialAltarMenu.MODE_INFUSION) {
            drawInfusionLayout(graphics, mouseX, mouseY, time);
        } else if (mode == CelestialAltarMenu.MODE_SALVAGE) {
            drawSalvageLayout(graphics, mouseX, mouseY, time);
        } else {
            drawEnhancementLayout(graphics, mouseX, mouseY, time);
        }

        if (mode != CelestialAltarMenu.MODE_ENHANCE) {
            drawActionButton(graphics, mouseX, mouseY, mode, time);
        }

        // Full-width bottom player inventory section (36 slots centered at x=75, y=142)
        graphics.fill(leftPos + 8, topPos + 127, leftPos + imageWidth - 8, topPos + 128, GOLD_DARK);
        graphics.fill(leftPos + 20, topPos + 127, leftPos + imageWidth - 20, topPos + 128, RUNE_DEEP);
        graphics.text(font, Component.translatable("container.inventory"),
                leftPos + 10, topPos + 129, GOLD, false);

        drawFullWidthPlayerInventory(graphics);
        drawFooterHint(graphics, mode);
    }

    private void drawMainAltarFrame(GuiGraphicsExtractor graphics, float time) {
        graphics.fill(leftPos - 5, topPos - 4, leftPos + imageWidth + 5, topPos + imageHeight + 6, 0x99030206);
        graphics.fill(leftPos - 3, topPos - 3, leftPos + imageWidth + 3, topPos + imageHeight + 3, OBSIDIAN_RAISED);
        graphics.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, GOLD_DARK);
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, GOLD);

        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, OBSIDIAN_DEEP);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, OBSIDIAN_PANEL);
        drawOutline(graphics, leftPos + 4, topPos + 4, imageWidth - 8, imageHeight - 8, OBSIDIAN_EDGE);

        drawCornerFiligree(graphics, leftPos + 5, topPos + 5, imageWidth - 10, imageHeight - 10, GOLD, GOLD_BRIGHT, 7);
        int pulseAlpha = 150 + (int) (Math.sin(time * 0.12F) * 65.0F);
        int gemColor = withAlpha(RUNE_BRIGHT, pulseAlpha);
        graphics.fill(leftPos + 6, topPos + 6, leftPos + 8, topPos + 8, gemColor);
        graphics.fill(leftPos + imageWidth - 8, topPos + 6, leftPos + imageWidth - 6, topPos + 8, gemColor);
        graphics.fill(leftPos + 6, topPos + imageHeight - 8, leftPos + 8, topPos + imageHeight - 6, gemColor);
        graphics.fill(leftPos + imageWidth - 8, topPos + imageHeight - 8, leftPos + imageWidth - 6, topPos + imageHeight - 6, gemColor);
    }

    private void drawHeaderBanner(GuiGraphicsExtractor graphics, float time) {
        int bannerX = leftPos + 8;
        int bannerY = topPos + 5;
        int bannerW = imageWidth - 16;
        graphics.fill(bannerX, bannerY, bannerX + bannerW, bannerY + 14, 0xAA1D1333);
        graphics.fill(bannerX, bannerY + 14, bannerX + bannerW, bannerY + 15, GOLD_DARK);
        graphics.fill(bannerX + 36, bannerY + 14, bannerX + bannerW - 36, bannerY + 15, GOLD_MID);

        Component titleText = Component.translatable("block.simpleskills.celestial_altar");
        int centerX = leftPos + imageWidth / 2;
        graphics.centeredText(font, titleText, centerX, topPos + 8, GOLD_BRIGHT);

        int halfTitle = font.width(titleText) / 2;
        int starGlow = withAlpha(STAR_CYAN, 170 + (int) (Math.sin(time * 0.16F) * 70.0F));
        drawMiniStar(graphics, centerX - halfTitle - 12, topPos + 11, GOLD, starGlow);
        drawMiniStar(graphics, centerX + halfTitle + 11, topPos + 11, GOLD, starGlow);
    }

    private void drawModeTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int mode) {
        drawSingleTab(graphics, leftPos + INFUSION_TAB_X, topPos + TAB_Y, TAB_W, TAB_H,
                Component.translatable("gui.simpleskills.celestial_altar.tab.infusion"),
                mode == CelestialAltarMenu.MODE_INFUSION,
                isInside(mouseX, mouseY, leftPos + INFUSION_TAB_X, topPos + TAB_Y, TAB_W, TAB_H));

        drawSingleTab(graphics, leftPos + SALVAGE_TAB_X, topPos + TAB_Y, TAB_W, TAB_H,
                Component.translatable("gui.simpleskills.celestial_altar.tab.salvage"),
                mode == CelestialAltarMenu.MODE_SALVAGE,
                isInside(mouseX, mouseY, leftPos + SALVAGE_TAB_X, topPos + TAB_Y, TAB_W, TAB_H));

        drawSingleTab(graphics, leftPos + ENHANCE_TAB_X, topPos + TAB_Y, TAB_W, TAB_H,
                Component.translatable("gui.simpleskills.celestial_altar.tab.enhance"),
                mode == CelestialAltarMenu.MODE_ENHANCE,
                isInside(mouseX, mouseY, leftPos + ENHANCE_TAB_X, topPos + TAB_Y, TAB_W, TAB_H));
    }

    private void drawSingleTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                               Component label, boolean selected, boolean hovered) {
        if (selected) {
            graphics.fill(x, y, x + w, y + h + 2, OBSIDIAN_CHAMBER);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h + 1, 0xCC24173D);
            graphics.fill(x, y, x + w, y + 1, GOLD_BRIGHT);
            graphics.fill(x, y, x + 1, y + h + 1, GOLD_MID);
            graphics.fill(x + w - 1, y, x + w, y + h + 1, GOLD_MID);
            graphics.fill(x + 16, y + 1, x + w - 16, y + 2, RUNE_BRIGHT);
            drawFittedCenteredText(graphics, label, x + w / 2, y + 4, w - 10, GOLD_BRIGHT);
        } else {
            int bg = hovered ? OBSIDIAN_RAISED : OBSIDIAN_DEEP;
            int border = hovered ? GOLD_MID : OBSIDIAN_EDGE;
            int textColor = hovered ? TEXT : MUTED;
            graphics.fill(x, y + 1, x + w, y + h, bg);
            drawOutline(graphics, x, y + 1, w, h - 1, border);
            drawFittedCenteredText(graphics, label, x + w / 2, y + 5, w - 10, textColor);
        }
    }

    private void drawInfusionLayout(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float time) {
        // Exact slot coordinates from CelestialAltarMenu:
        // Equipment: (148, 74), Top Essence: (148, 50), Left Essence: (112, 74), Right Essence: (184, 74)
        int equipmentX = leftPos + 148;
        int equipmentY = topPos + 74;
        int topEssenceX = leftPos + 148;
        int topEssenceY = topPos + 50;
        int leftEssenceX = leftPos + 112;
        int sideEssenceY = topPos + 74;
        int rightEssenceX = leftPos + 184;

        ItemStack equipStack = menu.getSlot(CelestialAltarBlockEntity.INFUSION_EQUIPMENT).getItem();
        ItemStack topStack = menu.getSlot(CelestialAltarBlockEntity.ESSENCE_TOP).getItem();
        ItemStack leftStack = menu.getSlot(CelestialAltarBlockEntity.ESSENCE_LEFT).getItem();
        ItemStack rightStack = menu.getSlot(CelestialAltarBlockEntity.ESSENCE_RIGHT).getItem();

        boolean topValid = topStack.is(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
        boolean leftValid = leftStack.is(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
        boolean rightValid = rightStack.is(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
        boolean equipValid = !equipStack.isEmpty() && CraftingEquipmentManager.isEligibleForCelestialAltar(equipStack);
        boolean equipAlreadyTranscendent = equipValid && CraftingEquipmentManager.isTranscendent(equipStack);
        boolean ready = menu.canInfuse();

        int centerEquipX = equipmentX + 8;
        int centerEquipY = equipmentY + 8;
        int centerTopX = topEssenceX + 8;
        int centerTopY = topEssenceY + 8;
        int centerLeftX = leftEssenceX + 8;
        int centerLeftY = sideEssenceY + 8;
        int centerRightX = rightEssenceX + 8;
        int centerRightY = sideEssenceY + 8;

        // Subtle arcane diamond aura around the central pedestal
        drawArcaneDiamondRing(graphics, centerEquipX, centerEquipY, 21,
                ready ? withAlpha(GOLD, 165) : withAlpha(RUNE_DEEP, 140));
        drawArcaneDiamondRing(graphics, centerEquipX, centerEquipY, 25,
                ready ? withAlpha(STAR_CYAN, 110) : withAlpha(OBSIDIAN_EDGE, 90));

        // Outer triangular ley-lines connecting the 3 essences
        drawEnergyConduit(graphics, centerTopX, centerTopY, centerLeftX, centerLeftY,
                topValid && leftValid, ready, time, 0.0F);
        drawEnergyConduit(graphics, centerTopX, centerTopY, centerRightX, centerRightY,
                topValid && rightValid, ready, time, 0.33F);
        drawEnergyConduit(graphics, centerLeftX, centerLeftY, centerRightX, centerRightY,
                leftValid && rightValid, ready, time, 0.66F);

        // Inner converging conduits from each essence directly into the central equipment
        drawEnergyConduit(graphics, centerTopX, centerTopY, centerEquipX, centerEquipY,
                topValid, ready, time, 0.15F);
        drawEnergyConduit(graphics, centerLeftX, centerLeftY, centerEquipX, centerEquipY,
                leftValid, ready, time, 0.45F);
        drawEnergyConduit(graphics, centerRightX, centerRightY, centerEquipX, centerEquipY,
                rightValid, ready, time, 0.75F);

        // Wide side status plaques (96px wide each) so all labels fit cleanly inside their boxes
        int validEssences = (topValid ? 1 : 0) + (leftValid ? 1 : 0) + (rightValid ? 1 : 0);
        drawEssenceCounterPlaque(graphics, leftPos + 11, topPos + 46, 96, 50, validEssences);
        drawEquipmentStatusPlaque(graphics, leftPos + 205, topPos + 46, 96, 50,
                equipStack, equipValid, equipAlreadyTranscendent);

        // Central equipment pedestal
        SlotState equipState = equipStack.isEmpty() ? SlotState.EMPTY
                : (!equipValid ? SlotState.INVALID : (equipAlreadyTranscendent ? SlotState.REFORGE : SlotState.VALID_GOLD));
        drawCentralPedestal(graphics, equipmentX, equipmentY, equipState,
                isInside(mouseX, mouseY, equipmentX, equipmentY, 16, 16), time);

        // Three triangular Transcendency Essence slots
        drawEssenceSlotFrame(graphics, topEssenceX, topEssenceY,
                essenceState(topStack, topValid), isInside(mouseX, mouseY, topEssenceX, topEssenceY, 16, 16), time);
        drawEssenceSlotFrame(graphics, leftEssenceX, sideEssenceY,
                essenceState(leftStack, leftValid), isInside(mouseX, mouseY, leftEssenceX, sideEssenceY, 16, 16), time);
        drawEssenceSlotFrame(graphics, rightEssenceX, sideEssenceY,
                essenceState(rightStack, rightValid), isInside(mouseX, mouseY, rightEssenceX, sideEssenceY, 16, 16), time);

        // Empty slot glyphs
        if (equipStack.isEmpty()) drawEquipmentGlyph(graphics, equipmentX, equipmentY);
        if (topStack.isEmpty()) drawEssenceGlyph(graphics, topEssenceX, topEssenceY);
        if (leftStack.isEmpty()) drawEssenceGlyph(graphics, leftEssenceX, sideEssenceY);
        if (rightStack.isEmpty()) drawEssenceGlyph(graphics, rightEssenceX, sideEssenceY);
    }

    private void drawSalvageLayout(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float time) {
        // Exact slot coordinates from CelestialAltarMenu:
        // Input: (104, 72), Output: (192, 72)
        int inputX = leftPos + 104;
        int outputX = leftPos + 192;
        int slotY = topPos + 72;

        ItemStack inputStack = menu.getSlot(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT).getItem();
        ItemStack outputStack = menu.getSlot(CelestialAltarBlockEntity.SALVAGE_OUTPUT).getItem();

        boolean inputValid = !inputStack.isEmpty()
                && CraftingEquipmentManager.isEligibleForCelestialAltar(inputStack)
                && CraftingEquipmentManager.isTranscendent(inputStack);
        boolean ready = menu.canSalvage();

        // Column headers cleanly centered over their respective sides
        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.celestial_altar.one_item"),
                inputX + 8, topPos + 45, 134, TEXT);
        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.celestial_altar.essence_output"),
                outputX + 8, topPos + 45, 120, RUNE_BRIGHT);

        // Transmutation conduit between sacrifice input and essence output
        int pipeY = slotY + 8;
        int pipeStartX = inputX + 21;
        int pipeEndX = outputX - 5;
        graphics.fill(pipeStartX, pipeY - 1, pipeEndX, pipeY + 2, OBSIDIAN_EDGE);
        if (ready || !outputStack.isEmpty()) {
            int flowColor = !outputStack.isEmpty() ? STAR_CYAN : RUNE_BRIGHT;
            graphics.fill(pipeStartX, pipeY, pipeEndX, pipeY + 1, flowColor);
            int pulseOffset = (int) ((time * 1.4F) % Math.max(1, (pipeEndX - pipeStartX - 4)));
            graphics.fill(pipeStartX + pulseOffset, pipeY - 1, pipeStartX + pulseOffset + 4, pipeY + 2, GOLD_BRIGHT);
        }
        int midX = (pipeStartX + pipeEndX) / 2;
        drawMiniStar(graphics, midX, pipeY, ready ? GOLD_BRIGHT : GOLD_DARK,
                !outputStack.isEmpty() ? STAR_CORE : (ready ? RUNE_BRIGHT : RUNE_DEEP));

        // Input pedestal
        SlotState inputState = inputStack.isEmpty() ? SlotState.EMPTY
                : (inputValid ? SlotState.VALID_GOLD : SlotState.INVALID);
        drawCentralPedestal(graphics, inputX, slotY, inputState,
                isInside(mouseX, mouseY, inputX, slotY, 16, 16), time);

        // Salvage Output Reliquary Pedestal
        drawSalvageOutputPedestal(graphics, outputX, slotY, !outputStack.isEmpty(),
                isInside(mouseX, mouseY, outputX, slotY, 16, 16), time);

        if (inputStack.isEmpty()) drawEquipmentGlyph(graphics, inputX, slotY);
        if (outputStack.isEmpty()) drawEssenceGlyph(graphics, outputX, slotY);

        // Double-click confirmation notice cleanly inside the chamber
        int confirmTextColor = isSalvageArmed() ? SALVAGE_WARN : MUTED;
        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.celestial_altar.double_confirm"),
                leftPos + imageWidth / 2, topPos + 95, imageWidth - 26, confirmTextColor);
    }

    private void drawEnhancementLayout(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float time) {
        int equipmentX = leftPos + 24;
        int runeX = leftPos + 272;
        int slotY = topPos + 72;
        ItemStack equipment = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT).getItem();
        ItemStack runes = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_RUNES).getItem();
        boolean validEquipment = !equipment.isEmpty()
                && CraftingEquipmentManager.isEligibleForCelestialAltar(equipment);
        boolean validRunes = !runes.isEmpty()
                && runes.is(com.github.ob_yekt.simpleskills.runes.RuneContent.RUNE);

        drawFittedCenteredText(graphics,
                Component.translatable("gui.simpleskills.celestial_altar.enhance_equipment"),
                equipmentX + 8, topPos + 53, 48, GOLD);
        drawFittedCenteredText(graphics,
                Component.translatable("gui.simpleskills.celestial_altar.enhance_runes"),
                runeX + 8, topPos + 53, 48, RUNE_BRIGHT);

        SlotState equipmentState = equipment.isEmpty() ? SlotState.EMPTY
                : (validEquipment ? SlotState.VALID_GOLD : SlotState.INVALID);
        drawCentralPedestal(graphics, equipmentX, slotY, equipmentState,
                isInside(mouseX, mouseY, equipmentX, slotY, 16, 16), time);
        drawEssenceSlotFrame(graphics, runeX, slotY, essenceState(runes, validRunes),
                isInside(mouseX, mouseY, runeX, slotY, 16, 16), time);
        if (equipment.isEmpty()) drawEquipmentGlyph(graphics, equipmentX, slotY);
        if (runes.isEmpty()) drawEssenceGlyph(graphics, runeX, slotY);

        // Small animated link makes the Rune flow visually clear without covering the option list.
        int conduitY = slotY + 8;
        graphics.fill(equipmentX + 22, conduitY, equipmentX + 25, conduitY + 1, OBSIDIAN_EDGE);
        graphics.fill(runeX - 8, conduitY, runeX - 5, conduitY + 1, OBSIDIAN_EDGE);
        if (validEquipment && validRunes) {
            int pulseX = equipmentX + 23 + (int) ((time * 1.2F) % Math.max(1, runeX - equipmentX - 48));
            graphics.fill(pulseX, conduitY - 1, pulseX + 3, conduitY + 2, RUNE_BRIGHT);
        }

        // Center column: each attribute/perk is its own button. Clicking it applies one Rune immediately.
        int listX = leftPos + ENHANCE_LIST_X;
        int listY = topPos + ENHANCE_LIST_Y;
        graphics.fill(listX - 2, listY - 3, listX + ENHANCE_LIST_W + 2,
                listY + ENHANCE_ROW_H * ENHANCE_VISIBLE_ROWS + 2, 0xAA090613);
        drawOutline(graphics, listX - 2, listY - 3, ENHANCE_LIST_W + 4,
                ENHANCE_ROW_H * ENHANCE_VISIBLE_ROWS + 5, OBSIDIAN_EDGE);
        graphics.centeredText(font,
                Component.translatable("gui.simpleskills.celestial_altar.enhance_options"),
                listX + ENHANCE_LIST_W / 2, topPos + 39, GOLD);

        List<EnhancementRow> rows = getEnhancementRows();
        if (!validEquipment) {
            drawFittedCenteredText(graphics,
                    Component.translatable("gui.simpleskills.celestial_altar.enhance_equipment_hint"),
                    listX + ENHANCE_LIST_W / 2, listY + 28, ENHANCE_LIST_W - 8, MUTED);
        } else if (rows.isEmpty()) {
            drawFittedCenteredText(graphics,
                    Component.translatable("gui.simpleskills.celestial_altar.enhance_no_stats"),
                    listX + ENHANCE_LIST_W / 2, listY + 28, ENHANCE_LIST_W - 8, MUTED);
        } else {
            enhancementScroll = Math.max(0, Math.min(maxEnhancementScroll(), enhancementScroll));
            for (int visible = 0; visible < ENHANCE_VISIBLE_ROWS; visible++) {
                int rowIndex = enhancementScroll + visible;
                if (rowIndex >= rows.size()) break;
                EnhancementRow row = rows.get(rowIndex);
                int rowY = listY + visible * ENHANCE_ROW_H;
                boolean hovered = isInside(mouseX, mouseY, listX, rowY, ENHANCE_LIST_W, ENHANCE_ROW_H);
                boolean enabled = row.actionable() && validEquipment && validRunes;
                int background = enabled ? (hovered ? 0xFF35234D : 0xFF20172F)
                        : (hovered ? 0xFF1B1528 : 0xFF120E1C);
                int edge = enabled ? (hovered ? GOLD_BRIGHT : RUNE_DEEP) : 0xFF30263F;
                graphics.fill(listX, rowY, listX + ENHANCE_LIST_W, rowY + ENHANCE_ROW_H - 1, background);
                graphics.fill(listX, rowY, listX + ENHANCE_LIST_W, rowY + 1, edge);

                int iconX = listX + 3;
                int iconY = rowY + 1;
                if (!row.perk()) {
                    drawPlusActionIcon(graphics, iconX, iconY, enabled);
                } else if (row.actionable()) {
                    drawRerollActionIcon(graphics, iconX, iconY, enabled, (int) time);
                } else {
                    drawLockedPerkIcon(graphics, iconX, iconY);
                }

                int textColor = enabled ? (hovered ? GOLD_BRIGHT : TEXT) : MUTED;
                drawFittedText(graphics, row.label(), listX + 19, rowY + 2,
                        ENHANCE_LIST_W - 24, textColor);
            }
            if (rows.size() > ENHANCE_VISIBLE_ROWS) {
                int trackX = listX + ENHANCE_LIST_W - 2;
                int trackY = listY + 1;
                int trackH = ENHANCE_ROW_H * ENHANCE_VISIBLE_ROWS - 3;
                graphics.fill(trackX, trackY, trackX + 1, trackY + trackH, OBSIDIAN_VOID);
                int thumbH = Math.max(8, trackH * ENHANCE_VISIBLE_ROWS / rows.size());
                int thumbY = trackY + (trackH - thumbH) * enhancementScroll / maxEnhancementScroll();
                graphics.fill(trackX, thumbY, trackX + 1, thumbY + thumbH, RUNE_BRIGHT);
            }
        }

        if (validEquipment && !validRunes) {
            drawFittedCenteredText(graphics,
                    Component.translatable("gui.simpleskills.celestial_altar.enhance_rune_hint"),
                    runeX + 8, topPos + 99, 48, MUTED);
        } else if (validEquipment && validRunes) {
            drawFittedCenteredText(graphics, Component.literal("×" + runes.getCount()),
                    runeX + 8, topPos + 99, 48, GOLD_BRIGHT);
        }
    }

    private record EnhancementRow(String id, Component label, boolean perk, boolean actionable) {}

    private List<EnhancementRow> getEnhancementRows() {
        ItemStack equipment = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT).getItem();
        if (equipment.isEmpty() || !CraftingEquipmentManager.isEligibleForCelestialAltar(equipment)) {
            return java.util.List.of();
        }
        java.util.ArrayList<EnhancementRow> rows = new java.util.ArrayList<>();
        for (StatUpgradeInfo info : menu.getEnhancementStats()) {
            String label = Component.translatable(info.translationKey()).getString()
                    + "  +" + formatScreenPercent(info.currentPercent()) + "%"
                    + (info.isMaxed() ? "  MAX" : "");
            rows.add(new EnhancementRow(info.id(), Component.literal(label), false, !info.isMaxed()));
        }
        for (PerkRerollInfo perk : menu.getEnhancementPerks()) {
            rows.add(new EnhancementRow(perk.id(), CraftingEquipmentManager.getPerkDisplayComponent(perk),
                    true, perk.rerollable()));
        }
        return rows;
    }

    private int maxEnhancementScroll() {
        return Math.max(0, getEnhancementRows().size() - ENHANCE_VISIBLE_ROWS);
    }

    private void drawFittedText(GuiGraphicsExtractor graphics, Component text,
                                int x, int y, int maxWidth, int color) {
        int rawWidth = font.width(text);
        if (rawWidth <= maxWidth || rawWidth <= 0) {
            graphics.text(font, text, x, y, color, false);
            return;
        }
        float scale = Math.max(0.60F, (float) maxWidth / (float) rawWidth);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + 1.0F);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    private void drawPlusActionIcon(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        int edge = enabled ? GOLD_MID : 0xFF51445E;
        int fill = enabled ? 0xFF3A2752 : 0xFF211A2B;
        graphics.fill(x, y, x + 11, y + 10, fill);
        drawOutline(graphics, x, y, 11, 10, edge);
        int color = enabled ? GOLD_BRIGHT : DIM;
        graphics.fill(x + 5, y + 2, x + 6, y + 8, color);
        graphics.fill(x + 3, y + 4, x + 8, y + 5, color);
    }

    private void drawRerollActionIcon(GuiGraphicsExtractor graphics, int x, int y,
                                      boolean enabled, int tick) {
        int color = enabled ? RUNE_BRIGHT : DIM;
        int dark = enabled ? RUNE_DEEP : 0xFF30283B;
        graphics.fill(x, y, x + 11, y + 10, enabled ? 0xFF2B1B40 : 0xFF211A2B);
        drawOutline(graphics, x, y, 11, 10, enabled ? RUNE_PURPLE : 0xFF51445E);
        // Tiny roulette wheel: outer ring, hub and rotating pointer.
        graphics.fill(x + 3, y + 2, x + 8, y + 3, dark);
        graphics.fill(x + 2, y + 3, x + 3, y + 7, dark);
        graphics.fill(x + 8, y + 3, x + 9, y + 7, dark);
        graphics.fill(x + 3, y + 7, x + 8, y + 8, dark);
        graphics.fill(x + 4, y + 3, x + 7, y + 4, color);
        graphics.fill(x + 4, y + 6, x + 7, y + 7, color);
        graphics.fill(x + 3, y + 4, x + 4, y + 6, color);
        graphics.fill(x + 7, y + 4, x + 8, y + 6, color);
        graphics.fill(x + 5, y + 4, x + 6, y + 6, GOLD_BRIGHT);
        if (enabled && (tick / 5) % 2 == 0) graphics.fill(x + 8, y + 1, x + 10, y + 2, GOLD_BRIGHT);
    }

    private void drawLockedPerkIcon(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 11, y + 10, 0xFF211A2B);
        drawOutline(graphics, x, y, 11, 10, 0xFF51445E);
        graphics.fill(x + 5, y + 3, x + 6, y + 7, DIM);
        graphics.fill(x + 3, y + 5, x + 8, y + 6, DIM);
    }

    private static String formatScreenPercent(double value) {
        double nearestWhole = Math.rint(value);
        if (Math.abs(value - nearestWhole) < 0.005) {
            return String.format(java.util.Locale.ROOT, "%.0f", nearestWhole);
        }
        double nearestTenth = Math.rint(value * 10.0) / 10.0;
        if (Math.abs(value - nearestTenth) < 0.005) {
            return String.format(java.util.Locale.ROOT, "%.1f", nearestTenth);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private void drawActionButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                  int mode, float time) {
        int x = leftPos + ACTION_BTN_X;
        int y = topPos + ACTION_BTN_Y;
        int w = ACTION_BTN_W;
        int h = ACTION_BTN_H;
        boolean infusion = mode == CelestialAltarMenu.MODE_INFUSION;
        boolean enabled = infusion ? menu.canInfuse() : menu.canSalvage();
        boolean hovered = enabled && isInside(mouseX, mouseY, x, y, w, h);
        boolean armed = !infusion && isSalvageArmed();

        String labelKey = infusion ? "gui.simpleskills.celestial_altar.ascend"
                : "gui.simpleskills.celestial_altar.salvage_confirm_button";
        Component label = Component.translatable(labelKey);

        if (!enabled) {
            graphics.fill(x, y, x + w, y + h, 0xFF120E1D);
            drawOutline(graphics, x, y, w, h, 0xFF2D2242);
            drawFittedCenteredText(graphics, label, x + w / 2, y + 5, w - 12, DIM);
            return;
        }

        int topFill = armed ? 0xFF5A3410 : (hovered ? 0xFF4B257E : 0xFF371A61);
        int bottomFill = armed ? 0xFF331B08 : (hovered ? 0xFF2A144A : 0xFF1F0E38);
        graphics.fill(x, y, x + w, y + h / 2, topFill);
        graphics.fill(x, y + h / 2, x + w, y + h, bottomFill);
        int outerBorder = armed ? SALVAGE_WARN : (hovered ? GOLD_BRIGHT : GOLD);
        drawOutline(graphics, x, y, w, h, outerBorder);
        drawOutline(graphics, x + 1, y + 1, w - 2, h - 2, armed ? GOLD_BRIGHT : RUNE_BRIGHT);
        int sweepX = x + 3 + (int) ((time * 2.2F) % (w - 14));
        graphics.fill(sweepX, y + 1, sweepX + 8, y + 2, STAR_CORE);
        graphics.fill(x + 3, y + h / 2 - 1, x + 5, y + h / 2 + 1, GOLD_BRIGHT);
        graphics.fill(x + w - 5, y + h / 2 - 1, x + w - 3, y + h / 2 + 1, GOLD_BRIGHT);
        drawFittedCenteredText(graphics, label, x + w / 2, y + 5, w - 14, STAR_CORE);
    }

    private void drawFullWidthPlayerInventory(GuiGraphicsExtractor graphics) {
        // Full-width bottom inventory vault spanning x=8..304 (296px wide)
        int trayX = leftPos + 8;
        int trayY = topPos + 139;
        int trayW = imageWidth - 16;
        int trayH = 80;
        graphics.fill(trayX, trayY, trayX + trayW, trayY + trayH, 0xCC0A0713);
        drawBevelBorder(graphics, trayX, trayY, trayW, trayH, OBSIDIAN_VOID, OBSIDIAN_EDGE);

        // Symmetrical left & right obsidian wings flanking the centered 9-column inventory (x=75..237)
        int leftWingX0 = trayX + 3;
        int leftWingX1 = leftPos + 72;
        int rightWingX0 = leftPos + 240;
        int rightWingX1 = trayX + trayW - 3;

        graphics.fill(leftWingX0, trayY + 3, leftWingX1, trayY + trayH - 3, 0x88120D20);
        drawOutline(graphics, leftWingX0, trayY + 3, leftWingX1 - leftWingX0, trayH - 6, 0xFF261C3B);
        graphics.fill(rightWingX0, trayY + 3, rightWingX1, trayY + trayH - 3, 0x88120D20);
        drawOutline(graphics, rightWingX0, trayY + 3, rightWingX1 - rightWingX0, trayH - 6, 0xFF261C3B);

        // Continuous golden hotbar separator line across the entire bottom section
        graphics.fill(leftWingX0 + 5, topPos + 197, leftWingX1 - 5, topPos + 198, GOLD_DARK);
        graphics.fill(leftPos + 74, topPos + 197, leftPos + 238, topPos + 198, GOLD_DARK);
        graphics.fill(rightWingX0 + 5, topPos + 197, rightWingX1 - 5, topPos + 198, GOLD_DARK);

        // Subtle diamond filigree centered inside each wing
        int leftMidX = (leftWingX0 + leftWingX1) / 2;
        int rightMidX = (rightWingX0 + rightWingX1) / 2;
        int wingMidY = trayY + 30;
        drawArcaneDiamondRing(graphics, leftMidX, wingMidY, 8, 0xFF2E2147);
        drawMiniStar(graphics, leftMidX, wingMidY, GOLD_DARK, RUNE_PURPLE);
        drawArcaneDiamondRing(graphics, rightMidX, wingMidY, 8, 0xFF2E2147);
        drawMiniStar(graphics, rightMidX, wingMidY, GOLD_DARK, RUNE_PURPLE);

        drawPlayerInventorySlotFrames(graphics);
    }

    private void drawPlayerInventorySlotFrames(GuiGraphicsExtractor graphics) {
        // Exact match for addStandardInventorySlots(inventory, 75, 142)
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = 8 + row * 9 + column;
                int sx = leftPos + 75 + column * 18;
                int sy = topPos + 142 + row * 18;
                drawInventorySlotFrame(graphics, sx, sy, slotIndex);
            }
        }
        for (int column = 0; column < 9; column++) {
            int slotIndex = 35 + column;
            int sx = leftPos + 75 + column * 18;
            int sy = topPos + 200;
            drawInventorySlotFrame(graphics, sx, sy, slotIndex);
        }
    }

    private void drawInventorySlotFrame(GuiGraphicsExtractor graphics, int x, int y, int menuSlotIndex) {
        ItemStack stack = menuSlotIndex < menu.slots.size()
                ? menu.getSlot(menuSlotIndex).getItem() : ItemStack.EMPTY;
        boolean isEssence = !stack.isEmpty() && stack.is(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
        boolean isTranscendent = !stack.isEmpty() && CraftingEquipmentManager.isTranscendent(stack);
        boolean isEligible = !stack.isEmpty() && CraftingEquipmentManager.isEligibleForCelestialAltar(stack);

        int border = isEssence ? RUNE_PURPLE : (isTranscendent ? GOLD_MID : 0xFF42325E);
        drawSlotFrame(graphics, x, y, border, OBSIDIAN_DEEP, false);

        if (isEssence) {
            graphics.fill(x + 1, y + 1, x + 3, y + 3, RUNE_BRIGHT);
        } else if (isTranscendent) {
            graphics.fill(x + 1, y + 1, x + 3, y + 3, GOLD_BRIGHT);
        } else if (isEligible) {
            graphics.fill(x + 1, y + 1, x + 2, y + 2, GOLD_DARK);
        }
    }

    private void drawFooterHint(GuiGraphicsExtractor graphics, int mode) {
        int fx = leftPos + 8;
        int fy = topPos + 222;
        int fw = imageWidth - 16;
        int fh = 15;
        graphics.fill(fx, fy, fx + fw, fy + fh, 0xAA0A0713);
        drawOutline(graphics, fx, fy, fw, fh, 0xFF281E3D);

        String key = mode == CelestialAltarMenu.MODE_INFUSION
                ? "gui.simpleskills.celestial_altar.infusion_hint"
                : (mode == CelestialAltarMenu.MODE_SALVAGE
                ? "gui.simpleskills.celestial_altar.salvage_hint"
                : "gui.simpleskills.celestial_altar.enhance_hint");
        Component hint = Component.translatable(key);
        drawFittedCenteredText(graphics, hint, leftPos + imageWidth / 2, fy + 4, fw - 12, MUTED);
    }

    private void drawEssenceCounterPlaque(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int validEssences) {
        graphics.fill(x, y, x + w, y + h, 0x99120D20);
        drawOutline(graphics, x, y, w, h, OBSIDIAN_EDGE);

        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.celestial_altar.essences"),
                x + w / 2, y + 5, w - 6, RUNE_BRIGHT);
        graphics.fill(x + 8, y + 16, x + w - 8, y + 17, RUNE_DEEP);

        int totalPipSpan = 2 * 16 + 4;
        int startPipX = x + (w - totalPipSpan) / 2;
        for (int i = 0; i < 3; i++) {
            int pipX = startPipX + i * 16;
            int pipY = y + 23;
            boolean active = i < validEssences;
            graphics.fill(pipX, pipY, pipX + 4, pipY + 6, active ? RUNE_BRIGHT : 0xFF241B38);
            graphics.fill(pipX + 1, pipY + 1, pipX + 3, pipY + 4, active ? STAR_CORE : 0xFF382B54);
        }

        int countColor = validEssences == 3 ? GOLD_BRIGHT : MUTED;
        graphics.centeredText(font, Component.literal(validEssences + " / 3"), x + w / 2, y + 36, countColor);
    }

    private void drawEquipmentStatusPlaque(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                           ItemStack stack, boolean valid, boolean alreadyTranscendent) {
        graphics.fill(x, y, x + w, y + h, 0x99120D20);
        drawOutline(graphics, x, y, w, h, OBSIDIAN_EDGE);

        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.celestial_altar.equipment"),
                x + w / 2, y + 5, w - 6, GOLD);
        graphics.fill(x + 8, y + 16, x + w - 8, y + 17, GOLD_DARK);

        String badge;
        int badgeColor;
        if (stack.isEmpty()) {
            badge = "—";
            badgeColor = DIM;
        } else if (!valid) {
            badge = "✕";
            badgeColor = INVALID_RED;
        } else if (alreadyTranscendent) {
            badge = "✦ ↻";
            badgeColor = RUNE_BRIGHT;
        } else {
            badge = "✦ ▲";
            badgeColor = GOLD_BRIGHT;
        }
        graphics.centeredText(font, Component.literal(badge), x + w / 2, y + 29, badgeColor);
    }

    /** Renders centered text at native 1.0x scale when it fits, scaling smoothly only if a translation exceeds maxWidth. */
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

    private enum SlotState {
        EMPTY,
        VALID_PURPLE,
        VALID_GOLD,
        REFORGE,
        INVALID
    }

    private static SlotState essenceState(ItemStack stack, boolean valid) {
        if (stack.isEmpty()) return SlotState.EMPTY;
        return valid ? SlotState.VALID_PURPLE : SlotState.INVALID;
    }

    private static void drawCentralPedestal(GuiGraphicsExtractor graphics, int slotX, int slotY,
                                            SlotState state, boolean hovered, float time) {
        int px = slotX - 4;
        int py = slotY - 4;
        int size = 24;

        int outerRim = switch (state) {
            case INVALID -> INVALID_RED;
            case REFORGE -> RUNE_BRIGHT;
            case VALID_GOLD -> GOLD_BRIGHT;
            case EMPTY -> hovered ? GOLD : GOLD_MID;
            default -> GOLD;
        };
        int innerRim = switch (state) {
            case INVALID -> 0xFF7A221B;
            case REFORGE -> GOLD_BRIGHT;
            case VALID_GOLD -> STAR_CYAN;
            default -> GOLD_DARK;
        };

        graphics.fill(px, py, px + size, py + size, OBSIDIAN_DEEP);
        drawOutline(graphics, px, py, size, size, outerRim);
        drawOutline(graphics, px + 1, py + 1, size - 2, size - 2, innerRim);

        graphics.fill(px - 1, py - 1, px + 2, py + 2, outerRim);
        graphics.fill(px + size - 2, py - 1, px + size + 1, py + 2, outerRim);
        graphics.fill(px - 1, py + size - 2, px + 2, py + size + 1, outerRim);
        graphics.fill(px + size - 2, py + size - 2, px + size + 1, py + size + 1, outerRim);

        int tickColor = (state == SlotState.VALID_GOLD || state == SlotState.REFORGE) ? STAR_CORE : GOLD_BRIGHT;
        graphics.fill(slotX + 7, py - 2, slotX + 9, py, tickColor);
        graphics.fill(slotX + 7, py + size, slotX + 9, py + size + 2, tickColor);
        graphics.fill(px - 2, slotY + 7, px, slotY + 9, tickColor);
        graphics.fill(px + size, slotY + 7, px + size + 2, slotY + 9, tickColor);

        int slotFill = switch (state) {
            case INVALID -> 0xFF261015;
            case REFORGE -> 0xFF1F1233;
            case VALID_GOLD -> 0xFF211A14;
            default -> OBSIDIAN_VOID;
        };
        drawSlotFrame(graphics, slotX, slotY, outerRim, slotFill, true);

        if (state == SlotState.VALID_GOLD || state == SlotState.REFORGE) {
            int pulse = 45 + (int) (Math.sin(time * 0.16F) * 25.0F);
            int glow = state == SlotState.REFORGE ? withAlpha(RUNE_BRIGHT, pulse) : withAlpha(GOLD_BRIGHT, pulse);
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, glow);
        } else if (state == SlotState.INVALID) {
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, INVALID_FILL);
        }
    }

    private static void drawEssenceSlotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY,
                                             SlotState state, boolean hovered, float time) {
        int px = slotX - 2;
        int py = slotY - 2;
        int outer = switch (state) {
            case INVALID -> INVALID_RED;
            case VALID_PURPLE -> RUNE_BRIGHT;
            default -> hovered ? RUNE_PURPLE : OBSIDIAN_EDGE;
        };
        graphics.fill(px, py, px + 20, py + 20, OBSIDIAN_DEEP);
        drawOutline(graphics, px, py, 20, 20, outer);

        graphics.fill(slotX + 6, py - 1, slotX + 10, py, state == SlotState.VALID_PURPLE ? GOLD_BRIGHT : GOLD_DARK);
        graphics.fill(slotX + 6, py + 20, slotX + 10, py + 21, state == SlotState.VALID_PURPLE ? GOLD_BRIGHT : GOLD_DARK);

        int fill = state == SlotState.INVALID ? 0xFF261015
                : (state == SlotState.VALID_PURPLE ? 0xFF1D1130 : OBSIDIAN_VOID);
        drawSlotFrame(graphics, slotX, slotY, state == SlotState.VALID_PURPLE ? GOLD_MID : outer, fill, false);

        if (state == SlotState.VALID_PURPLE) {
            int pulse = 40 + (int) (Math.sin(time * 0.16F) * 22.0F);
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, withAlpha(RUNE_BRIGHT, pulse));
        } else if (state == SlotState.INVALID) {
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, INVALID_FILL);
        }
    }

    private static void drawSalvageOutputPedestal(GuiGraphicsExtractor graphics, int slotX, int slotY,
                                                  boolean hasOutput, boolean hovered, float time) {
        int px = slotX - 4;
        int py = slotY - 4;
        int size = 24;

        int outer = hasOutput ? STAR_CYAN : (hovered ? RUNE_BRIGHT : RUNE_PURPLE);
        int inner = hasOutput ? GOLD_BRIGHT : GOLD_DARK;
        graphics.fill(px, py, px + size, py + size, OBSIDIAN_DEEP);
        drawOutline(graphics, px, py, size, size, outer);
        drawOutline(graphics, px + 1, py + 1, size - 2, size - 2, inner);

        graphics.fill(slotX + 6, py - 2, slotX + 10, py, hasOutput ? GOLD_BRIGHT : RUNE_PURPLE);
        graphics.fill(slotX + 6, py + size, slotX + 10, py + size + 2, hasOutput ? GOLD_BRIGHT : RUNE_PURPLE);
        graphics.fill(px - 2, slotY + 6, px, slotY + 10, hasOutput ? GOLD_BRIGHT : RUNE_PURPLE);
        graphics.fill(px + size, slotY + 6, px + size + 2, slotY + 10, hasOutput ? GOLD_BRIGHT : RUNE_PURPLE);

        drawSlotFrame(graphics, slotX, slotY, hasOutput ? GOLD_BRIGHT : RUNE_BRIGHT,
                hasOutput ? 0xFF22153B : OBSIDIAN_VOID, true);

        if (hasOutput) {
            int pulse = 55 + (int) (Math.sin(time * 0.2F) * 30.0F);
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, withAlpha(STAR_CYAN, pulse));
        }
    }

    private static void drawSlotFrame(GuiGraphicsExtractor graphics, int x, int y,
                                      int borderColor, int fillColor, boolean gildedInner) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
        graphics.fill(x, y, x + 16, y + 16, fillColor);
        int topBevel = gildedInner ? 0xFF594324 : 0xFF241B38;
        graphics.fill(x, y, x + 16, y + 1, topBevel);
        graphics.fill(x, y, x + 1, y + 16, topBevel);
        graphics.fill(x + 1, y + 15, x + 16, y + 16, OBSIDIAN_VOID);
    }

    private static void drawEssenceGlyph(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x + 7, y + 2, x + 9, y + 4, STAR_CORE);
        graphics.fill(x + 6, y + 4, x + 10, y + 6, RUNE_BRIGHT);
        graphics.fill(x + 4, y + 6, x + 12, y + 10, RUNE_PURPLE);
        graphics.fill(x + 6, y + 10, x + 10, y + 12, RUNE_DEEP);
        graphics.fill(x + 7, y + 12, x + 9, y + 14, 0xFF2B154D);
        graphics.fill(x + 7, y + 7, x + 9, y + 9, GOLD_BRIGHT);
    }

    private static void drawEquipmentGlyph(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x + 7, y + 2, x + 9, y + 4, STAR_CORE);
        graphics.fill(x + 6, y + 4, x + 10, y + 9, GOLD);
        graphics.fill(x + 7, y + 4, x + 8, y + 9, GOLD_BRIGHT);
        graphics.fill(x + 4, y + 9, x + 12, y + 11, GOLD_MID);
        graphics.fill(x + 7, y + 11, x + 9, y + 13, GOLD_DARK);
        graphics.fill(x + 6, y + 13, x + 10, y + 14, RUNE_PURPLE);
    }

    private static void drawEnergyConduit(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1,
                                          boolean active, boolean fullReady, float time, float phaseOffset) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps <= 0) return;

        int baseColor = fullReady ? GOLD_MID : (active ? RUNE_PURPLE : 0xFF261B3B);
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            graphics.fill(x, y, x + 2, y + 2, baseColor);
        }

        if (active) {
            float progress = ((time * 0.045F) + phaseOffset) % 1.0F;
            int px = x0 + Math.round((x1 - x0) * progress);
            int py = y0 + Math.round((y1 - y0) * progress);
            int pulseColor = fullReady ? STAR_CORE : GOLD_BRIGHT;
            graphics.fill(px - 1, py - 1, px + 3, py + 3, withAlpha(pulseColor, 130));
            graphics.fill(px, py, px + 2, py + 2, pulseColor);
        }
    }

    private static void drawArcaneDiamondRing(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int i = 0; i <= radius; i++) {
            int dx = i;
            int dy = radius - i;
            graphics.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            graphics.fill(cx - dx, cy + dy, cx - dx + 1, cy + dy + 1, color);
            graphics.fill(cx + dx, cy - dy, cx + dx + 1, cy - dy + 1, color);
            graphics.fill(cx - dx, cy - dy, cx - dx + 1, cy - dy + 1, color);
        }
    }

    private static void drawMiniStar(GuiGraphicsExtractor graphics, int cx, int cy, int rayColor, int coreColor) {
        graphics.fill(cx, cy - 2, cx + 1, cy + 3, rayColor);
        graphics.fill(cx - 2, cy, cx + 3, cy + 1, rayColor);
        graphics.fill(cx, cy, cx + 1, cy + 1, coreColor);
    }

    private static void drawAmbientStarlight(GuiGraphicsExtractor graphics, int x, int y, int w, int h, float time) {
        for (int i = 0; i < 14; i++) {
            double cycle = ((time * (0.012 + (i % 3) * 0.004)) + i * 0.31) % 1.0;
            int px = x + 10 + ((i * 53) % (w - 20));
            int py = y + h - 6 - (int) (cycle * (h - 12));
            int alpha = (int) (Math.sin(cycle * Math.PI) * 95.0);
            if (alpha > 8) {
                int color = (i % 3 == 0) ? withAlpha(GOLD_BRIGHT, alpha)
                        : (i % 3 == 1 ? withAlpha(RUNE_BRIGHT, alpha) : withAlpha(STAR_CYAN, alpha));
                graphics.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private static void drawCornerFiligree(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                           int primary, int accent, int len) {
        int x1 = x + w;
        int y1 = y + h;
        graphics.fill(x, y, x + len, y + 1, primary);
        graphics.fill(x, y, x + 1, y + len, primary);
        graphics.fill(x + 1, y + 1, x + 3, y + 3, accent);
        graphics.fill(x1 - len, y, x1, y + 1, primary);
        graphics.fill(x1 - 1, y, x1, y + len, primary);
        graphics.fill(x1 - 3, y + 1, x1 - 1, y + 3, accent);
        graphics.fill(x, y1 - 1, x + len, y1, primary);
        graphics.fill(x, y1 - len, x + 1, y1, primary);
        graphics.fill(x + 1, y1 - 3, x + 3, y1 - 1, accent);
        graphics.fill(x1 - len, y1 - 1, x1, y1, primary);
        graphics.fill(x1 - 1, y1 - len, x1, y1, primary);
        graphics.fill(x1 - 3, y1 - 3, x1 - 1, y1 - 1, accent);
    }

    private static void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static void drawBevelBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                        int topLeft, int bottomRight) {
        graphics.fill(x, y, x + w, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + h, topLeft);
        graphics.fill(x, y + h - 1, x + w, y + h, bottomRight);
        graphics.fill(x + w - 1, y, x + w, y + h, bottomRight);
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int withAlpha(int argb, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (clamped << 24) | (argb & 0x00FFFFFF);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Rendered inside extractBackground using the custom Celestial Altar palette.
    }
}
