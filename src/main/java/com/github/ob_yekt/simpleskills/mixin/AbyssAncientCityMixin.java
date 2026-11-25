package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Keeps Ancient City jigsaw structures (which contain sculk and shriekers) out of The Abyss. */
@Mixin(JigsawStructure.class)
public abstract class AbyssAncientCityMixin {
    @Shadow @Final private Holder<StructureTemplatePool> startPool;

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void simpleskills$keepAncientCitiesOutOfAbyss(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir
    ) {
        String poolPath = this.startPool.unwrapKey()
                .map(ResourceKey::identifier)
                .map(identifier -> identifier.getPath())
                .orElse("");
        if (!poolPath.startsWith("ancient_city/")) {
            return;
        }

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
