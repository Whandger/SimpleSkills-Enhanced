package com.github.ob_yekt.simpleskills.crafting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authoritative batch of notable crafted equipment to reveal on the client. */
public record CraftingRevealPayload(String entriesJson) implements CustomPacketPayload {
    public static final Type<CraftingRevealPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "crafting_equipment_reveal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingRevealPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CraftingRevealPayload::entriesJson,
                    CraftingRevealPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
