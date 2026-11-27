package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Registered relic equipment rewards from the Abyssal bosses. */
public final class AbyssalKingItems {
    private static final Identifier SWORD_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "abyssal_king_sword");
    private static final Identifier CROWN_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "abyssal_king_crown");
    private static final Identifier SAMURAI_CHESTPLATE_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "abyssal_samurai_chestplate");
    private static final Identifier SAMURAI_HELMET_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "abyssal_samurai_helmet");
    private static final Identifier SAMURAI_LEGGINGS_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "abyssal_samurai_leggings");
    private static final Identifier SAMURAI_BOOTS_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "abyssal_samurai_boots");

    private static final ResourceKey<Item> SWORD_KEY = ResourceKey.create(Registries.ITEM, SWORD_ID);
    private static final ResourceKey<Item> CROWN_KEY = ResourceKey.create(Registries.ITEM, CROWN_ID);
    private static final ResourceKey<Item> SAMURAI_CHESTPLATE_KEY =
            ResourceKey.create(Registries.ITEM, SAMURAI_CHESTPLATE_ID);
    private static final ResourceKey<Item> SAMURAI_HELMET_KEY =
            ResourceKey.create(Registries.ITEM, SAMURAI_HELMET_ID);
    private static final ResourceKey<Item> SAMURAI_LEGGINGS_KEY =
            ResourceKey.create(Registries.ITEM, SAMURAI_LEGGINGS_ID);
    private static final ResourceKey<Item> SAMURAI_BOOTS_KEY =
            ResourceKey.create(Registries.ITEM, SAMURAI_BOOTS_ID);

    private static final ResourceKey<EquipmentAsset> SAMURAI_CHESTPLATE_ASSET_KEY =
            ResourceKey.create(EquipmentAssets.ROOT_ID, SAMURAI_CHESTPLATE_ID);
    private static final ResourceKey<EquipmentAsset> SAMURAI_LEGGINGS_ASSET_KEY =
            ResourceKey.create(EquipmentAssets.ROOT_ID, SAMURAI_LEGGINGS_ID);
    private static final ResourceKey<EquipmentAsset> SAMURAI_BOOTS_ASSET_KEY =
            ResourceKey.create(EquipmentAssets.ROOT_ID, SAMURAI_BOOTS_ID);

    public static Item ABYSSAL_KING_SWORD;
    public static Item ABYSSAL_KING_CROWN;
    public static Item ABYSSAL_SAMURAI_CHESTPLATE;
    public static Item ABYSSAL_SAMURAI_HELMET;
    public static Item ABYSSAL_SAMURAI_LEGGINGS;
    public static Item ABYSSAL_SAMURAI_BOOTS;

    private static boolean registered;

    private AbyssalKingItems() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // Player base damage (1) + 7 baseline + Netherite material bonus (4) = 12 attack damage.
        ABYSSAL_KING_SWORD = Registry.register(BuiltInRegistries.ITEM, SWORD_KEY,
                new Item(new Item.Properties()
                        .setId(SWORD_KEY)
                        .modelId(SWORD_ID)
                        .sword(ToolMaterial.NETHERITE, 7.0F, -2.4F)
                        .durability(2600)
                        .enchantable(20)
                        .repairable(Items.NETHERITE_INGOT)
                        .fireResistant()));

        ABYSSAL_KING_CROWN = Registry.register(BuiltInRegistries.ITEM, CROWN_KEY,
                new Item(new Item.Properties()
                        .setId(CROWN_KEY)
                        .modelId(CROWN_ID)
                        .equippable(EquipmentSlot.HEAD)
                        .durability(1000)
                        .enchantable(20)
                        .repairable(Items.NETHERITE_INGOT)
                        .fireResistant()
                        .attributes(createCrownAttributes())));

        ABYSSAL_SAMURAI_CHESTPLATE = Registry.register(BuiltInRegistries.ITEM, SAMURAI_CHESTPLATE_KEY,
                new Item(new Item.Properties()
                        .setId(SAMURAI_CHESTPLATE_KEY)
                        .modelId(SAMURAI_CHESTPLATE_ID)
                        .component(DataComponents.EQUIPPABLE,
                                Equippable.builder(EquipmentSlot.CHEST)
                                        .setAsset(SAMURAI_CHESTPLATE_ASSET_KEY)
                                        .build())
                        .durability(1400)
                        .enchantable(24)
                        .repairable(Items.NETHERITE_INGOT)
                        .fireResistant()
                        .attributes(createSamuraiPieceAttributes(
                                "abyssal_samurai_chestplate", 9.0D, 4.0D, EquipmentSlotGroup.CHEST))));

        // Legacy registry entry only: players no longer receive/equip this helmet.
        // The Samurai's kasa remains part of AbyssalSamuraiModel and is unaffected.
        // Keeping the identifier avoids breaking old saved ItemStacks.
        ABYSSAL_SAMURAI_HELMET = Registry.register(BuiltInRegistries.ITEM, SAMURAI_HELMET_KEY,
                new Item(new Item.Properties()
                        .setId(SAMURAI_HELMET_KEY)
                        .modelId(SAMURAI_HELMET_ID)
                        .durability(900)
                        .enchantable(20)
                        .repairable(Items.NETHERITE_INGOT)
                        .fireResistant()));

        ABYSSAL_SAMURAI_LEGGINGS = Registry.register(BuiltInRegistries.ITEM, SAMURAI_LEGGINGS_KEY,
                new Item(new Item.Properties()
                        .setId(SAMURAI_LEGGINGS_KEY)
                        .modelId(SAMURAI_LEGGINGS_ID)
                        .component(DataComponents.EQUIPPABLE,
                                Equippable.builder(EquipmentSlot.LEGS)
                                        .setAsset(SAMURAI_LEGGINGS_ASSET_KEY)
                                        .build())
                        .durability(1400)
                        .enchantable(24)
                        .repairable(Items.NETHERITE_INGOT)
                        .fireResistant()
                        .attributes(createSamuraiPieceAttributes(
                                "abyssal_samurai_leggings", 7.0D, 4.0D, EquipmentSlotGroup.LEGS))));

        ABYSSAL_SAMURAI_BOOTS = Registry.register(BuiltInRegistries.ITEM, SAMURAI_BOOTS_KEY,
                new Item(new Item.Properties()
                        .setId(SAMURAI_BOOTS_KEY)
                        .modelId(SAMURAI_BOOTS_ID)
                        .component(DataComponents.EQUIPPABLE,
                                Equippable.builder(EquipmentSlot.FEET)
                                        .setAsset(SAMURAI_BOOTS_ASSET_KEY)
                                        .build())
                        .durability(1400)
                        .enchantable(24)
                        .repairable(Items.NETHERITE_INGOT)
                        .fireResistant()
                        .attributes(createSamuraiPieceAttributes(
                                "abyssal_samurai_boots", 4.0D, 4.0D, EquipmentSlotGroup.FEET))));

        Simpleskills.LOGGER.info("Registered Abyssal King relics and the Samurai armor set.");
    }

    private static ItemAttributeModifiers createCrownAttributes() {
        return ItemAttributeModifiers.EMPTY
                .withModifierAdded(Attributes.ARMOR,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(
                                Simpleskills.MOD_ID, "abyssal_king_crown_armor"),
                                5.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HEAD)
                .withModifierAdded(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(
                                Simpleskills.MOD_ID, "abyssal_king_crown_toughness"),
                                4.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HEAD);
    }

    /**
     * Samurai armor is deliberately stronger than Netherite per wearable slot:
     * chestplate 9/4, leggings 7/4 and boots 4/4 (armor/toughness).
     * The separate kasa is boss-only geometry, not a player armor item.
     */
    private static ItemAttributeModifiers createSamuraiPieceAttributes(
            String id, double armor, double toughness, EquipmentSlotGroup slot) {
        return ItemAttributeModifiers.EMPTY
                .withModifierAdded(Attributes.ARMOR,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(
                                Simpleskills.MOD_ID, id + "_armor"),
                                armor, AttributeModifier.Operation.ADD_VALUE),
                        slot)
                .withModifierAdded(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(
                                Simpleskills.MOD_ID, id + "_toughness"),
                                toughness, AttributeModifier.Operation.ADD_VALUE),
                        slot);
    }

    public static boolean isKingRelic(net.minecraft.world.item.ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(ABYSSAL_KING_SWORD) || stack.is(ABYSSAL_KING_CROWN)
                || stack.is(ABYSSAL_SAMURAI_CHESTPLATE));
    }

    public static List<Item> creativeItems() {
        return List.of(
                ABYSSAL_KING_SWORD, ABYSSAL_KING_CROWN,
                ABYSSAL_SAMURAI_CHESTPLATE,
                ABYSSAL_SAMURAI_LEGGINGS, ABYSSAL_SAMURAI_BOOTS);
    }
}
