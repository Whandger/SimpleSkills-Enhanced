package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server snapshot of every skill, as a JSON document with already translated text. */
public record SkillOverviewDataPayload(String json) implements CustomPacketPayload {
    public static final Type<SkillOverviewDataPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "skill_overview_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillOverviewDataPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SkillOverviewDataPayload::json,
                    SkillOverviewDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
