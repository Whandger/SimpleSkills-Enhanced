
package com.github.ob_yekt.simpleskills.enchanting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class RuneInfusionMinigameScreen extends Screen {
    private static final int PANEL_W = 286;
    private static final int PANEL_H = 224;

    private static final int GOLD = 0xFFE9CF7A;
    private static final int GOLD_DARK = 0xFF80612A;
    private static final int PANEL = 0xEE17120E;
    private static final int INNER = 0xFF292018;
    private static final int TEXT = 0xFFF3E8D2;
    private static final int MUTED = 0xFFB7A88D;
    private static final int RED = 0xFFE45A4A;
    private static final int GREEN = 0xFF67D68B;

    // Cores das rachaduras.
    private static final int CRACK_DARK = 0xFF252321;
    private static final int CRACK_GRAY = 0xFF999795;
    private static final int CRACK_LIGHT = 0xFFBDBAB5;

    // Brilho usado somente durante o reparo.
    private static final int REPAIR_GLOW = 0x88F5D98A;
    private static final int REPAIR_GLOW_INNER = 0x66FFFFFF;

    // Tempo necessário para a rachadura atingir seu tamanho máximo.
    private static final int CRACK_GROWTH_TICKS = 240;

    private int left;
    private int top;
    private int heldCrackId = -1;

    public RuneInfusionMinigameScreen() {
        super(Component.translatable("gui.simpleskills.rune_infusion.title"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
    }

    private ItemStack getEquipment(RuneInfusionClientState.State state) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || state.inventorySlot() < 0
                || state.inventorySlot() >= mc.player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }

        return mc.player.getInventory().getItem(state.inventorySlot());
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor g,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractBackground(g, mouseX, mouseY, partialTick);

        RuneInfusionClientState.State s = RuneInfusionClientState.get();

        if (s == null) {
            onClose();
            return;
        }

        g.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, GOLD_DARK);
        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, GOLD);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, PANEL);
        g.fill(left + 6, top + 6, left + PANEL_W - 6, top + PANEL_H - 6, INNER);

        centered(g, Component.translatable("gui.simpleskills.rune_infusion.title"), top + 12, GOLD);
        centered(g, Component.translatable("gui.simpleskills.rune_infusion.subtitle"), top + 27, MUTED);

        int itemX = left + 92;
        int itemY = top + 51;

        // Fundo do equipamento.
        g.fill(itemX + 5, itemY + 2, itemX + 87, itemY + 88, 0xFF17130F);
        g.fill(itemX + 7, itemY + 4, itemX + 85, itemY + 86, 0xFF393027);

        ItemStack equipment = getEquipment(s);

        if (!equipment.isEmpty()) {
            // Ícone real do item, ampliado para facilitar a visualização.
            g.pose().pushMatrix();
            g.pose().translate(itemX + 15, itemY + 12);
            g.pose().scale(4.0f, 4.0f);
            g.item(equipment, 0, 0);
            g.pose().popMatrix();

            g.itemDecorations(font, equipment, itemX + 15, itemY + 12);
        } else {
            centered(g, Component.translatable("gui.simpleskills.rune_infusion.missing_item"),
                    itemY + 40, RED);
        }

        /*
         * Crescimento individual das rachaduras.
         *
         * Cada rachadura começa em ageTicks = 0.
         * Conforme ageTicks aumenta, os segmentos aparecem.
         * Durante o reparo, ageTicks diminui e a rachadura
         * volta visualmente ao ponto inicial.
         */
        for (RuneInfusionClientState.Crack crack : s.cracks()) {
            if (crack.repaired()) continue;

            int cx = itemX + 7 + Math.round(
                    Math.max(0f, Math.min(1f, crack.x())) * 76f);

            int cy = itemY + 4 + Math.round(
                    Math.max(0f, Math.min(1f, crack.y())) * 78f);

            boolean repairing = heldCrackId == crack.id();

            // Progresso individual da rachadura.
            float growth = Math.max(0f, Math.min(
                    1f,
                    crack.ageTicks() / (float) CRACK_GROWTH_TICKS
            ));

            /*
             * Fenda principal.
             * Começa pequena e cresce gradualmente.
             */
            int[] mainCrack = {
                    -1, -12,
                     2,  -7,
                    -2,  -2,
                     2,   3,
                     0,   8
            };

            drawCrackPath(
                    g,
                    cx,
                    cy,
                    mainCrack,
                    growth,
                    repairing
            );

            /*
             * Ramificações laterais.
             * Elas começam a aparecer depois que a fenda principal
             * já começou a crescer.
             */
            if (growth > 0.18f) {
                float branchGrowth = Math.max(0f, Math.min(
                        1f,
                        (growth - 0.18f) / 0.45f
                ));

                int[] upperBranch = {
                        -2,  -2,
                        -7,  -5,
                       -10, -10,
                       -15, -12
                };

                drawCrackPath(
                        g,
                        cx,
                        cy,
                        upperBranch,
                        branchGrowth,
                        repairing
                );

                int[] lowerBranch = {
                         2,  3,
                         7,  5,
                        10, 10,
                        15, 13
                };

                drawCrackPath(
                        g,
                        cx,
                        cy,
                        lowerBranch,
                        branchGrowth,
                        repairing
                );
            }

            /*
             * Pequenas fissuras nas extremidades.
             * Aparecem quando a rachadura já está desenvolvida.
             */
            if (growth > 0.72f) {
                float smallGrowth = Math.max(0f, Math.min(
                        1f,
                        (growth - 0.72f) / 0.28f
                ));

                int[] upperTip = {
                        -10, -10,
                         -9, -15
                };

                drawCrackPath(
                        g,
                        cx,
                        cy,
                        upperTip,
                        smallGrowth,
                        repairing
                );

                int[] lowerTip = {
                        10, 10,
                        14,  8
                };

                drawCrackPath(
                        g,
                        cx,
                        cy,
                        lowerTip,
                        smallGrowth,
                        repairing
                );
            }
        }

        int barX = left + 25;
        int barY = top + 157;
        int barW = PANEL_W - 50;
        int integrity = Math.max(0, Math.min(1000, s.integrity()));

        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, 0xFF080706);
        g.fill(barX, barY, barX + barW, barY + 8, 0xFF39312A);

        int filled = barW * integrity / 1000;

        if (filled > 0) {
            g.fill(barX, barY, barX + filled, barY + 8,
                    integrity > 500 ? GREEN : RED);
        }

        centered(g,
                Component.translatable("gui.simpleskills.rune_infusion.integrity", integrity / 10),
                top + 170, TEXT);

        String time = String.format(java.util.Locale.ROOT,
                "%d / %d", s.elapsedTicks(), s.durationTicks());

        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.time", time),
                left + 18, top + 194, MUTED, false);

        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.repaired", s.repairedCracks()),
                left + 112, top + 194, GREEN, false);

        g.text(font, Component.translatable("gui.simpleskills.rune_infusion.spread", s.propagatedCracks()),
                left + 205, top + 194, RED, false);

        centered(g,
                Component.translatable("gui.simpleskills.rune_infusion.hold_to_repair"),
                top + PANEL_H - 15, MUTED);
    }

    /**
     * Desenha uma rachadura através de pontos.
     *
     * O progresso revela os segmentos gradualmente.
     * Quando diminui, os segmentos desaparecem na ordem inversa.
     */
    private void drawCrackPath(
            GuiGraphicsExtractor g,
            int centerX,
            int centerY,
            int[] points,
            float progress,
            boolean repairing
    ) {
        if (progress <= 0f || points.length < 4) return;

        int segmentCount = points.length / 2 - 1;
        float visibleSegments = segmentCount * Math.min(1f, progress);

        for (int i = 0; i < segmentCount; i++) {
            if (i >= visibleSegments) break;

            float segmentProgress = Math.min(1f, visibleSegments - i);

            int x1 = points[i * 2];
            int y1 = points[i * 2 + 1];
            int x2 = points[(i + 1) * 2];
            int y2 = points[(i + 1) * 2 + 1];

            int endX = x1 + Math.round((x2 - x1) * segmentProgress);
            int endY = y1 + Math.round((y2 - y1) * segmentProgress);

            drawCrackSegment(
                    g,
                    centerX + x1,
                    centerY + y1,
                    centerX + endX,
                    centerY + endY,
                    repairing
            );
        }
    }

    /**
     * Desenha uma fenda fina e irregular.
     *
     * A rachadura permanece cinza.
     * Durante o reparo, somente a luz abaixo dela é adicionada.
     */
    private void drawCrackSegment(
            GuiGraphicsExtractor g,
            int x1,
            int y1,
            int x2,
            int y2,
            boolean repairing
    ) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));

        if (steps == 0) steps = 1;

        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;

            int x = x1 + Math.round(dx * t);
            int y = y1 + Math.round(dy * t);

            // Brilho discreto abaixo de cada trecho da rachadura.
            if (repairing) {
                g.fill(x - 1, y + 2, x + 2, y + 4, REPAIR_GLOW);
                g.fill(x, y + 2, x + 1, y + 3, REPAIR_GLOW_INNER);
            }

            // Sombra escura deslocada, dando profundidade à fenda.
            g.fill(x + 1, y + 1, x + 2, y + 2, CRACK_DARK);

            // Interior cinza da rachadura.
            g.fill(x, y, x + 1, y + 1, CRACK_GRAY);

            // Pequeno reflexo na borda.
            if (i % 3 == 0) {
                g.fill(x, y, x + 1, y + 1, CRACK_LIGHT);
            }
        }
    }

    private void centered(GuiGraphicsExtractor g, Component text, int y, int color) {
        g.text(font, text, width / 2 - font.width(text) / 2, y, color, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 1) {
            return super.mouseClicked(event, doubleClick);
        }

        RuneInfusionClientState.State state = RuneInfusionClientState.get();

        if (state == null) {
            return true;
        }

        int crackId = findCrackAt(event.x(), event.y(), state);

        if (crackId < 0) {
            return true;
        }

        heldCrackId = crackId;

        ClientPlayNetworking.send(RuneInfusionInputPayload.of(
                state.sessionId(),
                RuneInfusionInputPayload.BEGIN,
                crackId
        ));

        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (heldCrackId < 0) return;

        RuneInfusionClientState.State state = RuneInfusionClientState.get();

        if (state == null) {
            heldCrackId = -1;
            return;
        }

        // Verifica se a rachadura ainda existe e não foi reparada.
        boolean exists = state.cracks().stream()
                .anyMatch(c -> c.id() == heldCrackId && !c.repaired());

        if (!exists) {
            heldCrackId = -1;
            return;
        }

        // Continua enviando HOLD enquanto o botão estiver pressionado.
        ClientPlayNetworking.send(RuneInfusionInputPayload.of(
                state.sessionId(),
                RuneInfusionInputPayload.HOLD,
                heldCrackId
        ));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 1 && heldCrackId >= 0) {
            sendRelease();
            return true;
        }

        return super.mouseReleased(event);
    }

    private int findCrackAt(
            double mouseX,
            double mouseY,
            RuneInfusionClientState.State state
    ) {
        int itemX = left + 92;
        int itemY = top + 51;

        int bestId = -1;
        double bestDistance = Double.MAX_VALUE;

        for (RuneInfusionClientState.Crack crack : state.cracks()) {
            if (crack.repaired()) continue;

            double cx = itemX + 7 + Math.max(0f, Math.min(1f, crack.x())) * 76f;
            double cy = itemY + 4 + Math.max(0f, Math.min(1f, crack.y())) * 78f;

            double dx = mouseX - cx;
            double dy = mouseY - cy;

            if (Math.abs(dx) > 24 || Math.abs(dy) > 24) {
                continue;
            }

            double distance = dx * dx + dy * dy;

            if (distance < bestDistance) {
                bestDistance = distance;
                bestId = crack.id();
            }
        }

        return bestId;
    }

    private void sendRelease() {
        RuneInfusionClientState.State state = RuneInfusionClientState.get();

        if (state != null && heldCrackId >= 0) {
            ClientPlayNetworking.send(RuneInfusionInputPayload.of(
                    state.sessionId(),
                    RuneInfusionInputPayload.RELEASE,
                    heldCrackId
            ));
        }

        heldCrackId = -1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        sendRelease();
        Minecraft.getInstance().gui.setScreen(null);
    }
}
