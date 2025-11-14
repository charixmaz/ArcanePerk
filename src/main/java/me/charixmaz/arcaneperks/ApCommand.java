package me.charixmaz.arcaneperks;

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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // All alias commands (fd, nv, godm, ...) toggle perks directly
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

        // /ap ...
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> {
                sender.sendMessage("§d§lArcanePerks §7– available perks:");
                for (PerkType type : PerkType.values()) {
                    String alias = getAlias(type);
                    sender.sendMessage(" §f- §b" + type.getId()
                            + " §7(§o" + type.getDisplayName() + "§7)"
                            + (alias != null ? " §8/§f" + alias : ""));
                }
                return true;
            }
            case "activate" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can activate perks.");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage("§7Usage: §b/ap activate <perkId>");
                    return true;
                }
                PerkType type = PerkType.byId(args[1]);
                if (type == null) {
                    p.sendMessage("§cUnknown perk: §f" + args[1]);
                    return true;
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
        sender.sendMessage("§b/ap help §7- this help");
        sender.sendMessage("§b/ap list §7- list all perks and shortcuts");
        sender.sendMessage("§b/ap activate <perkId> §7- force-activate a perk");
        sender.sendMessage("§b/ap deactivate <perkId|all> §7- stop a perk or all perks");
        sender.sendMessage("§b/ap edit <perkId> <duration> <cooldown> §7- edit defaults");
        sender.sendMessage("§b/ap reload §7- reload config.yml");
    }

    // ----- TAB COMPLETE FOR /ap -----

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("ap")) return null;

        if (args.length == 1) {
            List<String> subs = Arrays.asList("help", "list", "activate", "deactivate", "edit", "reload");
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
            return result;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("deactivate")) {
            List<String> result = new ArrayList<>();
            result.add("all");
            for (PerkType type : PerkType.values()) {
                if (type.getId().startsWith(args[1].toLowerCase())) {
                    result.add(type.getId());
                }
            }
            return result;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("activate")
                || args[0].equalsIgnoreCase("edit"))) {

            List<String> result = new ArrayList<>();
            for (PerkType type : PerkType.values()) {
                if (type.getId().startsWith(args[1].toLowerCase())) {
                    result.add(type.getId());
                }
            }
            return result;
        }

        return List.of();
    }

    // ----- alias mapping -----

    private PerkType aliasToPerk(String cmdName) {
        String cmd = cmdName.toLowerCase();
        return switch (cmd) {
            case "fd" -> PerkType.FAST_DIGGING;
            case "nv" -> PerkType.NIGHT_VISION;
            case "wb" -> PerkType.WATER_BREATHING;
            case "strp" -> PerkType.STRENGTH;
            case "flyp" -> PerkType.FLY;
            case "noh" -> PerkType.NO_HUNGER;
            case "kxp" -> PerkType.KEEP_XP;
            case "kinv" -> PerkType.KEEP_INVENTORY;
            case "nfd" -> PerkType.NO_FIRE_DAMAGE;
            case "nfall" -> PerkType.NO_FALL_DAMAGE;
            case "dexp" -> PerkType.DOUBLE_EXP;
            case "ddrops" -> PerkType.DOUBLE_MOB_DROPS;
            case "godm" -> PerkType.GOD;
            case "van" -> PerkType.VANISH;
            case "mig" -> PerkType.MOBS_IGNORE;
            case "glow" -> PerkType.GLOWING;
            case "tele" -> PerkType.TELEKINESIS;
            case "ismelt" -> PerkType.INSTANT_SMELT;
            default -> null;
        };
    }

    private String getAlias(PerkType type) {
        return switch (type) {
            case FAST_DIGGING -> "fd";
            case NIGHT_VISION -> "nv";
            case WATER_BREATHING -> "wb";
            case STRENGTH -> "strp";
            case FLY -> "flyp";
            case NO_HUNGER -> "noh";
            case KEEP_XP -> "kxp";
            case KEEP_INVENTORY -> "kinv";
            case NO_FIRE_DAMAGE -> "nfd";
            case NO_FALL_DAMAGE -> "nfall";
            case DOUBLE_EXP -> "dexp";
            case DOUBLE_MOB_DROPS -> "ddrops";
            case GOD -> "godm";
            case VANISH -> "van";
            case MOBS_IGNORE -> "mig";
            case GLOWING -> "glow";
            case TELEKINESIS -> "tele";
            case INSTANT_SMELT -> "ismelt";
        };
    }
}
