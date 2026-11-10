package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Server snapshot. state: 0 = STOP, 1 = RUNNING, 2 = SUCCEEDED, 3 = FAILED.
 * cracks: rows "id,x*1000,y*1000,ageTicks,growthRequired,repairing(0/1)" separated by ';'.
 */
public record RuneInfusionStatePayload(
        long sessionMost,
        long sessionLeast,
        int state,
        int integrity,
        int elapsedTicks,
        int durationTicks,
        int grade,
        int repairedCracks,
        int propagatedCracks,
        int inventorySlot,
        String cracks
) implements CustomPacketPayload {

    public static final int STOP = 0;

    public static final CustomPacketPayload.Type<RuneInfusionStatePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rune_infusion_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RuneInfusionStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, RuneInfusionStatePayload::sessionMost,
                    ByteBufCodecs.VAR_LONG, RuneInfusionStatePayload::sessionLeast,
                    ByteBufCodecs.VAR_INT, RuneInfusionStatePayload::state,
                    ByteBufCodecs.VAR_INT, RuneInfusionStatePayload::integrity,
                    ByteBufCodecs.VAR_INT, RuneInfusionStatePayload::elapsedTicks,
                    ByteBufCodecs.VAR_INT, RuneInfusionStatePayload::durationTicks,
                    ByteBufCodecs.VAR_INT, RuneInfusionStatePayload::grade,
                    ByteBufCodecs.VAR_INT, RuneInfusionStatePayload::repairedCracks,
                    ByteBufCodecs.VAR_INT, RuneInfusionStatePayload::propagatedCracks,
                    ByteBufCodecs.VAR_INT, RuneInfusionStatePayload::inventorySlot,
                    ByteBufCodecs.STRING_UTF8, RuneInfusionStatePayload::cracks,
                    RuneInfusionStatePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** STOP carries the real session id so the client can match and clean up the right session. */
    public static RuneInfusionStatePayload stop(UUID id) {
        return new RuneInfusionStatePayload(id.getMostSignificantBits(), id.getLeastSignificantBits(),
                STOP, 0, 0, 0, 0, 0, 0, -1, "");
    }

    public static RuneInfusionStatePayload from(UUID id, RuneInfusionMinigame.Snapshot s, int inventorySlot) {
        StringBuilder encoded = new StringBuilder();
        int count = 0;

        for (RuneInfusionMinigame.CrackSnapshot c : s.cracks()) {
            if (count++ >= 16) break;
            if (!encoded.isEmpty()) encoded.append(';');

            encoded.append(c.id()).append(',')
                    .append(Math.round(c.x() * 1000)).append(',')
                    .append(Math.round(c.y() * 1000)).append(',')
                    .append(c.ageTicks()).append(',')
                    .append(c.growthRequired()).append(',')
                    .append(c.repairing() ? 1 : 0);
        }

        return new RuneInfusionStatePayload(
                id.getMostSignificantBits(),
                id.getLeastSignificantBits(),
                s.state().ordinal() + 1,
                s.integrity(),
                s.elapsedTicks(),
                s.durationTicks(),
                s.grade().ordinal(),
                s.repairedCracks(),
                s.propagatedCracks(),
                inventorySlot,
                encoded.toString()
        );
    }
}
