package xyz.whatsyouss.frosty.utility;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import xyz.whatsyouss.frosty.mixin.accessor.MinecraftAccessor;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

public class Utils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("frosty.json");
    public static boolean rendering3D = true;
    private static final Pattern SB_LEVEL_PATTERN = Pattern.compile("(§.)*\\[([0-9]+)\\](§.)*");
    private static ProjectionMatrixBuffer matrixBuffer;

    public static void addChatMessage(String message) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§7[§9F§br§9o§bs§9t§by§7] " + message));
        }
    }

    public static void addModuleMessage(String moduleName, String message) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§7[§9F§br§9o§bs§9t§by§7] " + "§7[§f§l" + moduleName + "§7] " + message));
        }
    }

    public static void addChatMessage(String message, String messageCN) {
        if (mc.player != null) {
            if (UI.lang.getValue() == 1) {
                mc.player.sendSystemMessage(Component.literal("§7[§9F§br§9o§bs§9t§by§7] " + messageCN));
            } else {
                mc.player.sendSystemMessage(Component.literal("§7[§9F§br§9o§bs§9t§by§7] " + message));
            }
        }
    }

    public static void addModuleMessage(String moduleName, String message, String messageCN) {
        if (mc.player != null) {
            if (UI.lang.getValue() == 1) {
                mc.player.sendSystemMessage(Component.literal("§7[§9F§br§9o§bs§9t§by§7] " + "§7[§f§l" + moduleName + "§7] " + messageCN));
            } else {
                mc.player.sendSystemMessage(Component.literal("§7[§9F§br§9o§bs§9t§by§7] " + "§7[§f§l" + moduleName + "§7] " + message));
            }
        }
    }

    public static boolean nullCheck() {
        return mc.player != null && mc.level != null;
    }

    public static List<String> getScoreboardSidebarLines() {
        Scoreboard scoreboard = mc.level.getScoreboard();
        if (scoreboard == null) return Collections.emptyList();

        Objective activeObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (activeObjective == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();

        result.add(activeObjective.getDisplayName().getString());

        Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(activeObjective);
        if (entries == null) return result;

        entries.stream()
                .sorted((a, b) -> Integer.compare(b.value(), a.value()))
                .limit(15)
                .forEach(entry -> {
                    Component baseName = entry.display() != null ? entry.display() : entry.ownerName();
                    String rawOwner = entry.ownerName().getString();
                    Team team = scoreboard.getPlayersTeam(rawOwner);

                    if (team != null) {
                        result.add(team.getFormattedName(baseName).getString());
                    } else {
                        result.add(baseName.getString());
                    }
                });
        return result;
    }

    private static String cleanColorCodes(String input) {
        if (input == null) {
            return "";
        }
        return Pattern.compile("§[0-9a-fk-or]").matcher(input).replaceAll("");
    }

    public static boolean inCrystalHollow(String scoreboard) {
        return scoreboard.contains("Jungle") ||
                scoreboard.contains("Jungle Temple") ||
                scoreboard.contains("Mithril Deposits") ||
                scoreboard.contains("Mines of Divan") ||
                scoreboard.contains("Goblin Holdout") ||
                scoreboard.contains("Goblin Queen's Den") ||
                scoreboard.contains("Precursor Remnants") ||
                scoreboard.contains("Lost Precursor City") ||
                scoreboard.contains("Crystal Nucleus") ||
                scoreboard.contains("Magma Fields") ||
                scoreboard.contains("Khazad-dûm") ||
                scoreboard.contains("Fairy Grotto") ||
                scoreboard.contains("Dragon's Lair");
    }

    public static Map<String, String> getCurrentLocation() {
        Map<String, String> result = new HashMap<>();
        result.put("Area", "Unknown");
        result.put("Server", "Unknown");

        if (mc.player != null && mc.getConnection() != null) {
            for (var info : mc.getConnection().getOnlinePlayers()) {
                String displayName = info.getTabListDisplayName() != null ?
                        info.getTabListDisplayName().getString() :
                        (info.getProfile() != null ? info.getProfile().name() : "");

                displayName = stripFormatting(displayName);

                Matcher areaMatcher = Pattern.compile("Area: (.+)").matcher(displayName);
                if (areaMatcher.find()) {
                    result.put("Area", areaMatcher.group(1).trim());
                }

                Matcher serverMatcher = Pattern.compile("Server: (.+)").matcher(displayName);
                if (serverMatcher.find()) {
                    result.put("Server", serverMatcher.group(1).trim());
                }
            }
        }
        return result;
    }

    public static String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public static List<String> getIgnoredServers() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String content = Files.readString(CONFIG_PATH);
                JsonObject config = new Gson().fromJson(content, JsonObject.class);
                JsonArray servers = config.getAsJsonArray("ignore_servers");
                if (servers != null) {
                    List<String> result = new ArrayList<>();
                    for (var server : servers) {
                        result.add(server.getAsString());
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static void addIgnoredServer(String server) {
        try {
            JsonObject config = new JsonObject();
            if (Files.exists(CONFIG_PATH)) {
                String content = Files.readString(CONFIG_PATH);
                if (!content.isEmpty()) {
                    config = new Gson().fromJson(content, JsonObject.class);
                }
            }
            JsonArray servers = config.getAsJsonArray("ignore_servers");
            if (servers == null) {
                servers = new JsonArray();
            }
            servers.add(server + "|" + getCurrentTimestamp());
            config.add("ignore_servers", servers);
            Files.writeString(CONFIG_PATH, new Gson().toJson(config));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeIgnoredServer(String server) {
        try {
            if (Files.exists(CONFIG_PATH)) {
                JsonObject config = new Gson().fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
                JsonArray servers = config.getAsJsonArray("ignore_servers");
                if (servers != null) {
                    JsonArray newServers = new JsonArray();
                    for (var s : servers) {
                        if (!s.getAsString().startsWith(server + "|")) {
                            newServers.add(s);
                        }
                    }
                    config.add("ignore_servers", newServers);
                    Files.writeString(CONFIG_PATH, new Gson().toJson(config));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cleanExpiredIgnoredServers() {
        try {
            JsonObject config = new Gson().fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
            JsonArray servers = config.getAsJsonArray("ignore_servers");
            if (servers != null) {
                JsonArray validServers = new JsonArray();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (var entry : servers) {
                    String[] parts = entry.getAsString().split("\\|", 2);
                    if (parts.length == 2) {
                        try {
                            Date recordTime = sdf.parse(parts[1]);
                            long hours = (System.currentTimeMillis() - recordTime.getTime()) / (1000 * 60 * 60);
                            if (hours <= 12) {
                                validServers.add(entry);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                config.add("ignore_servers", validServers);
                Files.writeString(CONFIG_PATH, new Gson().toJson(config));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getSavedServer() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                JsonObject config = new Gson().fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
                return config.get("saved_server") != null ? config.get("saved_server").getAsString() : "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void setSavedServer(String server) {
        try {
            JsonObject config = new JsonObject();
            if (Files.exists(CONFIG_PATH)) {
                config = new Gson().fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
            }
            config.addProperty("saved_server", server);
            Files.writeString(CONFIG_PATH, new Gson().toJson(config));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String stripFormatting(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    public static int getChroma(long speed, long... delay) {
        long time = System.currentTimeMillis() + (delay.length > 0 ? delay[0] : 0L);
        return Color.getHSBColor((float) (time % (15000L / speed)) / (15000.0F / (float) speed), 1.0F, 1.0F).getRGB();
    }

    public static List<String> wrapWords(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    public static String readInputStream(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static void setSpeed(double speed) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (speed == 0.0) {
            player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
            return;
        }

        float yaw = player.getYRot();
        float yawRadians = (float) Math.toRadians(yaw);

        double motionX = -Mth.sin(yawRadians) * speed;
        double motionZ = Mth.cos(yawRadians) * speed;
        player.setDeltaMovement(motionX, player.getDeltaMovement().y, motionZ);
    }

    public static double getHorizontalSpeed() {
        return getHorizontalSpeed(mc.player);
    }

    public static double getHorizontalSpeed(LocalPlayer entity) {
        return Math.sqrt(entity.getDeltaMovement().x() * entity.getDeltaMovement().x() + entity.getDeltaMovement().z() * entity.getDeltaMovement().z());
    }

    public static float calculateYaw(float yaw, float forward, float sideways) {
        float factor = 1.0f;
        if (forward < 0.0f) {
            yaw += 180.0f;
            factor = -0.5f;
        } else if (forward > 0.0f) {
            factor = 0.5f;
        }
        if (sideways > 0.0f) {
            yaw -= 90.0f * factor;
        } else if (sideways < 0.0f) {
            yaw += 90.0f * factor;
        }
        return yaw * 0.017453292f;
    }

    public static int getSpeedAmplifier() {
        if (mc.player != null && mc.player.hasEffect(MobEffects.SPEED)) {
            MobEffectInstance effect = mc.player.getEffect(MobEffects.SPEED);
            return effect != null ? 1 + effect.getAmplifier() : 0;
        }
        return 0;
    }

    public static void addToClipboard(String str) {
        try {
            mc.keyboardHandler.setClipboard(str);
            mc.player.sendOverlayMessage(Component.literal("§bCopied§f: '" + str + "' §bto clipboard"));
        }
        catch (Exception e) {
            Utils.addChatMessage("§cFailed to copy '" + str + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static String stripColor(String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }
        return string.replaceAll("§.", "");
    }

    private static ProjectionMatrixBuffer getMatrixBuffer() {
        RenderSystem.assertOnRenderThread();
        if (matrixBuffer == null) {
            matrixBuffer = new ProjectionMatrixBuffer("frosty-projection-matrix");
        }
        return matrixBuffer;
    }

    public static void unscaledProjection() {
        float width = mc.getWindow().getWidth();
        float height = mc.getWindow().getHeight();

        var proj = new Projection();
        proj.setupOrtho(-10, 100, width, height, true);
        var matrix = proj.getMatrix(new Matrix4f());

        RenderSystem.setProjectionMatrix(getMatrixBuffer().getBuffer(matrix), ProjectionType.ORTHOGRAPHIC);
        RenderUtils.projection.set(matrix);

        rendering3D = false;
    }

    public static void scaledProjection() {
        float width = (float) (mc.getWindow().getWidth() / mc.getWindow().getGuiScale());
        float height = (float) (mc.getWindow().getHeight() / mc.getWindow().getGuiScale());

        var proj = new Projection();
        proj.setupOrtho(-10, 100, width, height, true);
        var matrix = proj.getMatrix(new Matrix4f());

        RenderSystem.setProjectionMatrix(getMatrixBuffer().getBuffer(matrix), ProjectionType.PERSPECTIVE);
        RenderUtils.projection.set(matrix);

        rendering3D = true;
    }

    public static boolean isHeldItem(String... names) {
        if (!nullCheck()) {
            return false;
        }
        for (String name : names) {
            if (mc.player.getMainHandItem().getHoverName().getString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static int getColorFromEntity(Player entity) {
        if (entity == null) return -1;

        Team team = entity.level().getScoreboard().getPlayersTeam(entity.getScoreboardName());

        if (team != null) {
            var teamColorOptional = team.getColor();

            if (teamColorOptional.isPresent()) {
                var teamColor = teamColorOptional.get();

                return teamColor.textColor().getValue();
            }
        }

        Component displayName = entity.getDisplayName();
        if (displayName != null) {
            Style style = displayName.getStyle();
            if (style != null && style.getColor() != null) {
                return style.getColor().getValue();
            }
        }

        return -1;
    }

    public static String getSkyblockLevel(LocalPlayer player) {
        Map<String, String> location = getCurrentLocation();
        if (location == null || !"Galatea".equals(location.get("Area"))) {
            return null;
        }

        if (mc.getConnection() == null) return null;
        PlayerInfo entry = mc.getConnection().getPlayerInfo(player.getUUID());
        if (entry == null) return null;

        Component displayText = entry.getTabListDisplayName() != null ?
                entry.getTabListDisplayName() :
                Component.literal(player.getName().getString());
        String formattedName = displayText.getString();

        Matcher matcher = SB_LEVEL_PATTERN.matcher(formattedName);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }

        return null;
    }

    public static String getLiteralByText(Component text) {
        StringBuilder builder = new StringBuilder();

        builder.append(text.getString());

        for (Component sibling : text.getSiblings()) {
            builder.append(sibling.getString());
        }

        return builder.toString();
    }

    public static String getLiteral(String s) {
        StringBuilder result = new StringBuilder();
        int index = 0;
        while ((index = s.indexOf("literal{", index)) != -1) {
            int start = index + "literal{".length();
            int braceCount = 1;
            int end = start;
            while (end < s.length() && braceCount > 0) {
                if (s.charAt(end) == '{') braceCount++;
                else if (s.charAt(end) == '}') braceCount--;
                end++;
            }
            if (braceCount == 0) {
                result.append(s, start, end - 1);
                index = end;
            } else {
                break;
            }
        }
        return result.toString();
    }

    public static String getColoredLiteral(String s) {
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("literal\\{(.*?)}\\[style=\\{color=(\\w+)");
        Matcher matcher = pattern.matcher(s);

        while (matcher.find()) {
            String text = matcher.group(1);
            String color = matcher.group(2);
            String colorCode = getMinecraftColorCode(color);
            result.append(colorCode).append(text);
        }

        return result.toString();
    }

    public static String getFirstLiteral(String s) {
        int index = s.indexOf("literal{");
        if (index == -1) return "";

        int start = index + "literal{".length();
        int braceCount = 1;
        int end = start;

        while (end < s.length() && braceCount > 0) {
            if (s.charAt(end) == '{') braceCount++;
            else if (s.charAt(end) == '}') braceCount--;
            end++;
        }

        if (braceCount == 0) {
            return s.substring(start, end - 1);
        }
        return "";
    }

    public static String getFirstColoredLiteral(String s) {
        Pattern pattern = Pattern.compile("literal\\{(.*?)\\}\\[style=\\{color=(\\w+)");
        Matcher matcher = pattern.matcher(s);

        if (matcher.find()) {
            String text = matcher.group(1);
            String color = matcher.group(2);
            String colorCode = getMinecraftColorCode(color);
            return colorCode + text;
        }
        return "";
    }

    public static String getFirstColor(String s) {
        Pattern pattern = Pattern.compile("literal\\{.*?}\\[style=\\{color=(\\w+)");
        Matcher matcher = pattern.matcher(s);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String getMinecraftColorCode(String color) {
        switch (color.toLowerCase()) {
            case "black": return "§0";
            case "dark_blue": return "§1";
            case "dark_green": return "§2";
            case "dark_aqua": return "§3";
            case "dark_red": return "§4";
            case "dark_purple": return "§5";
            case "gold": return "§6";
            case "gray": return "§7";
            case "dark_gray": return "§8";
            case "blue": return "§9";
            case "green": return "§a";
            case "aqua": return "§b";
            case "red": return "§c";
            case "light_purple": return "§d";
            case "yellow": return "§e";
            case "white": return "§f";
            default: return "§f"; // default to white
        }
    }

    public static String FormattedText(Component text) {
        StringBuilder sb = new StringBuilder();

        Style style = text.getStyle();
        String colorCode = getMinecraftColorCode(style.getColor());

        sb.append(colorCode).append(text.getString());

        for (Component sibling : text.getSiblings()) {
            sb.append(FormattedText(sibling));
        }

        return sb.toString();
    }

    public static String getMinecraftColorCode(@Nullable TextColor color) {
        if (color == null) return "§r";

        int rgb = color.getValue();

        Map<Integer, Character> colorMap = new HashMap<>();
        colorMap.put(0x000000, '0'); // black
        colorMap.put(0x0000AA, '1'); // dark_blue
        colorMap.put(0x00AA00, '2'); // dark_green
        colorMap.put(0x00AAAA, '3'); // dark_aqua
        colorMap.put(0xAA0000, '4'); // dark_red
        colorMap.put(0xAA00AA, '5'); // dark_purple
        colorMap.put(0xFFAA00, '6'); // gold
        colorMap.put(0xAAAAAA, '7'); // gray
        colorMap.put(0x555555, '8'); // dark_gray
        colorMap.put(0x5555FF, '9'); // blue
        colorMap.put(0x55FF55, 'a'); // green
        colorMap.put(0x55FFFF, 'b'); // aqua
        colorMap.put(0xFF5555, 'c'); // red
        colorMap.put(0xFF55FF, 'd'); // light_purple
        colorMap.put(0xFFFF55, 'e'); // yellow
        colorMap.put(0xFFFFFF, 'f'); // white

        if (colorMap.containsKey(rgb)) {
            char code = colorMap.get(rgb);
            ChatFormatting formatting = ChatFormatting.getByCode(code);
            if (formatting != null) {
                return formatting.toString();
            }
        }

        String hex = String.format("%06X", rgb);
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }



    public static String getCustomData(String s) {
        String key = "minecraft:custom_data=>{";
        int start = s.indexOf(key);
        if (start == -1) return "";

        int braceStart = start + key.length() - 1;
        int braceCount = 1;
        int end = braceStart + 1;

        while (end < s.length() && braceCount > 0) {
            char ch = s.charAt(end);
            if (ch == '{') braceCount++;
            else if (ch == '}') braceCount--;
            end++;
        }

        if (braceCount == 0) {
            return s.substring(start, end);
        } else {
            return "";
        }
    }

    public static String getCustomDataIId(String s) {
        String customData = getCustomData(s);
        if (customData.isEmpty()) return "";

        int idIndex = customData.indexOf("id:\"");
        if (idIndex == -1) return "";

        int valueStart = idIndex + 4;
        int valueEnd = customData.indexOf("\"", valueStart);
        if (valueEnd == -1) return "";

        return customData.substring(valueStart, valueEnd);
    }

    public static List<PlayerInfo> getTablist(boolean removeSelf) {
        final ArrayList<PlayerInfo> list = new ArrayList<>(mc.getConnection().getOnlinePlayers().stream().toList());
        removeDuplicates(list);
        if (removeSelf) {
            list.remove(mc.player.getName());
        }
        return list;
    }

    public static void removeDuplicates(final ArrayList list) {
        final HashSet set = new HashSet(list);
        list.clear();
        list.addAll(set);
    }

    public static void leftClick() {
        int attackCooldown = ((MinecraftAccessor) mc).frosty$getMissTime();
        if (attackCooldown == 10000) {
            ((MinecraftAccessor) mc).frosty$setMissTime(0);
        }

        mc.options.keyAttack.setDown(true);
        ((MinecraftAccessor) mc).frosty$leftClick();
        mc.options.keyAttack.setDown(false);
    }

    public static boolean holdingSword() {
        if (mc.player.getMainHandItem() == null) {
            return false;
        }
        return mc.player.getMainHandItem().getItem() == Items.WOODEN_SWORD ||
                mc.player.getMainHandItem().getItem() == Items.IRON_SWORD ||
                mc.player.getMainHandItem().getItem() == Items.GOLDEN_SWORD ||
                mc.player.getMainHandItem().getItem() == Items.STONE_SWORD ||
                mc.player.getMainHandItem().getItem() == Items.DIAMOND_SWORD ||
                mc.player.getMainHandItem().getItem() == Items.NETHERITE_SWORD;
    }

    public static Vector3d set(Vector3d vec, Vec3 v) {
        vec.x = v.x;
        vec.y = v.y;
        vec.z = v.z;

        return vec;
    }

    public static org.joml.Vector3d set(org.joml.Vector3d vec, LocalPlayer entity, double tickDelta) {
        Vec3 renderPos = entity.getPosition((float) tickDelta);

        vec.x = renderPos.x;
        vec.y = renderPos.y;
        vec.z = renderPos.z;

        return vec;
    }

    public static int getAPSToTicks(SliderSetting slider, double cap) {
        double apsv = slider.getInput();
        if (apsv > cap) {
            apsv = cap;
        }
        if (apsv >= 20) {
            return 0;
        }
        if (apsv >= 16) {
            return (int) MathUtils.randomizeDouble(0.0D, 1.0D);
        }
        if (apsv >= 15) {
            return 1;
        }
        if (apsv >= 11) {
            return (int) MathUtils.randomizeDouble(1.0D, 2.0D);
        }
        if (apsv >= 10) {
            return 2;
        }
        if (apsv >= 7) {
            return (int) MathUtils.randomizeDouble(2.0D, 3.0D);
        }
        if (apsv >= 6) {
            return (int) MathUtils.randomizeDouble(3.0D, 4.0D);
        }
        if (apsv >= 5) {
            return 4;
        }
        if (apsv >= 4) {
            return 5;
        }
        if (apsv >= 3) {
            return (int) MathUtils.randomizeDouble(6.0D, 7.0D);
        }
        if (apsv >= 2) {
            return 10;
        }
        if (apsv >= 1) {
            return 20;
        }
        if (apsv >= 0) {
            return -1;
        }
        return -1;
    }
}