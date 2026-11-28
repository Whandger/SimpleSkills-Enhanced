package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.CrimsonBlightEffect;
import com.github.ob_yekt.simpleskills.abyss.CrimsonBlightRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies server-synchronized Crimson Blight level into each entity's render state. */
@Mixin(LivingEntityRenderer.class)
public abstract class CrimsonBlightRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void simpleskills$copyCrimsonBlight(
            LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!(state instanceof CrimsonBlightRenderState blightState)) return;

        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(CrimsonBlightEffect.ID).orElse(null);
        MobEffectInstance active = effect == null ? null : entity.getEffect(effect);
        blightState.simpleskills$setCrimsonBlightLevel(active == null ? -1 : active.getAmplifier());
    }
}
