package com.github.ob_yekt.simpleskills.abyss.weapon;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request sent when the player starts an attack with the katana. */
public record KatanaStanceRequestPayload(int action) implements CustomPacketPayload {
    public static final int ATTACK = 0;

    public static final Type<KatanaStanceRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "katana_stance_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KatanaStanceRequestPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, KatanaStanceRequestPayload::action,
                    KatanaStanceRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
