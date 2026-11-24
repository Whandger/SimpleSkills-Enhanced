package com.github.ob_yekt.simpleskills.grinder;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Calculates the mineral return from dismantling equipment which received a
 * SimpleSkills crafting rarity. Only the mineral ingredient is returned; sticks,
 * planks, string, templates and other recipe components are never duplicated.
 *
 * Fractional recovery is handled as a chance to return the next whole item. This
 * keeps the long-term average at the configured 30% / 50% yield without rounding
 * every small recipe up to a guaranteed ingot.
 */
public final class OreGrinderSalvage {
    private OreGrinderSalvage() {}

    private record SalvageData(Item mineral, int ingredientCount, double recoveryRate) {}

    /**
     * Returns the maximum possible output stack. The block entity uses this only
     * to check whether the output slot has enough room before starting a cycle.
     */
    public static ItemStack getResult(ItemStack equipment) {
        SalvageData data = getSalvageData(equipment);
        if (data == null) return ItemStack.EMPTY;

        int maximumRecovered = (int) Math.ceil(data.ingredientCount() * data.recoveryRate());
        return maximumRecovered <= 0 ? ItemStack.EMPTY : new ItemStack(data.mineral(), maximumRecovered);
    }

    /** Rolls the actual output for one completed grinding cycle. */
    public static ItemStack rollResult(ItemStack equipment, RandomSource random) {
        SalvageData data = getSalvageData(equipment);
        if (data == null || random == null) return ItemStack.EMPTY;

        double expectedAmount = data.ingredientCount() * data.recoveryRate();
        int recovered = (int) Math.floor(expectedAmount);
        double fractionalPart = expectedAmount - recovered;
        if (fractionalPart > 0.0D && random.nextFloat() < fractionalPart) {
            recovered++;
        }

        return recovered <= 0 ? ItemStack.EMPTY : new ItemStack(data.mineral(), recovered);
    }

    public static boolean canRecover(ItemStack equipment) {
        return !getResult(equipment).isEmpty();
    }

    private static SalvageData getSalvageData(ItemStack equipment) {
        if (!CraftingEquipmentManager.isEligibleForCelestialAltar(equipment)) return null;

        Identifier id = BuiltInRegistries.ITEM.getKey(equipment.getItem());
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        Item mineral = getMineralFor(path);
        if (mineral == null) return null;

        int ingredientCount = getMineralIngredientCount(path);
        if (ingredientCount <= 0) return null;

        // Only swords receive the special 50% return for two-material recipes.
        // Every other accepted equipment type uses the standard 30% recovery rate.
        double recoveryRate = path.endsWith("_sword") && ingredientCount == 2 ? 0.50D : 0.30D;
        return new SalvageData(mineral, ingredientCount, recoveryRate);
    }

    private static Item getMineralFor(String path) {
        // SimpleSkills' custom Glowstone Shields have explicit, mineral-only recipes.
        if (path.equals("glowstone_shield")) return Items.IRON_INGOT;
        if (path.equals("diamond_glowstone_shield") || path.equals("netherite_glowstone_shield")) return Items.DIAMOND;

        // Netherite gear is made by upgrading a diamond base item with one netherite
        // ingot. That single upgrade ingot is below the >2-material recovery threshold;
        // recover the eligible portion of the original diamond recipe instead.
        if (path.startsWith("netherite_")) return Items.DIAMOND;
        if (path.startsWith("diamond_")) return Items.DIAMOND;
        if (path.startsWith("iron_")) return Items.IRON_INGOT;
        if (path.startsWith("golden_") || path.startsWith("gold_")) return Items.GOLD_INGOT;
        if (path.startsWith("copper_")) return Items.COPPER_INGOT;
        return null;
    }

    /** Counts only the mineral ingredient in the corresponding vanilla-style recipe. */
    private static int getMineralIngredientCount(String path) {
        if (path.equals("glowstone_shield") || path.equals("diamond_glowstone_shield")
                || path.equals("netherite_glowstone_shield")) return 4;
        if (path.endsWith("_sword")) return 2;
        if (path.endsWith("_pickaxe") || path.endsWith("_axe")) return 3;
        if (path.endsWith("_shovel")) return 1;
        if (path.endsWith("_hoe")) return 2;
        if (path.endsWith("_helmet")) return 5;
        if (path.endsWith("_chestplate")) return 8;
        if (path.endsWith("_leggings")) return 7;
        if (path.endsWith("_boots")) return 4;
        return 0;
    }
}
