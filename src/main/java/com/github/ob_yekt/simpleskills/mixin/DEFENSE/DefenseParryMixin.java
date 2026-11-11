package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import com.github.ob_yekt.simpleskills.DefenseParryAccess;
import com.github.ob_yekt.simpleskills.DefenseCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class DefenseParryMixin implements DefenseParryAccess {
    @Unique private int simpleskills$shieldRaisedTick = -1;

    @Inject(method = "startUsingItem", at = @At("HEAD"))
    private void simpleskills$recordShieldStart(InteractionHand hand, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player && !self.isUsingItem() && self.getItemInHand(hand).is(Items.SHIELD)) {
            simpleskills$shieldRaisedTick = player.tickCount;
        }
    }

    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void simpleskills$clearShieldStart(CallbackInfo ci) {
        simpleskills$shieldRaisedTick = -1;
    }

    @Inject(method = "blockUsingItem", at = @At("HEAD"))
    private void simpleskills$perfectParry(net.minecraft.server.level.ServerLevel level,
            LivingEntity attacker, net.minecraft.world.damagesource.DamageSource source,
            float damage, boolean fullyBlocked, CallbackInfo ci) {
        LivingEntity defender = (LivingEntity) (Object) this;
        if (defender instanceof ServerPlayer player && !source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
            DefenseCombat.onBlockedPhysicalHit(player, attacker, damage, fullyBlocked);
        }
    }

    @Override public int simpleskills$getShieldRaisedTick() { return simpleskills$shieldRaisedTick; }
    @Override public void simpleskills$setShieldRaisedTick(int tick) { simpleskills$shieldRaisedTick = tick; }
}
