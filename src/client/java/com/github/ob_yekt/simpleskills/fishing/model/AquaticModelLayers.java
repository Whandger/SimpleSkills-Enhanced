package com.github.ob_yekt.simpleskills.fishing.model;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/** One baked layer per fish anatomy so each species can use distinct geometry. */
public final class AquaticModelLayers {
    public static final ModelLayerLocation TROUT = layer("trout");
    public static final ModelLayerLocation BASS = layer("bass");
    public static final ModelLayerLocation CATFISH = layer("catfish");
    public static final ModelLayerLocation PIKE = layer("pike");
    public static final ModelLayerLocation CARP = layer("carp");
    public static final ModelLayerLocation TUNA = layer("tuna");
    public static final ModelLayerLocation SWORDFISH = layer("swordfish");
    public static final ModelLayerLocation MAHI_MAHI = layer("mahi_mahi");
    public static final ModelLayerLocation REEF_SHARK = layer("reef_shark");
    public static final ModelLayerLocation GREAT_WHITE_SHARK = layer("great_white_shark");
    public static final ModelLayerLocation WHALE_SHARK = layer("whale_shark");
    public static final ModelLayerLocation HUMPBACK_WHALE = layer("humpback_whale");
    public static final ModelLayerLocation ORCA = layer("orca");

    private AquaticModelLayers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath("simpleskills", name), "main");
    }

    public static void register() {
        register(TROUT, AquaticEntityModel.Shape.TROUT);
        register(BASS, AquaticEntityModel.Shape.BASS);
        register(CATFISH, AquaticEntityModel.Shape.CATFISH);
        register(PIKE, AquaticEntityModel.Shape.PIKE);
        register(CARP, AquaticEntityModel.Shape.CARP);
        register(TUNA, AquaticEntityModel.Shape.TUNA);
        register(SWORDFISH, AquaticEntityModel.Shape.SWORDFISH);
        register(MAHI_MAHI, AquaticEntityModel.Shape.MAHI_MAHI);
        register(REEF_SHARK, AquaticEntityModel.Shape.REEF_SHARK);
        register(GREAT_WHITE_SHARK, AquaticEntityModel.Shape.GREAT_WHITE_SHARK);
        register(WHALE_SHARK, AquaticEntityModel.Shape.WHALE_SHARK);
        register(HUMPBACK_WHALE, AquaticEntityModel.Shape.HUMPBACK_WHALE);
        register(ORCA, AquaticEntityModel.Shape.ORCA);
    }

    private static void register(ModelLayerLocation layer, AquaticEntityModel.Shape shape) {
        ModelLayerRegistry.registerModelLayer(layer, () -> AquaticEntityModel.createLayer(shape));
    }
}
