package com.github.ob_yekt.simpleskills.crafting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.mixin.LivingEntityDamageCooldownAccessor;
import com.github.ob_yekt.simpleskills.shields.GlowstoneShields;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
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
import net.minecraft.core.registries.Registries;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/**
 * Server-authoritative crafting rarity and random-affix system.
 * Rarity, stats and perks are stored on the item; active effects are derived from equipment.
 */
public final class CraftingEquipmentManager {
    private static final TagKey<Block> COMMON_ORE_BLOCKS = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final String DATA_RARITY = "simpleskills_crafting_rarity";
    private static final String DATA_ABYSSAL_LOOT_RARITY = "simpleskills_abyss_loot_rarity";
    private static final String DATA_DAMAGE = "simpleskills_crafting_damage_bonus";
    private static final String DATA_DAMAGE_AS_ATTRIBUTE = "simpleskills_crafting_damage_as_attribute";
    private static final String DATA_DAMAGE_ATTRIBUTE_INCLUDES_AFFIXES =
            "simpleskills_crafting_damage_attribute_includes_affixes";
    private static final String DATA_DURABILITY = "simpleskills_crafting_durability_bonus";
    private static final String DATA_BREAK_SPEED = "simpleskills_crafting_break_speed_bonus";
    private static final String DATA_EXCAVATION_TIME = "simpleskills_crafting_excavation_time_reduction";
    private static final String DATA_CRITICAL_CHANCE = "simpleskills_crafting_critical_chance";
    private static final String DATA_ATTACK_SPEED = "simpleskills_crafting_attack_speed_bonus";
    private static final String DATA_ARMOR = "simpleskills_crafting_armor_bonus";
    private static final String DATA_TOUGHNESS = "simpleskills_crafting_toughness_bonus";
    private static final String DATA_ATTACK_REACH = "simpleskills_crafting_attack_reach";
    private static final String DATA_ARMOR_PENETRATION = "simpleskills_crafting_armor_penetration";
    private static final String DATA_COMBAT_FRENZY = "simpleskills_crafting_combat_frenzy";
    private static final String DATA_FIRST_STRIKE = "simpleskills_crafting_first_strike";
    private static final String DATA_RHYTHMIC_DISCHARGE = "simpleskills_crafting_rhythmic_discharge";
    private static final String DATA_TENACITY = "simpleskills_crafting_tenacity";
    private static final String DATA_MOBILITY = "simpleskills_crafting_mobility";
    private static final String DATA_MOBILITY_TYPE = "simpleskills_crafting_mobility_type";
    private static final String DATA_ELEMENTAL_RESISTANCE = "simpleskills_crafting_elemental_resistance";
    private static final String DATA_ELEMENTAL_TYPE = "simpleskills_crafting_elemental_type";
    private static final String DATA_PERKS = "simpleskills_crafting_perks";
    private static final String DATA_ATTRIBUTE_ORDER = "simpleskills_crafting_attribute_order";
    private static final String DATA_BASE_NAME = "simpleskills_crafting_base_name";
    private static final String DATA_BASE_MAX_DAMAGE = "simpleskills_crafting_base_max_damage";
    private static final String DATA_LORE_LINE_COUNT = "simpleskills_crafting_lore_line_count";
    private static final String DATA_LORE_SCHEMA_VERSION = "simpleskills_crafting_lore_schema_version";
    private static final Pattern REQUIREMENT_LORE_LINE = Pattern.compile("^Requires \\d+ .+ to use$");
    private static final String DATA_ENHANCEMENT_REVISION = "simpleskills_crafting_enhancement_revision";
    private static final int CURRENT_CRAFTING_LORE_SCHEMA = 2;
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
    private static final Identifier TENACITY_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_tenacity");
    private static final Identifier MOBILITY_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_mobility_speed");
    private static final Identifier MOBILITY_JUMP_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_mobility_jump");
    private static final Identifier MOBILITY_WATER_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_mobility_water");
    private static final Identifier BLOCK_REACH_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_block_reach");
    private static final Identifier COMBAT_FRENZY_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_combat_frenzy");

    private static final Map<UUID, ProjectileDamageData> PROJECTILE_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ETERNAL_GUARDIAN_READY_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> APPLIED_VITALITY = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> APPLIED_RESILIENCE = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> FREEZE_TENACITY_REMAINDER = new ConcurrentHashMap<>();
    private static final Map<UUID, FrenzyState> COMBAT_FRENZY = new ConcurrentHashMap<>();
    private static final Map<UUID, ComboState> RHYTHMIC_COMBOS = new ConcurrentHashMap<>();
    private static final Map<String, InertiaState> EXCAVATION_INERTIA_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, VeinResonanceState> VEIN_RESONANCE_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CELESTIAL_FLIGHT_PREVIOUS_MAYFLY = new ConcurrentHashMap<>();
    private static final Set<UUID> BONUS_DAMAGE_GUARD = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, PendingRevealBatch> PENDING_REVEALS = new ConcurrentHashMap<>();
    private static final int REVEAL_BATCH_IDLE_TICKS = 2;
    private static final int MAX_REVEALS_PER_PACKET = 24;

    private static final class PendingRevealBatch {
        private final JsonArray entries = new JsonArray();
        private long lastQueuedAt;
    }

    private record ProjectileDamageData(double multiplier, double firstStrikeBonus, double armorPenetration,
                                         String encodedPerks, long expiresAt) {}
    private record FrenzyState(int stacks, long expiresAt, double perStackBonus) {}
    private record ComboState(UUID target, int hits, long lastTick) {}
    private static final class InertiaState {
        private int lastTick = Integer.MIN_VALUE;
        private int lastStateHash;
        private int continuousTicks;
        private float lastMultiplier = 1.0F;
    }
    private record VeinResonanceState(List<net.minecraft.core.BlockPos> ores, long expiresAt, int amplifier) {}
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
    // Basic jump affix: one to three additional jumps.
    private static final Perk DOUBLE_LEAP = jump("double_leap", 1, 3);
    // Kept for items created by older development builds; no longer rolled on new equipment.
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
    private static final Perk ETERNAL_GUARDIAN = passive("eternal_guardian", 4);
    private static final Perk EXCAVATION_INERTIA = passive("excavation_inertia", 5);
    private static final Perk EXTENDED_REACH = passive("extended_reach", 5);
    private static final Perk VEIN_RESONANCE = passive("vein_resonance", 3);

    // Unique-only affixes. Exactly one is guaranteed on every Unique item.
    private static final Perk RUNIC_EDGE = passive("runic_edge", 1);
    private static final Perk ANVIL_WARD = effect("anvil_ward", "minecraft:resistance", 1);
    private static final Perk GUARDIAN_OATH = effect("guardian_oath", "minecraft:resistance", 1);
    private static final Perk PROSPECTOR = passive("prospector", 3);

