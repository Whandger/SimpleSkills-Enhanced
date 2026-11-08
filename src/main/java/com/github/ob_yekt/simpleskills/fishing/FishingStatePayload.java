package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client snapshot of the fishing minigame.
 *
 * <p>The server owns the whole session (fish, fatigue, tension, rewards); the client only draws the
 * HUD from these snapshots. Nothing in here is ever trusted in the opposite direction: the client
 * does not send any fishing input, the server reads the player's camera and use-key presses itself.</p>
 *
 * @param state    one of the {@code STATE_*} constants
 * @param tension  line tension as a 0..1 ratio of the break limit
 * @param fatigue  fish fatigue 0..1
 * @param progress how much of the line has been reeled in 0..1
 * @param fishMove -1 = fish swims left, 0 = rests, 1 = swims right (from the player's point of view)
 * @param species  translation key of the species involved
 */
public record FishingStatePayload(int state, float tension, float fatigue, float progress,
                                  int fishMove, String species) implements CustomPacketPayload {

    /** Hide the HUD. */
    public static final int STATE_STOP = 0;
    /** A fish bit the bait: the player has to right-click to start the fight. */
    public static final int STATE_BITE = 1;
    /** Fight running. */
    public static final int STATE_FIGHT = 2;
    /** The fish was caught (species shown for a moment). */
    public static final int STATE_CAUGHT = 3;
    /** The line snapped. */
    public static final int STATE_LINE_BROKEN = 4;
    /** The fish spat the hook / swam away. */
    public static final int STATE_LOST = 5;

    public static final CustomPacketPayload.Type<FishingStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "fishing_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FishingStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FishingStatePayload::state,
                    ByteBufCodecs.FLOAT, FishingStatePayload::tension,
                    ByteBufCodecs.FLOAT, FishingStatePayload::fatigue,
                    ByteBufCodecs.FLOAT, FishingStatePayload::progress,
                    ByteBufCodecs.VAR_INT, FishingStatePayload::fishMove,
                    ByteBufCodecs.STRING_UTF8, FishingStatePayload::species,
                    FishingStatePayload::new
            );

    public static FishingStatePayload stop() {
        return new FishingStatePayload(STATE_STOP, 0f, 0f, 0f, 0, "");
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
