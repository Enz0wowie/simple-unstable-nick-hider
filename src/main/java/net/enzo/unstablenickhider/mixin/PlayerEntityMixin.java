package net.enzo.unstablenickhider.mixin;

import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void modifyName(CallbackInfoReturnable<Text> cir) {
        if (UnstableNickHiderConfig.instance().enabled) {
            PlayerEntity self = (PlayerEntity) (Object) this;
            MinecraftClient client = MinecraftClient.getInstance();

            // Check if this is the local player
            if (client.player != null && self.getUuid().equals(client.player.getUuid())) {
                String activeNick = UnstableNickHiderConfig.instance().getActiveNickname();
                if (!activeNick.isEmpty()) {
                    cir.setReturnValue(Text.literal(activeNick));
                }
            }
        }
    }
}