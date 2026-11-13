package com.github.ob_yekt.simpleskills.prayer;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;

/** Eight-slot circular spell selector. Choosing a slot returns to play; R casts its spell. */
public final class PrayerMagicWheelScreen extends Screen {
    private static PrayerMagicWheelScreen active;
    private PrayerMenuDataPayload state;
    private static final int SIZE = 270;
    private static final int GOLD = 0xFFFFD978, TEXT = 0xFFF3E8D2, MUTED = 0xFFB7A88D;
    private int left, top, centerX, centerY;

    private PrayerMagicWheelScreen(PrayerMenuDataPayload state) {
        super(PrayerText.text("gui.simpleskills.prayer.wheel_title"));
        this.state = state;
    }

    public static void show(PrayerMenuDataPayload payload) {
        active = new PrayerMagicWheelScreen(payload);
        Minecraft.getInstance().gui.setScreen(active);
    }
    public static void updateIfOpen(PrayerMenuDataPayload payload) { if (active != null) active.state = payload; }
    public static void clearActive() { active = null; }
    @Override public void onClose() { active = null; super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }

    private List<String> loadout() {
        List<String> values = new ArrayList<>();
        try {
            var array = JsonParser.parseString(state.loadoutJson()).getAsJsonArray();
            for (int i = 0; i < 8; i++) values.add(i < array.size() ? array.get(i).getAsString() : "");
        } catch (RuntimeException ignored) { for (int i = 0; i < 8; i++) values.add(""); }
        return values;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        left = (width - SIZE) / 2; top = (height - SIZE) / 2;
        centerX = width / 2; centerY = height / 2;
        g.fill(left - 4, top - 4, left + SIZE + 4, top + SIZE + 4, 0xFF80612A);
        g.fill(left, top, left + SIZE, top + SIZE, 0xE817120E);
        g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.wheel_title"), centerX, top + 8, GOLD);

        List<String> spells = loadout();
        for (int slot = 0; slot < 8; slot++) {
            double angle = -Math.PI / 2 + slot * Math.PI / 4;
            int x = centerX + (int) (Math.cos(angle) * 82);
            int y = centerY + (int) (Math.sin(angle) * 82);
            boolean selected = slot == state.activeSlot();
            int color = selected ? 0xFF49371F : 0xFF211A13;
            g.fill(x - 39, y - 14, x + 39, y + 14, selected ? GOLD : 0xFF05070A);
            g.fill(x - 38, y - 13, x + 38, y + 13, color);
            PrayerAbility ability = findAbility(spells.get(slot));
            String spell = ability == null ? "—" : PrayerText.text(ability.translationKey()).getString();
            if (ability != null) g.blit(RenderPipelines.GUI_TEXTURED, ability.icon(),
                    x - 35, y - 8, 0, 0, 16, 16, 128, 128, 128, 128);
            String clipped = font.plainSubstrByWidth(spell, 50);
            if (clipped.length() < spell.length()) clipped += "…";
            g.text(font, Component.literal((slot + 1) + ". " + clipped), x - 17, y - 4,
                    selected ? GOLD : spells.get(slot).isEmpty() ? MUTED : TEXT);
        }
        g.fill(centerX - 43, centerY - 25, centerX + 43, centerY + 25, 0xFF120F0C);
        PrayerAbility active = findAbility(state.equipped());
        String activeSpell = active == null ? PrayerText.text("gui.simpleskills.prayer.empty_slot").getString()
                : PrayerText.text(active.translationKey()).getString();
        g.centeredText(font, Component.literal(activeSpell), centerX, centerY - 6, TEXT);
        g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.wheel_hint",
                PrayerText.text("key.simpleskills.prayer_cast")), centerX, top + SIZE - 16, MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        for (int slot = 0; slot < 8; slot++) {
            double angle = -Math.PI / 2 + slot * Math.PI / 4;
            int x = centerX + (int) (Math.cos(angle) * 82);
            int y = centerY + (int) (Math.sin(angle) * 82);
            if (event.x() < x - 39 || event.x() > x + 39 || event.y() < y - 14 || event.y() > y + 14) continue;
            if (ClientPlayNetworking.canSend(PrayerActionPayload.TYPE)) {
                ClientPlayNetworking.send(new PrayerActionPayload(PrayerActionPayload.SELECT_SLOT, Integer.toString(slot)));
                PrayerClientState.optimisticSelectSlot(slot);
                onClose();
            } else if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        PrayerText.text("gui.simpleskills.prayer.connection_required"));
            }
            return true;
        }
        return false;
    }

    private static PrayerAbility findAbility(String id) {
        for (PrayerAbility ability : PrayerAbility.values()) if (ability.id().equals(id)) return ability;
        return null;
    }
}
