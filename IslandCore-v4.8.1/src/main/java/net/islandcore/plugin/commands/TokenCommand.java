package net.islandcore.plugin.commands;

import net.islandcore.plugin.IslandCorePlugin;
import net.islandcore.plugin.skilltree.SkillTreeManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class TokenCommand implements CommandExecutor, Listener {

    private final SkillTreeManager manager;

    private final NamespacedKey tokenKey;

    public TokenCommand(
            IslandCorePlugin plugin,
            SkillTreeManager manager
    ) {

        this.manager = manager;

        this.tokenKey =
                new NamespacedKey(
                        plugin,
                        "tree_token"
                );
    }



    /**
     * Creates a physical Tree Token item.
     */
    public ItemStack createToken(int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Token amount must be greater than zero."
            );
        }

        ItemStack item =
                new ItemStack(
                        Material.SUNFLOWER,
                        Math.min(amount, 64)
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                "§6§lTree Token"
        );

        meta.setLore(
                List.of(
                        "§7A token used by the",
                        "§7IslandCore skill tree."
                )
        );

        meta.getPersistentDataContainer()
                .set(
                        tokenKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        item.setItemMeta(meta);

        return item;
    }

    /**
     * Returns true if an ItemStack is a real IslandCore Tree Token.
     */
    public boolean isToken(ItemStack item) {

        if (item == null ||
                item.getType() != Material.SUNFLOWER) {

            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte value =
                meta.getPersistentDataContainer()
                        .get(
                                tokenKey,
                                PersistentDataType.BYTE
                        );

        return value != null && value == (byte) 1;
    }

    /**
     * /spawntoken <amount>
     *
     * Drops physical Tree Tokens at the player's location.
     */
    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "Only players can spawn Tree Tokens."
            );

            return true;
        }

        if (!player.hasPermission(
                "islandcore.tokens.spawn"
        )) {

            Msg.send(
                    player,
                    "&c" + Symbols.WARNING + " &7You don't have permission to spawn Tree Tokens."
            );

            return true;
        }

        if (args.length != 1) {

            Msg.send(
                    player,
                    "&cUsage: /spawntoken <amount>"
            );

            return true;
        }

        int amount;

        try {

            amount =
                    Integer.parseInt(args[0]);

        } catch (NumberFormatException exception) {

            Msg.send(
                    player,
                    "&c" + Symbols.WARNING + " &7Amount must be a whole number."
            );

            return true;
        }

        if (amount <= 0) {

            Msg.send(
                    player,
                    "&c" + Symbols.WARNING + " &7Amount must be greater than zero."
            );

            return true;
        }

        /*
         * Bukkit item stacks can only hold 64 tokens, so split large
         * amounts into multiple physical stacks.
         */
        int remaining = amount;

        Location location =
                player.getLocation();

        while (remaining > 0) {

            int stackAmount =
                    Math.min(remaining, 64);

            ItemStack token =
                    createToken(stackAmount);

            player.getWorld().dropItemNaturally(
                    location,
                    token
            );

            remaining -= stackAmount;
        }

        Msg.send(
                player,
                "&aSpawned &6"
                        + amount
                        + " &aTree Token"
                        + (amount == 1 ? "" : "s")
                        + "."
        );

        return true;
    }

    /**
     * Turns physical tokens into the player's token balance
     * when they pick them up.
     */
    @EventHandler
    public void onTokenPickup(
            EntityPickupItemEvent event
    ) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack stack =
                event.getItem().getItemStack();

        if (!isToken(stack)) {
            return;
        }

        int amount =
                stack.getAmount();

        manager.addTokens(
                player.getUniqueId(),
                amount
        );

        Msg.send(
                player,
                "&6+"
                        + amount
                        + " Tree Token"
                        + (amount == 1 ? "" : "s")
                        + "&7. Balance: &6"
                        + manager.getTokens(
                        player.getUniqueId()
                )
        );
    }
}