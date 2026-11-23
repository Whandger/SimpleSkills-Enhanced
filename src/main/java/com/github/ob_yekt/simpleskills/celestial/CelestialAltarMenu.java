package com.github.ob_yekt.simpleskills.celestial;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/** Server-authoritative two-tab menu for the Celestial Altar. */
public final class CelestialAltarMenu extends AbstractContainerMenu {
    public static final int MODE_INFUSION = 0;
    public static final int MODE_SALVAGE = 1;
    public static final int BUTTON_INFUSION_TAB = 0;
    public static final int BUTTON_SALVAGE_TAB = 1;
    public static final int BUTTON_ASCEND = 2;
    public static final int BUTTON_SALVAGE_CONFIRM = 3;

    private final Container container;
    private final CelestialAltarBlockEntity altar;
    private final DataSlot mode = DataSlot.standalone();
    private long lastSalvageConfirmTick = Long.MIN_VALUE;
    private ItemStack pendingSalvageConfirmation = ItemStack.EMPTY;

    /** Client constructor. */
    public CelestialAltarMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(CelestialAltarBlockEntity.SLOT_COUNT), null);
    }

    /** Server constructor. */
    public CelestialAltarMenu(int containerId, Inventory inventory, CelestialAltarBlockEntity altar) {
        this(containerId, inventory, altar, altar);
    }

    private CelestialAltarMenu(int containerId, Inventory inventory, Container container,
                               CelestialAltarBlockEntity altar) {
        super(CelestialAltarContent.MENU_TYPE, containerId);
        checkContainerSize(container, CelestialAltarBlockEntity.SLOT_COUNT);
        this.container = container;
        this.altar = altar;
        container.startOpen(inventory.player);

        addSlot(new Slot(container, CelestialAltarBlockEntity.INFUSION_EQUIPMENT, 111, 73) {
            @Override public boolean mayPlace(ItemStack stack) {
                return CraftingEquipmentManager.isEligibleForCelestialAltar(stack);
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return getMode() == MODE_INFUSION; }
        });
        addEssenceSlot(CelestialAltarBlockEntity.ESSENCE_TOP, 111, 51);
        addEssenceSlot(CelestialAltarBlockEntity.ESSENCE_LEFT, 71, 73);
        addEssenceSlot(CelestialAltarBlockEntity.ESSENCE_RIGHT, 151, 73);
        addSalvageSlot(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT, 84, 72);
        addSlot(new Slot(container, CelestialAltarBlockEntity.SALVAGE_OUTPUT, 148, 72) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isActive() { return getMode() == MODE_SALVAGE; }
        });

        addStandardInventorySlots(inventory, 8, 137);
        addDataSlot(mode);
    }

    private void addEssenceSlot(int index, int x, int y) {
        addSlot(new Slot(container, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return getMode() == MODE_INFUSION; }
        });
    }

    private void addSalvageSlot(int index, int x, int y) {
        addSlot(new Slot(container, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return CraftingEquipmentManager.isEligibleForCelestialAltar(stack)
                        && CraftingEquipmentManager.isTranscendent(stack);
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return getMode() == MODE_SALVAGE; }
        });
    }

    public int getMode() { return mode.get(); }

    public boolean canInfuse() {
        ItemStack equipment = container.getItem(CelestialAltarBlockEntity.INFUSION_EQUIPMENT);
        if (!CraftingEquipmentManager.isEligibleForCelestialAltar(equipment) || equipment.getCount() != 1) return false;
        for (int slot = CelestialAltarBlockEntity.ESSENCE_TOP;
             slot <= CelestialAltarBlockEntity.ESSENCE_RIGHT; slot++) {
            if (!container.getItem(slot).is(CelestialAltarContent.TRANSCENDENCY_ESSENCE)) return false;
        }
        return true;
    }

    public boolean canSalvage() {
        if (!container.getItem(CelestialAltarBlockEntity.SALVAGE_OUTPUT).isEmpty()) return false;
        ItemStack stack = container.getItem(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT);
        return CraftingEquipmentManager.isTranscendent(stack)
                && CraftingEquipmentManager.isEligibleForCelestialAltar(stack)
                && stack.getCount() == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_INFUSION_TAB) {
            mode.set(MODE_INFUSION);
            return true;
        }
        if (buttonId == BUTTON_SALVAGE_TAB) {
            mode.set(MODE_SALVAGE);
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || altar == null) return false;
        if (buttonId == BUTTON_ASCEND && getMode() == MODE_INFUSION) return executeAscension(serverPlayer);
        if (buttonId == BUTTON_SALVAGE_CONFIRM && getMode() == MODE_SALVAGE) return executeSalvage(serverPlayer);
        return false;
    }

    private boolean executeAscension(ServerPlayer player) {
        ItemStack equipment = container.getItem(CelestialAltarBlockEntity.INFUSION_EQUIPMENT);
        if (!CraftingEquipmentManager.isEligibleForCelestialAltar(equipment) || equipment.getCount() != 1) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.invalid_equipment"), true);
            return false;
        }
        for (int slot = CelestialAltarBlockEntity.ESSENCE_TOP; slot <= CelestialAltarBlockEntity.ESSENCE_RIGHT; slot++) {
            if (!container.getItem(slot).is(CelestialAltarContent.TRANSCENDENCY_ESSENCE)) {
                player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.missing_essence"), true);
                return false;
            }
        }

        if (!CraftingEquipmentManager.applyCelestialAltarReroll(equipment, player)) return false;
        // The ItemStack is mutated in place; mark the block entity dirty even if the player's
        // inventory is full and the upgraded equipment has to remain in the altar input slot.
        container.setChanged();
        for (int slot = CelestialAltarBlockEntity.ESSENCE_TOP; slot <= CelestialAltarBlockEntity.ESSENCE_RIGHT; slot++) {
            ItemStack essence = container.getItem(slot);
            essence.shrink(1);
            if (essence.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
        }

        ItemStack returned = equipment.copy();
        if (player.getInventory().add(returned)) {
            container.setItem(CelestialAltarBlockEntity.INFUSION_EQUIPMENT, ItemStack.EMPTY);
        }
        player.playSound(SoundEvents.PLAYER_LEVELUP, 0.9F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.ascended"), true);
        player.closeContainer();
        return true;
    }

    private boolean executeSalvage(ServerPlayer player) {
        ItemStack output = container.getItem(CelestialAltarBlockEntity.SALVAGE_OUTPUT);
        if (!output.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.take_output_first"), true);
            return false;
        }
        ItemStack sacrifice = container.getItem(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT);
        if (!CraftingEquipmentManager.isTranscendent(sacrifice)
                || !CraftingEquipmentManager.isEligibleForCelestialAltar(sacrifice)
                || sacrifice.getCount() != 1) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.need_one_transcendent"), true);
            return false;
        }

        long now = player.level().getGameTime();
        if (now - lastSalvageConfirmTick > 30L || !matchesPendingSalvageItems()) {
            rememberPendingSalvageItems();
            lastSalvageConfirmTick = now;
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.salvage_confirm"), true);
            return true;
        }

        container.setItem(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT, ItemStack.EMPTY);
        container.setItem(CelestialAltarBlockEntity.SALVAGE_OUTPUT,
                new ItemStack(CelestialAltarContent.TRANSCENDENCY_ESSENCE));
        lastSalvageConfirmTick = Long.MIN_VALUE;
        pendingSalvageConfirmation = ItemStack.EMPTY;
        player.playSound(SoundEvents.ANVIL_USE, 0.8F, 1.3F);
        player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.salvaged"), true);
        return true;
    }

    private void rememberPendingSalvageItems() {
        pendingSalvageConfirmation = container.getItem(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT).copy();
    }

    private boolean matchesPendingSalvageItems() {
        ItemStack current = container.getItem(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT);
        return !pendingSalvageConfirmation.isEmpty() && ItemStack.matches(pendingSalvageConfirmation, current);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Keep every ingredient in its explicitly validated slot; shift-click cannot bypass mode restrictions.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return altar == null || altar.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
