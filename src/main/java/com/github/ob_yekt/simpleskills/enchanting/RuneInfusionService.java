package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import com.github.ob_yekt.simpleskills.runes.RuneContent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative preparation and Rune payment for Rune Enchanting attempts. */
public final class RuneInfusionService {
    private static final Map<UUID, RuneInfusionSession> ACTIVE = new ConcurrentHashMap<>();
    /** Exact server-owned stack reserved for the active attempt. */
    private static final Map<UUID, ItemStack> ACTIVE_EQUIPMENT = new ConcurrentHashMap<>();

    private RuneInfusionService() {}

    public enum StartResult {
        STARTED, PLAYER_ALREADY_HAS_SESSION, EQUIPMENT_NOT_OWNED, INVALID_EQUIPMENT,
        INVALID_SELECTION, INSUFFICIENT_RUNES, PAYMENT_FAILED
    }

    /**
     * Revalidates the equipment, selection and payment on the logical server. The supplied stack
     * must be the exact stack object in the player's inventory; a copied client stack is rejected.
     *
     * `requested` is the FINAL set of enchantments (max 4), including the ones already on the item
     * that the player keeps. Existing enchantments missing from it are removed on success.
     * Only new or raised enchantments cost Runes. Runes are consumed only after all validation passed.
     */
    public static synchronized StartResult begin(ServerPlayer player, ItemStack equipment,
            List<RuneInfusionRules.Selection> requested) {
        if (player == null) return StartResult.INVALID_EQUIPMENT;
        UUID playerId = player.getUUID();
        RuneInfusionSession previous = ACTIVE.get(playerId);
        if (previous != null && !previous.isTerminal()) return StartResult.PLAYER_ALREADY_HAS_SESSION;
        if (!ownsExactStack(player, equipment)) return StartResult.EQUIPMENT_NOT_OWNED;
        if (equipment.isEmpty() || !EnchantmentHelper.canStoreEnchantments(equipment)) {
            return StartResult.INVALID_EQUIPMENT;
        }

        Set<String> requestedIds = new HashSet<>();
        if (requested == null || requested.isEmpty() || requested.size() > RuneInfusionRules.MAX_SELECTED_ENCHANTMENTS) {
            return StartResult.INVALID_SELECTION;
        }
        for (RuneInfusionRules.Selection selection : requested) {
            if (selection == null || !requestedIds.add(selection.enchantmentId())) return StartResult.INVALID_SELECTION;
        }

        List<RuneInfusionCatalog.Entry> available = RuneInfusionCatalog.getAvailable(player, equipment);
        Map<String, RuneInfusionCatalog.Entry> byId = new HashMap<>();
        for (RuneInfusionCatalog.Entry entry : available) byId.put(entry.id(), entry);

        // Levels currently on the server-owned stack.
        Map<String, Integer> existing = new HashMap<>();
        var currentEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(equipment);
        currentEnchantments.entrySet().forEach(entry -> entry.getKey().unwrapKey().ifPresent(key ->
                existing.put(key.identifier().toString(), entry.getValue())));

        var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<RuneInfusionRules.Selection> validated = new ArrayList<>();
        List<Holder<Enchantment>> selectedHolders = new ArrayList<>();
        for (RuneInfusionRules.Selection selection : requested) {
            RuneInfusionCatalog.Entry entry = byId.get(selection.enchantmentId());
            if (entry == null) return StartResult.INVALID_SELECTION;
            Holder<Enchantment> holder = registry.get(
                    net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,
                            net.minecraft.resources.Identifier.parse(selection.enchantmentId())))
                    .orElse(null);
            if (holder == null) return StartResult.INVALID_SELECTION;
            // The final set must be internally compatible (an existing conflicting enchantment
            // has to be dropped by the player, i.e. left out of the request).
            for (Holder<Enchantment> alreadySelected : selectedHolders) {
                if (!Enchantment.areCompatible(holder, alreadySelected)) return StartResult.INVALID_SELECTION;
            }
            selectedHolders.add(holder);
            validated.add(new RuneInfusionRules.Selection(selection.enchantmentId(),
                    selection.targetLevel(), holder.value().getMaxLevel(),
                    existing.getOrDefault(selection.enchantmentId(), 0)));
        }

