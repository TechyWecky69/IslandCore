package net.islandcore.plugin.ratings;

import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.Symbols;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

/** Builds the clickable "rate this island 1-5" chat prompt used in a couple of places. */
public final class RatingPrompt {

    private RatingPrompt() {}

    public static void send(Player player, String targetName) {
        Msg.send(player, "&6" + Symbols.STAR_FULL + " &7How was &b" + targetName + "&7's island? Click to rate:");

        TextComponent line = new TextComponent("  ");
        for (int i = 1; i <= 5; i++) {
            TextComponent star = new TextComponent(Symbols.STAR_FULL + " ");
            star.setColor(net.md_5.bungee.api.ChatColor.GOLD);
            star.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rate " + targetName + " " + i));
            star.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Msg.color("&7Rate " + i + "/5"))));
            line.addExtra(star);
        }
        player.spigot().sendMessage(line);
    }
}
