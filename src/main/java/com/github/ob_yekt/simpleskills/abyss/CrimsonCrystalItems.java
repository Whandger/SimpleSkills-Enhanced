package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.abyss.item.CrimsonRuneItem;
import com.github.ob_yekt.simpleskills.abyss.item.CrimsonCrystalShardItem;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Endgame Crimson Crystal gear. The set beats Netherite but deliberately stays
 * below the unique King and Samurai relics.
 */
public final class CrimsonCrystalItems {
    private static final String MOD = Simpleskills.MOD_ID;

    private static final TagKey<Item> REPAIR_ITEMS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(MOD, "crimson_crystal_repairs"));

    private static final ToolMaterial MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            2_600,
            10.0F,
            5.0F,
            18,
            REPAIR_ITEMS
    );

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD, path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD, path);
    }

    private static ResourceKey<EquipmentAsset> assetKey(String path) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, id(path));
    }

    public static Item CRIMSON_RUNE;
    public static Item RED_CRYSTAL_SHARD;
    public static Item ABYSSAL_WOOD_STICK;
    public static Item CRIMSON_CRYSTAL_SWORD;
    public static Item CRIMSON_CRYSTAL_PICKAXE;
    public static Item CRIMSON_CRYSTAL_AXE;
    public static Item CRIMSON_CRYSTAL_SHOVEL;
    public static Item CRIMSON_CRYSTAL_HOE;
    public static Item CRIMSON_CRYSTAL_HELMET;
    public static Item CRIMSON_CRYSTAL_CHESTPLATE;
    public static Item CRIMSON_CRYSTAL_LEGGINGS;
    public static Item CRIMSON_CRYSTAL_BOOTS;

    private static boolean registered;

    private CrimsonCrystalItems() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        CRIMSON_RUNE = registerRune();
        RED_CRYSTAL_SHARD = registerCrystalShard();
        ABYSSAL_WOOD_STICK = registerSimple("abyssal_wood_stick", 64);

        CRIMSON_CRYSTAL_SWORD = registerTool("crimson_crystal_sword",
                new Item.Properties().sword(MATERIAL, 3.0F, -2.4F));
        CRIMSON_CRYSTAL_PICKAXE = registerTool("crimson_crystal_pickaxe",
                new Item.Properties().pickaxe(MATERIAL, 1.0F, -2.8F));
        CRIMSON_CRYSTAL_AXE = registerTool("crimson_crystal_axe",
                new Item.Properties().axe(MATERIAL, 4.5F, -3.1F));
        CRIMSON_CRYSTAL_SHOVEL = registerTool("crimson_crystal_shovel",
                new Item.Properties().shovel(MATERIAL, 2.0F, -3.0F));
        CRIMSON_CRYSTAL_HOE = registerTool("crimson_crystal_hoe",
                new Item.Properties().hoe(MATERIAL, 1.0F, -3.0F));

        CRIMSON_CRYSTAL_HELMET = registerArmor("crimson_crystal_helmet", EquipmentSlot.HEAD,
                1_350, "crimson_crystal_helmet", 3.5D, 3.25D, EquipmentSlotGroup.HEAD);
        CRIMSON_CRYSTAL_CHESTPLATE = registerArmor("crimson_crystal_chestplate", EquipmentSlot.CHEST,
                1_680, "crimson_crystal_chestplate", 8.5D, 3.5D, EquipmentSlotGroup.CHEST);
        CRIMSON_CRYSTAL_LEGGINGS = registerArmor("crimson_crystal_leggings", EquipmentSlot.LEGS,
                1_570, "crimson_crystal_leggings", 6.5D, 3.5D, EquipmentSlotGroup.LEGS);
        CRIMSON_CRYSTAL_BOOTS = registerArmor("crimson_crystal_boots", EquipmentSlot.FEET,
                1_440, "crimson_crystal_boots", 3.5D, 3.25D, EquipmentSlotGroup.FEET);

        Simpleskills.LOGGER.info("Registered Crimson Runestone, Crimson Crystal shard, Abyssal timber stick and nine gear items.");
    }

    private static Item registerRune() {
        ResourceKey<Item> key = itemKey("crimson_rune");
        Identifier model = id("crimson_rune");
        return Registry.register(BuiltInRegistries.ITEM, key,
                new CrimsonRuneItem(new Item.Properties().setId(key).modelId(model)));
    }

    private static Item registerCrystalShard() {
        ResourceKey<Item> key = itemKey("red_crystal_shard");
        Identifier model = id("red_crystal_shard");
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .modelId(model)
                .stacksTo(64)
                // Same burn duration as a lava bucket (20,000 ticks) in furnaces and furnace-like blocks.
                .cookingFuel(ContextIntProviders.COOKING_TIME_LAVA_BUCKET)
                .fireResistant();
        return Registry.register(BuiltInRegistries.ITEM, key, new CrimsonCrystalShardItem(properties));
    }

    private static Item registerSimple(String path, int stackSize) {
        ResourceKey<Item> key = itemKey(path);
        Identifier model = id(path);
        return Registry.register(BuiltInRegistries.ITEM, key,
                new Item(new Item.Properties()
                        .setId(key)
                        .modelId(model)
                        .stacksTo(stackSize)));
    }

    private static Item registerTool(String path, Item.Properties toolProperties) {
        ResourceKey<Item> key = itemKey(path);
        Identifier model = id(path);
        Item.Properties properties = toolProperties
                .setId(key)
                .modelId(model)
                .enchantable(18)
                .repairable(REPAIR_ITEMS)
                .fireResistant();
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties));
    }

    private static Item registerArmor(String path, EquipmentSlot slot, int durability,
                                      String assetName, double armor, double toughness,
                                      EquipmentSlotGroup group) {
        ResourceKey<Item> key = itemKey(path);
        Identifier model = id(path);
        ResourceKey<EquipmentAsset> asset = assetKey("crimson_crystal");
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .modelId(model)
                .component(DataComponents.EQUIPPABLE,
                        Equippable.builder(slot).setAsset(asset).build())
                .durability(durability)
                .enchantable(20)
                .repairable(REPAIR_ITEMS)
                .fireResistant()
                .attributes(armorAttributes(path, armor, toughness, group));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties));
    }

    private static ItemAttributeModifiers armorAttributes(String path, double armor,
                                                           double toughness, EquipmentSlotGroup slot) {
        return ItemAttributeModifiers.EMPTY
                .withModifierAdded(Attributes.ARMOR,
                        new AttributeModifier(id(path + "_armor"), armor,
                                AttributeModifier.Operation.ADD_VALUE), slot)
                .withModifierAdded(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(id(path + "_toughness"), toughness,
                                AttributeModifier.Operation.ADD_VALUE), slot);
    }

    /** Items shown on the SimpleSkills creative tab. */
    public static List<Item> creativeItems() {
        return List.of(
                CRIMSON_RUNE, RED_CRYSTAL_SHARD, ABYSSAL_WOOD_STICK,
                CRIMSON_CRYSTAL_SWORD, CRIMSON_CRYSTAL_PICKAXE, CRIMSON_CRYSTAL_AXE,
                CRIMSON_CRYSTAL_SHOVEL, CRIMSON_CRYSTAL_HOE,
                CRIMSON_CRYSTAL_HELMET, CRIMSON_CRYSTAL_CHESTPLATE,
                CRIMSON_CRYSTAL_LEGGINGS, CRIMSON_CRYSTAL_BOOTS
        );
    }
}
