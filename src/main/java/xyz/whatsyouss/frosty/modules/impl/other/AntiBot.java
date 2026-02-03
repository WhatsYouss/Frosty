package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.realms.dto.PlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import xyz.whatsyouss.frosty.events.impl.EntityJoinEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.*;

public class AntiBot extends Module {
    private static final Map<UUID, Long> joinTimes = new HashMap<>();

    public AntiBot() {
        super("AntiBot", category.Other);
    }

    @EventHandler
    public void onPlayerJoin(EntityJoinEvent event) {
        if (event.entity instanceof PlayerEntity player && player != mc.player) {
            joinTimes.put(player.getUuid(), System.currentTimeMillis());
        }
    }

    public static boolean isBot(PlayerEntity entity) {
        if (!ModuleManager.antiBot.isEnabled()) return false;
        if (entity == mc.player) return false;

        String name = entity.getName().getString();

        boolean inTabList = MinecraftClient.getInstance().getNetworkHandler()
                .getPlayerList()
                .stream()
                .anyMatch(info -> info.getProfile().id().equals(entity.getUuid()));
        if (!inTabList) return true;

        Long spawnTime = joinTimes.get(entity.getUuid());
        if (spawnTime != null && System.currentTimeMillis() - spawnTime < 1000) return true;

        if (entity.maxHurtTime == 0) {
            if (entity.getHealth() == 20.0f) {
                if (entity.getDisplayName() != null) {
                    String unformattedText = entity.getDisplayName().getString();
                    if (unformattedText.length() == 10 && unformattedText.charAt(0) != '§') {
                        return true;
                    }
                    if (unformattedText.length() == 12 && entity.isSleeping() && unformattedText.charAt(0) == '§') {
                        return true;
                    }
                    if (unformattedText.length() >= 7 && unformattedText.charAt(2) == '[' && unformattedText.charAt(3) == 'N' && unformattedText.charAt(6) == ']') {
                        return true;
                    }
                    if (entity.getName().toString().contains(" ")) {
                        return true;
                    }
                }
            } else if (entity.isInvisible()) {
                if (entity.getDisplayName() != null) {
                    String unformattedText = entity.getDisplayName().getString();
                    if (unformattedText.length() >= 3 && unformattedText.charAt(0) == '§' && unformattedText.charAt(1) == 'c') {
                        return true;
                    }
                }
            }
        }

        if (entity.getHealth() != 20.0f && entity.getName().getString().startsWith("§c")) {
            return true;
        }

        if (entity.isDead()) {
            return true;
        }

        return false;
    }

}
