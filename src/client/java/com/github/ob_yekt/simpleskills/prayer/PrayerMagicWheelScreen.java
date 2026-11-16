package com.github.ob_yekt.simpleskills.prayer;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * Roda de 8 slots.
 * Clique esquerdo num slot abre a escolha de magia para aquele slot.
 * Clique direito num slot seleciona o slot ativo.
 */
public final class PrayerMagicWheelScreen extends Screen {
    private static PrayerMagicWheelScreen active;
    private PrayerMenuDataPayload state;

    // Geometria
    private static final int RING_RADIUS = 92;
    private static final int SLOT_W = 104, SLOT_H = 26;
    private static final int INNER_DEAD = 30, OUTER_HIT = 160;

    // Painel de escolha
    private static final int PICK_W = 300, PICK_H = 230;
    private static final int PICK_COLS = 3, PICK_CELL_W = 94, PICK_CELL_H = 30;
    private static final int PICK_ROWS = 4;

    // Cores
    private static final int GOLD = 0xFFFFD978, GOLD_DIM = 0xFF80612A, TEXT = 0xFFF3E8D2, MUTED = 0xFFB7A88D;
    private static final int BG_OUTER = 0xE8100C08, BG_INNER = 0xF217120E, SLOT_BG = 0xFF1B150F, SLOT_ACTIVE = 0xFF3A2C18;

    private static final int BUTTON_LEFT = 0, BUTTON_RIGHT = 1;

    private int cx, cy;
    private boolean pickerOpen;
    private int pickerSlot;
    private int pickerScroll;

    private PrayerMagicWheelScreen(PrayerMenuDataPayload state) {
        super(PrayerText.text("gui.simpleskills.prayer.wheel_title"));
        this.state = state;
    }

    public static void show(PrayerMenuDataPayload payload) {
        active = new PrayerMagicWheelScreen(payload);
        Minecraft.getInstance().gui.setScreen(active);
    }

    public static void updateIfOpen(PrayerMenuDataPayload payload) {
        if (active != null) active.state = payload;
    }

    public static void clearActive() { active = null; }

    @Override public void onClose() { active = null; super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }

    private void layout() {
        cx = width / 2;
        cy = height / 2;
    }

    private List<String> loadout() {
        List<String> values = new ArrayList<>();
        try {
            var array = JsonParser.parseString(state.loadoutJson()).getAsJsonArray();
            for (int i = 0; i < 8; i++) values.add(i < array.size() ? array.get(i).getAsString() : "");
        } catch (RuntimeException ignored) {
            for (int i = 0; i < 8; i++) values.add("");
        }
        return values;
    }

    private List<PrayerAbility> unlockedAbilities() {
        List<PrayerAbility> list = new ArrayList<>();
        if (state.abilityIds() == null || state.abilityIds().isEmpty()) return list;
        for (String id : state.abilityIds().split(",")) {
            PrayerAbility ability = findAbility(id);
            if (ability != null && ability.isUnlocked(state.level())) list.add(ability);
        }
        return list;
    }

    private int[] slotCenter(int slot) {
        double angle = -Math.PI / 2 + slot * Math.PI / 4;
        return new int[]{
                cx + (int) Math.round(Math.cos(angle) * RING_RADIUS),
                cy + (int) Math.round(Math.sin(angle) * RING_RADIUS)
        };
    }

