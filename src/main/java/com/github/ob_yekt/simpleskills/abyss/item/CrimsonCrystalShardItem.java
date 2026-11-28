package com.github.ob_yekt.simpleskills.abyss.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Stackable crimson crystal fuel; its cooking-fuel component matches a lava bucket. */
public final class CrimsonCrystalShardItem extends Item {
    public CrimsonCrystalShardItem(Properties properties) {
        super(properties.stacksTo(64).fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("item.simpleskills.red_crystal_shard.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
