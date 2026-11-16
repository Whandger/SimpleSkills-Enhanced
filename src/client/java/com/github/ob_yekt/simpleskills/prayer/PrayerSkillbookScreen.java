package com.github.ob_yekt.simpleskills.prayer;

import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;

/** Initial custom Prayer skillbook: specialization selection and rank-unlocked ability loadout. */
public final class PrayerSkillbookScreen extends Screen {
    private static PrayerSkillbookScreen active;
    private PrayerMenuDataPayload state;
    private int left, top, abilityScroll;
    private static final int WIDTH = 430, HEIGHT = 300;
    private static final int GOLD = 0xFFFFD978, TEXT = 0xFFF3E8D2, MUTED = 0xFFB7A88D;

    public PrayerSkillbookScreen(PrayerMenuDataPayload state) {
        super(PrayerText.text("gui.simpleskills.prayer.title"));
        this.state = state;
    }

    public void update(PrayerMenuDataPayload state) { this.state = state; }

    public static void showOrUpdate(PrayerMenuDataPayload payload) {
        if (active != null) active.update(payload);
        else {
            active = new PrayerSkillbookScreen(payload);
            net.minecraft.client.Minecraft.getInstance().gui.setScreen(active);
        }
    }

    public static void clearActive() { active = null; }
    public static void updateIfOpen(PrayerMenuDataPayload payload) { if (active != null) active.update(payload); }

    @Override public void onClose() { active = null; super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        g.fill(left - 3, top - 3, left + WIDTH + 3, top + HEIGHT + 3, 0xFF80612A);
        g.fill(left, top, left + WIDTH, top + HEIGHT, 0xF217120E);
        g.fill(left + 5, top + 5, left + WIDTH - 5, top + HEIGHT - 5, 0xFF292018);
        g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.title"), width / 2, top + 10, GOLD);
        g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.subtitle", state.level()),
                width / 2, top + 25, MUTED);

