package com.github.ob_yekt.simpleskills.celestial;

import com.github.ob_yekt.simpleskills.Simpleskills;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/** Registrations for the Celestial Altar and its crafting catalyst. */
public final class CelestialAltarContent {
    private static final ResourceKey<Block> ALTAR_BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "celestial_altar"));
    private static final ResourceKey<Item> ALTAR_ITEM_KEY = ResourceKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "celestial_altar"));
    private static final ResourceKey<Item> ESSENCE_ITEM_KEY = ResourceKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "transcendency_essence"));

    public static Block CELESTIAL_ALTAR;
    public static Item CELESTIAL_ALTAR_ITEM;
    public static Item TRANSCENDENCY_ESSENCE;
    public static BlockEntityType<CelestialAltarBlockEntity> BLOCK_ENTITY_TYPE;
    public static MenuType<CelestialAltarMenu> MENU_TYPE;
    private static boolean registered;

    private CelestialAltarContent() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        TRANSCENDENCY_ESSENCE = Registry.register(BuiltInRegistries.ITEM, ESSENCE_ITEM_KEY,
                new TranscendencyEssenceItem(new Item.Properties().setId(ESSENCE_ITEM_KEY).stacksTo(64)));

        CELESTIAL_ALTAR = Registry.register(BuiltInRegistries.BLOCK, ALTAR_BLOCK_KEY,
                new CelestialAltarBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PURPLE)
                        .instrument(NoteBlockInstrument.CHIME)
                        .strength(5.0F, 1200.0F)
                        .sound(SoundType.AMETHYST)
                        .requiresCorrectToolForDrops()
                        .setId(ALTAR_BLOCK_KEY)));

        BlockItem altarItem = new BlockItem(CELESTIAL_ALTAR,
                new Item.Properties().setId(ALTAR_ITEM_KEY).useBlockDescriptionPrefix());
        altarItem.registerBlocks(Item.BY_BLOCK, altarItem);
        CELESTIAL_ALTAR_ITEM = Registry.register(BuiltInRegistries.ITEM, ALTAR_ITEM_KEY, altarItem);

        BLOCK_ENTITY_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "celestial_altar"),
                new BlockEntityType<>(CelestialAltarBlockEntity::new, Set.of(CELESTIAL_ALTAR)));

        MENU_TYPE = Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "celestial_altar"),
                new MenuType<>(CelestialAltarMenu::new, FeatureFlagSet.of()));

        Simpleskills.LOGGER.info("Registered Celestial Altar and Transcendency Essence.");
    }
}
