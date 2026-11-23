package com.github.ob_yekt.simpleskills.crafting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.mixin.LivingEntityDamageCooldownAccessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
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
    private static final String DATA_CRITICAL_CHANCE = "simpleskills_crafting_critical_chance";
    private static final String DATA_ATTACK_SPEED = "simpleskills_crafting_attack_speed_bonus";
    private static final String DATA_PERKS = "simpleskills_crafting_perks";
    private static final int EFFECT_DURATION_TICKS = 40;
    private static final int EFFECT_REFRESH_INTERVAL = 10;
    private static final long PROJECTILE_BONUS_LIFETIME_TICKS = 1200L;
    private static final Identifier CRAFTING_DAMAGE_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_damage_bonus");
    private static final Identifier CRAFTING_ATTACK_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_attack_speed_bonus");
    private static final Identifier VITALITY_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_vitality");
    private static final Identifier RESILIENCE_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "crafting_resilience");

    private static final Map<UUID, ProjectileDamageData> PROJECTILE_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ETERNAL_GUARDIAN_READY_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> APPLIED_VITALITY = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> APPLIED_RESILIENCE = new ConcurrentHashMap<>();
    private static final Set<UUID> BONUS_DAMAGE_GUARD = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, PendingRevealBatch> PENDING_REVEALS = new ConcurrentHashMap<>();
    private static final int REVEAL_BATCH_IDLE_TICKS = 2;
    private static final int MAX_REVEALS_PER_PACKET = 24;

    private static final class PendingRevealBatch {
        private final JsonArray entries = new JsonArray();
        private long lastQueuedAt;
    }

    private record ProjectileDamageData(double multiplier, String encodedPerks, long expiresAt) {}
    private enum EquipmentKind { WEAPON, ARMOR, SHIELD, TOOL, OTHER }
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

    private static final Set<String> NO_LEVEL_PERKS = Set.of(
            "runic_edge", "anvil_ward",
            "guardian_oath", "arcane_mastery", "godslayer", "eternal_guardian",
            "divine_blessing", "aegis_of_ages", "fire_resistance", "featherweight");

    private CraftingEquipmentManager() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(CraftingRevealPayload.TYPE, CraftingRevealPayload.CODEC);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CraftingEquipmentManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            APPLIED_VITALITY.remove(id);
            APPLIED_RESILIENCE.remove(id);
            PENDING_REVEALS.remove(id);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CraftingEquipmentManager::afterDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CraftingEquipmentManager::preventLethalDamage);
        String problem = CraftingRarity.validateDistribution();
        if (problem != null) Simpleskills.LOGGER.error("Invalid Crafting rarity distribution: {}", problem);
    }

    /** Applies rarity exactly once to a new durable crafting result. */
    public static void applyCraftedEquipment(ItemStack stack, ServerPlayer player) {
        if (stack == null || stack.isEmpty() || player == null || stack.get(DataComponents.MAX_DAMAGE) == null) return;
        if (hasRarity(stack)) return;

        int level = Math.max(1, Math.min(99,
                com.github.ob_yekt.simpleskills.managers.XPManager.getSkillLevel(
                        player.getStringUUID(), com.github.ob_yekt.simpleskills.Skills.CRAFTING)));
        CraftingRarity rarity = CraftingRarity.roll(level, player.getRandom().nextDouble() * 100.0);
        EquipmentKind kind = classify(stack);
        List<Perk> perks = rollPerks(kind, rarity, level, player);

        boolean weapon = isWeapon(stack);
        boolean sword = stack.is(ItemTags.SWORDS);
        // Every newly forged item rolls its own values inside its rarity's stat ranges.
        // Fractions are rounded to 0.1 percentage-point steps to keep the results varied but readable.
        double damageBonus = weapon
                ? rollStat(player, rarity.minDamageBonus(), rarity.maxDamageBonus(), 0.001) : 0.0;
        double durabilityBonus = rollStat(player, rarity.minDurabilityBonus(), rarity.maxDurabilityBonus(), 0.01);
        double criticalChance = weapon
                ? rollStat(player, rarity.minCriticalChance(), rarity.maxCriticalChance(), 0.001) : 0.0;
        double attackSpeedBonus = sword
                ? rollStat(player, rarity.minAttackSpeedBonus(), rarity.maxAttackSpeedBonus(), 0.005) : 0.0;
        String encodedPerks = encodePerks(perks, level, rarity, player);

        // Runic Edge is included in the same final percentage shown in the lore and native attribute tooltip.
        boolean hasRunicEdge = perks.stream().anyMatch(perk -> perk.id().equals("runic_edge"));
        double totalDamageBonus = (1.0 + damageBonus) * (hasRunicEdge ? 1.08 : 1.0) - 1.0;
        boolean damageAppliedAsAttribute = applyAttackDamageAttributeBonus(stack, totalDamageBonus);
        boolean attackSpeedAppliedAsAttribute = sword && applyAttackSpeedAttributeBonus(stack, attackSpeedBonus);
        if (!attackSpeedAppliedAsAttribute) attackSpeedBonus = 0.0;
        final double finalAttackSpeedBonus = attackSpeedBonus;

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(DATA_RARITY, rarity.id());
            tag.putDouble(DATA_DAMAGE, damageBonus);
            tag.putDouble(DATA_DURABILITY, durabilityBonus);
            tag.putDouble(DATA_CRITICAL_CHANCE, criticalChance);
            tag.putDouble(DATA_ATTACK_SPEED, finalAttackSpeedBonus);
            tag.putString(DATA_PERKS, encodedPerks);
            tag.putBoolean(DATA_DAMAGE_AS_ATTRIBUTE, damageAppliedAsAttribute);
            tag.putBoolean(DATA_DAMAGE_ATTRIBUTE_INCLUDES_AFFIXES, damageAppliedAsAttribute);
        });

        Integer currentMaximum = stack.get(DataComponents.MAX_DAMAGE);
        if (currentMaximum != null && durabilityBonus > 0.0) {
            stack.set(DataComponents.MAX_DAMAGE, Math.max(1,
                    (int) Math.round(currentMaximum * (1.0 + durabilityBonus))));
        }

        Component originalName = stack.getHoverName();
        Component rarityName = Component.translatable("item.simpleskills.crafting_rarity." + rarity.id());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "item.simpleskills.crafted_quality_name", rarityName, originalName)
                .withStyle(style -> style.withColor(rarity.color()).withItalic(false)));
        double displayedCriticalChance = getEffectiveCriticalChance(stack);
        // Display the combined rarity + Runic Edge percentage so it matches the real main-hand attribute modifier.
        appendTooltip(stack, rarity, totalDamageBonus, durabilityBonus, displayedCriticalChance, attackSpeedBonus);

        if (weapon) unlockWeaponTitle(player, rarity);
        sendReveal(player, stack, originalName.getString(), rarity, totalDamageBonus, durabilityBonus,
                displayedCriticalChance, attackSpeedBonus, encodedPerks);
    }

    /**
     * Collects all high-rarity results from a short crafting burst. The client receives one batch,
     * so closing the reveal screen cannot cause later packets from the same shift-click to reopen it.
     */
    private static void sendReveal(ServerPlayer player, ItemStack stack, String itemName, CraftingRarity rarity,
                                   double damageBonus, double durabilityBonus, double criticalChance,
                                   double attackSpeedBonus, String encodedPerks) {
        // Common and Rare are intentionally silent; the reveal screen is reserved for Unique+.
        if (rarity.ordinal() < CraftingRarity.UNIQUE.ordinal()) return;
        if (!ServerPlayNetworking.canSend(player, CraftingRevealPayload.TYPE)) return;

        JsonObject details = new JsonObject();
        details.addProperty("damage", damageBonus * 100.0);
        details.addProperty("durability", (int) Math.round(durabilityBonus * 100.0));
        details.addProperty("critical", criticalChance * 100.0);
        details.addProperty("attackSpeed", attackSpeedBonus * 100.0);
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

    /** Gathering axes are tools for affix selection, while still counting as weapons for damage/title stats. */
    private static EquipmentKind classify(ItemStack stack) {
        if (stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) return EquipmentKind.TOOL;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("shield")) return EquipmentKind.SHIELD;
        if (stack.has(DataComponents.EQUIPPABLE)) return EquipmentKind.ARMOR;
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
            case WEAPON -> new ArrayList<>(List.of(STRENGTH, SPEED, REGENERATION, VAMPIRIC,
                    CRITICAL_MASTERY, THUNDEROUS, FROSTBITE, PHOENIX, DOUBLE_LEAP, WINDWALKER));
            case ARMOR -> new ArrayList<>(List.of(PROTECTION, RESILIENT, SPEED, REGENERATION,
                    FEATHERWEIGHT, VITALITY, FIRE_RESISTANCE, JUMP_BOOST, DOUBLE_LEAP));
            case SHIELD -> new ArrayList<>(List.of(PROTECTION, RESILIENT, REGENERATION, VITALITY,
                    FIRE_RESISTANCE, DOUBLE_LEAP));
            case TOOL -> new ArrayList<>(List.of(HASTE, EFFICIENT, SPEED, FORTUNE_MASTERY,
                    INDESTRUCTIBLE, TREASURE_HUNTER));
            default -> new ArrayList<>(List.of(SPEED, REGENERATION, VITALITY,
                    HASTE, EFFICIENT, INDESTRUCTIBLE, FORTUNE_MASTERY, TREASURE_HUNTER));
        };
    }

    private static List<Perk> uniquePool(EquipmentKind kind) {
        return switch (kind) {
            case WEAPON -> List.of(RUNIC_EDGE);
            case ARMOR -> List.of(ANVIL_WARD);
            case SHIELD -> List.of(GUARDIAN_OATH);
            case TOOL -> List.of(PROSPECTOR);
            default -> List.of(ANVIL_WARD);
        };
    }

    private static List<Perk> transcendentPool(EquipmentKind kind) {
        return switch (kind) {
            case WEAPON -> List.of(TITAN_STRENGTH, GODSLAYER, ARCANE_MASTERY);
            case ARMOR -> List.of(DIVINE_BLESSING, CELESTIAL_MOVEMENT, ETERNAL_GUARDIAN);
            case SHIELD -> List.of(DIVINE_BLESSING, ETERNAL_GUARDIAN, AEGIS_OF_AGES);
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

        int guaranteedGeneral = Math.max(0, rarity.minimumPerks() - selected.size());
        int maximum = Math.min(rarity.maximumPerks(), generalPool.size() + selected.size());
        for (int i = 0; i < guaranteedGeneral && !generalPool.isEmpty(); i++) {
            selected.add(generalPool.remove(player.getRandom().nextInt(generalPool.size())));
        }

        // Extra affixes become more likely with every Crafting level, without exceeding the tier cap.
        double extraPerkChance = Math.min(0.99, 0.05 + level * 0.0095);
        while (selected.size() < maximum && !generalPool.isEmpty()
                && player.getRandom().nextDouble() < extraPerkChance) {
            selected.add(generalPool.remove(player.getRandom().nextInt(generalPool.size())));
        }
        return List.copyOf(selected);
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
        if (stack == null || stack.isEmpty() || !stack.is(ItemTags.SWORDS) || speedBonus <= 0.0) return false;
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

    private static String formatPercent(double percentage) {
        double nearestWhole = Math.rint(percentage);
        return Math.abs(percentage - nearestWhole) < 0.05
                ? String.format(java.util.Locale.ROOT, "%.0f", nearestWhole)
                : String.format(java.util.Locale.ROOT, "%.1f", percentage);
    }

    private static void appendTooltip(ItemStack stack, CraftingRarity rarity, double damageBonus,
                                      double durabilityBonus, double criticalChance, double attackSpeedBonus) {
        ItemLore current = stack.getOrDefault(DataComponents.LORE, new ItemLore(List.of()));
        List<Component> lines = new ArrayList<>(current.lines());
        lines.add(Component.translatable("item.simpleskills.crafting_rarity_label",
                Component.translatable("item.simpleskills.crafting_rarity." + rarity.id()))
                .withStyle(rarity.color()).withStyle(style -> style.withItalic(false)));
        if (damageBonus > 0.0) {
            lines.add(Component.translatable("item.simpleskills.crafting_stat.damage",
                    formatPercent(damageBonus * 100.0)).withStyle(ChatFormatting.GREEN));
        }
        if (durabilityBonus > 0.0) {
            lines.add(Component.translatable("item.simpleskills.crafting_stat.durability",
                    String.format(java.util.Locale.ROOT, "%.0f", durabilityBonus * 100.0)).withStyle(ChatFormatting.GREEN));
        }
        if (criticalChance > 0.0) {
            lines.add(Component.translatable("item.simpleskills.crafting_stat.critical_chance",
                    formatPercent(criticalChance * 100.0)).withStyle(ChatFormatting.GREEN));
        }
        if (attackSpeedBonus > 0.0) {
            lines.add(Component.translatable("item.simpleskills.crafting_stat.attack_speed",
                    formatPercent(attackSpeedBonus * 100.0)).withStyle(ChatFormatting.GREEN));
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
            lines.add(perkLine.withStyle(ChatFormatting.AQUA));
        }
        stack.set(DataComponents.LORE, new ItemLore(lines));
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
        PROJECTILE_DATA.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        // Keep Guardian's cooldown across reconnects, but discard expired UUID entries.
        ETERNAL_GUARDIAN_READY_AT.entrySet().removeIf(entry -> entry.getValue() <= now);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.tickCount % EFFECT_REFRESH_INTERVAL != 0) continue;
            Map<String, Integer> wanted = collectActiveEffects(player);
            for (Map.Entry<String, Integer> effect : wanted.entrySet()) {
                addEffect(player, effect.getKey(), effect.getValue());
            }
            updateVitality(player);
            updateResilience(player);
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
        stacks.add(player.getMainHandItem());
        stacks.add(player.getOffhandItem());
        stacks.add(player.getItemBySlot(EquipmentSlot.HEAD));
        stacks.add(player.getItemBySlot(EquipmentSlot.CHEST));
        stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
        stacks.add(player.getItemBySlot(EquipmentSlot.FEET));
        return stacks;
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

    public static float getCriticalMultiplier(ItemStack weapon, RandomSource random) {
        if (weapon == null || weapon.isEmpty() || random == null || !hasRarity(weapon)) return 1.0F;
        double chance = getEffectiveCriticalChance(weapon);
        if (chance <= 0.0 || random.nextDouble() >= chance) return 1.0F;
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
            multiplier *= getCriticalMultiplier(weapon, shooter.getRandom());
        }
        PROJECTILE_DATA.put(projectile.getUUID(), new ProjectileDamageData(
                multiplier, getTag(weapon).getStringOr(DATA_PERKS, ""),
                now + PROJECTILE_BONUS_LIFETIME_TICKS));
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
        int level = getPerkAmplifier(stack, perkId);
        if (level < 0) return 0.0;
        double chance = Math.min(0.30, 0.06 + level * 0.06);
        if (perkId.equals("worldshaper")) chance = Math.max(chance, 0.15);
        return chance;
    }

    public static double getTreasureHunterChance(ItemStack stack) {
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
            if (data != null && data.expiresAt() > currentServerTime(projectile)) encodedPerks = data.encodedPerks();
        } else if (source.getEntity() instanceof ServerPlayer player) {
            attacker = player;
            encodedPerks = getTag(player.getMainHandItem()).getStringOr(DATA_PERKS, "");
        }
        if (attacker == null || encodedPerks.isBlank()) return;

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
