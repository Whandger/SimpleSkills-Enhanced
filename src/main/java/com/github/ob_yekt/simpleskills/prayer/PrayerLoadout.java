package com.github.ob_yekt.simpleskills.prayer;

import java.util.List;

/**
 * Regra de colocação das magias nos 8 espaços da roda. Servidor e cliente usam esta mesma
 * função, para que o que o jogador vê seja exatamente o que fica salvo.
 */
public final class PrayerLoadout {
    public static final int SLOTS = 8;

    private PrayerLoadout() {}

    /**
     * Coloca a magia no espaço escolhido sem apagar as outras:
     * - se ela já estava em outro espaço, troca de lugar com o que estava no destino;
     * - se não estava na roda, ocupa o espaço escolhido e devolve a magia que saiu dele (ou "" se vazio).
     */
    public static String place(List<String> loadout, int slot, String abilityId) {
        String displaced = loadout.get(slot);
        if (abilityId.equals(displaced)) return "";

        int from = loadout.indexOf(abilityId);
        loadout.set(slot, abilityId);
        if (from >= 0) {
            loadout.set(from, displaced);
            return "";
        }
        return displaced == null ? "" : displaced;
    }
}
