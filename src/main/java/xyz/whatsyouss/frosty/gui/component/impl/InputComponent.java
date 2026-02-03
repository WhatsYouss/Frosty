package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.gui.ClickGui;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.InputSetting;

import java.awt.*;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class InputComponent extends Component {
    private final InputSetting setting;
    private boolean focused;

    public InputComponent(InputSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        this.focused = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) {
            return;
        }

        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;
        isHovered = mouseX >= x + width - 150 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;

        context.drawText(mc.textRenderer, setting.getName(), (int) (x + 2), (int) (y + height / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        int boxColor = focused ? new Color(200, 200, 255).getRGB() :
                (isHovered ? new Color(200, 200, 200).getRGB() : new Color(180, 180, 180).getRGB());
        context.fill((int) (x + width - 150), (int) y, (int) (x + width - 10), (int) (y + height), boxColor);

        String displayText = setting.getValue();
        if (displayText.isEmpty() && !focused) {
            displayText = setting.getPlaceholder();
            context.drawText(mc.textRenderer, Text.literal(displayText), (int) (x + width - 145), (int) (y + height / 2 - 4), Color.GRAY.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, Text.literal(displayText + (focused ? "_" : "")), (int) (x + width - 145), (int) (y + height / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible() || button != 0) return;

        if (mouseX >= x + width - 150 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height) {
            ClickGui.getInstance().setFocusedInput(this);
            focused = true;
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isVisible() || !focused) return false;

        if (keyCode == 256) { // ESC
            ClickGui.getInstance().clearFocusedInput();
            return true;
        } else if (keyCode == 259) { // Backspace
            String current = setting.getValue();
            if (!current.isEmpty()) {
                setting.setValue(current.substring(0, current.length() - 1));
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;

        String current = setting.getValue();
        if (current.length() < setting.getMaxLength()) {
            setting.setValue(current + chr);
            return true;
        }
        return false;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}