package com.github.ob_yekt.simpleskills.runes;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server cosmetic mining state for Rune Ore blocks. */
public record RuneMiningPayload(int x, int y, int z, int charge) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RuneMiningPayload> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rune_mining_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RuneMiningPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RuneMiningPayload::x,
            ByteBufCodecs.VAR_INT, RuneMiningPayload::y,
            ByteBufCodecs.VAR_INT, RuneMiningPayload::z,
            ByteBufCodecs.VAR_INT, RuneMiningPayload::charge,
            RuneMiningPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
