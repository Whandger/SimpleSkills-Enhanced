package com.github.ob_yekt.simpleskills.prayer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/** Individual procedural compositions for the Prayer abilities beyond the three VFX prototypes. */
final class PrayerAbilityVfx {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private PrayerAbilityVfx() {}

    static void draw(Matrix4fc m, VertexConsumer out, PrayerAbility ability, Vec3 destination,
                     float age, int duration) {
        switch (ability) {
            case LIGHT_ORB -> lightOrb(m, out, destination, age, duration);
            case HOLY_BOLT -> holyBolt(m, out, destination, age, duration);
            case PURIFICATION -> purification(m, out, age, duration);
            case DIVINE_PROTECTION -> divineProtection(m, out, age, duration);
            case SANCTUARY -> sanctuary(m, out, age, duration);
            case DIVINE_SMITE -> divineSmite(m, out, age, duration);
            case RADIANT_NOVA -> radiantNova(m, out, age, duration);
            case HOLY_LIGHT -> holyLight(m, out, age, duration);
            case CELESTIAL_WRATH -> celestialWrath(m, out, age, duration);
            case SHADOW_BOLT -> shadowBolt(m, out, destination, age, duration);
            case LIFE_DRAIN -> lifeDrain(m, out, destination, age, duration);
            case CURSE_OF_WEAKNESS -> curseOfWeakness(m, out, destination, age, duration);
            case CORRUPTION -> corruption(m, out, destination, age, duration);
            case SHADOWSTEP -> shadowstep(m, out, destination, age, duration);
            case SOUL_HARVEST -> soulHarvest(m, out, age, duration);
            case DARK_PACT -> darkPact(m, out, age, duration);
            case SOUL_EXPLOSION -> soulExplosion(m, out, age, duration);
            case UNHOLY_GROUND -> unholyGround(m, out, age, duration);
            case APOCALYPSE -> apocalypse(m, out, age, duration);
            case SPARK -> spark(m, out, destination, age, duration);
            case CHAIN_LIGHTNING -> chainLightning(m, out, destination, age, duration);
            case THUNDERSTORM -> thunderstorm(m, out, age, duration);
            case STORM_JUDGMENT -> stormJudgment(m, out, age, duration);
            case FIREBALL -> fireball(m, out, destination, age, duration);
            case FLAME_BURST -> flameBurst(m, out, age, duration);
            case BURNING_GROUND -> burningGround(m, out, age, duration);
            case INFERNO -> inferno(m, out, age, duration);
            case ICE_SHARD -> iceShard(m, out, destination, age, duration);
            case FROST_NOVA -> frostNova(m, out, age, duration);
            case ICE_PRISON -> icePrison(m, out, destination, age, duration);
            case BLIZZARD -> blizzard(m, out, age, duration);
            case ABSOLUTE_ZERO -> absoluteZero(m, out, age, duration);
            default -> { }
        }
    }

