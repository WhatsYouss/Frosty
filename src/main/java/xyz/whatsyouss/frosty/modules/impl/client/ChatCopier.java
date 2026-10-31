package xyz.whatsyouss.frosty.modules.impl.client;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

public class ChatCopier extends Module {

    public ButtonSetting stripColorCode;
    public static ButtonSetting copiedMessageInChat;

    public ChatCopier() {
        super("ChatCopier", "聊天复制", category.Client);

        this.registerSetting(copiedMessageInChat = new ButtonSetting("Show Copied Message in Chat", "在聊天框显示复制信息", true));
        this.registerSetting(stripColorCode = new ButtonSetting("Strip color code", "去除颜色符号", true));
    }

    @Override
    public String getDesc() {
        if (UI.lang.getValue() == 1) {
            return "右键复制聊天消息";
        }
        return "Copy chat message by right click";
    }
}
