package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.runes.RuneContent;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Server-authoritative network boundary for Rune Infusion preparation and start. */
public final class RuneInfusionNetworking {
    private RuneInfusionNetworking() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(RuneInfusionStartPayload.TYPE, RuneInfusionStartPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RuneInfusionInputPayload.TYPE, RuneInfusionInputPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RuneInfusionCatalogRequestPayload.TYPE, RuneInfusionCatalogRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RuneInfusionCatalogResponsePayload.TYPE, RuneInfusionCatalogResponsePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RuneInfusionResultPayload.TYPE, RuneInfusionResultPayload.CODEC);
    }

    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(RuneInfusionCatalogRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleCatalog(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(RuneInfusionStartPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleStart(context.player(), payload)));
    }

    private static void handleCatalog(ServerPlayer player, RuneInfusionCatalogRequestPayload payload) {
        Inventory inventory = player.getInventory();
        if (payload == null || payload.inventorySlot() < 0
                || payload.inventorySlot() >= inventory.getNonEquipmentItems().size()) return;

        ItemStack equipment = inventory.getItem(payload.inventorySlot());
        List<RuneInfusionCatalog.Entry> entries = RuneInfusionCatalog.getAvailable(player, equipment, Set.of());
        StringBuilder rows = new StringBuilder();
        for (RuneInfusionCatalog.Entry entry : entries) {
            if (!rows.isEmpty()) rows.append(';');
            rows.append(entry.id()).append('|').append(entry.currentLevel()).append('|')
                    .append(entry.maximumSelectableLevel());
            if (rows.length() > 24_000) break;
        }

        int runes = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(RuneContent.RUNE)) runes += stack.getCount();
        }

        ServerPlayNetworking.send(player, new RuneInfusionCatalogResponsePayload(
                payload.inventorySlot(), runes, rows.toString()));
    }

    private static void handleStart(ServerPlayer player, RuneInfusionStartPayload payload) {
        Inventory inventory = player.getInventory();
        if (payload == null || payload.inventorySlot() < 0
                || payload.inventorySlot() >= inventory.getNonEquipmentItems().size()
                || payload.enchantmentIds().size() != payload.targetLevels().size()
                || payload.enchantmentIds().isEmpty()
                || payload.enchantmentIds().size() > RuneInfusionRules.MAX_SELECTED_ENCHANTMENTS) {
            send(player, RuneInfusionResultPayload.INVALID_SELECTION, 0, -1);
            return;
        }

        ItemStack equipment = inventory.getItem(payload.inventorySlot());
        List<RuneInfusionRules.Selection> selections = new ArrayList<>();
        int highestLevel = 1;
        for (int i = 0; i < payload.enchantmentIds().size(); i++) {
            String id = payload.enchantmentIds().get(i);
            Integer level = payload.targetLevels().get(i);
            if (id == null || id.length() > 128 || level == null || level < 1 || level > 10) {
                send(player, RuneInfusionResultPayload.INVALID_SELECTION, 0, -1);
                return;
            }
            selections.add(new RuneInfusionRules.Selection(id, level, 1));
            highestLevel = Math.max(highestLevel, level);
        }

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

        java.util.UUID sessionId = RuneInfusionService.getActiveSessionId(player);
        boolean stateReady = sessionId != null && RuneInfusionService.markInProgress(player);
        RuneInfusionMinigameController.StartResult gameStart = stateReady
                ? RuneInfusionMinigameController.start(player.getUUID(), sessionId, player.getRandom().nextLong(),
                    selections.size(), highestLevel)
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
