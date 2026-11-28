package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.CrimsonBlightRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Stores the effect amplifier in the immutable render snapshot. */
@Mixin(LivingEntityRenderState.class)
public abstract class CrimsonBlightRenderStateMixin implements CrimsonBlightRenderState {
    @Unique
    private int simpleskills$crimsonBlightLevel = -1;

    @Override
    public int simpleskills$getCrimsonBlightLevel() {
        return this.simpleskills$crimsonBlightLevel;
    }

    @Override
    public void simpleskills$setCrimsonBlightLevel(int level) {
        this.simpleskills$crimsonBlightLevel = level;
    }
}
