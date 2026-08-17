package net.islandcore.plugin.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Msg {

    private Msg() {}

    /** Translates '&' colour codes and '%nl%' (the Skript newline placeholder) into real newlines. */
    public static String color(String raw) {
        if (raw == null) return "";
        String withNewlines = raw.replace("%nl%", "\n");
        return ChatColor.translateAlternateColorCodes('&', withNewlines);
    }

    public static void send(CommandSender to, String raw) {
        for (String line : color(raw).split("\n")) {
            to.sendMessage(line);
        }
    }

    public static void sendTitle(Player to, String raw) {
        to.sendTitle(color(raw), "", 10, 70, 20);
    }

    public static void actionBar(Player to, String raw) {
        to.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(raw)));
    }
}
