package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.ui.RankTitleMenuActionPayload;
import com.github.ob_yekt.simpleskills.ui.RankTitleMenuDataPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Profession rank selector, styled to match the Rune Infusion and Fishing screens. */
public final class RankTitleMenuScreen extends Screen {
    private static RankTitleMenuScreen activeScreen;
    private record Title(String id, String name, int requiredLevel) {}

    private static final int WIDTH = 520;
    private static final int HEIGHT = 330;
    private static final int CATEGORY_ROW_HEIGHT = 13;
    private static final int TITLE_ROW_HEIGHT = 26;
    private static final int VISIBLE_TITLES = 7;

    private static final int GOLD = 0xFFE9CF7A;
    private static final int GOLD_DARK = 0xFF80612A;
    private static final int TEXT = 0xFFF3E8D2;
    private static final int MUTED = 0xFFB7A88D;
    private static final int DIM = 0xFF6F6454;
    private static final int GREEN = 0xFF67D68B;

    private final Map<String, List<Title>> titlesBySkill = new LinkedHashMap<>();
    private final Map<String, String> skillNames = new LinkedHashMap<>();
    private final Map<String, String> labels = new LinkedHashMap<>();
    private final List<String> skills = new ArrayList<>();
    private String equippedId = "";
    private String statusKey = "";
    private int selectedSkill;
    private int scroll;
    private int left;
    private int top;

    public RankTitleMenuScreen(RankTitleMenuDataPayload payload) {
        super(Component.literal(""));
        accept(payload);
    }

    public static void showOrUpdate(RankTitleMenuDataPayload payload) {
        if (activeScreen != null) {
            activeScreen.accept(payload);
            return;
        }
        activeScreen = new RankTitleMenuScreen(payload);
        Minecraft.getInstance().gui.setScreen(activeScreen);
    }

    public static void clearActive() {
        activeScreen = null;
    }

    private void accept(RankTitleMenuDataPayload payload) {
        titlesBySkill.clear();
        skillNames.clear();
        labels.clear();
        skills.clear();
        equippedId = payload.selectedId() == null ? "" : payload.selectedId();
        statusKey = switch (payload.status()) {
            case 1 -> "gui.simpleskills.rank_titles.equipped_status";
            case 2 -> "gui.simpleskills.rank_titles.cleared_status";
            case 3 -> "gui.simpleskills.rank_titles.invalid";
            case 4 -> "gui.simpleskills.rank_titles.locked";
            default -> "";
        };

        try {
            JsonObject localizedLabels = JsonParser.parseString(payload.labels()).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : localizedLabels.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) labels.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (RuntimeException ignored) { }

        if (payload.rows() == null || payload.rows().isBlank()) return;
        try {
            JsonArray rows = JsonParser.parseString(payload.rows()).getAsJsonArray();
            for (var rowElement : rows) {
                if (!rowElement.isJsonObject()) continue;
                JsonObject row = rowElement.getAsJsonObject();
                String id = row.get("id").getAsString();
                String skill = row.get("skill").getAsString();
                String skillName = row.get("skill_name").getAsString();
                String titleName = row.get("name").getAsString();
                if (id.length() > 64 || skill.length() > 24 || titleName.length() > 160) continue;
                int requiredLevel = row.get("level").getAsInt();
                if (requiredLevel < 1 || requiredLevel > 99) continue;
                if (!titlesBySkill.containsKey(skill)) {
                    titlesBySkill.put(skill, new ArrayList<>());
                    skills.add(skill);
                }
                titlesBySkill.get(skill).add(new Title(id, titleName, requiredLevel));
                skillNames.put(skill, skillName);
            }
        } catch (RuntimeException ignored) { }
        for (List<Title> titles : titlesBySkill.values()) {
            titles.sort(java.util.Comparator.comparingInt(Title::requiredLevel));
        }
        if (selectedSkill >= skills.size()) selectedSkill = Math.max(0, skills.size() - 1);
        scroll = Math.min(scroll, maxScroll());
    }

    private String label(String key) {
        return labels.getOrDefault(key, key);
    }

    public void update(RankTitleMenuDataPayload payload) {
        accept(payload);
    }

    private int panelLeft() { return left + 12; }
    private int panelTop() { return top + 48; }
    private int professionPanelWidth() { return 148; }
    private int titlePanelLeft() { return left + 168; }
    private int titlePanelRight() { return left + WIDTH - 12; }
    private int titleListTop() { return top + 68; }
    private int maxScroll() {
        List<Title> titles = selectedTitles();
        return Math.max(0, titles.size() - VISIBLE_TITLES);
    }
    private List<Title> selectedTitles() {
        if (skills.isEmpty() || selectedSkill < 0 || selectedSkill >= skills.size()) return List.of();
        return titlesBySkill.getOrDefault(skills.get(selectedSkill), List.of());
    }

