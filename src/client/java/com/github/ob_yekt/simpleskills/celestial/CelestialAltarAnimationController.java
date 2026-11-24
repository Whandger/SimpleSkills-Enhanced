package com.github.ob_yekt.simpleskills.celestial;

import com.github.ob_yekt.simpleskills.crafting.CraftingRevealPayload;
import com.github.ob_yekt.simpleskills.crafting.CraftingRevealScreen;
import com.github.ob_yekt.simpleskills.runes.RuneContent;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Dedicated client-side animation controller for Celestial Altar operations.
 * Separates Infusion, Salvage, and Enhance visual state machines, preserves pre-operation
 * item snapshots, and coordinates clean handoff into CraftingRevealScreen.
 */
public final class CelestialAltarAnimationController {
    public static final int INFUSION_DURATION_TICKS = 48; // 2.40s total (4 phases)
    public static final int SALVAGE_DURATION_TICKS = 50;  // 2.50s total (4 phases)
    public static final int ENHANCE_DURATION_TICKS = 24;  // 1.20s total

    private static InfusionState activeInfusion;
    private static SalvageState activeSalvage;
    private static EnhanceState activeEnhance;
    private static CraftingRevealPayload bufferedRevealPayload;

    private static final ItemSilhouette FALLBACK_SILHOUETTE = buildFallbackSilhouette();
    private static final Map<Item, ItemSilhouette> SILHOUETTE_CACHE = new HashMap<>();

    private CelestialAltarAnimationController() {}

    public static final class InfusionState {
        public final int containerId;
        public final ItemStack beforeEquipment;
        public final ItemStack afterEquipment;
        public final ItemStack essenceSnapshot;
        public int elapsedTicks;
        public boolean revealTriggered;

        InfusionState(int containerId, ItemStack beforeEquipment, ItemStack afterEquipment) {
            this.containerId = containerId;
            this.beforeEquipment = beforeEquipment.copy();
            this.afterEquipment = afterEquipment.copy();
            this.essenceSnapshot = new ItemStack(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
        }

        public float progress(float partialTick) {
            return Math.min(1.0F, (elapsedTicks + partialTick) / (float) INFUSION_DURATION_TICKS);
        }
    }

    public record CrackSegment(float x1, float y1, float x2, float y2, float d1, float d2, int depth) {}

    public record FracturePath(List<CrackSegment> segments, float maxDistance) {}

    public record DisintegrationFragment(
            float u, float v,
            float burstDx, float burstDy,
            float spinSpeed, int colorVariant
    ) {}

    public static final class SalvageState {
        public final int containerId;
        public final ItemStack sacrificedEquipment;
        public final ItemStack formedEssence;
        public final ItemSilhouette silhouette;
        public final List<FracturePath> fractures;
        public final List<DisintegrationFragment> fragments;
        public int elapsedTicks;

        SalvageState(int containerId, ItemStack sacrificedEquipment, ItemStack formedEssence) {
            this.containerId = containerId;
            this.sacrificedEquipment = sacrificedEquipment.copy();
            this.formedEssence = formedEssence.isEmpty()
                    ? new ItemStack(CelestialAltarContent.TRANSCENDENCY_ESSENCE)
                    : formedEssence.copy();
            this.silhouette = silhouetteOf(this.sacrificedEquipment);
            long seed = Item.getId(this.sacrificedEquipment.getItem()) * 1103515245L
                    ^ (long) this.sacrificedEquipment.getDamageValue() * 2654435761L;
            this.fractures = buildFractures(this.silhouette, seed);
            this.fragments = buildFragments(this.silhouette, seed ^ 0x5DEECE66DL);
        }

        public float progress(float partialTick) {
            return Math.min(1.0F, (elapsedTicks + partialTick) / (float) SALVAGE_DURATION_TICKS);
        }
    }

