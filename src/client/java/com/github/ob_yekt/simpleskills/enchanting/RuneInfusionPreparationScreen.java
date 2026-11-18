package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.runes.RuneContent;
import com.github.ob_yekt.simpleskills.runes.RuneVisualStyles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Rune Infusion preparation: the original enchantment editor plus a separate cosmetic-glint tab. */
public final class RuneInfusionPreparationScreen extends Screen {
    private record Entry(String id, int current, int maximum, int cap, Set<String> conflicts, Component name) {}

    private static final int WIDTH = 390;
    private static final int HEIGHT = 270;
    private static final int ROW_H = 24;
    private static final int VISIBLE_ROWS = 6;
    private static final int MAX_SELECTED = 4;
    private static final int MAX_DIFFICULTY = RuneInfusionDifficulty.MAX_SCORE;
    private static final int STYLE_COUNT = RuneVisualStyles.MAX_STYLE + 1;

    private static final int GOLD = 0xFFE9CF7A;
    private static final int GOLD_DARK = 0xFF80612A;
    private static final int TEXT = 0xFFF3E8D2;
    private static final int MUTED = 0xFFB7A88D;
    private static final int DIM = 0xFF6F6454;
    private static final int RED = 0xFFE45A4A;
    private static final int GREEN = 0xFF67D68B;

    private final RuneInfusionCatalogResponsePayload catalog;
    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Entry> byId = new HashMap<>();
    private final Map<String, Integer> selected = new LinkedHashMap<>();
    private int scroll;
    private int left;
    private int top;
    private int activeTab;
    private int selectedStyle;
    private int currentStyle;
    private int runeLoreCount;
    private ItemStack stylePreviewStack = ItemStack.EMPTY;
    private boolean stylePending;
    private Button start;
    private Button applyStyle;
    private Button enchantmentsTab;
    private Button stylesTab;
    private Component status = Component.empty();

    public RuneInfusionPreparationScreen(RuneInfusionCatalogResponsePayload catalog) {
        super(RuneInfusionText.text("gui.simpleskills.rune_infusion.prepare"));
        this.catalog = catalog;
        this.runeLoreCount = catalog.runeLoreCount();
        this.currentStyle = catalog.currentStyle();
        this.selectedStyle = currentStyle;

        if (catalog.rows() != null && !catalog.rows().isBlank()) {
            for (String row : catalog.rows().split(";")) {
                String[] parts = row.split("\\|", -1);
                if (parts.length != 5 || parts[0].length() > 128) continue;
                try {
                    int current = Integer.parseInt(parts[1]);
                    int maximum = Integer.parseInt(parts[2]);
                    int cap = Integer.parseInt(parts[3]);
                    if (maximum < 1 || maximum < current || maximum > 255 || cap < 1) continue;
                    Set<String> conflicts = new HashSet<>();
                    if (!parts[4].isEmpty()) {
                        for (String c : parts[4].split(",")) if (!c.isBlank()) conflicts.add(c);
                    }
                    Entry entry = new Entry(parts[0], current, maximum, cap, conflicts,
                            enchantmentName(parts[0]));
                    entries.add(entry);
                    byId.put(entry.id(), entry);
                } catch (NumberFormatException ignored) { }
            }
        }
        entries.sort(Comparator.comparing(e -> e.name().getString().toLowerCase(java.util.Locale.ROOT)));
        for (Entry e : entries) if (e.current() > 0) selected.put(e.id(), e.current());
    }

    private static String translationKey(String id) {
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        return "enchantment." + ns + "." + path.replace('/', '.');
    }

