package me.charixmaz.arcaneperks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!cmd.getName().equalsIgnoreCase("ap")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Only players can use this.");
                return true;
            }
            PerkType type = aliasToPerk(cmd.getName());
            if (type == null) {
                sender.sendMessage("Unknown perk alias.");
                return true;
            }
            perkManager.toggle(type, p);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can open GUI.");
                    return true;
                }
                plugin.getPerkGui().open(p);
                return true;
            }

            case "list" -> {
                sender.sendMessage("§d§lArcanePerks §7– available perks:");
                for (PerkType type : PerkType.values()) {
                    String alias = getAlias(type);
                    String desc = plugin.getConfig().getString("descriptions." + type.getId(), "&7No description.");
                    sender.sendMessage(" §f- §b" + type.getId()
                            + " §7(§o" + type.getDisplayName() + "§7)"
                            + (alias != null ? " §8/§f" + alias : "")
                            + " §7- " + org.bukkit.ChatColor.translateAlternateColorCodes('&', desc));
                }
                return true;
            }

            case "activate" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can activate perks.");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage("§7Usage: §b/ap activate <perkId> [level]");
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    p.sendMessage("§cUnknown perk: §f" + args[1]);
                    return true;
                }

                if (args.length >= 3 &&
                        (type == PerkType.FAST_DIGGING || type == PerkType.SPEED || type == PerkType.STRENGTH)) {
                    try {
                        int level = Integer.parseInt(args[2]);
                        if (level < 1) level = 1;
                        if (level > 10) level = 10;
                        perkManager.setTempLevel(p, type, level);
                    } catch (NumberFormatException ignored) {
                        p.sendMessage("§cLevel must be a number 1–10.");
                        return true;
                    }
                }

                perkManager.activate(type, p);
                return true;
            }

            case "deactivate" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can deactivate perks.");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage("§7Usage: §b/ap deactivate <perkId|all>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    perkManager.deactivateAll(p);
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    p.sendMessage("§cUnknown perk: §f" + args[1]);
                    return true;
                }
                perkManager.deactivate(type, p);
                return true;
            }

            case "edit" -> {
                if (!sender.hasPermission("arcaneperks.command.ap")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage("§7Usage: §b/ap edit <perkId> <durationSec> <cooldownSec>");
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    sender.sendMessage("§cUnknown perk: §f" + args[1]);
                    return true;
                }
                try {
                    long duration = Long.parseLong(args[2]);
                    long cooldown = Long.parseLong(args[3]);
                    perkManager.editDefault(type, duration, cooldown);
                    sender.sendMessage("§dArcanePerks §7→ Updated §b" + type.getId() +
                            "§7: duration=" + duration + "s, cooldown=" + cooldown + "s.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cDuration and cooldown must be numbers.");
                }
                return true;
            }

            case "setlevel" -> {
                if (!sender.hasPermission("arcaneperks.command.ap")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§7Usage: §b/ap setlevel <perkId> <level>");
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    sender.sendMessage("§cUnknown perk: §f" + args[1]);
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[2]);
                    if (level < 1) level = 1;
                    if (level > 10) level = 10;
                    perkManager.setConfigLevel(type, level);
                    sender.sendMessage("§dArcanePerks §7→ Set default level of §b" +
                            type.getId() + " §7to §f" + level + "§7.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cLevel must be a number 1–10.");
                }
                return true;
            }

            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage("§7Usage: §b/ap info <perkId>");
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    sender.sendMessage("§cUnknown perk: §f" + args[1]);
                    return true;
                }
                ConfigurationSection sec = plugin.getConfig().getConfigurationSection("perks." + type.getId() + ".default");
                long dur = sec != null ? sec.getLong("duration", 30) : 30;
                long cd = sec != null ? sec.getLong("cooldown", 300) : 300;
                int lvl = sec != null ? sec.getInt("level", 1) : 1;
                sender.sendMessage("§dArcanePerks §7– info for §b" + type.getId());
                sender.sendMessage("§7Default duration: §f" + dur + "s");
                sender.sendMessage("§7Default cooldown: §f" + cd + "s");
                sender.sendMessage("§7Default level: §f" + lvl);
                sender.sendMessage("§7Base perm: §f" + type.getPermissionNode());
                sender.sendMessage("§7Level perms: §f" + type.getPermissionNode() + ".<level>");
                sender.sendMessage("§7Cooldown perms: §f" + type.getPermissionNode() + ".cooldown.<seconds>");
                return true;
            }

            case "test" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can test perks.");
                    return true;
                }
                if (!sender.hasPermission("arcaneperks.command.ap")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage("§7Usage: §b/ap test <perkId> [level]");
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    p.sendMessage("§cUnknown perk: §f" + args[1]);
                    return true;
                }
                if (args.length >= 3 &&
                        (type == PerkType.FAST_DIGGING || type == PerkType.SPEED || type == PerkType.STRENGTH)) {
                    try {
                        int level = Integer.parseInt(args[2]);
                        if (level < 1) level = 1;
                        if (level > 10) level = 10;
                        perkManager.setTempLevel(p, type, level);
                    } catch (NumberFormatException e) {
                        p.sendMessage("§cLevel must be a number 1–10.");
                        return true;
                    }
                }
                // ignore cooldown by giving admin.nocooldown
                perkManager.activate(type, p);
                return true;
            }

            case "reload" -> {
                if (!sender.hasPermission("arcaneperks.command.ap")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage("§dArcanePerks §7config reloaded.");
                return true;
            }

            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§d§lArcanePerks §7commands:");
        sender.sendMessage("§b/ap help §7- show this help");
        sender.sendMessage("§b/ap gui §7- open perks GUI");
        sender.sendMessage("§b/ap list §7- list all perks and shortcuts");
        sender.sendMessage("§b/ap activate <perkId> [level] §7- activate (optional level for haste/speed/strength)");
        sender.sendMessage("§b/ap deactivate <perkId|all> §7- stop a perk or all perks");
        sender.sendMessage("§b/ap info <perkId> §7- show default settings and perm pattern");
        sender.sendMessage("§b/ap edit <perkId> <duration> <cooldown> §7- edit defaults");
        sender.sendMessage("§b/ap setlevel <perkId> <level> §7- set default potion level");
        sender.sendMessage("§b/ap test <perkId> [level] §7- test perk (ignores cooldown for admins)");
        sender.sendMessage("§b/ap reload §7- reload config.yml");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("ap")) return null;

        if (args.length == 1) {
            List<String> subs = Arrays.asList("help", "gui", "list", "activate", "deactivate",
                    "info", "edit", "setlevel", "test", "reload");
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
            return result;
        }

        if (args.length == 2 &&
                (args[0].equalsIgnoreCase("activate")
                        || args[0].equalsIgnoreCase("deactivate")
                        || args[0].equalsIgnoreCase("edit")
                        || args[0].equalsIgnoreCase("setlevel")
                        || args[0].equalsIgnoreCase("info")
                        || args[0].equalsIgnoreCase("test"))) {

            List<String> result = new ArrayList<>();
            if (args[0].equalsIgnoreCase("deactivate")) result.add("all");
            for (PerkType type : PerkType.values()) {
                if (type.getId().startsWith(args[1].toLowerCase())) {
                    result.add(type.getId());
                }
            }
            return result;
        }

        return List.of();
    }

    private PerkType aliasToPerk(String cmdName) {
        String cmd = cmdName.toLowerCase();
        return switch (cmd) {
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

    private String getAlias(PerkType type) {
        return switch (type) {
            case FAST_DIGGING -> "fd";
            case NIGHT_VISION -> "nv";
            case STRENGTH -> "strp";
            case FLY -> "flyp";
            case KEEP_XP -> "kxp";
            case KEEP_INVENTORY -> "kinv";
            case NO_FALL_DAMAGE -> "nfall";
            case DOUBLE_EXP -> "dexp";
            case DOUBLE_MOB_DROPS -> "ddrops";
            case GOD -> "godm";
            case VANISH -> "van";
            case MOBS_IGNORE -> "mig";
            case GLOWING -> "glow";
            case TELEKINESIS -> "tele";
            case INSTANT_SMELT -> "ismelt";
            case SPEED -> "spd";
        };
    }
}
