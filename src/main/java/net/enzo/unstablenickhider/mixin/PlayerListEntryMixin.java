package net.enzo.unstablenickhider.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.enzo.unstablenickhider.util.OtherPlayerRender;
import net.enzo.unstablenickhider.util.PlayerWhitelist;
import net.enzo.unstablenickhider.util.RankTeamWhitelist;
import net.enzo.unstablenickhider.util.SkinSpoofer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @WrapMethod(method = "getSkinTextures")
    private SkinTextures unhider$spoofSkins(Operation<SkinTextures> original) {
        UnstableNickHiderConfig config = UnstableNickHiderConfig.instance();
        if (!config.enabled) {
            return original.call();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerListEntry self = (PlayerListEntry) (Object) this;
        if (client.player == null || client.getNetworkHandler() == null) {
            return original.call();
        }

        // Local player: Automatic Skin / Hide Skin logic.
        if (client.getNetworkHandler().getPlayerListEntry(client.player.getUuid()) == self) {
            String nick = config.getActiveNickname();

            // Hide Skin: always a default Steve/Alex skin, overrides Automatic Skin.
            if (config.hideSkin) {
                SkinTextures def = SkinSpoofer.getDefaultSpoofedTextures(nick);
                if (def != null) {
                    return def;
                }
                return original.call();
            }

            // Automatic Skin: skin follows the nickname.
            if (!config.automaticSkin) {
                return original.call();
            }

            SkinTextures spoofed = SkinSpoofer.getSpoofedTextures();
            if (spoofed == null && !nick.isEmpty()) {
                spoofed = SkinSpoofer.getDefaultSpoofedTextures(nick);
            }
            return spoofed != null ? spoofed : original.call();
        }

        // Other players: randomized default skins (whitelist / rank / team exempt).
        if (config.otherPlayersRandomizedSkins) {
            String profileName = self.getProfile().name();
            if (!PlayerWhitelist.isWhitelisted(profileName)
                    && !RankTeamWhitelist.matchesRankOrTeamForHolder(client.world, profileName, self.getDisplayName())) {
                return OtherPlayerRender.randomizedSkin(self.getProfile().id());
            }
        }

        return original.call();
    }
}
