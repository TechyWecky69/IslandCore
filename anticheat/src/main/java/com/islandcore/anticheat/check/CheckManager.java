package com.islandcore.anticheat.check;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Owns every check instance and exposes a small public surface for the
 * listener classes to call into. The individual check classes are
 * package-private on purpose - this is the only class outside the
 * "check" package that anything should need to touch.
 */
public class CheckManager {

    private final Map<CheckType, Boolean> enabled = new EnumMap<>(CheckType.class);

    private final SpeedCheck speedCheck;
    private final FlyCheck flyCheck;
    private final NoFallCheck noFallCheck;
    private final JesusCheck jesusCheck;
    private final RotationCheck rotationCheck;
    private final ReachCheck reachCheck;
    private final KillAuraCheck killAuraCheck;
    private final AutoClickerCheck autoClickerCheck;
    private final FastBreakCheck fastBreakCheck;
    private final DupeClickCheck dupeClickCheck;
    private final NestedContainerCheck nestedContainerCheck;

    public CheckManager(IslandCoreAntiCheat plugin) {
        this.speedCheck = new SpeedCheck(plugin);
        this.flyCheck = new FlyCheck(plugin);
        this.noFallCheck = new NoFallCheck(plugin);
        this.jesusCheck = new JesusCheck(plugin);
        this.rotationCheck = new RotationCheck(plugin);
        this.reachCheck = new ReachCheck(plugin);
        this.killAuraCheck = new KillAuraCheck(plugin);
        this.autoClickerCheck = new AutoClickerCheck(plugin);
        this.fastBreakCheck = new FastBreakCheck(plugin);
        this.dupeClickCheck = new DupeClickCheck(plugin);
        this.nestedContainerCheck = new NestedContainerCheck(plugin);

        for (CheckType type : CheckType.values()) {
            enabled.put(type, true);
        }
    }

    public void handlePlayerMove(PlayerMoveEvent event) {
        speedCheck.handle(event);
        flyCheck.handle(event);
        noFallCheck.handle(event);
        jesusCheck.handle(event);
        rotationCheck.handle(event);
    }

    public void handleEntityDamageByEntity(EntityDamageByEntityEvent event) {
        reachCheck.handle(event);
        killAuraCheck.handle(event);
        autoClickerCheck.handle(event);
    }

    public void handleBlockBreak(BlockBreakEvent event) {
        fastBreakCheck.handle(event);
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        dupeClickCheck.handle(event);
        nestedContainerCheck.handle(event);
    }

    public boolean isEnabled(CheckType type) {
        return enabled.getOrDefault(type, true);
    }

    public void setEnabled(CheckType type, boolean value) {
        enabled.put(type, value);
    }
}
