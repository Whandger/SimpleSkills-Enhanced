package com.github.ob_yekt.simpleskills.prayer;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authored Prayer skillbook state. */
public record PrayerMenuDataPayload(String specialization, int level, String equipped, String abilityIds,
                                    String loadoutJson, int activeSlot)
        implements CustomPacketPayload {
    public static final Type<PrayerMenuDataPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "prayer_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PrayerMenuDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PrayerMenuDataPayload::specialization,
            ByteBufCodecs.VAR_INT, PrayerMenuDataPayload::level,
            ByteBufCodecs.STRING_UTF8, PrayerMenuDataPayload::equipped,
            ByteBufCodecs.STRING_UTF8, PrayerMenuDataPayload::abilityIds,
            ByteBufCodecs.STRING_UTF8, PrayerMenuDataPayload::loadoutJson,
            ByteBufCodecs.VAR_INT, PrayerMenuDataPayload::activeSlot,
            PrayerMenuDataPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
