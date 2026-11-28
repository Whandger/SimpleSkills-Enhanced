package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.SimpleSkillsSounds;
import java.util.Random;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/**
 * Trilhas do abismo, 100% cliente. A primeira entrada toca a épica (avisada pelo servidor). Depois,
 * de vez em quando, sorteia entre a épica e a calma, enquanto o jogador estiver lá (4 a 8 minutos).
 */
public final class AbyssThemeMusic {
    /** Duração das faixas, com 1 s de folga. */
    private static final int EPIC_TICKS = 114 * 20;
    private static final int CALM_TICKS = 133 * 20;
    /** Intervalo mínimo e aleatório entre execuções ao estar no abismo (4 a 8 minutos). */
    private static final int REPEAT_MIN_TICKS = 4 * 60 * 20;
    private static final int REPEAT_RANDOM_TICKS = 4 * 60 * 20;
    /** Pequena pausa após o aviso do servidor, para a tela de carregamento terminar. */
    private static final int FIRST_ENTRY_DELAY_TICKS = 40;

    private static final Random RANDOM = new Random();
    private static int playingTicks = 0;
    private static int untilRepeat = -1;
    private static int untilFirstEntry = -1;

    private AbyssThemeMusic() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(AbyssEntryPayload.TYPE, (payload, context) ->
                context.client().execute(() -> untilFirstEntry = FIRST_ENTRY_DELAY_TICKS));
        ClientTickEvents.END_CLIENT_TICK.register(AbyssThemeMusic::tick);
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }
        if (playingTicks > 0) playingTicks--;

        if (untilFirstEntry > 0 && --untilFirstEntry == 0) {
            untilFirstEntry = -1;
            play(mc, SimpleSkillsSounds.ABYSS_THEME, EPIC_TICKS);
            return;
        }

        if (!AbyssWorldgen.isAbyssY(mc.player.blockPosition().getY())) return;
        if (untilRepeat < 0) untilRepeat = nextInterval();
        if (playingTicks > 0) return;
        if (--untilRepeat <= 0) playRepeat(mc);
    }

    /** Nas repetições, sorteia uma das duas trilhas com a mesma chance. */
    private static void playRepeat(Minecraft mc) {
        if (RANDOM.nextBoolean()) {
            play(mc, SimpleSkillsSounds.ABYSS_THEME_CALM, CALM_TICKS);
        } else {
            play(mc, SimpleSkillsSounds.ABYSS_THEME, EPIC_TICKS);
        }
    }

    private static void play(Minecraft mc, SoundEvent event, int ticks) {
        // Evita a música ambiente do jogo tocar por cima; ela volta sozinha depois do intervalo dela.
        mc.getMusicManager().stopPlaying();
        mc.getSoundManager().play(SimpleSoundInstance.forMusic(event));
        playingTicks = ticks;
        untilRepeat = nextInterval();
    }

    private static int nextInterval() {
        return REPEAT_MIN_TICKS + RANDOM.nextInt(REPEAT_RANDOM_TICKS);
    }

    private static void reset() {
        playingTicks = 0;
        untilRepeat = -1;
        untilFirstEntry = -1;
    }
}
