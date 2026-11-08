package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client: a catch was confirmed by the server. The client only <em>shows</em> it (catch
 * result screen); it never generates or changes any value, and the screen has no influence on rewards.
 *
 * @param entityType  registry id of the fish entity type, for example {@code minecraft:cod}
 * @param nameKey     translation key of the species name
 * @param variant     species variant (packed tropical fish pattern), 0 when the species has none
 * @param weightKg    exact stored weight of the caught fish
 * @param lengthCm    exact stored length of the caught fish
 */
public record FishingCatchPayload(String entityType, String nameKey, int variant,
                                  float weightKg, float lengthCm, int xpGained) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FishingCatchPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "fishing_catch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FishingCatchPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, FishingCatchPayload::entityType,
                    ByteBufCodecs.STRING_UTF8, FishingCatchPayload::nameKey,
                    ByteBufCodecs.VAR_INT, FishingCatchPayload::variant,
                    ByteBufCodecs.FLOAT, FishingCatchPayload::weightKg,
                    ByteBufCodecs.FLOAT, FishingCatchPayload::lengthCm,
                    ByteBufCodecs.VAR_INT, FishingCatchPayload::xpGained,
                    FishingCatchPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
