package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.fishing.model.AquaticEntityModel;
import com.github.ob_yekt.simpleskills.fishing.model.AquaticModelLayers;
import com.github.ob_yekt.simpleskills.fishing.renderer.AquaticFishRenderer;
import com.github.ob_yekt.simpleskills.fishing.renderer.MarineMammalRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client-side model-layer and entity-renderer registration for the aquatic expansion. */
public final class FishingEntityClient {
    private FishingEntityClient() {}

    public static void register() {
        AquaticModelLayers.register();
        registerFish(FishingEntities.TROUT, AquaticModelLayers.TROUT, AquaticEntityModel.Shape.TROUT, "entity/fish/trout.png", 0.18F);
        registerFish(FishingEntities.BASS, AquaticModelLayers.BASS, AquaticEntityModel.Shape.BASS, "entity/fish/bass.png", 0.22F);
        registerFish(FishingEntities.CATFISH, AquaticModelLayers.CATFISH, AquaticEntityModel.Shape.CATFISH, "entity/fish/catfish.png", 0.24F);
        registerFish(FishingEntities.PIKE, AquaticModelLayers.PIKE, AquaticEntityModel.Shape.PIKE, "entity/fish/pike.png", 0.23F);
        registerFish(FishingEntities.CARP, AquaticModelLayers.CARP, AquaticEntityModel.Shape.CARP, "entity/fish/carp.png", 0.22F);
        registerFish(FishingEntities.TUNA, AquaticModelLayers.TUNA, AquaticEntityModel.Shape.TUNA, "entity/fish/tuna.png", 0.35F);
        registerFish(FishingEntities.SWORDFISH, AquaticModelLayers.SWORDFISH, AquaticEntityModel.Shape.SWORDFISH, "entity/fish/swordfish.png", 0.40F);
        registerFish(FishingEntities.MAHI_MAHI, AquaticModelLayers.MAHI_MAHI, AquaticEntityModel.Shape.MAHI_MAHI, "entity/fish/mahi_mahi.png", 0.32F);
        registerFish(FishingEntities.REEF_SHARK, AquaticModelLayers.REEF_SHARK, AquaticEntityModel.Shape.REEF_SHARK, "entity/fish/reef_shark.png", 0.48F);
        registerFish(FishingEntities.GREAT_WHITE_SHARK, AquaticModelLayers.GREAT_WHITE_SHARK, AquaticEntityModel.Shape.GREAT_WHITE_SHARK, "entity/fish/great_white_shark.png", 0.62F);
        registerFish(FishingEntities.WHALE_SHARK, AquaticModelLayers.WHALE_SHARK, AquaticEntityModel.Shape.WHALE_SHARK, "entity/fish/whale_shark.png", 0.85F);

        EntityRendererRegistry.register(FishingEntities.HUMPBACK_WHALE,
                context -> new MarineMammalRenderer(context, AquaticModelLayers.HUMPBACK_WHALE,
                        AquaticEntityModel.Shape.HUMPBACK_WHALE, "entity/fish/humpback_whale.png"));
        EntityRendererRegistry.register(FishingEntities.ORCA,
                context -> new MarineMammalRenderer(context, AquaticModelLayers.ORCA,
                        AquaticEntityModel.Shape.ORCA, "entity/fish/orca.png"));
    }

    private static void registerFish(net.minecraft.world.entity.EntityType<FishingFishEntity> type,
                                     net.minecraft.client.model.geom.ModelLayerLocation layer,
                                     AquaticEntityModel.Shape shape, String texture, float shadow) {
        EntityRendererRegistry.register(type, context -> new AquaticFishRenderer(context, layer, shape, texture, shadow));
    }
}