    private void sendAction(int action, String id) {
        if (ClientPlayNetworking.canSend(RankTitleMenuActionPayload.TYPE)) {
            ClientPlayNetworking.send(new RankTitleMenuActionPayload(action, id));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 4, top - 4, left + WIDTH + 4, top + HEIGHT + 4, 0xCC080706);
        g.fill(left - 3, top - 3, left + WIDTH + 3, top + HEIGHT + 3, GOLD_DARK);
        g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, GOLD);
        g.fill(left, top, left + WIDTH, top + HEIGHT, 0xF217120E);
        g.fill(left + 6, top + 6, left + WIDTH - 6, top + HEIGHT - 6, 0xFF292018);

        g.centeredText(font, Component.literal(label("title")),
                width / 2, top + 10, GOLD);
        g.centeredText(font, Component.literal(label("hint")),
                width / 2, top + 25, MUTED);

        drawProfessions(g, mouseX, mouseY);
        drawTitles(g, mouseX, mouseY);
        drawEquipped(g);
        drawActions(g, mouseX, mouseY);
    }

    private void drawProfessions(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = panelLeft();
        int y = panelTop();
        int rowHeight = 13;
        g.fill(x - 2, y - 2, x + professionPanelWidth(), top + 263, 0xFF120F0C);
        g.text(font, Component.literal(label("professions")),
                x + 4, y + 3, GOLD, false);

        for (int i = 0; i < skills.size(); i++) {
            int rowY = y + 18 + i * rowHeight;
            boolean selected = i == selectedSkill;
            boolean hovered = mouseX >= x && mouseX < x + professionPanelWidth() - 4
                    && mouseY >= rowY && mouseY < rowY + rowHeight;
            int rowRight = x + professionPanelWidth() - 4;
            g.fill(x, rowY, rowRight, rowY + rowHeight - 1,
                    selected ? 0xFF49371F : hovered ? 0xFF33281C : 0xFF211A13);
            if (selected) g.fill(x, rowY, x + 2, rowY + rowHeight - 1, GOLD);

            String skill = skills.get(i);
            String name = skillNames.getOrDefault(skill, skill);
            String displayed = font.plainSubstrByWidth(name, 106);
            if (displayed.length() < name.length()) displayed += "…";
            g.text(font, Component.literal(displayed), x + 7, rowY + 2,
                    selected ? TEXT : MUTED, false);
            String count = Integer.toString(titlesBySkill.getOrDefault(skill, List.of()).size());
            g.text(font, Component.literal(count), rowRight - font.width(count) - 5, rowY + 2,
                    selected ? GOLD : DIM, false);
        }
    }

    private void drawTitles(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x0 = titlePanelLeft();
        int x1 = titlePanelRight();
        g.fill(x0 - 2, panelTop() - 2, x1 + 2, top + 258, 0xFF120F0C);
        if (skills.isEmpty()) {
            g.centeredText(font, Component.literal(label("empty")),
                    x0 + (x1 - x0) / 2, top + 125, MUTED);
            return;
        }

        String skill = skills.get(selectedSkill);
        g.text(font, Component.literal(skillNames.getOrDefault(skill, skill)), x0 + 6, top + 51, GOLD, false);
        g.text(font, Component.literal(String.format(java.util.Locale.ROOT,
                        label("unlocked_count"), selectedTitles().size())),
                x1 - 124, top + 51, MUTED, false);

        List<Title> titles = selectedTitles();
        for (int i = 0; i < VISIBLE_TITLES; i++) {
            int index = scroll + i;
            if (index >= titles.size()) break;
            Title option = titles.get(index);
            int y = titleListTop() + i * TITLE_ROW_HEIGHT;
            boolean selected = option.id().equals(equippedId);
            boolean hovered = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + TITLE_ROW_HEIGHT - 2;
            g.fill(x0, y, x1, y + TITLE_ROW_HEIGHT - 2,
                    selected ? 0xFF3B2E1C : hovered ? 0xFF33281C : 0xFF241C14);
            if (selected) g.fill(x0, y, x0 + 3, y + TITLE_ROW_HEIGHT - 2, GOLD);
            g.fill(x0 + 8, y + 6, x0 + 19, y + 17, selected ? 0xFFB8892B : GOLD_DARK);
            if (selected) g.fill(x0 + 11, y + 9, x0 + 16, y + 14, 0xFFFFF4C2);

            int rank = index + 1;
            g.text(font, Component.literal(String.format(java.util.Locale.ROOT, label("rank"), rank)),
                    x0 + 27, y + 3, MUTED, false);
            g.text(font, Component.literal(option.name()),
                    x0 + 27, y + 13, selected ? GOLD : TEXT, false);
            String level = String.format(java.util.Locale.ROOT, label("level"), option.requiredLevel());
            g.text(font, Component.literal(level), x1 - font.width(level) - 9, y + 8,
                    selected ? GREEN : MUTED, false);
        }

        if (maxScroll() > 0) {
            int trackX = x1 + 4;
            int trackY = titleListTop();
            int trackH = VISIBLE_TITLES * TITLE_ROW_HEIGHT - 2;
            g.fill(trackX, trackY, trackX + 5, trackY + trackH, 0xFF17130F);
            int thumbH = Math.max(14, trackH * VISIBLE_TITLES / titles.size());
            int thumbY = trackY + (trackH - thumbH) * scroll / maxScroll();
            g.fill(trackX, thumbY, trackX + 5, thumbY + thumbH, GOLD_DARK);
        }
    }

    private void drawEquipped(GuiGraphicsExtractor g) {
        g.fill(left + 12, top + 263, left + WIDTH - 12, top + 285, 0xFF1D1711);
        g.text(font, Component.literal(label("equipped")),
                left + 18, top + 270, MUTED, false);
        String label = equippedId.isEmpty()
                ? this.label("none") : findTitleName(equippedId);
        int labelX = left + 128;
        int maxLabelWidth = WIDTH - 150;
        String shown = font.plainSubstrByWidth(label, maxLabelWidth);
        if (shown.length() < label.length()) shown += "…";
        g.text(font, Component.literal(shown), labelX, top + 270, equippedId.isEmpty() ? DIM : GOLD, false);
        if (!statusKey.isEmpty()) {
            g.text(font, Component.literal(this.label(statusKey.substring("gui.simpleskills.rank_titles.".length()))),
                    left + 18, top + 288,
                    statusKey.endsWith("locked") || statusKey.endsWith("invalid") ? 0xFFE45A4A : GREEN, false);
        }
    }

    private String findTitleName(String id) {
        for (List<Title> options : titlesBySkill.values()) {
            for (Title option : options) if (option.id().equals(id)) return option.name();
        }
        return label("none");
    }

    private void drawActions(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int y = top + 303;
        int clearX = left + 16;
        int clearW = 120;
        int closeX = left + WIDTH - 106;
        int closeW = 90;
        drawButton(g, clearX, y, clearW, 18, "clear", mouseX, mouseY,
                !equippedId.isEmpty());
        drawButton(g, closeX, y, closeW, 18, "close", mouseX, mouseY, true);
    }

    private void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String key,
            int mouseX, int mouseY, boolean active) {
        boolean hovered = active && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, active ? GOLD_DARK : 0xFF342D24);
        g.fill(x, y, x + w, y + h, hovered ? 0xFF49371F : 0xFF241C14);
        g.centeredText(font, Component.literal(label(key)), x + w / 2, y + 5,
                active ? hovered ? GOLD : TEXT : DIM);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        double mouseX = event.x();
        double mouseY = event.y();
        int categoryX = panelLeft();
        int categoryY = panelTop() + 18;
        if (mouseX >= categoryX && mouseX < categoryX + professionPanelWidth() - 4
                && mouseY >= categoryY && mouseY < categoryY + skills.size() * CATEGORY_ROW_HEIGHT) {
            selectedSkill = Math.max(0, Math.min(skills.size() - 1,
                    (int) ((mouseY - categoryY) / CATEGORY_ROW_HEIGHT)));
            scroll = 0;
            return true;
        }

        List<Title> titles = selectedTitles();
        int listBottom = titleListTop() + VISIBLE_TITLES * TITLE_ROW_HEIGHT;
        if (mouseX >= titlePanelLeft() && mouseX < titlePanelRight() && mouseY >= titleListTop()
                && mouseY < listBottom) {
            int index = scroll + (int) ((mouseY - titleListTop()) / TITLE_ROW_HEIGHT);
            if (index >= 0 && index < titles.size()) {
                sendAction(RankTitleMenuActionPayload.SELECT, titles.get(index).id());
                statusKey = "gui.simpleskills.rank_titles.waiting";
            }
            return true;
        }

        int buttonY = top + 303;
        if (mouseY >= buttonY && mouseY < buttonY + 18) {
            if (mouseX >= left + 16 && mouseX < left + 136 && !equippedId.isEmpty()) {
                sendAction(RankTitleMenuActionPayload.CLEAR, "");
                statusKey = "gui.simpleskills.rank_titles.waiting";
                return true;
            }
            if (mouseX >= left + WIDTH - 106 && mouseX < left + WIDTH - 16) {
                onClose();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= titlePanelLeft() && mouseX <= titlePanelRight() && maxScroll() > 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (activeScreen == this) activeScreen = null;
        super.onClose();
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }
}
