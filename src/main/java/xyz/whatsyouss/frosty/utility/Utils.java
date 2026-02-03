package xyz.whatsyouss.frosty.utility;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
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
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3d;
import xyz.whatsyouss.frosty.mixin.accessor.InGameHudAccessor;
import xyz.whatsyouss.frosty.mixin.accessor.MinecraftClientAccessor;
import xyz.whatsyouss.frosty.mixin.accessor.ProjectionMatrix2Accessor;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

public class Utils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("frosty.json");
    public static boolean rendering3D = true;
    private static final Pattern SB_LEVEL_PATTERN = Pattern.compile("(§.)*\\[([0-9]+)\\](§.)*");
    private static final ProjectionMatrix2 matrix = new ProjectionMatrix2("frosty-projection-matrix", -10, 100, true);

    public static void addChatMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§9F§br§9o§bs§9t§by§7] " + message), false);
        }
    }

    public static boolean nullCheck() {
        return mc.player != null && mc.world != null;
    }

    public static List<String> getSidebar() {
        List<String> lines = new ArrayList<>();
        if (mc.world == null || mc.player == null) return lines;

        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) return lines;

        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return lines;

        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
        if (entries == null) return lines;

        List<ScoreboardEntry> sortedEntries = new ArrayList<>(entries);
        sortedEntries.sort((a, b) -> b.value() - a.value());

        for (ScoreboardEntry entry : sortedEntries) {
            if (entry == null || entry.owner() == null) continue;

            Team team = scoreboard.getTeam(entry.owner());
            if (team != null) {
                String line = team.getPrefix().getString()
                        + team.getName()
                        + team.getSuffix().getString();
                lines.add(line.replaceAll("§.", ""));
            } else {
                lines.add(entry.owner().replaceAll("§.", ""));
            }
        }

        if (!lines.isEmpty()) {
            lines.add(0, objective.getDisplayName().getString().replaceAll("§.", ""));
        }

        return lines;
    }

    public static List<Text> getScoreboardSidebar() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return Collections.emptyList();
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective activeObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (activeObjective == null) {
            return Collections.emptyList();
        }

        List<Text> result = new ArrayList<>();

        result.add(activeObjective.getDisplayName());

        List<ScoreboardEntry> entries = (List<ScoreboardEntry>) scoreboard.getScoreboardEntries(activeObjective);
        entries.stream()
                .filter(entry -> !entry.hidden())
                .sorted(InGameHudAccessor.getScoreboardEntryComparator())
                .limit(15)
                .forEach(entry -> {
                    ScoreHolder scoreHolder = ScoreHolder.fromName(entry.owner());
                    String scoreHolderName = scoreHolder.getNameForScoreboard();
                    Team team = scoreboard.getScoreHolderTeam(scoreHolderName);
                    Text text = entry.name();
                    Text decoratedText = Team.decorateName(team, text);
                    result.add(decoratedText);
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

        if (mc.player != null && mc.getNetworkHandler() != null) {
            for (var info : mc.getNetworkHandler().getPlayerList()) {
                String displayName = info.getDisplayName() != null ?
                        info.getDisplayName().getString() :
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
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (speed == 0.0) {
            player.setVelocity(0.0, player.getVelocity().y, 0.0);
            return;
        }

        float yaw = player.getYaw();
        float yawRadians = (float) Math.toRadians(yaw);

        double motionX = -MathHelper.sin(yawRadians) * speed;
        double motionZ = MathHelper.cos(yawRadians) * speed;
        player.setVelocity(motionX, player.getVelocity().y, motionZ);
    }

    public static double getHorizontalSpeed() {
        return getHorizontalSpeed(mc.player);
    }

    public static double getHorizontalSpeed(Entity entity) {
        return Math.sqrt(entity.getVelocity().getX() * entity.getVelocity().getX() + entity.getVelocity().getZ() * entity.getVelocity().getZ());
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
        if (mc.player != null && mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.SPEED);
            return effect != null ? 1 + effect.getAmplifier() : 0;
        }
        return 0;
    }

    public static void addToClipboard(String str) {
        try {
            mc.keyboard.setClipboard(str);
            Utils.addChatMessage("Copied: '" + str + "'to clipboard");
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
        return string.replaceAll("§[0-9a-fk-or]", "");
    }

    public static void unscaledProjection() {
        float width = mc.getWindow().getFramebufferWidth();
        float height = mc.getWindow().getFramebufferHeight();

        RenderSystem.setProjectionMatrix(matrix.set(width, height), ProjectionType.ORTHOGRAPHIC);
        RenderUtils.projection.set(((ProjectionMatrix2Accessor) matrix).frosty$callGetMatrix(width, height));

        rendering3D = false;
    }

    public static void scaledProjection() {
        float width = (float) (mc.getWindow().getFramebufferWidth() / mc.getWindow().getScaleFactor());
        float height = (float) (mc.getWindow().getFramebufferHeight() / mc.getWindow().getScaleFactor());

        RenderSystem.setProjectionMatrix(matrix.set(width, height), ProjectionType.PERSPECTIVE);
        RenderUtils.projection.set(((ProjectionMatrix2Accessor) matrix).frosty$callGetMatrix(width, height));

        rendering3D = true;
    }

    public static boolean isHeldItem(String... names) {
        if (!nullCheck()) {
            return false;
        }
        for (String name : names) {
            if (mc.player.getMainHandStack().getName().getString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static int getColorFromEntity(Entity entity) {
        if (entity instanceof PlayerEntity) {
            Team team = ((LivingEntity) entity).getScoreboardTeam();

            if (team != null) {
                Formatting formatting = team.getColor();
                if (formatting != null && formatting != Formatting.RESET) {
                    return formatting.getColorValue() != null ?
                            formatting.getColorValue() :
                            -1;
                }
            }
        }

        Text displayName = entity.getDisplayName();
        String nameString = displayName.getString();

        Style style = displayName.getStyle();
        if (style.getColor() != null) {
            return style.getColor().getRgb();
        }

        return -1;
    }

    public static String getSkyblockLevel(PlayerEntity player) {
        Map<String, String> location = getCurrentLocation();
        if (location == null || !"Galatea".equals(location.get("Area"))) {
            return null;
        }

        if (mc.getNetworkHandler() == null) return null;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null) return null;

        Text displayText = entry.getDisplayName() != null ?
                entry.getDisplayName() :
                Text.literal(player.getName().getString());
        String formattedName = displayText.getString();

        Matcher matcher = SB_LEVEL_PATTERN.matcher(formattedName);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }

        return null;
    }

    public static String getLiteralByText(Text text) {
        StringBuilder builder = new StringBuilder();

        builder.append(text.getString());

        for (Text sibling : text.getSiblings()) {
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

    public static String FormattedText(Text text) {
        StringBuilder sb = new StringBuilder();

        Style style = text.getStyle();
        String colorCode = getMinecraftColorCode(style.getColor());

        sb.append(colorCode).append(text.getString());

        for (Text sibling : text.getSiblings()) {
            sb.append(FormattedText(sibling));
        }

        return sb.toString();
    }

    public static String getMinecraftColorCode(@Nullable TextColor color) {
        if (color == null) return "§r";

        String name = color.getName();

        if (name != null) {
            return switch (name) {
                case "black" -> "§0";
                case "dark_blue" -> "§1";
                case "dark_green" -> "§2";
                case "dark_aqua" -> "§3";
                case "dark_red" -> "§4";
                case "dark_purple" -> "§5";
                case "gold" -> "§6";
                case "gray" -> "§7";
                case "dark_gray" -> "§8";
                case "blue" -> "§9";
                case "green" -> "§a";
                case "aqua" -> "§b";
                case "red" -> "§c";
                case "light_purple" -> "§d";
                case "yellow" -> "§e";
                case "white" -> "§f";
                default -> "§r";
            };
        }

        int rgb = color.getRgb();
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

    public static List<PlayerListEntry> getTablist(boolean removeSelf) {
        final ArrayList<PlayerListEntry> list = new ArrayList<>(mc.getNetworkHandler().getPlayerList().stream().toList());
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
        int attackCooldown = ((MinecraftClientAccessor) mc).frosty$getAttackCooldown();
        if (attackCooldown == 10000) {
            ((MinecraftClientAccessor) mc).frosty$setAttackCooldown(0);
        }

        mc.options.attackKey.setPressed(true);
        ((MinecraftClientAccessor) mc).frosty$leftClick();
        mc.options.attackKey.setPressed(false);
    }

    public static boolean holdingSword() {
        if (mc.player.getMainHandStack() == null) {
            return false;
        }
        return mc.player.getMainHandStack().getItem() == Items.WOODEN_SWORD ||
                mc.player.getMainHandStack().getItem() == Items.IRON_SWORD ||
                mc.player.getMainHandStack().getItem() == Items.GOLDEN_SWORD ||
                mc.player.getMainHandStack().getItem() == Items.STONE_SWORD ||
                mc.player.getMainHandStack().getItem() == Items.DIAMOND_SWORD ||
                mc.player.getMainHandStack().getItem() == Items.NETHERITE_SWORD;
    }

    public static Vector3d set(Vector3d vec, Vec3d v) {
        vec.x = v.x;
        vec.y = v.y;
        vec.z = v.z;

        return vec;
    }

    public static Vector3d set(Vector3d vec, Entity entity, double tickDelta) {
        vec.x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        vec.y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        vec.z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

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