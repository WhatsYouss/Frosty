package xyz.whatsyouss.frosty.commands.impl;

import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.commands.Command;
import xyz.whatsyouss.frosty.config.ConfigManager;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.farming.FarmingMacro;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.List;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class FarmingCommand extends Command {

    public FarmingCommand() {
        super("fm", "Manage farming macro waypoints", "farmingmacro");
    }

    private FarmingMacro macro() {
        return ModuleManager.farmingMacro;
    }

    private boolean requireEnabled() {
        if (!macro().isEnabled()) {
            sendError("FarmingMacro must be enabled before using .fm commands");
            return false;
        }
        return true;
    }

    private boolean requireGarden() {
        String sidebar = Utils.getScoreboardSidebar().toString().toLowerCase();
        if (!sidebar.contains("the garde") && !sidebar.contains("plot")) {
            sendError("You must be in the Garden to use this command");
            return false;
        }
        return true;
    }


    private boolean axisAligned(double[] a, double[] b) {
        boolean sameX = Math.abs(a[0] - b[0]) < 0.5;
        boolean sameZ = Math.abs(a[2] - b[2]) < 0.5;
        return sameX || sameZ;
    }


    @Override
    public void execute(String[] args) {
        if (!Utils.nullCheck()) return;

        if (args.length < 1) {
            printUsage();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> cmdAdd();
            case "remove" -> cmdRemove();
            case "insert" -> cmdInsert(args);
            case "delete" -> cmdDelete(args);
            case "save" -> cmdSave(args);
            case "load" -> cmdLoad(args);
            case "start" -> cmdStart(args);
            case "stop" -> cmdStop();
            default -> printUsage();
        }
    }


    private void cmdAdd() {
        if (!requireEnabled() || !requireGarden()) return;

        List<double[]> wps = macro().getWaypoints();
        double[] pos = playerPos();

        if (!wps.isEmpty()) {
            double[] last = wps.get(wps.size() - 1);
            if (!axisAligned(last, pos)) {
                sendError("New waypoint must share the X or Z axis with the previous waypoint (#" + wps.size() + ")");
                return;
            }
        }

        wps.add(pos);
        Utils.addChatMessage("§aWaypoint §f#" + wps.size() + " §aadded at §f" + fmtPos(pos));
    }


    private void cmdRemove() {
        if (!requireEnabled()) return;

        List<double[]> wps = macro().getWaypoints();
        if (wps.isEmpty()) {
            sendError("No waypoints to remove");
            return;
        }

        double[] removed = wps.remove(wps.size() - 1);
        Utils.addChatMessage("§eRemoved last waypoint (was §f" + fmtPos(removed) + "§e). Total: §f" + wps.size());
    }


    private void cmdInsert(String[] args) {
        if (!requireEnabled() || !requireGarden()) return;
        if (args.length < 2) {
            sendError("Usage: .fm insert <index>");
            return;
        }

        List<double[]> wps = macro().getWaypoints();
        int idx;
        try {
            idx = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendError("Invalid index: " + args[1]);
            return;
        }


        if (idx < 1 || idx > wps.size()) {
            sendError("Index out of range. Valid range: 1 – " + wps.size());
            return;
        }

        double[] pos = playerPos();
        double[] prev = wps.get(idx - 1);
        if (!axisAligned(prev, pos)) {
            sendError("New waypoint must share the X or Z axis with waypoint #" + idx + ".");
            return;
        }


        if (idx < wps.size()) {
            double[] next = wps.get(idx);
            if (!axisAligned(pos, next)) {
                sendError("New waypoint must also share the X or Z axis with waypoint #" + (idx + 1) + ".");
                return;
            }
        }

        wps.add(idx, pos);
        Utils.addChatMessage("§aInserted waypoint §f#" + (idx + 1) + " §aat §f" + fmtPos(pos));
    }


    private void cmdDelete(String[] args) {
        if (!requireEnabled()) return;
        if (args.length < 2) {
            sendError("Usage: .fm delete <index>");
            return;
        }

        List<double[]> wps = macro().getWaypoints();
        int idx;
        try {
            idx = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendError("Invalid index: " + args[1]);
            return;
        }

        if (idx < 1 || idx > wps.size()) {
            sendError("Index out of range. Valid range: 1 – " + wps.size());
            return;
        }


        if (idx > 1 && idx < wps.size()) {
            double[] prev = wps.get(idx - 2);
            double[] next = wps.get(idx);
            if (!axisAligned(prev, next)) {
                sendError("Cannot delete waypoint #" + idx + ": it would leave #" + (idx - 1)
                        + " and #" + (idx + 1) + " misaligned");
                return;
            }
        }

        double[] removed = wps.remove(idx - 1);
        Utils.addChatMessage("§eDeleted waypoint §f#" + idx + " §e(was §f" + fmtPos(removed)
                + "§e). Total: §f" + wps.size());
    }


    private void cmdSave(String[] args) {
        if (!requireEnabled()) return;
        if (args.length < 2) {
            sendError("Usage: .fm save <name>");
            return;
        }

        List<double[]> wps = macro().getWaypoints();
        if (wps.isEmpty()) {
            sendError("No waypoints to save");
            return;
        }

        String name = args[1];
        if (ConfigManager.saveWaypoints(name, wps)) {
            Utils.addChatMessage("§aSaved §f" + wps.size() + " §awaypoints as §f\"" + name + "\"");
        } else {
            sendError("Failed to save waypoints. Check logs for details.");
        }
    }


    private void cmdLoad(String[] args) {
        if (!requireEnabled()) return;
        if (args.length < 2) {
            sendError("Usage: .fm load <name>");
            return;
        }

        String name = args[1];
        List<double[]> loaded = ConfigManager.loadWaypoints(name);
        if (loaded == null) {
            sendError("Could not load waypoint file \"" + name + "\". File may not exist");
            return;
        }
        if (loaded.isEmpty()) {
            sendError("Waypoint file \"" + name + "\" is empty");
            return;
        }


        int bad = ConfigManager.validateWaypoints(loaded);
        if (bad != -1) {
            sendError("Waypoint file \"" + name + "\" has an alignment issue at waypoint #" + bad
                    + " (it does not share X or Z with the previous waypoint). Load aborted");
            return;
        }

        macro().setWaypoints(loaded);
        Utils.addChatMessage("§aLoaded §f" + loaded.size() + " §awaypoints from §f\"" + name + "\"");
    }


    private void cmdStart(String[] args) {
        if (!requireGarden()) return;

        List<double[]> wps = macro().getWaypoints();
        if (wps.size() < 2) {
            sendError("At least 2 waypoints are required to start");
            return;
        }

        int startIdx = 0;
        if (args.length >= 2) {
            try {
                int parsed = Integer.parseInt(args[1]);
                if (parsed < 1 || parsed > wps.size()) {
                    sendError("Start index out of range. Valid range: 1 – " + wps.size());
                    return;
                }
                startIdx = parsed - 1;
            } catch (NumberFormatException e) {
                sendError("Invalid index: " + args[1]);
                return;
            }
        }

        macro().startMacro(startIdx);
        Utils.addChatMessage("§aFarmingMacro started from waypoint §f#" + (startIdx + 1));
    }


    private void cmdStop() {
        macro().stopMacro();
        Utils.addChatMessage("§eFarmingMacro stopped");
    }


    private double[] playerPos() {
        Vec3d p = mc.player.getEntityPos();
        return new double[]{p.x, p.y, p.z};
    }

    private String fmtPos(double[] pos) {
        return String.format("%.1f, %.1f, %.1f", pos[0], pos[1], pos[2]);
    }

    private void printUsage() {
        Utils.addChatMessage("§eUsage: .fm <add|remove|insert <index>|delete <index>|save <name>|load <name>|start <index>|stop>");
    }
}