package xyz.whatsyouss.frosty.modules.impl.movement;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.Utils;

public class Eagle extends Module {

    private boolean onEdge;

    public Eagle() {
        super("Eagle", category.Movement);
    }

    @Override
    public void onDisable() {
        if (!isPressingShift()) {
            mc.options.keyShift.setDown(false);
        }
        onEdge = false;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!mc.player.onGround()) {
            return;
        }
        if (BlockUtils.isEdgeOfBlock()) {
            mc.options.keyShift.setDown(true);
            if (!onEdge) {
                onEdge = true;
            }
        } else {
            if (onEdge) {
                if (!isPressingShift()) {
                    mc.options.keyShift.setDown(false);
                }
                onEdge = false;
            }
        }
    }

    private boolean isPressingShift() {
        Window windowHandle = mc.getWindow();
        return InputConstants.isKeyDown(windowHandle, mc.options.keyShift.getDefaultKey().getValue());
    }
}
