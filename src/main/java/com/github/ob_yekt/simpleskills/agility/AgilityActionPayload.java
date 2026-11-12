package com.github.ob_yekt.simpleskills.agility;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Untrusted client request for an Agility action; all conditions are checked by the server. */
public record AgilityActionPayload(int action) implements CustomPacketPayload {
    public static final int DASH = 0;
    public static final int DOUBLE_JUMP = 1;

    public static final Type<AgilityActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "agility_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AgilityActionPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, AgilityActionPayload::action, AgilityActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
