package net.enzo.unstablenickhider.util;

import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Detection helpers for the rank/team whitelists. Ranks and teams are
 * matched case-insensitively against plain text (formatting such as
 * capitalisation, bold, italics and colours is stripped before matching).
 */
public class RankTeamWhitelist {

    /**
     * True if any of the player's rank indicators (team name, team display
     * name, team prefix, display-name prefix) matches the ranks whitelist.
     */
    public static boolean matchesRankWhitelist(net.minecraft.entity.player.PlayerEntity player) {
        List<String> ranks = UnstableNickHiderConfig.instance().renderRanksWhitelist;
        if (ranks == null || ranks.isEmpty() || player == null) {
            return false;
        }

        // 1. Scoreboard team the player is in (name, display name and prefix,
        //    e.g. LuckPerms/OP, Assistant, Trusted prefixes).
        Team team = player.getScoreboardTeam();
        if (team != null) {
            if (matchesAny(ranks, team.getName())) return true;
            if (matchesAny(ranks, team.getDisplayName())) return true;
            if (matchesAny(ranks, team.getPrefix())) return true;
        }

        // 2. Display-name prefix (many rank plugins prepend the rank to the
        //    display name, often styled with colours/bold/italic).
        Text display = player.getDisplayName();
        if (display != null) {
            String plain = stripFormatting(display);
            for (String rank : ranks) {
                if (rank == null || rank.isBlank()) continue;
                if (plain.toLowerCase().contains(rank.trim().toLowerCase())) return true;
            }
        }

        return false;
    }

    /**
     * True if the player's scoreboard team matches the teams whitelist.
     */
    public static boolean matchesTeamWhitelist(net.minecraft.entity.player.PlayerEntity player) {
        List<String> teams = UnstableNickHiderConfig.instance().renderTeamsWhitelist;
        if (teams == null || teams.isEmpty() || player == null) {
            return false;
        }

        Team team = player.getScoreboardTeam();
        if (team == null) {
            return false;
        }

        return matchesAny(teams, team.getName())
                || matchesAny(teams, team.getDisplayName());
    }

    /**
     * Entity-less variant used for skin randomization: checks the rank and
     * team whitelists via the scoreboard (by score-holder username) and the
     * tab-list display name. Null-safe on every argument.
     */
    public static boolean matchesRankOrTeamForHolder(net.minecraft.world.World world, String username, Text tabDisplayName) {
        UnstableNickHiderConfig config = UnstableNickHiderConfig.instance();

        // Tab display name often carries the rank prefix (styled or not).
        if (matchesAny(config.renderRanksWhitelist, tabDisplayName)) {
            return true;
        }

        if (world == null || username == null) {
            return false;
        }

        Team team = world.getScoreboard().getScoreHolderTeam(username);
        if (team == null) {
            return false;
        }

        return matchesAny(config.renderTeamsWhitelist, team.getName())
                || matchesAny(config.renderTeamsWhitelist, team.getDisplayName())
                || matchesAny(config.renderRanksWhitelist, team.getPrefix())
                || matchesAny(config.renderRanksWhitelist, team.getName())
                || matchesAny(config.renderRanksWhitelist, team.getDisplayName());
    }

    private static boolean matchesAny(List<String> entries, Text text) {
        if (text == null) return false;
        return matchesAny(entries, stripFormatting(text));
    }

    private static boolean matchesAny(List<String> entries, String plain) {
        if (plain == null || plain.isBlank()) return false;
        String normalized = plain.trim().toLowerCase();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) continue;
            if (normalized.equals(entry.trim().toLowerCase())
                    || normalized.contains(entry.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flattens styled text to plain text (drops colour codes, bold, italic,
     * obfuscated etc.), so "§4§lOP" and "OP" both match "op".
     */
    public static String stripFormatting(Text text) {
        return text.getString();
    }
}
