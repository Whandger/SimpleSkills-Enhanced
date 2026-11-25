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
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import java.util.List;

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
                        .mapColor(MapColor.COLOR_PURPLE)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(9.0F, 12.0F)
                        .sound(SoundType.AMETHYST)
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> 15)
                        .setId(transcendentKey)));

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
                TRANSCENDENT_ORE_ITEM
        );
    }
}
