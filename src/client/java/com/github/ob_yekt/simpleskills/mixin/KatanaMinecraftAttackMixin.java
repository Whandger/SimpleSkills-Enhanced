package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientStanceState;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaStanceManager;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaStanceRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces vanilla sword attacks with stance-gated, server-authoritative katana combat. */
@Mixin(Minecraft.class)
public abstract class KatanaMinecraftAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void simpleskills$handleKatanaAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = (Minecraft) (Object) this;
        LocalPlayer player = client.player;
        if (player == null || !player.isAlive() || player.isSpectator() || player.isHandsBusy()
                || client.gui.screen() != null
                || !AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) return;

        int stance = KatanaClientStanceState.get(player.getUUID()).stance();
        boolean canDrawFromSheathedParry = stance == KatanaStanceManager.SHEATHED
                || stance == KatanaStanceManager.SHEATHING;

        // Keep normal block mining once the katana is already out. While sheathed in Parry,
        // however, a left click on a block must still be sent to the stance controller so it
        // can start DRAWING without requiring a mob target or the vanilla entity reach.
        if (client.hitResult instanceof BlockHitResult && !canDrawFromSheathedParry) return;

        // Do not trust a stale crosshair entity when the actual hit is a block: the first Parry
        // click is a free draw command and must not accidentally become a special attack target.
        LivingEntity hitLiving = client.hitResult instanceof BlockHitResult ? null
                : client.hitResult instanceof EntityHitResult entityHit
                    && entityHit.getEntity() instanceof LivingEntity living && living.isAlive()
                    ? living
                    : client.crosshairPickEntity instanceof LivingEntity fallback && fallback.isAlive()
                        ? fallback : null;

        // Lakai always uses a Mob-only ray cast at the full server-authoritative reach.
        // Parry keeps its normal vanilla target reach for actual damaging attacks.
        if (stance == KatanaStanceManager.LAKAI_READY) {
            hitLiving = simpleskills$findLakaiTarget(player);
        }
        int targetId = hitLiving != null ? hitLiving.getId() : KatanaStanceRequestPayload.NO_TARGET;

        // In Parry mode the first click while sheathed only starts draw; it never animates
        // an attack or deals damage. The server permits normal attacks only after UNSHEATHED.
        // In Lakai mode, a sheathed click on a Mob is routed to the special by the server.
        int action = KatanaStanceRequestPayload.ATTACK;
        boolean ambiguousSheathedMobClick = (stance == KatanaStanceManager.SHEATHED
                || stance == KatanaStanceManager.LAKAI_READY
                || stance == KatanaStanceManager.LAKAI_SHEATHING)
                && hitLiving instanceof net.minecraft.world.entity.Mob;
        // Let the server decide between draw (Parry mode) and special dash (Lakai mode)
        // for a sheathed click aimed at a Mob, avoiding a one-frame false draw prediction.
        if (!ambiguousSheathedMobClick && stance != KatanaStanceManager.LAKAI_READY
                && stance != KatanaStanceManager.LAKAI_SHEATHING) {
            KatanaClientStanceState.predictAttack(player.getUUID());
        }
        if (ClientPlayNetworking.canSend(KatanaStanceRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new KatanaStanceRequestPayload(action, targetId));
        }

        // Vanilla Player.attack is also cancelled by a common-side guard. This return value prevents
        // vanilla attack strength/cooldown logic and its default sword damage from running here.
        cir.setReturnValue(true);
    }

    /** Select the closest living Mob along the view ray, using Lakai's extended special range. */
    private static LivingEntity simpleskills$findLakaiTarget(LocalPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize()
                .scale(KatanaStanceManager.SPECIAL_MAX_TARGET_DISTANCE));
        AABB search = player.getBoundingBox()
                .expandTowards(end.subtract(start))
                .inflate(0.85D);

        Mob best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Mob candidate : player.level().getEntitiesOfClass(Mob.class, search, Mob::isAlive)) {
            if (!player.hasLineOfSight(candidate)) continue;
            var intersection = candidate.getBoundingBox().inflate(0.18D).clip(start, end);
            if (intersection.isEmpty()) continue;
            double distance = intersection.get().distanceToSqr(start);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }
}
