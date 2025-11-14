package me.charixmaz.arcaneperks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;

public class PerkManager {

    private enum ActivationMode {
        ALWAYS, TIMED
    }

    private final ArcanePerks plugin;

    // perk -> (player -> activeUntilMillis)
    private final Map<PerkType, Map<UUID, Long>> activeUntil = new EnumMap<>(PerkType.class);
    // perk -> (player -> cooldownUntilMillis)
    private final Map<PerkType, Map<UUID, Long>> cooldownUntil = new EnumMap<>(PerkType.class);
    // perk -> (player -> temporary level override)
    private final Map<PerkType, Map<UUID, Integer>> tempLevels = new EnumMap<>(PerkType.class);

    public PerkManager(ArcanePerks plugin) {
        this.plugin = plugin;
        for (PerkType type : PerkType.values()) {
            activeUntil.put(type, new HashMap<>());
            cooldownUntil.put(type, new HashMap<>());
            tempLevels.put(type, new HashMap<>());
        }
    }

    // -------- settings from config --------

    private ConfigurationSection getPerkSection(PerkType type) {
        ConfigurationSection perksSec = plugin.getConfig().getConfigurationSection("perks");
        if (perksSec == null) return null;
        return perksSec.getConfigurationSection(type.getId());
    }

    private ActivationMode getMode(PerkType type, Player player) {
        ConfigurationSection perkSec = getPerkSection(type);
        if (perkSec == null) return ActivationMode.TIMED;
        String mode = perkSec.getString("activation", "TIMED");
        return "ALWAYS".equalsIgnoreCase(mode) ? ActivationMode.ALWAYS : ActivationMode.TIMED;
    }

    private long getDuration(PerkType type, Player player) {
        ConfigurationSection perkSec = getPerkSection(type);
        if (perkSec == null) return 30;

        ConfigurationSection defSec = perkSec.getConfigurationSection("default");
        long duration = defSec != null ? defSec.getLong("duration", 30) : 30;

        ConfigurationSection groupsSec = perkSec.getConfigurationSection("groups");
        if (groupsSec != null) {
            for (String group : groupsSec.getKeys(false)) {
                if (player.hasPermission("group." + group)) {
                    ConfigurationSection g = groupsSec.getConfigurationSection(group);
                    if (g != null) {
                        duration = g.getLong("duration", duration);
                    }
                }
            }
        }
        return duration;
    }

    private long getCooldown(PerkType type, Player player) {
        ConfigurationSection perkSec = getPerkSection(type);
        if (perkSec == null) return 300;

        ConfigurationSection defSec = perkSec.getConfigurationSection("default");
        long cooldown = defSec != null ? defSec.getLong("cooldown", 300) : 300;

        ConfigurationSection groupsSec = perkSec.getConfigurationSection("groups");
        if (groupsSec != null) {
            for (String group : groupsSec.getKeys(false)) {
                if (player.hasPermission("group." + group)) {
                    ConfigurationSection g = groupsSec.getConfigurationSection(group);
                    if (g != null) {
                        cooldown = g.getLong("cooldown", cooldown);
                    }
                }
            }
        }
        return cooldown;
    }

    /** Level for potion-type perks (Haste, Speed, Strength). Default if absent. */
    public int getEffectLevel(PerkType type, Player player, int defaultLevel) {
        Map<UUID, Integer> temp = tempLevels.get(type);
        Integer override = temp.get(player.getUniqueId());
        if (override != null) return override;

        ConfigurationSection perkSec = getPerkSection(type);
        if (perkSec == null) return defaultLevel;

        int level = defaultLevel;
        ConfigurationSection defSec = perkSec.getConfigurationSection("default");
        if (defSec != null) {
            level = defSec.getInt("level", level);
        }

        ConfigurationSection groupsSec = perkSec.getConfigurationSection("groups");
        if (groupsSec != null) {
            for (String group : groupsSec.getKeys(false)) {
                if (player.hasPermission("group." + group)) {
                    ConfigurationSection g = groupsSec.getConfigurationSection(group);
                    if (g != null) {
                        level = g.getInt("level", level);
                    }
                }
            }
        }
        return level;
    }

    public void setTempLevel(Player p, PerkType type, int level) {
        tempLevels.get(type).put(p.getUniqueId(), level);
    }

    private void clearTempLevel(UUID id, PerkType type) {
        tempLevels.get(type).remove(id);
    }

    // -------- public API --------

    public void toggle(PerkType type, Player player) {
        ActivationMode mode = getMode(type, player);
        if (mode == ActivationMode.ALWAYS) {
            player.sendMessage("§dArcane §7→ §f" + type.getDisplayName() + " §7is always active when you have permission.");
            return;
        }

        if (hasPerk(player, type)) {
            deactivate(type, player);
        } else {
            activate(type, player);
        }
    }

