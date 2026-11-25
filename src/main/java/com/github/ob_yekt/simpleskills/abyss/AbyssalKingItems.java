package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Registered equipment rewards dropped by the Abyssal Fallen King. */
public final class AbyssalKingItems {
    private static final Identifier SWORD_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "abyssal_king_sword");
    private static final Identifier CROWN_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "abyssal_king_crown");

    private static final ResourceKey<Item> SWORD_KEY = ResourceKey.create(Registries.ITEM, SWORD_ID);
    private static final ResourceKey<Item> CROWN_KEY = ResourceKey.create(Registries.ITEM, CROWN_ID);

    public static Item ABYSSAL_KING_SWORD;
    public static Item ABYSSAL_KING_CROWN;

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

        Simpleskills.LOGGER.info("Registered Abyssal King's Sword and Crown.");
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

    public static boolean isKingRelic(net.minecraft.world.item.ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(ABYSSAL_KING_SWORD) || stack.is(ABYSSAL_KING_CROWN));
    }

    public static List<Item> creativeItems() {
        return List.of(ABYSSAL_KING_SWORD, ABYSSAL_KING_CROWN);
    }
}
