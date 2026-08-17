package net.islandcore.plugin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class VoidDamageListener implements Listener {

    /**
     * Instantly kills players that take void damage instead of letting the
     * vanilla chip-damage-over-time apply.
     *
     * Bug fix (was causing the "died in the void" message to appear twice
     * everywhere, not just on other islands): the old code called
     * player.setHealth(0.0) without cancelling the underlying event. That
     * manual call kills the player and fires PlayerDeathEvent immediately,
     * but the original EntityDamageEvent then kept running afterwards and
     * applied its own damage on top of the already-dead (0 HP) player,
     * which triggered a second, duplicate PlayerDeathEvent from Bukkit's
     * normal damage-application code. Cancelling the event before killing
     * the player (and ignoring further void damage once they're already
     * dead) means the player is only ever killed once per fall.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVoidDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;

        // Cancel first so the vanilla damage-application code never runs a
        // second, duplicate death for this same tick of void damage.
        event.setCancelled(true);

        if (player.isDead() || player.getHealth() <= 0.0) return;

        player.setHealth(0.0);
    }
}
