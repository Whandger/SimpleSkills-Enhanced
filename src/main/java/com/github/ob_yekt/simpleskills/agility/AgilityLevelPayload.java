package com.github.ob_yekt.simpleskills.agility;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-to-client Agility level used only for responsive local key handling. */
public record AgilityLevelPayload(int level, int dashUnlockLevel, int doubleJumpUnlockLevel)
        implements CustomPacketPayload {
    public static final Type<AgilityLevelPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "agility_level"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AgilityLevelPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AgilityLevelPayload::level,
                    ByteBufCodecs.VAR_INT, AgilityLevelPayload::dashUnlockLevel,
                    ByteBufCodecs.VAR_INT, AgilityLevelPayload::doubleJumpUnlockLevel,
                    AgilityLevelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
