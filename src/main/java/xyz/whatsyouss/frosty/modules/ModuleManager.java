package xyz.whatsyouss.frosty.modules;

import xyz.whatsyouss.frosty.modules.impl.client.*;
import xyz.whatsyouss.frosty.modules.impl.combat.AutoClicker;
import xyz.whatsyouss.frosty.modules.impl.combat.KillAura;
import xyz.whatsyouss.frosty.modules.impl.combat.Velocity;
import xyz.whatsyouss.frosty.modules.impl.fishing.AutoFish;
import xyz.whatsyouss.frosty.modules.impl.foraging.LushlilacNuker;
import xyz.whatsyouss.frosty.modules.impl.foraging.SeaLumiesNuker;
import xyz.whatsyouss.frosty.modules.impl.foraging.WoodNuker;
import xyz.whatsyouss.frosty.modules.impl.hunting.AutoReel;
import xyz.whatsyouss.frosty.modules.impl.hunting.Hideonleaf;
import xyz.whatsyouss.frosty.modules.impl.mining.FrozenTreasure;
import xyz.whatsyouss.frosty.modules.impl.mining.NoBreakReset;
import xyz.whatsyouss.frosty.modules.impl.mining.SandNuker;
import xyz.whatsyouss.frosty.modules.impl.movement.GuiMove;
import xyz.whatsyouss.frosty.modules.impl.movement.Sprint;
import xyz.whatsyouss.frosty.modules.impl.other.*;
import xyz.whatsyouss.frosty.modules.impl.render.*;
import xyz.whatsyouss.frosty.settings.Setting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleManager {
    public static List<Module> modules = new ArrayList<>();
    public static List<Module> organizedModules = new ArrayList<>();

    public static TPS tps;
    public static GuiMove guiMove;
    public static UI ui;
    public static HUD hud;
    public static Velocity velocity;
//    public static BlockOverlay blockOverlay;
    public static AutoReconnect autoReconnect;
//    public static Scheduler scheduler;
//    public static CHLobbySwitcher chLobbySwitcher;
    public static Sprint sprint;
    public static AntiDebuff antiDebuff;
    public static Fullbright fullbright;
    public static NoHurtCam noHurtCam;
    public static AutoFish autoFish;
    public static LushlilacNuker lushlilacNuker;
    public static AutoReel autoReel;
    public static SeaLumiesNuker seaLumiesNuker;
    public static FrozenTreasure frozenTreasure;
    public static AxolotlESP axolotlESP;
    public static AntiBot antiBot;
    public static PlayerESP playerESP;
//    public static Nametags nametags;
    public static SandNuker sandNuker;
    public static NoBreakReset noBreakReset;
    public static NoPlaceInteract stopPlacement;
    public static NoHudElement noHudElement;
    public static ArmorHider armorHider;
    public static NickHider nickHider;
    public static Hideonleaf hideonleaf;
    public static FreeLook freeLook;
    public static NoOverlay noOverlay;
    public static NoBlur noBlur;
    public static Title title;
    public static Commands commands;
    public static WoodNuker woodNuker;
    public static MoveFix moveFix;
    public static KillAura killAura;
    public static GhostBlock ghostBlock;
    public static ChestESP chestESP;
    public static AutoExperiment autoExperiment;
    public static AutoClicker autoClicker;
//    public static MangroveMacro mangroveMacro;
    public static FastPlace fastPlace;
//    public static Freecam freecam;
    public static CarnivalHelper carnivalHelper;

    public void register() {
        this.addModule(tps = new TPS());
        this.addModule(guiMove = new GuiMove());
        this.addModule(ui = new UI());
        this.addModule(autoReconnect = new AutoReconnect());
//        this.addModule(scheduler = new Scheduler());
//        this.addModule(chLobbySwitcher = new CHLobbySwitcher());
        this.addModule(hud = new HUD());
        this.addModule(sprint = new Sprint());
        this.addModule(velocity = new Velocity());
        this.addModule(antiDebuff = new AntiDebuff());
        this.addModule(fullbright = new Fullbright());
        this.addModule(noHurtCam = new NoHurtCam());
        this.addModule(autoFish = new AutoFish());
        this.addModule(lushlilacNuker = new LushlilacNuker());
        this.addModule(autoReel = new AutoReel());
        this.addModule(seaLumiesNuker = new SeaLumiesNuker());
//        this.addModule(blockOverlay = new BlockOverlay());
        this.addModule(axolotlESP = new AxolotlESP());
        this.addModule(antiBot = new AntiBot());
        this.addModule(playerESP = new PlayerESP());
//        this.addModule(nametags = new Nametags());
        this.addModule(frozenTreasure = new FrozenTreasure());
        this.addModule(sandNuker = new SandNuker());
        this.addModule(noBreakReset = new NoBreakReset());
        this.addModule(stopPlacement = new NoPlaceInteract());
        this.addModule(armorHider = new ArmorHider());
        this.addModule(nickHider = new NickHider());
        this.addModule(hideonleaf = new Hideonleaf());
        this.addModule(freeLook = new FreeLook());
        this.addModule(noHudElement = new NoHudElement());
        this.addModule(noOverlay = new NoOverlay());
        this.addModule(noBlur = new NoBlur());
        this.addModule(title = new Title());
        this.addModule(commands = new Commands());
        this.addModule(woodNuker = new WoodNuker());
        this.addModule(moveFix = new MoveFix());
        this.addModule(killAura = new KillAura());
        this.addModule(ghostBlock = new GhostBlock());
        this.addModule(chestESP = new ChestESP());
        this.addModule(autoExperiment = new AutoExperiment());
        this.addModule(autoClicker = new AutoClicker());
//        this.addModule(mangroveMacro = new MangroveMacro());
        this.addModule(fastPlace = new FastPlace());
//        this.addModule(freecam = new Freecam());
        this.addModule(carnivalHelper = new CarnivalHelper());
        modules.sort(Comparator.comparing(Module::getName));
    }

    public void addModule(Module m) {
        modules.add(m);
    }

    public static List<Module> getModules() {
        return modules;
    }

    public List<Module> inCategory(Module.category categ) {
        ArrayList<Module> categML = new ArrayList<>();

        for (Module mod : getModules()) {
            if (mod.moduleCategory().equals(categ)) {
                categML.add(mod);
            }
        }

        return categML;
    }

    public static Module getModule(String moduleName) {
        for (Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    public Module getModule(Class clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz)) {
                return module;
            }
        }
        return null;
    }

