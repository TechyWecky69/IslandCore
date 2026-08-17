package net.islandcore.plugin.util;

import org.bukkit.command.CommandSender;

/** Central permission check. Console is trusted to run administrative commands. */
public final class PermissionUtil {
    private PermissionUtil() {}

    public static boolean has(CommandSender sender, String permission) {
        // Bukkit's console is the server authority; it should never be blocked
        // by player rank attachments or the plugin's player-only hierarchy.
        if (!(sender instanceof org.bukkit.entity.Player player)) return true;
        // Operators always have full IslandCore access, independent of when
        // their rank attachment was last refreshed.
        if (player.isOp()) return true;
        return sender.hasPermission(permission);
    }

    public static boolean deny(CommandSender sender, String permission) {
        if (has(sender, permission)) return false;
        Msg.send(sender, "&4You do not have permission to do this!");
        return true;
    }
}
