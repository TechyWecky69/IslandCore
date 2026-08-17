package net.islandcore.plugin.tasks;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.skilltree.SkillTreeManager;
import net.islandcore.plugin.util.CountdownManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.Symbols;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Every loot interval:
 *  1. Awards 1 Tree Token to each eligible player.
 *  2. Gives one item drawn from their active skill tree node's pool.
 *
 * The old flat loot table is replaced entirely by the per-player skill tree.
 */
public class LootTask extends BukkitRunnable {

    private final DataStore data;
    private final SkillTreeManager skillTreeManager;
    private final int intervalSeconds;
    private final Random random = new Random();

    public LootTask(DataStore data, SkillTreeManager skillTreeManager, int intervalSeconds) {
        this.data = data;
        this.skillTreeManager = skillTreeManager;
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!WorldUtil.isInOwnWorld(player)) continue;
            if (!data.isLootingActive(player.getUniqueId())) continue;

            // Award a token every cycle
            skillTreeManager.addTokens(player.getUniqueId(), 1);

            // Pick item from skill tree
            ItemStack item = skillTreeManager.pickLoot(player.getUniqueId(), random);
            if (item == null) {
                Msg.send(player, "&7No active skill tree node selected. Use &b/skilltree &7to pick one.");
                CountdownManager.set(player.getUniqueId(), intervalSeconds);
                continue;
            }

            boolean isFull = isInventoryFull(player, item.getType());
            if (isFull) {
                player.sendMessage(Msg.color("&c" + Symbols.WARNING + " &7Item was not given because inventory is full!"));
            } else {
                player.getInventory().addItem(item);
                data.incrementLootPulls(player.getUniqueId());
            }

            CountdownManager.set(player.getUniqueId(), intervalSeconds);
        }
    }

    private boolean isInventoryFull(Player player, Material itemToGive) {
        if (player.getInventory().firstEmpty() != -1) return false;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == itemToGive && item.getAmount() < item.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }
}
