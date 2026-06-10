package xyz.whatsyouss.frosty.modules.impl.client;

import org.lwjgl.glfw.GLFW;
import xyz.whatsyouss.frosty.modules.Module;

public class UngrabMouse extends Module {

    public UngrabMouse() {
        super("UngrabMouse", category.Client);
    }

    @Override
    public void onEnable() {
        long window = mc.getWindow().getHandle();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    @Override
    public void onDisable() {
        long window = mc.getWindow().getHandle();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(window, mc.getWindow().getWidth() / 2.0, mc.getWindow().getHeight() / 2.0);
        GLFW.glfwFocusWindow(window);
    }
}