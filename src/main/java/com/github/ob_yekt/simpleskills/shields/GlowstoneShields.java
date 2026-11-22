package com.github.ob_yekt.simpleskills.shields;

import com.github.ob_yekt.simpleskills.Simpleskills;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Custom glowstone shields.
 *
 * Each shield uses the vanilla ShieldItem implementation and the vanilla BLOCKS_ATTACKS
 * component configuration. The component is registered as a delayed component because
 * its bypass tag is resolved from datapack registries after the world loads.
 */
public final class GlowstoneShields {
    /** All shields, including the vanilla shield, for the existing defense mechanics. */
    public static final TagKey<Item> SHIELDS = itemTag("shields");
    /** Only the three glowstone shields; used by the portable-light feature. */
    public static final TagKey<Item> GLOWSTONE_SHIELDS = itemTag("glowstone_shields");

    /** Vanilla shield: 336 durability. Glowstone is 50% more durable. */
    public static final int GLOWSTONE_DURABILITY = 504;
    /** Diamond variant: twice the vanilla shield's durability. */
    public static final int DIAMOND_GLOWSTONE_DURABILITY = 672;
    /** Netherite variant: three times the vanilla shield's durability. */
    public static final int NETHERITE_GLOWSTONE_DURABILITY = 1008;

    public static Item GLOWSTONE_SHIELD;
    public static Item DIAMOND_GLOWSTONE_SHIELD;
    public static Item NETHERITE_GLOWSTONE_SHIELD;

    private static boolean registered;

    private GlowstoneShields() {}

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path));
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        GLOWSTONE_SHIELD = registerShield("glowstone_shield", GLOWSTONE_DURABILITY);
        DIAMOND_GLOWSTONE_SHIELD = registerShield("diamond_glowstone_shield", DIAMOND_GLOWSTONE_DURABILITY);
        NETHERITE_GLOWSTONE_SHIELD = registerShield("netherite_glowstone_shield", NETHERITE_GLOWSTONE_DURABILITY);

        Simpleskills.LOGGER.info("Registered 3 blocking glowstone shields (durability: {}, {}, {}).",
                GLOWSTONE_DURABILITY, DIAMOND_GLOWSTONE_DURABILITY, NETHERITE_GLOWSTONE_DURABILITY);
    }

    /** True for any of the three glowstone shield variants. */
    public static boolean isGlowstoneShield(ItemStack stack) {
        if (GLOWSTONE_SHIELD == null) return false;
        return stack.is(GLOWSTONE_SHIELD) || stack.is(DIAMOND_GLOWSTONE_SHIELD)
                || stack.is(NETHERITE_GLOWSTONE_SHIELD);
    }

    private static Item registerShield(String path, int durability) {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

        Item.Properties properties = new Item.Properties()
                .setId(key)
                .modelId(id)
                .durability(durability)
                .repairable(ItemTags.PLANKS)
                // Keeps the shield in the offhand slot and gives it the normal shield item behavior.
                .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
                // This is the same block angle, delay, durability cost and sounds as a vanilla shield.
                // Delayed registration is required because BYPASSES_SHIELD is a datapack tag.
                .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                        0.25F,
                        1.0F,
                        List.of(new BlocksAttacks.DamageReduction(
                                90.0F, Optional.empty(), 0.0F, 1.0F
                        )),
                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK),
                        Optional.of(SoundEvents.SHIELD_BREAK)
                ))
                .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK);

        return Registry.register(BuiltInRegistries.ITEM, key, new ShieldItem(properties));
    }
}
