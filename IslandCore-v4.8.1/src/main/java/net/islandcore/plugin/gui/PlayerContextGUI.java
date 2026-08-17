package net.islandcore.plugin.gui;

import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.WorldUtil;
import net.islandcore.plugin.util.IslandVisitorUtil;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.ratings.RatingPrompt;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerContextGUI implements Listener {

    private static final String TITLE_PREFIX = "§8Player: §7";

    private final Map<UUID, UUID>      targets = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> menus   = new ConcurrentHashMap<>();

    private final VisitConfirmGUI visitConfirmGUI;
    private final RankManager ranks;
    private final RatingManager ratings;

    public PlayerContextGUI(VisitConfirmGUI visitConfirmGUI, RankManager ranks, RatingManager ratings) {
        this.visitConfirmGUI = visitConfirmGUI;
        this.ranks = ranks;
        this.ratings = ratings;
    }


    @EventHandler
    public void onPlayerRightClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player opener = event.getPlayer();
        if (!WorldUtil.isInOwnWorld(opener)) return;
        if (opener.getUniqueId().equals(target.getUniqueId())) return;

        event.setCancelled(true);
        open(opener, target);
    }

    private void open(Player opener, Player target) {
        String title = TITLE_PREFIX + target.getName();
        Inventory inv = Bukkit.createInventory(null, 9, title);

        // Slot 1: target's player head
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) Objects.requireNonNull(skull.getItemMeta());
        skullMeta.setOwningPlayer(target);
        skullMeta.setDisplayName(Msg.color("&b" + target.getName()));
        skullMeta.setLore(List.of(
                Msg.color("&7Visitor on your island."),
                Msg.color(""),
                Msg.color("&7Use the options to the right")
        ));
        skull.setItemMeta(skullMeta);
        inv.setItem(1, skull);

        // Slot 3: anvil — kick
        inv.setItem(3, makeItem(Material.ANVIL, "&cKick from Island",
                List.of(
                        "&7Send &b" + target.getName() + " &7back to",
                        "&7their own island immediately."
                )));

        // Slot 4: nether star — rate this island
        double avg = ratings.getAverage(target.getUniqueId());
        int votes = ratings.getVoteCount(target.getUniqueId());
        String badge = ratings.ownerStarBadge(target.getUniqueId());
        String ratingLine = votes > 0
                ? "&6" + ratings.formatStars(avg) + badge + " &7(" + votes + " ratings)"
                : "&7Not yet rated" + badge;

        inv.setItem(4, makeItem(Material.NETHER_STAR, "&6Rate Island",
                List.of(
                        ratingLine,
                        "&7Rate &b" + target.getName() + "&7's island",
                        "&7from 1 to 5 stars."
                )));

        // Slot 5: barrier — report
        inv.setItem(5, makeItem(Material.BARRIER, "&4Report Player",
                List.of(
                        "&7Report &b" + target.getName() + " &7for",
                        "&7misconduct on your island."
                )));

        // Slot 7: name tag — visit their island
        inv.setItem(7, makeItem(Material.NAME_TAG, "&aVisit Island",
                List.of(
                        "&7Teleport to &b" + target.getName() + "&7's",
                        "&7own island."
                )));

        targets.put(opener.getUniqueId(), target.getUniqueId());
        menus.put(opener.getUniqueId(), inv);
        opener.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player opener)) return;
        Inventory menu = menus.get(opener.getUniqueId());
        if (menu == null || event.getView().getTopInventory() != menu) return;
        if (event.getClickedInventory() != menu) return;

        event.setCancelled(true);

        UUID targetId = targets.get(opener.getUniqueId());
        if (targetId == null) { opener.closeInventory(); return; }

        Player target = Bukkit.getPlayer(targetId);
        int slot = event.getRawSlot();

        opener.closeInventory();

        switch (slot) {
            case 4 -> {
                if (target == null) {
                    Msg.send(opener, "&cThat player is no longer online.");
                    return;
                }
                RatingPrompt.send(opener, target.getName());
            }
            case 3 -> {
                if (target == null || !target.isOnline()) {
                    Msg.send(opener, "&c" + (target == null ? "That" : target.getName()) + " is no longer online.");
                    return;
                }
                if (IslandVisitorUtil.kickVisitor(target, opener.getName(), ranks)) {
                    Msg.send(opener, "&7Kicked &b" + target.getName() + " &7back to their island.");
                }
            }
            case 5 -> {
                if (target == null) {
                    Msg.send(opener, "&cThat player is no longer online.");
                    return;
                }
                TextComponent prompt = new TextComponent(Msg.color("&7Click here to report &b" + target.getName() + "&7, then type your reason."));
                prompt.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/report " + target.getName() + " "));
                opener.spigot().sendMessage(prompt);
                Msg.send(opener, "&7Type the reason further on.");
            }
            case 7 -> {
                if (target == null) {
                    Msg.send(opener, "&cThat player is no longer online.");
                    return;
                }
                visitConfirmGUI.open(opener, target);
            }
            default -> {}
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory menu = menus.get(player.getUniqueId());
        if (menu != null && event.getView().getTopInventory() == menu) {
            menus.remove(player.getUniqueId());
            targets.remove(player.getUniqueId());
        }
    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
        meta.setDisplayName(Msg.color(name));
        meta.setLore(lore.stream().map(Msg::color).toList());
        stack.setItemMeta(meta);
        return stack;
    }
}
