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

/** Builds native custom glint render types for item sprites, worn armor, and shields. */
public final class RuneVisualStylesClient {
    private static final String[] TEXTURES = {
            "", "rune_glint_arcane", "rune_glint_inferno", "rune_glint_frost",
            "rune_glint_storm", "rune_glint_divine", "rune_glint_shadow"
    };

    private record ItemCacheKey(Identifier atlas, int style, boolean translucent, boolean special) {}
    private record EntityLayerCacheKey(Identifier texture, int style) {}

    private static final Map<ItemCacheKey, RenderType> ITEM_CACHE = new ConcurrentHashMap<>();
    private static final Map<EntityLayerCacheKey, RenderType> ARMOR_LAYER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, RenderType> TRIMMED_ARMOR_CACHE = new ConcurrentHashMap<>();
    private static final Map<EntityLayerCacheKey, RenderType> SHIELD_ENTITY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, RenderType> PATTERNED_SHIELD_CACHE = new ConcurrentHashMap<>();

    private RuneVisualStylesClient() {}

    public static RenderType standard(int style, boolean translucent, Identifier itemAtlas) {
        return getItem(style, translucent, false, itemAtlas);
    }

    public static RenderType special(int style, boolean translucent, Identifier itemAtlas) {
        return getItem(style, translucent, true, itemAtlas);
    }

    /** Custom glint pass used by ordinary humanoid armor layers. */
    public static RenderType armorLayer(int style, Identifier armorTexture) {
        if (!isCustomStyle(style) || armorTexture == null) return null;
        EntityLayerCacheKey key = new EntityLayerCacheKey(armorTexture, style);
        return ARMOR_LAYER_CACHE.computeIfAbsent(key, ignored -> createArmorLayer(style, armorTexture));
    }

    /** Separate overlay pass used by armor pieces with trims. */
    public static RenderType trimmedArmor(int style) {
        if (!isCustomStyle(style)) return null;
        return TRIMMED_ARMOR_CACHE.computeIfAbsent(style, RuneVisualStylesClient::createTrimmedArmor);
    }

    /** Custom glint pass used by unpatterned shields (vanilla and Glowstone). */
    public static RenderType shieldEntity(int style, Identifier shieldAtlas) {
        if (!isCustomStyle(style) || shieldAtlas == null) return null;
        EntityLayerCacheKey key = new EntityLayerCacheKey(shieldAtlas, style);
        return SHIELD_ENTITY_CACHE.computeIfAbsent(key, ignored -> createShieldEntity(style, shieldAtlas));
    }

    /** Separate glint overlay pass used by shields carrying banner patterns. */
    public static RenderType patternedShield(int style) {
        if (!isCustomStyle(style)) return null;
        return PATTERNED_SHIELD_CACHE.computeIfAbsent(style, RuneVisualStylesClient::createPatternedShield);
    }

    private static RenderType getItem(int style, boolean translucent, boolean special, Identifier itemAtlas) {
        if (!isCustomStyle(style) || itemAtlas == null) return null;
        ItemCacheKey key = new ItemCacheKey(itemAtlas, style, translucent, special);
        return ITEM_CACHE.computeIfAbsent(key, ignored -> createItem(style, translucent, special, itemAtlas));
    }

    private static RenderType createItem(int style, boolean translucent, boolean special, Identifier itemAtlas) {
        Identifier glintTexture = glintTexture(style);
        var pipeline = translucent
                ? (special ? RenderPipelines.ITEM_TRANSLUCENT_GLINT_SPECIAL : RenderPipelines.ITEM_TRANSLUCENT_GLINT)
                : (special ? RenderPipelines.ITEM_CUTOUT_GLINT_SPECIAL : RenderPipelines.ITEM_CUTOUT_GLINT);

        RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(pipeline)
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

        return RenderType.create("simpleskills_rune_item_glint_" + style
                + (translucent ? "_translucent" : "_cutout")
                + (special ? "_special" : "")
                + "_" + itemAtlas.getPath().replace('/', '_'), builder.createRenderSetup());
    }

    private static RenderType createArmorLayer(int style, Identifier armorTexture) {
        RenderSetup setup = RenderSetup.builder(RenderPipelines.ARMOR_CUTOUT_NO_CULL_GLINT)
                .withTexture("Sampler0", armorTexture)
                .withTexture("GlintSampler", glintTexture(style))
                .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                .useLightmap()
                .useOverlay()
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup();
        return RenderType.create("simpleskills_rune_armor_glint_" + style + "_" + safeName(armorTexture), setup);
    }

    private static RenderType createTrimmedArmor(int style) {
        RenderSetup setup = RenderSetup.builder(RenderPipelines.GLINT)
                .withTexture("Sampler0", glintTexture(style))
                .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .withForcedSolidModelPhase()
                .createRenderSetup();
        return RenderType.create("simpleskills_rune_trimmed_armor_glint_" + style, setup);
    }

    private static RenderType createShieldEntity(int style, Identifier shieldAtlas) {
        RenderSetup setup = RenderSetup.builder(RenderPipelines.ARMOR_CUTOUT_NO_CULL_GLINT)
                .withTexture("Sampler0", shieldAtlas)
                .withTexture("GlintSampler", glintTexture(style))
                .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup();
        return RenderType.create("simpleskills_rune_shield_glint_" + style + "_" + safeName(shieldAtlas), setup);
    }

    private static RenderType createPatternedShield(int style) {
        RenderSetup setup = RenderSetup.builder(RenderPipelines.GLINT)
                .withTexture("Sampler0", glintTexture(style))
                .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .withForcedSolidModelPhase()
                .createRenderSetup();
        return RenderType.create("simpleskills_rune_patterned_shield_glint_" + style, setup);
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
}
