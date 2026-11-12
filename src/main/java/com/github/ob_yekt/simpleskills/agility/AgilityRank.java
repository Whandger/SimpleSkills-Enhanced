package com.github.ob_yekt.simpleskills.agility;

/** Ten Agility ranks spanning levels 1 through 99. */
public enum AgilityRank {
    WANDERER(1, 9),
    LIGHTFOOT(10, 19),
    JUMPER(20, 29),
    ACROBAT(30, 39),
    WIND_RUNNER(40, 49),
    WRAITH(50, 59),
    ZEPHYR(60, 69),
    GALE(70, 79),
    LIGHTNING(80, 89),
    UNTOUCHABLE_LEGEND(90, 99);

    private final int minimumLevel;
    private final int maximumLevel;

    AgilityRank(int minimumLevel, int maximumLevel) {
        this.minimumLevel = minimumLevel;
        this.maximumLevel = maximumLevel;
    }

    public int getMinimumLevel() {
        return minimumLevel;
    }

    public static AgilityRank forLevel(int level) {
        int safeLevel = Math.max(1, Math.min(99, level));
        for (AgilityRank rank : values()) {
            if (safeLevel >= rank.minimumLevel && safeLevel <= rank.maximumLevel) return rank;
        }
        return WANDERER;
    }
}
