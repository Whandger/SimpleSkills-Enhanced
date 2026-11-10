
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

        for (RuneInfusionClientState.Crack crack : s.cracks()) {
            if (crack.repaired()) continue;

            float progress = crack.repairRequired() <= 0
                    ? 0f
                    : Math.max(0f, Math.min(1f,
                            crack.repairProgress() / (float) crack.repairRequired()));

            int cx = itemX + 7 + Math.round(Math.max(0f, Math.min(1f, crack.x())) * 76f);
            int cy = itemY + 4 + Math.round(Math.max(0f, Math.min(1f, crack.y())) * 78f);

            int color = heldCrackId == crack.id() ? GOLD : RED;

            // O comprimento visual acompanha o crescimento real da rachadura.
            int length = Math.max(3, Math.round(12 * progress));

            g.fill(cx - 1, cy - length / 2, cx + 2, cy + length / 2 + 1, color);
            g.fill(cx - length / 2, cy - 1, cx + length / 2 + 1, cy + 2, color);

            g.fill(cx - length / 2, cy - length / 2,
                    cx - length / 2 + 2, cy - length / 2 + 2, color);

            g.fill(cx + length / 2 - 1, cy + length / 2 - 1,
                    cx + length / 2 + 2, cy + length / 2 + 2, color);

            if (heldCrackId == crack.id()) {
                g.fill(cx - 6, cy + 9, cx + 7, cy + 11, 0xFF39312A);
                int repairedWidth = Math.max(0, Math.min(13,
                        13 * (crack.repairRequired() - crack.repairProgress())
                                / Math.max(1, crack.repairRequired())));
                if (repairedWidth > 0) {
                    g.fill(cx - 6, cy + 9, cx - 6 + repairedWidth, cy + 11, GREEN);
                }
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

    private void centered(GuiGraphicsExtractor g, Component text, int y, int color) {
        g.text(font, text, width / 2 - font.width(text) / 2, y, color, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;

        RuneInfusionClientState.State state = RuneInfusionClientState.get();
        if (state == null) return false;

        int crackId = findCrackAt(event.x(), event.y(), state);
        if (crackId < 0) return false;

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
        if (event.button() == 0 && heldCrackId >= 0) {
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
        double bestDistance = 9.0 * 9.0;

        for (RuneInfusionClientState.Crack crack : state.cracks()) {
            if (crack.repaired()) continue;

            double cx = itemX + 7 + Math.max(0f, Math.min(1f, crack.x())) * 76f;
            double cy = itemY + 4 + Math.max(0f, Math.min(1f, crack.y())) * 78f;

            double dx = mouseX - cx;
            double dy = mouseY - cy;
            double distance = dx * dx + dy * dy;

            if (distance <= bestDistance) {
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