    /** Slot sob o mouse pelo ângulo, ou -1 se fora do anel. */
    private int slotAt(double mx, double my) {
        double dx = mx - cx, dy = my - cy;
        double dist = Math.hypot(dx, dy);
        if (dist < INNER_DEAD || dist > OUTER_HIT) return -1;
        double rel = Math.atan2(dy, dx) - (-Math.PI / 2);
        rel = ((rel % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        return (int) Math.round(rel / (Math.PI / 4)) % 8;
    }

    private int[] pickerBounds() {
        int x0 = cx - PICK_W / 2, y0 = cy - PICK_H / 2;
        return new int[]{x0, y0, x0 + PICK_W, y0 + PICK_H};
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        layout();

        drawRing(g);
        drawSlots(g, mouseX, mouseY);
        drawCenter(g);
        drawHint(g);

        if (pickerOpen) drawPicker(g, mouseX, mouseY);
    }

    private void drawRing(GuiGraphicsExtractor g) {
        // Anel decorativo: segmentos de 45° atrás dos slots
        int r = RING_RADIUS + 26;
        for (int i = 0; i < 64; i++) {
            double a = i * (Math.PI * 2 / 64);
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x - 1, y - 1, x + 1, y + 1, i % 8 == 0 ? GOLD_DIM : 0x6680612A);
        }
    }

    private void drawSlots(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<String> spells = loadout();
        int activeSlot = state.activeSlot();
        int hovered = pickerOpen ? -1 : slotAt(mouseX, mouseY);

        for (int slot = 0; slot < 8; slot++) {
            int[] c = slotCenter(slot);
            int x0 = c[0] - SLOT_W / 2, y0 = c[1] - SLOT_H / 2;
            boolean selected = slot == activeSlot;
            boolean hover = slot == hovered;
            PrayerAbility ability = findAbility(spells.get(slot));

            int border = selected ? GOLD : hover ? 0xFFB08A3E : 0xFF05070A;
            g.fill(x0 - 1, y0 - 1, x0 + SLOT_W + 1, y0 + SLOT_H + 1, border);
            g.fill(x0, y0, x0 + SLOT_W, y0 + SLOT_H, selected ? SLOT_ACTIVE : SLOT_BG);

            // Número do slot numa "placa" à esquerda
            g.fill(x0 + 2, y0 + 2, x0 + 16, y0 + SLOT_H - 2, selected ? GOLD_DIM : 0xFF0A0806);
            g.centeredText(font, Component.literal(String.valueOf(slot + 1)), x0 + 9, y0 + 9,
                    selected ? GOLD : MUTED);

            int labelX = x0 + 20;
            if (ability != null) {
                g.blit(RenderPipelines.GUI_TEXTURED, ability.icon(), x0 + 20, y0 + 5,
                        0, 0, 16, 16, 128, 128, 128, 128);
                labelX = x0 + 40;
            }
            String label = ability == null
                    ? PrayerText.text("gui.simpleskills.prayer.empty_slot").getString()
                    : PrayerText.text(ability.translationKey()).getString();
            label = font.plainSubstrByWidth(label, x0 + SLOT_W - labelX - 4);
            g.text(font, Component.literal(label), labelX, y0 + 9,
                    selected ? GOLD : ability == null ? MUTED : TEXT, false);
        }
    }

    private void drawCenter(GuiGraphicsExtractor g) {
        int w = 116, h = 44;
        g.fill(cx - w / 2 - 1, cy - h / 2 - 1, cx + w / 2 + 1, cy + h / 2 + 1, GOLD_DIM);
        g.fill(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, BG_INNER);

        PrayerAbility equipped = findAbility(state.equipped());
        String name = equipped == null
                ? PrayerText.text("gui.simpleskills.prayer.empty_slot").getString()
                : PrayerText.text(equipped.translationKey()).getString();
        name = font.plainSubstrByWidth(name, w - 10);
        g.centeredText(font, Component.literal(name), cx, cy - 12, equipped == null ? MUTED : GOLD);
        g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.assigned_slot", state.activeSlot() + 1),
                cx, cy + 2, TEXT);
    }

