package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.whatsyouss.frosty.gui.LiquidGlassStyle;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.settings.impl.KeyBindSetting;

import java.awt.*;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class KeyBindComponent extends Component {
    private final KeyBindSetting setting;
    private boolean listening;

    public KeyBindComponent(KeyBindSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        this.listening = false;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;
        isHovered = mouseX >= x + width - 70 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;

        String text = listening ? (UI.lang.getValue() == 1 ? "请按下按键" : "Listening...") : setting.getKeyText();
        context.text(mc.font, UI.lang.getValue() == 1 ? "开关" : "Bind", (int) (x + 2), (int) (y + height / 2 - 4), LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        int boxColor = isHovered ? 0xFFC8C8C8 : 0xFFB4B4B4;
        if (LiquidGlassStyle.isEnabled()) {
            LiquidGlassStyle.drawControl(context, x + width - 70, y, 60, height, listening, isHovered);
        } else {
            context.fill((int) (x + width - 70), (int) y, (int) (x + width - 10), (int) (y + height), boxColor);
        }
        context.text(mc.font, text, (int) (x + width - 65), (int) (y + height / 2 - 4), LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : isLight ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening) return false;

        if (keyCode == 256) { // ESC
            setting.getModule().setBind(0);
            listening = false;
            return true;
        }

        setting.getModule().setBind(keyCode);
        listening = false;
        return true;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x + width - 70 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height) {
            if (button == 0) { // Left click
                listening = !listening;
            } else if (button == 1 && listening) { // Right click cancels binding
                listening = false;
            }
        } else if (listening) {
            listening = false;
        }
    }

    public boolean isListening() {
        return listening;
    }
}
