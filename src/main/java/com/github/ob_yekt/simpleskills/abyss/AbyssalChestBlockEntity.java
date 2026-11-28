package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent six-row (54-slot) container with native loot-table support. */
public final class AbyssalChestBlockEntity extends RandomizableContainerBlockEntity {
    public static final int ROWS = 6;
    public static final int SLOT_COUNT = ROWS * 9;

    // Each physical Abyssal chest stores 54 slots. A joined pair therefore exposes
    // 108 slots through CompoundContainer, without virtual pages or hidden storage.
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public AbyssalChestBlockEntity(BlockPos pos, BlockState state) {
        super(AbyssContent.ABYSSAL_CHEST_BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    /**
     * Explicitly materialize the chest's loot table before applying its one-shot gear rarity markers.
     * CompoundContainer delegates this call to both physical halves of a double chest.
     */
    @Override
    public void startOpen(ContainerUser user) {
        if (!(user instanceof Player player) || level == null || level.isClientSide()) {
            return;
        }

        unpackLootTable(player);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean changed = false;
            for (int slot = 0; slot < getContainerSize(); slot++) {
                if (CraftingEquipmentManager.applyAbyssalLootRarity(getItem(slot), serverPlayer)) {
                    changed = true;
                }
            }
            if (changed) setChanged();
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.simpleskills.abyssal_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AbyssalChestMenu(containerId, inventory, this);
    }

    /** Drop all stored items when mined; chunk unloads do not call this removal hook. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) return;
        if (!isEmpty()) {
            Containers.dropContents(level, pos, this);
        }
        clearContent();
    }
}
