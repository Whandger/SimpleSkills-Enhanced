package com.github.ob_yekt.simpleskills.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Full-screen, manually dismissed crafting reveal with medieval fantasy RPG presentation.
 * Server batches are queued here and revealed sequentially; six equipment cards are displayed per page.
 */
public final class CraftingRevealScreen extends Screen {
    private static final int PANEL_WIDTH = 540;
    private static final int PANEL_HEIGHT = 316;
    private static final int PAGE_SIZE = 6;
    private static final int COLUMNS = 3;
    private static final int CARD_WIDTH = 160;
    private static final int CARD_HEIGHT = 93;
    private static final int CARD_GAP = 10;
    private static final int REVEAL_INTERVAL_TICKS = 7;
    private static final int REVEAL_FX_DURATION = 26;
    private static final int OPEN_ANIM_TICKS = 8;

    private static final int CLOSE_BTN_W = 20;
    private static final int CLOSE_BTN_H = 20;
    private static final int NAV_BTN_W = 26;
    private static final int NAV_BTN_H = 20;

    // Medieval Fantasy Palette (Obsidian, Forged Steel & Antique Gold)
    private static final int GOLD = 0xFFE9CF7A;
    private static final int GOLD_BRIGHT = 0xFFFFE79E;
    private static final int GOLD_DARK = 0xFF7D5F28;
    private static final int IRON_LIGHT = 0xFF3A4459;
    private static final int IRON_DARK = 0xFF0A0D14;
    private static final int PANEL_BG = 0xF5111520;
    private static final int PANEL_INNER = 0xFF171D2B;
    private static final int CHAMBER_BG = 0xFF0C0F17;
    private static final int TEXT = 0xFFF5F1E8;
    private static final int MUTED = 0xFFAAB6C8;
    private static final int DIM = 0xFF5D687A;
    private static final int STAT_GREEN = 0xFF7CE89E;
    private static final int STAT_RANGE_GRAY = 0xFF8792A3;
    private static final int PERK_CYAN = 0xFF7EE4FF;

    private static final List<String> NO_LEVEL_TEXT = List.of(
            "runic_edge", "anvil_ward",
            "guardian_oath", "arcane_mastery", "godslayer", "eternal_guardian",
            "divine_blessing", "aegis_of_ages", "fire_resistance", "featherweight", "thermal_smelting",
            "celestial_wings", "celestial_range", "oracle_vision", "aegis_of_purity",
            "phantom_stride", "divine_reflection", "astral_pursuit", "singularity_bolt",
            "poseidons_wrath", "worldbreaker", "buried_truth", "edens_dominion",
            "midas_touch", "abyssal_sovereign");

    private static final Set<String> GOLDEN_PERKS = Set.of(
            "celestial_wings", "celestial_range", "oracle_vision", "aegis_of_purity",
            "phantom_stride", "divine_reflection", "astral_pursuit", "singularity_bolt",
            "poseidons_wrath", "worldbreaker", "buried_truth", "edens_dominion",
            "midas_touch", "abyssal_sovereign");

    private static CraftingRevealScreen activeScreen;

    private record Entry(String itemName, String itemId, String rarityId,
                         JsonObject details, ItemStack icon) {}

    private record PanelMouse(double x, double y) {}

    private record RarityTheme(
            int primary,
            int bright,
            int deep,
            int glow,
            int cardBg,
            int cardHoverBg,
            int badgeBg,
            String tierStars,
            boolean highTier) {}

    private static final RarityTheme THEME_RARE = new RarityTheme(
            0xFF4DA6FF, 0xFFA6D6FF, 0xFF1A3D66, 0x334DA6FF,
            0xFF121A26, 0xFF192638, 0xCC102844, "✦", false);

    private static final RarityTheme THEME_UNIQUE = new RarityTheme(
            0xFF3CE8FF, 0xFFBDF9FF, 0xFF135061, 0x3D3CE8FF,
            0xFF101C24, 0xFF172936, 0xCC0D3542, "✦", false);

    private static final RarityTheme THEME_EPIC = new RarityTheme(
            0xFFBA55FF, 0xFFE5B8FF, 0xFF47196E, 0x4ABA55FF,
            0xFF171124, 0xFF231936, 0xCC2E1047, "✦✦", false);

    private static final RarityTheme THEME_LEGENDARY = new RarityTheme(
            0xFFFFB62E, 0xFFFFE79E, 0xFF6E430A, 0x5CFFB62E,
            0xFF1F170E, 0xFF2E2215, 0xCC4A2B06, "✦✦✦", true);

    private static final RarityTheme THEME_MYTHIC = new RarityTheme(
            0xFFFF455B, 0xFFFFB3BC, 0xFF6E101F, 0x6BFF455B,
            0xFF211015, 0xFF30171F, 0xCC4D0B16, "✦✦✦✦", true);

    private static final RarityTheme THEME_TRANSCENDENT = new RarityTheme(
            0xFFFFDF6B, 0xFFFFFBE0, 0xFF805B12, 0x7DFFE270,
            0xFF221B10, 0xFF332817, 0xCC543A08, "✦✦✦✦✦", true);

    private final List<Entry> entries = new ArrayList<>();
    private final Set<Integer> revealedEntries = new HashSet<>();
    private int currentPage;
    private int ticksUntilNextReveal;
    private int screenTicks;
    private final Map<Integer, Integer> revealFxTicksByEntry = new HashMap<>();

    private float uiScale = 1.0F;
    private int panelScreenLeft;
    private int panelScreenTop;

    private CraftingRevealScreen() {
        super(Component.translatable("gui.simpleskills.crafting_reveal.title"));
    }

    /** Called by the client packet receiver. A batch opens one screen and subsequent batches append. */
    public static void accept(CraftingRevealPayload payload) {
        if (payload == null || payload.entriesJson() == null || payload.entriesJson().isBlank()) return;

        List<Entry> incoming = parseEntries(payload.entriesJson());
        if (incoming.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        CraftingRevealScreen screen = activeScreen;
        if (screen == null) {
            screen = new CraftingRevealScreen();
            activeScreen = screen;
            screen.appendEntries(incoming);
            client.gui.setScreen(screen);
            return;
        }

        screen.appendEntries(incoming);
    }

    private static List<Entry> parseEntries(String json) {
        List<Entry> parsed = new ArrayList<>();
        try {
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String rarity = stringValue(object, "rarity", "common").toLowerCase(Locale.ROOT);
                // Common and Rare intentionally remain silent; the reveal is for Unique and above.
                if (rarityRank(rarity) < rarityRank("unique")) continue;

                String itemName = stringValue(object, "itemName", "Crafted Equipment");
                String itemId = stringValue(object, "itemId", "minecraft:barrier");
                Identifier identifier = Identifier.tryParse(itemId);
                Item item = identifier == null ? Items.BARRIER : BuiltInRegistries.ITEM.getValue(identifier);
                if (item == null) item = Items.BARRIER;

                JsonObject details = object.has("details") && object.get("details").isJsonObject()
                        ? object.getAsJsonObject("details") : new JsonObject();
                parsed.add(new Entry(itemName, itemId, rarity, details, new ItemStack(item)));
            }
        } catch (RuntimeException ignored) {
            // A malformed cosmetic packet must never interrupt crafting or client play.
        }
        return parsed;
    }

