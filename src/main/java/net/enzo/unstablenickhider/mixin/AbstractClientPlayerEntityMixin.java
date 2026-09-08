package net.enzo.unstablenickhider.mixin;

import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.enzo.unstablenickhider.util.SkinSpoofer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void unstableNickHider$spoofLocalSkin(CallbackInfoReturnable<SkinTextures> cir) {
        UnstableNickHiderConfig config = UnstableNickHiderConfig.instance();
        if (!config.enabled) return;

        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.player.getUuid().equals(self.getUuid())) return;

        // Hide Skin: always a default Steve/Alex skin, overrides Automatic Skin.
        if (config.hideSkin) {
            String nick = config.getActiveNickname();
            SkinTextures def = SkinSpoofer.getDefaultSpoofedTextures(nick);
            if (def != null) {
                cir.setReturnValue(def);
            }
            return;
        }

        // Automatic Skin: skin follows the nickname.
        if (!config.automaticSkin) return;

        String nick = config.getActiveNickname();
        SkinTextures spoofed = SkinSpoofer.getSpoofedTextures();
        if (spoofed == null && !nick.isEmpty()) {
            spoofed = SkinSpoofer.getDefaultSpoofedTextures(nick);
        }
        if (spoofed != null) {
            cir.setReturnValue(spoofed);
        }
    }
}
