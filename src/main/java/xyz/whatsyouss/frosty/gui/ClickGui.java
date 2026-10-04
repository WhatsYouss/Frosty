package xyz.whatsyouss.frosty.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.gui.component.*;
import xyz.whatsyouss.frosty.gui.component.impl.CategoryComponent;
import xyz.whatsyouss.frosty.gui.component.impl.InputComponent;
import xyz.whatsyouss.frosty.gui.component.impl.KeyBindComponent;
import xyz.whatsyouss.frosty.gui.component.impl.ModuleComponent;
import xyz.whatsyouss.frosty.gui.component.impl.SelectComponent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.client.UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class ClickGui extends Screen {
    private static ClickGui instance;
    private float x, y, width, height;
    private boolean dragging;
    private float dragX, dragY;
    private static Module.category lastSelectedCategory = null;
    private Module.category selectedCategory = Module.category.Client;
    private final List<CategoryComponent> categoryComponents;
    private final List<ModuleComponent> moduleComponents;
    private KeyBindComponent listeningKeyBind;
    private InputComponent focusedInput;
    private float scrollOffset = 0f;
    private float totalModuleHeight = 0f;
    private float lastLayoutX = Float.NaN;
    private float lastLayoutY = Float.NaN;
    private float lastLayoutScroll = Float.NaN;
    private boolean layoutDirty = true;
    private static final int LIGHT_BG = 0xFFFAFAFA;
    private static final int DARK_BG = 0xFF323232;
    private static final int LIGHT_HEADER = 0xFF6464FF;
    private static final int DARK_HEADER = 0xFF3C3CB4;
    private static final int LIGHT_SIDEBAR = 0xFFE6E6E6;
    private static final int DARK_SIDEBAR = 0xFF464646;
    private static final int LIGHT_BODY = 0xFFF0F0F0;
    private static final int DARK_BODY = 0xFF3C3C3C;
    private static final String TITLE_TEXT = "Frosty " + Frosty.MOD_VERSION;

    public ClickGui() {
        super(Component.literal("Frosty"));
        instance = this;
        this.width = 450;
        this.height = 300;
        this.x = (mc.getWindow().getGuiScaledWidth() - width) / 2.0f;
        this.y = (mc.getWindow().getGuiScaledHeight() - height) / 2.0f;
        this.dragging = false;
        if (lastSelectedCategory != null) {
            this.selectedCategory = lastSelectedCategory;
        }
        this.categoryComponents = new ArrayList<>();
        this.moduleComponents = new ArrayList<>();
        this.listeningKeyBind = null;
        this.focusedInput = null;

        float categoryY = y + 25;
        for (Module.category category : Module.category.values()) {
            categoryComponents.add(new CategoryComponent(category, x + 5, categoryY, 60, 15, category == selectedCategory));
            categoryY += 20;
        }

        updateModuleComponents();
    }

    public static ClickGui getInstance() {
        if (instance == null) {
            instance = new ClickGui();
        }
        return instance;
    }

    private void updateModuleComponents() {
        Map<String, Boolean> moduleExpandedStates = new HashMap<>();
        for (ModuleComponent component : moduleComponents) {
            String moduleName = component.getModule().getName();
            moduleExpandedStates.put(moduleName, component.isExpanded());
        }

        moduleComponents.clear();
        for (Module module : ModuleManager.getModulesByCategory(selectedCategory)) {
            ModuleComponent component = new ModuleComponent(module, x + 75, y + 25, width - 80, 15);
            component.setExpanded(moduleExpandedStates.getOrDefault(module.getName(), false));
            moduleComponents.add(component);
        }
        layoutDirty = true;
        layoutModuleComponents();
    }

    private void layoutModuleComponents() {
        if (!layoutDirty && x == lastLayoutX && y == lastLayoutY && scrollOffset == lastLayoutScroll) {
            boolean changed = false;
            for (ModuleComponent component : moduleComponents) {
                if (component.needsLayout()) {
                    changed = true;
                    break;
                }
            }
            if (!changed) return;
        }
        float totalHeight = 0;
        for (ModuleComponent component : moduleComponents) {
            component.layout(x + 75, y + 25 + totalHeight - scrollOffset);
            totalHeight += component.getTotalHeight();
        }
        // An open dropdown may extend past the final row and needs scroll room.
        float dropdownExtra = 0;
        for (ModuleComponent component : moduleComponents) {
            if (component.isExpanded()) {
                for (xyz.whatsyouss.frosty.gui.component.Component setting : component.getSettingComponents()) {
                    if (setting.isVisible() && setting instanceof SelectComponent select && select.isExpanded()) {
                        dropdownExtra = Math.max(dropdownExtra,
                                setting.getY() + setting.getHeight() * (select.getOptionsLength() + 1)
                                        - (y + 25 - scrollOffset + totalHeight));
                    }
                }
            }
        }
        totalModuleHeight = totalHeight + Math.max(0, dropdownExtra);
        float clamped = Mth.clamp(scrollOffset, 0, Math.max(0, totalModuleHeight - (height - 30)));
        if (clamped != scrollOffset) {
            float delta = scrollOffset - clamped;
            scrollOffset = clamped;
            for (ModuleComponent component : moduleComponents) {
                component.layout(component.getX(), component.getY() + delta);
            }
        }
        lastLayoutX = x;
        lastLayoutY = y;
        lastLayoutScroll = scrollOffset;
        layoutDirty = false;
    }

    public void setFocusedInput(InputComponent input) {
        if (focusedInput != null && focusedInput != input) {
            focusedInput.setFocused(false);
        }
        focusedInput = input;
    }

    public void clearFocusedInput() {
        if (focusedInput != null) {
            focusedInput.setFocused(false);
            focusedInput = null;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        layoutModuleComponents();
        boolean isLight = UI.clickGuiColor.getValue() == 0;

        if (LiquidGlassStyle.isEnabled()) {
            LiquidGlassStyle.drawPanel(context, x, y, width, height);
            LiquidGlassStyle.drawHeader(context, x, y, width, 20);
            LiquidGlassStyle.drawInsetBackground(context, x + 5, y + 25, 60, height - 30,
                    !categoryComponents.isEmpty());
            LiquidGlassStyle.drawInsetBackground(context, x + 75, y + 25, width - 80, height - 30,
                    !moduleComponents.isEmpty());
        } else {
            context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), isLight ? LIGHT_BG : DARK_BG);
            context.fill((int) x, (int) y, (int) (x + width), (int) (y + 20), isLight ? LIGHT_HEADER : DARK_HEADER);
            context.fill((int) (x + 5), (int) (y + 25), (int) (x + 65), (int) (y + height - 5), isLight ? LIGHT_SIDEBAR : DARK_SIDEBAR);
            context.fill((int) (x + 75), (int) (y + 25), (int) (x + width - 5), (int) (y + height - 5), isLight ? LIGHT_BODY : DARK_BODY);
        }

        context.text(this.font, TITLE_TEXT, (int) (x + width / 2), (int) (y + 6), 0xFFFFFFFF);

        for (CategoryComponent component : categoryComponents) {
            component.render(context, mouseX, mouseY, delta);
        }

        int scissorY1 = (int) (y + 25);
        int scissorY2 = (int) (y + height - 5);
        context.enableScissor((int) x, scissorY1, (int) (x + width), scissorY2);

        float renderBottom = y + height - 5;
        ModuleComponent hoveredDescription = null;

        for (ModuleComponent component : moduleComponents) {
            if (component.getY() < renderBottom && component.getY() + component.getTotalHeight() > y + 25) {
                component.render(context, mouseX, mouseY, delta);
                if (component.isExpanded()) {
                    for (xyz.whatsyouss.frosty.gui.component.Component settingComponent : component.getSettingComponents()) {
                        if (settingComponent.isVisible() && settingComponent.getY() < renderBottom
                                && settingComponent.getY() + settingComponent.getHeight() > y + 25) {
                            component.renderSettingBackground(context, settingComponent);
                            settingComponent.render(context, mouseX, mouseY, delta);
                        }
                    }
                }
                if (!component.getModule().getDesc().isEmpty() &&
                        mouseX >= component.getX() + component.getWidth() - 25 && mouseX <= component.getX() + component.getWidth() - 5 &&
                        mouseY >= component.getY() + 2 && mouseY <= component.getY() + component.getHeight() - 2) {
                    hoveredDescription = component;
                }
            }
        }

        for (ModuleComponent component : moduleComponents) {
            if (component.isExpanded() && component.getY() < renderBottom && component.getY() + component.getTotalHeight() > y + 25) {
                for (xyz.whatsyouss.frosty.gui.component.Component settingComponent : component.getSettingComponents()) {
                    if (settingComponent.isVisible() && settingComponent instanceof SelectComponent select && select.isExpanded()
                            && settingComponent.getY() < renderBottom && settingComponent.getY() + settingComponent.getHeight() > y + 25) {
                        select.renderDropdown(context, mouseX, mouseY);
                    }
                }
            }
        }

        context.disableScissor();

        if (hoveredDescription != null) {
            ModuleComponent component = hoveredDescription;
                int descWidth = 200;
                int descX = (int) (component.getX() + component.getWidth() / 2);
                int descY = (int) (component.getY() + component.getHeight() + 5);

                List<FormattedCharSequence> wrappedText = component.descriptionLines(this.font);
                int descHeight = wrappedText.size() * 10 + 10;

                if (LiquidGlassStyle.isEnabled()) {
                    LiquidGlassStyle.drawGlass(context, descX, descY, descWidth, descHeight, 10,
                            isLight ? 0xD8678CFF : 0xD22A4A95);
                } else {
                    context.fill(descX, descY, descX + descWidth, descY + descHeight,
                            isLight ? LIGHT_HEADER : DARK_HEADER);
                }

                for (int i = 0; i < wrappedText.size(); i++) {
                    context.text(this.font, wrappedText.get(i), descX + 5, descY + 5 + (i * 10), 0xFFFFFFFF);
                }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= x + 75 && mouseX <= x + width - 5 &&
                mouseY >= y + 25 && mouseY <= y + height - 5) {

            layoutModuleComponents();

            float scrollSpeed = 15f;
            scrollOffset -= verticalAmount * scrollSpeed;

            float maxScroll = Math.max(0, totalModuleHeight - (height - 30));
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
            layoutModuleComponents();

            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        layoutModuleComponents();
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        float scale = 1.0f;
        mouseX *= scale;
        mouseY *= scale;

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
            dragging = true;
            dragX = (float) mouseX - x;
            dragY = (float) mouseY - y;
            clearFocusedInput();
            return true;
        }

        float currentModuleY = y + 25 - scrollOffset;
        float renderBottom = y + height - 5;

        for (ModuleComponent moduleComponent : moduleComponents) {
            if (moduleComponent.isExpanded() && currentModuleY < renderBottom && currentModuleY + moduleComponent.getTotalHeight() > y + 25) {
                float currentSettingY = currentModuleY + moduleComponent.getHeight() + 5;
                for (xyz.whatsyouss.frosty.gui.component.Component component : moduleComponent.getSettingComponents()) {
                    if (component.isVisible() && component instanceof SelectComponent &&
                            ((SelectComponent) component).isExpanded() &&
                            currentSettingY < renderBottom && currentSettingY + component.getHeight() > y + 25) {

                        float selectX = component.getX();
                        float selectWidth = component.getWidth();
                        float selectY = component.getY();
                        float selectDropdownHeight = ((SelectComponent) component).getOptionsLength() * component.getHeight();
                        float selectBottom = selectY + component.getHeight() + selectDropdownHeight;

                        float clickTop = Math.max(selectY, y + 25);
                        float clickBottom = Math.min(selectBottom, renderBottom);

                        if (mouseX >= selectX && mouseX <= selectX + selectWidth &&
                                mouseY >= clickTop && mouseY <= clickBottom) {
                            component.mouseClicked(mouseX, mouseY, button);
                            if (((SelectComponent) component).isClickConsumed()) {
                                layoutModuleComponents();
                                return true;
                            }
                        }
                    }
                    if (component.isVisible()) {
                        currentSettingY += component.getHeight() + 2;
                    }
                }
            }
            currentModuleY += moduleComponent.getTotalHeight();
        }

        {
            float moduleY = y + 25 - scrollOffset;
            for (ModuleComponent moduleComponent : moduleComponents) {
                if (moduleComponent.isExpanded() && moduleY < renderBottom && moduleY + moduleComponent.getTotalHeight() > y + 25) {
                    float currentSettingY = moduleY + moduleComponent.getHeight() + 5;
                    for (xyz.whatsyouss.frosty.gui.component.Component component : moduleComponent.getSettingComponents()) {
                        if (component.isVisible() && component instanceof InputComponent && currentSettingY < renderBottom && currentSettingY + component.getHeight() > y + 25) {
                            float inputX = component.getX() + component.getWidth() - 150;
                            float inputWidth = 140;
                            float inputY = component.getY();
                            float inputHeight = component.getHeight();

                            float clickTop = Math.max(inputY, y + 25);
                            float clickBottom = Math.min(inputY + inputHeight, renderBottom);

                            if (mouseX >= inputX && mouseX <= inputX + inputWidth &&
                                    mouseY >= clickTop && mouseY <= clickBottom) {
                                component.mouseClicked(mouseX, mouseY, button);
                                return true;
                            }
                        }
                        if (component.isVisible()) {
                            currentSettingY += component.getHeight() + 2;
                        }
                    }
                }
                moduleY += moduleComponent.getTotalHeight();
            }
        }

        for (CategoryComponent component : categoryComponents) {
            if (component.isHovered(mouseX, mouseY) && button == 0) {
                for (CategoryComponent cat : categoryComponents) {
                    cat.setSelected(false);
                }
                component.setSelected(true);
                selectedCategory = component.getCategory();
                lastSelectedCategory = selectedCategory;
                scrollOffset = 0;
                updateModuleComponents();
                clearFocusedInput();
                return true;
            }
        }

        currentModuleY = y + 25 - scrollOffset;
        for (ModuleComponent component : moduleComponents) {
            if (currentModuleY < renderBottom && currentModuleY + component.getTotalHeight() > y + 25) {
                float clickTop = Math.max(currentModuleY, y + 25);
                float clickBottom = Math.min(currentModuleY + component.getTotalHeight(), renderBottom);
                if (mouseY >= clickTop && mouseY <= clickBottom) {
                    component.mouseClicked(mouseX, mouseY, button);
                    layoutModuleComponents();
                    clearFocusedInput();
                    return true;
                }
            }
            currentModuleY += component.getTotalHeight();
        }

        clearFocusedInput();
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        dragging = false;
        float scale = 1.0f;
        mouseX *= scale;
        mouseY *= scale;

        for (ModuleComponent component : moduleComponents) {
            component.mouseReleased(mouseX, mouseY, button);
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        float scale = 1.0f;
        if (dragging) {
            x = (float) mouseX - dragX;
            y = (float) mouseY - dragY;

            float categoryY = y + 25;
            for (CategoryComponent component : categoryComponents) {
                component.updatePosition(x + 5, categoryY);
                categoryY += 20;
            }

            layoutModuleComponents();
            return true;
        }

        mouseX *= scale;
        mouseY *= scale;

        for (ModuleComponent component : moduleComponents) {
            component.mouseDragged(mouseX, mouseY, button, offsetX, offsetY);
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (listeningKeyBind != null) {
            listeningKeyBind.keyPressed(keyCode, scanCode, modifiers);
            listeningKeyBind = null;
            updateModuleComponents();
            return true;
        }

        for (ModuleComponent moduleComponent : moduleComponents) {
            for (xyz.whatsyouss.frosty.gui.component.Component component : moduleComponent.getSettingComponents()) {
                if (component instanceof InputComponent && ((InputComponent) component).isFocused()) {
                    if (component.keyPressed(keyCode, scanCode, modifiers)) {
                        return true;
                    }
                }
                if (component instanceof KeyBindComponent && ((KeyBindComponent) component).isListening()) {
                    if (component.keyPressed(keyCode, scanCode, modifiers)) {
                        return true;
                    }
                }
            }
        }

        if (keyCode == 256) {
            ModuleManager.ui.disable();
            clearFocusedInput();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        char chr = Character.toChars(input.codepoint())[0];
        for (ModuleComponent moduleComponent : moduleComponents) {
            for (xyz.whatsyouss.frosty.gui.component.Component component : moduleComponent.getSettingComponents()) {
                if (component instanceof InputComponent && ((InputComponent) component).isFocused()) {
                    if (((InputComponent) component).charTyped(chr, 0)) {
                        return true;
                    }
                }
            }
        }

        return super.charTyped(input);
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
        if (LiquidGlassStyle.isEnabled()) {
            return;
        }
        super.extractBlurredBackground(graphics);
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
        if (LiquidGlassStyle.isEnabled()) {
            return;
        }
        super.extractMenuBackground(graphics);
    }

    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor graphics) {
        if (LiquidGlassStyle.isEnabled()) {
            return;
        }
        super.extractTransparentBackground(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public List<ModuleComponent> getModuleComponents() {
        return moduleComponents;
    }
}
