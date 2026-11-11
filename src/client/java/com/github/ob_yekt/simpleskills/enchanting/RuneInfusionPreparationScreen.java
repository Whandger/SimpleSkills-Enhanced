package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.runes.RuneContent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rune Infusion preparation UI. The server revalidates every selected enchantment and level and
 * derives the Rune cost and difficulty itself; this screen is display + request only.
 *
 * Enchantments already on the item start selected (kept as they are). They count toward the
 * 4-enchantment limit and the difficulty. Clicking a selected row (left OR right button) drops it:
 * it will be removed from the item when the infusion succeeds.
 */
public final class RuneInfusionPreparationScreen extends Screen {
    private record Entry(String id, int current, int maximum, int cap, Set<String> conflicts, Component name) {}

    private static final int WIDTH = 360;
    private static final int HEIGHT = 238;
    private static final int ROW_H = 24;
    private static final int VISIBLE_ROWS = 6;
    private static final int MAX_SELECTED = 4;
    private static final int MAX_DIFFICULTY = RuneInfusionDifficulty.MAX_SCORE;

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
    private Button start;
    private Component status = Component.empty();

    public RuneInfusionPreparationScreen(RuneInfusionCatalogResponsePayload catalog) {
        super(Component.translatable("gui.simpleskills.rune_infusion.prepare"));
        this.catalog = catalog;

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
                            Component.translatable(translationKey(parts[0])));
                    entries.add(entry);
                    byId.put(entry.id(), entry);
                } catch (NumberFormatException ignored) { }
            }
        }
        entries.sort(Comparator.comparing(e -> e.name().getString().toLowerCase(java.util.Locale.ROOT)));

        // Enchantments already on the item start selected at their current level ("keep").
        for (Entry e : entries) {
            if (e.current() > 0) selected.put(e.id(), e.current());
        }
    }

    private static String translationKey(String id) {
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        return "enchantment." + ns + "." + path.replace('/', '.');
    }

    // ------------------------------------------------------------------ layout

    private int listX0() { return left + 118; }
    private int listX1() { return left + WIDTH - 24; }
    private int listY0() { return top + 36; }
    private int maxScroll() { return Math.max(0, entries.size() - VISIBLE_ROWS); }
    private int minusX() { return listX1() - 69; }
    private int plusX() { return listX1() - 17; }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.simpleskills.rune_infusion.close"),
                b -> onClose()).bounds(left + WIDTH - 106, top + 210, 90, 20).build());
        start = addRenderableWidget(Button.builder(Component.translatable("gui.simpleskills.rune_infusion.start"),
                b -> start()).bounds(left + WIDTH - 206, top + 210, 96, 20).build());
        scroll = Math.min(scroll, maxScroll());
        refresh();
    }

    // ------------------------------------------------------------------ selection logic

    private boolean isBlocked(Entry e) {
        for (String id : selected.keySet()) {
            if (!id.equals(e.id()) && e.conflicts().contains(id)) return true;
        }
        return false;
    }

    /** Number of enchantments that are added or raised (each costs one Rune). */
    private int changedCount() {
        int n = 0;
        for (Map.Entry<String, Integer> s : selected.entrySet()) {
            Entry e = byId.get(s.getKey());
            if (e != null && s.getValue() > e.current()) n++;
        }
        return n;
    }

    /** Same formula as the server: every selected enchantment (existing ones included) counts. */
    private int difficulty() {
        if (changedCount() == 0) return 0;
        double raw = 0.0;
        for (Map.Entry<String, Integer> s : selected.entrySet()) {
            Entry e = byId.get(s.getKey());
            if (e == null) continue;
            raw += RuneInfusionDifficulty.contribution(s.getValue(), e.cap(), catalog.unlockedLevel());
        }
        return RuneInfusionDifficulty.toScore(raw);
    }

    private void step(Entry e, int delta) {
        Integer level = selected.get(e.id());
        status = Component.empty();

        if (level == null) {
            if (delta <= 0) {
                refresh();
                return;
            }
            if (isBlocked(e)) {
                status = Component.translatable("gui.simpleskills.rune_infusion.conflict");
                return;
            }
            if (selected.size() >= MAX_SELECTED) {
                status = Component.translatable("gui.simpleskills.rune_infusion.max_selected");
                return;
            }
            // Re-adding an existing enchantment restores it as-is; a new one starts at level I.
            selected.put(e.id(), e.current() > 0 ? e.current() : 1);
        } else {
            int min = Math.max(1, e.current());
            int next = level + delta;
            if (next < min) {
                // A new enchantment is dropped when stepped below I; an existing one stops at its
                // current level (click the row to remove it).
                if (e.current() == 0) selected.remove(e.id());
            } else {
                selected.put(e.id(), Math.min(e.maximum(), next));
            }
        }
        refresh();
    }

    private void toggle(Entry e) {
        if (selected.containsKey(e.id())) {
            selected.remove(e.id());
            status = Component.empty();
            refresh();
        } else {
            step(e, 1);
        }
    }

    private void refresh() {
        if (start == null) return;
        int cost = changedCount();
        boolean enough = catalog.runeCount() >= cost;
        boolean tooMany = selected.size() > MAX_SELECTED;
        start.active = cost > 0 && enough && !tooMany;
        if (tooMany) {
            status = Component.translatable("gui.simpleskills.rune_infusion.max_selected");
        } else if (cost > 0 && !enough) {
            status = Component.translatable("gui.simpleskills.rune_infusion.not_enough");
        } else if (selected.size() >= MAX_SELECTED && status.getString().isEmpty()) {
            // 4/4 already (e.g. the item has 4 enchantments): explain why nothing else can be added.
            status = Component.translatable("gui.simpleskills.rune_infusion.max_selected");
        }
    }

    private void start() {
        int cost = changedCount();
        if (cost == 0 || catalog.runeCount() < cost || selected.size() > MAX_SELECTED) return;
        // The request is the FINAL set; existing enchantments left out are removed by the server.
        List<String> ids = new ArrayList<>(selected.keySet());
        List<Integer> levels = new ArrayList<>();
        for (String id : ids) levels.add(selected.get(id));
        ClientPlayNetworking.send(new RuneInfusionStartPayload(catalog.inventorySlot(), ids, levels));
        Minecraft.getInstance().gui.setScreen(null);
    }

    // ------------------------------------------------------------------ input

    private int rowAt(double mx, double my) {
        if (mx < listX0() || mx >= listX1() || my < listY0() || my >= listY0() + VISIBLE_ROWS * ROW_H) return -1;
        int index = scroll + (int) ((my - listY0()) / ROW_H);
        return index < entries.size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        int index = rowAt(mx, my);
        if (index >= 0) {
            // Left and right buttons behave the same: [-] / [+] change the level,
            // anywhere else on the row toggles it (clicking a selected row removes it).
            Entry e = entries.get(index);
            if (mx >= minusX() && mx < minusX() + 14) {
                step(e, -1);
            } else if (mx >= plusX() && mx < plusX() + 14) {
                step(e, 1);
            } else {
                toggle(e);
            }
            return true;
        }

        // Scrollbar track.
        int trackX = listX1() + 4;
        if (maxScroll() > 0 && mx >= trackX && mx < trackX + 6
                && my >= listY0() && my < listY0() + VISIBLE_ROWS * ROW_H) {
            double ratio = (my - listY0()) / (VISIBLE_ROWS * ROW_H);
            scroll = Math.max(0, Math.min(maxScroll(), (int) Math.round(ratio * maxScroll())));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------------ render

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

        g.centeredText(font, Component.translatable("gui.simpleskills.rune_infusion.prepare"),
                width / 2, top + 10, GOLD);
        g.centeredText(font, Component.translatable("gui.simpleskills.rune_infusion.hint"),
                width / 2, top + 23, MUTED);

        drawLeftPanel(g, mc);
        drawList(g, mouseX, mouseY);

        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.selected", selected.size()),
                listX0(), top + 186, selected.size() > MAX_SELECTED ? RED : MUTED, false);
        if (!status.getString().isEmpty()) {
            g.text(font, status, listX0(), top + 198, RED, false);
        }
        if (entries.isEmpty()) {
            g.centeredText(font, Component.translatable("gui.simpleskills.rune_infusion.no_enchantments"),
                    listX0() + (listX1() - listX0()) / 2, top + 100, RED);
        }
    }

    private void drawLeftPanel(GuiGraphicsExtractor g, Minecraft mc) {
        int bx = left + 18;
        int by = top + 36;

        g.fill(bx - 2, by - 2, bx + 90, by + 90, GOLD_DARK);
        g.fill(bx, by, bx + 88, by + 88, 0xFF17130F);
        g.fill(bx + 2, by + 2, bx + 86, by + 86, 0xFF393027);

        ItemStack item = mc.player != null
                && catalog.inventorySlot() >= 0
                && catalog.inventorySlot() < mc.player.getInventory().getNonEquipmentItems().size()
                ? mc.player.getInventory().getItem(catalog.inventorySlot())
                : ItemStack.EMPTY;

        if (!item.isEmpty()) {
            g.pose().pushMatrix();
            g.pose().translate(bx + 12, by + 12);
            g.pose().scale(4.0f, 4.0f);
            g.item(item, 0, 0);
            g.pose().popMatrix();

            String name = font.plainSubstrByWidth(item.getHoverName().getString(), 96);
            g.centeredText(font, Component.literal(name), bx + 44, top + 128, TEXT);
        } else {
            g.centeredText(font, Component.translatable("gui.simpleskills.rune_infusion.no_item"),
                    bx + 44, by + 40, RED);
        }

        int cost = changedCount();
        boolean enough = catalog.runeCount() >= cost;

        g.item(new ItemStack(RuneContent.RUNE), bx, top + 142);
        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.runes", catalog.runeCount()),
                bx + 20, top + 146, enough ? TEXT : RED, false);
        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.cost", cost),
                bx, top + 162, enough ? GOLD : RED, false);

        int score = difficulty();
        int color = score == 0 ? MUTED : difficultyColor(score);
        Component label = score == 0
                ? Component.literal("-")
                : Component.literal(score + " ").append(
                        Component.translatable("gui.simpleskills.rune_infusion.difficulty." + difficultyBand(score)));
        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.difficulty", label),
                bx, top + 176, color, false);

        g.fill(bx, top + 190, bx + 88, top + 196, 0xFF080706);
        g.fill(bx + 1, top + 191, bx + 87, top + 195, 0xFF39312A);
        int filled = 86 * score / MAX_DIFFICULTY;
        if (filled > 0) g.fill(bx + 1, top + 191, bx + 1 + filled, top + 195, color);
    }

    private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x0 = listX0();
        int x1 = listX1();
        int nameW = minusX() - (x0 + 22) - 6;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scroll + i;
            if (index >= entries.size()) break;

            Entry e = entries.get(index);
            int y = listY0() + i * ROW_H;
            Integer lvl = selected.get(e.id());
            boolean sel = lvl != null;
            boolean existing = e.current() > 0;
            boolean removing = existing && !sel;
            boolean blocked = !sel && isBlocked(e);
            boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + ROW_H - 2;

            int bg = sel ? 0xFF3B2E1C : blocked ? 0xFF1B1713 : hover ? 0xFF33281C : 0xFF241C14;
            g.fill(x0, y, x1, y + ROW_H - 2, bg);
            if (sel) g.fill(x0, y, x0 + 2, y + ROW_H - 2, GOLD);
            if (removing) g.fill(x0, y, x0 + 2, y + ROW_H - 2, RED);

            // Rune-style checkbox.
            g.fill(x0 + 7, y + 6, x0 + 17, y + 16, sel ? GOLD : GOLD_DARK);
            g.fill(x0 + 8, y + 7, x0 + 16, y + 15, sel ? 0xFFB8892B : 0xFF17130F);
            if (sel) g.fill(x0 + 10, y + 9, x0 + 14, y + 13, 0xFFFFF4C2);

            String name = e.name().getString();
            String shown = font.plainSubstrByWidth(name, nameW);
            if (shown.length() < name.length()) shown = shown + "..";
            g.text(font, Component.literal(shown), x0 + 22, y + 8,
                    sel ? GOLD : removing ? RED : blocked ? DIM : TEXT, false);

            // Level stepper: [-]  current -> target  [+]
            int stepColor = blocked ? 0xFF1B1713 : 0xFF3A2D1C;
            g.fill(minusX(), y + 5, minusX() + 14, y + 17, stepColor);
            g.fill(plusX(), y + 5, plusX() + 14, y + 17, stepColor);
            g.text(font, Component.literal("-"), minusX() + 5, y + 8, blocked ? DIM : TEXT, false);
            g.text(font, Component.literal("+"), plusX() + 4, y + 8, blocked ? DIM : TEXT, false);

            Component levels;
            int levelColor;
            if (sel && existing && lvl == e.current()) {
                levels = level(e.current());                  // kept as it is
                levelColor = GOLD;
            } else if (removing) {
                levels = Component.empty().append(level(e.current())).append(" > -");   // will be removed
                levelColor = RED;
            } else {
                int target = sel ? lvl : 1;
                levels = Component.empty().append(level(e.current())).append(" > ").append(level(target));
                levelColor = sel ? GREEN : blocked ? DIM : MUTED;
            }
            int lx = (minusX() + 14 + plusX()) / 2 - font.width(levels) / 2;
            g.text(font, levels, lx, y + 8, levelColor, false);
        }

        // Scrollbar.
        if (maxScroll() > 0) {
            int trackX = x1 + 4;
            int trackH = VISIBLE_ROWS * ROW_H;
            g.fill(trackX, listY0(), trackX + 6, listY0() + trackH, 0xFF17130F);
            int thumbH = Math.max(12, trackH * VISIBLE_ROWS / entries.size());
            int thumbY = listY0() + (trackH - thumbH) * scroll / maxScroll();
            g.fill(trackX, thumbY, trackX + 6, thumbY + thumbH, GOLD_DARK);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
