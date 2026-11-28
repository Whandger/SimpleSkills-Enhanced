package com.github.ob_yekt.simpleskills.abyss.weapon;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Authoritative stance snapshot for one player. Times are expressed in game ticks. */
public record KatanaStancePayload(String playerUuid, int stance, int elapsedTicks, int sinceAttackTicks)
        implements CustomPacketPayload {
    public static final Type<KatanaStancePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "katana_stance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KatanaStancePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, KatanaStancePayload::playerUuid,
            ByteBufCodecs.VAR_INT, KatanaStancePayload::stance,
            ByteBufCodecs.VAR_INT, KatanaStancePayload::elapsedTicks,
            ByteBufCodecs.VAR_INT, KatanaStancePayload::sinceAttackTicks,
            KatanaStancePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
