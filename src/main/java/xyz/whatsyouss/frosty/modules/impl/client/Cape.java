package xyz.whatsyouss.frosty.modules.impl.client;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.InputSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Utils;

import net.minecraft.util.Identifier;

import java.io.File;

public class Cape extends Module {
    public SelectSetting cape;
    public InputSetting customCape;

    private String[] capes = new String[]{"Frosty 1", "Frosty 2"};
    private Identifier currentCape = null;

    public Cape() {
        super("Cape", category.Client);

        this.registerSetting(cape = new SelectSetting("Cape", 0, capes));
        this.registerSetting(customCape = new InputSetting("File name", 16, ""));
    }

    @Override
    public String getDesc() {
        return "● File name: if failed on load, try lowercase\n\nMight needs re-toggle to apply";
    }

    @Override
    public void onEnable() {
        loadCapes();
        applyCape();
    }

    @Override
    public void onDisable() {
        if (!Utils.nullCheck()) {
            return;
        }

        if (currentCape != null) {
            mc.getTextureManager().destroyTexture(currentCape);
        }

        currentCape = null;
    }

    @EventHandler
    public void onSettingUpdate(SettingUpdateEvent event) {
        applyCape();
    }

    private void loadCapes() {
        File capeFolder = new File(mc.runDirectory, "config/Frosty/cape");
    }

    public void applyCape() {
        if (!Utils.nullCheck()) {
            return;
        }

        if (customCape.getValue() == null || customCape.getValue().isEmpty() || customCape.getValue() == "") {
            if (cape.getValue() == 0) {
                currentCape = Identifier.of("frosty", "capes/frosty_1.png");
            } else if (cape.getValue() == 1) {
                currentCape = Identifier.of("frosty", "capes/frosty_2.png");
            }
        } else {
            try {
                File capeFile = new File(mc.runDirectory, "config/Frosty/cape/" + customCape.getValue());
                if (capeFile.exists() && capeFile.isFile()) {
                    currentCape = Identifier.of("frosty", "capes/" + customCape.getValue());
                    Frosty.registerCapeTexture(currentCape, capeFile);
                } else {
                    try {
                        File capeFile2 = new File(mc.runDirectory, "config/Frosty/cape/" + customCape.getValue() + ".png");
                        if (capeFile2.exists() && capeFile2.isFile()) {
                            currentCape = Identifier.of("frosty", "capes/" + customCape.getValue());
                            Frosty.registerCapeTexture(currentCape, capeFile2);
                        }
                    } catch (Exception e) {
                        Utils.addChatMessage("§cCannot find cape file: " + customCape.getValue() + ".png");
                        loadCapes();
                        currentCape = Identifier.of("frosty", "capes/frosty_1.png");
                    }
                }
            } catch (Exception e) {
                Utils.addChatMessage("§cAn error on apply cape: " + e.getMessage());
                currentCape = Identifier.of("frosty", "capes/frosty_1.png");
            }
        }
    }


    public void onCapeSelectionChanged() {
        applyCape();
    }

    public Identifier getCurrentCape() {
        return currentCape;
    }
}