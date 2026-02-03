package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.gui.ClickGui;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.settings.Setting;
import xyz.whatsyouss.frosty.utility.RenderUtils;

import java.util.ArrayList;
import java.util.List;

import java.awt.*;
import static xyz.whatsyouss.frosty.Frosty.mc;

public class ModuleComponent extends Component {
    private final Module module;
    private boolean expanded;
    private final List<Component> settingComponents;
    private float totalHeight;
    private final float moduleSpacing = 5f;

    public ModuleComponent(Module module, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.module = module;
        this.expanded = false;
        this.settingComponents = new ArrayList<>();
        this.totalHeight = height + moduleSpacing;

        float settingY = y + height + moduleSpacing;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof xyz.whatsyouss.frosty.settings.impl.KeyBindSetting) {
                settingComponents.add(new KeyBindComponent((xyz.whatsyouss.frosty.settings.impl.KeyBindSetting) setting,
                        x + 5, settingY, width - 10, height - 5));
                settingY += height - 5 + 2;
            } else if (setting instanceof xyz.whatsyouss.frosty.settings.impl.ButtonSetting) {
                settingComponents.add(new ButtonComponent((xyz.whatsyouss.frosty.settings.impl.ButtonSetting) setting,
                        x + 5, settingY, width - 10, height - 5));
                settingY += height - 5 + 2;
            } else if (setting instanceof xyz.whatsyouss.frosty.settings.impl.SliderSetting) {
                settingComponents.add(new SliderComponent((xyz.whatsyouss.frosty.settings.impl.SliderSetting) setting,
                        x + 5, settingY, width - 10, height - 5));
                settingY += height - 5 + 2;
            } else if (setting instanceof xyz.whatsyouss.frosty.settings.impl.SelectSetting) {
                settingComponents.add(new SelectComponent((xyz.whatsyouss.frosty.settings.impl.SelectSetting) setting,
                        x + 5, settingY, width - 10, height - 5));
                settingY += height - 5 + 2;
            } else if (setting instanceof xyz.whatsyouss.frosty.settings.impl.InputSetting) {
                settingComponents.add(new InputComponent((xyz.whatsyouss.frosty.settings.impl.InputSetting) setting,
                        x + 5, settingY, width - 10, height - 5));
                settingY += height - 5 + 2;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;

        RenderUtils.drawBorder(context, (int) x, (int) y, (int) width, (int) height, isLight ? 0xFF000000 : 0xFFFFFFFF);
        int bgColor = isLight ? (module.isEnabled() ? new Color(220, 220, 255).getRGB() : new Color(240,240, 240).getRGB()) : (module.isEnabled() ? new Color(100, 100, 180).getRGB() : new Color(60,60, 60).getRGB());
        context.fill((int) x + 1, (int) y + 1, (int) (x + width - 1), (int) (y + height - 1), bgColor);
        context.fill((int) x + 2, (int) (y + height), (int) (x + width - 2), (int) (y + height + 2), isLight ? 0x60000000 : 0x60FFFFFF);

        context.drawText(mc.textRenderer, module.getName(), (int) (x + 5), (int) (y + height / 2 - 4), isLight ? (module.isHidden() ? (new Color(255, 100, 100).getRGB()) : Color.BLACK.getRGB()) : (module.isHidden() ? (new Color(255, 100, 100).getRGB()) : Color.WHITE.getRGB()), false);

        // description
        boolean showDescription = false;
        boolean descHovered = false;
        if (!module.getDesc().isEmpty()) {
            int descButtonX = (int) (x + width - 25);
            int descButtonY = (int) y + 2;
            int descButtonWidth = 20;
            int descButtonHeight = (int) height - 4;

            descHovered = mouseX >= descButtonX && mouseX <= descButtonX + descButtonWidth &&
                    mouseY >= descButtonY && mouseY <= descButtonY + descButtonHeight;

            int descButtonColor = isLight ? (descHovered ? new Color(200, 200, 200).getRGB() : new Color(180, 180, 180).getRGB()) : (descHovered ? new Color(100, 100, 100).getRGB() : new Color(180, 180, 180).getRGB());
            context.fill(descButtonX, descButtonY, descButtonX + descButtonWidth, descButtonY + descButtonHeight, descButtonColor);
            context.drawText(mc.textRenderer, Text.literal("?"), descButtonX + 6, descButtonY + (descButtonHeight / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

            showDescription = descHovered;
        }

        // Draw expanded settings
        if (expanded) {
            float currentY = y + height + moduleSpacing;
            for (Component component : settingComponents) {
                if (component.isVisible()) {
                    component.updatePosition(x + 5, currentY);
                    context.fill((int) (x + 5), (int) currentY, (int) (x + width - 5), (int) (currentY + component.getHeight()), isLight ? new Color(230, 230, 230).getRGB() : new Color(70, 70, 70).getRGB());
                    component.render(context, mouseX, mouseY, delta);
                    currentY += component.getHeight() + 2;
                }
            }
            totalHeight = currentY - y;
        } else {
            totalHeight = height + moduleSpacing;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!module.getDesc().isEmpty()) {
            int descButtonX = (int) (x + width - 25);
            int descButtonY = (int) y + 2;
            int descButtonWidth = 20;
            int descButtonHeight = (int) height - 4;

            if (mouseX >= descButtonX && mouseX <= descButtonX + descButtonWidth &&
                    mouseY >= descButtonY && mouseY <= descButtonY + descButtonHeight) {
                return;
            }
        }

        // Check main module area, excluding description button
        if (mouseX >= x && mouseX <= x + width - (module.getDesc().isEmpty() ? 0 : 25) &&
                mouseY >= y && mouseY <= y + height) {
            if (button == 0 && module != ModuleManager.ui) {
                module.toggle();
            } else if (button == 1) {
                expanded = !expanded;
            } else if (button == 2) {
                module.setHidden(!module.isHidden());
            }
            return;
        }

        // Check settings area if expanded
        if (expanded) {
            float currentY = y + height + moduleSpacing;
            for (Component component : settingComponents) {
                if (component.isVisible()) {
                    if (mouseX >= component.getX() && mouseX <= component.getX() + component.getWidth() &&
                            mouseY >= component.getY() && mouseY <= component.getY() + component.getHeight()) {
                        component.mouseClicked(mouseX, mouseY, button);
                        break;
                    }
                    currentY += component.getHeight() + 2;
                }
            }
        }
    }

    public float getTotalHeight() {
        return totalHeight;
    }

    public float getExpandedTotalHeight() {
        if (!expanded) {
            return height + moduleSpacing;
        }
        float currentY = height + moduleSpacing;
        for (Component component : settingComponents) {
            if (component.isVisible()) {
                float componentHeight = component.getHeight();
                if (component instanceof SelectComponent && ((SelectComponent) component).isExpanded()) {
                    componentHeight += ((SelectComponent) component).getOptionsLength() * component.getHeight();
                }
                currentY += componentHeight + 2;
            }
        }
        return currentY;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public Module getModule() {
        return module;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (expanded) {
            for (Component component : settingComponents) {
                if (component instanceof SliderComponent) {
                    ((SliderComponent) component).mouseReleased(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (expanded) {
            for (Component component : settingComponents) {
                if (component instanceof SliderComponent) {
                    ((SliderComponent) component).mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
                }
            }
        }
    }

    public List<Component> getSettingComponents() {
        return settingComponents;
    }
}