    // Transcendent-only affixes. At least one is guaranteed on every Transcendent item.
    private static final Perk TITAN_STRENGTH = effect("titan_strength", "minecraft:strength", 4);
    private static final Perk DIVINE_BLESSING = passive("divine_blessing", 2);
    // Rare Transcendent mobility affix: four to six additional jumps.
    private static final Perk CELESTIAL_MOVEMENT = jump("celestial_movement", 4, 3);
    private static final Perk AEGIS_OF_AGES = passive("aegis_of_ages", 4);
    private static final Perk WORLD_SHAPER = effect("worldshaper", "minecraft:haste", 4);
    private static final Perk THERMAL_SMELTING = passive("thermal_smelting", 0);
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
            "guardian_oath", "arcane_mastery", "godslayer",
            "divine_blessing", "fire_resistance", "featherweight", "thermal_smelting",
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
            FREEZE_TENACITY_REMAINDER.remove(id);
            COMBAT_FRENZY.remove(id);
            RHYTHMIC_COMBOS.remove(id);
            EXCAVATION_INERTIA_STATES.keySet().removeIf(key -> key.startsWith(id.toString() + ":"));
            VEIN_RESONANCE_STATES.remove(id);
            AttributeInstance attackSpeed = handler.player.getAttribute(Attributes.ATTACK_SPEED);
            if (attackSpeed != null) attackSpeed.removeModifier(COMBAT_FRENZY_MODIFIER_ID);
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
            return currentPercent >= maximumPercent - 0.000001;
        }
    }

    /** A currently rolled perk, with whether it has a legal replacement in its own perk pool. */
    public record PerkRerollInfo(String id, int amplifier, boolean signatureAbility,
                                 boolean rerollable, int index) {
        public String translationKey() { return "item.simpleskills.crafting_perk." + id; }

        /** Stable target for the Altar: perk position + exact rolled entry, never a display-name sort key. */
        public String actionTargetId() {
            return "perk:" + index + ":" + id + ":" + amplifier;
        }
    }

    /** Revision of the equipment state used to reject stale/delayed Altar actions. */
    public static int getEnhancementRevision(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        return getTag(stack).getIntOr(DATA_ENHANCEMENT_REVISION, 0);
    }

    /** Advances the revision after one successful, atomic Altar mutation. */
    private static void markEnhancementMutation(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            int current = data.getIntOr(DATA_ENHANCEMENT_REVISION, 0);
            data.putInt(DATA_ENHANCEMENT_REVISION, current == Integer.MAX_VALUE ? 1 : current + 1);
        });
    }

    /** Builds the Transcendent general-affix pool, including its rare high-tier mobility perk. */
    private static List<Perk> transcendentGeneralPool(EquipmentKind kind) {
        List<Perk> pool = commonPool(kind);
        if (kind == EquipmentKind.HELMET || kind == EquipmentKind.CHESTPLATE
                || kind == EquipmentKind.LEGGINGS || kind == EquipmentKind.BOOTS
                || kind == EquipmentKind.ARMOR || kind == EquipmentKind.SHIELD) {
            pool.removeIf(perk -> perk.id().equals("double_leap") || perk.id().equals("windwalker"));
            pool.add(CELESTIAL_MOVEMENT);
        }
        return pool;
    }

    private static List<Perk> perkPoolForRarity(CraftingRarity rarity, EquipmentKind kind, String perkId) {
        if (rarity == CraftingRarity.TRANSCENDENT) {
            boolean signature = transcendentPool(kind).stream().anyMatch(perk -> perk.id().equals(perkId));
            return signature ? transcendentPool(kind) : transcendentGeneralPool(kind);
        }
        return rarity == CraftingRarity.UNIQUE ? uniquePool(kind) : commonPool(kind);
    }

    /** Returns every currently rolled perk and marks whether it has a legal replacement. */
    public static List<PerkRerollInfo> getEnhanceablePerks(ItemStack stack) {
        if (!isEligibleForCelestialAltar(stack)) return List.of();
        ensureCraftingTooltipCurrent(stack);
        CraftingRarity rarity = CraftingRarity.byId(getRarityId(stack));
        if (rarity == null) return List.of();

        EquipmentKind kind = classify(stack);
        List<String> encodedPerks = getEncodedPerks(stack);
        List<PerkRerollInfo> result = new ArrayList<>();
        // Iterate the serialized equipment list by index. This order is the player's rolled
        // order and is deliberately preserved in both the tooltip and the Altar; never sort by name.
        for (int index = 0; index < encodedPerks.size(); index++) {
            String encoded = encodedPerks.get(index);
            String[] parts = encoded.split(":", 2);
            if (parts.length == 0 || parts[0].isBlank()) continue;
            String perkId = parts[0];
            int amplifier = parts.length > 1 ? Math.max(0, parseInt(parts[1], 0)) : 0;
            Perk currentPerk = perkById(perkId);
            // Keep historical/unknown perk IDs visible in the Altar even when their
            // implementation is no longer part of the current reroll pools.
            if (currentPerk == null) {
                result.add(new PerkRerollInfo(perkId, amplifier, false, false, index));
                continue;
            }

            boolean signature = rarity == CraftingRarity.TRANSCENDENT
                    && transcendentPool(kind).stream().anyMatch(perk -> perk.id().equals(perkId));
            List<Perk> pool = perkPoolForRarity(rarity, kind, perkId);
            boolean rerollable = hasAlternativePerk(pool, encodedPerks, currentPerk);
            result.add(new PerkRerollInfo(perkId, amplifier, signature, rerollable, index));
        }
        // Explicitly retain the original CustomData sequence. The Altar and item tooltip
        // both use this order; sorting by translated perk name would make a rerolled perk
        // appear to jump to another position even though its serialized slot did not move.
        result.sort(java.util.Comparator.comparingInt(PerkRerollInfo::index));
        return List.copyOf(result);
    }

    /** Returns only perks that can be replaced without changing any of the item's other perks. */
    public static List<PerkRerollInfo> getRerollablePerks(ItemStack stack) {
        return getEnhanceablePerks(stack).stream().filter(PerkRerollInfo::rerollable).toList();
    }

    public static boolean canRerollPerk(ItemStack stack, String perkId) {
        if (perkId == null || perkId.isBlank()) return false;
        List<PerkRerollInfo> matches = getRerollablePerks(stack).stream()
                .filter(info -> info.id().equals(perkId)).toList();
        // Legacy callers identify a perk only by ID. Refuse ambiguous matches rather than
        // silently rerolling the first one; the Altar uses the stable indexed target below.
        return matches.size() == 1
                && canRerollPerkAt(stack, matches.getFirst().index(),
                matches.getFirst().id() + ":" + matches.getFirst().amplifier());
    }

    public static boolean canRerollPerkAt(ItemStack stack, int perkIndex, String expectedEntry) {
        if (!isEligibleForCelestialAltar(stack) || expectedEntry == null || expectedEntry.isBlank()) return false;
        List<String> encodedPerks = getEncodedPerks(stack);
        if (perkIndex < 0 || perkIndex >= encodedPerks.size()
                || !encodedPerks.get(perkIndex).equals(expectedEntry)) return false;
        String[] parts = expectedEntry.split(":", 2);
        if (parts.length != 2) return false;
        Perk currentPerk = perkById(parts[0]);
        if (currentPerk == null) return false;
        CraftingRarity rarity = CraftingRarity.byId(getRarityId(stack));
        if (rarity == null) return false;
        EquipmentKind kind = classify(stack);
        return hasAlternativePerk(perkPoolForRarity(rarity, kind, currentPerk.id()),
                encodedPerks, currentPerk);
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
        if (id.equals("eternal_guardian")) {
            return Component.translatable(key, 10 + amplifier * 5,
                    roman(Math.min(5, 3 + amplifier)), roman(Math.min(5, 2 + amplifier)));
        }
        if (id.equals("aegis_of_ages")) {
            return Component.translatable(key, roman(amplifier + 1),
                    roman(Math.min(5, 3 + amplifier)), roman(Math.min(4, 2 + amplifier)));
        }
        if (NO_LEVEL_PERKS.contains(id)) return Component.translatable(key);
        return Component.translatable(key, roman(amplifier + 1));
    }

    private static Set<String> grantedEffectIds(Perk perk) {
        if (perk == null) return Set.of();
        Set<String> effects = new HashSet<>();
        if (!perk.effectId().isBlank()) effects.add(perk.effectId());
        // Some passive affixes grant potion effects without being represented by a single Effect perk.
        switch (perk.id()) {
            case "celestial_movement" -> effects.add("minecraft:speed");
            case "aegis_of_ages", "divine_blessing", "eternal_guardian" -> {
                effects.add("minecraft:resistance");
                effects.add("minecraft:regeneration");
            }
            default -> { }
        }
        return effects;
    }

    private static boolean hasAlternativePerk(List<Perk> pool, List<String> encodedPerks, Perk currentPerk) {
        Set<String> occupiedIds = new HashSet<>();
        Set<String> occupiedEffects = new HashSet<>();
        for (String encoded : encodedPerks) {
            String[] parts = encoded.split(":", 2);
            if (parts.length != 2 || parts[0].equals(currentPerk.id())) continue;
            occupiedIds.add(parts[0]);
            Perk existing = perkById(parts[0]);
            occupiedEffects.addAll(grantedEffectIds(existing));
        }

        for (Perk candidate : pool) {
            if (candidate == null || candidate.id().equals(currentPerk.id())
                    || occupiedIds.contains(candidate.id())) continue;
            Set<String> candidateEffects = grantedEffectIds(candidate);
            if (candidateEffects.stream().anyMatch(occupiedEffects::contains)) continue;
            return true;
        }
        return false;
    }

    /**
     * Legacy ID-only entry point. It is safe only when the ID occurs exactly once;
     * ambiguous entries are rejected instead of rerolling an arbitrary occurrence.
     */
    public static boolean rerollEquipmentPerk(ItemStack stack, String perkId, ServerPlayer player) {
        if (perkId == null || perkId.isBlank()) return false;
        List<String> entries = getEncodedPerks(stack);
        int found = -1;
        for (int i = 0; i < entries.size(); i++) {
            String[] parts = entries.get(i).split(":", 2);
            if (parts.length == 2 && parts[0].equals(perkId)) {
                if (found >= 0) return false;
                found = i;
            }
        }
        return found >= 0 && rerollEquipmentPerkAt(stack, found, entries.get(found), player);
    }

    /** Replaces exactly the selected serialized perk entry, keeping every other entry in its original position. */
    public static boolean rerollEquipmentPerkAt(ItemStack stack, int targetIndex,
                                                String expectedEntry, ServerPlayer player) {
        if (!isEligibleForCelestialAltar(stack) || player == null
                || !canRerollPerkAt(stack, targetIndex, expectedEntry)) return false;

        CraftingRarity rarity = CraftingRarity.byId(getRarityId(stack));
        if (rarity == null) return false;
        List<String> encodedPerks = new ArrayList<>(getEncodedPerks(stack));
        // Keep a before-snapshot so this operation can only replace one serialized slot.
        List<String> originalOrder = List.copyOf(encodedPerks);
        String[] selectedParts = expectedEntry.split(":", 2);
        String perkId = selectedParts[0];
        Perk currentPerk = perkById(perkId);
        if (currentPerk == null) return false;

        EquipmentKind kind = classify(stack);
        List<Perk> pool = perkPoolForRarity(rarity, kind, perkId);
        Set<String> occupiedIds = new HashSet<>();
        Set<String> occupiedEffects = new HashSet<>();
        for (int i = 0; i < encodedPerks.size(); i++) {
            if (i == targetIndex) continue;
            String[] parts = encodedPerks.get(i).split(":", 2);
            if (parts.length != 2) continue;
            occupiedIds.add(parts[0]);
            occupiedEffects.addAll(grantedEffectIds(perkById(parts[0])));
        }

        List<Perk> candidates = new ArrayList<>();
        for (Perk candidate : pool) {
            if (candidate == null || candidate.id().equals(perkId) || occupiedIds.contains(candidate.id())) continue;
            Set<String> candidateEffects = grantedEffectIds(candidate);
            if (candidateEffects.stream().anyMatch(occupiedEffects::contains)) continue;
            candidates.add(candidate);
        }
        if (candidates.isEmpty()) return false;

        Perk replacement = candidates.get(player.getRandom().nextInt(candidates.size()));
        String replacementEncoding = encodePerks(List.of(replacement), getCraftingLevel(player), rarity, player);
        if (replacementEncoding.isBlank()) return false;

        // Replace the entry in-place. Do not alphabetize or rebuild the list from a Set;
        // the remaining perks retain the exact order already stored on the equipment.
        encodedPerks.set(targetIndex, replacementEncoding);
        for (int i = 0; i < encodedPerks.size(); i++) {
            if (i != targetIndex && !encodedPerks.get(i).equals(originalOrder.get(i))) {
                return false;
            }
        }
        String persistedPerks = String.join(",", encodedPerks);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putString(DATA_PERKS, persistedPerks));
        refreshCraftingAttributes(stack);
        refreshCraftingTooltip(stack);
        markEnhancementMutation(stack);
        return true;
    }

    /**
     * Returns the attributes already rolled on an item. Attributes which were not generated
     * for the equipment category are not offered by the altar.
     */
    public static List<StatUpgradeInfo> getEnhanceableStats(ItemStack stack) {
        if (!isEligibleForCelestialAltar(stack)) return List.of();
        ensureCraftingTooltipCurrent(stack);
        CraftingRarity rarity = CraftingRarity.byId(getRarityId(stack));
        if (rarity == null) return List.of();

        CompoundTag tag = getTag(stack);
        List<StatUpgradeInfo> result = new ArrayList<>();
        boolean runicEdge = getPerkAmplifier(stack, "runic_edge") >= 0;
        addUpgradeInfo(result, tag, "damage", "item.simpleskills.crafting_stat.damage",
                rarity.minDamageBonus(), rarity.maxDamageBonus(), runicEdge);
        addUpgradeInfo(result, tag, "durability", "item.simpleskills.crafting_stat.durability",
                rarity.minDurabilityBonus(), rarity.maxDurabilityBonus(), false);
        addUpgradeInfo(result, tag, "breakSpeed", "item.simpleskills.crafting_stat.break_speed",
                rarity.minBreakSpeedBonus(), rarity.maxBreakSpeedBonus(), false);
        if (classify(stack) == EquipmentKind.SHOVEL) {
            addUpgradeInfo(result, tag, "excavationTime", "item.simpleskills.crafting_stat.excavation_time",
                    rarity.minBreakSpeedBonus(), rarity.maxBreakSpeedBonus(), false);
        }
        addCriticalUpgradeInfo(result, stack, tag, rarity);
        addUpgradeInfo(result, tag, "attackSpeed", "item.simpleskills.crafting_stat.attack_speed",
                rarity.minAttackSpeedBonus(), rarity.maxAttackSpeedBonus(), false);
        addUpgradeInfo(result, tag, "armor", "item.simpleskills.crafting_stat.armor",
                rarity.minArmorBonus(), rarity.maxArmorBonus(), false);
        addUpgradeInfo(result, tag, "toughness", "item.simpleskills.crafting_stat.armor_toughness",
                rarity.minToughnessBonus(), rarity.maxToughnessBonus(), false);

        // Keep the Altar rows in exactly the same order as the generated item tooltip.
        EquipmentKind kind = classify(stack);
        if (kind == EquipmentKind.SWORD || kind == EquipmentKind.AXE
                || kind == EquipmentKind.MACE || kind == EquipmentKind.TRIDENT) {
            addRangeUpgradeInfo(result, tag, "armorPenetration", "item.simpleskills.crafting_stat.armor_penetration", rarity);
            addRangeUpgradeInfo(result, tag, "combatFrenzy", "item.simpleskills.crafting_stat.combat_frenzy", rarity);
            addRangeUpgradeInfo(result, tag, "firstStrike", "item.simpleskills.crafting_stat.first_strike", rarity);
            addRangeUpgradeInfo(result, tag, "rhythmicDischarge", "item.simpleskills.crafting_stat.rhythmic_discharge", rarity);
        }
        if (kind == EquipmentKind.BOW || kind == EquipmentKind.CROSSBOW) {
            addRangeUpgradeInfo(result, tag, "firstStrike", "item.simpleskills.crafting_stat.first_strike", rarity);
        }
        if (kind == EquipmentKind.ARMOR || kind == EquipmentKind.HELMET
                || kind == EquipmentKind.CHESTPLATE || kind == EquipmentKind.LEGGINGS
                || kind == EquipmentKind.BOOTS || kind == EquipmentKind.SHIELD) {
            addRangeUpgradeInfo(result, tag, "tenacity", "item.simpleskills.crafting_stat.tenacity", rarity);
            if (kind == EquipmentKind.LEGGINGS || kind == EquipmentKind.BOOTS) {
                addRangeUpgradeInfo(result, tag, "mobility",
                        "item.simpleskills.crafting_stat.mobility_" + tag.getStringOr(DATA_MOBILITY_TYPE, "walk"), rarity);
            }
        }
        if (kind == EquipmentKind.SWORD || kind == EquipmentKind.AXE
                || kind == EquipmentKind.MACE || kind == EquipmentKind.TRIDENT) {
            addRangeUpgradeInfo(result, tag, "attackReach", "item.simpleskills.crafting_stat.attack_reach", rarity);
        }
        if (kind == EquipmentKind.ARMOR || kind == EquipmentKind.HELMET
                || kind == EquipmentKind.CHESTPLATE || kind == EquipmentKind.LEGGINGS
                || kind == EquipmentKind.BOOTS || kind == EquipmentKind.SHIELD) {
            addRangeUpgradeInfo(result, tag, "elementalResistance",
                    elementalResistanceTranslationKey(tag.getStringOr(DATA_ELEMENTAL_TYPE, "fire")), rarity);
        }
        orderStatUpgradeInfo(tag, result);
        return List.copyOf(result);
    }

    /**
     * Applies the item's persisted randomized attribute order to the Altar list.
     * Older items without this field retain the legacy category-aware display order;
     * any newly added/replaced attribute is appended only as a safe migration fallback.
     */
    private static void orderStatUpgradeInfo(CompoundTag tag, List<StatUpgradeInfo> stats) {
        if (stats.size() < 2) return;
        List<String> fallback = stats.stream().map(StatUpgradeInfo::id).toList();
        List<String> order = resolveAttributeOrder(tag, fallback);
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < order.size(); i++) positions.put(order.get(i), i);
        stats.sort(java.util.Comparator.comparingInt(info -> positions.getOrDefault(info.id(), Integer.MAX_VALUE)));
    }

    /**
     * Returns a stable attribute order containing only attributes currently present.
     * The fallback list is already in the legacy display order and also migrates old gear.
     */
    private static List<String> resolveAttributeOrder(CompoundTag tag, List<String> fallbackOrder) {
        List<String> result = new ArrayList<>();
        Set<String> available = new HashSet<>(fallbackOrder);
        String encoded = tag.getStringOr(DATA_ATTRIBUTE_ORDER, "");
        if (!encoded.isBlank()) {
            for (String id : encoded.split(",")) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty() && available.contains(trimmed) && !result.contains(trimmed)) {
                    result.add(trimmed);
                }
            }
        }
        for (String id : fallbackOrder) {
            if (!result.contains(id)) result.add(id);
        }
        return List.copyOf(result);
    }

    /** Keeps the altar's Critical Chance value and range identical to the tooltip. */
    private static void addCriticalUpgradeInfo(List<StatUpgradeInfo> result, ItemStack stack,
                                               CompoundTag tag, CraftingRarity rarity) {
        // Critical Mastery can grant an effective chance by itself. That is a perk effect,
        // not a rolled attribute, so it must not create an extra green attribute line or
        // count against the rarity's attribute limit when no critical stat was rolled.
        double rolledCritical = tag.getDoubleOr(DATA_CRITICAL_CHANCE, 0.0);
        if (rolledCritical <= 0.0) return;

        int mastery = getPerkAmplifier(stack, "critical_mastery");
        double masteryBonus = mastery >= 0 ? 0.05 + mastery * 0.05 : 0.0;
        double current = getEffectiveCriticalChance(stack) * 100.0;
        double minimum = Math.min(0.60, rarity.minCriticalChance() + masteryBonus) * 100.0;
        double maximum = Math.min(0.60, rarity.maxCriticalChance() + masteryBonus) * 100.0;
        result.add(new StatUpgradeInfo("critical", "item.simpleskills.crafting_stat.critical_chance",
                current, minimum, maximum));
    }

    private static void addUpgradeInfo(List<StatUpgradeInfo> result, CompoundTag tag, String field,
                                       String translationKey, double minimum, double maximum,
                                       boolean runicEdge) {
        addUpgradeInfo(result, tag, field, translationKey, minimum, maximum,
                runicEdge ? 1.08 : 1.0, 0.0);
    }

    private static void addRangeUpgradeInfo(List<StatUpgradeInfo> result, CompoundTag tag,
                                            String field, String translationKey, CraftingRarity rarity) {
        double[] bounds = statRange(rarity, field);
        if (bounds == null) return;
        double current = tag.getDoubleOr(statDataKey(field), 0.0);
        if (current > 0.0) {
            result.add(new StatUpgradeInfo(field, translationKey, current * (field.equals("attackReach") ? 1.0 : 100.0),
                    bounds[0] * (field.equals("attackReach") ? 1.0 : 100.0),
                    bounds[1] * (field.equals("attackReach") ? 1.0 : 100.0)));
        }
    }

    private static String elementalResistanceTranslationKey(String type) {
        return "item.simpleskills.crafting_stat.elemental_resistance_" + type;
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
            case "excavationTime" -> DATA_EXCAVATION_TIME;
            case "critical" -> DATA_CRITICAL_CHANCE;
            case "attackSpeed" -> DATA_ATTACK_SPEED;
            case "armor" -> DATA_ARMOR;
            case "toughness" -> DATA_TOUGHNESS;
            case "attackReach" -> DATA_ATTACK_REACH;
            case "armorPenetration" -> DATA_ARMOR_PENETRATION;
            case "combatFrenzy" -> DATA_COMBAT_FRENZY;
            case "firstStrike" -> DATA_FIRST_STRIKE;
            case "rhythmicDischarge" -> DATA_RHYTHMIC_DISCHARGE;
            case "tenacity" -> DATA_TENACITY;
            case "mobility" -> DATA_MOBILITY;
            case "elementalResistance" -> DATA_ELEMENTAL_RESISTANCE;
            default -> "";
        };
    }

    /** True when the selected stat still has room below its rarity's maximum roll. */
    public static boolean canEnhanceStat(ItemStack stack, String field) {
        double[] bounds = statBounds(stack, field);
        if (bounds == null) return false;
        double current = getTag(stack).getDoubleOr(statDataKey(field), 0.0);
        return current > 0.0 && bounds[1] - current > 0.000000001;
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
        // at exactly +1.00 percentage point per Rune. The final Rune always fills the exact
        // remaining gap, even when it is smaller than one percentage point.
        double displayMultiplier = "damage".equals(field)
                && getPerkAmplifier(stack, "runic_edge") >= 0 ? 1.08 : 1.0;
        double oneRuneProgress = field.equals("attackReach") ? 0.10 : 0.01 / displayMultiplier;
        if (oneRuneProgress <= 0.0) return false;
        double remaining = bounds[1] - current;
        if (remaining <= 0.000000001) return false;
        double progress = Math.min(oneRuneProgress, remaining);
        double upgraded = remaining <= oneRuneProgress + 0.000000001
                ? bounds[1] : current + progress;
        if (upgraded <= current) return false;

        String dataKey = statDataKey(field);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putDouble(dataKey, upgraded));
        refreshCraftingAttributes(stack);
        refreshCraftingTooltip(stack);
        markEnhancementMutation(stack);
        return true;
    }

    /** Returns whether the current attribute can be replaced by a different valid attribute. */
    public static boolean canRerollStat(ItemStack stack, String field) {
        if (!isEligibleForCelestialAltar(stack) || field == null || field.isBlank()) return false;
        double[] currentBounds = statBounds(stack, field);
        if (currentBounds == null || getTag(stack).getDoubleOr(statDataKey(field), 0.0) <= 0.0) return false;
        return !getReplacementAttributeCandidates(stack, field).isEmpty();
    }

    /**
     * Replaces the selected attribute with a different randomly chosen attribute that is
     * valid for this equipment category and rarity. It does not merely reroll the number.
     * The total number of rolled attributes remains unchanged and duplicate attributes are
     * never introduced.
     */
    public static boolean replaceEquipmentAttribute(ItemStack stack, String field, ServerPlayer player) {
        if (!canRerollStat(stack, field) || player == null) return false;
        List<String> currentAttributeOrder = getEnhanceableStats(stack).stream()
                .map(StatUpgradeInfo::id).toList();
        CompoundTag currentTag = getTag(stack);
        List<String> persistedOrder = resolveAttributeOrder(currentTag, currentAttributeOrder);
        int selectedPosition = persistedOrder.indexOf(field);
        if (selectedPosition < 0) return false;

        List<String> candidates = getReplacementAttributeCandidates(stack, field);
        if (candidates.isEmpty()) return false;
        String replacementField = candidates.get(player.getRandom().nextInt(candidates.size()));
        double[] replacementBounds = statBounds(stack, replacementField);
        if (replacementBounds == null) return false;
        double replacementValue = rollStat(player, replacementBounds[0], replacementBounds[1],
                statRollStep(replacementField));
        if (replacementValue <= 0.0) return false;

        if (replacementField.equals("attackReach")) storeBaseAttackRangeIfMissing(stack, player);
        String replacementType = switch (replacementField) {
            case "mobility" -> new String[]{"walk", "jump", "water"}[player.getRandom().nextInt(3)];
            case "elementalResistance" -> new String[]{"fire", "explosion", "projectile", "magic"}[player.getRandom().nextInt(4)];
            default -> "";
        };

        String oldDataKey = statDataKey(field);
        String newDataKey = statDataKey(replacementField);
        List<String> updatedOrder = new ArrayList<>(persistedOrder);
        // Replace the attribute ID at its existing display position. Other stats keep
        // their order even when the replacement belongs to another equipment category.
        updatedOrder.set(selectedPosition, replacementField);
        String encodedOrder = String.join(",", updatedOrder);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            data.remove(oldDataKey);
            if (field.equals("mobility")) data.remove(DATA_MOBILITY_TYPE);
            if (field.equals("elementalResistance")) data.remove(DATA_ELEMENTAL_TYPE);
            data.putDouble(newDataKey, replacementValue);
            data.putString(DATA_ATTRIBUTE_ORDER, encodedOrder);
            if (replacementField.equals("mobility")) data.putString(DATA_MOBILITY_TYPE, replacementType);
            if (replacementField.equals("elementalResistance")) data.putString(DATA_ELEMENTAL_TYPE, replacementType);
        });

        refreshCraftingAttributes(stack);
        refreshCraftingTooltip(stack);
        markEnhancementMutation(stack);
        return true;
    }

    private static List<String> getReplacementAttributeCandidates(ItemStack stack, String replacedField) {
        if (!isEligibleForCelestialAltar(stack)) return List.of();
        EquipmentKind kind = classify(stack);
        List<String> eligible = new ArrayList<>();

        // Keep this list aligned with the attributes available during initial crafting.
        // A Rune can select any category-valid attribute, even if its original appearance
        // chance did not roll on this item.
        eligible.add("durability");
        boolean weapon = isWeapon(stack);
        boolean damageEligible = weapon || kind == EquipmentKind.TOOL;
        boolean attackSpeedEligible = stack.is(ItemTags.SWORDS) || kind == EquipmentKind.TOOL;
        boolean breakSpeedEligible = kind == EquipmentKind.PICKAXE
                || kind == EquipmentKind.AXE || kind == EquipmentKind.SHOVEL;
        boolean defenseEligible = isArmorKind(kind);
        boolean meleeEligible = isMeleeCombatKind(kind);

        if (damageEligible) {
            eligible.add("damage");
            if (weapon) eligible.add("critical");
        }
        if (breakSpeedEligible) eligible.add("breakSpeed");
        if (kind == EquipmentKind.SHOVEL) eligible.add("excavationTime");
        if (attackSpeedEligible) eligible.add("attackSpeed");
        if (defenseEligible) {
            eligible.add("armor");
            eligible.add("toughness");
            eligible.add("tenacity");
            eligible.add("elementalResistance");
        }
        if (kind == EquipmentKind.SWORD || kind == EquipmentKind.AXE
                || kind == EquipmentKind.MACE || kind == EquipmentKind.TRIDENT) {
            eligible.add("attackReach");
        }
        if (meleeEligible) {
            eligible.add("armorPenetration");
            eligible.add("combatFrenzy");
            eligible.add("rhythmicDischarge");
        }
        if (meleeEligible || kind == EquipmentKind.BOW || kind == EquipmentKind.CROSSBOW) {
            eligible.add("firstStrike");
        }
        if (kind == EquipmentKind.LEGGINGS || kind == EquipmentKind.BOOTS) eligible.add("mobility");

        CompoundTag tag = getTag(stack);
        List<String> candidates = new ArrayList<>();
        for (String candidate : eligible) {
            if (candidate.equals(replacedField)) continue;
            String key = statDataKey(candidate);
            if (key.isBlank() || tag.getDoubleOr(key, 0.0) > 0.0) continue;
            if (statBounds(stack, candidate) == null) continue;
            candidates.add(candidate);
        }
        return candidates;
    }

    private static double statRollStep(String field) {
        return switch (field) {
            case "durability", "breakSpeed", "excavationTime", "attackSpeed" -> 0.005;
            case "attackReach" -> 0.05;
            default -> 0.001;
        };
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
            case "excavationTime" -> classify(stack) == EquipmentKind.SHOVEL
                    ? new double[]{rarity.minBreakSpeedBonus(), rarity.maxBreakSpeedBonus()} : null;
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
            case "attackReach" -> statRange(rarity, field);
            case "armorPenetration", "combatFrenzy", "firstStrike", "rhythmicDischarge",
                    "tenacity", "mobility", "elementalResistance" -> statRange(rarity, field);
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
        applyAttackSpeedAttributeBonus(stack, speed);
        EquipmentKind kind = classify(stack);
        applyDefenseAttributeBonuses(stack, kind, armor, toughness);
        applyAdditionalCraftingAttributeModifiers(stack, kind, tag);
        if (tag.getBooleanOr(DATA_BASE_RANGE_PRESENT, false)) applyCraftingAttackRangeFromSavedBase(stack);
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
        // Refreshing modifiers must never rewrite the rolled NBT values. A missing or
        // temporarily unsupported vanilla modifier is not permission to delete other
        // attributes from the item. Crafting eligibility is resolved when the roll is made;
        // the Altar only changes the explicitly selected stat/perk.
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            data.putBoolean(DATA_DAMAGE_AS_ATTRIBUTE, damageApplied);
            data.putBoolean(DATA_DAMAGE_ATTRIBUTE_INCLUDES_AFFIXES, damageApplied);
        });
    }

    /** Rebuilds stale generated lore once when older equipment is opened in the Celestial Altar. */
    private static void ensureCraftingTooltipCurrent(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !hasRarity(stack)) return;
        CompoundTag tag = getTag(stack);
        if (tag.getIntOr(DATA_LORE_SCHEMA_VERSION, 0) >= CURRENT_CRAFTING_LORE_SCHEMA) return;
        refreshCraftingTooltip(stack);
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
            generated += tag.getDoubleOr(DATA_EXCAVATION_TIME, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_CRITICAL_CHANCE, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_ATTACK_SPEED, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_ARMOR, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_TOUGHNESS, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_ATTACK_REACH, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_ARMOR_PENETRATION, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_COMBAT_FRENZY, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_FIRST_STRIKE, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_RHYTHMIC_DISCHARGE, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_TENACITY, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_MOBILITY, 0.0) > 0.0 ? 1 : 0;
            generated += tag.getDoubleOr(DATA_ELEMENTAL_RESISTANCE, 0.0) > 0.0 ? 1 : 0;
            String encoded = tag.getStringOr(DATA_PERKS, "");
            if (!encoded.isBlank()) generated += encoded.split(",").length;
        }
        // LoreManager appends its "Requires ... to use" line after generated crafting lore.
        // Remove that exact requirement line before using the generated-line count, otherwise
        // every refresh leaves one stale crafting line behind and appends a new copy.
        List<Component> loreWithoutRequirements = removeRequirementLoreLines(currentLore.lines());
        int keep = Math.max(0, loreWithoutRequirements.size() - generated);
        stack.set(DataComponents.LORE,
                new ItemLore(new ArrayList<>(loreWithoutRequirements.subList(0, keep))));

        CraftingRarity rarity = CraftingRarity.byId(tag.getStringOr(DATA_RARITY, ""));
        if (rarity == null) return;
        double damage = tag.getDoubleOr(DATA_DAMAGE, 0.0);
        double totalDamage = damage > 0.0
                ? (1.0 + damage) * (getPerkAmplifier(stack, "runic_edge") >= 0 ? 1.08 : 1.0) - 1.0 : 0.0;
        appendTooltip(stack, rarity, totalDamage,
                tag.getDoubleOr(DATA_DURABILITY, 0.0),
                tag.getDoubleOr(DATA_BREAK_SPEED, 0.0),
                tag.getDoubleOr(DATA_EXCAVATION_TIME, 0.0),
                getEffectiveCriticalChance(stack),
                tag.getDoubleOr(DATA_ATTACK_SPEED, 0.0),
                tag.getDoubleOr(DATA_ARMOR, 0.0),
                tag.getDoubleOr(DATA_TOUGHNESS, 0.0));
    }

    private static List<Component> removeRequirementLoreLines(List<Component> lines) {
        return lines.stream()
                .filter(line -> !REQUIREMENT_LORE_LINE.matcher(line.getString()).matches())
                .toList();
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
        // Read the current visible name instead of the original crafting name stored in NBT.
        // This preserves names players applied later with an anvil before the new rarity prefix is added.
        String savedBaseName = getUnprefixedItemName(stack);
        if (savedBaseName.isBlank()) savedBaseName = tag.getStringOr(DATA_BASE_NAME, "");
        final String persistedBaseName = savedBaseName;

        // Remove only the lore lines previously generated by this system; unrelated/custom lore stays intact.
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            int generated = tag.getIntOr(DATA_LORE_LINE_COUNT, -1);
            if (generated < 0) {
                generated = 1;
                generated += tag.getDoubleOr(DATA_DAMAGE, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_DURABILITY, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_BREAK_SPEED, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_EXCAVATION_TIME, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_CRITICAL_CHANCE, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_ATTACK_SPEED, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_ARMOR, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_TOUGHNESS, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_ATTACK_REACH, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_ARMOR_PENETRATION, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_COMBAT_FRENZY, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_FIRST_STRIKE, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_RHYTHMIC_DISCHARGE, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_TENACITY, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_MOBILITY, 0.0) > 0.0 ? 1 : 0;
                generated += tag.getDoubleOr(DATA_ELEMENTAL_RESISTANCE, 0.0) > 0.0 ? 1 : 0;
                String oldPerks = tag.getStringOr(DATA_PERKS, "");
                if (!oldPerks.isBlank()) generated += oldPerks.split(",").length;
            }
            // Apply the same requirement-line filtering used by refreshCraftingTooltip().
            // Otherwise a reroll can preserve one stale generated line before regenerating lore.
            List<Component> loreWithoutRequirements = removeRequirementLoreLines(lore.lines());
            int keep = Math.max(0, loreWithoutRequirements.size() - Math.max(0, generated));
            stack.set(DataComponents.LORE,
                    new ItemLore(new ArrayList<>(loreWithoutRequirements.subList(0, keep))));
        }

        removeCraftingAttributeModifiers(stack);
        restoreBaseAttackRange(stack, player, tag);
        stack.set(DataComponents.MAX_DAMAGE, savedBaseMaximum);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            data.putInt(DATA_BASE_MAX_DAMAGE, savedBaseMaximum);
            data.putString(DATA_BASE_NAME, persistedBaseName);
            data.remove(DATA_LORE_LINE_COUNT);
        });
    }

    private static void removeCraftingAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        List<ItemAttributeModifiers.Entry> retained = modifiers.modifiers().stream()
                .filter(entry -> {
                    Identifier id = entry.modifier().id();
                    String path = id.getPath();
                    return !Set.of(CRAFTING_DAMAGE_MODIFIER_ID, CRAFTING_ATTACK_SPEED_MODIFIER_ID,
                            CRAFTING_ARMOR_MODIFIER_ID, CRAFTING_TOUGHNESS_MODIFIER_ID,
                            TENACITY_MODIFIER_ID, MOBILITY_SPEED_MODIFIER_ID, MOBILITY_JUMP_MODIFIER_ID,
                            MOBILITY_WATER_MODIFIER_ID, BLOCK_REACH_MODIFIER_ID).contains(id)
                            && !path.startsWith("crafting_tenacity_")
                            && !path.startsWith("crafting_mobility_speed_")
                            && !path.startsWith("crafting_mobility_jump_")
                            && !path.startsWith("crafting_mobility_water_");
                })
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
        if (getPerkAmplifier(stack, "celestial_range") < 0
                && tag.getDoubleOr(DATA_ATTACK_REACH, 0.0) <= 0.0) return;
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

    private static int maximumAttributes(CraftingRarity rarity) {
        return switch (rarity) {
            case COMMON -> 1;
            case RARE, UNIQUE -> 2;
            case EPIC -> 3;
            case LEGENDARY -> 4;
            case MYTHIC -> 5;
            case TRANSCENDENT -> 6;
        };
    }

    private static List<String> selectAttributeSet(CraftingRarity rarity, ServerPlayer player,
            double damage, double durability, double breakSpeed, double excavationTime,
            double critical, double attackSpeed, double armor, double toughness,
            double attackReach, double armorPenetration, double combatFrenzy,
            double firstStrike, double rhythmicDischarge, double tenacity,
            double mobility, double elementalResistance) {
        List<List<String>> groups = new ArrayList<>();
        // Each visible attribute line consumes one slot in the rarity's attribute budget.
        // Damage and Critical Chance are independent attributes, not an inseparable pair.
        addAttributeGroup(groups, "damage", damage);
        addAttributeGroup(groups, "critical", critical);
        addAttributeGroup(groups, "breakSpeed", breakSpeed);
        addAttributeGroup(groups, "excavationTime", excavationTime);
        addAttributeGroup(groups, "attackSpeed", attackSpeed);
        addAttributeGroup(groups, "armor", armor);
        addAttributeGroup(groups, "toughness", toughness);
        addAttributeGroup(groups, "attackReach", attackReach);
        addAttributeGroup(groups, "armorPenetration", armorPenetration);
        addAttributeGroup(groups, "combatFrenzy", combatFrenzy);
        addAttributeGroup(groups, "firstStrike", firstStrike);
        addAttributeGroup(groups, "rhythmicDischarge", rhythmicDischarge);
        addAttributeGroup(groups, "tenacity", tenacity);
        addAttributeGroup(groups, "mobility", mobility);
        addAttributeGroup(groups, "elementalResistance", elementalResistance);

        // Fisher-Yates shuffle using the server player's RandomSource.
        for (int i = groups.size() - 1; i > 0; i--) {
            int swap = player.getRandom().nextInt(i + 1);
            List<String> value = groups.get(i);
            groups.set(i, groups.get(swap));
            groups.set(swap, value);
        }

        // Persist the order in which attributes were selected. A HashSet would discard
        // this order, causing an attribute to jump to a category-defined display position
        // after a reroll even though only its type was replaced.
        List<String> selected = new ArrayList<>();
        if (durability > 0.0) selected.add("durability");
        int cap = maximumAttributes(rarity);
        for (List<String> group : groups) {
            if (selected.size() + group.size() <= cap) selected.addAll(group);
        }
        return List.copyOf(selected);
    }

    private static void addAttributeGroup(List<List<String>> groups, String id, double value) {
        if (value > 0.0) groups.add(List.of(id));
    }

    private static void applyEquipmentRarity(ItemStack stack, ServerPlayer player, CraftingRarity rarity, int level) {
        applyEquipmentRarity(stack, player, rarity, level, true);
    }

    private static void applyEquipmentRarity(ItemStack stack, ServerPlayer player, CraftingRarity rarity,
            int level, boolean announceAndUnlock) {
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
        // Shovels can also roll a separate percentage reduction to sand investigation time.
        // It follows the same rarity ranges and appearance chance as the Break Speed stat.
        double excavationTimeReduction = kind == EquipmentKind.SHOVEL
                && rollStatPresence(player, rarity.breakSpeedStatChance())
                ? rollStat(player, rarity.minBreakSpeedBonus(), rarity.maxBreakSpeedBonus(), 0.01) : 0.0;
        double criticalChance = weapon && damageBonus > 0.0
                ? rollStat(player, rarity.minCriticalChance(), rarity.maxCriticalChance(), 0.001) : 0.0;
        double attackSpeedBonus = attackSpeedEligible && rollStatPresence(player, rarity.attackSpeedStatChance())
                ? rollStat(player, rarity.minAttackSpeedBonus(), rarity.maxAttackSpeedBonus(), 0.005) : 0.0;
        double armorBonus = defenseEligible && rollStatPresence(player, rarity.armorStatChance())
                ? rollStat(player, rarity.minArmorBonus(), rarity.maxArmorBonus(), 0.001) : 0.0;
        double toughnessBonus = defenseEligible && rollStatPresence(player, rarity.toughnessStatChance())
                ? rollStat(player, rarity.minToughnessBonus(), rarity.maxToughnessBonus(), 0.001) : 0.0;

        boolean meleeCombatEligible = isMeleeCombatKind(kind);
        boolean reachEligible = kind == EquipmentKind.SWORD || kind == EquipmentKind.AXE
                || kind == EquipmentKind.MACE || kind == EquipmentKind.TRIDENT;
        double attackReachBonus = reachEligible && rollStatPresence(player, rarity.attackSpeedStatChance())
                ? rollRarityStat(player, rarity, "attackReach") : 0.0;
        double armorPenetration = meleeCombatEligible && rollStatPresence(player, rarity.damageStatChance())
                ? rollRarityStat(player, rarity, "armorPenetration") : 0.0;
        double combatFrenzy = meleeCombatEligible && rollStatPresence(player, rarity.attackSpeedStatChance())
                ? rollRarityStat(player, rarity, "combatFrenzy") : 0.0;
        boolean firstStrikeEligible = meleeCombatEligible || kind == EquipmentKind.BOW
                || kind == EquipmentKind.CROSSBOW;
        double firstStrike = firstStrikeEligible && rollStatPresence(player, rarity.damageStatChance())
                ? rollRarityStat(player, rarity, "firstStrike") : 0.0;
        double rhythmicDischarge = meleeCombatEligible && rollStatPresence(player, rarity.damageStatChance())
                ? rollRarityStat(player, rarity, "rhythmicDischarge") : 0.0;
        double tenacity = defenseEligible && rollStatPresence(player, rarity.armorStatChance())
                ? rollRarityStat(player, rarity, "tenacity") : 0.0;
        boolean mobilityEligible = kind == EquipmentKind.LEGGINGS || kind == EquipmentKind.BOOTS;
        double mobility = mobilityEligible && rollStatPresence(player, rarity.attackSpeedStatChance())
                ? rollRarityStat(player, rarity, "mobility") : 0.0;
        String mobilityType = mobility > 0.0
                ? new String[]{"walk", "jump", "water"}[player.getRandom().nextInt(3)] : "";
        double elementalResistance = defenseEligible && rollStatPresence(player, rarity.toughnessStatChance())
                ? rollRarityStat(player, rarity, "elementalResistance") : 0.0;
        String elementalType = elementalResistance > 0.0
                ? new String[]{"fire", "explosion", "projectile", "magic"}[player.getRandom().nextInt(4)] : "";

        // A crafted item can only display a limited number of green attribute lines.
        // Durability remains the baseline stat; the remaining eligible attributes are
        // selected as random groups. Damage and Critical Chance stay linked so an item
        // never receives a critical stat without its corresponding damage attribute.
        List<String> selectedAttributes = selectAttributeSet(rarity, player,
                damageBonus, durabilityBonus, breakSpeedBonus, excavationTimeReduction,
                criticalChance, attackSpeedBonus, armorBonus, toughnessBonus, attackReachBonus,
                armorPenetration, combatFrenzy, firstStrike, rhythmicDischarge,
                tenacity, mobility, elementalResistance);
        if (!selectedAttributes.contains("damage")) damageBonus = 0.0;
        if (!selectedAttributes.contains("critical")) criticalChance = 0.0;
        if (!selectedAttributes.contains("durability")) durabilityBonus = 0.0;
        if (!selectedAttributes.contains("breakSpeed")) breakSpeedBonus = 0.0;
        if (!selectedAttributes.contains("excavationTime")) excavationTimeReduction = 0.0;
        if (!selectedAttributes.contains("attackSpeed")) attackSpeedBonus = 0.0;
        if (!selectedAttributes.contains("armor")) armorBonus = 0.0;
        if (!selectedAttributes.contains("toughness")) toughnessBonus = 0.0;
        if (!selectedAttributes.contains("attackReach")) attackReachBonus = 0.0;
        if (!selectedAttributes.contains("armorPenetration")) armorPenetration = 0.0;
        if (!selectedAttributes.contains("combatFrenzy")) combatFrenzy = 0.0;
        if (!selectedAttributes.contains("firstStrike")) firstStrike = 0.0;
        if (!selectedAttributes.contains("rhythmicDischarge")) rhythmicDischarge = 0.0;
        if (!selectedAttributes.contains("tenacity")) tenacity = 0.0;
        if (!selectedAttributes.contains("mobility")) { mobility = 0.0; mobilityType = ""; }
        if (!selectedAttributes.contains("elementalResistance")) { elementalResistance = 0.0; elementalType = ""; }

        String encodedPerks = encodePerks(perks, level, rarity, player);
        boolean celestialRangeApplied = perks.stream().anyMatch(perk -> perk.id().equals("celestial_range"));
        if (celestialRangeApplied || attackReachBonus > 0.0) {
            storeBaseAttackRangeIfMissing(stack, player);
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
        // Snapshot every mutable rolled value before entering the CustomData lambda.
        // Several values may be cleared after attribute eligibility is resolved above,
        // so they are not effectively final and cannot be captured directly.
        final double finalDamageBonus = damageBonus;
        final double finalDurabilityBonus = durabilityBonus;
        final double finalBreakSpeedBonus = breakSpeedBonus;
        final double finalExcavationTimeReduction = excavationTimeReduction;
        final double finalCriticalChance = criticalChance;
        final double finalAttackSpeedBonus = attackSpeedBonus;
        final double finalArmorBonus = armorBonus;
        final double finalToughnessBonus = toughnessBonus;
        final double finalAttackReachBonus = attackReachBonus;
        final double finalArmorPenetration = armorPenetration;
        final double finalCombatFrenzy = combatFrenzy;
        final double finalFirstStrike = firstStrike;
        final double finalRhythmicDischarge = rhythmicDischarge;
        final double finalTenacity = tenacity;
        final double finalMobility = mobility;
        final String finalMobilityType = mobilityType;
        final double finalElementalResistance = elementalResistance;
        final String finalElementalType = elementalType;
        final String finalAttributeOrder = String.join(",", selectedAttributes.stream()
                .filter(field -> switch (field) {
                    case "damage" -> finalDamageBonus > 0.0;
                    case "durability" -> finalDurabilityBonus > 0.0;
                    case "breakSpeed" -> finalBreakSpeedBonus > 0.0;
                    case "excavationTime" -> finalExcavationTimeReduction > 0.0;
                    case "critical" -> finalCriticalChance > 0.0;
                    case "attackSpeed" -> finalAttackSpeedBonus > 0.0;
                    case "armor" -> finalArmorBonus > 0.0;
                    case "toughness" -> finalToughnessBonus > 0.0;
                    case "attackReach" -> finalAttackReachBonus > 0.0;
                    case "armorPenetration" -> finalArmorPenetration > 0.0;
                    case "combatFrenzy" -> finalCombatFrenzy > 0.0;
                    case "firstStrike" -> finalFirstStrike > 0.0;
                    case "rhythmicDischarge" -> finalRhythmicDischarge > 0.0;
                    case "tenacity" -> finalTenacity > 0.0;
                    case "mobility" -> finalMobility > 0.0;
                    case "elementalResistance" -> finalElementalResistance > 0.0;
                    default -> false;
                }).toList());

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(DATA_RARITY, rarity.id());
            tag.putString(DATA_BASE_NAME, persistedBaseName);
            tag.putInt(DATA_BASE_MAX_DAMAGE, persistedBaseMaximum);
            tag.putDouble(DATA_DAMAGE, finalDamageBonus);
            tag.putDouble(DATA_DURABILITY, finalDurabilityBonus);
            tag.putDouble(DATA_BREAK_SPEED, finalBreakSpeedBonus);
            tag.putDouble(DATA_EXCAVATION_TIME, finalExcavationTimeReduction);
            tag.putDouble(DATA_CRITICAL_CHANCE, finalCriticalChance);
            tag.putDouble(DATA_ATTACK_SPEED, finalAttackSpeedBonus);
            tag.putDouble(DATA_ARMOR, finalArmorBonus);
            tag.putDouble(DATA_TOUGHNESS, finalToughnessBonus);
            tag.putDouble(DATA_ATTACK_REACH, finalAttackReachBonus);
            tag.putDouble(DATA_ARMOR_PENETRATION, finalArmorPenetration);
            tag.putDouble(DATA_COMBAT_FRENZY, finalCombatFrenzy);
            tag.putDouble(DATA_FIRST_STRIKE, finalFirstStrike);
            tag.putDouble(DATA_RHYTHMIC_DISCHARGE, finalRhythmicDischarge);
            tag.putDouble(DATA_TENACITY, finalTenacity);
            tag.putDouble(DATA_MOBILITY, finalMobility);
            tag.putString(DATA_MOBILITY_TYPE, finalMobilityType);
            tag.putDouble(DATA_ELEMENTAL_RESISTANCE, finalElementalResistance);
            tag.putString(DATA_ELEMENTAL_TYPE, finalElementalType);
            tag.putString(DATA_ATTRIBUTE_ORDER, finalAttributeOrder);
            tag.putString(DATA_PERKS, encodedPerks);
            tag.putBoolean(DATA_DAMAGE_AS_ATTRIBUTE, damageAppliedAsAttribute);
            tag.putBoolean(DATA_DAMAGE_ATTRIBUTE_INCLUDES_AFFIXES, damageAppliedAsAttribute);
        });

        stack.set(DataComponents.MAX_DAMAGE, Math.max(1,
                (int) Math.round(baseMaximum * (1.0 + durabilityBonus))));
        refreshCraftingAttributes(stack);
        if (celestialRangeApplied || attackReachBonus > 0.0) applyCraftingAttackRange(stack, player);

        Component originalName = Component.literal(baseName);
        Component rarityName = Component.translatable("item.simpleskills.crafting_rarity." + rarity.id());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "item.simpleskills.crafted_quality_name", rarityName, originalName)
                .withStyle(style -> style.withColor(rarity.color()).withItalic(false)));
        double displayedCriticalChance = getEffectiveCriticalChance(stack);
        // Display the combined rarity + Runic Edge percentage so it matches the real main-hand attribute modifier.
        appendTooltip(stack, rarity, totalDamageBonus, durabilityBonus, breakSpeedBonus,
                excavationTimeReduction, displayedCriticalChance, attackSpeedBonus, armorBonus, toughnessBonus);

        // Equipment found in an Abyssal chest receives the full affix pipeline, but it was
        // discovered rather than crafted: do not unlock crafting titles, broadcast a craft
        // announcement, or open the crafting reveal UI for loot items.
        if (announceAndUnlock) {
            if (weapon) unlockWeaponTitle(player, rarity);
            announceHighRarityCraft(player, stack, rarity);
            sendReveal(player, stack, originalName.getString(), rarity, totalDamageBonus, durabilityBonus,
                    breakSpeedBonus, excavationTimeReduction, displayedCriticalChance,
                    attackSpeedBonus, armorBonus, toughnessBonus, encodedPerks);
        }
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
                                   double excavationTimeReduction, double criticalChance,
                                   double attackSpeedBonus, double armorBonus, double toughnessBonus,
                                   String encodedPerks) {
        // Common and Rare are intentionally silent; the reveal screen is reserved for Unique+.
        if (rarity.ordinal() < CraftingRarity.UNIQUE.ordinal()) return;
        if (!ServerPlayNetworking.canSend(player, CraftingRevealPayload.TYPE)) return;

        JsonObject details = new JsonObject();
        details.addProperty("damage", damageBonus * 100.0);
        details.addProperty("durability", durabilityBonus * 100.0);
        details.addProperty("breakSpeed", breakSpeedBonus * 100.0);
        details.addProperty("excavationTime", excavationTimeReduction * 100.0);
        details.addProperty("attackSpeed", attackSpeedBonus * 100.0);
        details.addProperty("armor", armorBonus * 100.0);
        details.addProperty("toughness", toughnessBonus * 100.0);

        // Serialize the same newer rolled attributes that are stored on the equipment.
        // The explicit order keeps the reveal, inventory tooltip and Celestial Altar aligned.
        CompoundTag rolledStats = getTag(stack);
        JsonArray attributeOrder = new JsonArray();
        for (StatUpgradeInfo stat : getEnhanceableStats(stack)) attributeOrder.add(stat.id());
        details.add("attributeOrder", attributeOrder);
        if (rolledStats.getDoubleOr(DATA_CRITICAL_CHANCE, 0.0) > 0.0) {
            details.addProperty("critical", criticalChance * 100.0);
        }
        addRevealStoredStat(details, rolledStats, rarity, "armorPenetration", DATA_ARMOR_PENETRATION, 100.0);
        addRevealStoredStat(details, rolledStats, rarity, "combatFrenzy", DATA_COMBAT_FRENZY, 100.0);
        addRevealStoredStat(details, rolledStats, rarity, "firstStrike", DATA_FIRST_STRIKE, 100.0);
        addRevealStoredStat(details, rolledStats, rarity, "rhythmicDischarge", DATA_RHYTHMIC_DISCHARGE, 100.0);
        addRevealStoredStat(details, rolledStats, rarity, "tenacity", DATA_TENACITY, 100.0);
        if (rolledStats.getDoubleOr(DATA_MOBILITY, 0.0) > 0.0) {
            details.addProperty("mobilityType", rolledStats.getStringOr(DATA_MOBILITY_TYPE, "walk"));
            addRevealStoredStat(details, rolledStats, rarity, "mobility", DATA_MOBILITY, 100.0);
        }
        if (rolledStats.getDoubleOr(DATA_ELEMENTAL_RESISTANCE, 0.0) > 0.0) {
            details.addProperty("elementalType", rolledStats.getStringOr(DATA_ELEMENTAL_TYPE, "fire"));
            addRevealStoredStat(details, rolledStats, rarity, "elementalResistance", DATA_ELEMENTAL_RESISTANCE, 100.0);
        }
        // Attack Reach is a flat distance bonus, so its values are displayed in blocks.
        addRevealStoredStat(details, rolledStats, rarity, "attackReach", DATA_ATTACK_REACH, 1.0);

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
        if (classify(stack) == EquipmentKind.SHOVEL) {
            addRevealStatRange(details, "excavationTime",
                    rarity.minBreakSpeedBonus() * 100.0, rarity.maxBreakSpeedBonus() * 100.0);
        }
        if (rolledStats.getDoubleOr(DATA_CRITICAL_CHANCE, 0.0) > 0.0) {
            int criticalMastery = getPerkAmplifier(stack, "critical_mastery");
            double criticalMasteryBonus = criticalMastery >= 0 ? 5.0 + criticalMastery * 5.0 : 0.0;
            addRevealStatRange(details, "critical",
                    Math.min(60.0, rarity.minCriticalChance() * 100.0 + criticalMasteryBonus),
                    Math.min(60.0, rarity.maxCriticalChance() * 100.0 + criticalMasteryBonus));
        }
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

    private static void addRevealStoredStat(JsonObject details, CompoundTag tag, CraftingRarity rarity,
                                            String field, String dataKey, double displayMultiplier) {
        double value = tag.getDoubleOr(dataKey, 0.0);
        if (value <= 0.0) return;
        details.addProperty(field, value * displayMultiplier);
        double[] bounds = statRange(rarity, field);
        if (bounds != null) {
            addRevealStatRange(details, field, bounds[0] * displayMultiplier, bounds[1] * displayMultiplier);
        }
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
        if (stack.is(GlowstoneShields.SHIELDS) || path.contains("shield")) return EquipmentKind.SHIELD;

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
        // 26.3 uses the Tool data component for generic tools that are not covered by
        // pickaxe/axe/shovel/hoe tags. This makes the existing Worldshaper pool reachable.
        if (stack.get(DataComponents.TOOL) != null) return EquipmentKind.TOOL;
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
                            CRITICAL_MASTERY, THUNDEROUS, FROSTBITE, PHOENIX, DOUBLE_LEAP));
            case ARMOR, HELMET, CHESTPLATE, LEGGINGS, BOOTS ->
                    new ArrayList<>(List.of(PROTECTION, RESILIENT, SPEED, REGENERATION,
                            FEATHERWEIGHT, VITALITY, FIRE_RESISTANCE, JUMP_BOOST, DOUBLE_LEAP,
                            ETERNAL_GUARDIAN, AEGIS_OF_AGES));
            case SHIELD -> new ArrayList<>(List.of(PROTECTION, RESILIENT, REGENERATION, VITALITY,
                    FIRE_RESISTANCE, DOUBLE_LEAP, ETERNAL_GUARDIAN, AEGIS_OF_AGES));
            case SHOVEL -> new ArrayList<>(List.of(HASTE, EFFICIENT, SPEED,
                    INDESTRUCTIBLE, TREASURE_HUNTER, EXCAVATION_INERTIA, EXTENDED_REACH));
            case PICKAXE -> new ArrayList<>(List.of(HASTE, EFFICIENT, SPEED,
                    FORTUNE_MASTERY, INDESTRUCTIBLE, EXCAVATION_INERTIA, EXTENDED_REACH, VEIN_RESONANCE));
            case AXE, TOOL, HOE -> new ArrayList<>(List.of(HASTE, EFFICIENT, SPEED,
                    INDESTRUCTIBLE, EXCAVATION_INERTIA, EXTENDED_REACH));
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
            case PICKAXE, SHOVEL -> List.of(PROSPECTOR, THERMAL_SMELTING);
            case TOOL, HOE -> List.of(PROSPECTOR);
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
        List<Perk> generalPool = rarity == CraftingRarity.TRANSCENDENT
                ? transcendentGeneralPool(kind) : commonPool(kind);
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
        selected.removeIf(perk -> {
            Set<String> effects = grantedEffectIds(perk);
            if (effects.stream().anyMatch(usedEffectIds::contains)) return true;
            usedEffectIds.addAll(effects);
            return false;
        });

        // Prefer composite perks such as Aegis of Ages and Celestial Movement over
        // separate single-effect perks that would otherwise occupy the same effect slot.
        List<Perk> orderedCandidates = new ArrayList<>(candidates);
        orderedCandidates.sort((left, right) -> Integer.compare(
                perkEffectPriority(right), perkEffectPriority(left)));
        candidates.clear();
        for (Perk perk : orderedCandidates) {
            Set<String> effects = grantedEffectIds(perk);
            if (effects.stream().anyMatch(usedEffectIds::contains)) continue;
            usedEffectIds.addAll(effects);
            candidates.add(perk);
        }
    }

    private static int perkEffectPriority(Perk perk) {
        int priority = grantedEffectIds(perk).size();
        // These named composite perks should win against individual Speed/Resistance/Regeneration aliases.
        if (perk.id().equals("celestial_movement")) priority += 2;
        if (perk.id().equals("aegis_of_ages") || perk.id().equals("divine_blessing")
                || perk.id().equals("eternal_guardian")) priority += 2;
        return priority;
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
            int minimumRank = perk.id().equals("celestial_movement")
                    ? 1 : Math.min(perk.maximumEffectLevel(), rarity.minimumPerkEffectLevel());
            int normalizedLevel = Math.max(1, level);
            int skillMaximum = 1 + (normalizedLevel - 1) / 25;
            if (perk.id().equals("excavation_inertia") || perk.id().equals("extended_reach")) {
                skillMaximum = 1 + (normalizedLevel - 1) / 20;
            }
            int maximumRank = perk.id().equals("celestial_movement")
                    ? perk.maximumEffectLevel()
                    : Math.min(perk.maximumEffectLevel(), Math.max(minimumRank, skillMaximum));
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
        applyCraftingAttackRange(stack, player);
    }

    /** Applies the rolled Attack Reach stat together with the Transcendent +6 reach signature. */
    private static void applyCraftingAttackRange(ItemStack stack, ServerPlayer player) {
        CompoundTag tag = getTag(stack);
        boolean celestial = getPerkAmplifier(stack, "celestial_range") >= 0;
        double bonus = Math.max(0.0, tag.getDoubleOr(DATA_ATTACK_REACH, 0.0))
                + (celestial ? 6.0 : 0.0);
        if (bonus <= 0.0) return;
        if (tag.getBooleanOr(DATA_BASE_RANGE_PRESENT, false)) {
            applyCraftingAttackRangeFromSavedBase(stack);
            return;
        }
        AttackRange base = AttackRange.defaultFor(player);
        float safeBonus = (float) Math.min(60.0, bonus);
        stack.set(DataComponents.ATTACK_RANGE, new AttackRange(
                base.minReach(), Math.min(64.0F, base.maxReach() + safeBonus),
                base.minCreativeReach(), Math.min(64.0F, base.maxCreativeReach() + safeBonus),
                base.hitboxMargin(), base.mobFactor()));
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

    private static void applyAdditionalCraftingAttributeModifiers(ItemStack stack, EquipmentKind kind,
                                                                  CompoundTag tag) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        EquipmentSlotGroup armorGroup = kind == EquipmentKind.SHIELD ? EquipmentSlotGroup.OFFHAND
                : (equippable == null ? null : switch (equippable.slot()) {
                    case HEAD -> EquipmentSlotGroup.HEAD;
                    case CHEST -> EquipmentSlotGroup.CHEST;
                    case LEGS -> EquipmentSlotGroup.LEGS;
                    case FEET -> EquipmentSlotGroup.FEET;
                    default -> null;
                });

        double tenacity = tag.getDoubleOr(DATA_TENACITY, 0.0);
        if (tenacity > 0.0 && armorGroup != null) {
            modifiers = modifiers.withModifierAdded(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(slotModifierId("crafting_tenacity", armorGroup),
                            tenacity, AttributeModifier.Operation.ADD_VALUE), armorGroup);
        }

        double mobility = tag.getDoubleOr(DATA_MOBILITY, 0.0);
        if (mobility > 0.0 && armorGroup != null) {
            String mobilityType = tag.getStringOr(DATA_MOBILITY_TYPE, "walk");
            if (mobilityType.equals("jump")) {
                modifiers = modifiers.withModifierAdded(Attributes.JUMP_STRENGTH,
                        new AttributeModifier(slotModifierId("crafting_mobility_jump", armorGroup), mobility,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE), armorGroup);
            } else if (mobilityType.equals("water")) {
                modifiers = modifiers.withModifierAdded(Attributes.WATER_MOVEMENT_EFFICIENCY,
                        new AttributeModifier(slotModifierId("crafting_mobility_water", armorGroup), mobility,
                                AttributeModifier.Operation.ADD_VALUE), armorGroup);
            } else {
                modifiers = modifiers.withModifierAdded(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(slotModifierId("crafting_mobility_speed", armorGroup), mobility,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE), armorGroup);
            }
        }

        int reachRank = getPerkAmplifier(stack, "extended_reach");
        if (reachRank >= 0 && (kind == EquipmentKind.PICKAXE || kind == EquipmentKind.SHOVEL
                || kind == EquipmentKind.AXE || kind == EquipmentKind.HOE || kind == EquipmentKind.TOOL)) {
            double reach = Math.min(2.0, 0.4 * (reachRank + 1));
            modifiers = modifiers.withModifierAdded(Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(BLOCK_REACH_MODIFIER_ID, reach, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
    }

    private static Identifier slotModifierId(String basePath, EquipmentSlotGroup slotGroup) {
        String suffix = slotGroup.equals(EquipmentSlotGroup.HEAD) ? "head"
                : slotGroup.equals(EquipmentSlotGroup.CHEST) ? "chest"
                : slotGroup.equals(EquipmentSlotGroup.LEGS) ? "legs"
                : slotGroup.equals(EquipmentSlotGroup.FEET) ? "feet"
                : slotGroup.equals(EquipmentSlotGroup.OFFHAND) ? "offhand" : "mainhand";
        return Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, basePath + "_" + suffix);
    }

    private static void applyCraftingAttackRangeFromSavedBase(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.getBooleanOr(DATA_BASE_RANGE_PRESENT, false)) return;
        boolean celestial = getPerkAmplifier(stack, "celestial_range") >= 0;
        double bonus = Math.max(0.0, tag.getDoubleOr(DATA_ATTACK_REACH, 0.0))
                + (celestial ? 6.0 : 0.0);
        AttackRange base = new AttackRange(
                tag.getFloatOr(DATA_BASE_RANGE_MIN, 0.0F),
                tag.getFloatOr(DATA_BASE_RANGE_MAX, 3.0F),
                tag.getFloatOr(DATA_BASE_RANGE_MIN_CREATIVE, 0.0F),
                tag.getFloatOr(DATA_BASE_RANGE_MAX_CREATIVE, 5.0F),
                tag.getFloatOr(DATA_BASE_RANGE_HITBOX, 0.3F),
                tag.getFloatOr(DATA_BASE_RANGE_MOB_FACTOR, 1.0F));
        stack.set(DataComponents.ATTACK_RANGE, new AttackRange(
                base.minReach(), Math.min(64.0F, base.maxReach() + (float) Math.min(60.0, bonus)),
                base.minCreativeReach(), Math.min(64.0F, base.maxCreativeReach() + (float) Math.min(60.0, bonus)),
                base.hitboxMargin(), base.mobFactor()));
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

    private static double[] statRange(CraftingRarity rarity, String field) {
        if (rarity == null) return null;
        return switch (field) {
            case "attackReach" -> switch (rarity) {
                case COMMON -> new double[]{0.10, 1.00};
                case RARE -> new double[]{1.00, 2.00};
                case UNIQUE -> new double[]{1.00, 2.50};
                case EPIC -> new double[]{1.50, 3.00};
                case LEGENDARY -> new double[]{2.00, 4.00};
                case MYTHIC -> new double[]{3.00, 5.00};
                case TRANSCENDENT -> new double[]{4.00, 6.00};
            };
            case "armorPenetration" -> switch (rarity) {
                case COMMON -> new double[]{0.02, 0.05};
                case RARE -> new double[]{0.04, 0.08};
                case UNIQUE -> new double[]{0.06, 0.10};
                case EPIC -> new double[]{0.08, 0.12};
                case LEGENDARY -> new double[]{0.12, 0.17};
                case MYTHIC -> new double[]{0.17, 0.23};
                case TRANSCENDENT -> new double[]{0.23, 0.30};
            };
            case "combatFrenzy" -> switch (rarity) {
                case COMMON -> new double[]{0.10, 0.10};
                case RARE -> new double[]{0.10, 0.12};
                case UNIQUE -> new double[]{0.10, 0.14};
                case EPIC -> new double[]{0.10, 0.16};
                case LEGENDARY -> new double[]{0.10, 0.18};
                case MYTHIC -> new double[]{0.10, 0.20};
                case TRANSCENDENT -> new double[]{0.10, 0.22};
            };
            case "firstStrike" -> switch (rarity) {
                case COMMON -> new double[]{0.30, 0.35};
                case RARE -> new double[]{0.32, 0.38};
                case UNIQUE -> new double[]{0.35, 0.40};
                case EPIC -> new double[]{0.38, 0.43};
                case LEGENDARY -> new double[]{0.42, 0.46};
                case MYTHIC -> new double[]{0.45, 0.48};
                case TRANSCENDENT -> new double[]{0.47, 0.50};
            };
            case "rhythmicDischarge" -> switch (rarity) {
                case COMMON -> new double[]{0.05, 0.10};
                case RARE -> new double[]{0.10, 0.15};
                case UNIQUE -> new double[]{0.15, 0.20};
                case EPIC -> new double[]{0.20, 0.28};
                case LEGENDARY -> new double[]{0.25, 0.35};
                case MYTHIC -> new double[]{0.30, 0.42};
                case TRANSCENDENT -> new double[]{0.35, 0.50};
            };
            case "tenacity" -> switch (rarity) {
                case COMMON -> new double[]{0.02, 0.05};
                case RARE -> new double[]{0.04, 0.08};
                case UNIQUE -> new double[]{0.06, 0.10};
                case EPIC -> new double[]{0.08, 0.12};
                case LEGENDARY -> new double[]{0.12, 0.17};
                case MYTHIC -> new double[]{0.17, 0.23};
                case TRANSCENDENT -> new double[]{0.23, 0.30};
            };
            case "mobility" -> switch (rarity) {
                case COMMON -> new double[]{0.01, 0.02};
                case RARE -> new double[]{0.02, 0.04};
                case UNIQUE -> new double[]{0.03, 0.05};
                case EPIC -> new double[]{0.05, 0.08};
                case LEGENDARY -> new double[]{0.08, 0.11};
                case MYTHIC -> new double[]{0.10, 0.15};
                case TRANSCENDENT -> new double[]{0.12, 0.18};
            };
            case "elementalResistance" -> switch (rarity) {
                case COMMON -> new double[]{0.02, 0.05};
                case RARE -> new double[]{0.04, 0.08};
                case UNIQUE -> new double[]{0.06, 0.10};
                case EPIC -> new double[]{0.08, 0.12};
                case LEGENDARY -> new double[]{0.12, 0.17};
                case MYTHIC -> new double[]{0.17, 0.23};
                case TRANSCENDENT -> new double[]{0.23, 0.30};
            };
            default -> null;
        };
    }

    private static double rollRarityStat(ServerPlayer player, CraftingRarity rarity, String field) {
        double[] bounds = statRange(rarity, field);
        return bounds == null ? 0.0 : rollStat(player, bounds[0], bounds[1], 0.001);
    }

    private static boolean isMeleeCombatKind(EquipmentKind kind) {
        return kind == EquipmentKind.SWORD || kind == EquipmentKind.AXE
                || kind == EquipmentKind.MACE || kind == EquipmentKind.TRIDENT
                || kind == EquipmentKind.WEAPON;
    }

    private static boolean isArmorKind(EquipmentKind kind) {
        return kind == EquipmentKind.ARMOR || kind == EquipmentKind.HELMET
                || kind == EquipmentKind.CHESTPLATE || kind == EquipmentKind.LEGGINGS
                || kind == EquipmentKind.BOOTS || kind == EquipmentKind.SHIELD;
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
                                      double durabilityBonus, double breakSpeedBonus,
                                      double excavationTimeReduction, double criticalChance,
                                      double attackSpeedBonus, double armorBonus, double toughnessBonus) {
        ItemLore current = stack.getOrDefault(DataComponents.LORE, new ItemLore(List.of()));
        List<Component> lines = new ArrayList<>(current.lines());
        int generatedLines = 0;
        lines.add(Component.translatable("item.simpleskills.crafting_rarity_label",
                Component.translatable("item.simpleskills.crafting_rarity." + rarity.id()))
                .withStyle(rarity.color()).withStyle(style -> style.withItalic(false)));
        generatedLines++;

        // Build every generated stat line by stable ID first, then append them using the
        // attribute order persisted on the item. A replacement roll changes the attribute
        // type in-place instead of moving it to a hard-coded sword/pickaxe/armor position.
        Map<String, Component> statLines = new HashMap<>();
        List<String> fallbackStatOrder = new ArrayList<>();
        CompoundTag rolledStats = getTag(stack);

        boolean runicEdge = getPerkAmplifier(stack, "runic_edge") >= 0;
        double damageMin = rarity.minDamageBonus() * 100.0;
        double damageMax = rarity.maxDamageBonus() * 100.0;
        if (runicEdge) {
            damageMin = ((1.0 + rarity.minDamageBonus()) * 1.08 - 1.0) * 100.0;
            damageMax = ((1.0 + rarity.maxDamageBonus()) * 1.08 - 1.0) * 100.0;
        }
        if (damageBonus > 0.0) {
            statLines.put("damage", rangeStatLine("item.simpleskills.crafting_stat.damage",
                    damageBonus * 100.0, damageMin, damageMax));
            fallbackStatOrder.add("damage");
        }
        if (durabilityBonus > 0.0) {
            statLines.put("durability", rangeStatLine("item.simpleskills.crafting_stat.durability",
                    durabilityBonus * 100.0, rarity.minDurabilityBonus() * 100.0,
                    rarity.maxDurabilityBonus() * 100.0));
            fallbackStatOrder.add("durability");
        }
        if (breakSpeedBonus > 0.0) {
            statLines.put("breakSpeed", rangeStatLine("item.simpleskills.crafting_stat.break_speed",
                    breakSpeedBonus * 100.0, rarity.minBreakSpeedBonus() * 100.0,
                    rarity.maxBreakSpeedBonus() * 100.0));
            fallbackStatOrder.add("breakSpeed");
        }
        if (excavationTimeReduction > 0.0 && classify(stack) == EquipmentKind.SHOVEL) {
            statLines.put("excavationTime", rangeStatLine("item.simpleskills.crafting_stat.excavation_time",
                    excavationTimeReduction * 100.0, rarity.minBreakSpeedBonus() * 100.0,
                    rarity.maxBreakSpeedBonus() * 100.0));
            fallbackStatOrder.add("excavationTime");
        }
        // Critical Mastery can grant a chance by itself. Do not render that perk-only
        // effect as an additional green attribute line; its perk line already describes it.
        if (rolledStats.getDoubleOr(DATA_CRITICAL_CHANCE, 0.0) > 0.0 && criticalChance > 0.0) {
            int mastery = getPerkAmplifier(stack, "critical_mastery");
            double masteryBonus = mastery >= 0 ? 5.0 + mastery * 5.0 : 0.0;
            statLines.put("critical", rangeStatLine("item.simpleskills.crafting_stat.critical_chance",
                    criticalChance * 100.0,
                    Math.min(60.0, rarity.minCriticalChance() * 100.0 + masteryBonus),
                    Math.min(60.0, rarity.maxCriticalChance() * 100.0 + masteryBonus)));
            fallbackStatOrder.add("critical");
        }
        if (attackSpeedBonus > 0.0) {
            statLines.put("attackSpeed", rangeStatLine("item.simpleskills.crafting_stat.attack_speed",
                    attackSpeedBonus * 100.0, rarity.minAttackSpeedBonus() * 100.0,
                    rarity.maxAttackSpeedBonus() * 100.0));
            fallbackStatOrder.add("attackSpeed");
        }
        if (armorBonus > 0.0) {
            statLines.put("armor", rangeStatLine("item.simpleskills.crafting_stat.armor",
                    armorBonus * 100.0, rarity.minArmorBonus() * 100.0, rarity.maxArmorBonus() * 100.0));
            fallbackStatOrder.add("armor");
        }
        if (toughnessBonus > 0.0) {
            statLines.put("toughness", rangeStatLine("item.simpleskills.crafting_stat.armor_toughness",
                    toughnessBonus * 100.0, rarity.minToughnessBonus() * 100.0,
                    rarity.maxToughnessBonus() * 100.0));
            fallbackStatOrder.add("toughness");
        }

        Component percentLine = percentTooltip(rolledStats, "armorPenetration", DATA_ARMOR_PENETRATION,
                "item.simpleskills.crafting_stat.armor_penetration");
        if (percentLine != null) { statLines.put("armorPenetration", percentLine); fallbackStatOrder.add("armorPenetration"); }
        percentLine = percentTooltip(rolledStats, "combatFrenzy", DATA_COMBAT_FRENZY,
                "item.simpleskills.crafting_stat.combat_frenzy");
        if (percentLine != null) { statLines.put("combatFrenzy", percentLine); fallbackStatOrder.add("combatFrenzy"); }
        percentLine = percentTooltip(rolledStats, "firstStrike", DATA_FIRST_STRIKE,
                "item.simpleskills.crafting_stat.first_strike");
        if (percentLine != null) { statLines.put("firstStrike", percentLine); fallbackStatOrder.add("firstStrike"); }
        percentLine = percentTooltip(rolledStats, "rhythmicDischarge", DATA_RHYTHMIC_DISCHARGE,
                "item.simpleskills.crafting_stat.rhythmic_discharge");
        if (percentLine != null) { statLines.put("rhythmicDischarge", percentLine); fallbackStatOrder.add("rhythmicDischarge"); }
        percentLine = percentTooltip(rolledStats, "tenacity", DATA_TENACITY,
                "item.simpleskills.crafting_stat.tenacity");
        if (percentLine != null) { statLines.put("tenacity", percentLine); fallbackStatOrder.add("tenacity"); }
        String mobilityType = rolledStats.getStringOr(DATA_MOBILITY_TYPE, "walk");
        percentLine = percentTooltip(rolledStats, "mobility", DATA_MOBILITY,
                "item.simpleskills.crafting_stat.mobility_" + mobilityType);
        if (percentLine != null) { statLines.put("mobility", percentLine); fallbackStatOrder.add("mobility"); }

        double attackReach = rolledStats.getDoubleOr(DATA_ATTACK_REACH, 0.0);
        if (attackReach > 0.0) {
            double[] range = statRange(rarity, "attackReach");
            statLines.put("attackReach", rangeBlockStatLine("item.simpleskills.crafting_stat.attack_reach",
                    attackReach, range[0], range[1]));
            fallbackStatOrder.add("attackReach");
        }
        double elemental = rolledStats.getDoubleOr(DATA_ELEMENTAL_RESISTANCE, 0.0);
        if (elemental > 0.0) {
            String type = rolledStats.getStringOr(DATA_ELEMENTAL_TYPE, "fire");
            double[] range = statRange(rarity, "elementalResistance");
            statLines.put("elementalResistance", rangeStatLine(elementalResistanceTranslationKey(type),
                    elemental * 100.0, range[0] * 100.0, range[1] * 100.0));
            fallbackStatOrder.add("elementalResistance");
        }

        List<String> statOrder = resolveAttributeOrder(rolledStats, fallbackStatOrder);
        for (String statId : statOrder) {
            Component line = statLines.get(statId);
            if (line != null) {
                lines.add(line);
                generatedLines++;
            }
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
            } else if (id.equals("eternal_guardian")) {
                perkLine = Component.translatable(key, 10 + amplifier * 5,
                        roman(Math.min(5, 3 + amplifier)), roman(Math.min(5, 2 + amplifier)));
            } else if (id.equals("aegis_of_ages")) {
                perkLine = Component.translatable(key, roman(amplifier + 1),
                        roman(Math.min(5, 3 + amplifier)), roman(Math.min(4, 2 + amplifier)));
            } else if (NO_LEVEL_PERKS.contains(id)) {
                perkLine = Component.translatable(key);
            } else {
                perkLine = Component.translatable(key, roman(amplifier + 1));
            }
            ChatFormatting perkColor = Set.of("celestial_wings", "celestial_range", "oracle_vision",
                    "aegis_of_purity", "phantom_stride", "divine_reflection", "astral_pursuit",
                    "singularity_bolt", "poseidons_wrath", "worldbreaker", "buried_truth",
                    "edens_dominion", "midas_touch", "abyssal_sovereign",
                    "thermal_smelting").contains(id)
                    ? ChatFormatting.GOLD : ChatFormatting.AQUA;
            lines.add(perkLine.withStyle(perkColor));
            generatedLines++;
        }
        stack.set(DataComponents.LORE, new ItemLore(lines));
        final int persistedGeneratedLines = generatedLines;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(DATA_LORE_LINE_COUNT, persistedGeneratedLines);
            tag.putInt(DATA_LORE_SCHEMA_VERSION, CURRENT_CRAFTING_LORE_SCHEMA);
        });
    }

    private static Component percentTooltip(CompoundTag tag, String field,
                                            String dataKey, String translationKey) {
        double value = tag.getDoubleOr(dataKey, 0.0);
        if (value <= 0.0) return null;
        CraftingRarity rarity = CraftingRarity.byId(tag.getStringOr(DATA_RARITY, ""));
        double[] bounds = statRange(rarity, field);
        if (bounds == null) return null;
        return rangeStatLine(translationKey, value * 100.0, bounds[0] * 100.0, bounds[1] * 100.0);
    }

    private static Component rangeBlockStatLine(String translationKey, double value,
                                                double minimum, double maximum) {
        boolean maxRoll = Math.abs(value - maximum) <= 0.0001;
        ChatFormatting color = maxRoll ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        MutableComponent line = Component.translatable(translationKey).withStyle(color);
        line.append(Component.literal(": +" + String.format(java.util.Locale.ROOT, "%.2f", value)
                + " blocks").withStyle(color));
        line.append(Component.literal(" [" + String.format(java.util.Locale.ROOT, "%.2f", minimum)
                + " - " + String.format(java.util.Locale.ROOT, "%.2f", maximum) + " blocks]")
                .withStyle(ChatFormatting.GRAY));
        return line;
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
        List<String> ordered = new ArrayList<>();
        for (String entry : encoded.split(",")) {
            if (!entry.isBlank()) ordered.add(entry);
        }
        return List.copyOf(ordered);
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
            updateCombatFrenzy(player, now);
            if (player.tickCount % 4 == 0) tickVeinResonance(player, now);
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
                    // Higher equipment/perk ranks improve both defensive effects.
                    effects.merge("minecraft:resistance", Math.min(4, 2 + amplifier), Math::max);
                    effects.merge("minecraft:regeneration", Math.min(3, 1 + amplifier), Math::max);
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
        if (encoded.isBlank()) return List.of();
        List<String> ordered = new ArrayList<>();
        for (String entry : encoded.split(",")) {
            if (!entry.isBlank()) ordered.add(entry);
        }
        return List.copyOf(ordered);
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
            case "excavation_inertia" -> EXCAVATION_INERTIA;
            case "extended_reach" -> EXTENDED_REACH;
            case "vein_resonance" -> VEIN_RESONANCE;
            case "thermal_smelting" -> THERMAL_SMELTING;
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

    public static boolean hasPerk(ItemStack stack, String perkId) {
        return getPerkAmplifier(stack, perkId) >= 0;
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

    public static float getExcavationInertiaMultiplier(Player player, net.minecraft.world.level.block.state.BlockState state) {
        if (player == null || state == null || player.isSpectator()) return 1.0F;
        ItemStack tool = player.getMainHandItem();
        int amplifier = getPerkAmplifier(tool, "excavation_inertia");
        if (amplifier < 0) return 1.0F;
        String key = player.getUUID() + ":" + player.level().dimension().identifier();
        InertiaState progress = EXCAVATION_INERTIA_STATES.computeIfAbsent(key, ignored -> new InertiaState());
        int tick = player.tickCount;
        int stateHash = state.hashCode();
        if (progress.lastTick != tick) {
            if (progress.lastTick != tick - 1 || progress.lastStateHash != stateHash) {
                progress.continuousTicks = 0;
            } else {
                progress.continuousTicks = Math.min(40, progress.continuousTicks + 1);
            }
            progress.lastTick = tick;
            progress.lastStateHash = stateHash;
        }
        double ramp = Math.min(1.0, progress.continuousTicks / 40.0);
        double maximumBonus = (amplifier + 1) * 0.10;
        progress.lastMultiplier = (float) (1.0 + maximumBonus * ramp);
        return progress.lastMultiplier;
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

    public static double getArmorPenetration(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty() || !hasRarity(weapon)) return 0.0;
        return Math.max(0.0, Math.min(0.60, getTag(weapon).getDoubleOr(DATA_ARMOR_PENETRATION, 0.0)));
    }

    public static double getTenacityReduction(Player player) {
        if (player == null) return 0.0;
        double total = 0.0;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND)) {
            ItemStack armor = player.getItemBySlot(slot);
            total += getTag(armor).getDoubleOr(DATA_TENACITY, 0.0);
        }
        return Math.min(0.60, Math.max(0.0, total));
    }

    public static double getElementalDamageReduction(Player player, DamageSource source) {
        if (player == null || source == null) return 0.0;
        String matchingType = source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ? "fire"
                : source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) ? "explosion"
                : source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) ? "projectile"
                : source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO) ? "magic" : "";
        if (matchingType.isBlank()) return 0.0;
        double total = 0.0;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND)) {
            CompoundTag armor = getTag(player.getItemBySlot(slot));
            if (matchingType.equals(armor.getStringOr(DATA_ELEMENTAL_TYPE, ""))) {
                total += armor.getDoubleOr(DATA_ELEMENTAL_RESISTANCE, 0.0);
            }
        }
        return Math.min(0.60, Math.max(0.0, total));
    }

    public static MobEffectInstance applyTenacityToEffect(Player player, MobEffectInstance effect) {
        if (player == null || player.level().isClientSide() || effect == null
                || effect.getEffect().value().getCategory()
                != net.minecraft.world.effect.MobEffectCategory.HARMFUL) return effect;
        double reduction = getTenacityReduction(player);
        if (reduction <= 0.0 || effect.getDuration() <= 1) return effect;
        return effect.withScaledDuration((float) (1.0 - reduction));
    }

    /** Applies Tenacity to the incremental Powder Snow freeze meter without rounding away small reductions. */
    public static int reduceFreezeMeterProgress(Player player, int currentTicks, int requestedTicks) {
        if (player == null || player.level().isClientSide()) return requestedTicks;
        int delta = requestedTicks - currentTicks;
        if (delta <= 0) {
            FREEZE_TENACITY_REMAINDER.remove(player.getUUID());
            return requestedTicks;
        }
        double reduction = getTenacityReduction(player);
        UUID id = player.getUUID();
        if (reduction <= 0.0) {
            FREEZE_TENACITY_REMAINDER.remove(id);
            return requestedTicks;
        }
        double accumulated = delta * (1.0 - reduction)
                + FREEZE_TENACITY_REMAINDER.getOrDefault(id, 0.0);
        int appliedTicks = (int) Math.floor(accumulated + 1.0E-9);
        FREEZE_TENACITY_REMAINDER.put(id, Math.max(0.0, accumulated - appliedTicks));
        return Math.min(requestedTicks, currentTicks + appliedTicks);
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
        if (target instanceof LivingEntity living) {
            if (isBoss(living) && getPerkAmplifier(stack, "godslayer") >= 0) multiplier *= 1.25;
            double firstStrike = tag.getDoubleOr(DATA_FIRST_STRIKE, 0.0);
            if (firstStrike > 0.0 && living.isAlive()
                    && living.getHealth() >= living.getMaxHealth() - 0.001F) {
                multiplier *= 1.0 + firstStrike;
            }
        }
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
     * Returns the percentage reduction to sand investigation time rolled on a shovel.
     * The value is stored as a fraction (0.20 = 20%) and is capped to preserve a short
     * but non-instant investigation even with the strongest equipment.
     */
    public static double getExcavationTimeReduction(ItemStack stack) {
        if (stack == null || stack.isEmpty() || classify(stack) != EquipmentKind.SHOVEL) return 0.0;
        double reduction = getTag(stack).getDoubleOr(DATA_EXCAVATION_TIME, 0.0);
        return Math.max(0.0, Math.min(0.70, reduction));
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
        double firstStrikeBonus = getTag(weapon).getDoubleOr(DATA_FIRST_STRIKE, 0.0);
        double armorPenetration = getArmorPenetration(weapon);
        PROJECTILE_DATA.put(projectile.getUUID(), new ProjectileDamageData(
                multiplier, firstStrikeBonus, armorPenetration, encodedPerks,
                now + PROJECTILE_BONUS_LIFETIME_TICKS));
        TranscendentAbilities.onProjectileFired(projectile, weapon, encodedPerks);
    }

    /** Registers thrown tridents once, retaining the crafted weapon's affixes after it leaves the hand. */
    public static void registerThrownTridentDamageBonus(
            net.minecraft.world.entity.projectile.arrow.ThrownTrident trident) {
        if (trident == null || trident.level().isClientSide()
                || PROJECTILE_DATA.containsKey(trident.getUUID())) return;
        registerProjectileDamageBonus(trident, trident.getWeaponItem());
    }

    public static double getProjectileArmorPenetration(Projectile projectile) {
        if (projectile == null) return 0.0;
        ProjectileDamageData data = PROJECTILE_DATA.get(projectile.getUUID());
        if (data == null || data.expiresAt() <= currentServerTime(projectile)) return 0.0;
        return Math.max(0.0, Math.min(0.60, data.armorPenetration()));
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
        if (target instanceof LivingEntity living && data != null) {
            if (isBoss(living) && containsPerk(data.encodedPerks(), "godslayer")) multiplier *= 1.25;
            if (data.firstStrikeBonus() > 0.0 && living.isAlive()
                    && living.getHealth() >= living.getMaxHealth() - 0.001F) {
                multiplier *= 1.0 + data.firstStrikeBonus();
            }
        }
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
        FREEZE_TENACITY_REMAINDER.remove(player.getUUID());
        COMBAT_FRENZY.remove(player.getUUID());
        RHYTHMIC_COMBOS.remove(player.getUUID());
        EXCAVATION_INERTIA_STATES.keySet().removeIf(key -> key.startsWith(player.getUUID().toString() + ":"));
        TranscendentAbilities.clearPlayer(player.getUUID());
    }

    public static boolean hasRarity(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !getRarityId(stack).isBlank();
    }

    public static String getRarityId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return getTag(stack).getStringOr(DATA_RARITY, "");
    }

    /**
     * Applies a fixed high-tier rarity to equipment generated by Abyssal chest loot tables.
     * The loot table only supplies the requested tier marker; all real stats, perks, durability,
     * name and lore are rolled by the same pipeline as crafted equipment.
     */
    public static boolean applyAbyssalLootRarity(ItemStack stack, ServerPlayer player) {
        if (stack == null || stack.isEmpty() || player == null
                || stack.get(DataComponents.MAX_DAMAGE) == null || hasRarity(stack)) {
            return false;
        }

        CompoundTag data = getTag(stack);
        CraftingRarity rarity = CraftingRarity.byId(data.getStringOr(DATA_ABYSSAL_LOOT_RARITY, ""));
        if (rarity != CraftingRarity.MYTHIC && rarity != CraftingRarity.TRANSCENDENT) {
            return false;
        }

        // Consume the one-shot marker before the normal pipeline writes the final rarity data.
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.remove(DATA_ABYSSAL_LOOT_RARITY));
        applyEquipmentRarity(stack, player, rarity, getCraftingLevel(player), false);
        return true;
    }

    /** Returns the Indestructible perk chance without rolling it. */
    public static double getIndestructibleChance(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0;
        int level = getPerkAmplifier(stack, "indestructible");
        if (level < 0) return 0.0;
        return Math.min(0.45, 0.10 + level * 0.10);
    }

    /**
     * Rolls Indestructible for durability damage outside the normal gathering-block path.
     * Gathering block damage combines this chance with profession protection in one roll.
     */
    public static boolean shouldPreventDurabilityLoss(ItemStack stack, ServerPlayer player) {
        return stack != null && !stack.isEmpty() && player != null
                && player.getRandom().nextDouble() < getIndestructibleChance(stack);
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
        if (attacker == null) return;

        if (!encodedPerks.isBlank()) {
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

        ItemStack activeWeapon = attacker.getMainHandItem();
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident trident) {
            ItemStack thrownWeapon = trident.getWeaponItem();
            if (!thrownWeapon.isEmpty() && hasRarity(thrownWeapon)) activeWeapon = thrownWeapon;
        }
        if (victim.isDeadOrDying()) activateCombatFrenzy(attacker, activeWeapon);
        triggerRhythmicDischarge(attacker, victim, activeWeapon);
    }


    private static void activateCombatFrenzy(ServerPlayer player, ItemStack weapon) {
        double bonus = getTag(weapon).getDoubleOr(DATA_COMBAT_FRENZY, 0.0);
        if (bonus <= 0.0) return;
        long now = player.level().getServer() == null ? player.level().getGameTime()
                : player.level().getServer().overworld().getGameTime();
        FrenzyState previous = COMBAT_FRENZY.get(player.getUUID());
        int stacks = previous == null || previous.expiresAt() <= now ? 1 : Math.min(3, previous.stacks() + 1);
        COMBAT_FRENZY.put(player.getUUID(), new FrenzyState(stacks, now + 80L, bonus));
        updateCombatFrenzy(player, now);
    }

    private static void updateCombatFrenzy(ServerPlayer player, long now) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return;
        FrenzyState state = COMBAT_FRENZY.get(player.getUUID());
        attackSpeed.removeModifier(COMBAT_FRENZY_MODIFIER_ID);
        if (state == null) return;
        if (state.expiresAt() <= now) {
            COMBAT_FRENZY.remove(player.getUUID());
            return;
        }
        double totalBonus = Math.min(0.90, state.stacks() * state.perStackBonus());
        if (totalBonus > 0.0) {
            attackSpeed.addTransientModifier(new AttributeModifier(
                    COMBAT_FRENZY_MODIFIER_ID, totalBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static void triggerRhythmicDischarge(ServerPlayer attacker, LivingEntity victim, ItemStack weapon) {
        double chance = getTag(weapon).getDoubleOr(DATA_RHYTHMIC_DISCHARGE, 0.0);
        if (chance <= 0.0 || !(attacker.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        ComboState previous = RHYTHMIC_COMBOS.get(attacker.getUUID());
        int hits = previous != null && previous.target().equals(victim.getUUID())
                && now - previous.lastTick() <= 40L ? previous.hits() + 1 : 1;
        RHYTHMIC_COMBOS.put(attacker.getUUID(), new ComboState(victim.getUUID(), hits, now));
        if (hits < 3) return;
        RHYTHMIC_COMBOS.put(attacker.getUUID(), new ComboState(victim.getUUID(), 0, now));
        if (attacker.getRandom().nextDouble() >= chance) return;

        int perkRank = Math.max(1, CraftingRarity.byId(getRarityId(weapon)) == null ? 1
                : CraftingRarity.byId(getRarityId(weapon)).ordinal() + 1);
        int mode = attacker.getRandom().nextInt(3);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                victim.getBoundingBox().inflate(4.0), entity -> entity != attacker && entity.isAlive()
                        && !entity.isAlliedTo(attacker));
        if (mode == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                    24, 0.7, 0.5, 0.7, 0.12);
            for (LivingEntity target : nearby) {
                applyBonusDamage(target, attacker, 2.0F + perkRank);
            }
            level.playSound(null, victim.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 0.65F, 1.4F);
        } else if (mode == 1) {
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    victim.getX(), victim.getY() + 0.8, victim.getZ(),
                    28, 1.0, 0.45, 1.0, 0.02);
            for (LivingEntity target : nearby) addEffectToLiving(target, "minecraft:slowness",
                    Math.min(2, (perkRank - 1) / 2), 35 + perkRank * 15);
            level.playSound(null, victim.blockPosition(), SoundEvents.ANVIL_USE,
                    SoundSource.PLAYERS, 0.6F, 1.6F);
        } else {
            level.sendParticles(ParticleTypes.FLAME,
                    victim.getX(), victim.getY() + 0.8, victim.getZ(),
                    24, 0.8, 0.45, 0.8, 0.04);
            for (LivingEntity target : nearby) target.igniteForSeconds(1.5F + perkRank * 0.5F);
            level.playSound(null, victim.blockPosition(), SoundEvents.ANVIL_USE,
                    SoundSource.PLAYERS, 0.6F, 1.25F);
        }
    }

    /** Starts a two-second local ore highlight pulse when a Resonance pickaxe breaks an ore. */
    public static void triggerVeinResonance(ServerPlayer player, net.minecraft.core.BlockPos origin) {
        if (player == null || origin == null || !(player.level() instanceof ServerLevel level)) return;
        ItemStack tool = player.getMainHandItem();
        int amplifier = getPerkAmplifier(tool, "vein_resonance");
        if (amplifier < 0) return;
        int radius = Math.min(10, 6 + amplifier * 2);
        List<net.minecraft.core.BlockPos> ores = new ArrayList<>();
        int radiusSq = radius * radius;
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (pos.distToCenterSqr(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5) > radiusSq) continue;
            BlockState candidateState = level.getBlockState(pos);
            if (!candidateState.is(BlockTags.ORES) && !candidateState.is(COMMON_ORE_BLOCKS)) continue;
            if (pos.equals(origin)) continue;
            if (ores.size() >= 48) break;
            ores.add(pos.immutable());
        }
        if (!ores.isEmpty()) {
            VEIN_RESONANCE_STATES.put(player.getUUID(), new VeinResonanceState(List.copyOf(ores),
                    level.getGameTime() + 40L, amplifier));
        }
    }

    private static void tickVeinResonance(ServerPlayer player, long now) {
        VeinResonanceState state = VEIN_RESONANCE_STATES.get(player.getUUID());
        if (state == null) return;
        if (state.expiresAt() <= now) {
            VEIN_RESONANCE_STATES.remove(player.getUUID());
            return;
        }
        ServerLevel level = player.level();
        for (net.minecraft.core.BlockPos ore : state.ores()) {
            BlockState oreState = level.getBlockState(ore);
            if (!oreState.is(BlockTags.ORES) && !oreState.is(COMMON_ORE_BLOCKS)) continue;
            double x = ore.getX() + 0.5;
            double y = ore.getY() + 0.55;
            double z = ore.getZ() + 0.5;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z,
                    1 + Math.min(3, state.amplifier()), 0.18, 0.18, 0.18, 0.005);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                    1, 0.25, 0.25, 0.25, 0.01);
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
        double chance = Math.min(0.25, 0.10 + guardian * 0.05);
        if (player.getRandom().nextDouble() >= chance) return true;

        ETERNAL_GUARDIAN_READY_AT.put(player.getUUID(), now + 6000L);
        player.setHealth(1.0F);
        // Rank I starts at Regeneration III / Resistance II; higher ranks improve
        // the chance and can raise the protective effects by one level.
        addEffect(player, "minecraft:regeneration", Math.min(4, 2 + guardian));
        addEffect(player, "minecraft:resistance", Math.min(4, 1 + guardian));
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
