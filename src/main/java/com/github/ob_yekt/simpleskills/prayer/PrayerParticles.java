package com.github.ob_yekt.simpleskills.prayer;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Custom Prayer particles shared by the server and client. */
public final class PrayerParticles {
    public static final SimpleParticleType DIVINE_SIGIL = FabricParticleTypes.simple();
    public static final SimpleParticleType UNHOLY_SIGIL = FabricParticleTypes.simple();
    public static final SimpleParticleType FIRE_SIGIL = FabricParticleTypes.simple();
    public static final SimpleParticleType ICE_SIGIL = FabricParticleTypes.simple();
    public static final SimpleParticleType LIGHTNING_SIGIL = FabricParticleTypes.simple();
    public static final SimpleParticleType DIVINE_HEALING = FabricParticleTypes.simple();
    public static final SimpleParticleType LIGHTNING_BOLT = FabricParticleTypes.simple();
    public static final SimpleParticleType METEOR = FabricParticleTypes.simple();

    private PrayerParticles() {}

    public static void register() {
        register("prayer_divine", DIVINE_SIGIL);
        register("prayer_unholy", UNHOLY_SIGIL);
        register("prayer_fire", FIRE_SIGIL);
        register("prayer_ice", ICE_SIGIL);
        register("prayer_lightning", LIGHTNING_SIGIL);
        register("prayer_divine_healing", DIVINE_HEALING);
        register("prayer_lightning_bolt", LIGHTNING_BOLT);
        register("prayer_meteor", METEOR);
    }

    public static SimpleParticleType forAbility(PrayerAbility ability) {
        if (ability.id().equals("divine_healing")) return DIVINE_HEALING;
        if (ability.id().equals("lightning_bolt")) return LIGHTNING_BOLT;
        if (ability.id().equals("meteor")) return METEOR;
        if (ability.specialization() == PrayerSpecialization.DIVINE) return DIVINE_SIGIL;
        if (ability.specialization() == PrayerSpecialization.UNHOLY) return UNHOLY_SIGIL;
        return switch (ability.element()) {
            case FIRE -> FIRE_SIGIL;
            case ICE -> ICE_SIGIL;
            case ELECTRICITY -> LIGHTNING_SIGIL;
        };
    }

    private static void register(String path, SimpleParticleType type) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath("simpleskills", path), type);
    }
}
