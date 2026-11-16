package com.github.ob_yekt.simpleskills.prayer;

/** Elemental paths available inside the Elementalist discipline. */
public enum PrayerElement {
    ELECTRICITY("electricity", 0xFF72CFFF),
    FIRE("fire", 0xFFFF8748),
    ICE("ice", 0xFF9CEBFF);

    private final String id;
    private final int color;

    PrayerElement(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() { return id; }
    public int color() { return color; }
}
