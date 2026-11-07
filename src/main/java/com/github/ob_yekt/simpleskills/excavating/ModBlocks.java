package com.github.ob_yekt.simpleskills.excavating;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Registers {@code simpleskills:investigated_sand}.
 *
 * It is a real vanilla {@link SandBlock} created with exactly the same properties as
 * {@code minecraft:sand} (same strength, sound, map colour, instrument and dust colour), so it
 * falls, breaks, drops, can be picked up and placed again, and sounds exactly like sand.
 * Its model uses the vanilla sand texture, so it looks identical as well.
 *
 * The block itself is the permanent "already investigated" mark: no coordinates are stored.
 */
public final class ModBlocks {
    private ModBlocks() {}

    public static final String INVESTIGATED_SAND_NAME = "investigated_sand";

    public static final ResourceKey<Block> INVESTIGATED_SAND_BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, INVESTIGATED_SAND_NAME));

    public static final ResourceKey<Item> INVESTIGATED_SAND_ITEM_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, INVESTIGATED_SAND_NAME));

    public static Block INVESTIGATED_SAND;
    public static Item INVESTIGATED_SAND_ITEM;

    private static boolean registered = false;

    /** Registers the block and its item. Must run during mod initialization. */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        // Same dust colour and properties as Blocks.SAND.
        Block block = new SandBlock(
                new ColorRGBA(14406560),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.SAND)
                        .instrument(NoteBlockInstrument.SNARE)
                        .strength(0.5F)
                        .sound(SoundType.SAND)
                        .setId(INVESTIGATED_SAND_BLOCK_KEY)
        );
        INVESTIGATED_SAND = Registry.register(BuiltInRegistries.BLOCK, INVESTIGATED_SAND_BLOCK_KEY, block);

        BlockItem item = new BlockItem(
                block,
                new Item.Properties()
                        .setId(INVESTIGATED_SAND_ITEM_KEY)
                        .useBlockDescriptionPrefix()
        );
        // Same as vanilla Items.registerItem: lets Block#asItem() resolve to this item.
        item.registerBlocks(Item.BY_BLOCK, item);
        INVESTIGATED_SAND_ITEM = Registry.register(BuiltInRegistries.ITEM, INVESTIGATED_SAND_ITEM_KEY, item);

        Simpleskills.LOGGER.info("Registered {}:{}", Simpleskills.MOD_ID, INVESTIGATED_SAND_NAME);
    }
}
