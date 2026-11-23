package com.github.ob_yekt.simpleskills.crafting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Small, frequently updated preview of the blocks Worldbreaker will break. */
public record WorldbreakerPreviewPayload(String json) implements CustomPacketPayload {
    public static final Type<WorldbreakerPreviewPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "worldbreaker_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldbreakerPreviewPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, WorldbreakerPreviewPayload::json,
                    WorldbreakerPreviewPayload::new);

    public static void registerPayload() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