        int professionLevel = XPManager.getSkillLevel(player.getStringUUID(), Skills.ENCHANTING);
        int unlocked = EnchantProgression.getUnlockedEnchantmentLevel(professionLevel);
        Set<String> compatible = new HashSet<>(byId.keySet());

        RuneInfusionRules.Validation validation = RuneInfusionRules.validate(
                professionLevel, validated, compatible, existing);
        if (!validation.accepted()) return StartResult.INVALID_SELECTION;

        int cost = RuneInfusionRules.getRuneCost(validated);
        if (cost < 1) return StartResult.INVALID_SELECTION;
        if (countRunes(player) < cost) return StartResult.INSUFFICIENT_RUNES;

        // Existing (removable) enchantments the player left out of the final set.
        List<String> removed = new ArrayList<>();
        for (RuneInfusionCatalog.Entry entry : available) {
            if (entry.existing() && !requestedIds.contains(entry.id())) removed.add(entry.id());
        }

        // Difficulty counts EVERY enchantment in the final set (existing ones included).
        double rawDifficulty = 0.0;
        for (RuneInfusionRules.Selection selection : validated) {
            int cap = EnchantmentLimits.getMaxLevel(selection.enchantmentId(), selection.vanillaMaxLevel());
            rawDifficulty += RuneInfusionDifficulty.contribution(selection.targetLevel(), cap, unlocked);
        }
        int difficulty = RuneInfusionDifficulty.toScore(rawDifficulty);

