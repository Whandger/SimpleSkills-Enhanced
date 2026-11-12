package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server snapshot of unlocked title options and the player's equipped title. */
public record RankTitleMenuDataPayload(String rows, String labels, String selectedId, int status) implements CustomPacketPayload {
    public static final Type<RankTitleMenuDataPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rank_title_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RankTitleMenuDataPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RankTitleMenuDataPayload::rows,
                    ByteBufCodecs.STRING_UTF8, RankTitleMenuDataPayload::labels,
                    ByteBufCodecs.STRING_UTF8, RankTitleMenuDataPayload::selectedId,
                    ByteBufCodecs.VAR_INT, RankTitleMenuDataPayload::status,
                    RankTitleMenuDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
