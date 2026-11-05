package com.github.ob_yekt.simpleskills;

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

        // Gathering Speed: levels pushed by the server (applied in GatheringSpeedMixin)
        // Redireciona a atualização para a thread principal do cliente para garantir thread safety
        ClientPlayNetworking.registerGlobalReceiver(GatheringSpeed.LevelsPayload.TYPE, (payload, context) -> 
            context.client().execute(() -> 
                GatheringSpeed.setClientLevels(payload.mining(), payload.woodcutting(), payload.excavating())
            )
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> GatheringSpeed.resetClientLevels());
        Simpleskills.LOGGER.info("[simpleskills] Skill HUD & keybinds registered.");
    }

    public static boolean isMultiplayer() {
        return !Minecraft.getInstance().hasSingleplayerServer();
    }
}