package com.github.ob_yekt.simpleskills.excavating;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client progress of the investigation that is currently running.
 *
 * The server owns the investigation; the client only draws the bar under the crosshair.
 * A payload with {@code durationTicks <= 0} means "no investigation running" (cancelled or
 * finished) and hides the bar.
 */
public record InvestigationPayload(int elapsedTicks, int durationTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InvestigationPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "excavating_investigation")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, InvestigationPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InvestigationPayload::elapsedTicks,
                    ByteBufCodecs.VAR_INT, InvestigationPayload::durationTicks,
                    InvestigationPayload::new
            );

    /** Payload that tells the client to hide the progress bar. */
    public static InvestigationPayload stop() {
        return new InvestigationPayload(0, 0);
    }

    public boolean isStop() {
        return durationTicks <= 0;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
