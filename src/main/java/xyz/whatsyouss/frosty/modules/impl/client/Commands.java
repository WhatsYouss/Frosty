package xyz.whatsyouss.frosty.modules.impl.client;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

public class Commands extends Module {

    public ButtonSetting copiedMessageInChat;

    public Commands() {
        super("Commands", "指令", category.Client);

        this.registerSetting(copiedMessageInChat = new ButtonSetting("Show Copied Message in Chat", "在聊天框显示复制信息", true));
    }

    @Override
    public String getDesc() {
        if (UI.lang.getValue() == 1) {
            return "客户端指令 (.help)";
        }
        return "Client commands (.help)";
    }
}