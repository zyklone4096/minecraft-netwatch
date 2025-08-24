package dev.zyklone.netwatch.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

// listener for async check
public class JoinListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        NetWatchPlugin inst = NetWatchPlugin.getInstance();
        if (inst.config().async()) {    // async check
            inst.checkAsync(event.getPlayer().getUniqueId());
        }
    }
}
