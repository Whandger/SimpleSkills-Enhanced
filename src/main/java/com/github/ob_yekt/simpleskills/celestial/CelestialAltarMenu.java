package com.github.ob_yekt.simpleskills.celestial;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.PerkRerollInfo;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.StatUpgradeInfo;
import com.github.ob_yekt.simpleskills.runes.RuneContent;
import net.minecraft.network.chat.Component;
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

import java.util.List;

/** Server-authoritative three-tab menu for the Celestial Altar. */
public final class CelestialAltarMenu extends AbstractContainerMenu {
    public static final int MODE_INFUSION = 0;
    public static final int MODE_SALVAGE = 1;
    public static final int MODE_ENHANCE = 2;

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

        addSlot(new Slot(container, CelestialAltarBlockEntity.INFUSION_EQUIPMENT, 168, 74) {
            @Override public boolean mayPlace(ItemStack stack) {
                return CraftingEquipmentManager.isEligibleForCelestialAltar(stack);
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return getMode() == MODE_INFUSION; }
        });
        addEssenceSlot(CelestialAltarBlockEntity.ESSENCE_TOP, 168, 50);
        addEssenceSlot(CelestialAltarBlockEntity.ESSENCE_LEFT, 132, 74);
        addEssenceSlot(CelestialAltarBlockEntity.ESSENCE_RIGHT, 204, 74);
        addSalvageSlot(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT, 124, 72);
        addSlot(new Slot(container, CelestialAltarBlockEntity.SALVAGE_OUTPUT, 212, 72) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isActive() { return getMode() == MODE_SALVAGE; }
        });

        // Enhance tab: equipment on the left, enhancement options in the center, runes on the right.
        addSlot(new Slot(container, CelestialAltarBlockEntity.ENHANCE_EQUIPMENT, 36, 72) {
            @Override public boolean mayPlace(ItemStack stack) {
                return CraftingEquipmentManager.isEligibleForCelestialAltar(stack);
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return getMode() == MODE_ENHANCE; }
        });
        addSlot(new Slot(container, CelestialAltarBlockEntity.ENHANCE_RUNES, 300, 72) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(RuneContent.RUNE); }
            @Override public int getMaxStackSize() { return 64; }
            @Override public boolean isActive() { return getMode() == MODE_ENHANCE; }
        });

        addStandardInventorySlots(inventory, 95, 142);
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

    public List<StatUpgradeInfo> getEnhancementStats() {
        return CraftingEquipmentManager.getEnhanceableStats(
                container.getItem(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT));
    }

    public List<PerkRerollInfo> getEnhancementPerks() {
        return CraftingEquipmentManager.getEnhanceablePerks(
                container.getItem(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT));
    }

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

    public boolean hasEnhancementRunes() {
        ItemStack runes = container.getItem(CelestialAltarBlockEntity.ENHANCE_RUNES);
        return !runes.isEmpty() && runes.is(RuneContent.RUNE);
    }

    /** Called only by the server-side receiver after confirming this menu is still open. */
    public void handleUiAction(ServerPlayer player, int action, String targetId, int expectedRevision) {
        if (player == null || player.containerMenu != this || !stillValid(player)) return;
        if (targetId == null || targetId.length() > 128) return;

        switch (action) {
            case CelestialAltarActionPayload.TAB_INFUSION -> mode.set(MODE_INFUSION);
            case CelestialAltarActionPayload.TAB_SALVAGE -> mode.set(MODE_SALVAGE);
            case CelestialAltarActionPayload.TAB_ENHANCE -> mode.set(MODE_ENHANCE);
            case CelestialAltarActionPayload.ASCEND -> {
                if (getMode() == MODE_INFUSION) executeAscension(player);
            }
            case CelestialAltarActionPayload.SALVAGE -> {
                if (getMode() == MODE_SALVAGE) executeSalvage(player);
            }
            case CelestialAltarActionPayload.ENHANCE_STAT -> {
                if (getMode() == MODE_ENHANCE) executeEnhancement(player, targetId, false, expectedRevision);
            }
            case CelestialAltarActionPayload.REROLL_PERK -> {
                if (getMode() == MODE_ENHANCE) executeEnhancement(player, targetId, true, expectedRevision);
            }
            case CelestialAltarActionPayload.REROLL_STAT -> {
                if (getMode() == MODE_ENHANCE) executeStatReroll(player, targetId, expectedRevision);
            }
            default -> { return; }
        }
        broadcastChanges();
    }

    private void executeAscension(ServerPlayer player) {
        ItemStack equipment = container.getItem(CelestialAltarBlockEntity.INFUSION_EQUIPMENT);
        if (!CraftingEquipmentManager.isEligibleForCelestialAltar(equipment) || equipment.getCount() != 1) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.invalid_equipment"), true);
            return;
        }
        for (int slot = CelestialAltarBlockEntity.ESSENCE_TOP; slot <= CelestialAltarBlockEntity.ESSENCE_RIGHT; slot++) {
            if (!container.getItem(slot).is(CelestialAltarContent.TRANSCENDENCY_ESSENCE)) {
                player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.missing_essence"), true);
                return;
            }
        }

        if (!CraftingEquipmentManager.applyCelestialAltarReroll(equipment, player)) return;
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
    }

    private void executeSalvage(ServerPlayer player) {
        ItemStack output = container.getItem(CelestialAltarBlockEntity.SALVAGE_OUTPUT);
        if (!output.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.take_output_first"), true);
            return;
        }
        ItemStack sacrifice = container.getItem(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT);
        if (!CraftingEquipmentManager.isTranscendent(sacrifice)
                || !CraftingEquipmentManager.isEligibleForCelestialAltar(sacrifice)
                || sacrifice.getCount() != 1) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.need_one_transcendent"), true);
            return;
        }

        long now = player.level().getGameTime();
        if (now - lastSalvageConfirmTick > 30L || !matchesPendingSalvageItems()) {
            pendingSalvageConfirmation = sacrifice.copy();
            lastSalvageConfirmTick = now;
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.salvage_confirm"), true);
            return;
        }

        container.setItem(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT, ItemStack.EMPTY);
        container.setItem(CelestialAltarBlockEntity.SALVAGE_OUTPUT,
                new ItemStack(CelestialAltarContent.TRANSCENDENCY_ESSENCE));
        lastSalvageConfirmTick = Long.MIN_VALUE;
        pendingSalvageConfirmation = ItemStack.EMPTY;
        player.playSound(SoundEvents.ANVIL_USE, 0.8F, 1.3F);
        player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.salvaged"), true);
    }

    private boolean matchesPendingSalvageItems() {
        ItemStack current = container.getItem(CelestialAltarBlockEntity.SALVAGE_EQUIPMENT);
        return !pendingSalvageConfirmation.isEmpty() && ItemStack.matches(pendingSalvageConfirmation, current);
    }

    private void executeEnhancement(ServerPlayer player, String targetId, boolean rerollPerk,
                                    int expectedRevision) {
        ItemStack equipment = container.getItem(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT);
        ItemStack runes = container.getItem(CelestialAltarBlockEntity.ENHANCE_RUNES);
        if (targetId.isBlank() || equipment.getCount() != 1 || runes.isEmpty() || !runes.is(RuneContent.RUNE)) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.enhancement_invalid"), true);
            return;
        }
        // Reject clicks queued against an older GUI snapshot. One successful operation
        // advances this revision, so a delayed second click cannot mutate another row.
        if (expectedRevision < 0
                || CraftingEquipmentManager.getEnhancementRevision(equipment) != expectedRevision) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.enhancement_invalid"), true);
            return;
        }

        boolean applied;
        if (rerollPerk) {
            // The client sends perk:<original-index>:<id>:<amplifier>. Validate the full
            // serialized entry before replacing it, so a reordered display can never target
            // a different perk with the same or similar label.
            String[] targetParts = targetId.split(":", 4);
            if (targetParts.length != 4 || !targetParts[0].equals("perk")) return;
            int perkIndex;
            try {
                perkIndex = Integer.parseInt(targetParts[1]);
            } catch (NumberFormatException ignored) {
                return;
            }
            String expectedEntry = targetParts[2] + ":" + targetParts[3];
            if (!CraftingEquipmentManager.canRerollPerkAt(equipment, perkIndex, expectedEntry)) return;
            applied = CraftingEquipmentManager.rerollEquipmentPerkAt(
                    equipment, perkIndex, expectedEntry, player);
        } else {
            // Verify the requested stat exists on this item and is not already at its cap.
            if (!CraftingEquipmentManager.canEnhanceStat(equipment, targetId)) return;
            applied = CraftingEquipmentManager.enhanceEquipmentStat(equipment, targetId);
        }
        if (!applied) return;

        runes.shrink(1);
        if (runes.isEmpty()) container.setItem(CelestialAltarBlockEntity.ENHANCE_RUNES, ItemStack.EMPTY);
        container.setChanged();
        player.playSound(SoundEvents.ANVIL_USE, 0.65F, rerollPerk ? 1.25F : 1.55F);
        if (rerollPerk) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.perk_rerolled"), true);
        } else {
            String translationKey = getEnhancementStats().stream()
                    .filter(info -> info.id().equals(targetId))
                    .map(StatUpgradeInfo::translationKey)
                    .findFirst().orElse("item.simpleskills.crafting_stat." + targetId);
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.enhanced",
                    Component.translatable(translationKey)), true);
        }
    }

    private void executeStatReroll(ServerPlayer player, String targetId, int expectedRevision) {
        ItemStack equipment = container.getItem(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT);
        ItemStack runes = container.getItem(CelestialAltarBlockEntity.ENHANCE_RUNES);
        if (targetId.isBlank() || equipment.getCount() != 1 || runes.isEmpty()
                || !runes.is(RuneContent.RUNE)
                || expectedRevision < 0
                || CraftingEquipmentManager.getEnhancementRevision(equipment) != expectedRevision
                || !CraftingEquipmentManager.canRerollStat(equipment, targetId)) {
            player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.enhancement_invalid"), true);
            return;
        }
        // This left-side action replaces the selected attribute type with a different
        // category-valid random attribute; it is intentionally separate from + enhancement.
        if (!CraftingEquipmentManager.replaceEquipmentAttribute(equipment, targetId, player)) return;
        container.setItem(CelestialAltarBlockEntity.ENHANCE_EQUIPMENT, equipment.copy());
        runes.shrink(1);
        if (runes.isEmpty()) container.setItem(CelestialAltarBlockEntity.ENHANCE_RUNES, ItemStack.EMPTY);
        container.setChanged();
        player.playSound(SoundEvents.ANVIL_USE, 0.8F, 0.95F);
        player.sendSystemMessage(Component.translatable("message.simpleskills.celestial_altar.stat_rerolled"), true);
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
