package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;

/**
 * The unique player weapon dropped by the Abyssal Samurai.
 *
 * The item keeps the vanilla melee damage pipeline for now. A dedicated client
 * renderer handles the sheathed/drawn player visuals; Parry and Lakai combat
 * actions are implemented separately from this item registration.
 */
public final class AbyssalKatanaItems {
    private static final Identifier KATANA_ID = Identifier.fromNamespaceAndPath(
            Simpleskills.MOD_ID, "katana_ultimo_juramento");
    private static final ResourceKey<Item> KATANA_KEY = ResourceKey.create(Registries.ITEM, KATANA_ID);

    public static Item KATANA_ULTIMO_JURAMENTO;

    private static boolean registered;

    private AbyssalKatanaItems() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // Initial combat baseline: slightly faster than a Netherite sword.
        // Final damage and attack speed will be tuned after Parry/Lakai playtests.
        KATANA_ULTIMO_JURAMENTO = Registry.register(BuiltInRegistries.ITEM, KATANA_KEY,
                new Item(new Item.Properties()
                        .setId(KATANA_KEY)
                        .modelId(KATANA_ID)
                        .sword(ToolMaterial.NETHERITE, 6.0F, -2.2F)
                        .durability(2800)
                        .enchantable(20)
                        .repairable(Items.NETHERITE_INGOT)
                        .fireResistant()));

        Simpleskills.LOGGER.info("Registered Katana do Último Juramento.");
    }

    public static boolean isKatana(Item item) {
        return KATANA_ULTIMO_JURAMENTO != null && item == KATANA_ULTIMO_JURAMENTO;
    }

    public static List<Item> creativeItems() {
        return KATANA_ULTIMO_JURAMENTO == null
                ? List.of()
                : List.of(KATANA_ULTIMO_JURAMENTO);
    }
}
