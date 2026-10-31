package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.Locale;

public class FarmingProtector extends Module {

    private static final float ROTATION_THRESHOLD_DEGREES = 10.0f;
    private static final long ROTATION_ALERT_COOLDOWN_MS = 5_000L;

    private String[] sensitivity = new String[]{"High", "Medium", "Low"};
    private ButtonSetting movement, bps, block, rotation, item, restart;
    private SelectSetting sensitive;

    private long rsTime = 0;
    private LocalPlayer expectedRotationPlayer;
    private float expectedYaw;
    private float expectedPitch;
    private long lastRotationAlertMs;
    public boolean rejoined;
    public static boolean stopped;
    public boolean canRestart;

    public FarmingProtector() {
        super("FarmingProtector", "农业保护", category.Farming);

        this.registerSetting(restart = new ButtonSetting("Restart", "自动重启", true));

        // this.registerSetting(movement = new ButtonSetting("Movement", true));
//         this.registerSetting(bps = new ButtonSetting("BPS", true));
        // this.registerSetting(sensitive = new SelectSetting("Sensitive", 1, sensitivity));
        // this.registerSetting(block = new ButtonSetting("Spawned block", true));
        this.registerSetting(rotation = new ButtonSetting("Rotation", "视角异常", true));
        // this.registerSetting(item = new ButtonSetting("Item change", true));
    }

    @Override
    public void onEnable() {
        resetRotationMonitor();
        canRestart = false;
        stopped = false;
        rejoined = false;
        rsTime = 0;
    }

    @Override
    public void onDisable() {
        resetRotationMonitor();
    }

    public void beforeMacroRotation(FarmingMacro macro) {
        if (!isEnabled() || !rotation.isToggled() || !macro.isFarmingState()) {
            resetRotationMonitor();
            return;
        }
        if (expectedRotationPlayer != mc.player) return;

        float yawDifference = Math.abs(Mth.wrapDegrees(mc.player.getYRot() - expectedYaw));
        float pitchDifference = Math.abs(mc.player.getXRot() - expectedPitch);
        if (yawDifference < ROTATION_THRESHOLD_DEGREES
                && pitchDifference < ROTATION_THRESHOLD_DEGREES) return;

        long now = System.currentTimeMillis();
        if (now - lastRotationAlertMs < ROTATION_ALERT_COOLDOWN_MS) return;
        lastRotationAlertMs = now;
        Utils.addModuleMessage(getName(), String.format(Locale.ROOT,
                "§cUnexpected view rotation! §fYaw +%.1f°, Pitch +%.1f°", yawDifference, pitchDifference));
        mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 3.0f, 0.65f);
    }

    public void afterMacroRotation(FarmingMacro macro) {
        if (!isEnabled() || !rotation.isToggled() || !macro.isFarmingState()) {
            resetRotationMonitor();
            return;
        }
        expectedRotationPlayer = mc.player;
        expectedYaw = mc.player.getYRot();
        expectedPitch = mc.player.getXRot();
    }

    public void resetRotationMonitor() {
        expectedRotationPlayer = null;
        lastRotationAlertMs = 0L;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        FarmingMacro fm = ModuleManager.farmingMacro;

        String sidebar = Utils.stripColor(Utils.getScoreboardSidebarLines().toString().toLowerCase());
        boolean inGarden = sidebar.contains("the garden") || sidebar.contains("plot");

        if (!inGarden) {
            if (fm.running && !stopped) {
                stopMacro("§eWorld changed / Not in garden");
                stopped = true;
                rsTime = System.currentTimeMillis();
                return;
            }

            if (stopped && restart.isToggled()) {
                long now = System.currentTimeMillis();
                if (now - rsTime >= 3000) {
                    if (sidebar.contains("skyblock")) {
                        mc.getConnection().sendCommand("warp garden");
                        rsTime = now;
                    } else if (sidebar.contains("hypixel")) {
                        mc.getConnection().sendCommand("play sb");
                        rsTime = now;
                    }
                }
            }
        } else {
            if (stopped) {
                Utils.addModuleMessage(this.getName(), "§aRestarting FarmingMacro...");

                fm.startMacro(0);

                stopped = false;
                rejoined = false;
                canRestart = false;
                rsTime = 0;
            }
        }
    }

    private void stopMacro(String reason) {
        Utils.addModuleMessage(this.getName(), reason);

        FarmingMacro fm = ModuleManager.farmingMacro;
        if (fm != null && fm.isEnabled()) {
            fm.stopMacro();
            Utils.addModuleMessage(this.getName(), "§cFarmingMacro stopped");
        }

        PestCleaner pc = ModuleManager.pestCleaner;
        if (pc != null && pc.isEnabled()) {
            pc.disable();
        }
    }
}