    private static String stringValue(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull()
                    ? object.get(key).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private void appendEntries(List<Entry> incoming) {
        if (incoming.isEmpty()) return;
        entries.addAll(incoming);
        // Keep the player's current page. New entries on that page start their own reveal sequence.
        ticksUntilNextReveal = 0;
    }

    @Override
    protected void init() {
        this.uiScale = Math.min(1.0F, Math.min((this.width - 20.0F) / PANEL_WIDTH,
                (this.height - 20.0F) / PANEL_HEIGHT));
        this.uiScale = Math.max(0.1F, this.uiScale);
        this.panelScreenLeft = Math.round((this.width - PANEL_WIDTH * this.uiScale) / 2.0F);
        this.panelScreenTop = Math.round((this.height - PANEL_HEIGHT * this.uiScale) / 2.0F);
    }

    private void changePage(int direction) {
        int target = Math.max(0, Math.min(pageCount() - 1, currentPage + direction));
        if (target == currentPage) return;
        currentPage = target;
        ticksUntilNextReveal = 0;
    }

    @Override
    public void tick() {
        super.tick();
        screenTicks++;
        int pageStart = currentPage * PAGE_SIZE;
        int pageEnd = Math.min(entries.size(), pageStart + PAGE_SIZE);
        int nextToReveal = -1;
        for (int i = pageStart; i < pageEnd; i++) {
            if (!revealedEntries.contains(i)) {
                nextToReveal = i;
                break;
            }
        }
        if (nextToReveal >= 0) {
            if (ticksUntilNextReveal > 0) {
                ticksUntilNextReveal--;
            } else {
                Entry revealed = entries.get(nextToReveal);
                revealedEntries.add(nextToReveal);
                revealFxTicksByEntry.put(nextToReveal, REVEAL_FX_DURATION);
                ticksUntilNextReveal = REVEAL_INTERVAL_TICKS;
                playRaritySound(revealed.rarityId());
            }
        }
        revealFxTicksByEntry.replaceAll((index, ticks) -> ticks - 1);
        revealFxTicksByEntry.values().removeIf(ticks -> ticks <= 0);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        float openRaw = Math.min(1.0F, (screenTicks + partialTick) / OPEN_ANIM_TICKS);
        float openEase = 1.0F - (float) Math.pow(1.0F - openRaw, 3.0);

        int backdropAlpha = Math.round(0xC6 * openEase);
        graphics.fill(0, 0, this.width, this.height, withAlpha(0x05070C, backdropAlpha));

        float animScale = 0.94F + 0.06F * openEase;
        float slideY = (1.0F - openEase) * 8.0F;
        PanelMouse panelMouse = toPanelMouse(mouseX, mouseY, partialTick);
        int localMouseX = (int) Math.round(panelMouse.x());
        int localMouseY = (int) Math.round(panelMouse.y());

        graphics.pose().pushMatrix();
        graphics.pose().translate(panelScreenLeft, panelScreenTop + slideY);
        graphics.pose().scale(uiScale, uiScale);
        graphics.pose().translate(PANEL_WIDTH / 2.0F, PANEL_HEIGHT / 2.0F);
        graphics.pose().scale(animScale, animScale);
        graphics.pose().translate(-PANEL_WIDTH / 2.0F, -PANEL_HEIGHT / 2.0F);

        drawMainFrame(graphics, partialTick);
        drawHeader(graphics, localMouseX, localMouseY);

        if (entries.size() == 1) {
            drawSingleItem(graphics, localMouseX, localMouseY, partialTick);
        } else {
            drawGrid(graphics, localMouseX, localMouseY, partialTick);
        }

        drawNavigation(graphics, localMouseX, localMouseY);
        drawCloseHint(graphics);
        // Render inspect details last so they always appear above cards, navigation and close controls.
        drawHoveredDetails(graphics, localMouseX, localMouseY);
        graphics.pose().popMatrix();
    }

    private void drawMainFrame(GuiGraphicsExtractor graphics, float partialTick) {
        // Soft drop shadow
        graphics.fill(-5, -4, PANEL_WIDTH + 5, PANEL_HEIGHT + 6, 0x88020306);
        // Outer forged-iron & gilded double bezel
        graphics.fill(-3, -3, PANEL_WIDTH + 3, PANEL_HEIGHT + 3, 0xFF1F2636);
        graphics.fill(-2, -2, PANEL_WIDTH + 2, PANEL_HEIGHT + 2, GOLD_DARK);
        graphics.fill(-1, -1, PANEL_WIDTH + 1, PANEL_HEIGHT + 1, GOLD);
        // Main obsidian slate body
        graphics.fill(0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_BG);
        graphics.fill(6, 6, PANEL_WIDTH - 6, PANEL_HEIGHT - 6, PANEL_INNER);
        drawOutline(graphics, 6, 6, PANEL_WIDTH - 12, PANEL_HEIGHT - 12, IRON_LIGHT);

        // Corner gold filigree brackets on main panel
        drawCornerBrackets(graphics, 8, 8, PANEL_WIDTH - 16, PANEL_HEIGHT - 16, GOLD, GOLD_BRIGHT, 7);

        // Recessed central forge chamber
        int chamberX = 14;
        int chamberY = 55;
        int chamberW = PANEL_WIDTH - 28;
        int chamberH = 212;
        graphics.fill(chamberX, chamberY, chamberX + chamberW, chamberY + chamberH, CHAMBER_BG);
        drawBevelBox(graphics, chamberX, chamberY, chamberW, chamberH, IRON_DARK, 0xFF283146);
        drawCornerBrackets(graphics, chamberX + 2, chamberY + 2, chamberW - 4, chamberH - 4, GOLD_DARK, GOLD, 5);

        // Subtle ambient forge embers rising in the background of the chamber
        double time = screenTicks + partialTick;
        for (int i = 0; i < 14; i++) {
            double cycle = ((time * (0.011 + (i % 4) * 0.003)) + i * 0.27) % 1.0;
            int ex = chamberX + 18 + ((i * 73) % (chamberW - 36));
            int ey = chamberY + chamberH - 8 - (int) (cycle * (chamberH - 18));
            int alpha = (int) (Math.sin(cycle * Math.PI) * 55.0);
            if (alpha > 4) {
                int emberColor = (i % 3 == 0) ? withAlpha(GOLD, alpha) : withAlpha(0xFF5D769E, alpha);
                graphics.fill(ex, ey, ex + 2, ey + 2, emberColor);
            }
        }
    }

    private void drawHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int centerX = PANEL_WIDTH / 2;

        // Decorative title plaque
        int bannerW = 248;
        int bannerX = centerX - bannerW / 2;
        graphics.fill(bannerX, 10, bannerX + bannerW, 27, 0xCC0D111A);
        drawOutline(graphics, bannerX, 10, bannerW, 17, GOLD_DARK);
        graphics.fill(bannerX + 2, 12, bannerX + 4, 25, GOLD);
        graphics.fill(bannerX + bannerW - 4, 12, bannerX + bannerW - 2, 25, GOLD);

        Component headerTitle = Component.translatable("gui.simpleskills.crafting_reveal.forged");
        int titleW = this.font.width(headerTitle);
        graphics.text(this.font, headerTitle, centerX - titleW / 2, 15, GOLD_BRIGHT, true);
        graphics.text(this.font, Component.literal("✦"), centerX - titleW / 2 - 12, 15, GOLD, true);
        graphics.text(this.font, Component.literal("✦"), centerX + titleW / 2 + 6, 15, GOLD, true);

        // Counter & ornamental divider line
        Component counter = Component.translatable(
                "gui.simpleskills.crafting_reveal.counter", revealedEntries.size(), entries.size());
        centered(graphics, counter, 32, MUTED);

        int lineY = 46;
        graphics.fill(26, lineY, centerX - 10, lineY + 1, GOLD_DARK);
        graphics.fill(centerX + 10, lineY, PANEL_WIDTH - 26, lineY + 1, GOLD_DARK);
        graphics.fill(centerX - 60, lineY, centerX - 10, lineY + 1, GOLD);
        graphics.fill(centerX + 10, lineY, centerX + 60, lineY + 1, GOLD);
        drawDiamond(graphics, centerX, lineY, 3, GOLD_BRIGHT);

        // Integrated ornate Close Button (top-right)
        int cx = closeButtonX();
        int cy = closeButtonY();
        boolean closeHover = inside(mouseX, mouseY, cx, cy, CLOSE_BTN_W, CLOSE_BTN_H);
        graphics.fill(cx, cy, cx + CLOSE_BTN_W, cy + CLOSE_BTN_H, closeHover ? 0xFF3B1822 : 0xFF141924);
        drawOutline(graphics, cx, cy, CLOSE_BTN_W, CLOSE_BTN_H, closeHover ? 0xFFFF6B7A : GOLD_DARK);
        drawCornerBrackets(graphics, cx + 1, cy + 1, CLOSE_BTN_W - 2, CLOSE_BTN_H - 2,
                closeHover ? 0xFFFF9AA4 : GOLD, closeHover ? 0xFFFFFFFF : GOLD_BRIGHT, 2);
        centeredIn(graphics, Component.literal("×"), cx, cy + 6, CLOSE_BTN_W,
                closeHover ? 0xFFFFD6DA : GOLD_BRIGHT);
    }

