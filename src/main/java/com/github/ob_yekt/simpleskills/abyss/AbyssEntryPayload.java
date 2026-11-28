package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Enviado pelo servidor quando o jogador entra no abismo pela primeira vez. */
public record AbyssEntryPayload(boolean firstTime) implements CustomPacketPayload {
    public static final Type<AbyssEntryPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyss_entry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbyssEntryPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AbyssEntryPayload::firstTime,
            AbyssEntryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
