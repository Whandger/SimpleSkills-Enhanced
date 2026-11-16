package com.github.ob_yekt.simpleskills.prayer;

import net.minecraft.client.Minecraft;

/** Client mirror of the server-approved Prayer book and equipped spell. */
public final class PrayerClientState {
    private static PrayerMenuDataPayload state;
    private static boolean openRequested;
    private static boolean wheelRequested;
    private PrayerClientState() {}
    public static void requestOpen() { openRequested = true; }
    public static void requestWheel() { wheelRequested = true; }
    public static String equipped() { return state == null ? "" : state.equipped(); }
    public static void accept(PrayerMenuDataPayload payload) {
        state = payload;
        if (wheelRequested && Minecraft.getInstance().player != null) {
            wheelRequested = false;
            PrayerMagicWheelScreen.show(payload);
            return;
        }
        if (openRequested && Minecraft.getInstance().player != null) {
            openRequested = false;
            PrayerSkillbookScreen.showOrUpdate(payload);
        } else {
            PrayerSkillbookScreen.updateIfOpen(payload);
            PrayerMagicWheelScreen.updateIfOpen(payload);
        }
    }
    public static void optimisticSelectSlot(int slot) {
        if (state == null) return;
        var loadout = com.google.gson.JsonParser.parseString(state.loadoutJson()).getAsJsonArray();
        String equipped = slot >= 0 && slot < loadout.size() ? loadout.get(slot).getAsString() : "";
        state = new PrayerMenuDataPayload(state.specialization(), state.level(), equipped,
                state.abilityIds(), state.loadoutJson(), slot);
        PrayerMagicWheelScreen.updateIfOpen(state);
        PrayerSkillbookScreen.updateIfOpen(state);
    }

    public static void optimisticSelectSpecialization(String specialization) {
        if (state == null) return;
        PrayerSpecialization selected = PrayerSpecialization.fromId(specialization);
        if (selected.id().equals(state.specialization())) return;
        String abilities = PrayerAbility.forSpecialization(selected).stream().map(PrayerAbility::id)
                .collect(java.util.stream.Collectors.joining(","));
        String emptyLoadout = "[\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"]";
        state = new PrayerMenuDataPayload(selected.id(), state.level(), "", abilities, emptyLoadout, 0);
        PrayerMagicWheelScreen.updateIfOpen(state);
        PrayerSkillbookScreen.updateIfOpen(state);
    }

    public static void optimisticEquip(String abilityId) {
        if (state == null) return;
        var loadout = com.google.gson.JsonParser.parseString(state.loadoutJson()).getAsJsonArray();
        while (loadout.size() < 8) loadout.add("");
        loadout.set(state.activeSlot(), new com.google.gson.JsonPrimitive(abilityId));
        state = new PrayerMenuDataPayload(state.specialization(), state.level(), abilityId,
                state.abilityIds(), loadout.toString(), state.activeSlot());
        PrayerMagicWheelScreen.updateIfOpen(state);
        PrayerSkillbookScreen.updateIfOpen(state);
    }
    public static void reset() {
        state = null; openRequested = false; wheelRequested = false;
        PrayerSkillbookScreen.clearActive(); PrayerMagicWheelScreen.clearActive();
    }
}
