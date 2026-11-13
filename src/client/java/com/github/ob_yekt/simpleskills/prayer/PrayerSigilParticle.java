package com.github.ob_yekt.simpleskills.prayer;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/** Emissive, animated 4x4 sigil particle; each magic school supplies its own art atlas. */
public final class PrayerSigilParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    private PrayerSigilParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.lifetime = 32;
        this.quadSize = 1.45f;
        this.gravity = 0.0f;
        this.friction = 1.0f;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;
        this.setSpriteFromAge(this.sprites);
        float progress = this.age / (float) this.lifetime;
        this.quadSize = 1.05f + (float) Math.sin(progress * Math.PI) * 0.85f;
        this.roll = progress * (float) Math.PI * 2.0f;
        this.setAlpha(progress < 0.12f ? progress / 0.12f
                : progress > 0.76f ? (1.0f - progress) / 0.24f : 1.0f);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 0x00F000F0;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public PrayerSigilParticle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double velocityX, double velocityY, double velocityZ,
                net.minecraft.util.RandomSource random) {
            return new PrayerSigilParticle(level, x, y, z, this.sprites);
        }
    }
}
