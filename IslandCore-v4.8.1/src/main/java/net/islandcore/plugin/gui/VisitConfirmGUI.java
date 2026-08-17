package net.islandcore.plugin.gui;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.managers.IslandManager;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.util.IslandVisitorUtil;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.TeleportCooldownManager;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VisitConfirmGUI implements Listener {

    private static final String TITLE_PREFIX = "§8Visit §7";

    private final DataStore data;
    private final IslandManager islandManager;
    private final RatingManager ratings;
    private final Map<UUID, UUID> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> menus = new ConcurrentHashMap<>();

    public VisitConfirmGUI(DataStore data, IslandManager islandManager, RatingManager ratings) {
        this.data = data;
        this.islandManager = islandManager;
        this.ratings = ratings;
    }

    public void open(Player visitor, OfflinePlayer target) {
        String title = TITLE_PREFIX + target.getName();
        Inventory inv = Bukkit.createInventory(null, 9, title);

        ItemStack confirm = makeItem(Material.LIME_STAINED_GLASS_PANE, "&aConfirm Visit",
                List.of(
                        "&7Click to visit &b" + target.getName() + "&7's island.",
                        "",
                        "&7On their island you can explore freely,",
                        "&7build anything you like, and make it",
                        "&7your own creative space!"
                ));
        for (int i = 0; i < 4; i++) inv.setItem(i, confirm);

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwningPlayer(target);
        skullMeta.setDisplayName(Msg.color("&b" + target.getName() + "&7's Island"));

        double avg = ratings.getAverage(target.getUniqueId());
        int votes = ratings.getVoteCount(target.getUniqueId());
        int autoScore = ratings.getAutoScore(target.getUniqueId());
        String badge = ratings.ownerStarBadge(target.getUniqueId());
        String ratingLine = votes > 0
                ? "&6" + ratings.formatStars(avg) + badge + " &7(" + votes + " ratings) &8| &7Score: &a" + autoScore
                : "&7Not yet rated" + badge + " &8| &7Score: &a" + autoScore;

        skullMeta.setLore(List.of(
                Msg.color("&7Visit this island and explore or"),
                Msg.color("&7build whatever you want — it's their"),
                Msg.color("&7space but you're free to create!"),
                Msg.color(""),
                Msg.color(ratingLine),
                Msg.color(""),
                Msg.color("&a▶ Click to confirm teleport")
        ));
        skull.setItemMeta(skullMeta);
        inv.setItem(4, skull);

        ItemStack cancel = makeItem(Material.RED_STAINED_GLASS_PANE, "&cCancel",
                List.of("&7Click to cancel this visit."));
        for (int i = 5; i < 9; i++) inv.setItem(i, cancel);

        pending.put(visitor.getUniqueId(), target.getUniqueId());
        menus.put(visitor.getUniqueId(), inv);
        visitor.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player visitor)) return;
        Inventory menu = menus.get(visitor.getUniqueId());
        if (menu == null || event.getView().getTopInventory() != menu) return;
        if (event.getClickedInventory() != menu) return;

        event.setCancelled(true);

        UUID targetId = pending.get(visitor.getUniqueId());
        if (targetId == null) {
            visitor.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (slot >= 0 && slot <= 4) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
            visitor.closeInventory();

            long remaining = TeleportCooldownManager.remainingSeconds(visitor.getUniqueId());
            if (remaining > 0) {
                Msg.send(visitor, "&c⚠ &7Please wait &e" + remaining + "s &7before teleporting again.");
                return;
            }

            if (!WorldUtil.islandExists(targetId)) {
                Msg.send(visitor, "&c‼ &7Oops! Looks like " + target.getName() + " does not have an island!");
                return;
            }

            if (!data.isVisitable(targetId) && !visitor.isOp()) {
                Msg.send(visitor, "&c‼ &7Oops! Looks like " + target.getName() + " has disabled island visits!");
                return;
            }

            if (!islandManager.load(targetId)) {
                Msg.send(visitor, "&c‼ &7The island could not be loaded right now.");
                return;
            }

            // Multiverse loads the world asynchronously from the command's
            // point of view, so wait briefly before teleporting.
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("IslandCore"),
                    () -> {
                        if (!visitor.isOnline()) return;

                        World world = WorldUtil.getIslandWorld(targetId);
                        if (world == null) {
                            Msg.send(visitor, "&c‼ &7The island could not be loaded right now.");
                            return;
                        }

                        visitor.teleport(world.getSpawnLocation());
                        visitor.setAllowFlight(true);
                        TeleportCooldownManager.markTeleport(visitor.getUniqueId());
                        ratings.recordVisitStart(visitor.getUniqueId(), targetId);
                        Msg.send(visitor, "&aYou have arrived at &b" + target.getName() + "&a's island!");
                    },
                    10L
            );
        } else {
            visitor.closeInventory();
            Msg.send(visitor, "&7Visit cancelled.");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory menu = menus.get(player.getUniqueId());
        if (menu != null && event.getView().getTopInventory() == menu) {
            menus.remove(player.getUniqueId());
            pending.remove(player.getUniqueId());
        }
    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(Msg.color(name));
        meta.setLore(lore.stream().map(Msg::color).toList());
        stack.setItemMeta(meta);
        return stack;
    }
}
