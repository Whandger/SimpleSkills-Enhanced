package com.github.ob_yekt.simpleskills.farming;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Auto Plant (Farming 5+): breaking (left-click) a MATURE crop with any hoe harvests it and replants
 * the same crop in the same block.
 *
 * If the harvest produces a seed, one matching seed is consumed for replanting.
 * If no seed drops, the crop is still replanted without consuming an item.
 *
 * How it stays duplicate-free: this is called from {@code AutoPlantMixin}, which sits on
 * {@link Block#dropResources} - the single funnel vanilla uses to spawn a broken block's drops. We
 * roll the loot table once (with the player and the hoe, so Fortune & co. apply), replant, and spawn
 * the remaining drops. Vanilla's own drop call is cancelled only when we handled it, so drops
 * are never spawned twice. Farming XP and the Harvest Bonus are NOT touched here: they keep running
 * once from the existing {@code PlayerBlockBreakEvents.AFTER} handler, which receives the original
 * mature state.
 */
public final class AutoPlant {
    private AutoPlant() {}

    /**
     * @return true when Auto Plant handled the drops (the caller must then cancel vanilla's drop call),
     *         false when the break must proceed exactly as vanilla.
     */
    public static boolean tryHarvestAndReplant(BlockState state, Level level, BlockPos pos,
                                               BlockEntity blockEntity, Entity breaker, ItemStack tool) {
        if (!(level instanceof ServerLevel serverLevel) || !(breaker instanceof ServerPlayer player)) {
            return false;
        }
        if (tool == null || !tool.is(ItemTags.HOES)) {
            return false;
        }
        if (!CropHelper.isAutoPlantCrop(state) || !CropHelper.isMature(state)) {
            return false; // immature or unidentifiable -> vanilla behaviour
        }
        if (!FarmingPerks.isAutoPlantUnlocked(FarmingPerks.getFarmingLevel(player))) {
            return false;
        }

        // The block was already removed by vanilla right before dropResources; it must still be free.
        BlockState replant = CropHelper.freshlyPlanted(state);
        if (replant == null) {
            return false;
        }

        // Normal harvest drops, rolled exactly once (Fortune etc. come from the tool).
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, player, tool);

        boolean replanted = false;
        if (serverLevel.getBlockState(pos).isAir() && replant.canSurvive(serverLevel, pos)) {
            // Replant independently of random seed drops.
            serverLevel.setBlockAndUpdate(pos, replant);
            replanted = true;

            // Consume one matching seed only if the harvest actually produced one.
            for (ItemStack drop : drops) {
                if (CropHelper.isSeedFor(drop, state)) {
                    drop.shrink(1);
                    break;
                }
            }
        }

        // Deliver everything that is left, same as vanilla would have.
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                Block.popResource(serverLevel, pos, drop);
            }
        }
        state.spawnAfterBreak(serverLevel, pos, tool, true);

        Simpleskills.LOGGER.debug("Auto Plant: {} harvested {} at {} (replanted={})",
                player.getName().getString(), state.getBlock().getDescriptionId(), pos, replanted);
        return true;
    }
}
