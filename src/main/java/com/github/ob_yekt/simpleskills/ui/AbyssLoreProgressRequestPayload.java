package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request for the player's current general level and the Abyss structure locations. */
public record AbyssLoreProgressRequestPayload(int request) implements CustomPacketPayload {
    public static final int REFRESH = 0;
    public static final Type<AbyssLoreProgressRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyss_lore_progress_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbyssLoreProgressRequestPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, AbyssLoreProgressRequestPayload::request,
                    AbyssLoreProgressRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
