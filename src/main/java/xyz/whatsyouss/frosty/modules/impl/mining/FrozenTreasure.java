package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.BlockDataObject;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrozenTreasure extends Module {
    private ButtonSetting esp, nuker, ignoreIcebait;
    private final Map<BlockPos, ArmorStandEntity> treasureMap = new HashMap<>();
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
        super("FrozenTreasure", category.Mining);
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

        for (Map.Entry<BlockPos, ArmorStandEntity> entry : treasureMap.entrySet()) {
            BlockPos pos = entry.getKey();
            ArmorStandEntity stand = entry.getValue();
            if (ignoreIcebait.isToggled() && isIceBait(stand)) continue;

            Color color = getColorForTreasure(stand);
            RenderUtils.drawBlockOutline(event.getMatrix(), pos, color, 1.5f);
            if (pos.equals(lastTargetPos)) {
                RenderUtils.drawBlockFilled(event.getMatrix(), pos,
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 180), 0.2f);
            }
        }
    }

    private void findAndBreakNearbyTreasures() {
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -6; x <= 6; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -6; z <= 6; z++) {
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    if (distance > 6) continue;

                    BlockPos targetPos = playerPos.add(x, y, z);

                    if (treasureMap.containsKey(targetPos)) {
                        ArmorStandEntity stand = treasureMap.get(targetPos);

                        if (ignoreIcebait.isToggled() && isIceBait(stand)) {
                            continue;
                        }

                        if (mc.world.getBlockState(targetPos).getBlock() == Blocks.ICE) {
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
        if (mc.interactionManager == null || mc.player == null) return;

        if (System.currentTimeMillis() - lastBreakTime < 50) {
            return;
        }

        if (!pos.equals(lastTargetPos)) {
            lastTargetPos = pos;
        }

        mc.interactionManager.updateBlockBreakingProgress(pos, BlockUtils.getDirection(pos));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                pos, BlockUtils.getDirection(pos)));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.networkHandler.sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, BlockUtils.getDirection(pos)));

        lastBreakTime = System.currentTimeMillis();
    }

    private void updateTreasureCache() {
        treasureMap.clear();
        Vec3d playerPos = mc.player.getEntityPos();
        Box searchBox = new Box(
                playerPos.x - 100, playerPos.y - 128, playerPos.z - 100,
                playerPos.x + 100, playerPos.y + 128, playerPos.z + 100
        );

        for (ArmorStandEntity stand : mc.world.getEntitiesByClass(ArmorStandEntity.class, searchBox, this::isValidTreasure)) {
            BlockPos treasurePos = stand.getBlockPos().up().up();
            if (mc.world.getBlockState(treasurePos).getBlock() == Blocks.ICE ||
                    mc.world.getBlockState(treasurePos).getBlock() == Blocks.PACKED_ICE) {
                treasureMap.put(treasurePos, stand);
            }
        }
    }

    private boolean isValidTreasure(ArmorStandEntity stand) {
        ItemStack helmet = stand.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        if (helmet == null || helmet.getCustomName() == null) return false;
        String name = helmet.getName().getString();
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

    private boolean isIceBait(ArmorStandEntity stand) {
        ItemStack helmet = stand.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        return helmet != null && helmet.getCustomName() != null &&
                helmet.getName().getString().contains("Ice Bait");
    }

    private Color getColorForTreasure(ArmorStandEntity stand) {
        ItemStack helmet = stand.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        if (helmet == null || helmet.getCustomName() == null) {
            return new Color(0xFFFFFF);
        }
        String name = helmet.getName().getString();
        for (Map.Entry<String, Color> entry : TREASURE_COLORS.entrySet()) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new Color(0xFFFFFF);
    }

    private boolean isInGlacialCave() {
        List<Text> sidebar = Utils.getScoreboardSidebar();
        return sidebar.toString().toLowerCase().contains("glacial c");
    }
}