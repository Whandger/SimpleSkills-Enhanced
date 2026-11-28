package com.github.ob_yekt.simpleskills.abyss.client;

import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;

public class CrimsonColossusRenderState extends LivingEntityRenderState {
    public float crystalGlowPulse = 1.0F;
    public CrimsonColossusEntity.AttackType attackType = CrimsonColossusEntity.AttackType.NONE;
    public int attackTick = 0;
    /** Attack tick at which the leap landed, or -1 while still airborne. */
    public int impactTick = -1;
    public float partialTick = 0.0F;
    /** Synced aim point (or landing point) relative to the Colossus base, in world blocks. */
    public Vec3 targetOffset = Vec3.ZERO;
}