    private void drawHint(GuiGraphicsExtractor g) {
        g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.wheel_hint",
                PrayerText.text("key.simpleskills.prayer_cast")), cx, cy + RING_RADIUS + 46, MUTED);
    }

    private void drawPicker(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int[] b = pickerBounds();
        int x0 = b[0], y0 = b[1], x1 = b[2], y1 = b[3];
        g.fill(x0 - 4, y0 - 4, x1 + 4, y1 + 4, GOLD_DIM);
        g.fill(x0, y0, x1, y1, BG_OUTER);
        g.fill(x0 + 4, y0 + 4, x1 - 4, y1 - 4, BG_INNER);

        g.centeredText(font, Component.literal("Slot " + (pickerSlot + 1)), cx, y0 + 10, GOLD);

        List<PrayerAbility> unlocked = unlockedAbilities();
        List<String> spells = loadout();
        int perPage = PICK_COLS * PICK_ROWS;
        int maxScroll = Math.max(0, (unlocked.size() + PICK_COLS - 1) / PICK_COLS - PICK_ROWS);
        pickerScroll = Math.min(pickerScroll, maxScroll);

        if (unlocked.isEmpty()) {
            g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.empty"), cx, cy, MUTED);
        }

        int gridX = x0 + (PICK_W - PICK_COLS * PICK_CELL_W) / 2;
        int gridY = y0 + 30;
        int start = pickerScroll * PICK_COLS;
        for (int i = 0; i < perPage && start + i < unlocked.size(); i++) {
            PrayerAbility a = unlocked.get(start + i);
            int col = i % PICK_COLS, row = i / PICK_COLS;
            int cx0 = gridX + col * PICK_CELL_W, cy0 = gridY + row * PICK_CELL_H;
            boolean hover = mouseX >= cx0 && mouseX < cx0 + PICK_CELL_W - 4
                    && mouseY >= cy0 && mouseY < cy0 + PICK_CELL_H - 4;
            boolean inSlot = a.id().equals(spells.get(pickerSlot));

            g.fill(cx0, cy0, cx0 + PICK_CELL_W - 4, cy0 + PICK_CELL_H - 4,
                    hover ? 0xFF4A3A20 : inSlot ? SLOT_ACTIVE : SLOT_BG);
            g.blit(RenderPipelines.GUI_TEXTURED, a.icon(), cx0 + 4, cy0 + 4,
                    0, 0, 16, 16, 128, 128, 128, 128);
            String name = font.plainSubstrByWidth(PrayerText.text(a.translationKey()).getString(), PICK_CELL_W - 30);
            g.text(font, Component.literal(name), cx0 + 24, cy0 + 8, inSlot ? GOLD : TEXT, false);
        }

        if (maxScroll > 0) {
            g.centeredText(font, Component.literal((pickerScroll + 1) + " / " + (maxScroll + 1)),
                    cx, y1 - 18, MUTED);
        }
        g.centeredText(font, Component.literal("Esc fecha • roda do mouse rola"), cx, y1 - 32, MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();

        if (pickerOpen) {
            int[] b = pickerBounds();
            if (mx < b[0] || mx > b[2] || my < b[1] || my > b[3]) {
                pickerOpen = false;
                return true;
            }
            List<PrayerAbility> unlocked = unlockedAbilities();
            int gridX = b[0] + (PICK_W - PICK_COLS * PICK_CELL_W) / 2;
            int gridY = b[1] + 30;
            for (int i = 0; i < PICK_COLS * PICK_ROWS; i++) {
                int idx = pickerScroll * PICK_COLS + i;
                if (idx >= unlocked.size()) break;
                int col = i % PICK_COLS, row = i / PICK_COLS;
                int cx0 = gridX + col * PICK_CELL_W, cy0 = gridY + row * PICK_CELL_H;
                if (mx >= cx0 && mx < cx0 + PICK_CELL_W - 4 && my >= cy0 && my < cy0 + PICK_CELL_H - 4) {
                    equipToSlot(pickerSlot, unlocked.get(idx));
                    pickerOpen = false;
                    return true;
                }
            }
            return true;
        }

        int slot = slotAt(mx, my);
        if (slot >= 0) {
            if (button == BUTTON_LEFT) {
                // Esquerdo: abre a escolha de magia para este slot
                pickerSlot = slot;
                pickerScroll = 0;
                pickerOpen = true;
            } else if (button == BUTTON_RIGHT) {
                // Direito: seleciona este slot como o ativo
                selectSlot(slot);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pickerOpen) {
            int rows = (unlockedAbilities().size() + PICK_COLS - 1) / PICK_COLS;
            int maxScroll = Math.max(0, rows - PICK_ROWS);
            pickerScroll = Math.max(0, Math.min(maxScroll, pickerScroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void selectSlot(int slot) {
        if (ClientPlayNetworking.canSend(PrayerActionPayload.TYPE)) {
            ClientPlayNetworking.send(new PrayerActionPayload(PrayerActionPayload.SELECT_SLOT, Integer.toString(slot)));
            PrayerClientState.optimisticSelectSlot(slot);
        } else {
            connectionRequired();
        }
    }

    private void equipToSlot(int slot, PrayerAbility ability) {
        if (!ClientPlayNetworking.canSend(PrayerActionPayload.TYPE)) {
            connectionRequired();
            return;
        }
        ClientPlayNetworking.send(new PrayerActionPayload(PrayerActionPayload.EQUIP_TO_SLOT,
                slot + ":" + ability.id()));

        // Atualização local imediata; o servidor confirma em seguida com o estado oficial.
        List<String> spells = new ArrayList<>(loadout());
        for (int i = 0; i < spells.size(); i++) if (ability.id().equals(spells.get(i))) spells.set(i, "");
        spells.set(slot, ability.id());
        PrayerMenuDataPayload updated = new PrayerMenuDataPayload(state.specialization(), state.level(),
                ability.id(), state.abilityIds(), new Gson().toJson(spells), slot);
        PrayerClientState.accept(updated);
    }

    private void connectionRequired() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    PrayerText.text("gui.simpleskills.prayer.connection_required"));
        }
    }

    private static PrayerAbility findAbility(String id) {
        if (id == null || id.isEmpty()) return null;
        for (PrayerAbility ability : PrayerAbility.values()) if (ability.id().equals(id)) return ability;
        return null;
    }
}
