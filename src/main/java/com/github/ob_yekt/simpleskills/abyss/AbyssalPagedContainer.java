package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shows one six-row page of a 54- or 108-slot container to the vanilla chest menu. */
final class AbyssalPagedContainer implements Container {
    static final int PAGE_SIZE = 54;

    private final Container delegate;
    private int page;
    private int pageCount;

    AbyssalPagedContainer(Container delegate) {
        this.delegate = delegate;
        this.pageCount = Math.max(1, (delegate.getContainerSize() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    int getPage() {
        return page;
    }

    int getPageCount() {
        return pageCount;
    }

    void setPageCount(int pageCount) {
        this.pageCount = Math.max(1, pageCount);
        this.page = Math.min(this.page, this.pageCount - 1);
    }

    void setPage(int page) {
        this.page = Math.max(0, Math.min(page, pageCount - 1));
    }

    void nextPage() {
        if (pageCount > 1) page = (page + 1) % pageCount;
    }

    private int toBackingIndex(int slot) {
        return page * PAGE_SIZE + slot;
    }

    @Override public int getContainerSize() { return PAGE_SIZE; }
    @Override public boolean isEmpty() { return delegate.isEmpty(); }
    @Override public ItemStack getItem(int slot) {
        int index = toBackingIndex(slot);
        return slot < 0 || slot >= PAGE_SIZE || index >= delegate.getContainerSize()
                ? ItemStack.EMPTY : delegate.getItem(index);
    }
    @Override public ItemStack removeItem(int slot, int count) {
        int index = toBackingIndex(slot);
        return slot < 0 || slot >= PAGE_SIZE || index >= delegate.getContainerSize()
                ? ItemStack.EMPTY : delegate.removeItem(index, count);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        int index = toBackingIndex(slot);
        return slot < 0 || slot >= PAGE_SIZE || index >= delegate.getContainerSize()
                ? ItemStack.EMPTY : delegate.removeItemNoUpdate(index);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        int index = toBackingIndex(slot);
        if (slot >= 0 && slot < PAGE_SIZE && index < delegate.getContainerSize()) {
            delegate.setItem(index, stack);
        }
    }
    @Override public void setChanged() { delegate.setChanged(); }
    @Override public boolean stillValid(Player player) { return delegate.stillValid(player); }
    @Override public void startOpen(ContainerUser user) { delegate.startOpen(user); }
    @Override public void stopOpen(ContainerUser user) { delegate.stopOpen(user); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        int index = toBackingIndex(slot);
        return slot >= 0 && slot < PAGE_SIZE && index < delegate.getContainerSize()
                && delegate.canPlaceItem(index, stack);
    }
    @Override public int getMaxStackSize() { return delegate.getMaxStackSize(); }
    @Override public void clearContent() { delegate.clearContent(); }
}
