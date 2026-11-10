package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RuneInfusionCatalogRequestPayload(int inventorySlot) implements CustomPacketPayload {
    public static final Type<RuneInfusionCatalogRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rune_infusion_catalog_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuneInfusionCatalogRequestPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RuneInfusionCatalogRequestPayload::inventorySlot,
                    RuneInfusionCatalogRequestPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
