package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.excavating.InvestigationHud;
import com.github.ob_yekt.simpleskills.excavating.InvestigationPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class SimpleskillsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Keybinds.register();
        SkillHudRenderer.register();
        ClientConfig.load();
        InvestigationHud.register();

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

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            GatheringSpeed.resetClientLevels();
            InvestigationHud.reset();
        });
        Simpleskills.LOGGER.info("[simpleskills] Skill HUD & keybinds registered.");
    }

    public static boolean isMultiplayer() {
        return !Minecraft.getInstance().hasSingleplayerServer();
    }
}