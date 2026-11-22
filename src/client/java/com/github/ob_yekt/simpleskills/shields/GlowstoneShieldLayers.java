package com.github.ob_yekt.simpleskills.shields;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/** Model layer used by the custom Glowstone Shield renderer. */
public final class GlowstoneShieldLayers {
    public static final ModelLayerLocation GLOWSTONE_SHIELD = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "glowstone_shield"),
            "main"
    );

    private GlowstoneShieldLayers() {
    }
}
