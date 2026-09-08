package net.enzo.unstablenickhider.util;

import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Helpers for the "Other Players" render features: randomized default
 * skins and locked random look directions for non-whitelisted players.
 */
public class OtherPlayerRender {

    // Stable random look direction per player UUID: {yaw, pitch}
    private static final Map<UUID, float[]> LOCKED_ANGLES = new ConcurrentHashMap<>();

    /**
     * Stable per-player default skin (Steve or Alex, consistent per UUID).
     */
    public static SkinTextures randomizedSkin(UUID uuid) {
        return DefaultSkinHelper.getSkinTextures(uuid);
    }

    /**
     * Locked random look direction {yaw, pitch} for this player, stable for
     * the whole session. The head stays locked even while the player moves.
     */
    public static float[] getLockedAngles(UUID uuid) {
        return LOCKED_ANGLES.computeIfAbsent(uuid, id -> new float[]{
                ThreadLocalRandom.current().nextFloat() * 360.0f,
                ThreadLocalRandom.current().nextFloat() * 120.0f - 60.0f
        });
    }

    public static void clear() {
        LOCKED_ANGLES.clear();
    }
}
