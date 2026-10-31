package xyz.whatsyouss.frosty.modules.impl.fun;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.impl.other.AntiBot;
import xyz.whatsyouss.frosty.utility.Utils;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MurderMystery extends Module {

    private final Set<UUID> murderers = new HashSet<>();
    private final Set<UUID> detectives = new HashSet<>();
    private final Set<UUID> innocents = new HashSet<>();

    private String currentGameId = "";
    private boolean renderEsp = false;

    private final Pattern serverIdPattern = Pattern.compile("m\\d+[A-Za-z0-9]+");

    public MurderMystery() {
        super("MurderMystery", "密室杀手", category.Fun);
    }

    @Override
    public void onDisable() {
        clearAllData();
        currentGameId = "";
        renderEsp = false;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        String sidebar = Utils.stripColor(Utils.getScoreboardSidebarLines().toString());

        if (!sidebar.contains("MURDER MYSTERY") || !sidebar.contains("Time Left")) {
            renderEsp = false;
            return;
        }

        renderEsp = true;

        Matcher matcher = serverIdPattern.matcher(sidebar);
        if (matcher.find()) {
            String serverId = matcher.group();
            if (!serverId.equals(currentGameId)) {
                currentGameId = serverId;
                clearAllData();
            }
        }

        for (Player player : mc.level.players()) {
            if (player == mc.player || AntiBot.isBot(player)) {
                continue;
            }

            UUID uuid = player.getUUID();

            if (murderers.contains(uuid)) {
                continue;
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            boolean holdsMurderItem = isMurderItem(mainHand.getItem()) || isMurderItem(offHand.getItem());
            boolean holdsDetectiveItem = mainHand.is(Items.BOW) || mainHand.is(Items.ARROW)
                    || offHand.is(Items.BOW) || offHand.is(Items.ARROW);

            if (holdsMurderItem) {
                if (!murderers.contains(uuid)) {
                    murderers.add(uuid);
                    detectives.remove(uuid);
                    innocents.remove(uuid);

                    String playerName = player.getName().getString();
                    int distance = (int) mc.player.distanceTo(player);

                    mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 3.0F, 1.0F);
                    Utils.addModuleMessage(this.getTransName(), "§bMurderer detected: §d" + playerName + " §f(§b" + distance + "m§f)");
                }
            } else if (holdsDetectiveItem) {
                if (!detectives.contains(uuid)) {
                    detectives.add(uuid);
                    innocents.remove(uuid);
                }
            } else {
                if (!detectives.contains(uuid) && !innocents.contains(uuid)) {
                    innocents.add(uuid);
                }
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!renderEsp || !Utils.nullCheck()) {
            return;
        }

        float partialTicks = event.getDelta();

        for (Player player : mc.level.players()) {
            if (player == mc.player || AntiBot.isBot(player)) {
                continue;
            }

            UUID uuid = player.getUUID();
            Color renderColor = null;

            if (murderers.contains(uuid)) {
                renderColor = Color.RED;
            } else if (detectives.contains(uuid)) {
                renderColor = Color.BLUE;
            } else if (innocents.contains(uuid)) {
                renderColor = Color.GREEN;
            }

            if (renderColor != null) {
                double offsetX = (player.xo + (player.getX() - player.xo) * partialTicks) - player.getX();
                double offsetY = (player.yo + (player.getY() - player.yo) * partialTicks) - player.getY();
                double offsetZ = (player.zo + (player.getZ() - player.zo) * partialTicks) - player.getZ();
                AABB box = player.getBoundingBox().move(offsetX, offsetY, offsetZ);

                RenderUtils.drawBoxFilled(
                        event.getMatrix(),
                        box,
                        new Color(renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), 100),
                        false
                );
            }
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity itemEntity) {
                if (itemEntity.getItem().is(Items.GOLD_INGOT)) {
                    double offsetX = (itemEntity.xo + (itemEntity.getX() - itemEntity.xo) * partialTicks) - itemEntity.getX();
                    double offsetY = (itemEntity.yo + (itemEntity.getY() - itemEntity.yo) * partialTicks) - itemEntity.getY();
                    double offsetZ = (itemEntity.zo + (itemEntity.getZ() - itemEntity.zo) * partialTicks) - itemEntity.getZ();
                    AABB box = itemEntity.getBoundingBox().move(offsetX, offsetY, offsetZ).inflate(0.15);

                    RenderUtils.drawBoxFilled(
                            event.getMatrix(),
                            box,
                            new Color(255, 255, 0, 100),
                            false
                    );
                }
            }
        }
    }

    private boolean isMurderItem(Item item) {
        if (item == null || item == Items.AIR) return false;

        if (item == Items.IRON_SWORD || item == Items.STONE_SWORD || item == Items.WOODEN_SWORD || item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD ||
                item == Items.WOODEN_AXE || item == Items.GOLDEN_AXE || item == Items.DIAMOND_AXE ||
                item == Items.GOLDEN_PICKAXE || item == Items.DIAMOND_PICKAXE ||
                item == Items.IRON_SHOVEL || item == Items.STONE_SHOVEL || item == Items.GOLDEN_SHOVEL || item == Items.DIAMOND_SHOVEL ||
                item == Items.GOLDEN_HOE || item == Items.DIAMOND_HOE || item == Items.SHEARS) {
            return true;
        }

        if (item == Items.CARROT || item == Items.GOLDEN_CARROT || item == Items.COOKIE || item == Items.COOKED_BEEF ||
                item == Items.CHICKEN || item == Items.SALMON || item == Items.BREAD || item == Items.GLISTERING_MELON_SLICE ||
                item == Items.PUMPKIN_PIE) {
            return true;
        }

        if (item == Items.STICK || item == Items.DEAD_BUSH ||
                item == Items.SUGAR_CANE || item == Items.BLAZE_ROD || item == Items.QUARTZ || item == Items.LEATHER ||
                item == Items.NAME_TAG || item == Items.COAL || item == Items.FLINT || item == Items.BONE ||
                item == Items.PRISMARINE_SHARD || item == Items.NETHER_BRICK || item == Items.LAPIS_LAZULI ||
                item == Items.BOOK || item == Items.VINE || item == Items.ROSE_BUSH || item == Items.CRIMSON_FUNGUS) {
            return true;
        }

        String namespaceId = item.getDescriptionId().toLowerCase();
        return namespaceId.contains("music_disc") || namespaceId.contains("boat");
    }

    private void clearAllData() {
        murderers.clear();
        detectives.clear();
        innocents.clear();
    }
}