package com.github.ob_yekt.simpleskills.grinder;

import com.github.ob_yekt.simpleskills.abyss.AbyssContent;
import com.github.ob_yekt.simpleskills.abyss.CrimsonCrystalItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Calculates the mineral return from dismantling durable equipment made with
 * recoverable mining materials. Equipment does not need a SimpleSkills rarity or rank.
 * Only the mineral ingredient is returned; sticks, planks, string, templates and
 * other recipe components are never duplicated.
 *
 * Recovery is fixed at 50% of the equipment's known mineral ingredient cost.
 * Fractional amounts are handled as a chance to return the next whole item, keeping
 * the long-term average at exactly the configured recovery rate.
 */
public final class OreGrinderSalvage {
    private static final double RECOVERY_RATE = 0.50D;

    private OreGrinderSalvage() {}

    private record SalvageData(Item mineral, int ingredientCount, double recoveryRate) {}

    /**
     * Returns the maximum possible output stack. The block entity uses this only
     * to check whether the output slot has enough room before starting a cycle.
     */
    public static ItemStack getResult(ItemStack equipment) {
        if (isAbyssalTimber(equipment)) {
            return new ItemStack(CrimsonCrystalItems.ABYSSAL_WOOD_STICK, 1);
        }
        SalvageData data = getSalvageData(equipment);
        if (data == null) return ItemStack.EMPTY;

        int maximumRecovered = (int) Math.ceil(data.ingredientCount() * data.recoveryRate());
        return maximumRecovered <= 0 ? ItemStack.EMPTY : new ItemStack(data.mineral(), maximumRecovered);
    }

    /** Rolls the actual output for one completed grinding cycle. */
    public static ItemStack rollResult(ItemStack equipment, RandomSource random) {
        if (isAbyssalTimber(equipment)) {
            return new ItemStack(CrimsonCrystalItems.ABYSSAL_WOOD_STICK, 1);
        }
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


    private static boolean isAbyssalTimber(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.is(AbyssContent.ABYSSAL_CHARRED_TIMBER_ITEM);
    }

    private static SalvageData getSalvageData(ItemStack equipment) {
        // The Ore Grinder accepts ordinary equipment too; a crafting rarity/rank is
        // intentionally not required. Durability prevents unrelated material items
        // from being treated as equipment.
        if (equipment == null || equipment.isEmpty()
                || equipment.get(DataComponents.MAX_DAMAGE) == null) {
            return null;
        }

        Identifier id = BuiltInRegistries.ITEM.getKey(equipment.getItem());
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        Item mineral = getMineralFor(path);
        if (mineral == null) return null;

        int ingredientCount = getMineralIngredientCount(path);
        if (ingredientCount <= 0) return null;

        return new SalvageData(mineral, ingredientCount, RECOVERY_RATE);
    }

    private static Item getMineralFor(String path) {
        // SimpleSkills' custom Glowstone Shields have explicit, mineral-only recipes.
        if (path.equals("glowstone_shield")) return Items.IRON_INGOT;
        if (path.equals("diamond_glowstone_shield") || path.equals("netherite_glowstone_shield")) {
            return Items.DIAMOND;
        }

        // Vanilla shield and crossbow recipes each consume one iron ingot.
        if (path.equals("shield") || path.equals("crossbow")) return Items.IRON_INGOT;

        // Netherite gear is made by upgrading a diamond base item with one netherite
        // ingot. The grinder recovers the original equipment's primary mineral cost.
        if (path.startsWith("netherite_")) return Items.DIAMOND;
        if (path.startsWith("diamond_")) return Items.DIAMOND;
        if (path.startsWith("iron_")) return Items.IRON_INGOT;
        if (path.startsWith("golden_") || path.startsWith("gold_")) return Items.GOLD_INGOT;
        if (path.startsWith("copper_")) return Items.COPPER_INGOT;
        return null;
    }

    /** Counts only the primary mineral ingredient in the corresponding equipment recipe. */
    private static int getMineralIngredientCount(String path) {
        if (path.equals("glowstone_shield") || path.equals("diamond_glowstone_shield")
                || path.equals("netherite_glowstone_shield")) return 4;
        if (path.equals("shield") || path.equals("crossbow")) return 1;
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
