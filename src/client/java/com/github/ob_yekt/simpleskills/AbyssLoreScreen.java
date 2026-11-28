package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.ui.AbyssLoreProgressDataPayload;
import com.github.ob_yekt.simpleskills.ui.AbyssLoreProgressRequestPayload;
import com.github.ob_yekt.simpleskills.ui.SkillOverviewRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The eight-spread Vhal'Kareth chronicle, unlocked by the player's combined profession levels. */
public final class AbyssLoreScreen extends Screen {
    private static final int WIDTH = 628;
    private static final int HEIGHT = 336;
    private static final int GOLD = 0xFFE9CF7A;
    private static final int GOLD_BRIGHT = 0xFFFFE89E;
    private static final int GOLD_DARK = 0xFF7B5D27;
    private static final int LEATHER_OUTER = 0xFF140E0A;
    private static final int LEATHER_MAIN = 0xFF281C13;
    private static final int LEATHER_INNER = 0xFF1E150E;
    private static final int PAPER = 0xFFE6D3AD;
    private static final int PAPER_UNDER = 0xFFD6BF92;
    private static final int PAPER_EDGE = 0xFF8F7247;
    private static final int INK = 0xFF3E301F;
    private static final int MUTED_INK = 0xFF756043;
    private static final int RIBBON = 0xFF8C1D24;
    private static final int RIBBON_SHADE = 0xFF561015;
    private static final int PAGE_COUNT = 8;
    private static final int[] UNLOCK_LEVELS = {15, 30, 45, 65, 85, 110, 140, 170};
    private static final String[] ROMAN_NUMERALS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
    private static final String[] ILLUSTRATIONS = {
            "discovery", "city", "crimson_gift", "rupture", "last_king", "bridge", "castle", "colossus"
    };

    private static AbyssLoreScreen active;
    private int totalLevel = 15;
    private int bridgeX = AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
    private int bridgeY = AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
    private int bridgeZ = AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
    private int castleX = AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
    private int castleY = AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
    private int castleZ = AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
    private int colossusX = AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
    private int colossusZ = AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
    private int currentPage;
    private int refreshTicks;
    private int left;
    private int top;
    private boolean previousCornerHovered;
    private boolean nextCornerHovered;
    private float previousFoldProgress;
    private float nextFoldProgress;

    // Animated page-flip sweep state (-1 = turning back, +1 = turning forward, 0 = idle)
    private int turnDirection;
    private float turnProgress;

    private AbyssLoreScreen(AbyssLoreProgressDataPayload payload) {
        super(Component.empty());
        accept(payload);
    }

    public static void show(AbyssLoreProgressDataPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (active != null && minecraft.gui.screen() == active) {
            active.accept(payload);
            return;
        }
        // The lore request is only allowed to replace the K menu that initiated it.
        if (minecraft.gui.screen() instanceof SkillOverviewScreen) {
            active = new AbyssLoreScreen(payload);
            minecraft.gui.setScreen(active);
        }
    }

    public static boolean isOpen() {
        return active != null && Minecraft.getInstance().gui.screen() == active;
    }

    private void accept(AbyssLoreProgressDataPayload payload) {
        totalLevel = Math.max(0, payload.totalLevel());
        bridgeX = payload.bridgeX();
        bridgeY = payload.bridgeY();
        bridgeZ = payload.bridgeZ();
        castleX = payload.castleX();
        castleY = payload.castleY();
        castleZ = payload.castleZ();
        boolean colossusLocationKnown = !payload.colossusRegionCenter()
                .equals(AbyssLoreProgressDataPayload.UNKNOWN_BLOCK_POS);
        colossusX = colossusLocationKnown ? payload.colossusRegionCenter().getX()
                : AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
        colossusZ = colossusLocationKnown ? payload.colossusRegionCenter().getZ()
                : AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
        currentPage = Math.min(currentPage, unlockedPageCount() - 1);
        currentPage = Math.max(0, currentPage);
    }

