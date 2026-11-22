package com.github.ob_yekt.simpleskills.runes;

import com.github.ob_yekt.simpleskills.mixin.ItemStackLayerRenderStateAccessor;
import com.github.ob_yekt.simpleskills.mixin.ItemStackRenderStateAccessor;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.Bounds;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.EquipmentPart;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.ItemCategory;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.ItemGeometryInfo;
import com.github.ob_yekt.simpleskills.runes.armor.ArcaneArmorVfx;
import com.github.ob_yekt.simpleskills.runes.armor.DivineArmorVfx;
import com.github.ob_yekt.simpleskills.runes.armor.FrostArmorVfx;
import com.github.ob_yekt.simpleskills.runes.armor.InfernoArmorVfx;
import com.github.ob_yekt.simpleskills.runes.armor.ShadowArmorVfx;
import com.github.ob_yekt.simpleskills.runes.armor.StormArmorVfx;
import com.github.ob_yekt.simpleskills.runes.shields.ShieldRuneVfx;
import com.github.ob_yekt.simpleskills.runes.tools.ArcaneToolVfx;
import com.github.ob_yekt.simpleskills.runes.tools.DivineToolVfx;
import com.github.ob_yekt.simpleskills.runes.tools.FrostToolVfx;
import com.github.ob_yekt.simpleskills.runes.tools.InfernoToolVfx;
import com.github.ob_yekt.simpleskills.runes.tools.ShadowToolVfx;
import com.github.ob_yekt.simpleskills.runes.tools.StormToolVfx;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.BODY;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.FOOT;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.HEAD;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.LEFT_ARM;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.LEG;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.RIGHT_ARM;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;

/**
 * Entry point and geometry sampler for Rune visual styles on held tools/weapons, shields, and armor.
 * Delegates rendering per style to {@code runes.tools.*}, {@code runes.shields.*}, and {@code runes.armor.*}.
 */
public final class RuneEquipmentVfxRenderer {
    private static final int STORM = RuneVisualStyles.STORM;
    private static final int INFERNO = RuneVisualStyles.INFERNO;
    private static final int SHADOW = RuneVisualStyles.SHADOW;
    private static final int ARCANE = RuneVisualStyles.ARCANE;
    private static final int FROST = RuneVisualStyles.FROST;
    private static final int DIVINE = RuneVisualStyles.DIVINE;

    // ShieldModel local coordinates (1 model unit = 1/16 block):
    // plate: X [-6..6] (-0.375..0.375), Y [-11..11] (-0.6875..0.6875), Z [-2..-1] (-0.125..-0.0625)
    private static final Bounds VANILLA_SHIELD_BOUNDS =
            new Bounds(-0.385, 0.385, -0.70, 0.70, -0.14, 0.08);
    // Glowstone shields add a royal crown (-13/16 = -0.8125), keel (+12/16 = 0.75), and central crystal apex (-3.85/16 = -0.2406)
    private static final Bounds GLOWSTONE_SHIELD_BOUNDS =
            new Bounds(-0.39, 0.39, -0.82, 0.76, -0.25, 0.08);

    private static final Map<ItemStackRenderState, PendingVfx> PENDING_ITEM_STYLES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final ThreadLocal<ItemDisplayContext> ACTIVE_DISPLAY_CONTEXT =
            ThreadLocal.withInitial(() -> ItemDisplayContext.NONE);

    private RuneEquipmentVfxRenderer() {}

    /** Pushes the active ItemDisplayContext during LayerRenderState.submit so SpecialModelRenderers can inspect it. */
    public static ItemDisplayContext pushDisplayContext(@Nullable ItemDisplayContext context) {
        ItemDisplayContext previous = ACTIVE_DISPLAY_CONTEXT.get();
        ACTIVE_DISPLAY_CONTEXT.set(context != null ? context : ItemDisplayContext.NONE);
        return previous;
    }

    /** Restores the previous ItemDisplayContext at the end of LayerRenderState.submit. */
    public static void popDisplayContext(@Nullable ItemDisplayContext previous) {
        ACTIVE_DISPLAY_CONTEXT.set(previous != null ? previous : ItemDisplayContext.NONE);
    }

    /** Associates the stack's cosmetic style with the render state built by ItemModelResolver. */
    public static void clearItemStyle(ItemStackRenderState renderState) {
        if (renderState != null) PENDING_ITEM_STYLES.remove(renderState);
    }

