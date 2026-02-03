package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

import java.awt.*;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class ButtonComponent extends Component {
    private final ButtonSetting setting;

    public ButtonComponent(ButtonSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x + width - 60 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) {
            return;
        }
        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;
        int toggleX = (int) (x + width - 60);
        int toggleWidth = 50;
        int toggleColor = setting.isToggled() ? 0xFF00AA00 : 0xFFAA0000;
        isHovered = mouseX >= x + width - 60 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;

        context.drawText(mc.textRenderer, setting.getName(), (int) (x + 2), (int) (y + height / 2 - 4), isLight ? 0xFF000000 : 0xFFFFFFFF, false);

        context.fill(toggleX, (int) y, toggleX + toggleWidth, (int) (y + height), isLight ? 0xFF000000 : 0xFFFFFFFF);
        context.fill(toggleX + 1, (int) y + 1, toggleX + toggleWidth - 1, (int) (y + height - 1), toggleColor);

        String toggleText = setting.isToggled() ? "ON" : "OFF";
        int textWidth = mc.textRenderer.getWidth(toggleText);
        context.drawText(mc.textRenderer, toggleText,
                toggleX + (toggleWidth - textWidth) / 2, (int) (y + height / 2 - 4), 0xFFFFFFFF, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible()) {
            return;
        }

        if (isHovered(mouseX, mouseY)) {
            if (setting.isMethodButton) {
                setting.runMethod();
            } else {
                setting.toggle();
            }
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}