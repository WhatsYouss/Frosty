package xyz.whatsyouss.frosty.modules.impl.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import xyz.whatsyouss.frosty.events.impl.MouseButtonEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.KeyAction;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.Random;

public class AutoClicker extends Module {

    private SelectSetting mode;
    private SliderSetting cps;
    private ButtonSetting breakBlocks, inventoryFill;

    private String[] modes = new String[]{"Hold", "Toggle"};

    private int leftClickTimer;

    private Random rand = new Random();
    private boolean pressingLeft, pressingRight;

    public AutoClicker() {
        super("AutoClicker", "自动点击", category.Combat);

        this.registerSetting(mode = new SelectSetting("Mode", 0, modes));
        this.registerSetting(cps = new SliderSetting("CPS", 12, 15, 1, 20, 1));
        this.registerSetting(breakBlocks = new ButtonSetting("Break blocks", false));
//        this.registerSetting(inventoryFill = new ButtonSetting("Inventory fill", true));
    }

    @Override
    public String getInfo() {
        return (int) cps.getInputMin() + "-" + (int) cps.getInputMax();
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck()) {
            return;
        }
        leftClickTimer = 0;
        mc.options.keyAttack.setDown(false);
    }

    @Override
    public void onDisable() {
        if (!Utils.nullCheck()) {
            return;
        }
        mc.options.keyAttack.setDown(false);
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Press) {
            if (event.button == mc.options.keyAttack.getDefaultKey().getValue()) {
                pressingLeft = true;
            } else if (event.button == mc.options.keyUse.getDefaultKey().getValue()) {
                pressingRight = true;
            }
        } else if (event.action == KeyAction.Release) {
            if (event.button == mc.options.keyAttack.getDefaultKey().getValue()) {
                pressingLeft = false;
            } else if (event.button == mc.options.keyUse.getDefaultKey().getValue()) {
                pressingRight = false;
            }
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        if (mc.gui.screen() != null) {
            return;
        }

        switch ((int) mode.getValue()) {
            case 0 -> {
                if (pressingLeft) {
                    if (breakBlocks.isToggled()) {
                        var hit = mc.hitResult;
                        if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                            BlockHitResult blockHit = (BlockHitResult) hit;
                            BlockPos pos = blockHit.getBlockPos();
                            BlockState state = mc.level.getBlockState(pos);
                            if (isBreakable(state)) {
                                mc.options.keyAttack.setDown(true);
                                return;
                            }
                        }
                    }

                    leftClickTimer++;
                    int randCps = (int) (20 / (rand.nextInt((int) (cps.getInputMax() - cps.getInputMin() + 1)) + cps.getInputMin()));
                    if (leftClickTimer > randCps) {
                        Utils.leftClick();
                        leftClickTimer = 0;
                    }
                }
            }
            case 1 -> {
                if (breakBlocks.isToggled() && !mc.player.isCreative()) {
                    var hit = mc.hitResult;
                    if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos pos = blockHit.getBlockPos();
                        BlockState state = mc.level.getBlockState(pos);
                        if (isBreakable(state)) {
                            mc.options.keyAttack.setDown(true);
                            return;
                        }
                    }
                }
                leftClickTimer++;
                int randCps = (int) (20 / (rand.nextInt((int) (cps.getInputMax() - cps.getInputMin() + 1)) + cps.getInputMin()));
                if (leftClickTimer > randCps) {
                    Utils.leftClick();
                    leftClickTimer = 0;
                }
            }
        }
    }

    public static boolean isBreakable(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }
}