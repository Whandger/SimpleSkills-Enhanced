package com.github.ob_yekt.simpleskills;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Custom sound event IDs backed by vanilla samples in assets/simpleskills/sounds.json. */
public final class SimpleSkillsSounds {
    public static final SoundEvent PERFECT_PARRY = register("perfect_parry");
    public static final SoundEvent MASTER_DEFENDER_PARRY = register("master_defender_parry");
    public static final SoundEvent PROJECTILE_REFLECTION = register("projectile_reflection");

    private SimpleSkillsSounds() {}

    public static void register() {
        // Referencing these fields initializes and registers the events.
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
