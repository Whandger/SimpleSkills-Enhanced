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
        var player = Minecraft.getInstance().player;
        if (wheelRequested && player != null) {
            wheelRequested = false;
            // Morto: não abre a roda por cima da tela de morte (o jogador ficaria sem o botão de renascer).
            if (player.isAlive()) PrayerMagicWheelScreen.show(payload);
            return;
        }
        if (openRequested && player != null) {
            openRequested = false;
            if (player.isAlive()) PrayerSkillbookScreen.showOrUpdate(payload);
            return;
        }
        PrayerSkillbookScreen.updateIfOpen(payload);
        PrayerMagicWheelScreen.updateIfOpen(payload);
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

    /** Troca a aba do livro sem mexer na roda: a roda é a mesma para todas as escolas. */
    public static void optimisticSelectSpecialization(String specialization) {
        if (state == null) return;
        PrayerSpecialization selected = PrayerSpecialization.fromId(specialization);
        if (selected.id().equals(state.specialization())) return;
        String abilities = PrayerAbility.forSpecialization(selected).stream().map(PrayerAbility::id)
                .collect(java.util.stream.Collectors.joining(","));
        state = new PrayerMenuDataPayload(selected.id(), state.level(), state.equipped(),
                abilities, state.loadoutJson(), state.activeSlot());
        PrayerSkillbookScreen.updateIfOpen(state);
    }

    public static void optimisticEquip(String abilityId) {
        if (state == null) return;
        var array = com.google.gson.JsonParser.parseString(state.loadoutJson()).getAsJsonArray();
        java.util.List<String> loadout = new java.util.ArrayList<>();
        for (int i = 0; i < PrayerLoadout.SLOTS; i++) loadout.add(i < array.size() ? array.get(i).getAsString() : "");
        PrayerLoadout.place(loadout, state.activeSlot(), abilityId);
        state = new PrayerMenuDataPayload(state.specialization(), state.level(), abilityId,
                state.abilityIds(), new com.google.gson.Gson().toJson(loadout), state.activeSlot());
        PrayerMagicWheelScreen.updateIfOpen(state);
        PrayerSkillbookScreen.updateIfOpen(state);
    }
    public static void reset() {
        state = null; openRequested = false; wheelRequested = false;
        PrayerSkillbookScreen.clearActive(); PrayerMagicWheelScreen.clearActive();
    }
}
