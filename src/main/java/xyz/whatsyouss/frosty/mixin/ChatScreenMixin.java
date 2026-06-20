package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.interfaces.IChatComponent;
import xyz.whatsyouss.frosty.utility.Utils;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!ModuleManager.chatCopier.isEnabled()) return;
        if (event.button() != 1) return;

        ChatComponent chat = mc.gui.hud.getChat();
        Component message = findMessageAt(chat, event.x(), event.y(), mc.getWindow().getGuiScaledHeight());

        if (message != null) {
            Utils.addToClipboard(ModuleManager.chatCopier.stripColorCode.isEnabled ? Utils.stripColor(message.getString()) : message.getString());
            cir.setReturnValue(true);
        }
    }

    private static Component findMessageAt(ChatComponent chat, double mouseX, double mouseY, int screenHeight) {
        IChatComponent access = (IChatComponent) chat;
        return access.frosty$getMessageAt(mouseX, mouseY, screenHeight);
    }
}