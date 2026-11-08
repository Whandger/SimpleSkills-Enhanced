package com.github.ob_yekt.simpleskills.mixin.FISHING;

import com.github.ob_yekt.simpleskills.fishing.TropicalFishVariantAccess;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Exposes the private packed variant of tropical fish (pattern + colours) to the fishing code. */
@Mixin(TropicalFish.class)
public abstract class TropicalFishVariantMixin implements TropicalFishVariantAccess {

    @Shadow
    protected abstract int getPackedVariant();

    @Shadow
    protected abstract void setPackedVariant(int packedVariant);

    @Override
    public int simpleskills$getVariant() {
        return this.getPackedVariant();
    }

    @Override
    public void simpleskills$setVariant(int packedVariant) {
        this.setPackedVariant(packedVariant);
    }
}
