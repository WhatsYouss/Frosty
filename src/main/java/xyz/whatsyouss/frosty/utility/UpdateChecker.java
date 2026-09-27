package xyz.whatsyouss.frosty.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.client.Update;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.whatsyouss.frosty.Frosty.mc;

public final class UpdateChecker {
    private static final URI LATEST_RELEASE = URI.create("https://api.github.com/repos/WhatsYouss/Frosty/releases/latest");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:(?:[-_.\\s]+)(alpha|a|beta|b|hotfix|hf)(?:[-_.\\s]*(\\d+))?)?",
            Pattern.CASE_INSENSITIVE);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "frosty-update-checker");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean UPDATE_NOTIFICATION_SENT = new AtomicBoolean();

    private static volatile CompletableFuture<UpdateInfo> checkFuture;
    private static volatile boolean lastCheckFailed;

    private UpdateChecker() { }

    public static void requestStartupCheck() {
        requestCheck();
    }

    public static void requestManualCheck() {
        Utils.addModuleMessage(ModuleManager.update.getTransName(), "Checking for Frosty updates...", "正在检查 Frosty 更新...");
        requestCheck(true).thenAccept(update -> Minecraft.getInstance().execute(() -> notifyManualResult(update)));
    }

    public static void notifyIfAvailable() {
        if (notificationsSuppressed()) return;
        requestCheck(false).thenAccept(UpdateChecker::notifyStartupResult);
    }

    private static CompletableFuture<UpdateInfo> requestCheck() {
        return requestCheck(false);
    }

    private static CompletableFuture<UpdateInfo> requestCheck(boolean forceRefresh) {
        CompletableFuture<UpdateInfo> future = checkFuture;
        if (!forceRefresh && future != null && !future.isCompletedExceptionally()) {
            return future;
        }

        synchronized (UpdateChecker.class) {
            future = checkFuture;
            if (forceRefresh || future == null || future.isCompletedExceptionally()) {
                lastCheckFailed = false;
                future = CompletableFuture.supplyAsync(UpdateChecker::fetchAvailableUpdate, EXECUTOR)
                        .exceptionally(error -> {
                            lastCheckFailed = true;
                            Frosty.LOGGER.warn("Unable to check Frosty updates", error);
                            return null;
                        });
                checkFuture = future;
            }
        }
        return future;
    }

    private static UpdateInfo fetchAvailableUpdate() {
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Frosty-UpdateChecker")
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Frosty.LOGGER.warn("GitHub update API returned HTTP {}", response.statusCode());
                return null;
            }

            JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
            String tag = stringValue(release, "tag_name");
            String name = stringValue(release, "name");
            ReleaseVersion remote = ReleaseVersion.parse(!tag.isBlank() ? tag : name);
            ReleaseVersion local = ReleaseVersion.parse(localVersion());
            if (remote.compareTo(local) <= 0) return null;

            String gameVersion = SharedConstants.getCurrentVersion().name();
            Asset asset = findAsset(release.getAsJsonArray("assets"), gameVersion);
            String releaseUrl = stringValue(release, "html_url");
            return new UpdateInfo(name.isBlank() ? tag : name, tag, remote, asset, releaseUrl);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub update request failed", exception);
        }
    }

    private static Asset findAsset(JsonArray assets, String gameVersion) {
        if (assets == null) return null;
        String suffix = "+" + gameVersion;
        for (JsonElement element : assets) {
            JsonObject asset = element.getAsJsonObject();
            String name = stringValue(asset, "name");
            if (!name.contains(suffix) || !name.endsWith(".jar")) continue;
            return new Asset(name, stringValue(asset, "browser_download_url"));
        }
        return null;
    }

    private static void notifyStartupResult(UpdateInfo update) {
        if (notificationsSuppressed()) return;
        if (!UPDATE_NOTIFICATION_SENT.compareAndSet(false, true)) return;
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player == null) return;
            if (update == null) {
                sendCheckResult();
            } else {
                sendUpdateNotification(update);
            }
        });
    }

    private static void notifyManualResult(UpdateInfo update) {
        if (update == null) {
            sendCheckResult();
            return;
        }
        sendUpdateNotification(update);
    }

    private static void sendCheckResult() {
        Utils.addModuleMessage(ModuleManager.update.getTransName(),
                lastCheckFailed ? "Unable to check for Frosty updates." : "Frosty is up to date.",
                lastCheckFailed ? "无法检查 Frosty 更新" : "Frosty 已是最新版本");
    }

    private static void sendUpdateNotification(UpdateInfo update) {
        Utils.addModuleMessage("Update", "§aUpdate available: §f" + update.name(), "§a发现新版本: §f" + update.name());
        URI target = update.asset() == null ? URI.create(update.releaseUrl()) : URI.create(update.asset().downloadUrl());
        Component action = Component.literal(update.asset() == null
                        ? "§b§l[Open release/打开发布页]"
                        : "§b§l[Download/下载 " + SharedConstants.getCurrentVersion().name() + "]")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(target)));
        Minecraft.getInstance().player.sendSystemMessage(action);
        if (update.asset() == null) {
            Utils.addModuleMessage("Update", "§eNo matching mod file was published for Minecraft " + SharedConstants.getCurrentVersion().name(),
                    "§e没有适用于 Minecraft " + SharedConstants.getCurrentVersion().name() + " 的模组文件");
        }
    }

    private static boolean notificationsSuppressed() {
        Update module = ModuleManager.update;
        return module != null && module.ignoreNotifications.isToggled();
    }

    private static String localVersion() {
        return Frosty.MOD_VERSION;
    }

    private static String stringValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private record Asset(String name, String downloadUrl) { }
    private record UpdateInfo(String name, String tag, ReleaseVersion version, Asset asset, String releaseUrl) { }

    private record ReleaseVersion(int major, int minor, int patch, int stage, int qualifier) implements Comparable<ReleaseVersion> {
        private static final int ALPHA = 1;
        private static final int BETA = 2;
        private static final int STABLE = 3;
        private static final int HOTFIX = 4;

        static ReleaseVersion parse(String source) {
            Matcher matcher = VERSION_PATTERN.matcher(source == null ? "" : source);
            if (!matcher.find()) return new ReleaseVersion(0, 0, 0, ALPHA, 0);
            String label = matcher.group(4);
            int stage = label == null ? STABLE : switch (label.toLowerCase()) {
                case "a", "alpha" -> ALPHA;
                case "b", "beta" -> BETA;
                case "hf", "hotfix" -> HOTFIX;
                default -> STABLE;
            };
            return new ReleaseVersion(number(matcher.group(1)), number(matcher.group(2)), number(matcher.group(3)),
                    stage, number(matcher.group(5)));
        }

        @Override
        public int compareTo(ReleaseVersion other) {
            int compare = Integer.compare(major, other.major);
            if (compare == 0) compare = Integer.compare(minor, other.minor);
            if (compare == 0) compare = Integer.compare(patch, other.patch);
            if (compare == 0) compare = Integer.compare(stage, other.stage);
            if (compare == 0) compare = Integer.compare(qualifier, other.qualifier);
            return compare;
        }

        private static int number(String value) {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        }
    }
}
