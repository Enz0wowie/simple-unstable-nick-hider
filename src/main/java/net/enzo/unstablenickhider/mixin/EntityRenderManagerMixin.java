package net.enzo.unstablenickhider.mixin;

import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.enzo.unstablenickhider.util.PlayerWhitelist;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {

    /**
     * Cancels rendering entirely for players (model, nametag, shadow, everything).
     * Only players are affected; exempt players (username/rank/team whitelist)
     * still render normally.
     */
    @Inject(
            method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void unhider$hidePlayers(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        UnstableNickHiderConfig config = UnstableNickHiderConfig.instance();
        if (!config.disablePlayers) return;
        if (!(entity instanceof PlayerEntity player)) return;
        if (PlayerWhitelist.isExemptFromRenderSettings(player)) return;

        cir.setReturnValue(false);
    }
}
