package com.github.ob_yekt.simpleskills.abyss.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Block item tooltip explaining that charred Abyssal timber can be processed by the Ore Grinder. */
public final class AbyssalCharredTimberBlockItem extends BlockItem {
    public AbyssalCharredTimberBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipAdder, flag);
        tooltipAdder.accept(Component.translatable("block.simpleskills.abyssal_charred_timber.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
