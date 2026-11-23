package com.github.ob_yekt.simpleskills.celestial;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request for a Celestial Altar UI action. All item and rune checks happen server-side. */
public record CelestialAltarActionPayload(int action, String targetId) implements CustomPacketPayload {
    public static final int TAB_INFUSION = 0;
    public static final int TAB_SALVAGE = 1;
    public static final int TAB_ENHANCE = 2;
    public static final int ASCEND = 3;
    public static final int SALVAGE = 4;
    public static final int ENHANCE_STAT = 5;
    public static final int REROLL_PERK = 6;

    public static final Type<CelestialAltarActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "celestial_altar_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CelestialAltarActionPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CelestialAltarActionPayload::action,
                    ByteBufCodecs.STRING_UTF8, CelestialAltarActionPayload::targetId,
                    CelestialAltarActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
