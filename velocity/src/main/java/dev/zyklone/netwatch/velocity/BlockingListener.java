package dev.zyklone.netwatch.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;

public class BlockingListener {
    private final NetWatchPlugin plugin = NetWatchPlugin.getInstance();

    @Subscribe
    public void onLogin(PreLoginEvent event) {
        if (this.plugin.check(event.getUniqueId())) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(plugin.kickMessage()));
        }
    }
}
