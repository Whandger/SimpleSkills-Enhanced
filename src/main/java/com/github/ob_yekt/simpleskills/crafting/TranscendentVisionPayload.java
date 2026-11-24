package com.github.ob_yekt.simpleskills.crafting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Per-player scan results for Oracle's Vision and Worldbreaker. */
public record TranscendentVisionPayload(String json) implements CustomPacketPayload {
    public static final Type<TranscendentVisionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "transcendent_vision"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TranscendentVisionPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, TranscendentVisionPayload::json,
                    TranscendentVisionPayload::new);

    public static void registerPayload() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
