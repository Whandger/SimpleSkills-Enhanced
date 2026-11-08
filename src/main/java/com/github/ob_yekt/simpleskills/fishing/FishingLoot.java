package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewards of a finished fight, delivered straight into the inventory.
 *
 * <p><b>Main reward:</b> the species of the real fish that bit (a salmon is always delivered as a
 * salmon). <b>Extra reward:</b> the existing rank loot table (Novice ... Grandmaster, with its Fish /
 * Junk / Treasure categories) is rolled once; the Fish category is already covered by the main
 * reward, so only Junk and Treasure results are handed out as a bonus. That keeps the original
 * junk/treasure odds and luck handling while never replacing the identity of the caught fish.</p>
 */
public final class FishingLoot {
    private FishingLoot() {}

    /** Junk / Treasure bonus for this catch (may be empty). */
    public static List<ItemStack> rollBonus(ServerLevel level, ServerPlayer player, FishingHook hook,
                                            ItemStack rod, int fishingLevel, int hookLuck) {
        List<ItemStack> bonus = new ArrayList<>();
        try {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, hook.position())
                    .withParameter(LootContextParams.TOOL, rod)
                    .withParameter(LootContextParams.THIS_ENTITY, hook)
                    .withLuck(hookLuck + player.getLuck())
                    .create(LootContextParamSets.FISHING);

            LootTable table = resolveTable(level, fishingLevel);
            for (ItemStack stack : table.getRandomItems(params)) {
                if (!stack.is(ItemTags.FISHES)) {
                    bonus.add(stack);
                }
            }
        } catch (Exception e) {
            // A broken data pack must never take the catch away from the player.
            Simpleskills.LOGGER.warn("Could not roll the bonus fishing loot: {}", e.getMessage());
        }
        return bonus;
    }

    private static LootTable resolveTable(ServerLevel level, int fishingLevel) {
        var registries = level.getServer().reloadableRegistries();
        if (ConfigManager.isCustomFishingLootEnabled()) {
            Identifier id = ConfigManager.getFishingLootTable(fishingLevel);
            if (id != null) {
                LootTable custom = registries.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
                if (custom != LootTable.EMPTY) {
                    return custom;
                }
                Simpleskills.LOGGER.warn("Fishing loot table {} is empty or invalid, using vanilla.", id);
            }
        }
        return registries.getLootTable(BuiltInLootTables.FISHING);
    }

    /**
     * Hands the rewards straight to the player: into the inventory, and whatever does not fit is dropped
     * at the player's feet. Nothing is ever left floating next to the water (the hook can be far away,
     * on the other side of a river), so the catch can not be "lost at the shore".
     *
     * <p>Fishing statistics and the vanilla fishing advancement trigger are still awarded, exactly once.</p>
     */
    public static void deliver(ServerLevel level, ServerPlayer player, FishingHook hook, ItemStack rod,
                               ItemStack main, List<ItemStack> bonus, int experience) {
        List<ItemStack> all = new ArrayList<>();
        all.add(main);
        all.addAll(bonus);

        // Like vanilla: the trigger sees the full list of rewards before they change hands.
        CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, rod, hook, new ArrayList<>(all));

        for (ItemStack stack : all) {
            if (stack.isEmpty()) continue;
            boolean isFish = stack.is(ItemTags.FISHES);
            Item item = stack.getItem();
            int count = stack.getCount();

            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                // Inventory full: the rest lands at the player's feet (no arm swing, short pickup delay).
                ItemEntity overflow = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack);
                overflow.setDefaultPickUpDelay();
                level.addFreshEntity(overflow);
            }
            player.awardStat(Stats.ITEM_PICKED_UP.get(item), count);
            if (isFish) {
                player.awardStat(Stats.FISH_CAUGHT, 1);
            }
        }
        if (experience > 0) {
            player.giveExperiencePoints(experience);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 0.2F, (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 1.4F + 2.0F);
    }
}
