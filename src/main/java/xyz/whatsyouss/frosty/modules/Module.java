package xyz.whatsyouss.frosty.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.MouseScrollEvent;
import xyz.whatsyouss.frosty.modules.impl.render.FreeLook;
import xyz.whatsyouss.frosty.settings.Setting;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.KeyBindSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class Module {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    protected ArrayList<Setting> settings;
    private String moduleName, moduleCnName;
    private Module.category moduleCategory;
    private boolean enabled;
    private int keycode;
    private boolean isToggled = false;
    public boolean canBeEnabled = true;
    public boolean ignoreOnSave = false;
    public boolean hidden = false;
    public boolean closetModule = false;
    public boolean alwaysOn = false;
    public String lastInfo;
    public static boolean sort;
    public static List<String> categoriesString = new ArrayList<>();
    private boolean bindingKey = false;
    public boolean shouldEnable;

    static {
        for (category cat : category.values()) {
            categoriesString.add(cat.name());
        }
    }

    public Module(String name, Module.category moduleCategory) {
        this.moduleName = name;
        this.moduleCategory = moduleCategory;
        this.keycode = 0;
        this.enabled = false;
        mc = MinecraftClient.getInstance();
        this.settings = new ArrayList();
        this.registerSetting(new KeyBindSetting(this));
    }

    public Module(String moduleName, Module.category moduleCategory, int keycode) {
        this.moduleName = moduleName;
        this.moduleCategory = moduleCategory;
        this.keycode = keycode;
        this.enabled = false;
        mc = MinecraftClient.getInstance();
        this.settings = new ArrayList();
        this.registerSetting(new KeyBindSetting(this));
    }

    public Module(String name, String cnName, Module.category moduleCategory) {
        this.moduleName = name;
        this.moduleCnName = cnName;
        this.moduleCategory = moduleCategory;
        this.keycode = 0;
        this.enabled = false;
        mc = MinecraftClient.getInstance();
        this.settings = new ArrayList();
        this.registerSetting(new KeyBindSetting(this));
    }

    public Module(String moduleName, String cnName, Module.category moduleCategory, int keycode) {
        this.moduleName = moduleName;
        this.moduleCnName = cnName;
        this.moduleCategory = moduleCategory;
        this.keycode = keycode;
        this.enabled = false;
        mc = MinecraftClient.getInstance();
        this.settings = new ArrayList();
        this.registerSetting(new KeyBindSetting(this));
    }

    public static Module getModule(Class<? extends Module> a) {
        Iterator var1 = ModuleManager.modules.iterator();

        Module module;
        do {
            if (!var1.hasNext()) {
                return null;
            }

            module = (Module)var1.next();
        } while(module.getClass() != a);

        return module;
    }

    public String getInfo() {
        return "";
    }

    public String getDesc() {
        return "";
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return this.moduleName;
    }

    public String getTransName() {
        if (this.moduleCnName != null && !this.moduleCnName.isEmpty()) {
            return this.moduleCnName;
        }
        return this.moduleName;
    }

    public void enable() {
        if (this.isEnabled()) {
            return;
        }
        this.setEnabled(true);
        ModuleManager.organizedModules.add(this);
        if (!alwaysOn) {
            Frosty.EVENT_BUS.subscribe(this);
        }
        this.onEnable();
    }

    public void disable() {
        if (!this.isEnabled()) {
            return;
        }
        this.setEnabled(false);
        ModuleManager.organizedModules.remove(this);
        if (!alwaysOn) {
            Frosty.EVENT_BUS.unsubscribe(this);
        }
        this.onDisable();
    }

    public String getNameInHud() {
        return this.moduleName;
    }

    public ArrayList<Setting> getSettings() {
        return this.settings;
    }

    public void registerSetting(Setting Setting) {
        this.settings.add(Setting);
    }

    public Module.category moduleCategory() {
        return this.moduleCategory;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void toggle() {
        if (this.isEnabled()) {
            this.disable();
        } else {
            this.enable();
        }
        Frosty.EVENT_BUS.post(new SettingUpdateEvent());
    }

    public void onKeyBind() {
        if (this.keycode == 0) return;
        try {
            long windowHandle = mc.getWindow().getHandle();
            if (windowHandle == 0) return; // Skip if window is invalid

            boolean isPressed;
            if (this.keycode == 1069 || this.keycode == 1070) {
                // Mouse scroll detection
                isPressed = isScrollDown(this.keycode);
            } else if (this.keycode >= 1000) {
                // Mouse button detection
                isPressed = GLFW.glfwGetMouseButton(windowHandle, this.keycode - 1000) == GLFW.GLFW_PRESS;
            } else {
                // Keyboard detection
                isPressed = GLFW.glfwGetKey(windowHandle, this.keycode) == GLFW.GLFW_PRESS;
            }
            if (this.keycode == ModuleManager.freeLook.getKeycode() && ModuleManager.freeLook.mode.getValue() == 1) {
                return;
            }

            if (!this.isToggled && isPressed) {
                this.toggle();
                this.isToggled = true;
            } else if (!isPressed) {
                this.isToggled = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.addChatMessage("Keybind check failed. Reset to NONE");
            this.keycode = 0;
        }
    }

    public static boolean isScrollDown(int key) {
        if (key == 1069) {
            return MouseScrollEvent.getCurrentScrollDelta() > 0; // Scroll up
        } else if (key == 1070) {
            return MouseScrollEvent.getCurrentScrollDelta() < 0; // Scroll down
        }
        return false;
    }

    public void onUpdate() {}

    public void guiUpdate() {}

    public void guiButtonToggled(ButtonSetting b) {}

    public void onSlide(SliderSetting setting) {}

    public int getKeycode() {
        return this.keycode;
    }

    public boolean isBindingKey() {
        return bindingKey;
    }

    public void setBindingKey(boolean binding) {
        this.bindingKey = binding;
    }

    public String getKeybindText() {
        if (bindingKey) return "Listening...";
        if (keycode == 0) return "None";
        if (keycode >= 1000) {
            if (keycode == 1069) return "Scroll Up";
            if (keycode == 1070) return "Scroll Down";
            return "Mouse " + (keycode - 1000);
        }
        InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(keycode);
        return key.getLocalizedText().getString();
    }

    public void setBind(int keybind) {
        this.keycode = keybind;
        Frosty.EVENT_BUS.post(new SettingUpdateEvent());
    }

    public static enum category {
        Combat, Movement, Render, Other, Client, Fishing, Foraging, Hunting, Mining, Farming
    }
}