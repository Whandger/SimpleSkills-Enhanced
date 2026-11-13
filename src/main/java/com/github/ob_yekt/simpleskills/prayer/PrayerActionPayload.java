package com.github.ob_yekt.simpleskills.prayer;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Untrusted Prayer UI/cast request; action and unlock state are verified on the server. */
public record PrayerActionPayload(int action, String value) implements CustomPacketPayload {
    public static final int OPEN = 0, SELECT_SPECIALIZATION = 1, EQUIP = 2, CAST = 3,
            SELECT_SLOT = 4, OPEN_WHEEL = 5;
    public static final Type<PrayerActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "prayer_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PrayerActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PrayerActionPayload::action, ByteBufCodecs.STRING_UTF8, PrayerActionPayload::value,
            PrayerActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
