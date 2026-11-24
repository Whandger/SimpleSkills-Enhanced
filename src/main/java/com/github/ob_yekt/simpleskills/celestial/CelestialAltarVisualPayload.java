package com.github.ob_yekt.simpleskills.celestial;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server-to-client confirmation payload that starts Celestial Altar visual animations
 * only after the server has validated and applied the operation.
 */
public record CelestialAltarVisualPayload(
        int containerId,
        int kind,
        ItemStack beforeEquipment,
        ItemStack afterEquipment,
        String targetId,
        String oldId,
        String oldTranslationKey,
        String newTranslationKey,
        double oldValue,
        double newValue,
        double minValue,
        double maxValue,
        int oldAmplifier,
        int newAmplifier,
        boolean reachedMax
) implements CustomPacketPayload {
    public static final int KIND_INFUSION = 1;
    public static final int KIND_SALVAGE = 2;
    public static final int KIND_ENHANCE_STAT = 3;
    public static final int KIND_REROLL_STAT = 4;
    public static final int KIND_REROLL_PERK = 5;

    public static final Type<CelestialAltarVisualPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "celestial_altar_visual"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CelestialAltarVisualPayload> CODEC =
            StreamCodec.ofMember(CelestialAltarVisualPayload::write, CelestialAltarVisualPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(kind);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, beforeEquipment == null ? ItemStack.EMPTY : beforeEquipment);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, afterEquipment == null ? ItemStack.EMPTY : afterEquipment);
        buf.writeUtf(targetId == null ? "" : targetId);
        buf.writeUtf(oldId == null ? "" : oldId);
        buf.writeUtf(oldTranslationKey == null ? "" : oldTranslationKey);
        buf.writeUtf(newTranslationKey == null ? "" : newTranslationKey);
        buf.writeDouble(oldValue);
        buf.writeDouble(newValue);
        buf.writeDouble(minValue);
        buf.writeDouble(maxValue);
        buf.writeVarInt(oldAmplifier);
        buf.writeVarInt(newAmplifier);
        buf.writeBoolean(reachedMax);
    }

    private static CelestialAltarVisualPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int kind = buf.readVarInt();
        ItemStack beforeEquipment = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack afterEquipment = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        String targetId = buf.readUtf();
        String oldId = buf.readUtf();
        String oldTranslationKey = buf.readUtf();
        String newTranslationKey = buf.readUtf();
        double oldValue = buf.readDouble();
        double newValue = buf.readDouble();
        double minValue = buf.readDouble();
        double maxValue = buf.readDouble();
        int oldAmplifier = buf.readVarInt();
        int newAmplifier = buf.readVarInt();
        boolean reachedMax = buf.readBoolean();
        return new CelestialAltarVisualPayload(
                containerId, kind, beforeEquipment, afterEquipment,
                targetId, oldId, oldTranslationKey, newTranslationKey,
                oldValue, newValue, minValue, maxValue,
                oldAmplifier, newAmplifier, reachedMax
        );
    }

    public static CelestialAltarVisualPayload infusion(int containerId, ItemStack beforeEquipment, ItemStack afterEquipment) {
        return new CelestialAltarVisualPayload(
                containerId, KIND_INFUSION,
                beforeEquipment.copy(), afterEquipment.copy(),
                "", "", "", "",
                0.0, 0.0, 0.0, 0.0,
                0, 0, false
        );
    }

    public static CelestialAltarVisualPayload salvage(int containerId, ItemStack sacrificedEquipment) {
        return new CelestialAltarVisualPayload(
                containerId, KIND_SALVAGE,
                sacrificedEquipment.copy(), new ItemStack(CelestialAltarContent.TRANSCENDENCY_ESSENCE),
                "", "", "", "",
                0.0, 0.0, 0.0, 0.0,
                0, 0, false
        );
    }

    public static CelestialAltarVisualPayload enhanceStat(
            int containerId, String statId, String translationKey,
            double oldValue, double newValue, double minValue, double maxValue, boolean reachedMax) {
        return new CelestialAltarVisualPayload(
                containerId, KIND_ENHANCE_STAT,
                ItemStack.EMPTY, ItemStack.EMPTY,
                statId, statId, translationKey, translationKey,
                oldValue, newValue, minValue, maxValue,
                0, 0, reachedMax
        );
    }

    public static CelestialAltarVisualPayload rerollStat(
            int containerId, String oldStatId, String oldTranslationKey, double oldValue,
            String newStatId, String newTranslationKey, double newValue,
            double newMinValue, double newMaxValue, boolean reachedMax) {
        return new CelestialAltarVisualPayload(
                containerId, KIND_REROLL_STAT,
                ItemStack.EMPTY, ItemStack.EMPTY,
                newStatId, oldStatId, oldTranslationKey, newTranslationKey,
                oldValue, newValue, newMinValue, newMaxValue,
                0, 0, reachedMax
        );
    }

    public static CelestialAltarVisualPayload rerollPerk(
            int containerId, String oldTargetId, String oldPerkId, int oldAmplifier,
            String newTargetId, String newPerkId, int newAmplifier) {
        return new CelestialAltarVisualPayload(
                containerId, KIND_REROLL_PERK,
                ItemStack.EMPTY, ItemStack.EMPTY,
                newTargetId, oldTargetId, oldPerkId, newPerkId,
                0.0, 0.0, 0.0, 0.0,
                oldAmplifier, newAmplifier, false
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
