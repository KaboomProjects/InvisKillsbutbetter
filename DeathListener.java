package dev.killcorrupt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DeathListener implements Listener {

    private static final String GLITCH_CHARS = "#?!$%&*@~^+=";

    private final KillCorrupt plugin;

    public DeathListener(KillCorrupt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Component original = event.deathMessage();
        if (original == null) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller(); // also covers arrows, tridents and other indirect kills

        boolean corruptKiller = killer != null && plugin.isCorrupted(killer.getUniqueId());
        boolean corruptVictim = plugin.getConfig().getBoolean("corrupt-victim", false)
                && plugin.isCorrupted(victim.getUniqueId());
        if (!corruptKiller && !corruptVictim) return;

        String mode = plugin.getConfig().getString("mode", "GLITCH").toUpperCase(Locale.ROOT);

        // CUSTOM: replace the whole message (only for kills by a corrupted killer)
        if (mode.equals("CUSTOM") && corruptKiller) {
            String format = plugin.getConfig().getString("custom-format", "&k???&r was slain by &k???")
                    .replace("%killer%", killer.getName())
                    .replace("%victim%", victim.getName());
            event.deathMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
            return;
        }

        // Collect every name form (account name + display name) that should be replaced
        Map<String, Component> replacements = new LinkedHashMap<>();
        if (corruptKiller) addNames(replacements, killer, mode);
        if (corruptVictim) addNames(replacements, victim, mode);
        if (replacements.isEmpty()) return;

        // Rebuild from plain text: this also removes the hover/click entity data that would leak the name
        String plain = PlainTextComponentSerializer.plainText().serialize(original);
        event.deathMessage(replace(plain, replacements));
    }

    private void addNames(Map<String, Component> map, Player player, String mode) {
        Component replacement = makeReplacement(player.getName(), mode);
        map.put(player.getName(), replacement);
        String display = PlainTextComponentSerializer.plainText().serialize(player.displayName());
        if (!display.isBlank() && !display.equals(player.getName())) {
            map.put(display, replacement);
        }
    }

    private Component makeReplacement(String realName, String mode) {
        return switch (mode) {
            case "HIDDEN" -> Component.text(plugin.getConfig().getString("hidden-name", "Unknown"));
            case "OBFUSCATED" -> Component.text(realName).decorate(TextDecoration.OBFUSCATED);
            default -> Component.text(glitch(Math.max(1, plugin.getConfig().getInt("glitch-length", 6))));
        };
    }

    private String glitch(int length) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(GLITCH_CHARS.charAt(rnd.nextInt(GLITCH_CHARS.length())));
        return sb.toString();
    }

    /** Replaces whole-word occurrences of the given names, longest names first. */
    private Component replace(String text, Map<String, Component> replacements) {
        String alternation = replacements.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        Pattern pattern = Pattern.compile("(?<![A-Za-z0-9_])(" + alternation + ")(?![A-Za-z0-9_])");
        Matcher m = pattern.matcher(text);

        Component out = Component.empty();
        int last = 0;
        while (m.find()) {
            if (m.start() > last) out = out.append(Component.text(text.substring(last, m.start())));
            out = out.append(replacements.get(m.group(1)));
            last = m.end();
        }
        if (last < text.length()) out = out.append(Component.text(text.substring(last)));
        return out;
    }
}
