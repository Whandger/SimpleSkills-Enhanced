package com.github.ob_yekt.simpleskills.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemStackLayerRenderStateAccessor {
    @Accessor("quads")
    ItemQuads simpleskills$getQuads();

    @Accessor("quads")
    void simpleskills$setQuads(ItemQuads quads);

    @Accessor("foilType")
    net.minecraft.client.renderer.item.ItemStackRenderState.FoilType simpleskills$getFoilType();

    @Accessor("foilType")
    void simpleskills$setFoilType(net.minecraft.client.renderer.item.ItemStackRenderState.FoilType foilType);
}
