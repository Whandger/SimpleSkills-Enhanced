package com.github.ob_yekt.simpleskills.enchanting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-side rules shared by the Rune Enchanting preparation menu and the infusion manager.
 * This class deliberately contains no client-provided costs or outcomes: the server derives the
 * Rune cost and validates every requested level again when the player starts an infusion.
 */
public final class RuneInfusionRules {
    public static final int MAX_SELECTED_ENCHANTMENTS = 4;
    public static final int RUNES_PER_ENCHANTMENT = 1;

    private static final Set<String> NEVER_INFUSABLE = Set.of(
            "minecraft:binding_curse",
            "minecraft:vanishing_curse"
    );

    private RuneInfusionRules() {}

    /** One requested final level for an enchantment, identified by its namespaced registry id. */
    public record Selection(String enchantmentId, int targetLevel, int vanillaMaxLevel) {
        public Selection {
            if (enchantmentId == null) throw new IllegalArgumentException("enchantmentId cannot be null");
            enchantmentId = normalize(enchantmentId);
        }
    }

    public enum Rejection {
        NONE,
        EMPTY_SELECTION,
        TOO_MANY_ENCHANTMENTS,
        DUPLICATE_ENCHANTMENT,
        UNKNOWN_ENCHANTMENT,
        INCOMPATIBLE_ENCHANTMENT,
        CURSE_NOT_ALLOWED,
        INVALID_LEVEL,
        LEVEL_LOCKED,
        ENCHANTMENT_CAP_REACHED,
        NOT_AN_UPGRADE
    }

    public record Validation(Rejection rejection, String enchantmentId, int allowedMaximum) {
        public boolean accepted() { return rejection == Rejection.NONE; }

        public static Validation success() {
            return new Validation(Rejection.NONE, "", 0);
        }
    }

    /**
     * Validates a complete selection. `compatibleIds` must be built by the server from the actual
     * target stack and registry; existingLevels must also come from that server-owned stack.
     * An empty compatibleIds set means no enchantment is compatible, not "skip compatibility".
     */
    public static Validation validate(
            int professionLevel,
            List<Selection> selections,
            Set<String> compatibleIds,
            Map<String, Integer> existingLevels) {
        if (selections == null || selections.isEmpty()) {
            return new Validation(Rejection.EMPTY_SELECTION, "", 0);
        }
        if (selections.size() > MAX_SELECTED_ENCHANTMENTS) {
            return new Validation(Rejection.TOO_MANY_ENCHANTMENTS, "", 0);
        }

        Set<String> seen = new HashSet<>();
        Set<String> normalizedCompatible = normalizeSet(compatibleIds);
        Map<String, Integer> current = normalizeMap(existingLevels);

        for (Selection selection : selections) {
            if (selection == null) return new Validation(Rejection.UNKNOWN_ENCHANTMENT, "", 0);
            String id = selection.enchantmentId();
            if (!seen.add(id)) return new Validation(Rejection.DUPLICATE_ENCHANTMENT, id, 0);
            if (NEVER_INFUSABLE.contains(id)) return new Validation(Rejection.CURSE_NOT_ALLOWED, id, 0);
            if (!normalizedCompatible.contains(id)) {
                return new Validation(Rejection.INCOMPATIBLE_ENCHANTMENT, id, 0);
            }
            if (selection.vanillaMaxLevel() < 1) {
                return new Validation(Rejection.UNKNOWN_ENCHANTMENT, id, 0);
            }
            if (selection.targetLevel() < 1) {
                return new Validation(Rejection.INVALID_LEVEL, id, 0);
            }

            int cap = EnchantmentLimits.getMaxLevel(id, selection.vanillaMaxLevel());
            int unlocked = EnchantProgression.getUnlockedEnchantmentLevel(professionLevel);
            int allowed = Math.min(unlocked, cap);
            if (selection.targetLevel() > unlocked) {
                return new Validation(Rejection.LEVEL_LOCKED, id, allowed);
            }
            if (selection.targetLevel() > cap) {
                return new Validation(Rejection.ENCHANTMENT_CAP_REACHED, id, allowed);
            }
            if (selection.targetLevel() > allowed) {
                return new Validation(Rejection.INVALID_LEVEL, id, allowed);
            }
            if (selection.targetLevel() <= current.getOrDefault(id, 0)) {
                return new Validation(Rejection.NOT_AN_UPGRADE, id, allowed);
            }
        }
        return Validation.success();
    }

    /** The authoritative cost is exactly one Rune per distinct selected enchantment. */
    public static int getRuneCost(List<Selection> selections) {
        if (selections == null || selections.isEmpty() || selections.size() > MAX_SELECTED_ENCHANTMENTS) {
            return 0;
        }
        Set<String> unique = new HashSet<>();
        for (Selection selection : selections) {
            if (selection == null || !unique.add(selection.enchantmentId())) return 0;
        }
        return unique.size() * RUNES_PER_ENCHANTMENT;
    }

    public static List<Selection> immutableCopy(List<Selection> selections) {
        return selections == null ? List.of() : List.copyOf(new ArrayList<>(selections));
    }

    private static Set<String> normalizeSet(Set<String> values) {
        if (values == null || values.isEmpty()) return Set.of();
        Set<String> result = new HashSet<>();
        for (String value : values) if (value != null) result.add(normalize(value));
        return result;
    }

    private static Map<String, Integer> normalizeMap(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) return Map.of();
        java.util.HashMap<String, Integer> result = new java.util.HashMap<>();
        values.forEach((id, level) -> {
            if (id != null && level != null && level > 0) result.put(normalize(id), level);
        });
        return result;
    }

    private static String normalize(String id) {
        String value = id.trim().toLowerCase(java.util.Locale.ROOT);
        return value.contains(":") ? value : "minecraft:" + value;
    }
}