    private void drawSingleItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Entry entry = entries.get(0);
        RarityTheme theme = themeFor(entry.rarityId());
        int cardWidth = 408;
        int cardHeight = 182;
        int x = (PANEL_WIDTH - cardWidth) / 2;
        int y = 70;
        boolean revealed = revealedEntries.contains(0);
        boolean hover = revealed && inside(mouseX, mouseY, x, y, cardWidth, cardHeight);

        drawCardFrame(graphics, x, y, cardWidth, cardHeight, entry.rarityId(), hover, revealed, partialTick, 0);

        if (!revealed) {
            int sealSize = 68;
            int sx = x + (cardWidth - sealSize) / 2;
            int sy = y + 38;
            drawSealedCore(graphics, sx, sy, sealSize, theme, partialTick, 0);
            centered(graphics, Component.translatable("gui.simpleskills.crafting_reveal.revealing"), y + 122, MUTED);
            return;
        }

        int fxTicks = revealFxTicksByEntry.getOrDefault(0, 0);
        float popScale = revealPopScale(fxTicks, partialTick);
        float bob = theme.highTier() ? (float) Math.sin((screenTicks + partialTick) * 0.12) * 2.0F : 0.0F;

        // Left Grand Reliquary Pedestal
        int pedSize = 114;
        int pedX = x + 18;
        int pedY = y + 34;
        drawIconPedestal(graphics, pedX, pedY, pedSize, theme, hover, partialTick, 0);

        float iconScale = 3.6F * popScale;
        float iconDrawSize = 16.0F * iconScale;
        float iconX = pedX + (pedSize - iconDrawSize) / 2.0F;
        float iconY = pedY + (pedSize - iconDrawSize) / 2.0F + bob;

        graphics.pose().pushMatrix();
        graphics.pose().translate(iconX, iconY);
        graphics.pose().scale(iconScale, iconScale);
        graphics.item(entry.icon(), 0, 0);
        graphics.pose().popMatrix();

        // Right Showcase Panel
        int infoX = pedX + pedSize + 18;
        int infoW = cardWidth - (pedSize + 50);

        // Rarity banner
        Component rarityLabel = Component.literal(theme.tierStars() + " ")
                .append(rarityName(entry.rarityId()))
                .append(Component.literal(" " + theme.tierStars()));
        graphics.fill(infoX, y + 20, infoX + infoW, y + 36, theme.badgeBg());
        drawOutline(graphics, infoX, y + 20, infoW, 16, withAlpha(theme.primary(), 170));
        centeredIn(graphics, rarityLabel, infoX, y + 24, infoW, theme.bright());

        // Equipment Name
        String shownName = fitText(entry.itemName(), infoW);
        graphics.text(this.font, Component.literal(shownName), infoX, y + 44, TEXT, true);

        // Ornamental divider
        graphics.fill(infoX, y + 58, infoX + infoW, y + 59, withAlpha(theme.primary(), 130));

        // Inline rolled stats & perks preview
        List<Component> lines = buildDetailLines(entry);
        int statCount = countPrimaryStats(entry);
        int maxInline = Math.min(4, lines.size());
        int lineY = y + 66;
        for (int i = 0; i < maxInline; i++) {
            String lineText = fitText("◆ " + lines.get(i).getString(), infoW);
            int lineColor = i < statCount ? STAT_GREEN
                    : (isGoldenPerkLine(entry, i) ? GOLD_BRIGHT : PERK_CYAN);
            graphics.text(this.font, Component.literal(lineText), infoX, lineY, lineColor, false);
            lineY += 13;
        }

        // Hover inspect pill
        int hintY = y + cardHeight - 25;
        graphics.fill(infoX, hintY, infoX + infoW, hintY + 15, hover ? withAlpha(theme.primary(), 55) : 0x800B0E15);
        drawOutline(graphics, infoX, hintY, infoW, 15, hover ? theme.primary() : IRON_LIGHT);
        centeredIn(graphics, Component.translatable("gui.simpleskills.crafting_reveal.hover_details"),
                infoX, hintY + 4, infoW, hover ? theme.bright() : MUTED);

