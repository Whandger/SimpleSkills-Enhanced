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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the authoritative, equipment-specific enchantment list used by the Rune Enchanting menu.
 * The client should render this snapshot, but must submit IDs and target levels for server revalidation.
 */
public final class RuneInfusionCatalog {
    private RuneInfusionCatalog() {}

    /** A menu row. IDs are namespaced registry IDs, never client-created enchantment objects. */
    public record Entry(String id, int currentLevel, int maximumSelectableLevel, boolean selected) {}

    /**
     * Returns only enchantments that can actually be applied to the supplied server-owned stack.
     * Existing enchantments and the current menu selection both participate in exclusivity checks.
     */
    public static List<Entry> getAvailable(ServerPlayer player, ItemStack equipment, Set<String> selectedIds) {
        if (player == null || equipment == null || equipment.isEmpty()
                || !EnchantmentHelper.canStoreEnchantments(equipment)) {
            return List.of();
        }

        int professionLevel = XPManager.getSkillLevel(player.getStringUUID(), Skills.ENCHANTING);
        int progressionMaximum = EnchantProgression.getUnlockedEnchantmentLevel(professionLevel);
        if (progressionMaximum < 1) return List.of();

        var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(equipment);
        Set<String> normalizedSelection = normalize(selectedIds);
        List<Entry> result = new ArrayList<>();

        for (Holder<Enchantment> candidate : registry.listElements().toList()) {
            if (candidate.is(EnchantmentTags.CURSE) || !candidate.value().isSupportedItem(equipment)) continue;

            String id = candidate.unwrapKey().map(key -> key.identifier().toString()).orElse(null);
            if (id == null || id.equals("minecraft:binding_curse") || id.equals("minecraft:vanishing_curse")) continue;

            boolean compatible = true;
            for (Holder<Enchantment> current : existing.keySet()) {
                if (!current.equals(candidate) && !Enchantment.areCompatible(candidate, current)) {
                    compatible = false;
                    break;
                }
            }
            if (!compatible) continue;

            for (String selectedId : normalizedSelection) {
                if (selectedId.equals(id)) continue;
                Holder<Enchantment> selected = registry.get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,
                        net.minecraft.resources.Identifier.parse(selectedId))).orElse(null);
                if (selected != null && !Enchantment.areCompatible(candidate, selected)) {
                    compatible = false;
                    break;
                }
            }
            if (!compatible) continue;

            int currentLevel = existing.getLevel(candidate);
            int cap = EnchantmentLimits.getMaxLevel(id, candidate.value().getMaxLevel());
            int maximum = Math.min(progressionMaximum, cap);
            if (maximum <= currentLevel) continue;

            result.add(new Entry(id, currentLevel, maximum, normalizedSelection.contains(id)));
        }

        result.sort(Comparator.comparing(Entry::id));
        return List.copyOf(result);
    }

    private static Set<String> normalize(Set<String> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        Set<String> result = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            String normalized = id.trim().toLowerCase(java.util.Locale.ROOT);
            result.add(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        }
        return result;
    }
}