        RuneInfusionSession session = RuneInfusionSession.create(playerId, validated, removed, difficulty);
        if (!consumeRunes(player, cost)) return StartResult.PAYMENT_FAILED;
        if (!session.markStarted()) return StartResult.PAYMENT_FAILED;
        ACTIVE.put(playerId, session);
        ACTIVE_EQUIPMENT.put(playerId, equipment);
        return StartResult.STARTED;
    }

    /** Marks the currently active attempt as having entered its interactive phase. */
    public static synchronized boolean markInProgress(ServerPlayer player) {
        RuneInfusionSession session = player == null ? null : ACTIVE.get(player.getUUID());
        return session != null && session.markInProgress();
    }

    /**
     * Applies the selected enchantments after a successful minigame. The equipment must still be
     * the exact stack reserved at begin(); the registry and current stack are checked again here.
     * This method must only be called by server-side result logic.
     */
    public static synchronized boolean applySuccessfulInfusion(ServerPlayer player) {
        return applySuccessfulInfusion(player, RuneInfusionMinigame.Grade.POOR);
    }

    public static synchronized boolean applySuccessfulInfusion(ServerPlayer player, RuneInfusionMinigame.Grade grade) {
        if (player == null) return false;
        UUID playerId = player.getUUID();
        RuneInfusionSession session = ACTIVE.get(playerId);
        ItemStack equipment = ACTIVE_EQUIPMENT.get(playerId);
        if (session == null || session.state() != RuneInfusionSession.State.IN_PROGRESS
                || equipment == null || !ownsExactStack(player, equipment)
                || equipment.isEmpty() || !EnchantmentHelper.canStoreEnchantments(equipment)) {
            return false;
        }

        var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, Holder<Enchantment>> holders = new HashMap<>();
        for (RuneInfusionRules.Selection selection : session.selections()) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,
                    net.minecraft.resources.Identifier.parse(selection.enchantmentId()));
            Holder<Enchantment> holder = registry.get(key).orElse(null);
            if (holder == null || holder.value().getMaxLevel() < 1) return false;
            holders.put(selection.enchantmentId(), holder);
        }

        Set<String> removed = new HashSet<>(session.removedIds());

        // Recheck conflicts against what will remain on the equipment (removed ones are ignored).
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(equipment);
        for (Holder<Enchantment> requested : holders.values()) {
            for (Holder<Enchantment> current : existing.keySet()) {
                String currentId = current.unwrapKey().map(k -> k.identifier().toString()).orElse("");
                if (removed.contains(currentId)) continue;
                if (!current.equals(requested) && !Enchantment.areCompatible(requested, current)) return false;
            }
        }
        List<Holder<Enchantment>> selected = new ArrayList<>(holders.values());
        for (int i = 0; i < selected.size(); i++) {
            for (int j = i + 1; j < selected.size(); j++) {
                if (!Enchantment.areCompatible(selected.get(i), selected.get(j))) return false;
            }
        }

        int professionLevel = XPManager.getSkillLevel(player.getStringUUID(), Skills.ENCHANTING);
        int unlocked = EnchantProgression.getUnlockedEnchantmentLevel(professionLevel);

        EnchantmentHelper.updateEnchantments(equipment, mutable -> {
            // Enchantments the player chose to drop.
            mutable.removeIf(holder -> holder.unwrapKey()
                    .map(key -> removed.contains(key.identifier().toString()))
                    .orElse(false));

            for (RuneInfusionRules.Selection selection : session.selections()) {
                if (!selection.changed()) continue; // "keep" entries are left untouched

                int finalLevel = selection.targetLevel();
                int cap = EnchantmentLimits.getMaxLevel(selection.enchantmentId(),
                        holders.get(selection.enchantmentId()).value().getMaxLevel());
                if (finalLevel < Math.min(unlocked, cap)
                        && (player.getRandom().nextInt(100) < RuneInfusionMinigame.bonusChancePercent(grade))) {
                    finalLevel++;
                }
                mutable.set(holders.get(selection.enchantmentId()), finalLevel);
            }
        });
        boolean completed = session.complete();
        if (completed) ACTIVE_EQUIPMENT.remove(playerId);
        return completed;
    }

    /** Failure after payment never refunds Runes and never touches the equipment. */
    public static synchronized boolean fail(ServerPlayer player) {
        RuneInfusionSession session = player == null ? null : ACTIVE.get(player.getUUID());
        if (session == null) return false;
        boolean failed = session.fail();
        if (failed) ACTIVE_EQUIPMENT.remove(player.getUUID());
        return failed;
    }

    public static RuneInfusionSession getActiveSession(ServerPlayer player) {
        return player == null ? null : ACTIVE.get(player.getUUID());
    }

    public static java.util.UUID getActiveSessionId(ServerPlayer player) {
        RuneInfusionSession session = getActiveSession(player);
        return session == null ? null : session.sessionId();
    }

    public static synchronized void clearTerminalSession(ServerPlayer player) {
        if (player == null) return;
        ACTIVE.computeIfPresent(player.getUUID(), (id, session) -> {
            if (!session.isTerminal()) return session;
            ACTIVE_EQUIPMENT.remove(id);
            return null;
        });
    }

    /** Checks main inventory and every equipment slot exposed by Inventory.getItem(int). */
    private static boolean ownsExactStack(ServerPlayer player, ItemStack target) {
        if (target == null || target.isEmpty()) return false;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == target) return true;
        }
        return false;
    }

    public static synchronized int getActiveEquipmentSlot(ServerPlayer player) {
        if (player == null) return -1;

        ItemStack reserved = ACTIVE_EQUIPMENT.get(player.getUUID());
        if (reserved == null || reserved.isEmpty()) return -1;

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == reserved) {
                return slot;
            }
        }
        return -1;
    }

    private static int countRunes(ServerPlayer player) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(RuneContent.RUNE)) count += stack.getCount();
        }
        return count;
    }

    private static boolean consumeRunes(ServerPlayer player, int amount) {
        if (amount < 1 || countRunes(player) < amount) return false;
        int remaining = amount;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(RuneContent.RUNE)) continue;
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
        return remaining == 0;
    }
}
