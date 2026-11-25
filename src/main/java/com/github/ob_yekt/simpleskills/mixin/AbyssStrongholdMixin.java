package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Prevents a vanilla stronghold whose generated rooms overlap The Abyss from being accepted. */
@Mixin(StrongholdStructure.class)
public abstract class AbyssStrongholdMixin {

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void simpleskills$keepStrongholdsOutOfAbyss(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir
    ) {
        Optional<Structure.GenerationStub> result = cir.getReturnValue();
        if (result == null || result.isEmpty()) {
            return;
        }

        // Strongholds build their complete room graph before returning the generation stub.
        // Inspect the final graph rather than only its start Y: corridors can descend far below it.
        BoundingBox bounds = result.get().getPiecesBuilder().getBoundingBox();
        if (AbyssWorldgen.intersectsAbyss(bounds.minY(), bounds.maxY())) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
