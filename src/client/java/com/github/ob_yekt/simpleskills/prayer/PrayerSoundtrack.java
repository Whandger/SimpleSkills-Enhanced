package com.github.ob_yekt.simpleskills.prayer;

import static net.minecraft.sounds.SoundEvents.*;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntToDoubleFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/**
 * Trilha sonora + impact frames de todas as magias de Prayer, 100% client-side.
 *
 * Cada magia tem uma linha do tempo de "cues" (sons, tremor de tela, flash de impact frame e
 * impact frame 3D). O renderer chama {@link #update} uma vez por frame por entidade; cada cue
 * dispara uma única vez (ou repete em intervalos) conforme a idade da animação.
 */
public final class PrayerSoundtrack {
    private PrayerSoundtrack() {}

    private enum At { ORIGIN, HAND, TARGET }

    private record Snd(Object event, float vol, float pitch, float jitter) {}

    private static final class Cue {
        IntToDoubleFunction start;
        IntToDoubleFunction until = d -> Double.MAX_VALUE;
        double every = 0;
        At where = At.ORIGIN;
        final List<Snd> sounds = new ArrayList<>();
        float shake = 0f;
        long shakeMs = 350L;
        long flashMs = 0L;
        double frameRadius = 0.0;
        float gain = 1f;

        Cue where(At a) { where = a; return this; }
        Cue snd(Object e, float vol, float pitch) { sounds.add(new Snd(e, vol, pitch, 0f)); return this; }
        Cue snd(Object e, float vol, float pitch, float jitter) { sounds.add(new Snd(e, vol, pitch, jitter)); return this; }
        Cue repeat(double everyTicks, IntToDoubleFunction untilTick) { every = everyTicks; until = untilTick; return this; }
        Cue impact(float shakeDeg, long shakeDurMs, long flash) { shake = shakeDeg; shakeMs = shakeDurMs; flashMs = flash; return this; }
        Cue frame(double radius) { frameRadius = radius; return this; }
    }

    private static Cue ticks(double t) { Cue c = new Cue(); c.start = d -> t; return c; }
    private static Cue frac(double f) { Cue c = new Cue(); c.start = d -> d * f; return c; }
    private static Cue when(IntToDoubleFunction f) { Cue c = new Cue(); c.start = f; return c; }

    /** Monta o cue de um golpe: índice do golpe e número total de golpes da magia. */
    @FunctionalInterface
    private interface HitSpec { void apply(Cue c, int index, int count); }

    /** Um cue por golpe do servidor, no mesmo tick do dano (vem de PrayerAbility.impactTicks). */
    private static void hits(List<Cue> l, PrayerAbility ab, HitSpec spec) {
        int[] hitTicks = ab.impactTicks();
        for (int i = 0; i < hitTicks.length; i++) {
            Cue c = ticks(hitTicks[i]);
            c.gain = hitTicks.length > 1 ? 0.8f : 1f;
            spec.apply(c, i, hitTicks.length);
            l.add(c);
        }
    }

    // ------------------------------------------------------------------ cues por magia
    private static final Map<PrayerAbility, List<Cue>> CACHE = new EnumMap<>(PrayerAbility.class);

    private static List<Cue> cuesFor(PrayerAbility ab) {
        return CACHE.computeIfAbsent(ab, PrayerSoundtrack::build);
    }

