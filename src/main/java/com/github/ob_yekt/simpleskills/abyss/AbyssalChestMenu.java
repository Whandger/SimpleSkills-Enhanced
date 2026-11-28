package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Custom Abyssal chest menu: 54 slots singly, 108 slots in a double chest. */
public final class AbyssalChestMenu extends AbstractContainerMenu {
    private static final int ROWS = 6;
    private static final int SLOT_SIZE = 18;
    private static final int MENU_SIDE_PADDING = 14;

    private final Container container;
    private final int columns;
    private final int playerInventoryX;
    private final int playerInventoryY;

    /** Client factory for one Abyssal chest (54 slots). */
    public AbyssalChestMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(AbyssalChestBlockEntity.SLOT_COUNT),
                AbyssContent.ABYSSAL_CHEST_MENU_TYPE);
    }

    /** Server constructor. Uses the double menu type when two chests are combined. */
    public AbyssalChestMenu(int containerId, Inventory inventory, Container container) {
        this(containerId, inventory, container,
                container.getContainerSize() > AbyssalChestBlockEntity.SLOT_COUNT
                        ? AbyssContent.ABYSSAL_DOUBLE_CHEST_MENU_TYPE
                        : AbyssContent.ABYSSAL_CHEST_MENU_TYPE);
    }

    /** Client factory for a joined double Abyssal chest (108 slots). */
    public static AbyssalChestMenu createDoubleChestClient(int containerId, Inventory inventory) {
        return new AbyssalChestMenu(containerId, inventory,
                new SimpleContainer(AbyssalChestBlockEntity.SLOT_COUNT * 2),
                AbyssContent.ABYSSAL_DOUBLE_CHEST_MENU_TYPE);
    }

    private AbyssalChestMenu(int containerId, Inventory inventory, Container container,
                             MenuType<AbyssalChestMenu> menuType) {
        super(menuType, containerId);
        int size = container.getContainerSize();
        if (size != AbyssalChestBlockEntity.SLOT_COUNT
                && size != AbyssalChestBlockEntity.SLOT_COUNT * 2) {
            throw new IllegalArgumentException("Abyssal chest must contain 54 or 108 slots, got " + size);
        }

        this.container = container;
        this.columns = size / ROWS;
        checkContainerSize(container, size);
        container.startOpen(inventory.player);

        // Chest contents occupy one six-row grid. A double chest simply grows to 18 columns.
        for (int slot = 0; slot < size; slot++) {
            int column = slot % columns;
            int row = slot / columns;
            addSlot(new Slot(container, slot,
                    8 + column * SLOT_SIZE,
                    18 + row * SLOT_SIZE));
        }

        // Keep the player's 36 slots centered under the chest, even when the chest is 18 columns wide.
        this.playerInventoryX = (getImageWidthForColumns(columns) - 162) / 2;
        this.playerInventoryY = ROWS * SLOT_SIZE + 31;
        addStandardInventorySlots(inventory, playerInventoryX, playerInventoryY);
    }

    public int getRows() {
        return ROWS;
    }

    public int getColumns() {
        return columns;
    }

    public int getImageWidth() {
        return getImageWidthForColumns(columns);
    }

    /** X coordinate of the player's 9-column inventory grid inside the menu. */
    public int getPlayerInventoryX() {
        return playerInventoryX;
    }

    /** Y coordinate of the player's main inventory grid inside the menu. */
    public int getPlayerInventoryY() {
        return playerInventoryY;
    }

    private static int getImageWidthForColumns(int columns) {
        return columns * SLOT_SIZE + MENU_SIDE_PADDING;
    }

    public int getImageHeight() {
        return 114 + ROWS * SLOT_SIZE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        int chestSlots = container.getContainerSize();
        int playerInventoryStart = chestSlots;
        int hotbarStart = slots.size() - 9;

        if (index < chestSlots) {
            if (!moveItemStackTo(source, playerInventoryStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(source, 0, chestSlots, false)) {
                // Preserve the familiar vanilla main-inventory <-> hotbar fallback.
                boolean moved = index < hotbarStart
                        ? moveItemStackTo(source, hotbarStart, slots.size(), false)
                        : moveItemStackTo(source, playerInventoryStart, hotbarStart, false);
                if (!moved) return ItemStack.EMPTY;
            }
        }

        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
