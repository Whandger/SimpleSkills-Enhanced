package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client input request. The server validates session, crack and timing before applying it. */
public record RuneInfusionInputPayload(long sessionMost, long sessionLeast, int action,
                                       int crackId) implements CustomPacketPayload {
    public static final int BEGIN = 0;
    public static final int HOLD = 1;
    public static final int RELEASE = 2;

    public static final Type<RuneInfusionInputPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rune_infusion_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RuneInfusionInputPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, RuneInfusionInputPayload::sessionMost,
                    ByteBufCodecs.VAR_LONG, RuneInfusionInputPayload::sessionLeast,
                    ByteBufCodecs.VAR_INT, RuneInfusionInputPayload::action,
                    ByteBufCodecs.VAR_INT, RuneInfusionInputPayload::crackId,
                    RuneInfusionInputPayload::new);

    public static RuneInfusionInputPayload of(java.util.UUID session, int action, int crackId) {
        return new RuneInfusionInputPayload(session.getMostSignificantBits(),
                session.getLeastSignificantBits(), action, crackId);
    }

    public java.util.UUID sessionId() {
        return new java.util.UUID(sessionMost, sessionLeast);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
