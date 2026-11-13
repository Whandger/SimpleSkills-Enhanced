package com.github.ob_yekt.simpleskills.prayer;

import com.github.ob_yekt.simpleskills.prayer.divine.*;
import com.github.ob_yekt.simpleskills.prayer.eletro.*;
import com.github.ob_yekt.simpleskills.prayer.fire.*;
import com.github.ob_yekt.simpleskills.prayer.ice.*;
import com.github.ob_yekt.simpleskills.prayer.unholy.*;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/** Dispatcher delegating each Prayer ability to its individual VFX composition class. */
final class PrayerAbilityVfx {
    private PrayerAbilityVfx() {}

    static void draw(Matrix4fc m, VertexConsumer out, PrayerAbility ability, Vec3 hand,
                     Vec3 destination, float age, int duration) {
        switch (ability) {
            case LIGHT_ORB -> PrayerLightOrbVfx.draw(m, out, destination, age, duration);
            case DIVINE_HEALING -> PrayerDivineHealingVfx.draw(m, out, age, duration);
            case HOLY_BOLT -> PrayerHolyBoltVfx.draw(m, out, destination, age, duration);
            case PURIFICATION -> PrayerPurificationVfx.draw(m, out, age, duration);
            case DIVINE_PROTECTION -> PrayerDivineProtectionVfx.draw(m, out, age, duration);
            case SANCTUARY -> PrayerSanctuaryVfx.draw(m, out, age, duration);
            case DIVINE_SMITE -> PrayerDivineSmiteVfx.draw(m, out, age, duration);
            case RADIANT_NOVA -> PrayerRadiantNovaVfx.draw(m, out, age, duration);
            case HOLY_LIGHT -> PrayerHolyLightVfx.draw(m, out, age, duration);
            case CELESTIAL_WRATH -> PrayerCelestialWrathVfx.draw(m, out, age, duration);
            case SHADOW_BOLT -> PrayerShadowBoltVfx.draw(m, out, destination, age, duration);
            case LIFE_DRAIN -> PrayerLifeDrainVfx.draw(m, out, destination, age, duration);
            case CURSE_OF_WEAKNESS -> PrayerCurseOfWeaknessVfx.draw(m, out, destination, age, duration);
            case CORRUPTION -> PrayerCorruptionVfx.draw(m, out, destination, age, duration);
            case SHADOWSTEP -> PrayerShadowstepVfx.draw(m, out, destination, age, duration);
            case SOUL_HARVEST -> PrayerSoulHarvestVfx.draw(m, out, age, duration);
            case DARK_PACT -> PrayerDarkPactVfx.draw(m, out, age, duration);
            case SOUL_EXPLOSION -> PrayerSoulExplosionVfx.draw(m, out, age, duration);
            case UNHOLY_GROUND -> PrayerUnholyGroundVfx.draw(m, out, age, duration);
            case APOCALYPSE -> PrayerApocalypseVfx.draw(m, out, age, duration);
            case LIGHTNING_BOLT -> PrayerLightningVfx.draw(m, out, hand, destination, age, duration);
            case SPARK -> PrayerSparkVfx.draw(m, out, destination, age, duration);
            case CHAIN_LIGHTNING -> PrayerChainLightningVfx.draw(m, out, destination, age, duration);
            case THUNDERSTORM -> PrayerThunderstormVfx.draw(m, out, age, duration);
            case STORM_JUDGMENT -> PrayerStormJudgmentVfx.draw(m, out, age, duration);
            case FIREBALL -> PrayerFireballVfx.draw(m, out, destination, age, duration);
            case FLAME_BURST -> PrayerFlameBurstVfx.draw(m, out, age, duration);
            case BURNING_GROUND -> PrayerBurningGroundVfx.draw(m, out, age, duration);
            case INFERNO -> PrayerInfernoVfx.draw(m, out, age, duration);
            case METEOR -> PrayerMeteorVfx.draw(m, out, age, duration);
            case ICE_SHARD -> PrayerIceShardVfx.draw(m, out, destination, age, duration);
            case FROST_NOVA -> PrayerFrostNovaVfx.draw(m, out, age, duration);
            case ICE_PRISON -> PrayerIcePrisonVfx.draw(m, out, destination, age, duration);
            case BLIZZARD -> PrayerBlizzardVfx.draw(m, out, age, duration);
            case ABSOLUTE_ZERO -> PrayerAbsoluteZeroVfx.draw(m, out, age, duration);
            default -> { }
        }
    }
}