//    public static void sort() {
//        if (HUD.alphabeticalSort.isToggled()) {
//            Collections.sort(organizedModules, Comparator.comparing(Module::getNameInHud));
//        }
//        else {
//            organizedModules.sort((o1, o2) -> mc.fontRendererObj.getStringWidth(o2.getNameInHud() + ((HUD.showInfo.isToggled() && !o2.getInfo().isEmpty()) ? " " + o2.getInfo() : "")) - mc.fontRendererObj.getStringWidth(o1.getNameInHud() + (HUD.showInfo.isToggled() && !o1.getInfo().isEmpty() ? " " + o1.getInfo() : "")));
//        }
//    }

//    public static boolean canExecuteChatCommand() {
//        return ModuleManager.chatCommands != null && ModuleManager.chatCommands.isEnabled();
//    }

    public static List<Module> getModulesByName(String name) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getName().toLowerCase().contains(name.toLowerCase())) {
                result.add(module);
            }
        }
        return result;
    }

    public static List<Module> getModulesByCategory(Module.category selectedCategory) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.moduleCategory() == selectedCategory) {
                result.add(module);
            }
        }
        return result;
    }

    public static Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public static Module getModuleBySetting(Setting setting) {
        for (Module module : getModules()) {
            if (module.getSettings().contains(setting)) {
                return module;
            }
        }
        return null;
    }
}
