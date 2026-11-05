package com.github.ob_yekt.simpleskills.utils;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single source of truth for "which skill does this block belong to".
 * Used by both the XP system (EventHandlers) and Gathering Speed, so a block can never
 * give XP for one skill and a speed bonus for another.
 *
 * Priority (unchanged from the original XP logic):
 *   1. Configured crop blocks -> FARMING
 *   2. Explicit mapping from block_mappings.json (ConfigManager)
 *   3. Vanilla tag fallback: pickaxe -> MINING, shovel -> EXCAVATING, axe -> WOODCUTTING
 *   4. Otherwise null (no skill)
 */
public final class BlockSkillClassifier {
    private BlockSkillClassifier() {}

    public static Skills classify(BlockState state) {
        String blockTranslationKey = state.getBlock().getDescriptionId();

        if (ConfigManager.isCropBlock(blockTranslationKey)) {
            return Skills.FARMING;
        }

        Skills explicit = ConfigManager.getBlockSkill(blockTranslationKey);
        if (explicit != null) {
            return explicit;
        }

        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return Skills.MINING;
        } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return Skills.EXCAVATING;
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return Skills.WOODCUTTING;
        }
        return null;
    }
}
