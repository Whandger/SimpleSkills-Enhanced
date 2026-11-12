package com.github.ob_yekt.simpleskills.agility;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-to-client notification that starts the Dash cooldown indicator. */
public record AgilityDashCooldownPayload(int cooldownTicks) implements CustomPacketPayload {
    public static final Type<AgilityDashCooldownPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "agility_dash_cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AgilityDashCooldownPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT,
                    AgilityDashCooldownPayload::cooldownTicks, AgilityDashCooldownPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
