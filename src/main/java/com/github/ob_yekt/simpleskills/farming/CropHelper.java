package com.github.ob_yekt.simpleskills.farming;

import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collections;

/**
 * Generic (non vanilla-ID based) helpers shared by Bone Meal Mastery and Auto Plant.
 * Nothing here hard-codes a block id, so crops from other mods work whenever they follow the usual
 * conventions (CropBlock subclass, or #minecraft:crops / farming_xp.json entry with an "age" property).
 */
public final class CropHelper {
    private CropHelper() {}

    /**
     * Blocks that count as a "plantation" for Bone Meal Mastery. The caller must still check
     * {@code BonemealableBlock#isValidBonemealTarget}. Grass, saplings, moss, etc. are NOT plantations,
     * so a hoe right-click on them keeps its normal behaviour (tilling).
     */
    public static boolean isBoneMealPlantation(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.CROPS)
                || block instanceof CropBlock
                || block instanceof StemBlock
                || block instanceof CocoaBlock
                || ConfigManager.isCropBlock(block.getDescriptionId());
    }

    /**
     * Crops that Auto Plant can harvest and replant: they must have an "age" integer property
     * (so "mature" and "freshly planted" are well defined) and must not be two-block plants or stems.
     * Anything we cannot identify safely keeps the vanilla behaviour.
     */
    public static boolean isAutoPlantCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof StemBlock || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return false;
        }
        if (ageProperty(state) == null) {
            return false;
        }
        return block instanceof CropBlock
                || state.is(BlockTags.CROPS)
                || ConfigManager.isCropBlock(block.getDescriptionId());
    }

    /** The "age" property of a crop state, or null when it has none. */
    public static IntegerProperty ageProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && "age".equals(property.getName())) {
                return integerProperty;
            }
        }
        return null;
    }

    /** True when the crop has reached its highest age. */
    public static boolean isMature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        IntegerProperty age = ageProperty(state);
        return age != null && state.getValue(age) >= Collections.max(age.getPossibleValues());
    }

    /** The same crop block, back at its lowest age (keeps facing etc.). Null if it has no age. */
    public static BlockState freshlyPlanted(BlockState state) {
        IntegerProperty age = ageProperty(state);
        if (age == null) {
            return null;
        }
        return state.setValue(age, Collections.min(age.getPossibleValues()));
    }

    /**
     * True when this stack is the "seed" of the given crop: an item that places exactly that block.
     * Wheat seeds -> wheat, carrot -> carrots, potato -> potatoes, beetroot seeds -> beetroots,
     * nether wart -> nether_wart, cocoa beans -> cocoa, and any modded item that places the same block.
     */
    public static boolean isSeedFor(ItemStack stack, BlockState cropState) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() == cropState.getBlock();
    }
}
