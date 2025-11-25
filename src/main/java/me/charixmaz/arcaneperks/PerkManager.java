package me.charixmaz.arcaneperks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.*;

public class PerkManager {

    public enum ActivationMode {
        ALWAYS,
        TIMED,
        TOGGLE
    }

    private final ArcanePerks plugin;

    private final EnumMap<PerkType, PerkSettings> perkSettings = new EnumMap<>(PerkType.class);
    private final Map<UUID, EnumMap<PerkType, PerkRuntime>> states = new HashMap<>();
    private final Map<UUID, EnumMap<PerkType, Integer>> tempLevels = new HashMap<>();

    private final MiniMessage mini = MiniMessage.miniMessage();

    public PerkManager(ArcanePerks plugin) {
        this.plugin = plugin;
        loadSettingsFromConfig();
    }

    // ---------------------------------------------------------------------
    // CONFIG LOADING
    // ---------------------------------------------------------------------

    private void loadSettingsFromConfig() {
        perkSettings.clear();

        ConfigurationSection perksSection = plugin.getConfig().getConfigurationSection("perks");
        if (perksSection == null) {
            plugin.getLogger().warning("No 'perks' section found in config.yml");
            return;
        }

        for (PerkType type : PerkType.values()) {
            String id = type.getId();
            ConfigurationSection sec = perksSection.getConfigurationSection(id);
            if (sec == null) continue;

            String modeStr = sec.getString("activation", "TIMED").toUpperCase(Locale.ROOT);
            ActivationMode mode;
            try {
                mode = ActivationMode.valueOf(modeStr);
            } catch (IllegalArgumentException ex) {
                mode = ActivationMode.TIMED;
            }

            ConfigurationSection def = sec.getConfigurationSection("default");
            long duration = def != null ? def.getLong("duration", 30) : 30;
            long cooldown = def != null ? def.getLong("cooldown", 60) : 60;

            perkSettings.put(type, new PerkSettings(mode, duration, cooldown));
        }
    }

    public void reload() {
        plugin.reloadConfig();
        loadSettingsFromConfig();
    }

    // ---------------------------------------------------------------------
    // INTERNAL MODELS
    // ---------------------------------------------------------------------

    private EnumMap<PerkType, PerkRuntime> getPlayerMap(UUID id) {
        return states.computeIfAbsent(id, k -> new EnumMap<>(PerkType.class));
    }

    private PerkRuntime getOrCreateState(Player p, PerkType type) {
        EnumMap<PerkType, PerkRuntime> map = getPlayerMap(p.getUniqueId());
        return map.computeIfAbsent(type, t -> new PerkRuntime());
    }

    private EnumMap<PerkType, Integer> getTempLevelMap(UUID id) {
        return tempLevels.computeIfAbsent(id, k -> new EnumMap<>(PerkType.class));
    }

    private static class PerkRuntime {
        boolean active = false;
        long expiresAt = -1L;
        long cooldownUntil = 0L;
    }

    private static class PerkSettings {
        private final ActivationMode mode;
        private final long durationSeconds;
        private final long cooldownSeconds;

        PerkSettings(ActivationMode mode, long durationSeconds, long cooldownSeconds) {
            this.mode = mode;
            this.durationSeconds = durationSeconds;
            this.cooldownSeconds = cooldownSeconds;
        }

        public ActivationMode getMode() {
            return mode;
        }

        public long getDurationSeconds() {
            return durationSeconds;
        }

        public long getCooldownSeconds() {
            return cooldownSeconds;
        }
    }

    // ---------------------------------------------------------------------
    // PUBLIC API
    // ---------------------------------------------------------------------

    public boolean hasPerk(Player p, PerkType type) {
        EnumMap<PerkType, PerkRuntime> map = states.get(p.getUniqueId());
        if (map == null) return false;
        PerkRuntime rt = map.get(type);
        return rt != null && rt.active;
    }

    public int getEffectLevel(PerkType type, Player p, int defaultLevel) {
        // 1) temporary override from /ap test
        EnumMap<PerkType, Integer> tempMap = tempLevels.get(p.getUniqueId());
        if (tempMap != null) {
            Integer tmp = tempMap.get(type);
            if (tmp != null && tmp >= 1) {
                return tmp;
            }
        }

        int level = defaultLevel;

        // 2) config default
        ConfigurationSection perks = plugin.getConfig().getConfigurationSection("perks");
        if (perks != null) {
            ConfigurationSection sec = perks.getConfigurationSection(type.getId());
            if (sec != null) {
                ConfigurationSection def = sec.getConfigurationSection("default");
                if (def != null) {
                    level = def.getInt("level", level);
                }
            }
        }

        // 3) LuckPerms override arcaneperks.<id>.<level> (max)
        int permLevel = getMaxSuffixInt(p, (type.getPermissionNode() + ".").toLowerCase(Locale.ROOT));
        if (permLevel > 0) level = permLevel;

        // 4) limits.<id>.max-level
        ConfigurationSection limits = plugin.getConfig().getConfigurationSection("limits");
        if (limits != null) {
            ConfigurationSection lsec = limits.getConfigurationSection(type.getId());
            if (lsec != null) {
                int max = lsec.getInt("max-level", 0);
                if (max > 0 && level > max) level = max;
            }
        }

        if (level < 1) level = 1;
        return level;
    }

