package dev.zyklone.netwatch.paper;

import com.google.gson.Gson;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import dev.zyklone.netwatch.core.NetWatchAPI;
import dev.zyklone.netwatch.core.NetWatchConfig;
import dev.zyklone.netwatch.core.NetWatchResponse;
import dev.zyklone.netwatch.core.NetWatchSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetWatchPlugin extends JavaPlugin {
    static final Component header = Component
            .text()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("NetWatch", NamedTextColor.RED))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .build();

    private static NetWatchAPI api = null;
    private static NetWatchPlugin instance;

    public static NetWatchAPI getApi() {
        return api;
    }

    public static NetWatchPlugin getInstance() {
        return instance;
    }

    private MiniMessage mm;
    private NetWatchConfig config;

    public NetWatchConfig config() {
        return config;
    }

    @Override
    public void onLoad() {
        NetWatchPlugin.instance = this;
        this.saveResource("config.jsonc", false);

        Server server = Bukkit.getServer();
        if (server.getServerConfig().isProxyEnabled()) {
            this.getSLF4JLogger().warn("WARNING: Proxy is enabled, it's recommended only install NetWatch on proxy server");
        } else {
            if (!server.getOnlineMode()) {
                this.getSLF4JLogger().warn("WARNING: Server is running in offline mode, please only use sources with offline mode data");
            }
        }

        try {
            JsonReader reader = new JsonReader(Files.newBufferedReader(this.getDataPath().resolve("config.jsonc")));
            reader.setStrictness(Strictness.LENIENT);
            this.config = new Gson().fromJson(reader, NetWatchConfig.class);
        } catch (Exception e) {
            this.getSLF4JLogger().warn("Error loading configurations", e);
            return;
        }

        ExecutorService exec = Executors.newCachedThreadPool();

        // create sources
        List<NetWatchSource> sources = new ArrayList<>();
        for (NetWatchConfig.Source source : this.config.sources()) {
            try {
                sources.add(NetWatchSource.create(source));
            } catch (Exception e) {
                this.getSLF4JLogger().warn("Error loading source {}: {}", source.base(), e.toString());
            }
        }
        sources = NetWatchAPI.check(exec, sources, (s, e) ->
                this.getSLF4JLogger().warn("Error checking source {}: {}", s.getBase(), String.valueOf(e)),
                this.config.timeout()); // check sources connection and authorization
        if (sources.isEmpty())
            this.getSLF4JLogger().warn("No source available");

        // create API instance
        NetWatchConfig.Cache cache = this.config.cache();
        NetWatchPlugin.api = new NetWatchAPI(
                exec,
                cache.getCacheMax(), cache.getCacheExpire(),
                sources
        );
    }

    @Override
    public void onEnable() {
        if (NetWatchPlugin.api == null) {
            throw new RuntimeException("Loading failed");
        }

        if (NetWatchPlugin.api.available()) {
            this.mm = MiniMessage.miniMessage();
            Listener listener = this.config.async() ? new JoinListener() : new LoginListener();
            Bukkit.getPluginManager().registerEvents(listener, this);
            Bukkit.getCommandMap().register("netwatch", new SubmitCommand());
        } else {
            this.getSLF4JLogger().warn("No source available, please check your configuration");
        }
    }

    public boolean check(UUID player) {
        return NetWatchPlugin.api.query(player, this.config.timeout()).parallelStream()
                .anyMatch(NetWatchResponse::valid);
    }

    public void checkAsync(UUID player) {
        NetWatchPlugin.api.getExecutor().execute(() -> {
            boolean result = this.check(player);
            if (result) {
                Bukkit.getGlobalRegionScheduler().execute(this, () -> {
                    Player ent = Bukkit.getPlayer(player);
                    if (ent != null) {  // kick online player
                        ent.getScheduler().execute(this, () ->
                                ent.kick(kickMessage(), PlayerKickEvent.Cause.BANNED), null, 0);
                    }
                });
            }
        });
    }

    public Component kickMessage() {
        return Component.join(
                JoinConfiguration.builder()
                        .separator(Component.newline())
                        .build(),
                this.config.message().getKick().parallelStream()
                        .map(mm::deserialize)
                        .toList());
    }
}
