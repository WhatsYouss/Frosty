package xyz.whatsyouss.frosty.config;

import com.google.gson.*;
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
        try (Reader reader = Files.newBufferedReader(DEFAULT_CONFIG)) {
            JsonObject config = gson.fromJson(reader, JsonObject.class);
            if (config == null) return;
            for (Module module : ModuleManager.getModules()) {
                if (!config.has(module.getName())) continue;
                JsonObject moduleObject = config.getAsJsonObject(module.getName());
                if (moduleObject.get("enabled").getAsBoolean() && !module.isEnabled()) {
                    module.enable();
                } else if (!moduleObject.get("enabled").getAsBoolean() && module.isEnabled()) {
                    module.disable();
                }
                if (moduleObject.has("keybind")) module.setBind(moduleObject.get("keybind").getAsInt());
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
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }
    }

    public static boolean shouldShowLanguagePrompt() {
        return firstDefaultConfigMissing && !languagePromptCompleted;
    }

    public static void completeLanguagePrompt() {
        languagePromptCompleted = true;
        firstDefaultConfigMissing = false;
    }
}
