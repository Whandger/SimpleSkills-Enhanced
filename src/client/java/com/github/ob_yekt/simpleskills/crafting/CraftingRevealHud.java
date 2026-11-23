package com.github.ob_yekt.simpleskills.crafting;

/** @deprecated The timed HUD has been replaced by {@link CraftingRevealScreen}. */
@Deprecated
public final class CraftingRevealHud {
    private CraftingRevealHud() {}

    /** Retained as a no-op for source compatibility with older integrations. */
    public static void register() {}

    /** Forwards legacy calls to the full-screen reveal UI. */
    public static void accept(CraftingRevealPayload payload) {
        CraftingRevealScreen.accept(payload);
    }
}
