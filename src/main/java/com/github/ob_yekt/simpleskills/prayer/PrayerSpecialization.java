package com.github.ob_yekt.simpleskills.prayer;

import java.util.Locale;
import net.minecraft.resources.Identifier;

/** The three mutually selectable Prayer magic disciplines. */
public enum PrayerSpecialization {
    DIVINE("divine", 0xFFFFD978, "divine"),
    UNHOLY("unholy", 0xFFB66BE8, "unholy"),
    ELEMENTALIST("elementalist", 0xFF68D9F2, "elementalist");

    private final String id;
    private final int color;
    private final String atlas;

    PrayerSpecialization(String id, int color, String atlas) {
        this.id = id;
        this.color = color;
        this.atlas = atlas;
    }

    public String id() { return id; }
    public int color() { return color; }
    public String atlas() { return "textures/gui/prayer/specializations/specialization_atlas.png"; }
    public Identifier icon() { return Identifier.fromNamespaceAndPath("simpleskills", "textures/gui/prayer/specializations/" + atlas + ".png"); }

    public static PrayerSpecialization fromId(String id) {
        if (id == null) return DIVINE;
        for (PrayerSpecialization value : values()) {
            if (value.id.equals(id.toLowerCase(Locale.ROOT))) return value;
        }
        return DIVINE;
    }
}
