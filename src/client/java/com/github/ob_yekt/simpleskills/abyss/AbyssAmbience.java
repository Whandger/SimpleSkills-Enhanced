package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.SimpleSkillsSounds;
import java.util.Random;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Sons ambiente da caverna do abismo: cristais ecoando, rachaduras e pedrinhas caindo, em intervalos
 * aleatórios. Só toca com o jogador dentro do abismo, e cada som sai de um ponto sorteado ao redor dele.
 */
public final class AbyssAmbience {
    private static final Random RANDOM = new Random();

    /** Intervalos em ticks (20 = 1 s). */
    private static final int CRYSTAL_MIN = 25 * 20, CRYSTAL_RANDOM = 25 * 20;   // 25 a 50 s
    private static final int CRACK_MIN = 40 * 20, CRACK_RANDOM = 50 * 20;       // 40 a 90 s
    private static final int PEBBLES_MIN = 30 * 20, PEBBLES_RANDOM = 40 * 20;   // 30 a 70 s

    private static int untilCrystal = -1;
    private static int untilCrack = -1;
    private static int untilPebbles = -1;

    private AbyssAmbience() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AbyssAmbience::tick);
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }
        if (!AbyssWorldgen.isAbyssY(mc.player.blockPosition().getY())) return;

        if (untilCrystal < 0) untilCrystal = CRYSTAL_MIN + RANDOM.nextInt(CRYSTAL_RANDOM);
        if (untilCrack < 0) untilCrack = CRACK_MIN + RANDOM.nextInt(CRACK_RANDOM);
        if (untilPebbles < 0) untilPebbles = PEBBLES_MIN + RANDOM.nextInt(PEBBLES_RANDOM);

        if (--untilCrystal <= 0) {
            untilCrystal = CRYSTAL_MIN + RANDOM.nextInt(CRYSTAL_RANDOM);
            play(mc, mc.player, SimpleSkillsSounds.ABYSS_CRYSTAL, 8, 16, -3, 3, 0.7F);
        }
        if (--untilCrack <= 0) {
            untilCrack = CRACK_MIN + RANDOM.nextInt(CRACK_RANDOM);
            play(mc, mc.player, SimpleSkillsSounds.ABYSS_CRACK, 4, 12, -2, 2, 0.85F);
        }
        if (--untilPebbles <= 0) {
            untilPebbles = PEBBLES_MIN + RANDOM.nextInt(PEBBLES_RANDOM);
            // Pedrinhas caem do teto: o som vem de cima, perto do jogador.
            play(mc, mc.player, SimpleSkillsSounds.ABYSS_PEBBLES, 2, 6, 4, 7, 0.9F);
        }
    }

    /** Toca o som num ponto sorteado ao redor do jogador, com distância horizontal e altura entre os limites dados. */
    private static void play(Minecraft mc, Player player, SoundEvent event,
                             double minDist, double maxDist, double minDy, double maxDy, float volume) {
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double dist = minDist + RANDOM.nextDouble() * (maxDist - minDist);
        double dy = minDy + RANDOM.nextDouble() * (maxDy - minDy);
        double x = player.getX() + Math.cos(angle) * dist;
        double y = player.getY() + dy;
        double z = player.getZ() + Math.sin(angle) * dist;
        float pitch = 0.9F + RANDOM.nextFloat() * 0.2F;
        mc.level.playLocalSound(x, y, z, event, SoundSource.AMBIENT, volume, pitch, false);
    }

    private static void reset() {
        untilCrystal = -1;
        untilCrack = -1;
        untilPebbles = -1;
    }
}
