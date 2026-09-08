package net.enzo.unstablenickhider.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.enzo.unstablenickhider.SimpleUnstableNickHider;
import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Unique
    private static boolean unhider$sampleLogged = false;

    /**
     * Removes your row from the tab list. Covers vanilla real entries (UUID)
     * and tab-plugin fake entries (random profile + team-prefixed text) by
     * matching your UUID, username, nickname and the row's raw rendered text
     * with formatting codes stripped (per-character gradients included).
     */
    @WrapMethod(method = "collectPlayerEntries()Ljava/util/List;")
    private List<PlayerListEntry> unhider$hideSelfFromTab(Operation<List<PlayerListEntry>> original) {
        List<PlayerListEntry> entries = original.call();

        UnstableNickHiderConfig config = UnstableNickHiderConfig.instance();
        if (!config.enabled || !config.hideFromTab) {
            return entries;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return entries;
        }

        UUID selfId = client.player.getUuid();
        String realName = client.getSession() != null ? client.getSession().getUsername() : null;
        String nick = config.getActiveNickname();

        List<PlayerListEntry> filtered = new ArrayList<>(entries.size());
        List<String> sampleTexts = unhider$sampleLogged ? null : new ArrayList<>();
        for (PlayerListEntry entry : entries) {
            if (unhider$isSelfEntry(entry, selfId, realName, nick, sampleTexts)) {
                continue;
            }
            filtered.add(entry);
        }

        if (sampleTexts != null) {
            unhider$sampleLogged = true;
            SimpleUnstableNickHider.LOGGER.info(
                    "[TabDebug] hideFromTab active: {} entries checked, {} rows kept. Sample row texts (formatting stripped): {}",
                    entries.size(), filtered.size(), sampleTexts);
        }
        return filtered;
    }

    /**
     * Safety net: if a row still renders a name containing your real name or
     * nickname after the entry filter, blank the name text entirely.
     */
    @WrapMethod(method = "getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;")
    private Text unhider$blankSelfName(PlayerListEntry entry, Operation<Text> original) {
        Text result = original.call(entry);

        UnstableNickHiderConfig config = UnstableNickHiderConfig.instance();
        if (!config.enabled || !config.hideFromTab) {
            return result;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return result;
        }

        String realName = client.getSession() != null ? client.getSession().getUsername() : null;
        String nick = config.getActiveNickname();
        String clean = unhider$stripCodes(result.getString()).toLowerCase();

        if (unhider$isSelfEntryBasic(entry, client.player.getUuid(), realName, nick)
                || (realName != null && !realName.isEmpty() && clean.contains(realName.toLowerCase()))
                || (nick != null && !nick.isEmpty() && clean.contains(nick.toLowerCase()))) {
            return Text.empty();
        }

        return result;
    }

    @Unique
    private boolean unhider$isSelfEntry(PlayerListEntry entry, UUID selfId, String realName, String nick, List<String> sampleTexts) {
        // 1. Real entry with your own UUID.
        if (entry.getProfile().id().equals(selfId)) {
            return true;
        }

        String realTrimmed = realName == null ? "" : realName.trim();
        String nickTrimmed = nick == null ? "" : nick.trim();

        // 2. Profile name (codes stripped).
        String profileClean = unhider$stripCodes(entry.getProfile().name());
        if (!realTrimmed.isEmpty() && profileClean.equalsIgnoreCase(realTrimmed)) {
            return true;
        }
        if (!nickTrimmed.isEmpty() && profileClean.equalsIgnoreCase(nickTrimmed)) {
            return true;
        }

        // 3. Row text exactly as vanilla composes it for drawing:
        //    team prefix + name + suffix. TAB layout entries show the player
        //    name through the team prefix, so this covers them.
        String raw = "";
        Team team = entry.getScoreboardTeam();
        if (team != null) {
            String prefix = team.getPrefix() == null ? "" : team.getPrefix().getString();
            String suffix = team.getSuffix() == null ? "" : team.getSuffix().getString();
            raw = prefix + entry.getProfile().name() + suffix;
        }

        String rawClean = unhider$stripCodes(raw);
        String cleanLower = rawClean.toLowerCase();
        if (!realTrimmed.isEmpty() && cleanLower.contains(realTrimmed.toLowerCase())) {
            return true;
        }
        if (!nickTrimmed.isEmpty() && cleanLower.contains(nickTrimmed.toLowerCase())) {
            return true;
        }

        if (sampleTexts != null && sampleTexts.size() < 10) {
            sampleTexts.add("[entry=" + profileClean + ", row=" + rawClean + "]");
        }
        return false;
    }

    @Unique
    private static boolean unhider$isSelfEntryBasic(PlayerListEntry entry, UUID selfId, String realName, String nick) {
        if (entry.getProfile().id().equals(selfId)) {
            return true;
        }
        String profileClean = unhider$stripCodes(entry.getProfile().name());
        if (realName != null && !realName.isEmpty() && profileClean.equalsIgnoreCase(realName.trim())) {
            return true;
        }
        return nick != null && !nick.isEmpty() && profileClean.equalsIgnoreCase(nick.trim());
    }

    @Unique
    private static String unhider$stripCodes(String s) {
        if (s == null) return "";
        // Strips colour/obfuscation codes and "§x" hex sequences, so gradient
        // per-character formatted names still match.
        return s.replaceAll("§[0-9A-FK-ORXa-fk-orx]", "").trim();
    }
}
