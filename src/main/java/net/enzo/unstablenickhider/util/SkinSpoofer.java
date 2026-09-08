package net.enzo.unstablenickhider.util;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.enzo.unstablenickhider.SimpleUnstableNickHider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinSpoofer {
    // Single-threaded executor: fetches run one at a time, in order,
    // so the most recently requested nickname always wins.
    private static final ExecutorService FETCH_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NickHider-SkinFetch");
        t.setDaemon(true);
        return t;
    });

    private static volatile GameProfile cachedProfile = null;
    private static volatile SkinTextures spoofedTextures = null;
    private static String lastFetchedName = "";

    public static void fetchSkin(String username) {
        if (username == null || username.isEmpty()) {
            clearCache();
            return;
        }
        FETCH_EXECUTOR.execute(() -> fetchSkinBlocking(username));
    }

    private static void fetchSkinBlocking(String username) {
        if (username.equals(lastFetchedName) && cachedProfile != null) {
            return;
        }

        lastFetchedName = username;
        cachedProfile = null;
        spoofedTextures = null;

        try {
            SimpleUnstableNickHider.LOGGER.info("Fetching skin for username: {}", username);

            // 1. Get UUID from Mojang API
            URI uuidUri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + username);
            URL uuidUrl = uuidUri.toURL();
            HttpURLConnection uuidConnection = (HttpURLConnection) uuidUrl.openConnection();
            uuidConnection.setRequestMethod("GET");
            uuidConnection.setConnectTimeout(5000);
            uuidConnection.setReadTimeout(5000);
            uuidConnection.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (uuidConnection.getResponseCode() != 200) {
                SimpleUnstableNickHider.LOGGER.warn(
                        "'{}' is not a real Minecraft account (HTTP {}). Falling back to a default Steve/Alex skin.",
                        username, uuidConnection.getResponseCode());
                lastFetchedName = "";
                return;
            }

            JsonObject uuidJson = JsonParser.parseReader(
                    new InputStreamReader(uuidConnection.getInputStream(), StandardCharsets.UTF_8)
            ).getAsJsonObject();

            String id = uuidJson.get("id").getAsString();
            String name = uuidJson.get("name").getAsString();

            UUID uuid = UUID.fromString(id.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                    "$1-$2-$3-$4-$5"
            ));

            // 2. Fetch profile with skin data from session server
            URI profileUri = URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id.replace("-", ""));
            URL profileUrl = profileUri.toURL();
            HttpURLConnection profileConnection = (HttpURLConnection) profileUrl.openConnection();
            profileConnection.setRequestMethod("GET");
            profileConnection.setConnectTimeout(5000);
            profileConnection.setReadTimeout(5000);
            profileConnection.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (profileConnection.getResponseCode() != 200) {
                SimpleUnstableNickHider.LOGGER.warn(
                        "Failed to fetch profile for username: {} (HTTP {}). Falling back to a default Steve/Alex skin.",
                        username, profileConnection.getResponseCode());
                lastFetchedName = "";
                return;
            }

            JsonObject profileJson = JsonParser.parseReader(
                    new InputStreamReader(profileConnection.getInputStream(), StandardCharsets.UTF_8)
            ).getAsJsonObject();

            // 3. In authlib 7.x (MC 1.21.11), PropertyMap is immutable -- putting
            //    into it throws UnsupportedOperationException. Build the entries
            //    in a plain HashMultimap first, then let PropertyMap copy them.
            HashMultimap<String, Property> propertyMap = HashMultimap.create();

            // 4. Add the texture properties
            if (profileJson.has("properties")) {
                var propertiesArray = profileJson.getAsJsonArray("properties");
                for (var element : propertiesArray) {
                    JsonObject prop = element.getAsJsonObject();
                    String propName = prop.get("name").getAsString();
                    String propValue = prop.get("value").getAsString();
                    if (prop.has("signature")) {
                        String signature = prop.get("signature").getAsString();
                        propertyMap.put(propName, new Property(propName, propValue, signature));
                    } else {
                        propertyMap.put(propName, new Property(propName, propValue));
                    }
                }
            }

            // 5. Create GameProfile with the property map
            GameProfile profile = new GameProfile(uuid, name, new PropertyMap(propertyMap));
            cachedProfile = profile;
            SimpleUnstableNickHider.LOGGER.info("Successfully fetched profile for username: {}", username);

            // 6. Resolve the SkinTextures ONCE on the render thread and cache them,
            //    so mixins can return them synchronously on every call.
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    client.getSkinProvider().fetchSkinTextures(profile).thenAccept(optional -> {
                        if (optional.isPresent()) {
                            spoofedTextures = optional.get();
                            SimpleUnstableNickHider.LOGGER.info("Spoofed skin resolved for '{}'", username);
                        } else {
                            SimpleUnstableNickHider.LOGGER.warn("Skin provider returned no textures for '{}'", username);
                        }
                    });
                });
            } else {
                SimpleUnstableNickHider.LOGGER.warn("Client not ready, could not resolve skin textures for '{}'", username);
            }

        } catch (Exception e) {
            SimpleUnstableNickHider.LOGGER.error("Failed to fetch skin for username: {}", username, e);
            cachedProfile = null;
            lastFetchedName = "";
        }
    }

    public static GameProfile getCachedProfile() {
        return cachedProfile;
    }

    /**
     * Returns the resolved spoofed skin, or the default skin of the spoofed profile
     * while the real textures are still loading. Returns null when nothing is spoofed.
     */
    public static SkinTextures getSpoofedTextures() {
        SkinTextures textures = spoofedTextures;
        if (textures != null) {
            return textures;
        }
        GameProfile profile = cachedProfile;
        if (profile == null) {
            return null;
        }
        return DefaultSkinHelper.getSkinTextures(profile);
    }

    /**
     * Stable default skin (Steve/Alex variant) derived from the nickname itself.
     * Used when the nickname is not a real Minecraft account, so the local
     * player's REAL skin is never shown while the mod is enabled.
     */
    public static SkinTextures getDefaultSpoofedTextures(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return null;
        }
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8));
        return DefaultSkinHelper.getSkinTextures(uuid);
    }

    public static void clearCache() {
        cachedProfile = null;
        spoofedTextures = null;
        lastFetchedName = "";
    }
}
