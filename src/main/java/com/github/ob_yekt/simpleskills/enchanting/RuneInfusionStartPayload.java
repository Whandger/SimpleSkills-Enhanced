package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Client request only. Every field is treated as untrusted and revalidated on the server. */
public record RuneInfusionStartPayload(int inventorySlot, List<String> enchantmentIds,
                                       List<Integer> targetLevels) implements CustomPacketPayload {
    public static final Type<RuneInfusionStartPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rune_infusion_start"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<String>> STRING_LIST =
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8, 4);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Integer>> INT_LIST =
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT, 4);

    public static final StreamCodec<RegistryFriendlyByteBuf, RuneInfusionStartPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RuneInfusionStartPayload::inventorySlot,
                    STRING_LIST, RuneInfusionStartPayload::enchantmentIds,
                    INT_LIST, RuneInfusionStartPayload::targetLevels,
                    RuneInfusionStartPayload::new);

    public RuneInfusionStartPayload {
        enchantmentIds = enchantmentIds == null ? List.of() : List.copyOf(enchantmentIds);
        targetLevels = targetLevels == null ? List.of() : List.copyOf(targetLevels);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
