package xyz.whatsyouss.frosty.modules.impl.client;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.utility.UpdateChecker;

public class Update extends Module {
    public final ButtonSetting ignoreNotifications;

    public Update() {
        super("Update", "版本更新", category.Client);
        registerSetting(ignoreNotifications = new ButtonSetting("Ignore Update Notification", "屏蔽更新通知", false));
    }

    @Override
    public void onEnable() {
        UpdateChecker.requestManualCheck();
        disable();
    }
}
