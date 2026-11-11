package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;

/** Server-authoritative combat perks unlocked by Defense. */
public final class DefenseCombat {
    public static final int PERFECT_WINDOW_TICKS = 8;
    public static final int ENHANCED_WINDOW_TICKS = 12;
    public static final int NORMAL_STUN_TICKS = 40;
    public static final int BOSS_STUN_TICKS = 20;
    public static final double PERFECT_XP_MULTIPLIER = 2.0;
    public static final double ENHANCED_XP_MULTIPLIER = 2.5;
    public static final double PROJECTILE_REFLECTION_XP_MULTIPLIER = 2.0;
    public static final double COUNTER_DAMAGE_MULTIPLIER = 1.20;
    public static final int COUNTER_DURATION_TICKS = 100;
    public static final ProjectileDeflection PROJECTILE_REFLECTION = (projectile, deflector, random, power) -> {
        if (deflector == null || projectile.level().isClientSide()) return;
        Entity oldOwner = projectile.getOwner();
        Vec3 velocity = projectile.getDeltaMovement();
        double speed = Math.max(0.1, velocity.length());
        boolean hasTarget = oldOwner != null && oldOwner.isAlive() && oldOwner != deflector;
        Vec3 direction = hasTarget ? oldOwner.getEyePosition().subtract(projectile.position()).normalize()
                : deflector.getLookAngle().normalize();
        if (projectile instanceof ShulkerBullet) {
            ((com.github.ob_yekt.simpleskills.mixin.DEFENSE.ShulkerBulletTargetAccessor) projectile)
                    .simpleskills$setFinalTarget(net.minecraft.world.entity.EntityReference.of(hasTarget ? oldOwner : null));
        }
        projectile.setDeltaMovement(direction.scale(speed));
        projectile.needsSync = true;
        if (deflector instanceof ServerPlayer player) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.simpleskills.defense.projectile_reflected"), true);
            projectile.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SimpleSkillsSounds.PROJECTILE_REFLECTION, SoundSource.PLAYERS, 0.9f, 1.3f);
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.CRIT,
                    projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.2, 0.2, 0.2, 0.08);
            float xpDamage = projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow
                    ? (float) Math.ceil(arrow.getDeltaMovement().length()
                            * ((com.github.ob_yekt.simpleskills.mixin.DEFENSE.AbstractArrowDamageAccessor) arrow)
                                    .simpleskills$getBaseDamage())
                    : 4.0f;
            grantReflectionXp(player, xpDamage);
        }
    };

    private record TimedEntity(Entity entity, long endsAt) {}
    private record TimedPlayer(ServerPlayer player, long endsAt) {}
    private record ParryAward(ServerPlayer player, long gameTime, UUID attacker, float damage) {}

    private static final Map<UUID, TimedEntity> STUNS = new ConcurrentHashMap<>();
    private static final Map<UUID, TimedPlayer> COUNTERS = new ConcurrentHashMap<>();
    private static final Map<UUID, ParryAward> PARRY_AWARDS = new ConcurrentHashMap<>();

    private DefenseCombat() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            STUNS.entrySet().removeIf(entry -> {
                TimedEntity stun = entry.getValue();
                if (stun.endsAt() <= stun.entity().level().getGameTime()
                        || stun.entity().isRemoved() || !stun.entity().isAlive()) return true;
                stun.entity().setDeltaMovement(Vec3.ZERO);
                if (stun.entity() instanceof Mob mob) mob.getNavigation().stop();
                return false;
            });
            COUNTERS.entrySet().removeIf(entry -> entry.getValue().endsAt() <= entry.getValue().player().level().getGameTime()
                    || entry.getValue().player().isRemoved() || !entry.getValue().player().isAlive());
            PARRY_AWARDS.entrySet().removeIf(entry -> entry.getValue().player().isRemoved()
                    || entry.getValue().gameTime() < entry.getValue().player().level().getGameTime());
        });
    }

    public static void onBlockedPhysicalHit(ServerPlayer defender, Entity attacker, float incomingDamage, boolean fullyBlocked) {
        if (!fullyBlocked || attacker == null || defender.level().isClientSide()) return;
        int level = Defense.getSkillLevelFromCache(defender.getUUID());
        if (level < 10 || !(attacker instanceof net.minecraft.world.entity.LivingEntity)) return;
        int raisedAt = ((DefenseParryAccess) defender).simpleskills$getShieldRaisedTick();
        int elapsed = defender.tickCount - raisedAt;
        int window = level >= 50 ? ENHANCED_WINDOW_TICKS : PERFECT_WINDOW_TICKS;
        if (raisedAt < 0 || elapsed < 0 || elapsed > window) return;

        PARRY_AWARDS.put(defender.getUUID(), new ParryAward(defender, defender.level().getGameTime(), attacker.getUUID(), incomingDamage));
        long now = defender.level().getGameTime();
        int stunDuration = attacker instanceof EnderDragon || attacker instanceof WitherBoss
                ? BOSS_STUN_TICKS : NORMAL_STUN_TICKS;
        STUNS.merge(attacker.getUUID(), new TimedEntity(attacker, now + stunDuration),
                (old, fresh) -> old.endsAt() > fresh.endsAt() ? old : fresh);
        if (level >= 75) COUNTERS.put(defender.getUUID(), new TimedPlayer(defender, now + COUNTER_DURATION_TICKS));

        defender.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                level >= 99 ? "message.simpleskills.defense.master_defender" : "message.simpleskills.defense.perfect_parry"), true);
        defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(),
                level >= 99 ? SimpleSkillsSounds.MASTER_DEFENDER_PARRY : SimpleSkillsSounds.PERFECT_PARRY,
                SoundSource.PLAYERS, 1.0f, level >= 99 ? 1.0f : level >= 50 ? 1.35f : 1.15f);
        ServerLevel serverLevel = (ServerLevel) defender.level();
        if (level >= 99) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    defender.getX(), defender.getY() + 1.0, defender.getZ(), 22, 0.55, 0.7, 0.55, 0.12);
            serverLevel.sendParticles(ParticleTypes.FIREWORK,
                    defender.getX(), defender.getY() + 1.0, defender.getZ(), 12, 0.45, 0.55, 0.45, 0.08);
        } else {
            int burst = level >= 50 ? 20 : 14;
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    defender.getX(), defender.getY() + 1.0, defender.getZ(), burst, 0.45, 0.55, 0.45, 0.12);
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    defender.getX(), defender.getY() + 1.0, defender.getZ(), level >= 50 ? 12 : 8,
                    0.4, 0.45, 0.4, 0.08);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    defender.getX(), defender.getY() + 1.0, defender.getZ(), 4, 0.45, 0.3, 0.45, 0.0);
        }
        spawnShieldParryRing(serverLevel, defender, level);
        grantParryXp(defender, incomingDamage, level >= 50 ? ENHANCED_XP_MULTIPLIER : PERFECT_XP_MULTIPLIER);
    }

    private static void spawnShieldParryRing(ServerLevel level, ServerPlayer player, int defenseLevel) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = up.cross(look).normalize();
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1.0, 0.0, 0.0);
        Vec3 vertical = look.cross(right).normalize();
        double handSide = player.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND ? -1.0 : 1.0;
        Vec3 center = player.getEyePosition().add(look.scale(0.5)).add(right.scale(0.24 * handSide)).add(0.0, -0.1, 0.0);
        int points = defenseLevel >= 99 ? 28 : defenseLevel >= 50 ? 22 : 16;
        double radius = defenseLevel >= 99 ? 0.62 : 0.48;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            Vec3 position = center.add(right.scale(Math.cos(angle) * radius))
                    .add(vertical.scale(Math.sin(angle) * radius));
            var particle = defenseLevel >= 99 && i % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING
                    : i % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.ENCHANTED_HIT;
            level.sendParticles(particle, position.x, position.y, position.z, 1, 0.0, 0.0, 0.0, 0.015);
        }
    }

    private static void grantParryXp(ServerPlayer player, float damage, double multiplier) {
        JsonObject cfg = ConfigManager.getCombatConfig();
        float perDamage = cfg.has("defense_xp_per_damage") ? cfg.get("defense_xp_per_damage").getAsFloat() : 200.0f;
        float threshold = cfg.has("defense_min_damage_threshold") ? cfg.get("defense_min_damage_threshold").getAsFloat() : 2.0f;
        float shieldMultiplier = cfg.has("defense_shield_xp_multiplier") ? cfg.get("defense_shield_xp_multiplier").getAsFloat() : 0.04f;
        if (damage >= threshold) XPManager.addXPWithNotification(player, Skills.DEFENSE,
                Math.max(1, (int) (damage * perDamage * shieldMultiplier * multiplier)));
    }

    public static void grantReflectionXp(ServerPlayer player, float blockedDamage) {
        JsonObject cfg = ConfigManager.getCombatConfig();
        float perDamage = cfg.has("defense_xp_per_damage") ? cfg.get("defense_xp_per_damage").getAsFloat() : 200.0f;
        float threshold = cfg.has("defense_min_damage_threshold") ? cfg.get("defense_min_damage_threshold").getAsFloat() : 2.0f;
        float shieldMultiplier = cfg.has("defense_shield_xp_multiplier") ? cfg.get("defense_shield_xp_multiplier").getAsFloat() : 0.04f;
        if (blockedDamage >= threshold) {
            XPManager.addXPWithNotification(player, Skills.DEFENSE,
                    Math.max(1, (int) (blockedDamage * perDamage * shieldMultiplier * PROJECTILE_REFLECTION_XP_MULTIPLIER)));
        }
    }

    public static boolean consumeParryXp(ServerPlayer player, Entity attacker, float damage) {
        ParryAward award = PARRY_AWARDS.get(player.getUUID());
        if (award == null || award.player() != player || award.gameTime() != player.level().getGameTime() || attacker == null
                || !award.attacker().equals(attacker.getUUID()) || Math.abs(award.damage() - damage) > 0.01f) return false;
        PARRY_AWARDS.remove(player.getUUID(), award);
        return true;
    }

    public static boolean isStunned(Entity entity) {
        TimedEntity stun = STUNS.get(entity.getUUID());
        return stun != null && stun.endsAt() > entity.level().getGameTime();
    }

    public static boolean hasCounterMastery(ServerPlayer player) {
        TimedPlayer counter = COUNTERS.get(player.getUUID());
        return Defense.getSkillLevelFromCache(player.getUUID()) >= 75
                && counter != null && counter.player() == player && player.isAlive()
                && counter.endsAt() > player.level().getGameTime();
    }
}
