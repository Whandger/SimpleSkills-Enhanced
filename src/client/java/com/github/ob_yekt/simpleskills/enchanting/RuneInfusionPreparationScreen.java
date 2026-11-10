package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client preparation UI. The server revalidates every selected enchantment and level. */
public final class RuneInfusionPreparationScreen extends Screen {
    private record Entry(String id, int current, int maximum) {}
    private static final int WIDTH = 330;
    private static final int HEIGHT = 238;
    private final RuneInfusionCatalogResponsePayload catalog;
    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Integer> selected = new LinkedHashMap<>();
    private final List<Button> rowButtons = new ArrayList<>();
    private int page;
    private int left;
    private int top;
    private Button previous;
    private Button next;
    private Button start;
    private String status = "";

    public RuneInfusionPreparationScreen(RuneInfusionCatalogResponsePayload catalog) {
        super(Component.translatable("gui.simpleskills.rune_infusion.prepare"));
        this.catalog = catalog;
        if (catalog.rows() != null && !catalog.rows().isBlank()) {
            for (String row : catalog.rows().split(";")) {
                String[] parts = row.split("\\|", -1);
                if (parts.length != 3 || parts[0].length() > 128) continue;
                try {
                    int current = Integer.parseInt(parts[1]);
                    int maximum = Integer.parseInt(parts[2]);
                    if (maximum > current && maximum <= 10) entries.add(new Entry(parts[0], current, maximum));
                } catch (NumberFormatException ignored) { }
            }
        }
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        rowButtons.clear();
        for (int i = 0; i < 5; i++) {
            final int row = i;
            Button button = Button.builder(Component.literal(""), b -> toggle(row))
                    .bounds(left + 18, top + 58 + i * 25, WIDTH - 36, 22).build();
            rowButtons.add(addRenderableWidget(button));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); refreshRows(); })
                .bounds(left + 18, top + 190, 28, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = Math.min(maxPage(), page + 1); refreshRows(); })
                .bounds(left + 50, top + 190, 28, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.simpleskills.rune_infusion.close"), b -> onClose())
                .bounds(left + WIDTH - 112, top + 207, 94, 20).build());
        start = addRenderableWidget(Button.builder(Component.translatable("gui.simpleskills.rune_infusion.start"), b -> start())
                .bounds(left + WIDTH - 218, top + 207, 98, 20).build());
        refreshRows();
    }

    private int maxPage() { return Math.max(0, (entries.size() - 1) / 5); }

    private void toggle(int row) {
        int index = page * 5 + row;
        if (index < 0 || index >= entries.size()) return;
        Entry entry = entries.get(index);
        Integer level = selected.get(entry.id());
        if (level == null) {
            if (selected.size() >= 4) { status = Component.translatable("gui.simpleskills.rune_infusion.max_selected").getString(); }
            else selected.put(entry.id(), entry.current() + 1);
        } else if (level < entry.maximum()) {
            selected.put(entry.id(), level + 1);
        } else {
            selected.remove(entry.id());
        }
        refreshRows();
    }

    private void refreshRows() {
        for (int i = 0; i < rowButtons.size(); i++) {
            int index = page * 5 + i;
            if (index >= entries.size()) {
                rowButtons.get(i).setMessage(Component.literal("—"));
                rowButtons.get(i).active = false;
                continue;
            }
            rowButtons.get(i).active = true;
            Entry entry = entries.get(index);
            Integer level = selected.get(entry.id());
            String label = (level == null ? "[ ] " : "[x] ") + entry.id()
                    + "  " + entry.current() + " → " + (level == null ? entry.current() + 1 : level)
                    + " / " + entry.maximum();
            rowButtons.get(i).setMessage(Component.literal(label));
        }
        previous.active = page > 0;
        next.active = page < maxPage();
        start.active = !selected.isEmpty();
    }

    private void start() {
        if (selected.isEmpty()) return;
        List<String> ids = new ArrayList<>(selected.keySet());
        List<Integer> levels = new ArrayList<>();
        for (String id : ids) levels.add(selected.get(id));
        ClientPlayNetworking.send(new RuneInfusionStartPayload(catalog.inventorySlot(), ids, levels));
        Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, 0xFF80612A);
        g.fill(left, top, left + WIDTH, top + HEIGHT, 0xEE17120E);
        g.fill(left + 5, top + 5, left + WIDTH - 5, top + HEIGHT - 5, 0xFF292018);
        g.centeredText(font, Component.translatable("gui.simpleskills.rune_infusion.prepare"), width / 2, top + 12, 0xFFE9CF7A);
        Minecraft mc = Minecraft.getInstance();
        ItemStack item = mc.player != null
        && catalog.inventorySlot() >= 0
        && catalog.inventorySlot() < mc.player.getInventory().getNonEquipmentItems().size()
        ? mc.player.getInventory().getItem(catalog.inventorySlot())
        : ItemStack.EMPTY;
        g.centeredText(font, item.isEmpty() ? Component.translatable("gui.simpleskills.rune_infusion.no_item")
                : item.getHoverName(), width / 2, top + 28, 0xFFF3E8D2);
        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.runes", catalog.runeCount()), left + 18, top + 43, 0xFFB7A88D, false);
        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.selected", selected.size()), left + 220, top + 43, 0xFFB7A88D, false);
        if (entries.isEmpty()) g.centeredText(font, Component.translatable("gui.simpleskills.rune_infusion.no_enchantments"), width / 2, top + 112, 0xFFE45A4A);
        if (!status.isEmpty()) g.centeredText(font, Component.literal(status), width / 2, top + 178, 0xFFE45A4A);
        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.page", page + 1, maxPage() + 1), left + 90, top + 195, 0xFFB7A88D, false);
    }

    @Override public boolean isPauseScreen() { return false; }
}
