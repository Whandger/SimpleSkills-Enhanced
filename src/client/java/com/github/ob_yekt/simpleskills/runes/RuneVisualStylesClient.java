package com.github.ob_yekt.simpleskills.runes;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Builds native custom glint render types for item sprites and worn armor. */
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
    private record ArmorLayerCacheKey(Identifier armorTexture, int style) {}

    private static final Map<CacheKey, RenderType> CACHE = new ConcurrentHashMap<>();
    private static final Map<ArmorLayerCacheKey, RenderType> ARMOR_LAYER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, RenderType> TRIMMED_ARMOR_CACHE = new ConcurrentHashMap<>();

    private RuneVisualStylesClient() {}

    public static RenderType standard(int style, boolean translucent, Identifier itemAtlas) {
        return get(style, translucent, false, itemAtlas);
    }

    public static RenderType special(int style, boolean translucent, Identifier itemAtlas) {
        return get(style, translucent, true, itemAtlas);
    }


    /**
     * Creates the custom glint pass used by ordinary humanoid armor layers.
     * Armor uses a dedicated entity pipeline and the armor-specific texture
     * transform; item-glint render types are not compatible with this path.
     */
    public static RenderType armorLayer(int style, Identifier armorTexture) {
        if (!isCustomStyle(style) || armorTexture == null) return null;

        ArmorLayerCacheKey key = new ArmorLayerCacheKey(armorTexture, style);
        return ARMOR_LAYER_CACHE.computeIfAbsent(key,
                ignored -> createArmorLayer(style, armorTexture));
    }

    /**
     * Creates the glint pass used after armor trims. This is a separate vanilla
     * path from armorCutoutNoCullGlint and must also be replaced for trimmed armor.
     */
    public static RenderType trimmedArmor(int style) {
        if (!isCustomStyle(style)) return null;
        return TRIMMED_ARMOR_CACHE.computeIfAbsent(style, RuneVisualStylesClient::createTrimmedArmor);
    }

    private static RenderType createArmorLayer(int style, Identifier armorTexture) {
        Identifier texture = glintTexture(style);
        RenderSetup setup = RenderSetup.builder(RenderPipelines.ARMOR_CUTOUT_NO_CULL_GLINT)
                .withTexture("Sampler0", armorTexture)
                .withTexture("GlintSampler", texture)
                .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                .useLightmap()
                .useOverlay()
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup();
        return RenderType.create("simpleskills_armor_glint_" + style + "_" + safeName(armorTexture), setup);
    }

    private static RenderType createTrimmedArmor(int style) {
        RenderSetup setup = RenderSetup.builder(RenderPipelines.GLINT)
                .withTexture("Sampler0", glintTexture(style))
                .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .withForcedSolidModelPhase()
                .createRenderSetup();
        return RenderType.create("simpleskills_trimmed_armor_glint_" + style, setup);
    }

    private static boolean isCustomStyle(int style) {
        return style > RuneVisualStyles.VANILLA && style <= RuneVisualStyles.MAX_STYLE;
    }

    private static Identifier glintTexture(int style) {
        return Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID,
                "textures/misc/" + TEXTURES[style] + ".png");
    }

    private static String safeName(Identifier identifier) {
        return identifier.getPath().replace('/', '_');
    }

    private static RenderType get(int style, boolean translucent, boolean special, Identifier itemAtlas) {
        if (style <= RuneVisualStyles.VANILLA || style > RuneVisualStyles.MAX_STYLE || itemAtlas == null) {
            return null;
        }

        CacheKey key = new CacheKey(itemAtlas, style, translucent, special);
        return CACHE.computeIfAbsent(key, ignored -> create(style, translucent, special, itemAtlas));
    }

    private static RenderType create(int style, boolean translucent, boolean special, Identifier itemAtlas) {
        Identifier glintTexture = glintTexture(style);
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
