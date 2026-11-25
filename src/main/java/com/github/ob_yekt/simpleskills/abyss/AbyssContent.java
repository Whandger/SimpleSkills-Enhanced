package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.runes.RuneOreBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import java.util.List;
import java.util.Set;

/** Content registrations for The Abyss underground expansion. */
public final class AbyssContent {
    private AbyssContent() {}

    public static Block ABYSSAL_STONE;
    public static Block CRACKED_ABYSSAL_STONE;
    public static Block AMETHYST_CORRUPTED_STONE;
    public static Block ABYSSAL_PILLAR;

    public static Block ABYSSAL_COAL_ORE;
    public static Block ABYSSAL_COPPER_ORE;
    public static Block ABYSSAL_IRON_ORE;
    public static Block ABYSSAL_GOLD_ORE;
    public static Block ABYSSAL_REDSTONE_ORE;
    public static Block ABYSSAL_LAPIS_ORE;
    public static Block ABYSSAL_DIAMOND_ORE;
    public static Block ABYSSAL_EMERALD_ORE;
    public static Block ABYSSAL_RUNE_ORE;
    public static Block TRANSCENDENT_ORE;
    public static Block ABYSSAL_POINTED_STALACTITE;

    // Gothic architectural blocks used by the monumental Abyssal Castle.
    public static Block ABYSSAL_BRICKS;
    public static Block CRACKED_ABYSSAL_BRICKS;
    public static Block ABYSSAL_TILES;
    public static Block CRACKED_ABYSSAL_TILES;
    public static Block POLISHED_ABYSSAL_STONE;
    public static Block CHISELED_ABYSSAL_STONE;
    public static Block GILDED_ABYSSAL_BRICKS;
    public static Block FROSTED_ABYSSAL_STONE;
    public static Block ABYSSAL_BRAZIER;
    public static Block ABYSSAL_BRICK_STAIRS;
    public static Block ABYSSAL_BRICK_SLAB;
    public static Block ABYSSAL_BRICK_WALL;
    public static Block ABYSSAL_TILE_STAIRS;
    public static Block ABYSSAL_TILE_SLAB;
    public static Block ABYSSAL_CHARRED_TIMBER;
    public static Block ABYSSAL_BLACK_BOOKSHELF;
    public static Block ABYSSAL_CHEST;
    public static Block ABYSSAL_FURNACE;
    public static BlockEntityType<AbyssalChestBlockEntity> ABYSSAL_CHEST_BLOCK_ENTITY_TYPE;
    public static BlockEntityType<AbyssalFurnaceBlockEntity> ABYSSAL_FURNACE_BLOCK_ENTITY_TYPE;
    public static MenuType<AbyssalChestMenu> ABYSSAL_CHEST_MENU_TYPE;
    public static MenuType<AbyssalChestMenu> ABYSSAL_DOUBLE_CHEST_MENU_TYPE;

    public static Item ABYSSAL_STONE_ITEM;
    public static Item CRACKED_ABYSSAL_STONE_ITEM;
    public static Item AMETHYST_CORRUPTED_STONE_ITEM;
    public static Item ABYSSAL_PILLAR_ITEM;
    public static Item ABYSSAL_COAL_ORE_ITEM;
    public static Item ABYSSAL_COPPER_ORE_ITEM;
    public static Item ABYSSAL_IRON_ORE_ITEM;
    public static Item ABYSSAL_GOLD_ORE_ITEM;
    public static Item ABYSSAL_REDSTONE_ORE_ITEM;
    public static Item ABYSSAL_LAPIS_ORE_ITEM;
    public static Item ABYSSAL_DIAMOND_ORE_ITEM;
    public static Item ABYSSAL_EMERALD_ORE_ITEM;
    public static Item ABYSSAL_RUNE_ORE_ITEM;
    public static Item TRANSCENDENT_ORE_ITEM;
    public static Item ABYSSAL_POINTED_STALACTITE_ITEM;

