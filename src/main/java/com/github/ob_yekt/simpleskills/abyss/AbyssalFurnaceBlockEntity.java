package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla smelting furnace whose cooking progress runs at three times normal speed. */
public final class AbyssalFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("block.simpleskills.abyssal_furnace");

    public AbyssalFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(AbyssContent.ABYSSAL_FURNACE_BLOCK_ENTITY_TYPE, pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FurnaceMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    protected float getSpeedMultiplier(ServerLevel level, ItemStack fuelItem) {
        return 3.0F;
    }
}