    public void toggle(PerkType type, Player player) {
        PerkRuntime rt = getOrCreateState(player, type);
        if (rt.active) {
            deactivate(type, player);
        } else {
            activate(type, player, true);
        }
    }

    public void activate(PerkType type, Player player) {
        activate(type, player, true);
    }

    public void activate(PerkType type, Player player, boolean fromCommand) {
        PerkSettings settings = perkSettings.get(type);
        if (settings == null) return;

        long now = System.currentTimeMillis();
        PerkRuntime rt = getOrCreateState(player, type);

        if (rt.cooldownUntil > now && settings.getMode() != ActivationMode.ALWAYS) {
            long remaining = (rt.cooldownUntil - now) / 1000L;
            String msg = plugin.getConfig().getString("messages.cooldown",
                    "&cPerk on cooldown: &f<time>&cs.");
            msg = msg.replace("<time>", String.valueOf(remaining));
            player.sendMessage(cc(msg));
            playError(player);
            return;
        }

        rt.active = true;

        if (settings.getMode() == ActivationMode.TIMED) {
            long duration = getDurationSeconds(type, player, settings.getDurationSeconds());
            rt.expiresAt = now + duration * 1000L;
        } else {
            rt.expiresAt = -1L;
        }

        applySideEffectsOnActivate(type, player);

        playActivate(player);
        showActivateTitle(type, player);      // hologram in center
        sendActionBarActivated(type, player);
    }

    public void deactivate(PerkType type, Player player) {
        PerkSettings settings = perkSettings.get(type);
        if (settings == null) return;

        PerkRuntime rt = getOrCreateState(player, type);
        if (!rt.active) return;

        rt.active = false;
        rt.expiresAt = -1L;

        long now = System.currentTimeMillis();
        long cd = getCooldownSeconds(type, player, settings.getCooldownSeconds());
        if (settings.getMode() != ActivationMode.ALWAYS) {
            rt.cooldownUntil = now + cd * 1000L;
            showCooldownStart(type, player, cd);   // chat cooldown
        }

        applySideEffectsOnDeactivate(type, player);

        playDeactivate(player);
        sendActionBarDeactivated(type, player);
    }