    public static void captureItemStyle(ItemStackRenderState renderState, ItemStack stack, int style) {
        if (renderState == null || stack == null || stack.isEmpty() || !supportsCustomVfx(style)) return;
        // Shields are rendered through SpecialModelRenderer (submitShield), not baked quads.
        if (stack.getItem() instanceof ShieldItem) return;
        int seed = stack.getItem().hashCode() ^ stack.getDamageValue() * 73 ^ stack.getCount() * 19 ^ style * 31;
        ItemCategory category = classifyItem(stack);
        PENDING_ITEM_STYLES.put(renderState, new PendingVfx(style, seed, category));
    }

    private static ItemCategory classifyItem(ItemStack stack) {
        if (stack.is(ItemTags.PICKAXES)) return ItemCategory.PICKAXE;
        if (stack.is(ItemTags.SWORDS)) return ItemCategory.SWORD;
        if (stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) {
            return ItemCategory.TOOL;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        if (path.contains("pickaxe")) return ItemCategory.PICKAXE;
        if (path.contains("sword") || path.contains("blade") || path.contains("dagger") || path.contains("katana")) {
            return ItemCategory.SWORD;
        }
        return ItemCategory.TOOL;
    }

    private static boolean isHeldContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    /**
     * Called after a layer has submitted the item model, while the exact ItemDisplayContext
     * transform is active again.
     */
    public static void submitHeldItem(ItemStackRenderState renderState,
                                      PoseStack poseStack, SubmitNodeCollector collector) {
        if (renderState == null) return;
        PendingVfx pending = PENDING_ITEM_STYLES.remove(renderState);
        if (pending == null || !supportsCustomVfx(pending.style())) return;

        ItemStackRenderStateAccessor state = (ItemStackRenderStateAccessor) (Object) renderState;
        ItemDisplayContext context = state.simpleskills$getDisplayContext();
        if (!isHeldContext(context)) return;

        ItemGeometryInfo itemInfo = extractItemGeometry(state, pending.category());
        if (itemInfo == null) return;

        double time = Util.getMillis() / 50.0;
        int seed = pending.seed() ^ context.hashCode();
        int style = pending.style();

        if (style == STORM) {
            StormToolVfx.submit(collector, poseStack, itemInfo, time, seed);
        } else if (style == INFERNO) {
            InfernoToolVfx.submit(collector, poseStack, itemInfo, time, seed);
        } else if (style == SHADOW) {
            ShadowToolVfx.submit(collector, poseStack, itemInfo, time, seed);
        } else if (style == ARCANE) {
            ArcaneToolVfx.submit(collector, poseStack, itemInfo, time, seed);
        } else if (style == FROST) {
            FrostToolVfx.submit(collector, poseStack, itemInfo, time, seed);
        } else if (style == DIVINE) {
            DivineToolVfx.submit(collector, poseStack, itemInfo, time, seed);
        }
    }

    /**
     * Called by ShieldSpecialRenderer and GlowstoneShieldSpecialRenderer while the shield model's
     * local transform (including blocking pose and special model scale) is active.
     */
    public static void submitShield(@Nullable DataComponentMap components,
                                    PoseStack poseStack,
                                    SubmitNodeCollector collector,
                                    boolean isGlowstoneShield) {
        if (components == null) return;
        int style = RuneVisualStyles.getStyle(components);
        if (!supportsCustomVfx(style)) return;

        ItemDisplayContext context = ACTIVE_DISPLAY_CONTEXT.get();
        if (!isHeldContext(context)) return;

        Bounds bounds = isGlowstoneShield ? GLOWSTONE_SHIELD_BOUNDS : VANILLA_SHIELD_BOUNDS;
        double frontZ = isGlowstoneShield ? -0.258 : -0.148;
        double time = Util.getMillis() / 50.0;
        int seed = (isGlowstoneShield ? 0x51A7 : 0x2B93) ^ (style * 31) ^ context.hashCode();

        if (style == STORM) {
            StormArmorVfx.submit(collector, poseStack, bounds, time, seed);
        } else if (style == INFERNO) {
            InfernoArmorVfx.submit(collector, poseStack, bounds, time, seed);
        } else if (style == SHADOW) {
            ShadowArmorVfx.submit(collector, poseStack, bounds, time, seed);
        } else if (style == FROST) {
            FrostArmorVfx.submit(collector, poseStack, EquipmentPart.CHEST_TORSO, bounds, time, seed);
        } else if (style == ARCANE) {
            ShieldRuneVfx.submitArcane(collector, poseStack, bounds, frontZ, time, seed);
        } else if (style == DIVINE) {
            ShieldRuneVfx.submitDivine(collector, poseStack, bounds, frontZ, time, seed);
        }
    }

    /** Called by EquipmentLayerRenderer while the entity's armor model is active. */
    public static void submitArmor(ItemStack stack, Model<?> model, PoseStack poseStack,
                                   SubmitNodeCollector collector) {
        if (stack == null || stack.isEmpty() || !(model instanceof HumanoidModel<?> humanoid)) return;
        int style = RuneVisualStyles.getStyle(stack);
        if (!supportsCustomVfx(style)) return;

        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return;

        int seed = stack.getItem().hashCode() ^ equippable.slot().hashCode();
        poseStack.pushPose();
        humanoid.root().translateAndRotate(poseStack);
        switch (equippable.slot()) {
            case HEAD -> submitPart(humanoid.head, EquipmentPart.HELMET, HEAD, poseStack, collector, style, seed + 11);
            case CHEST -> {
                submitPart(humanoid.body, EquipmentPart.CHEST_TORSO, BODY, poseStack, collector, style, seed + 23);
                submitPart(humanoid.rightArm, EquipmentPart.ARM_RIGHT, RIGHT_ARM, poseStack, collector, style, seed + 29);
                submitPart(humanoid.leftArm, EquipmentPart.ARM_LEFT, LEFT_ARM, poseStack, collector, style, seed + 31);
            }
            case LEGS -> {
                submitPart(humanoid.body, EquipmentPart.LEG_WAIST, BODY, poseStack, collector, style, seed + 37);
                submitPart(humanoid.rightLeg, EquipmentPart.KNEE_RIGHT, LEG, poseStack, collector, style, seed + 41);
                submitPart(humanoid.leftLeg, EquipmentPart.KNEE_LEFT, LEG, poseStack, collector, style, seed + 43);
            }
            case FEET -> {
                submitPart(humanoid.rightLeg, EquipmentPart.BOOT_RIGHT, FOOT, poseStack, collector, style, seed + 47);
                submitPart(humanoid.leftLeg, EquipmentPart.BOOT_LEFT, FOOT, poseStack, collector, style, seed + 53);
            }
            default -> {
            }
        }
        poseStack.popPose();
    }

    private static void submitPart(ModelPart part, EquipmentPart equipPart, Bounds bounds,
                                   PoseStack poseStack, SubmitNodeCollector collector, int style, int seed) {
        if (part == null || !part.visible || part.isEmpty()) return;
        poseStack.pushPose();
        part.translateAndRotate(poseStack);

        double time = Util.getMillis() / 50.0;
        if (style == STORM) {
            StormArmorVfx.submit(collector, poseStack, bounds, time, seed);
        } else if (style == INFERNO) {
            InfernoArmorVfx.submit(collector, poseStack, bounds, time, seed);
        } else if (style == SHADOW) {
            ShadowArmorVfx.submit(collector, poseStack, bounds, time, seed);
        } else if (style == ARCANE) {
            ArcaneArmorVfx.submit(collector, poseStack, equipPart, time, seed);
        } else if (style == FROST) {
            FrostArmorVfx.submit(collector, poseStack, equipPart, bounds, time, seed);
        } else if (style == DIVINE) {
            DivineArmorVfx.submit(collector, poseStack, equipPart, bounds, time, seed);
        }

        poseStack.popPose();
    }

    private static boolean supportsCustomVfx(int style) {
        return style == STORM
                || style == INFERNO
                || style == SHADOW
                || style == ARCANE
                || style == FROST
                || style == DIVINE;
    }

    private static ItemGeometryInfo extractItemGeometry(ItemStackRenderStateAccessor state, ItemCategory category) {
        ItemStackRenderState.LayerRenderState[] layers = state.simpleskills$getLayers();
        int layerCount = Math.min(state.simpleskills$getActiveLayerCount(), layers.length);
        if (layerCount <= 0) return null;

        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        List<Vec3> rawQuadCenters = new ArrayList<>();

        for (int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
            ItemStackLayerRenderStateAccessor layer = (ItemStackLayerRenderStateAccessor) (Object) layers[layerIndex];
            ItemQuads itemQuads = layer.simpleskills$getQuads();
            if (itemQuads == null) continue;
            for (BakedQuad quad : itemQuads.all()) {
                double qx = 0, qy = 0, qz = 0;
                for (int vertex = 0; vertex < 4; vertex++) {
                    Vector3fc position = quad.position(vertex);
                    double vx = position.x();
                    double vy = position.y();
                    double vz = position.z();
                    minX = Math.min(minX, vx);
                    minY = Math.min(minY, vy);
                    minZ = Math.min(minZ, vz);
                    maxX = Math.max(maxX, vx);
                    maxY = Math.max(maxY, vy);
                    maxZ = Math.max(maxZ, vz);
                    qx += vx;
                    qy += vy;
                    qz += vz;
                }
                rawQuadCenters.add(new Vec3(qx * 0.25, qy * 0.25, qz * 0.25));
            }
        }
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ)) return null;

