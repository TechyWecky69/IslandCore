package net.islandcore.plugin.commands;

import net.islandcore.plugin.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;


public class DiscordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof org.bukkit.entity.Player p) {
            p.spigot().sendMessage(new net.md_5.bungee.api.chat.ComponentBuilder(Msg.color("&1DISCORD SERVER >> &f"))
                    .append(Msg.color("&b[CLICK]"))
                    .event(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, "https://discord.gg/"))
                    .event(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text("Click to copy to clipboard")))
                    .create());
        }
        return true;
    }

}
