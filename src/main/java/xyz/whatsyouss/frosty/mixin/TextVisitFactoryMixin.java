package xyz.whatsyouss.frosty.mixin;

import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.NickHider;

import java.util.regex.Matcher;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
    @ModifyArg(method = "visitFormatted(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z"), index = 0)
    private static String modifyText(String text) {
        if (!ModuleManager.nickHider.isEnabled() || ModuleManager.nickHider.name.getValue().isEmpty()) {
            return text;
        }
        return NickHider.getUsernamePattern().matcher(text).replaceAll(Matcher.quoteReplacement(ModuleManager.nickHider.name.getValue()));
    }
}
