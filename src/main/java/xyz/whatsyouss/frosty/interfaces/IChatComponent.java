package xyz.whatsyouss.frosty.interfaces;

import net.minecraft.network.chat.Component;

public interface IChatComponent {
    Component frosty$getMessageAt(double mouseX, double mouseY, int screenHeight);
}