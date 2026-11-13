package com.github.ob_yekt.simpleskills.prayer;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Server-authoritative Prayer progression, loadout, cooldowns, and first playable cast loop. */
public final class PrayerManager {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_XP_TICK = new ConcurrentHashMap<>();
    private PrayerManager() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(PrayerActionPayload.TYPE, PrayerActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PrayerMenuDataPayload.TYPE, PrayerMenuDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PrayerCooldownPayload.TYPE, PrayerCooldownPayload.CODEC);
    }

    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PrayerActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handle(context.player(), payload)));
    }

    private static void handle(ServerPlayer player, PrayerActionPayload payload) {
        if (payload == null || payload.value().length() > 64) return;
        DatabaseManager db = DatabaseManager.getInstance();
        String uuid = player.getStringUUID();
        db.ensurePlayerInitialized(uuid);
        switch (payload.action()) {
            case PrayerActionPayload.OPEN -> sendMenu(player);
            case PrayerActionPayload.OPEN_WHEEL -> sendMenu(player);
            case PrayerActionPayload.SELECT_SPECIALIZATION -> {
                PrayerSpecialization selected = PrayerSpecialization.fromId(payload.value());
                if (!selected.id().equals(db.getPrayerSpecialization(uuid))) {
                    db.setPrayerLoadout(uuid, "[]");
                    db.setPrayerActiveSlot(uuid, 0);
                }
                db.setPrayerSpecialization(uuid, selected.id());
                sendMenu(player);
            }
            case PrayerActionPayload.EQUIP -> {
                PrayerAbility ability = find(payload.value());
                int level = XPManager.getSkillLevel(uuid, Skills.PRAYER);
                if (ability == null || !ability.isUnlocked(level)
                        || ability.specialization() != PrayerSpecialization.fromId(db.getPrayerSpecialization(uuid))) {
                    player.sendSystemMessage(Component.translatable("gui.simpleskills.prayer.unavailable"), true);
                    return;
                }
                List<String> loadout = readLoadout(db.getPrayerLoadout(uuid));
                loadout.set(db.getPrayerActiveSlot(uuid), ability.id());
                db.setPrayerLoadout(uuid, writeLoadout(loadout));
                sendMenu(player);
                player.sendSystemMessage(Component.translatable("gui.simpleskills.prayer.equip_success",
                        Component.translatable(ability.translationKey())), true);
            }
            case PrayerActionPayload.SELECT_SLOT -> {
                try {
                    int slot = Integer.parseInt(payload.value());
                    if (slot < 0 || slot > 7) return;
                    db.setPrayerActiveSlot(uuid, slot);
                    sendMenu(player);
                } catch (NumberFormatException ignored) { }
            }
            case PrayerActionPayload.CAST -> cast(player, payload.value());
            default -> { }
        }
    }

    private static void cast(ServerPlayer player, String abilityId) {
        DatabaseManager db = DatabaseManager.getInstance();
        String uuid = player.getStringUUID();
        int activeSlot = db.getPrayerActiveSlot(uuid);
        List<String> loadout = readLoadout(db.getPrayerLoadout(uuid));
        if (!abilityId.equals(loadout.get(activeSlot))) return;
        PrayerAbility ability = find(abilityId);
        int level = XPManager.getSkillLevel(uuid, Skills.PRAYER);
        if (ability == null || !ability.isUnlocked(level)
                || ability.specialization() != PrayerSpecialization.fromId(db.getPrayerSpecialization(uuid))) return;
        long now = player.level().getGameTime();
        long readyAt = COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>())
                .getOrDefault(ability.id(), 0L);
        if (now < readyAt || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return;

        boolean effective = false;
        float scaling = 1.0f + Math.max(0, level - 1) * 0.006f;
        var castStart = player.position();
        PrayerTargeting.Aim aim = PrayerTargeting.aim(player, 24.0);
        LivingEntity target = aim.entity();
        var castPoint = aim.point();
        List<net.minecraft.world.phys.Vec3> chainSegments = new java.util.ArrayList<>();

        if (ability.id().equals("lightning_bolt")) {
            // The server-resolved aim stops at the first living hit or the first block.
            effective = true;
            if (target != null && target != player && target.isAlive() && !(target instanceof Player)
                    && player.hasLineOfSight(target)) {
                float damage = (4.0f + level * 0.12f) * scaling;
                target.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), damage);
            }
        } else if (ability.id().equals("light_orb")) {
            effective = applyEffect(player, "minecraft:night_vision", 400, 0);
        } else if (ability.id().equals("purification")) {
            for (String effect : List.of("minecraft:poison", "minecraft:wither", "minecraft:slowness",
                    "minecraft:weakness", "minecraft:blindness", "minecraft:mining_fatigue")) {
                effective |= removeEffect(player, effect);
            }
        } else if (ability.id().equals("divine_protection")) {
            effective = applyEffect(player, "minecraft:resistance", 160, Math.min(2, level / 35));
        } else if (ability.id().equals("holy_light")) {
            LivingEntity recipient = target instanceof Player ? target : player;
            float before = recipient.getHealth();
            recipient.heal(Math.min(8.0f, (3.0f + level * 0.08f) * scaling));
            effective = recipient.getHealth() > before;
        } else if (ability.specialization() == PrayerSpecialization.DIVINE && ability.id().contains("healing")) {
            float before = player.getHealth();
            player.heal(Math.min(8.0f, (3.0f + level * 0.08f) * scaling));
            effective = player.getHealth() > before;
        } else if (ability.id().equals("sanctuary")) {
            for (ServerPlayer ally : ((ServerLevel) player.level()).players()) {
                if (ally.level() != player.level() || ally.distanceToSqr(player) > 144) continue;
                float before = ally.getHealth();
                ally.heal(Math.min(5.0f, 2.0f + level * 0.05f));
                effective |= ally.getHealth() > before;
            }
        } else if (ability.specialization() == PrayerSpecialization.UNHOLY
                && ability.id().equals("shadowstep")) {
            var look = player.getLookAngle();
            player.teleportTo(player.getX() + look.x * 4, player.getY(), player.getZ() + look.z * 4);
            effective = true;
        } else if (ability.id().equals("dark_pact")) {
            if (player.getHealth() > 6.0f) {
                player.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), 4.0f);
                effective = applyEffect(player, "minecraft:strength", 160, Math.min(2, level / 35));
            }
        } else if (ability.id().equals("radiant_nova") || ability.id().equals("celestial_wrath")
                || ability.id().equals("burning_ground") || ability.id().equals("soul_harvest")
                || ability.id().equals("soul_explosion") || ability.id().equals("unholy_ground")
                || ability.id().equals("apocalypse") || ability.id().equals("flame_burst")
                || ability.id().equals("frost_nova") || ability.id().equals("inferno")
                || ability.id().equals("blizzard") || ability.id().equals("meteor")
                || ability.id().equals("thunderstorm") || ability.id().equals("absolute_zero")
                || ability.id().equals("storm_judgment")) {
            double radius = ability.requiredLevel() >= 70 ? 9.0 : ability.requiredLevel() >= 30 ? 6.0 : 4.0;
            float damage = (3.0f + level * 0.15f) * scaling;
            var areaOrigin = ability.id().equals("meteor") ? castPoint : player.position();
            for (LivingEntity victim : ((ServerLevel) player.level()).getEntitiesOfClass(LivingEntity.class,
                    new AABB(net.minecraft.core.BlockPos.containing(areaOrigin)).inflate(radius), entity -> entity != player && entity.isAlive()
                            && !(entity instanceof Player) && player.hasLineOfSight(entity))) {
                if (victim.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), damage)) {
                    effective = true;
                    if (ability.element() == PrayerElement.FIRE) victim.igniteForSeconds(3.0f);
                    if (ability.element() == PrayerElement.ICE || ability.id().equals("radiant_nova"))
                        applyEffect(victim, "minecraft:slowness", 60, 1);
                    if (ability.id().equals("soul_harvest")) player.heal(Math.max(0.5f, damage * 0.12f));
                }
            }
            if (ability.id().equals("absolute_zero")) {
                for (LivingEntity victim : ((ServerLevel) player.level()).getEntitiesOfClass(LivingEntity.class,
                        new AABB(player.blockPosition()).inflate(5.0), entity -> entity != player && entity.isAlive()
                                && !(entity instanceof Player))) {
                    effective |= applyEffect(victim, "minecraft:slowness", 100, 4);
                }
            }
        } else if (target != null && target != player && target.isAlive() && !(target instanceof Player) && player.hasLineOfSight(target)) {
            float damage = (4.0f + level * 0.12f) * scaling;
            if (ability.specialization() == PrayerSpecialization.UNHOLY && ability.id().equals("life_drain")) damage *= 0.7f;
            if (ability.id().equals("holy_bolt")) damage *= 1.2f;
            if (ability.id().equals("divine_smite")) damage *= 1.75f;
            effective = target.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), damage);
            if (effective && ability.specialization() == PrayerSpecialization.UNHOLY && ability.id().equals("life_drain")) {
                player.heal(Math.max(1.0f, damage * 0.35f));
            }
            if (effective && ability.id().equals("curse_of_weakness")) applyEffect(target, "minecraft:weakness", 100, Math.min(2, level / 40));
            if (effective && ability.id().equals("corruption")) applyEffect(target, "minecraft:wither", 80, Math.min(1, level / 60));
            if (effective && (ability.element() == PrayerElement.ICE || ability.id().equals("ice_prison")))
                applyEffect(target, "minecraft:slowness", ability.id().equals("ice_prison") ? 100 : 50, ability.id().equals("ice_prison") ? 3 : 1);
            if (effective && ability.element() == PrayerElement.FIRE) target.igniteForSeconds(3.0f);
            if (effective && ability.id().equals("chain_lightning")) {
                LivingEntity firstTarget = target;
                var nearby = ((ServerLevel) player.level()).getEntitiesOfClass(LivingEntity.class,
                        new AABB(firstTarget.blockPosition()).inflate(5.0), entity -> entity != player && entity != firstTarget
                                && entity.isAlive() && !(entity instanceof Player));
                int jumps = 0;
                LivingEntity source = firstTarget;
                for (LivingEntity chained : nearby) {
                    if (jumps++ >= 3) break;
                    chainSegments.add(source.position().add(0, source.getBbHeight() * 0.55, 0));
                    chainSegments.add(chained.position().add(0, chained.getBbHeight() * 0.55, 0));
                    chained.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), damage * 0.65f);
                    source = chained;
                }
            }
        }

        if (!effective) {
            player.sendSystemMessage(Component.translatable("gui.simpleskills.prayer.no_target"), true);
            return;
        }
        boolean areaSpell = isAreaSpell(ability);
        LivingEntity visualTarget = ability.id().equals("divine_healing") ? player
                : !areaSpell && target != null && target.isAlive()
                && (!ability.id().equals("holy_light") || target instanceof Player) ? target : player;
        ServerLevel serverLevel = (ServerLevel) player.level();
        var impact = switch (ability) {
            case HOLY_BOLT, DIVINE_SMITE, SHADOW_BOLT, LIFE_DRAIN, CURSE_OF_WEAKNESS, CORRUPTION,
                    SPARK, CHAIN_LIGHTNING, FIREBALL, ICE_SHARD, ICE_PRISON, LIGHTNING_BOLT, METEOR -> castPoint;
            case SHADOWSTEP -> player.position();
            default -> new net.minecraft.world.phys.Vec3(visualTarget.getX(), visualTarget.getY(), visualTarget.getZ());
        };
        var look = player.getLookAngle();
        var handSide = look.cross(new net.minecraft.world.phys.Vec3(0, 1, 0)).normalize().scale(0.28);
        var origin = new net.minecraft.world.phys.Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.72, player.getZ())
                .add(handSide).add(look.scale(0.28));
        if (ability == PrayerAbility.SHADOWSTEP) origin = castStart;
        PrayerVisualEffects.play(serverLevel, origin, impact, ability, level, player);
        if (ability == PrayerAbility.CHAIN_LIGHTNING) {
            for (int i = 0; i + 1 < chainSegments.size(); i += 2) {
                PrayerVisualEffects.playConnection(serverLevel, chainSegments.get(i), chainSegments.get(i + 1), ability);
            }
        }
        int cooldown = Math.max(20, (int) (ability.baseCooldownTicks() * Math.max(0.65, 1.0 - (level - 1) * 0.0035)));
        COOLDOWNS.get(player.getUUID()).put(ability.id(), now + cooldown);
        if (ServerPlayNetworking.canSend(player, PrayerCooldownPayload.TYPE))
            ServerPlayNetworking.send(player, new PrayerCooldownPayload(ability.id(), cooldown));
        long lastXp = LAST_XP_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - lastXp >= 60) {
            LAST_XP_TICK.put(player.getUUID(), now);
            XPManager.addXPWithNotification(player, Skills.PRAYER, Math.max(1, 8 + level / 5));
        }
        sendMenu(player);
    }

    private static boolean applyEffect(ServerPlayer player, String effectId, int duration, int amplifier) {
        return applyEffect((LivingEntity) player, effectId, duration, amplifier);
    }

    private static boolean applyEffect(LivingEntity entity, String effectId, int duration, int amplifier) {
        return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(effectId))
                .map(effect -> entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true)))
                .orElse(false);
    }

    private static boolean removeEffect(LivingEntity entity, String effectId) {
        return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(effectId)).map(entity::removeEffect).orElse(false);
    }

    private static PrayerAbility find(String id) {
        for (PrayerAbility ability : PrayerAbility.values()) if (ability.id().equals(id)) return ability;
        return null;
    }

    private static boolean isAreaSpell(PrayerAbility ability) {
        return switch (ability.id()) {
            case "radiant_nova", "celestial_wrath", "soul_harvest", "soul_explosion", "unholy_ground",
                    "apocalypse", "flame_burst", "burning_ground", "frost_nova", "inferno", "blizzard",
                    "meteor", "thunderstorm", "absolute_zero", "storm_judgment", "sanctuary" -> true;
            default -> false;
        };
    }

    private static List<String> readLoadout(String raw) {
        List<String> slots = new java.util.ArrayList<>(java.util.Collections.nCopies(8, ""));
        if (raw == null || raw.isBlank() || raw.equals("[]")) return slots;
        try {
            var json = JsonParser.parseString(raw);
            if (json.isJsonArray()) {
                JsonArray array = json.getAsJsonArray();
                for (int i = 0; i < Math.min(8, array.size()); i++) {
                    if (array.get(i).isJsonPrimitive()) slots.set(i, array.get(i).getAsString());
                }
            } else if (json.isJsonPrimitive()) {
                // Migrate the early single-spell Prayer loadout to slot 1.
                slots.set(0, json.getAsString());
            }
        } catch (RuntimeException ignored) {
            if (!raw.contains(",") && raw.length() <= 64) slots.set(0, raw);
        }
        for (int i = 0; i < slots.size(); i++) if (find(slots.get(i)) == null) slots.set(i, "");
        return slots;
    }

    private static String writeLoadout(List<String> loadout) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < 8; i++) array.add(i < loadout.size() ? loadout.get(i) : "");
        return array.toString();
    }

    private static void sendMenu(ServerPlayer player) {
        var db = DatabaseManager.getInstance();
        String uuid = player.getStringUUID();
        int level = XPManager.getSkillLevel(uuid, Skills.PRAYER);
        String specialization = db.getPrayerSpecialization(uuid);
        int activeSlot = db.getPrayerActiveSlot(uuid);
        List<String> loadout = readLoadout(db.getPrayerLoadout(uuid));
        var abilities = PrayerAbility.forSpecialization(PrayerSpecialization.fromId(specialization)).stream()
                .map(PrayerAbility::id).collect(java.util.stream.Collectors.joining(","));
        if (ServerPlayNetworking.canSend(player, PrayerMenuDataPayload.TYPE)) {
            ServerPlayNetworking.send(player, new PrayerMenuDataPayload(specialization, level, loadout.get(activeSlot),
                    abilities, writeLoadout(loadout), activeSlot));
        }
    }
}
