package com.github.ob_yekt.simpleskills.grinder;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Furnace-style input/fuel/output menu aligned to the industrial Ore Grinder layout. */
public final class OreGrinderMenu extends AbstractContainerMenu {
    private final Container container;
    private final OreGrinderBlockEntity grinder;
    private final DataSlot burnTime = DataSlot.standalone();
    private final DataSlot totalBurnTime = DataSlot.standalone();
    private final DataSlot progress = DataSlot.standalone();
    private final DataSlot totalProgress = DataSlot.standalone();

    /** Client constructor. */
    public OreGrinderMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(OreGrinderBlockEntity.SLOT_COUNT), null);
    }

    /** Server constructor. */
    public OreGrinderMenu(int containerId, Inventory inventory, OreGrinderBlockEntity grinder) {
        this(containerId, inventory, grinder, grinder);
    }

    private OreGrinderMenu(int containerId, Inventory inventory, Container container,
                           OreGrinderBlockEntity grinder) {
        super(OreGrinderContent.MENU_TYPE, containerId);
        checkContainerSize(container, OreGrinderBlockEntity.SLOT_COUNT);
        this.container = container;
        this.grinder = grinder;
        container.startOpen(inventory.player);

        // Left control column: Equipment input (top) & Fuel input (bottom)
        addSlot(new Slot(container, OreGrinderBlockEntity.INPUT_SLOT, 23, 42) {
            @Override public boolean mayPlace(ItemStack stack) { return OreGrinderSalvage.canRecover(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(container, OreGrinderBlockEntity.FUEL_SLOT, 23, 82) {
            @Override public boolean mayPlace(ItemStack stack) {
                return grinder != null ? grinder.canPlaceItem(OreGrinderBlockEntity.FUEL_SLOT, stack)
                        : OreGrinderBlockEntity.isFuel(stack);
            }
        });

        // Right control column: Recovered mineral output
        addSlot(new Slot(container, OreGrinderBlockEntity.OUTPUT_SLOT, 165, 62) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Centered 9-column player inventory (27 main slots at y=127..179, 9 hotbar slots at y=185..201)
        addStandardInventorySlots(inventory, 22, 127);

        addDataSlot(burnTime);
        addDataSlot(totalBurnTime);
        addDataSlot(progress);
        addDataSlot(totalProgress);
    }

    @Override
    public void broadcastChanges() {
        if (grinder != null) {
            burnTime.set(grinder.getBurnTime());
            totalBurnTime.set(grinder.getTotalBurnTime());
            progress.set(grinder.getGrindProgress());
            totalProgress.set(grinder.getGrindTime());
        }
        super.broadcastChanges();
    }

    public int getBurnTime() { return burnTime.get(); }
    public int getTotalBurnTime() { return totalBurnTime.get(); }
    public int getProgress() { return progress.get(); }
    public int getTotalProgress() { return Math.max(1, totalProgress.get()); }
    public boolean isBurning() { return getBurnTime() > 0; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index == OreGrinderBlockEntity.OUTPUT_SLOT) {
            if (!moveItemStackTo(source, 3, slots.size(), true)) return ItemStack.EMPTY;
            slot.onQuickCraft(source, original);
        } else if (index >= 3) {
            boolean moved = false;
            if (OreGrinderSalvage.canRecover(source) && !slots.get(OreGrinderBlockEntity.INPUT_SLOT).hasItem()) {
                moved = moveItemStackTo(source, 0, 1, false);
            }
            if (!moved && isFuel(source) && !slots.get(OreGrinderBlockEntity.FUEL_SLOT).hasItem()) {
                moved = moveItemStackTo(source, 1, 2, false);
            }
            if (!moved) {
                if (index < 30) moved = moveItemStackTo(source, 30, slots.size(), false);
                else moved = moveItemStackTo(source, 3, 30, false);
            }
            if (!moved) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(source, 3, slots.size(), false)) return ItemStack.EMPTY;
        }

        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        return original;
    }

    private boolean isFuel(ItemStack stack) {
        if (grinder != null) return grinder.canPlaceItem(OreGrinderBlockEntity.FUEL_SLOT, stack);
        return OreGrinderBlockEntity.isFuel(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return grinder == null || grinder.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
