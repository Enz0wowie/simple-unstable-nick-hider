package net.enzo.unstablenickhider.mixin;

import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.enzo.unstablenickhider.util.OtherPlayerRender;
import net.enzo.unstablenickhider.util.PlayerWhitelist;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    /**
     * Nametags only render when EntityRenderState.displayName is non-null,
     * so null-ing it here removes the nametag entirely.
     */
    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void unhider$hideNametags(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        UnstableNickHiderConfig config = UnstableNickHiderConfig.instance();
        if (!config.disableNametags) return;
        if (entity instanceof PlayerEntity player && PlayerWhitelist.isExemptFromRenderSettings(player)) return;

        state.displayName = null;
    }

    /**
     * Locks every non-whitelisted player's head to a random look direction.
     * The absolute head angle stays fixed even while the player moves,
     * because relativeHeadYaw is re-compensated against bodyYaw every frame.
     */
    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void unhider$randomLookAngle(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        UnstableNickHiderConfig config = UnstableNickHiderConfig.instance();
        if (!config.enabled || !config.otherPlayersRandomAngle) return;
        if (!(entity instanceof PlayerEntity player)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getUuid().equals(player.getUuid())) return;
        if (PlayerWhitelist.isExemptFromRenderSettings(player)) return;

        float[] angles = OtherPlayerRender.getLockedAngles(player.getUuid());
        float relativeYaw = angles[0] - state.bodyYaw;
        relativeYaw = ((relativeYaw % 360.0f) + 540.0f) % 360.0f - 180.0f;

        state.relativeHeadYaw = relativeYaw;
        state.pitch = angles[1];
    }
}
