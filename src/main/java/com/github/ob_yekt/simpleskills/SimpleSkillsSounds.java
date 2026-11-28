package com.github.ob_yekt.simpleskills;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import com.github.ob_yekt.simpleskills.prayer.PrayerAbility;
import java.util.EnumMap;
import java.util.Map;

/** Custom sound event IDs backed by vanilla samples in assets/simpleskills/sounds.json. */
public final class SimpleSkillsSounds {
    public static final SoundEvent PERFECT_PARRY = register("perfect_parry");
    public static final SoundEvent MASTER_DEFENDER_PARRY = register("master_defender_parry");
    public static final SoundEvent ABYSS_THEME = register("abyss_theme");
    public static final SoundEvent ABYSS_THEME_CALM = register("abyss_theme_calm");
    public static final SoundEvent ABYSS_CRYSTAL = register("abyss_crystal");
    public static final SoundEvent ABYSS_CRACK = register("abyss_crack");
    public static final SoundEvent ABYSS_PEBBLES = register("abyss_pebbles");
    public static final SoundEvent COLOSSUS_STEP = register("colossus_step");
    public static final SoundEvent COLOSSUS_SWIPE = register("colossus_swipe");
    public static final SoundEvent COLOSSUS_LASER_CHARGE = register("colossus_laser_charge");
    public static final SoundEvent COLOSSUS_LASER_FIRE = register("colossus_laser_fire");
    public static final SoundEvent COLOSSUS_SLAM = register("colossus_slam");
    public static final SoundEvent COLOSSUS_LEAP_LAUNCH = register("colossus_leap_launch");
    public static final SoundEvent COLOSSUS_LEAP_LAND = register("colossus_leap_land");
    public static final SoundEvent COLOSSUS_BARRAGE = register("colossus_barrage");
    public static final SoundEvent COLOSSUS_AMBIENT = register("colossus_ambient");
    public static final SoundEvent COLOSSUS_HURT = register("colossus_hurt");
    public static final SoundEvent COLOSSUS_DEATH = register("colossus_death");
    public static final SoundEvent PROJECTILE_REFLECTION = register("projectile_reflection");
    private static final Map<PrayerAbility, SoundEvent> PRAYER_SPELLS = registerPrayerSounds();

    private SimpleSkillsSounds() {}

    public static void register() {
        // Referencing these fields initializes and registers the events.
    }

    public static SoundEvent prayerSpell(PrayerAbility ability) {
        return PRAYER_SPELLS.get(ability);
    }

    private static Map<PrayerAbility, SoundEvent> registerPrayerSounds() {
        Map<PrayerAbility, SoundEvent> sounds = new EnumMap<>(PrayerAbility.class);
        for (PrayerAbility ability : PrayerAbility.values()) {
            sounds.put(ability, register("prayer_" + ability.id()));
        }
        return Map.copyOf(sounds);
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
