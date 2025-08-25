package dev.zyklone.netwatch.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;

public class AsyncListener {
    private final NetWatchPlugin plugin = NetWatchPlugin.getInstance();

    @Subscribe
    public void onLogin(PostLoginEvent event) {
        this.plugin.getApi().getExecutor().execute(() -> {
            if (this.plugin.check(event.getPlayer().getUniqueId())) {
                this.plugin.getServer().getScheduler().buildTask(this.plugin, () -> {   // kick player on proxy scheduler
                    event.getPlayer().disconnect(this.plugin.kickMessage());
                });
            }
        });
    }
}