        PrayerSpecialization[] specs = PrayerSpecialization.values();
        int tabWidth = (WIDTH - 24) / 3;
        for (int i = 0; i < specs.length; i++) {
            int x = left + 8 + i * (tabWidth + 4), y = top + 43;
            boolean selected = specs[i].id().equals(state.specialization());
            g.fill(x, y, x + tabWidth, y + 25, selected ? 0xFF49371F : 0xFF211A13);
            if (selected) g.fill(x, y, x + 2, y + 25, specs[i].color());
            // Draw the full 256x256 source image into the compact tab icon. The shorter
            // overload treated 21x21 as the source region and only sampled its top-left corner.
            g.blit(RenderPipelines.GUI_TEXTURED, specs[i].icon(), x + 4, y + 2,
                    0, 0, 21, 21, 256, 256, 256, 256);
            g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.spec." + specs[i].id()), x + 80, y + 8,
                    selected ? specs[i].color() : MUTED);
        }
        int slotWidth = 49;
        for (int slot = 0; slot < 8; slot++) {
            int x = left + 11 + slot * 51;
            boolean selected = slot == state.activeSlot();
            g.fill(x, top + 73, x + slotWidth, top + 92, selected ? 0xFF80612A : 0xFF120F0C);
            g.centeredText(font, Component.literal("S" + (slot + 1)), x + slotWidth / 2, top + 79,
                    selected ? GOLD : MUTED);
        }
        g.fill(left + 10, top + 97, left + WIDTH - 10, top + HEIGHT - 32, 0xFF120F0C);
        List<PrayerAbility> abilities = Arrays.stream(state.abilityIds().split(",")).map(PrayerSkillbookScreen::findAbility)
                .filter(java.util.Objects::nonNull).toList();
        abilityScroll = Math.min(abilityScroll, Math.max(0, abilities.size() - 6));
        int rows = Math.min(6, abilities.size() - abilityScroll);
        if (rows == 0) g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.empty"), width / 2, top + 132, MUTED);
        for (int i = 0; i < rows; i++) {
            PrayerAbility ability = abilities.get(i + abilityScroll);
            String id = ability.id();
            int y = top + 103 + i * 24;
            boolean unlocked = ability.isUnlocked(state.level());
            boolean equipped = id.equals(state.equipped());
            int assignedSlot = assignedSlot(id);
            g.fill(left + 15, y, left + WIDTH - 15, y + 21, equipped ? 0xFF49371F : unlocked ? 0xFF211A13 : 0xFF181512);
            // Scale the complete ability icon into its 17px row slot.
            g.blit(RenderPipelines.GUI_TEXTURED, ability.icon(), left + 19, y + 2,
                    0, 0, 17, 17, 128, 128, 128, 128);
            g.text(font, PrayerText.text(ability.translationKey()), left + 41, y + 6,
                    equipped ? GOLD : unlocked ? TEXT : 0xFF756B5E, false);
            Component detail = equipped ? PrayerText.text("gui.simpleskills.prayer.equipped")
                    : assignedSlot >= 0 ? PrayerText.text("gui.simpleskills.prayer.assigned_slot", assignedSlot + 1)
                    : unlocked ? PrayerText.text("gui.simpleskills.prayer.equip")
                    : PrayerText.text("gui.simpleskills.prayer.locked", ability.requiredLevel());
            g.text(font, detail, left + WIDTH - 132, y + 6,
                    equipped ? 0xFF67D68B : unlocked ? MUTED : 0xFF8A665D, false);
        }
        g.centeredText(font, PrayerText.text("gui.simpleskills.prayer.cast_hint",
                PrayerText.text("key.simpleskills.prayer_cast")), width / 2,
                top + HEIGHT - 21, MUTED);
    }

    private boolean send(int action, String value) {
        if (ClientPlayNetworking.canSend(PrayerActionPayload.TYPE)) {
            ClientPlayNetworking.send(new PrayerActionPayload(action, value));
            return true;
        }
        if (net.minecraft.client.Minecraft.getInstance().player != null)
            net.minecraft.client.Minecraft.getInstance().player.sendSystemMessage(
                    PrayerText.text("gui.simpleskills.prayer.connection_required"));
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int tabWidth = (WIDTH - 24) / 3;
        if (event.y() >= top + 43 && event.y() < top + 68) {
            int relativeX = (int) event.x() - left - 8;
            if (relativeX < 0 || relativeX >= tabWidth * 3 + 8) return false;
            int index = relativeX / (tabWidth + 4);
            if (index >= 0 && index < PrayerSpecialization.values().length) {
                String specialization = PrayerSpecialization.values()[index].id();
                if (send(PrayerActionPayload.SELECT_SPECIALIZATION, specialization))
                    PrayerClientState.optimisticSelectSpecialization(specialization);
                return true;
            }
        }
        if (event.y() >= top + 73 && event.y() < top + 92 && event.x() >= left + 11 && event.x() < left + 419) {
            int slot = Math.min(7, (int) ((event.x() - left - 11) / 51));
            if (send(PrayerActionPayload.SELECT_SLOT, Integer.toString(slot)))
                PrayerClientState.optimisticSelectSlot(slot);
            return true;
        }
        if (event.x() >= left + 15 && event.x() < left + WIDTH - 15 && event.y() >= top + 103
                && event.y() < top + 103 + Math.min(6, Math.max(0,
                        Arrays.stream(state.abilityIds().split(",")).map(PrayerSkillbookScreen::findAbility)
                                .filter(java.util.Objects::nonNull).toList().size() - abilityScroll)) * 24) {
            int index = (int) ((event.y() - top - 103) / 24);
            List<PrayerAbility> abilities = Arrays.stream(state.abilityIds().split(",")).map(PrayerSkillbookScreen::findAbility)
                    .filter(java.util.Objects::nonNull).toList();
            index += abilityScroll;
            if (index >= 0 && index < abilities.size()) {
                PrayerAbility ability = abilities.get(index);
                if (!ability.isUnlocked(state.level())) {
                    net.minecraft.client.Minecraft.getInstance().player.sendSystemMessage(
                            PrayerText.text("gui.simpleskills.prayer.locked", ability.requiredLevel()));
                } else if (send(PrayerActionPayload.EQUIP, ability.id())) {
                    PrayerClientState.optimisticEquip(ability.id());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= left + 10 && mouseX < left + WIDTH - 10 && mouseY >= top + 97 && mouseY < top + HEIGHT - 28) {
            List<PrayerAbility> abilities = Arrays.stream(state.abilityIds().split(",")).map(PrayerSkillbookScreen::findAbility)
                    .filter(java.util.Objects::nonNull).toList();
            abilityScroll = Math.max(0, Math.min(Math.max(0, abilities.size() - 6), abilityScroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static PrayerAbility findAbility(String id) {
        for (PrayerAbility ability : PrayerAbility.values()) if (ability.id().equals(id)) return ability;
        return null;
    }

    private int assignedSlot(String abilityId) {
        try {
            var loadout = com.google.gson.JsonParser.parseString(state.loadoutJson()).getAsJsonArray();
            for (int i = 0; i < loadout.size(); i++)
                if (abilityId.equals(loadout.get(i).getAsString())) return i;
        } catch (RuntimeException ignored) { }
        return -1;
    }
}
