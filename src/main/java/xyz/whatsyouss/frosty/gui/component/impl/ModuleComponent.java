package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import xyz.whatsyouss.frosty.gui.LiquidGlassStyle;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.settings.Setting;
import xyz.whatsyouss.frosty.utility.RenderUtils;

import java.util.ArrayList;
import java.util.List;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class ModuleComponent extends Component {
    private final Module module;
    private boolean expanded;
    private final List<Component> settingComponents;
    private final boolean[] layoutVisibility;
    private final boolean[] layoutDropdownState;
    private boolean layoutExpanded;
    private float totalHeight;
    private final float moduleSpacing = 5f;
    private String cachedDescription;
    private List<FormattedCharSequence> cachedDescriptionLines = List.of();
    private static final net.minecraft.network.chat.Component HELP_ICON = net.minecraft.network.chat.Component.literal("?");

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
        layoutVisibility = new boolean[settingComponents.size()];
        layoutDropdownState = new boolean[settingComponents.size()];
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        boolean isLight = UI.clickGuiColor.getValue() == 0;
        boolean isDangerous = ModuleManager.isDangerousModule(this.module);

        int bgColor = isLight ? (module.isEnabled() ? 0xFFDCDCFF : 0xFFF0F0F0) : (module.isEnabled() ? 0xFF6464B4 : 0xFF3C3C3C);
        if (LiquidGlassStyle.isEnabled()) {
            LiquidGlassStyle.drawControl(context, x, y, width, height, module.isEnabled(), isHovered);
            if (isDangerous) {
                LiquidGlassStyle.drawControlBorder(context, x, y, width, height, 0xFFB01235);
            }
        } else {
            RenderUtils.drawBorder(context, (int) x, (int) y, (int) width, (int) height, isDangerous ? 0xFFB01235 : (isLight ? 0xFF000000 : 0xFFFFFFFF));
            context.fill((int) x + 1, (int) y + 1, (int) (x + width - 1), (int) (y + height - 1), bgColor);
            context.fill((int) x + 2, (int) (y + height), (int) (x + width - 2), (int) (y + height + 2), isLight ? 0x60000000 : 0x60FFFFFF);
        }

        context.text(mc.font, module.getTransName(), (int) (x + 5), (int) (y + height / 2 - 4), module.isHidden() ? 0xFFFF6464 : LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : isLight ? 0xFF000000 : 0xFFFFFFFF, false);

        boolean descHovered = false;
        if (!module.getDesc().isEmpty()) {
            int descButtonX = (int) (x + width - 25);
            int descButtonY = (int) y + 2;
            int descButtonWidth = 20;
            int descButtonHeight = (int) height - 4;

            descHovered = mouseX >= descButtonX && mouseX <= descButtonX + descButtonWidth &&
                    mouseY >= descButtonY && mouseY <= descButtonY + descButtonHeight;

            int descButtonColor = isLight ? (descHovered ? 0xFFC8C8C8 : 0xFFB4B4B4) : (descHovered ? 0xFF646464 : 0xFFB4B4B4);
            if (LiquidGlassStyle.isEnabled()) {
                LiquidGlassStyle.drawControl(context, descButtonX, descButtonY, descButtonWidth, descButtonHeight, false, descHovered);
            } else {
                context.fill(descButtonX, descButtonY, descButtonX + descButtonWidth, descButtonY + descButtonHeight, descButtonColor);
            }
            context.text(mc.font, HELP_ICON, descButtonX + 6, descButtonY + (descButtonHeight / 2 - 4), LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : isLight ? 0xFF000000 : 0xFFFFFFFF, false);
        }
    }

    public void renderSettingBackground(GuiGraphicsExtractor context, Component component) {
        if (LiquidGlassStyle.isEnabled()) {
            LiquidGlassStyle.drawControl(context, component.getX(), component.getY(), component.getWidth(), component.getHeight(), false, false);
        } else {
            context.fill((int) component.getX(), (int) component.getY(),
                    (int) (component.getX() + component.getWidth()), (int) (component.getY() + component.getHeight()),
                    UI.clickGuiColor.getValue() == 0 ? 0xFFE6E6E6 : 0xFF464646);
        }
    }

    public void layout(float x, float y) {
        updatePosition(x, y);
        layoutExpanded = expanded;
        float currentY = y + height + moduleSpacing;
        for (int i = 0; i < settingComponents.size(); i++) {
            Component component = settingComponents.get(i);
            boolean visible = component.isVisible();
            layoutVisibility[i] = visible;
            layoutDropdownState[i] = component instanceof SelectComponent select && select.isExpanded();
            if (expanded && visible) {
                component.updatePosition(x + 5, currentY);
                currentY += component.getHeight() + 2;
            }
        }
        totalHeight = currentY - y;
    }

    public boolean needsLayout() {
        if (expanded != layoutExpanded) return true;
        for (int i = 0; i < settingComponents.size(); i++) {
            Component component = settingComponents.get(i);
            if (component.isVisible() != layoutVisibility[i]
                    || (component instanceof SelectComponent select && select.isExpanded()) != layoutDropdownState[i]) {
                return true;
            }
        }
        return false;
    }

    public List<FormattedCharSequence> descriptionLines(Font font) {
        String description = module.getDesc();
        if (!description.equals(cachedDescription)) {
            cachedDescription = description;
            cachedDescriptionLines = font.split(net.minecraft.network.chat.Component.literal(description), 190);
        }
        return cachedDescriptionLines;
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
