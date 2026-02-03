package xyz.whatsyouss.frosty.modules.impl.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
        super("AutoClicker", category.Combat);

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
        mc.options.attackKey.setPressed(false);
    }

    @Override
    public void onDisable() {
        if (!Utils.nullCheck()) {
            return;
        }
        mc.options.attackKey.setPressed(false);
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Press) {
            if (event.button == mc.options.attackKey.getDefaultKey().getCode()) {
                pressingLeft = true;
            } else if (event.button == mc.options.useKey.getDefaultKey().getCode()) {
                pressingRight = true;
            }
        } else if (event.action == KeyAction.Release) {
            pressingLeft = false;
            pressingRight = false;
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        if (mc.currentScreen != null) {
            return;
        }

        switch ((int) mode.getValue()) {
            case 0 -> {
                if (pressingLeft) {
                    if (breakBlocks.isToggled()) {
                        HitResult hit = mc.crosshairTarget;
                        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                            BlockHitResult blockHit = (BlockHitResult) hit;
                            BlockPos pos = blockHit.getBlockPos();
                            BlockState state = mc.world.getBlockState(pos);
                            if (isBreakable(state)) {
                                mc.options.attackKey.setPressed(true);
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
                    HitResult hit = mc.crosshairTarget;
                    if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos pos = blockHit.getBlockPos();
                        BlockState state = mc.world.getBlockState(pos);
                        if (isBreakable(state)) {
                            mc.options.attackKey.setPressed(true);
                            return;
                        }
                    }
                }
                leftClickTimer++;
                int randCps = Utils.getAPSToTicks(cps, 20);
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