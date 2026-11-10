package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server acknowledgement/result. Client values are display-only. */
public record RuneInfusionResultPayload(int resultCode, int runeCost, int grade) implements CustomPacketPayload {
    public static final int STARTED = 0;
    public static final int PLAYER_BUSY = 1;
    public static final int INVALID_EQUIPMENT = 2;
    public static final int INVALID_SELECTION = 3;
    public static final int NOT_ENOUGH_RUNES = 4;
    public static final int PAYMENT_FAILED = 5;
    public static final int SUCCEEDED = 6;
    public static final int FAILED = 7;

    public static final Type<RuneInfusionResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rune_infusion_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuneInfusionResultPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RuneInfusionResultPayload::resultCode,
                    ByteBufCodecs.VAR_INT, RuneInfusionResultPayload::runeCost,
                    ByteBufCodecs.VAR_INT, RuneInfusionResultPayload::grade,
                    RuneInfusionResultPayload::new);

    public RuneInfusionResultPayload(int resultCode, int runeCost) { this(resultCode, runeCost, -1); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
