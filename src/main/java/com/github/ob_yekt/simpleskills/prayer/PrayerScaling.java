package com.github.ob_yekt.simpleskills.prayer;

import java.util.Set;

/**
 * Números de Prayer que dependem do nível: dano/cura, recarga, XP por magia,
 * duração e poder dos buffs, e raio das magias de área. Servidor, visual e
 * menu K leem daqui para não divergirem.
 */
public final class PrayerScaling {
    /** Divine Protection (resistência) para em Resistência IV, para não virar imunidade total. */
    public static final int DIVINE_PROTECTION_MAX_POWER = 3;

    /** Fator sobre o XP base para magias que não contam como ofensivas e têm cooldown curto. */
    public static final double NON_COMBAT_XP_FACTOR = 0.5;

    /** Cooldown (ticks) a partir do qual a magia conta como de cooldown longo (30s). */
    private static final int LONG_COOLDOWN_TICKS = 600;

    /** Magias que não miram mob (buffs, curas, utilidade). */
    private static final Set<String> NON_OFFENSIVE = Set.of(
            "light_orb", "divine_healing", "sacred_healing", "sanctuary", "purification",
            "divine_protection", "dark_pact", "shadowstep");

    private PrayerScaling() {}

    /** Dano e cura: +0,6% por nível acima do 1. */
    public static float damageScale(int prayerLevel) {
        return 1.0f + Math.max(0, prayerLevel - 1) * 0.006f;
    }

    /** Multiplicador de recarga: -0,35% por nível acima do 1, piso de 65%. */
    public static double cooldownMultiplier(int prayerLevel) {
        return Math.max(0.65, 1.0 - (prayerLevel - 1) * 0.0035);
    }

    public static int cooldownTicks(PrayerAbility ability, int prayerLevel) {
        return Math.max(20, (int) (ability.baseCooldownTicks() * cooldownMultiplier(prayerLevel)));
    }

    /** Multiplicador de XP por magia: igual ao antigo até o nível 20; depois cresce +0,15x por nível. */
    public static double xpMultiplier(int prayerLevel) {
        return prayerLevel <= 20 ? 1.0 : 1.0 + (prayerLevel - 20) * 0.15;
    }

    /** Só magias ofensivas ou de cooldown longo recebem o bônus de nível no XP. */
    public static boolean earnsLevelXpBonus(PrayerAbility ability) {
        return !NON_OFFENSIVE.contains(ability.id()) || ability.baseCooldownTicks() >= LONG_COOLDOWN_TICKS;
    }

    public static int castXp(PrayerAbility ability, int prayerLevel) {
        double base = 360 + ability.requiredLevel() * 2 + prayerLevel * 6;
        if (earnsLevelXpBonus(ability)) return (int) Math.round(base * xpMultiplier(prayerLevel));
        return (int) Math.round(base * NON_COMBAT_XP_FACTOR);
    }

    /** Duração de qualquer buff do Prayer: +2,31% por nível acima do 1 (≈ +67% no nível 30). */
    public static int buffDurationTicks(int baseTicks, int prayerLevel) {
        return (int) Math.round(baseTicks * (1.0 + Math.max(0, prayerLevel - 1) * 0.0231));
    }

    /** Poder de efeito dos buffs: sobe 1 a cada 20 níveis (I de 1 a 19, II de 20 a 39, III de 40 a 59...). */
    public static int buffPower(int prayerLevel) {
        return Math.min(4, Math.max(1, prayerLevel) / 20);
    }

    /** Raio das magias de área (mantém os raios de antes; só muda o nível em que aparecem). */
    public static double areaRadius(PrayerAbility ability) {
        return switch (ability.id()) {
            case "meteor", "thunderstorm", "absolute_zero", "storm_judgment",
                 "celestial_wrath", "apocalypse" -> 9.0;
            default -> ability.requiredLevel() >= 30 ? 6.0 : 4.0;
        };
    }
}
