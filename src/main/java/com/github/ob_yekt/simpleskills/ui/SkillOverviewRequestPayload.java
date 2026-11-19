package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client asks the server for a fresh skill overview snapshot. */
public record SkillOverviewRequestPayload(int action) implements CustomPacketPayload {
    public static final int OPEN = 0;
    public static final Type<SkillOverviewRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "skill_overview_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillOverviewRequestPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SkillOverviewRequestPayload::action,
                    SkillOverviewRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
