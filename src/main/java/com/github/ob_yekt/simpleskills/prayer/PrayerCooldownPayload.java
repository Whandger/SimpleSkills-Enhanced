package com.github.ob_yekt.simpleskills.prayer;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authored cooldown feedback for the equipped Prayer ability HUD. */
public record PrayerCooldownPayload(String abilityId, int cooldownTicks) implements CustomPacketPayload {
    public static final Type<PrayerCooldownPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "prayer_cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PrayerCooldownPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PrayerCooldownPayload::abilityId,
            ByteBufCodecs.VAR_INT, PrayerCooldownPayload::cooldownTicks,
            PrayerCooldownPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
