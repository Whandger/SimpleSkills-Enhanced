package com.github.ob_yekt.simpleskills.abyss.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Crimson Runestone: the guaranteed trophy of the Crimson Colossus.
 * Acts as a focus for the currently selected Prayer ability while held.
 */
public final class CrimsonRuneItem extends Item {
    public CrimsonRuneItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltipAdder,
                                TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("item.simpleskills.crimson_rune.desc")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        tooltipAdder.accept(Component.translatable("item.simpleskills.crimson_rune.use")
                .withStyle(ChatFormatting.GRAY));
    }

    public static boolean isHeldBy(Player player) {
        return player.getMainHandItem().getItem() instanceof CrimsonRuneItem
                || player.getOffhandItem().getItem() instanceof CrimsonRuneItem;
    }
}