    private static Component enchantmentName(String id) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier identifier = Identifier.tryParse(id);
        if (identifier != null && minecraft.level != null) {
            var enchantment = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(identifier);
            if (enchantment.isPresent()) return enchantment.get().value().description();
        }
        return Component.literal(translationKey(id));
    }

    private int listX0() { return left + 118; }
    private int listX1() { return left + WIDTH - 24; }
    private int listY0() { return top + 60; }
    private int maxScroll() { return Math.max(0, entries.size() - VISIBLE_ROWS); }
    private int minusX() { return listX1() - 69; }
    private int plusX() { return listX1() - 17; }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;

        enchantmentsTab = addRenderableWidget(Button.builder(
                RuneInfusionText.text("gui.simpleskills.rune_infusion.tab.enchantments"), b -> switchTab(0))
                .bounds(left + 20, top + 34, 170, 18).build());
        stylesTab = addRenderableWidget(Button.builder(
                RuneInfusionText.text("gui.simpleskills.rune_infusion.tab.styles"), b -> switchTab(1))
                .bounds(left + 198, top + 34, 170, 18).build());
        addRenderableWidget(Button.builder(RuneInfusionText.text("gui.simpleskills.rune_infusion.close"),
                b -> onClose()).bounds(left + WIDTH - 106, top + 240, 90, 20).build());
        start = addRenderableWidget(Button.builder(RuneInfusionText.text("gui.simpleskills.rune_infusion.start"),
                b -> start()).bounds(left + WIDTH - 206, top + 240, 96, 20).build());
        applyStyle = addRenderableWidget(Button.builder(
                RuneInfusionText.text("gui.simpleskills.rune_infusion.apply_style"), b -> applyStyle())
                .bounds(left + WIDTH - 206, top + 240, 96, 20).build());
        scroll = Math.min(scroll, maxScroll());
        refresh();
        refreshTabVisibility();
    }

    private void switchTab(int tab) {
        activeTab = tab;
        status = Component.empty();
        if (activeTab == 1) rebuildStylePreview();
        refreshTabVisibility();
    }

    private void refreshTabVisibility() {
        if (enchantmentsTab != null) enchantmentsTab.active = activeTab != 0;
        if (stylesTab != null) stylesTab.active = activeTab != 1;
        if (start != null) start.visible = activeTab == 0;
        if (applyStyle != null) applyStyle.visible = activeTab == 1;
        updateStyleButton();
    }

    private ItemStack selectedEquipment() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || catalog.inventorySlot() < 0
                || catalog.inventorySlot() >= mc.player.getInventory().getNonEquipmentItems().size()) return ItemStack.EMPTY;
        return mc.player.getInventory().getItem(catalog.inventorySlot());
    }

    /**
     * Builds a client-only display copy. The selected style is visible immediately,
     * even when the source stack currently has no glint. The inventory stack is never
     * changed here; only the server-side Apply Style handler can persist a style or
     * consume Rune Lore Ore.
     */
    private void rebuildStylePreview() {
        ItemStack equipment = selectedEquipment();
        if (equipment.isEmpty()) {
            stylePreviewStack = ItemStack.EMPTY;
            return;
        }

        stylePreviewStack = equipment.copy();
        RuneVisualStyles.setStyle(stylePreviewStack, selectedStyle);
        stylePreviewStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    private ItemStack equipmentPreviewStack() {
        if (activeTab != 1) return selectedEquipment();
        if (stylePreviewStack.isEmpty()) rebuildStylePreview();
        return stylePreviewStack;
    }

    private boolean itemHasEnchantments() {
        ItemStack stack = selectedEquipment();
        return !stack.isEmpty() && !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
    }

    private void updateStyleButton() {
        if (applyStyle == null) return;
        boolean hasEnchantments = itemHasEnchantments();
        boolean isDifferent = selectedStyle != currentStyle;
        boolean hasCurrency = selectedStyle == RuneVisualStyles.VANILLA || runeLoreCount >= 1;
        applyStyle.active = !stylePending && hasEnchantments && isDifferent && hasCurrency;
    }

    private boolean isBlocked(Entry e) {
        for (String id : selected.keySet()) if (!id.equals(e.id()) && e.conflicts().contains(id)) return true;
        return false;
    }

    private int changedCount() {
        int n = 0;
        for (Map.Entry<String, Integer> s : selected.entrySet()) {
            Entry e = byId.get(s.getKey());
            if (e != null && s.getValue() > e.current()) n++;
        }
        return n;
    }

    private int difficulty() {
        if (changedCount() == 0) return 0;
        double raw = 0.0;
        for (Map.Entry<String, Integer> s : selected.entrySet()) {
            Entry e = byId.get(s.getKey());
            if (e != null) raw += RuneInfusionDifficulty.contribution(s.getValue(), e.cap(), catalog.unlockedLevel());
        }
        return RuneInfusionDifficulty.toScore(raw);
    }

    private void step(Entry e, int delta) {
        Integer level = selected.get(e.id());
        status = Component.empty();
        if (level == null) {
            if (delta <= 0) { refresh(); return; }
            if (isBlocked(e)) { status = RuneInfusionText.text("gui.simpleskills.rune_infusion.conflict"); return; }
            if (selected.size() >= MAX_SELECTED) { status = RuneInfusionText.text("gui.simpleskills.rune_infusion.max_selected"); return; }
            selected.put(e.id(), e.current() > 0 ? e.current() : 1);
        } else {
            int min = Math.max(1, e.current());
            int next = level + delta;
            if (next < min) {
                if (e.current() == 0) selected.remove(e.id());
            } else selected.put(e.id(), Math.min(e.maximum(), next));
        }
        refresh();
    }

    private void toggle(Entry e) {
        if (selected.containsKey(e.id())) {
            selected.remove(e.id());
            status = Component.empty();
            refresh();
        } else step(e, 1);
    }

    private void refresh() {
        if (start == null) return;
        int cost = changedCount();
        boolean enough = catalog.runeCount() >= cost;
        boolean tooMany = selected.size() > MAX_SELECTED;
        start.active = cost > 0 && enough && !tooMany;
        if (tooMany) status = RuneInfusionText.text("gui.simpleskills.rune_infusion.max_selected");
        else if (cost > 0 && !enough) status = RuneInfusionText.text("gui.simpleskills.rune_infusion.not_enough");
        else if (selected.size() >= MAX_SELECTED && status.getString().isEmpty())
            status = RuneInfusionText.text("gui.simpleskills.rune_infusion.max_selected");
        updateStyleButton();
    }

    private void start() {
        int cost = changedCount();
        if (cost == 0 || catalog.runeCount() < cost || selected.size() > MAX_SELECTED) return;
        List<String> ids = new ArrayList<>(selected.keySet());
        List<Integer> levels = new ArrayList<>();
        for (String id : ids) levels.add(selected.get(id));
        ClientPlayNetworking.send(new RuneInfusionStartPayload(catalog.inventorySlot(), ids, levels));
        Minecraft.getInstance().gui.setScreen(null);
    }

    private void applyStyle() {
        if (!applyStyle.active || stylePending) return;
        stylePending = true;
        status = Component.empty();
        updateStyleButton();
        ClientPlayNetworking.send(new RuneInfusionStylePayload(catalog.inventorySlot(), selectedStyle));
    }

    public static void acceptStyleResult(RuneInfusionResultPayload payload) {
        if (Minecraft.getInstance().gui.screen() instanceof RuneInfusionPreparationScreen screen) {
            screen.onStyleResult(payload);
        }
    }

    private void onStyleResult(RuneInfusionResultPayload payload) {
        stylePending = false;
        switch (payload.resultCode()) {
            case RuneInfusionResultPayload.STYLE_APPLIED -> {
                currentStyle = payload.grade();
                selectedStyle = currentStyle;
                runeLoreCount = payload.runeCost();
                rebuildStylePreview();
                status = RuneInfusionText.text("gui.simpleskills.rune_infusion.style_applied");
            }
            case RuneInfusionResultPayload.STYLE_NOT_ENOUGH_LORE -> {
                runeLoreCount = payload.runeCost();
                status = RuneInfusionText.text("gui.simpleskills.rune_infusion.style_not_enough");
            }
            case RuneInfusionResultPayload.STYLE_INVALID ->
                    status = RuneInfusionText.text("gui.simpleskills.rune_infusion.style_invalid");
            case RuneInfusionResultPayload.STYLE_BUSY ->
                    status = RuneInfusionText.text("message.simpleskills.rune_infusion.busy");
            default -> status = RuneInfusionText.text("gui.simpleskills.rune_infusion.invalid_selection");
        }
        updateStyleButton();
    }

    private int rowAt(double mx, double my) {
        if (activeTab != 0 || mx < listX0() || mx >= listX1()
                || my < listY0() || my >= listY0() + VISIBLE_ROWS * ROW_H) return -1;
        int index = scroll + (int) ((my - listY0()) / ROW_H);
        return index < entries.size() ? index : -1;
    }

    private int styleAt(double mx, double my) {
        if (activeTab != 1 || mx < listX0() || mx >= listX1()) return -1;
        int colWidth = (listX1() - listX0() - 6) / 2;
        int row = (int) ((my - listY0()) / 36);
        int col = mx < listX0() + colWidth ? 0 : 1;
        if (row < 0 || row >= 4) return -1;
        int idx = row * 2 + col;
        int cardX = listX0() + col * (colWidth + 6);
        if (mx < cardX || mx >= cardX + colWidth || my < listY0() + row * 36 || my >= listY0() + row * 36 + 32)
            return -1;
        return idx < STYLE_COUNT ? idx : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (activeTab == 1) {
            int style = styleAt(mx, my);
            if (style >= 0) {
                selectedStyle = style;
                status = Component.empty();
                rebuildStylePreview();
                updateStyleButton();
                return true;
            }
        }
        int index = rowAt(mx, my);
        if (index >= 0) {
            Entry e = entries.get(index);
            if (mx >= minusX() && mx < minusX() + 14) step(e, -1);
            else if (mx >= plusX() && mx < plusX() + 14) step(e, 1);
            else toggle(e);
            return true;
        }
        int trackX = listX1() + 4;
        if (activeTab == 0 && maxScroll() > 0 && mx >= trackX && mx < trackX + 6
                && my >= listY0() && my < listY0() + VISIBLE_ROWS * ROW_H) {
            double ratio = (my - listY0()) / (VISIBLE_ROWS * ROW_H);
            scroll = Math.max(0, Math.min(maxScroll(), (int) Math.round(ratio * maxScroll())));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (activeTab == 0 && maxScroll() > 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static Component level(int n) {
        if (n <= 0) return Component.literal("-");
        return n <= 10 ? Component.translatable("enchantment.level." + n) : Component.literal(String.valueOf(n));
    }

    private static int difficultyColor(int score) {
        float t = Math.max(0f, Math.min(1f, (score - 1) / (float) (MAX_DIFFICULTY - 1)));
        int r = Math.round(0x67 + (0xE4 - 0x67) * t);
        int g = Math.round(0xD6 + (0x5A - 0xD6) * t);
        int b = Math.round(0x8B + (0x4A - 0x8B) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int difficultyBand(int score) {
        if (score <= 4) return 0;
        if (score <= 10) return 1;
        if (score <= 20) return 2;
        if (score <= 30) return 3;
        return 4;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        Minecraft mc = Minecraft.getInstance();
        g.fill(left - 3, top - 3, left + WIDTH + 3, top + HEIGHT + 3, GOLD_DARK);
        g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, GOLD);
        g.fill(left, top, left + WIDTH, top + HEIGHT, 0xEE17120E);
        g.fill(left + 6, top + 6, left + WIDTH - 6, top + HEIGHT - 6, 0xFF292018);
        g.centeredText(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.prepare"), width / 2, top + 10, GOLD);
        g.centeredText(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.hint"), width / 2, top + 23, MUTED);

        drawLeftPanel(g, mc);
        if (activeTab == 0) {
            drawEnchantmentList(g, mouseX, mouseY);
            g.text(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.selected", selected.size()),
                    listX0(), top + 208, selected.size() > MAX_SELECTED ? RED : MUTED, false);
            if (entries.isEmpty()) g.centeredText(font,
                    RuneInfusionText.text("gui.simpleskills.rune_infusion.no_enchantments"),
                    listX0() + (listX1() - listX0()) / 2, top + 120, RED);
        } else {
            drawStyleList(g, mouseX, mouseY);
            if (!itemHasEnchantments()) {
                g.text(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.style_requires_enchantments"),
                        listX0(), top + 208, RED, false);
            } else if (!status.getString().isEmpty()) {
                g.text(font, status, listX0(), top + 208, status.getString().contains("updated") || status.getString().contains("atualizado") ? GREEN : RED, false);
            } else if (selectedStyle == RuneVisualStyles.VANILLA) {
                g.text(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.style_free"), listX0(), top + 208, GOLD, false);
            } else if (selectedStyle == currentStyle) {
                g.text(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.style_unchanged"), listX0(), top + 208, MUTED, false);
            } else {
                g.text(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.style_cost"), listX0(), top + 208, MUTED, false);
            }
        }
        if (!status.getString().isEmpty() && activeTab == 0) g.text(font, status, listX0(), top + 220, RED, false);
    }

    private void drawLeftPanel(GuiGraphicsExtractor g, Minecraft mc) {
        int bx = left + 18;
        int by = top + 60;
        g.fill(bx - 2, by - 2, bx + 90, by + 90, GOLD_DARK);
        g.fill(bx, by, bx + 88, by + 88, 0xFF17130F);
        g.fill(bx + 2, by + 2, bx + 86, by + 86, 0xFF393027);
        ItemStack item = selectedEquipment();
        if (!item.isEmpty()) {
            // In the Styles tab, render the chosen style on a copy of the equipment.
            // This gives an immediate, full-size preview without modifying inventory data.
            ItemStack renderedItem = equipmentPreviewStack();
            g.pose().pushMatrix();
            g.pose().translate(bx + 12, by + 12);
            g.pose().scale(4.0f, 4.0f);
            g.item(renderedItem, 0, 0);
            g.pose().popMatrix();
            String name = font.plainSubstrByWidth(item.getHoverName().getString(), 96);
            g.centeredText(font, Component.literal(name), bx + 44, top + 154, TEXT);
        } else {
            g.centeredText(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.no_item"), bx + 44, by + 40, RED);
        }

        if (activeTab == 0) {
            int cost = changedCount();
            boolean enough = catalog.runeCount() >= cost;
            g.item(new ItemStack(RuneContent.RUNE), bx, top + 174);
            g.text(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.runes", catalog.runeCount()), bx + 20, top + 178, enough ? TEXT : RED, false);
            g.text(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.cost", cost), bx, top + 194, enough ? GOLD : RED, false);
            int score = difficulty();
            int color = score == 0 ? MUTED : difficultyColor(score);
            Component label = score == 0 ? Component.literal("-") : Component.literal(score + " ").append(
                    RuneInfusionText.text("gui.simpleskills.rune_infusion.difficulty." + difficultyBand(score)));
            g.text(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.difficulty", label), bx, top + 208, color, false);
            g.fill(bx, top + 224, bx + 88, top + 230, 0xFF080706);
            g.fill(bx + 1, top + 225, bx + 87, top + 229, 0xFF39312A);
            int filled = 86 * score / MAX_DIFFICULTY;
            if (filled > 0) g.fill(bx + 1, top + 225, bx + 1 + filled, top + 229, color);
        } else {
            g.item(new ItemStack(RuneContent.RUNE_LORE_ORE), bx, top + 174);
            g.text(font, RuneInfusionText.text("gui.simpleskills.rune_infusion.lore_ore", runeLoreCount), bx + 20, top + 178,
                    runeLoreCount > 0 ? TEXT : RED, false);
            // Display the selected style rather than the stored one: this is the live preview.
            g.text(font, RuneVisualStylesClientText.styleLabel(selectedStyle), bx, top + 198,
                    RuneVisualStyles.color(selectedStyle), false);
            Component costLabel = selectedStyle == RuneVisualStyles.VANILLA
                    ? RuneInfusionText.text("gui.simpleskills.rune_infusion.style_free")
                    : selectedStyle == currentStyle
                    ? RuneInfusionText.text("gui.simpleskills.rune_infusion.style_unchanged")
                    : RuneInfusionText.text("gui.simpleskills.rune_infusion.style_cost");
            g.text(font, costLabel, bx, top + 221, MUTED, false);
        }
    }

    private void drawEnchantmentList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x0 = listX0(), x1 = listX1();
        int nameW = minusX() - (x0 + 22) - 6;
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scroll + i;
            if (index >= entries.size()) break;
            Entry e = entries.get(index);
            int y = listY0() + i * ROW_H;
            Integer lvl = selected.get(e.id());
            boolean sel = lvl != null, existing = e.current() > 0, removing = existing && !sel, blocked = !sel && isBlocked(e);
            boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + ROW_H - 2;
            int bg = sel ? 0xFF3B2E1C : blocked ? 0xFF1B1713 : hover ? 0xFF33281C : 0xFF241C14;
            g.fill(x0, y, x1, y + ROW_H - 2, bg);
            if (sel) g.fill(x0, y, x0 + 2, y + ROW_H - 2, GOLD);
            if (removing) g.fill(x0, y, x0 + 2, y + ROW_H - 2, RED);
            g.fill(x0 + 7, y + 6, x0 + 17, y + 16, sel ? GOLD : GOLD_DARK);
            g.fill(x0 + 8, y + 7, x0 + 16, y + 15, sel ? 0xFFB8892B : 0xFF17130F);
            if (sel) g.fill(x0 + 10, y + 9, x0 + 14, y + 13, 0xFFFFF4C2);
            String name = e.name().getString(), shown = font.plainSubstrByWidth(name, nameW);
            if (shown.length() < name.length()) shown += "..";
            g.text(font, Component.literal(shown), x0 + 22, y + 8, sel ? GOLD : removing ? RED : blocked ? DIM : TEXT, false);
            int stepColor = blocked ? 0xFF1B1713 : 0xFF3A2D1C;
            g.fill(minusX(), y + 5, minusX() + 14, y + 17, stepColor);
            g.fill(plusX(), y + 5, plusX() + 14, y + 17, stepColor);
            g.text(font, Component.literal("-"), minusX() + 5, y + 8, blocked ? DIM : TEXT, false);
            g.text(font, Component.literal("+"), plusX() + 4, y + 8, blocked ? DIM : TEXT, false);
            Component levels; int color;
            if (sel && existing && lvl == e.current()) { levels = level(e.current()); color = GOLD; }
            else if (removing) { levels = Component.empty().append(level(e.current())).append(" > -"); color = RED; }
            else { int target = sel ? lvl : 1; levels = Component.empty().append(level(e.current())).append(" > ").append(level(target)); color = sel ? GREEN : blocked ? DIM : MUTED; }
            int lx = (minusX() + 14 + plusX()) / 2 - font.width(levels) / 2;
            g.text(font, levels, lx, y + 8, color, false);
        }
        if (maxScroll() > 0) {
            int trackX = x1 + 4, trackH = VISIBLE_ROWS * ROW_H;
            g.fill(trackX, listY0(), trackX + 6, listY0() + trackH, 0xFF17130F);
            int thumbH = Math.max(12, trackH * VISIBLE_ROWS / entries.size());
            int thumbY = listY0() + (trackH - thumbH) * scroll / maxScroll();
            g.fill(trackX, thumbY, trackX + 6, thumbY + thumbH, GOLD_DARK);
        }
    }

    private void drawStyleList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int colWidth = (listX1() - listX0() - 6) / 2;
        ItemStack equipment = selectedEquipment();
        for (int style = 0; style < STYLE_COUNT; style++) {
            int col = style % 2, row = style / 2;
            int x = listX0() + col * (colWidth + 6);
            int y = listY0() + row * 36;
            boolean selectedCard = selectedStyle == style;
            boolean currentCard = currentStyle == style;
            boolean hover = mouseX >= x && mouseX < x + colWidth && mouseY >= y && mouseY < y + 32;
            g.fill(x, y, x + colWidth, y + 32, selectedCard ? 0xFF3D3020 : hover ? 0xFF33281C : 0xFF241C14);
            if (selectedCard) g.fill(x, y, x + 2, y + 32, GOLD);
            if (currentCard) g.fill(x + colWidth - 3, y, x + colWidth, y + 32, GREEN);
            int swatch = RuneVisualStyles.color(style);
            g.fill(x + 7, y + 7, x + 25, y + 25, 0xFF11100E);
            for (int k = 0; k < 4; k++) g.fill(x + 9 + k * 4, y + 10 + (k % 2) * 4, x + 12 + k * 4, y + 14 + (k % 2) * 4, style == 0 ? GOLD : swatch);
            Component label = RuneInfusionText.text(RuneVisualStyles.translationKey(style));
            g.text(font, label, x + 32, y + 7, selectedCard ? GOLD : TEXT, false);
            if (currentCard) g.text(font, Component.literal("✓"), x + colWidth - 14, y + 7, GREEN, false);
            // The preview uses a copy, so the selected style is only a visual preview.
            if (!equipment.isEmpty()) {
                ItemStack preview = equipment.copy();
                RuneVisualStyles.setStyle(preview, style);
                preview.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                g.pose().pushMatrix();
                g.pose().translate(x + colWidth - 24, y + 10);
                g.pose().scale(0.9f, 0.9f);
                g.item(preview, 0, 0);
                g.pose().popMatrix();
            }
        }
    }

    private static final class RuneVisualStylesClientText {
        private static Component styleLabel(int style) { return RuneInfusionText.text(RuneVisualStyles.translationKey(style)); }
    }

    @Override public boolean isPauseScreen() { return false; }
}
