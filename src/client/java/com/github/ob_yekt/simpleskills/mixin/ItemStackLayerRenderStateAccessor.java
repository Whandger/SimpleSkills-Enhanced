package com.github.ob_yekt.simpleskills.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import com.mojang.blaze3d.vertex.PoseStack;

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

    @Accessor("this$0")
    ItemStackRenderState simpleskills$getParentState();

    @Invoker("applyTransform")
    void simpleskills$applyTransform(PoseStack.Pose pose);
}
