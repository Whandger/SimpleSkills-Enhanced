package com.github.ob_yekt.simpleskills.crafting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.mixin.LivingEntityDamageCooldownAccessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/**
 * Server-authoritative crafting rarity and random-affix system.
 * Rarity, stats and perks are stored on the item; active effects are derived from equipment.
 */
public final class CraftingEquipmentManager {
    private static final String DATA_RARITY = "simpleskills_crafting_rarity";
    private static final String DATA_DAMAGE = "simpleskills_crafting_damage_bonus";
    private static final String DATA_DAMAGE_AS_ATTRIBUTE = "simpleskills_crafting_damage_as_attribute";
    private static final String DATA_DAMAGE_ATTRIBUTE_INCLUDES_AFFIXES =
            "simpleskills_crafting_damage_attribute_includes_affixes";
    private static final String DATA_DURABILITY = "simpleskills_crafting_durability_bonus";
    private static final String DATA_BREAK_SPEED = "simpleskills_crafting_break_speed_bonus";
    private static final String DATA_CRITICAL_CHANCE = "simpleskills_crafting_critical_chance";
    private static final String DATA_ATTACK_SPEED = "simpleskills_crafting_attack_speed_bonus";
    private static final String DATA_ARMOR = "simpleskills_crafting_armor_bonus";
    private static final String DATA_TOUGHNESS = "simpleskills_crafting_toughness_bonus";
    private static final String DATA_PERKS = "simpleskills_crafting_perks";
    private static final String DATA_BASE_NAME = "simpleskills_crafting_base_name";
    private static final String DATA_BASE_MAX_DAMAGE = "simpleskills_crafting_base_max_damage";
    private static final String DATA_LORE_LINE_COUNT = "simpleskills_crafting_lore_line_count";
    private static final String DATA_BASE_RANGE_PRESENT = "simpleskills_crafting_base_range_present";
    private static final String DATA_BASE_RANGE_MIN = "simpleskills_crafting_base_range_min";
    private static final String DATA_BASE_RANGE_MAX = "simpleskills_crafting_base_range_max";
    private static final String DATA_BASE_RANGE_MIN_CREATIVE = "simpleskills_crafting_base_range_min_creative";
    private static final String DATA_BASE_RANGE_MAX_CREATIVE = "simpleskills_crafting_base_range_max_creative";
    private static final String DATA_BASE_RANGE_HITBOX = "simpleskills_crafting_base_range_hitbox";
    private static final String DATA_BASE_RANGE_MOB_FACTOR = "simpleskills_crafting_base_range_mob_factor";
    private static final int EFFECT_DURATION_TICKS = 40;
    private static final int EFFECT_REFRESH_INTERVAL = 10;
    private static final long PROJECTILE_BONUS_LIFETIME_TICKS = 1200L;
    private static final Identifier CRAFTING_DAMAGE_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_damage_bonus");
    private static final Identifier CRAFTING_ATTACK_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_attack_speed_bonus");
    private static final Identifier CRAFTING_ARMOR_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_armor_bonus");
    private static final Identifier CRAFTING_TOUGHNESS_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_armor_toughness_bonus");
    private static final Identifier VITALITY_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_vitality");
    private static final Identifier RESILIENCE_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_resilience");

    private static final Map<UUID, ProjectileDamageData> PROJECTILE_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ETERNAL_GUARDIAN_READY_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> APPLIED_VITALITY = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> APPLIED_RESILIENCE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CELESTIAL_FLIGHT_PREVIOUS_MAYFLY = new ConcurrentHashMap<>();
    private static final Set<UUID> BONUS_DAMAGE_GUARD = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, PendingRevealBatch> PENDING_REVEALS = new ConcurrentHashMap<>();
    private static final int REVEAL_BATCH_IDLE_TICKS = 2;
    private static final int MAX_REVEALS_PER_PACKET = 24;

    private static final class PendingRevealBatch {
        private final JsonArray entries = new JsonArray();
        private long lastQueuedAt;
    }

    private record ProjectileDamageData(double multiplier, String encodedPerks, long expiresAt) {}
    private enum EquipmentKind {
        WEAPON, SWORD, ARMOR, HELMET, CHESTPLATE, LEGGINGS, BOOTS, SHIELD,
        BOW, CROSSBOW, TRIDENT, FISHING_ROD, PICKAXE, SHOVEL, HOE, AXE, MACE, TOOL, OTHER
    }
    private record Perk(String id, String effectId, int maximumEffectLevel, int extraJumps) {}

    private static Perk effect(String id, String effectId, int maxLevel) {
        return new Perk(id, effectId, maxLevel, 0);
    }
    private static Perk passive(String id, int maxLevel) { return new Perk(id, "", maxLevel, 0); }
    private static Perk jump(String id, int jumps, int maxRank) { return new Perk(id, "", maxRank, jumps); }

    private static final Perk STRENGTH = effect("strength", "minecraft:strength", 4);
    private static final Perk SPEED = effect("speed", "minecraft:speed", 3);
    private static final Perk PROTECTION = effect("protection", "minecraft:resistance", 2);
    private static final Perk RESILIENT = passive("resilient", 2);
    private static final Perk REGENERATION = effect("regeneration", "minecraft:regeneration", 3);
    private static final Perk HASTE = effect("haste", "minecraft:haste", 4);
    private static final Perk EFFICIENT = effect("efficient", "minecraft:haste", 4);
    private static final Perk JUMP_BOOST = effect("jump_boost", "minecraft:jump_boost", 3);
    private static final Perk FIRE_RESISTANCE = effect("fire_resistance", "minecraft:fire_resistance", 1);
    private static final Perk FEATHERWEIGHT = effect("featherweight", "minecraft:slow_falling", 1);
    private static final Perk VITALITY = passive("vitality", 4);
    private static final Perk DOUBLE_LEAP = jump("double_leap", 1, 4);
    private static final Perk WINDWALKER = jump("windwalker", 1, 4);
    private static final Perk VAMPIRIC = passive("vampiric", 4);
    private static final Perk CRITICAL_MASTERY = passive("critical_mastery", 4);
    private static final Perk THUNDEROUS = passive("thunderous", 4);
    private static final Perk FROSTBITE = passive("frostbite", 3);
    private static final Perk PHOENIX = passive("phoenix", 3);
    private static final Perk FORTUNE_MASTERY = passive("fortune_mastery", 4);
    private static final Perk INDESTRUCTIBLE = passive("indestructible", 4);
    private static final Perk TREASURE_HUNTER = passive("treasure_hunter", 4);
    private static final Perk ARCANE_MASTERY = passive("arcane_mastery", 1);
    private static final Perk GODSLAYER = passive("godslayer", 1);
    private static final Perk ETERNAL_GUARDIAN = passive("eternal_guardian", 1);

    // Unique-only affixes. Exactly one is guaranteed on every Unique item.
    private static final Perk RUNIC_EDGE = passive("runic_edge", 1);
    private static final Perk ANVIL_WARD = effect("anvil_ward", "minecraft:resistance", 1);
    private static final Perk GUARDIAN_OATH = effect("guardian_oath", "minecraft:resistance", 1);
    private static final Perk PROSPECTOR = passive("prospector", 3);

    // Transcendent-only affixes. At least one is guaranteed on every Transcendent item.
    private static final Perk TITAN_STRENGTH = effect("titan_strength", "minecraft:strength", 4);
    private static final Perk DIVINE_BLESSING = passive("divine_blessing", 2);
    private static final Perk CELESTIAL_MOVEMENT = jump("celestial_movement", 2, 4);
    private static final Perk AEGIS_OF_AGES = passive("aegis_of_ages", 1);
    private static final Perk WORLD_SHAPER = effect("worldshaper", "minecraft:haste", 4);
    private static final Perk CELESTIAL_WINGS = passive("celestial_wings", 1);
    private static final Perk CELESTIAL_RANGE = passive("celestial_range", 1);
    // Exclusive Transcendent abilities by equipment category.
    private static final Perk ORACLE_VISION = passive("oracle_vision", 1);
    private static final Perk AEGIS_OF_PURITY = passive("aegis_of_purity", 1);
    private static final Perk PHANTOM_STRIDE = passive("phantom_stride", 1);
    private static final Perk DIVINE_REFLECTION = passive("divine_reflection", 1);
    private static final Perk ASTRAL_PURSUIT = passive("astral_pursuit", 1);
    private static final Perk SINGULARITY_BOLT = passive("singularity_bolt", 1);
    private static final Perk POSEIDONS_WRATH = passive("poseidons_wrath", 1);
    private static final Perk WORLDBREAKER = passive("worldbreaker", 1);
    private static final Perk BURIED_TRUTH = passive("buried_truth", 1);
    private static final Perk EDENS_DOMINION = passive("edens_dominion", 1);
    private static final Perk MIDAS_TOUCH = passive("midas_touch", 1);
    private static final Perk ABYSSAL_SOVEREIGN = passive("abyssal_sovereign", 1);

    private static final Set<String> NO_LEVEL_PERKS = Set.of(
            "runic_edge", "anvil_ward",
            "guardian_oath", "arcane_mastery", "godslayer", "eternal_guardian",
            "divine_blessing", "aegis_of_ages", "fire_resistance", "featherweight",
            "celestial_wings", "celestial_range", "oracle_vision", "aegis_of_purity",
            "phantom_stride", "divine_reflection", "astral_pursuit", "singularity_bolt",
            "poseidons_wrath", "worldbreaker", "buried_truth", "edens_dominion",
            "midas_touch", "abyssal_sovereign");

