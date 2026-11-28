package com.github.ob_yekt.simpleskills.mixin;

import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Expands the vanilla reference scan to match the outer districts of the Abyssal city. */
@Mixin(ChunkGenerator.class)
public abstract class AbyssStructureReferenceChunkGeneratorMixin {

    /**
     * Minecraft 26.3 createReferences has five literal 8 values: the range local and
     * the lower/upper bounds of its X and Z source-chunk loops.
     */
    @ModifyConstant(
            method = "createReferences",
            constant = @Constant(intValue = 8),
            require = 5
    )
    private int simpleskills$extendStructureReferenceScan(int original) {
        return MixinPlugin.getAbyssStructureReferenceRadius();
    }
}
