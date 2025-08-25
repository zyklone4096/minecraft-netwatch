package dev.zyklone.netwatch.velocity;

import com.google.gson.Gson;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.zyklone.netwatch.core.NetWatchAPI;
import dev.zyklone.netwatch.core.NetWatchConfig;
import dev.zyklone.netwatch.core.NetWatchResponse;
import dev.zyklone.netwatch.core.NetWatchSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Plugin(
        id = "netwatch",
        name = "NetWatch",
        version = "1.0.0",
        authors = {"Zyklone"},
        description = "NetWatch cloud ban system"
)
public class NetWatchPlugin {
    static final Component header = Component
            .text()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("NetWatch", NamedTextColor.RED))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .build();

    private static NetWatchPlugin instance;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDir;
    private final NetWatchConfig config;
    private final NetWatchAPI api;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public NetWatchPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDir) {
        instance = this;
        this.server = server;
        this.logger = logger;
        this.dataDir = dataDir;

        try {
            if (!Files.exists(dataDir))
                Files.createDirectories(dataDir);

            // save default config
            Path cfgPath = dataDir.resolve("config.jsonc");
            if (Files.notExists(cfgPath)) {
                try (InputStream is = Objects.requireNonNull(NetWatchPlugin.class.getResourceAsStream("/config.jsonc"))) {
                    Files.copy(is, cfgPath);
                }
            }

            // load config
            JsonReader jr = new JsonReader(Files.newBufferedReader(cfgPath));
            jr.setStrictness(Strictness.LENIENT);
            this.config = new Gson().fromJson(jr, NetWatchConfig.class);

            ExecutorService exec = Executors.newCachedThreadPool();

            // create sources
            List<NetWatchSource> sources = new ArrayList<>();
            for (NetWatchConfig.Source source : this.config.sources()) {
                try {
                    sources.add(NetWatchSource.create(source));
                } catch (Exception e) {
                    this.logger.warn("Error loading source {}: {}", source.base(), e.toString());
                }
            }
            sources = NetWatchAPI.check(exec, sources, (s, e) ->
                            this.logger.warn("Error checking source {}: {}", s.getBase(), String.valueOf(e)),
                    this.config.timeout()); // check sources connection and authorization
            if (sources.isEmpty()) {
                this.logger.warn("No source available");
            } else {
                this.logger.info("{} sources available", sources.size());
            }

            // create API instance
            NetWatchConfig.Cache cache = this.config.cache();
            this.api = new NetWatchAPI(
                    exec,
                    cache.getCacheMax(), cache.getCacheExpire(),
                    sources
            );
        } catch (Exception e) {
            throw new RuntimeException("Loading failed", e);
        }


    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // register events and command
        if (this.api.available()) {
            this.logger.info("Async handler: {}", this.config.async());
            this.server.getEventManager().register(this, this.config.async() ? new AsyncListener() : new BlockingListener());
            CommandManager cm = this.server.getCommandManager();
            cm.register(cm.metaBuilder("netwatch-submit").build(), new SubmitCommand());
        }
    }

    public boolean check(UUID player) {
        return this.api.query(player, this.config.timeout())
                .parallelStream()
                .anyMatch(NetWatchResponse::valid);
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

    public static NetWatchPlugin getInstance() {
        return instance;
    }

    public NetWatchAPI getApi() {
        return api;
    }

    public NetWatchConfig getConfig() {
        return config;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}
