package net.islandcore.plugin.commands;

import net.islandcore.plugin.skilltree.SkillTreeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillTreeCommand implements CommandExecutor {

    private final SkillTreeGUI gui;

    public SkillTreeCommand(SkillTreeGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        gui.openTreeTab(player);
        return true;
    }
}