    public boolean activate(PerkType type, Player player) {
        if (!player.hasPermission(type.getPermissionNode())) {
            player.sendMessage("§cYou don't have this perk.");
            return false;
        }

        ActivationMode mode = getMode(type, player);
        if (mode == ActivationMode.ALWAYS) {
            player.sendMessage("§dArcane §7→ §f" + type.getDisplayName() + " §7is always active.");
            return true;
        }

        long now = System.currentTimeMillis();
        long duration = getDuration(type, player);
        long cooldown = getCooldown(type, player);

        boolean bypassCd = player.hasPermission("arcaneperks.admin.nocooldown");

        if (!bypassCd) {
            Map<UUID, Long> cdMap = cooldownUntil.get(type);
            Long cdUntil = cdMap.get(player.getUniqueId());
            if (cdUntil != null && cdUntil > now) {
                long left = (cdUntil - now) / 1000L;
                sendCooldownChat(player, type, left);
                return false;
            }
        }

        long activeMillis;
        if (duration < 0) {
            activeMillis = Long.MAX_VALUE; // infinite
        } else {
            activeMillis = now + Math.max(1L, duration) * 1000L;
        }

        activeUntil.get(type).put(player.getUniqueId(), activeMillis);

        if (!bypassCd) {
            cooldownUntil.get(type).put(player.getUniqueId(), now + Math.max(0L, cooldown) * 1000L);
        }

        sendActivationVisuals(player, type, duration);
        applySideEffectsOnActivate(type, player);

        return true;
    }

    public void deactivate(PerkType type, Player player) {
        activeUntil.get(type).remove(player.getUniqueId());
        clearTempLevel(player.getUniqueId(), type);
        sendDeactivationChat(player, type);
        applySideEffectsOnDeactivate(type, player);
    }

    public void deactivateAll(Player player) {
        UUID id = player.getUniqueId();
        for (PerkType type : PerkType.values()) {
            if (hasPerk(player, type)) {
                activeUntil.get(type).remove(id);
                clearTempLevel(id, type);
                sendDeactivationChat(player, type);
                applySideEffectsOnDeactivate(type, player);
            }
        }
    }

    public boolean hasPerk(Player player, PerkType type) {
        if (!player.hasPermission(type.getPermissionNode())) return false;

        ActivationMode mode = getMode(type, player);
        if (mode == ActivationMode.ALWAYS) return true;

        Map<UUID, Long> map = activeUntil.get(type);
        Long until = map.get(player.getUniqueId());
        if (until == null) return false;
        if (until != Long.MAX_VALUE && System.currentTimeMillis() > until) {
            map.remove(player.getUniqueId());
            clearTempLevel(player.getUniqueId(), type);
            return false;
        }
        return true;
    }

    public void tick() {
        long now = System.currentTimeMillis();

        // expiry
        for (PerkType type : PerkType.values()) {
            Map<UUID, Long> map = activeUntil.get(type);
            Iterator<Map.Entry<UUID, Long>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> e = it.next();
                long until = e.getValue();
                if (until != Long.MAX_VALUE && until < now) {
                    it.remove();
                    UUID id = e.getKey();
                    clearTempLevel(id, type);
                    Player p = Bukkit.getPlayer(id);
                    if (p != null && p.isOnline()) {
                        long cdLeft = 0;
                        Map<UUID, Long> cdMap = cooldownUntil.get(type);
                        Long cdUntil = cdMap.get(id);
                        if (cdUntil != null && cdUntil > now) {
                            cdLeft = (cdUntil - now) / 1000L;
                        }
                        sendExpireChat(p, type, cdLeft);
                        applySideEffectsOnDeactivate(type, p);
                    }
                }
            }
        }

        // clean cooldowns
        for (Map<UUID, Long> map : cooldownUntil.values()) {
            map.entrySet().removeIf(e -> e.getValue() < now);
        }

        // stacked active perks in action bar
        boolean showActive = plugin.getConfig().getBoolean("visuals.show-active-actionbar", true);
        String activeFormat = plugin.getConfig().getString(
                "visuals.active-actionbar",
                "&dArcane &7» &f%perks%");

        if (!showActive) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            List<String> segments = new ArrayList<>();

            for (PerkType type : PerkType.values()) {
                Map<UUID, Long> map = activeUntil.get(type);
                Long until = map.get(id);
                if (until == null) continue;

                String timeStr;
                if (until == Long.MAX_VALUE) {
                    timeStr = "∞";
                } else {
                    long left = (until - now) / 1000L;
                    if (left < 0) continue;
                    timeStr = left + "s";
                }
                segments.add(type.getDisplayName() + " " + timeStr);
            }

