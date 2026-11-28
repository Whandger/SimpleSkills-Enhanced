package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blocks natural spawns from vanilla/other mods in The Abyss while reserving the SimpleSkills namespace for future Abyss mobs. */
@Mixin(NaturalSpawner.class)
public abstract class AbyssNaturalMobSpawnMixin {

    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void simpleskills$restrictNaturalSpawnsInAbyss(
            ServerLevel level,
            MobCategory mobCategory,
            StructureManager structureManager,
            ChunkGenerator generator,
            MobSpawnSettings.SpawnerData currentSpawnData,
            BlockPos.MutableBlockPos pos,
            double nearestPlayerDistanceSqr,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Level.OVERWORLD.equals(level.dimension()) || !AbyssWorldgen.isAbyssY(pos.getY())) {
            return;
        }

        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(currentSpawnData.type()).getNamespace();
        if (!Simpleskills.MOD_ID.equals(namespace)) {
            cir.setReturnValue(false);
        }
    }
}
