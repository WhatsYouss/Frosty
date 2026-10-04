package xyz.whatsyouss.frosty.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.whatsyouss.frosty.modules.impl.client.UI;

public final class LiquidGlassStyle {
    private static final float PANEL_RADIUS = 16.0f;
    private static final float CONTROL_RADIUS = 7.0f;

    private LiquidGlassStyle() {
    }

    public static boolean isEnabled() {
        return UI.liquidGlass.isToggled();
    }

    public static int textColor() {
        return isLight() ? 0xFF172033 : 0xFFF2F7FF;
    }

    public static int mutedTextColor() {
        return isLight() ? 0xFF526176 : 0xFFB8C5D8;
    }

    public static int accentColor() {
        return isLight() ? 0xFF5D81FF : 0xFF88A8FF;
    }

    public static void drawPanel(GuiGraphicsExtractor context, float x, float y,
                                 float width, float height) {
        GlassRenderer.recordPanel(context, x, y, width, height, PANEL_RADIUS, isLight());
    }

    public static void drawHeader(GuiGraphicsExtractor context, float x, float y,
                                  float width, float height) {
        SmoothRoundedRenderer.topFill(context, x + 1, y + 2, width, height, PANEL_RADIUS,
                0x18000000);
        SmoothRoundedRenderer.topFill(context, x, y, width, height, PANEL_RADIUS,
                isLight() ? 0x78678CFF : 0x8A2A4A95);
        SmoothRoundedRenderer.topBorder(context, x, y, width, height, PANEL_RADIUS,
                isLight() ? 0x70FFFFFF : 0x60CFE1FF);
        SmoothRoundedRenderer.fill(context, x + PANEL_RADIUS, y + 2,
                width - PANEL_RADIUS * 2, Math.min(3.0f, height - 4), 2.0f,
                0x1AFFFFFF);
    }

    public static void drawControl(GuiGraphicsExtractor context, float x, float y,
                                   float width, float height, boolean active,
                                   boolean hovered) {
        int color = active
                ? (isLight() ? 0x745D82F5 : 0x804D72D2)
                : (isLight() ? 0x28FFFFFF : 0x3024334D);
        if (hovered) {
            color = active
                    ? (isLight() ? 0x9A799BFF : 0xA06D94F5)
                    : (isLight() ? 0x4AE4EDFF : 0x50617AA8);
        }
        drawGlass(context, x, y, width, height, CONTROL_RADIUS, color);
    }

    public static void drawInsetBackground(GuiGraphicsExtractor context, float x, float y,
                                           float width, float height, boolean hasFirstRow) {
        SmoothRoundedRenderer.fill(context, x + 1, y + 2, width, height, CONTROL_RADIUS,
                0x18000000);
        SmoothRoundedRenderer.fill(context, x, y, width, height, CONTROL_RADIUS,
                isLight() ? 0x28FFFFFF : 0x3024334D);
        if (hasFirstRow) {
            context.enableScissor((int) Math.floor(x), (int) Math.ceil(y + CONTROL_RADIUS + 1),
                    (int) Math.ceil(x + width), (int) Math.ceil(y + height));
        }
        SmoothRoundedRenderer.border(context, x, y, width, height, CONTROL_RADIUS,
                isLight() ? 0x70FFFFFF : 0x60CFE1FF);
        if (hasFirstRow) context.disableScissor();
    }

    public static void drawControlBorder(GuiGraphicsExtractor context, float x, float y,
                                         float width, float height, int color) {
        SmoothRoundedRenderer.border(context, x, y, width, height, CONTROL_RADIUS, color);
    }

    public static void drawGlass(GuiGraphicsExtractor context, float x, float y,
                                 float width, float height, float radius, int fillColor) {
        SmoothRoundedRenderer.fill(context, x + 1, y + 2, width, height, radius,
                0x18000000);
        SmoothRoundedRenderer.fill(context, x, y, width, height, radius, fillColor);
        SmoothRoundedRenderer.border(context, x, y, width, height, radius,
                isLight() ? 0x70FFFFFF : 0x60CFE1FF);
        float highlightInset = Math.max(2.0f, (float) Math.ceil(radius * 0.5f));
        SmoothRoundedRenderer.fill(context, x + highlightInset, y + 2,
                width - highlightInset * 2, Math.min(3.0f, height - 4),
                Math.min(2.0f, radius - 2),
                0x1AFFFFFF);
    }

    private static boolean isLight() {
        return UI.clickGuiColor.getValue() == 0;
    }
}
