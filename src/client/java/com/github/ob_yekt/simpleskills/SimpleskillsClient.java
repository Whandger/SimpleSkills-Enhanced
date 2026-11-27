package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.excavating.InvestigationHud;
import com.github.ob_yekt.simpleskills.crafting.CraftingRevealPayload;
import com.github.ob_yekt.simpleskills.crafting.CraftingRevealScreen;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarAnimationController;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarContent;
import com.github.ob_yekt.simpleskills.abyss.AbyssContent;
import com.github.ob_yekt.simpleskills.abyss.AbyssalChestScreen;
import com.github.ob_yekt.simpleskills.abyss.AbyssalFallenKingClient;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientStanceState;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientRendering;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaStancePayload;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarScreen;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarVisualPayload;
import com.github.ob_yekt.simpleskills.grinder.OreGrinderContent;
import com.github.ob_yekt.simpleskills.grinder.OreGrinderRenderer;
import com.github.ob_yekt.simpleskills.grinder.OreGrinderScreen;
import com.github.ob_yekt.simpleskills.crafting.TranscendentVisionPayload;
import com.github.ob_yekt.simpleskills.crafting.TranscendentVisionHud;
import com.github.ob_yekt.simpleskills.crafting.WorldbreakerPreviewPayload;
import com.github.ob_yekt.simpleskills.excavating.InvestigationPayload;
import com.github.ob_yekt.simpleskills.fishing.FishingCatchPayload;
import com.github.ob_yekt.simpleskills.fishing.FishingCatchScreen;
import com.github.ob_yekt.simpleskills.fishing.FishingHud;
import com.github.ob_yekt.simpleskills.fishing.FishingEntityClient;
import com.github.ob_yekt.simpleskills.fishing.FishingStatePayload;
import com.github.ob_yekt.simpleskills.agility.AgilityLevelPayload;
import com.github.ob_yekt.simpleskills.agility.AgilityDashCooldownPayload;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionStatePayload;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionClientState;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionCatalogResponsePayload;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionPreparationScreen;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionResultPayload;
import com.github.ob_yekt.simpleskills.runes.RuneMiningClient;
import com.github.ob_yekt.simpleskills.ui.RankTitleMenuDataPayload;
import com.github.ob_yekt.simpleskills.ui.SkillOverviewDataPayload;
import com.github.ob_yekt.simpleskills.prayer.PrayerMenuDataPayload;
import com.github.ob_yekt.simpleskills.prayer.PrayerClientState;
import com.github.ob_yekt.simpleskills.prayer.PrayerCooldownPayload;
import com.github.ob_yekt.simpleskills.prayer.PrayerCooldownHud;
import com.github.ob_yekt.simpleskills.prayer.PrayerImpactFeedback;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxEntity;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxRenderer;
import com.github.ob_yekt.simpleskills.shields.GlowstoneShieldLayers;
import com.github.ob_yekt.simpleskills.shields.GlowstoneShieldSpecialRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;

