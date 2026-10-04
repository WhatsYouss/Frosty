package xyz.whatsyouss.frosty.modules.impl.client;

import org.lwjgl.glfw.GLFW;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;

public class UngrabMouse extends Module {

    public UngrabMouse() {
        super("UngrabMouse", "释放鼠标", category.Client);
    }

    @Override
    public void onEnable() {
        long window = mc.getWindow().handle();
        // Keep MouseHandler's logical state in sync with GLFW. The vanilla
        // held-attack gate is relaxed by MinecraftMixin while this module is
        // active, so pretending that the cursor is still captured is no longer
        // necessary.
        mc.mouseHandler.releaseMouse();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    @Override
    public void onDisable() {
        long window = mc.getWindow().handle();
        if (ModuleManager.farmingMacro != null && ModuleManager.farmingMacro.isControllingMouse()) {
            mc.mouseHandler.releaseMouse();
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            return;
        }
        GLFW.glfwFocusWindow(window);
        mc.mouseHandler.grabMouse();

        // glfwFocusWindow can be asynchronous on some window managers. Keep
        // the old cursor fallback, while the next click will synchronize the
        // logical MouseHandler state if grabMouse returned early.
        if (!mc.mouseHandler.isMouseGrabbed()) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            GLFW.glfwSetCursorPos(window,
                    mc.getWindow().getWidth() / 2.0,
                    mc.getWindow().getHeight() / 2.0);
        }
    }
}
