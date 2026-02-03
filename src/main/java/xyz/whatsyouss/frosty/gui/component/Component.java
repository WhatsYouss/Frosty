package xyz.whatsyouss.frosty.gui.component;

import net.minecraft.client.gui.DrawContext;
import xyz.whatsyouss.frosty.settings.Setting;

public abstract class Component {
    protected float x, y, width, height;
    protected boolean isHovered;
    protected Setting setting;

    public Component(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Component(Setting setting, float x, float y, float width, float height) {
        this(x, y, width, height);
        this.setting = setting;
    }

    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);

    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);

    public abstract void mouseClicked(double mouseX, double mouseY, int button);

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void updatePosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Setting getSetting() {
        return setting;
    }

    public boolean isVisible() {
        return setting == null || setting.isVisible();
    }
}