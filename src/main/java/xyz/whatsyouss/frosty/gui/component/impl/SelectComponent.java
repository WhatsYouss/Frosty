package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.whatsyouss.frosty.gui.LiquidGlassStyle;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
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
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;
        boolean isLight = UI.clickGuiColor.getValue() == 0;
        isHovered = mouseX >= x + width - 100 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;

        context.text(mc.font, setting.getTransName(), (int) (x + 2), (int) (y + height / 2 - 4), LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        int boxColor = isHovered ? 0xFFC8C8C8 : 0xFFB4B4B4;
        if (LiquidGlassStyle.isEnabled()) {
            LiquidGlassStyle.drawControl(context, x + width - 100, y, 90, height, expanded, isHovered);
        } else {
            context.fill((int) (x + width - 100), (int) y, (int) (x + width - 10), (int) (y + height), boxColor);
        }

        String currentOption = setting.getTransOptions()[(int) setting.getValue()];
        context.text(mc.font, currentOption, (int) (x + width - 95), (int) (y + height / 2 - 4), LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        context.text(mc.font, expanded ? "▲" : "▼", (int) (x + width - 20), (int) (y + height / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);
    }

    public void renderDropdown(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (expanded && isVisible()) {
            boolean isLight = UI.clickGuiColor.getValue() == 0;
            if (LiquidGlassStyle.isEnabled()) {
                LiquidGlassStyle.drawGlass(context, x + width - 100, y + height, 90,
                        setting.getOptions().length * height, 10,
                        isLight ? 0xD8EAF2FF : 0xD624334D);
            } else {
                context.fill((int) (x + width - 100), (int) (y + height),
                        (int) (x + width - 10), (int) (y + height + setting.getOptions().length * height),
                        0xFF000000);
            }

            for (int i = 0; i < setting.getTransOptions().length; i++) {
                int optionY = (int) (y + height * (i + 1));
                boolean optionHovered = mouseX >= x + width - 100 && mouseX <= x + width - 10 &&
                        mouseY >= optionY && mouseY < optionY + height;

                if (LiquidGlassStyle.isEnabled()) {
                    LiquidGlassStyle.drawControl(context, x + width - 98, optionY + 1,
                            86, height - 2, false, optionHovered);
                } else {
                    context.fill((int) (x + width - 100), optionY,
                            (int) (x + width - 10), optionY + (int) height,
                            optionHovered ? 0xFF4444AA : 0xFF333333);
                }

                context.text(mc.font, setting.getTransOptions()[i],
                        (int) (x + width - 95), optionY + (int) (height / 2 - 4),
                        LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : 0xFFFFFFFF, false);
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
            for (int i = 0; i < setting.getTransOptions().length; i++) {
                int optionY = (int) (y + height * (i + 1));
                if (mouseX >= x + width - 100 && mouseX <= x + width - 10 &&
                        mouseY >= optionY && mouseY < optionY + height) {
                    setting.setValue(i);
                    expanded = false;
                    clickConsumed = true;
                    return;
                }
            }

            float selectBottom = y + height + setting.getTransOptions().length * height;
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
        return setting != null ? setting.getTransOptions().length : 0;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
