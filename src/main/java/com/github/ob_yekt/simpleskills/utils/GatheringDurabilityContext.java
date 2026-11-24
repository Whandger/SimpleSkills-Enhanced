package com.github.ob_yekt.simpleskills.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Carries the profession-specific durability chance from ItemStack.mineBlock to its
 * nested hurtAndBreak call, allowing both protection sources to be rolled exactly once.
 */
public final class GatheringDurabilityContext {
    private record Context(ItemStack stack, Player player, double professionChance) {}

    private static final ThreadLocal<Deque<Context>> ACTIVE =
            ThreadLocal.withInitial(ArrayDeque::new);

    private GatheringDurabilityContext() {}

    public static void begin(ItemStack stack, ServerPlayer player, double professionChance) {
        if (stack == null || player == null) return;
        ACTIVE.get().push(new Context(stack, player, Math.max(0.0, professionChance)));
    }

    public static double getProfessionChance(ItemStack stack, ServerPlayer player) {
        if (stack == null || player == null) return 0.0;
        Deque<Context> contexts = ACTIVE.get();
        Context context = contexts.peek();
        if (context == null) {
            ACTIVE.remove();
            return 0.0;
        }
        return context.stack() == stack && context.player() == player
                ? context.professionChance() : 0.0;
    }

    public static void end(ItemStack stack, Player player) {
        if (stack == null || player == null) return;
        Deque<Context> contexts = ACTIVE.get();
        Context context = contexts.peek();
        if (context != null && context.stack() == stack && context.player() == player) {
            contexts.pop();
        }
        if (contexts.isEmpty()) ACTIVE.remove();
    }
}