    // 1. LIGHT_ORB — three orbital planes surround a liquid-looking core as it flies from the hand.
    private static void lightOrb(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 4, 5);
        double travel = smooth(5, duration * 0.66, age);
        Vec3 center = new Vec3(0, 1.18, 0).lerp(destination.add(0, 1.18, 0), travel);
        double pulse = 1 + Math.sin(age * 0.48) * 0.09;
        double size = (0.12 + 0.21 * smooth(0, 8, age)) * pulse;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(size, size * 1.12, size), 10, 16,
                1.0f, 0.91f, 0.63f, 0.98f * fade, age * 0.18);
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(size * 0.66, size * 0.76, size * 0.66),
                8, 14, 1.0f, 1.0f, 0.94f, 0.98f * fade, -age * 0.31);
        ringPlane(m, out, center, UP, size * 1.55, 0.018, age * 0.22,
                1.0f, 0.78f, 0.25f, 0.92f * fade);
        ringPlane(m, out, center, new Vec3(1, 0.4, 0), size * 1.83, 0.014, -age * 0.31,
                1.0f, 0.93f, 0.55f, 0.84f * fade);
        ringPlane(m, out, center, new Vec3(0.25, 0.35, 1), size * 2.12, 0.012, age * 0.38,
                0.93f, 0.76f, 0.38f, 0.76f * fade);
        for (int fragment = 0; fragment < 8; fragment++) {
            double angle = fragment * Math.PI / 4 + age * (fragment % 2 == 0 ? 0.11 : -0.14);
            Vec3 axis = new Vec3(Math.cos(angle), Math.sin(angle * 1.7) * 0.6, Math.sin(angle)).normalize();
            Vec3 point = center.add(axis.scale(size * (2.3 + (fragment % 3) * 0.22)));
            crystal(m, out, point, axis, 0.11 * fade, 0.035 * fade,
                    1.0f, 0.86f, 0.39f, 0.9f * fade);
        }
        if (travel > 0.96) {
            double burst = smooth(0.96, 1, travel);
            ringPlane(m, out, destination, UP, 0.2 + burst * 1.2, 0.022,
                    age * 0.14, 1.0f, 0.91f, 0.53f, (float) (0.8 * (1 - burst) * fade));
            PrayerVfxGeometry.wireSphere(m, out, destination, 0.16 + burst * 0.6, 10,
                    age * 0.2, 1.0f, 0.93f, 0.66f, (float) (0.55 * (1 - burst) * fade), 0.012);
        }
    }

    // 3. HOLY_BOLT — a narrow, faceted lance with a rotating tail cross and star-shaped contact flash.
    private static void holyBolt(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 2, 4);
        double travel = smooth(2, duration * 0.56, age);
        Vec3 direction = safeNormalize(destination.subtract(Vec3.ZERO), new Vec3(0, 0, -1));
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        drawLance(m, out, center, direction, 1.35, 0.17, age * 0.24,
                1.0f, 0.88f, 0.33f, 0.92f * fade);
        ringPlane(m, out, center.subtract(direction.scale(0.34)), direction, 0.22, 0.016,
                age * 0.5, 1.0f, 0.81f, 0.2f, 0.9f * fade);
        ringPlane(m, out, center.subtract(direction.scale(0.56)), direction, 0.13, 0.012,
                -age * 0.62, 1.0f, 0.96f, 0.65f, 0.92f * fade);
        if (travel > 0.88) {
            double impact = smooth(0.88, 1, travel);
            Vec3 point = destination;
            starburst(m, out, point, UP, 0.35 + impact * 0.9,
                    (float) (0.9 * (1 - impact) * fade), 1.0f, 0.85f, 0.32f);
            ringPlane(m, out, point, direction, 0.16 + impact * 0.55, 0.021,
                    age * 0.13, 1.0f, 0.97f, 0.69f, (float) (0.82 * (1 - impact) * fade));
        }
    }

    // 4. PURIFICATION — a scanning halo strips dark fragments from the body into a white-gold seal.
    private static void purification(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 5, 8);
        double rise = smooth(3, duration * 0.72, age) * 2.25;
        double scan = 0.10 + smooth(2, duration * 0.68, age) * 2.05;
        circleSigil(m, out, Vec3.ZERO, 1.65, age * 0.018, fade, 1.0f, 0.82f, 0.32f);
        ringPlane(m, out, new Vec3(0, scan, 0), UP, 1.05 + Math.sin(age * 0.3) * 0.08,
                0.025, age * 0.08, 0.96f, 0.97f, 0.79f, fade);
        ringPlane(m, out, new Vec3(0, scan, 0), new Vec3(1, 0, 0), 0.82,
                0.012, -age * 0.12, 1.0f, 0.74f, 0.25f, 0.78f * fade);
        runeHalo(m, out, new Vec3(0, 2.55, 0), 0.62, age * 0.035, fade,
                0.98f, 0.87f, 0.45f);
        for (int mote = 0; mote < 10; mote++) {
            double angle = mote * Math.PI * 2 / 10 + age * 0.06;
            double collapse = smooth(duration * 0.15, duration * 0.6, age);
            double radius = 1.35 + (1 - collapse) * 0.8;
            double y = 0.3 + (mote % 4) * 0.48 + Math.sin(age * 0.12 + mote) * 0.12;
            Vec3 point = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            double outward = smooth(0, duration * 0.2, age) * 0.8;
            Vec3 dark = point.add(Math.cos(angle) * outward, 0.7 + rise * 0.4, Math.sin(angle) * outward);
            crystal(m, out, dark, point.subtract(dark), 0.16, 0.065,
                    0.21f, 0.13f, 0.29f, (float) (0.7 * fade * (1 - collapse * 0.72)));
            Vec3 light = point.add(Math.cos(angle) * 0.28, rise * 0.25, Math.sin(angle) * 0.28);
            PrayerVfxGeometry.ellipsoid(m, out, light, new Vec3(0.035, 0.045, 0.035), 6, 8,
                    1.0f, 0.93f, 0.62f, fade, angle);
        }
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 1.0 + rise * 0.25, 0),
                new Vec3(0.48, 1.05, 0.48), 8, 12, 1.0f, 0.88f, 0.48f, 0.08f * fade, age * 0.08);
    }

    // 5. DIVINE_PROTECTION — a segmented celestial shell, not a simple outline.
    private static void divineProtection(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 7, 10);
        double build = smooth(0, 12, age);
        Vec3 center = new Vec3(0, 1.0, 0);
        double pulse = 1 + Math.sin(age * 0.15) * 0.025;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(1.02 * pulse, 1.38 * pulse, 0.78 * pulse),
                10, 18, 0.78f, 0.88f, 1.0f, 0.075f * fade, age * 0.02);
        PrayerVfxGeometry.wireSphere(m, out, center, 1.38 * pulse, 20, age * 0.018,
                1.0f, 0.79f, 0.26f, 0.52f * fade, 0.018);
        ringPlane(m, out, center, UP, 1.12 * pulse, 0.024, age * 0.025,
                1.0f, 0.87f, 0.43f, 0.88f * fade);
        ringPlane(m, out, center, new Vec3(1, 0.1, 0), 1.08 * pulse, 0.016,
                -age * 0.03, 0.88f, 0.96f, 1.0f, 0.72f * fade);
        ringPlane(m, out, center, new Vec3(0.2, 0, 1), 1.08 * pulse, 0.016,
                age * 0.028, 0.88f, 0.96f, 1.0f, 0.72f * fade);
        for (int panel = 0; panel < 12; panel++) {
            double longitude = panel * Math.PI * 2 / 12 + age * 0.008;
            double latitude = (panel % 3 - 1) * 0.56;
            Vec3 radial = new Vec3(Math.cos(longitude) * Math.cos(latitude), Math.sin(latitude),
                    Math.sin(longitude) * Math.cos(latitude));
            Vec3 point = center.add(radial.scale(1.25 * build));
            Vec3 tangent = new Vec3(-Math.sin(longitude), 0.3, Math.cos(longitude)).normalize();
            crystal(m, out, point, radial, 0.55, 0.22,
                    0.95f, 0.86f, 0.58f, 0.36f * fade);
            PrayerVfxGeometry.tube(m, out, point.subtract(tangent.scale(0.22)),
                    point.add(tangent.scale(0.22)), 0.016, 1.0f, 0.94f, 0.7f, 0.9f * fade);
        }
        for (int rune = 0; rune < 6; rune++) {
            double angle = rune * Math.PI / 3 - age * 0.025;
            Vec3 p = center.add(Math.cos(angle) * 1.41, Math.sin(age * 0.08 + rune) * 0.12,
                    Math.sin(angle) * 1.05);
            runeHalo(m, out, p, 0.18, angle, fade, 1.0f, 0.92f, 0.63f);
        }
    }

    // 6. SANCTUARY — a fixed sacred territory with eight standing light-pillars and a central altar.
    private static void sanctuary(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 10, 12);
        double expansion = smooth(0, 12, age);
        double radius = 3.15 * expansion;
        circleSigil(m, out, new Vec3(0, 0.04, 0), radius, age * 0.008, fade,
                0.96f, 0.78f, 0.27f);
        ringPlane(m, out, new Vec3(0, 0.07, 0), new Vec3(1, 0.08, 0), radius * 0.72,
                0.025, -age * 0.01, 1.0f, 0.9f, 0.53f, 0.82f * fade);
        for (int pillar = 0; pillar < 8; pillar++) {
            double angle = pillar * Math.PI / 4 + age * 0.003;
            Vec3 base = new Vec3(Math.cos(angle) * radius * 0.82, 0.15, Math.sin(angle) * radius * 0.82);
            double height = (0.55 + smooth(pillar * 1.3, pillar * 1.3 + 6, age) * 1.65)
                    * (0.92 + 0.08 * Math.sin(age * 0.12 + pillar));
            Vec3 top = base.add(0, height, 0);
            PrayerVfxGeometry.taperedTube(m, out, base, top, 0.11, 0.025,
                    0.91f, 0.75f, 0.27f, 0.6f * fade);
            PrayerVfxGeometry.ellipsoid(m, out, top, new Vec3(0.12, 0.16, 0.12), 6, 8,
                    1.0f, 0.94f, 0.66f, 0.84f * fade, age * 0.1 + pillar);
            ringPlane(m, out, top, UP, 0.18, 0.012, age * 0.035 + pillar,
                    1.0f, 0.84f, 0.37f, 0.8f * fade);
        }
        // A stepped altar gives the centre a landmark distinct from the boundary circle.
        for (int tier = 0; tier < 3; tier++) {
            double r = 0.68 - tier * 0.16;
            double y = 0.14 + tier * 0.29;
            ringPlane(m, out, new Vec3(0, y, 0), UP, r, 0.035,
                    age * (tier % 2 == 0 ? 0.025 : -0.03), 0.95f, 0.78f, 0.29f, 0.85f * fade);
        }
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 0.92, 0), new Vec3(0.25, 0.42, 0.25),
                8, 12, 1.0f, 0.91f, 0.56f, 0.58f * fade, age * 0.035);
    }

    // 7. DIVINE_SMITE — a suspended seal compresses into a focused vertical sentence of light.
    private static void divineSmite(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 3, 7);
        double descend = smooth(5, Math.min(duration - 4, 18), age);
        double charge = smooth(0, 8, age);
        double altitude = 12 * (1 - descend);
        Vec3 crown = new Vec3(0, 12, 0);
        ringPlane(m, out, crown, UP, 1.55 * (0.8 + charge * 0.2), 0.028,
                age * 0.035, 1.0f, 0.77f, 0.24f, 0.92f * fade);
        ringPlane(m, out, crown, new Vec3(1, 0.15, 0), 1.08, 0.018,
                -age * 0.06, 1.0f, 0.94f, 0.62f, 0.9f * fade);
        runeHalo(m, out, crown, 0.82, age * 0.03, fade, 1.0f, 0.9f, 0.48f);
        Vec3 beamTop = new Vec3(0, Math.max(altitude, 0.45), 0);
        double beamWidth = 0.24 + 0.16 * Math.sin(age * 0.6);
        PrayerVfxGeometry.taperedTube(m, out, beamTop, new Vec3(0, 0.12, 0), beamWidth,
                0.035, 1.0f, 0.75f, 0.25f, 0.42f * fade);
        PrayerVfxGeometry.taperedTube(m, out, beamTop, new Vec3(0, 0.12, 0), beamWidth * 0.48,
                0.018, 1.0f, 0.95f, 0.7f, 0.92f * fade);
        for (int lance = 0; lance < 8; lance++) {
            double angle = lance * Math.PI / 4 + age * 0.025;
            Vec3 start = new Vec3(Math.cos(angle) * 0.4, altitude + 0.35, Math.sin(angle) * 0.4);
            Vec3 end = new Vec3(Math.cos(angle) * 1.45, 0.08, Math.sin(angle) * 1.45);
            PrayerVfxGeometry.taperedTube(m, out, start, end, 0.075, 0.004,
                    1.0f, 0.88f, 0.48f, 0.7f * fade);
        }
        double impact = smooth(0.7, 0.92, age / duration);
        ringPlane(m, out, new Vec3(0, 0.07, 0), UP, 0.25 + impact * 2.0, 0.035,
                age * 0.08, 1.0f, 0.88f, 0.46f, (float) (0.8 * (1 - impact) * fade));
        if (impact > 0) starburst(m, out, new Vec3(0, 0.1, 0), UP, 1.8 * impact,
                (float) (0.65 * (1 - impact) * fade), 1.0f, 0.92f, 0.59f);
    }

    // 8. RADIANT_NOVA — a compressed star detonates into a volumetric shell and three orthogonal waves.
    private static void radiantNova(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 4, 8);
        double charge = 1 - smooth(0, 12, age);
        double release = smooth(10, duration * 0.72, age);
        Vec3 center = new Vec3(0, 1.0, 0);
        double core = 0.16 + charge * 0.36;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(core, core, core), 10, 16,
                1.0f, 0.93f, 0.68f, 0.94f * fade, age * 0.12);
        PrayerVfxGeometry.wireSphere(m, out, center, core * 1.65, 12, age * 0.14,
                1.0f, 0.75f, 0.22f, 0.8f * fade, 0.016);
        for (int wave = 0; wave < 3; wave++) {
            double radius = 0.15 + release * (1.35 + wave * 0.72);
            Vec3 normal = wave == 0 ? UP : wave == 1 ? new Vec3(1, 0.22, 0) : new Vec3(0.2, 0, 1);
            ringPlane(m, out, center, normal, radius, 0.035 - wave * 0.005,
                    age * (wave % 2 == 0 ? 0.035 : -0.04), 1.0f, 0.87f, 0.43f,
                    (float) ((1 - release * 0.72) * fade));
        }
        PrayerVfxGeometry.wireSphere(m, out, center, 0.25 + release * 3.6, 20, age * 0.018,
                0.98f, 0.82f, 0.49f, (float) (0.35 * (1 - release) * fade), 0.018);
        for (int shard = 0; shard < 14; shard++) {
            double angle = shard * Math.PI * 2 / 14 + age * 0.025;
            Vec3 dir = new Vec3(Math.cos(angle), Math.sin(angle * 2.1) * 0.72, Math.sin(angle)).normalize();
            double radius = 0.38 + release * (1.3 + (shard % 4) * 0.38);
            Vec3 point = center.add(dir.scale(radius));
            crystal(m, out, point, dir, 0.42 * (1 - release * 0.55), 0.09,
                    1.0f, 0.81f, 0.36f, (float) (0.86 * fade * (1 - release * 0.35)));
        }
    }

    // 9. HOLY_LIGHT — calm column, suspended script and slow orbiting crown rings.
    private static void holyLight(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 10, 12);
        double height = 5.2 * smooth(0, 13, age);
        double pulse = 1 + Math.sin(age * 0.12) * 0.035;
        circleSigil(m, out, new Vec3(0, 0.06, 0), 1.7 * pulse, age * 0.009, fade,
                0.94f, 0.85f, 0.49f);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 0.12, 0), new Vec3(0, height, 0),
                0.72, 0.32, 1.0f, 0.82f, 0.36f, 0.12f * fade);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 0.12, 0), new Vec3(0, height, 0),
                0.28, 0.12, 1.0f, 0.94f, 0.68f, 0.25f * fade);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 0.12, 0), new Vec3(0, height, 0),
                0.09, 0.045, 1.0f, 1.0f, 0.93f, 0.6f * fade);
        for (int ring = 0; ring < 4; ring++) {
            double y = 0.5 + ring * 1.22 + Math.sin(age * 0.08 + ring) * 0.12;
            ringPlane(m, out, new Vec3(0, y, 0), UP, 0.45 + ring * 0.13,
                    0.022, age * (ring % 2 == 0 ? 0.02 : -0.025),
                    0.98f, 0.87f, 0.56f, 0.82f * fade);
            runeHalo(m, out, new Vec3(0, y + 0.26, 0), 0.24, age * 0.018 + ring,
                    0.78f * fade, 1.0f, 0.92f, 0.68f);
        }
        for (int ray = 0; ray < 8; ray++) {
            double angle = ray * Math.PI / 4 + age * 0.012;
            Vec3 base = new Vec3(Math.cos(angle) * 0.68, 0.2, Math.sin(angle) * 0.68);
            Vec3 top = base.add(0, height * (0.45 + (ray % 3) * 0.12), 0);
            PrayerVfxGeometry.taperedTube(m, out, base, top, 0.022, 0.008,
                    0.95f, 0.84f, 0.5f, 0.62f * fade);
        }
    }

    // 10. CELESTIAL_WRATH — a sky-sized seal commands four staggered descending pillars.
    private static void celestialWrath(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 8, 12);
        double open = smooth(0, 15, age);
        Vec3 sky = new Vec3(0, 15, 0);
        ringPlane(m, out, sky, UP, 4.1 * open, 0.06, age * 0.018,
                1.0f, 0.78f, 0.26f, 0.8f * fade);
        ringPlane(m, out, sky, new Vec3(1, 0.25, 0), 3.1 * open, 0.035,
                -age * 0.03, 1.0f, 0.92f, 0.58f, 0.9f * fade);
        ringPlane(m, out, sky, new Vec3(0.15, 0, 1), 2.1 * open, 0.028,
                age * 0.04, 0.96f, 0.84f, 0.45f, 0.8f * fade);
        runeHalo(m, out, sky, 1.35, -age * 0.018, fade, 1.0f, 0.9f, 0.5f);
        for (int wing = 0; wing < 2; wing++) {
            double side = wing == 0 ? -1 : 1;
            for (int feather = 0; feather < 5; feather++) {
                Vec3 start = sky.add(side * (0.6 + feather * 0.42), -0.4, 0);
                Vec3 tip = sky.add(side * (2.8 + feather * 0.32), -0.9 - feather * 0.22,
                        (feather - 2) * 0.35);
                PrayerVfxGeometry.taperedTube(m, out, start, tip, 0.08, 0.006,
                        1.0f, 0.88f, 0.48f, 0.58f * fade);
            }
        }
        double strikePhase = (age - 14) / 36.0;
        for (int strike = 0; strike < 4; strike++) {
            double startAt = strike * 0.17;
            double fall = smooth(startAt, startAt + 0.16, strikePhase);
            double retract = smooth(startAt + 0.25, startAt + 0.42, strikePhase);
            double length = 14 * fall * (1 - retract * 0.35);
            double angle = strike * Math.PI / 2 + 0.4;
            Vec3 base = new Vec3(Math.cos(angle) * (1.0 + strike % 2), 0.1,
                    Math.sin(angle) * (1.0 + strike % 2));
            Vec3 top = base.add(0, length, 0);
            PrayerVfxGeometry.taperedTube(m, out, top, base, 0.34, 0.06,
                    0.94f, 0.76f, 0.34f, 0.24f * fade);
            PrayerVfxGeometry.taperedTube(m, out, top, base, 0.12, 0.025,
                    1.0f, 0.97f, 0.72f, 0.82f * fade);
            if (fall > 0.55) {
                double ring = 0.25 + fall * (1.1 + retract * 1.5);
                ringPlane(m, out, base, UP, ring, 0.026, age * 0.07,
                        1.0f, 0.83f, 0.4f, (float) (0.7 * (1 - retract) * fade));
            }
        }
    }

    // 11. SHADOW_BOLT — an unstable, cracked mass wrapped in rotating shards and a torn wake.
    private static void shadowBolt(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 3, 5);
        double travel = smooth(3, duration * 0.63, age);
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        double pulse = 1 + 0.14 * Math.sin(age * 0.5);
        double radius = 0.27 * pulse;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(radius, radius * 1.15, radius), 9, 14,
                0.055f, 0.025f, 0.09f, 0.94f * fade, age * 0.17);
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(radius * 0.48, radius * 0.64, radius * 0.48),
                8, 12, 0.48f, 0.07f, 0.22f, 0.85f * fade, -age * 0.27);
        for (int crack = 0; crack < 7; crack++) {
            double angle = crack * Math.PI * 2 / 7 + age * 0.04;
            Vec3 start = center.add(Math.cos(angle) * radius * 0.3, Math.sin(angle * 2) * radius * 0.7,
                    Math.sin(angle) * radius * 0.3);
            Vec3 end = center.add(Math.cos(angle + 0.6) * radius * 1.25,
                    Math.sin(angle * 1.4) * radius * 1.1, Math.sin(angle + 0.6) * radius * 1.25);
            bolt(m, out, start, end, crack * 421L + (long) age / 3, 4, radius * 0.38, 0.92f * fade);
        }
        for (int shard = 0; shard < 9; shard++) {
            double angle = shard * Math.PI * 2 / 9 - age * (shard % 2 == 0 ? 0.09 : -0.12);
            Vec3 axis = new Vec3(Math.cos(angle), Math.sin(age * 0.13 + shard) * 0.58,
                    Math.sin(angle)).normalize();
            Vec3 point = center.add(axis.scale(radius * (1.42 + (shard % 3) * 0.22)));
            crystal(m, out, point, axis, 0.24, 0.085, 0.3f, 0.04f, 0.42f, 0.83f * fade);
        }
        for (int tail = 0; tail < 4; tail++) {
            double angle = age * 0.16 + tail * Math.PI / 2;
            Vec3 end = center.subtract(destination.subtract(Vec3.ZERO).normalize().scale(0.24 + tail * 0.08))
                    .add(Math.cos(angle) * 0.2, Math.sin(angle * 1.4) * 0.16, Math.sin(angle) * 0.2);
            PrayerVfxGeometry.taperedTube(m, out, center, end, 0.065, 0.003,
                    0.24f, 0.05f, 0.34f, 0.48f * fade);
        }
        if (travel > 0.94) {
            double burst = smooth(0.94, 1, travel);
            PrayerVfxGeometry.wireSphere(m, out, destination, 0.22 + burst * 0.8, 12,
                    age * 0.12, 0.55f, 0.08f, 0.45f, (float) (0.65 * (1 - burst) * fade), 0.02);
        }
    }

    // 12. LIFE_DRAIN — a living tether returns moving blood-soul motes from the victim to the caster.
    private static void lifeDrain(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 3, 6);
        double connect = smooth(2, 8, age);
        Vec3 direction = safeNormalize(destination, new Vec3(0, 0, -1));
        Vec3 side = basis(direction)[0];
        Vec3 previous = Vec3.ZERO;
        int segments = 28;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            double sway = Math.sin(t * Math.PI * 3 + age * 0.16) * 0.16;
            Vec3 point = destination.scale(t).add(side.scale(sway * Math.sin(age * 0.08 + t * 2)));
            double width = 0.065 + Math.sin(t * Math.PI) * 0.075;
            PrayerVfxGeometry.tube(m, out, previous, point, width * 2.2,
                    0.22f, 0.025f, 0.13f, 0.19f * fade * (float) connect);
            PrayerVfxGeometry.tube(m, out, previous, point, width,
                    0.58f, 0.055f, 0.19f, 0.68f * fade * (float) connect);
            PrayerVfxGeometry.tube(m, out, previous, point, width * 0.28,
                    1.0f, 0.22f, 0.28f, 0.86f * fade * (float) connect);
            previous = point;
        }
        for (int mote = 0; mote < 9; mote++) {
            double t = (mote / 9.0 + age * 0.075) % 1.0;
            Vec3 point = destination.scale(1 - t).add(side.scale(Math.sin(t * Math.PI * 5 + age * 0.12) * 0.13));
            double size = 0.055 + 0.02 * Math.sin(age * 0.3 + mote);
            PrayerVfxGeometry.ellipsoid(m, out, point, new Vec3(size, size * 1.25, size), 6, 9,
                    1.0f, 0.34f, 0.28f, fade * (float) connect, age * 0.22 + mote);
            PrayerVfxGeometry.wireSphere(m, out, point, size * 1.7, 7, -age * 0.18 + mote,
                    0.72f, 0.09f, 0.3f, 0.62f * fade * (float) connect, 0.009);
        }
        PrayerVfxGeometry.wireSphere(m, out, Vec3.ZERO, 0.2 + connect * 0.18, 10, age * 0.2,
                0.83f, 0.15f, 0.3f, 0.72f * fade, 0.015);
        PrayerVfxGeometry.ellipsoid(m, out, destination, new Vec3(0.36, 0.55, 0.36), 8, 12,
                0.47f, 0.07f, 0.19f, 0.14f * fade * (float) connect, age * 0.12);
    }

    // 13. CURSE_OF_WEAKNESS — a closing hex seals above the victim as two spectral chains cinch inward.
    private static void curseOfWeakness(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 3, 7);
        double travel = smooth(2, duration * 0.32, age);
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        double settle = smooth(duration * 0.25, duration * 0.43, age);
        ringPlane(m, out, center.add(0, 0.08, 0), UP, 0.38 + settle * 0.72,
                0.023, age * 0.06, 0.42f, 0.08f, 0.48f, fade);
        runeHalo(m, out, center.add(0, 2.55, 0), 0.72 * (0.65 + settle * 0.35),
                -age * 0.035, fade, 0.68f, 0.18f, 0.74f);
        for (int chain = 0; chain < 2; chain++) {
            double phase = age * 0.14 + chain * Math.PI;
            Vec3 previous = center.add(Math.cos(phase) * 0.63, 0.18, Math.sin(phase) * 0.63);
            for (int step = 1; step <= 30; step++) {
                double t = step / 30.0;
                double y = 0.18 + t * 2.05;
                double angle = phase + t * Math.PI * (3.2 + chain * 0.4);
                Vec3 point = center.add(Math.cos(angle) * (0.62 - 0.12 * settle), y,
                        Math.sin(angle) * (0.62 - 0.12 * settle));
                PrayerVfxGeometry.tube(m, out, previous, point, 0.045,
                        0.17f, 0.035f, 0.25f, 0.72f * fade);
                if (step % 5 == 0) crystal(m, out, point, point.subtract(previous), 0.15, 0.045,
                        0.65f, 0.17f, 0.76f, 0.82f * fade);
                previous = point;
            }
        }
        for (int rune = 0; rune < 6; rune++) {
            double angle = rune * Math.PI / 3 - age * 0.04;
            Vec3 point = center.add(Math.cos(angle) * 0.78, 0.6 + (rune % 3) * 0.58,
                    Math.sin(angle) * 0.78);
            runeHalo(m, out, point, 0.18, angle, 0.75f * fade, 0.72f, 0.24f, 0.84f);
        }
    }

    // 14. CORRUPTION — a projectile implants a fractured core; angular veins crawl across the impact.
    private static void corruption(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 3, 6);
        double travel = smooth(2, duration * 0.46, age);
        Vec3 point = Vec3.ZERO.lerp(destination, travel);
        double emerge = smooth(0.36, 0.55, travel);
        double radius = 0.12 + emerge * (0.5 + Math.sin(age * 0.22) * 0.04);
        PrayerVfxGeometry.ellipsoid(m, out, point, new Vec3(radius, radius * 1.18, radius), 8, 14,
                0.065f, 0.025f, 0.11f, 0.9f * fade, age * 0.17);
        PrayerVfxGeometry.wireSphere(m, out, point, radius * 1.35, 10, -age * 0.11,
                0.66f, 0.07f, 0.26f, 0.72f * fade, 0.018);
        for (int vein = 0; vein < 9; vein++) {
            double angle = vein * Math.PI * 2 / 9 + age * 0.018;
            Vec3 start = point.add(Math.cos(angle) * radius * 0.35, Math.sin(angle * 1.7) * radius * 0.35,
                    Math.sin(angle) * radius * 0.35);
            Vec3 end = point.add(Math.cos(angle + 0.26) * (radius * 1.85),
                    Math.sin(angle * 2.1 + age * 0.1) * radius * 1.4,
                    Math.sin(angle + 0.26) * (radius * 1.85));
            bolt(m, out, start, end, vein * 67L + (long) age / 4, 5, 0.11, 0.85f * fade * (float) emerge);
            if (vein % 2 == 0) crystal(m, out, end, end.subtract(start), 0.3, 0.11,
                    0.32f, 0.05f, 0.34f, 0.74f * fade * (float) emerge);
        }
        if (travel > 0.92) {
            double implosion = smooth(0.92, 1, travel);
            ringPlane(m, out, destination, UP, 0.2 + implosion * 1.1, 0.025,
                    -age * 0.05, 0.75f, 0.12f, 0.38f, (float) (0.72 * (1 - implosion) * fade));
        }
    }

    // 15. SHADOWSTEP — the departure silhouette collapses while a second one re-forms inside a portal.
    private static void shadowstep(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 2, 5);
        double vanish = smooth(0, 0.42, age / duration);
        double appear = smooth(0.42, 0.78, age / duration);
        portal(m, out, Vec3.ZERO.add(0, 0.88, 0), 0.72 * (1 + vanish * 0.45),
                age * 0.035, fade * (float) (1 - vanish * 0.6), 0.28f, 0.06f, 0.48f);
        portal(m, out, destination.add(0, 0.88, 0), 0.72 * (0.45 + appear * 0.55),
                -age * 0.04, fade * (float) appear, 0.36f, 0.08f, 0.64f);
        silhouette(m, out, Vec3.ZERO.add(0, -0.95, 0), (float) (1 - vanish), fade,
                0.12f, 0.035f, 0.22f);
        silhouette(m, out, destination.add(0, 0.1, 0), (float) appear, fade,
                0.2f, 0.055f, 0.34f);
        Vec3 path = destination.subtract(Vec3.ZERO);
        for (int shard = 0; shard < 11; shard++) {
            double t = (shard / 11.0 + age * 0.06) % 1.0;
            double wobble = Math.sin(t * Math.PI * 5 + age * 0.16) * 0.28;
            Vec3 point = path.scale(t).add(Math.cos(age * 0.09 + shard) * wobble,
                    Math.sin(age * 0.14 + shard * 2.1) * 0.38, Math.sin(age * 0.1 + shard) * wobble);
            crystal(m, out, point, safeNormalize(path, UP), 0.18, 0.065,
                    0.54f, 0.13f, 0.82f, 0.72f * fade * (float) (1 - Math.abs(0.5 - t)));
        }
    }

    // 16. SOUL_HARVEST — separate spectral figures spiral inward and dissolve into the caster.
    private static void soulHarvest(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 4, 8);
        circleSigil(m, out, new Vec3(0, 0.06, 0), 2.3, age * -0.012, 0.52f * fade,
                0.55f, 0.13f, 0.76f);
        for (int soul = 0; soul < 7; soul++) {
            double phase = soul * Math.PI * 2 / 7 + age * (soul % 2 == 0 ? 0.045 : -0.052);
            double progress = (age / 44.0 + soul * 0.137) % 1.0;
            double radius = 2.25 * (1 - progress) + 0.18;
            double y = 0.35 + (soul % 4) * 0.52 + progress * 1.3;
            Vec3 center = new Vec3(Math.cos(phase) * radius, y, Math.sin(phase) * radius);
            double scale = 0.16 + (1 - progress) * 0.09;
            soulFigure(m, out, center, scale, phase, fade * (float) (1 - progress * 0.35));
            Vec3 mote = center.add(0, -0.18 * scale, 0);
            PrayerVfxGeometry.taperedTube(m, out, center, mote, 0.08 * scale, 0.002,
                    0.47f, 0.14f, 0.8f, 0.65f * fade);
            PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(0.08, 0.12, 0.08), 6, 8,
                    0.75f, 0.28f, 0.96f, 0.36f * fade, phase);
        }
        PrayerVfxGeometry.wireSphere(m, out, new Vec3(0, 1, 0), 0.45 + Math.sin(age * 0.12) * 0.06,
                12, age * 0.06, 0.68f, 0.2f, 0.86f, 0.34f * fade, 0.014);
    }

    // 17. DARK_PACT — contract circle, floating glyph stones and binding chains converging on the caster.
    private static void darkPact(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 6, 8);
        circleSigil(m, out, new Vec3(0, 0.045, 0), 1.9, age * -0.018, fade,
                0.41f, 0.045f, 0.2f);
        ringPlane(m, out, new Vec3(0, 0.07, 0), UP, 1.35, 0.026, age * 0.035,
                0.78f, 0.1f, 0.28f, 0.85f * fade);
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 1.02, 0), new Vec3(0.52, 0.86, 0.52),
                9, 14, 0.035f, 0.018f, 0.055f, 0.42f * fade, age * 0.04);
        PrayerVfxGeometry.wireSphere(m, out, new Vec3(0, 1.05, 0), 0.78, 14, -age * 0.026,
                0.49f, 0.055f, 0.2f, 0.75f * fade, 0.017);
        for (int seal = 0; seal < 6; seal++) {
            double angle = seal * Math.PI / 3 - age * 0.023;
            Vec3 p = new Vec3(Math.cos(angle) * 1.42, 0.62 + (seal % 3) * 0.52, Math.sin(angle) * 1.42);
            crystal(m, out, p, new Vec3(Math.cos(angle), 0.25, Math.sin(angle)), 0.48, 0.16,
                    0.32f, 0.035f, 0.15f, 0.9f * fade);
            runeHalo(m, out, p, 0.27, angle, 0.92f * fade, 0.74f, 0.12f, 0.28f);
            List<Vec3> chain = jagged(p, new Vec3(0, 1.05, 0), 5, seal * 709L + (long) age / 4, 0.1);
            for (int i = 0; i < chain.size() - 1; i++) {
                PrayerVfxGeometry.tube(m, out, chain.get(i), chain.get(i + 1), 0.032,
                        0.19f, 0.025f, 0.12f, 0.72f * fade);
            }
        }
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 2.15, 0), new Vec3(0.22, 0.3, 0.22),
                7, 10, 0.71f, 0.09f, 0.24f, 0.72f * fade, age * 0.09);
    }

    // 18. SOUL_EXPLOSION — a sealed black core releases orbiting souls as independent radial fragments.
    private static void soulExplosion(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 3, 9);
        double charge = 1 - smooth(0.28, 0.48, age / duration);
        double burst = smooth(0.34, 0.72, age / duration);
        Vec3 center = new Vec3(0, 1.0, 0);
        double radius = 0.18 + charge * 0.55;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(radius, radius, radius), 9, 14,
                0.04f, 0.018f, 0.07f, 0.95f * fade, age * 0.1);
        PrayerVfxGeometry.wireSphere(m, out, center, radius * 1.34, 14, age * 0.07,
                0.64f, 0.12f, 0.88f, 0.78f * fade, 0.018);
        for (int soul = 0; soul < 12; soul++) {
            double angle = soul * Math.PI * 2 / 12 + age * 0.055;
            Vec3 dir = new Vec3(Math.cos(angle), Math.sin(angle * 1.7) * 0.65, Math.sin(angle)).normalize();
            double orbit = 0.72 + (soul % 3) * 0.14;
            double expelled = burst * (1.25 + (soul % 4) * 0.42);
            Vec3 point = center.add(dir.scale(orbit + expelled));
            soulFigure(m, out, point, 0.13 + (soul % 3) * 0.018, angle + age * 0.08,
                    fade * (float) (0.9 - burst * 0.25));
            PrayerVfxGeometry.taperedTube(m, out, center, point, 0.035, 0.002,
                    0.57f, 0.11f, 0.78f, 0.42f * fade * (float) burst);
        }
        for (int ring = 0; ring < 3; ring++) {
            double r = 0.2 + burst * (1.3 + ring * 0.9);
            ringPlane(m, out, center, ring == 0 ? UP : ring == 1 ? new Vec3(1, 0.3, 0) : new Vec3(0.2, 0, 1),
                    r, 0.025, age * (ring % 2 == 0 ? 0.04 : -0.05), 0.72f, 0.17f, 0.91f,
                    (float) (0.62 * (1 - burst * 0.45) * fade));
        }
    }

    // 19. UNHOLY_GROUND — the floor fractures into a dark sigil with uneven erupting thorns.
    private static void unholyGround(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 7, 11);
        double growth = smooth(0, 11, age);
        double radius = 3.25 * growth;
        ringPlane(m, out, new Vec3(0, 0.055, 0), UP, radius, 0.04, age * -0.013,
                0.38f, 0.035f, 0.18f, 0.88f * fade);
        ringPlane(m, out, new Vec3(0, 0.07, 0), UP, radius * 0.64, 0.018, age * 0.025,
                0.61f, 0.08f, 0.5f, 0.75f * fade);
        for (int crack = 0; crack < 14; crack++) {
            double angle = crack * Math.PI * 2 / 14 + age * 0.006;
            double reach = radius * (0.55 + (crack % 4) * 0.12);
            Vec3 from = new Vec3(Math.cos(angle) * 0.3, 0.075, Math.sin(angle) * 0.3);
            Vec3 knee = new Vec3(Math.cos(angle + 0.12) * reach * 0.55, 0.07,
                    Math.sin(angle + 0.12) * reach * 0.55);
            Vec3 end = new Vec3(Math.cos(angle - 0.08) * reach, 0.06,
                    Math.sin(angle - 0.08) * reach);
            PrayerVfxGeometry.tube(m, out, from, knee, 0.026, 0.12f, 0.025f, 0.14f, 0.84f * fade);
            PrayerVfxGeometry.tube(m, out, knee, end, 0.018, 0.59f, 0.06f, 0.47f, 0.82f * fade);
            if (crack % 2 == 0) {
                Vec3 thorn = knee.add(0, 0.3 + (crack % 3) * 0.2, 0);
                crystal(m, out, thorn, thorn.subtract(knee), 0.58, 0.19,
                        0.19f, 0.025f, 0.2f, 0.82f * fade);
            }
        }
        for (int mote = 0; mote < 9; mote++) {
            double angle = mote * Math.PI * 2 / 9 + age * 0.014;
            double r = radius * (0.35 + (mote % 4) * 0.13);
            Vec3 point = new Vec3(Math.cos(angle) * r, 0.22 + Math.sin(age * 0.1 + mote) * 0.12,
                    Math.sin(angle) * r);
            PrayerVfxGeometry.ellipsoid(m, out, point, new Vec3(0.08, 0.15, 0.08), 6, 8,
                    0.58f, 0.08f, 0.48f, 0.78f * fade, angle);
        }
    }

    // 20. APOCALYPSE — a huge torn sky-rift sheds black monoliths and sends red fractures to the ground.
    private static void apocalypse(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 10, 12);
        double open = smooth(0, 18, age);
        double close = smooth(duration * 0.76, duration, age);
        double half = 4.8 * open * (1 - close * 0.35);
        double y = 12.0;
        List<Vec3> left = jagged(new Vec3(-half, y, -0.2), new Vec3(0, y + 0.7, 0.18), 11,
                0xA90C1L + (long) age / 5, 0.42);
        List<Vec3> right = jagged(new Vec3(half, y, 0.2), new Vec3(0, y + 0.7, -0.18), 11,
                0xA90C2L + (long) age / 5, 0.42);
        for (int i = 0; i < left.size() - 1; i++) {
            PrayerVfxGeometry.tube(m, out, left.get(i), left.get(i + 1), 0.14,
                    0.2f, 0.015f, 0.045f, 0.74f * fade);
            PrayerVfxGeometry.tube(m, out, left.get(i), left.get(i + 1), 0.043,
                    0.94f, 0.12f, 0.18f, 0.94f * fade);
            PrayerVfxGeometry.tube(m, out, right.get(i), right.get(i + 1), 0.14,
                    0.2f, 0.015f, 0.045f, 0.74f * fade);
            PrayerVfxGeometry.tube(m, out, right.get(i), right.get(i + 1), 0.043,
                    0.94f, 0.12f, 0.18f, 0.94f * fade);
        }
        ringPlane(m, out, new Vec3(0, y, 0), new Vec3(1, 0.05, 0), half * 0.9,
                0.045, age * 0.02, 0.62f, 0.04f, 0.22f, 0.76f * fade);
        for (int shard = 0; shard < 12; shard++) {
            double angle = shard * Math.PI * 2 / 12 - age * 0.012;
            double r = half * (0.65 + (shard % 3) * 0.2);
            Vec3 center = new Vec3(Math.cos(angle) * r, y + Math.sin(angle * 2.5 + age * 0.06),
                    Math.sin(angle) * 1.4);
            crystal(m, out, center, new Vec3(Math.cos(angle) * 0.3, 1, Math.sin(angle)),
                    0.75, 0.24, 0.11f, 0.025f, 0.14f, 0.92f * fade);
        }
        for (int fissure = 0; fissure < 5; fissure++) {
            double angle = fissure * Math.PI * 2 / 5 + age * 0.004;
            Vec3 sky = new Vec3(Math.cos(angle) * 0.25, y - 0.2, Math.sin(angle) * 0.25);
            Vec3 ground = new Vec3(Math.cos(angle) * (1.2 + fissure % 2), 0.07,
                    Math.sin(angle) * (1.2 + fissure % 2));
            PrayerVfxGeometry.taperedTube(m, out, sky, ground, 0.18, 0.04,
                    0.55f, 0.03f, 0.18f, 0.32f * fade);
            bolt(m, out, sky, ground, fissure * 977L + (long) age / 3, 8, 0.22, 0.92f * fade);
        }
    }

    // 21. SPARK — a short, almost instantaneous arc with tiny branching discharges.
    private static void spark(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 1, 3);
        double travel = smooth(1, duration * 0.4, age);
        Vec3 tip = Vec3.ZERO.lerp(destination, travel);
        bolt(m, out, Vec3.ZERO, tip, (long) age / 2, 12, 0.13 + travel * 0.06, fade);
        for (int branch = 0; branch < 5; branch++) {
            double t = 0.25 + branch * 0.14;
            if (t > travel) continue;
            Vec3 root = destination.scale(t);
            double angle = branch * 2.4 + age * 0.16;
            Vec3 end = root.add(Math.cos(angle) * 0.38, Math.sin(angle * 1.7) * 0.27,
                    Math.sin(angle) * 0.38);
            bolt(m, out, root, end, branch * 53L + (long) age / 2, 4, 0.07, 0.82f * fade);
        }
        PrayerVfxGeometry.ellipsoid(m, out, Vec3.ZERO, new Vec3(0.11, 0.11, 0.11), 7, 10,
                0.35f, 0.78f, 1.0f, 0.8f * fade, age * 0.2);
        if (travel > 0.8) {
            PrayerVfxGeometry.wireSphere(m, out, destination, 0.16 + travel * 0.2, 8, age * 0.18,
                    0.42f, 0.84f, 1.0f, fade * 0.66f, 0.012);
        }
    }

    // 23. CHAIN_LIGHTNING — the main discharge branches into sequential, visibly thinning jumps.
    private static void chainLightning(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 2, 5);
        double progress = smooth(2, duration * 0.42, age);
        Vec3 first = destination.scale(progress);
        bolt(m, out, Vec3.ZERO, first, 0xC4A1L + (long) age / 2, 17, 0.25, fade);
        for (int hop = 0; hop < 3; hop++) {
            double startPhase = 0.25 + hop * 0.2;
            double grow = smooth(startPhase, startPhase + 0.16, progress);
            if (grow <= 0) continue;
            double angle = hop * Math.PI * 2 / 3 + age * 0.018;
            Vec3 from = destination.add(0, 0.35, 0).add(Math.cos(angle) * hop * 0.22,
                    0, Math.sin(angle) * hop * 0.22);
            Vec3 to = from.add(Math.cos(angle) * (1.5 + hop * 0.48) * grow,
                    (hop % 2 == 0 ? 1 : -1) * 0.65 * grow,
                    Math.sin(angle) * (1.5 + hop * 0.48) * grow);
            bolt(m, out, from, to, hop * 739L + (long) age / 2,
                    8 - hop, 0.16 * (1 - hop * 0.2), (float) (fade * (1 - hop * 0.17)));
            PrayerVfxGeometry.wireSphere(m, out, to, 0.12 + grow * 0.12, 7,
                    age * 0.12 + hop, 0.62f, 0.88f, 1.0f, fade * (float) grow, 0.012);
        }
        PrayerVfxGeometry.ellipsoid(m, out, destination, new Vec3(0.18, 0.23, 0.18), 7, 10,
                0.8f, 0.94f, 1.0f, 0.75f * fade, age * 0.3);
    }

    // 24. THUNDERSTORM — a layered storm cell builds overhead and sends staggered bolts to its ground.
    private static void thunderstorm(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 10, 12);
        double build = smooth(0, 14, age);
        for (int lobe = 0; lobe < 7; lobe++) {
            double angle = lobe * Math.PI * 2 / 7;
            Vec3 center = new Vec3(Math.cos(angle) * (1.1 + lobe % 3 * 0.35),
                    10.6 + Math.sin(age * 0.08 + lobe) * 0.15,
                    Math.sin(angle) * (0.8 + lobe % 2 * 0.4));
            double size = (0.74 + (lobe % 3) * 0.13) * build;
            PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(size, size * 0.42, size * 0.72),
                    7, 12, 0.12f, 0.2f, 0.34f, 0.72f * fade, angle);
        }
        ringPlane(m, out, new Vec3(0, 10.35, 0), UP, 2.3, 0.045, age * 0.018,
                0.21f, 0.38f, 0.63f, 0.65f * fade);
        for (int strand = 0; strand < 6; strand++) {
            double angle = strand * Math.PI / 3 + age * 0.02;
            Vec3 from = new Vec3(Math.cos(angle) * 1.55, 10.7, Math.sin(angle) * 1.2);
            Vec3 to = new Vec3(Math.cos(angle + 0.42) * 1.3, 10.55, Math.sin(angle + 0.42) * 1.3);
            bolt(m, out, from, to, strand * 331L + (long) age / 2, 5, 0.16, 0.8f * fade);
        }
        for (int strike = 0; strike < 4; strike++) {
            double phase = (age - 8 - strike * 11) / 9.0;
            double grow = smooth(0, 0.34, phase) * (1 - smooth(0.64, 1, phase));
            if (grow <= 0) continue;
            double angle = strike * 2.4 + 0.35;
            Vec3 ground = new Vec3(Math.cos(angle) * (0.9 + strike % 2), 0.1,
                    Math.sin(angle) * (0.9 + strike % 2));
            Vec3 start = ground.add(0, 10.2 * grow, 0);
            bolt(m, out, start, ground, strike * 1301L + (long) age / 2,
                    16, 0.32, fade * (float) grow);
            ringPlane(m, out, ground, UP, 0.2 + grow * 1.05, 0.024, age * 0.08,
                    0.42f, 0.78f, 1.0f, 0.7f * fade * (float) grow);
        }
    }

    // 25. STORM_JUDGMENT — a celestial electric seal focuses into a single broad, branching verdict.
    private static void stormJudgment(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 8, 11);
        double charge = smooth(0, 18, age);
        double strike = smooth(0.38, 0.58, age / duration);
        Vec3 crown = new Vec3(0, 14, 0);
        ringPlane(m, out, crown, UP, 3.6 * (0.5 + charge * 0.5), 0.055,
                age * 0.026, 0.35f, 0.74f, 1.0f, 0.87f * fade);
        ringPlane(m, out, crown, new Vec3(1, 0.2, 0), 2.7, 0.035,
                -age * 0.04, 0.86f, 0.94f, 1.0f, 0.92f * fade);
        ringPlane(m, out, crown, new Vec3(0.15, 0, 1), 1.75, 0.025,
                age * 0.06, 0.5f, 0.82f, 1.0f, 0.8f * fade);
        runeHalo(m, out, crown, 1.0, age * -0.03, fade, 0.76f, 0.91f, 1.0f);
        PrayerVfxGeometry.ellipsoid(m, out, crown, new Vec3(0.36, 0.5, 0.36), 8, 12,
                0.55f, 0.86f, 1.0f, 0.7f * fade, age * 0.12);
        Vec3 ground = new Vec3(0, 0.12, 0);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 14 * strike, 0), ground,
                0.56, 0.16, 0.16f, 0.58f, 1.0f, 0.26f * fade);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 14 * strike, 0), ground,
                0.22, 0.055, 0.65f, 0.91f, 1.0f, 0.9f * fade);
        for (int branch = 0; branch < 8; branch++) {
            double angle = branch * Math.PI / 4 + age * 0.016;
            Vec3 end = ground.add(Math.cos(angle) * (1.4 + branch % 3 * 0.52) * strike,
                    (branch % 3) * 0.24, Math.sin(angle) * (1.4 + branch % 3 * 0.52) * strike);
            bolt(m, out, ground, end, branch * 449L + (long) age / 2, 7, 0.17,
                    0.9f * fade * (float) strike);
        }
        ringPlane(m, out, ground, UP, 0.2 + strike * 2.5, 0.04, age * 0.07,
                0.35f, 0.78f, 1.0f, 0.78f * fade * (float) (1 - strike * 0.6));
    }

    // 26. FIREBALL — three independently moving thermal shells and a turbulent tail surround the projectile.
    private static void fireball(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 2, 4);
        double travel = smooth(2, duration * 0.58, age);
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        double pulse = 1 + 0.12 * Math.sin(age * 0.5);
        double radius = 0.34 * pulse;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(radius * 1.1, radius, radius), 9, 14,
                0.92f, 0.19f, 0.025f, 0.8f * fade, age * 0.2);
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(radius * 0.75, radius * 0.76, radius * 0.75),
                8, 12, 1.0f, 0.48f, 0.04f, 0.9f * fade, -age * 0.23);
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(radius * 0.42, radius * 0.5, radius * 0.42),
                7, 10, 1.0f, 0.91f, 0.37f, 0.96f * fade, age * 0.33);
        Vec3 back = safeNormalize(destination.scale(-1), new Vec3(0, 0, 1));
        Vec3[] frame = basis(back);
        for (int tongue = 0; tongue < 7; tongue++) {
            double angle = tongue * Math.PI * 2 / 7 + age * 0.13;
            Vec3 side = frame[0].scale(Math.cos(angle)).add(frame[1].scale(Math.sin(angle)));
            Vec3 start = center.add(side.scale(radius * 0.35));
            Vec3 end = center.add(back.scale(0.4 + (tongue % 3) * 0.2))
                    .add(side.scale(radius * (1.8 + 0.45 * Math.sin(age * 0.4 + tongue))));
            PrayerVfxGeometry.taperedTube(m, out, start, end, 0.11, 0.006,
                    1.0f, 0.24f + (tongue % 2) * 0.16f, 0.025f, 0.84f * fade);
            PrayerVfxGeometry.taperedTube(m, out, start, end, 0.035, 0.002,
                    1.0f, 0.8f, 0.23f, 0.92f * fade);
        }
        if (travel > 0.9) flameBurstImpact(m, out, destination, age, duration, fade);
    }

    // 27. FLAME_BURST — a compressed ember opens as an asymmetric three-dimensional fire flower.
    private static void flameBurst(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 2, 6);
        double open = smooth(4, duration * 0.52, age);
        Vec3 center = new Vec3(0, 1.0, 0);
        double core = 0.14 + (1 - open) * 0.18;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(core, core, core), 8, 12,
                1.0f, 0.56f, 0.08f, 0.95f * fade, age * 0.2);
        for (int petal = 0; petal < 12; petal++) {
            double angle = petal * Math.PI / 6 + age * 0.012;
            Vec3 dir = new Vec3(Math.cos(angle), (petal % 3 - 1) * 0.34 + 0.15, Math.sin(angle)).normalize();
            Vec3 start = center.add(dir.scale(0.08));
            Vec3 knee = center.add(dir.scale(0.5 + open * (0.5 + (petal % 3) * 0.22)))
                    .add(0, Math.sin(open * Math.PI) * (0.22 + petal % 2 * 0.16), 0);
            Vec3 tip = center.add(dir.scale(0.72 + open * (1.0 + (petal % 4) * 0.25)));
            flamePetal(m, out, start, knee, tip, 0.2 - (petal % 3) * 0.025,
                    0.86f * fade * (float) (1 - open * 0.2));
        }
        ringPlane(m, out, center, UP, 0.24 + open * 1.5, 0.035, age * 0.045,
                1.0f, 0.37f, 0.045f, 0.74f * fade * (float) (1 - open * 0.4));
    }

    // 28. BURNING_GROUND — a scorched plate, branching molten fractures and separated ember chimneys.
    private static void burningGround(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 7, 10);
        double expansion = smooth(0, 10, age);
        double radius = 3.1 * expansion;
        ringPlane(m, out, new Vec3(0, 0.04, 0), UP, radius, 0.045, age * 0.006,
                0.65f, 0.12f, 0.025f, 0.8f * fade);
        ringPlane(m, out, new Vec3(0, 0.055, 0), UP, radius * 0.58, 0.025, -age * 0.012,
                1.0f, 0.42f, 0.04f, 0.7f * fade);
        for (int crack = 0; crack < 12; crack++) {
            double angle = crack * Math.PI / 6 + Math.sin(age * 0.025 + crack) * 0.04;
            double reach = radius * (0.54 + (crack % 4) * 0.12);
            Vec3 start = new Vec3(Math.cos(angle) * 0.25, 0.065, Math.sin(angle) * 0.25);
            Vec3 knee = new Vec3(Math.cos(angle + 0.12) * reach * 0.52, 0.06,
                    Math.sin(angle + 0.12) * reach * 0.52);
            Vec3 end = new Vec3(Math.cos(angle - 0.08) * reach, 0.055, Math.sin(angle - 0.08) * reach);
            PrayerVfxGeometry.tube(m, out, start, knee, 0.038, 0.72f, 0.14f, 0.018f, 0.8f * fade);
            PrayerVfxGeometry.tube(m, out, start, knee, 0.012, 1.0f, 0.79f, 0.18f, fade);
            PrayerVfxGeometry.tube(m, out, knee, end, 0.027, 0.95f, 0.24f, 0.025f, 0.76f * fade);
        }
        for (int vent = 0; vent < 8; vent++) {
            double angle = vent * Math.PI / 4 + age * 0.004;
            Vec3 base = new Vec3(Math.cos(angle) * radius * 0.68, 0.12, Math.sin(angle) * radius * 0.68);
            Vec3 tip = base.add(Math.cos(angle) * 0.1, 0.55 + (vent % 3) * 0.24, Math.sin(angle) * 0.1);
            flamePetal(m, out, base, base.add(0, tip.y * 0.55, 0), tip, 0.14,
                    0.82f * fade);
        }
    }

    // 29. INFERNO — a rising fire vortex with six independent corkscrew tongues and a hollow eye.
    private static void inferno(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 6, 10);
        double rise = smooth(0, 12, age);
        double height = 6.2 * rise;
        ringPlane(m, out, new Vec3(0, 0.08, 0), UP, 1.6 * rise, 0.04,
                age * 0.04, 1.0f, 0.31f, 0.045f, 0.85f * fade);
        for (int strand = 0; strand < 7; strand++) {
            Vec3 previousOuter = null;
            Vec3 previousCore = null;
            for (int step = 0; step <= 28; step++) {
                double t = step / 28.0;
                double y = 0.1 + t * height;
                double angle = age * (0.09 + strand * 0.006) + t * Math.PI * 4.3 + strand * Math.PI * 2 / 7;
                double radius = (0.55 + t * 1.15) * (0.75 + 0.25 * Math.sin(age * 0.13 + strand));
                Vec3 point = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                if (previousOuter != null) {
                    double width = (0.15 + 0.07 * (strand % 3)) * (1 - t * 0.72);
                    PrayerVfxGeometry.tube(m, out, previousOuter, point, width * 1.8,
                            0.76f, 0.08f, 0.012f, 0.22f * fade);
                    PrayerVfxGeometry.tube(m, out, previousOuter, point, width,
                            1.0f, 0.22f + (strand % 2) * 0.12f, 0.018f, 0.76f * fade);
                    PrayerVfxGeometry.tube(m, out, previousCore, point, width * 0.28,
                            1.0f, 0.78f, 0.2f, 0.92f * fade);
                }
                previousOuter = point;
                previousCore = point;
            }
        }
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 1.1, 0), new Vec3(0.47, 1.2, 0.47),
                8, 14, 0.12f, 0.025f, 0.015f, 0.7f * fade, age * 0.08);
        for (int ring = 0; ring < 3; ring++) {
            double y = 0.24 + ring * (height * 0.3);
            ringPlane(m, out, new Vec3(0, y, 0), UP, 0.7 + ring * 0.43,
                    0.022, age * (ring % 2 == 0 ? 0.05 : -0.04), 1.0f, 0.55f, 0.12f, 0.75f * fade);
        }
    }

    // 31. ICE_SHARD — a long, rotating, glass-faceted crystal is the projectile itself.
    private static void iceShard(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 2, 4);
        double travel = smooth(2, duration * 0.58, age);
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        Vec3 direction = safeNormalize(destination, new Vec3(0, 0, -1));
        crystal(m, out, center, direction, 1.22, 0.29, 0.28f, 0.75f, 1.0f, 0.75f * fade);
        crystal(m, out, center, direction, 0.92, 0.14, 0.86f, 0.98f, 1.0f, 0.92f * fade);
        Vec3[] frame = basis(direction);
        for (int shard = 0; shard < 4; shard++) {
            double angle = shard * Math.PI / 2 + age * 0.14;
            Vec3 axis = frame[0].scale(Math.cos(angle)).add(frame[1].scale(Math.sin(angle)));
            Vec3 point = center.add(axis.scale(0.24));
            crystal(m, out, point, direction.add(axis.scale(0.34)), 0.55, 0.09,
                    0.48f, 0.89f, 1.0f, 0.82f * fade);
        }
        PrayerVfxGeometry.taperedTube(m, out, center.subtract(direction.scale(0.48)),
                center.subtract(direction.scale(1.4)), 0.18, 0.005,
                0.22f, 0.72f, 1.0f, 0.45f * fade);
        if (travel > 0.92) {
            double impact = smooth(0.92, 1, travel);
            for (int shard = 0; shard < 6; shard++) {
                double angle = shard * Math.PI / 3;
                Vec3 dir = new Vec3(Math.cos(angle), 0.6 + shard % 2 * 0.4, Math.sin(angle));
                crystal(m, out, destination.add(dir.scale(0.2 + impact * 0.65)), dir,
                        0.5 * (1 - impact * 0.35), 0.1, 0.42f, 0.84f, 1.0f, 0.85f * fade);
            }
            ringPlane(m, out, destination, UP, 0.18 + impact * 0.7, 0.025,
                    age * 0.08, 0.51f, 0.87f, 1.0f, (float) (0.7 * (1 - impact) * fade));
        }
    }

    // 32. FROST_NOVA — crystalline growth erupts at the caster as a low expanding ice wall.
    private static void frostNova(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 3, 8);
        double wave = smooth(4, duration * 0.64, age);
        Vec3 center = new Vec3(0, 0.72, 0);
        PrayerVfxGeometry.wireSphere(m, out, center, 0.45 + wave * 2.2, 16, age * 0.025,
                0.27f, 0.77f, 1.0f, 0.32f * fade * (float) (1 - wave * 0.35), 0.018);
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(0.55, 0.65, 0.55), 8, 12,
                0.55f, 0.88f, 1.0f, 0.13f * fade, age * 0.04);
        ringPlane(m, out, new Vec3(0, 0.08, 0), UP, 0.25 + wave * 4.0, 0.07,
                age * 0.025, 0.46f, 0.84f, 1.0f, 0.8f * fade * (float) (1 - wave * 0.65));
        for (int shard = 0; shard < 16; shard++) {
            double angle = shard * Math.PI / 8 + age * 0.006;
            double radius = 0.55 + wave * (1.65 + (shard % 4) * 0.36);
            double height = (0.34 + (shard % 4) * 0.2) * smooth(0.08, 0.42, wave);
            Vec3 point = new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            Vec3 axis = new Vec3(Math.cos(angle) * 0.45, 1.0, Math.sin(angle) * 0.45);
            crystal(m, out, point, axis, 0.9 + (shard % 3) * 0.22, 0.17,
                    0.33f, 0.73f, 1.0f, 0.84f * fade * (float) (1 - wave * 0.25));
        }
    }

    // 33. ICE_PRISON — six large facets close around the chosen victim from different angles.
    private static void icePrison(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = fade(age, duration, 4, 7);
        double build = smooth(2, duration * 0.46, age);
        ringPlane(m, out, destination.add(0, 0.08, 0), UP, 0.68 + build * 0.36,
                0.035, age * 0.012, 0.42f, 0.81f, 1.0f, 0.86f * fade);
        for (int stake = 0; stake < 8; stake++) {
            double angle = stake * Math.PI / 4 + 0.1;
            double radius = 0.68 + (stake % 2) * 0.18;
            Vec3 base = destination.add(Math.cos(angle) * radius, 0.12, Math.sin(angle) * radius);
            double height = (2.4 + (stake % 3) * 0.48) * build;
            Vec3 axis = new Vec3(Math.cos(angle) * 0.25, 1.0, Math.sin(angle) * 0.25);
            Vec3 center = base.add(0, height * 0.48, 0);
            crystal(m, out, center, axis, height, 0.31 - (stake % 3) * 0.035,
                    0.36f, 0.75f, 1.0f, 0.78f * fade);
            crystal(m, out, center, axis, height * 0.68, 0.13,
                    0.88f, 0.98f, 1.0f, 0.85f * fade);
        }
        ringPlane(m, out, destination.add(0, 1.15, 0), new Vec3(1, 0.1, 0),
                0.86, 0.023, -age * 0.028, 0.51f, 0.87f, 1.0f, 0.75f * fade);
        PrayerVfxGeometry.ellipsoid(m, out, destination.add(0, 1.0, 0), new Vec3(0.4, 0.85, 0.4),
                8, 12, 0.38f, 0.75f, 1.0f, 0.08f * fade * (float) build, age * 0.03);
    }

    // 34. BLIZZARD — a low blue storm ceiling drives a spiral of suspended faceted ice.
    private static void blizzard(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 9, 12);
        double build = smooth(0, 14, age);
        for (int lobe = 0; lobe < 9; lobe++) {
            double angle = lobe * Math.PI * 2 / 9;
            Vec3 center = new Vec3(Math.cos(angle) * (1.4 + lobe % 3 * 0.32),
                    4.8 + Math.sin(age * 0.06 + lobe) * 0.28,
                    Math.sin(angle) * (1.1 + lobe % 2 * 0.38));
            double size = (0.7 + (lobe % 3) * 0.14) * build;
            PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(size, size * 0.55, size * 0.74),
                    7, 12, 0.31f, 0.57f, 0.82f, 0.56f * fade, angle);
        }
        ringPlane(m, out, new Vec3(0, 4.0, 0), UP, 3.1, 0.04, age * 0.035,
                0.43f, 0.78f, 1.0f, 0.6f * fade);
        for (int crystalIndex = 0; crystalIndex < 18; crystalIndex++) {
            double angle = crystalIndex * 2.399963 + age * (crystalIndex % 2 == 0 ? 0.012 : -0.016);
            double radius = 0.45 + (crystalIndex % 5) * 0.47;
            double y = 1.2 + (crystalIndex % 6) * 0.72 + Math.sin(age * 0.1 + crystalIndex) * 0.2;
            Vec3 point = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Vec3 axis = new Vec3(Math.cos(angle) * 0.3, -1.0, Math.sin(angle) * 0.3);
            crystal(m, out, point, axis, 0.46 + (crystalIndex % 3) * 0.16, 0.11,
                    0.45f, 0.79f, 1.0f, 0.82f * fade);
        }
        for (int wind = 0; wind < 3; wind++) {
            double y = 1.0 + wind * 1.2;
            ringPlane(m, out, new Vec3(0, y, 0), new Vec3(0.2, 1, 0.1),
                    1.1 + wind * 0.68, 0.022, age * (wind % 2 == 0 ? 0.045 : -0.052),
                    0.37f, 0.71f, 0.98f, 0.62f * fade);
        }
    }

    // 35. ABSOLUTE_ZERO — a near-motionless monumental crystal contracts the surrounding space.
    private static void absoluteZero(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fade(age, duration, 10, 14);
        double charge = smooth(0, 18, age);
        double freeze = smooth(0.42, 0.65, age / duration);
        Vec3 core = new Vec3(0, 3.4, 0);
        crystal(m, out, core, UP, 3.5, 1.05, 0.43f, 0.78f, 1.0f, 0.32f * fade);
        crystal(m, out, core, UP, 2.65, 0.58, 0.8f, 0.96f, 1.0f, 0.42f * fade);
        crystal(m, out, core, UP, 1.62, 0.24, 0.96f, 1.0f, 1.0f, 0.88f * fade);
        PrayerVfxGeometry.ellipsoid(m, out, core, new Vec3(0.32, 0.8, 0.32), 9, 14,
                0.8f, 0.96f, 1.0f, 0.46f * fade, age * 0.012);
        for (int orbit = 0; orbit < 3; orbit++) {
            Vec3 normal = orbit == 0 ? UP : orbit == 1 ? new Vec3(1, 0.15, 0) : new Vec3(0.2, 0, 1);
            ringPlane(m, out, core, normal, 1.5 + orbit * 0.62, 0.027,
                    age * (orbit % 2 == 0 ? 0.008 : -0.01), 0.52f, 0.84f, 1.0f, 0.74f * fade);
        }
        for (int shard = 0; shard < 14; shard++) {
            double angle = shard * 2.399963 + age * (shard % 2 == 0 ? 0.004 : -0.006);
            double radius = 1.8 + (shard % 4) * 0.52;
            Vec3 point = core.add(Math.cos(angle) * radius, Math.sin(angle * 1.6 + age * 0.008) * 1.35,
                    Math.sin(angle) * radius);
            Vec3 axis = safeNormalize(point.subtract(core), UP);
            crystal(m, out, point, axis, 0.68 + (shard % 3) * 0.18, 0.14,
                    0.55f, 0.87f, 1.0f, 0.78f * fade);
        }
        double shell = 0.4 + freeze * 5.0;
        PrayerVfxGeometry.wireSphere(m, out, new Vec3(0, 1.8, 0), shell, 24, age * 0.004,
                0.5f, 0.85f, 1.0f, (float) (0.25 * (1 - freeze * 0.6) * fade), 0.018);
        ringPlane(m, out, new Vec3(0, 0.07, 0), UP, 0.2 + freeze * 4.5,
                0.035, age * 0.004, 0.67f, 0.91f, 1.0f, (float) (0.58 * (1 - freeze * 0.55) * fade));
    }

    private static void portal(Matrix4fc m, VertexConsumer out, Vec3 center, double radius, double rotation,
                               float alpha, float r, float g, float b) {
        Vec3 facing = new Vec3(0, 0, 1);
        ringPlane(m, out, center, facing, radius, 0.055, rotation, r, g, b, 0.88f * alpha);
        ringPlane(m, out, center, facing, radius * 0.84, 0.022, -rotation * 1.7,
                Math.min(1, r + 0.18f), Math.min(1, g + 0.18f), Math.min(1, b + 0.18f), alpha);
        ringPlane(m, out, center, facing, radius * 0.62, 0.035, rotation * 1.25,
                r, g * 0.8f, b, 0.78f * alpha);
        ringPlane(m, out, center, facing, radius * 0.22, 0.018, -rotation * 2.2,
                0.78f, 0.45f, 1.0f, 0.72f * alpha);
        for (int rune = 0; rune < 10; rune++) {
            double angle = rune * Math.PI * 2 / 10 + rotation;
            Vec3 radial = new Vec3(Math.cos(angle), Math.sin(angle), 0);
            Vec3 tangent = new Vec3(-Math.sin(angle), Math.cos(angle), 0);
            Vec3 root = center.add(radial.scale(radius * 0.66));
            Vec3 outer = center.add(radial.scale(radius * 0.97));
            Vec3 crossbar = root.add(tangent.scale(radius * 0.10));
            PrayerVfxGeometry.tube(m, out, root, outer, 0.018, r, g, b, 0.9f * alpha);
            PrayerVfxGeometry.tube(m, out, root.subtract(tangent.scale(radius * 0.11)), crossbar,
                    0.018, 1.0f, 0.88f, 0.98f, 0.84f * alpha);
            PrayerVfxGeometry.tube(m, out, crossbar, root.add(tangent.scale(radius * 0.11)),
                    0.018, 1.0f, 0.88f, 0.98f, 0.84f * alpha);
        }
        PrayerVfxGeometry.wireSphere(m, out, center, radius * 0.46, 12, -rotation * 0.8,
                r, g * 0.7f, b, 0.18f * alpha, 0.014);
    }

    private static void silhouette(Matrix4fc m, VertexConsumer out, Vec3 center, float scale, float alpha,
                                   float r, float g, float b) {
        if (scale <= 0.015f || alpha <= 0.01f) return;
        Vec3 feet = center.add(0, 0.91 * scale, 0);
        Vec3 pelvis = feet.add(0, 0.42 * scale, 0);
        Vec3 chest = feet.add(0, 0.96 * scale, 0);
        Vec3 shoulders = feet.add(0, 1.10 * scale, 0);
        Vec3 head = feet.add(0, 1.38 * scale, 0);
        Vec3 leftShoulder = shoulders.add(-0.22 * scale, 0, 0);
        Vec3 rightShoulder = shoulders.add(0.22 * scale, 0, 0);
        Vec3 leftHand = leftShoulder.add(-0.15 * scale, -0.48 * scale, 0.02 * scale);
        Vec3 rightHand = rightShoulder.add(0.15 * scale, -0.48 * scale, 0.02 * scale);
        Vec3 leftKnee = pelvis.add(-0.13 * scale, -0.34 * scale, 0);
        Vec3 rightKnee = pelvis.add(0.13 * scale, -0.34 * scale, 0);
        PrayerVfxGeometry.ellipsoid(m, out, head, new Vec3(0.15, 0.18, 0.13).scale(scale),
                7, 10, r, g, b, 0.45f * alpha, 0);
        PrayerVfxGeometry.ellipsoid(m, out, chest, new Vec3(0.27, 0.43, 0.15).scale(scale),
                8, 12, r, g, b, 0.28f * alpha, 0);
        PrayerVfxGeometry.taperedTube(m, out, pelvis, chest, 0.18 * scale, 0.12 * scale,
                r, g, b, 0.9f * alpha);
        for (Vec3[] limb : new Vec3[][] {
                {shoulders, leftShoulder, leftHand}, {shoulders, rightShoulder, rightHand},
                {pelvis, leftKnee, feet.add(-0.16 * scale, -0.43 * scale, 0)},
                {pelvis, rightKnee, feet.add(0.16 * scale, -0.43 * scale, 0)}
        }) {
            PrayerVfxGeometry.taperedTube(m, out, limb[0], limb[1], 0.085 * scale, 0.055 * scale,
                    r, g, b, 0.8f * alpha);
            PrayerVfxGeometry.taperedTube(m, out, limb[1], limb[2], 0.06 * scale, 0.025 * scale,
                    r, g, b, 0.76f * alpha);
        }
    }

    private static void soulFigure(Matrix4fc m, VertexConsumer out, Vec3 center, double scale,
                                   double rotation, float alpha) {
        Vec3 head = center.add(0, scale * 0.72, 0);
        PrayerVfxGeometry.ellipsoid(m, out, head, new Vec3(scale * 0.28, scale * 0.34, scale * 0.25),
                7, 10, 0.64f, 0.30f, 0.94f, 0.72f * alpha, rotation);
        Vec3 chest = center.add(0, scale * 0.15, 0);
        PrayerVfxGeometry.ellipsoid(m, out, chest, new Vec3(scale * 0.43, scale * 0.52, scale * 0.31),
                8, 12, 0.40f, 0.12f, 0.77f, 0.56f * alpha, -rotation * 0.7);
        for (int ribbon = 0; ribbon < 3; ribbon++) {
            double angle = rotation + ribbon * Math.PI * 2 / 3;
            Vec3 previous = center.add(Math.cos(angle) * scale * 0.36, scale * 0.38,
                    Math.sin(angle) * scale * 0.36);
            for (int step = 1; step <= 7; step++) {
                double t = step / 7.0;
                double a = angle + t * Math.PI * 1.5;
                Vec3 next = center.add(Math.cos(a) * scale * (0.34 * (1 - t) + 0.06),
                        scale * (0.38 - t * 1.25), Math.sin(a) * scale * (0.34 * (1 - t) + 0.06));
                PrayerVfxGeometry.taperedTube(m, out, previous, next, scale * 0.07 * (1 - t * 0.65),
                        0.003, 0.7f, 0.35f, 1.0f, alpha * (float) (0.72 * (1 - t * 0.5)));
                previous = next;
            }
        }
        ringPlane(m, out, center, new Vec3(0.3, 1, 0.2), scale * 0.63, scale * 0.035,
                rotation, 0.80f, 0.40f, 1.0f, 0.66f * alpha);
    }

    private static void flameBurstImpact(Matrix4fc m, VertexConsumer out, Vec3 center, float age,
                                        int duration, float fade) {
        double burst = smooth(duration * 0.68, duration * 0.87, age);
        double radius = 0.18 + burst * 2.75;
        float alpha = (float) ((1 - burst) * fade);
        ringPlane(m, out, center.add(0, 0.09, 0), UP, radius, 0.10 * (1 - burst * 0.65),
                age * 0.055, 1.0f, 0.27f, 0.025f, 0.86f * alpha);
        ringPlane(m, out, center.add(0, 0.13, 0), UP, radius * 0.70, 0.035,
                -age * 0.08, 1.0f, 0.78f, 0.16f, 0.88f * alpha);
        PrayerVfxGeometry.wireSphere(m, out, center.add(0, 0.52, 0), 0.25 + burst * 1.65,
                14, age * 0.09, 1.0f, 0.39f, 0.035f, 0.37f * alpha, 0.028);
        for (int ray = 0; ray < 12; ray++) {
            double angle = ray * Math.PI / 6 + age * 0.018;
            double reach = radius * (0.72 + (ray % 3) * 0.13);
            Vec3 base = center.add(Math.cos(angle) * 0.16, 0.12, Math.sin(angle) * 0.16);
            Vec3 knee = center.add(Math.cos(angle) * reach * 0.58, 0.42 + (ray % 3) * 0.12,
                    Math.sin(angle) * reach * 0.58);
            Vec3 tip = center.add(Math.cos(angle) * reach, 0.08, Math.sin(angle) * reach);
            flamePetal(m, out, base, knee, tip, 0.20 * (1 - burst * 0.55), alpha);
        }
    }

    private static void flamePetal(Matrix4fc m, VertexConsumer out, Vec3 base, Vec3 knee, Vec3 tip,
                                   double width, float alpha) {
        if (alpha <= 0.005f) return;
        PrayerVfxGeometry.taperedTube(m, out, base, knee, width, width * 0.62,
                1.0f, 0.20f, 0.018f, 0.48f * alpha);
        PrayerVfxGeometry.taperedTube(m, out, knee, tip, width * 0.62, 0.003,
                1.0f, 0.48f, 0.055f, 0.82f * alpha);
        PrayerVfxGeometry.tube(m, out, base, tip, width * 0.20,
                1.0f, 0.88f, 0.28f, 0.86f * alpha);
    }

    private static void circleSigil(Matrix4fc m, VertexConsumer out, Vec3 center, double radius,
                                    double rotation, float alpha, float r, float g, float b) {
        ringPlane(m, out, center, UP, radius, 0.024, rotation, r, g, b, 0.92f * alpha);
        ringPlane(m, out, center.add(0, 0.025, 0), UP, radius * 0.76, 0.014,
                -rotation * 1.5, 1.0f, 0.93f, 0.69f, 0.76f * alpha);
        ringPlane(m, out, center.add(0, 0.035, 0), UP, radius * 0.35, 0.018,
                rotation * 0.8, r, g, b, 0.82f * alpha);
        for (int mark = 0; mark < 24; mark++) {
            double angle = mark * Math.PI * 2 / 24 + rotation;
            double inner = radius * (mark % 3 == 0 ? 0.81 : 0.88);
            double outer = radius * (mark % 3 == 0 ? 1.04 : 0.96);
            Vec3 from = center.add(Math.cos(angle) * inner, 0.06, Math.sin(angle) * inner);
            Vec3 to = center.add(Math.cos(angle + 0.012) * outer, 0.06, Math.sin(angle + 0.012) * outer);
            PrayerVfxGeometry.tube(m, out, from, to, mark % 3 == 0 ? 0.018 : 0.01,
                    r, g, b, (mark % 3 == 0 ? 0.84f : 0.5f) * alpha);
        }
        for (int spoke = 0; spoke < 8; spoke++) {
            double angle = spoke * Math.PI / 4 + rotation;
            Vec3 from = center.add(Math.cos(angle) * radius * 0.34, 0.045, Math.sin(angle) * radius * 0.34);
            Vec3 to = center.add(Math.cos(angle + Math.PI / 4) * radius * 0.71, 0.045,
                    Math.sin(angle + Math.PI / 4) * radius * 0.71);
            PrayerVfxGeometry.tube(m, out, from, to, 0.01, r, g, b, 0.64f * alpha);
        }
    }

    private static void runeHalo(Matrix4fc m, VertexConsumer out, Vec3 center, double scale,
                                 double rotation, float alpha, float r, float g, float b) {
        Vec3 u = new Vec3(Math.cos(rotation), 0, Math.sin(rotation));
        Vec3 v = UP;
        Vec3[] p = {
                runePoint(center, u, v, 0, scale), runePoint(center, u, v, -0.55 * scale, 0.3 * scale),
                runePoint(center, u, v, 0.55 * scale, 0.3 * scale), runePoint(center, u, v, -0.42 * scale, -0.45 * scale),
                runePoint(center, u, v, 0.42 * scale, -0.45 * scale), runePoint(center, u, v, 0, -scale),
                runePoint(center, u, v, 0, 0)
        };
        int[][] edges = {{0, 1}, {0, 2}, {1, 2}, {1, 3}, {2, 4}, {3, 4}, {3, 5}, {4, 5}, {0, 6}, {6, 5}};
        for (int[] edge : edges) {
            PrayerVfxGeometry.tube(m, out, p[edge[0]], p[edge[1]], 0.019, r, g, b, alpha);
            PrayerVfxGeometry.tube(m, out, p[edge[0]], p[edge[1]], 0.006,
                    1.0f, 0.98f, 0.83f, alpha * 0.82f);
        }
    }

    private static void starburst(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 normal, double radius,
                                  float alpha, float r, float g, float b) {
        Vec3[] basis = basis(normal);
        for (int ray = 0; ray < 12; ray++) {
            double angle = ray * Math.PI / 6;
            Vec3 dir = basis[0].scale(Math.cos(angle)).add(basis[1].scale(Math.sin(angle)));
            Vec3 tip = center.add(dir.scale(radius * (ray % 3 == 0 ? 1.0 : 0.72)));
            PrayerVfxGeometry.taperedTube(m, out, center, tip, 0.025, 0.002, r, g, b, alpha);
        }
        ringPlane(m, out, center, normal, radius * 0.48, 0.025, 0.0, r, g, b, alpha * 0.9f);
    }

    private static void drawLance(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 direction,
                                  double length, double width, double rotation,
                                  float r, float g, float b, float alpha) {
        Vec3 axis = safeNormalize(direction, new Vec3(0, 0, -1));
        Vec3 base = center.subtract(axis.scale(length * 0.44));
        Vec3 tip = center.add(axis.scale(length * 0.56));
        Vec3[] frame = basis(axis);
        Vec3 up = frame[0].scale(Math.cos(rotation)).add(frame[1].scale(Math.sin(rotation)));
        Vec3 side = frame[1].scale(Math.cos(rotation)).subtract(frame[0].scale(Math.sin(rotation)));
        Vec3[] corners = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            corners[i] = center.add(up.scale(Math.cos(angle) * width)).add(side.scale(Math.sin(angle) * width));
        }
        Vec3 rear = base.subtract(axis.scale(length * 0.18));
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            PrayerVfxGeometry.quad(m, out, rear, corners[i], corners[next], rear,
                    r * 0.72f, g * 0.65f, b * 0.42f, alpha * 0.8f);
            PrayerVfxGeometry.triangle(m, out, corners[i], corners[next], tip,
                    r, g, b, alpha);
            PrayerVfxGeometry.tube(m, out, corners[i], tip, 0.012,
                    Math.min(1, r + 0.12f), Math.min(1, g + 0.1f), Math.min(1, b + 0.1f), alpha);
        }
        PrayerVfxGeometry.taperedTube(m, out, rear, tip, width * 0.28, 0.006,
                1.0f, 0.98f, 0.82f, alpha);
    }

    private static void crystal(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 axis,
                                double length, double radius, float r, float g, float b, float alpha) {
        if (length <= 0 || radius <= 0) return;
        Vec3 n = safeNormalize(axis, UP);
        Vec3[] frame = basis(n);
        Vec3 upper = center.add(n.scale(length * 0.5));
        Vec3 lower = center.subtract(n.scale(length * 0.5));
        Vec3[] ring = new Vec3[6];
        for (int i = 0; i < ring.length; i++) {
            double a = i * Math.PI * 2 / ring.length;
            double ripple = 0.88 + 0.12 * Math.sin(a * 3 + length * 7);
            ring[i] = center.add(frame[0].scale(Math.cos(a) * radius * ripple))
                    .add(frame[1].scale(Math.sin(a) * radius * ripple));
        }
        for (int i = 0; i < ring.length; i++) {
            int next = (i + 1) % ring.length;
            float shade = 0.72f + 0.28f * (float) ((i % 3) / 2.0);
            PrayerVfxGeometry.triangle(m, out, ring[i], ring[next], upper,
                    r * shade, g * shade, b * shade, alpha);
            PrayerVfxGeometry.triangle(m, out, ring[next], ring[i], lower,
                    r * shade * 0.68f, g * shade * 0.75f, b * shade, alpha * 0.84f);
        }
        PrayerVfxGeometry.tube(m, out, lower, upper, 0.008,
                Math.min(1, r + 0.16f), Math.min(1, g + 0.16f), Math.min(1, b + 0.12f), alpha * 0.82f);
    }

    private static void ringPlane(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 normal,
                                  double radius, double thickness, double rotation,
                                  float r, float g, float b, float alpha) {
        if (radius <= 0 || thickness <= 0) return;
        Vec3[] frame = basis(normal);
        Vec3 previous = center.add(frame[0].scale(radius));
        int segments = 48;
        for (int i = 1; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments + rotation;
            Vec3 next = center.add(frame[0].scale(Math.cos(angle) * radius))
                    .add(frame[1].scale(Math.sin(angle) * radius));
            PrayerVfxGeometry.tube(m, out, previous, next, thickness, r, g, b, alpha);
            previous = next;
        }
    }

    private static void bolt(Matrix4fc m, VertexConsumer out, Vec3 start, Vec3 end, long seed,
                             int segments, double amplitude, float alpha) {
        List<Vec3> points = jagged(start, end, segments, seed, amplitude);
        for (int i = 0; i < points.size() - 1; i++) {
            double taper = 1 - i / (double) (points.size() - 1) * 0.38;
            PrayerVfxGeometry.tube(m, out, points.get(i), points.get(i + 1), 0.09 * taper,
                    0.12f, 0.48f, 1.0f, 0.18f * alpha);
            PrayerVfxGeometry.tube(m, out, points.get(i), points.get(i + 1), 0.034 * taper,
                    0.44f, 0.82f, 1.0f, 0.82f * alpha);
            PrayerVfxGeometry.tube(m, out, points.get(i), points.get(i + 1), 0.012 * taper,
                    0.96f, 1.0f, 1.0f, alpha);
        }
    }

    private static List<Vec3> jagged(Vec3 from, Vec3 to, int segments, long seed, double amplitude) {
        java.util.Random random = new java.util.Random(seed);
        List<Vec3> points = new ArrayList<>(segments + 1);
        Vec3 direction = to.subtract(from);
        Vec3[] frame = basis(direction);
        points.add(from);
        for (int i = 1; i < segments; i++) {
            double t = i / (double) segments;
            double wobble = amplitude * (0.32 + 0.68 * Math.sin(Math.PI * t));
            points.add(from.add(direction.scale(t))
                    .add(frame[0].scale((random.nextDouble() * 2 - 1) * wobble))
                    .add(frame[1].scale((random.nextDouble() * 2 - 1) * wobble)));
        }
        points.add(to);
        return points;
    }

    private static Vec3[] basis(Vec3 direction) {
        Vec3 axis = safeNormalize(direction, UP);
        Vec3 ref = Math.abs(axis.y) < 0.92 ? UP : new Vec3(1, 0, 0);
        Vec3 u = safeNormalize(axis.cross(ref), new Vec3(1, 0, 0));
        Vec3 v = safeNormalize(axis.cross(u), new Vec3(0, 0, 1));
        return new Vec3[] {u, v};
    }

    private static Vec3 runePoint(Vec3 center, Vec3 u, Vec3 v, double x, double y) {
        return center.add(u.scale(x)).add(v.scale(y));
    }

    private static Vec3 safeNormalize(Vec3 value, Vec3 fallback) {
        return value.lengthSqr() < 1.0E-8 ? fallback : value.normalize();
    }

    private static double smooth(double start, double end, double value) {
        double t = clamp((value - start) / Math.max(1.0E-5, end - start));
        return t * t * (3 - 2 * t);
    }

    private static float fade(float age, int duration, int fadeIn, int fadeOut) {
        return (float) Math.min(smooth(0, fadeIn, age), 1 - smooth(duration - fadeOut, duration, age));
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
