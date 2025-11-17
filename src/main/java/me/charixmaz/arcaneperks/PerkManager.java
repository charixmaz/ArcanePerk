package me.charixmaz.arcaneperks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;

public class PerkManager {

    private enum ActivationMode {
        ALWAYS, TIMED
    }

    private final ArcanePerks plugin;

    private final Map<PerkType, Map<UUID, Long>> activeUntil = new EnumMap<>(PerkType.class);
    private final Map<PerkType, Map<UUID, Long>> cooldownUntil = new EnumMap<>(PerkType.class);
    private final Map<PerkType, Map<UUID, Integer>> tempLevels = new EnumMap<>(PerkType.class);

    public PerkManager(ArcanePerks plugin) {
        this.plugin = plugin;
        for (PerkType type : PerkType.values()) {
            activeUntil.put(type, new HashMap<>());
            cooldownUntil.put(type, new HashMap<>());
            tempLevels.put(type, new HashMap<>());
        }
    }

    // -------- config base --------

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
        return defSec != null ? defSec.getLong("duration", 30) : 30;
    }

    // -------- LuckPerms helpers --------

    private int getMaxSuffixInt(Player p, String prefix) {
        int best = -1;
        for (PermissionAttachmentInfo info : p.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission().toLowerCase(Locale.ROOT);
            if (!perm.startsWith(prefix)) continue;
            String suffix = perm.substring(prefix.length());
            if (suffix.isEmpty()) continue;
            try {
                int val = Integer.parseInt(suffix);
                if (val > best) best = val;
            } catch (NumberFormatException ignored) {}
        }
        return best;
    }

    private int getMinSuffixInt(Player p, String prefix) {
        int best = -1;
        for (PermissionAttachmentInfo info : p.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission().toLowerCase(Locale.ROOT);
            if (!perm.startsWith(prefix)) continue;
            String suffix = perm.substring(prefix.length());
            if (suffix.isEmpty()) continue;
            try {
                int val = Integer.parseInt(suffix);
                if (best == -1 || val < best) best = val;
            } catch (NumberFormatException ignored) {}
        }
        return best;
    }

    private long getCooldown(PerkType type, Player player) {
        long cooldown = 300;

        ConfigurationSection perkSec = getPerkSection(type);
        if (perkSec != null) {
            ConfigurationSection defSec = perkSec.getConfigurationSection("default");
            if (defSec != null) {
                cooldown = defSec.getLong("cooldown", cooldown);
            }
        }

        String prefix = ("arcaneperks." + type.getId() + ".cooldown.").toLowerCase(Locale.ROOT);
        int permCd = getMinSuffixInt(player, prefix);
        if (permCd >= 0) cooldown = permCd;

        // apply min-cooldown limit
        long minCd = getMinCooldownLimit(type);
        if (cooldown < minCd) cooldown = minCd;

        return cooldown;
    }

    private long getMinCooldownLimit(PerkType type) {
        ConfigurationSection limits = plugin.getConfig().getConfigurationSection("limits");
        if (limits == null) return 0;
        ConfigurationSection sec = limits.getConfigurationSection(type.getId());
        if (sec == null) return 0;
        return sec.getLong("min-cooldown", 0);
    }

    public int getEffectLevel(PerkType type, Player player, int defaultLevel) {
        Map<UUID, Integer> temp = tempLevels.get(type);
        Integer override = temp.get(player.getUniqueId());
        if (override != null) return override;

        int level = defaultLevel;

        ConfigurationSection perkSec = getPerkSection(type);
        if (perkSec != null) {
            ConfigurationSection defSec = perkSec.getConfigurationSection("default");
            if (defSec != null) {
                level = defSec.getInt("level", level);
            }
        }

        String prefix = ("arcaneperks." + type.getId() + ".").toLowerCase(Locale.ROOT);
        int permLevel = getMaxSuffixInt(player, prefix);
        if (permLevel > 0) level = permLevel;

        int max = getMaxLevelLimit(type);
        if (max > 0 && level > max) level = max;
        if (level < 1) level = 1;
        return level;
    }

    private int getMaxLevelLimit(PerkType type) {
        ConfigurationSection limits = plugin.getConfig().getConfigurationSection("limits");
        if (limits == null) return 0;
        ConfigurationSection sec = limits.getConfigurationSection(type.getId());
        if (sec == null) return 0;
        return sec.getInt("max-level", 0);
    }

    public void setTempLevel(Player p, PerkType type, int level) {
        tempLevels.get(type).put(p.getUniqueId(), level);
    }

    private void clearTempLevel(UUID id, PerkType type) {
        tempLevels.get(type).remove(id);
    }

    // -------- restriction checks --------

    private boolean isWorldDisabled(Player p) {
        List<String> disabled = plugin.getConfig().getStringList("restrictions.disabled-worlds");
        return disabled.stream().anyMatch(w ->
                w.equalsIgnoreCase(p.getWorld().getName()));
    }

    private boolean isGamemodeDisabled(Player p) {
        List<String> list = plugin.getConfig().getStringList("restrictions.disable-in-gamemodes");
        for (String gmName : list) {
            try {
                GameMode gm = GameMode.valueOf(gmName.toUpperCase(Locale.ROOT));
                if (p.getGameMode() == gm) return true;
            } catch (IllegalArgumentException ignored) {}
        }
        return false;
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
            playError(player);
            return false;
        }

        if (isWorldDisabled(player) || isGamemodeDisabled(player)) {
            player.sendMessage("§cYou cannot use Arcane perks here.");
            playError(player);
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
                playError(player);
                log("COOLDOWN " + type.getId() + " for " + player.getName() + " (" + left + "s left)");
                return false;
            }
        }

        long activeMillis;
        if (duration < 0) {
            activeMillis = Long.MAX_VALUE;
        } else {
            activeMillis = now + Math.max(1L, duration) * 1000L;
        }

        activeUntil.get(type).put(player.getUniqueId(), activeMillis);

        if (!bypassCd) {
            cooldownUntil.get(type).put(player.getUniqueId(), now + Math.max(0L, cooldown) * 1000L);
        }

        sendActivationVisuals(player, type, duration);
        applySideEffectsOnActivate(type, player);
        playActivate(player);
        log("ACTIVATE " + type.getId() + " for " + player.getName());
        return true;
    }

    public void deactivate(PerkType type, Player player) {
        activeUntil.get(type).remove(player.getUniqueId());
        clearTempLevel(player.getUniqueId(), type);
        sendDeactivationChat(player, type);
        applySideEffectsOnDeactivate(type, player);
        playDeactivate(player);
        log("DEACTIVATE " + type.getId() + " for " + player.getName());
    }

    public void deactivateAll(Player player) {
        UUID id = player.getUniqueId();
        for (PerkType type : PerkType.values()) {
            if (hasPerk(player, type)) {
                activeUntil.get(type).remove(id);
                clearTempLevel(id, type);
                sendDeactivationChat(player, type);
                applySideEffectsOnDeactivate(type, player);
                log("DEACTIVATE ALL " + type.getId() + " for " + player.getName());
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
                        playDeactivate(p);
                        log("EXPIRE " + type.getId() + " for " + p.getName());
                    }
                }
            }
        }

        for (Map<UUID, Long> map : cooldownUntil.values()) {
            map.entrySet().removeIf(e -> e.getValue() < now);
        }

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

    // ---- visuals, sounds, logging ----

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

        if (plugin.getConfig().getBoolean("visuals.particles.enabled", true)) {
            String typeName = plugin.getConfig().getString("visuals.particles.activate", "HAPPY_VILLAGER");
            try {
                Particle particle = Particle.valueOf(typeName.toUpperCase(Locale.ROOT));
                p.getWorld().spawnParticle(particle,
                        p.getLocation().add(0, 1.2, 0),
                        10, 0.3, 0.3, 0.3, 0.01);
            } catch (IllegalArgumentException ignored) {}
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

    private void log(String message) {
        if (!plugin.getConfig().getBoolean("logging.enabled", true)) return;
        if (plugin.getConfig().getBoolean("logging.to-console", true)) {
            plugin.getLogger().info(message);
        }
    }

    public void playActivate(Player p) {
        if (!plugin.getConfig().getBoolean("visuals.sounds.enabled", true)) return;
        String s = plugin.getConfig().getString("visuals.sounds.activate", "BLOCK_NOTE_BLOCK_PLING");
        try {
            Sound sound = Sound.valueOf(s.toUpperCase(Locale.ROOT));
            p.playSound(p.getLocation(), sound, 1.0f, 1.2f);
        } catch (IllegalArgumentException ignored) {}
    }

    public void playDeactivate(Player p) {
        if (!plugin.getConfig().getBoolean("visuals.sounds.enabled", true)) return;
        String s = plugin.getConfig().getString("visuals.sounds.deactivate", "BLOCK_NOTE_BLOCK_BASS");
        try {
            Sound sound = Sound.valueOf(s.toUpperCase(Locale.ROOT));
            p.playSound(p.getLocation(), sound, 1.0f, 0.8f);
        } catch (IllegalArgumentException ignored) {}
    }

    public void playError(Player p) {
        if (!plugin.getConfig().getBoolean("visuals.sounds.enabled", true)) return;
        String s = plugin.getConfig().getString("visuals.sounds.error", "BLOCK_NOTE_BLOCK_BASS");
        try {
            Sound sound = Sound.valueOf(s.toUpperCase(Locale.ROOT));
            p.playSound(p.getLocation(), sound, 1.0f, 0.5f);
        } catch (IllegalArgumentException ignored) {}
    }

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
