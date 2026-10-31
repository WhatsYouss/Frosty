package xyz.whatsyouss.frosty.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.Setting;
import xyz.whatsyouss.frosty.settings.impl.*;
import xyz.whatsyouss.frosty.modules.Module;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConfigManager {
    private static final Path CONFIG_DIR = Minecraft.getInstance().gameDirectory.toPath().resolve("config/Frosty/");
    private static final Path CAPE_DIR = Minecraft.getInstance().gameDirectory.toPath().resolve("config/Frosty/cape");
    private static final Path DEFAULT_CONFIG = CONFIG_DIR.resolve("default.json");
    private static final Path SERVER_CONFIG = CONFIG_DIR.resolve("servers.json");
    private static final Path WAYPOINTS_DIR = CONFIG_DIR.resolve("FarmingWaypoints");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static JsonObject serverConfig;
    private static boolean firstDefaultConfigMissing;
    private static boolean languagePromptCompleted;
    private static boolean loadingConfig;

    public static boolean isLoadingConfig() {
        return loadingConfig;
    }


    public static void createConfigDir() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.createDirectories(CAPE_DIR);
            Files.createDirectories(WAYPOINTS_DIR);
        } catch (IOException e) {
            System.err.println("Failed to create config directories: " + e.getMessage());
        }
    }


    public static int validateWaypoints(List<double[]> waypoints) {
        for (int i = 1; i < waypoints.size(); i++) {
            double[] prev = waypoints.get(i - 1);
            double[] curr = waypoints.get(i);
            boolean sameX = Math.abs(prev[0] - curr[0]) < 0.5;
            boolean sameZ = Math.abs(prev[2] - curr[2]) < 0.5;
            if (!sameX && !sameZ) {
                return i + 1;
            }
        }
        return -1;
    }


    public static boolean saveWaypoints(String name, List<double[]> waypoints) {
        Path file = WAYPOINTS_DIR.resolve(name + ".json");
        JsonArray array = new JsonArray();
        for (double[] wp : waypoints) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", wp[0]);
            obj.addProperty("y", wp[1]);
            obj.addProperty("z", wp[2]);
            array.add(obj);
        }
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(array, writer);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save waypoints '" + name + "': " + e.getMessage());
            return false;
        }
    }


    public static List<double[]> loadWaypoints(String name) {
        Path file = WAYPOINTS_DIR.resolve(name + ".json");
        if (!Files.exists(file)) {
            System.err.println("Waypoint file not found: " + name);
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement element = gson.fromJson(reader, JsonElement.class);
            if (element == null || !element.isJsonArray()) return null;

            List<double[]> waypoints = new ArrayList<>();
            for (JsonElement el : element.getAsJsonArray()) {
                JsonObject obj = el.getAsJsonObject();
                waypoints.add(new double[]{
                        obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(),
                        obj.get("z").getAsDouble()
                });
            }
            return waypoints;
        } catch (IOException e) {
            System.err.println("Failed to load waypoints '" + name + "': " + e.getMessage());
            return null;
        }
    }


    public static void loadServerConfig() {
        if (!Files.exists(SERVER_CONFIG)) {
            serverConfig = new JsonObject();
            serverConfig.add("ignore", new JsonArray());
            serverConfig.addProperty("saved_server", "");
            saveServerConfig();
            return;
        }
        try (Reader reader = Files.newBufferedReader(SERVER_CONFIG)) {
            JsonElement element = gson.fromJson(reader, JsonElement.class);
            if (element == null || element.isJsonNull()) {
                serverConfig = new JsonObject();
                serverConfig.add("ignore", new JsonArray());
                serverConfig.addProperty("saved_server", "");
                saveServerConfig();
            } else {
                serverConfig = element.getAsJsonObject();
                cleanExpiredIgnoredServers();
            }
        } catch (IOException e) {
            System.err.println("Failed to load server config: " + e.getMessage());
            serverConfig = new JsonObject();
            serverConfig.add("ignore", new JsonArray());
            serverConfig.addProperty("saved_server", "");
        }
    }

    public static void saveServerConfig() {
        try (Writer writer = Files.newBufferedWriter(SERVER_CONFIG)) {
            gson.toJson(serverConfig, writer);
        } catch (IOException e) {
            System.err.println("Failed to save server config: " + e.getMessage());
        }
    }

    public static void cleanExpiredIgnoredServers() {
        if (serverConfig == null || !serverConfig.has("ignore")) return;
        JsonArray validServers = new JsonArray();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (JsonElement element : serverConfig.getAsJsonArray("ignore")) {
            try {
                String entry = element.getAsString();
                String[] parts = entry.split("\\|", 2);
                if (parts.length == 2) {
                    Date recordTime = sdf.parse(parts[1]);
                    long hours = (System.currentTimeMillis() - recordTime.getTime()) / (1000 * 60 * 60);
                    if (hours <= 12) validServers.add(entry);
                }
            } catch (Exception ignored) {
            }
        }
        serverConfig.add("ignore", validServers);
        saveServerConfig();
    }

    public static List<String> getIgnoredServers() {
        if (serverConfig == null || !serverConfig.has("ignore")) return new ArrayList<>();
        List<String> servers = new ArrayList<>();
        for (JsonElement element : serverConfig.getAsJsonArray("ignore")) {
            servers.add(element.getAsString().split("\\|")[0]);
        }
        return servers;
    }

    public static void addIgnoredServer(String server) {
        if (serverConfig == null) return;
        JsonArray servers = serverConfig.has("ignore") ?
                serverConfig.getAsJsonArray("ignore") : new JsonArray();
        servers.add(server + "|" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        serverConfig.add("ignore", servers);
        saveServerConfig();
    }

    public static void removeIgnoredServer(String server) {
        if (serverConfig == null || !serverConfig.has("ignore")) return;
        JsonArray newServers = new JsonArray();
        for (JsonElement element : serverConfig.getAsJsonArray("ignore")) {
            if (!element.getAsString().startsWith(server + "|")) newServers.add(element);
        }
        serverConfig.add("ignore", newServers);
        saveServerConfig();
    }

    public static String getSavedServer() {
        if (serverConfig == null || !serverConfig.has("saved_server")) return "";
        return serverConfig.get("saved_server").getAsString();
    }

    public static void setSavedServer(String server) {
        if (serverConfig == null) return;
        serverConfig.addProperty("saved_server", server);
        saveServerConfig();
    }


    public static void saveConfig() {
        try (Writer writer = Files.newBufferedWriter(DEFAULT_CONFIG)) {
            JsonObject config = new JsonObject();
            config.addProperty("keybindFormat", "sdl3-scancode");
            config.addProperty("mouseBindFormat", "sdl3-button");
            for (Module module : ModuleManager.getModules()) {
                if (module.ignoreOnSave) continue;
                JsonObject moduleObject = new JsonObject();
                boolean shouldBeEnabled = module.isEnabled();
                if (module.getName().equalsIgnoreCase("ClickGui")) shouldBeEnabled = false;
                moduleObject.addProperty("enabled", shouldBeEnabled);
                moduleObject.addProperty("keybind", module.getKeycode());
                moduleObject.addProperty("hidden", module.isHidden());
                JsonObject settingsObject = new JsonObject();
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof SliderSetting) {
                        if (((SliderSetting) setting).isRange()) {
                            settingsObject.addProperty(setting.getName(),
                                    ((SliderSetting) setting).getInputMin() + "-" + ((SliderSetting) setting).getInputMax());
                        } else {
                            settingsObject.addProperty(setting.getName(), ((SliderSetting) setting).getInput());
                        }
                    } else if (setting instanceof SelectSetting) {
                        settingsObject.addProperty(setting.getName(), ((SelectSetting) setting).getValue());
                    } else if (setting instanceof ButtonSetting && !(setting instanceof KeyBindSetting)) {
                        settingsObject.addProperty(setting.getName(), ((ButtonSetting) setting).isToggled());
                    } else if (setting instanceof InputSetting) {
                        settingsObject.addProperty(setting.getName(), ((InputSetting) setting).getValue());
                    }
                }
                moduleObject.add("settings", settingsObject);
                config.add(module.getName(), moduleObject);
            }
            gson.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public static void loadConfig() {
        if (!Files.exists(DEFAULT_CONFIG)) {
            firstDefaultConfigMissing = true;
            return;
        }
        boolean migrated = false;
        loadingConfig = true;
        try (Reader reader = Files.newBufferedReader(DEFAULT_CONFIG)) {
            JsonObject config = gson.fromJson(reader, JsonObject.class);
            if (config == null) return;
            boolean legacyKeybinds = !config.has("keybindFormat");
            boolean legacyMouseBinds = !config.has("mouseBindFormat");
            for (Module module : ModuleManager.getModules()) {
                if (!config.has(module.getName())) continue;
                JsonObject moduleObject = config.getAsJsonObject(module.getName());
                if (moduleObject.get("enabled").getAsBoolean() && !module.isEnabled()) {
                    module.enable();
                } else if (!moduleObject.get("enabled").getAsBoolean() && module.isEnabled()) {
                    module.disable();
                }
                if (moduleObject.has("keybind")) {
                    int keybind = moduleObject.get("keybind").getAsInt();
                    if (legacyKeybinds) keybind = migrateGlfwKeybind(keybind);
                    else if (legacyMouseBinds && keybind == 1000) keybind = 1000 + InputConstants.MOUSE_BUTTON_LEFT;
                    module.setBind(keybind);
                }
                if (moduleObject.has("hidden")) module.setHidden(moduleObject.get("hidden").getAsBoolean());
                if (!moduleObject.has("settings")) continue;
                JsonObject settingsObject = moduleObject.getAsJsonObject("settings");
                for (Setting setting : module.getSettings()) {
                    if (!settingsObject.has(setting.getName())) continue;
                    if (setting instanceof SliderSetting slider) {
                        String raw = settingsObject.get(setting.getName()).getAsString();
                        if (slider.isRange()) {
                            String[] parts = raw.split("-");
                            if (parts.length == 2) {
                                try {
                                    slider.setInputMin(Double.parseDouble(parts[0]));
                                    slider.setInputMax(Double.parseDouble(parts[1]));
                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid range for " + setting.getName() + ": " + raw);
                                }
                            }
                        } else {
                            slider.setInput(Double.parseDouble(raw));
                        }
                    } else if (setting instanceof SelectSetting) {
                        ((SelectSetting) setting).setValue(settingsObject.get(setting.getName()).getAsDouble());
                    } else if (setting instanceof ButtonSetting && !(setting instanceof KeyBindSetting)) {
                        ((ButtonSetting) setting).setEnabled(settingsObject.get(setting.getName()).getAsBoolean());
                    } else if (setting instanceof InputSetting) {
                        ((InputSetting) setting).setValue(settingsObject.get(setting.getName()).getAsString());
                    }
                }
            }
            migrated = legacyKeybinds || legacyMouseBinds;
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
        } finally {
            loadingConfig = false;
        }
        if (migrated) saveConfig();
    }

    private static int migrateGlfwKeybind(int key) {
        if (key == 0) return key;
        // Legacy mouse binds used GLFW's left=0, right=1, middle=2.
        // Scroll binds (1069/1070) are Frosty-specific and stay unchanged.
        if (key >= 1000) {
            if (key == 1000) return 1000 + InputConstants.MOUSE_BUTTON_LEFT;
            if (key == 1001) return 1000 + InputConstants.MOUSE_BUTTON_RIGHT;
            if (key == 1002) return 1000 + InputConstants.MOUSE_BUTTON_MIDDLE;
            if (key >= 1003 && key <= 1007) return key + 1;
            return key;
        }
        if (key >= 65 && key <= 90) return InputConstants.KEY_A + key - 65;
        if (key >= 49 && key <= 57) return InputConstants.KEY_1 + key - 49;
        if (key >= 290 && key <= 301) return InputConstants.KEY_F1 + key - 290;
        if (key >= 302 && key <= 313) return InputConstants.KEY_F13 + key - 302;
        return switch (key) {
            case 32 -> InputConstants.KEY_SPACE;
            case 39 -> InputConstants.KEY_APOSTROPHE;
            case 44 -> InputConstants.KEY_COMMA;
            case 45 -> InputConstants.KEY_MINUS;
            case 46 -> InputConstants.KEY_PERIOD;
            case 47 -> InputConstants.KEY_SLASH;
            case 48 -> InputConstants.KEY_0;
            case 59 -> InputConstants.KEY_SEMICOLON;
            case 61 -> InputConstants.KEY_EQUALS;
            case 91 -> InputConstants.KEY_LBRACKET;
            case 92 -> InputConstants.KEY_BACKSLASH;
            case 93 -> InputConstants.KEY_RBRACKET;
            case 96 -> InputConstants.KEY_GRAVE;
            case 256 -> InputConstants.KEY_ESCAPE;
            case 257 -> InputConstants.KEY_RETURN;
            case 258 -> InputConstants.KEY_TAB;
            case 259 -> InputConstants.KEY_BACKSPACE;
            case 260 -> InputConstants.KEY_INSERT;
            case 261 -> InputConstants.KEY_DELETE;
            case 262 -> InputConstants.KEY_RIGHT;
            case 263 -> InputConstants.KEY_LEFT;
            case 264 -> InputConstants.KEY_DOWN;
            case 265 -> InputConstants.KEY_UP;
            case 266 -> InputConstants.KEY_PAGEUP;
            case 267 -> InputConstants.KEY_PAGEDOWN;
            case 268 -> InputConstants.KEY_HOME;
            case 269 -> InputConstants.KEY_END;
            case 280 -> InputConstants.KEY_CAPSLOCK;
            case 281 -> InputConstants.KEY_SCROLLLOCK;
            case 282 -> InputConstants.KEY_NUMLOCK;
            case 283 -> InputConstants.KEY_PRINTSCREEN;
            case 284 -> InputConstants.KEY_PAUSE;
            case 320 -> InputConstants.KEY_NUMPAD0;
            case 321 -> InputConstants.KEY_NUMPAD1;
            case 322 -> InputConstants.KEY_NUMPAD2;
            case 323 -> InputConstants.KEY_NUMPAD3;
            case 324 -> InputConstants.KEY_NUMPAD4;
            case 325 -> InputConstants.KEY_NUMPAD5;
            case 326 -> InputConstants.KEY_NUMPAD6;
            case 327 -> InputConstants.KEY_NUMPAD7;
            case 328 -> InputConstants.KEY_NUMPAD8;
            case 329 -> InputConstants.KEY_NUMPAD9;
            case 330 -> InputConstants.KEY_PERIOD;
            case 331 -> InputConstants.KEY_SLASH;
            case 332 -> InputConstants.KEY_MULTIPLY;
            case 333 -> InputConstants.KEY_MINUS;
            case 334 -> InputConstants.KEY_ADD;
            case 336 -> InputConstants.KEY_NUMPADENTER;
            case 340 -> InputConstants.KEY_LSHIFT;
            case 341 -> InputConstants.KEY_LCONTROL;
            case 342 -> InputConstants.KEY_LALT;
            case 343 -> InputConstants.KEY_LGUI;
            case 344 -> InputConstants.KEY_RSHIFT;
            case 345 -> InputConstants.KEY_RCONTROL;
            case 346 -> InputConstants.KEY_RALT;
            case 347 -> InputConstants.KEY_RGUI;
            default -> key;
        };
    }

    public static boolean shouldShowLanguagePrompt() {
        return firstDefaultConfigMissing && !languagePromptCompleted;
    }

    public static void completeLanguagePrompt() {
        languagePromptCompleted = true;
        firstDefaultConfigMissing = false;
    }
}