    private static List<Cue> build(PrayerAbility ab) {
        List<Cue> l = new ArrayList<>();
        switch (ab) {
            // ---------------------------------------------------------------- DIVINE
            case LIGHT_ORB -> {
                l.add(ticks(0).snd(ENCHANTMENT_TABLE_USE, .9f, 1.5f).snd(AMETHYST_BLOCK_CHIME, .8f, 1.8f));
                l.add(ticks(5).snd(BEACON_POWER_SELECT, .6f, 1.7f));
            }
            case DIVINE_HEALING -> {
                l.add(ticks(0).snd(BEACON_ACTIVATE, .7f, 1.6f));
                l.add(ticks(8).repeat(20, d -> d * .92).snd(AMETHYST_BLOCK_CHIME, .55f, 1.5f, .15f)
                        .snd(EXPERIENCE_ORB_PICKUP, .3f, 1.3f, .1f));
            }
            case HOLY_BOLT -> {
                l.add(ticks(0).where(At.HAND).snd(ENCHANTMENT_TABLE_USE, .9f, 1.7f).snd(AMETHYST_BLOCK_RESONATE, .8f, 1.6f));
                // 7 feixes: um golpe por feixe, no tick em que ele perfura o alvo
                hits(l, ab, (c, i, n) -> c.where(At.TARGET).snd(AMETHYST_CLUSTER_BREAK, .9f, 1.4f)
                        .snd(EXPERIENCE_ORB_PICKUP, .8f, .9f).impact(.6f, 150, i == 0 ? 110 : 0).frame(i == 0 ? 1.2 : 0));
            }
            case PURIFICATION -> {
                l.add(ticks(0).snd(BEACON_ACTIVATE, .8f, 1.8f).snd(PLAYER_LEVELUP, .45f, 1.4f));
                l.add(ticks(10).repeat(20, d -> d * .9).snd(AMETHYST_BLOCK_CHIME, .6f, 1.2f, .2f));
            }
            case DIVINE_PROTECTION -> {
                l.add(ticks(0).snd(BEACON_ACTIVATE, .9f, 1.9f).snd(ENCHANTMENT_TABLE_USE, 1f, 1.2f));
                l.add(ticks(4).snd(AMETHYST_BLOCK_RESONATE, .9f, 1.2f));
                l.add(frac(.9).snd(BEACON_DEACTIVATE, .5f, 1.8f));
            }
            case SANCTUARY -> {
                l.add(ticks(0).snd(BEACON_ACTIVATE, 1f, 1.2f).snd(PLAYER_LEVELUP, .5f, 1f));
                l.add(ticks(12).repeat(25, d -> d * .92).snd(AMETHYST_BLOCK_CHIME, .5f, 1.3f, .2f));
            }
            case SACRED_HEALING -> l.add(ticks(0).snd(BEACON_ACTIVATE, .7f, 2f).snd(AMETHYST_BLOCK_CHIME, .8f, 1.6f));
            case DIVINE_SMITE -> {
                // o impact frame/tremor do Smite já é disparado dentro do próprio PrayerDivineSmiteVfx
                l.add(ticks(0).snd(BEACON_POWER_SELECT, 1f, .8f).snd(ENCHANTMENT_TABLE_USE, 1f, .7f));
                // Impacto visual e tremor são do próprio PrayerDivineSmiteVfx; aqui só o som no tick do golpe
                hits(l, ab, (c, i, n) -> c.snd(LIGHTNING_BOLT_THUNDER, 1.4f, 1.2f)
                        .snd(LIGHTNING_BOLT_IMPACT, 1.2f, 1.3f).snd(AMETHYST_CLUSTER_BREAK, 1f, .8f)
                        .snd(GENERIC_EXPLODE, .7f, 1.5f));
            }
            case RADIANT_NOVA -> {
                l.add(ticks(0).snd(BEACON_POWER_SELECT, .9f, 1.2f).snd(ENCHANTMENT_TABLE_USE, 1f, 1f));
                hits(l, ab, (c, i, n) -> c.snd(GENERIC_EXPLODE, .9f, 1.5f).snd(FIREWORK_ROCKET_LARGE_BLAST, 1.3f, .9f)
                        .snd(AMETHYST_CLUSTER_BREAK, 1f, 1.1f).impact(2.2f, 380, 95).frame(3.0));
                l.add(ticks(ab.impactTick() + 3).repeat(2.5, d -> d * .7).snd(FIREWORK_ROCKET_TWINKLE, .7f, 1.2f, .25f));
            }
            case CELESTIAL_WRATH -> {
                l.add(ticks(0).snd(BEACON_ACTIVATE, 1f, .7f).snd(ENCHANTMENT_TABLE_USE, 1f, .6f));
                // 16 pilares: um trovão por pilar, no tick em que ele toca o chão
                hits(l, ab, (c, i, n) -> c.snd(LIGHTNING_BOLT_THUNDER, i == 0 ? 1.2f : 1.0f, 1.1f)
                        .snd(LIGHTNING_BOLT_IMPACT, i == 0 ? .9f : .8f, 1.2f)
                        .impact(i == 0 ? 1.8f : 1.1f, i == 0 ? 320 : 240, i == 0 ? 90 : 0).frame(i == 0 ? 2.5 : 0));
                l.add(ticks(42).repeat(14, d -> d * .9).snd(LIGHTNING_BOLT_THUNDER, .8f, 1f, .2f)
                        .snd(LIGHTNING_BOLT_IMPACT, .6f, 1.2f, .15f).impact(.8f, 200, 0));
            }
            // ---------------------------------------------------------------- UNHOLY
            case SHADOW_BOLT -> {
                l.add(ticks(0).where(At.HAND).snd(WITHER_SHOOT, .6f, 1.6f).snd(VEX_CHARGE, .9f, .8f));
                l.add(frac(.26).where(At.TARGET).snd(ENDERMAN_TELEPORT, 1f, .5f)
                        .snd(RESPAWN_ANCHOR_DEPLETE, .9f, .6f).impact(1.1f, 240, 0));
                l.add(frac(.32).repeat(6, d -> d * .76).where(At.TARGET)
                        .snd(RESPAWN_ANCHOR_AMBIENT, .65f, .55f, .1f));
                hits(l, ab, (c, i, n) -> c.where(At.TARGET).snd(GENERIC_EXPLODE, 1.15f, .65f)
                        .snd(WITHER_BREAK_BLOCK, 1f, .75f).snd(RESPAWN_ANCHOR_DEPLETE, 1f, .5f)
                        .impact(2.6f, 440, 115).frame(2.4));
            }
            case LIFE_DRAIN -> {
                l.add(ticks(0).where(At.HAND).snd(SOUL_ESCAPE, .9f, .8f).snd(VEX_CHARGE, .7f, .7f));
                l.add(ticks(10).repeat(20, d -> d * .95).where(At.TARGET).snd(WITHER_HURT, .35f, 1.7f, .1f)
                        .snd(EXPERIENCE_ORB_PICKUP, .5f, .6f));
            }
            case CURSE_OF_WEAKNESS -> {
                l.add(ticks(0).where(At.HAND).snd(EVOKER_PREPARE_ATTACK, .8f, .8f));
                hits(l, ab, (c, i, n) -> c.where(At.TARGET).snd(CHAIN_PLACE, 1.2f, .7f).snd(ELDER_GUARDIAN_CURSE, .4f, 1.2f)
                        .impact(1.0f, 220, 0).frame(1.2));
                l.add(when(d -> d * .32 + 5).repeat(2, d -> d * .32 + 9).where(At.TARGET).snd(CHAIN_PLACE, .8f, .8f, .2f));
            }
            case CORRUPTION -> {
                l.add(ticks(0).snd(RESPAWN_ANCHOR_CHARGE, 1f, .6f).snd(ENCHANTMENT_TABLE_USE, .8f, .6f));
                // O núcleo implanta no alvo quando o projétil chega (mesmo tick do dano)
                hits(l, ab, (c, i, n) -> c.where(At.TARGET).snd(ANVIL_LAND, .8f, .5f).snd(WITHER_SPAWN, .5f, 1.6f)
                        .snd(ELDER_GUARDIAN_CURSE, .5f, 1f).impact(2.6f, 420, 110).frame(4.0));
                l.add(ticks(ab.impactTick() + 9).repeat(12, d -> d * .95).snd(WITHER_AMBIENT, .35f, .6f, .2f));
            }
            case SHADOWSTEP -> {
                l.add(ticks(0).snd(ENDERMAN_TELEPORT, 1f, .7f));
                l.add(frac(.42).where(At.TARGET).snd(ENDERMAN_TELEPORT, 1f, 1.2f).snd(PLAYER_ATTACK_SWEEP, .5f, .6f));
            }
            case SOUL_HARVEST -> {
                l.add(ticks(0).where(At.TARGET).snd(SOUL_ESCAPE, .9f, .7f).snd(WITHER_AMBIENT, .5f, .7f));
                hits(l, ab, (c, i, n) -> c.where(At.TARGET).snd(PLAYER_ATTACK_SWEEP, 1.3f, .6f).snd(PLAYER_ATTACK_CRIT, .9f, .7f)
                        .snd(SOUL_ESCAPE, 1f, 1f).impact(1.6f, 260, 70).frame(1.6));
            }
            case DARK_PACT -> {
                l.add(ticks(0).snd(ENCHANTMENT_TABLE_USE, 1f, .6f).snd(ELDER_GUARDIAN_CURSE, .35f, 1.3f));
                l.add(ticks(4).repeat(5, d -> d * .55).snd(CHAIN_PLACE, .4f, .8f, .2f));
                l.add(frac(.62).snd(GLASS_BREAK, 1f, .6f).snd(ZOMBIE_VILLAGER_CURE, .7f, .7f).snd(GENERIC_EXPLODE, .6f, 1.4f)
                        .impact(1.8f, 320, 80).frame(1.8));
            }
            case SOUL_EXPLOSION -> {
                l.add(ticks(0).repeat(7, d -> d * .5).snd(GLASS_BREAK, .35f, .6f, .15f));
                hits(l, ab, (c, i, n) -> c.snd(GENERIC_EXPLODE, .9f, .7f).snd(SOUL_ESCAPE, 1f, .7f).snd(WITHER_BREAK_BLOCK, .7f, .9f)
                        .impact(2.4f, 400, 100).frame(2.4));
            }
            case UNHOLY_GROUND -> {
                l.add(ticks(0).snd(RESPAWN_ANCHOR_DEPLETE, .9f, .6f).snd(WITHER_AMBIENT, .6f, .6f));
                hits(l, ab, (c, i, n) -> c.snd(VEX_CHARGE, .5f, .7f, .2f).snd(WITHER_HURT, .3f, 1.5f, .2f)
                        .impact(i == 0 ? .9f : .4f, 180, 0).frame(i == 0 ? 2.0 : 0));
            }
            case APOCALYPSE -> {
                l.add(ticks(0).snd(WITHER_SPAWN, .9f, .7f).snd(ELDER_GUARDIAN_CURSE, .6f, .8f));
                // Três golpes: o impacto principal (tick 10) e dois tremores de chão
                hits(l, ab, (c, i, n) -> {
                    if (i == 0) {
                        c.snd(LIGHTNING_BOLT_THUNDER, 1.3f, .5f).snd(GENERIC_EXPLODE, 1f, .5f)
                                .impact(3.0f, 520, 140).frame(5.0);
                    } else {
                        c.snd(GENERIC_EXPLODE, .9f, .6f).impact(2f, 300, 0).frame(3.0);
                    }
                });
                l.add(ticks(ab.impactTick() + 8).repeat(11, d -> d * .75).snd(WITHER_BREAK_BLOCK, .45f, .6f, .2f));
            }
            // ---------------------------------------------------------------- ELETRO
            case SPARK -> {
                l.add(ticks(0).where(At.HAND).snd(LIGHTNING_BOLT_IMPACT, .30f, 2f).snd(AMETHYST_BLOCK_RESONATE, .6f, 2f));
                l.add(ticks(0).where(At.TARGET).snd(PLAYER_ATTACK_CRIT, .6f, 1.9f));
            }
            case LIGHTNING_BOLT -> {
                // Carga na mão: só um estalo leve (o trovão é do impacto, não do cast)
                l.add(ticks(0).where(At.HAND).snd(LIGHTNING_BOLT_IMPACT, .2f, 2f).snd(AMETHYST_BLOCK_RESONATE, .7f, 1.8f));
                hits(l, ab, (c, i, n) -> c.where(At.TARGET).snd(LIGHTNING_BOLT_IMPACT, 1f, 1.3f)
                        .snd(LIGHTNING_BOLT_THUNDER, .8f, 1.5f).snd(PLAYER_ATTACK_CRIT, .8f, 1.6f)
                        .impact(1.0f, 250, 0).frame(1.6));
                l.add(ticks(ab.impactTick() + 2).repeat(4, d -> d * .9).where(At.TARGET)
                        .snd(PLAYER_ATTACK_CRIT, .22f, 1.9f, .1f));
            }
            case CHAIN_LIGHTNING -> {
                l.add(ticks(0).where(At.HAND).snd(LIGHTNING_BOLT_THUNDER, .5f, 1.7f));
                l.add(ticks(0).repeat(2, d -> 8).where(At.TARGET).snd(LIGHTNING_BOLT_IMPACT, .5f, 1.8f, .1f)
                        .snd(PLAYER_ATTACK_CRIT, .4f, 1.9f));
                l.add(ticks(2).where(At.TARGET).impact(.8f, 220, 0));
            }
            case THUNDERSTORM -> {
                l.add(ticks(0).snd(AMETHYST_BLOCK_RESONATE, .5f, .7f).snd(WEATHER_RAIN, .9f, 1f));
                // Um trovão por raio, no tick em que ele toca o chão (mesmos ticks do dano no servidor)
                hits(l, ab, (c, i, n) -> c.snd(LIGHTNING_BOLT_THUNDER, .85f, .9f, .25f)
                        .snd(LIGHTNING_BOLT_IMPACT, .85f, 1.2f, .2f).impact(.9f, 220, i == 0 ? 95 : 0));
            }
            case STORM_JUDGMENT -> {
                l.add(ticks(0).snd(AMETHYST_BLOCK_RESONATE, .5f, .7f));
                l.add(ticks(30).snd(BEACON_POWER_SELECT, 1f, 1.4f).snd(LIGHTNING_BOLT_THUNDER, 1f, .6f));
                l.add(ticks(400).repeat(9, d -> d * .5).snd(LIGHTNING_BOLT_IMPACT, .5f, 1.5f, .25f));
                hits(l, ab, (c, i, n) -> c.snd(LIGHTNING_BOLT_THUNDER, 1.6f, .7f).snd(LIGHTNING_BOLT_IMPACT, 1.4f, .6f)
                        .snd(GENERIC_EXPLODE, 1.2f, .7f).snd(TRIDENT_THUNDER, 1f, .8f)
                        .impact(3.2f, 560, 130).frame(4.0));
            }
            // ---------------------------------------------------------------- FIRE
            case FIREBALL -> {
                l.add(ticks(0).where(At.HAND).snd(BLAZE_SHOOT, .9f, .9f).snd(FIRECHARGE_USE, .9f, 1f));
                hits(l, ab, (c, i, n) -> c.where(At.TARGET).snd(GENERIC_EXPLODE, .55f, 1.4f).snd(LAVA_POP, .8f, 1f)
                        .impact(1.3f, 260, 0).frame(1.4));
            }
            case FLAME_BURST -> {
                l.add(ticks(0).snd(FIRECHARGE_USE, 1f, .7f).snd(BLAZE_SHOOT, 1f, .8f));
                l.add(ticks(2).repeat(3, d -> d * .5).snd(FIRECHARGE_USE, .45f, 1f, .3f).snd(FIRE_AMBIENT, .5f, 1f, .2f));
                hits(l, ab, (c, i, n) -> c.snd(FIRECHARGE_USE, .8f, 1.2f).impact(.8f, 200, 0).frame(2.0));
            }
            case BURNING_GROUND -> {
                l.add(ticks(0).snd(LAVA_AMBIENT, .9f, .9f).snd(FIRECHARGE_USE, 1f, .7f).snd(GENERIC_EXPLODE, .4f, 1.6f)
                        .impact(1.2f, 280, 0));
                hits(l, ab, (c, i, n) -> c.snd(LAVA_POP, .6f, 1f, .4f).impact(i == 0 ? .9f : .5f, 200, 0)
                        .frame(i == 0 ? 2.2 : 0));
                l.add(ticks(8).repeat(22, d -> d * .92).snd(FIRE_AMBIENT, .8f, .9f, .1f).snd(LAVA_AMBIENT, .5f, 1f));
            }
            case INFERNO -> {
                l.add(ticks(0).snd(BLAZE_SHOOT, 1.1f, .5f).snd(FIRECHARGE_USE, 1.2f, .6f));
                // Vórtice: cada golpe é um pulso de fogo na base do tornado
                hits(l, ab, (c, i, n) -> c.snd(BLAZE_BURN, .9f, .7f).snd(GENERIC_EXPLODE, i == 0 ? .8f : .35f, .7f)
                        .impact(i == 0 ? 2.0f : 1.0f, i == 0 ? 380 : 220, i == 0 ? 100 : 0).frame(i == 0 ? 3.0 : 0));
                l.add(ticks(14).repeat(14, d -> d * .95).snd(LAVA_AMBIENT, .8f, .8f, .1f).snd(BLAZE_BURN, .5f, .8f, .15f));
            }
            case METEOR -> {
                l.add(ticks(0).snd(FIRECHARGE_USE, 1f, .5f).snd(GHAST_WARN, .7f, .6f));
                l.add(ticks(4).snd(FIREWORK_ROCKET_LAUNCH, 1.2f, .5f).snd(ENDER_DRAGON_FLAP, .9f, .6f));
                hits(l, ab, (c, i, n) -> c.snd(GENERIC_EXPLODE, 2f, .5f).snd(DRAGON_FIREBALL_EXPLODE, 1.5f, .6f)
                        .snd(LIGHTNING_BOLT_IMPACT, 1.2f, .6f).impact(4.2f, 720, 160).frame(6.0));
                l.add(frac(.72).snd(LAVA_POP, 1f, .8f, .3f).snd(FIRE_AMBIENT, 1f, .8f));
            }
            // ---------------------------------------------------------------- ICE
            case ICE_SHARD -> {
                l.add(ticks(0).where(At.HAND).snd(AMETHYST_BLOCK_RESONATE, .8f, 1.2f).snd(PLAYER_HURT_FREEZE, .6f, 1.3f));
                l.add(ticks(2).repeat(2, d -> d * .58).where(At.HAND).snd(AMETHYST_CLUSTER_PLACE, .45f, 1.6f, .3f));
                hits(l, ab, (c, i, n) -> c.where(At.TARGET).snd(GLASS_BREAK, 1f, .9f).snd(AMETHYST_CLUSTER_BREAK, 1f, .8f)
                        .snd(PLAYER_HURT_FREEZE, .7f, 1.1f).impact(1.4f, 260, 0).frame(1.4));
                l.add(frac(.86).where(At.TARGET).snd(GLASS_BREAK, .9f, 1.4f).snd(AMETHYST_BLOCK_BREAK, .9f, 1.2f));
            }
            case FROST_NOVA -> {
                l.add(ticks(0).snd(PLAYER_HURT_FREEZE, 1f, .8f).snd(AMETHYST_BLOCK_RESONATE, 1f, .7f));
                l.add(ticks(4).snd(GLASS_BREAK, 1f, .6f).snd(AMETHYST_CLUSTER_BREAK, 1.2f, .6f));
                hits(l, ab, (c, i, n) -> c.snd(GLASS_BREAK, 1.3f, .5f).snd(AMETHYST_CLUSTER_BREAK, 1.3f, .6f).snd(GENERIC_EXPLODE, .6f, 1.7f)
                        .impact(2.6f, 420, 110).frame(3.5));
            }
            case ICE_PRISON -> {
                l.add(ticks(0).where(At.TARGET).snd(PLAYER_HURT_FREEZE, 1f, 1f).snd(AMETHYST_BLOCK_RESONATE, .8f, 1f));
                l.add(ticks(3).repeat(3, d -> d * .46).where(At.TARGET).snd(AMETHYST_CLUSTER_PLACE, .5f, 1.5f, .3f));
                // A cúpula fecha sobre a vítima no tick do dano
                hits(l, ab, (c, i, n) -> c.where(At.TARGET).snd(ANVIL_LAND, .5f, 1.4f).snd(GLASS_BREAK, 1f, 1.1f)
                        .impact(1.8f, 300, 0).frame(2.2));
                // Estilhaço da prisão, no fim (só som)
                l.add(frac(.78).where(At.TARGET).snd(GLASS_BREAK, 1.3f, .7f).snd(AMETHYST_CLUSTER_BREAK, 1f, .8f));
            }
            case BLIZZARD -> {
                l.add(ticks(0).snd(PLAYER_HURT_FREEZE, .8f, .7f).snd(WEATHER_RAIN, .6f, .6f));
                l.add(ticks(4).repeat(12, d -> d * .95).snd(PHANTOM_FLAP, .5f, .5f, .2f));
                // Cada rajada de cristais que se estilhaça atinge a área neste tick
                hits(l, ab, (c, i, n) -> c.snd(GLASS_BREAK, .5f, 1.4f, .3f).snd(AMETHYST_CLUSTER_BREAK, .4f, 1.2f, .2f)
                        .impact(i == 0 ? .9f : .4f, 160, 0).frame(i == 0 ? 2.5 : 0));
                l.add(ticks(8).repeat(22, d -> d * .95).snd(AMETHYST_BLOCK_CHIME, .3f, .6f, .1f));
            }
            case ABSOLUTE_ZERO -> {
                // impact frame logo no início (pedido no design) + estilhaçar no final
                l.add(ticks(0).snd(PLAYER_HURT_FREEZE, 1.2f, .5f).snd(GLASS_BREAK, 1f, .5f).snd(BEACON_DEACTIVATE, 1f, .5f)
                        .impact(3.5f, 560, 160).frame(6.0));
                l.add(frac(.5).snd(AMETHYST_BLOCK_RESONATE, 1.3f, .5f).snd(PLAYER_HURT_FREEZE, 1f, .6f)
                        .impact(1.5f, 300, 0));
                // Colapso da singularidade: é o golpe de área (mesmo tick do dano)
                hits(l, ab, (c, i, n) -> c.snd(GLASS_BREAK, 1.2f, .8f).snd(GENERIC_EXPLODE, .8f, .9f)
                        .impact(3.0f, 500, 120).frame(5.0));
                l.add(ticks(20).repeat(20, d -> d * .88).snd(AMETHYST_BLOCK_CHIME, .5f, .5f, .1f));
                l.add(frac(.9).snd(GLASS_BREAK, 1.6f, .5f).snd(GLASS_BREAK, 1.4f, .8f).snd(GENERIC_EXPLODE, 1f, .8f)
                        .snd(AMETHYST_CLUSTER_BREAK, 1.4f, .5f).impact(4.0f, 640, 180).frame(6.5));
            }
            default -> { }
        }
        return l;
    }

