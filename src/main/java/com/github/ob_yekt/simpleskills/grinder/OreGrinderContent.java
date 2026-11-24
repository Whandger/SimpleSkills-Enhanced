package com.github.ob_yekt.simpleskills.grinder;

import com.github.ob_yekt.simpleskills.Simpleskills;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/** Registry entries for the Ore Grinder workstation. */
public final class OreGrinderContent {
    private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "ore_grinder"));
    private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "ore_grinder"));

    public static Block BLOCK;
    public static Item ITEM;
    public static BlockEntityType<OreGrinderBlockEntity> BLOCK_ENTITY_TYPE;
    public static MenuType<OreGrinderMenu> MENU_TYPE;
    private static boolean registered;

    private OreGrinderContent() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        BLOCK = Registry.register(BuiltInRegistries.BLOCK, BLOCK_KEY,
                new OreGrinderBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(4.0F, 6.0F)
                        .sound(SoundType.DEEPSLATE)
                        .lightLevel(state -> state.getValue(OreGrinderBlock.LIT) ? 7 : 0)
                        .requiresCorrectToolForDrops()
                        .setId(BLOCK_KEY)));

        BlockItem blockItem = new BlockItem(BLOCK,
                new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix());
        blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
        ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY, blockItem);

        BLOCK_ENTITY_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "ore_grinder"),
                new BlockEntityType<>(OreGrinderBlockEntity::new, Set.of(BLOCK)));

        MENU_TYPE = Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "ore_grinder"),
                new MenuType<>(OreGrinderMenu::new, FeatureFlagSet.of()));

        Simpleskills.LOGGER.info("Registered {}:ore_grinder", Simpleskills.MOD_ID);
    }
}
