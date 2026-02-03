package xyz.whatsyouss.frosty.modules.impl.render;

import jdk.jshell.execution.Util;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import xyz.whatsyouss.frosty.events.impl.KeyEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Input;
import xyz.whatsyouss.frosty.utility.Utils;

public class FreeLook extends Module {

    public SelectSetting mode;
    private String[] modes = new String[]{"Toggle", "Hold"};

    public static boolean freelooking;
    private int pers;
    private boolean gotPers ,setPers;

    public FreeLook() {
        super("FreeLook", category.Render, InputUtil.GLFW_KEY_LEFT_ALT);

        this.registerSetting(mode = new SelectSetting("Mode", 0, modes));
    }

    @Override
    public String getDesc() {
        return "Hold mode needs module enabled";
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mode.getValue() == 1) {
            return;
        }
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            pers = 1;
        } else if (mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK) {
            pers = 2;
        } else {
            pers = 3;
        }
        freelooking = true;
    }

    @Override
    public void onDisable() {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mode.getValue() == 1) {
            return;
        }
        freelooking = false;
        if (pers == 1) {
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mode.getValue() == 1 && this.isEnabled()) {
            if (Input.isKeyDown(this.getKeycode())) {
                if (!gotPers) {
                    if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
                        pers = 1;
                    } else if (mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK) {
                        pers = 2;
                    } else {
                        pers = 3;
                    }
                    gotPers = true;
                    setPers = false;
                }
                freelooking = true;
            } else {
                freelooking = false;
                gotPers = false;
                if (pers == 1 && !setPers) {
                    mc.options.setPerspective(Perspective.FIRST_PERSON);
                }
                setPers = true;
            }
        }
        if (freelooking && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }
}
