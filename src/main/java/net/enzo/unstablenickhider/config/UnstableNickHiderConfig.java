package net.enzo.unstablenickhider.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.enzo.unstablenickhider.UnstableNicknames;
import net.enzo.unstablenickhider.util.OtherPlayerRender;
import net.enzo.unstablenickhider.util.SkinSpoofer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UnstableNickHiderConfig {
    public static final ConfigClassHandler<UnstableNickHiderConfig> HANDLER = ConfigClassHandler.createBuilder(UnstableNickHiderConfig.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve("simple-unstable-nick-hider.json"))
                    .build())
            .build();

    @SerialEntry public boolean enabled = false;
    @SerialEntry public boolean useRandomNickList = false;
    @SerialEntry public String serverNickname = "";
    @SerialEntry public boolean automaticSkin = true;
    @SerialEntry public boolean hideSkin = false;
    @SerialEntry public boolean disableNametags = false;
    @SerialEntry public boolean disablePlayers = false;
    @SerialEntry public boolean otherPlayersRandomizedSkins = false;
    @SerialEntry public boolean otherPlayersRandomAngle = false;
    @SerialEntry public boolean hideFromTab = false;
    @SerialEntry public List<String> renderWhitelist = new ArrayList<>(UnstableNicknames.RENDER_WHITELIST_DEFAULTS);
    @SerialEntry public List<String> renderRanksWhitelist = new ArrayList<>(UnstableNicknames.RANKS_WHITELIST_DEFAULTS);
    @SerialEntry public List<String> renderTeamsWhitelist = new ArrayList<>(UnstableNicknames.TEAMS_WHITELIST_DEFAULTS);

    private static String cachedRandomNick = null;

    public static void load() {
        HANDLER.load();
        refreshRandomNick();
    }

    public static UnstableNickHiderConfig instance() {
        return HANDLER.instance();
    }

    /**
     * Picks a new random nick from the predetermined list (UnstableNicknames.NAMES)
     * and refreshes the skin. Called on startup and on every save.
     */
    public static void refreshRandomNick() {
        List<String> names = UnstableNicknames.NAMES;
        if (names == null || names.isEmpty()) {
            cachedRandomNick = "FallbackNick";
        } else {
            cachedRandomNick = names.get(new Random().nextInt(names.size()));
        }

        // Only fetch the nickname's skin when Automatic Skin is actually in use
        // (Hide Skin overrides it and needs no network requests at all).
        UnstableNickHiderConfig cfg = HANDLER.instance();
        if (cfg.enabled && !cfg.hideSkin && cfg.automaticSkin) {
            String activeNick = cfg.getActiveNickname();
            if (!activeNick.isEmpty()) {
                SkinSpoofer.clearCache();
                SkinSpoofer.fetchSkin(activeNick);
            }
        }
    }

    public String getActiveNickname() {
        if (!enabled) return "";
        if (useRandomNickList) {
            return cachedRandomNick != null ? cachedRandomNick : "FallbackNick";
        }
        return serverNickname;
    }

    private static List<String> parseList(String value) {
        List<String> list = new ArrayList<>();
        if (value != null) {
            for (String entry : value.split(",")) {
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    public static Screen configScreen(Screen parent) {
        return YetAnotherConfigLib.create(HANDLER, ((defaults, config, builder) -> builder
                .title(Text.translatable("config.unstablenickhider.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("config.unstablenickhider.category.settings"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.enable"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.enable.desc")))
                                .binding(defaults.enabled, () -> config.enabled, newVal -> {
                                    config.enabled = newVal;
                                    refreshRandomNick();
                                })
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.random_mode"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.random_mode.desc")))
                                .binding(defaults.useRandomNickList, () -> config.useRandomNickList, newVal -> {
                                    config.useRandomNickList = newVal;
                                    refreshRandomNick();
                                })
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.server_nick"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.server_nick.desc")))
                                .binding(defaults.serverNickname, () -> config.serverNickname, newVal -> {
                                    config.serverNickname = newVal;
                                    // Immediately refresh the skin for the new nickname
                                    refreshRandomNick();
                                })
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.automatic_skin"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.automatic_skin.desc")))
                                .binding(defaults.automaticSkin, () -> config.automaticSkin, newVal -> {
                                    config.automaticSkin = newVal;
                                    if (newVal && config.enabled && !config.hideSkin) {
                                        SkinSpoofer.clearCache();
                                        SkinSpoofer.fetchSkin(config.getActiveNickname());
                                    } else if (!newVal) {
                                        SkinSpoofer.clearCache();
                                    }
                                })
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.hide_skin"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.hide_skin.desc")))
                                .binding(defaults.hideSkin, () -> config.hideSkin, newVal -> {
                                    config.hideSkin = newVal;
                                    // Hide Skin overrides Automatic Skin and needs no fetching.
                                    SkinSpoofer.clearCache();
                                    if (!newVal && config.enabled && config.automaticSkin) {
                                        SkinSpoofer.fetchSkin(config.getActiveNickname());
                                    }
                                })
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.disable_nametags"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.disable_nametags.desc")))
                                .binding(defaults.disableNametags, () -> config.disableNametags, newVal -> config.disableNametags = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.disable_players"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.disable_players.desc")))
                                .binding(defaults.disablePlayers, () -> config.disablePlayers, newVal -> config.disablePlayers = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.render_whitelist"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.render_whitelist.desc")))
                                .binding(String.join(", ", UnstableNicknames.RENDER_WHITELIST_DEFAULTS), () -> config.renderWhitelist == null ? "" : String.join(", ", config.renderWhitelist), newVal -> {
                                    config.renderWhitelist = parseList(newVal);
                                })
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.ranks_whitelist"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.ranks_whitelist.desc")))
                                .binding(String.join(", ", UnstableNicknames.RANKS_WHITELIST_DEFAULTS), () -> config.renderRanksWhitelist == null ? "" : String.join(", ", config.renderRanksWhitelist), newVal -> {
                                    config.renderRanksWhitelist = parseList(newVal);
                                })
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.teams_whitelist"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.teams_whitelist.desc")))
                                .binding(String.join(", ", UnstableNicknames.TEAMS_WHITELIST_DEFAULTS), () -> config.renderTeamsWhitelist == null ? "" : String.join(", ", config.renderTeamsWhitelist), newVal -> {
                                    config.renderTeamsWhitelist = parseList(newVal);
                                })
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.other_random_skins"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.other_random_skins.desc")))
                                .binding(defaults.otherPlayersRandomizedSkins, () -> config.otherPlayersRandomizedSkins, newVal -> config.otherPlayersRandomizedSkins = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.other_random_angle"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.other_random_angle.desc")))
                                .binding(defaults.otherPlayersRandomAngle, () -> config.otherPlayersRandomAngle, newVal -> {
                                    config.otherPlayersRandomAngle = newVal;
                                    if (!newVal) {
                                        OtherPlayerRender.clear();
                                    }
                                })
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.unstablenickhider.option.hide_from_tab"))
                                .description(OptionDescription.of(Text.translatable("config.unstablenickhider.option.hide_from_tab.desc")))
                                .binding(defaults.hideFromTab, () -> config.hideFromTab, newVal -> config.hideFromTab = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .save(() -> {
                    HANDLER.save();
                    // Make sure skin state matches the options after EVERY save
                    refreshRandomNick();
                })
        )).generateScreen(parent);
    }
}
