package com.github.ob_yekt.simpleskills.shields;

import java.util.Objects;
import java.util.function.Consumer;

import com.github.ob_yekt.simpleskills.runes.RuneEquipmentVfxRenderer;
import com.github.ob_yekt.simpleskills.runes.RuneVisualStyles;
import com.github.ob_yekt.simpleskills.runes.RuneVisualStylesClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Renderiza os Escudos Reais de Glowstone em 3D:
 * 1) Corpo metalico, coroa, nervuras de ouro/lapis-lazuli e engastes (iluminacao normal do mundo).
 * 2) Pedras de glowstone extrudadas em 3D em fullbright (brilham no escuro).
 * 3) Suporte completo a padroes de banner, glint customizado de runas e efeitos visuais 3D.
 */
public final class GlowstoneShieldSpecialRenderer implements SpecialModelRenderer<DataComponentMap> {
    public static final Transformation DEFAULT_TRANSFORMATION =
            new Transformation(null, null, new Vector3f(1.0F, -1.0F, -1.0F), null);

    private static final int FULL_BRIGHT = 0x00F000F0;

    private final SpriteGetter sprites;
    private final ShieldModel model;
    private final ShieldModel crystalModel;
    private final SpriteId baseSprite;
    private final SpriteId baseSpriteNoPattern;

    public GlowstoneShieldSpecialRenderer(
            SpriteGetter sprites,
            ShieldModel model,
            ShieldModel crystalModel,
            SpriteId baseSprite,
            SpriteId baseSpriteNoPattern
    ) {
        this.sprites = sprites;
        this.model = model;
        this.crystalModel = crystalModel;
        this.baseSprite = baseSprite;
        this.baseSpriteNoPattern = baseSpriteNoPattern;
    }

    @Override
    public @Nullable DataComponentMap extractArgument(ItemStack stack) {
        return stack.immutableComponents();
    }

    @Override
    public void submit(
            @Nullable DataComponentMap components,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        BannerPatternLayers patterns = components != null
                ? components.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                : BannerPatternLayers.EMPTY;
        DyeColor baseColor = components != null ? components.get(DataComponents.BASE_COLOR) : null;
        boolean hasPatterns = !patterns.layers().isEmpty() || baseColor != null;
        SpriteId sprite = hasPatterns ? this.baseSprite : this.baseSpriteNoPattern;
        int style = RuneVisualStyles.getStyle(components);

        if (hasFoil && !hasPatterns) {
            RenderType glintType = RuneVisualStylesClient.shieldEntity(style, sprite.atlasLocation());
            if (glintType == null) {
                glintType = RenderTypes.entitySolidGlint(sprite.atlasLocation());
            }
            submitNodeCollector.submitModel(
                    this.model, Unit.INSTANCE, poseStack,
                    glintType,
                    lightCoords, overlayCoords, -1, this.sprites.get(sprite), outlineColor
            );
            submitNodeCollector.submitModel(
                    this.crystalModel, Unit.INSTANCE, poseStack,
                    glintType,
                    FULL_BRIGHT, overlayCoords, -1, this.sprites.get(sprite), outlineColor
            );
        } else {
            submitNodeCollector.submitModel(
                    this.model, Unit.INSTANCE, poseStack,
                    lightCoords, overlayCoords, -1, sprite, this.sprites, outlineColor
            );
            submitNodeCollector.submitModel(
                    this.crystalModel, Unit.INSTANCE, poseStack,
                    FULL_BRIGHT, overlayCoords, -1, sprite, this.sprites, outlineColor
            );
        }

        if (hasPatterns) {
            BannerRenderer.submitPatterns(
                    this.sprites, poseStack, submitNodeCollector, lightCoords, overlayCoords,
                    this.model, Unit.INSTANCE, false, Objects.requireNonNullElse(baseColor, DyeColor.WHITE), patterns
            );
            if (hasFoil) {
                RenderType patternGlintType = RuneVisualStylesClient.patternedShield(style);
                if (patternGlintType == null) {
                    patternGlintType = RenderTypes.patternedShieldGlint();
                }
                submitNodeCollector.order(patterns.layers().size() + 2).submitModel(
                        this.model, Unit.INSTANCE, poseStack, patternGlintType,
                        lightCoords, overlayCoords, -1, this.sprites.get(sprite), 0
                );
                submitNodeCollector.order(patterns.layers().size() + 3).submitModel(
                        this.crystalModel, Unit.INSTANCE, poseStack, patternGlintType,
                        FULL_BRIGHT, overlayCoords, -1, this.sprites.get(sprite), 0
                );
            }
        }

        RuneEquipmentVfxRenderer.submitShield(components, poseStack, submitNodeCollector, true);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.root().getExtentsForGui(poseStack, output);
        this.crystalModel.root().getExtentsForGui(poseStack, output);
    }

    public record Unbaked(Identifier base, Identifier noPattern)
            implements SpecialModelRenderer.Unbaked<DataComponentMap> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::base),
                Identifier.CODEC.fieldOf("no_pattern_texture").forGetter(Unbaked::noPattern)
        ).apply(i, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public GlowstoneShieldSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new GlowstoneShieldSpecialRenderer(
                    context.sprites(),
                    new ShieldModel(context.entityModelSet().bakeLayer(GlowstoneShieldLayers.GLOWSTONE_SHIELD)),
                    new ShieldModel(context.entityModelSet().bakeLayer(GlowstoneShieldLayers.GLOWSTONE_SHIELD_CRYSTALS)),
                    Sheets.SHIELD_MAPPER.apply(this.base),
                    Sheets.SHIELD_MAPPER.apply(this.noPattern)
            );
        }
    }
}
