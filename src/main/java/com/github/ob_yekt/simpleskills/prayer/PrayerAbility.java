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
    SACRED_HEALING("sacred_healing", PrayerSpecialization.DIVINE, null, 60, 400),
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
    LIGHTNING_BOLT("lightning_bolt", PrayerSpecialization.ELEMENTALIST, PrayerElement.ELECTRICITY, 10, 200),
    FLAME_BURST("flame_burst", PrayerSpecialization.ELEMENTALIST, PrayerElement.FIRE, 10, 240),
    FROST_NOVA("frost_nova", PrayerSpecialization.ELEMENTALIST, PrayerElement.ICE, 10, 300),
    CHAIN_LIGHTNING("chain_lightning", PrayerSpecialization.ELEMENTALIST, PrayerElement.ELECTRICITY, 20, 400),
    BURNING_GROUND("burning_ground", PrayerSpecialization.ELEMENTALIST, PrayerElement.FIRE, 20, 500),
    ICE_PRISON("ice_prison", PrayerSpecialization.ELEMENTALIST, PrayerElement.ICE, 20, 600),
    THUNDERSTORM("thunderstorm", PrayerSpecialization.ELEMENTALIST, PrayerElement.ELECTRICITY, 30, 1200),
    INFERNO("inferno", PrayerSpecialization.ELEMENTALIST, PrayerElement.FIRE, 30, 800),
    BLIZZARD("blizzard", PrayerSpecialization.ELEMENTALIST, PrayerElement.ICE, 30, 900),
    STORM_JUDGMENT("storm_judgment", PrayerSpecialization.ELEMENTALIST, PrayerElement.ELECTRICITY, 40, 1800),
    METEOR("meteor", PrayerSpecialization.ELEMENTALIST, PrayerElement.FIRE, 40, 1000),
    ABSOLUTE_ZERO("absolute_zero", PrayerSpecialization.ELEMENTALIST, PrayerElement.ICE, 40, 1500);

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

    /** Duração visual e de vida da entidade VFX, em ticks. Servidor e cliente usam este valor. */
    public int durationTicks() {
        return switch (this) {
            case LIGHTNING_BOLT -> 30;
            case DIVINE_HEALING -> 42;
            case SHADOW_BOLT -> 44;
            case METEOR -> 52;
            case DIVINE_PROTECTION, SANCTUARY, DARK_PACT -> 76;
            case CELESTIAL_WRATH, APOCALYPSE, THUNDERSTORM, ABSOLUTE_ZERO, STORM_JUDGMENT -> 88;
            case RADIANT_NOVA, SACRED_HEALING, SOUL_HARVEST, SOUL_EXPLOSION, UNHOLY_GROUND,
                 FLAME_BURST, BURNING_GROUND, INFERNO, FROST_NOVA, BLIZZARD -> 64;
            case SHADOWSTEP, PURIFICATION -> 48;
            default -> 38;
        };
    }

    /**
     * Tick absoluto do primeiro golpe (0 = imediato). Em magia de golpe único é o próprio golpe;
     * em magia de vários golpes é o primeiro de {@link #impactTicks()}.
     */
    public int impactTick() {
        int[] hits = multiHitTicks();
        return hits != null ? hits[0] : singleImpactTick();
    }

    /**
     * Ticks em que cada golpe encosta no alvo ou na área. Dano (servidor), visual (raios, pilares,
     * colapsos), sons e impact frames do cliente usam esta mesma lista: tudo acontece no mesmo tick.
     * O dano total da magia é dividido igualmente entre os golpes.
     */
    public int[] impactTicks() {
        int[] hits = multiHitTicks();
        return hits != null ? hits : new int[] { singleImpactTick() };
    }

    /** Golpes múltiplos. Retorna null para magias de golpe único. */
    private int[] multiHitTicks() {
        int d = durationTicks();
        return switch (this) {
            // 7 feixes: mesmos instantes da fase de perfuração de cada feixe (PrayerHolyBoltVfx).
            case HOLY_BOLT -> {
                int[] ticks = new int[7];
                for (int i = 0; i < ticks.length; i++) {
                    ticks[i] = (int) Math.round((i * 0.028 + 0.045) * d);
                }
                yield ticks;
            }
            // 10 raios: tick em que cada raio toca o chão (PrayerThunderstormVfx).
            case THUNDERSTORM -> spread(10, 9, 58);
            // 16 pilares: tick em que cada pilar toca o chão (PrayerCelestialWrathVfx: início + 0,05).
            case CELESTIAL_WRATH -> {
                int[] ticks = new int[16];
                for (int i = 0; i < ticks.length; i++) {
                    ticks[i] = (int) Math.round((0.14 + i / 16.0 * 0.42 + 0.05) * d);
                }
                yield ticks;
            }
            case APOCALYPSE -> new int[] { 10, (int) Math.round(d * 0.50), (int) Math.round(d * 0.78) };
            // Vórtice e chão ativos: golpes periódicos durante a animação.
            case INFERNO -> spread(6, 12, 52);
            case BURNING_GROUND -> spread(6, 6, 56);
            case UNHOLY_GROUND -> spread(5, 11, 51);
            case BLIZZARD -> spread(8, 12, 54);
            default -> null;
        };
    }

    private int singleImpactTick() {
        int d = durationTicks();
        return switch (this) {
            case LIGHTNING_BOLT -> (int) Math.round(Math.max(10, d * 0.52));
            case SPARK, CHAIN_LIGHTNING -> 0;
            case FIREBALL, ICE_SHARD -> (int) Math.round(d * 0.58);
            case SHADOW_BOLT -> (int) Math.round(d * 0.82);
            case METEOR -> (int) Math.round(d * 0.64);
            case CURSE_OF_WEAKNESS -> (int) Math.round(d * 0.32);
            case CORRUPTION -> (int) Math.round(d * 0.46);
            case ICE_PRISON -> (int) Math.round(d * 0.35);
            case DIVINE_SMITE -> Math.min(d - 4, (int) Math.round(Math.max(6.5, d * 0.34) + 1.2));
            case RADIANT_NOVA -> (int) Math.round(d * 0.24);
            case FLAME_BURST, FROST_NOVA -> 12;
            case SOUL_EXPLOSION -> (int) Math.round(d * 0.52);
            case SOUL_HARVEST, STORM_JUDGMENT -> (int) Math.round(d * 0.42);
            case ABSOLUTE_ZERO -> (int) Math.round(d * 0.68);
            default -> 0;
        };
    }

    /** count golpes igualmente espaçados de first até last (inclusive). */
    private static int[] spread(int count, int first, int last) {
        int[] ticks = new int[count];
        for (int i = 0; i < count; i++) {
            ticks[i] = first + (int) Math.round(i * (last - first) / (double) (count - 1));
        }
        return ticks;
    }

    /** IDs antigos salvos nos loadouts: holy_light virou sacred_healing. */
    public static String migrateLegacyId(String id) {
        return "holy_light".equals(id) ? "sacred_healing" : id;
    }

    public static List<PrayerAbility> forSpecialization(PrayerSpecialization specialization) {
        return List.of(values()).stream().filter(ability -> ability.specialization == specialization).toList();
    }
}