    // ------------------------------------------------------------------ execução
    private static final class Track {
        final PrayerAbility ability;
        final double[] next;
        float lastAge = -1f;
        long seen;
        Track(PrayerAbility ability, List<Cue> cues, int duration) {
            this.ability = ability;
            this.next = new double[cues.size()];
            for (int i = 0; i < next.length; i++) next[i] = cues.get(i).start.applyAsDouble(duration);
        }
    }

    private static final Map<Integer, Track> TRACKS = new HashMap<>();
    private static final Random RNG = new Random();
    private static int sweepCounter = 0;

    /** Chamar uma vez por frame por entidade (fora do lambda de geometria). */
    public static void update(Entity entity, PrayerAbility ability, Vec3 hand, Vec3 end, float age, int duration) {
        if (ability == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        List<Cue> cues = cuesFor(ability);
        if (cues.isEmpty()) return;

        Track t = TRACKS.get(entity.getId());
        if (t == null || t.ability != ability || age + 0.5f < t.lastAge) {
            t = new Track(ability, cues, duration);
            TRACKS.put(entity.getId(), t);
        }
        t.lastAge = age;
        t.seen = mc.level.getGameTime();

        for (int i = 0; i < cues.size(); i++) {
            Cue c = cues.get(i);
            double nxt = t.next[i];
            if (nxt == Double.POSITIVE_INFINITY) continue;
            double until = c.until.applyAsDouble(duration);
            while (age >= nxt && nxt <= until) {
                boolean stale = age - nxt > Math.max(6.0, c.every * 1.5);
                if (!stale) fire(mc, entity, c, hand, end);
                nxt = c.every > 0 ? nxt + c.every : Double.POSITIVE_INFINITY;
            }
            if (nxt > until) nxt = Double.POSITIVE_INFINITY;
            t.next[i] = nxt;
        }

        if (++sweepCounter >= 200) {
            sweepCounter = 0;
            long now = mc.level.getGameTime();
            Iterator<Track> it = TRACKS.values().iterator();
            while (it.hasNext()) if (now - it.next().seen > 100) it.remove();
        }
    }

    private static Vec3 local(At at, Vec3 hand, Vec3 end) {
        return switch (at) {
            case ORIGIN -> new Vec3(0, 1.0, 0);
            case HAND -> hand;
            case TARGET -> end;
        };
    }

    private static void fire(Minecraft mc, Entity entity, Cue c, Vec3 hand, Vec3 end) {
        Vec3 pos = entity.position().add(local(c.where, hand, end));
        for (Snd s : c.sounds) {
            SoundEvent ev = resolve(s.event());
            if (ev == null) continue;
            float pitch = s.pitch() + (s.jitter() > 0f ? (RNG.nextFloat() * 2f - 1f) * s.jitter() : 0f);
            pitch = Math.max(0.5f, Math.min(2.0f, pitch));
            mc.level.playLocalSound(pos.x, pos.y, pos.z, ev, SoundSource.PLAYERS, s.vol() * c.gain, pitch, false);
        }
        if (c.shake > 0f || c.flashMs > 0L) {
            long flash = c.flashMs;
            // flash de tela inteira só para quem está perto (evita incomodar quem está longe)
            if (mc.player != null && mc.player.position().distanceTo(pos) > 18.0) flash = 0L;
            PrayerImpactFeedback.triggerCue(pos, c.shake, c.shakeMs, flash);
        }
    }

    /** Aceita tanto SoundEvent quanto Holder&lt;SoundEvent&gt; (varia entre versões do MC). */
    private static SoundEvent resolve(Object o) {
        if (o instanceof SoundEvent s) return s;
        if (o instanceof Holder<?> h && h.value() instanceof SoundEvent s) return s;
        return null;
    }

    /** Impact frame 3D (starburst + espinhos) para os cues que pedem frame(raio). */
    public static void drawWorldFrames(Matrix4fc m, VertexConsumer out, PrayerAbility ability, Vec3 hand, Vec3 end,
                                       float age, int duration) {
        if (ability == null) return;
        for (Cue c : cuesFor(ability)) {
            if (c.frameRadius <= 0.0) continue;
            double tick = c.start.applyAsDouble(duration);
            double el = age - tick;
            if (el < 0.0 || el > 1.8) continue;
            Vec3 ground = switch (c.where) {
                case ORIGIN -> Vec3.ZERO;
                case HAND -> hand;
                case TARGET -> end;
            };
            PrayerImpactFeedback.drawWorldImpactFrame(m, out, ground.add(0, 0.12, 0), c.frameRadius, el, 1.8);
        }
    }
}
