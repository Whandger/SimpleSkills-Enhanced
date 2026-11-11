package com.github.ob_yekt.simpleskills.runes;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/** Content registrations for the Rune Enchanting resource system. */
public final class RuneContent {
    private RuneContent() {}

    public static final ResourceKey<Block> RUNE_ORE_BLOCK_KEY = blockKey("rune_ore");
    public static final ResourceKey<Block> DEEPSLATE_RUNE_ORE_BLOCK_KEY = blockKey("deepslate_rune_ore");
    public static final ResourceKey<Item> RUNE_ORE_ITEM_KEY = itemKey("rune_ore");
    public static final ResourceKey<Item> DEEPSLATE_RUNE_ORE_ITEM_KEY = itemKey("deepslate_rune_ore");
    public static final ResourceKey<Item> RUNE_ITEM_KEY = itemKey("rune");

    public static Block RUNE_ORE;
    public static Block DEEPSLATE_RUNE_ORE;
    public static Item RUNE_ORE_ITEM;
    public static Item DEEPSLATE_RUNE_ORE_ITEM;
    public static Item RUNE;
    private static boolean registered;

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path));
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        RUNE_ORE = Registry.register(BuiltInRegistries.BLOCK, RUNE_ORE_BLOCK_KEY,
                new RuneOreBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(3.0F, 3.0F)
                        .sound(SoundType.STONE)
                        .requiresCorrectToolForDrops()
                        .setId(RUNE_ORE_BLOCK_KEY)));

        DEEPSLATE_RUNE_ORE = Registry.register(BuiltInRegistries.BLOCK, DEEPSLATE_RUNE_ORE_BLOCK_KEY,
                new RuneOreBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(4.5F, 3.0F)
                        .sound(SoundType.DEEPSLATE)
                        .requiresCorrectToolForDrops()
                        .setId(DEEPSLATE_RUNE_ORE_BLOCK_KEY)));

        RUNE_ORE_ITEM = registerBlockItem(RUNE_ORE, RUNE_ORE_ITEM_KEY);
        DEEPSLATE_RUNE_ORE_ITEM = registerBlockItem(DEEPSLATE_RUNE_ORE, DEEPSLATE_RUNE_ORE_ITEM_KEY);
        RUNE = Registry.register(BuiltInRegistries.ITEM, RUNE_ITEM_KEY,
                new Item(new Item.Properties().setId(RUNE_ITEM_KEY)));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            entries.accept(RUNE_ORE_ITEM);
            entries.accept(DEEPSLATE_RUNE_ORE_ITEM);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> entries.accept(RUNE));

        Simpleskills.LOGGER.info("Registered Rune, Rune Ore and Deepslate Rune Ore.");
    }

    private static Item registerBlockItem(Block block, ResourceKey<Item> key) {
        BlockItem item = new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix());
        item.registerBlocks(Item.BY_BLOCK, item);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }
}
