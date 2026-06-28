package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import xyz.whatsyouss.frosty.events.impl.MouseScrollEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.mixin.accessor.AbstractContainerScreenAccessor;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.Utils;

public class ScrollableTooltips extends Module {

    private static final float SCROLL_SPEED = 10f;
    private static final float ZOOM_SPEED = 0.1f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;

    private float offset = 0f;
    private float scale = 1f;

    private Screen lastScreen = null;

    public ScrollableTooltips() {
        super("ScrollableTooltips", "可滚动物品提示", category.Render);
    }

    @Override
    public String getDesc() {
        return "Scroll adjust position\n\nCtrl + Scroll adjust size";
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        Screen current = mc.gui.screen();
        if (current != lastScreen) {
            offset = 0f;
            scale = 1f;
            lastScreen = current;
        }
    }

    @EventHandler
    public void onMouseScroll(MouseScrollEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!isHoveringItemTooltip()) return;

        boolean ctrlDown = GLFW.glfwGetKey(
                mc.getWindow().handle(),
                GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(
                mc.getWindow().handle(),
                GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;

        if (ctrlDown) {
            scale += (float) (event.value * ZOOM_SPEED);
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        } else {
            offset += (float) (event.value * SCROLL_SPEED);
        }

        event.setCancelled(true);
    }

    private boolean isHoveringItemTooltip() {
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }

        Slot hoveredSlot = ((AbstractContainerScreenAccessor) screen).frosty$getHoveredSlot();
        return hoveredSlot != null && hoveredSlot.hasItem();
    }

    public float getOffset() {
        return offset;
    }

    public float getScale() {
        return scale;
    }

    @Override
    public void onDisable() {
        offset = 0f;
        scale = 1f;
        lastScreen = null;
    }
}
