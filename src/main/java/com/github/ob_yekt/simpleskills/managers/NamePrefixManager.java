package com.github.ob_yekt.simpleskills.managers;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.ui.RankTitleManager;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;

/**
 * Maintains per-player scoreboard decorations for prestige, Ironman, and the selected profession title.
 */
public class NamePrefixManager {
    private static final String TEAM_PREFIX = "simpleskills_prefix_";

    public static void updatePlayerNameDecorations(ServerPlayer player) {
        if (player == null) return;
        String playerUuid = player.getStringUUID();
        DatabaseManager db = DatabaseManager.getInstance();
        boolean isIronman = db.isPlayerInIronmanMode(playerUuid);
        int prestige = db.getPrestige(playerUuid);
        String selectedTitleId = db.getSelectedTitleKey(playerUuid);
        RankTitleManager.TitleOption selectedTitle = RankTitleManager.find(selectedTitleId);
        if (selectedTitleId != null && !RankTitleManager.isUnlocked(player, selectedTitle)) {
            db.setSelectedTitleKey(playerUuid, null);
            selectedTitle = null;
        }

        String starPart = prestige > 0 ? ("§6★" + prestige + " §f") : "";
        String skullPart = isIronman ? "§c§l☠ §f" : "";

        String prefix = skullPart + starPart;

        ServerScoreboard scoreboard = Objects.requireNonNull(player.level().getServer()).getScoreboard();
        String teamName = TEAM_PREFIX + player.getUUID().toString().substring(0, 16);
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setAllowFriendlyFire(false);
            team.setSeeFriendlyInvisibles(true);
        }

        team.setPlayerPrefix(Component.literal(prefix));
        Component suffix = selectedTitle == null ? Component.empty()
                : Component.literal(" (").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(RankTitleManager.getLocalizedTitle(player, selectedTitle))
                                .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
        team.setPlayerSuffix(suffix);
        team.setColor(Optional.of(TeamColor.WHITE));

        // Ensure membership
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);

        Simpleskills.LOGGER.debug("Updated name decorations for {} (title: {})",
                player.getName().getString(), selectedTitle == null ? "none" : selectedTitle.id());
    }
}