        double[] x = expandAxis(minX, maxX, 0.016, 0.004);
        double[] y = expandAxis(minY, maxY, 0.016, 0.004);
        double[] z = expandAxis(minZ, maxZ, 0.016, 0.004);
        Bounds bounds = new Bounds(x[0], x[1], y[0], y[1], z[0], z[1]);

        double w = Math.max(1.0E-4, bounds.width());
        double h = Math.max(1.0E-4, bounds.height());
        double centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5;
        double halfDepth = Math.max(0.032, bounds.depth() * 0.5);

        List<Vec3> allSamples = new ArrayList<>();
        List<Vec3> headSamples = new ArrayList<>();
        int stride = Math.max(1, rawQuadCenters.size() / 64);

        for (int i = 0; i < rawQuadCenters.size(); i += stride) {
            Vec3 p = rawQuadCenters.get(i);
            allSamples.add(p);
            double u = (p.x - bounds.minX()) / w;
            double v = (p.y - bounds.minY()) / h;
            double diag = (u + v) * 0.5;

            if (category == ItemCategory.PICKAXE) {
                if (diag >= 0.67 || (u >= 0.70 && v >= 0.45) || (v >= 0.70 && u >= 0.45)) {
                    headSamples.add(p);
                }
            } else if (category == ItemCategory.SWORD) {
                boolean isCrossguardWing = diag < 0.50 && Math.abs(u - v) > 0.13;
                if (diag >= 0.44 && !isCrossguardWing) {
                    headSamples.add(p);
                }
            } else {
                if (diag >= 0.54) {
                    headSamples.add(p);
                }
            }
        }

