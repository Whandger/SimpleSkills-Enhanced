package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Crimson Blight: progressive movement slow, periodic magical damage and crystal fragments.
 * The attached full-bright 3D crystals are rendered client-side from the target's hitbox size.
 */
public final class CrimsonBlightEffect extends MobEffect {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "crimson_blight");

    private static final Identifier SLOWNESS_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "crimson_blight_slowness");

    private static boolean registered;

    private CrimsonBlightEffect() {
        super(MobEffectCategory.HARMFUL, 0x9C0E2A);
        // The attribute template is multiplied by amplifier + 1 in Minecraft 26.3:
        // I = 10%, II = 20%, III = 30% movement-speed reduction.
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                SLOWNESS_MODIFIER_ID,
                -0.10D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.setBlendDuration(8, 12, 6);
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        Registry.register(BuiltInRegistries.MOB_EFFECT, ID, new CrimsonBlightEffect());
        Simpleskills.LOGGER.info("Registered Crimson Blight status effect.");
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        int interval = switch (Math.min(amplifier, 2)) {
            case 0 -> 40; // 0.75 damage per second
            case 1 -> 25; // 2.0 damage per second
            default -> 14; // about 5.0 damage per second
        };
        return tickCount % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        int levelIndex = Math.min(amplifier, 2);
        float damage = switch (levelIndex) {
            case 0 -> 1.5F;
            case 1 -> 2.5F;
            default -> 3.5F;
        };

        entity.hurtServer(level, entity.damageSources().magic(), damage);

        double width = Math.max(0.35D, entity.getBbWidth());
        double height = Math.max(0.75D, entity.getBbHeight());
        double centerY = entity.getY() + height * 0.55D;

        level.sendParticles(
                ParticleTypes.CRIMSON_SPORE,
                entity.getX(), centerY, entity.getZ(),
                4 + levelIndex * 3,
                width * 0.42D, height * 0.36D, width * 0.42D,
                0.012D
        );

        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, AbyssContent.RED_CRYSTAL_BLOCK.defaultBlockState()),
                entity.getX(), centerY, entity.getZ(),
                2 + levelIndex * 2,
                width * 0.40D, height * 0.40D, width * 0.40D,
                0.035D
        );
        return true;
    }
}
