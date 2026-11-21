package com.github.ob_yekt.simpleskills.prayer;

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
 * Roda de 8 espaços para escolher a magia ativa: basta passar o mouse sobre um espaço.
 * Ao entrar num espaço, ele vira o ativo e a roda fecha. Para colocar magias nos espaços,
 * use o grimório (tecla B), que mostra o que há em cada espaço antes de trocar.
 */
public final class PrayerMagicWheelScreen extends Screen {
    private static PrayerMagicWheelScreen active;
    private PrayerMenuDataPayload state;

    // Geometria: os espaços são retângulos fixos e a área de seleção é exatamente o retângulo desenhado.
    private static final int RING_RADIUS = 92;
    private static final int SLOT_W = 104, SLOT_H = 26;
    private static final int CENTER_W = 116, CENTER_H = 44;

    // Cores
    private static final int GOLD = 0xFFFFD978, GOLD_DIM = 0xFF80612A, TEXT = 0xFFF3E8D2, MUTED = 0xFFB7A88D;
    private static final int BG_INNER = 0xF217120E, SLOT_BG = 0xFF1B150F, SLOT_ACTIVE = 0xFF3A2C18;

    private int cx, cy;
    // A seleção só vale depois que o cursor sai de um espaço. Assim a roda não fecha sozinha
    // se abrir com o mouse já em cima de um espaço.
    private boolean armed;
    // Espaço escolhido por passagem do mouse; a escolha é aplicada no próximo tick.
    private int pending = -1;
    private boolean closing;

    private PrayerMagicWheelScreen(PrayerMenuDataPayload state) {
        super(PrayerText.text("gui.simpleskills.prayer.wheel_title"));
        this.state = state;
    }

    public static void show(PrayerMenuDataPayload payload) {
        active = new PrayerMagicWheelScreen(payload);
        Minecraft.getInstance().gui.setScreen(active);
    }

    public static void updateIfOpen(PrayerMenuDataPayload payload) {
        if (active != null && Minecraft.getInstance().gui.screen() == active) active.state = payload;
    }

    public static void clearActive() { active = null; }

    @Override public void onClose() { active = null; super.onClose(); }

    /** Tela trocada (ex.: morte com a roda aberta): não deixa a referência presa. */
    @Override public void removed() {
        if (active == this) active = null;
        super.removed();
    }

    @Override public boolean isPauseScreen() { return false; }

    private void layout() {
        cx = width / 2;
        cy = height / 2;
    }

    private List<String> loadout() {
        List<String> values = new ArrayList<>();
        try {
            var array = JsonParser.parseString(state.loadoutJson()).getAsJsonArray();
            for (int i = 0; i < PrayerLoadout.SLOTS; i++) values.add(i < array.size() ? array.get(i).getAsString() : "");
        } catch (RuntimeException ignored) {
            for (int i = 0; i < PrayerLoadout.SLOTS; i++) values.add("");
        }
        return values;
    }

    private int[] slotCenter(int slot) {
        double angle = -Math.PI / 2 + slot * Math.PI / 4;
        return new int[]{
                cx + (int) Math.round(Math.cos(angle) * RING_RADIUS),
                cy + (int) Math.round(Math.sin(angle) * RING_RADIUS)
        };
    }

    /** Espaço sob o mouse (o retângulo desenhado), ou -1. O centro da roda não seleciona nada. */
    private int slotAt(double mx, double my) {
        if (Math.abs(mx - cx) <= CENTER_W / 2.0 && Math.abs(my - cy) <= CENTER_H / 2.0) return -1;
        for (int slot = 0; slot < PrayerLoadout.SLOTS; slot++) {
            int[] c = slotCenter(slot);
            int x0 = c[0] - SLOT_W / 2, y0 = c[1] - SLOT_H / 2;
            if (mx >= x0 && mx < x0 + SLOT_W && my >= y0 && my < y0 + SLOT_H) return slot;
        }
        return -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        layout();

        int hovered = slotAt(mouseX, mouseY);
        if (hovered < 0) {
            armed = true;
        } else if (armed && pending < 0 && !closing) {
            pending = hovered;
        }

        drawRing(g);
        drawSlots(g, hovered);
        drawCenter(g);
        drawHint(g);
    }

    @Override
    public void tick() {
        super.tick();
        if (pending >= 0) choose(pending);
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

    private void drawSlots(GuiGraphicsExtractor g, int hovered) {
        List<String> spells = loadout();
        int activeSlot = state.activeSlot();

        for (int slot = 0; slot < PrayerLoadout.SLOTS; slot++) {
            int[] c = slotCenter(slot);
            int x0 = c[0] - SLOT_W / 2, y0 = c[1] - SLOT_H / 2;
            boolean selected = slot == activeSlot;
            boolean hover = slot == hovered;
            PrayerAbility ability = findAbility(spells.get(slot));

            int border = selected ? GOLD : hover ? 0xFFB08A3E : 0xFF05070A;
            g.fill(x0 - 1, y0 - 1, x0 + SLOT_W + 1, y0 + SLOT_H + 1, border);
            g.fill(x0, y0, x0 + SLOT_W, y0 + SLOT_H, selected ? SLOT_ACTIVE : hover ? 0xFF4A3A20 : SLOT_BG);

            // Número do espaço numa "placa" à esquerda
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
        int w = CENTER_W, h = CENTER_H;
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int slot = slotAt(event.x(), event.y());
        if (slot >= 0) {
            choose(slot);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Torna o espaço o ativo no servidor e fecha a roda. */
    private void choose(int slot) {
        if (closing) return;
        closing = true;
        pending = -1;
        if (ClientPlayNetworking.canSend(PrayerActionPayload.TYPE)) {
            ClientPlayNetworking.send(new PrayerActionPayload(PrayerActionPayload.SELECT_SLOT, Integer.toString(slot)));
            PrayerClientState.optimisticSelectSlot(slot);
        } else {
            connectionRequired();
        }
        onClose();
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
