package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client requests to open the title menu, equip an unlocked title, or clear it. */
public record RankTitleMenuActionPayload(int action, String titleId) implements CustomPacketPayload {
    public static final int OPEN = 0;
    public static final int SELECT = 1;
    public static final int CLEAR = 2;
    public static final Type<RankTitleMenuActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rank_title_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RankTitleMenuActionPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RankTitleMenuActionPayload::action,
                    ByteBufCodecs.STRING_UTF8, RankTitleMenuActionPayload::titleId,
                    RankTitleMenuActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
