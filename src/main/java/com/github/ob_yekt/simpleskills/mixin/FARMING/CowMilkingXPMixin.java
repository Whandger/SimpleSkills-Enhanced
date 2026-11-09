package com.github.ob_yekt.simpleskills.mixin.FARMING;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Farming XP for milking cows (also covers mooshrooms, which delegate bucket use to AbstractCow).
 *
 * Injected at the exact moment the empty bucket is turned into a milk bucket, so it fires once per
 * successful milking and never for calves, bowls, breeding or any failed interaction.
 * The XP value follows the existing pattern: the "milk_cow" key of farming_xp.json "actions", and if the
 * key is absent the skill's base XP (the same fallback shearing/breeding use). No value is invented.
 */
@Mixin(AbstractCow.class)
public abstract class CowMilkingXPMixin {

    @Inject(
            method = "mobInteract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private void simpleskills$addXPOnMilk(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.level().isClientSide()) {
            int xp = ConfigManager.getFarmingActionXP("milk_cow", Skills.FARMING);
            XPManager.addXPWithNotification(serverPlayer, Skills.FARMING, xp);
        }
    }
}
