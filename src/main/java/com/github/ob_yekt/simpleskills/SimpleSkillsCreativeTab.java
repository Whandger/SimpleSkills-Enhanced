package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.runes.RuneContent;
import com.github.ob_yekt.simpleskills.abyss.AbyssContent;
import com.github.ob_yekt.simpleskills.abyss.AbyssalKingItems;
import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import com.github.ob_yekt.simpleskills.abyss.CrimsonCrystalItems;
import com.github.ob_yekt.simpleskills.celestial.CelestialAltarContent;
import com.github.ob_yekt.simpleskills.grinder.OreGrinderContent;
import com.github.ob_yekt.simpleskills.shields.GlowstoneShields;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Aba própria do SimpleSkills no inventário criativo. Reúne os itens do mod:
 * escudos de glowstone, runas e os blocos de minério de runa.
 */
public final class SimpleSkillsCreativeTab {
    public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "items"));

    private SimpleSkillsCreativeTab() {}

    public static void register() {
        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.simpleskills.items"))
                .icon(() -> new ItemStack(GlowstoneShields.GLOWSTONE_SHIELD))
                .displayItems((parameters, output) -> {
                    output.accept(GlowstoneShields.GLOWSTONE_SHIELD);
                    output.accept(GlowstoneShields.DIAMOND_GLOWSTONE_SHIELD);
                    output.accept(GlowstoneShields.NETHERITE_GLOWSTONE_SHIELD);
                    output.accept(RuneContent.RUNE);
                    output.accept(RuneContent.RUNE_LORE_ORE);
                    output.accept(RuneContent.RUNE_ORE_ITEM);
                    output.accept(RuneContent.DEEPSLATE_RUNE_ORE_ITEM);
                    output.accept(CelestialAltarContent.CELESTIAL_ALTAR_ITEM);
                    output.accept(CelestialAltarContent.TRANSCENDENCY_ESSENCE);
                    output.accept(OreGrinderContent.ITEM);
                    for (var abyssItem : AbyssContent.creativeItems()) output.accept(abyssItem);
                    for (var kingItem : AbyssalKingItems.creativeItems()) output.accept(kingItem);
                    for (var katana : AbyssalKatanaItems.creativeItems()) output.accept(katana);
                    for (var crimsonItem : CrimsonCrystalItems.creativeItems()) output.accept(crimsonItem);
                })
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, tab);
    }
}
