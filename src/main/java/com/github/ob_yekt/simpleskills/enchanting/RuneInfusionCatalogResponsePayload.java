package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-derived enchantment catalog and cosmetic style state for the selected inventory item. */
public record RuneInfusionCatalogResponsePayload(
        int inventorySlot, int runeCount, int runeLoreCount, int currentStyle, int unlockedLevel, String rows)
        implements CustomPacketPayload {
    public static final Type<RuneInfusionCatalogResponsePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rune_infusion_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuneInfusionCatalogResponsePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RuneInfusionCatalogResponsePayload::inventorySlot,
                    ByteBufCodecs.VAR_INT, RuneInfusionCatalogResponsePayload::runeCount,
                    ByteBufCodecs.VAR_INT, RuneInfusionCatalogResponsePayload::runeLoreCount,
                    ByteBufCodecs.VAR_INT, RuneInfusionCatalogResponsePayload::currentStyle,
                    ByteBufCodecs.VAR_INT, RuneInfusionCatalogResponsePayload::unlockedLevel,
                    ByteBufCodecs.STRING_UTF8, RuneInfusionCatalogResponsePayload::rows,
                    RuneInfusionCatalogResponsePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
