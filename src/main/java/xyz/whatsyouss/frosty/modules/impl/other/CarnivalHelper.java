package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.*;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.*;
import xyz.whatsyouss.frosty.utility.skyblock.HeadTextures;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarnivalHelper extends Module {

    private ButtonSetting autoFruitDigging;
    private SliderSetting fishLatency, zombieShootoutLatency, rotateSmoothing;

    // 0=None, 1=Fruit Digging, 2=Catch A Fish, 3=Zombie Shootout
    private int currentPlay;

    // Fruit Digging
    private BlockPos fdCurrentTarget;
    private Map<BlockPos, Integer> fdBombInfoMap = new HashMap<>();
    // 1=no bomb, 2=may bomb, 3=bomb
    private Map<BlockPos, Integer> fdBlockStates = new HashMap<>();
    private int fdShovelSlot = -1;
    private static final Pattern BOMB_PATTERN = Pattern.compile("MINES! There (?:are|is) (\\d+) bombs? hidden");

    // Zombie Shootout
    private int zsStickSlot = -1;
    private final List<BlockPos> zsLampPos = List.of(
            new BlockPos(-96, 76, 61),
            new BlockPos(-99, 77, 62),
            new BlockPos(-102, 75, 62),
            new BlockPos(-106, 77, 61),
            new BlockPos(-109, 75, 60),
            new BlockPos(-112, 76, 58),
            new BlockPos(-115, 77, 55),
            new BlockPos(-117, 76, 52),
            new BlockPos(-118, 76, 49),
            new BlockPos(-119, 75, 45),
            new BlockPos(-119, 77, 42),
            new BlockPos(-118, 76, 39)
    );

    // Catch A Fish
    private int cfRodSlot = -1;
    private boolean isCast = false;
    private boolean reeling = false;
    private long castTime;
    private ArmorStand castTarget;

    private Map<UUID, TargetData> targetDataMap = new HashMap<>();
    private Object currentTarget;
    private Vec3 targetAimPos;

    private static class TargetData {
        Vec3 pos1;
        Vec3 pos2;
        long tick1;
        boolean hasPos1;
        boolean hasPos2;

        TargetData() {
            hasPos1 = false;
            hasPos2 = false;
        }
    }

    public CarnivalHelper() {
        super("CarnivalHelper", category.Other);

//        this.registerSetting(autoFruitDigging = new ButtonSetting("Auto fruit digging", true));
        this.registerSetting(fishLatency = new SliderSetting("Fish latency", 75, 50, 300, 25));
        this.registerSetting(zombieShootoutLatency = new SliderSetting("Shoot latency", 150, 50, 300, 25));
        this.registerSetting(rotateSmoothing = new SliderSetting("Rotate smoothing", 0, 0, 10, 1));
    }

    @Override
    public void onDisable() {
        Rotations.cancelRotate(this);
    }

    @EventHandler
    public void onReceiveMessage(ReceiveMessageEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (event.getMessage().getString().contains("MINES! There")) {
            Matcher matcher = BOMB_PATTERN.matcher(event.getMessage().getString());
            if (matcher.find()) {
                int bombCount = Integer.parseInt(matcher.group(1));
                if (fdCurrentTarget != null) {
                    fdBombInfoMap.put(fdCurrentTarget, bombCount);
                    updateBlockStates();
                }
            }
        } else if (event.getMessage().getString().contains("Your Score")) {
            fdBombInfoMap.clear();
            fdBlockStates.clear();
            fdShovelSlot = -1;
            fdCurrentTarget = null;
            targetDataMap.clear();
            currentTarget = null;
            targetAimPos = null;
            zsStickSlot = -1;
            cfRodSlot = -1;
            isCast = false;
            reeling = false;
            castTarget = null;
            Rotations.cancelRotate(this);
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        String sidebar = Utils.stripColor(Utils.getScoreboardSidebarLines().toString().toLowerCase());
        if (sidebar.contains("fruit dig")) {
            currentPlay = 1;
        } else if (sidebar.contains("catch a fish")) {
            currentPlay = 2;
        } else if (sidebar.contains("zombie sh")) {
            currentPlay = 3;
        } else {
            currentPlay = 0;
        }
        if (currentPlay == 1) {
            for (int i = 0; i < 9; i++) {
                var stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;

                String itemName = Utils.getLiteral(stack.getCustomName() != null ? stack.getCustomName().toString() : "").toLowerCase();

                if ((itemName.contains("carnival shovel")) && fdShovelSlot == -1) {
                    fdShovelSlot = i;
                }
            }
            if (fdShovelSlot == -1) {
//                Utils.addChatMessage("Carnival Shovel not found!");
                return;
            }
        } else if (currentPlay == 2) {
            for (int i = 0; i < 9; i++) {
                var stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;

                String itemName = Utils.getLiteral(stack.getCustomName() != null ? stack.getCustomName().toString() : "").toLowerCase();

                if ((itemName.contains("carnival rod")) && cfRodSlot == -1) {
                    cfRodSlot = i;
                }
            }
            if (cfRodSlot == -1) {
//                Utils.addChatMessage("Carnival Rod not found!");
//                return;
            } else {
                mc.player.getInventory().setSelectedSlot(cfRodSlot);
                handleCarnivalAimGame();
            }
        } else if (currentPlay == 3) {
            for (int i = 0; i < 9; i++) {
                var stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;

                String itemName = Utils.getLiteral(stack.getCustomName() != null ? stack.getCustomName().toString() : "").toLowerCase();

                if ((itemName.contains("carnival dart tube")) && zsStickSlot == -1) {
                    zsStickSlot = i;
                }
            }
            if (zsStickSlot == -1) {
//                Utils.addChatMessage("Carnival Dart Tube not found!");
//                return;
            } else {
                mc.player.getInventory().setSelectedSlot(zsStickSlot);
                handleCarnivalAimGame();
            }
        } else {
            targetDataMap.clear();
            currentTarget = null;
            targetAimPos = null;
            isCast = false;
            reeling = false;
            castTarget = null;
        }
    }

    private void handleCarnivalAimGame() {
        long currentTick = mc.level.getGameTime();

        if (currentPlay == 2 && isCast) {
            if (castTarget == null || castTarget.isRemoved() || (mc.player.fishing == null && currentTick - castTime > 10)) {
                isCast = false;
                reeling = false;
                currentTarget = null;
                targetAimPos = null;
                return;
            }

            if (castTarget != null) {
                if (mc.player.fishing != null) {
                    AABB hookBox = mc.player.fishing.getBoundingBox().inflate(2.0, 2.0, 2.0);
                    List<ArmorStand> stands = mc.level.getEntitiesOfClass(ArmorStand.class, hookBox, entity -> true);

                    for (ArmorStand stand : stands) {
                        Component customName = stand.getCustomName();
                        String name = customName != null ? customName.getString() : "";
                        if (name.equals("|||||||||||||||||||||||||||||||||||||||||||||||")) {
                            castTarget = stand;
                            reeling = true;
                            break;
                        }
                    }
                }

                targetAimPos = castTarget.getEyePosition().add(0, 0.2, 0);
                aimAtTarget(targetAimPos);

                if (reeling) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.player.swing(InteractionHand.MAIN_HAND);

                    if (castTarget != null) {
                        Component customName = castTarget.getCustomName();
                        String name = customName != null ? customName.getString().toLowerCase() : "";
                        if (name.contains("points")) {
                            isCast = false;
                            reeling = false;
                            targetDataMap.remove(castTarget.getUUID());
                            castTarget = null;
                            currentTarget = null;
                            targetAimPos = null;
                        }
                    }
                    return;
                }

                if (currentTick - castTime > 36) {
                    boolean hasFullBar = false;
                    if (mc.player.fishing != null) {
                        AABB hookBox = mc.player.fishing.getBoundingBox().inflate(2.0);
                        List<ArmorStand> stands = mc.level.getEntitiesOfClass(ArmorStand.class, hookBox, entity -> true);
                        for (ArmorStand stand : stands) {
                            Component customName = stand.getCustomName();
                            String name = customName != null ? customName.getString() : "";
                            if (name.equals("|||||||||||||||||||||||||||||||||||||||||||||||")) {
                                hasFullBar = true;
                                break;
                            }
                        }
                    }

                    if (!hasFullBar) {
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        isCast = false;
                        reeling = false;
                        if (castTarget != null) {
                            targetDataMap.remove(castTarget.getUUID());
                        }
                        castTarget = null;
                        currentTarget = null;
                        targetAimPos = null;
                    }
                }
                return;
            }
        }

        BlockPos litLamp = null;
        if (currentPlay == 3) {
            for (BlockPos lampPos : zsLampPos) {
                var state = mc.level.getBlockState(lampPos);
                if (state.getBlock() == Blocks.REDSTONE_LAMP && state.getValue(RedstoneLampBlock.LIT)) {
                    litLamp = lampPos;
                    break;
                }
            }
        }

        if (litLamp != null) {
            currentTarget = litLamp;
            targetAimPos = new Vec3(litLamp.getX() + 0.5, litLamp.getY() + 0.8, litLamp.getZ() + 0.5);
        } else {
            Vec3 playerPos = mc.player.position();
            double searchRadius = (currentPlay == 2) ? 16 : 32;
            AABB searchBox = new AABB(
                    playerPos.x() - searchRadius, playerPos.y() - searchRadius, playerPos.z() - searchRadius,
                    playerPos.x + searchRadius, playerPos.y() + searchRadius, playerPos.z() + searchRadius
            );

            Object bestTarget = null;
            int bestPriority = -1;
            double bestDistance = Double.MAX_VALUE;

            if (currentPlay == 3) {
                List<Zombie> zombies = mc.level.getEntitiesOfClass(Zombie.class, searchBox, entity -> true);

                for (Zombie zombie : zombies) {
                    var headStack = zombie.getItemBySlot(EquipmentSlot.HEAD);
                    if (headStack.isEmpty()) continue;

                    int priority = getHelmetPriority(headStack.getItem());
                    if (priority <= 0) continue;

                    updateTargetData(zombie, currentTick);

                    double dist = playerPos.distanceTo(zombie.getEyePosition());
                    if (priority > bestPriority || (priority == bestPriority && dist < bestDistance)) {
                        bestPriority = priority;
                        bestDistance = dist;
                        bestTarget = zombie;
                    }
                }
            } else if (currentPlay == 2) {
                List<ArmorStand> stands = mc.level.getEntitiesOfClass(ArmorStand.class, searchBox, entity -> true);

                for (ArmorStand stand : stands) {
                    var headStack = stand.getItemBySlot(EquipmentSlot.HEAD);
                    if (headStack.isEmpty() || !headStack.is(Items.PLAYER_HEAD)) continue;

                    Component customName = stand.getCustomName();
                    String name = customName != null ? customName.getString() : "";
                    if (name.contains("points")) continue;

                    Vec3 targetPos = stand.getEyePosition().add(0, 0.2, 0);
                    float[] rotations = RotationUtils.getYawPitchTo(mc.player.getEyePosition(), targetPos);
                    float targetYaw = normalizeAngle(rotations[0]);
                    if (targetYaw < -90 || targetYaw > -45) continue;

                    String texture = ItemUtils.getHeadTexture(headStack);
                    int priority = 0;
                    if (texture != null && texture.equals(HeadTextures.CARNIVAL_YELLOW_FISH)) {
                        priority = 3;
                    } else if (texture != null && texture.equals(HeadTextures.CARNIVAL_BLUE_FISH)) {
                        priority = 2;
                    } else if (texture != null && texture.equals(HeadTextures.CARNIVAL_RED_FISH)) {
                        priority = 1;
                    }
                    if (priority <= 0) continue;

                    updateTargetData(stand, currentTick);

                    double dist = playerPos.distanceTo(stand.getEyePosition());
                    if (priority > bestPriority || (priority == bestPriority && dist < bestDistance)) {
                        bestPriority = priority;
                        bestDistance = dist;
                        bestTarget = stand;
                    }
                }
            }

            if (bestTarget != null) {
                currentTarget = bestTarget;
                if (currentPlay == 2) {
                    UUID uuid = ((Entity) bestTarget).getUUID();
                    TargetData data = targetDataMap.get(uuid);
                    if (data != null && data.hasPos2) {
                        Vec3 movement = data.pos2.subtract(data.pos1);
                        double latencyFactor = fishLatency.getInput() / 100.0;
                        Vec3 offset = movement.multiply(latencyFactor, latencyFactor, latencyFactor);
                        targetAimPos = data.pos2.add(offset).add(0, 0.2, 0);
                    } else {
                        targetAimPos = ((ArmorStand) bestTarget).getEyePosition().add(0, 0.2, 0);
                    }
                } else if (currentPlay == 3) {
                    UUID uuid = ((Entity) bestTarget).getUUID();
                    TargetData data = targetDataMap.get(uuid);
                    if (data != null && data.hasPos2) {
                        Vec3 movement = data.pos2.subtract(data.pos1);
                        double latencyFactor = zombieShootoutLatency.getInput() / 100.0;
                        Vec3 offset = movement.multiply(latencyFactor, latencyFactor, latencyFactor);
                        targetAimPos = data.pos2.add(offset);
                    } else {
                        currentTarget = null;
                        targetAimPos = null;
                    }
                }
            } else {
                currentTarget = null;
                targetAimPos = null;
            }
        }

        if (targetAimPos != null) {
            aimAtTarget(targetAimPos);

            float normalizedServerYaw = normalizeAngle(Rotations.serverYaw);
            float normalizedTargetYaw = normalizeAngle(RotationUtils.getYawPitchTo(mc.player.getEyePosition(), targetAimPos)[0]);
            float yawDiff = Math.abs(normalizedServerYaw - normalizedTargetYaw);
            float pitchDiff = Math.abs((Rotations.serverPitch) -
                    RotationUtils.getYawPitchTo(mc.player.getEyePosition(), targetAimPos)[1]);

            if (yawDiff < 1.3f && pitchDiff < 1.3f) {
                if (currentPlay == 3) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    if (currentTarget instanceof BlockPos) {
                        currentTarget = null;
                        targetAimPos = null;
                    } else if (currentTarget instanceof Zombie) {
                        targetDataMap.remove(((Zombie) currentTarget).getUUID());
                        currentTarget = null;
                        targetAimPos = null;
                    }
                } else if (currentPlay == 2 && !isCast) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    isCast = true;
                    castTime = currentTick;
                    castTarget = (ArmorStand) currentTarget;
                }
            }
        }
    }

    private void aimAtTarget(Vec3 targetPos) {
        Vec3 from = mc.player.getEyePosition();
        float[] rotations = RotationUtils.getYawPitchTo(from, targetPos);
        float targetYaw = rotations[0];
        float targetPitch = rotations[1];

        if (rotateSmoothing.getInput() == 0) {
            Rotations.setRotate(this, targetYaw, targetPitch, 3);
        } else {
            Rotations.setSmoothRotate(this, targetYaw, targetPitch, 3, (float) rotateSmoothing.getInput());
        }
    }

    private void updateTargetData(Entity entity, long currentTick) {
        UUID uuid = entity.getUUID();
        TargetData data = targetDataMap.computeIfAbsent(uuid, k -> new TargetData());

        Vec3 headPos = entity.getEyePosition();
        if (entity instanceof ArmorStand) {
            headPos = headPos.add(0, 0.2, 0);
        } else if (entity instanceof Zombie) {
            headPos = headPos.add(0, 0.6, 0);
        }

        int interval = 6;

        if (!data.hasPos1) {
            data.pos1 = headPos;
            data.tick1 = currentTick;
            data.hasPos1 = true;
        } else if (!data.hasPos2 && currentTick - data.tick1 >= interval) {
            data.pos2 = headPos;
            data.hasPos2 = true;
        } else if (data.hasPos2) {
            if (currentTick - data.tick1 >= interval) {
                data.pos1 = data.pos2;
                data.pos2 = headPos;
                data.tick1 = currentTick;
            }

            if (currentTick - data.tick1 > 20) {
                data.hasPos1 = false;
                data.hasPos2 = false;
            }
        }
    }

    private float normalizeAngle(float angle) {
        angle = angle % 360.0f;
        if (angle > 180.0f) {
            angle -= 360.0f;
        } else if (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    private int getHelmetPriority(Object item) {
        if (item == Items.DIAMOND_HELMET) return 4;
        if (item == Items.GOLDEN_HELMET) return 3;
        if (item == Items.IRON_HELMET) return 2;
        if (item == Items.LEATHER_HELMET) return 1;
        return 0;
    }

    @EventHandler
    public void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (isInRange(event.blockPos) && currentPlay == 1) {
            fdCurrentTarget = event.blockPos;
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        if (currentPlay == 1) {
            BlockPos minPos = new BlockPos(-112, 72, 19);
            BlockPos maxPos = new BlockPos(-106, 72, 25);

            for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, minPos.getY(), z);
                    if (isSand(pos)) {
                        Integer state = fdBlockStates.get(pos);
                        if (state != null) {
                            switch (state) {
                                case 1:
                                    RenderUtils.drawBlockFilled(event.getMatrix(), pos, Color.GREEN, 0.1f, false);
                                    break;
                                case 2:
                                    RenderUtils.drawBlockFilled(event.getMatrix(), pos, Color.YELLOW, 0.1f, false);
                                    break;
                                case 3:
                                    RenderUtils.drawBlockFilled(event.getMatrix(), pos, Color.RED, 0.1f, false);
                                    break;
                            }
                        }
                    }
                }
            }

            if (fdCurrentTarget != null && isInRange(fdCurrentTarget)) {
                RenderUtils.drawBlockFilled(event.getMatrix(), fdCurrentTarget, Color.BLACK, 0.3f, false);
            }
        }
    }

    private void updateBlockStates() {
        fdBlockStates.clear();

        for (Map.Entry<BlockPos, Integer> entry : fdBombInfoMap.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos == null) continue;
            int bombCount = entry.getValue();

            if (bombCount == 0) {
                for (BlockPos surrounding : getSurroundingBlocks(pos)) {
                    if (isInRange(surrounding) && isSand(surrounding)) {
                        fdBlockStates.put(surrounding, 1);
                    }
                }
            } else {
                List<BlockPos> surroundingSands = new ArrayList<>();

                for (BlockPos surrounding : getSurroundingBlocks(pos)) {
                    if (isInRange(surrounding) && isSand(surrounding)) {
                        surroundingSands.add(surrounding);
                    }
                }

                if (surroundingSands.size() == bombCount) {
                    for (BlockPos sandPos : surroundingSands) {
                        fdBlockStates.put(sandPos, 3);
                    }
                } else if (surroundingSands.size() > bombCount) {
                    for (BlockPos sandPos : surroundingSands) {
                        if (!fdBlockStates.containsKey(sandPos) || fdBlockStates.get(sandPos) == 0 || fdBlockStates.get(sandPos) == 2) {
                            fdBlockStates.put(sandPos, 2);
                        }
                    }
                }
            }
        }
    }

    private boolean isInRange(BlockPos pos) {
        BlockPos minPos = new BlockPos(-112, 72, 19);
        BlockPos maxPos = new BlockPos(-106, 72, 25);
        return pos.getX() >= minPos.getX() && pos.getX() <= maxPos.getX() &&
                pos.getZ() >= minPos.getZ() && pos.getZ() <= maxPos.getZ() &&
                pos.getY() == minPos.getY();
    }

    private boolean isSand(BlockPos pos) {
        return mc.level.getBlockState(pos).getBlock() == Blocks.SAND;
    }

    private List<BlockPos> getSurroundingBlocks(BlockPos center) {
        List<BlockPos> surrounding = new ArrayList<>();
        if (center == null) return surrounding;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                surrounding.add(center.offset(dx, 0, dz));
            }
        }
        return surrounding;
    }
}