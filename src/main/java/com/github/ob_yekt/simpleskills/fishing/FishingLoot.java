package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewards of a finished fight.
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

    /** Throws the rewards towards the player like vanilla and triggers the vanilla fishing stats. */
    public static void deliver(ServerLevel level, ServerPlayer player, FishingHook hook, ItemStack rod,
                               ItemStack main, List<ItemStack> bonus) {
        List<ItemStack> all = new ArrayList<>();
        all.add(main);
        all.addAll(bonus);

        CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, rod, hook, all);

        for (ItemStack stack : all) {
            ItemEntity entity = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), stack);
            double dx = player.getX() - hook.getX();
            double dy = player.getY() - hook.getY();
            double dz = player.getZ() - hook.getZ();
            entity.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
            level.addFreshEntity(entity);
            if (stack.is(ItemTags.FISHES)) {
                player.awardStat(Stats.FISH_CAUGHT, 1);
            }
        }
        level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ() + 0.5,
                level.getRandom().nextInt(6) + 1));
    }
}
