package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Prevents ruined Nether portal structures from generating if their template overlaps The Abyss. */
@Mixin(RuinedPortalStructure.class)
public abstract class AbyssRuinedPortalMixin {

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void simpleskills$keepRuinedPortalsOutOfAbyss(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir
    ) {
        Optional<Structure.GenerationStub> result = cir.getReturnValue();
        if (result == null || result.isEmpty()) {
            return;
        }

        BoundingBox bounds = result.get().getPiecesBuilder().getBoundingBox();
        if (AbyssWorldgen.intersectsAbyss(bounds.minY(), bounds.maxY())) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
