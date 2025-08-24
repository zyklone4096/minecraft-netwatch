package dev.zyklone.netwatch.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

// listener for blocking check
public class LoginListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            NetWatchPlugin inst = NetWatchPlugin.getInstance();
            if (!inst.config().async())  {    // blocking check
                if (inst.check(event.getPlayerProfile().getId())) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, inst.kickMessage());
                }
            }
        }
    }
}
