package com.github.ob_yekt.simpleskills.crafting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request to toggle the 3x3 area effect of Transcendent tools. */
public record TranscendentToolTogglePayload(int action) implements CustomPacketPayload {
    public static final int TOGGLE_AREA_EFFECT = 0;

    public static final Type<TranscendentToolTogglePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "transcendent_tool_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TranscendentToolTogglePayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, TranscendentToolTogglePayload::action,
                    TranscendentToolTogglePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
