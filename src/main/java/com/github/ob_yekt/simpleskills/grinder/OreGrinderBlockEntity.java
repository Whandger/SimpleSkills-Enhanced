package com.github.ob_yekt.simpleskills.grinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Server-authoritative three-slot inventory and fuel-driven ore grinding process. */
public final class OreGrinderBlockEntity extends AbstractFurnaceBlockEntity {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int GRIND_TIME = 200; // 10 seconds at 20 TPS, matching a furnace cycle.

    private int burnTime;
    private int totalBurnTime;
    private int grindProgress;

    public OreGrinderBlockEntity(BlockPos pos, BlockState state) {
        super(OreGrinderContent.BLOCK_ENTITY_TYPE, pos, state, RecipeType.SMELTING);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = Math.max(0, input.getIntOr("BurnTime", 0));
        totalBurnTime = Math.max(0, input.getIntOr("TotalBurnTime", 0));
        grindProgress = Math.max(0, Math.min(GRIND_TIME, input.getIntOr("GrindProgress", 0)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("BurnTime", burnTime);
        output.putInt("TotalBurnTime", totalBurnTime);
        output.putInt("GrindProgress", grindProgress);
    }

    /** Small orbiting sparks and smoke make the machine visibly grind while its casing is lit. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, OreGrinderBlockEntity grinder) {
        if (!state.getValue(OreGrinderBlock.LIT)) return;
        long time = level.getGameTime();
        double angle = time * 0.19D;
        double radius = 0.20D;
        double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
        double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
        level.addParticle(ParticleTypes.CRIT, x, pos.getY() + 1.02D, z,
                Math.cos(angle) * 0.015D, 0.015D, Math.sin(angle) * 0.015D);
        if (time % 6L == 0L) {
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                    0.0D, 0.035D, 0.0D);
        }
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, OreGrinderBlockEntity grinder) {
        if (!(world instanceof ServerLevel level)) return;
        boolean changed = false;
        boolean wasLit = state.getValue(OreGrinderBlock.LIT);

        if (grinder.burnTime > 0) {
            grinder.burnTime--;
            changed = true;
        }

        boolean canGrind = grinder.canGrind();
        if (canGrind && grinder.burnTime <= 0) {
            int duration = grinder.getBurnDuration(level, grinder.items.get(FUEL_SLOT));
            if (duration > 0) {
                grinder.burnTime = duration;
                grinder.totalBurnTime = duration;
                grinder.consumeFuel();
                changed = true;
            }
        }

        if (canGrind && grinder.burnTime > 0) {
            grinder.grindProgress++;
            changed = true;
            if (grinder.grindProgress >= GRIND_TIME) {
                grinder.completeGrinding();
                changed = true;
            }
        } else if (!canGrind && grinder.grindProgress != 0) {
            grinder.grindProgress = 0;
            changed = true;
        }

        boolean isLit = grinder.burnTime > 0;
        if (wasLit != isLit) {
            level.setBlock(pos, state.setValue(OreGrinderBlock.LIT, isLit), 3);
            changed = true;
        }

        if (changed) grinder.setChanged();
    }

    private boolean canGrind() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty() || input.getCount() != 1) return false;
        ItemStack result = OreGrinderSalvage.getResult(input);
        if (result.isEmpty()) return false;
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void consumeFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (fuel.isEmpty()) return;
        if (fuel.is(Items.LAVA_BUCKET)) {
            items.set(FUEL_SLOT, Items.BUCKET.getDefaultInstance());
            return;
        }
        fuel.shrink(1);
        if (fuel.isEmpty()) items.set(FUEL_SLOT, ItemStack.EMPTY);
    }

    private void completeGrinding() {
        if (!canGrind()) {
            grindProgress = 0;
            return;
        }
        ItemStack result = OreGrinderSalvage.rollResult(items.get(INPUT_SLOT), level.getRandom());
        ItemStack output = items.get(OUTPUT_SLOT);
        if (!result.isEmpty()) {
            if (output.isEmpty()) {
                items.set(OUTPUT_SLOT, result.copy());
            } else {
                output.grow(result.getCount());
            }
        }
        items.set(INPUT_SLOT, ItemStack.EMPTY);
        grindProgress = 0;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.85F, 0.85F);
        }
    }

    public int getBurnTime() { return burnTime; }
    public int getTotalBurnTime() { return totalBurnTime; }
    public int getGrindProgress() { return grindProgress; }
    public int getGrindTime() { return GRIND_TIME; }
    public boolean isBurning() { return burnTime > 0; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) return OreGrinderSalvage.canRecover(stack);
        if (slot == FUEL_SLOT) return isFuel(stack);
        return false;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot == INPUT_SLOT) grindProgress = 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.has(DataComponents.COOKING_FUEL);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.simpleskills.ore_grinder");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new OreGrinderMenu(containerId, inventory, this);
    }
}
