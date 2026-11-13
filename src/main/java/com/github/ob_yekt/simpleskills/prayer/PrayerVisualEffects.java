package com.github.ob_yekt.simpleskills.prayer;

import com.github.ob_yekt.simpleskills.SimpleSkillsSounds;
import java.util.Set;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Creates server-tracked carriers for client-side procedural Prayer spell renderers. */
public final class PrayerVisualEffects {
    private static final Set<PrayerAbility> HAND_CASTS = Set.of(
            PrayerAbility.HOLY_BOLT,
            PrayerAbility.SHADOW_BOLT,
            PrayerAbility.LIFE_DRAIN,
            PrayerAbility.CURSE_OF_WEAKNESS,
            PrayerAbility.CORRUPTION,
            PrayerAbility.SPARK,
            PrayerAbility.CHAIN_LIGHTNING,
            PrayerAbility.FIREBALL,
            PrayerAbility.ICE_SHARD,
            PrayerAbility.ICE_PRISON,
            PrayerAbility.LIGHTNING_BOLT
    );

    private PrayerVisualEffects() {}

    /** Kept as an initialization hook; VFX now advance locally on tracked render entities. */
    public static void register() {}

    public static PrayerVfxEntity play(ServerLevel level, Vec3 hand, Vec3 impact, PrayerAbility ability,
                                       int prayerLevel, ServerPlayer caster) {
        int style = styleFor(ability);
        int duration = durationFor(ability);
        Vec3 start = switch (ability) {
            case METEOR, DIVINE_HEALING -> impact;
            case SHADOWSTEP -> hand;
            case LIGHT_ORB -> caster.position();
            default -> HAND_CASTS.contains(ability) ? hand : impact;
        };
        Vec3 end = impact;
        int owner = ability == PrayerAbility.DIVINE_HEALING || ability == PrayerAbility.LIGHTNING_BOLT
                ? caster.getId() : -1;
        PrayerVfxEntity visual = new PrayerVfxEntity(level, style, start, end, owner, duration);
        level.addFreshEntity(visual);

        // If the spell hits immediately (tick 0), spawn impact particles now; otherwise emit cast particles at the hand.
        if (impactTickFor(ability) <= 0) {
            spawnImpactParticles(level, impact, ability);
        } else {
            level.sendParticles(particleFor(ability), hand.x, hand.y, hand.z, 4, 0.15, 0.15, 0.15, 0.02);
        }

        float volume = ability.requiredLevel() >= 90 ? 1.0f : ability.requiredLevel() >= 50 ? 0.85f : 0.7f;
        level.playSound(null, hand.x, hand.y, hand.z, SimpleSkillsSounds.prayerSpell(ability),
                SoundSource.PLAYERS, volume, 1.0f);
        return visual;
    }

    public static void spawnImpactParticles(ServerLevel level, Vec3 pos, PrayerAbility ability) {
        int count = 6 + ability.requiredLevel() / 4;
        level.sendParticles(particleFor(ability), pos.x, pos.y + 0.3, pos.z, count, 0.6, 0.4, 0.6, 0.03);
    }

    private static ParticleOptions particleFor(PrayerAbility ability) {
        return switch (ability.specialization()) {
            case DIVINE -> ParticleTypes.END_ROD;
            case UNHOLY -> ParticleTypes.SOUL_FIRE_FLAME;
            case ELEMENTALIST -> switch (ability.element()) {
                case FIRE -> ParticleTypes.FLAME;
                case ICE -> ParticleTypes.SNOWFLAKE;
                case ELECTRICITY -> ParticleTypes.ELECTRIC_SPARK;
            };
        };
    }

    public static void playConnection(ServerLevel level, Vec3 start, Vec3 end, PrayerAbility ability) {
        PrayerVfxEntity connection = new PrayerVfxEntity(level, styleFor(ability), start, end, -1, 18);
        level.addFreshEntity(connection);
    }

    public static int styleFor(PrayerAbility ability) {
        return switch (ability) {
            case LIGHTNING_BOLT -> 0;
            case DIVINE_HEALING -> 1;
            case METEOR -> 2;
            default -> 3 + ability.ordinal();
        };
    }

    public static PrayerAbility abilityForStyle(int style) {
        if (style < 3) return null;
        int ordinal = style - 3;
        PrayerAbility[] abilities = PrayerAbility.values();
        return ordinal >= 0 && ordinal < abilities.length ? abilities[ordinal] : null;
    }

    /** Exact tick when the 3D projectile/strike makes visual contact with the target. */
    public static int impactTickFor(PrayerAbility ability) {
        int duration = durationFor(ability);
        return switch (ability) {
            case LIGHTNING_BOLT -> (int) Math.round(Math.max(10, duration * 0.52));
            case SPARK -> (int) Math.round(duration * 0.40);
            case CHAIN_LIGHTNING -> (int) Math.round(duration * 0.42);
            case HOLY_BOLT -> (int) Math.round(duration * 0.56);
            case FIREBALL, ICE_SHARD -> (int) Math.round(duration * 0.58);
            case SHADOW_BOLT -> (int) Math.round(duration * 0.63);
            case METEOR -> (int) Math.round(duration * 0.64);
            case CURSE_OF_WEAKNESS -> (int) Math.round(duration * 0.32);
            case CORRUPTION -> (int) Math.round(duration * 0.46);
            case ICE_PRISON -> (int) Math.round(duration * 0.35);
            case DIVINE_SMITE -> Math.min(duration - 4, 18);
            case RADIANT_NOVA, FLAME_BURST, FROST_NOVA -> 12;
            case SOUL_EXPLOSION -> (int) Math.round(duration * 0.42);
            case CELESTIAL_WRATH, THUNDERSTORM -> 18;
            case STORM_JUDGMENT, ABSOLUTE_ZERO -> (int) Math.round(duration * 0.55);
            default -> 0; // Immediate self-buffs, heals, tethers, and ground zones
        };
    }

    private static int durationFor(PrayerAbility ability) {
        return switch (ability) {
            case LIGHTNING_BOLT -> 30;
            case DIVINE_HEALING -> 42;
            case METEOR -> 52;
            case DIVINE_PROTECTION, SANCTUARY, DARK_PACT -> 76;
            case CELESTIAL_WRATH, APOCALYPSE, THUNDERSTORM, ABSOLUTE_ZERO, STORM_JUDGMENT -> 88;
            case RADIANT_NOVA, HOLY_LIGHT, SOUL_HARVEST, SOUL_EXPLOSION, UNHOLY_GROUND,
                    FLAME_BURST, BURNING_GROUND, INFERNO, FROST_NOVA, BLIZZARD -> 64;
            case SHADOWSTEP, PURIFICATION -> 48;
            default -> 38;
        };
    }
}
