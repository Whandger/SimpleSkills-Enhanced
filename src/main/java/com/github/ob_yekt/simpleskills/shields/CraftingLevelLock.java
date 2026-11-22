package com.github.ob_yekt.simpleskills.shields;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Trava de receita: o escudo de glowstone só pode ser retirado da mesa de crafting
 * a partir do nível {@link #GLOWSTONE_SHIELD_LEVEL} de Crafting.
 */
public final class CraftingLevelLock {
    public static final int GLOWSTONE_SHIELD_LEVEL = 15;

    private CraftingLevelLock() {}

    /** True quando o resultado é o escudo de glowstone e o jogador ainda não tem o nível necessário. */
    public static boolean isLocked(ServerPlayer player, ItemStack result) {
        if (result.isEmpty() || !result.is(GlowstoneShields.GLOWSTONE_SHIELD)) return false;
        int level = XPManager.getSkillLevel(player.getStringUUID(), Skills.CRAFTING);
        return level < GLOWSTONE_SHIELD_LEVEL;
    }

    public static void deny(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(String.format(
                "§6[simpleskills]§f You need Crafting level %d to craft the Glowstone Shield!",
                GLOWSTONE_SHIELD_LEVEL)), true);
    }

    /** Envia no chat a receita de crafting quando o jogador atinge o nível 15 de Crafting. */
    public static void notifyUnlocked(ServerPlayer player, int previousLevel, int newLevel) {
        if (player == null || previousLevel >= GLOWSTONE_SHIELD_LEVEL || newLevel < GLOWSTONE_SHIELD_LEVEL) {
            return;
        }
        player.sendSystemMessage(
                Component.translatable("message.simpleskills.glowstone_shield.unlocked")
                        .withStyle(ChatFormatting.GOLD),
                false
        );
    }
}
