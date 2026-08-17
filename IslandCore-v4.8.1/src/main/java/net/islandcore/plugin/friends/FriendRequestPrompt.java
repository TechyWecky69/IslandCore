package net.islandcore.plugin.friends;

import net.islandcore.plugin.util.Msg;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

/** Builds the clickable "you got a friend request" chat prompt. */
public final class FriendRequestPrompt {

    private FriendRequestPrompt() {}

    public static void send(Player target, String requesterName) {
        Msg.send(target, "&e-------------------------------");
        Msg.send(target, "&b" + requesterName + " &7has sent you a friend request!");

        TextComponent line = new TextComponent("  ");

        TextComponent accept = new TextComponent("[ACCEPT]");
        accept.setColor(ChatColor.GREEN);
        accept.setBold(true);
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend accept " + requesterName));
        accept.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT, new Text(Msg.color("&aAccept " + requesterName + "'s request"))));
        line.addExtra(accept);

        line.addExtra(new TextComponent("   "));

        TextComponent deny = new TextComponent("[DENY]");
        deny.setColor(ChatColor.RED);
        deny.setBold(true);
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend deny " + requesterName));
        deny.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT, new Text(Msg.color("&cDeny " + requesterName + "'s request"))));
        line.addExtra(deny);

        target.spigot().sendMessage(line);
        Msg.send(target, "&e-------------------------------");
    }
}