        if (fxTicks > 0) {
            drawRevealBurst(graphics, pedX + pedSize / 2, pedY + pedSize / 2, entry.rarityId(), partialTick, fxTicks);
        }
    }

    private void drawGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int gridWidth = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * CARD_GAP;
        int gridLeft = (PANEL_WIDTH - gridWidth) / 2;
        int gridTop = 64;
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);

        for (int index = start; index < end; index++) {
            int pageIndex = index - start;
            int x = gridLeft + (pageIndex % COLUMNS) * (CARD_WIDTH + CARD_GAP);
            int y = gridTop + (pageIndex / COLUMNS) * (CARD_HEIGHT + CARD_GAP);
            Entry entry = entries.get(index);
            RarityTheme theme = themeFor(entry.rarityId());
            boolean revealed = revealedEntries.contains(index);
            boolean hover = revealed && inside(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT);

            drawCardFrame(graphics, x, y, CARD_WIDTH, CARD_HEIGHT, entry.rarityId(), hover, revealed, partialTick, index);

            if (!revealed) {
                int sealSize = 36;
                drawSealedCore(graphics, x + (CARD_WIDTH - sealSize) / 2, y + 22, sealSize, theme, partialTick, index);
                centeredIn(graphics, Component.translatable("gui.simpleskills.crafting_reveal.revealing"),
                        x + 6, y + 66, CARD_WIDTH - 12, DIM);
                continue;
            }

            // Top Rarity Header Ribbon
            graphics.fill(x + 2, y + 2, x + CARD_WIDTH - 2, y + 15, theme.badgeBg());
            graphics.fill(x + 2, y + 15, x + CARD_WIDTH - 2, y + 16, withAlpha(theme.primary(), 150));

            Component rarityHeader = Component.literal(theme.tierStars() + " ").append(rarityName(entry.rarityId()));
            graphics.text(this.font, rarityHeader, x + 6, y + 5, theme.bright(), true);

            int perkCount = countPerks(entry);
            if (perkCount > 0) {
                String badge = "+" + perkCount;
                int bw = this.font.width(badge) + 6;
                int bx = x + CARD_WIDTH - bw - 4;
                graphics.fill(bx, y + 4, bx + bw, y + 13, withAlpha(theme.primary(), 70));
                graphics.text(this.font, Component.literal(badge), bx + 3, y + 5, theme.bright(), false);
            }

            // Highlighted Item Icon Pedestal
            int fxTicks = revealFxTicksByEntry.getOrDefault(index, 0);
            float popScale = revealPopScale(fxTicks, partialTick);
            float bob = theme.highTier() ? (float) Math.sin((screenTicks + partialTick + index * 3) * 0.14) * 1.2F : 0.0F;

            int pedSize = 44;
            int pedX = x + 7;
            int pedY = y + 22;
            drawIconPedestal(graphics, pedX, pedY, pedSize, theme, hover, partialTick, index);

            float iconScale = 1.85F * popScale;
            float iconDrawSize = 16.0F * iconScale;
            float iconX = pedX + (pedSize - iconDrawSize) / 2.0F;
            float iconY = pedY + (pedSize - iconDrawSize) / 2.0F + bob;

            graphics.pose().pushMatrix();
            graphics.pose().translate(iconX, iconY);
            graphics.pose().scale(iconScale, iconScale);
            graphics.item(entry.icon(), 0, 0);
            graphics.pose().popMatrix();

            // Right Info Area
            int infoX = pedX + pedSize + 6;
            int infoW = CARD_WIDTH - (pedSize + 18);

            String shownName = fitText(entry.itemName(), infoW);
            graphics.text(this.font, Component.literal(shownName), infoX, y + 22, TEXT, true);

            // Compact stat chips on card
            List<StatChip> chips = buildStatChips(entry);
            int chipY = y + 36;
            for (int c = 0; c < Math.min(2, chips.size()); c++) {
                StatChip chip = chips.get(c);
                int cw = Math.min(infoW, this.font.width(chip.label()) + 8);
                graphics.fill(infoX, chipY, infoX + cw, chipY + 12, chip.bgColor());
                graphics.fill(infoX, chipY, infoX + 2, chipY + 12, chip.accentColor());
                graphics.text(this.font, Component.literal(chip.label()), infoX + 5, chipY + 2, chip.textColor(), false);
                chipY += 14;
            }

            // Bottom Inspect Prompt Bar
            int footerY = y + CARD_HEIGHT - 14;
            graphics.fill(x + 3, footerY, x + CARD_WIDTH - 3, y + CARD_HEIGHT - 3,
                    hover ? withAlpha(theme.primary(), 55) : 0x66080A10);
            String inspectStr = fitText(
                    Component.translatable("gui.simpleskills.crafting_reveal.hover_details").getString(),
                    CARD_WIDTH - 12);
            centeredIn(graphics, Component.literal(inspectStr), x + 4, footerY + 2, CARD_WIDTH - 8,
                    hover ? theme.bright() : MUTED);

            if (fxTicks > 0) {
                drawRevealBurst(graphics, pedX + pedSize / 2, pedY + pedSize / 2, entry.rarityId(), partialTick, fxTicks);
            }
        }

    }

    private void drawCardFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                               String rarity, boolean hover, boolean revealed, float partialTick, int seed) {
        RarityTheme theme = themeFor(rarity);
        double time = screenTicks + partialTick + seed * 5.0;
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * (theme.highTier() ? 0.16 : 0.08));

        if (!revealed) {
            int subtleBorder = lerpColor(0xFF333B4D, theme.deep(), 0.45F + 0.35F * pulse);
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, subtleBorder);
            graphics.fill(x, y, x + width, y + height, 0xFF10141E);
            drawCornerBrackets(graphics, x + 2, y + 2, width - 4, height - 4, 0xFF424C63, subtleBorder, 4);
            return;
        }

        // Outer ambient aura for high-tier rarities (Legendary, Mythic, Transcendent)
        if (theme.highTier() || hover) {
            int auraAlpha = hover ? 85 : (int) (32 + pulse * 36);
            int auraSpread = rarity.equals("transcendent") ? 3 : 2;
            graphics.fill(x - auraSpread, y - auraSpread, x + width + auraSpread, y + height + auraSpread,
                    withAlpha(theme.primary(), auraAlpha));
        }

        // Outer dark bevel + colored primary border
        int borderColor = hover ? theme.bright() : lerpColor(theme.primary(), theme.bright(), pulse * 0.35F);
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, theme.deep());
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);

        // Card interior background
        graphics.fill(x, y, x + width, y + height, hover ? theme.cardHoverBg() : theme.cardBg());

        // Inner subtle specular rim
        drawOutline(graphics, x + 1, y + 1, width - 2, height - 2, withAlpha(theme.primary(), hover ? 95 : 48));

        // Ornate corner brackets
        int bracketLen = theme.highTier() ? 6 : 4;
        drawCornerBrackets(graphics, x + 1, y + 1, width - 2, height - 2, borderColor, theme.bright(), bracketLen);

        // Transcendent crown diamond ornament on top edge
        if (rarity.equals("transcendent")) {
            int cx = x + width / 2;
            drawDiamond(graphics, cx, y - 1, 3, theme.bright());
            drawDiamond(graphics, cx - 10, y - 1, 2, theme.primary());
            drawDiamond(graphics, cx + 10, y - 1, 2, theme.primary());
        } else if (rarity.equals("mythic") || rarity.equals("legendary")) {
            drawDiamond(graphics, x + width / 2, y - 1, 2, theme.bright());
        }

        // Animated light sheen sweep across Mythic & Transcendent borders
        if (rarity.equals("transcendent") || rarity.equals("mythic")) {
            float sweep = (float) ((time * 0.025) % 1.0);
            int sweepX = x + 6 + (int) (sweep * (width - 24));
            int sheenColor = withAlpha(theme.bright(), rarity.equals("transcendent") ? 170 : 120);
            graphics.fill(sweepX, y - 1, sweepX + 12, y, sheenColor);
            graphics.fill(x + width - (sweepX - x) - 12, y + height, x + width - (sweepX - x), y + height + 1, sheenColor);
        }

        // Ambient ascending particles inside Legendary, Mythic & Transcendent cards
        if (theme.highTier()) {
            int count = rarity.equals("transcendent") ? 6 : (rarity.equals("mythic") ? 4 : 3);
            for (int p = 0; p < count; p++) {
                double pCycle = ((time * (0.022 + p * 0.004)) + p * 0.31) % 1.0;
                int px = x + 8 + ((p * 47 + seed * 19) % Math.max(1, width - 16));
                int py = y + height - 6 - (int) (pCycle * (height - 14));
                int pAlpha = (int) (Math.sin(pCycle * Math.PI) * 165.0);
                if (pAlpha > 10) {
                    int pColor = (rarity.equals("transcendent") && p % 2 == 0)
                            ? withAlpha(0xFF9BF6FF, pAlpha)
                            : withAlpha(theme.bright(), pAlpha);
                    graphics.fill(px, py, px + 2, py + 2, pColor);
                }
            }
        }
    }

    private void drawIconPedestal(GuiGraphicsExtractor graphics, int x, int y, int size,
                                  RarityTheme theme, boolean hover, float partialTick, int seed) {
        double time = screenTicks + partialTick + seed * 4.0;
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * 0.14);

        // Recessed dark socket with rarity border
        graphics.fill(x, y, x + size, y + size, hover ? theme.bright() : theme.primary());
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF07090F);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF0E131E);

        // Concentric soft glow behind the item icon
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r1 = size / 2 - 4;
        int r2 = size / 3;
        int r3 = size / 5;
        int alphaBoost = hover ? 30 : (int) (pulse * 22);

        graphics.fill(cx - r1, cy - r1, cx + r1, cy + r1, withAlpha(theme.primary(), 26 + alphaBoost / 2));
        graphics.fill(cx - r2, cy - r2, cx + r2, cy + r2, withAlpha(theme.primary(), 46 + alphaBoost));
        graphics.fill(cx - r3, cy - r3, cx + r3, cy + r3, withAlpha(theme.bright(), 55 + alphaBoost));

        // Inner corner ticks on pedestal
        drawCornerBrackets(graphics, x + 2, y + 2, size - 4, size - 4,
                withAlpha(theme.primary(), 180), theme.bright(), 3);
    }

    private void drawSealedCore(GuiGraphicsExtractor graphics, int x, int y, int size,
                                RarityTheme theme, float partialTick, int seed) {
        double time = screenTicks + partialTick + seed * 6.0;
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * 0.18);

        graphics.fill(x, y, x + size, y + size, lerpColor(0xFF283042, theme.primary(), 0.35F + 0.45F * pulse));
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF0B0E16);
        drawCornerBrackets(graphics, x + 2, y + 2, size - 4, size - 4, theme.deep(), theme.primary(), 3);

        int cx = x + size / 2;
        int cy = y + size / 2;
        int glowRadius = size / 4 + (int) (pulse * 3);
        graphics.fill(cx - glowRadius, cy - glowRadius, cx + glowRadius, cy + glowRadius,
                withAlpha(theme.primary(), (int) (35 + pulse * 55)));

        centeredIn(graphics, Component.literal("✦"), x, cy - 4, size,
                lerpColor(MUTED, theme.bright(), 0.4F + 0.6F * pulse));
    }

    /** Finds the revealed card under the pointer and draws its inspect panel on the final render layer. */
    private void drawHoveredDetails(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (entries.isEmpty()) return;

        if (entries.size() == 1) {
            int cardWidth = 408;
            int cardHeight = 182;
            int x = (PANEL_WIDTH - cardWidth) / 2;
            int y = 70;
            if (revealedEntries.contains(0) && inside(mouseX, mouseY, x, y, cardWidth, cardHeight)) {
                drawHoverDetails(graphics, entries.get(0), mouseX, mouseY);
            }
            return;
        }

        int gridWidth = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * CARD_GAP;
        int gridLeft = (PANEL_WIDTH - gridWidth) / 2;
        int gridTop = 64;
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        for (int index = start; index < end; index++) {
            if (!revealedEntries.contains(index)) continue;
            int pageIndex = index - start;
            int x = gridLeft + (pageIndex % COLUMNS) * (CARD_WIDTH + CARD_GAP);
            int y = gridTop + (pageIndex / COLUMNS) * (CARD_HEIGHT + CARD_GAP);
            if (inside(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT)) {
                drawHoverDetails(graphics, entries.get(index), mouseX, mouseY);
                return;
            }
        }
    }

    private void drawHoverDetails(GuiGraphicsExtractor graphics, Entry entry, int mouseX, int mouseY) {
        RarityTheme theme = themeFor(entry.rarityId());
        List<Component> lines = buildDetailLines(entry);

        // Let the inspect panel grow to the available panel width. This keeps long perk
        // descriptions readable and avoids the old fitText(...)+ellipsis data loss.
        int contentMaxW = Math.max(220, this.font.width(entry.itemName()) + 16);
        for (Component line : lines) {
            contentMaxW = Math.max(contentMaxW, this.font.width(line) + 22);
        }
        int width = Math.min(PANEL_WIDTH - 12, Math.max(250, contentMaxW + 4));
        int textWidth = width - 24;
        List<List<String>> wrappedLines = new ArrayList<>(lines.size());
        int visualLineCount = 0;
        for (Component line : lines) {
            List<String> fragments = wrapTooltipText(line.getString(), textWidth);
            wrappedLines.add(fragments);
            visualLineCount += fragments.size();
        }

        int lineHeight = 13;
        int maxBodyHeight = PANEL_HEIGHT - 58;
        if (visualLineCount * lineHeight > maxBodyHeight) {
            // Keep all entries visible even for Transcendent items with long translated perks.
            lineHeight = Math.max(9, maxBodyHeight / Math.max(1, visualLineCount));
        }
        int height = 46 + visualLineCount * lineHeight;

        int x = mouseX + 14;
        int y = mouseY + 14;
        if (x + width > PANEL_WIDTH - 6) x = mouseX - width - 14;
        if (y + height > PANEL_HEIGHT - 6) y = PANEL_HEIGHT - height - 6;
        x = Math.max(6, Math.min(x, PANEL_WIDTH - width - 6));
        y = Math.max(6, Math.min(y, PANEL_HEIGHT - height - 6));

        graphics.nextStratum();
        graphics.fill(x - 2, y - 2, x + width + 4, y + height + 4, 0xA0020306);
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, theme.deep());
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, theme.primary());
        graphics.fill(x, y, x + width, y + height, 0xF70A0D15);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 32, theme.badgeBg());
        drawCornerBrackets(graphics, x + 1, y + 1, width - 2, height - 2, theme.primary(), theme.bright(), 5);
        graphics.text(this.font, Component.literal(fitText(entry.itemName(), width - 14)),
                x + 8, y + 6, TEXT, true);
        Component raritySub = Component.literal(theme.tierStars() + " ").append(rarityName(entry.rarityId()));
        graphics.text(this.font, raritySub, x + 8, y + 19, theme.bright(), true);
        graphics.fill(x + 6, y + 34, x + width - 6, y + 35, withAlpha(theme.primary(), 170));

        int statCount = countPrimaryStats(entry);
        int lineY = y + 40;
        for (int i = 0; i < lines.size(); i++) {
            boolean isStat = i < statCount;
            boolean maxRollStat = isStat && isMaxRollStatLine(entry, i);
            boolean goldenPerk = !isStat && isGoldenPerkLine(entry, i);
            int bulletColor = maxRollStat || goldenPerk ? GOLD_BRIGHT
                    : (isStat ? STAT_GREEN : PERK_CYAN);
            int textColor = maxRollStat || goldenPerk ? GOLD_BRIGHT
                    : (isStat ? 0xFFE5F7EC : 0xFFD8F6FF);
            List<String> fragments = wrappedLines.get(i);
            for (int part = 0; part < fragments.size(); part++) {
                String fragment = fragments.get(part);
                if (part == 0) {
                    graphics.text(this.font, Component.literal(isStat ? "◆" : "✦"),
                            x + 8, lineY, bulletColor, false);
                }
                if (isStat && fragments.size() == 1) {
                    int rangeStart = fragment.lastIndexOf(" [");
                    if (rangeStart >= 0) {
                        String rolledText = fragment.substring(0, rangeStart);
                        String rangeText = fragment.substring(rangeStart + 1);
                        int availableWidth = width - 24;
                        int rangeWidth = this.font.width(rangeText);
                        String fittedRolled = fitText(rolledText, Math.max(1, availableWidth - rangeWidth));
                        graphics.text(this.font, Component.literal(fittedRolled), x + 18, lineY, textColor, false);
                        int rangeX = x + 18 + this.font.width(fittedRolled);
                        if (rangeX + rangeWidth <= x + 18 + availableWidth) {
                            graphics.text(this.font, Component.literal(rangeText), rangeX, lineY,
                                    STAT_RANGE_GRAY, false);
                        }
                    } else {
                        graphics.text(this.font, Component.literal(fragment), x + 18, lineY, textColor, false);
                    }
                } else {
                    // Wrapped continuation lines are indented and never discarded.
                    graphics.text(this.font, Component.literal(fragment), x + 18, lineY, textColor, false);
                }
                lineY += lineHeight;
            }
        }
    }

    private List<String> wrapTooltipText(String text, int maxWidth) {
        if (text == null || text.isBlank()) return List.of("");
        List<String> wrapped = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (this.font.width(candidate) <= maxWidth) {
                if (current.length() > 0) current.append(' ');
                current.append(word);
                continue;
            }
            if (current.length() > 0) {
                wrapped.add(current.toString());
                current.setLength(0);
            }
            if (this.font.width(word) <= maxWidth) {
                current.append(word);
                continue;
            }
            StringBuilder piece = new StringBuilder();
            for (int offset = 0; offset < word.length(); offset++) {
                char character = word.charAt(offset);
                if (piece.length() > 0 && this.font.width(piece.toString() + character) > maxWidth) {
                    wrapped.add(piece.toString());
                    piece.setLength(0);
                }
                piece.append(character);
            }
            current.append(piece);
        }
        if (current.length() > 0) wrapped.add(current.toString());
        return wrapped.isEmpty() ? List.of("") : List.copyOf(wrapped);
    }

    private record StatChip(String label, int bgColor, int accentColor, int textColor) {}

    private List<StatChip> buildStatChips(Entry entry) {
        List<StatChip> chips = new ArrayList<>();
        double dmg = doubleStat(entry.details(), "damage");
        int dur = intStat(entry.details(), "durability");
        double breakSpeed = doubleStat(entry.details(), "breakSpeed");
        double crit = doubleStat(entry.details(), "critical");
        double attackSpeed = doubleStat(entry.details(), "attackSpeed");
        double armor = doubleStat(entry.details(), "armor");
        double toughness = doubleStat(entry.details(), "toughness");
        if (dmg > 0.0) {
            chips.add(new StatChip("+" + formatPercent(dmg) + "% ATK", 0x66163826, STAT_GREEN, 0xFFC8F7D8));
        }
        if (breakSpeed > 0.0) {
            chips.add(new StatChip("+" + formatPercent(breakSpeed) + "% BREAK SPD", 0x66163826, STAT_GREEN, 0xFFC8F7D8));
        }
        if (attackSpeed > 0.0) {
            chips.add(new StatChip("+" + formatPercent(attackSpeed) + "% ATK SPD", 0x66163826, STAT_GREEN, 0xFFC8F7D8));
        }
        if (crit > 0.0) {
            chips.add(new StatChip("+" + formatPercent(crit) + "% CRIT", 0x66163826, STAT_GREEN, 0xFFC8F7D8));
        }
        if (armor > 0.0) {
            chips.add(new StatChip("+" + formatPercent(armor) + "% ARMOR", 0x66163826, STAT_GREEN, 0xFFC8F7D8));
        }
        if (toughness > 0.0) {
            chips.add(new StatChip("+" + formatPercent(toughness) + "% TOUGH", 0x66163826, STAT_GREEN, 0xFFC8F7D8));
        }
        if (dur > 0) {
            chips.add(new StatChip("+" + dur + "% DUR", 0x66163826, STAT_GREEN, 0xFFC8F7D8));
        }
        if (chips.isEmpty()) {
            int perks = countPerks(entry);
            if (perks > 0) {
                chips.add(new StatChip(perks + (perks == 1 ? " PERK" : " PERKS"), 0x661B2238, PERK_CYAN, 0xFFD6F5FF));
            }
        }
        return chips;
    }

    /** Identifies all signature Transcendent affixes so they receive a gold accent. */
    private static boolean isGoldenPerkLine(Entry entry, int detailLineIndex) {
        int perkIndex = detailLineIndex - countPrimaryStats(entry);
        if (perkIndex < 0 || !entry.details().has("perks")
                || !entry.details().get("perks").isJsonArray()) return false;

        JsonArray perks = entry.details().getAsJsonArray("perks");
        int visiblePerkIndex = 0;
        for (int i = 0; i < perks.size(); i++) {
            if (!perks.get(i).isJsonObject()) continue;
            String id = stringValue(perks.get(i).getAsJsonObject(), "id", "");
            if (id.isBlank()) continue;
            if (visiblePerkIndex++ == perkIndex) {
                return GOLDEN_PERKS.contains(id);
            }
        }
        return false;
    }

    private List<Component> buildDetailLines(Entry entry) {
        List<Component> lines = new ArrayList<>();
        for (String field : getOrderedStatFields(entry.details())) {
            addOrderedStat(lines, entry.details(), field);
        }
        JsonArray perks = entry.details().has("perks") && entry.details().get("perks").isJsonArray()
                ? entry.details().getAsJsonArray("perks") : new JsonArray();
        for (int i = 0; i < perks.size(); i++) {
            if (!perks.get(i).isJsonObject()) continue;
            JsonObject perk = perks.get(i).getAsJsonObject();
            String id = stringValue(perk, "id", "");
            if (id.isBlank()) continue;
            int level = 1;
            try { level = Math.max(1, perk.get("level").getAsInt()); } catch (RuntimeException ignored) {}
            String key = "item.simpleskills.crafting_perk." + id;
            if (CraftingEquipmentManager.getAdditionalJumpsForPerk(id, level) > 0) {
                lines.add(Component.translatable(key,
                        Integer.toString(CraftingEquipmentManager.getAdditionalJumpsForPerk(id, level))));
            } else if (id.equals("vitality")) {
                lines.add(Component.translatable(key, roman(level), level * 4));
            } else if (id.equals("resilient")) {
                lines.add(Component.translatable(key, roman(level), level * 2));
            } else if (id.equals("eternal_guardian")) {
                int amplifier = level - 1;
                lines.add(Component.translatable(key, 10 + amplifier * 5,
                        roman(Math.min(5, 3 + amplifier)), roman(Math.min(5, 2 + amplifier))));
            } else if (id.equals("aegis_of_ages")) {
                int amplifier = level - 1;
                lines.add(Component.translatable(key, roman(level),
                        roman(Math.min(5, 3 + amplifier)), roman(Math.min(4, 2 + amplifier))));
            } else {
                lines.add(NO_LEVEL_TEXT.contains(id) ? Component.translatable(key)
                        : Component.translatable(key, roman(level)));
            }
        }
        if (lines.isEmpty()) lines.add(Component.translatable("gui.simpleskills.crafting_reveal.no_extra_stats"));
        return lines;
    }

    /** Returns the persisted crafting order, with a canonical fallback for old reveal packets. */
    private static List<String> getOrderedStatFields(JsonObject details) {
        List<String> fallback = List.of("damage", "durability", "breakSpeed", "excavationTime", "critical",
                "attackSpeed", "armor", "toughness", "armorPenetration", "combatFrenzy", "firstStrike",
                "rhythmicDischarge", "tenacity", "mobility", "attackReach", "elementalResistance");
        List<String> ordered = new ArrayList<>();
        if (details.has("attributeOrder") && details.get("attributeOrder").isJsonArray()) {
            JsonArray stored = details.getAsJsonArray("attributeOrder");
            for (int i = 0; i < stored.size(); i++) {
                if (!stored.get(i).isJsonPrimitive()) continue;
                String field;
                try { field = stored.get(i).getAsString(); } catch (RuntimeException ignored) { continue; }
                if (fallback.contains(field) && doubleStat(details, field) > 0.0 && !ordered.contains(field)) {
                    ordered.add(field);
                }
            }
        }
        // Older reveal payloads do not carry attributeOrder; append any still-present field
        // using the historical order so old batches remain readable.
        for (String field : fallback) {
            if (doubleStat(details, field) > 0.0 && !ordered.contains(field)) ordered.add(field);
        }
        return List.copyOf(ordered);
    }

    private static void addOrderedStat(List<Component> lines, JsonObject details, String field) {
        switch (field) {
            case "damage" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.damage");
            case "durability" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.durability");
            case "breakSpeed" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.break_speed");
            case "excavationTime" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.excavation_time");
            case "critical" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.critical_chance");
            case "attackSpeed" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.attack_speed");
            case "armor" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.armor");
            case "toughness" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.armor_toughness");
            case "armorPenetration" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.armor_penetration");
            case "combatFrenzy" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.combat_frenzy");
            case "firstStrike" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.first_strike");
            case "rhythmicDischarge" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.rhythmic_discharge");
            case "tenacity" -> addStat(lines, details, field, "item.simpleskills.crafting_stat.tenacity");
            case "mobility" -> addStat(lines, details, field,
                    "item.simpleskills.crafting_stat.mobility_" + stringValue(details, "mobilityType", "walk"));
            case "attackReach" -> addBlockStat(lines, details, field, "item.simpleskills.crafting_stat.attack_reach");
            case "elementalResistance" -> addStat(lines, details, field,
                    "item.simpleskills.crafting_stat.elemental_resistance_" + stringValue(details, "elementalType", "fire"));
            default -> { }
        }
    }

    private static void addStat(List<Component> lines, JsonObject details, String field, String key) {
        double value = doubleStat(details, field);
        if (value <= 0.0) return;

        // Older reveal packets may not contain ranges; use the rolled value as a
        // fallback rather than allowing raw format placeholders to appear on screen.
        double minimum = doubleStat(details, field + "Min", value);
        double maximum = doubleStat(details, field + "Max", value);
        // Labels are translated separately from values to avoid fragile printf-style
        // placeholders in language files and to let the tooltip color the range on its own.
        String label = Component.translatable(key).getString();
        String text = label + ": +" + formatPercent(value) + "% ["
                + formatPercent(minimum) + "% - " + formatPercent(maximum) + "%]";
        lines.add(Component.literal(text));
    }

    private static void addBlockStat(List<Component> lines, JsonObject details, String field, String key) {
        double value = doubleStat(details, field);
        if (value <= 0.0) return;
        double minimum = doubleStat(details, field + "Min", value);
        double maximum = doubleStat(details, field + "Max", value);
        String label = Component.translatable(key).getString();
        lines.add(Component.literal(label + ": +" + formatPercent(value) + " blocks ["
                + formatPercent(minimum) + " - " + formatPercent(maximum) + " blocks]"));
    }

    private static boolean isMaxRollStatLine(Entry entry, int statLineIndex) {
        List<String> fields = getOrderedStatFields(entry.details());
        if (statLineIndex < 0 || statLineIndex >= fields.size()) return false;
        return isMaxRoll(entry.details(), fields.get(statLineIndex));
    }

    private static boolean isMaxRoll(JsonObject details, String field) {
        if (!details.has(field + "Max")) return false;
        double value = doubleStat(details, field);
        double maximum = doubleStat(details, field + "Max");
        double step = switch (field) {
            case "durability", "breakSpeed", "excavationTime" -> 1.0;
            case "attackReach" -> 0.05;
            case "attackSpeed" -> 0.5;
            default -> 0.1;
        };
        return value > 0.0 && Math.abs(value - maximum) <= Math.max(0.0001, step * 0.51);
    }

    private static double doubleStat(JsonObject details, String field) {
        return doubleStat(details, field, 0.0);
    }

    private static double doubleStat(JsonObject details, String field, double fallback) {
        try {
            return details.has(field) ? details.get(field).getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String formatPercent(double percentage) {
        double nearestWhole = Math.rint(percentage);
        return Math.abs(percentage - nearestWhole) < 0.05
                ? String.format(Locale.ROOT, "%.0f", nearestWhole)
                : String.format(Locale.ROOT, "%.1f", percentage);
    }

    private static int intStat(JsonObject details, String field) {
        try {
            return details.has(field) ? details.get(field).getAsInt() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static int countPrimaryStats(Entry entry) {
        int count = 0;
        if (doubleStat(entry.details(), "damage") > 0.0) count++;
        if (doubleStat(entry.details(), "durability") > 0.0) count++;
        if (doubleStat(entry.details(), "breakSpeed") > 0.0) count++;
        if (doubleStat(entry.details(), "excavationTime") > 0.0) count++;
        if (doubleStat(entry.details(), "critical") > 0.0) count++;
        if (doubleStat(entry.details(), "attackSpeed") > 0.0) count++;
        if (doubleStat(entry.details(), "armor") > 0.0) count++;
        if (doubleStat(entry.details(), "toughness") > 0.0) count++;
        if (doubleStat(entry.details(), "armorPenetration") > 0.0) count++;
        if (doubleStat(entry.details(), "combatFrenzy") > 0.0) count++;
        if (doubleStat(entry.details(), "firstStrike") > 0.0) count++;
        if (doubleStat(entry.details(), "rhythmicDischarge") > 0.0) count++;
        if (doubleStat(entry.details(), "tenacity") > 0.0) count++;
        if (doubleStat(entry.details(), "mobility") > 0.0) count++;
        if (doubleStat(entry.details(), "attackReach") > 0.0) count++;
        if (doubleStat(entry.details(), "elementalResistance") > 0.0) count++;
        return count;
    }

    private static int countPerks(Entry entry) {
        if (!entry.details().has("perks") || !entry.details().get("perks").isJsonArray()) return 0;
        return entry.details().getAsJsonArray("perks").size();
    }

    private void drawNavigation(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int pages = pageCount();
        int centerX = PANEL_WIDTH / 2;
        int navY = navButtonY();

        if (pages > 1) {
            // Central integrated page plaque
            int plaqueW = 92;
            int plaqueX = centerX - plaqueW / 2;
            graphics.fill(plaqueX, navY, plaqueX + plaqueW, navY + NAV_BTN_H, 0xFF0E121B);
            drawOutline(graphics, plaqueX, navY, plaqueW, NAV_BTN_H, GOLD_DARK);

            centered(graphics, Component.translatable("gui.simpleskills.crafting_reveal.page", currentPage + 1, pages),
                    navY + 5, TEXT);

            // Page indicator pips along bottom of plaque
            int maxPips = Math.min(pages, 12);
            int pipTotalW = maxPips * 5 - 2;
            int pipStartX = centerX - pipTotalW / 2;
            int activePip = Math.min(maxPips - 1, (currentPage * maxPips) / pages);
            for (int i = 0; i < maxPips; i++) {
                int px = pipStartX + i * 5;
                graphics.fill(px, navY + NAV_BTN_H - 3, px + 3, navY + NAV_BTN_H - 2,
                        i == activePip ? GOLD_BRIGHT : 0xFF394256);
            }

            // Previous Button (‹)
            int prevX = prevButtonX();
            boolean prevActive = currentPage > 0;
            boolean prevHover = prevActive && inside(mouseX, mouseY, prevX, navY, NAV_BTN_W, NAV_BTN_H);
            drawOrnateNavButton(graphics, prevX, navY, NAV_BTN_W, NAV_BTN_H, "‹", prevActive, prevHover);

            // Next Button (›)
            int nextX = nextButtonX();
            boolean nextActive = currentPage < pages - 1;
            boolean nextHover = nextActive && inside(mouseX, mouseY, nextX, navY, NAV_BTN_W, NAV_BTN_H);
            drawOrnateNavButton(graphics, nextX, navY, NAV_BTN_W, NAV_BTN_H, "›", nextActive, nextHover);
        } else {
            int badgeW = 54;
            int bx = centerX - badgeW / 2;
            graphics.fill(bx, navY + 2, bx + badgeW, navY + 17, 0xAA0E121B);
            drawOutline(graphics, bx, navY + 2, badgeW, 15, 0xFF2B3447);
            centered(graphics, Component.translatable("gui.simpleskills.crafting_reveal.single_page"),
                    navY + 6, MUTED);
        }
    }

    private void drawOrnateNavButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                     String glyph, boolean active, boolean hovered) {
        int border = !active ? 0xFF283040 : (hovered ? GOLD_BRIGHT : GOLD_DARK);
        int bg = !active ? 0xFF0D1018 : (hovered ? 0xFF2B2316 : 0xFF151A26);
        int textColor = !active ? DIM : (hovered ? GOLD_BRIGHT : GOLD);

        graphics.fill(x, y, x + w, y + h, bg);
        drawOutline(graphics, x, y, w, h, border);
        if (active) {
            drawCornerBrackets(graphics, x + 1, y + 1, w - 2, h - 2,
                    hovered ? GOLD_BRIGHT : GOLD_DARK, GOLD_BRIGHT, 2);
        }
        centeredIn(graphics, Component.literal(glyph), x, y + 6, w, textColor);
    }

    private void drawCloseHint(GuiGraphicsExtractor graphics) {
        centered(graphics, Component.translatable("gui.simpleskills.crafting_reveal.close_hint"),
                PANEL_HEIGHT - 16, MUTED);
    }

    private void drawRevealBurst(GuiGraphicsExtractor graphics, int centerX, int centerY,
                                 String rarity, float partialTick, int remainingTicks) {
        RarityTheme theme = themeFor(rarity);
        int age = REVEAL_FX_DURATION - remainingTicks;
        double progress = Math.min(1.0, (age + partialTick) / REVEAL_FX_DURATION);
        double ease = 1.0 - Math.pow(1.0 - progress, 2.2);

        // Expanding shockwave diamond ring
        int ringRadius = (int) Math.round(6 + ease * (rarity.equals("transcendent") ? 34 : 25));
        int ringAlpha = (int) Math.round((1.0 - progress) * 190);
        if (ringAlpha > 8) {
            int ringColor = withAlpha(theme.bright(), ringAlpha);
            for (int s = 0; s < 24; s++) {
                double a = (Math.PI * 2.0 * s) / 24.0;
                int rx = centerX + (int) Math.round(Math.cos(a) * ringRadius);
                int ry = centerY + (int) Math.round(Math.sin(a) * ringRadius * 0.72);
                graphics.fill(rx, ry, rx + 2, ry + 2, ringColor);
            }
        }

        int count = switch (rarity) {
            case "unique" -> 12;
            case "epic" -> 18;
            case "legendary" -> 24;
            case "mythic" -> 30;
            case "transcendent" -> 40;
            default -> 10;
        };
        double baseRadius = 7.0 + ease * (rarity.equals("transcendent") ? 44.0 : 32.0);
        double rotation = progress * (rarity.equals("mythic") ? -2.6 : 2.0);
        int particleAlpha = (int) Math.round(Math.min(1.0, (1.0 - progress) * 1.45) * 255);

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i / count) + rotation;
            double radius = baseRadius * (rarity.equals("epic") ? (0.68 + 0.32 * ((i % 3) + 1) / 3.0) : 1.0);
            if (rarity.equals("transcendent") && i % 2 == 0) radius *= 0.60;
            if (rarity.equals("legendary") && i % 4 == 0) radius *= 1.24;
            int px = centerX + (int) Math.round(Math.cos(angle) * radius);
            int py = centerY + (int) Math.round(Math.sin(angle) * radius * 0.64);
            int size = rarity.equals("transcendent") ? 4 : (rarity.equals("mythic") ? 3 : 2);
            int baseColor = (i % 3 == 0) ? 0xFFFFFFFF : theme.bright();
            int particleColor = withAlpha(baseColor, particleAlpha);
            graphics.fill(px - size / 2, py - size / 2, px + size / 2 + 1, py + size / 2 + 1, particleColor);
        }

        if (theme.highTier()) {
            int ray = (int) Math.round(8 + ease * (rarity.equals("transcendent") ? 26 : 20));
            int rayAlpha = (int) Math.round((1.0 - progress) * 210);
            int rayColor = withAlpha(theme.bright(), rayAlpha);
            graphics.fill(centerX - ray, centerY, centerX + ray, centerY + 1, rayColor);
            graphics.fill(centerX, centerY - ray, centerX + 1, centerY + ray, rayColor);
            if (rarity.equals("transcendent")) {
                int diag = (int) Math.round(ray * 0.65);
                for (int d = -diag; d <= diag; d += 2) {
                    graphics.fill(centerX + d, centerY + d, centerX + d + 1, centerY + d + 1, withAlpha(0xFFFFFFFF, rayAlpha));
                    graphics.fill(centerX + d, centerY - d, centerX + d + 1, centerY - d + 1, withAlpha(0xFFFFFFFF, rayAlpha));
                }
            }
        }
    }

    private static float revealPopScale(int fxTicks, float partialTick) {
        if (fxTicks <= 0) return 1.0F;
        float age = (REVEAL_FX_DURATION - fxTicks) + partialTick;
        if (age >= 8.0F) return 1.0F;
        float t = age / 8.0F;
        return 1.0F + (float) Math.sin(t * Math.PI) * 0.24F;
    }

    private void playRaritySound(String rarity) {
        var sound = switch (rarity) {
            case "unique" -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case "epic" -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
            case "legendary" -> SoundEvents.PLAYER_LEVELUP;
            case "mythic" -> SoundEvents.BEACON_ACTIVATE;
            case "transcendent" -> SoundEvents.TOTEM_USE;
            default -> SoundEvents.ITEM_PICKUP;
        };
        float pitch = switch (rarity) {
            case "unique" -> 1.35F;
            case "epic" -> 1.15F;
            case "legendary" -> 1.28F;
            case "mythic" -> 0.9F;
            case "transcendent" -> 1.0F;
            default -> 1.0F;
        };
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    /**
     * Converts absolute GUI mouse coordinates into the panel's design coordinate space.
     * This mirrors the exact translation/scale sequence used by extractBackground so
     * the visible buttons and their hitboxes stay aligned at every GUI size and during
     * the opening animation.
     */
    private PanelMouse toPanelMouse(double screenX, double screenY, float partialTick) {
        float openRaw = Math.min(1.0F, (screenTicks + partialTick) / OPEN_ANIM_TICKS);
        float openEase = 1.0F - (float) Math.pow(1.0F - openRaw, 3.0);
        double animScale = 0.94F + 0.06F * openEase;
        double slideY = (1.0F - openEase) * 8.0F;

        double scaledX = (screenX - panelScreenLeft) / uiScale;
        double scaledY = (screenY - panelScreenTop - slideY) / uiScale;
        double panelX = (scaledX - PANEL_WIDTH / 2.0) / animScale + PANEL_WIDTH / 2.0;
        double panelY = (scaledY - PANEL_HEIGHT / 2.0) / animScale + PANEL_HEIGHT / 2.0;
        return new PanelMouse(panelX, panelY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Minecraft 26.3 uses SDL3 mouse-button values: LEFT = 1 (not the old GLFW value 0).
        if (event.button() == 1) {
            PanelMouse panelMouse = toPanelMouse(event.x(), event.y(), 0.0F);
            double localX = panelMouse.x();
            double localY = panelMouse.y();

            // Slightly generous hit areas make the small ornamental arrows easier to click
            // without overlapping the central page plaque.
            if (inside(localX, localY, closeButtonX() - 2, closeButtonY() - 2,
                    CLOSE_BTN_W + 4, CLOSE_BTN_H + 4)) {
                playUiClick();
                onClose();
                return true;
            }

            if (pageCount() > 1) {
                int navY = navButtonY();
                if (currentPage > 0 && inside(localX, localY, prevButtonX() - 3, navY - 3,
                        NAV_BTN_W + 6, NAV_BTN_H + 6)) {
                    playUiClick();
                    changePage(-1);
                    return true;
                }
                if (currentPage < pageCount() - 1 && inside(localX, localY, nextButtonX() - 3, navY - 3,
                        NAV_BTN_W + 6, NAV_BTN_H + 6)) {
                    playUiClick();
                    changePage(1);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static void playUiClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static int closeButtonX() { return PANEL_WIDTH - 31; }
    private static int closeButtonY() { return 10; }
    private static int navButtonY() { return PANEL_HEIGHT - 39; }
    private static int prevButtonX() { return PANEL_WIDTH / 2 - 76; }
    private static int nextButtonX() { return PANEL_WIDTH / 2 + 50; }

    private int pageCount() {
        return Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private static RarityTheme themeFor(String rarity) {
        return switch (rarity) {
            case "rare" -> THEME_RARE;
            case "unique" -> THEME_UNIQUE;
            case "epic" -> THEME_EPIC;
            case "legendary" -> THEME_LEGENDARY;
            case "mythic" -> THEME_MYTHIC;
            case "transcendent" -> THEME_TRANSCENDENT;
            default -> THEME_RARE;
        };
    }

    private static int rarityRank(String rarity) {
        return switch (rarity) {
            case "rare" -> 1;
            case "unique" -> 2;
            case "epic" -> 3;
            case "legendary" -> 4;
            case "mythic" -> 5;
            case "transcendent" -> 6;
            default -> 0;
        };
    }

    private static Component rarityName(String rarity) {
        return Component.translatable("item.simpleskills.crafting_rarity." + rarity);
    }

    private String fitText(String raw, int maxWidth) {
        if (raw == null || raw.isEmpty() || maxWidth <= 6) return "";
        if (this.font.width(raw) <= maxWidth) return raw;
        int ellipsisW = this.font.width("…");
        if (maxWidth <= ellipsisW) return this.font.plainSubstrByWidth(raw, maxWidth);
        return this.font.plainSubstrByWidth(raw, maxWidth - ellipsisW) + "…";
    }

    private void centered(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(this.font, text, PANEL_WIDTH / 2 - this.font.width(text) / 2, y, color, true);
    }

    private void centeredIn(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        graphics.text(this.font, text, x + width / 2 - this.font.width(text) / 2, y, color, true);
    }

    private static void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void drawBevelBox(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int topLeft, int bottomRight) {
        graphics.fill(x, y, x + w, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + h, topLeft);
        graphics.fill(x, y + h - 1, x + w, y + h, bottomRight);
        graphics.fill(x + w - 1, y, x + w, y + h, bottomRight);
    }

    private static void drawCornerBrackets(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                             int armColor, int tipColor, int len) {
        int r = x + w;
        int b = y + h;
        // Top-left
        graphics.fill(x, y, x + len, y + 1, armColor);
        graphics.fill(x, y, x + 1, y + len, armColor);
        graphics.fill(x, y, x + 1, y + 1, tipColor);
        // Top-right
        graphics.fill(r - len, y, r, y + 1, armColor);
        graphics.fill(r - 1, y, r, y + len, armColor);
        graphics.fill(r - 1, y, r, y + 1, tipColor);
        // Bottom-left
        graphics.fill(x, b - 1, x + len, b, armColor);
        graphics.fill(x, b - len, x + 1, b, armColor);
        graphics.fill(x, b - 1, x + 1, b, tipColor);
        // Bottom-right
        graphics.fill(r - len, b - 1, r, b, armColor);
        graphics.fill(r - 1, b - len, r, b, armColor);
        graphics.fill(r - 1, b - 1, r, b, tipColor);
    }

    private static void drawDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int span = radius - Math.abs(dy);
            graphics.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, color);
        }
    }

    private static int withAlpha(int argb, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (argb & 0x00FFFFFF) | (clamped << 24);
    }

    private static int lerpColor(int c1, int c2, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * clamped);
        int r = Math.round(r1 + (r2 - r1) * clamped);
        int g = Math.round(g1 + (g2 - g1) * clamped);
        int b = Math.round(b1 + (b2 - b1) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> Integer.toString(level);
        };
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (activeScreen == this) activeScreen = null;
        super.onClose();
    }

    @Override
    public void removed() {
        if (activeScreen == this) activeScreen = null;
        super.removed();
    }
}
