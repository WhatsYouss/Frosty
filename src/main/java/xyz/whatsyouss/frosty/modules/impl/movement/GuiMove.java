package xyz.whatsyouss.frosty.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import xyz.whatsyouss.frosty.events.impl.KeyEvent;
import xyz.whatsyouss.frosty.events.impl.MouseButtonEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.SendPacketEvent;
import xyz.whatsyouss.frosty.gui.ClickGui;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.gui.component.impl.InputComponent;
import xyz.whatsyouss.frosty.gui.component.impl.ModuleComponent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Input;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.event.MouseEvent;
import java.util.List;

public class GuiMove extends Module {

    public boolean setMotion;
    public int ticks;
    private SelectSetting mode;
    private String[] modes = new String[] {"Vanilla", "Motion", "Legit"};

    public GuiMove() {
        super("GuiMove", "背包行走", category.Movement);
        this.registerSetting(mode = new SelectSetting("Mode", 2, modes));
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!guiCheck()) {
            reset();
            return;
        }
        if (mc.player.isSprinting() && this.mode.getValue() == 1) {
            mc.player.setSprinting(false);
        }

        if (setMotion && ticks < 10 && !(mc.gui.screen() instanceof ClickGui)) {
            if (mode.getValue() == 2) {
                ++ticks;
                mc.options.keyUp.setDown(false);
                mc.options.keyDown.setDown(false);
                mc.options.keyRight.setDown(false);
                mc.options.keyLeft.setDown(false);
                mc.options.keyJump.setDown(false);
                mc.options.keySprint.setDown(false);
                return;
            } else if (mode.getValue() == 1) {
                ++ticks;
                handleBlatantMode();
            }
        } else {
            reset();
        }

        mc.options.keyUp.setDown(Input.isKeyDown(mc.options.keyUp.getDefaultKey().getValue()));
        mc.options.keyDown.setDown(Input.isKeyDown(mc.options.keyDown.getDefaultKey().getValue()));
        mc.options.keyRight.setDown(Input.isKeyDown(mc.options.keyRight.getDefaultKey().getValue()));
        mc.options.keyLeft.setDown(Input.isKeyDown(mc.options.keyLeft.getDefaultKey().getValue()));
        mc.options.keyJump.setDown(Input.isKeyDown(mc.options.keyJump.getDefaultKey().getValue()));
    }

//    @EventHandler
//    public void onMouse(MouseButtonEvent event) {
//        setMotion = true;
//        ticks = 0;
//    }

    @EventHandler
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof ServerboundContainerClickPacket) {
            setMotion = true;
            ticks = 0;
        }
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (event.key == mc.options.keyDrop.getDefaultKey().getValue()) {
            setMotion = true;
            ticks = 0;
            return;
        }

        for (KeyMapping hotbarKey : mc.options.keyHotbarSlots) {
            if (event.key == hotbarKey.getDefaultKey().getValue()) {
                setMotion = true;
                ticks = 0;
                return;
            }
        }
    }

    private void handleBlatantMode() {
        double slowedMotion = 0.65;
        int speedAmplifier = Utils.getSpeedAmplifier();

        switch (speedAmplifier) {
            case 1:
                slowedMotion = 0.615;
                break;
            case 2:
                slowedMotion = 0.3;
                break;
        }

        Utils.setSpeed(Utils.getHorizontalSpeed() * slowedMotion);
    }


    private void reset() {
        ticks = 0;
        setMotion = false;
    }

    private boolean guiCheck() {
        if (mc.gui.screen() == null || mc.gui.screen() instanceof ChatScreen) {
            return false;
        }

        if (mc.gui.screen() instanceof ClickGui) {
            ClickGui clickGui = (ClickGui) mc.gui.screen();
            for (ModuleComponent moduleComponent : clickGui.getModuleComponents()) {
                if (moduleComponent.isExpanded()) {
                    for (Component component : moduleComponent.getSettingComponents()) {
                        if (component instanceof InputComponent && ((InputComponent) component).isFocused()) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean isBindDown(KeyMapping keyBinding) {
        return keyBinding.isDown();
    }

    public boolean isHotbarKeyPressed() {
        Options options = Minecraft.getInstance().options;
        for (KeyMapping hotbarKey : options.keyHotbarSlots) {
            if (hotbarKey.isDown()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getInfo() {
        return mode.getOption();
    }
}