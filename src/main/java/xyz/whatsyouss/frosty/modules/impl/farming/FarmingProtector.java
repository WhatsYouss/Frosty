package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class FarmingProtector extends Module {

    private String[] sensitivity = new String[]{"High", "Medium", "Low"};
    private ButtonSetting movement, bps, block, rotation, item;
    private SelectSetting sensitive;

    public FarmingProtector() {
        super("FarmingProtector", "农业保护", category.Farming);

//        this.registerSetting(movement = new ButtonSetting("Movement", true));
//        this.registerSetting(bps = new ButtonSetting("BPS", true));
//        this.registerSetting(sensitive = new SelectSetting("Sensitive", 1, sensitivity));
//        this.registerSetting(block = new ButtonSetting("Spawned block", true));
//        this.registerSetting(rotation = new ButtonSetting("Rotation", true));
//        this.registerSetting(item = new ButtonSetting("Item change", true));
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck() || !ModuleManager.farmingMacro.isEnabled() || !ModuleManager.farmingMacro.running) {
            return;
        }
        String sidebar = Utils.stripColor(Utils.getScoreboardSidebarLines().toString().toLowerCase());
        if (!sidebar.contains("the garden") && !sidebar.contains("plot")) {
            stopMacro("§eWorld changed / Not in garden");
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
