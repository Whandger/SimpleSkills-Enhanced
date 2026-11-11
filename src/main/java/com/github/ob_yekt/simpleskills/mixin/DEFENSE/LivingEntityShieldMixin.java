package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityShieldMixin {

    @Shadow
    public abstract ItemStack getUseItem();

    @Shadow
    public abstract ItemStack getItemBlockingWith();

    @Unique
    private static final String SHIELD_ID = "minecraft:shield";
    @Unique
    private int lastShieldMessageTick = 0;

    /** Skip the vanilla 0.25 second shield raise delay for players allowed to use shields. */
    @Inject(method = "getItemBlockingWith", at = @At("HEAD"), cancellable = true)
    private void simpleskills$raiseShieldImmediately(CallbackInfoReturnable<ItemStack> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player) || !entity.isUsingItem()) return;

        ItemStack activeStack = this.getUseItem();
        if (!activeStack.is(Items.SHIELD)) return;

        if (entity instanceof ServerPlayer serverPlayer && !simpleskills$meetsShieldRequirement(serverPlayer)) {
            cir.setReturnValue(null);
            return;
        }
        cir.setReturnValue(activeStack);
    }

    /** Widen the shield's horizontal block arc from vanilla 90 degrees to 120 degrees. */
    @ModifyArg(
            method = "applyItemBlocking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F"),
            index = 2)
    private double simpleskills$widenShieldCoverage(double angle) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack blockingStack = this.getItemBlockingWith();
        if (entity instanceof Player && blockingStack != null && blockingStack.is(Items.SHIELD)) {
            return angle * 0.75; // 120° incoming arc against the component's 90° threshold
        }
        return angle;
    }

    @Unique
    private static boolean simpleskills$meetsShieldRequirement(ServerPlayer player) {
        SkillRequirement requirement = ConfigManager.getToolRequirement(SHIELD_ID);
        if (requirement == null) return true;
        int level = XPManager.getSkillLevel(player.getStringUUID(), requirement.getSkill());
        if (level < requirement.getLevel()) return false;
        int prestige = requirement.getRequiredPrestige();
        return prestige <= 0 || DatabaseManager.getInstance().getPrestige(player.getStringUUID()) >= prestige;
    }

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void onIsBlocking(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack activeStack = this.getUseItem();
        if (activeStack.isEmpty() || !activeStack.is(Items.SHIELD)) {
            return;
        }

        // Check shield requirement
        SkillRequirement requirement = ConfigManager.getToolRequirement(SHIELD_ID);
        if (requirement == null) {
            return;
        }

        Skills skill = requirement.getSkill();
        int playerLevel = XPManager.getSkillLevel(serverPlayer.getStringUUID(), skill);

        if (playerLevel < requirement.getLevel()) {
            // Send message only once every 60 ticks (3 seconds) to avoid spam
            int currentTick = serverPlayer.tickCount;
            if (currentTick - lastShieldMessageTick > 60) {
                serverPlayer.sendSystemMessage(Component.literal(String.format("§6[simpleskills]§f You need %s level %d to use a shield!",
                        skill.getDisplayName(), requirement.getLevel())), true);
                lastShieldMessageTick = currentTick;
            }

            // Stop blocking
            serverPlayer.releaseUsingItem();
            cir.setReturnValue(false);
            return;
        }

        int requiredPrestige = requirement.getRequiredPrestige();
        if (requiredPrestige > 0) {
            int playerPrestige = DatabaseManager.getInstance().getPrestige(serverPlayer.getStringUUID());
            if (playerPrestige < requiredPrestige) {
                int currentTick = serverPlayer.tickCount;
                if (currentTick - lastShieldMessageTick > 60) {
                    serverPlayer.sendSystemMessage(Component.literal(String.format("§6[simpleskills]§f You need Prestige ★%d to use a shield!",
                            requiredPrestige)), true);
                    lastShieldMessageTick = currentTick;
                }
                serverPlayer.releaseUsingItem();
                cir.setReturnValue(false);
            }
        }
    }
}
