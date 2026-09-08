package net.enzo.unstablenickhider.util;

import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public class PlayerWhitelist {

    /**
     * Players on the render whitelist (matched by real username,
     * case-insensitive) are not affected by the render settings.
     */
    public static boolean isWhitelisted(PlayerEntity player) {
        return player != null && isWhitelisted(player.getGameProfile().name());
    }

    public static boolean isWhitelisted(String username) {
        List<String> whitelist = UnstableNickHiderConfig.instance().renderWhitelist;
        if (whitelist == null || whitelist.isEmpty() || username == null) {
            return false;
        }
        for (String entry : whitelist) {
            if (entry != null && entry.trim().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Full exemption check for the render features: the manual username
     * whitelist, the ranks whitelist (e.g. OP, Assistant, Trusted) and the
     * teams whitelist (e.g. RED, BLUE), all case-insensitive.
     */
    public static boolean isExemptFromRenderSettings(net.minecraft.entity.player.PlayerEntity player) {
        return isWhitelisted(player)
                || RankTeamWhitelist.matchesRankWhitelist(player)
                || RankTeamWhitelist.matchesTeamWhitelist(player);
    }
}
