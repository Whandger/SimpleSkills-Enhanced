package com.github.ob_yekt.simpleskills.enchanting;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class RuneInfusionMinigameScreen extends Screen {
    private static final int PANEL_W = 286;
    private static final int PANEL_H = 224;
    private static final int BOX = 104;          // item frame
    private static final int ICON = 96;          // 16px item scaled x6

    private static final int GOLD = 0xFFE9CF7A;
    private static final int GOLD_DARK = 0xFF80612A;
    private static final int PANEL = 0xEE17120E;
    private static final int INNER = 0xFF292018;
    private static final int TEXT = 0xFFF3E8D2;
    private static final int MUTED = 0xFFB7A88D;
    private static final int RED = 0xFFE45A4A;
    private static final int GREEN = 0xFF67D68B;
    private static final int ORANGE = 0xFFFF9F3A;

    private static final int CLOSE_DELAY_TICKS = 30;

    private enum Look { GROWING, WARNING, CRITICAL, REPAIRING }

    private record Seg(float x1, float y1, float x2, float y2, float d1, float d2, int depth) {}

    private static final class Shape {
        final List<Seg> segs = new ArrayList<>();
        float maxDist = 1f;
        float minY = 0f;
    }

    private int left, top, boxX, boxY, iconX, iconY;
    private int heldCrackId = -1;
    private boolean mouseDown;
    private int lastMouseX, lastMouseY;
    private int closeTicks;

    private UUID shapeSession;
    private final Map<Integer, Shape> shapes = new HashMap<>();

    public RuneInfusionMinigameScreen() {
        super(Component.translatable("gui.simpleskills.rune_infusion.title"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        boxX = left + (PANEL_W - BOX) / 2;
        boxY = top + 36;
        iconX = boxX + (BOX - ICON) / 2;
        iconY = boxY + (BOX - ICON) / 2;
    }

    // ------------------------------------------------------------------ crack geometry

    private Shape shapeFor(RuneInfusionClientState.State s, int crackId) {
        if (!s.sessionId().equals(shapeSession)) {
            shapes.clear();
            shapeSession = s.sessionId();
        }
        return shapes.computeIfAbsent(crackId, id ->
                generate(s.sessionId().getLeastSignificantBits() * 31L + id * 0x9E3779B97F4A7C15L));
    }

    /** Procedural fracture: two jagged arms from the origin with recursive side branches. */
    private static Shape generate(long seed) {
        Random r = new Random(seed);
        Shape s = new Shape();
        float scale = 0.85f + r.nextFloat() * 0.35f;
        float a = r.nextFloat() * 6.2831855f;
        arm(r, s, 0f, 0f, a, 22f * scale, 0, 0f);
        arm(r, s, 0f, 0f, a + 3.1415927f + (r.nextFloat() - 0.5f) * 0.9f, 17f * scale, 0, 0f);

        float max = 1f;
        for (Seg seg : s.segs) {
            max = Math.max(max, seg.d2());
            s.minY = Math.min(s.minY, Math.min(seg.y1(), seg.y2()));
        }
        s.maxDist = max;
        return s;
    }

    private static void arm(Random r, Shape s, float x, float y, float angle,
                            float length, int depth, float dist) {
        float walked = 0f;
        while (walked < length) {
            float step = 2.5f + r.nextFloat() * 3.5f;
            if (walked + step > length) step = length - walked;
            if (step < 1f) break;

            angle += (float) r.nextGaussian() * 0.42f;   // irregular, jagged direction
            float nx = x + (float) Math.cos(angle) * step;
            float ny = y + (float) Math.sin(angle) * step;
            s.segs.add(new Seg(x, y, nx, ny, dist, dist + step, depth));

            dist += step;
            walked += step;
            x = nx;
            y = ny;

            if (depth < 3 && walked > 4f && r.nextFloat() < (depth == 0 ? 0.34f : 0.22f)) {
                float side = r.nextBoolean() ? 1f : -1f;
                float branchAngle = angle + side * (0.45f + r.nextFloat() * 0.8f);
                float branchLength = (length - walked) * (0.55f + r.nextFloat() * 0.35f) + 3f;
                arm(r, s, x, y, branchAngle, branchLength, depth + 1, dist);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static float growthOf(RuneInfusionClientState.Crack c) {
        return Math.max(0f, Math.min(1f, c.ageTicks() / (float) c.growthRequired()));
    }

    private static Look lookOf(RuneInfusionClientState.Crack c) {
        if (c.repairing()) return Look.REPAIRING;
        if (c.ageTicks() >= c.growthRequired()) return Look.CRITICAL;
        if (growthOf(c) >= 0.70f) return Look.WARNING;
        return Look.GROWING;
    }

    private int crackX(RuneInfusionClientState.Crack c) {
        return iconX + Math.round(Math.max(0.12f, Math.min(0.88f, c.x())) * ICON);
    }

    private int crackY(RuneInfusionClientState.Crack c) {
        return iconY + Math.round(Math.max(0.12f, Math.min(0.88f, c.y())) * ICON);
    }

    private static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private static float pulse() {
        return 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 110.0);
    }

    /** All crack pixels are clipped to the item icon so cracks always sit ON the item. */
    private void dot(GuiGraphicsExtractor g, int x, int y, int color) {
        if (x < iconX || y < iconY || x >= iconX + ICON || y >= iconY + ICON) return;
        g.fill(x, y, x + 1, y + 1, color);
    }

    private void line(GuiGraphicsExtractor g, float ax, float ay, float bx, float by,
                      boolean outlinePass, int core, int outline, boolean thick) {
        float dx = bx - ax;
        float dy = by - ay;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));

        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(ax + dx * t);
            int y = Math.round(ay + dy * t);
            if (outlinePass) {
                dot(g, x - 1, y, outline);
                dot(g, x + 1, y, outline);
                dot(g, x, y - 1, outline);
                dot(g, x, y + 1, outline);
            } else {
                dot(g, x, y, core);
                if (thick) dot(g, x + 1, y, core);
            }
        }
    }

    private void drawCrack(GuiGraphicsExtractor g, RuneInfusionClientState.Crack crack, Shape shape) {
        int cx = crackX(crack);
        int cy = crackY(crack);
        float growth = growthOf(crack);
        float revealed = growth * shape.maxDist;
        Look look = lookOf(crack);
        float pulse = pulse();

        int core;
        int outline;
        switch (look) {
            case REPAIRING -> { core = 0xFFFFF4C2; outline = argb(150 + (int) (60 * pulse), 0xF2C14E); }
            case CRITICAL -> { core = 0xFF2B0505; outline = argb(110 + (int) (130 * pulse), 0xFF3B30); }
            case WARNING -> { core = 0xFF3D1A05; outline = 0xB8FF9F3A; }
            default -> { core = 0xFF15110E; outline = 0x80E6DFD0; }
        }

        for (int pass = 0; pass < 2; pass++) {
            boolean outlinePass = pass == 0;
            for (Seg seg : shape.segs) {
                if (revealed <= seg.d1()) continue;
                float f = Math.min(1f, (revealed - seg.d1()) / (seg.d2() - seg.d1()));
                float ex = seg.x1() + (seg.x2() - seg.x1()) * f;
                float ey = seg.y1() + (seg.y2() - seg.y1()) * f;
                line(g, cx + seg.x1(), cy + seg.y1(), cx + ex, cy + ey,
                        outlinePass, core, outline, seg.depth() == 0 && growth > 0.25f);
            }
        }

        // Sparks while being repaired.
        if (look == Look.REPAIRING && !shape.segs.isEmpty()) {
            Random sr = new Random((System.currentTimeMillis() / 80L) * 131L + crack.id());
            for (int i = 0; i < 5; i++) {
                Seg sg = shape.segs.get(sr.nextInt(shape.segs.size()));
                if (revealed <= sg.d1()) continue;
                float t = sr.nextFloat();
                int px = Math.round(cx + sg.x1() + (sg.x2() - sg.x1()) * t);
                int py = Math.round(cy + sg.y1() + (sg.y2() - sg.y1()) * t) - sr.nextInt(5);
                dot(g, px, py, 0xFFFFFFE0);
            }
        }

        // Size/urgency meter above the crack.
        int barW = 22;
        int bx = Math.max(iconX + 1, Math.min(iconX + ICON - barW - 1, cx - barW / 2));
        int by = Math.max(iconY + 1, cy + Math.round(shape.minY) - 7);
        int barColor = switch (look) {
            case REPAIRING -> 0xFFF2C14E;
            case CRITICAL -> argb(170 + (int) (85 * pulse), 0xFF3B30);
            case WARNING -> ORANGE;
            default -> 0xFFBFB8A8;
        };
        g.fill(bx - 1, by - 1, bx + barW + 1, by + 3, 0xC0000000);
        g.fill(bx, by, bx + Math.max(1, Math.round(barW * growth)), by + 2, barColor);
    }

    // ------------------------------------------------------------------ render

    private ItemStack getEquipment(RuneInfusionClientState.State state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || state.inventorySlot() < 0
                || state.inventorySlot() >= mc.player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return mc.player.getInventory().getItem(state.inventorySlot());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        RuneInfusionClientState.State s = RuneInfusionClientState.get();
        if (s == null) return;   // tick() closes the screen

        boolean running = s.state() == RuneInfusionClientState.RUNNING;
        boolean anyCritical = false;
        for (RuneInfusionClientState.Crack c : s.cracks()) {
            if (lookOf(c) == Look.CRITICAL) anyCritical = true;
        }

        g.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, GOLD_DARK);
        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, GOLD);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, PANEL);
        g.fill(left + 6, top + 6, left + PANEL_W - 6, top + PANEL_H - 6, INNER);

        centered(g, Component.translatable("gui.simpleskills.rune_infusion.title"), top + 10, GOLD);
        centered(g, Component.translatable("gui.simpleskills.rune_infusion.subtitle"), top + 23, MUTED);

        // Item frame (pulses red while any crack is damaging the enchantment).
        int frame = anyCritical && running ? argb(180 + (int) (75 * pulse()), 0xFF3B30) : GOLD_DARK;
        g.fill(boxX - 2, boxY - 2, boxX + BOX + 2, boxY + BOX + 2, frame);
        g.fill(boxX, boxY, boxX + BOX, boxY + BOX, 0xFF17130F);
        g.fill(boxX + 2, boxY + 2, boxX + BOX - 2, boxY + BOX - 2, 0xFF393027);

        ItemStack equipment = getEquipment(s);
        if (!equipment.isEmpty()) {
            g.pose().pushMatrix();
            g.pose().translate(iconX, iconY);
            g.pose().scale(6.0f, 6.0f);
            g.item(equipment, 0, 0);
            g.pose().popMatrix();
        } else {
            centered(g, Component.translatable("gui.simpleskills.rune_infusion.missing_item"),
                    boxY + BOX / 2 - 4, RED);
        }

        for (RuneInfusionClientState.Crack crack : s.cracks()) {
            drawCrack(g, crack, shapeFor(s, crack.id()));
        }

        if (!running) {
            boolean ok = s.state() == RuneInfusionClientState.SUCCEEDED;
            g.fill(boxX + 2, boxY + 2, boxX + BOX - 2, boxY + BOX - 2, 0xB0000000);
            centered(g, Component.translatable(ok
                            ? "gui.simpleskills.rune_infusion.result.success"
                            : "gui.simpleskills.rune_infusion.result.failed"),
                    boxY + BOX / 2 - 10, ok ? GREEN : RED);
            if (ok) {
                int grade = Math.max(0, Math.min(4, s.grade()));
                centered(g, Component.translatable("gui.simpleskills.rune_infusion.grade",
                                Component.translatable("gui.simpleskills.rune_infusion.grade." + grade)),
                        boxY + BOX / 2 + 4, GOLD);
            }
        }

        // Integrity bar.
        int barX = left + 25;
        int barY = top + 148;
        int barW = PANEL_W - 50;
        int integrity = Math.max(0, Math.min(1000, s.integrity()));
        int barColor = integrity > 500 ? GREEN : integrity > 250 ? ORANGE : RED;
        if (anyCritical && running && pulse() > 0.6f) barColor = RED;

        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, 0xFF080706);
        g.fill(barX, barY, barX + barW, barY + 8, 0xFF39312A);
        int filled = barW * integrity / 1000;
        if (filled > 0) g.fill(barX, barY, barX + filled, barY + 8, barColor);

        centered(g, Component.translatable("gui.simpleskills.rune_infusion.integrity", integrity / 10),
                top + 160, TEXT);

        String time = String.format(java.util.Locale.ROOT, "%d / %ds",
                s.elapsedTicks() / 20, s.durationTicks() / 20);
        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.time", time),
                left + 18, top + 174, MUTED, false);
        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.repaired", s.repairedCracks()),
                left + 112, top + 174, GREEN, false);
        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.spread", s.propagatedCracks()),
                left + 205, top + 174, RED, false);

        drawLegend(g);

        centered(g, Component.translatable("gui.simpleskills.rune_infusion.hold_to_repair"),
                top + PANEL_H - 19, MUTED);
    }

    private void drawLegend(GuiGraphicsExtractor g) {
        int[] colors = {0xFFBFB8A8, 0xFFF2C14E, 0xFFFF3B30};
        Component[] labels = {
                Component.translatable("gui.simpleskills.rune_infusion.legend.growing"),
                Component.translatable("gui.simpleskills.rune_infusion.legend.repairing"),
                Component.translatable("gui.simpleskills.rune_infusion.legend.critical")
        };
        int total = 0;
        for (Component label : labels) total += 10 + font.width(label) + 14;
        total -= 14;

        int x = width / 2 - total / 2;
        int y = top + 190;
        for (int i = 0; i < labels.length; i++) {
            g.fill(x, y + 1, x + 6, y + 7, colors[i]);
            g.text(font, labels[i], x + 10, y, MUTED, false);
            x += 10 + font.width(labels[i]) + 14;
        }
    }

    private void centered(GuiGraphicsExtractor g, Component text, int y, int color) {
        g.text(font, text, width / 2 - font.width(text) / 2, y, color, true);
    }

    // ------------------------------------------------------------------ input

    private static boolean isRepairButton(int button) {
        return button == 0 || button == 1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isRepairButton(event.button())) return super.mouseClicked(event, doubleClick);

        mouseDown = true;
        lastMouseX = (int) event.x();
        lastMouseY = (int) event.y();

        RuneInfusionClientState.State state = RuneInfusionClientState.get();
        if (state == null || state.state() != RuneInfusionClientState.RUNNING) return true;

        int crackId = findCrackAt(event.x(), event.y(), state);
        if (crackId >= 0) {
            heldCrackId = crackId;
            send(state, RuneInfusionInputPayload.BEGIN, crackId);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (isRepairButton(event.button())) {
            mouseDown = false;
            sendRelease();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void tick() {
        super.tick();

        RuneInfusionClientState.State state = RuneInfusionClientState.get();
        if (state == null) {
            close();
            return;
        }

        // Finished: show the result briefly, then close.
        if (RuneInfusionClientState.isEnding() || state.state() != RuneInfusionClientState.RUNNING) {
            heldCrackId = -1;
            mouseDown = false;
            if (++closeTicks >= CLOSE_DELAY_TICKS) close();
            return;
        }

        if (heldCrackId >= 0) {
            final int held = heldCrackId;
            if (state.cracks().stream().noneMatch(c -> c.id() == held)) heldCrackId = -1;
        }

        // Button still down after a crack vanished: continue onto the crack under the cursor.
        if (heldCrackId < 0 && mouseDown) {
            int next = findCrackAt(lastMouseX, lastMouseY, state);
            if (next >= 0) {
                heldCrackId = next;
                send(state, RuneInfusionInputPayload.BEGIN, next);
            }
        }

        if (heldCrackId >= 0) {
            send(state, RuneInfusionInputPayload.HOLD, heldCrackId);
        }
    }

    private int findCrackAt(double mx, double my, RuneInfusionClientState.State state) {
        double best = 10.0;      // px tolerance: thin cracks must stay easy to grab
        int bestId = -1;

        for (RuneInfusionClientState.Crack crack : state.cracks()) {
            Shape shape = shapeFor(state, crack.id());
            int cx = crackX(crack);
            int cy = crackY(crack);
            float revealed = growthOf(crack) * shape.maxDist;

            double d = Math.hypot(mx - cx, my - cy) - 4.0;
            for (Seg seg : shape.segs) {
                if (revealed <= seg.d1()) continue;
                float f = Math.min(1f, (revealed - seg.d1()) / (seg.d2() - seg.d1()));
                double ex = cx + seg.x1() + (seg.x2() - seg.x1()) * f;
                double ey = cy + seg.y1() + (seg.y2() - seg.y1()) * f;
                d = Math.min(d, distToSegment(mx, my, cx + seg.x1(), cy + seg.y1(), ex, ey));
            }
            if (d < best) {
                best = d;
                bestId = crack.id();
            }
        }
        return bestId;
    }

    private static double distToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    private void send(RuneInfusionClientState.State state, int action, int crackId) {
        ClientPlayNetworking.send(RuneInfusionInputPayload.of(state.sessionId(), action, crackId));
    }

    private void sendRelease() {
        RuneInfusionClientState.State state = RuneInfusionClientState.get();
        if (state != null && heldCrackId >= 0) {
            send(state, RuneInfusionInputPayload.RELEASE, heldCrackId);
        }
        heldCrackId = -1;
    }

    private void close() {
        Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** The server owns the session; ESC must not leave a ghost attempt running without a screen. */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        sendRelease();
        close();
    }

    /** Called when the screen is unloaded; lets the client state reset or reopen correctly. */
    public void removed() {
        RuneInfusionClientState.screenClosed();
    }
}
