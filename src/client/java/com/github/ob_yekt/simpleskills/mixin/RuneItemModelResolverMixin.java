package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.runes.RuneVisualStyles;
import com.github.ob_yekt.simpleskills.runes.RuneVisualStylesClient;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Overrides only the glint passes for stacks carrying a Rune Visual Style component. */
@Mixin(ItemModelResolver.class)
public abstract class RuneItemModelResolverMixin {
    @Inject(method = "appendItemLayers", at = @At("TAIL"))
    private void simpleskills$applyRuneGlintStyle(ItemStackRenderState output, ItemStack item,
                                                  ItemDisplayContext displayContext, Level level,
                                                  ItemOwner owner, int seed, CallbackInfo ci) {
        int style = RuneVisualStyles.getStyle(item);
        if (style == RuneVisualStyles.VANILLA) return;

        ItemStackRenderStateAccessor state = (ItemStackRenderStateAccessor) output;
        ItemStackRenderState.LayerRenderState[] layers = state.simpleskills$getLayers();
        int count = Math.min(state.simpleskills$getActiveLayerCount(), layers.length);
        for (int i = 0; i < count; i++) {
            ItemStackLayerRenderStateAccessor layer = (ItemStackLayerRenderStateAccessor) layers[i];
            ItemQuads quads = layer.simpleskills$getQuads();
            if (quads == null || quads.isEmpty()) continue;

            // Replacing the glint RenderTypes alone is not enough: ItemFeatureRenderer
            // chooses the normal material whenever the layer FoilType is NONE. This can
            // happen for the client-only preview copy even though its stack has the
            // glint-override component. Force the foil pass for styled stacks so the
            // custom itemGlintRenderType is actually selected. Preserve SPECIAL foil.
            layer.simpleskills$setQuads(simpleskills$replaceGlint(quads, style));
            if (layer.simpleskills$getFoilType() == ItemStackRenderState.FoilType.NONE) {
                layer.simpleskills$setFoilType(ItemStackRenderState.FoilType.STANDARD);
            }
        }
    }

    private static ItemQuads simpleskills$replaceGlint(ItemQuads source, int style) {
        List<BakedQuad> all = simpleskills$replaceList(source.all(), style);
        List<BakedQuad> solid = simpleskills$replaceList(source.solid(), style);
        List<BakedQuad> translucent = simpleskills$replaceList(source.translucent(), style);
        return new ItemQuads(all, solid, translucent);
    }

    private static List<BakedQuad> simpleskills$replaceList(List<BakedQuad> source, int style) {
        if (source.isEmpty()) return source;
        List<BakedQuad> result = new ArrayList<>(source.size());
        for (BakedQuad quad : source) {
            BakedQuad.MaterialInfo info = quad.materialInfo();
            boolean translucent = info.itemRenderType().pipeline() == RenderPipelines.ITEM_TRANSLUCENT;
            // Each item quad carries the atlas its base texture is sampled from.
            // Use that same atlas for Sampler0 in the glint pass; hard-coding the
            // blocks atlas makes item-atlas UVs sample unrelated regions as a large square.
            var itemAtlas = info.sprite().atlasLocation();
            var standard = RuneVisualStylesClient.standard(style, translucent, itemAtlas);
            var special = RuneVisualStylesClient.special(style, translucent, itemAtlas);
            BakedQuad.MaterialInfo updated = new BakedQuad.MaterialInfo(
                    info.sprite(), info.layer(), info.itemRenderType(), standard, special,
                    info.tintIndex(), info.shadeDirectionOverride(), info.lightEmission());
            result.add(new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                    quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                    quad.direction(), updated));
        }
        return result;
    }
}