            if (!segments.isEmpty()) {
                String list = String.join(" §7| §f", segments);
                String msg = activeFormat
                        .replace("&", "§")
                        .replace("%perks%", list);
                player.sendActionBar(Component.text(msg));
            }
        }
    }

    // ---- side effects ----

    private void applySideEffectsOnActivate(PerkType type, Player p) {
        switch (type) {
            case FLY -> p.setAllowFlight(true);
            case VANISH -> {
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!other.equals(p)) {
                        other.hidePlayer(plugin, p);
                    }
                }
            }
            case GLOWING -> p.setGlowing(true);
            case NIGHT_VISION, FAST_DIGGING, SPEED, STRENGTH -> plugin.applyPassivePotionPerks(p);
            default -> {}
        }
    }

    private void applySideEffectsOnDeactivate(PerkType type, Player p) {
        switch (type) {
            case FLY -> {
                if (p.getGameMode() != GameMode.CREATIVE) {
                    p.setAllowFlight(false);
                }
            }
            case VANISH -> {
                for (Player other : Bukkit.getOnlinePlayers()) {
                    other.showPlayer(plugin, p);
                }
            }
            case GLOWING -> p.setGlowing(false);
            case NIGHT_VISION -> p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            case FAST_DIGGING -> p.removePotionEffect(PotionEffectType.HASTE);
            case SPEED -> p.removePotionEffect(PotionEffectType.SPEED);
            case STRENGTH -> p.removePotionEffect(PotionEffectType.STRENGTH);
            default -> {}
        }
    }

    // ---- visuals ----

    private void sendActivationVisuals(Player p, PerkType type, long durationSec) {
        if (plugin.getConfig().getBoolean("visuals.show-activation-chat", true)) {
            String time = durationSec < 0 ? "∞" : durationSec + "s";
            String msg = plugin.getConfig().getString(
                            "visuals.activation-chat",
                            "&dArcane &f%perk% &7activated for &f%time%")
                    .replace("&", "§")
                    .replace("%perk%", type.getDisplayName())
                    .replace("%time%", time);
            p.sendMessage(msg);
        }

        if (plugin.getConfig().getBoolean("visuals.show-activation-title", true)) {
            String title = plugin.getConfig().getString(
                            "visuals.activation-title",
                            "&dArcane")
                    .replace("&", "§")
                    .replace("%perk%", type.getDisplayName());
            String subtitle = plugin.getConfig().getString(
                            "visuals.activation-subtitle",
                            "&f%perk% &7activated")
                    .replace("&", "§")
                    .replace("%perk%", type.getDisplayName());

            int staySec = plugin.getConfig().getInt("visuals.activation-title-seconds", 3);

            Title t = Title.title(
                    Component.text(title),
                    Component.text(subtitle),
                    Title.Times.times(
                            Duration.ofMillis(150),
                            Duration.ofSeconds(staySec),
                            Duration.ofMillis(150)
                    )
            );
            p.showTitle(t);
        }
    }

    private void sendDeactivationChat(Player p, PerkType type) {
        if (!plugin.getConfig().getBoolean("visuals.show-deactivation-chat", true)) return;

        String msg = plugin.getConfig().getString(
                        "visuals.deactivation-chat",
                        "&dArcane &f%perk% &7deactivated")
                .replace("&", "§")
                .replace("%perk%", type.getDisplayName());

        p.sendMessage(msg);
    }

    private void sendExpireChat(Player p, PerkType type, long cdLeft) {
        if (!plugin.getConfig().getBoolean("visuals.show-expire-chat", true)) return;

        String time = cdLeft > 0 ? cdLeft + "s" : "0s";
        String msg = plugin.getConfig().getString(
                        "visuals.expire-chat",
                        "&dArcane &f%perk% &7expired. Cooldown &f%time%")
                .replace("&", "§")
                .replace("%perk%", type.getDisplayName())
                .replace("%time%", time);

        p.sendMessage(msg);
    }

    private void sendCooldownChat(Player p, PerkType type, long secondsLeft) {
        if (!plugin.getConfig().getBoolean("visuals.show-cooldown-chat", true)) return;

        String time = secondsLeft + "s";
        String msg = plugin.getConfig().getString(
                        "visuals.cooldown-chat",
                        "&dArcane &f%perk% &7cooldown &f%time%")
                .replace("&", "§")
                .replace("%perk%", type.getDisplayName())
                .replace("%time%", time);

        p.sendMessage(msg);
    }

    // /ap edit and /ap setlevel

    public void editDefault(PerkType type, long duration, long cooldown) {
        plugin.getConfig().set("perks." + type.getId() + ".default.duration", duration);
        plugin.getConfig().set("perks." + type.getId() + ".default.cooldown", cooldown);
        plugin.saveConfig();
    }

    public void setConfigLevel(PerkType type, int level) {
        plugin.getConfig().set("perks." + type.getId() + ".default.level", level);
        plugin.saveConfig();
    }
}
