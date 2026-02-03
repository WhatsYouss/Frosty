package xyz.whatsyouss.frosty.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
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
        super("GuiMove", category.Movement);
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

        if (setMotion && ticks < 10 && !(mc.currentScreen instanceof ClickGui)) {
            if (mode.getValue() == 2) {
                ++ticks;
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
                mc.options.sprintKey.setPressed(false);
                return;
            } else if (mode.getValue() == 1) {
                ++ticks;
                handleBlatantMode();
            }
        } else {
            reset();
        }

        mc.options.forwardKey.setPressed(Input.isKeyDown(mc.options.forwardKey.getDefaultKey().getCode()));
        mc.options.backKey.setPressed(Input.isKeyDown(mc.options.backKey.getDefaultKey().getCode()));
        mc.options.rightKey.setPressed(Input.isKeyDown(mc.options.rightKey.getDefaultKey().getCode()));
        mc.options.leftKey.setPressed(Input.isKeyDown(mc.options.leftKey.getDefaultKey().getCode()));
        mc.options.jumpKey.setPressed(Input.isKeyDown(mc.options.jumpKey.getDefaultKey().getCode()));
    }

//    @EventHandler
//    public void onMouse(MouseButtonEvent event) {
//        setMotion = true;
//        ticks = 0;
//    }

    @EventHandler
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof ClickSlotC2SPacket) {
            setMotion = true;
            ticks = 0;
        }
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (event.key == mc.options.dropKey.getDefaultKey().getCode()) {
            setMotion = true;
            ticks = 0;
            return;
        }

        for (KeyBinding hotbarKey : mc.options.hotbarKeys) {
            if (event.key == hotbarKey.getDefaultKey().getCode()) {
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
        if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) {
            return false;
        }

        if (mc.currentScreen instanceof ClickGui) {
            ClickGui clickGui = (ClickGui) mc.currentScreen;
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

    private boolean isBindDown(KeyBinding keyBinding) {
        return keyBinding.isPressed();
    }

    public boolean isHotbarKeyPressed() {
        GameOptions options = MinecraftClient.getInstance().options;
        for (KeyBinding hotbarKey : options.hotbarKeys) {
            if (hotbarKey.isPressed()) {
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