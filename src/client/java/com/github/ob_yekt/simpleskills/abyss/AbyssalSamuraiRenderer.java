package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaAnimationLibrary;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientStanceState;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaStanceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class AbyssalSamuraiRenderer
        extends MobRenderer<AbyssalSamuraiEntity, AbyssalSamuraiRenderState, AbyssalSamuraiModel> {
    private static final Identifier GOLD_TEXTURE =
            Identifier.fromNamespaceAndPath("simpleskills", "textures/entity/abyssal_samurai_gold.png");
    private static final Identifier BLADE_MODEL =
            Identifier.fromNamespaceAndPath("simpleskills", "katana_ultimo_juramento");
    private static final Identifier SAYA_MODEL =
            Identifier.fromNamespaceAndPath("simpleskills", "katana_ultimo_juramento_saya");
    private static final Identifier SHEATHED_MODEL =
            Identifier.fromNamespaceAndPath("simpleskills", "katana_ultimo_juramento_sheathed");

    public AbyssalSamuraiRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalSamuraiModel(context.bakeLayer(AbyssalSamuraiModel.LAYER_LOCATION)), 0.65F);
        this.addLayer(new AbyssalSamuraiKatanaLayer(this));
        this.addLayer(new AbyssalSamuraiStoneShellLayer(this, context.getModelSet()));
        this.addLayer(new AbyssalSamuraiEmissiveLayer(this));
    }

    @Override
    public AbyssalSamuraiRenderState createRenderState() {
        return new AbyssalSamuraiRenderState();
    }

    @Override
    public void extractRenderState(AbyssalSamuraiEntity entity, AbyssalSamuraiRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.deathTime = 0.0F;
        state.samuraiState = entity.getSamuraiState();
        state.petrificationStage = entity.getPetrificationStage();
        state.isDying = entity.isDeadOrDying();

        state.petrifiedAnimationState.copyFrom(entity.petrifiedAnimationState);
        state.awakeningAnimationState.copyFrom(entity.awakeningAnimationState);
        state.idleAnimationState.copyFrom(entity.idleAnimationState);
        state.walkAnimationState.copyFrom(entity.walkAnimationState);
        state.attackAnimationState.copyFrom(entity.attackAnimationState);
        state.deathAnimationState.copyFrom(entity.deathAnimationState);

        state.hasKatana = AbyssalKatanaItems.isKatana(entity.getMainHandItem().getItem());
        if (state.hasKatana) {
            ItemStack bladeStack = entity.getMainHandItem().copy();
            bladeStack.set(DataComponents.ITEM_MODEL, BLADE_MODEL);
            ItemStack sayaStack = entity.getMainHandItem().copy();
            sayaStack.set(DataComponents.ITEM_MODEL, SAYA_MODEL);
            ItemStack sheathedStack = entity.getMainHandItem().copy();
            sheathedStack.set(DataComponents.ITEM_MODEL, SHEATHED_MODEL);
            Minecraft resolver = Minecraft.getInstance();
            resolver.getItemModelResolver().updateForTopItem(
                    state.katanaRenderState, bladeStack, ItemDisplayContext.NONE,
                    entity.level(), null, entity.getId());
            resolver.getItemModelResolver().updateForTopItem(
                    state.katanaSayaRenderState, sayaStack, ItemDisplayContext.NONE,
                    entity.level(), null, entity.getId());
            resolver.getItemModelResolver().updateForTopItem(
                    state.katanaSheathedRenderState, sheathedStack, ItemDisplayContext.NONE,
                    entity.level(), null, entity.getId());
        } else {
            state.katanaRenderState.clear();
            state.katanaSayaRenderState.clear();
            state.katanaSheathedRenderState.clear();
        }

        // Use the same clip mapping as the player, but with a server-synchronized elapsed
        // tick counter so client and server clock offsets cannot skip short attack/draw clips.
        int stance = entity.getKatanaStance();
        float elapsedSeconds = (entity.getKatanaStanceAgeTicks() + partialTick) / 20.0F;
        String clip = KatanaAnimationLibrary.clipFor(stance);
        boolean walkEligible = stance == KatanaStanceManager.UNSHEATHED
                || stance == KatanaStanceManager.LOWER_GUARD;
        float walkWeight = walkEligible ? Mth.clamp(
                (float) (entity.getDeltaMovement().horizontalDistance() / 0.12D), 0.0F, 1.0F) : 0.0F;
        float walkPhase = (entity.tickCount + partialTick) * 0.045F;
        String blendFromClip = null;
        float blendFromSeconds = entity.getKatanaBlendFromSeconds();
        float blendWeight = 1.0F;
        int blendFromStance = entity.getKatanaBlendFromStance();
        int blendDurationTicks = entity.getKatanaBlendDurationTicks();
        if (blendFromStance >= 0 && blendDurationTicks > 0) {
            float blendElapsed = Math.max(0.0F, entity.getKatanaBlendAgeTicks() + partialTick);
            float linear = Mth.clamp(blendElapsed / blendDurationTicks, 0.0F, 1.0F);
            if (linear < 1.0F) {
                blendWeight = linear * linear * (3.0F - 2.0F * linear);
                blendFromClip = KatanaAnimationLibrary.clipFor(blendFromStance);
            }
        }
        state.katanaPose = new KatanaClientStanceState.RenderPose(
                stance, clip, elapsedSeconds, elapsedSeconds,
                blendFromClip, blendFromSeconds, blendWeight, walkWeight, walkPhase);
    }

    @Override
    protected void scale(AbyssalSamuraiRenderState state, PoseStack poseStack) {
        poseStack.scale(1.18F, 1.18F, 1.18F);
    }

    @Override
    public Identifier getTextureLocation(AbyssalSamuraiRenderState state) {
        return GOLD_TEXTURE;
    }
}
