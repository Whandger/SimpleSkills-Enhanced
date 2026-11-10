package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.runes.RuneContent;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative network boundary for Rune Infusion preparation and input.
 */
public final class RuneInfusionNetworking {
    private RuneInfusionNetworking() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(
                RuneInfusionStartPayload.TYPE, RuneInfusionStartPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                RuneInfusionInputPayload.TYPE, RuneInfusionInputPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                RuneInfusionCatalogRequestPayload.TYPE, RuneInfusionCatalogRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                RuneInfusionCatalogResponsePayload.TYPE, RuneInfusionCatalogResponsePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                RuneInfusionResultPayload.TYPE, RuneInfusionResultPayload.CODEC);
    }

    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(RuneInfusionCatalogRequestPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handleCatalog(context.player(), payload)));

        ServerPlayNetworking.registerGlobalReceiver(RuneInfusionStartPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handleStart(context.player(), payload)));

        ServerPlayNetworking.registerGlobalReceiver(RuneInfusionInputPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handleInput(context.player(), payload)));

        // Enchanting Table -> Rune Infusion (vanilla menu is never opened).
        RuneInfusionTable.register();
    }

    /** Called when the player right-clicks an Enchanting Table: uses the selected hotbar slot. */
    public static void openFromTable(ServerPlayer player) {
        if (player == null) return;
        openCatalog(player, player.getInventory().getSelectedSlot());
    }

    private static void handleInput(ServerPlayer player, RuneInfusionInputPayload payload) {
        if (player == null || payload == null || payload.crackId() < 0 || payload.crackId() > 4096) return;

        UUID sessionId = payload.sessionId();
        UUID playerId = player.getUUID();
        int crackId = payload.crackId();

        switch (payload.action()) {
            case RuneInfusionInputPayload.BEGIN ->
                    RuneInfusionMinigameController.beginRepair(playerId, sessionId, crackId);
            case RuneInfusionInputPayload.HOLD ->
                    RuneInfusionMinigameController.holdRepair(playerId, sessionId, crackId);
            case RuneInfusionInputPayload.RELEASE ->
                    RuneInfusionMinigameController.releaseRepair(playerId, sessionId, crackId);
            default -> { }
        }
    }

    private static void handleCatalog(ServerPlayer player, RuneInfusionCatalogRequestPayload payload) {
        if (player == null || payload == null) return;
        openCatalog(player, payload.inventorySlot());
    }

    private static void openCatalog(ServerPlayer player, int slot) {
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.getNonEquipmentItems().size()) return;

        RuneInfusionSession active = RuneInfusionService.getActiveSession(player);
        if (active != null && !active.isTerminal()) {
            send(player, RuneInfusionResultPayload.PLAYER_BUSY, 0, -1);
            return;
        }

        ItemStack equipment = inventory.getItem(slot);
        if (equipment.isEmpty() || !EnchantmentHelper.canStoreEnchantments(equipment)) {
            send(player, RuneInfusionResultPayload.INVALID_EQUIPMENT, 0, -1);
            return;
        }

        List<RuneInfusionCatalog.Entry> entries =
                RuneInfusionCatalog.getAvailable(player, equipment, Set.of());
        Map<String, List<String>> conflicts = conflictsOf(player, entries);

        // Row format: id|currentLevel|maximumSelectableLevel|conflictId,conflictId
        StringBuilder rows = new StringBuilder();
        for (RuneInfusionCatalog.Entry entry : entries) {
            StringBuilder row = new StringBuilder();
            row.append(entry.id()).append('|')
                    .append(entry.currentLevel()).append('|')
                    .append(entry.maximumSelectableLevel()).append('|')
                    .append(String.join(",", conflicts.getOrDefault(entry.id(), List.of())));

            if (rows.length() + row.length() + 1 > 24_000) break;
            if (!rows.isEmpty()) rows.append(';');
            rows.append(row);
        }

        ServerPlayNetworking.send(player,
                new RuneInfusionCatalogResponsePayload(slot, countRunes(inventory), rows.toString()));
    }

    /** Pairwise incompatibility among the offered enchantments, so the UI can block bad picks. */
    private static Map<String, List<String>> conflictsOf(ServerPlayer player,
                                                         List<RuneInfusionCatalog.Entry> entries) {
        var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, Holder<Enchantment>> holders = new LinkedHashMap<>();
        for (RuneInfusionCatalog.Entry entry : entries) {
            registry.get(ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(entry.id())))
                    .ifPresent(holder -> holders.put(entry.id(), holder));
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (var a : holders.entrySet()) {
            for (var b : holders.entrySet()) {
                if (a.getKey().equals(b.getKey())) continue;
                if (!Enchantment.areCompatible(a.getValue(), b.getValue())) {
                    result.computeIfAbsent(a.getKey(), k -> new ArrayList<>()).add(b.getKey());
                }
            }
        }
        return result;
    }

    private static int countRunes(Inventory inventory) {
        int runes = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(RuneContent.RUNE)) runes += stack.getCount();
        }
        return runes;
    }

    private static void handleStart(ServerPlayer player, RuneInfusionStartPayload payload) {
        Inventory inventory = player.getInventory();

        if (payload == null
                || payload.inventorySlot() < 0
                || payload.inventorySlot() >= inventory.getNonEquipmentItems().size()
                || payload.enchantmentIds().size() != payload.targetLevels().size()
                || payload.enchantmentIds().isEmpty()
                || payload.enchantmentIds().size() > RuneInfusionRules.MAX_SELECTED_ENCHANTMENTS) {
            send(player, RuneInfusionResultPayload.INVALID_SELECTION, 0, -1);
            return;
        }

        ItemStack equipment = inventory.getItem(payload.inventorySlot());
        List<RuneInfusionRules.Selection> selections = new ArrayList<>();

        for (int i = 0; i < payload.enchantmentIds().size(); i++) {
            String id = payload.enchantmentIds().get(i);
            Integer level = payload.targetLevels().get(i);

            if (id == null || id.length() > 128 || level == null || level < 1 || level > 10) {
                send(player, RuneInfusionResultPayload.INVALID_SELECTION, 0, -1);
                return;
            }
            selections.add(new RuneInfusionRules.Selection(id, level, 1));
        }

        // Validation + Rune payment (the ONLY cost: 1 Rune per enchantment, no XP, no lapis).
        RuneInfusionService.StartResult result = RuneInfusionService.begin(player, equipment, selections);

        if (result != RuneInfusionService.StartResult.STARTED) {
            int code = switch (result) {
                case PLAYER_ALREADY_HAS_SESSION -> RuneInfusionResultPayload.PLAYER_BUSY;
                case EQUIPMENT_NOT_OWNED, INVALID_EQUIPMENT -> RuneInfusionResultPayload.INVALID_EQUIPMENT;
                case INVALID_SELECTION -> RuneInfusionResultPayload.INVALID_SELECTION;
                case INSUFFICIENT_RUNES -> RuneInfusionResultPayload.NOT_ENOUGH_RUNES;
                case PAYMENT_FAILED -> RuneInfusionResultPayload.PAYMENT_FAILED;
                case STARTED -> RuneInfusionResultPayload.STARTED;
            };
            send(player, code, 0, -1);
            return;
        }

        RuneInfusionSession session = RuneInfusionService.getActiveSession(player);
        UUID sessionId = session == null ? null : session.sessionId();

        // Difficulty = SUM of the (server-validated) target levels of every selected enchantment.
        int difficulty = 0;
        if (session != null) {
            for (RuneInfusionRules.Selection selection : session.selections()) {
                difficulty += selection.targetLevel();
            }
        }

        boolean stateReady = sessionId != null && RuneInfusionService.markInProgress(player);
        RuneInfusionMinigameController.StartResult gameStart = stateReady
                ? RuneInfusionMinigameController.start(player.getUUID(), sessionId,
                        player.getRandom().nextLong(), Math.max(1, difficulty))
                : RuneInfusionMinigameController.StartResult.INVALID_ARGUMENTS;

        if (gameStart != RuneInfusionMinigameController.StartResult.STARTED) {
            RuneInfusionService.fail(player);
            RuneInfusionService.clearTerminalSession(player);
            send(player, RuneInfusionResultPayload.PAYMENT_FAILED, 0, -1);
            return;
        }

        send(player, RuneInfusionResultPayload.STARTED, selections.size(), -1);
    }

    private static void send(ServerPlayer player, int result, int cost, int grade) {
        ServerPlayNetworking.send(player, new RuneInfusionResultPayload(result, cost, grade));
    }
}