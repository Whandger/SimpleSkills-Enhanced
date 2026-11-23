package com.github.ob_yekt.simpleskills.celestial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.github.ob_yekt.simpleskills.runes.RuneContent;
import org.jetbrains.annotations.Nullable;

/** Persistent altar inventory for infusion, salvage and rune-based attribute enhancement. */
public final class CelestialAltarBlockEntity extends BlockEntity implements Container, net.minecraft.world.MenuProvider {
    public static final int SLOT_COUNT = 8;
    public static final int INFUSION_EQUIPMENT = 0;
    public static final int ESSENCE_TOP = 1;
    public static final int ESSENCE_LEFT = 2;
    public static final int ESSENCE_RIGHT = 3;
    public static final int SALVAGE_EQUIPMENT = 4;
    public static final int SALVAGE_OUTPUT = 5;
    public static final int ENHANCE_EQUIPMENT = 6;
    public static final int ENHANCE_RUNES = 7;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public CelestialAltarBlockEntity(BlockPos pos, BlockState state) {
        super(CelestialAltarContent.BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot < 0 || slot >= SLOT_COUNT ? ItemStack.EMPTY : items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) return ItemStack.EMPTY;
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed;
        if (stack.getCount() <= amount) {
            removed = stack;
            items.set(slot, ItemStack.EMPTY);
        } else {
            removed = stack.split(amount);
            if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return ItemStack.EMPTY;
        ItemStack stack = items.set(slot, ItemStack.EMPTY);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        items.set(slot, stack == null ? ItemStack.EMPTY : stack);
        if (!items.get(slot).isEmpty() && items.get(slot).getCount() > getMaxStackSize()) {
            items.get(slot).setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override public int getMaxStackSize() { return 64; }
    @Override public void setChanged() { super.setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INFUSION_EQUIPMENT) return com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.isEligibleForCelestialAltar(stack);
        if (slot >= ESSENCE_TOP && slot <= ESSENCE_RIGHT) {
            return stack.is(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
        }
        if (slot == SALVAGE_EQUIPMENT) {
            return com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.isTranscendent(stack)
                    && com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.isEligibleForCelestialAltar(stack);
        }
        if (slot == ENHANCE_EQUIPMENT) {
            return com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.isEligibleForCelestialAltar(stack);
        }
        if (slot == ENHANCE_RUNES) return stack.is(RuneContent.RUNE);
        return false;
    }

    @Override public Component getDisplayName() { return Component.translatable("block.simpleskills.celestial_altar"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CelestialAltarMenu(containerId, inventory, this);
    }
}
