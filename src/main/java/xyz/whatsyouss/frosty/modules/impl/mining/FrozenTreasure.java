package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FrozenTreasure extends Module {
    private ButtonSetting esp, nuker, ignoreIcebait;
    private final Map<BlockPos, ArmorStand> treasureMap = new HashMap<>();
    private long lastBreakTime;
    private BlockPos lastTargetPos;

    private static final Map<String, Color> TREASURE_COLORS = new HashMap<String, Color>() {{
        put("Ice Bait", new Color(0xFFFFFF));
        put("Enchanted Ice", new Color(0x55FF55));
        put("Glacial Fragment", new Color(0xAA00AA));
        put("Packed Ice", new Color(0xFFFFFF));
        put("White Gift", new Color(0xFFFFFF));
        put("Green Gift", new Color(0x55FF55));
        put("Red Gift", new Color(0x5555FF));
        put("Glacial Talisman", new Color(0xFFFFFF));
        put("Enchanted Packed Ice", new Color(0x5555FF));
        put("Einary's Red Hoodie", new Color(0x5555FF));
        put("Glowy Chum Bait", new Color(0x55FF55));
        put("Frozen Bait", new Color(0x5555FF));
    }};

    public FrozenTreasure() {
        super("FrozenTreasure", "冰洞宝藏", category.Mining);
        this.registerSetting(esp = new ButtonSetting("ESP", true));
        this.registerSetting(nuker = new ButtonSetting("Nuker", false));
        this.registerSetting(ignoreIcebait = new ButtonSetting("Ignore Ice Bait", true));
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck() || !isInGlacialCave()) {
            treasureMap.clear();
            return;
        }

        updateTreasureCache();

        if (nuker.isToggled()) {
            findAndBreakNearbyTreasures();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck() || !isInGlacialCave() || !esp.isToggled()) return;

        for (Map.Entry<BlockPos, ArmorStand> entry : treasureMap.entrySet()) {
            BlockPos pos = entry.getKey();
            ArmorStand stand = entry.getValue();
            if (ignoreIcebait.isToggled() && isIceBait(stand)) continue;

            Color color = getColorForTreasure(stand);
            RenderUtils.drawBlockOutline(event.getMatrix(), pos, color, 1.5f, false);
            if (pos.equals(lastTargetPos)) {
                RenderUtils.drawBlockFilled(event.getMatrix(), pos,
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 180), 0.2f, false);
            }
        }
    }

    private void findAndBreakNearbyTreasures() {
        BlockPos playerPos = mc.player.blockPosition();

        for (int x = -6; x <= 6; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -6; z <= 6; z++) {
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    if (distance > 6) continue;

                    BlockPos targetPos = playerPos.offset(x, y, z);

                    if (treasureMap.containsKey(targetPos)) {
                        ArmorStand stand = treasureMap.get(targetPos);

                        if (ignoreIcebait.isToggled() && isIceBait(stand)) {
                            continue;
                        }

                        if (mc.level.getBlockState(targetPos).getBlock() == Blocks.ICE) {
                            lastTargetPos = targetPos;
                            breakBlock(targetPos);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void breakBlock(BlockPos pos) {
        if (mc.gameMode == null || mc.player == null) return;

        if (System.currentTimeMillis() - lastBreakTime < 50) {
            return;
        }

        if (!pos.equals(lastTargetPos)) {
            lastTargetPos = pos;
        }

        mc.gameMode.continueDestroyBlock(pos, BlockUtils.getDirection(pos));
        mc.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos, BlockUtils.getDirection(pos)));
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.player.connection.send(
                new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, BlockUtils.getDirection(pos)));

        lastBreakTime = System.currentTimeMillis();
    }

    private void updateTreasureCache() {
        treasureMap.clear();
        Vec3 playerPos = mc.player.position();
        AABB searchBox = new AABB(
                playerPos.x - 100, playerPos.y - 128, playerPos.z - 100,
                playerPos.x + 100, playerPos.y + 128, playerPos.z + 100
        );

        for (ArmorStand stand : mc.level.getEntitiesOfClass(ArmorStand.class, searchBox, this::isValidTreasure)) {
            BlockPos treasurePos = stand.blockPosition().above().above();
            if (mc.level.getBlockState(treasurePos).getBlock() == Blocks.ICE ||
                    mc.level.getBlockState(treasurePos).getBlock() == Blocks.PACKED_ICE) {
                treasureMap.put(treasurePos, stand);
            }
        }
    }

    private boolean isValidTreasure(ArmorStand stand) {
        ItemStack helmet = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet == null || helmet.getCustomName() == null) return false;
        String name = helmet.getHoverName().getString();
        return name.contains("Ice Bait") ||
                name.contains("Enchanted Ice") ||
                name.contains("Glacial Fragment") ||
                name.contains("Packed Ice") ||
                name.contains("White Gift") ||
                name.contains("Green Gift") ||
                name.contains("Red Gift") ||
                name.contains("Glacial Talisman") ||
                name.contains("Enchanted Packed Ice") ||
                name.contains("Einary's Red Hoodie") ||
                name.contains("Glowy Chum Bait") ||
                name.contains("Frozen Bait");
    }

    private boolean isIceBait(ArmorStand stand) {
        ItemStack helmet = stand.getItemBySlot(EquipmentSlot.HEAD);
        return helmet != null && helmet.getCustomName() != null &&
                helmet.getHoverName().getString().contains("Ice Bait");
    }

    private Color getColorForTreasure(ArmorStand stand) {
        ItemStack helmet = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet == null || helmet.getCustomName() == null) {
            return new Color(0xFFFFFF);
        }
        String name = helmet.getHoverName().getString();
        for (Map.Entry<String, Color> entry : TREASURE_COLORS.entrySet()) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new Color(0xFFFFFF);
    }

    private boolean isInGlacialCave() {
        String sidebar = Utils.stripColor(Utils.getScoreboardSidebarLines().toString().toLowerCase());
        return sidebar.contains("glacial c");
    }
}