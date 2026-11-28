package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Abyssal stone furnace with the vanilla furnace inventory and UI. */
public final class AbyssalFurnaceBlock extends AbstractFurnaceBlock {
    public AbyssalFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AbyssalFurnaceBlockEntity(pos, state);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof AbyssalFurnaceBlockEntity furnace) {
            player.openMenu(furnace);
        }
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createFurnaceTicker(level, type, AbyssContent.ABYSSAL_FURNACE_BLOCK_ENTITY_TYPE);
    }
}
