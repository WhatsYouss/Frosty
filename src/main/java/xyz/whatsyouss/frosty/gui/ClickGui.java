package xyz.whatsyouss.frosty.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.gui.component.*;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.gui.component.impl.CategoryComponent;
import xyz.whatsyouss.frosty.gui.component.impl.InputComponent;
import xyz.whatsyouss.frosty.gui.component.impl.KeyBindComponent;
import xyz.whatsyouss.frosty.gui.component.impl.ModuleComponent;
import xyz.whatsyouss.frosty.gui.component.impl.SelectComponent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.client.UI;

import java.awt.*;
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
    private InputComponent focusedInput; // currently focused InputComponent
    private float scrollOffset = 0f; // scroll position
    private float totalModuleHeight = 0f; // total height of modules

    public ClickGui() {
        super(Text.literal("Frosty"));
        instance = this;
        this.width = 450;
        this.height = 300;
        this.x = (mc.getWindow().getScaledWidth() - width) / 2.0f;
        this.y = (mc.getWindow().getScaledHeight() - height) / 2.0f;
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
        float moduleY = y + 25;
        totalModuleHeight = 0;

        for (Module module : ModuleManager.getModulesByCategory(selectedCategory)) {
            ModuleComponent component = new ModuleComponent(module, x + 75, moduleY, width - 80, 15);
            component.setExpanded(moduleExpandedStates.getOrDefault(module.getName(), false));
            moduleComponents.add(component);
            totalModuleHeight += component.getExpandedTotalHeight();
        }

        float maxScroll = Math.max(0, totalModuleHeight - (height - 30));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float scale = 1.0f;
        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        // Draw GUI bg
        context.fill((int) (x / scale), (int) (y / scale), (int) ((x + width) / scale), (int) ((y + height) / scale), isLight ? new Color(250, 250, 250).getRGB() : new Color(50, 50, 50).getRGB());
        context.fill((int) (x / scale), (int) (y / scale), (int) ((x + width) / scale), (int) ((y + 20) / scale), isLight ? new Color(100, 100, 255).getRGB() : new Color(60, 60, 180).getRGB());
        context.drawCenteredTextWithShadow(mc.textRenderer, "Frosty 1.0.0", (int) ((x + width / 2) / scale), (int) ((y + 6) / scale), Color.WHITE.getRGB());

        context.fill((int) ((x + 5) / scale), (int) ((y + 25) / scale), (int) ((x + 65) / scale), (int) ((y + height - 5) / scale), isLight ? new Color(230, 230, 230).getRGB() : new Color(70, 70, 70).getRGB());
        context.fill((int) ((x + 75) / scale), (int) ((y + 25) / scale), (int) ((x + width - 5) / scale), (int) ((y + height - 5) / scale), isLight ?  new Color(240, 240, 240).getRGB() : new Color(60, 60, 60).getRGB());

        for (CategoryComponent component : categoryComponents) {
            component.render(context, mouseX, mouseY, delta);
        }

        int scissorY1 = (int) ((y + 25) / scale);
        int scissorY2 = (int) ((y + height - 5) / scale);
        context.enableScissor((int) (x / scale), scissorY1, (int) ((x + width) / scale), scissorY2);

        float moduleY = y + 25 - scrollOffset;
        float renderBottom = y + height - 5;
        Map<ModuleComponent, Boolean> showDescriptions = new HashMap<>();
        for (ModuleComponent component : moduleComponents) {

            if (moduleY < y + height - 5 && moduleY + component.getTotalHeight() > y + 25) {
                component.updatePosition(x + 75, moduleY);
                if (component.isExpanded()) {
                    float currentY = moduleY + component.getHeight() + 5;
                    for (Component settingComponent : component.getSettingComponents()) {
                        if (settingComponent.isVisible()) {
                            if (currentY < y + height - 5 && currentY + settingComponent.getHeight() > y + 25) {
                                settingComponent.updatePosition(x + 80, currentY);
                                if (!(settingComponent instanceof SelectComponent && ((SelectComponent) settingComponent).isExpanded())) {
                                    settingComponent.render(context, mouseX, mouseY, delta);
                                }
                            }
                            currentY += settingComponent.getHeight() + 2;
                        }
                    }
                }
                component.render(context, mouseX, mouseY, delta);
                // Check if description should be shown
                boolean showDescription = !component.getModule().getDesc().isEmpty() &&
                        mouseX >= component.getX() + component.getWidth() - 25 && mouseX <= component.getX() + component.getWidth() - 5 &&
                        mouseY >= component.getY() + 2 && mouseY <= component.getY() + component.getHeight() - 2;
                showDescriptions.put(component, showDescription);
            }
            moduleY += component.getTotalHeight();
        }

        // render expanded SelectComponent dropdowns
        moduleY = y + 25 - scrollOffset;
        for (ModuleComponent component : moduleComponents) {

            if (component.isExpanded() && moduleY < y + height - 5 && moduleY + component.getTotalHeight() > y + 25) {
                float currentY = moduleY + component.getHeight() + 5;
                for (Component settingComponent : component.getSettingComponents()) {
                    if (settingComponent.isVisible() && settingComponent instanceof SelectComponent && ((SelectComponent) settingComponent).isExpanded() &&
                            currentY < y + height - 5 && currentY + settingComponent.getHeight() > y + 25) {
                        settingComponent.updatePosition(x + 80, currentY);
                        settingComponent.render(context, mouseX, mouseY, delta);
                    }
                    if (settingComponent.isVisible()) {
                        currentY += settingComponent.getHeight() + 2;
                    }
                }
            }
            moduleY += component.getTotalHeight();
        }

        context.disableScissor();

        moduleY = y + 25 - scrollOffset;
        for (ModuleComponent component : moduleComponents) {

            if (moduleY < y + height - 5 && moduleY + component.getTotalHeight() > y + 25 && showDescriptions.getOrDefault(component, false)) {
                int descWidth = 200;
                int descX = (int) (component.getX() + component.getWidth() / 2);
                int descY = (int) (component.getY() + component.getHeight() + 5);

                List<OrderedText> wrappedText = mc.textRenderer.wrapLines(Text.literal(component.getModule().getDesc()), descWidth - 10);
                int descHeight = wrappedText.size() * 10 + 10;

                context.fill(descX, descY, descX + descWidth, descY + descHeight, isLight ? new Color(100, 100, 255).getRGB() : new Color(60, 60, 180).getRGB());

                for (int i = 0; i < wrappedText.size(); i++) {
                    context.drawText(mc.textRenderer, wrappedText.get(i), descX + 5, descY + 5 + (i * 10), Color.WHITE.getRGB(), false);
                }
            }
            moduleY += component.getTotalHeight();
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= x + 75 && mouseX <= x + width - 5 &&
                mouseY >= y + 25 && mouseY <= y + height - 5) {

            updateModuleComponents();

            float scrollSpeed = 15f;
            scrollOffset -= verticalAmount * scrollSpeed;

            float maxScroll = Math.max(0, totalModuleHeight - (height - 30));
            scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
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

        boolean selectClicked = false;
        float currentModuleY = y + 25 - scrollOffset;
        float renderBottom = y + height - 5;

        for (ModuleComponent moduleComponent : moduleComponents) {
            if (moduleComponent.isExpanded() && currentModuleY < renderBottom && currentModuleY + moduleComponent.getTotalHeight() > y + 25) {
                float currentSettingY = currentModuleY + moduleComponent.getHeight() + 5;
                for (Component component : moduleComponent.getSettingComponents()) {
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

        if (!selectClicked) {
            float moduleY = y + 25 - scrollOffset;
            for (ModuleComponent moduleComponent : moduleComponents) {
                if (moduleComponent.isExpanded() && moduleY < renderBottom && moduleY + moduleComponent.getTotalHeight() > y + 25) {
                    float currentSettingY = moduleY + moduleComponent.getHeight() + 5;
                    for (Component component : moduleComponent.getSettingComponents()) {
                        if (component.isVisible() && component instanceof InputComponent && currentSettingY < renderBottom && currentSettingY + component.getHeight() > y + 25) {
                            float inputX = component.getX() + component.getWidth() - 150;
                            float inputWidth = 140; // width of input box (150 - 10)
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
                    for (Component settingComponent : moduleComponents) {
                        if (settingComponent instanceof InputComponent) {
                            ((InputComponent) settingComponent).setFocused(false);
                        }
                    }
                    clearFocusedInput();
                    return true;
                }
            }
            currentModuleY += component.getTotalHeight();
        }

        // Clear focus if click
        clearFocusedInput();
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
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
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
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

            updateModuleComponents();
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
    public boolean keyPressed(KeyInput input) {
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
            for (Component component : moduleComponent.getSettingComponents()) {
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
    public boolean charTyped(CharInput input) {
        char chr = Character.toChars(input.codepoint())[0];
        int modifiers = input.modifiers();
        for (ModuleComponent moduleComponent : moduleComponents) {
            for (Component component : moduleComponent.getSettingComponents()) {
                if (component instanceof InputComponent && ((InputComponent) component).isFocused()) {
                    if (((InputComponent) component).charTyped(chr, modifiers)) {
                        return true;
                    }
                }
            }
        }

        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    public List<ModuleComponent> getModuleComponents() {
        return moduleComponents;
    }
}