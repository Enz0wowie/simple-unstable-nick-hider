package net.enzo.unstablenickhider.mixin;

import net.enzo.unstablenickhider.SimpleUnstableNickHider;
import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.regex.Matcher;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
    @ModifyArg(
            method = "visitFormatted(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", ordinal = 0),
            index = 0
    )
    private static String hideName(String text) {
        if (!UnstableNickHiderConfig.instance().enabled) return text;
        String replacement = UnstableNickHiderConfig.instance().getActiveNickname();
        if (replacement.isEmpty()) return text;

        return SimpleUnstableNickHider.getUsernamePattern().matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
    }
}