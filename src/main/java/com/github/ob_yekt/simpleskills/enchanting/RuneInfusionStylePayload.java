package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request to change only the cosmetic glint style of an already enchanted item. */
public record RuneInfusionStylePayload(int inventorySlot, int styleId) implements CustomPacketPayload {
    public static final Type<RuneInfusionStylePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rune_infusion_style"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuneInfusionStylePayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RuneInfusionStylePayload::inventorySlot,
                    ByteBufCodecs.VAR_INT, RuneInfusionStylePayload::styleId,
                    RuneInfusionStylePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