public class SimpleskillsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpecialModelRenderers.ID_MAPPER.put(
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "glowstone_shield"),
                GlowstoneShieldSpecialRenderer.Unbaked.MAP_CODEC
        );
        ModelLayerRegistry.registerModelLayer(
                GlowstoneShieldLayers.GLOWSTONE_SHIELD,
                GlowstoneShieldLayers::createBodyLayer
        );
        ModelLayerRegistry.registerModelLayer(
                GlowstoneShieldLayers.GLOWSTONE_SHIELD_CRYSTALS,
                GlowstoneShieldLayers::createCrystalLayer
        );

        FishingEntityClient.register();
        AbyssalFallenKingClient.register();
        MenuScreens.register(CelestialAltarContent.MENU_TYPE, CelestialAltarScreen::new);
        MenuScreens.register(OreGrinderContent.MENU_TYPE, OreGrinderScreen::new);
        MenuScreens.register(AbyssContent.ABYSSAL_CHEST_MENU_TYPE, AbyssalChestScreen::new);
        MenuScreens.register(AbyssContent.ABYSSAL_DOUBLE_CHEST_MENU_TYPE, AbyssalChestScreen::new);
        BlockEntityRenderers.register(OreGrinderContent.BLOCK_ENTITY_TYPE, OreGrinderRenderer::new);
        EntityRendererRegistry.register(PrayerVfxEntity.TYPE, PrayerVfxRenderer::new);
        Keybinds.register();
        KatanaClientRendering.register();
        SkillHudRenderer.register();
        ClientConfig.load();
        InvestigationHud.register();
        TranscendentVisionHud.register();
        FishingHud.register();
        AgilityDashHud.register();
        PrayerCooldownHud.register();
        PrayerImpactFeedback.registerClientHooks();

        ClientPlayNetworking.registerGlobalReceiver(KatanaStancePayload.TYPE, (payload, context) ->
                context.client().execute(() -> KatanaClientStanceState.accept(payload)));

        ClientPlayNetworking.registerGlobalReceiver(CelestialAltarVisualPayload.TYPE, (payload, context) ->
                context.client().execute(() -> CelestialAltarAnimationController.onServerConfirmed(payload)));
        ClientPlayNetworking.registerGlobalReceiver(CraftingRevealPayload.TYPE, (payload, context) ->
                context.client().execute(() -> CelestialAltarAnimationController.interceptOrForwardReveal(payload)));
        ClientPlayNetworking.registerGlobalReceiver(TranscendentVisionPayload.TYPE, (payload, context) ->
                context.client().execute(() -> TranscendentVisionHud.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(WorldbreakerPreviewPayload.TYPE, (payload, context) ->
                context.client().execute(() -> TranscendentVisionHud.acceptWorldbreakerPreview(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(TranscendentVisionHud::clear));

        ClientPlayNetworking.registerGlobalReceiver(AgilityLevelPayload.TYPE, (payload, context) ->
                context.client().execute(() -> AgilityClientState.set(payload)));
        ClientPlayNetworking.registerGlobalReceiver(AgilityDashCooldownPayload.TYPE, (payload, context) ->
                context.client().execute(() -> AgilityDashHud.onCooldown(payload)));
        ClientPlayNetworking.registerGlobalReceiver(RankTitleMenuDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> RankTitleMenuScreen.showOrUpdate(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SkillOverviewDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> SkillOverviewScreen.showOrUpdate(payload)));
        ClientPlayNetworking.registerGlobalReceiver(PrayerMenuDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PrayerClientState.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(PrayerCooldownPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PrayerCooldownHud.accept(payload)));

        // Gathering Speed: levels pushed by the server (applied in GatheringSpeedMixin)
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
                if (payload.resultCode() == RuneInfusionResultPayload.STYLE_APPLIED
                        || payload.resultCode() == RuneInfusionResultPayload.STYLE_NOT_ENOUGH_LORE
                        || payload.resultCode() == RuneInfusionResultPayload.STYLE_INVALID
                        || payload.resultCode() == RuneInfusionResultPayload.STYLE_BUSY
                        || payload.resultCode() == RuneInfusionResultPayload.STYLE_TOO_FAR) {
                    RuneInfusionPreparationScreen.acceptStyleResult(payload);
                    if (!(context.client().gui.screen() instanceof RuneInfusionPreparationScreen)) {
                        String styleKey = payload.resultCode() == RuneInfusionResultPayload.STYLE_APPLIED
                                ? "message.simpleskills.rune_infusion.style_applied"
                                : payload.resultCode() == RuneInfusionResultPayload.STYLE_NOT_ENOUGH_LORE
                                ? "message.simpleskills.rune_infusion.style_not_enough"
                                : payload.resultCode() == RuneInfusionResultPayload.STYLE_BUSY
                                ? "message.simpleskills.rune_infusion.busy"
                                : payload.resultCode() == RuneInfusionResultPayload.STYLE_TOO_FAR
                                ? "message.simpleskills.rune_infusion.style_too_far"
                                : "message.simpleskills.rune_infusion.style_invalid";
                        context.client().player.sendSystemMessage(Component.translatable(styleKey));
                    }
                    return;
                }
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

                context.client().player.sendSystemMessage(Component.translatable(key));
            })
        );

        // Rune Infusion: authoritative server snapshot for the minigame screen.
        ClientPlayNetworking.registerGlobalReceiver(RuneInfusionStatePayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                RuneInfusionClientState.accept(payload);
                if (payload.state() == RuneInfusionStatePayload.STOP) {
                    if (!RuneInfusionClientState.hasScreen()) RuneInfusionClientState.reset();
                    return;
                }
                java.util.UUID sessionId = new java.util.UUID(payload.sessionMost(), payload.sessionLeast());
                if (!sessionId.equals(RuneInfusionClientState.shownSession())
                        && context.client().player != null) {
                    RuneInfusionClientState.markShown(sessionId);
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
            KatanaClientStanceState.clear();
            CelestialAltarAnimationController.reset();
            GatheringSpeed.resetClientLevels();
            AgilityClientState.reset();
            RankTitleMenuScreen.clearActive();
            PrayerClientState.reset();
            PrayerCooldownHud.reset();
            AgilityDashHud.reset();
            InvestigationHud.reset();
            FishingHud.reset();
            RuneInfusionClientState.reset();
            RuneMiningClient.clear();
        });
        Simpleskills.LOGGER.info("[simpleskills] Skill HUD & keybinds registered.");
    }

    public static boolean isMultiplayer() {
        return !Minecraft.getInstance().hasSingleplayerServer();
    }
}