    public void deactivateAll(Player player) {
        EnumMap<PerkType, PerkRuntime> map = getPlayerMap(player.getUniqueId());
        for (PerkType type : PerkType.values()) {
            PerkRuntime rt = map.get(type);
            if (rt != null && rt.active) {
                deactivate(type, player);
            }
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();

        for (Player p : Bukkit.getOnlinePlayers()) {
            EnumMap<PerkType, PerkRuntime> map = states.get(p.getUniqueId());
            if (map == null) continue;

            List<String> activeLines = new ArrayList<>();

            for (PerkType type : PerkType.values()) {
                PerkRuntime rt = map.get(type);
                if (rt == null || !rt.active) continue;

                PerkSettings settings = perkSettings.get(type);
                if (settings == null) continue;

                if (settings.getMode() == ActivationMode.TIMED && rt.expiresAt > 0) {
                    long remaining = (rt.expiresAt - now) / 1000L;
                    if (remaining <= 0) {
                        deactivate(type, p);
                        continue;
                    }

                    String line = plugin.getConfig().getString("visuals.actionbar.perk-format",
                            "<white><name> <gray>(<time>s)");
                    line = line.replace("<name>", type.getDisplayName())
                            .replace("<time>", String.valueOf(remaining));
                    activeLines.add(line);
                } else {
                    String line = plugin.getConfig().getString("visuals.actionbar.perk-format-always",
                            "<white><name>");
                    line = line.replace("<name>", type.getDisplayName());
                    activeLines.add(line);
                }
            }

            if (!activeLines.isEmpty() &&
                    plugin.getConfig().getBoolean("visuals.actionbar.enabled", true)) {
                String joined = String.join("  |  ", activeLines);
                Component comp = mini.deserialize(
                        plugin.getConfig().getString("visuals.actionbar.prefix",
                                "<gradient:#ff00ff:#00ffff>Arcane </gradient>")
                                + joined
                );
                p.sendActionBar(comp);
            }
        }
    }

    // ---------------------------------------------------------------------
    // TEMP LEVELS (for /ap test)
// ---------------------------------------------------------------------

    public void setTempLevel(Player p, PerkType type, int level) {
        EnumMap<PerkType, Integer> map = getTempLevelMap(p.getUniqueId());
        map.put(type, level);
    }

    // ---------------------------------------------------------------------
    // PERMISSION HELPERS
    // ---------------------------------------------------------------------

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
            } catch (NumberFormatException ignored) {
            }
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
            } catch (NumberFormatException ignored) {
            }
        }
        return best;
    }

    private long getCooldownSeconds(PerkType type, Player p, long defaultCd) {
        long cd = defaultCd;

        String prefix = (type.getPermissionNode() + ".cooldown.").toLowerCase(Locale.ROOT);
        int permCd = getMinSuffixInt(p, prefix);
        if (permCd >= 0) cd = permCd;

        ConfigurationSection limits = plugin.getConfig().getConfigurationSection("limits");
        if (limits != null) {
            ConfigurationSection sec = limits.getConfigurationSection(type.getId());
            if (sec != null) {
                long min = sec.getLong("min-cooldown", 0L);
                if (cd < min) cd = min;
            }
        }

        if (cd < 0) cd = 0;
        return cd;
    }

    private long getDurationSeconds(PerkType type, Player p, long defaultDuration) {
        long duration = defaultDuration;

        String prefix = (type.getPermissionNode() + ".duration.").toLowerCase(Locale.ROOT);
        int permDur = getMaxSuffixInt(p, prefix);
        if (permDur > 0) duration = permDur;

        if (duration < 0) duration = 0;
        return duration;
    }

    // ---------------------------------------------------------------------
    // VISUALS: sounds + actionbar + titles
    // ---------------------------------------------------------------------

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public void playActivate(Player p) {
        if (!plugin.getConfig().getBoolean("visuals.sounds.enabled", true)) return;
        String s = plugin.getConfig().getString("visuals.sounds.activate", "BLOCK_NOTE_BLOCK_PLING");
        try {
            Sound sound = Sound.valueOf(s.toUpperCase(Locale.ROOT));
            p.playSound(p.getLocation(), sound, 1.0f, 1.2f);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void playDeactivate(Player p) {
        if (!plugin.getConfig().getBoolean("visuals.sounds.enabled", true)) return;
        String s = plugin.getConfig().getString("visuals.sounds.deactivate", "BLOCK_NOTE_BLOCK_BASS");
        try {
            Sound sound = Sound.valueOf(s.toUpperCase(Locale.ROOT));
            p.playSound(p.getLocation(), sound, 1.0f, 0.8f);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void playError(Player p) {
        if (!plugin.getConfig().getBoolean("visuals.sounds.enabled", true)) return;
        String s = plugin.getConfig().getString("visuals.sounds.error", "BLOCK_NOTE_BLOCK_BASS");
        try {
            Sound sound = Sound.valueOf(s.toUpperCase(Locale.ROOT));
            p.playSound(p.getLocation(), sound, 1.0f, 0.5f);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void sendActionBarActivated(PerkType type, Player p) {
        if (!plugin.getConfig().getBoolean("visuals.actionbar.show-activation", true)) return;
        String fmt = plugin.getConfig().getString("visuals.actionbar.activated-format",
                "<green>Arcane <name> activated");
        fmt = fmt.replace("<name>", type.getDisplayName());
        p.sendActionBar(mini.deserialize(fmt));
    }

    private void sendActionBarDeactivated(PerkType type, Player p) {
        if (!plugin.getConfig().getBoolean("visuals.actionbar.show-deactivation", true)) return;
        String fmt = plugin.getConfig().getString("visuals.actionbar.deactivated-format",
                "<red>Arcane <name> deactivated");
        fmt = fmt.replace("<name>", type.getDisplayName());
        p.sendActionBar(mini.deserialize(fmt));
    }

    private void showActivateTitle(PerkType type, Player p) {
        if (!plugin.getConfig().getBoolean("visuals.title.enabled", true)) return;

        String title = plugin.getConfig().getString(
                "visuals.title.activate.title", "&d&lArcane");
        String subtitle = plugin.getConfig().getString(
                "visuals.title.activate.subtitle", "&f<name> &7activated");

        title = cc(title.replace("<name>", type.getDisplayName()));
        subtitle = cc(subtitle.replace("<name>", type.getDisplayName()));

        p.sendTitle(title, subtitle, 5, 60, 10);
    }

    private void showCooldownStart(PerkType type, Player p, long cooldownSeconds) {
        if (!plugin.getConfig().getBoolean("messages.cooldown-start.enabled", true)) return;

        String msg = plugin.getConfig().getString(
                "messages.cooldown-start.text",
                "&dArcane &b<name>&7 cooldown: &b<time>&7s."
        );
        msg = msg.replace("<name>", type.getDisplayName())
                .replace("<time>", String.valueOf(cooldownSeconds));
        p.sendMessage(cc(msg));
    }

    // ---------------------------------------------------------------------
    // SIDE EFFECTS (fly, vanish, glow)
// ---------------------------------------------------------------------

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
            case NIGHT_VISION, FAST_DIGGING, SPEED, STRENGTH ->
                    plugin.applyPassivePotionPerks(p);
            default -> {
            }
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
            default -> {
            }
        }
    }
}
