package net.enzo.unstablenickhider.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.enzo.unstablenickhider.SimpleUnstableNickHider;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches the nickname pool from the Vercel API at startup, so the random
 * nickname list can be updated remotely without shipping a new mod build.
 * Falls back to the hardcoded list when the
 * fetch fails or returns nothing.
 */
public class RemoteUsernameFetcher {
    private static final String API_URL = "https://unstablenickhider-api.vercel.app/api/usernames";

    private static volatile List<String> remoteNames = null;

    public static void fetchAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = URI.create(API_URL).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() != 200) {
                    SimpleUnstableNickHider.LOGGER.warn(
                            "Username API returned HTTP {} -- using the built-in nickname list.",
                            conn.getResponseCode());
                    return;
                }

                JsonArray array = JsonParser.parseReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                ).getAsJsonArray();

                List<String> parsed = new ArrayList<>();
                for (var element : array) {
                    if (element.isJsonPrimitive() && !element.getAsString().isBlank()) {
                        parsed.add(element.getAsString().trim());
                    }
                }

                if (parsed.isEmpty()) {
                    SimpleUnstableNickHider.LOGGER.warn("Username API returned an empty list -- using the built-in nickname list.");
                    return;
                }

                remoteNames = List.copyOf(parsed);
                SimpleUnstableNickHider.LOGGER.info(
                        "Loaded {} usernames from the remote API ({}).",
                        parsed.size(), API_URL);

            } catch (Exception e) {
                SimpleUnstableNickHider.LOGGER.warn(
                        "Could not reach the username API ({}). Using the built-in nickname list.",
                        e.toString());
            }
        });
    }

    /**
     * The remote list when available, otherwise null (caller falls back to
     * the hardcoded pool).
     */
    public static List<String> getRemoteNames() {
        return remoteNames;
    }
}
