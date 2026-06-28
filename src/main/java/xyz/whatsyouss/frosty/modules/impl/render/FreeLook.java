package xyz.whatsyouss.frosty.modules.impl.render;

import jdk.jshell.execution.Util;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import xyz.whatsyouss.frosty.events.impl.KeyEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Input;
import xyz.whatsyouss.frosty.utility.Utils;

public class FreeLook extends Module {

    public static boolean freelooking;
    public SelectSetting mode;
    public float cameraYaw;
    public float cameraPitch;
    private String[] modes = new String[]{"Toggle", "Hold"};
    private int pers;
    private boolean gotPers, setPers;


    public FreeLook() {
        super("FreeLook", "自由视角", category.Render, InputConstants.KEY_LALT);

        this.registerSetting(mode = new SelectSetting("Mode", 0, modes));
    }

    @Override
    public String getDesc() {
        return "mode 'Hold' needs module enabled";
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck()) {
            return;
        }
        cameraYaw = mc.player.getYRot();
        cameraPitch = mc.player.getXRot();
        if (mode.getValue() == 1) {
            return;
        }
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            pers = 1;
        } else if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
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
            mc.options.setCameraType(CameraType.FIRST_PERSON);
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
                    if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                        pers = 1;
                    } else if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
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
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                }
                setPers = true;
            }
        }
        if (freelooking && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        mc.player.setXRot(Mth.clamp(mc.player.getXRot(), -90, 90));
        cameraPitch = Mth.clamp(cameraPitch, -90, 90);
    }
}
