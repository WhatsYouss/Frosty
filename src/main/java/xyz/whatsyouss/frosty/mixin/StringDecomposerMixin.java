package xyz.whatsyouss.frosty.mixin;

import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.NickHider;

import java.util.regex.Matcher;

@Mixin(StringDecomposer.class)
public class StringDecomposerMixin {
    @ModifyArg(method = "iterateFormatted(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z"), index = 0)
    private static String modifyText(String text) {
        if (!ModuleManager.nickHider.isEnabled() || ModuleManager.nickHider.name.getValue().isEmpty()) {
            return text;
        }
        return NickHider.getUsernamePattern().matcher(text).replaceAll(Matcher.quoteReplacement(ModuleManager.nickHider.name.getValue()));
    }
}