        double bladeStartT = category == ItemCategory.PICKAXE ? 0.65
                : (category == ItemCategory.SWORD ? 0.42 : 0.52);

        if (allSamples.isEmpty()) {
            for (int i = 0; i <= 12; i++) {
                double t = i / 12.0;
                Vec3 p = new Vec3(lerp(bounds.minX(), bounds.maxX(), t), lerp(bounds.minY(), bounds.maxY(), t), centerZ);
                allSamples.add(p);
                if (t >= bladeStartT) headSamples.add(p);
            }
        }
        if (headSamples.isEmpty()) {
            for (int i = 0; i <= 8; i++) {
                double t = lerp(bladeStartT, 0.96, i / 8.0);
                headSamples.add(new Vec3(lerp(bounds.minX(), bounds.maxX(), t), lerp(bounds.minY(), bounds.maxY(), t), centerZ));
            }
        }

        double guardT = category == ItemCategory.PICKAXE ? 0.62
                : (category == ItemCategory.SWORD ? 0.39 : 0.48);
        Vec3 guardPoint = new Vec3(
                lerp(bounds.minX(), bounds.maxX(), guardT),
                lerp(bounds.minY(), bounds.maxY(), guardT),
                centerZ);
        Vec3 bladeStart = new Vec3(
                lerp(bounds.minX(), bounds.maxX(), bladeStartT),
                lerp(bounds.minY(), bounds.maxY(), bladeStartT),
                centerZ);
        Vec3 bladeTip = new Vec3(
                lerp(bounds.minX(), bounds.maxX(), 0.96),
                lerp(bounds.minY(), bounds.maxY(), 0.96),
                centerZ);

        return new ItemGeometryInfo(category, bounds, halfDepth, allSamples, headSamples,
                guardPoint, bladeStart, bladeTip);
    }

    private static double[] expandAxis(double min, double max, double minSize, double padding) {
        if (max - min < minSize) {
            double center = (min + max) * 0.5;
            min = center - minSize * 0.5;
            max = center + minSize * 0.5;
        }
        return new double[]{min - padding, max + padding};
    }

    private record PendingVfx(int style, int seed, ItemCategory category) {}
}