    private int unlockedPageCount() {
        int count = 0;
        for (int required : UNLOCK_LEVELS) if (totalLevel >= required) count++;
        return Math.max(1, count);
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    public void tick() {
        super.tick();
        previousFoldProgress = approach(previousFoldProgress, previousCornerHovered ? 1.0f : 0.0f, 0.30f);
        nextFoldProgress = approach(nextFoldProgress, nextCornerHovered ? 1.0f : 0.0f, 0.30f);

        if (turnDirection != 0) {
            turnProgress += 0.24f;
            if (turnProgress >= 1.0f) {
                turnProgress = 0.0f;
                turnDirection = 0;
            }
        }

        if (++refreshTicks >= 100) {
            refreshTicks = 0;
            if (ClientPlayNetworking.canSend(AbyssLoreProgressRequestPayload.TYPE)) {
                ClientPlayNetworking.send(new AbyssLoreProgressRequestPayload(AbyssLoreProgressRequestPayload.REFRESH));
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;

        // Header banner floating slightly above the open tome
        g.centeredText(font, loreText("title"), left + WIDTH / 2, top + 4, GOLD);
        g.centeredText(font, loreText("subtitle"), left + WIDTH / 2, top + 16, 0xFFB7A88D);
        g.text(font, loreText("general_level", totalLevel), left + 16, top + 28, GOLD, false);

        String next = unlockedPageCount() >= PAGE_COUNT
                ? SkillOverviewText.get("gui.simpleskills.lore.all_unlocked")
                : SkillOverviewText.format("gui.simpleskills.lore.next_unlock", UNLOCK_LEVELS[unlockedPageCount()]);
        int nextWidth = font.width(next);
        g.text(font, Component.literal(next), left + WIDTH - 16 - nextWidth, top + 28, 0xFFB7A88D, false);

        drawLeatherCover(g);
        drawOpenBook(g, mouseX, mouseY);
        drawNavigationTabs(g, mouseX, mouseY);
    }

    /** Renders the bound leather tome boards, spine caps, bookmark ribbon, and brass corner clasps. */
    private void drawLeatherCover(GuiGraphicsExtractor g) {
        int coverLeft = left + 4;
        int coverRight = left + WIDTH - 4;
        int coverTop = top + 42;
        int coverBottom = top + HEIGHT - 12;
        int midX = left + WIDTH / 2;

        // Drop shadow under the physical book
        g.fill(coverLeft + 4, coverTop + 5, coverRight + 6, coverBottom + 7, 0x88050403);
        g.fill(coverLeft + 2, coverTop + 3, coverRight + 4, coverBottom + 5, 0xAA080604);

        // Raised central leather spine hump protruding at top & bottom center
        g.fill(midX - 24, coverTop - 4, midX + 24, coverBottom + 4, LEATHER_OUTER);
        g.fill(midX - 22, coverTop - 3, midX + 22, coverBottom + 3, LEATHER_MAIN);
        g.fill(midX - 18, coverTop - 2, midX + 18, coverBottom + 2, 0xFF352519);
        g.fill(midX - 20, coverTop - 3, midX + 20, coverTop - 2, GOLD_DARK);
        g.fill(midX - 20, coverBottom + 2, midX + 20, coverBottom + 3, GOLD_DARK);

        // Main left & right leather cover boards
        g.fill(coverLeft, coverTop, coverRight, coverBottom, LEATHER_OUTER);
        g.fill(coverLeft + 2, coverTop + 2, coverRight - 2, coverBottom - 2, LEATHER_MAIN);
        g.fill(coverLeft + 5, coverTop + 5, coverRight - 5, coverBottom - 5, LEATHER_INNER);

        // Subtle blind-tooled gold border around the leather cover
        drawRectOutline(g, coverLeft + 4, coverTop + 4, coverRight - 4, coverBottom - 4, GOLD_DARK);

        // Ornate brass/gold corner brackets on all four corners of the leather cover
        drawCornerClasp(g, coverLeft + 1, coverTop + 1, true, true);
        drawCornerClasp(g, coverRight - 1, coverTop + 1, false, true);
        drawCornerClasp(g, coverLeft + 1, coverBottom - 1, true, false);
        drawCornerClasp(g, coverRight - 1, coverBottom - 1, false, false);

        // Crimson bookmark ribbon hanging below the central spine
        int ribbonX = midX - 5;
        int ribbonTop = coverBottom + 1;
        int ribbonBottom = coverBottom + 14;
        g.fill(ribbonX - 1, ribbonTop, ribbonX + 9, ribbonBottom, 0xAA090605);
        g.fill(ribbonX, ribbonTop, ribbonX + 8, ribbonBottom - 2, RIBBON);
        g.fill(ribbonX + 1, ribbonTop, ribbonX + 3, ribbonBottom - 3, 0xFFB02831);
        g.fill(ribbonX + 6, ribbonTop, ribbonX + 8, ribbonBottom - 2, RIBBON_SHADE);
        // Forked V-cut ribbon tail
        g.fill(ribbonX, ribbonBottom - 2, ribbonX + 3, ribbonBottom, RIBBON);
        g.fill(ribbonX + 5, ribbonBottom - 2, ribbonX + 8, ribbonBottom, RIBBON_SHADE);
    }

    private void drawCornerClasp(GuiGraphicsExtractor g, int cornerX, int cornerY, boolean leftSide, boolean topSide) {
        int dirX = leftSide ? 1 : -1;
        int dirY = topSide ? 1 : -1;
        int x1 = Math.min(cornerX, cornerX + dirX * 14);
        int x2 = Math.max(cornerX, cornerX + dirX * 14);
        int y1 = Math.min(cornerY, cornerY + dirY * 14);
        int y2 = Math.max(cornerY, cornerY + dirY * 14);

        // Outer metal plate along the two edges
        g.fill(x1, topSide ? cornerY : cornerY - 3, x2, topSide ? cornerY + 3 : cornerY, GOLD_DARK);
        g.fill(leftSide ? cornerX : cornerX - 3, y1, leftSide ? cornerX + 3 : cornerX, y2, GOLD_DARK);
        g.fill(x1 + 1, topSide ? cornerY + 1 : cornerY - 2, x2 - 1, topSide ? cornerY + 2 : cornerY - 1, GOLD);
        g.fill(leftSide ? cornerX + 1 : cornerX - 2, y1 + 1, leftSide ? cornerX + 2 : cornerX - 1, y2 - 1, GOLD);

        // Corner rivet
        int rx = cornerX + dirX * 4;
        int ry = cornerY + dirY * 4;
        g.fill(Math.min(rx, rx + dirX * 2), Math.min(ry, ry + dirY * 2),
                Math.max(rx, rx + dirX * 2), Math.max(ry, ry + dirY * 2), GOLD_BRIGHT);
    }

    private void drawOpenBook(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int bookTop = top + 51;
        int bookBottom = top + HEIGHT - 23;
        int pageLeft = left + 18;
        int pageRight = left + WIDTH - 18;
        int pageMid = left + WIDTH / 2;

        // Dynamic thickness of stacked pages on left vs right side of the open book
        int leftStack = 2 + Math.min(4, currentPage);
        int rightStack = 2 + Math.min(4, Math.max(0, PAGE_COUNT - 1 - currentPage));

        // 1. Draw stacked page block edges (gives genuine 3D book thickness)
        for (int i = leftStack; i >= 1; i--) {
            int offset = i * 2;
            int sheetColor = (i % 2 == 0) ? 0xFFCDB588 : 0xFFB89E70;
            g.fill(pageLeft - offset, bookTop + i, pageMid, bookBottom + i, 0xFF6B5332);
            g.fill(pageLeft - offset + 1, bookTop + i, pageMid, bookBottom + i - 1, sheetColor);
        }
        for (int i = rightStack; i >= 1; i--) {
            int offset = i * 2;
            int sheetColor = (i % 2 == 0) ? 0xFFCDB588 : 0xFFB89E70;
            g.fill(pageMid, bookTop + i, pageRight + offset, bookBottom + i, 0xFF6B5332);
            g.fill(pageMid, bookTop + i, pageRight + offset - 1, bookBottom + i - 1, sheetColor);
        }

        // 2. Main open parchment spread
        g.fill(pageLeft - 1, bookTop - 1, pageRight + 1, bookBottom + 1, PAPER_EDGE);
        g.fill(pageLeft, bookTop, pageMid, bookBottom, PAPER);
        g.fill(pageMid, bookTop, pageRight, bookBottom, PAPER);

        // Subtle inner page margin border & warm paper highlight crest
        g.fill(pageLeft + 14, bookTop + 4, pageMid - 28, bookBottom - 4, 0x22FFF8E7);
        g.fill(pageMid + 28, bookTop + 4, pageRight - 14, bookBottom - 4, 0x22FFF8E7);
        drawRectOutline(g, pageLeft + 8, bookTop + 7, pageMid - 10, bookBottom - 9, 0x389B7E52);

        // 3. Curved central spine gutter shading (pages dipping into the binding)
        drawGutterShading(g, pageMid, bookTop, bookBottom);

        // 4. Top page Roman numerals & subtle footer page counter inside the parchment
        g.text(font, Component.literal(ROMAN_NUMERALS[currentPage]), pageLeft + 14, bookTop + 10, MUTED_INK, false);
        String spreadLabel = (currentPage + 1) + " / " + PAGE_COUNT;
        g.text(font, Component.literal(spreadLabel), pageRight - 14 - font.width(spreadLabel), bookTop + 10, MUTED_INK, false);

        // 5. Left page story content & right page framed illustration
        drawStoryPage(g, pageLeft + 16, bookTop + 23, pageMid - pageLeft - 32);
        drawIllustration(g, pageMid + 18, bookTop + 22,
                pageRight - pageMid - 36, bookBottom - bookTop - 52);

        // 6. Animated page sweep when turning pages
        if (turnDirection != 0) {
            drawPageTurnSweep(g, pageLeft, pageMid, pageRight, bookTop, bookBottom);
        }

        // 7. Realistic peeled & curled bottom page corners
        drawPageCorners(g, mouseX, mouseY, pageLeft, pageRight, bookTop, bookBottom);
    }

    /** Multi-band curved shading around the center crease so the pages look arched out of the spine. */
    private void drawGutterShading(GuiGraphicsExtractor g, int pageMid, int bookTop, int bookBottom) {
        // Soft curved shadow bands radiating from pageMid
        g.fill(pageMid - 22, bookTop, pageMid + 22, bookBottom, 0x0C5A4122);
        g.fill(pageMid - 15, bookTop, pageMid + 15, bookBottom, 0x165A4122);
        g.fill(pageMid - 10, bookTop, pageMid + 10, bookBottom, 0x24523A1D);
        g.fill(pageMid - 6, bookTop, pageMid + 6, bookBottom, 0x3A473016);
        g.fill(pageMid - 3, bookTop, pageMid + 3, bookBottom, 0x663B2610);
        // Deep center binding crease
        g.fill(pageMid - 1, bookTop, pageMid + 1, bookBottom, 0xFF5D4527);
        // Binding stitch marks along the center crease
        for (int stitchY = bookTop + 24; stitchY < bookBottom - 20; stitchY += 38) {
            g.fill(pageMid - 2, stitchY, pageMid + 2, stitchY + 6, 0xFF3E2C16);
            g.fill(pageMid - 1, stitchY + 1, pageMid + 1, stitchY + 5, 0xFF8E7147);
        }
    }

    /** Draws an animated foreshortened sheet sweeping across the book when a page is turned. */
    private void drawPageTurnSweep(GuiGraphicsExtractor g, int pageLeft, int pageMid, int pageRight,
                                   int bookTop, int bookBottom) {
        float t = turnProgress;
        // Ease-in-out curve
        float eased = t * t * (3.0f - 2.0f * t);
        float sweepPhase = turnDirection > 0 ? eased : (1.0f - eased);
        int halfWidth = pageRight - pageMid;

        // Turning edge travels from pageRight (sweepPhase=0) across pageMid to pageLeft (sweepPhase=1)
        int movingX = Math.round(Mth.lerp(sweepPhase, pageRight, pageLeft));
        int sheetMinX = Math.min(pageMid, movingX);
        int sheetMaxX = Math.max(pageMid, movingX);
        int sheetWidth = Math.max(3, sheetMaxX - sheetMinX);

        // Arch lift: middle of the flip lifts higher, casting a wider shadow onto the underlying page
        float arch = (float) Math.sin(sweepPhase * Math.PI);
        int liftY = Math.round(arch * 7.0f);

        // Cast shadow on the stationary page beneath the sweeping sheet
        if (movingX >= pageMid) {
            int shadowRight = Math.min(pageRight, movingX + Math.round(14 * arch) + 4);
            g.fill(pageMid, bookTop, shadowRight, bookBottom, 0x382A1C0D);
        } else {
            int shadowLeft = Math.max(pageLeft, movingX - Math.round(14 * arch) - 4);
            g.fill(shadowLeft, bookTop, pageMid, bookBottom, 0x382A1C0D);
        }

        // The sweeping parchment sheet itself
        int sheetTop = bookTop - liftY;
        int sheetBottom = bookBottom - liftY / 2;
        g.fill(sheetMinX, sheetTop, sheetMaxX, sheetBottom, 0xFFEAD9B6);

        // Cylindrical highlight crest along the turning sheet
        int crestX = sheetMinX + Math.round(sheetWidth * 0.65f);
        int crestSpan = Math.max(2, sheetWidth / 4);
        g.fill(Math.max(sheetMinX, crestX - crestSpan), sheetTop,
                Math.min(sheetMaxX, crestX + crestSpan), sheetBottom, 0xFFF7EBD2);

        // Crisp outer edge of the turning page
        g.fill(movingX - 1, sheetTop, movingX + 1, sheetBottom, PAPER_EDGE);
        g.fill(sheetMinX, sheetTop, sheetMaxX, sheetTop + 1, PAPER_EDGE);
        g.fill(sheetMinX, sheetBottom - 1, sheetMaxX, sheetBottom, PAPER_EDGE);
    }

    private void drawStoryPage(GuiGraphicsExtractor g, int x, int y, int maxWidth) {
        int page = currentPage + 1;
        g.text(font, Component.literal(SkillOverviewText.get("gui.simpleskills.lore.page." + page + ".title")), x, y, 0xFF543818, false);
        g.fill(x, y + 12, x + maxWidth, y + 13, 0xFFB89966);
        g.fill(x, y + 13, x + maxWidth / 2, y + 14, 0x44FFF6E0);
        g.text(font, Component.literal(SkillOverviewText.get("gui.simpleskills.lore.page." + page + ".author")), x, y + 19, MUTED_INK, false);

        String body = SkillOverviewText.get("gui.simpleskills.lore.page." + page + ".body");
        int textY = y + 35;
        textY = drawWrappedText(g, body, x, textY, maxWidth, INK, 2);

        if (page == 6) {
            textY += 6;
            drawCoordinateLine(g, x, textY, maxWidth, bridgeX, bridgeY, bridgeZ);
        } else if (page == 7) {
            textY += 6;
            drawCoordinateLine(g, x, textY, maxWidth, castleX, castleY, castleZ);
        } else if (page == 8) {
            textY += 6;
            drawColossusCoordinateNote(g, x, textY, maxWidth);
        }
    }

    /** Writes the crimson-region X/Z marker and its variable underground depth as a pencil note. */
    private void drawColossusCoordinateNote(GuiGraphicsExtractor g, int x, int y, int maxWidth) {
        boolean known = colossusX != AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE
                && colossusZ != AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
        String coordinates = known
                ? SkillOverviewText.format("gui.simpleskills.lore.coordinates_xz", colossusX, colossusZ)
                : SkillOverviewText.get("gui.simpleskills.lore.coordinates_unknown");

        if (known) {
            int noteX = x + 10;
            int noteY = y + 2;
            g.fill(x + 5, noteY, x + 7, noteY + 2, 0xFF80786B);
            g.fill(x + 3, noteY + 2, x + 5, noteY + 4, 0xFF80786B);
            g.fill(x + 1, noteY + 4, x + 3, noteY + 6, 0xFF777064);
            g.fill(x, noteY + 6, x + 2, noteY + 8, 0xFF8A8172);
            g.fill(x + 5, noteY, x + 6, noteY + 1, 0xFFC2B59B);
            Component note = Component.literal(coordinates).withStyle(style -> style.withItalic(true));
            g.text(font, note, noteX + 1, noteY + 1, 0x22807869, false);
            g.text(font, note, noteX, noteY, 0xFF7D7568, false);
            int underline = Math.min(font.width(coordinates), maxWidth - 14);
            if (underline > 12) {
                g.fill(noteX + 2, noteY + 11, noteX + underline / 3, noteY + 12, 0x367D7466);
                g.fill(noteX + underline / 2, noteY + 12, noteX + underline - 5, noteY + 13, 0x247D7466);
            }
        } else {
            drawWrappedText(g, coordinates, x, y + 2, maxWidth, 0xFF827967, 1);
        }

        String depth = SkillOverviewText.get("gui.simpleskills.lore.colossus_depth");
        int depthY = y + 17;
        drawWrappedText(g, depth, x + 10, depthY, maxWidth - 10, 0xFF827967, 1);
    }

    /**
     * Writes the recovered position as a quiet pencil note in the cartographer's margin.
     * It deliberately has no panel, border, or filled background so it feels part of the page.
     */
    private void drawCoordinateLine(GuiGraphicsExtractor g, int x, int y, int maxWidth,
                                    int coordinateX, int coordinateY, int coordinateZ) {
        boolean known = coordinateX != AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE
                && coordinateY != AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE
                && coordinateZ != AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
        String text = known
                ? SkillOverviewText.format("gui.simpleskills.lore.coordinates", coordinateX, coordinateY, coordinateZ)
                : SkillOverviewText.get("gui.simpleskills.lore.coordinates_unknown");

        if (known) {
            int noteX = x + 10;
            int noteY = y + 2;

            // A tiny graphite pencil mark beside the note.
            g.fill(x + 5, noteY, x + 7, noteY + 2, 0xFF80786B);
            g.fill(x + 3, noteY + 2, x + 5, noteY + 4, 0xFF80786B);
            g.fill(x + 1, noteY + 4, x + 3, noteY + 6, 0xFF777064);
            g.fill(x, noteY + 6, x + 2, noteY + 8, 0xFF8A8172);
            g.fill(x + 5, noteY, x + 6, noteY + 1, 0xFFC2B59B);

            // Slight graphite grain, followed by the muted pencil-colored lettering.
            Component note = Component.literal(text).withStyle(style -> style.withItalic(true));
            g.text(font, note, noteX + 1, noteY + 1, 0x22807869, false);
            g.text(font, note, noteX, noteY, 0xFF7D7568, false);

            // A few broken, faint pencil strokes keep the annotation organic rather than boxed.
            int underline = Math.min(font.width(text), maxWidth - 14);
            if (underline > 12) {
                g.fill(noteX + 2, noteY + 11, noteX + underline / 3, noteY + 12, 0x367D7466);
                g.fill(noteX + underline / 2, noteY + 12, noteX + underline - 5, noteY + 13, 0x247D7466);
            }
        } else {
            drawWrappedText(g, text, x, y + 2, maxWidth, 0xFF827967, 1);
        }
    }

    private void drawIllustration(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        int size = Math.min(width, height);
        int imageX = x + (width - size) / 2;
        int imageY = y + (height - size) / 2;

        // Keep the original graphite illustration unframed, directly on the parchment.
        Identifier image = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID,
                "textures/gui/abyss_lore/" + ILLUSTRATIONS[currentPage] + ".png");
        g.blit(RenderPipelines.GUI_TEXTURED, image, imageX, imageY,
                0, 0, size, size, 512, 512, 512, 512);
    }

    private int drawWrappedText(GuiGraphicsExtractor g, String text, int x, int y,
                                int maxWidth, int color, int paragraphGap) {
        String[] paragraphs = text.split("\\n", -1);
        for (String paragraph : paragraphs) {
            String remaining = paragraph.strip();
            if (remaining.isEmpty()) {
                y += paragraphGap + 3;
                continue;
            }
            while (!remaining.isEmpty()) {
                String line = font.plainSubstrByWidth(remaining, maxWidth);
                if (line.isEmpty()) line = remaining.substring(0, 1);
                if (line.length() < remaining.length()) {
                    int space = line.lastIndexOf(' ');
                    if (space > 0) line = line.substring(0, space);
                }
                g.text(font, Component.literal(line), x, y, color, false);
                y += 11;
                remaining = remaining.substring(line.length()).stripLeading();
            }
        }
        return y;
    }

    private void drawPageCorners(GuiGraphicsExtractor g, int mouseX, int mouseY,
                                 int pageLeft, int pageRight, int bookTop, int bookBottom) {
        int unlocked = unlockedPageCount();
        int hitSize = 44;
        boolean canGoBack = currentPage > 0;
        boolean canGoForward = currentPage + 1 < unlocked;

        previousCornerHovered = canGoBack
                && inside(mouseX, mouseY, pageLeft - 4, bookBottom - hitSize, hitSize + 4, hitSize + 4);
        nextCornerHovered = canGoForward
                && inside(mouseX, mouseY, pageRight - hitSize, bookBottom - hitSize, hitSize + 4, hitSize + 4);

        int previousSize = canGoBack ? 13 + Math.round(23.0f * previousFoldProgress) : 0;
        int nextSize = canGoForward ? 13 + Math.round(23.0f * nextFoldProgress) : 0;

        if (previousSize > 0) {
            drawPeeledPageCorner(g, pageLeft, bookBottom, previousSize, false, previousCornerHovered, true);
        }
        if (nextSize > 0) {
            drawPeeledPageCorner(g, pageRight, bookBottom, nextSize, true, nextCornerHovered, true);
        }
    }

    /**
     * Renders a genuinely peeled and curled page corner:
     * 1. Exposes the underlying page (with cast shadow) where the corner was lifted away.
     * 2. Draws a soft 3D drop shadow onto the current page underneath the lifted tip.
     * 3. Draws the curled-over back of the page with a curved ridge and cylindrical paper highlight.
     */
    private void drawPeeledPageCorner(GuiGraphicsExtractor g, int edgeX, int bottom, int size,
                                      boolean rightCorner, boolean hovered, boolean hasMorePagesBeneath) {
        float dirX = rightCorner ? -1.0f : 1.0f;
        float w = size * 1.15f;
        float h = size;

        // Crease endpoints on the bottom edge (A) and outer vertical edge (B)
        float ax = edgeX + dirX * w;
        float ay = bottom;
        float bx = edgeX;
        float by = bottom - h;

        // Unit normal pointing inward onto the page (away from corner (edgeX, bottom))
        float vx = bx - ax;
        float vy = by - ay;
        float len = (float) Math.hypot(vx, vy);
        float nx = -vy / len;
        float ny = vx / len;
        float toCornerDot = (edgeX - ax) * nx + (bottom - ay) * ny;
        if (toCornerDot > 0.0f) {
            nx = -nx;
            ny = -ny;
            toCornerDot = -toCornerDot;
        }
        float distToCrease = Math.abs(toCornerDot);

        // Position of the peeled & lifted corner tip C' curled onto the current page
        float lift = distToCrease * (hovered ? 1.78f : 1.66f);
        float tipX = edgeX + nx * lift;
        float tipY = bottom + ny * lift - h * (hovered ? 0.14f : 0.08f);

        int intW = Math.max(1, Math.round(w));
        int intH = Math.max(1, Math.round(h));

        // ---------------------------------------------------------------------
        // PASS 1: Expose the underneath page/cover in the peeled-away corner
        // ---------------------------------------------------------------------
        int underBase = hasMorePagesBeneath ? PAPER_UNDER : LEATHER_INNER;
        for (int row = 0; row <= intH; row++) {
            int y = bottom - intH + row;
            float t = (float) row / intH; // 0 at top anchor B, 1 at bottom anchor A
            // Curved crease bulge toward the outer corner
            float bulge = -dirX * (h * 0.13f * (float) Math.sin(Math.PI * t));
            int creaseX = Math.round(edgeX + dirX * (w * t) + bulge);

            int minX = rightCorner ? creaseX : edgeX;
            int maxX = rightCorner ? edgeX : creaseX;
            if (maxX <= minX) continue;

            g.fill(minX, y, maxX, y + 1, underBase);

            // Outer edge border of the exposed underlying page
            if (hasMorePagesBeneath) {
                if (rightCorner) {
                    g.fill(edgeX - 1, y, edgeX, y + 1, PAPER_EDGE);
                } else {
                    g.fill(edgeX, y, edgeX + 1, y + 1, PAPER_EDGE);
                }
                if (y >= bottom - 1) {
                    g.fill(minX, y, maxX, y + 1, PAPER_EDGE);
                }
            }

            // Graduated cast shadow from the lifted flap onto the exposed corner underneath
            int span = maxX - minX;
            int shadow1 = Math.min(span, hovered ? 6 : 4);
            int shadow2 = Math.min(span, hovered ? 12 : 7);
            if (rightCorner) {
                g.fill(minX, y, minX + shadow2, y + 1, 0x342B1D0E);
                g.fill(minX, y, minX + shadow1, y + 1, 0x6226180B);
            } else {
                g.fill(maxX - shadow2, y, maxX, y + 1, 0x342B1D0E);
                g.fill(maxX - shadow1, y, maxX, y + 1, 0x6226180B);
            }
        }

        // ---------------------------------------------------------------------
        // PASS 2: Soft drop shadow beneath the lifted flap onto the current page
        // ---------------------------------------------------------------------
        float shOffX = -dirX * (hovered ? 4.0f : 2.5f);
        float shOffY = hovered ? 3.5f : 2.0f;
        fillTriangle(g,
                ax + shOffX * 0.4f, ay,
                bx, by + shOffY * 0.4f,
                tipX + shOffX, tipY + shOffY,
                0x2626170A);
        fillTriangle(g,
                ax + shOffX * 0.25f, ay,
                bx, by + shOffY * 0.25f,
                tipX + shOffX * 0.55f, tipY + shOffY * 0.55f,
                0x3E26170A);

        // ---------------------------------------------------------------------
        // PASS 3: The curled-over back of the page (triangle A - B - C')
        // ---------------------------------------------------------------------
        int minY = Math.max(top + 51, (int) Math.floor(Math.min(by, tipY)));
        int maxY = bottom;
        int outerLipColor = hovered ? 0xFF8E7147 : 0xFF9C7F54;
        int ridgeHighlight = hovered ? 0xFFFCF3DF : 0xFFF5E6C8;
        int ridgeSecondary = hovered ? 0xFFF5E6C7 : 0xFFECDBB8;
        int flapMidColor = hovered ? 0xFFEAD7B2 : 0xFFE2CEA6;
        int flapTipColor = hovered ? 0xFFDFC89C : 0xFFD7BF93;
        int freeEdgeColor = hovered ? 0xFF7D613B : 0xFF8D7048;

        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            float x1 = Float.POSITIVE_INFINITY;
            float x2 = Float.NEGATIVE_INFINITY;

            // Intersect scanline py with the 3 edges of triangle (A, B, Tip)
            float[] xs = {
                    intersectEdgeX(ax, ay, bx, by, py),
                    intersectEdgeX(bx, by, tipX, tipY, py),
                    intersectEdgeX(tipX, tipY, ax, ay, py)
            };
            for (float ix : xs) {
                if (!Float.isNaN(ix)) {
                    if (ix < x1) x1 = ix;
                    if (ix > x2) x2 = ix;
                }
            }
            if (x1 > x2) continue;

            // Apply matching curved bulge along the crease side of the flap
            float tCrease = Mth.clamp((py - by) / Math.max(1.0f, ay - by), 0.0f, 1.0f);
            float bulge = -dirX * (h * 0.13f * (float) Math.sin(Math.PI * tCrease));
            if (rightCorner) {
                x2 += bulge;
            } else {
                x1 += bulge;
            }

            int startX = Math.round(x1);
            int endX = Math.round(x2);
            if (endX <= startX) continue;

            for (int x = startX; x < endX; x++) {
                float px = x + 0.5f;
                // Perpendicular distance from the curved crease (measures how far up the curl we are)
                float creaseLineX = ax + (py - ay) * (bx - ax) / Math.max(1.0f, by - ay) + bulge;
                float u = Math.abs((px - creaseLineX) * nx);

                int color;
                if (x == startX || x == endX - 1 || y == maxY) {
                    // Outer crease lip vs. lifted free edge of the paper
                    boolean onCreaseSide = rightCorner ? (x >= endX - 2) : (x <= startX + 1);
                    color = onCreaseSide ? outerLipColor : freeEdgeColor;
                } else if (u < 2.2f) {
                    color = outerLipColor;
                } else if (u < 6.0f) {
                    color = ridgeHighlight;
                } else if (u < 10.5f) {
                    color = ridgeSecondary;
                } else if (u < 18.0f) {
                    color = flapMidColor;
                } else {
                    color = flapTipColor;
                }
                g.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private static float intersectEdgeX(float x1, float y1, float x2, float y2, float y) {
        if ((y < y1 && y < y2) || (y > y1 && y > y2) || Math.abs(y2 - y1) < 0.001f) {
            return Float.NaN;
        }
        return x1 + (y - y1) * (x2 - x1) / (y2 - y1);
    }

    private static void fillTriangle(GuiGraphicsExtractor g, float x1, float y1,
                                     float x2, float y2, float x3, float y3, int color) {
        int minY = (int) Math.floor(Math.min(y1, Math.min(y2, y3)));
        int maxY = (int) Math.ceil(Math.max(y1, Math.max(y2, y3)));
        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            float lx = Float.POSITIVE_INFINITY;
            float rx = Float.NEGATIVE_INFINITY;
            float[] xs = {
                    intersectEdgeX(x1, y1, x2, y2, py),
                    intersectEdgeX(x2, y2, x3, y3, py),
                    intersectEdgeX(x3, y3, x1, y1, py)
            };
            for (float ix : xs) {
                if (!Float.isNaN(ix)) {
                    if (ix < lx) lx = ix;
                    if (ix > rx) rx = ix;
                }
            }
            int sx = Math.round(lx);
            int ex = Math.round(rx);
            if (ex > sx) {
                g.fill(sx, y, ex, y + 1, color);
            }
        }
    }

    private static void drawRectOutline(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    private static float approach(float current, float target, float amount) {
        return current + (target - current) * amount;
    }

    private void drawNavigationTabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = navigationX();
        int skillsY = top + 64;
        int loreY = top + 104;
        drawNavigationButton(g, x, skillsY, new ItemStack(Items.IRON_PICKAXE),
                inside(mouseX, mouseY, x, skillsY, 28, 32), false);
        drawNavigationButton(g, x, loreY, new ItemStack(Items.BOOK),
                inside(mouseX, mouseY, x, loreY, 28, 32), true);
        if (inside(mouseX, mouseY, x, skillsY, 28, 32)) {
            drawTooltip(g, SkillOverviewText.get("gui.simpleskills.lore.nav_skills"), x - 116, skillsY + 6);
        } else if (inside(mouseX, mouseY, x, loreY, 28, 32)) {
            drawTooltip(g, SkillOverviewText.get("gui.simpleskills.lore.nav_book"), x - 116, loreY + 6);
        }
    }

    private void drawNavigationButton(GuiGraphicsExtractor g, int x, int y, ItemStack icon,
                                      boolean hovered, boolean selected) {
        g.fill(x - 1, y - 1, x + 29, y + 33, selected ? GOLD : GOLD_DARK);
        g.fill(x, y, x + 28, y + 32, selected ? 0xFF49371F : hovered ? 0xFF3C2E20 : LEATHER_OUTER);
        g.item(icon, x + 6, y + 8);
    }

    private void drawTooltip(GuiGraphicsExtractor g, String text, int x, int y) {
        int width = font.width(text) + 10;
        g.fill(x, y, x + width, y + 18, 0xF20B0907);
        g.fill(x, y, x + width, y + 1, GOLD_DARK);
        g.text(font, Component.literal(text), x + 5, y + 5, GOLD, false);
    }

    private int navigationX() {
        return Math.min(left + WIDTH + 6, width - 34);
    }

    private static boolean inside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    private static Component loreText(String suffix) {
        return Component.literal(SkillOverviewText.get("gui.simpleskills.lore." + suffix));
    }

    private static Component loreText(String suffix, Object... args) {
        return Component.literal(SkillOverviewText.format("gui.simpleskills.lore." + suffix, args));
    }

    private void turnPage(int delta) {
        int target = Mth.clamp(currentPage + delta, 0, unlockedPageCount() - 1);
        if (target != currentPage) {
            currentPage = target;
            turnDirection = delta > 0 ? 1 : -1;
            turnProgress = 0.0f;
        }
    }

    // GLFW-compatible keyboard codes are used by Minecraft's KeyEvent, but referencing
    // org.lwjgl.glfw.GLFW directly is not available on this project's compile classpath.
    private static final int KEY_A = 65;
    private static final int KEY_D = 68;
    private static final int KEY_RIGHT = 262;
    private static final int KEY_LEFT = 263;
    private static final int KEY_PAGE_UP = 266;
    private static final int KEY_PAGE_DOWN = 267;

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == KEY_RIGHT || key == KEY_D || key == KEY_PAGE_DOWN) {
            turnPage(1);
            return true;
        }
        if (key == KEY_LEFT || key == KEY_A || key == KEY_PAGE_UP) {
            turnPage(-1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int navX = navigationX();
        int skillsY = top + 64;
        int loreY = top + 104;
        if (inside(event.x(), event.y(), navX, skillsY, 28, 32)) {
            if (ClientPlayNetworking.canSend(SkillOverviewRequestPayload.TYPE)) {
                ClientPlayNetworking.send(new SkillOverviewRequestPayload(SkillOverviewRequestPayload.OPEN));
            }
            return true;
        }
        if (inside(event.x(), event.y(), navX, loreY, 28, 32)) return true;

        int pageLeft = left + 18;
        int pageRight = left + WIDTH - 18;
        int bookBottom = top + HEIGHT - 23;
        int cornerHitSize = 44;
        if (inside(event.x(), event.y(), pageLeft - 4, bookBottom - cornerHitSize,
                cornerHitSize + 4, cornerHitSize + 4)) {
            turnPage(-1);
            return true;
        }
        if (inside(event.x(), event.y(), pageRight - cornerHitSize, bookBottom - cornerHitSize,
                cornerHitSize + 4, cornerHitSize + 4)) {
            turnPage(1);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (active == this) active = null;
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}