package com.github.ob_yekt.simpleskills.abyss;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Adds or strengthens Crimson Blight after a direct hit from Crimson Crystal weapons. */
public final class CrimsonCrystalCombat {
    private static boolean registered;

    private CrimsonCrystalCombat() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CrimsonCrystalCombat::afterDamage);
    }

    private static void afterDamage(LivingEntity victim, DamageSource source,
                                    float originalAmount, float actualAmount, boolean blocked) {
        if (victim == null || victim.level().isClientSide() || actualAmount <= 0.0F) return;
        if (!(source.getEntity() instanceof ServerPlayer attacker)
                || source.getDirectEntity() != attacker) return;

        ItemStack weapon = attacker.getMainHandItem();
        boolean crimsonWeapon = weapon.is(CrimsonCrystalItems.CRIMSON_CRYSTAL_SWORD)
                || weapon.is(CrimsonCrystalItems.CRIMSON_CRYSTAL_AXE);
        if (!crimsonWeapon) return;

        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(CrimsonBlightEffect.ID).orElse(null);
        if (effect == null) return;

        MobEffectInstance current = victim.getEffect(effect);
        int nextLevel = current == null ? 0 : Math.min(2, current.getAmplifier() + 1);
        victim.addEffect(new MobEffectInstance(effect, 120, nextLevel, false, true), attacker);
    }
}
