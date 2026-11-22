package com.github.ob_yekt.simpleskills.runes;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/** Persistent, synchronized cosmetic enchantment-glint style stored on each equipment stack. */
public final class RuneVisualStyles {
    public static final int VANILLA = 0;
    public static final int ARCANE = 1;
    public static final int INFERNO = 2;
    public static final int FROST = 3;
    public static final int STORM = 4;
    public static final int DIVINE = 5;
    public static final int SHADOW = 6;
    public static final int MAX_STYLE = SHADOW;

    public static DataComponentType<Integer> ENCHANTMENT_STYLE;
    private static boolean registered;

    private RuneVisualStyles() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ENCHANTMENT_STYLE = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "enchantment_style"),
                DataComponentType.<Integer>builder()
                        .persistent(Codec.intRange(VANILLA, MAX_STYLE))
                        .networkSynchronized(ByteBufCodecs.VAR_INT)
                        .build());
    }

    public static int getStyle(ItemStack stack) {
        if (stack == null || stack.isEmpty() || ENCHANTMENT_STYLE == null) return VANILLA;
        // The style is kept on the stack, but it only applies while the item still has enchantments.
        if (EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()) return VANILLA;
        int style = stack.getOrDefault(ENCHANTMENT_STYLE, VANILLA);
        return style < VANILLA || style > MAX_STYLE ? VANILLA : style;
    }

    /** Overload used by SpecialModelRenderers (such as shields) that receive an immutable DataComponentMap. */
    public static int getStyle(@Nullable DataComponentMap components) {
        if (components == null || ENCHANTMENT_STYLE == null) return VANILLA;
        ItemEnchantments enchantments = components.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return VANILLA;
        int style = components.getOrDefault(ENCHANTMENT_STYLE, VANILLA);
        return style < VANILLA || style > MAX_STYLE ? VANILLA : style;
    }

    public static void setStyle(ItemStack stack, int style) {
        if (stack == null || stack.isEmpty() || ENCHANTMENT_STYLE == null) return;
        if (style <= VANILLA) stack.remove(ENCHANTMENT_STYLE);
        else if (style <= MAX_STYLE) stack.set(ENCHANTMENT_STYLE, style);
    }

    public static String translationKey(int style) {
        return switch (style) {
            case ARCANE -> "gui.simpleskills.rune_infusion.style.arcane";
            case INFERNO -> "gui.simpleskills.rune_infusion.style.inferno";
            case FROST -> "gui.simpleskills.rune_infusion.style.frost";
            case STORM -> "gui.simpleskills.rune_infusion.style.storm";
            case DIVINE -> "gui.simpleskills.rune_infusion.style.divine";
            case SHADOW -> "gui.simpleskills.rune_infusion.style.shadow";
            default -> "gui.simpleskills.rune_infusion.style.vanilla";
        };
    }

    public static int color(int style) {
        return switch (style) {
            case ARCANE -> 0xFFB36CFF;
            case INFERNO -> 0xFFFF7045;
            case FROST -> 0xFF83D9FF;
            case STORM -> 0xFF43E7D5;
            case DIVINE -> 0xFFFFD96B;
            case SHADOW -> 0xFF8B5BC1;
            default -> 0xFFAA83FF;
        };
    }
}
