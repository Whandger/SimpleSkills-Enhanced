package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Raw and cooked items for the additional fish species. */
public final class FishingItems {
    public static Item RAW_TROUT;
    public static Item RAW_BASS;
    public static Item RAW_CATFISH;
    public static Item RAW_PIKE;
    public static Item RAW_CARP;
    public static Item RAW_TUNA;
    public static Item RAW_SWORDFISH;
    public static Item RAW_MAHI_MAHI;
    public static Item RAW_REEF_SHARK;
    public static Item RAW_GREAT_WHITE_SHARK;
    public static Item RAW_WHALE_SHARK;

    public static Item COOKED_TROUT;
    public static Item COOKED_BASS;
    public static Item COOKED_CATFISH;
    public static Item COOKED_PIKE;
    public static Item COOKED_CARP;
    public static Item COOKED_TUNA;
    public static Item COOKED_SWORDFISH;
    public static Item COOKED_MAHI_MAHI;
    public static Item COOKED_REEF_SHARK;
    public static Item COOKED_GREAT_WHITE_SHARK;
    public static Item COOKED_WHALE_SHARK;

    private static boolean registered;

    private FishingItems() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // Raw fish restore a little hunger and very little saturation, matching the role of raw cod.
        RAW_TROUT = registerFood("raw_trout", 2, 0.4F);
        RAW_BASS = registerFood("raw_bass", 2, 0.4F);
        RAW_CATFISH = registerFood("raw_catfish", 3, 0.6F);
        RAW_PIKE = registerFood("raw_pike", 2, 0.4F);
        RAW_CARP = registerFood("raw_carp", 2, 0.4F);
        RAW_TUNA = registerFood("raw_tuna", 3, 0.6F);
        RAW_SWORDFISH = registerFood("raw_swordfish", 3, 0.6F);
        RAW_MAHI_MAHI = registerFood("raw_mahi_mahi", 3, 0.6F);
        RAW_REEF_SHARK = registerFood("raw_reef_shark", 3, 0.6F);
        RAW_GREAT_WHITE_SHARK = registerFood("raw_great_white_shark", 3, 0.6F);
        RAW_WHALE_SHARK = registerFood("raw_whale_shark", 4, 0.8F);

        // Cooked fish provide substantially more nutrition and saturation.
        COOKED_TROUT = registerFood("cooked_trout", 5, 6.0F);
        COOKED_BASS = registerFood("cooked_bass", 5, 6.0F);
        COOKED_CATFISH = registerFood("cooked_catfish", 6, 7.2F);
        COOKED_PIKE = registerFood("cooked_pike", 5, 6.0F);
        COOKED_CARP = registerFood("cooked_carp", 5, 6.0F);
        COOKED_TUNA = registerFood("cooked_tuna", 6, 7.2F);
        COOKED_SWORDFISH = registerFood("cooked_swordfish", 6, 7.2F);
        COOKED_MAHI_MAHI = registerFood("cooked_mahi_mahi", 6, 7.2F);
        COOKED_REEF_SHARK = registerFood("cooked_reef_shark", 6, 7.2F);
        COOKED_GREAT_WHITE_SHARK = registerFood("cooked_great_white_shark", 7, 8.4F);
        COOKED_WHALE_SHARK = registerFood("cooked_whale_shark", 7, 8.4F);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(RAW_TROUT);
            entries.accept(RAW_BASS);
            entries.accept(RAW_CATFISH);
            entries.accept(RAW_PIKE);
            entries.accept(RAW_CARP);
            entries.accept(RAW_TUNA);
            entries.accept(RAW_SWORDFISH);
            entries.accept(RAW_MAHI_MAHI);
            entries.accept(RAW_REEF_SHARK);
            entries.accept(RAW_GREAT_WHITE_SHARK);
            entries.accept(RAW_WHALE_SHARK);

            entries.accept(COOKED_TROUT);
            entries.accept(COOKED_BASS);
            entries.accept(COOKED_CATFISH);
            entries.accept(COOKED_PIKE);
            entries.accept(COOKED_CARP);
            entries.accept(COOKED_TUNA);
            entries.accept(COOKED_SWORDFISH);
            entries.accept(COOKED_MAHI_MAHI);
            entries.accept(COOKED_REEF_SHARK);
            entries.accept(COOKED_GREAT_WHITE_SHARK);
            entries.accept(COOKED_WHALE_SHARK);
        });

        Simpleskills.LOGGER.info("Registered 11 raw and 11 cooked additional Fishing food items.");
    }

    private static Item registerFood(String path, int nutrition, float saturation) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path));
        FoodProperties food = new FoodProperties(nutrition, saturation, false);
        return Registry.register(BuiltInRegistries.ITEM, key,
                new Item(new Item.Properties().setId(key).food(food)));
    }
}
