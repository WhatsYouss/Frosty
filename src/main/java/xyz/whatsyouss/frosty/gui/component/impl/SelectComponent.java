package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;

import java.awt.*;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class SelectComponent extends Component {
    private final SelectSetting setting;
    private boolean expanded;
    private boolean clickConsumed;

    public SelectComponent(SelectSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        this.expanded = false;
        this.clickConsumed = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;
        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;
        isHovered = mouseX >= x + width - 100 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;

        context.drawText(mc.textRenderer, setting.getName(), (int) (x + 2), (int) (y + height / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        int boxColor = isHovered ? new Color(200, 200, 200).getRGB() : new Color(180, 180, 180).getRGB();
        context.fill((int) (x + width - 100), (int) y, (int) (x + width - 10), (int) (y + height), boxColor);

        String currentOption = setting.getOptions()[(int) setting.getValue()];
        context.drawText(mc.textRenderer, Text.literal(currentOption), (int) (x + width - 95), (int) (y + height / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        context.drawText(mc.textRenderer, Text.literal(expanded ? "▲" : "▼"), (int) (x + width - 20), (int) (y + height / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        if (expanded) {
            context.fill((int) (x + width - 100), (int) (y + height),
                    (int) (x + width - 10), (int) (y + height + setting.getOptions().length * height),
                    0xFF000000);

            for (int i = 0; i < setting.getOptions().length; i++) {
                int optionY = (int) (y + height * (i + 1));
                boolean optionHovered = mouseX >= x + width - 100 && mouseX <= x + width - 10 &&
                        mouseY >= optionY && mouseY < optionY + height;

                context.fill((int) (x + width - 100), optionY,
                        (int) (x + width - 10), optionY + (int) height, // -1
                        optionHovered ? 0xFF4444AA : 0xFF333333);

                context.drawText(mc.textRenderer, Text.literal(setting.getOptions()[i]),
                        (int) (x + width - 95), optionY + (int) (height / 2 - 4),
                        0xFFFFFFFF, false);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible() || button != 0) return;

        clickConsumed = false;

        if (mouseX >= x + width - 100 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height) {
            expanded = !expanded;
            clickConsumed = true;
            return;
        }

        if (expanded) {
            for (int i = 0; i < setting.getOptions().length; i++) {
                int optionY = (int) (y + height * (i + 1));
                if (mouseX >= x + width - 100 && mouseX <= x + width - 10 &&
                        mouseY >= optionY && mouseY < optionY + height) {
                    setting.setValue(i);
                    expanded = false;
                    clickConsumed = true;
                    return;
                }
            }

            float selectBottom = y + height + setting.getOptions().length * height;
            if (mouseX >= x + width - 100 && mouseX <= x + width - 10 &&
                    mouseY >= y + height && mouseY <= selectBottom) {
                expanded = false;
                clickConsumed = true;
                return;
            }
        }
    }

    public boolean isClickConsumed() {
        return clickConsumed;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public int getOptionsLength() {
        return setting != null ? setting.getOptions().length : 0;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}