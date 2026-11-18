package com.github.ob_yekt.simpleskills.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] simpleskills$getLayers();

    @Accessor("activeLayerCount")
    int simpleskills$getActiveLayerCount();

    @Accessor("displayContext")
    ItemDisplayContext simpleskills$getDisplayContext();
}
