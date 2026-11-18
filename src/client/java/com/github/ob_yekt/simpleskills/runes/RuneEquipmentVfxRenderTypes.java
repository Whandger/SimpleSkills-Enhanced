
package com.github.ob_yekt.simpleskills.runes;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Texture-free render types for procedural Rune equipment VFX.
 *
 * All visual effects are generated through vertex geometry and colors.
 * No PNG textures or sprite assets are required.
 */
public final class RuneEquipmentVfxRenderTypes {

    private RuneEquipmentVfxRenderTypes() {}

    /**
     * Procedural flame geometry.
     * Uses vertex colors to create animated fire effects.
     */
    public static RenderType flame() {
        return RenderTypes.lightning();
    }

    /**
     * Procedural shadow geometry.
     * Uses vertex colors and transparency for dark energy effects.
     */
    public static RenderType shadow() {
        return RenderTypes.lightning();
    }
}

