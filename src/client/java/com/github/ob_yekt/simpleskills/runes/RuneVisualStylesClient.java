package com.github.ob_yekt.simpleskills.runes;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Builds native item-glint render types while preserving the atlas used by each item sprite. */
public final class RuneVisualStylesClient {
    private static final String[] TEXTURES = {
            "", "rune_glint_arcane", "rune_glint_inferno", "rune_glint_frost",
            "rune_glint_storm", "rune_glint_divine", "rune_glint_shadow"
    };

    /**
     * Item sprites can come from different atlases (notably blocks and items in 26.3).
     * The base Sampler0 must match the atlas of the BakedQuad being rendered.
     */
    private record CacheKey(Identifier atlas, int style, boolean translucent, boolean special) {}

    private static final Map<CacheKey, RenderType> CACHE = new ConcurrentHashMap<>();

    private RuneVisualStylesClient() {}

    public static RenderType standard(int style, boolean translucent, Identifier itemAtlas) {
        return get(style, translucent, false, itemAtlas);
    }

    public static RenderType special(int style, boolean translucent, Identifier itemAtlas) {
        return get(style, translucent, true, itemAtlas);
    }

    private static RenderType get(int style, boolean translucent, boolean special, Identifier itemAtlas) {
        if (style <= RuneVisualStyles.VANILLA || style > RuneVisualStyles.MAX_STYLE || itemAtlas == null) {
            return null;
        }

        CacheKey key = new CacheKey(itemAtlas, style, translucent, special);
        return CACHE.computeIfAbsent(key, ignored -> create(style, translucent, special, itemAtlas));
    }

    private static RenderType create(int style, boolean translucent, boolean special, Identifier itemAtlas) {
        Identifier glintTexture = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID,
                "textures/misc/" + TEXTURES[style] + ".png");
        var pipeline = translucent
                ? (special ? RenderPipelines.ITEM_TRANSLUCENT_GLINT_SPECIAL : RenderPipelines.ITEM_TRANSLUCENT_GLINT)
                : (special ? RenderPipelines.ITEM_CUTOUT_GLINT_SPECIAL : RenderPipelines.ITEM_CUTOUT_GLINT);

        RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(pipeline)
                // Do not hard-code the blocks atlas: item sprites may be baked into items.png-atlas.
                .withTexture("Sampler0", itemAtlas)
                .withTexture("GlintSampler", glintTexture)
                .setTextureTransform(TextureTransform.GLINT_TEXTURING)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        if (translucent) {
            builder.setOitPipelines(special ? RenderPipelines.OIT_ITEM_GLINT_SPECIAL : RenderPipelines.OIT_ITEM_GLINT)
                    .sortOnUpload();
        }

        return RenderType.create("simpleskills_rune_glint_" + style
                + (translucent ? "_translucent" : "_cutout")
                + (special ? "_special" : "")
                + "_" + itemAtlas.getPath().replace('/', '_'), builder.createRenderSetup());
    }
}
