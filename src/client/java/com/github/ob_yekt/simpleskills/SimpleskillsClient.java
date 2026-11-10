package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.excavating.InvestigationHud;
import com.github.ob_yekt.simpleskills.excavating.InvestigationPayload;
import com.github.ob_yekt.simpleskills.fishing.FishingCatchPayload;
import com.github.ob_yekt.simpleskills.fishing.FishingCatchScreen;
import com.github.ob_yekt.simpleskills.fishing.FishingHud;
import com.github.ob_yekt.simpleskills.fishing.FishingStatePayload;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionStatePayload;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionClientState;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionCatalogResponsePayload;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionPreparationScreen;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class SimpleskillsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Keybinds.register();
        SkillHudRenderer.register();
        ClientConfig.load();
        InvestigationHud.register();
        FishingHud.register();

        // Gathering Speed: levels pushed by the server (applied in GatheringSpeedMixin)
        // Redireciona a atualização para a thread principal do cliente para garantir thread safety
        ClientPlayNetworking.registerGlobalReceiver(GatheringSpeed.LevelsPayload.TYPE, (payload, context) ->
            context.client().execute(() ->
                GatheringSpeed.setClientLevels(payload.mining(), payload.woodcutting(), payload.excavating())
            )
        );

        // Excavating: progress of the server-side investigation, drawn as a bar under the crosshair
        ClientPlayNetworking.registerGlobalReceiver(InvestigationPayload.TYPE, (payload, context) ->
            context.client().execute(() -> InvestigationHud.onPayload(payload))
        );

        // Fishing: server-owned minigame state, drawn as tension / fatigue / progress bars
        ClientPlayNetworking.registerGlobalReceiver(FishingStatePayload.TYPE, (payload, context) ->
            context.client().execute(() -> FishingHud.onPayload(payload))
        );

        ClientPlayNetworking.registerGlobalReceiver(RuneInfusionCatalogResponsePayload.TYPE, (payload, context) ->
            context.client().execute(() ->
                context.client().gui.setScreen(new RuneInfusionPreparationScreen(payload))
            )
        );

        ClientPlayNetworking.registerGlobalReceiver(RuneInfusionResultPayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                if (context.client().player == null) return;
                String key = switch (payload.resultCode()) {
                    case RuneInfusionResultPayload.STARTED -> "message.simpleskills.rune_infusion.started";
                    case RuneInfusionResultPayload.PLAYER_BUSY -> "message.simpleskills.rune_infusion.busy";
                    case RuneInfusionResultPayload.INVALID_EQUIPMENT -> "message.simpleskills.rune_infusion.invalid_equipment";
                    case RuneInfusionResultPayload.INVALID_SELECTION -> "message.simpleskills.rune_infusion.invalid_selection";
                    case RuneInfusionResultPayload.NOT_ENOUGH_RUNES -> "message.simpleskills.rune_infusion.not_enough_runes";
                    case RuneInfusionResultPayload.PAYMENT_FAILED -> "message.simpleskills.rune_infusion.payment_failed";
                    case RuneInfusionResultPayload.SUCCEEDED -> "message.simpleskills.rune_infusion.success";
                    case RuneInfusionResultPayload.FAILED -> "message.simpleskills.rune_infusion.failed";
                    default -> "message.simpleskills.rune_infusion.invalid_selection";
                };

                // In this Minecraft API, LocalPlayer exposes sendSystemMessage(Component).
                // Gui.getChat() and Gui.setOverlayMessage() are not available.
                context.client().player.sendSystemMessage(Component.translatable(key));
            })
        );

        // Rune Infusion: authoritative server snapshot for the minigame screen.
        ClientPlayNetworking.registerGlobalReceiver(RuneInfusionStatePayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                boolean wasActive = RuneInfusionClientState.get() != null;
                RuneInfusionClientState.accept(payload);
                if (payload.state() == RuneInfusionStatePayload.STOP) {
                    if (wasActive) {
                        context.client().gui.setScreen(null);
                    }
                } else if (!wasActive && context.client().player != null) {
                    context.client().gui.setScreen(
                        new com.github.ob_yekt.simpleskills.enchanting.RuneInfusionMinigameScreen()
                    );
                }
            })
        );

        // Fishing: result screen of a catch the server already confirmed (display only)
        ClientPlayNetworking.registerGlobalReceiver(FishingCatchPayload.TYPE, (payload, context) ->
            context.client().execute(() ->
                context.client().gui.setScreen(new FishingCatchScreen(payload))
            )
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            GatheringSpeed.resetClientLevels();
            InvestigationHud.reset();
            FishingHud.reset();
            RuneInfusionClientState.reset();
        });
        Simpleskills.LOGGER.info("[simpleskills] Skill HUD & keybinds registered.");
    }

    public static boolean isMultiplayer() {
        return !Minecraft.getInstance().hasSingleplayerServer();
    }
}
