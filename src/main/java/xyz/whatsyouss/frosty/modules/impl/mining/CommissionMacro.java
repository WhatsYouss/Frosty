package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.utility.commission.pathfinder.Path;
import xyz.whatsyouss.frosty.utility.commission.pathfinder.BlockUtil;
import xyz.whatsyouss.frosty.interfaces.IKeyMapping;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.RotationUtils;
import xyz.whatsyouss.frosty.utility.Rotations;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;

public class CommissionMacro extends Module {

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("(\\d{1,3}(?:\\.\\d+)?)\\s*%");
    private static final List<Vec3> EMISSARIES = List.of(
            new Vec3(42.5, 134.5, 22.5), new Vec3(-72.5, 153.0, -10.5),
            new Vec3(171.5, 150.0, 31.5), new Vec3(58.5, 198.0, -8.5),
            new Vec3(-132.5, 174.0, -50.5));
    private static final long TARGET_TIMEOUT_MS = 3_500L;
    private static final long NO_TARGET_REPATH_MS = 8_000L;
    private static final long ABILITY_COOLDOWN_MS = 61_000L;
    public final SelectSetting commissionArea;
    public final SliderSetting miningSlot;
    public final SliderSetting weaponSlot;
    public final SliderSetting rotateSmoothing;
    public final ButtonSetting prioritizeTitanium;
    public final ButtonSetting useMiningAbility;
    private final CommissionRouteNavigator routeNavigator = new CommissionRouteNavigator();
    private final CommissionVeinPathfinder combatPathfinder = new CommissionVeinPathfinder();
    private final CommissionPathExecutor combatPathExecutor = new CommissionPathExecutor();
    private String[] modes = new String[]{"Dwarven"};
    private String[] modesCN = new String[]{"矮人矿坑"};
    private State state;
    private DwarvenCommission currentCommission;
    private Vec3 pathTarget;
    private int pathFailures;
    private long pathStartedAt;
    private long lastAbilityUse;
    private long noTargetSince;
    private long lastAttack;
    private int completedClaims;
    private BlockPos miningTarget;
    private Vec3 miningHitVec;
    private Block miningBlock;
    private long miningStartedAt;
    private boolean miningStartSent;
    private LivingEntity mobTarget;
    private CompletableFuture<Path> pendingCombatPath;
    private BlockPos combatApproach;
    private long nextCombatRepathAt;
    private int emptyClaimChecks;
    private long nextClaimActionAt;

