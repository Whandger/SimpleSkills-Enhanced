package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** General skill level plus world-specific locations for the final lore chapters. */
public record AbyssLoreProgressDataPayload(
        int totalLevel,
        int bridgeX, int bridgeY, int bridgeZ,
        int castleX, int castleY, int castleZ,
        BlockPos colossusRegionCenter
) implements CustomPacketPayload {
    public static final int UNKNOWN_COORDINATE = Integer.MIN_VALUE;
    public static final BlockPos UNKNOWN_BLOCK_POS = BlockPos.ZERO;
    public static final Type<AbyssLoreProgressDataPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyss_lore_progress_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbyssLoreProgressDataPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AbyssLoreProgressDataPayload::totalLevel,
                    ByteBufCodecs.VAR_INT, AbyssLoreProgressDataPayload::bridgeX,
                    ByteBufCodecs.VAR_INT, AbyssLoreProgressDataPayload::bridgeY,
                    ByteBufCodecs.VAR_INT, AbyssLoreProgressDataPayload::bridgeZ,
                    ByteBufCodecs.VAR_INT, AbyssLoreProgressDataPayload::castleX,
                    ByteBufCodecs.VAR_INT, AbyssLoreProgressDataPayload::castleY,
                    ByteBufCodecs.VAR_INT, AbyssLoreProgressDataPayload::castleZ,
                    BlockPos.STREAM_CODEC, AbyssLoreProgressDataPayload::colossusRegionCenter,
                    AbyssLoreProgressDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