    public static Item ABYSSAL_BRICKS_ITEM;
    public static Item CRACKED_ABYSSAL_BRICKS_ITEM;
    public static Item ABYSSAL_TILES_ITEM;
    public static Item CRACKED_ABYSSAL_TILES_ITEM;
    public static Item POLISHED_ABYSSAL_STONE_ITEM;
    public static Item CHISELED_ABYSSAL_STONE_ITEM;
    public static Item GILDED_ABYSSAL_BRICKS_ITEM;
    public static Item FROSTED_ABYSSAL_STONE_ITEM;
    public static Item ABYSSAL_BRAZIER_ITEM;
    public static Item ABYSSAL_BRICK_STAIRS_ITEM;
    public static Item ABYSSAL_BRICK_SLAB_ITEM;
    public static Item ABYSSAL_BRICK_WALL_ITEM;
    public static Item ABYSSAL_TILE_STAIRS_ITEM;
    public static Item ABYSSAL_TILE_SLAB_ITEM;
    public static Item ABYSSAL_CHARRED_TIMBER_ITEM;
    public static Item ABYSSAL_BLACK_BOOKSHELF_ITEM;
    public static Item ABYSSAL_CHEST_ITEM;
    public static Item ABYSSAL_FURNACE_ITEM;

    private static boolean registered;

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path));
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        ABYSSAL_STONE = registerStone("abyssal_stone", 9.0F, 12.0F);
        CRACKED_ABYSSAL_STONE = registerStone("cracked_abyssal_stone", 8.0F, 12.0F);
        AMETHYST_CORRUPTED_STONE = registerStone("amethyst_corrupted_stone", 8.0F, 12.0F);
        ABYSSAL_PILLAR = registerStone("abyssal_pillar", 9.0F, 12.0F);

        // Legacy registry entry kept so existing worlds that already generated this
        // block can still load. New Abyss formations use solid Abyssal Pillar blocks.
        ResourceKey<Block> stalactiteKey = blockKey("abyssal_pointed_stalactite");
        ABYSSAL_POINTED_STALACTITE = Registry.register(BuiltInRegistries.BLOCK, stalactiteKey,
                new PointedDripstoneBlock(ABYSSAL_PILLAR.defaultBlockState(),
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.COLOR_GRAY)
                                .instrument(NoteBlockInstrument.BASEDRUM)
                                .strength(3.0F, 3.0F)
                                .sound(SoundType.DEEPSLATE)
                                .noOcclusion()
                                .setId(stalactiteKey)));

        ABYSSAL_COAL_ORE = registerOre("abyssal_coal_ore", 8.0F, 12.0F);
        ABYSSAL_COPPER_ORE = registerOre("abyssal_copper_ore", 8.0F, 12.0F);
        ABYSSAL_IRON_ORE = registerOre("abyssal_iron_ore", 8.0F, 12.0F);
        ABYSSAL_GOLD_ORE = registerOre("abyssal_gold_ore", 8.0F, 12.0F);
        ABYSSAL_REDSTONE_ORE = registerOre("abyssal_redstone_ore", 8.0F, 12.0F);
        ABYSSAL_LAPIS_ORE = registerOre("abyssal_lapis_ore", 8.0F, 12.0F);
        ABYSSAL_DIAMOND_ORE = registerOre("abyssal_diamond_ore", 8.0F, 12.0F);
        ABYSSAL_EMERALD_ORE = registerOre("abyssal_emerald_ore", 8.0F, 12.0F);

        ResourceKey<Block> runeKey = blockKey("abyssal_rune_ore");
        ABYSSAL_RUNE_ORE = Registry.register(BuiltInRegistries.BLOCK, runeKey,
                new RuneOreBlock(baseProperties(runeKey, 8.0F, 12.0F)));

        ResourceKey<Block> transcendentKey = blockKey("transcendent_ore");
        TRANSCENDENT_ORE = Registry.register(BuiltInRegistries.BLOCK, transcendentKey,
                new Block(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.GOLD)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(9.0F, 12.0F)
                        .sound(SoundType.AMETHYST)
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> 15)
                        .setId(transcendentKey)));

        // Monumental castle masonry and decoration blocks.
        ABYSSAL_BRICKS = registerStone("abyssal_bricks", 8.5F, 12.0F);
        CRACKED_ABYSSAL_BRICKS = registerStone("cracked_abyssal_bricks", 7.5F, 12.0F);
        ABYSSAL_TILES = registerStone("abyssal_tiles", 8.5F, 12.0F);
        CRACKED_ABYSSAL_TILES = registerStone("cracked_abyssal_tiles", 7.5F, 12.0F);
        POLISHED_ABYSSAL_STONE = registerStone("polished_abyssal_stone", 9.0F, 12.0F);
        CHISELED_ABYSSAL_STONE = registerStone("chiseled_abyssal_stone", 9.0F, 12.0F);
        GILDED_ABYSSAL_BRICKS = registerStone("gilded_abyssal_bricks", 9.0F, 12.0F);
        FROSTED_ABYSSAL_STONE = registerStone("frosted_abyssal_stone", 8.5F, 12.0F);

        ResourceKey<Block> brazierKey = blockKey("abyssal_brazier");
        ABYSSAL_BRAZIER = Registry.register(BuiltInRegistries.BLOCK, brazierKey,
                new Block(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.GOLD)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(8.0F, 12.0F)
                        .sound(SoundType.DEEPSLATE)
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> 14)
                        .setId(brazierKey)));

        ResourceKey<Block> brickStairsKey = blockKey("abyssal_brick_stairs");
        ABYSSAL_BRICK_STAIRS = Registry.register(BuiltInRegistries.BLOCK, brickStairsKey,
                new StairBlock(ABYSSAL_BRICKS.defaultBlockState(), baseProperties(brickStairsKey, 8.5F, 12.0F)));

        ResourceKey<Block> brickSlabKey = blockKey("abyssal_brick_slab");
        ABYSSAL_BRICK_SLAB = Registry.register(BuiltInRegistries.BLOCK, brickSlabKey,
                new SlabBlock(baseProperties(brickSlabKey, 8.5F, 12.0F)));

        ResourceKey<Block> brickWallKey = blockKey("abyssal_brick_wall");
        ABYSSAL_BRICK_WALL = Registry.register(BuiltInRegistries.BLOCK, brickWallKey,
                new WallBlock(baseProperties(brickWallKey, 8.5F, 12.0F)));

        ResourceKey<Block> tileStairsKey = blockKey("abyssal_tile_stairs");
        ABYSSAL_TILE_STAIRS = Registry.register(BuiltInRegistries.BLOCK, tileStairsKey,
                new StairBlock(ABYSSAL_TILES.defaultBlockState(), baseProperties(tileStairsKey, 8.5F, 12.0F)));

        ResourceKey<Block> tileSlabKey = blockKey("abyssal_tile_slab");
        ABYSSAL_TILE_SLAB = Registry.register(BuiltInRegistries.BLOCK, tileSlabKey,
                new SlabBlock(baseProperties(tileSlabKey, 8.5F, 12.0F)));

        ResourceKey<Block> charredTimberKey = blockKey("abyssal_charred_timber");
        // Abyssal timber is fossilized wood: stone-hard, axe-mineable, and harvested only
        // with diamond-tier axes or better. Its pillar axis supports trunks and dead branches.
        ABYSSAL_CHARRED_TIMBER = Registry.register(BuiltInRegistries.BLOCK, charredTimberKey,
                new RotatedPillarBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .instrument(NoteBlockInstrument.BASS)
                        .strength(9.0F, 12.0F)
                        .sound(SoundType.DEEPSLATE)
                        .requiresCorrectToolForDrops()
                        .setId(charredTimberKey)));

        ResourceKey<Block> blackBookshelfKey = blockKey("abyssal_black_bookshelf");
        ABYSSAL_BLACK_BOOKSHELF = Registry.register(BuiltInRegistries.BLOCK, blackBookshelfKey,
                new AbyssalBookshelfBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(5.0F, 12.0F)
                        .sound(SoundType.DEEPSLATE)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()
                        .setId(blackBookshelfKey)));

        ResourceKey<Block> chestKey = blockKey("abyssal_chest");
        ABYSSAL_CHEST = Registry.register(BuiltInRegistries.BLOCK, chestKey,
                new AbyssalChestBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(6.0F, 1200.0F)
                        .sound(SoundType.DEEPSLATE)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()
                        .setId(chestKey)));

        ResourceKey<Block> furnaceKey = blockKey("abyssal_furnace");
        ABYSSAL_FURNACE = Registry.register(BuiltInRegistries.BLOCK, furnaceKey,
                new AbyssalFurnaceBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(9.0F, 12.0F)
                        .sound(SoundType.DEEPSLATE)
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> state.getValue(AbstractFurnaceBlock.LIT) ? 9 : 0)
                        .setId(furnaceKey)));

        ABYSSAL_STONE_ITEM = registerBlockItem(ABYSSAL_STONE, "abyssal_stone");
        CRACKED_ABYSSAL_STONE_ITEM = registerBlockItem(CRACKED_ABYSSAL_STONE, "cracked_abyssal_stone");
        AMETHYST_CORRUPTED_STONE_ITEM = registerBlockItem(AMETHYST_CORRUPTED_STONE, "amethyst_corrupted_stone");
        ABYSSAL_PILLAR_ITEM = registerBlockItem(ABYSSAL_PILLAR, "abyssal_pillar");
        ABYSSAL_COAL_ORE_ITEM = registerBlockItem(ABYSSAL_COAL_ORE, "abyssal_coal_ore");
        ABYSSAL_COPPER_ORE_ITEM = registerBlockItem(ABYSSAL_COPPER_ORE, "abyssal_copper_ore");
        ABYSSAL_IRON_ORE_ITEM = registerBlockItem(ABYSSAL_IRON_ORE, "abyssal_iron_ore");
        ABYSSAL_GOLD_ORE_ITEM = registerBlockItem(ABYSSAL_GOLD_ORE, "abyssal_gold_ore");
        ABYSSAL_REDSTONE_ORE_ITEM = registerBlockItem(ABYSSAL_REDSTONE_ORE, "abyssal_redstone_ore");
        ABYSSAL_LAPIS_ORE_ITEM = registerBlockItem(ABYSSAL_LAPIS_ORE, "abyssal_lapis_ore");
        ABYSSAL_DIAMOND_ORE_ITEM = registerBlockItem(ABYSSAL_DIAMOND_ORE, "abyssal_diamond_ore");
        ABYSSAL_EMERALD_ORE_ITEM = registerBlockItem(ABYSSAL_EMERALD_ORE, "abyssal_emerald_ore");
        ABYSSAL_RUNE_ORE_ITEM = registerBlockItem(ABYSSAL_RUNE_ORE, "abyssal_rune_ore");
        TRANSCENDENT_ORE_ITEM = registerBlockItem(TRANSCENDENT_ORE, "transcendent_ore");
        ABYSSAL_POINTED_STALACTITE_ITEM = registerBlockItem(
                ABYSSAL_POINTED_STALACTITE, "abyssal_pointed_stalactite");

        ABYSSAL_BRICKS_ITEM = registerBlockItem(ABYSSAL_BRICKS, "abyssal_bricks");
        CRACKED_ABYSSAL_BRICKS_ITEM = registerBlockItem(CRACKED_ABYSSAL_BRICKS, "cracked_abyssal_bricks");
        ABYSSAL_TILES_ITEM = registerBlockItem(ABYSSAL_TILES, "abyssal_tiles");
        CRACKED_ABYSSAL_TILES_ITEM = registerBlockItem(CRACKED_ABYSSAL_TILES, "cracked_abyssal_tiles");
        POLISHED_ABYSSAL_STONE_ITEM = registerBlockItem(POLISHED_ABYSSAL_STONE, "polished_abyssal_stone");
        CHISELED_ABYSSAL_STONE_ITEM = registerBlockItem(CHISELED_ABYSSAL_STONE, "chiseled_abyssal_stone");
        GILDED_ABYSSAL_BRICKS_ITEM = registerBlockItem(GILDED_ABYSSAL_BRICKS, "gilded_abyssal_bricks");
        FROSTED_ABYSSAL_STONE_ITEM = registerBlockItem(FROSTED_ABYSSAL_STONE, "frosted_abyssal_stone");
        ABYSSAL_BRAZIER_ITEM = registerBlockItem(ABYSSAL_BRAZIER, "abyssal_brazier");
        ABYSSAL_BRICK_STAIRS_ITEM = registerBlockItem(ABYSSAL_BRICK_STAIRS, "abyssal_brick_stairs");
        ABYSSAL_BRICK_SLAB_ITEM = registerBlockItem(ABYSSAL_BRICK_SLAB, "abyssal_brick_slab");
        ABYSSAL_BRICK_WALL_ITEM = registerBlockItem(ABYSSAL_BRICK_WALL, "abyssal_brick_wall");
        ABYSSAL_TILE_STAIRS_ITEM = registerBlockItem(ABYSSAL_TILE_STAIRS, "abyssal_tile_stairs");
        ABYSSAL_TILE_SLAB_ITEM = registerBlockItem(ABYSSAL_TILE_SLAB, "abyssal_tile_slab");
        ABYSSAL_CHARRED_TIMBER_ITEM = registerBlockItem(ABYSSAL_CHARRED_TIMBER, "abyssal_charred_timber");
        ABYSSAL_BLACK_BOOKSHELF_ITEM = registerBlockItem(ABYSSAL_BLACK_BOOKSHELF, "abyssal_black_bookshelf");
        ABYSSAL_CHEST_ITEM = registerBlockItem(ABYSSAL_CHEST, "abyssal_chest");
        ABYSSAL_FURNACE_ITEM = registerBlockItem(ABYSSAL_FURNACE, "abyssal_furnace");

        ABYSSAL_CHEST_BLOCK_ENTITY_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyssal_chest"),
                new BlockEntityType<>(AbyssalChestBlockEntity::new, Set.of(ABYSSAL_CHEST)));
        ABYSSAL_FURNACE_BLOCK_ENTITY_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyssal_furnace"),
                new BlockEntityType<>(AbyssalFurnaceBlockEntity::new, Set.of(ABYSSAL_FURNACE)));
        ABYSSAL_CHEST_MENU_TYPE = Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyssal_chest"),
                new MenuType<>(AbyssalChestMenu::new, FeatureFlagSet.of()));
        ABYSSAL_DOUBLE_CHEST_MENU_TYPE = Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyssal_double_chest"),
                new MenuType<>(AbyssalChestMenu::createDoubleChestClient, FeatureFlagSet.of()));

        // Transcendent Ore drops CelestialAltarContent.TRANSCENDENCY_ESSENCE via loot table.
        // Do not register a duplicate essence item: the altar already owns this item.
        Simpleskills.LOGGER.info("Registered The Abyss blocks and ores.");
    }

    private static Block registerStone(String id, float hardness, float resistance) {
        ResourceKey<Block> key = blockKey(id);
        return Registry.register(BuiltInRegistries.BLOCK, key,
                new Block(baseProperties(key, hardness, resistance)));
    }

    private static Block registerOre(String id, float hardness, float resistance) {
        ResourceKey<Block> key = blockKey(id);
        return Registry.register(BuiltInRegistries.BLOCK, key,
                new Block(baseProperties(key, hardness, resistance)));
    }

    private static BlockBehaviour.Properties baseProperties(ResourceKey<Block> key,
                                                              float hardness,
                                                              float resistance) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(hardness, resistance)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops()
                .setId(key);
    }

    private static Item registerBlockItem(Block block, String id) {
        ResourceKey<Item> key = itemKey(id);
        BlockItem item = new BlockItem(block,
                new Item.Properties().setId(key).useBlockDescriptionPrefix());
        item.registerBlocks(Item.BY_BLOCK, item);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    /** Items visible in the SimpleSkills creative tab. */
    public static List<Item> creativeItems() {
        return List.of(
                ABYSSAL_STONE_ITEM,
                CRACKED_ABYSSAL_STONE_ITEM,
                AMETHYST_CORRUPTED_STONE_ITEM,
                ABYSSAL_PILLAR_ITEM,
                ABYSSAL_COAL_ORE_ITEM,
                ABYSSAL_COPPER_ORE_ITEM,
                ABYSSAL_IRON_ORE_ITEM,
                ABYSSAL_GOLD_ORE_ITEM,
                ABYSSAL_REDSTONE_ORE_ITEM,
                ABYSSAL_LAPIS_ORE_ITEM,
                ABYSSAL_DIAMOND_ORE_ITEM,
                ABYSSAL_EMERALD_ORE_ITEM,
                ABYSSAL_RUNE_ORE_ITEM,
                TRANSCENDENT_ORE_ITEM,
                ABYSSAL_BRICKS_ITEM,
                CRACKED_ABYSSAL_BRICKS_ITEM,
                ABYSSAL_TILES_ITEM,
                CRACKED_ABYSSAL_TILES_ITEM,
                POLISHED_ABYSSAL_STONE_ITEM,
                CHISELED_ABYSSAL_STONE_ITEM,
                GILDED_ABYSSAL_BRICKS_ITEM,
                FROSTED_ABYSSAL_STONE_ITEM,
                ABYSSAL_BRAZIER_ITEM,
                ABYSSAL_BRICK_STAIRS_ITEM,
                ABYSSAL_BRICK_SLAB_ITEM,
                ABYSSAL_BRICK_WALL_ITEM,
                ABYSSAL_TILE_STAIRS_ITEM,
                ABYSSAL_TILE_SLAB_ITEM,
                ABYSSAL_CHARRED_TIMBER_ITEM,
                ABYSSAL_BLACK_BOOKSHELF_ITEM,
                ABYSSAL_CHEST_ITEM,
                ABYSSAL_FURNACE_ITEM
        );
    }
}