    public static final class EnhanceState {
        public final int containerId;
        public final int kind;
        public final ItemStack runeSnapshot;
        public final String targetId;
        public final String oldId;
        public final String oldTranslationKey;
        public final String newTranslationKey;
        public final double oldValue;
        public final double newValue;
        public final double minValue;
        public final double maxValue;
        public final int oldAmplifier;
        public final int newAmplifier;
        public final boolean reachedMax;
        public int elapsedTicks;

        EnhanceState(CelestialAltarVisualPayload payload) {
            this.containerId = payload.containerId();
            this.kind = payload.kind();
            this.runeSnapshot = new ItemStack(RuneContent.RUNE);
            this.targetId = payload.targetId();
            this.oldId = payload.oldId();
            this.oldTranslationKey = payload.oldTranslationKey();
            this.newTranslationKey = payload.newTranslationKey();
            this.oldValue = payload.oldValue();
            this.newValue = payload.newValue();
            this.minValue = payload.minValue();
            this.maxValue = payload.maxValue();
            this.oldAmplifier = payload.oldAmplifier();
            this.newAmplifier = payload.newAmplifier();
            this.reachedMax = payload.reachedMax();
        }

        public float progress(float partialTick) {
            return Math.min(1.0F, (elapsedTicks + partialTick) / (float) ENHANCE_DURATION_TICKS);
        }
    }

    public static final class ItemSilhouette {
        public final int width;
        public final int height;
        public final boolean[] opaque;
        public final boolean fallback;
        public final List<float[]> solidPixels;

        ItemSilhouette(int width, int height, boolean[] opaque, boolean fallback) {
            this.width = width;
            this.height = height;
            this.opaque = opaque;
            this.fallback = fallback;
            List<float[]> pixels = new ArrayList<>();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (opaque[y * width + x]) {
                        pixels.add(new float[]{(x + 0.5F) / width, (y + 0.5F) / height});
                    }
                }
            }
            if (pixels.isEmpty()) pixels.add(new float[]{0.5F, 0.5F});
            this.solidPixels = List.copyOf(pixels);
        }

