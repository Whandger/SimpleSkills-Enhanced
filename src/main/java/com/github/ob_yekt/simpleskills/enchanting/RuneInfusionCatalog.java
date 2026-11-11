package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the authoritative, equipment-specific enchantment list used by the Rune Enchanting menu.
 * The client should render this snapshot, but must submit IDs and target levels for server revalidation.
 *
 * Enchantments already on the item are listed too (existing = true): the menu pre-selects them,
 * they count toward the 4-enchantment limit and the difficulty, and the player may drop them.
 * Conflicts are resolved pairwise by the client/server over the final selection, so an existing
 * enchantment that blocks another one can simply be removed.
 */
public final class RuneInfusionCatalog {
    private RuneInfusionCatalog() {}

    /**
     * A menu row. IDs are namespaced registry IDs, never client-created enchantment objects.
     * configuredCap is the enchantment's own cap (used for the shared difficulty scale);
     * maximumSelectableLevel already includes the player's progression.
     */
    public record Entry(String id, int currentLevel, int maximumSelectableLevel, int configuredCap,
                        boolean existing) {}

    public static List<Entry> getAvailable(ServerPlayer player, ItemStack equipment) {
        if (player == null || equipment == null || equipment.isEmpty()
                || !EnchantmentHelper.canStoreEnchantments(equipment)) {
            return List.of();
        }

        int professionLevel = XPManager.getSkillLevel(player.getStringUUID(), Skills.ENCHANTING);
        int progressionMaximum = EnchantProgression.getUnlockedEnchantmentLevel(professionLevel);
        if (progressionMaximum < 1) return List.of();

        var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(equipment);
        List<Entry> result = new ArrayList<>();

        for (Holder<Enchantment> candidate : registry.listElements().toList()) {
            if (candidate.is(EnchantmentTags.CURSE) || !candidate.value().isSupportedItem(equipment)) continue;

            String id = candidate.unwrapKey().map(key -> key.identifier().toString()).orElse(null);
            if (id == null || id.equals("minecraft:binding_curse") || id.equals("minecraft:vanishing_curse")) continue;

            int currentLevel = existing.getLevel(candidate);
            int cap = EnchantmentLimits.getMaxLevel(id, candidate.value().getMaxLevel());
            // An existing level above the player's ceiling stays selectable as "keep".
            int maximum = Math.max(currentLevel, Math.min(progressionMaximum, cap));
            if (currentLevel == 0 && maximum <= currentLevel) continue;

            result.add(new Entry(id, currentLevel, maximum, cap, currentLevel > 0));
        }

        result.sort(Comparator.comparing(Entry::id));
        return List.copyOf(result);
    }
}
