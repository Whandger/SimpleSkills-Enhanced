package com.github.ob_yekt.simpleskills.abyss.client;

import static com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity.BARRAGE_WAVE_END;
import static com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity.BARRAGE_WAVE_START;
import static com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity.LASER_CHARGE_END;
import static com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity.LASER_FIRE_END;
import static com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity.LEAP_LAUNCH_TICK;
import static com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity.SLAM_IMPACT_TICK;
import static com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity.SWIPE_IMPACT_TICK;

import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import com.github.ob_yekt.simpleskills.prayer.PrayerImpactFeedback;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tremor de câmera nos ataques do Crimson Colossus. Não cria pacote de rede: o cliente lê o estado
 * sincronizado de cada chefe (tipo de ataque, tick e tick do impacto) e dispara o tremor quando um
 * marco do ataque é cruzado. A intensidade cai com a distância, dentro do PrayerImpactFeedback.
 */
public final class CrimsonColossusFeedback {
    /** Raio em volta do jogador onde os chefes são observados. */
    private static final double SCAN_RADIUS = 48.0D;

    private static final Map<Integer, Track> TRACKS = new HashMap<>();

    /** Último estado visto de um chefe, para detectar os marcos do ataque. */
    private static final class Track {
        boolean known;
        CrimsonColossusEntity.AttackType type = CrimsonColossusEntity.AttackType.NONE;
        int tick;
        int impact = -1;
    }

    private CrimsonColossusFeedback() {}

    /** Chamado a cada tick do cliente. */
    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            TRACKS.clear();
            return;
        }

        AABB box = client.player.getBoundingBox().inflate(SCAN_RADIUS);
        List<CrimsonColossusEntity> bosses = level.getEntitiesOfClass(CrimsonColossusEntity.class, box);

        Map<Integer, Track> next = new HashMap<>();
        for (CrimsonColossusEntity boss : bosses) {
            Track t = TRACKS.get(boss.getId());
            if (t == null) t = new Track();
            observe(boss, t);
            next.put(boss.getId(), t);
        }
        TRACKS.clear();
        TRACKS.putAll(next);
    }

    private static void observe(CrimsonColossusEntity boss, Track t) {
        CrimsonColossusEntity.AttackType type = boss.getAttackType();
        int tick = boss.getAttackTick();
        int impact = boss.getImpactTick();
        boolean newAttack = type != t.type || tick < t.tick;

        // Na primeira vez que o chefe aparece só registra o estado, para não tremer por ataques antigos.
        if (t.known && type != CrimsonColossusEntity.AttackType.NONE) {
            Vec3 pos = boss.position();
            int from = newAttack ? 0 : t.tick;
            for (int k = from + 1; k <= tick; k++) {
                onMark(type, k, pos);
            }
            if (impact >= 0 && (newAttack || t.impact < 0)) {
                onLanding(type, pos);
            }
        }

        t.known = true;
        t.type = type;
        t.tick = tick;
        t.impact = impact;
    }

    /** Marcos de cada ataque, pelo tick do ataque (os mesmos números que o servidor usa). */
    private static void onMark(CrimsonColossusEntity.AttackType type, int k, Vec3 pos) {
        switch (type) {
            case CRYSTAL_LASER -> {
                if (k < LASER_CHARGE_END) {
                    // Carregamento: pulsos que crescem, sentindo a energia subir.
                    if (k % 4 == 0) cue(pos, 0.3F + 0.9F * k / LASER_CHARGE_END, 180L);
                } else if (k == LASER_CHARGE_END) {
                    cue(pos, 2.6F, 420L);
                } else if (k < LASER_FIRE_END && k % 6 == 0) {
                    cue(pos, 0.9F, 140L);
                }
            }
            case SEISMIC_SLAM -> {
                if (k == SLAM_IMPACT_TICK) cue(pos, 3.6F, 480L);
            }
            case CRYSTAL_BARRAGE -> {
                if (k >= BARRAGE_WAVE_START && k <= BARRAGE_WAVE_END && (k - BARRAGE_WAVE_START) % 3 == 0) {
                    cue(pos, 1.1F, 160L);
                }
            }
            case COLOSSUS_LEAP -> {
                if (k == LEAP_LAUNCH_TICK) cue(pos, 1.2F, 220L);
            }
            case SWIPE -> {
                if (k == SWIPE_IMPACT_TICK) cue(pos, 1.8F, 200L);
            }
            default -> {
            }
        }
    }

    /** Aterrissagem do salto: o tremor mais forte, junto com a onda de choque. */
    private static void onLanding(CrimsonColossusEntity.AttackType type, Vec3 pos) {
        if (type == CrimsonColossusEntity.AttackType.COLOSSUS_LEAP) {
            cue(pos, 4.2F, 520L);
        }
    }

    private static void cue(Vec3 pos, float degrees, long durationMs) {
        PrayerImpactFeedback.triggerCue(pos, degrees, durationMs, 0L);
    }
}