        public boolean isOpaqueAt(float u, float v) {
            if (u < 0.0F || v < 0.0F || u >= 1.0F || v >= 1.0F) return false;
            int px = (int) (u * width);
            int py = (int) (v * height);
            return opaque[py * width + px];
        }
    }

    /** Called only from the client packet receiver after server confirmation. */
    public static void onServerConfirmed(CelestialAltarVisualPayload payload) {
        if (payload == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.gui.screen() instanceof CelestialAltarScreen altarScreen)) {
            return;
        }
        if (altarScreen.getMenu().containerId != payload.containerId()) {
            return;
        }

        switch (payload.kind()) {
            case CelestialAltarVisualPayload.KIND_INFUSION -> {
                activeSalvage = null;
                activeEnhance = null;
                activeInfusion = new InfusionState(
                        payload.containerId(), payload.beforeEquipment(), payload.afterEquipment());
            }
            case CelestialAltarVisualPayload.KIND_SALVAGE -> {
                activeInfusion = null;
                activeEnhance = null;
                activeSalvage = new SalvageState(
                        payload.containerId(), payload.beforeEquipment(), payload.afterEquipment());
            }
            case CelestialAltarVisualPayload.KIND_ENHANCE_STAT,
                 CelestialAltarVisualPayload.KIND_REROLL_STAT,
                 CelestialAltarVisualPayload.KIND_REROLL_PERK -> {
                activeEnhance = new EnhanceState(payload);
            }
            default -> { }
        }
    }

    /**
     * Intercepts CraftingRevealPayload when an Infusion ritual is playing in CelestialAltarScreen
     * so the reveal opens cleanly at Phase 4 instead of cutting off the ritual animation.
     */
    public static void interceptOrForwardReveal(CraftingRevealPayload payload) {
        if (payload == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (activeInfusion != null && mc.gui.screen() instanceof CelestialAltarScreen) {
            bufferedRevealPayload = payload;
            return;
        }
        CraftingRevealScreen.accept(payload);
    }

    public static void tick(int containerId) {
        if (activeInfusion != null) {
            if (activeInfusion.containerId != containerId) {
                flushBufferedRevealAndClearInfusion();
            } else {
                activeInfusion.elapsedTicks++;
                if (activeInfusion.elapsedTicks >= INFUSION_DURATION_TICKS && !activeInfusion.revealTriggered) {
                    activeInfusion.revealTriggered = true;
                    completeInfusionToReveal();
                }
            }
        }

        if (activeSalvage != null) {
            if (activeSalvage.containerId != containerId) {
                activeSalvage = null;
            } else {
                activeSalvage.elapsedTicks++;
                if (activeSalvage.elapsedTicks >= SALVAGE_DURATION_TICKS) {
                    activeSalvage = null;
                }
            }
        }

        if (activeEnhance != null) {
            if (activeEnhance.containerId != containerId) {
                activeEnhance = null;
            } else {
                activeEnhance.elapsedTicks++;
                if (activeEnhance.elapsedTicks >= ENHANCE_DURATION_TICKS) {
                    activeEnhance = null;
                }
            }
        }
    }

    private static void completeInfusionToReveal() {
        CraftingRevealPayload reveal = bufferedRevealPayload;
        bufferedRevealPayload = null;
        activeInfusion = null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.gui.screen() instanceof CelestialAltarScreen) {
            mc.player.closeContainer();
        }
        if (reveal != null) {
            CraftingRevealScreen.accept(reveal);
        }
    }

    public static void onScreenClosed() {
        flushBufferedRevealAndClearInfusion();
        activeSalvage = null;
        activeEnhance = null;
    }

    public static void reset() {
        bufferedRevealPayload = null;
        activeInfusion = null;
        activeSalvage = null;
        activeEnhance = null;
    }

    private static void flushBufferedRevealAndClearInfusion() {
        CraftingRevealPayload reveal = bufferedRevealPayload;
        bufferedRevealPayload = null;
        activeInfusion = null;
        if (reveal != null) {
            CraftingRevealScreen.accept(reveal);
        }
    }

    public static InfusionState getInfusion() { return activeInfusion; }
    public static SalvageState getSalvage() { return activeSalvage; }
    public static EnhanceState getEnhance() { return activeEnhance; }

    public static boolean isBlockingRitualActive() {
        return activeInfusion != null || activeSalvage != null;
    }

    // ------------------------------------------------------------------ Item silhouette & procedural fractures

    private static ItemSilhouette buildFallbackSilhouette() {
        boolean[] opaque = new boolean[16 * 16];
        for (int y = 2; y < 14; y++) {
            for (int x = 2; x < 14; x++) {
                opaque[y * 16 + x] = true;
            }
        }
        return new ItemSilhouette(16, 16, opaque, true);
    }

    public static ItemSilhouette silhouetteOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return FALLBACK_SILHOUETTE;
        Item item = stack.getItem();
        ItemSilhouette cached = SILHOUETTE_CACHE.get(item);
        if (cached != null) return cached;
        ItemSilhouette loaded = loadSilhouette(item);
        SILHOUETTE_CACHE.put(item, loaded);
        return loaded;
    }

    private static ItemSilhouette loadSilhouette(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        for (String folder : new String[]{"item", "block"}) {
            Identifier texture = Identifier.fromNamespaceAndPath(
                    id.getNamespace(), "textures/" + folder + "/" + id.getPath() + ".png");
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isEmpty()) continue;

            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                int w = image.getWidth();
                int h = Math.min(image.getHeight(), w);
                if (w < 1 || h < 1) continue;

                boolean[] opaque = new boolean[w * h];
                int count = 0;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        if ((image.getPixel(x, y) >>> 24) > 40) {
                            opaque[y * w + x] = true;
                            count++;
                        }
                    }
                }
                if (count >= 4) return new ItemSilhouette(w, h, opaque, false);
            } catch (Exception ignored) {
                // Fall back cleanly if an item uses a dynamic/special model.
            }
        }
        return FALLBACK_SILHOUETTE;
    }

    private static List<FracturePath> buildFractures(ItemSilhouette silhouette, long seed) {
        Random rng = new Random(seed);
        List<FracturePath> result = new ArrayList<>();
        List<float[]> pixels = silhouette.solidPixels;
        int crackCount = 4;
        for (int c = 0; c < crackCount; c++) {
            float[] anchor = pixels.get(rng.nextInt(pixels.size()));
            List<CrackSegment> segs = new ArrayList<>();
            float startAngle = (float) (c * (Math.PI * 0.5) + (rng.nextFloat() - 0.5F) * 0.8F);
            growCrackArm(rng, silhouette, segs, anchor[0], anchor[1], startAngle, 0.46F, 0, 0.0F);
            growCrackArm(rng, silhouette, segs, anchor[0], anchor[1],
                    startAngle + (float) Math.PI + (rng.nextFloat() - 0.5F) * 0.7F, 0.38F, 0, 0.0F);
            float maxDist = 0.01F;
            for (CrackSegment seg : segs) {
                maxDist = Math.max(maxDist, seg.d2());
            }
            result.add(new FracturePath(List.copyOf(segs), maxDist));
        }
        return List.copyOf(result);
    }

    private static void growCrackArm(Random rng, ItemSilhouette silhouette, List<CrackSegment> out,
                                     float u, float v, float angle, float length, int depth, float dist) {
        float walked = 0.0F;
        while (walked < length) {
            float step = Math.min(length - walked, 0.055F + rng.nextFloat() * 0.055F);
            if (step < 0.015F) break;

            float nu = u;
            float nv = v;
            boolean moved = false;
            for (int attempt = 0; attempt < 8 && !moved; attempt++) {
                float candidate = attempt == 0
                        ? angle + (float) rng.nextGaussian() * 0.45F
                        : angle + (rng.nextFloat() - 0.5F) * 3.0F;
                float tu = u + (float) Math.cos(candidate) * step;
                float tv = v + (float) Math.sin(candidate) * step;
                if (silhouette.isOpaqueAt(tu, tv)) {
                    angle = candidate;
                    nu = tu;
                    nv = tv;
                    moved = true;
                }
            }
            if (!moved) break;

            out.add(new CrackSegment(u, v, nu, nv, dist, dist + step, depth));
            dist += step;
            walked += step;
            u = nu;
            v = nv;

            if (depth < 2 && walked > 0.08F && rng.nextFloat() < (depth == 0 ? 0.38F : 0.24F)) {
                float side = rng.nextBoolean() ? 1.0F : -1.0F;
                growCrackArm(rng, silhouette, out, u, v,
                        angle + side * (0.5F + rng.nextFloat() * 0.75F),
                        (length - walked) * 0.65F + 0.06F, depth + 1, dist);
            }
        }
    }

    private static List<DisintegrationFragment> buildFragments(ItemSilhouette silhouette, long seed) {
        Random rng = new Random(seed);
        List<float[]> pixels = silhouette.solidPixels;
        int count = Math.min(28, Math.max(12, pixels.size()));
        List<DisintegrationFragment> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float[] uv = pixels.get((i * 17 + rng.nextInt(pixels.size())) % pixels.size());
            float angle = (float) (rng.nextDouble() * Math.PI * 2.0);
            float radius = 6.0F + rng.nextFloat() * 14.0F;
            list.add(new DisintegrationFragment(
                    uv[0], uv[1],
                    (float) Math.cos(angle) * radius,
                    (float) Math.sin(angle) * radius - 3.0F,
                    (rng.nextFloat() - 0.5F) * 0.45F,
                    i % 3
            ));
        }
        return List.copyOf(list);
    }
}
