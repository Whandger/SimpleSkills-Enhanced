package com.github.ob_yekt.simpleskills.prayer;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-side aim resolution shared by Prayer spells. Blocks and the first living hit stop the cast. */
public final class PrayerTargeting {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private PrayerTargeting() {}

    public record Aim(Vec3 point, LivingEntity entity, Vec3 normal, boolean blockHit) {}

    public static Aim aim(ServerPlayer caster, double range) {
        ServerLevel level = (ServerLevel) caster.level();
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().normalize().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, caster));
        boolean hitBlock = block.getType() != HitResult.Type.MISS;
        Vec3 limit = hitBlock ? block.getLocation() : end;

        double nearestDistance = eye.distanceToSqr(limit);
        LivingEntity nearest = null;
        Vec3 nearestPoint = null;
        AABB search = new AABB(eye, limit).inflate(1.25);
        for (Entity entity : level.getEntities(caster, search,
                candidate -> candidate instanceof LivingEntity living && living != caster && living.isAlive()
                        && !living.isSpectator() && living.isPickable())) {
            LivingEntity living = (LivingEntity) entity;
            AABB body = living.getBoundingBox();
            double margin = Math.max(0.0, living.getPickRadius()) + 0.12;
            Optional<Vec3> forgivingPoint = body.inflate(margin).clip(eye, limit);
            if (forgivingPoint.isEmpty()) continue;
            Vec3 point = body.clip(eye, limit).orElse(forgivingPoint.get());
            double distance = eye.distanceToSqr(point);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = living;
                nearestPoint = point;
            }
        }

        if (nearest != null) {
            Vec3 center = nearest.position().add(0, nearest.getBbHeight() * 0.5, 0);
            Vec3 normal = nearestPoint.subtract(center);
            normal = normal.lengthSqr() < 1.0E-6 ? UP : normal.normalize();
            return new Aim(nearestPoint, nearest, normal, false);
        }
        if (hitBlock) {
            Direction face = block.getDirection();
            return new Aim(block.getLocation(), null,
                    new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()), true);
        }
        return new Aim(end, null, UP, false);
    }
}
