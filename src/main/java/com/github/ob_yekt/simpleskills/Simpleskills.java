package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.commands.SimpleskillsCommands;
import com.github.ob_yekt.simpleskills.managers.*;
import com.github.ob_yekt.simpleskills.events.EventHandlers;
import com.github.ob_yekt.simpleskills.excavating.ExcavatingManager;
import com.github.ob_yekt.simpleskills.excavating.ModBlocks;
import com.github.ob_yekt.simpleskills.runes.RuneContent;
import com.github.ob_yekt.simpleskills.runes.RuneWorldgen;
import com.github.ob_yekt.simpleskills.runes.RuneMiningManager;
import com.github.ob_yekt.simpleskills.farming.BoneMealMastery;
import com.github.ob_yekt.simpleskills.fishing.FishingManager;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionMinigameRuntime;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionStatePayload;
import com.github.ob_yekt.simpleskills.enchanting.RuneInfusionNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Simpleskills implements ModInitializer {
	public static final String MOD_ID = "simpleskills";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);



	@Override
	public void onInitialize() {
		LOGGER.info("Initializing SimpleSkills mod...");
		SimpleSkillsSounds.register();

		// Blocks must be registered during initialization, before the registries freeze.
		ModBlocks.register();
		RuneContent.register();
		RuneMiningManager.register();
		RuneWorldgen.register();

		GatheringSpeed.registerPayloads();
		ExcavatingManager.registerPayloads();
		FishingManager.registerPayloads();
		SimpleskillsCommands.registerCommands();
		IronmanManager.init();
		AttributeManager.registerPlayerEvents();
		LoreManager.initialize();
		ConfigManager.initialize();
		EventHandlers.registerAll();
		DefenseCombat.register();
		BoneMealMastery.register();
		TreeFeller.register();
		ExcavatingManager.register();
		FishingManager.register();
		RuneInfusionNetworking.registerPayloads();
		PayloadTypeRegistry.clientboundPlay().register(RuneInfusionStatePayload.TYPE, RuneInfusionStatePayload.CODEC);
		RuneInfusionNetworking.registerReceivers();
		RuneInfusionMinigameRuntime.register();

		// Initialize database and setup server lifecycle hooks
		DatabaseManager db = DatabaseManager.getInstance();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			ConfigManager.initialize();
			db.initializeDatabase(server);
			LOGGER.info("Database and config initialized for server.");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			db.close();
			LOGGER.info("Database connection closed on server stop.");
		});

		LOGGER.info("SimpleSkills mod initialized successfully!");
	}
}
