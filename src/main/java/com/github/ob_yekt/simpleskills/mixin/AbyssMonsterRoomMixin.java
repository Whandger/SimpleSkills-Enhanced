package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents vanilla dungeon rooms (and their zombie/skeleton/spider spawners) from generating in The Abyss. */
@Mixin(MonsterRoomFeature.class)
public abstract class AbyssMonsterRoomMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void simpleskills$stopMonsterRoomsInAbyss(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos origin,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (AbyssWorldgen.isOverworld(level)
                && AbyssWorldgen.intersectsAbyss(origin.getY() - 1, origin.getY() + 4)) {
            cir.setReturnValue(false);
        }
    }
}