    private CraftingEquipmentManager() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(CraftingRevealPayload.TYPE, CraftingRevealPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TranscendentToolTogglePayload.TYPE,
                TranscendentToolTogglePayload.CODEC);
        TranscendentVisionPayload.registerPayload();
        WorldbreakerPreviewPayload.registerPayload();
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TranscendentToolTogglePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (payload.action() == TranscendentToolTogglePayload.TOGGLE_AREA_EFFECT) {
                        TranscendentAbilities.toggleToolAreaEffect(context.player());
                    }
                }));
        ServerTickEvents.END_SERVER_TICK.register(CraftingEquipmentManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            APPLIED_VITALITY.remove(id);
            APPLIED_RESILIENCE.remove(id);
            restoreCelestialFlight(handler.player);
            TranscendentAbilities.clearPlayer(id);
            PENDING_REVEALS.remove(id);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CraftingEquipmentManager::afterDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CraftingEquipmentManager::preventLethalDamage);
        String problem = CraftingRarity.validateDistribution();
        if (problem != null) Simpleskills.LOGGER.error("Invalid Crafting rarity distribution: {}", problem);
    }

    /** Applies rarity exactly once to a new durable crafting result. */
    public static void applyCraftedEquipment(ItemStack stack, ServerPlayer player) {
        applyCraftedEquipment(stack, player, null);
    }

    /** Applies a material-scaled Transcendent chance using the actual recipe ingredients. */
    public static void applyCraftedEquipment(ItemStack stack, ServerPlayer player, ItemStack[] ingredients) {
        if (stack == null || stack.isEmpty() || player == null || stack.get(DataComponents.MAX_DAMAGE) == null
                || hasRarity(stack)) return;
        int level = getCraftingLevel(player);
        Identifier craftedItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean externalModEquipment = !craftedItemId.getNamespace().equals("minecraft")
                && !craftedItemId.getNamespace().equals(Simpleskills.MOD_ID);
        // Equipment supplied by another mod uses the same conservative 0.60% level-99
        // Transcendent rate as Iron, even if the modded item's ID contains a material name.
        CraftingRarity.MaterialTier materialTier = externalModEquipment
                ? CraftingRarity.MaterialTier.OTHER : getRecipeMaterialTier(ingredients);
        CraftingRarity rarity = CraftingRarity.roll(
                level, player.getRandom().nextDouble() * 100.0, materialTier);
        applyEquipmentRarity(stack, player, rarity, level);
    }

    /**
     * Uses the strongest material present in the recipe. Neutral ingredients such as
     * string, sticks, leather and planks map to the basic tier; unknown modded ingredients
     * are treated conservatively as Other / Modded instead of receiving the diamond rate.
     */
    private static CraftingRarity.MaterialTier getRecipeMaterialTier(ItemStack[] ingredients) {
        CraftingRarity.MaterialTier strongest = null;
        if (ingredients != null) {
            for (ItemStack ingredient : ingredients) {
                if (ingredient == null || ingredient.isEmpty()) continue;
                CraftingRarity.MaterialTier tier = getIngredientMaterialTier(ingredient);
                if (tier != null && (strongest == null || materialPriority(tier) > materialPriority(strongest))) {
                    strongest = tier;
                }
            }
        }
        return strongest == null ? CraftingRarity.MaterialTier.OTHER : strongest;
    }

    private static int materialPriority(CraftingRarity.MaterialTier tier) {
        return switch (tier) {
            case WOOD -> 1;
            case STONE -> 2;
            case COPPER -> 3;
            case GOLD -> 4;
            case IRON -> 5;
            case OTHER -> 5; // Unknown/modded materials use the Iron probability tier.
            case DIAMOND -> 6;
            case NETHERITE -> 7;
        };
    }

    /** Returns the material tier represented by a recipe ingredient. */
    private static CraftingRarity.MaterialTier getIngredientMaterialTier(ItemStack stack) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!itemId.getNamespace().equals("minecraft")
                && !itemId.getNamespace().equals(Simpleskills.MOD_ID)) {
            return CraftingRarity.MaterialTier.OTHER;
        }
        String path = itemId.getPath().toLowerCase(java.util.Locale.ROOT);

        if (path.contains("netherite") || path.contains("ancient_debris")) {
            return CraftingRarity.MaterialTier.NETHERITE;
        }
        if (path.contains("diamond")) return CraftingRarity.MaterialTier.DIAMOND;
        if (path.startsWith("iron_") || path.contains("_iron_") || path.endsWith("_iron_ingot")) {
            return CraftingRarity.MaterialTier.IRON;
        }
        if (path.startsWith("gold_") || path.startsWith("golden_")
                || path.contains("_gold_") || path.endsWith("_gold_ingot")) {
            return CraftingRarity.MaterialTier.GOLD;
        }
        if (path.startsWith("copper_") || path.contains("_copper_") || path.endsWith("_copper_ingot")) {
            return CraftingRarity.MaterialTier.COPPER;
        }

        if (path.equals("stone") || path.contains("cobblestone") || path.contains("deepslate")
                || path.contains("blackstone") || path.contains("tuff") || path.contains("andesite")
                || path.contains("diorite") || path.contains("granite") || path.equals("end_stone")) {
            return CraftingRarity.MaterialTier.STONE;
        }

        if (path.startsWith("wooden_") || path.startsWith("leather_")
                || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.LOGS)
                || stack.is(Items.STICK) || stack.is(Items.BAMBOO)
                || stack.is(Items.LEATHER) || stack.is(Items.RABBIT_HIDE)
                || stack.is(Items.STRING)) {
            return CraftingRarity.MaterialTier.WOOD;
        }

        // Modded ingredients without a recognized tier use the conservative fallback.
        return itemId.getNamespace().equals("minecraft") ? null : CraftingRarity.MaterialTier.OTHER;
    }

    /** Returns whether the item belongs to a supported category and can be ascended by the altar. */
    public static boolean isEligibleForCelestialAltar(ItemStack stack) {
        return stack != null && !stack.isEmpty() && hasRarity(stack)
                && CraftingRarity.byId(getRarityId(stack)) != null
                && stack.get(DataComponents.MAX_DAMAGE) != null
                && classify(stack) != EquipmentKind.OTHER;
    }

    public static boolean isTranscendent(ItemStack stack) {
        return hasRarity(stack) && CraftingRarity.byId(getRarityId(stack)) == CraftingRarity.TRANSCENDENT;
    }

    /** Applies a fresh Transcendent roll while retaining vanilla enchantments, custom name and damage. */
    public static boolean applyCelestialAltarReroll(ItemStack stack, ServerPlayer player) {
        if (!isEligibleForCelestialAltar(stack) || player == null) return false;
        CraftingRarity current = CraftingRarity.byId(getRarityId(stack));
        if (current == null) {
            // The altar can only ascend equipment which has already entered the SimpleSkills rarity system.
            return false;
        }
        // Both lower rarities and existing Transcendent equipment receive a fresh Transcendent roll.
        prepareEquipmentForReroll(stack, player);
        applyEquipmentRarity(stack, player, CraftingRarity.TRANSCENDENT, getCraftingLevel(player));
        return true;
    }

    /** Snapshot of one rolled attribute available for rune enhancement. Values are percentages. */
    public record StatUpgradeInfo(String id, String translationKey, double currentPercent,
                                  double minimumPercent, double maximumPercent) {
        public double progressPercent() {
            double width = maximumPercent - minimumPercent;
            return width <= 0.0 ? 100.0
                    : Math.max(0.0, Math.min(100.0, (currentPercent - minimumPercent) / width * 100.0));
        }

        public boolean isMaxed() {
            return currentPercent >= maximumPercent - 0.0001;
        }
    }

    /** A currently rolled perk, with whether it has a legal replacement in its own perk pool. */
    public record PerkRerollInfo(String id, int amplifier, boolean signatureAbility, boolean rerollable) {
        public String translationKey() { return "item.simpleskills.crafting_perk." + id; }
    }

    /** Returns every currently rolled perk and marks whether it has a legal replacement. */
    public static List<PerkRerollInfo> getEnhanceablePerks(ItemStack stack) {
        if (!isEligibleForCelestialAltar(stack)) return List.of();
        CraftingRarity rarity = CraftingRarity.byId(getRarityId(stack));
        if (rarity == null) return List.of();

        EquipmentKind kind = classify(stack);
        List<String> encodedPerks = getEncodedPerks(stack);
        List<PerkRerollInfo> result = new ArrayList<>();
        for (String encoded : encodedPerks) {
            String[] parts = encoded.split(":", 2);
            if (parts.length != 2) continue;
            String perkId = parts[0];
            Perk currentPerk = perkById(perkId);
            if (currentPerk == null) continue;

            boolean signature = rarity == CraftingRarity.TRANSCENDENT
                    && transcendentPool(kind).stream().anyMatch(perk -> perk.id().equals(perkId));
            List<Perk> pool = signature ? transcendentPool(kind)
                    : (rarity == CraftingRarity.UNIQUE ? uniquePool(kind) : commonPool(kind));
            boolean rerollable = hasAlternativePerk(pool, encodedPerks, currentPerk);
            result.add(new PerkRerollInfo(perkId, Math.max(0, parseInt(parts[1], 0)), signature, rerollable));
        }
        return List.copyOf(result);
    }

    /** Returns only perks that can be replaced without changing any of the item's other perks. */
    public static List<PerkRerollInfo> getRerollablePerks(ItemStack stack) {
        return getEnhanceablePerks(stack).stream().filter(PerkRerollInfo::rerollable).toList();
    }

    public static boolean canRerollPerk(ItemStack stack, String perkId) {
        if (perkId == null || perkId.isBlank()) return false;
        return getRerollablePerks(stack).stream().anyMatch(info -> info.id().equals(perkId));
    }

    /** Formats a selected perk exactly like its equipment tooltip, including its rolled rank. */
    public static Component getPerkDisplayComponent(PerkRerollInfo info) {
        if (info == null) return Component.empty();
        String id = info.id();
        int amplifier = info.amplifier();
        String key = info.translationKey();
        if (isJumpPerk(id)) {
            return Component.translatable(key, getAdditionalJumpsForPerk(id, amplifier + 1));
        }
        if (id.equals("vitality")) {
            return Component.translatable(key, roman(amplifier + 1), (amplifier + 1) * 4);
        }
        if (id.equals("resilient")) {
            return Component.translatable(key, roman(amplifier + 1), (amplifier + 1) * 2);
        }
        if (NO_LEVEL_PERKS.contains(id)) return Component.translatable(key);
        return Component.translatable(key, roman(amplifier + 1));
    }

    private static boolean hasAlternativePerk(List<Perk> pool, List<String> encodedPerks, Perk currentPerk) {
        Set<String> occupiedIds = new HashSet<>();
        Set<String> occupiedEffects = new HashSet<>();
        for (String encoded : encodedPerks) {
            String[] parts = encoded.split(":", 2);
            if (parts.length != 2 || parts[0].equals(currentPerk.id())) continue;
            occupiedIds.add(parts[0]);
            Perk existing = perkById(parts[0]);
            if (existing != null && !existing.effectId().isBlank()) occupiedEffects.add(existing.effectId());
        }

        for (Perk candidate : pool) {
            if (candidate == null || candidate.id().equals(currentPerk.id())
                    || occupiedIds.contains(candidate.id())) continue;
            if (!candidate.effectId().isBlank()
                    && (candidate.effectId().equals(currentPerk.effectId())
                    || occupiedEffects.contains(candidate.effectId()))) continue;
            return true;
        }
        return false;
    }

    /** Replaces exactly one selected perk with another valid perk from the same rarity/category pool. */
    public static boolean rerollEquipmentPerk(ItemStack stack, String perkId, ServerPlayer player) {
        if (!isEligibleForCelestialAltar(stack) || player == null || perkId == null || perkId.isBlank()) return false;
        CraftingRarity rarity = CraftingRarity.byId(getRarityId(stack));
        if (rarity == null) return false;

        List<String> encodedPerks = new ArrayList<>(getEncodedPerks(stack));
        int targetIndex = -1;
        for (int i = 0; i < encodedPerks.size(); i++) {
            String[] parts = encodedPerks.get(i).split(":", 2);
            if (parts.length == 2 && parts[0].equals(perkId)) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex < 0) return false;

        Perk currentPerk = perkById(perkId);
        if (currentPerk == null) return false;
        EquipmentKind kind = classify(stack);
        boolean signature = rarity == CraftingRarity.TRANSCENDENT
                && transcendentPool(kind).stream().anyMatch(perk -> perk.id().equals(perkId));
        List<Perk> pool = signature ? transcendentPool(kind)
                : (rarity == CraftingRarity.UNIQUE ? uniquePool(kind) : commonPool(kind));
        if (!hasAlternativePerk(pool, encodedPerks, currentPerk)) return false;

        Set<String> occupiedIds = new HashSet<>();
        Set<String> occupiedEffects = new HashSet<>();
        for (int i = 0; i < encodedPerks.size(); i++) {
            if (i == targetIndex) continue;
            String[] parts = encodedPerks.get(i).split(":", 2);
            if (parts.length != 2) continue;
            occupiedIds.add(parts[0]);
            Perk existing = perkById(parts[0]);
            if (existing != null && !existing.effectId().isBlank()) occupiedEffects.add(existing.effectId());
        }

        List<Perk> candidates = new ArrayList<>();
        for (Perk candidate : pool) {
            if (candidate.id().equals(perkId) || occupiedIds.contains(candidate.id())) continue;
            if (!candidate.effectId().isBlank()
                    && (candidate.effectId().equals(currentPerk.effectId())
                    || occupiedEffects.contains(candidate.effectId()))) continue;
            candidates.add(candidate);
        }
        if (candidates.isEmpty()) return false;

        Perk replacement = candidates.get(player.getRandom().nextInt(candidates.size()));
        String replacementEncoding = encodePerks(List.of(replacement), getCraftingLevel(player), rarity, player);
        if (replacementEncoding.isBlank()) return false;
        encodedPerks.set(targetIndex, replacementEncoding);
        String persistedPerks = String.join(",", encodedPerks);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putString(DATA_PERKS, persistedPerks));
        refreshCraftingAttributes(stack);
        refreshCraftingTooltip(stack);
        return true;
    }

    /**
     * Returns the attributes already rolled on an item. Attributes which were not generated
     * for the equipment category are not offered by the altar.
     */
    public static List<StatUpgradeInfo> getEnhanceableStats(ItemStack stack) {
        if (!isEligibleForCelestialAltar(stack)) return List.of();
        CraftingRarity rarity = CraftingRarity.byId(getRarityId(stack));
        if (rarity == null) return List.of();

        CompoundTag tag = getTag(stack);
        List<StatUpgradeInfo> result = new ArrayList<>();
        boolean runicEdge = getPerkAmplifier(stack, "runic_edge") >= 0;
        int criticalMastery = getPerkAmplifier(stack, "critical_mastery");
        double criticalMasteryBonus = criticalMastery >= 0 ? 5.0 + criticalMastery * 5.0 : 0.0;

        addUpgradeInfo(result, tag, "damage", "item.simpleskills.crafting_stat.damage",
                rarity.minDamageBonus(), rarity.maxDamageBonus(), runicEdge);
        addUpgradeInfo(result, tag, "durability", "item.simpleskills.crafting_stat.durability",
                rarity.minDurabilityBonus(), rarity.maxDurabilityBonus(), false);
        addUpgradeInfo(result, tag, "breakSpeed", "item.simpleskills.crafting_stat.break_speed",
                rarity.minBreakSpeedBonus(), rarity.maxBreakSpeedBonus(), false);
        addUpgradeInfo(result, tag, "critical", "item.simpleskills.crafting_stat.critical_chance",
                rarity.minCriticalChance(), rarity.maxCriticalChance(), criticalMasteryBonus);
        addUpgradeInfo(result, tag, "attackSpeed", "item.simpleskills.crafting_stat.attack_speed",
                rarity.minAttackSpeedBonus(), rarity.maxAttackSpeedBonus(), false);
        addUpgradeInfo(result, tag, "armor", "item.simpleskills.crafting_stat.armor",
                rarity.minArmorBonus(), rarity.maxArmorBonus(), false);
        addUpgradeInfo(result, tag, "toughness", "item.simpleskills.crafting_stat.armor_toughness",
                rarity.minToughnessBonus(), rarity.maxToughnessBonus(), false);
        return List.copyOf(result);
    }

    private static void addUpgradeInfo(List<StatUpgradeInfo> result, CompoundTag tag, String field,
                                       String translationKey, double minimum, double maximum,
                                       boolean runicEdge) {
        addUpgradeInfo(result, tag, field, translationKey, minimum, maximum,
                runicEdge ? 1.08 : 1.0, 0.0);
    }

    private static void addUpgradeInfo(List<StatUpgradeInfo> result, CompoundTag tag, String field,
                                       String translationKey, double minimum, double maximum,
                                       double additiveBonus) {
        addUpgradeInfo(result, tag, field, translationKey, minimum, maximum, 1.0, additiveBonus);
    }

    private static void addUpgradeInfo(List<StatUpgradeInfo> result, CompoundTag tag, String field,
                                       String translationKey, double minimum, double maximum,
                                       double multiplier, double additiveBonus) {
        double current = tag.getDoubleOr(statDataKey(field), 0.0);
        if (current <= 0.0 || maximum <= minimum) return;
        double shownCurrent = current;
        double shownMin = minimum;
        double shownMax = maximum;
        if (multiplier != 1.0) {
            shownCurrent = (1.0 + current) * multiplier - 1.0;
            shownMin = (1.0 + minimum) * multiplier - 1.0;
            shownMax = (1.0 + maximum) * multiplier - 1.0;
        }
        if (additiveBonus != 0.0) {
            shownCurrent = Math.min(0.60, shownCurrent + additiveBonus);
            shownMin = Math.min(0.60, shownMin + additiveBonus);
            shownMax = Math.min(0.60, shownMax + additiveBonus);
        }
        result.add(new StatUpgradeInfo(field, translationKey,
                shownCurrent * 100.0, shownMin * 100.0, shownMax * 100.0));
    }

    private static String statDataKey(String field) {
        return switch (field) {
            case "damage" -> DATA_DAMAGE;
            case "durability" -> DATA_DURABILITY;
            case "breakSpeed" -> DATA_BREAK_SPEED;
            case "critical" -> DATA_CRITICAL_CHANCE;
            case "attackSpeed" -> DATA_ATTACK_SPEED;
            case "armor" -> DATA_ARMOR;
            case "toughness" -> DATA_TOUGHNESS;
            default -> "";
        };
    }

    /** True when the selected stat still has room below its rarity's maximum roll. */
    public static boolean canEnhanceStat(ItemStack stack, String field) {
        double[] bounds = statBounds(stack, field);
        if (bounds == null) return false;
        double current = getTag(stack).getDoubleOr(statDataKey(field), 0.0);
        return current > 0.0 && current < bounds[1] - 0.0000001;
    }

    /**
     * Consumes one Rune to increase the selected attribute by exactly 1.00 percentage point.
     * The increase is capped at the attribute's maximum roll for the equipment rarity.
     */
    public static boolean enhanceEquipmentStat(ItemStack stack, String field) {
        if (!isEligibleForCelestialAltar(stack) || !canEnhanceStat(stack, field)) return false;
        double[] bounds = statBounds(stack, field);
        if (bounds == null) return false;

        double current = getTag(stack).getDoubleOr(statDataKey(field), 0.0);
        // Stored values are fractions (0.25 = +25%). Damage with Runic Edge is displayed
        // with an additional 1.08 multiplier, so compensate to keep the visible increase
        // at exactly +1.00 percentage point per Rune as well.
        double displayMultiplier = "damage".equals(field)
                && getPerkAmplifier(stack, "runic_edge") >= 0 ? 1.08 : 1.0;
        double oneRuneProgress = 0.01 / displayMultiplier;
        if (oneRuneProgress <= 0.0) return false;
        double upgraded = Math.min(bounds[1], current + oneRuneProgress);
        if (upgraded <= current) return false;

        String dataKey = statDataKey(field);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putDouble(dataKey, upgraded));
        refreshCraftingAttributes(stack);
        refreshCraftingTooltip(stack);
        return true;
    }

    /** Returns the raw stored range as fractions (e.g. 0.23 = +23%). */
    private static double[] statBounds(ItemStack stack, String field) {
        if (!isEligibleForCelestialAltar(stack)) return null;
        CraftingRarity rarity = CraftingRarity.byId(getRarityId(stack));
        if (rarity == null) return null;
        return switch (field) {
            case "damage" -> new double[]{rarity.minDamageBonus(), rarity.maxDamageBonus()};
            case "durability" -> new double[]{rarity.minDurabilityBonus(), rarity.maxDurabilityBonus()};
            case "breakSpeed" -> new double[]{rarity.minBreakSpeedBonus(), rarity.maxBreakSpeedBonus()};
            case "critical" -> {
                int mastery = getPerkAmplifier(stack, "critical_mastery");
                double masteryBonus = mastery >= 0 ? 0.05 + mastery * 0.05 : 0.0;
                double cappedMaximum = Math.min(rarity.maxCriticalChance(), 0.60 - masteryBonus);
                yield cappedMaximum > rarity.minCriticalChance()
                        ? new double[]{rarity.minCriticalChance(), cappedMaximum} : null;
            }
            case "attackSpeed" -> new double[]{rarity.minAttackSpeedBonus(), rarity.maxAttackSpeedBonus()};
            case "armor" -> new double[]{rarity.minArmorBonus(), rarity.maxArmorBonus()};
            case "toughness" -> new double[]{rarity.minToughnessBonus(), rarity.maxToughnessBonus()};
            default -> null;
        };
    }

    private static void refreshCraftingAttributes(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        double damage = tag.getDoubleOr(DATA_DAMAGE, 0.0);
        double speed = tag.getDoubleOr(DATA_ATTACK_SPEED, 0.0);
        double armor = tag.getDoubleOr(DATA_ARMOR, 0.0);
        double toughness = tag.getDoubleOr(DATA_TOUGHNESS, 0.0);
        double durability = tag.getDoubleOr(DATA_DURABILITY, 0.0);
        boolean hasRunicEdge = getPerkAmplifier(stack, "runic_edge") >= 0;
        double totalDamage = damage > 0.0
                ? (1.0 + damage) * (hasRunicEdge ? 1.08 : 1.0) - 1.0 : 0.0;

        removeCraftingAttributeModifiers(stack);
        boolean damageApplied = applyAttackDamageAttributeBonus(stack, totalDamage);
        boolean speedApplied = applyAttackSpeedAttributeBonus(stack, speed);
        boolean[] defenseApplied = applyDefenseAttributeBonuses(stack, classify(stack), armor, toughness);
        Integer currentMax = stack.get(DataComponents.MAX_DAMAGE);
        int baseMaximum = tag.getIntOr(DATA_BASE_MAX_DAMAGE, -1);
        if (baseMaximum < 1 && currentMax != null) {
            baseMaximum = Math.max(1, (int) Math.round(currentMax / (1.0 + Math.max(0.0, durability))));
        }
        if (baseMaximum > 0) {
            stack.set(DataComponents.MAX_DAMAGE, Math.max(1, (int) Math.round(baseMaximum * (1.0 + durability))));
            final int savedBaseMaximum = baseMaximum;
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    data -> data.putInt(DATA_BASE_MAX_DAMAGE, savedBaseMaximum));
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            data.putBoolean(DATA_DAMAGE_AS_ATTRIBUTE, damageApplied);
            data.putBoolean(DATA_DAMAGE_ATTRIBUTE_INCLUDES_AFFIXES, damageApplied);
            if (!speedApplied) data.putDouble(DATA_ATTACK_SPEED, 0.0);
            if (!defenseApplied[0]) data.putDouble(DATA_ARMOR, 0.0);
            if (!defenseApplied[1]) data.putDouble(DATA_TOUGHNESS, 0.0);
        });
    }

    private static void refreshCraftingTooltip(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        ItemLore currentLore = stack.getOrDefault(DataComponents.LORE, new ItemLore(List.of()));
        int generated = tag.getIntOr(DATA_LORE_LINE_COUNT, -1);
        if (generated < 0) {
            generated = 1;
            generated += tag.getDoubleOr(DATA_DAMAGE, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_DURABILITY, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_BREAK_SPEED, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_CRITICAL_CHANCE, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_ATTACK_SPEED, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_ARMOR, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_TOUGHNESS, 0.0) > 0.0 ? 1 : 0;
            String encoded = tag.getStringOr(DATA_PERKS, "");
            if (!encoded.isBlank()) generated += encoded.split(",").length;
        }
        int keep = Math.max(0, currentLore.lines().size() - generated);
        stack.set(DataComponents.LORE, new ItemLore(new ArrayList<>(currentLore.lines().subList(0, keep))));

        CraftingRarity rarity = CraftingRarity.byId(tag.getStringOr(DATA_RARITY, ""));
        if (rarity == null) return;
        double damage = tag.getDoubleOr(DATA_DAMAGE, 0.0);
        double totalDamage = damage > 0.0
                ? (1.0 + damage) * (getPerkAmplifier(stack, "runic_edge") >= 0 ? 1.08 : 1.0) - 1.0 : 0.0;
        appendTooltip(stack, rarity, totalDamage,
                tag.getDoubleOr(DATA_DURABILITY, 0.0),
                tag.getDoubleOr(DATA_BREAK_SPEED, 0.0),
                getEffectiveCriticalChance(stack),
                tag.getDoubleOr(DATA_ATTACK_SPEED, 0.0),
                tag.getDoubleOr(DATA_ARMOR, 0.0),
                tag.getDoubleOr(DATA_TOUGHNESS, 0.0));
    }

    private static String getUnprefixedItemName(ItemStack stack) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null && customName.getContents() instanceof TranslatableContents translated
                && translated.getKey().equals("item.simpleskills.crafted_quality_name")) {
            Object[] arguments = translated.getArgs();
            if (arguments.length > 1 && arguments[1] instanceof Component originalName) {
                return originalName.getString();
            }
        }
        return stack.getHoverName().getString();
    }

    private static void prepareEquipmentForReroll(ItemStack stack, ServerPlayer player) {
        CompoundTag tag = getTag(stack);
        Integer currentMax = stack.get(DataComponents.MAX_DAMAGE);
        int baseMaximum = tag.getIntOr(DATA_BASE_MAX_DAMAGE, -1);
        if (baseMaximum < 1) {
            double oldBonus = tag.getDoubleOr(DATA_DURABILITY, 0.0);
            baseMaximum = currentMax == null ? 1 : Math.max(1,
                    (int) Math.round(currentMax / (1.0 + Math.max(0.0, oldBonus))));
        }
        final int savedBaseMaximum = baseMaximum;
        String baseName = tag.getStringOr(DATA_BASE_NAME, "");
        if (baseName.isBlank()) baseName = getUnprefixedItemName(stack);
        final String savedBaseName = baseName;

        // Remove only the lore lines previously generated by this system; unrelated/custom lore stays intact.
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            int generated = tag.getIntOr(DATA_LORE_LINE_COUNT, -1);
            if (generated < 0) {
                generated = 1;
                generated += tag.getDoubleOr(DATA_DAMAGE, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_DURABILITY, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_BREAK_SPEED, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_CRITICAL_CHANCE, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_ATTACK_SPEED, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_ARMOR, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_TOUGHNESS, 0.0) > 0.0 ? 1 : 0;
                String oldPerks = tag.getStringOr(DATA_PERKS, "");
                if (!oldPerks.isBlank()) generated += oldPerks.split(",").length;
            }
            int keep = Math.max(0, lore.lines().size() - Math.max(0, generated));
            stack.set(DataComponents.LORE, new ItemLore(new ArrayList<>(lore.lines().subList(0, keep))));
        }

        removeCraftingAttributeModifiers(stack);
        restoreBaseAttackRange(stack, player, tag);
        stack.set(DataComponents.MAX_DAMAGE, savedBaseMaximum);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            data.putInt(DATA_BASE_MAX_DAMAGE, savedBaseMaximum);
            data.putString(DATA_BASE_NAME, savedBaseName);
            data.remove(DATA_LORE_LINE_COUNT);
        });
    }

    private static void removeCraftingAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        List<ItemAttributeModifiers.Entry> retained = modifiers.modifiers().stream()
                .filter(entry -> !Set.of(CRAFTING_DAMAGE_MODIFIER_ID, CRAFTING_ATTACK_SPEED_MODIFIER_ID,
                        CRAFTING_ARMOR_MODIFIER_ID, CRAFTING_TOUGHNESS_MODIFIER_ID)
                        .contains(entry.modifier().id()))
                .toList();
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(retained));
    }

    private static void storeBaseAttackRangeIfMissing(ItemStack stack, ServerPlayer player) {
        CompoundTag tag = getTag(stack);
        if (tag.getBooleanOr(DATA_BASE_RANGE_PRESENT, false)) return;
        AttackRange range = stack.getOrDefault(DataComponents.ATTACK_RANGE, AttackRange.defaultFor(player));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            data.putBoolean(DATA_BASE_RANGE_PRESENT, true);
            data.putFloat(DATA_BASE_RANGE_MIN, range.minReach());
            data.putFloat(DATA_BASE_RANGE_MAX, range.maxReach());
            data.putFloat(DATA_BASE_RANGE_MIN_CREATIVE, range.minCreativeReach());
            data.putFloat(DATA_BASE_RANGE_MAX_CREATIVE, range.maxCreativeReach());
            data.putFloat(DATA_BASE_RANGE_HITBOX, range.hitboxMargin());
            data.putFloat(DATA_BASE_RANGE_MOB_FACTOR, range.mobFactor());
        });
    }

    private static void restoreBaseAttackRange(ItemStack stack, ServerPlayer player, CompoundTag tag) {
        if (getPerkAmplifier(stack, "celestial_range") < 0) return;
        if (tag.getBooleanOr(DATA_BASE_RANGE_PRESENT, false)) {
            stack.set(DataComponents.ATTACK_RANGE, new AttackRange(
                    tag.getFloatOr(DATA_BASE_RANGE_MIN, 0.0F),
                    tag.getFloatOr(DATA_BASE_RANGE_MAX, 3.0F),
                    tag.getFloatOr(DATA_BASE_RANGE_MIN_CREATIVE, 0.0F),
                    tag.getFloatOr(DATA_BASE_RANGE_MAX_CREATIVE, 5.0F),
                    tag.getFloatOr(DATA_BASE_RANGE_HITBOX, 0.3F),
                    tag.getFloatOr(DATA_BASE_RANGE_MOB_FACTOR, 1.0F)));
        } else {
            AttackRange current = stack.getOrDefault(DataComponents.ATTACK_RANGE, AttackRange.defaultFor(player));
            stack.set(DataComponents.ATTACK_RANGE, new AttackRange(current.minReach(),
                    Math.max(current.minReach(), current.maxReach() - 6.0F),
                    current.minCreativeReach(), Math.max(current.minCreativeReach(), current.maxCreativeReach() - 6.0F),
                    current.hitboxMargin(), current.mobFactor()));
        }
    }

    /** Applies a requested rarity to a test item held in the main hand. */
    public static boolean applyCommandRarity(ServerPlayer player, CraftingRarity rarity) {
        if (player == null || rarity == null) return false;
        ItemStack stack = player.getMainHandItem();
        if (stack == null || stack.isEmpty() || stack.get(DataComponents.MAX_DAMAGE) == null || hasRarity(stack)) {
            return false;
        }
        // A rarity command simulates the item being crafted: same affixes, attributes, lore,
        // title unlock, announcement and reveal packet. It intentionally grants no crafting XP.
        applyEquipmentRarity(stack, player, rarity, getCraftingLevel(player));
        return true;
    }

    private static int getCraftingLevel(ServerPlayer player) {
        return Math.max(1, Math.min(99,
                com.github.ob_yekt.simpleskills.managers.XPManager.getSkillLevel(
                        player.getStringUUID(), com.github.ob_yekt.simpleskills.Skills.CRAFTING)));
    }

    private static void applyEquipmentRarity(ItemStack stack, ServerPlayer player, CraftingRarity rarity, int level) {
        CompoundTag previousTag = getTag(stack);
        String baseName = previousTag.getStringOr(DATA_BASE_NAME, "");
        if (baseName.isBlank()) baseName = getUnprefixedItemName(stack);
        final String persistedBaseName = baseName;
        Integer baseMaximum = previousTag.getIntOr(DATA_BASE_MAX_DAMAGE, -1);
        if (baseMaximum == null || baseMaximum < 1) {
            baseMaximum = stack.get(DataComponents.MAX_DAMAGE);
        }
        if (baseMaximum == null || baseMaximum < 1) baseMaximum = 1;
        final int persistedBaseMaximum = baseMaximum;

        EquipmentKind kind = classify(stack);
        List<Perk> perks = rollPerks(kind, rarity, level, player);

        boolean weapon = isWeapon(stack);
        boolean sword = stack.is(ItemTags.SWORDS);
        boolean damageEligible = weapon || kind == EquipmentKind.TOOL;
        boolean attackSpeedEligible = sword || kind == EquipmentKind.TOOL;
        boolean breakSpeedEligible = kind == EquipmentKind.PICKAXE
                || kind == EquipmentKind.AXE || kind == EquipmentKind.SHOVEL;
        boolean defenseEligible = kind == EquipmentKind.ARMOR || kind == EquipmentKind.HELMET
                || kind == EquipmentKind.CHESTPLATE || kind == EquipmentKind.LEGGINGS
                || kind == EquipmentKind.BOOTS || kind == EquipmentKind.SHIELD;

        // Each attribute has its own rarity-weighted chance to exist. Common gear often
        // has no extra combat/defense stat; Epic and higher always roll applicable stats.
        double damageBonus = damageEligible && rollStatPresence(player, rarity.damageStatChance())
                ? rollStat(player, rarity.minDamageBonus(), rarity.maxDamageBonus(), 0.001) : 0.0;
        double durabilityBonus = rollStat(player, rarity.minDurabilityBonus(), rarity.maxDurabilityBonus(), 0.01);
        // Break Speed is a basic equipment stat (not a blue perk) and has its own rarity-based appearance roll.
        double breakSpeedBonus = breakSpeedEligible && rollStatPresence(player, rarity.breakSpeedStatChance())
                ? rollStat(player, rarity.minBreakSpeedBonus(), rarity.maxBreakSpeedBonus(), 0.01) : 0.0;
        double criticalChance = weapon && damageBonus > 0.0
                ? rollStat(player, rarity.minCriticalChance(), rarity.maxCriticalChance(), 0.001) : 0.0;
        double attackSpeedBonus = attackSpeedEligible && rollStatPresence(player, rarity.attackSpeedStatChance())
                ? rollStat(player, rarity.minAttackSpeedBonus(), rarity.maxAttackSpeedBonus(), 0.005) : 0.0;
        double armorBonus = defenseEligible && rollStatPresence(player, rarity.armorStatChance())
                ? rollStat(player, rarity.minArmorBonus(), rarity.maxArmorBonus(), 0.001) : 0.0;
        double toughnessBonus = defenseEligible && rollStatPresence(player, rarity.toughnessStatChance())
                ? rollStat(player, rarity.minToughnessBonus(), rarity.maxToughnessBonus(), 0.001) : 0.0;
        String encodedPerks = encodePerks(perks, level, rarity, player);
        boolean celestialRangeApplied = perks.stream().anyMatch(perk -> perk.id().equals("celestial_range"));
        if (celestialRangeApplied) {
            storeBaseAttackRangeIfMissing(stack, player);
            applyCelestialRange(stack, player);
        }

        // Runic Edge is included in the same final percentage shown in the lore and native attribute tooltip.
        boolean hasRunicEdge = perks.stream().anyMatch(perk -> perk.id().equals("runic_edge"));
        double requestedDamageBonus = damageBonus > 0.0
                ? (1.0 + damageBonus) * (hasRunicEdge ? 1.08 : 1.0) - 1.0 : 0.0;
        boolean damageAppliedAsAttribute = applyAttackDamageAttributeBonus(stack, requestedDamageBonus);
        boolean attackSpeedAppliedAsAttribute = attackSpeedBonus > 0.0
                && applyAttackSpeedAttributeBonus(stack, attackSpeedBonus);
        if (!damageAppliedAsAttribute && !isRangedWeapon(stack)) damageBonus = 0.0;
        if (!attackSpeedAppliedAsAttribute) attackSpeedBonus = 0.0;
        double totalDamageBonus = damageBonus > 0.0
                ? (1.0 + damageBonus) * (hasRunicEdge ? 1.08 : 1.0) - 1.0 : 0.0;
        boolean[] defenseApplied = applyDefenseAttributeBonuses(stack, kind, armorBonus, toughnessBonus);
        if (!defenseApplied[0]) armorBonus = 0.0;
        if (!defenseApplied[1]) toughnessBonus = 0.0;
        final double finalDamageBonus = damageBonus;
        final double finalAttackSpeedBonus = attackSpeedBonus;
        final double finalArmorBonus = armorBonus;
        final double finalToughnessBonus = toughnessBonus;

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(DATA_RARITY, rarity.id());
            tag.putString(DATA_BASE_NAME, persistedBaseName);
            tag.putInt(DATA_BASE_MAX_DAMAGE, persistedBaseMaximum);
            tag.putDouble(DATA_DAMAGE, finalDamageBonus);
            tag.putDouble(DATA_DURABILITY, durabilityBonus);
            tag.putDouble(DATA_BREAK_SPEED, breakSpeedBonus);
            tag.putDouble(DATA_CRITICAL_CHANCE, criticalChance);
            tag.putDouble(DATA_ATTACK_SPEED, finalAttackSpeedBonus);
            tag.putDouble(DATA_ARMOR, finalArmorBonus);
            tag.putDouble(DATA_TOUGHNESS, finalToughnessBonus);
            tag.putString(DATA_PERKS, encodedPerks);
            tag.putBoolean(DATA_DAMAGE_AS_ATTRIBUTE, damageAppliedAsAttribute);
            tag.putBoolean(DATA_DAMAGE_ATTRIBUTE_INCLUDES_AFFIXES, damageAppliedAsAttribute);
        });

        stack.set(DataComponents.MAX_DAMAGE, Math.max(1,
                (int) Math.round(baseMaximum * (1.0 + durabilityBonus))));

        Component originalName = Component.literal(baseName);
        Component rarityName = Component.translatable("item.simpleskills.crafting_rarity." + rarity.id());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "item.simpleskills.crafted_quality_name", rarityName, originalName)
                .withStyle(style -> style.withColor(rarity.color()).withItalic(false)));
        double displayedCriticalChance = getEffectiveCriticalChance(stack);
        // Display the combined rarity + Runic Edge percentage so it matches the real main-hand attribute modifier.
        appendTooltip(stack, rarity, totalDamageBonus, durabilityBonus, breakSpeedBonus, displayedCriticalChance,
                attackSpeedBonus, armorBonus, toughnessBonus);

        if (weapon) unlockWeaponTitle(player, rarity);
        announceHighRarityCraft(player, stack, rarity);
        sendReveal(player, stack, originalName.getString(), rarity, totalDamageBonus, durabilityBonus,
                breakSpeedBonus, displayedCriticalChance, attackSpeedBonus, armorBonus, toughnessBonus, encodedPerks);
    }

    /** Announces Legendary, Mythic and Transcendent equipment to the entire server. */
    private static void announceHighRarityCraft(ServerPlayer player, ItemStack stack, CraftingRarity rarity) {
        if (rarity.ordinal() < CraftingRarity.LEGENDARY.ordinal()) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        Component announcement = Component.translatable(
                "message.simpleskills.crafting.high_rarity_announcement",
                player.getDisplayName(), stack.getHoverName());
        MutableComponent styledAnnouncement = Component.empty()
                .append(announcement)
                .withStyle(ChatFormatting.GRAY);
        Component message = Component.literal("[SimpleSkills] ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(styledAnnouncement);
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /** Returns the XP multiplier carried by a newly crafted equipment item. */
    public static double getCraftingXpMultiplier(ItemStack stack) {
        if (!hasRarity(stack)) return 1.0;
        CraftingRarity rarity = CraftingRarity.byId(getRarityId(stack));
        return rarity == null ? 1.0 : rarity.craftingXpMultiplier();
    }

    /**
     * Collects all high-rarity results from a short crafting burst. The client receives one batch,
     * so closing the reveal screen cannot cause later packets from the same shift-click to reopen it.
     */
    private static void sendReveal(ServerPlayer player, ItemStack stack, String itemName, CraftingRarity rarity,
                                   double damageBonus, double durabilityBonus, double breakSpeedBonus,
                                   double criticalChance,
                                   double attackSpeedBonus, double armorBonus, double toughnessBonus,
                                   String encodedPerks) {
        // Common and Rare are intentionally silent; the reveal screen is reserved for Unique+.
        if (rarity.ordinal() < CraftingRarity.UNIQUE.ordinal()) return;
        if (!ServerPlayNetworking.canSend(player, CraftingRevealPayload.TYPE)) return;

        JsonObject details = new JsonObject();
        details.addProperty("damage", damageBonus * 100.0);
        details.addProperty("durability", (int) Math.round(durabilityBonus * 100.0));
        details.addProperty("breakSpeed", breakSpeedBonus * 100.0);
        details.addProperty("critical", criticalChance * 100.0);
        details.addProperty("attackSpeed", attackSpeedBonus * 100.0);
        details.addProperty("armor", armorBonus * 100.0);
        details.addProperty("toughness", toughnessBonus * 100.0);

        // Send the same rarity ranges used by the item's tooltip so the reveal screen
        // can display the rolled value and identify a true maximum roll.
        boolean hasRunicEdge = getPerkAmplifier(stack, "runic_edge") >= 0;
        double damageMin = rarity.minDamageBonus() * 100.0;
        double damageMax = rarity.maxDamageBonus() * 100.0;
        if (hasRunicEdge) {
            damageMin = ((1.0 + rarity.minDamageBonus()) * 1.08 - 1.0) * 100.0;
            damageMax = ((1.0 + rarity.maxDamageBonus()) * 1.08 - 1.0) * 100.0;
        }
        addRevealStatRange(details, "damage", damageMin, damageMax);
        addRevealStatRange(details, "durability",
                rarity.minDurabilityBonus() * 100.0, rarity.maxDurabilityBonus() * 100.0);
        addRevealStatRange(details, "breakSpeed",
                rarity.minBreakSpeedBonus() * 100.0, rarity.maxBreakSpeedBonus() * 100.0);
        int criticalMastery = getPerkAmplifier(stack, "critical_mastery");
        double criticalMasteryBonus = criticalMastery >= 0 ? 5.0 + criticalMastery * 5.0 : 0.0;
        addRevealStatRange(details, "critical",
                Math.min(60.0, rarity.minCriticalChance() * 100.0 + criticalMasteryBonus),
                Math.min(60.0, rarity.maxCriticalChance() * 100.0 + criticalMasteryBonus));
        addRevealStatRange(details, "attackSpeed",
                rarity.minAttackSpeedBonus() * 100.0, rarity.maxAttackSpeedBonus() * 100.0);
        addRevealStatRange(details, "armor",
                rarity.minArmorBonus() * 100.0, rarity.maxArmorBonus() * 100.0);
        addRevealStatRange(details, "toughness",
                rarity.minToughnessBonus() * 100.0, rarity.maxToughnessBonus() * 100.0);
        JsonArray perks = new JsonArray();
        for (String encoded : encodedPerks.isBlank() ? List.<String>of() : List.of(encodedPerks.split(","))) {
            String[] parts = encoded.split(":", 2);
            JsonObject perk = new JsonObject();
            perk.addProperty("id", parts[0]);
            perk.addProperty("level", parseInt(parts.length > 1 ? parts[1] : "0", 0) + 1);
            perks.add(perk);
        }
        details.add("perks", perks);

        JsonObject entry = new JsonObject();
        entry.addProperty("itemName", itemName);
        entry.addProperty("itemId", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        entry.addProperty("rarity", rarity.id());
        entry.add("details", details);

        PendingRevealBatch pending = PENDING_REVEALS.computeIfAbsent(player.getUUID(), ignored -> new PendingRevealBatch());
        pending.entries.add(entry);
        pending.lastQueuedAt = player.level().getGameTime();
    }

    private static void addRevealStatRange(JsonObject details, String field, double minimum, double maximum) {
        details.addProperty(field + "Min", minimum);
        details.addProperty(field + "Max", maximum);
    }

    private static void flushPendingReveals(MinecraftServer server, long now) {
        for (Map.Entry<UUID, PendingRevealBatch> queued : new ArrayList<>(PENDING_REVEALS.entrySet())) {
            UUID playerId = queued.getKey();
            PendingRevealBatch batch = queued.getValue();
            if (now - batch.lastQueuedAt < REVEAL_BATCH_IDLE_TICKS) continue;
            if (!PENDING_REVEALS.remove(playerId, batch)) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || !ServerPlayNetworking.canSend(player, CraftingRevealPayload.TYPE)) continue;

            JsonArray packetEntries = new JsonArray();
            int inPacket = 0;
            for (var element : batch.entries) {
                packetEntries.add(element);
                inPacket++;
                if (inPacket >= MAX_REVEALS_PER_PACKET) {
                    ServerPlayNetworking.send(player, new CraftingRevealPayload(packetEntries.toString()));
                    packetEntries = new JsonArray();
                    inPacket = 0;
                }
            }
            if (inPacket > 0) {
                ServerPlayNetworking.send(player, new CraftingRevealPayload(packetEntries.toString()));
            }
        }
    }

    private static void unlockWeaponTitle(ServerPlayer player, CraftingRarity rarity) {
        DatabaseManager db = DatabaseManager.getInstance();
        String uuid = player.getStringUUID();
        db.ensurePlayerInitialized(uuid);
        if (db.unlockCraftedWeaponRank(uuid, rarity.id())) {
            String titleKey = "skill.simpleskills.rank.crafting.weapon_" + rarity.id();
            player.sendSystemMessage(Component.translatable(
                    "message.simpleskills.crafting.weapon_rank_unlocked", Component.translatable(titleKey))
                    .withStyle(ChatFormatting.GOLD));
            player.playSound(SoundEvents.PLAYER_LEVELUP, 0.55F, 1.2F);
        }
    }

    /** Resolve the exact equipment class so Transcendent items receive their own signature ability. */
    private static EquipmentKind classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return EquipmentKind.OTHER;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("shield")) return EquipmentKind.SHIELD;

        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            return switch (equippable.slot()) {
                case HEAD -> EquipmentKind.HELMET;
                case CHEST -> EquipmentKind.CHESTPLATE;
                case LEGS -> EquipmentKind.LEGGINGS;
                case FEET -> EquipmentKind.BOOTS;
                default -> EquipmentKind.ARMOR;
            };
        }
        if (stack.is(ItemTags.SWORDS)) return EquipmentKind.SWORD;
        if (stack.is(ItemTags.PICKAXES)) return EquipmentKind.PICKAXE;
        if (stack.is(ItemTags.SHOVELS)) return EquipmentKind.SHOVEL;
        if (stack.is(ItemTags.HOES)) return EquipmentKind.HOE;
        if (stack.is(ItemTags.AXES)) return EquipmentKind.AXE;
        if (stack.is(Items.BOW) || path.endsWith("_bow")) return EquipmentKind.BOW;
        if (stack.is(Items.CROSSBOW) || path.endsWith("_crossbow")) return EquipmentKind.CROSSBOW;
        if (stack.is(Items.TRIDENT) || path.endsWith("_trident")) return EquipmentKind.TRIDENT;
        if (stack.is(Items.FISHING_ROD) || path.equals("fishing_rod")) return EquipmentKind.FISHING_ROD;
        if (stack.is(Items.MACE) || path.endsWith("_mace")) return EquipmentKind.MACE;
        if (isWeapon(stack)) return EquipmentKind.WEAPON;
        return EquipmentKind.OTHER;
    }

    private static boolean isWeapon(ItemStack stack) {
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) return true;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(java.util.Locale.ROOT);
        return path.equals("bow") || path.endsWith("_bow")
                || path.equals("crossbow") || path.endsWith("_crossbow")
                || path.equals("trident") || path.endsWith("_trident")
                || path.equals("mace") || path.endsWith("_mace")
                || path.equals("spear") || path.endsWith("_spear");
    }

    private static List<Perk> commonPool(EquipmentKind kind) {
        return switch (kind) {
            case WEAPON, SWORD, BOW, CROSSBOW, TRIDENT, MACE ->
                    new ArrayList<>(List.of(STRENGTH, SPEED, REGENERATION, VAMPIRIC,
                            CRITICAL_MASTERY, THUNDEROUS, FROSTBITE, PHOENIX, DOUBLE_LEAP, WINDWALKER));
            case ARMOR, HELMET, CHESTPLATE, LEGGINGS, BOOTS ->
                    new ArrayList<>(List.of(PROTECTION, RESILIENT, SPEED, REGENERATION,
                            FEATHERWEIGHT, VITALITY, FIRE_RESISTANCE, JUMP_BOOST, DOUBLE_LEAP));
            case SHIELD -> new ArrayList<>(List.of(PROTECTION, RESILIENT, REGENERATION, VITALITY,
                    FIRE_RESISTANCE, DOUBLE_LEAP));
            case SHOVEL -> new ArrayList<>(List.of(HASTE, EFFICIENT, SPEED,
                    INDESTRUCTIBLE, TREASURE_HUNTER));
            case PICKAXE -> new ArrayList<>(List.of(HASTE, EFFICIENT, SPEED,
                    FORTUNE_MASTERY, INDESTRUCTIBLE));
            case AXE, TOOL, HOE -> new ArrayList<>(List.of(HASTE, EFFICIENT, SPEED,
                    INDESTRUCTIBLE));
            default -> new ArrayList<>(List.of(SPEED, REGENERATION, VITALITY,
                    HASTE, EFFICIENT, INDESTRUCTIBLE));
        };
    }

    private static List<Perk> uniquePool(EquipmentKind kind) {
        return switch (kind) {
            case WEAPON, SWORD, BOW, CROSSBOW, TRIDENT, MACE -> List.of(RUNIC_EDGE);
            case AXE -> List.of(RUNIC_EDGE);
            case ARMOR, HELMET, CHESTPLATE, LEGGINGS, BOOTS -> List.of(ANVIL_WARD);
            case SHIELD -> List.of(GUARDIAN_OATH);
            case TOOL, PICKAXE, SHOVEL -> List.of(PROSPECTOR);
            case HOE -> List.of(PROSPECTOR);
            default -> List.of(ANVIL_WARD);
        };
    }

    private static List<Perk> transcendentPool(EquipmentKind kind) {
        return switch (kind) {
            case HELMET -> List.of(ORACLE_VISION);
            case CHESTPLATE -> List.of(AEGIS_OF_PURITY);
            case LEGGINGS -> List.of(PHANTOM_STRIDE);
            case BOOTS -> List.of(CELESTIAL_WINGS);
            case SWORD -> List.of(CELESTIAL_RANGE);
            case SHIELD -> List.of(DIVINE_REFLECTION);
            case BOW -> List.of(ASTRAL_PURSUIT);
            case CROSSBOW -> List.of(SINGULARITY_BOLT);
            case TRIDENT -> List.of(POSEIDONS_WRATH);
            case FISHING_ROD -> List.of(ABYSSAL_SOVEREIGN);
            case AXE -> List.of(MIDAS_TOUCH);
            case PICKAXE -> List.of(WORLDBREAKER);
            case SHOVEL -> List.of(BURIED_TRUTH);
            case HOE -> List.of(EDENS_DOMINION);
            case WEAPON, MACE -> List.of(TITAN_STRENGTH, GODSLAYER, ARCANE_MASTERY);
            case TOOL -> List.of(WORLD_SHAPER);
            default -> List.of(DIVINE_BLESSING);
        };
    }

    private static List<Perk> rollPerks(EquipmentKind kind, CraftingRarity rarity, int level, ServerPlayer player) {
        List<Perk> generalPool = commonPool(kind);
        List<Perk> selected = new ArrayList<>();

        if (rarity == CraftingRarity.UNIQUE) {
            List<Perk> special = uniquePool(kind);
            selected.add(special.get(player.getRandom().nextInt(special.size())));
            return List.copyOf(selected);
        }

        if (rarity == CraftingRarity.TRANSCENDENT) {
            List<Perk> special = new ArrayList<>(transcendentPool(kind));
            Perk guaranteedExclusive = special.remove(player.getRandom().nextInt(special.size()));
            selected.add(guaranteedExclusive);
            generalPool.remove(guaranteedExclusive);
        }

        // A single item must never roll two perks that grant the same potion effect.
        // This also handles aliases such as Haste and Efficient, which both grant minecraft:haste,
        // and prevents a Transcendent signature effect from being repeated by a regular affix.
        removeDuplicatePotionEffects(generalPool, selected);

        int exclusiveCount = selected.size();
        int guaranteedGeneral = rarity == CraftingRarity.TRANSCENDENT
                ? rarity.minimumPerks() : Math.max(0, rarity.minimumPerks() - exclusiveCount);
        int maximumGeneral = rarity == CraftingRarity.TRANSCENDENT
                ? rarity.maximumPerks()
                : Math.min(rarity.maximumPerks(), generalPool.size() + exclusiveCount);
        for (int i = 0; i < guaranteedGeneral && !generalPool.isEmpty(); i++) {
            selected.add(generalPool.remove(player.getRandom().nextInt(generalPool.size())));
        }

        // Extra affixes become more likely with every Crafting level, without exceeding the tier cap.
        // A Transcendent signature ability is an extra slot: it does not replace its 4-5 regular perks.
        double extraPerkChance = Math.min(0.99, 0.05 + level * 0.0095);
        while (selected.size() - exclusiveCount < maximumGeneral && !generalPool.isEmpty()
                && player.getRandom().nextDouble() < extraPerkChance) {
            selected.add(generalPool.remove(player.getRandom().nextInt(generalPool.size())));
        }
        return List.copyOf(selected);
    }

    /** Removes duplicate potion-effect IDs from both already selected perks and remaining candidates. */
    private static void removeDuplicatePotionEffects(List<Perk> candidates, List<Perk> selected) {
        Set<String> usedEffectIds = new HashSet<>();
        selected.removeIf(perk -> !perk.effectId().isBlank() && !usedEffectIds.add(perk.effectId()));
        candidates.removeIf(perk -> !perk.effectId().isBlank() && !usedEffectIds.add(perk.effectId()));
    }

    private static String encodePerks(List<Perk> perks, int level, CraftingRarity rarity, ServerPlayer player) {
        List<String> encoded = new ArrayList<>();
        for (Perk perk : perks) {
            if (perk.maximumEffectLevel() <= 0) {
                encoded.add(perk.id() + ":0");
                continue;
            }

            // High rarities cannot roll low-tier ranks: Epic starts at II, Legendary at III,
            // Mythic at III and Transcendent at IV. Crafting level controls the upper bound.
            int minimumRank = Math.min(perk.maximumEffectLevel(), rarity.minimumPerkEffectLevel());
            int skillMaximum = 1 + (Math.max(1, level) - 1) / 25;
            int maximumRank = Math.min(perk.maximumEffectLevel(), Math.max(minimumRank, skillMaximum));
            int rank = rollPerkRank(player, minimumRank, maximumRank, rarity);
            encoded.add(perk.id() + ":" + (rank - 1));
        }
        return String.join(",", encoded);
    }

    private static int rollPerkRank(ServerPlayer player, int minimumRank, int maximumRank, CraftingRarity rarity) {
        if (maximumRank <= minimumRank) return minimumRank;
        int tierWeight = rarity.ordinal() + 1;
        int totalWeight = 0;
        for (int rank = minimumRank; rank <= maximumRank; rank++) {
            totalWeight += 1 + (rank - minimumRank) * tierWeight;
        }
        int roll = player.getRandom().nextInt(totalWeight);
        for (int rank = minimumRank; rank <= maximumRank; rank++) {
            int weight = 1 + (rank - minimumRank) * tierWeight;
            if (roll < weight) return rank;
            roll -= weight;
        }
        return maximumRank;
    }

    private static double rollStat(ServerPlayer player, double minimum, double maximum, double step) {
        if (maximum <= minimum) return minimum;
        double rolled = minimum + player.getRandom().nextDouble() * (maximum - minimum);
        return Math.max(minimum, Math.min(maximum, Math.round(rolled / step) * step));
    }

    /**
     * Adds the complete rarity + affix bonus to the item's actual main-hand ATTACK_DAMAGE attribute.
     * The amount is calculated from the item's vanilla attack value: a 13.4% total bonus on a
     * diamond sword (+7 attack damage) becomes +0.938 (shown as +0.94 in the vanilla tooltip).
     *
     * @return true when melee damage is now represented by the item attribute modifier.
     */
    private static boolean applyAttackDamageAttributeBonus(ItemStack stack, double totalDamageBonus) {
        if (stack == null || stack.isEmpty() || totalDamageBonus <= 0.0) return false;

        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        // Do not add a melee attribute to bows/crossbows or other ranged-only items.
        boolean hasMainHandAttackDamage = modifiers.modifiers().stream().anyMatch(entry ->
                entry.attribute().equals(Attributes.ATTACK_DAMAGE)
                        && entry.slot().test(EquipmentSlot.MAINHAND));
        if (!hasMainHandAttackDamage) return false;

        // Protect against duplicate application if this method is ever called on a partially
        // migrated item that already contains our stable modifier ID.
        boolean alreadyApplied = modifiers.modifiers().stream().anyMatch(entry ->
                entry.modifier().id().equals(CRAFTING_DAMAGE_MODIFIER_ID));
        if (alreadyApplied) return true;

        double currentAttackDamage = modifiers.compute(
                Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND);
        double bonusAmount = currentAttackDamage * totalDamageBonus;
        if (!Double.isFinite(bonusAmount) || bonusAmount <= 0.0) return false;

        AttributeModifier damageModifier = new AttributeModifier(
                CRAFTING_DAMAGE_MODIFIER_ID, bonusAmount, AttributeModifier.Operation.ADD_VALUE);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                modifiers.withModifierAdded(Attributes.ATTACK_DAMAGE,
                        damageModifier, EquipmentSlotGroup.MAINHAND));
        return true;
    }

    /** Adds a percentage of the sword's current main-hand attack speed as a real item attribute. */
    private static boolean applyAttackSpeedAttributeBonus(ItemStack stack, double speedBonus) {
        if (stack == null || stack.isEmpty() || speedBonus <= 0.0) return false;
        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        boolean hasMainHandAttackSpeed = modifiers.modifiers().stream().anyMatch(entry ->
                entry.attribute().equals(Attributes.ATTACK_SPEED)
                        && entry.slot().test(EquipmentSlot.MAINHAND));
        if (!hasMainHandAttackSpeed) return false;
        boolean alreadyApplied = modifiers.modifiers().stream().anyMatch(entry ->
                entry.modifier().id().equals(CRAFTING_ATTACK_SPEED_MODIFIER_ID));
        if (alreadyApplied) return true;

        double currentAttackSpeed = modifiers.compute(
                Attributes.ATTACK_SPEED, Attributes.DEFAULT_ATTACK_SPEED, EquipmentSlot.MAINHAND);
        double bonusAmount = currentAttackSpeed * speedBonus;
        if (!Double.isFinite(bonusAmount) || bonusAmount <= 0.0) return false;
        AttributeModifier speedModifier = new AttributeModifier(
                CRAFTING_ATTACK_SPEED_MODIFIER_ID, bonusAmount, AttributeModifier.Operation.ADD_VALUE);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                modifiers.withModifierAdded(Attributes.ATTACK_SPEED,
                        speedModifier, EquipmentSlotGroup.MAINHAND));
        return true;
    }

    /** Adds six blocks of melee reach through Minecraft 26.3's native attack_range component. */
    private static void applyCelestialRange(ItemStack stack, ServerPlayer player) {
        AttackRange current = stack.getOrDefault(DataComponents.ATTACK_RANGE, AttackRange.defaultFor(player));
        float maxReach = Math.min(64.0F, current.maxReach() + 6.0F);
        float maxCreativeReach = Math.min(64.0F, current.maxCreativeReach() + 6.0F);
        stack.set(DataComponents.ATTACK_RANGE, new AttackRange(
                current.minReach(), maxReach,
                current.minCreativeReach(), maxCreativeReach,
                current.hitboxMargin(), current.mobFactor()));
    }

    /**
     * Adds randomized Armor and Armor Toughness bonuses to armor pieces or shields.
     * Armor pieces reuse their native equipment slot group. Shields receive the bonus
     * while held in the offhand. Toughness uses a small 0.5 baseline on materials whose
     * vanilla toughness is zero, so leather/iron/gold armor can still gain this stat.
     */
    private static boolean[] applyDefenseAttributeBonuses(ItemStack stack, EquipmentKind kind,
                                                           double armorBonus, double toughnessBonus) {
        boolean armorPiece = kind == EquipmentKind.ARMOR || kind == EquipmentKind.HELMET
                || kind == EquipmentKind.CHESTPLATE || kind == EquipmentKind.LEGGINGS
                || kind == EquipmentKind.BOOTS;
        if (stack == null || stack.isEmpty() || (!armorPiece && kind != EquipmentKind.SHIELD)) {
            return new boolean[]{false, false};
        }

        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        EquipmentSlotGroup slotGroup = null;
        EquipmentSlot equipmentSlot = null;

        if (kind == EquipmentKind.SHIELD) {
            slotGroup = EquipmentSlotGroup.OFFHAND;
            equipmentSlot = EquipmentSlot.OFFHAND;
        } else {
            for (var entry : modifiers.modifiers()) {
                if (!entry.attribute().equals(Attributes.ARMOR)) continue;
                slotGroup = entry.slot();
                for (EquipmentSlot candidate : List.of(
                        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                    if (slotGroup.test(candidate)) {
                        equipmentSlot = candidate;
                        break;
                    }
                }
                if (equipmentSlot != null) break;
            }
        }

        if (slotGroup == null || equipmentSlot == null) return new boolean[]{false, false};

        boolean armorApplied = false;
        boolean toughnessApplied = false;
        double baseArmor = modifiers.compute(Attributes.ARMOR, 0.0, equipmentSlot);
        double baseToughness = modifiers.compute(Attributes.ARMOR_TOUGHNESS, 0.0, equipmentSlot);
        // A vanilla shield has no armor attribute; use a modest 2-point reference
        // so a rolled shield affix grants a small, readable offhand defense bonus.
        if (kind == EquipmentKind.SHIELD) baseArmor = Math.max(baseArmor, 2.0);
        // Materials without native Armor Toughness still receive a small reference value.
        double toughnessReference = baseToughness > 0.0
                ? baseToughness : Math.max(0.5, baseArmor * 0.10);

        if (armorBonus > 0.0 && !hasModifier(modifiers, CRAFTING_ARMOR_MODIFIER_ID)) {
            double amount = baseArmor * armorBonus;
            if (Double.isFinite(amount) && amount > 0.0) {
                AttributeModifier modifier = new AttributeModifier(
                        CRAFTING_ARMOR_MODIFIER_ID, amount, AttributeModifier.Operation.ADD_VALUE);
                modifiers = modifiers.withModifierAdded(Attributes.ARMOR, modifier, slotGroup);
                armorApplied = true;
            }
        } else if (hasModifier(modifiers, CRAFTING_ARMOR_MODIFIER_ID)) {
            armorApplied = true;
        }

        if (toughnessBonus > 0.0 && !hasModifier(modifiers, CRAFTING_TOUGHNESS_MODIFIER_ID)) {
            double amount = toughnessReference * toughnessBonus;
            if (Double.isFinite(amount) && amount > 0.0) {
                AttributeModifier modifier = new AttributeModifier(
                        CRAFTING_TOUGHNESS_MODIFIER_ID, amount, AttributeModifier.Operation.ADD_VALUE);
                modifiers = modifiers.withModifierAdded(Attributes.ARMOR_TOUGHNESS, modifier, slotGroup);
                toughnessApplied = true;
            }
        } else if (hasModifier(modifiers, CRAFTING_TOUGHNESS_MODIFIER_ID)) {
            toughnessApplied = true;
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
        return new boolean[]{armorApplied, toughnessApplied};
    }

    private static boolean hasModifier(ItemAttributeModifiers modifiers, Identifier modifierId) {
        return modifiers.modifiers().stream().anyMatch(entry -> entry.modifier().id().equals(modifierId));
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        return stack != null && !stack.isEmpty() && (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT));
    }

    private static boolean rollStatPresence(ServerPlayer player, double chance) {
        return chance >= 1.0 || (chance > 0.0 && player.getRandom().nextDouble() < chance);
    }

    private static String formatPercent(double percentage) {
        double nearestWhole = Math.rint(percentage);
        if (Math.abs(percentage - nearestWhole) < 0.005) {
            return String.format(java.util.Locale.ROOT, "%.0f", nearestWhole);
        }
        double nearestTenth = Math.rint(percentage * 10.0) / 10.0;
        if (Math.abs(percentage - nearestTenth) < 0.005) {
            return String.format(java.util.Locale.ROOT, "%.1f", nearestTenth);
        }
        // Rune upgrades can move a stat by only 1% of its range per use; retain enough
        // precision to make each consumed Rune visibly change the displayed attribute.
        return String.format(java.util.Locale.ROOT, "%.2f", percentage);
    }

    private static void appendTooltip(ItemStack stack, CraftingRarity rarity, double damageBonus,
                                      double durabilityBonus, double breakSpeedBonus, double criticalChance,
                                      double attackSpeedBonus, double armorBonus, double toughnessBonus) {
        ItemLore current = stack.getOrDefault(DataComponents.LORE, new ItemLore(List.of()));
        List<Component> lines = new ArrayList<>(current.lines());
        int generatedLines = 0;
        lines.add(Component.translatable("item.simpleskills.crafting_rarity_label",
                Component.translatable("item.simpleskills.crafting_rarity." + rarity.id()))
                .withStyle(rarity.color()).withStyle(style -> style.withItalic(false)));
        generatedLines++;

        boolean runicEdge = getPerkAmplifier(stack, "runic_edge") >= 0;
        double damageMin = rarity.minDamageBonus() * 100.0;
        double damageMax = rarity.maxDamageBonus() * 100.0;
        if (runicEdge) {
            damageMin = ((1.0 + rarity.minDamageBonus()) * 1.08 - 1.0) * 100.0;
            damageMax = ((1.0 + rarity.maxDamageBonus()) * 1.08 - 1.0) * 100.0;
        }
        if (damageBonus > 0.0) {
            lines.add(rangeStatLine("item.simpleskills.crafting_stat.damage", damageBonus * 100.0,
                    damageMin, damageMax));
            generatedLines++;
        }
        if (durabilityBonus > 0.0) {
            lines.add(rangeStatLine("item.simpleskills.crafting_stat.durability", durabilityBonus * 100.0,
                    rarity.minDurabilityBonus() * 100.0, rarity.maxDurabilityBonus() * 100.0));
            generatedLines++;
        }
        if (breakSpeedBonus > 0.0) {
            lines.add(rangeStatLine("item.simpleskills.crafting_stat.break_speed", breakSpeedBonus * 100.0,
                    rarity.minBreakSpeedBonus() * 100.0, rarity.maxBreakSpeedBonus() * 100.0));
            generatedLines++;
        }
        if (criticalChance > 0.0) {
            int mastery = getPerkAmplifier(stack, "critical_mastery");
            double masteryBonus = mastery >= 0 ? 5.0 + mastery * 5.0 : 0.0;
            lines.add(rangeStatLine("item.simpleskills.crafting_stat.critical_chance", criticalChance * 100.0,
                    Math.min(60.0, rarity.minCriticalChance() * 100.0 + masteryBonus),
                    Math.min(60.0, rarity.maxCriticalChance() * 100.0 + masteryBonus)));
            generatedLines++;
        }
        if (attackSpeedBonus > 0.0) {
            lines.add(rangeStatLine("item.simpleskills.crafting_stat.attack_speed", attackSpeedBonus * 100.0,
                    rarity.minAttackSpeedBonus() * 100.0, rarity.maxAttackSpeedBonus() * 100.0));
            generatedLines++;
        }
        if (armorBonus > 0.0) {
            lines.add(rangeStatLine("item.simpleskills.crafting_stat.armor", armorBonus * 100.0,
                    rarity.minArmorBonus() * 100.0, rarity.maxArmorBonus() * 100.0));
            generatedLines++;
        }
        if (toughnessBonus > 0.0) {
            lines.add(rangeStatLine("item.simpleskills.crafting_stat.armor_toughness", toughnessBonus * 100.0,
                    rarity.minToughnessBonus() * 100.0, rarity.maxToughnessBonus() * 100.0));
            generatedLines++;
        }
        for (String encoded : encodePerksWithAmplifiers(stack)) {
            String[] parts = encoded.split(":", 2);
            String id = parts[0];
            int amplifier = parts.length > 1 ? parseInt(parts[1], 0) : 0;
            String key = "item.simpleskills.crafting_perk." + id;
            MutableComponent perkLine;
            if (isJumpPerk(id)) {
                perkLine = Component.translatable(key, getAdditionalJumpsForPerk(id, amplifier + 1));
            } else if (id.equals("vitality")) {
                perkLine = Component.translatable(key, roman(amplifier + 1), (amplifier + 1) * 4);
            } else if (id.equals("resilient")) {
                perkLine = Component.translatable(key, roman(amplifier + 1), (amplifier + 1) * 2);
            } else if (NO_LEVEL_PERKS.contains(id)) {
                perkLine = Component.translatable(key);
            } else {
                perkLine = Component.translatable(key, roman(amplifier + 1));
            }
            ChatFormatting perkColor = Set.of("celestial_wings", "celestial_range", "oracle_vision",
                    "aegis_of_purity", "phantom_stride", "divine_reflection", "astral_pursuit",
                    "singularity_bolt", "poseidons_wrath", "worldbreaker", "buried_truth",
                    "edens_dominion", "midas_touch", "abyssal_sovereign").contains(id)
                    ? ChatFormatting.GOLD : ChatFormatting.AQUA;
            lines.add(perkLine.withStyle(perkColor));
            generatedLines++;
        }
        stack.set(DataComponents.LORE, new ItemLore(lines));
        final int persistedGeneratedLines = generatedLines;
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(DATA_LORE_LINE_COUNT, persistedGeneratedLines));
    }

    private static Component rangeStatLine(String translationKey, double value, double minimum,
                                           double maximum) {
        // Only the actual maximum is gold; a Rune still remaining must not look like Max Roll.
        boolean maxRoll = Math.abs(value - maximum) <= 0.0001;
        ChatFormatting valueColor = maxRoll ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        MutableComponent line = Component.translatable(translationKey).withStyle(valueColor);
        line.append(Component.literal(": +" + formatPercent(value) + "%").withStyle(valueColor));
        // The range is reference information, so keep it gray even when the rolled
        // attribute itself reached its maximum and is highlighted in gold.
        line.append(Component.literal(" [" + formatPercent(minimum) + "% - "
                + formatPercent(maximum) + "%]").withStyle(ChatFormatting.GRAY));
        return line;
    }

    private static List<String> encodePerksWithAmplifiers(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        String encoded = tag.getStringOr(DATA_PERKS, "");
        if (encoded.isBlank()) return List.of();
        return List.of(encoded.split(","));
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; }
    }

    private static String roman(int value) {
        return switch (value) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> Integer.toString(value); };
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        flushPendingReveals(server, now);
        TranscendentAbilities.tick(server);
        PROJECTILE_DATA.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        // Keep Guardian's cooldown across reconnects, but discard expired UUID entries.
        ETERNAL_GUARDIAN_READY_AT.entrySet().removeIf(entry -> entry.getValue() <= now);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updateCelestialFlight(player);
            if (player.tickCount % EFFECT_REFRESH_INTERVAL != 0) continue;
            Map<String, Integer> wanted = collectActiveEffects(player);
            for (Map.Entry<String, Integer> effect : wanted.entrySet()) {
                addEffect(player, effect.getKey(), effect.getValue());
            }
            updateVitality(player);
            updateResilience(player);
        }
    }

    /** Grants vanilla flight only while the Celestial Wings boots are equipped in the FEET slot. */
    private static void updateCelestialFlight(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;

        UUID playerId = player.getUUID();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        boolean hasCelestialWings = getPerkAmplifier(boots, "celestial_wings") >= 0;
        var abilities = player.getAbilities();

        if (hasCelestialWings) {
            CELESTIAL_FLIGHT_PREVIOUS_MAYFLY.putIfAbsent(playerId, abilities.mayfly);
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                player.onUpdateAbilities();
            }
            return;
        }

        restoreCelestialFlight(player);
    }

    /** Restores the player's previous mayfly permission when Celestial Wings is removed. */
    private static void restoreCelestialFlight(ServerPlayer player) {
        Boolean previouslyAllowed = CELESTIAL_FLIGHT_PREVIOUS_MAYFLY.remove(player.getUUID());
        if (previouslyAllowed == null || player.isCreative() || player.isSpectator()) return;

        var abilities = player.getAbilities();
        if (!previouslyAllowed) {
            abilities.flying = false;
            abilities.mayfly = false;
            player.onUpdateAbilities();
        }
    }

    private static Map<String, Integer> collectActiveEffects(ServerPlayer player) {
        Map<String, Integer> effects = new HashMap<>();
        for (ItemStack stack : getActiveEquipment(player)) {
            boolean held = stack == player.getMainHandItem() || stack == player.getOffhandItem();
            for (String encoded : getEncodedPerks(stack)) {
                String[] parts = encoded.split(":", 2);
                if (parts.length != 2) continue;
                String id = parts[0];
                int amplifier = Math.max(0, parseInt(parts[1], 0));
                if (requiresHeldItem(id) && !held) continue;
                Perk perk = perkById(id);
                if (perk != null && !perk.effectId().isBlank()) {
                    int bounded = Math.max(0, Math.min(perk.maximumEffectLevel() - 1, amplifier));
                    effects.merge(perk.effectId(), bounded, Math::max);
                }
                if (id.equals("divine_blessing")) {
                    effects.merge("minecraft:resistance", 1, Math::max);
                    effects.merge("minecraft:regeneration", 2, Math::max);
                } else if (id.equals("celestial_movement")) {
                    effects.merge("minecraft:speed", 1, Math::max);
                } else if (id.equals("aegis_of_ages")) {
                    effects.merge("minecraft:resistance", 2, Math::max);
                    effects.merge("minecraft:regeneration", 1, Math::max);
                }
            }
        }
        // Featherweight only activates while airborne and descending; it is not a permanent slow-falling buff.
        if (hasActivePerk(player, "featherweight") && !player.onGround()
                && player.getDeltaMovement().y < -0.05 && player.fallDistance > 2.5F) {
            effects.merge("minecraft:slow_falling", 0, Math::max);
        }
        return effects;
    }

    private static boolean requiresHeldItem(String id) {
        return id.equals("strength") || id.equals("haste") || id.equals("efficient")
                || id.equals("titan_strength") || id.equals("worldshaper");
    }

    private static void addEffect(ServerPlayer player, String effectId, int amplifier) {
        BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(effectId)).ifPresent(holder -> {
            MobEffectInstance current = player.getEffect(holder);
            if (current == null || current.getAmplifier() < amplifier
                    || (current.getAmplifier() == amplifier && current.getDuration() < EFFECT_REFRESH_INTERVAL * 2)) {
                player.addEffect(new MobEffectInstance(holder, EFFECT_DURATION_TICKS, amplifier, false, false));
            }
        });
    }

    private static void updateVitality(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        int amplifier = getActivePerkAmplifier(player, "vitality");
        double wanted = amplifier < 0 ? 0.0 : (amplifier + 1) * 4.0;
        Double previous = APPLIED_VITALITY.get(player.getUUID());
        if (previous != null && Math.abs(previous - wanted) < 0.001) return;
        attribute.removeModifier(VITALITY_MODIFIER_ID);
        if (wanted > 0.0) attribute.addPermanentModifier(new AttributeModifier(
                VITALITY_MODIFIER_ID, wanted, AttributeModifier.Operation.ADD_VALUE));
        APPLIED_VITALITY.put(player.getUUID(), wanted);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void updateResilience(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ARMOR);
        if (attribute == null) return;
        int amplifier = getActivePerkAmplifier(player, "resilient");
        double wanted = amplifier < 0 ? 0.0 : (amplifier + 1) * 2.0;
        Double previous = APPLIED_RESILIENCE.get(player.getUUID());
        if (previous != null && Math.abs(previous - wanted) < 0.001) return;
        attribute.removeModifier(RESILIENCE_MODIFIER_ID);
        if (wanted > 0.0) attribute.addPermanentModifier(new AttributeModifier(
                RESILIENCE_MODIFIER_ID, wanted, AttributeModifier.Operation.ADD_VALUE));
        APPLIED_RESILIENCE.put(player.getUUID(), wanted);
    }

    private static List<ItemStack> getActiveEquipment(Player player) {
        List<ItemStack> stacks = new ArrayList<>(6);

        // Equippable armor must only activate its perks while it occupies an armor slot.
        // Do not treat a helmet/chestplate/leggings/boots held in either hand as equipped.
        ItemStack mainHand = player.getMainHandItem();
        if (!isArmorItem(mainHand)) stacks.add(mainHand);

        ItemStack offHand = player.getOffhandItem();
        if (!isArmorItem(offHand)) stacks.add(offHand);

        // These are the only slots from which armor perks are considered active.
        stacks.add(player.getItemBySlot(EquipmentSlot.HEAD));
        stacks.add(player.getItemBySlot(EquipmentSlot.CHEST));
        stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
        stacks.add(player.getItemBySlot(EquipmentSlot.FEET));
        return stacks;
    }

    /** True for items that use Minecraft's equippable equipment-slot component. */
    private static boolean isArmorItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return false;
        // Only the four armor slots are excluded from the hand-held list. Offhand
        // equippable items (for example shields or modded offhand gear) must keep
        // their held-item perks active while actually held in the offhand.
        return switch (equippable.slot()) {
            case HEAD, CHEST, LEGS, FEET -> true;
            default -> false;
        };
    }

    private static List<String> getEncodedPerks(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !hasRarity(stack)) return List.of();
        String encoded = getTag(stack).getStringOr(DATA_PERKS, "");
        return encoded.isBlank() ? List.of() : List.of(encoded.split(","));
    }

    private static Perk perkById(String id) {
        return switch (id) {
            case "strength" -> STRENGTH;
            case "speed" -> SPEED;
            case "protection" -> PROTECTION;
            case "resilient" -> RESILIENT;
            case "regeneration" -> REGENERATION;
            case "haste" -> HASTE;
            case "efficient" -> EFFICIENT;
            case "jump_boost" -> JUMP_BOOST;
            case "fire_resistance" -> FIRE_RESISTANCE;
            case "featherweight" -> FEATHERWEIGHT;
            case "vitality" -> VITALITY;
            case "double_leap" -> DOUBLE_LEAP;
            case "windwalker" -> WINDWALKER;
            case "vampiric" -> VAMPIRIC;
            case "critical_mastery" -> CRITICAL_MASTERY;
            case "thunderous" -> THUNDEROUS;
            case "frostbite" -> FROSTBITE;
            case "phoenix" -> PHOENIX;
            case "fortune_mastery" -> FORTUNE_MASTERY;
            case "indestructible" -> INDESTRUCTIBLE;
            case "treasure_hunter" -> TREASURE_HUNTER;
            case "arcane_mastery" -> ARCANE_MASTERY;
            case "godslayer" -> GODSLAYER;
            case "eternal_guardian" -> ETERNAL_GUARDIAN;
            case "runic_edge" -> RUNIC_EDGE;
            case "anvil_ward" -> ANVIL_WARD;
            case "guardian_oath" -> GUARDIAN_OATH;
            case "prospector" -> PROSPECTOR;
            case "titan_strength" -> TITAN_STRENGTH;
            case "divine_blessing" -> DIVINE_BLESSING;
            case "celestial_movement" -> CELESTIAL_MOVEMENT;
            case "aegis_of_ages" -> AEGIS_OF_AGES;
            case "worldshaper" -> WORLD_SHAPER;
            case "celestial_wings" -> CELESTIAL_WINGS;
            case "celestial_range" -> CELESTIAL_RANGE;
            case "oracle_vision" -> ORACLE_VISION;
            case "aegis_of_purity" -> AEGIS_OF_PURITY;
            case "phantom_stride" -> PHANTOM_STRIDE;
            case "divine_reflection" -> DIVINE_REFLECTION;
            case "astral_pursuit" -> ASTRAL_PURSUIT;
            case "singularity_bolt" -> SINGULARITY_BOLT;
            case "poseidons_wrath" -> POSEIDONS_WRATH;
            case "worldbreaker" -> WORLDBREAKER;
            case "buried_truth" -> BURIED_TRUTH;
            case "edens_dominion" -> EDENS_DOMINION;
            case "midas_touch" -> MIDAS_TOUCH;
            case "abyssal_sovereign" -> ABYSSAL_SOVEREIGN;
            default -> null;
        };
    }

    private static int getPerkAmplifier(ItemStack stack, String perkId) {
        for (String encoded : getEncodedPerks(stack)) {
            String[] parts = encoded.split(":", 2);
            if (parts.length == 2 && parts[0].equals(perkId)) return Math.max(0, parseInt(parts[1], 0));
        }
        return -1;
    }

    private static int getActivePerkAmplifier(ServerPlayer player, String perkId) {
        int result = -1;
        for (ItemStack stack : getActiveEquipment(player)) result = Math.max(result, getPerkAmplifier(stack, perkId));
        return result;
    }

    public static boolean hasActivePerk(ServerPlayer player, String perkId) {
        return getActivePerkAmplifier(player, perkId) >= 0;
    }

    public static int getAdditionalDoubleJumps(Player player) {
        if (player == null) return 0;
        int additional = 0;
        for (ItemStack stack : getActiveEquipment(player)) {
            for (String encoded : getEncodedPerks(stack)) {
                String[] parts = encoded.split(":", 2);
                if (parts.length != 2) continue;
                String id = parts[0];
                int effectLevel = Math.max(1, parseInt(parts[1], 0) + 1);
                additional += getAdditionalJumpsForPerk(id, effectLevel);
            }
        }
        return Math.min(8, additional);
    }

    /** Returns the exact extra-jump count represented by a jump affix and its rolled rank. */
    public static int getAdditionalJumpsForPerk(String perkId, int effectLevel) {
        Perk perk = perkById(perkId);
        if (perk == null || perk.extraJumps() <= 0) return 0;
        return perk.extraJumps() + Math.max(0, effectLevel - 1);
    }

    private static boolean isJumpPerk(String perkId) {
        return "double_leap".equals(perkId) || "windwalker".equals(perkId)
                || "celestial_movement".equals(perkId);
    }

    public static double getDamageMultiplier(ItemStack stack) {
        if (!hasRarity(stack)) return 1.0;
        double multiplier = 1.0 + Math.max(0.0, getTag(stack).getDoubleOr(DATA_DAMAGE, 0.0));
        if (getPerkAmplifier(stack, "runic_edge") >= 0) multiplier *= 1.08;
        return multiplier;
    }

    public static double getDamageMultiplier(ItemStack stack, Entity target) {
        CompoundTag tag = getTag(stack);
        // Melee weapons with an attribute modifier already receive their rarity bonus through
        // the vanilla ATTACK_DAMAGE attribute. Only keep independent affix multipliers here.
        // Ranged-only weapons still receive the stored rarity multiplier through getDamageMultiplier(stack).
        double multiplier;
        if (tag.getBooleanOr(DATA_DAMAGE_AS_ATTRIBUTE, false)) {
            // New items store rarity + Runic Edge together in ATTACK_DAMAGE.
            // Older items only stored the rarity bonus there, so preserve their legacy
            // Runic Edge multiplier instead of unexpectedly nerfing equipment already owned.
            boolean includesAffixes = tag.getBooleanOr(DATA_DAMAGE_ATTRIBUTE_INCLUDES_AFFIXES, false);
            multiplier = !includesAffixes && getPerkAmplifier(stack, "runic_edge") >= 0 ? 1.08 : 1.0;
        } else {
            multiplier = getDamageMultiplier(stack);
        }
        if (target instanceof LivingEntity living && isBoss(living)
                && getPerkAmplifier(stack, "godslayer") >= 0) multiplier *= 1.25;
        return multiplier;
    }

    /** Rolls Critical Mastery once for the current melee attack. */
    public static double getEffectiveCriticalChance(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty() || !hasRarity(weapon)) return 0.0;
        double chance = getTag(weapon).getDoubleOr(DATA_CRITICAL_CHANCE, 0.0);
        int mastery = getPerkAmplifier(weapon, "critical_mastery");
        if (mastery >= 0) chance += 0.05 + mastery * 0.05;
        return Math.min(0.60, Math.max(0.0, chance));
    }

    /**
     * Returns the additive Break Speed equipment stat. It stacks with the player's
     * Mining, Woodcutting or Excavating profession bonus and is not a perk slot.
     * The legacy encoded-perk fallback keeps equipment created by the earlier 1.2.2
     * development build functional after this stat migration.
     */
    public static float getBreakSpeedBonus(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0F;
        CompoundTag tag = getTag(stack);
        double storedBonus = tag.getDoubleOr(DATA_BREAK_SPEED, 0.0);
        if (storedBonus > 0.0) return (float) storedBonus;

        String encodedPerks = tag.getStringOr(DATA_PERKS, "");
        int perkStart = encodedPerks.indexOf("gathering_speed:");
        if (perkStart < 0) return 0.0F;

        CraftingRarity rarity = CraftingRarity.byId(tag.getStringOr(DATA_RARITY, ""));
        if (rarity == null) return 0.0F;
        int rankStart = perkStart + "gathering_speed:".length();
        int rankEnd = encodedPerks.indexOf(',', rankStart);
        String encodedRank = rankEnd < 0
                ? encodedPerks.substring(rankStart) : encodedPerks.substring(rankStart, rankEnd);
        int amplifier = parseInt(encodedRank, -1);
        if (amplifier < 0) return 0.0F;

        double minimum;
        double maximum;
        switch (rarity) {
            case COMMON -> { minimum = 0.02; maximum = 0.05; }
            case RARE -> { minimum = 0.04; maximum = 0.08; }
            case UNIQUE -> { minimum = 0.06; maximum = 0.10; }
            case EPIC -> { minimum = 0.08; maximum = 0.12; }
            case LEGENDARY -> { minimum = 0.12; maximum = 0.17; }
            case MYTHIC -> { minimum = 0.17; maximum = 0.23; }
            case TRANSCENDENT -> { minimum = 0.23; maximum = 0.30; }
            default -> { return 0.0F; }
        }
        double rankProgress = Math.min(1.0, Math.max(0.0, amplifier / 3.0));
        return (float) (minimum + (maximum - minimum) * rankProgress);
    }

    /** Returns one critical roll using the sum of equipment and Slaying chances. */
    public static float getCriticalMultiplier(ItemStack weapon, Player attacker, RandomSource random,
                                              boolean includeSlayingChance) {
        if (weapon == null || weapon.isEmpty() || random == null) return 1.0F;
        double equipmentChance = getEffectiveCriticalChance(weapon);
        double slayingChance = includeSlayingChance && attacker != null && weapon.is(ItemTags.SWORDS)
                ? com.github.ob_yekt.simpleskills.Slaying.getCriticalChance(attacker.getUUID()) : 0.0;
        double combinedChance = Math.min(1.0, Math.max(0.0, equipmentChance + slayingChance));
        if (combinedChance <= 0.0 || random.nextDouble() >= combinedChance) return 1.0F;
        return 1.5F;
    }

    /** Stores the weapon's damage multiplier and rolled affixes on fired projectiles. */
    public static void registerProjectileDamageBonus(Projectile projectile, ItemStack weapon) {
        if (projectile == null || weapon == null || weapon.isEmpty() || !hasRarity(weapon)) return;
        long now = projectile.level().getServer() == null
                ? projectile.level().getGameTime()
                : projectile.level().getServer().overworld().getGameTime();
        double multiplier = getDamageMultiplier(weapon);
        if (projectile.getOwner() instanceof ServerPlayer shooter) {
            multiplier *= getCriticalMultiplier(weapon, shooter, shooter.getRandom(), false);
        }
        String encodedPerks = getTag(weapon).getStringOr(DATA_PERKS, "");
        PROJECTILE_DATA.put(projectile.getUUID(), new ProjectileDamageData(
                multiplier, encodedPerks, now + PROJECTILE_BONUS_LIFETIME_TICKS));
        TranscendentAbilities.onProjectileFired(projectile, weapon, encodedPerks);
    }

    public static double getProjectileDamageMultiplier(Projectile projectile) {
        if (projectile == null) return 1.0;
        ProjectileDamageData data = PROJECTILE_DATA.get(projectile.getUUID());
        if (data == null || data.expiresAt() <= currentServerTime(projectile)) return 1.0;
        return data.multiplier();
    }

    public static double getProjectileDamageMultiplier(Projectile projectile, Entity target) {
        double multiplier = getProjectileDamageMultiplier(projectile);
        ProjectileDamageData data = projectile == null ? null : PROJECTILE_DATA.get(projectile.getUUID());
        if (target instanceof LivingEntity living && isBoss(living) && data != null
                && containsPerk(data.encodedPerks(), "godslayer")) multiplier *= 1.25;
        return multiplier;
    }

    private static long currentServerTime(Projectile projectile) {
        return projectile.level().getServer() == null ? projectile.level().getGameTime()
                : projectile.level().getServer().overworld().getGameTime();
    }

    /** Checks a perk on a specific equipped slot; held items never qualify for armor perks. */
    public static boolean hasEquippedPerkInSlot(Player player, EquipmentSlot slot, String perkId) {
        if (player == null || slot == null || perkId == null) return false;
        return getPerkAmplifier(player.getItemBySlot(slot), perkId) >= 0;
    }

    public static int getPerkAmplifierFromStack(ItemStack stack, String perkId) {
        return getPerkAmplifier(stack, perkId);
    }

    public static boolean stackHasPerk(ItemStack stack, String perkId) {
        return getPerkAmplifier(stack, perkId) >= 0;
    }

    public static boolean hasActivePerk(Player player, String perkId) {
        if (player == null || perkId == null) return false;
        for (ItemStack stack : getActiveEquipment(player)) {
            if (getPerkAmplifier(stack, perkId) >= 0) return true;
        }
        return false;
    }

    /** Server-authoritative cleanup for temporary abilities owned by a disconnecting player. */
    public static void onPlayerDisconnect(ServerPlayer player) {
        if (player == null) return;
        restoreCelestialFlight(player);
        APPLIED_VITALITY.remove(player.getUUID());
        APPLIED_RESILIENCE.remove(player.getUUID());
        TranscendentAbilities.clearPlayer(player.getUUID());
    }

    public static boolean hasRarity(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !getRarityId(stack).isBlank();
    }

    public static String getRarityId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return getTag(stack).getStringOr(DATA_RARITY, "");
    }

    public static boolean shouldPreventDurabilityLoss(ItemStack stack, ServerPlayer player) {
        if (stack == null || stack.isEmpty() || player == null) return false;
        int level = getPerkAmplifier(stack, "indestructible");
        if (level < 0) return false;
        double chance = Math.min(0.45, 0.10 + level * 0.10);
        return player.getRandom().nextDouble() < chance;
    }

    public static double getBonusDropChance(ItemStack stack, String perkId) {
        // Fortune Mastery is a pickaxe-only affix. This guard also disables the bonus
        // on older equipment that may already have rolled the perk before this restriction.
        if ("fortune_mastery".equals(perkId) && classify(stack) != EquipmentKind.PICKAXE) return 0.0;
        int level = getPerkAmplifier(stack, perkId);
        if (level < 0) return 0.0;
        double chance = Math.min(0.30, 0.06 + level * 0.06);
        if (perkId.equals("worldshaper")) chance = Math.max(chance, 0.15);
        return chance;
    }

    public static double getTreasureHunterChance(ItemStack stack) {
        // Treasure Hunter belongs exclusively to shovel equipment. This also prevents
        // legacy or manually edited non-shovel items from activating the excavation perk.
        if (classify(stack) != EquipmentKind.SHOVEL) return 0.0;
        return getBonusDropChance(stack, "treasure_hunter");
    }

    private static boolean containsPerk(String encodedPerks, String perkId) {
        if (encodedPerks == null || encodedPerks.isBlank()) return false;
        for (String encoded : encodedPerks.split(",")) {
            if (encoded.startsWith(perkId + ":")) return true;
        }
        return false;
    }

    private static boolean isBoss(LivingEntity target) {
        // Resolve the registry path instead of referring to EntityType constants whose
        // names differ between Minecraft mapping generations.
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
        return entityId.equals("ender_dragon") || entityId.equals("wither") || entityId.equals("warden");
    }

    private static void afterDamage(LivingEntity victim, DamageSource source, float originalAmount,
                                    float actualAmount, boolean blocked) {
        if (victim == null || victim.level().isClientSide() || actualAmount <= 0.0F
                || BONUS_DAMAGE_GUARD.contains(victim.getUUID())) return;

        ServerPlayer attacker = null;
        String encodedPerks = "";
        if (source.getDirectEntity() instanceof Projectile projectile) {
            if (source.getEntity() instanceof ServerPlayer player) attacker = player;
            ProjectileDamageData data = PROJECTILE_DATA.get(projectile.getUUID());
            if (data != null && data.expiresAt() > currentServerTime(projectile)) {
                encodedPerks = data.encodedPerks();
            } else if (projectile instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident trident) {
                if (attacker == null && trident.getOwner() instanceof ServerPlayer owner) attacker = owner;
                encodedPerks = getTag(trident.getWeaponItem()).getStringOr(DATA_PERKS, "");
            }
        } else if (source.getEntity() instanceof ServerPlayer player) {
            attacker = player;
            encodedPerks = getTag(player.getMainHandItem()).getStringOr(DATA_PERKS, "");
        }
        if (attacker == null || encodedPerks.isBlank()) return;

        TranscendentAbilities.onWeaponHit(attacker, victim, encodedPerks, source.getDirectEntity());
        int vampiric = perkAmplifier(encodedPerks, "vampiric");
        if (vampiric >= 0) {
            float heal = Math.min(3.0F, actualAmount * (0.08F + vampiric * 0.04F));
            if (heal > 0.0F) attacker.heal(heal);
        }

        int frostbite = perkAmplifier(encodedPerks, "frostbite");
        if (frostbite >= 0 && attacker.getRandom().nextFloat() < 0.18F + frostbite * 0.08F) {
            addEffectToLiving(victim, "minecraft:slowness", frostbite, 60 + frostbite * 30);
        }

        int phoenix = perkAmplifier(encodedPerks, "phoenix");
        if (phoenix >= 0 && attacker.getRandom().nextFloat() < 0.15F + phoenix * 0.10F) {
            victim.igniteForSeconds(2.0F + phoenix * 1.5F);
        }

        int thunderous = perkAmplifier(encodedPerks, "thunderous");
        if (thunderous >= 0 && victim.isAlive()
                && attacker.getRandom().nextFloat() < 0.10F + thunderous * 0.07F
                && victim.level() instanceof ServerLevel serverLevel) {
            strikeThunderousLightning(serverLevel, attacker, victim, thunderous);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                    18, 0.3, 0.45, 0.3, 0.10);
        }

    }


    /** Spawns a real, server-side vanilla lightning bolt at the struck target. */
    private static void strikeThunderousLightning(ServerLevel level, ServerPlayer attacker,
                                                   LivingEntity victim, int perkAmplifier) {
        // In this Fabric/Minecraft version, the lightning entity type is resolved from
        // the registry and LightningBolt uses its native vanilla damage value.
        EntityType<?> lightningType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.parse("minecraft:lightning_bolt"));
        if (lightningType == null) return;

        Entity created = lightningType.create(level, EntitySpawnReason.TRIGGERED);
        if (!(created instanceof LightningBolt lightning)) return;

        lightning.setCause(attacker);
        lightning.setVisualOnly(false);
        lightning.setPos(victim.getX(), victim.getY(), victim.getZ());
        level.addFreshEntity(lightning);
    }

    private static int perkAmplifier(String encodedPerks, String perkId) {
        if (encodedPerks == null || encodedPerks.isBlank()) return -1;
        for (String encoded : encodedPerks.split(",")) {
            String[] parts = encoded.split(":", 2);
            if (parts.length == 2 && parts[0].equals(perkId)) return Math.max(0, parseInt(parts[1], 0));
        }
        return -1;
    }

    private static void applyBonusDamage(LivingEntity victim, ServerPlayer attacker, float damage) {
        if (damage <= 0.0F || !victim.isAlive() || !BONUS_DAMAGE_GUARD.add(victim.getUUID())) return;
        try {
            // Thunderous is a separate affix hit. Temporarily clear the vanilla hurt cooldown
            // so the extra damage is not silently discarded by the original attack's i-frames.
            LivingEntityDamageCooldownAccessor accessor = (LivingEntityDamageCooldownAccessor) victim;
            int previousInvulnerability = accessor.simpleskills$getDamageCooldownTime();
            accessor.simpleskills$setDamageCooldownTime(0);
            try {
                victim.hurt(attacker.damageSources().playerAttack(attacker), damage);
            } finally {
                accessor.simpleskills$setDamageCooldownTime(previousInvulnerability);
            }
        } finally {
            BONUS_DAMAGE_GUARD.remove(victim.getUUID());
        }
    }

    private static void addEffectToLiving(LivingEntity entity, String effectId, int amplifier, int duration) {
        BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(effectId)).ifPresent(holder ->
                entity.addEffect(new MobEffectInstance(holder, duration, amplifier, false, true)));
    }

    private static boolean preventLethalDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player) || amount < player.getHealth() + player.getAbsorptionAmount()) return true;
        int guardian = getActivePerkAmplifier(player, "eternal_guardian");
        if (guardian < 0) return true;
        long now = player.level().getServer() == null ? player.level().getGameTime()
                : player.level().getServer().overworld().getGameTime();
        if (ETERNAL_GUARDIAN_READY_AT.getOrDefault(player.getUUID(), 0L) > now) return true;
        double chance = 0.10 + guardian * 0.04;
        if (player.getRandom().nextDouble() >= chance) return true;

        ETERNAL_GUARDIAN_READY_AT.put(player.getUUID(), now + 6000L);
        player.setHealth(1.0F);
        addEffect(player, "minecraft:regeneration", 2);
        addEffect(player, "minecraft:resistance", 1);
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0,
                player.getZ(), 35, 0.45, 0.7, 0.45, 0.12);
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.simpleskills.crafting.eternal_guardian").withStyle(ChatFormatting.GOLD));
        return false;
    }

    private static CompoundTag getTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }
}
