package xyz.whatsyouss.frosty.config;

import com.google.gson.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.Setting;
import xyz.whatsyouss.frosty.settings.impl.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConfigManager {
    private static final Path CONFIG_DIR = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/Frosty/");
    private static final Path CAPE_DIR = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/Frosty/cape");
    private static final Path DEFAULT_CONFIG = CONFIG_DIR.resolve("default.json");
    private static final Path SERVER_CONFIG = CONFIG_DIR.resolve("servers.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static JsonObject serverConfig;

    public static void createConfigDir() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.createDirectories(CAPE_DIR);
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
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
                    if (hours <= 12) {
                        validServers.add(entry);
                    }
                }
            } catch (Exception e) {
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
            if (!element.getAsString().startsWith(server + "|")) {
                newServers.add(element);
            }
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
                moduleObject.addProperty("enabled", module.isEnabled());
                moduleObject.addProperty("keybind", module.getKeycode());
                moduleObject.addProperty("hidden", module.isHidden());

                JsonObject settingsObject = new JsonObject();
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof SliderSetting) {
                        if (((SliderSetting) setting).isRange()) {
                            settingsObject.addProperty(setting.getName(), ((SliderSetting) setting).getInputMin() + "-" + ((SliderSetting) setting).getInputMax());
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
        if (!Files.exists(DEFAULT_CONFIG)) return;

        try (Reader reader = Files.newBufferedReader(DEFAULT_CONFIG)) {
            JsonObject config = gson.fromJson(reader, JsonObject.class);
            if (config == null) return;

            for (Module module : ModuleManager.getModules()) {
                if (!config.has(module.getName())) continue;

                JsonObject moduleObject = config.getAsJsonObject(module.getName());
                if (moduleObject.get("enabled").getAsBoolean() && !module.isEnabled()) {
                    module.shouldEnable = true;
                } else if (!moduleObject.get("enabled").getAsBoolean() && module.isEnabled()) {
                    module.shouldEnable = false;
                }

                if (moduleObject.has("keybind")) {
                    module.setBind(moduleObject.get("keybind").getAsInt());
                }

                if (moduleObject.has("hidden")) {
                    module.setHidden(moduleObject.get("hidden").getAsBoolean());
                }

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
                                    double min = Double.parseDouble(parts[0]);
                                    double max = Double.parseDouble(parts[1]);
                                    slider.setInputMin(min);
                                    slider.setInputMax(max);
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
}