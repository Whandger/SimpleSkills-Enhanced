package com.github.ob_yekt.simpleskills.abyss.client;

import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;

public class CrimsonColossusRenderState extends LivingEntityRenderState {
    public float crystalGlowPulse = 1.0F;
    public CrimsonColossusEntity.AttackType attackType = CrimsonColossusEntity.AttackType.NONE;
    public int attackTick = 0;
    public float partialTick = 0.0F;
    public Vec3 targetOffset = Vec3.ZERO;
}
