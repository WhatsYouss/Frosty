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
import xyz.whatsyouss.frosty.modules.impl.client.ChatCopier;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.utility.Utils;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!ModuleManager.chatCopier.isEnabled()) return;
        if (event.button() != 1) return;

        ChatComponent chat = mc.gui.getChat();
        Component message = findMessageAt(chat, event.x(), event.y(), mc.getWindow().getGuiScaledHeight());

        if (message != null) {
            String str = ModuleManager.chatCopier.stripColorCode.isEnabled ? Utils.stripColor(message.getString()) : message.getString();
            try {
                mc.keyboardHandler.setClipboard(str);
                if (ChatCopier.copiedMessageInChat.isToggled()) {
                    Utils.addModuleMessage(ModuleManager.chatCopier.getTransName(), "§bCopied§f: '" + str + "' §bto clipboard", "§b已将§f: '" + str + "' §b复制到剪贴板");
                } else {
                    if (UI.lang.getValue() == 1) {
                        mc.player.sendOverlayMessage(Component.literal("§b已将§f: '" + str + "' §b复制到剪贴板"));
                    } else {
                        mc.player.sendOverlayMessage(Component.literal("§bCopied§f: '" + str + "' §bto clipboard"));
                    }
                }
            } catch (Exception e) {
                Utils.addModuleMessage(ModuleManager.chatCopier.getTransName(), "§cFailed to copy '" + str + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        cir.setReturnValue(true);
    }

    private static Component findMessageAt(ChatComponent chat, double mouseX, double mouseY, int screenHeight) {
        IChatComponent access = (IChatComponent) chat;
        return access.frosty$getMessageAt(mouseX, mouseY, screenHeight);
    }
}
