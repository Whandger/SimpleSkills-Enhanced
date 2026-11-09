package com.github.ob_yekt.simpleskills.farming;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Single source of truth for the Farming perks that do not already live elsewhere.
 *
 * <ul>
 *   <li><b>Harvest Bonus</b> (crop extra drops) is NOT defined here: it already exists in
 *       {@code EventHandlers#getHarvestBonusChance} and is only displayed by the tab menu.</li>
 *   <li><b>Animal Loot Bonus</b>: one chance formula ({@link #animalLootBonusChance(int)}) shared by
 *       animal death drops and sheep shearing wool.</li>
 *   <li><b>Bone Meal Mastery</b> and <b>Auto Plant</b>: unlock level and hunger cost.</li>
 * </ul>
 */
public final class FarmingPerks {
    private FarmingPerks() {}

    /** Farming level at which Bone Meal Mastery and Auto Plant unlock. */
    public static final int UNLOCK_LEVEL = 5;

    /** Highest level that still increases the percentage bonuses (same cap as Harvest Bonus). */
    public static final int MAX_BONUS_LEVEL = 99;

    // ------------------------------------------------------------------ levels

    public static int getFarmingLevel(ServerPlayer player) {
        return XPManager.getSkillLevel(player.getStringUUID(), Skills.FARMING);
    }

    // ------------------------------------------------------------ Animal Loot Bonus

    /**
     * +1% per Farming level, capped at 99%. This is the ONLY place the Animal Loot Bonus
     * percentage is computed; killed-animal drops, sheep wool and the tab menu all call it.
     */
    public static double animalLootBonusChance(int farmingLevel) {
        return Math.min(Math.max(farmingLevel, 0), MAX_BONUS_LEVEL) / 100.0;
    }

    /** Rolls the Animal Loot Bonus once for the given player. */
    public static boolean rollAnimalLootBonus(ServerPlayer player) {
        double chance = animalLootBonusChance(getFarmingLevel(player));
        return chance > 0.0 && player.getRandom().nextDouble() < chance;
    }

    // ------------------------------------------------------------ Bone Meal Mastery

    public static boolean isBoneMealMasteryUnlocked(int farmingLevel) {
        return farmingLevel >= UNLOCK_LEVEL;
    }

    /**
     * Hunger cost, in hunger POINTS (2 points = 1 drumstick/"bar"), of one Bone Meal Mastery use.
     *
     * @return -1 while locked (level 1-4), 4 (5-9), 3 (10-14), 2 (15-19), 0 (20+, free)
     */
    public static int boneMealHungerCost(int farmingLevel) {
        if (farmingLevel < UNLOCK_LEVEL) return -1;
        if (farmingLevel < 10) return 4;
        if (farmingLevel < 15) return 3;
        if (farmingLevel < 20) return 2;
        return 0;
    }

    /** Human readable cost in hunger bars for the tab menu: "2", "1.5", "1" or "FREE". */
    public static String boneMealCostLabel(int farmingLevel) {
        int points = boneMealHungerCost(farmingLevel);
        if (points < 0) return "LOCKED";
        if (points == 0) return "FREE";
        return (points % 2 == 0) ? Integer.toString(points / 2) : String.format(java.util.Locale.US, "%.1f", points / 2.0);
    }

    // ------------------------------------------------------------------ Auto Plant

    public static boolean isAutoPlantUnlocked(int farmingLevel) {
        return farmingLevel >= UNLOCK_LEVEL;
    }

    // --------------------------------------------------------------- notifications

    /**
     * Sends the two instructive unlock messages (Bone Meal Mastery, then Auto Plant) when a Farming
     * level-up crosses {@link #UNLOCK_LEVEL}. Detection is purely "previous < 5 <= new", so it fires
     * exactly once per crossing, never on login, and only to the player who levelled up.
     * Harvest Bonus and Animal Loot Bonus intentionally have no notification.
     */
    public static void notifyUnlocked(ServerPlayer player, int previousLevel, int newLevel) {
        if (player == null || previousLevel >= UNLOCK_LEVEL || newLevel < UNLOCK_LEVEL) {
            return;
        }

        player.sendSystemMessage(
                Component.translatable("message.simpleskills.bone_meal_mastery.unlocked")
                        .withStyle(ChatFormatting.GOLD),
                false
        );
        player.sendSystemMessage(
                Component.translatable("message.simpleskills.auto_plant.unlocked")
                        .withStyle(ChatFormatting.GOLD),
                false
        );
    }
}