    public CommissionMacro() {
        super("CommissionMacro", "任务宏", category.Mining);

        registerSetting(commissionArea = new SelectSetting("Commission Area", "任务区域", 0, modes, modesCN));
        registerSetting(miningSlot = new SliderSetting("Mining Slot", 1, 1, 9, 1, "挖掘栏位"));
        registerSetting(weaponSlot = new SliderSetting("Weapon Slot", 2, 1, 9, 1, "武器栏位"));
        registerSetting(rotateSmoothing = new SliderSetting("Rotate Smoothing", 3, 1, 10, 1, "转向丝滑度"));
        registerSetting(prioritizeTitanium = new ButtonSetting("Prioritize Titanium", "优先钛", true));
        registerSetting(useMiningAbility = new ButtonSetting("Use Mining Ability", "使用挖掘技能", false));
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck()) {
            disable();
            return;
        }
        state = State.SCANNING;
        currentCommission = null;
        pathTarget = null;
        pathFailures = 0;
        lastAbilityUse = 0L;
        noTargetSince = 0L;
        completedClaims = 0;
        resetMiningTarget();
        routeNavigator.stop();
        resetCombatPathing();
    }

    @Override
    public void onDisable() {
        routeNavigator.stop();
        resetMiningTarget();
        mobTarget = null;
        resetCombatPathing();
        Rotations.cancelRotate(this);
        releaseKeys();
    }

    @Override
    public String getInfo() {
        if (state == null) return "";
        return currentCommission == null ? state.name() : state.name() + " - " + currentCommission.getName();
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck() || state == null) return;

        if (state != State.CLAIM_GUI && mc.screen != null) return;

        DwarvenCommission parsed = readCurrentCommission();
        if (parsed == DwarvenCommission.CLAIM && currentCommission != DwarvenCommission.CLAIM) {
            currentCommission = parsed;
            resetMiningTarget();
            mobTarget = null;
            resetCombatPathing();
            state = State.PATHING;
            routeNavigator.stop();
        } else if (parsed != null && currentCommission != DwarvenCommission.CLAIM) {
            currentCommission = parsed;
        }

        switch (state) {
            case SCANNING -> tickScanning();
            case PATHING -> tickPathing();
            case MINING -> tickMining();
            case KILLING -> tickKilling();
            case CLAIM_INTERACT -> tickClaimInteract();
            case CLAIM_GUI -> tickClaimGui();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        BlockPos target = routeNavigator.isRunning() ? routeNavigator.getTarget() : combatPathExecutor.getTarget();
        if (target == null) return;

        Color color = Color.CYAN;
        RenderUtils.drawBoxFilled(event.getMatrix(), new net.minecraft.world.phys.AABB(target),
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 80), false);
        RenderUtils.drawBox(event.getMatrix(), target, color, 2.0F, false);
    }

    private void tickScanning() {
        if (currentCommission == null) return;
        state = State.PATHING;
        routeNavigator.stop();
    }

    private void tickPathing() {
        if (currentCommission == null) {
            state = State.SCANNING;
            return;
        }

        Vec3 destination = currentCommission == DwarvenCommission.CLAIM
                ? DwarvenCommission.CLAIM.getClosestWaypointTo(closestEmissaryPosition())
                : currentCommission.getBestWaypoint();
        if (destination == null) return;

        if (pathTarget == null || pathTarget.distanceToSqr(destination) > 1.0) {
            pathTarget = destination;
            pathStartedAt = System.currentTimeMillis();
            routeNavigator.start(blockStandingOn(), destination);
            return;
        }

        if (routeNavigator.tick()) {
            finishPath();
            return;
        }
        if (routeNavigator.failed() || (!routeNavigator.isRunning() && System.currentTimeMillis() - pathStartedAt > 700L)) {
            if (++pathFailures > 2) {
                Utils.addModuleMessage(getName(), "Unable to find a route; waiting for a new commission update.");
                pathTarget = null;
                pathFailures = 0;
                state = State.SCANNING;
            } else {
                pathTarget = null;
            }
        }
    }

    private void finishPath() {
        pathFailures = 0;
        pathTarget = null;
        if (currentCommission == null) {
            state = State.SCANNING;
        } else if (currentCommission == DwarvenCommission.CLAIM) {
            state = State.CLAIM_INTERACT;
        } else if (currentCommission.getKind() == DwarvenCommission.Kind.MINING) {
            state = State.MINING;
            noTargetSince = 0L;
        } else {
            state = State.KILLING;
            noTargetSince = 0L;
        }
    }

    private void tickMining() {
        selectHotbarSlot(miningSlot);
        if (useMiningAbility.isToggled() && System.currentTimeMillis() - lastAbilityUse >= ABILITY_COOLDOWN_MS) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            lastAbilityUse = System.currentTimeMillis();
        }

        if (miningTarget != null && (mc.level.getBlockState(miningTarget).is(Blocks.BEDROCK)
                || System.currentTimeMillis() - miningStartedAt > TARGET_TIMEOUT_MS)) {
            resetMiningTarget();
        }
        if (miningTarget == null) findMiningTarget();

        if (miningTarget == null || miningHitVec == null) {
            if (noTargetSince == 0L) noTargetSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - noTargetSince >= NO_TARGET_REPATH_MS) {
                pathTarget = null;
                state = State.PATHING;
            }
            return;
        }

        noTargetSince = 0L;
        RotationUtils.aimByPos(miningHitVec, (float) rotateSmoothing.getInput());
        if (isLookingAt(miningTarget)) breakMiningBlock();
    }

    private void findMiningTarget() {
        List<MiningNode> nodes = new ArrayList<>();
        BlockPos origin = mc.player.blockPosition();
        boolean titaniumActive = currentCommission != null && currentCommission.isTitanium();
        for (int x = -4; x <= 4; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    int priority = miningPriority(pos, titaniumActive);
                    if (priority <= 0) continue;
                    Vec3 hit = visibleHit(pos);
                    if (hit != null) nodes.add(new MiningNode(pos, hit, priority));
                }
            }
        }
        nodes.stream().max(Comparator.comparingInt(MiningNode::priority)
                        .thenComparingDouble(node -> -mc.player.getEyePosition().distanceToSqr(node.hit())))
                .ifPresent(node -> {
                    miningTarget = node.pos();
                    miningHitVec = node.hit();
                    miningBlock = mc.level.getBlockState(miningTarget).getBlock();
                    miningStartedAt = System.currentTimeMillis();
                    miningStartSent = false;
                });
    }

    private int miningPriority(BlockPos pos, boolean titaniumActive) {
        String id = mc.level.getBlockState(pos).getBlock().getDescriptionId();
        boolean titanium = id.contains("polished_diorite");
        if (titanium) return titaniumActive || prioritizeTitanium.isToggled() ? 100 : 5;
        if (id.contains("gray_wool") || id.contains("cyan_terracotta")) return titaniumActive ? 3 : 10;
        if (id.contains("prismarine")) return titaniumActive ? 2 : 6;
        if (id.contains("light_blue_wool")) return titaniumActive ? 1 : 3;
        return 0;
    }

    private Vec3 visibleHit(BlockPos pos) {
        Vec3 eyes = mc.player.getEyePosition();
        BlockHitResult result = mc.level.clip(new ClipContext(eyes, Vec3.atCenterOf(pos),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos) ? result.getLocation() : null;
    }

    private boolean isLookingAt(BlockPos pos) {
        return mc.hitResult instanceof BlockHitResult result && result.getBlockPos().equals(pos);
    }

    private void breakMiningBlock() {
        Block current = mc.level.getBlockState(miningTarget).getBlock();
        if (current != miningBlock) {
            resetMiningTarget();
            return;
        }
        if (!miningStartSent) {
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, miningTarget, BlockUtils.getDirection(miningTarget)));
            miningStartSent = true;
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void tickKilling() {
        selectHotbarSlot(currentCommission.usesMiningToolForSlayer() ? miningSlot : weaponSlot);
        if (!isValidMob(mobTarget)) {
            resetCombatPathing();
            mobTarget = findMobTarget();
        }

        if (mobTarget == null) {
            resetCombatPathing();
            if (noTargetSince == 0L) noTargetSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - noTargetSince >= NO_TARGET_REPATH_MS) {
                pathTarget = null;
                state = State.PATHING;
            }
            return;
        }
        noTargetSince = 0L;

        double distance = mc.player.distanceTo(mobTarget);
        if (distance > 3.15) {
            tickCombatPathing();
            return;
        }

        resetCombatPathing();
        Vec3 aim = mobTarget.position().add(0, mobTarget.getBbHeight() - 1, 0);
        RotationUtils.aimByPos(aim, (float) rotateSmoothing.getInput());
        float[] targetRotations = RotationUtils.getYawPitchTo(mc.player.getEyePosition(), aim);
        Rotations.setRotate(this, targetRotations[0], targetRotations[1], 10, 0);
        if (System.currentTimeMillis() - lastAttack < 250L) return;
        if (!RotationUtils.isPossibleToHit(mobTarget, 3.4,
                new float[]{Rotations.serverYaw, Rotations.serverPitch})) return;

        mc.player.attack(mobTarget);
        mc.player.connection.send(new ServerboundAttackPacket(mobTarget.getId()));
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastAttack = System.currentTimeMillis();
    }

    private LivingEntity findMobTarget() {
        if (currentCommission == null) return null;
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity living : mc.level.getEntitiesOfClass(LivingEntity.class,
                mc.player.getBoundingBox().inflate(32.0), this::isValidMob)) {
            if (!matchesSlayerMobName(living)) continue;
            double distance = mc.player.distanceToSqr(living);
            if (distance < closestDistance) {
                closest = living;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private boolean matchesSlayerMobName(LivingEntity entity) {
        if (currentCommission == null) return false;
        String entityName = Utils.stripColor(entity.getName().getString()).trim().toLowerCase(Locale.ROOT);
        for (String mobName : currentCommission.getMobNames()) {
            String expectedName = mobName.toLowerCase(Locale.ROOT);
            if (entityName.equals(expectedName) || entityName.startsWith(expectedName + " ")
                    || entityName.contains(expectedName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidMob(LivingEntity entity) {
        return entity != null && !(entity instanceof Player) && entity.isAlive() && !entity.isDeadOrDying() && !entity.isRemoved()
                && mc.player.distanceTo(entity) <= 32.0;
    }

    private void tickCombatPathing() {
        BlockPos approach = standableBeside(mobTarget);
        if (approach == null) {
            resetCombatPathing();
            return;
        }

        long now = System.currentTimeMillis();
        if (!approach.equals(combatApproach)) {
            resetCombatPathing();
            combatApproach = approach;
            queueCombatPath(approach);
            return;
        }

        if (pendingCombatPath != null) {
            if (!pendingCombatPath.isDone()) return;
            try {
                combatPathExecutor.start(pendingCombatPath.join());
            } catch (RuntimeException ignored) {
                combatPathExecutor.start(null);
            }
            pendingCombatPath = null;
            return;
        }

        if (combatPathExecutor.isRunning()) {
            combatPathExecutor.tick();
            return;
        }

        if (now >= nextCombatRepathAt) queueCombatPath(approach);
    }

    private void queueCombatPath(BlockPos approach) {
        combatPathExecutor.stop();
        if (pendingCombatPath != null) pendingCombatPath.cancel(true);
        pendingCombatPath = combatPathfinder.find(blockStandingOn(), approach);
        nextCombatRepathAt = System.currentTimeMillis() + 180L;
    }

    private void resetCombatPathing() {
        Rotations.cancelRotate(this);
        combatPathExecutor.stop();
        if (pendingCombatPath != null) pendingCombatPath.cancel(true);
        pendingCombatPath = null;
        combatApproach = null;
        nextCombatRepathAt = 0L;
    }

    private BlockPos standableBeside(Entity target) {
        BlockPos base = BlockPos.containing(target.getX(), Math.ceil(target.getY() - 0.25) - 1, target.getZ());
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos candidate = base.offset(x, y, z);
                    if (!BlockUtil.canStandOn(candidate) || !canSeeTargetFrom(candidate, target)) continue;
                    if (Vec3.atCenterOf(candidate).distanceToSqr(target.position()) > 9.0) continue;
                    double distance = candidate.distSqr(blockStandingOn());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private boolean canSeeTargetFrom(BlockPos position, Entity target) {
        Vec3 eyePosition = new Vec3(position.getX() + 0.5,
                position.getY() + 1.0 + mc.player.getEyeHeight(mc.player.getPose()), position.getZ() + 0.5);
        BlockHitResult hit = mc.level.clip(new ClipContext(eyePosition, target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void tickClaimInteract() {
        Player emissary = closestEmissary();
        if (emissary == null || mc.player.distanceToSqr(emissary) > 16.0) {
            pathTarget = null;
            state = State.PATHING;
            return;
        }
        RotationUtils.aimByPos(emissary.getEyePosition(), (float) rotateSmoothing.getInput());
        if (mc.gameMode != null) {
            mc.gameMode.interact(mc.player, emissary, new EntityHitResult(emissary), InteractionHand.MAIN_HAND);
            nextClaimActionAt = System.currentTimeMillis() + 3_500L;
            emptyClaimChecks = 0;
            state = State.CLAIM_GUI;
        }
    }

    private void tickClaimGui() {
        if (!(mc.screen instanceof ContainerScreen screen) || !screen.getTitle().getString().contains("Commissions")) {
            if (System.currentTimeMillis() > nextClaimActionAt) state = State.CLAIM_INTERACT;
            return;
        }
        if (System.currentTimeMillis() < nextClaimActionAt) return;

        AbstractContainerMenu menu = screen.getMenu();
        int slot = completedCommissionSlot(menu);
        if (slot != -1) {
            mc.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
            completedClaims++;
            emptyClaimChecks = 0;
            nextClaimActionAt = System.currentTimeMillis() + 140L;
            return;
        }
        if (++emptyClaimChecks < 4) {
            nextClaimActionAt = System.currentTimeMillis() + 140L;
            return;
        }
        mc.player.closeContainer();
        currentCommission = null;
        state = State.SCANNING;
    }

    private int completedCommissionSlot(AbstractContainerMenu menu) {
        for (int i = 0; i < menu.slots.size(); i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.isEmpty() || !Utils.stripColor(stack.getHoverName().getString()).startsWith("Commission"))
                continue;
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            boolean completed = lore.lines().stream()
                    .map(line -> Utils.stripColor(line.getString()).trim().toLowerCase(Locale.ROOT))
                    .anyMatch(line -> line.equals("completed"));
            if (completed) return i;
        }
        return -1;
    }

    private DwarvenCommission readCurrentCommission() {
        Map<DwarvenCommission, Double> commissions = new HashMap<>();
        boolean inSection = false;
        for (String raw : tabListLines()) {
            String line = Utils.stripColor(raw).trim();
            if (!inSection) {
                if (line.equalsIgnoreCase("Commissions:") || line.equalsIgnoreCase("Commissions")) inSection = true;
                continue;
            }
            if (line.isEmpty()) break;
            String upper = line.toUpperCase(Locale.ROOT);
            if (upper.contains("DONE") || upper.contains("COMPLETED")) return DwarvenCommission.CLAIM;
            int separator = line.indexOf(':');
            String name = separator >= 0 ? line.substring(0, separator).trim() : line;
            DwarvenCommission commission = DwarvenCommission.fromName(name);
            if (commission == null) continue;
            double progress = progressOf(line);
            if (progress >= 0.999) return DwarvenCommission.CLAIM;
            commissions.merge(commission, progress, Math::max);
        }
        return DwarvenCommission.selectBest(commissions);
    }

    private List<String> tabListLines() {
        List<String> lines = new ArrayList<>();
        if (mc.getConnection() == null || mc.gui == null) return lines;
        List<PlayerInfo> players = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        players.sort(Comparator
                .comparing((PlayerInfo info) -> info.getTeam() == null ? "" : info.getTeam().getName())
                .thenComparing(info -> info.getProfile().name()));
        players.forEach(info -> lines.add(mc.gui.getTabList().getNameForDisplay(info).getString()));
        return lines;
    }

    private double progressOf(String line) {
        Matcher matcher = PROGRESS_PATTERN.matcher(line);
        if (!matcher.find()) return 0.0;
        try {
            return Math.clamp(Double.parseDouble(matcher.group(1)) / 100.0, 0.0, 1.0);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private Vec3 closestEmissaryPosition() {
        return EMISSARIES.stream().min(Comparator.comparingDouble(point -> point.distanceToSqr(mc.player.position()))).orElse(null);
    }

    private Player closestEmissary() {
        Vec3 position = closestEmissaryPosition();
        if (position == null) return null;
        return mc.level.players().stream()
                .filter(player -> player != mc.player && player.isAlive())
                .filter(player -> player.position().distanceToSqr(position) <= 4.0)
                .min(Comparator.comparingDouble(player -> mc.player.distanceToSqr(player)))
                .orElse(null);
    }

    private void selectHotbarSlot(SliderSetting setting) {
        mc.player.getInventory().setSelectedSlot((int) setting.getInput() - 1);
    }

    private BlockPos blockStandingOn() {
        return BlockPos.containing(mc.player.getX(), Math.ceil(mc.player.getY() - 0.25) - 1, mc.player.getZ());
    }

    private void resetMiningTarget() {
        if (miningTarget != null && miningStartSent && Utils.nullCheck()) {
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, miningTarget, BlockUtils.getDirection(miningTarget)));
        }
        miningTarget = null;
        miningHitVec = null;
        miningBlock = null;
        miningStartedAt = 0L;
        miningStartSent = false;
    }

    private void releaseKeys() {
        if (mc.options == null) return;
        IKeyMapping.get(mc.options.keyAttack).setDown(false);
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keySprint.setDown(false);
    }

    private enum State {SCANNING, PATHING, MINING, KILLING, CLAIM_INTERACT, CLAIM_GUI}

    private record MiningNode(BlockPos pos, Vec3 hit, int priority) {
    }
}
