package com.github.ob_yekt.simpleskills.mixin;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Prevents vanilla mineshafts from generating when any part of their bounding box enters The Abyss. */
@Mixin(MineshaftStructure.class)
public abstract class AbyssMineshaftMixin {

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void simpleskills$keepMineshaftsAboveAbyss(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir
    ) {
        Optional<Structure.GenerationStub> result = cir.getReturnValue();
        if (result == null || result.isEmpty()) {
            return;
        }

        int lowestGeneratedY = result.get().getPiecesBuilder().getBoundingBox().minY();
        if (lowestGeneratedY < -80) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
