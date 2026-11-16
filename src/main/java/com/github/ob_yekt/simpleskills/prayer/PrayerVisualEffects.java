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

        // Impacto imediato (tick 0): partículas agora; caso contrário, partículas de conjuração na mão.
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

    /** Tick exato em que o golpe encosta no alvo. Delegado para PrayerAbility (fonte única). */
    public static int impactTickFor(PrayerAbility ability) {
        return ability.impactTick();
    }

    /** Duração da animação. Delegado para PrayerAbility (fonte única). */
    public static int durationFor(PrayerAbility ability) {
        return ability.durationTicks();
    }
}