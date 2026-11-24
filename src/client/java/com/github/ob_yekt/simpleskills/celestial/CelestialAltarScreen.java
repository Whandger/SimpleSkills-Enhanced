package com.github.ob_yekt.simpleskills.celestial;

import com.github.ob_yekt.simpleskills.celestial.CelestialAltarAnimationController.CrackSegment;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarAnimationController.DisintegrationFragment;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarAnimationController.EnhanceState;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarAnimationController.FracturePath;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarAnimationController.InfusionState;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarAnimationController.ItemSilhouette;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarAnimationController.SalvageState;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.PerkRerollInfo;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.StatUpgradeInfo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
    private static final int INFUSION_TAB_X = 30;
    private static final int SALVAGE_TAB_X = 129;
    private static final int ENHANCE_TAB_X = 228;
    private static final int TAB_W = 94;

    private static final int ACTION_BTN_X = 101;
    private static final int ACTION_BTN_Y = 105;
    private static final int ACTION_BTN_W = 150;
    private static final int ACTION_BTN_H = 18;

    // Enhancement list: each row is an individual action, so no selection arrows or shared confirm button.
    private static final int ENHANCE_LIST_X = 78;
    private static final int ENHANCE_LIST_Y = 53;
    private static final int ENHANCE_LIST_W = 196;
    private static final int ENHANCE_ROW_H = 9;
    private static final int ENHANCE_VISIBLE_ROWS = 8;

    private int clientTicks;
    private int salvageArmedTick = -100;
    private int enhancementScroll;
    private final List<Button> enhancementRowButtons = new ArrayList<>();
    private final List<Button> enhancementStatRerollButtons = new ArrayList<>();
    // Bind each visible hitbox to the exact row rendered beneath it. The stat list can
    // reorder after a reroll and server slot synchronization; resolving a click by its
    // current visible index could otherwise apply the action to the previous row.
    private final EnhancementRow[] renderedEnhancementRows = new EnhancementRow[ENHANCE_VISIBLE_ROWS];
    private ItemStack renderedEnhancementEquipment = ItemStack.EMPTY;
    private Button infusionTabButton;
    private Button salvageTabButton;
    private Button enhanceTabButton;
    private Button ritualActionButton;

    public CelestialAltarScreen(CelestialAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 352, 244);
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 10;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        enhancementRowButtons.clear();
        enhancementStatRerollButtons.clear();
        for (int i = 0; i < renderedEnhancementRows.length; i++) renderedEnhancementRows[i] = null;
        renderedEnhancementEquipment = ItemStack.EMPTY;

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
            // Attribute controls: reroll on the left, strengthen on the right.
            // Perk rows use the left control for their replacement roll and have no plus button.
            enhancementRowButtons.add(addInvisibleButton(
                    leftPos + ENHANCE_LIST_X + ENHANCE_LIST_W - 12,
                    topPos + ENHANCE_LIST_Y + row * ENHANCE_ROW_H,
                    12, ENHANCE_ROW_H,
                    () -> clickEnhancementRow(visibleRow)));
            enhancementStatRerollButtons.add(addInvisibleButton(leftPos + ENHANCE_LIST_X,
                    topPos + ENHANCE_LIST_Y + row * ENHANCE_ROW_H,
                    12, ENHANCE_ROW_H,
                    () -> clickEnhancementStatReroll(visibleRow)));
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
        if (CelestialAltarAnimationController.isBlockingRitualActive()) return;
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

    /** Right-side button: spend one Rune to strengthen only the attribute shown on this row. */
    private void clickEnhancementRow(int visibleRow) {
        if (menu.getMode() != CelestialAltarMenu.MODE_ENHANCE || !menu.hasEnhancementRunes()) return;
        EnhancementRow row = getRenderedEnhancementRow(visibleRow);
        if (row == null || row.perk() || !row.actionable()) return;
        ItemStack equipment = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT).getItem();
        if (!matchesRenderedEquipment(equipment)) return;
        sendAction(CelestialAltarActionPayload.ENHANCE_STAT, row.id());
    }

    /** Left-side button: replace only the exact attribute/perk represented by this rendered row. */
    private void clickEnhancementStatReroll(int visibleRow) {
        if (menu.getMode() != CelestialAltarMenu.MODE_ENHANCE || !menu.hasEnhancementRunes()) return;
        EnhancementRow row = getRenderedEnhancementRow(visibleRow);
        if (row == null || !row.rerollable()) return;
        ItemStack equipment = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT).getItem();
        if (!matchesRenderedEquipment(equipment)) return;
        sendAction(row.perk() ? CelestialAltarActionPayload.REROLL_PERK
                : CelestialAltarActionPayload.REROLL_STAT, row.id());
    }

    private EnhancementRow getRenderedEnhancementRow(int visibleRow) {
        if (visibleRow < 0 || visibleRow >= renderedEnhancementRows.length) return null;
        return renderedEnhancementRows[visibleRow];
    }

    private boolean matchesRenderedEquipment(ItemStack equipment) {
        return equipment != null && !equipment.isEmpty()
                && !renderedEnhancementEquipment.isEmpty()
                && ItemStack.matches(renderedEnhancementEquipment, equipment);
    }

    private void clickRitualAction() {
        if (CelestialAltarAnimationController.isBlockingRitualActive()) return;
        int mode = menu.getMode();
        if (mode == CelestialAltarMenu.MODE_INFUSION) {
            if (menu.canInfuse()) sendAction(CelestialAltarActionPayload.ASCEND, "");
        } else if (mode == CelestialAltarMenu.MODE_SALVAGE && menu.canSalvage()) {
            salvageArmedTick = isSalvageArmed() ? -100 : clientTicks;
            sendAction(CelestialAltarActionPayload.SALVAGE, "");
        }
    }

    private void updateWidgetStates() {
        boolean blocking = CelestialAltarAnimationController.isBlockingRitualActive();
        boolean enhance = menu.getMode() == CelestialAltarMenu.MODE_ENHANCE;
        updateEnhancementButtonStates(enhance);
        if (ritualActionButton != null) {
            boolean infusion = menu.getMode() == CelestialAltarMenu.MODE_INFUSION;
            boolean salvage = menu.getMode() == CelestialAltarMenu.MODE_SALVAGE;
            ritualActionButton.visible = infusion || salvage;
            ritualActionButton.active = !blocking && (infusion ? menu.canInfuse() : (salvage && menu.canSalvage()));
        }
        if (infusionTabButton != null) infusionTabButton.active = !blocking && menu.getMode() != CelestialAltarMenu.MODE_INFUSION;
        if (salvageTabButton != null) salvageTabButton.active = !blocking && menu.getMode() != CelestialAltarMenu.MODE_SALVAGE;
        if (enhanceTabButton != null) enhanceTabButton.active = !blocking && menu.getMode() != CelestialAltarMenu.MODE_ENHANCE;
    }

    private void updateEnhancementButtonStates(boolean enhanceMode) {
        boolean hasRunes = menu.hasEnhancementRunes();
        for (int i = 0; i < enhancementRowButtons.size(); i++) {
            EnhancementRow row = renderedEnhancementRows[i];
            boolean hasRow = enhanceMode && row != null;
            Button enhanceButton = enhancementRowButtons.get(i);
            Button rerollButton = enhancementStatRerollButtons.get(i);
            enhanceButton.visible = enhanceMode;
            enhanceButton.active = hasRow && hasRunes && !row.perk() && row.actionable();
            rerollButton.visible = enhanceMode;
            rerollButton.active = hasRow && hasRunes && row.rerollable();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        clientTicks++;
        CelestialAltarAnimationController.tick(menu.containerId);
        if (menu.getMode() != CelestialAltarMenu.MODE_SALVAGE || !menu.canSalvage()) {
            salvageArmedTick = -100;
        }
        enhancementScroll = Math.max(0, Math.min(maxEnhancementScroll(), enhancementScroll));
        updateWidgetStates();
    }

    private void sendAction(int action, String targetId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ClientPlayNetworking.canSend(CelestialAltarActionPayload.TYPE)) return;

        int expectedRevision = -1;
        if (action == CelestialAltarActionPayload.ENHANCE_STAT
                || action == CelestialAltarActionPayload.REROLL_STAT
                || action == CelestialAltarActionPayload.REROLL_PERK) {
            ItemStack currentEquipment = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT).getItem();
            // Bind the request to the exact item snapshot whose row was rendered. The server
            // rejects it if another operation has already changed any enhancement on the item.
            if (!matchesRenderedEquipment(currentEquipment)) return;
            expectedRevision = CraftingEquipmentManager.getEnhancementRevision(renderedEnhancementEquipment);
            if (expectedRevision < 0) return;
        }

        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        ClientPlayNetworking.send(new CelestialAltarActionPayload(
                action, targetId == null ? "" : targetId, expectedRevision));
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

        // Ritual sanctuary chamber (full width 8..344)
        int chamberX = leftPos + 8;
        int chamberY = topPos + 38;
        int chamberW = imageWidth - 16;
        int chamberH = 88;
        graphics.fill(chamberX, chamberY, chamberX + chamberW, chamberY + chamberH, OBSIDIAN_CHAMBER);
        drawBevelBorder(graphics, chamberX, chamberY, chamberW, chamberH, OBSIDIAN_VOID, OBSIDIAN_EDGE);
        drawCornerFiligree(graphics, chamberX + 2, chamberY + 2, chamberW - 4, chamberH - 4, GOLD_DARK, RUNE_PURPLE, 5);
        drawAmbientStarlight(graphics, chamberX, chamberY, chamberW, chamberH, time);

        if (mode == CelestialAltarMenu.MODE_INFUSION) {
            drawInfusionLayout(graphics, mouseX, mouseY, time, partialTick);
        } else if (mode == CelestialAltarMenu.MODE_SALVAGE) {
            drawSalvageLayout(graphics, mouseX, mouseY, time, partialTick);
        } else {
            drawEnhancementLayout(graphics, mouseX, mouseY, time, partialTick);
        }

        if (mode != CelestialAltarMenu.MODE_ENHANCE) {
            drawActionButton(graphics, mouseX, mouseY, mode, time);
        }

        // Full-width bottom player inventory section
        graphics.fill(leftPos + 8, topPos + 127, leftPos + imageWidth - 8, topPos + 128, GOLD_DARK);
        graphics.fill(leftPos + 20, topPos + 127, leftPos + imageWidth - 20, topPos + 128, RUNE_DEEP);
        graphics.text(font, Component.translatable("container.inventory"),
                leftPos + 10, topPos + 129, GOLD, false);

        drawFullWidthPlayerInventory(graphics);
        drawFooterHint(graphics, mode);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // extractLabels renders AFTER container slots, allowing preserved snapshots and ritual
        // animations to layer cleanly over the slots even after the server consumes/updates items.
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float time = clientTicks + partialTick;
        int mode = menu.getMode();

        graphics.pose().pushMatrix();
        graphics.pose().translate(-leftPos, -topPos);

        if (mode == CelestialAltarMenu.MODE_INFUSION) {
            InfusionState infusion = CelestialAltarAnimationController.getInfusion();
            if (infusion != null) {
                renderInfusionForeground(graphics, infusion, time, partialTick);
            }
        } else if (mode == CelestialAltarMenu.MODE_SALVAGE) {
            SalvageState salvage = CelestialAltarAnimationController.getSalvage();
            if (salvage != null) {
                renderSalvageForeground(graphics, salvage, time, partialTick);
            }
        } else if (mode == CelestialAltarMenu.MODE_ENHANCE) {
            EnhanceState enhance = CelestialAltarAnimationController.getEnhance();
            if (enhance != null) {
                renderEnhanceForeground(graphics, enhance, time, partialTick);
            }
        }

        graphics.pose().popMatrix();
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

    // ------------------------------------------------------------------ INFUSION LAYOUT & RITUAL VFX

    private void drawInfusionLayout(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                    float time, float partialTick) {
        int equipmentX = leftPos + 168;
        int equipmentY = topPos + 74;
        int topEssenceX = leftPos + 168;
        int topEssenceY = topPos + 50;
        int leftEssenceX = leftPos + 132;
        int sideEssenceY = topPos + 74;
        int rightEssenceX = leftPos + 204;

        InfusionState infusion = CelestialAltarAnimationController.getInfusion();
        boolean ritualActive = infusion != null;

        ItemStack equipStack = ritualActive
                ? infusion.beforeEquipment
                : menu.getSlot(CelestialAltarBlockEntity.INFUSION_EQUIPMENT).getItem();
        ItemStack topStack = ritualActive
                ? infusion.essenceSnapshot
                : menu.getSlot(CelestialAltarBlockEntity.ESSENCE_TOP).getItem();
        ItemStack leftStack = ritualActive
                ? infusion.essenceSnapshot
                : menu.getSlot(CelestialAltarBlockEntity.ESSENCE_LEFT).getItem();
        ItemStack rightStack = ritualActive
                ? infusion.essenceSnapshot
                : menu.getSlot(CelestialAltarBlockEntity.ESSENCE_RIGHT).getItem();

        boolean topValid = ritualActive || topStack.is(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
        boolean leftValid = ritualActive || leftStack.is(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
        boolean rightValid = ritualActive || rightStack.is(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
        boolean equipValid = ritualActive
                || (!equipStack.isEmpty() && CraftingEquipmentManager.isEligibleForCelestialAltar(equipStack));
        boolean equipAlreadyTranscendent = equipValid && CraftingEquipmentManager.isTranscendent(equipStack);
        boolean ready = ritualActive || menu.canInfuse();

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

        // Rotating magical rune circle beneath the equipment during Infusion ritual
        if (ritualActive) {
            float p = infusion.progress(partialTick);
            float circleAlpha = Mth.clamp(p / 0.18F, 0.0F, 1.0F)
                    * (p > 0.85F ? Mth.clamp((1.0F - p) / 0.15F, 0.0F, 1.0F) : 1.0F);
            drawRotatingRuneCircle(graphics, centerEquipX, centerEquipY + 2, 23.0F,
                    time * 0.11F, circleAlpha);
        }

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

        int validEssences = (topValid ? 1 : 0) + (leftValid ? 1 : 0) + (rightValid ? 1 : 0);
        drawEssenceCounterPlaque(graphics, leftPos + 31, topPos + 46, 96, 50, validEssences);
        drawEquipmentStatusPlaque(graphics, leftPos + 225, topPos + 46, 96, 50,
                equipStack, equipValid, equipAlreadyTranscendent);

        SlotState equipState = equipStack.isEmpty() ? SlotState.EMPTY
                : (!equipValid ? SlotState.INVALID : (equipAlreadyTranscendent ? SlotState.REFORGE : SlotState.VALID_GOLD));
        drawCentralPedestal(graphics, equipmentX, equipmentY, equipState,
                isInside(mouseX, mouseY, equipmentX, equipmentY, 16, 16), time);

        drawEssenceSlotFrame(graphics, topEssenceX, topEssenceY,
                essenceState(topStack, topValid), isInside(mouseX, mouseY, topEssenceX, topEssenceY, 16, 16), time);
        drawEssenceSlotFrame(graphics, leftEssenceX, sideEssenceY,
                essenceState(leftStack, leftValid), isInside(mouseX, mouseY, leftEssenceX, sideEssenceY, 16, 16), time);
        drawEssenceSlotFrame(graphics, rightEssenceX, sideEssenceY,
                essenceState(rightStack, rightValid), isInside(mouseX, mouseY, rightEssenceX, sideEssenceY, 16, 16), time);

        if (!ritualActive) {
            if (equipStack.isEmpty()) drawEquipmentGlyph(graphics, equipmentX, equipmentY);
            if (topStack.isEmpty()) drawEssenceGlyph(graphics, topEssenceX, topEssenceY);
            if (leftStack.isEmpty()) drawEssenceGlyph(graphics, leftEssenceX, sideEssenceY);
            if (rightStack.isEmpty()) drawEssenceGlyph(graphics, rightEssenceX, sideEssenceY);
        }
    }

    private void renderInfusionForeground(GuiGraphicsExtractor graphics, InfusionState infusion,
                                          float time, float partialTick) {
        float p = infusion.progress(partialTick);
        // Phases:
        // Phase 1: 0.00..0.28 (Essence Activation & three beams launching)
        // Phase 2: 0.28..0.72 (Equipment Ascension, levitation, vertical rotation, intensifying glow)
        // Phase 3: 0.72..0.93 (Transcendence Burst, flash, expanding magical rings)
        // Phase 4: 0.93..1.00 (Clean settle into CraftingRevealScreen)
        int topX = leftPos + 168;
        int topY = topPos + 50;
        int leftX = leftPos + 132;
        int sideY = topPos + 74;
        int rightX = leftPos + 204;
        int equipSlotX = leftPos + 168;
        int equipSlotY = topPos + 74;

        float ascensionFactor = p < 0.25F ? 0.0F
                : (p < 0.75F ? easeInOutCubic((p - 0.25F) / 0.50F) : 1.0F);
        float floatOffsetY = -13.0F * ascensionFactor - (float) Math.sin(time * 0.24F) * 1.4F * ascensionFactor;
        float equipCenterX = equipSlotX + 8.0F;
        float equipCenterY = equipSlotY + 8.0F + floatOffsetY;

        // 1. Render the three preserved Transcendency Essences glowing in their input slots
        float essencePresence = p < 0.72F ? 1.0F : Mth.clamp((0.86F - p) / 0.14F, 0.0F, 1.0F);
        if (essencePresence > 0.02F) {
            drawGlowingEssenceSlot(graphics, infusion.essenceSnapshot, topX, topY, essencePresence, time, 0.0F);
            drawGlowingEssenceSlot(graphics, infusion.essenceSnapshot, leftX, sideY, essencePresence, time, 2.1F);
            drawGlowingEssenceSlot(graphics, infusion.essenceSnapshot, rightX, sideY, essencePresence, time, 4.2F);
        }

        // 2. Render the three continuous energy beams from each individual Essence into the floating equipment
        if (p < 0.80F) {
            float beamReach = Mth.clamp(p / 0.24F, 0.0F, 1.0F);
            float intensity = p < 0.70F
                    ? (0.45F + 0.55F * Mth.clamp(p / 0.65F, 0.0F, 1.0F))
                    : Mth.clamp((0.80F - p) / 0.10F, 0.0F, 1.0F);
            drawInfusionEnergyBeam(graphics, topX + 8.0F, topY + 8.0F, equipCenterX, equipCenterY,
                    beamReach, intensity, time, 0.0F, -6.0F);
            drawInfusionEnergyBeam(graphics, leftX + 8.0F, sideY + 8.0F, equipCenterX, equipCenterY,
                    beamReach, intensity, time, 0.33F, 7.0F);
            drawInfusionEnergyBeam(graphics, rightX + 8.0F, sideY + 8.0F, equipCenterX, equipCenterY,
                    beamReach, intensity, time, 0.66F, 7.0F);
        }

        // 3. Levitating, rotating, glowing central equipment
        boolean postBurst = p >= 0.73F;
        ItemStack shownEquipment = postBurst && !infusion.afterEquipment.isEmpty()
                ? infusion.afterEquipment
                : infusion.beforeEquipment;

        float glowStrength = p < 0.73F
                ? Mth.clamp(p / 0.73F, 0.15F, 1.0F)
                : Mth.clamp((1.0F - p) / 0.27F, 0.25F, 1.0F);
        int auraRadius = Math.round(11 + glowStrength * 9.0F + (float) Math.sin(time * 0.45F) * 2.0F);
        int purpleAuraAlpha = Math.round(55 + glowStrength * 115.0F);
        int whiteCoreAlpha = Math.round(30 + glowStrength * 110.0F);
        fillSoftCircle(graphics, Math.round(equipCenterX), Math.round(equipCenterY),
                auraRadius, withAlpha(RUNE_PURPLE, purpleAuraAlpha));
        fillSoftCircle(graphics, Math.round(equipCenterX), Math.round(equipCenterY),
                Math.max(5, auraRadius - 5), withAlpha(STAR_CORE, whiteCoreAlpha));

        // Orbiting energized particles rising around the ascending equipment
        int orbitCount = 10;
        float orbitRadius = 15.0F - ascensionFactor * 3.0F;
        for (int i = 0; i < orbitCount; i++) {
            float angle = time * (0.16F + ascensionFactor * 0.14F) + i * (float) (Math.PI * 2.0 / orbitCount);
            float ox = equipCenterX + (float) Math.cos(angle) * orbitRadius;
            float oy = equipCenterY + (float) Math.sin(angle) * (orbitRadius * 0.65F);
            int pColor = (i % 2 == 0)
                    ? withAlpha(STAR_CORE, Math.round(140 + 115 * glowStrength))
                    : withAlpha(RUNE_BRIGHT, Math.round(150 + 105 * glowStrength));
            graphics.fill(Math.round(ox) - 1, Math.round(oy) - 1, Math.round(ox) + 1, Math.round(oy) + 1, pColor);
        }

        // Vertical-axis rotation + pulse scale
        float spinPhase = ascensionFactor * (float) (Math.PI * 4.0);
        float rawCos = (float) Math.cos(spinPhase);
        float horizontalSpinScale = Math.copySign(Math.max(0.16F, Math.abs(rawCos)), rawCos);
        float burstPop = postBurst
                ? 1.0F + 0.34F * (float) Math.sin(Math.min(1.0F, (p - 0.73F) / 0.18F) * Math.PI)
                : 1.0F + 0.14F * ascensionFactor + 0.04F * (float) Math.sin(time * 0.5F);
        float tiltRad = (float) Math.sin(spinPhase * 0.5F) * 0.09F * ascensionFactor;

        graphics.pose().pushMatrix();
        graphics.pose().translate(equipCenterX, equipCenterY);
        if (tiltRad != 0.0F) graphics.pose().rotate(tiltRad);
        graphics.pose().scale(horizontalSpinScale * burstPop, burstPop);
        graphics.item(shownEquipment, -8, -8);
        graphics.pose().popMatrix();

        // 4. Phase 3 — Transcendence Burst (intense flash, expanding rings, radial particles)
        if (p >= 0.70F && p <= 0.96F) {
            float burstT = (p - 0.70F) / 0.26F;
            int cx = Math.round(equipCenterX);
            int cy = Math.round(equipCenterY);

            // Brief intense purple & white flash over equipment
            if (burstT < 0.45F) {
                float flashAlpha = (1.0F - burstT / 0.45F);
                fillSoftCircle(graphics, cx, cy, Math.round(16 + flashAlpha * 14),
                        withAlpha(STAR_CORE, Math.round(flashAlpha * 225)));
                fillSoftCircle(graphics, cx, cy, Math.round(22 + flashAlpha * 18),
                        withAlpha(RUNE_BRIGHT, Math.round(flashAlpha * 165)));
            }

            // Dual expanding magical rings
            float ring1R = 8.0F + burstT * 44.0F;
            float ring2R = 4.0F + Math.max(0.0F, burstT - 0.12F) * 38.0F;
            int ringAlpha = Math.round((1.0F - burstT) * 230.0F);
            drawCircleRing(graphics, cx, cy, ring1R, withAlpha(STAR_CORE, ringAlpha));
            drawCircleRing(graphics, cx, cy, ring1R - 1.5F, withAlpha(RUNE_BRIGHT, ringAlpha));
            if (burstT > 0.12F) {
                drawCircleRing(graphics, cx, cy, ring2R, withAlpha(GOLD_BRIGHT, Math.round(ringAlpha * 0.85F)));
            }

            // Radial burst particles
            int rayCount = 24;
            for (int i = 0; i < rayCount; i++) {
                double a = i * (Math.PI * 2.0 / rayCount) + 0.15;
                float dist = 6.0F + burstT * (28.0F + (i % 3) * 10.0F);
                int px = cx + (int) Math.round(Math.cos(a) * dist);
                int py = cy + (int) Math.round(Math.sin(a) * dist * 0.82);
                int color = (i % 3 == 0) ? withAlpha(STAR_CORE, ringAlpha)
                        : (i % 3 == 1 ? withAlpha(RUNE_BRIGHT, ringAlpha) : withAlpha(GOLD_BRIGHT, ringAlpha));
                graphics.fill(px - 1, py - 1, px + 1, py + 1, color);
            }
        }
    }

    private void drawGlowingEssenceSlot(GuiGraphicsExtractor graphics, ItemStack essence,
                                        int slotX, int slotY, float strength, float time, float phase) {
        int cx = slotX + 8;
        int cy = slotY + 8;
        float pulse = 0.7F + 0.3F * (float) Math.sin(time * 0.35F + phase);
        int haloAlpha = Math.round(115 * strength * pulse);
        fillSoftCircle(graphics, cx, cy, 11, withAlpha(RUNE_BRIGHT, haloAlpha));
        fillSoftCircle(graphics, cx, cy, 7, withAlpha(STAR_CORE, Math.round(90 * strength * pulse)));

        float scale = 1.0F + 0.14F * strength * pulse;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);
        graphics.item(essence, -8, -8);
        graphics.pose().popMatrix();
    }

    private void drawInfusionEnergyBeam(GuiGraphicsExtractor graphics,
                                        float x0, float y0, float x1, float y1,
                                        float reach, float intensity, float time,
                                        float phaseOffset, float arcOffset) {
        float midX = (x0 + x1) * 0.5F + arcOffset * 0.45F;
        float midY = (y0 + y1) * 0.5F - Math.abs(arcOffset) * 0.55F;
        int steps = 28;
        int maxStep = Math.max(1, Math.round(steps * reach));

        int outerAlpha = Math.round(135 * intensity);
        int coreAlpha = Math.round(235 * intensity);
        for (int i = 0; i <= maxStep; i++) {
            float t = i / (float) steps;
            float inv = 1.0F - t;
            float bx = inv * inv * x0 + 2.0F * inv * t * midX + t * t * x1;
            float by = inv * inv * y0 + 2.0F * inv * t * midY + t * t * y1;
            int px = Math.round(bx);
            int py = Math.round(by);
            graphics.fill(px - 2, py - 2, px + 3, py + 3, withAlpha(RUNE_PURPLE, outerAlpha));
            graphics.fill(px - 1, py - 1, px + 2, py + 2, withAlpha(RUNE_BRIGHT, Math.min(255, outerAlpha + 45)));
            graphics.fill(px, py, px + 1, py + 1, withAlpha(STAR_CORE, coreAlpha));
        }

        // Traveling high-energy pulses along each stream
        for (int p = 0; p < 3; p++) {
            float pulseT = ((time * 0.085F) + phaseOffset + p * 0.33F) % 1.0F;
            if (pulseT > reach) continue;
            float inv = 1.0F - pulseT;
            float bx = inv * inv * x0 + 2.0F * inv * pulseT * midX + pulseT * pulseT * x1;
            float by = inv * inv * y0 + 2.0F * inv * pulseT * midY + pulseT * pulseT * y1;
            int px = Math.round(bx);
            int py = Math.round(by);
            graphics.fill(px - 2, py - 2, px + 3, py + 3, withAlpha(STAR_CORE, coreAlpha));
        }
    }

    private void drawRotatingRuneCircle(GuiGraphicsExtractor graphics, int cx, int cy,
                                        float radius, float angleRad, float alphaFactor) {
        if (alphaFactor <= 0.02F) return;
        int outerColor = withAlpha(RUNE_BRIGHT, Math.round(160 * alphaFactor));
        int innerColor = withAlpha(GOLD_BRIGHT, Math.round(145 * alphaFactor));
        drawCircleRing(graphics, cx, cy, radius, outerColor);
        drawCircleRing(graphics, cx, cy, radius - 4.0F, innerColor);

        int glyphCount = 8;
        for (int i = 0; i < glyphCount; i++) {
            float a = angleRad + i * (float) (Math.PI * 2.0 / glyphCount);
            int gx = cx + Math.round((float) Math.cos(a) * (radius - 2.0F));
            int gy = cy + Math.round((float) Math.sin(a) * (radius - 2.0F));
            graphics.fill(gx - 1, gy - 1, gx + 2, gy + 2,
                    i % 2 == 0 ? withAlpha(STAR_CORE, Math.round(210 * alphaFactor)) : innerColor);
        }
    }

    // ------------------------------------------------------------------ SALVAGE LAYOUT & DESTRUCTION VFX

    private void drawSalvageLayout(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float time, float partialTick) {
        int inputX = leftPos + 124;
        int outputX = leftPos + 212;
        int slotY = topPos + 72;

        SalvageState salvage = CelestialAltarAnimationController.getSalvage();
        boolean salvageAnimating = salvage != null;

        ItemStack inputStack = menu.getSlot(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT).getItem();
        ItemStack outputStack = salvageAnimating
                ? ItemStack.EMPTY
                : menu.getSlot(CelestialAltarBlockEntity.SALVAGE_OUTPUT).getItem();

        boolean inputValid = !inputStack.isEmpty()
                && CraftingEquipmentManager.isEligibleForCelestialAltar(inputStack)
                && CraftingEquipmentManager.isTranscendent(inputStack);
        boolean ready = menu.canSalvage();

        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.celestial_altar.one_item"),
                inputX + 8, topPos + 45, 134, TEXT);
        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.celestial_altar.essence_output"),
                outputX + 8, topPos + 45, 120, RUNE_BRIGHT);

        int pipeY = slotY + 8;
        int pipeStartX = inputX + 21;
        int pipeEndX = outputX - 5;
        graphics.fill(pipeStartX, pipeY - 1, pipeEndX, pipeY + 2, OBSIDIAN_EDGE);
        if (ready || !outputStack.isEmpty() || salvageAnimating) {
            int flowColor = (!outputStack.isEmpty() || salvageAnimating) ? STAR_CYAN : RUNE_BRIGHT;
            graphics.fill(pipeStartX, pipeY, pipeEndX, pipeY + 1, flowColor);
            int pulseOffset = (int) ((time * 1.4F) % Math.max(1, (pipeEndX - pipeStartX - 4)));
            graphics.fill(pipeStartX + pulseOffset, pipeY - 1, pipeStartX + pulseOffset + 4, pipeY + 2, GOLD_BRIGHT);
        }
        int midX = (pipeStartX + pipeEndX) / 2;
        drawMiniStar(graphics, midX, pipeY, (ready || salvageAnimating) ? GOLD_BRIGHT : GOLD_DARK,
                !outputStack.isEmpty() ? STAR_CORE : ((ready || salvageAnimating) ? RUNE_BRIGHT : RUNE_DEEP));

        SlotState inputState = inputStack.isEmpty() ? SlotState.EMPTY
                : (inputValid ? SlotState.VALID_GOLD : SlotState.INVALID);
        drawCentralPedestal(graphics, inputX, slotY, inputState,
                isInside(mouseX, mouseY, inputX, slotY, 16, 16), time);

        drawSalvageOutputPedestal(graphics, outputX, slotY, !outputStack.isEmpty(),
                isInside(mouseX, mouseY, outputX, slotY, 16, 16), time);

        if (inputStack.isEmpty() && !salvageAnimating) drawEquipmentGlyph(graphics, inputX, slotY);
        if (outputStack.isEmpty()) drawEssenceGlyph(graphics, outputX, slotY);

        int confirmTextColor = isSalvageArmed() ? SALVAGE_WARN : MUTED;
        drawFittedCenteredText(graphics, Component.translatable("gui.simpleskills.celestial_altar.double_confirm"),
                leftPos + imageWidth / 2, topPos + 95, imageWidth - 26, confirmTextColor);
    }

    private void renderSalvageForeground(GuiGraphicsExtractor graphics, SalvageState salvage,
                                         float time, float partialTick) {
        float p = salvage.progress(partialTick);
        // Phases:
        // Phase 1: 0.00..0.18 (Magical Destabilization: levitate + vibrate + purple aura)
        // Phase 2: 0.18..0.54 (Progressive Cracking: glowing purple/white cracks + detaching fragments)
        // Phase 3: 0.54..0.80 (Disintegration: item breaks into fragments converging at center + burst)
        // Phase 4: 0.80..1.00 (Essence Formation: Transcendency Essence forms & moves to output slot)
        int inputCenterX = leftPos + 124 + 8;
        int inputCenterY = topPos + 72 + 8;
        int chamberCenterX = leftPos + 176;
        int chamberCenterY = topPos + 80;
        int outputCenterX = leftPos + 212 + 8;
        int outputCenterY = topPos + 72 + 8;

        // Mask the real output slot until the formed Essence arrives in Phase 4
        if (p < 0.94F) {
            int outX = leftPos + 212;
            int outY = topPos + 72;
            graphics.fill(outX, outY, outX + 16, outY + 16, OBSIDIAN_VOID);
            drawEssenceGlyph(graphics, outX, outY);
        }

        // Phases 1–3: Sacrificed equipment destabilizing, cracking, and disintegrating
        if (p < 0.78F) {
            float liftT = Mth.clamp(p / 0.18F, 0.0F, 1.0F);
            float crackT = Mth.clamp((p - 0.16F) / 0.42F, 0.0F, 1.0F);
            float disintegrateT = Mth.clamp((p - 0.52F) / 0.26F, 0.0F, 1.0F);

            float vibAmp = 0.65F + crackT * 1.45F + disintegrateT * 1.1F;
            float itemX = inputCenterX + (float) Math.cos(time * 2.7F) * vibAmp;
            float itemY = inputCenterY - liftT * 6.0F + (float) Math.sin(time * 3.3F) * vibAmp;
            float itemScale = (1.18F + 0.06F * (float) Math.sin(time * 0.65F)) * (1.0F - disintegrateT * 0.58F);

            // Destabilizing purple aura surrounding the equipment
            int auraR = Math.round(13 + crackT * 6.0F);
            fillSoftCircle(graphics, Math.round(itemX), Math.round(itemY), auraR,
                    withAlpha(RUNE_PURPLE, Math.round(70 + crackT * 95)));

            // Render preserved equipment copy while it still has structural cohesion
            if (disintegrateT < 0.85F) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(itemX, itemY);
                graphics.pose().rotate((float) Math.sin(time * 1.9F) * (0.04F + crackT * 0.08F));
                graphics.pose().scale(itemScale, itemScale);
                graphics.item(salvage.sacrificedEquipment, -8, -8);
                graphics.pose().popMatrix();

                // Progressive glowing purple & white cracks clipped strictly to the item's silhouette
                if (crackT > 0.01F) {
                    drawSalvageCracksOverlay(graphics, salvage, itemX, itemY, itemScale,
                            Math.min(1.0F, crackT + disintegrateT * 0.5F), time);
                }
            }

            // Rising destabilization particles in Phase 1 & 2
            for (int i = 0; i < 8; i++) {
                float cycle = ((time * 0.09F) + i * 0.125F) % 1.0F;
                int px = Math.round(itemX - 8 + (i * 5) % 17 + (float) Math.sin(time * 0.4F + i) * 2.0F);
                int py = Math.round(itemY + 6 - cycle * 18.0F);
                int alpha = Math.round((float) Math.sin(cycle * Math.PI) * 190.0F);
                graphics.fill(px, py, px + 2, py + 2,
                        i % 2 == 0 ? withAlpha(RUNE_BRIGHT, alpha) : withAlpha(STAR_CORE, alpha));
            }
        }

        // Phase 2 & 3: Detached glowing fragments separating and converging to the center
        if (p >= 0.28F && p <= 0.82F) {
            float fragPhase = (p - 0.28F) / 0.54F;
            float detachT = Mth.clamp(fragPhase / 0.42F, 0.0F, 1.0F);
            float convergeT = Mth.clamp((fragPhase - 0.35F) / 0.65F, 0.0F, 1.0F);
            float easeConverge = easeInOutCubic(convergeT);

            float originX = inputCenterX;
            float originY = inputCenterY - 6.0F;

            for (DisintegrationFragment frag : salvage.fragments) {
                float localX = originX + (frag.u() - 0.5F) * 18.0F + frag.burstDx() * detachT;
                float localY = originY + (frag.v() - 0.5F) * 18.0F + frag.burstDy() * detachT;

                // Spiral inward toward central transmutation point
                float swirlAngle = easeConverge * 3.8F + frag.spinSpeed() * 10.0F;
                float swirlRadius = (1.0F - easeConverge) * 6.0F;
                float fx = Mth.lerp(easeConverge, localX, chamberCenterX) + (float) Math.cos(swirlAngle) * swirlRadius;
                float fy = Mth.lerp(easeConverge, localY, chamberCenterY) + (float) Math.sin(swirlAngle) * swirlRadius;

                int fragAlpha = Math.round(235 * (1.0F - Math.max(0.0F, (convergeT - 0.82F) / 0.18F)));
                int coreColor = switch (frag.colorVariant()) {
                    case 0 -> withAlpha(STAR_CORE, fragAlpha);
                    case 1 -> withAlpha(RUNE_BRIGHT, fragAlpha);
                    default -> withAlpha(GOLD_BRIGHT, fragAlpha);
                };
                int ix = Math.round(fx);
                int iy = Math.round(fy);
                graphics.fill(ix - 1, iy - 1, ix + 2, iy + 2, withAlpha(RUNE_PURPLE, fragAlpha / 2));
                graphics.fill(ix, iy, ix + 1, iy + 1, coreColor);
            }
        }

        // Phase 3 -> 4 Convergence Burst at center
        if (p >= 0.72F && p <= 0.90F) {
            float burstT = (p - 0.72F) / 0.18F;
            float flash = (float) Math.sin(burstT * Math.PI);
            fillSoftCircle(graphics, chamberCenterX, chamberCenterY, Math.round(8 + flash * 16),
                    withAlpha(RUNE_BRIGHT, Math.round(flash * 190)));
            fillSoftCircle(graphics, chamberCenterX, chamberCenterY, Math.round(4 + flash * 10),
                    withAlpha(STAR_CORE, Math.round(flash * 235)));
            drawCircleRing(graphics, chamberCenterX, chamberCenterY, 5.0F + burstT * 22.0F,
                    withAlpha(STAR_CORE, Math.round((1.0F - burstT) * 210)));
        }

        // Phase 4: Transcendency Essence formation, amplified glint, orbiting particles, glide to output slot
        if (p >= 0.78F) {
            float formT = (p - 0.78F) / 0.22F;
            float moveT = easeInOutCubic(Mth.clamp((formT - 0.28F) / 0.72F, 0.0F, 1.0F));

            float ex = Mth.lerp(moveT, chamberCenterX, outputCenterX);
            float ey = Mth.lerp(moveT, chamberCenterY, outputCenterY)
                    - (float) Math.sin(moveT * Math.PI) * 7.0F;
            float essenceScale = Mth.lerp(moveT, 1.38F, 1.0F);

            // Prominent enchanted glint & starlight aura
            float glintPulse = 0.65F + 0.35F * (float) Math.sin(time * 0.55F);
            fillSoftCircle(graphics, Math.round(ex), Math.round(ey), 12,
                    withAlpha(RUNE_BRIGHT, Math.round(135 * glintPulse)));
            fillSoftCircle(graphics, Math.round(ex), Math.round(ey), 7,
                    withAlpha(GOLD_BRIGHT, Math.round(115 * glintPulse)));

            // Orbiting particles around the newly formed Essence
            for (int i = 0; i < 6; i++) {
                float a = time * 0.28F + i * (float) (Math.PI / 3.0);
                float r = 11.0F * (1.0F - moveT * 0.35F);
                int ox = Math.round(ex + (float) Math.cos(a) * r);
                int oy = Math.round(ey + (float) Math.sin(a) * r);
                graphics.fill(ox, oy, ox + 2, oy + 2,
                        i % 2 == 0 ? STAR_CORE : RUNE_BRIGHT);
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate(ex, ey);
            graphics.pose().scale(essenceScale, essenceScale);
            graphics.item(salvage.formedEssence, -8, -8);
            graphics.pose().popMatrix();
        }
    }

    private void drawSalvageCracksOverlay(GuiGraphicsExtractor graphics, SalvageState salvage,
                                          float centerX, float centerY, float scale,
                                          float crackProgress, float time) {
        ItemSilhouette sil = salvage.silhouette;
        float iconSize = 16.0F * scale;
        float iconLeft = centerX - iconSize * 0.5F;
        float iconTop = centerY - iconSize * 0.5F;
        float pulse = 0.65F + 0.35F * (float) Math.sin(time * 0.65F);

        int glowColor = withAlpha(RUNE_BRIGHT, Math.round(155 + 90 * pulse));
        int coreColor = withAlpha(STAR_CORE, Math.round(210 + 45 * pulse));

        for (FracturePath path : salvage.fractures) {
            float revealed = crackProgress * path.maxDistance();
            for (CrackSegment seg : path.segments()) {
                if (revealed <= seg.d1()) continue;
                float t = Math.min(1.0F, (revealed - seg.d1()) / Math.max(0.0001F, seg.d2() - seg.d1()));
                float eu = seg.x1() + (seg.x2() - seg.x1()) * t;
                float ev = seg.y1() + (seg.y2() - seg.y1()) * t;

                int steps = Math.max(1, Math.round(Math.max(Math.abs(eu - seg.x1()), Math.abs(ev - seg.y1())) * iconSize * 1.5F));
                for (int s = 0; s <= steps; s++) {
                    float f = s / (float) steps;
                    float u = seg.x1() + (eu - seg.x1()) * f;
                    float v = seg.y1() + (ev - seg.y1()) * f;
                    if (!sil.isOpaqueAt(u, v)) continue;

                    int px = Math.round(iconLeft + u * iconSize);
                    int py = Math.round(iconTop + v * iconSize);
                    graphics.fill(px - 1, py, px + 2, py + 1, glowColor);
                    graphics.fill(px, py - 1, px + 1, py + 2, glowColor);
                    graphics.fill(px, py, px + 1, py + 1, coreColor);
                }
            }
        }
    }

    // ------------------------------------------------------------------ ENHANCE LAYOUT & RUNE ENERGY STREAM

    private void drawEnhancementLayout(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       float time, float partialTick) {
        int equipmentX = leftPos + 36;
        int runeX = leftPos + 300;
        int slotY = topPos + 72;
        ItemStack equipment = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT).getItem();
        ItemStack runes = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_RUNES).getItem();
        boolean validEquipment = !equipment.isEmpty()
                && CraftingEquipmentManager.isEligibleForCelestialAltar(equipment);
        boolean validRunes = !runes.isEmpty()
                && runes.is(com.github.ob_yekt.simpleskills.runes.RuneContent.RUNE);

        int sidePanelY = topPos + 45;
        int sidePanelH = 70;
        graphics.fill(leftPos + 12, sidePanelY, leftPos + 76, sidePanelY + sidePanelH, 0x99120D20);
        drawOutline(graphics, leftPos + 12, sidePanelY, 64, sidePanelH, OBSIDIAN_EDGE);
        graphics.fill(leftPos + 276, sidePanelY, leftPos + 340, sidePanelY + sidePanelH, 0x99120D20);
        drawOutline(graphics, leftPos + 276, sidePanelY, 64, sidePanelH, OBSIDIAN_EDGE);

        drawFittedCenteredText(graphics,
                Component.translatable("gui.simpleskills.celestial_altar.enhance_equipment"),
                equipmentX + 8, topPos + 53, 56, GOLD);
        drawFittedCenteredText(graphics,
                Component.translatable("gui.simpleskills.celestial_altar.enhance_runes"),
                runeX + 8, topPos + 53, 56, RUNE_BRIGHT);

        SlotState equipmentState = equipment.isEmpty() ? SlotState.EMPTY
                : (validEquipment ? SlotState.VALID_GOLD : SlotState.INVALID);
        drawCentralPedestal(graphics, equipmentX, slotY, equipmentState,
                isInside(mouseX, mouseY, equipmentX, slotY, 16, 16), time);
        drawEssenceSlotFrame(graphics, runeX, slotY, essenceState(runes, validRunes),
                isInside(mouseX, mouseY, runeX, slotY, 16, 16), time);
        if (equipment.isEmpty()) drawEquipmentGlyph(graphics, equipmentX, slotY);
        if (runes.isEmpty()) drawEssenceGlyph(graphics, runeX, slotY);

        int conduitY = slotY + 8;
        graphics.fill(equipmentX + 22, conduitY, equipmentX + 25, conduitY + 1, OBSIDIAN_EDGE);
        graphics.fill(runeX - 8, conduitY, runeX - 5, conduitY + 1, OBSIDIAN_EDGE);
        if (validEquipment && validRunes) {
            int pulseX = equipmentX + 23 + (int) ((time * 1.2F) % Math.max(1, runeX - equipmentX - 48));
            graphics.fill(pulseX, conduitY - 1, pulseX + 3, conduitY + 2, RUNE_BRIGHT);
        }

        int listX = leftPos + ENHANCE_LIST_X;
        int listY = topPos + ENHANCE_LIST_Y;
        graphics.fill(listX - 2, listY - 2, listX + ENHANCE_LIST_W + 2,
                listY + ENHANCE_ROW_H * ENHANCE_VISIBLE_ROWS, 0xAA090613);
        drawOutline(graphics, listX - 2, listY - 2, ENHANCE_LIST_W + 4,
                ENHANCE_ROW_H * ENHANCE_VISIBLE_ROWS + 2, OBSIDIAN_EDGE);
        graphics.centeredText(font,
                Component.translatable("gui.simpleskills.celestial_altar.enhance_options"),
                listX + ENHANCE_LIST_W / 2, topPos + 40, GOLD);

        EnhanceState enhance = CelestialAltarAnimationController.getEnhance();
        float enhanceProgress = enhance != null ? enhance.progress(partialTick) : -1.0F;

        List<EnhancementRow> rows = getEnhancementRows();
        for (int i = 0; i < renderedEnhancementRows.length; i++) renderedEnhancementRows[i] = null;
        renderedEnhancementEquipment = validEquipment ? equipment.copy() : ItemStack.EMPTY;
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
                renderedEnhancementRows[visible] = row;
                int rowY = listY + visible * ENHANCE_ROW_H;
                boolean hovered = isInside(mouseX, mouseY, listX, rowY, ENHANCE_LIST_W, ENHANCE_ROW_H);
                boolean enabled = (row.actionable() || row.rerollable()) && validEquipment && validRunes;
                boolean isAnimatedTarget = enhance != null && row.id().equals(enhance.targetId);

                int baseBg = row.perk() ? 0xFF102631 : 0xFF14291E;
                int hoverBg = row.perk() ? 0xFF1A3B4B : 0xFF1C3A2B;
                int accent = row.perk() ? 0xFF70DDF2 : 0xFF78E39A;
                int edge = enabled ? (hovered ? (row.perk() ? 0xFFB7F2FF : 0xFFC0FFD0)
                        : (row.perk() ? 0xFF28758A : 0xFF347C50)) : 0xFF30263F;
                int background = enabled ? (hovered ? hoverBg : baseBg)
                        : (hovered ? 0xFF1B1528 : 0xFF120E1C);

                if (isAnimatedTarget && enhanceProgress >= 0.30F) {
                    float pulseT = (float) Math.sin(Math.min(1.0F, (enhanceProgress - 0.30F) / 0.70F) * Math.PI);
                    int pulseTint = switch (enhance.kind) {
                        case CelestialAltarVisualPayload.KIND_ENHANCE_STAT -> 0xFF2C5E3B;
                        case CelestialAltarVisualPayload.KIND_REROLL_STAT -> 0xFF43256B;
                        default -> 0xFF1E5269;
                    };
                    background = lerpColor(background, pulseTint, pulseT * 0.85F);
                    edge = lerpColor(edge, GOLD_BRIGHT, pulseT);
                }

                graphics.fill(listX, rowY, listX + ENHANCE_LIST_W, rowY + ENHANCE_ROW_H - 1, background);
                graphics.fill(listX, rowY, listX + ENHANCE_LIST_W, rowY + 1, edge);

                int iconX = listX + 3;
                int iconY = rowY;
                if (!row.perk()) {
                    drawRerollActionIcon(graphics, iconX, iconY,
                            row.rerollable() && validEquipment && validRunes, (int) time,
                            0xFF78E39A, 0xFF245B3B, 0xFF183626);
                    drawPlusActionIcon(graphics, listX + ENHANCE_LIST_W - 13, iconY,
                            row.actionable() && validEquipment && validRunes);
                } else if (row.rerollable()) {
                    drawRerollActionIcon(graphics, iconX, iconY, enabled, (int) time,
                            0xFF70DDF2, 0xFF1D6173, 0xFF143541);
                } else {
                    drawLockedPerkIcon(graphics, iconX, iconY);
                }

                int textMaxW = ENHANCE_LIST_W - (row.perk() ? 24 : 39);
                int textColor = enabled ? (hovered ? GOLD_BRIGHT : accent) : MUTED;
                if (isAnimatedTarget) {
                    drawAnimatedEnhancementRowText(graphics, enhance, row, listX + 19, rowY,
                            textMaxW, textColor, enhanceProgress, time);
                } else {
                    drawFittedText(graphics, row.label(), listX + 19, rowY, textMaxW, textColor);
                }
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
        updateEnhancementButtonStates(true);

        if (validEquipment && !validRunes) {
            drawFittedCenteredText(graphics,
                    Component.translatable("gui.simpleskills.celestial_altar.enhance_rune_hint"),
                    runeX + 8, topPos + 99, 48, MUTED);
        } else if (validEquipment && validRunes) {
            drawFittedCenteredText(graphics, Component.literal("×" + runes.getCount()),
                    runeX + 8, topPos + 99, 48, GOLD_BRIGHT);
        }
    }

    private void drawAnimatedEnhancementRowText(GuiGraphicsExtractor graphics, EnhanceState enhance,
                                                EnhancementRow row, int x, int y, int maxWidth,
                                                int defaultColor, float p, float time) {
        if (enhance.kind == CelestialAltarVisualPayload.KIND_ENHANCE_STAT) {
            // Smoothly interpolate the numerical value from oldValue -> newValue
            float numT = easeInOutCubic(Mth.clamp((p - 0.32F) / 0.52F, 0.0F, 1.0F));
            double shownValue = Mth.lerp(numT, enhance.oldValue, enhance.newValue);
            boolean reach = enhance.targetId.equals("attackReach");
            String unit = reach ? " blocks" : "%";
            boolean showMax = enhance.reachedMax && numT >= 0.85F;
            String label = Component.translatable(enhance.newTranslationKey).getString()
                    + "  +" + formatScreenPercent(shownValue) + unit
                    + " [" + formatScreenPercent(enhance.minValue) + " - "
                    + formatScreenPercent(enhance.maxValue) + unit + "]"
                    + (showMax ? "  MAX" : "");
            int color = lerpColor(defaultColor, GOLD_BRIGHT,
                    (float) Math.sin(Mth.clamp((p - 0.30F) / 0.70F, 0.0F, 1.0F) * Math.PI));
            drawFittedText(graphics, Component.literal(label), x, y, maxWidth, color);

            // Stronger golden sparkle burst when attribute reaches MAX
            if (enhance.reachedMax && p >= 0.45F) {
                float sparkleT = (p - 0.45F) / 0.55F;
                int alpha = Math.round((1.0F - sparkleT) * 240.0F);
                for (int i = 0; i < 10; i++) {
                    int sx = x + 6 + ((i * 23) % Math.max(12, maxWidth - 12));
                    int sy = y + 4 - (int) (Math.sin(sparkleT * Math.PI + i) * 5.0F);
                    graphics.fill(sx, sy, sx + 2, sy + 2,
                            i % 2 == 0 ? withAlpha(GOLD_BRIGHT, alpha) : withAlpha(STAR_CORE, alpha));
                }
            }
            return;
        }

        // Attribute Reroll or Perk Reroll: fade out old label in place, then fade in replacement at exact same position
        Component oldComp;
        Component newComp;
        int accentFlash = enhance.kind == CelestialAltarVisualPayload.KIND_REROLL_STAT
                ? RUNE_BRIGHT : STAR_CYAN;

        if (enhance.kind == CelestialAltarVisualPayload.KIND_REROLL_STAT) {
            boolean oldReach = "attackReach".equals(enhance.oldId);
            String oldUnit = oldReach ? " blocks" : "%";
            oldComp = Component.literal(Component.translatable(enhance.oldTranslationKey).getString()
                    + "  +" + formatScreenPercent(enhance.oldValue) + oldUnit);

            float newCountT = easeInOutCubic(Mth.clamp((p - 0.55F) / 0.40F, 0.0F, 1.0F));
            double shownNew = Mth.lerp(newCountT, enhance.minValue, enhance.newValue);
            boolean newReach = "attackReach".equals(enhance.targetId);
            String newUnit = newReach ? " blocks" : "%";
            String newStr = Component.translatable(enhance.newTranslationKey).getString()
                    + "  +" + formatScreenPercent(shownNew) + newUnit
                    + " [" + formatScreenPercent(enhance.minValue) + " - "
                    + formatScreenPercent(enhance.maxValue) + newUnit + "]"
                    + (enhance.reachedMax ? "  MAX" : "");
            newComp = Component.literal(newStr);
        } else {
            oldComp = getCompactPerkDisplayComponent(
                    new PerkRerollInfo(enhance.oldTranslationKey, enhance.oldAmplifier, false, true, 0));
            newComp = row.label();
        }

        if (p < 0.56F) {
            float fadeOut = 1.0F - Mth.clamp((p - 0.30F) / 0.26F, 0.0F, 1.0F);
            int alpha = Math.max(24, Math.round(255 * fadeOut));
            int fadedColor = withAlpha(lerpColor(defaultColor, accentFlash, 1.0F - fadeOut), alpha);
            drawFittedText(graphics, oldComp, x, y, maxWidth, fadedColor);
        } else {
            float fadeIn = Mth.clamp((p - 0.56F) / 0.34F, 0.0F, 1.0F);
            int alpha = Math.max(32, Math.round(255 * fadeIn));
            int revealColor = withAlpha(lerpColor(STAR_CORE, defaultColor, fadeIn), alpha);
            drawFittedText(graphics, newComp, x, y, maxWidth, revealColor);
        }
    }

    private void renderEnhanceForeground(GuiGraphicsExtractor graphics, EnhanceState enhance,
                                         float time, float partialTick) {
        float p = enhance.progress(partialTick);
        int runeSlotX = leftPos + 300;
        int runeSlotY = topPos + 72;
        float runeCenterX = runeSlotX + 8.0F;
        float runeCenterY = runeSlotY + 8.0F;

        int primaryColor;
        int secondaryColor;
        switch (enhance.kind) {
            case CelestialAltarVisualPayload.KIND_ENHANCE_STAT -> {
                primaryColor = 0xFF5CE68A;  // Emerald Green
                secondaryColor = GOLD_BRIGHT;
            }
            case CelestialAltarVisualPayload.KIND_REROLL_STAT -> {
                primaryColor = RUNE_BRIGHT; // Violet
                secondaryColor = STAR_CORE;   // White
            }
            default -> {
                primaryColor = STAR_CYAN;   // Cyan
                secondaryColor = RUNE_PURPLE; // Violet
            }
        }

        // 1. Preserve and glow the Rune in the right slot even if the stack reached 0
        float runeGlow = p < 0.35F
                ? (float) Math.sin((p / 0.35F) * Math.PI)
                : Math.max(0.0F, 1.0F - (p - 0.35F) / 0.65F);
        fillSoftCircle(graphics, Math.round(runeCenterX), Math.round(runeCenterY), 11,
                withAlpha(primaryColor, Math.round(135 * runeGlow)));
        fillSoftCircle(graphics, Math.round(runeCenterX), Math.round(runeCenterY), 7,
                withAlpha(secondaryColor, Math.round(115 * runeGlow)));

        if (menu.getSlot(CelestialAltarBlockEntity.ENHANCE_RUNES).getItem().isEmpty() && p < 0.75F) {
            graphics.item(enhance.runeSnapshot, runeSlotX, runeSlotY);
        }

        // Particles emerging from the Rune slot
        for (int i = 0; i < 6; i++) {
            float cycle = ((time * 0.12F) + i * 0.16F) % 1.0F;
            int px = Math.round(runeCenterX - 5 + (i * 3) % 11);
            int py = Math.round(runeCenterY + 4 - cycle * 15.0F);
            int alpha = Math.round((1.0F - p) * (float) Math.sin(cycle * Math.PI) * 210.0F);
            graphics.fill(px, py, px + 2, py + 2,
                    i % 2 == 0 ? withAlpha(primaryColor, alpha) : withAlpha(secondaryColor, alpha));
        }

        // 2. Locate the exact target row by stable targetId
        int targetVisibleRow = -1;
        for (int i = 0; i < renderedEnhancementRows.length; i++) {
            EnhancementRow r = renderedEnhancementRows[i];
            if (r != null && r.id().equals(enhance.targetId)) {
                targetVisibleRow = i;
                break;
            }
        }
        if (targetVisibleRow < 0) return;

        float targetX = leftPos + ENHANCE_LIST_X + ENHANCE_LIST_W - 6.0F;
        float targetY = topPos + ENHANCE_LIST_Y + targetVisibleRow * ENHANCE_ROW_H + ENHANCE_ROW_H * 0.5F;

        // 3. Visible energy stream from Rune slot to the exact target row
        if (p < 0.78F) {
            float travelReach = Mth.clamp((p - 0.06F) / 0.34F, 0.0F, 1.0F);
            float streamAlpha = p < 0.45F ? 1.0F : Mth.clamp((0.78F - p) / 0.33F, 0.0F, 1.0F);
            float ctrlX = (runeCenterX + targetX) * 0.5F;
            float ctrlY = Math.min(runeCenterY, targetY) - 10.0F;

            int steps = 26;
            int maxStep = Math.max(1, Math.round(steps * travelReach));
            for (int s = 0; s <= maxStep; s++) {
                float t = s / (float) steps;
                float inv = 1.0F - t;
                float bx = inv * inv * runeCenterX + 2.0F * inv * t * ctrlX + t * t * targetX;
                float by = inv * inv * runeCenterY + 2.0F * inv * t * ctrlY + t * t * targetY;
                int px = Math.round(bx);
                int py = Math.round(by);
                graphics.fill(px - 1, py - 1, px + 2, py + 2,
                        withAlpha(primaryColor, Math.round(170 * streamAlpha)));
                graphics.fill(px, py, px + 1, py + 1,
                        withAlpha(secondaryColor, Math.round(240 * streamAlpha)));
            }

            // Luminous head at the front of the stream
            float headT = travelReach;
            float inv = 1.0F - headT;
            int hx = Math.round(inv * inv * runeCenterX + 2.0F * inv * headT * ctrlX + headT * headT * targetX);
            int hy = Math.round(inv * inv * runeCenterY + 2.0F * inv * headT * ctrlY + headT * headT * targetY);
            fillSoftCircle(graphics, hx, hy, 4, withAlpha(secondaryColor, Math.round(220 * streamAlpha)));
        }

        // 4. Short luminous pulse border on the selected row upon impact
        if (p >= 0.30F) {
            float impactT = (p - 0.30F) / 0.70F;
            float pulse = (float) Math.sin(impactT * Math.PI);
            int rowX = leftPos + ENHANCE_LIST_X;
            int rowY = topPos + ENHANCE_LIST_Y + targetVisibleRow * ENHANCE_ROW_H;
            drawOutline(graphics, rowX, rowY, ENHANCE_LIST_W, ENHANCE_ROW_H - 1,
                    withAlpha(secondaryColor, Math.round(220 * pulse)));
        }
    }

    private record EnhancementRow(String id, Component label, boolean perk,
                                  boolean actionable, boolean rerollable) {}

    /** Shows a concise perk name/rank in the narrow Altar row; the full effect stays in the item tooltip. */
    private Component getCompactPerkDisplayComponent(PerkRerollInfo perk) {
        String display = CraftingEquipmentManager.getPerkDisplayComponent(perk).getString();
        int descriptionSeparator = display.indexOf(':');
        if (descriptionSeparator > 0) display = display.substring(0, descriptionSeparator).trim();
        return Component.literal(display);
    }

    private List<EnhancementRow> getEnhancementRows() {
        ItemStack equipment = menu.getSlot(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT).getItem();
        if (equipment.isEmpty() || !CraftingEquipmentManager.isEligibleForCelestialAltar(equipment)) {
            return java.util.List.of();
        }
        java.util.ArrayList<EnhancementRow> rows = new java.util.ArrayList<>();
        for (StatUpgradeInfo info : menu.getEnhancementStats()) {
            boolean reach = info.id().equals("attackReach");
            String unit = reach ? " blocks" : "%";
            String label = Component.translatable(info.translationKey()).getString()
                    + "  +" + formatScreenPercent(info.currentPercent()) + unit
                    + " [" + formatScreenPercent(info.minimumPercent()) + " - "
                    + formatScreenPercent(info.maximumPercent()) + unit + "]"
                    + (info.isMaxed() ? "  MAX" : "");
            boolean canEnhance = CraftingEquipmentManager.canEnhanceStat(equipment, info.id());
            boolean canReplace = CraftingEquipmentManager.canRerollStat(equipment, info.id());
            rows.add(new EnhancementRow(info.id(), Component.literal(label), false,
                    canEnhance, canReplace));
        }
        for (PerkRerollInfo perk : menu.getEnhancementPerks()) {
            rows.add(new EnhancementRow(perk.actionTargetId(), getCompactPerkDisplayComponent(perk),
                    true, perk.rerollable(), perk.rerollable()));
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
        float scale = Math.min(1.0F, (float) maxWidth / (float) rawWidth);
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
        drawRerollActionIcon(graphics, x, y, enabled, tick,
                RUNE_BRIGHT, RUNE_DEEP, 0xFF2B1B40);
    }

    private void drawRerollActionIcon(GuiGraphicsExtractor graphics, int x, int y,
                                      boolean enabled, int tick, int accent, int darkAccent, int background) {
        int color = enabled ? accent : DIM;
        int dark = enabled ? darkAccent : 0xFF30283B;
        graphics.fill(x, y, x + 11, y + 10, enabled ? background : 0xFF211A2B);
        drawOutline(graphics, x, y, 11, 10, enabled ? accent : 0xFF51445E);
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
        boolean blocking = CelestialAltarAnimationController.isBlockingRitualActive();
        boolean enabled = !blocking && (infusion ? menu.canInfuse() : menu.canSalvage());
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
        int trayX = leftPos + 8;
        int trayY = topPos + 139;
        int trayW = imageWidth - 16;
        int trayH = 80;
        graphics.fill(trayX, trayY, trayX + trayW, trayY + trayH, 0xCC0A0713);
        drawBevelBorder(graphics, trayX, trayY, trayW, trayH, OBSIDIAN_VOID, OBSIDIAN_EDGE);

        int leftWingX0 = trayX + 3;
        int leftWingX1 = leftPos + 92;
        int rightWingX0 = leftPos + 260;
        int rightWingX1 = trayX + trayW - 3;

        graphics.fill(leftWingX0, trayY + 3, leftWingX1, trayY + trayH - 3, 0x88120D20);
        drawOutline(graphics, leftWingX0, trayY + 3, leftWingX1 - leftWingX0, trayH - 6, 0xFF261C3B);
        graphics.fill(rightWingX0, trayY + 3, rightWingX1, trayY + trayH - 3, 0x88120D20);
        drawOutline(graphics, rightWingX0, trayY + 3, rightWingX1 - rightWingX0, trayH - 6, 0xFF261C3B);

        graphics.fill(leftWingX0 + 5, topPos + 197, leftWingX1 - 5, topPos + 198, GOLD_DARK);
        graphics.fill(leftPos + 94, topPos + 197, leftPos + 258, topPos + 198, GOLD_DARK);
        graphics.fill(rightWingX0 + 5, topPos + 197, rightWingX1 - 5, topPos + 198, GOLD_DARK);

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
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = 8 + row * 9 + column;
                int sx = leftPos + 95 + column * 18;
                int sy = topPos + 142 + row * 18;
                drawInventorySlotFrame(graphics, sx, sy, slotIndex);
            }
        }
        for (int column = 0; column < 9; column++) {
            int slotIndex = 35 + column;
            int sx = leftPos + 95 + column * 18;
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
        drawFooterFittedCenteredText(graphics, hint, leftPos + imageWidth / 2, fy + 4, fw - 12, MUTED);
    }

    private void drawFooterFittedCenteredText(GuiGraphicsExtractor graphics, Component text,
                                              int centerX, int y, int maxWidth, int color) {
        int rawWidth = font.width(text);
        if (rawWidth <= maxWidth || rawWidth <= 0) {
            graphics.centeredText(font, text, centerX, y, color);
            return;
        }
        float scale = (float) maxWidth / (float) rawWidth;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y + 4.0F);
        graphics.pose().scale(scale, scale);
        graphics.centeredText(font, text, 0, -4, color);
        graphics.pose().popMatrix();
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

    private void drawFittedCenteredText(GuiGraphicsExtractor graphics, Component text,
                                        int centerX, int y, int maxWidth, int color) {
        int rawWidth = font.width(text);
        if (rawWidth <= maxWidth || rawWidth <= 0) {
            graphics.centeredText(font, text, centerX, y, color);
            return;
        }
        float scale = Math.min(1.0F, (float) maxWidth / (float) rawWidth);
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

    private static void fillSoftCircle(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        if (radius <= 0) return;
        for (int dy = -radius; dy <= radius; dy++) {
            int span = (int) Math.round(Math.sqrt(radius * radius - dy * dy));
            graphics.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, color);
        }
    }

    private static void drawCircleRing(GuiGraphicsExtractor graphics, int cx, int cy, float radius, int color) {
        if (radius <= 0.5F) return;
        int segments = Math.max(24, Math.round(radius * 3.5F));
        for (int i = 0; i < segments; i++) {
            double a = i * (Math.PI * 2.0 / segments);
            int px = cx + (int) Math.round(Math.cos(a) * radius);
            int py = cy + (int) Math.round(Math.sin(a) * radius);
            graphics.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static float easeInOutCubic(float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        return clamped < 0.5F
                ? 4.0F * clamped * clamped * clamped
                : 1.0F - (float) Math.pow(-2.0F * clamped + 2.0F, 3.0) / 2.0F;
    }

    private static int lerpColor(int c1, int c2, float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * clamped);
        int r = Math.round(r1 + (r2 - r1) * clamped);
        int g = Math.round(g1 + (g2 - g1) * clamped);
        int b = Math.round(b1 + (b2 - b1) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int withAlpha(int argb, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (clamped << 24) | (argb & 0x00FFFFFF);
    }

    @Override
    public void onClose() {
        CelestialAltarAnimationController.onScreenClosed();
        super.onClose();
    }

    @Override
    public void removed() {
        CelestialAltarAnimationController.onScreenClosed();
        super.removed();
    }
}
