package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.interfaces.IChatComponent;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class IChatComponentMixin implements IChatComponent {
    @Unique
    private int nextId;

    @Shadow
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    private int chatScrollbarPos;

    @Shadow
    public abstract int getLinesPerPage();

    @Shadow
    public abstract boolean isChatFocused();

    @ModifyExpressionValue(method = "addMessageToQueue", at = @At(value = "CONSTANT", args = "intValue=100"))
    private int maxLength(int size) {
        return size + 32767;
    }

    @ModifyExpressionValue(method = "addMessageToDisplayQueue", at = @At(value = "CONSTANT", args = "intValue=100"))
    private int maxLengthVisible(int size) {
        return size + 32767;
    }

    @Inject(at = @At("HEAD"), method = "addMessage", cancellable = true)
    private void onAddMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag indicator, CallbackInfo ci) {
        ReceiveMessageEvent event = Frosty.EVENT_BUS.post(ReceiveMessageEvent.get(message, indicator, nextId));

        if (event.isCancelled()) ci.cancel();
    }

    @Override
    @Unique
    public Component frosty$getMessageAt(double mouseX, double mouseY, int screenHeight) {
        if (!this.isChatFocused()) return null;

        double scale = Minecraft.getInstance().options.chatScale().get();
        int testX = (int) (mouseX / scale);
        int testY = (int) (mouseY / scale);

        int chatBottom = Mth.floor((screenHeight - 40) / (float) scale);

        int messageHeight = 9;
        double chatLineSpacing = Minecraft.getInstance().options.chatLineSpacing().get();
        int entryHeight = (int) (messageHeight * (chatLineSpacing + 1.0F));

        int perPage = this.getLinesPerPage();
        int total = Math.min(this.trimmedMessages.size() - this.chatScrollbarPos, perPage);

        for (int i = 0; i < total; i++) {
            int messageIndex = i + this.chatScrollbarPos;
            GuiMessage.Line line = this.trimmedMessages.get(messageIndex);

            int entryBottom = chatBottom - i * entryHeight;
            int entryTop = entryBottom - entryHeight;

            if (testY >= entryTop && testY < entryBottom) {
                return line.parent().content();
            }
        }
        return null;
    }
}