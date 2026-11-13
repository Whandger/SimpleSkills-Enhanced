package com.github.ob_yekt.simpleskills.prayer;

import java.util.List;
import net.minecraft.resources.Identifier;

/** Data catalog for the Prayer skill tree; unlocks are derived from Prayer level and never spent. */
public enum PrayerAbility {
    LIGHT_ORB("light_orb", PrayerSpecialization.DIVINE, null, 1, 100),
    DIVINE_HEALING("divine_healing", PrayerSpecialization.DIVINE, null, 5, 240),
    HOLY_BOLT("holy_bolt", PrayerSpecialization.DIVINE, null, 10, 80),
    PURIFICATION("purification", PrayerSpecialization.DIVINE, null, 15, 300),
    DIVINE_PROTECTION("divine_protection", PrayerSpecialization.DIVINE, null, 20, 600),
    SANCTUARY("sanctuary", PrayerSpecialization.DIVINE, null, 30, 900),
    DIVINE_SMITE("divine_smite", PrayerSpecialization.DIVINE, null, 40, 300),
    RADIANT_NOVA("radiant_nova", PrayerSpecialization.DIVINE, null, 50, 500),
    HOLY_LIGHT("holy_light", PrayerSpecialization.DIVINE, null, 60, 400),
    CELESTIAL_WRATH("celestial_wrath", PrayerSpecialization.DIVINE, null, 99, 1800),

    SHADOW_BOLT("shadow_bolt", PrayerSpecialization.UNHOLY, null, 1, 80),
    LIFE_DRAIN("life_drain", PrayerSpecialization.UNHOLY, null, 5, 240),
    CURSE_OF_WEAKNESS("curse_of_weakness", PrayerSpecialization.UNHOLY, null, 10, 400),
    CORRUPTION("corruption", PrayerSpecialization.UNHOLY, null, 15, 300),
    SHADOWSTEP("shadowstep", PrayerSpecialization.UNHOLY, null, 20, 240),
    SOUL_HARVEST("soul_harvest", PrayerSpecialization.UNHOLY, null, 30, 500),
    DARK_PACT("dark_pact", PrayerSpecialization.UNHOLY, null, 40, 600),
    SOUL_EXPLOSION("soul_explosion", PrayerSpecialization.UNHOLY, null, 50, 600),
    UNHOLY_GROUND("unholy_ground", PrayerSpecialization.UNHOLY, null, 60, 900),
    APOCALYPSE("apocalypse", PrayerSpecialization.UNHOLY, null, 99, 1800),

    SPARK("spark", PrayerSpecialization.ELEMENTALIST, PrayerElement.ELECTRICITY, 1, 60),
    FIREBALL("fireball", PrayerSpecialization.ELEMENTALIST, PrayerElement.FIRE, 1, 100),
    ICE_SHARD("ice_shard", PrayerSpecialization.ELEMENTALIST, PrayerElement.ICE, 1, 80),
    FLAME_BURST("flame_burst", PrayerSpecialization.ELEMENTALIST, PrayerElement.FIRE, 5, 240),
    LIGHTNING_BOLT("lightning_bolt", PrayerSpecialization.ELEMENTALIST, PrayerElement.ELECTRICITY, 10, 200),
    FROST_NOVA("frost_nova", PrayerSpecialization.ELEMENTALIST, PrayerElement.ICE, 15, 300),
    BURNING_GROUND("burning_ground", PrayerSpecialization.ELEMENTALIST, PrayerElement.FIRE, 20, 500),
    CHAIN_LIGHTNING("chain_lightning", PrayerSpecialization.ELEMENTALIST, PrayerElement.ELECTRICITY, 30, 400),
    ICE_PRISON("ice_prison", PrayerSpecialization.ELEMENTALIST, PrayerElement.ICE, 40, 600),
    INFERNO("inferno", PrayerSpecialization.ELEMENTALIST, PrayerElement.FIRE, 50, 800),
    BLIZZARD("blizzard", PrayerSpecialization.ELEMENTALIST, PrayerElement.ICE, 60, 900),
    METEOR("meteor", PrayerSpecialization.ELEMENTALIST, PrayerElement.FIRE, 70, 1000),
    THUNDERSTORM("thunderstorm", PrayerSpecialization.ELEMENTALIST, PrayerElement.ELECTRICITY, 80, 1200),
    ABSOLUTE_ZERO("absolute_zero", PrayerSpecialization.ELEMENTALIST, PrayerElement.ICE, 90, 1500),
    STORM_JUDGMENT("storm_judgment", PrayerSpecialization.ELEMENTALIST, PrayerElement.ELECTRICITY, 99, 1800);

    private final String id;
    private final PrayerSpecialization specialization;
    private final PrayerElement element;
    private final int requiredLevel;
    private final int baseCooldownTicks;

    PrayerAbility(String id, PrayerSpecialization specialization, PrayerElement element, int requiredLevel, int baseCooldownTicks) {
        this.id = id;
        this.specialization = specialization;
        this.element = element;
        this.requiredLevel = requiredLevel;
        this.baseCooldownTicks = baseCooldownTicks;
    }

    public String id() { return id; }
    public PrayerSpecialization specialization() { return specialization; }
    public PrayerElement element() { return element; }
    public int requiredLevel() { return requiredLevel; }
    public int baseCooldownTicks() { return baseCooldownTicks; }
    public String translationKey() { return "skill.simpleskills.prayer.ability." + id; }
    public Identifier icon() {
        return Identifier.fromNamespaceAndPath("simpleskills", "textures/gui/prayer/abilities/" + id + ".png");
    }
    public boolean isUnlocked(int prayerLevel) { return prayerLevel >= requiredLevel; }

    public static List<PrayerAbility> forSpecialization(PrayerSpecialization specialization) {
        return List.of(values()).stream().filter(ability -> ability.specialization == specialization).toList();
    }
}
