package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;

/** Server-authoritative right-click collection for the King's planted sword and fallen crown. */
public final class AbyssalKingRelicPickup {
    private static boolean registered;

    private AbyssalKingRelicPickup() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        UseEntityCallback.EVENT.register((Player player, net.minecraft.world.level.Level level,
                                          InteractionHand hand, net.minecraft.world.entity.Entity entity,
                                          net.minecraft.world.phys.EntityHitResult hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || !(entity instanceof ItemEntity relic)
                    || !AbyssalKingItems.isKingRelic(relic.getItem())) {
                return InteractionResult.PASS;
            }

            // This callback runs before vanilla's spectator check; enforce it ourselves.
            if (player.isSpectator()) return InteractionResult.FAIL;

            // SUCCESS on the client sends the interaction to the server. The server owns the item transfer.
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer serverPlayer) || relic.isRemoved()
                    || relic.getItem().isEmpty()) {
                return InteractionResult.FAIL;
            }

            ItemStack prepared = relic.getItem().copy();
            if (CraftingEquipmentManager.hasPendingAbyssalLootRarity(prepared)) {
                CraftingEquipmentManager.applyAbyssalLootRarity(prepared, serverPlayer);
            }

            // Keep the world entity intact until the inventory accepts the complete (single) relic.
            ItemStack insertion = prepared.copy();
            boolean added = serverPlayer.getInventory().add(insertion);
            if (!added || !insertion.isEmpty()) {
                // Persist the finalized rarity on the world item, so a later attempt never rerolls it.
                relic.setItem(prepared);
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.simpleskills.abyssal_king_relic_inventory_full"), true);
                return InteractionResult.SUCCESS;
            }

            relic.discard();
            serverPlayer.awardStat(Stats.ITEM_PICKED_UP.get(prepared.getItem()), prepared.getCount());
            level.playSound(null, relic.getX(), relic.getY(), relic.getZ(), SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.35F,
                    (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 1.4F + 2.0F);
            serverPlayer.swing(hand, SwingAnimation.DEFAULT, true);
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.simpleskills.abyssal_king_relic_collected",
                            prepared.getHoverName()), true);
            return InteractionResult.SUCCESS;
        });
    }
}
