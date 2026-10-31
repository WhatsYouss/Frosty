package xyz.whatsyouss.frosty.modules.impl.client;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;

public class UngrabMouse extends Module {

    public UngrabMouse() {
        super("UngrabMouse", "释放鼠标", category.Client);
    }

    @Override
    public void onEnable() {
        mc.mouseHandler.releaseMouse();
    }

    @Override
    public void onDisable() {
        if (ModuleManager.farmingMacro != null && ModuleManager.farmingMacro.isControllingMouse()) {
            mc.mouseHandler.releaseMouse();
            return;
        }
        mc.mouseHandler.grabMouse();
    }
}
