package me.charixmaz.arcaneperks;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApCommand implements CommandExecutor, TabCompleter {

    private final ArcanePerks plugin;
    private final PerkManager perkManager;

    public ApCommand(ArcanePerks plugin) {
        this.plugin = plugin;
        this.perkManager = plugin.getPerkManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // aliases like /fd, /nv, /spd...
        if (!command.getName().equalsIgnoreCase("ap")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(ChatColor.RED + "Only players can use perk commands.");
                return true;
            }

            PerkType type = aliasToPerk(command.getName());
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Unknown perk alias.");
                return true;
            }

            if (!p.hasPermission(type.getPermissionNode())) {
                p.sendMessage(ChatColor.RED + "You do not have permission for this perk.");
                return true;
            }

            perkManager.toggle(type, p);
            return true;
        }

        // /ap commands
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }



        String sub = args[0].toLowerCase();

        switch (sub) {
            case "gui" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.RED + "Only players can open the GUI.");
                    return true;
                }
                plugin.getPerkGui().open(p);
                return true;
            }

            case "activate" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this.");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /ap activate <perkId>");
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    p.sendMessage(ChatColor.RED + "Unknown perk: " + args[1]);
                    return true;
                }
                if (!p.hasPermission(type.getPermissionNode())) {
                    p.sendMessage(ChatColor.RED + "You do not have permission for this perk.");
                    return true;
                }
                perkManager.activate(type, p, true);
                return true;
            }

            case "deactivate" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this.");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /ap deactivate <perkId|all>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    perkManager.deactivateAll(p);
                    p.sendMessage(ChatColor.GRAY + "All Arcane perks deactivated.");
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    p.sendMessage(ChatColor.RED + "Unknown perk: " + args[1]);
                    return true;
                }
                perkManager.deactivate(type, p);
                return true;
            }

            case "reload" -> {
                if (!sender.hasPermission("arcaneperks.command.reload")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                plugin.reloadConfig();
                perkManager.reload();
                sender.sendMessage(ChatColor.GREEN + "ArcanePerks reloaded.");
                return true;
            }

            case "test" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this.");
                    return true;
                }
                if (!p.hasPermission("arcaneperks.command.test")) {
                    p.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (args.length < 3) {
                    p.sendMessage(ChatColor.RED + "Usage: /ap test <perkId> <level>");
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    p.sendMessage(ChatColor.RED + "Unknown perk: " + args[1]);
                    return true;
                }
                int level;
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.RED + "Level must be a number.");
                    return true;
                }
                perkManager.setTempLevel(p, type, level);
                p.sendMessage(ChatColor.GRAY + "Temporary level for " + type.getId()
                        + " set to " + level + ".");
                return true;
            }
            case "passive" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.RED + "Only players can open passive GUI.");
                    return true;
                }
                plugin.getPassiveGui().openRoot(p);
                return true;
            }


            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "ArcanePerks commands:");
        sender.sendMessage(ChatColor.GRAY + "/ap gui" + ChatColor.DARK_GRAY + " - " +
                ChatColor.WHITE + "Open perks GUI");
        sender.sendMessage(ChatColor.GRAY + "/ap activate <perkId>");
        sender.sendMessage(ChatColor.GRAY + "/ap deactivate <perkId|all>");
        sender.sendMessage(ChatColor.GRAY + "/ap reload");
        sender.sendMessage(ChatColor.GRAY + "/ap test <perkId> <level>");
        sender.sendMessage(ChatColor.GRAY + "/ap passive" + ChatColor.DARK_GRAY + " - " +
                           ChatColor.WHITE + "Open passive perks (anatomy) GUI");

    }

    private PerkType aliasToPerk(String cmdName) {
        String n = cmdName.toLowerCase();
        return switch (n) {
            case "fd" -> PerkType.FAST_DIGGING;
            case "nv" -> PerkType.NIGHT_VISION;
            case "strp" -> PerkType.STRENGTH;
            case "flyp" -> PerkType.FLY;
            case "kxp" -> PerkType.KEEP_XP;
            case "kinv" -> PerkType.KEEP_INVENTORY;
            case "nfall" -> PerkType.NO_FALL_DAMAGE;
            case "dexp" -> PerkType.DOUBLE_EXP;
            case "ddrops" -> PerkType.DOUBLE_MOB_DROPS;
            case "godm" -> PerkType.GOD;
            case "van" -> PerkType.VANISH;
            case "mig" -> PerkType.MOBS_IGNORE;
            case "glow" -> PerkType.GLOWING;
            case "tele" -> PerkType.TELEKINESIS;
            case "ismelt" -> PerkType.INSTANT_SMELT;
            case "spd" -> PerkType.SPEED;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("ap")) {
            return List.of();
        }


        if (args.length == 1) {
            List<String> base = Arrays.asList("gui", "activate", "deactivate", "reload", "test");
            List<String> result = new ArrayList<>();
            String start = args[0].toLowerCase();
            for (String s : base) {
                if (s.startsWith(start)) result.add(s);
            }
            return result;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("activate")
                || args[0].equalsIgnoreCase("deactivate")
                || args[0].equalsIgnoreCase("test"))) {
            String start = args[1].toLowerCase();
            List<String> result = new ArrayList<>();
            for (PerkType type : PerkType.values()) {
                if (type.getId().startsWith(start)) {
                    result.add(type.getId());
                }
            }
            if (args[0].equalsIgnoreCase("deactivate")) {
                if ("all".startsWith(start)) result.add("all");
            }
            return result;
        }

        return List.of();
    }
    List<String> base = Arrays.asList("gui", "passive", "activate", "deactivate", "reload", "test");

}
