package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.agility.AgilityLevelPayload;

public final class AgilityClientState {
    private static volatile int level;
    private static volatile int dashUnlockLevel = 15;
    private static volatile int doubleJumpUnlockLevel = 10;

    private AgilityClientState() {}

    public static int getLevel() {
        return level;
    }

    public static void setLevel(int newLevel) {
        level = Math.max(0, Math.min(99, newLevel));
    }

    public static void set(AgilityLevelPayload payload) {
        setLevel(payload.level());
        dashUnlockLevel = payload.dashUnlockLevel();
        doubleJumpUnlockLevel = payload.doubleJumpUnlockLevel();
    }

    public static int getDashUnlockLevel() { return dashUnlockLevel; }
    public static int getDoubleJumpUnlockLevel() { return doubleJumpUnlockLevel; }

    public static void reset() {
        level = 0;
        dashUnlockLevel = 15;
        doubleJumpUnlockLevel = 10;
    }
}
