package dev.zyklone.netwatch.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;

public class SubmitCommand implements SimpleCommand {
    private final Component msgCompleted = Component.text()
            .append(NetWatchPlugin.header)
            .append(Component.text("Submitted to source"))
            .build();
    private final Component msgError = Component.text()
            .append(NetWatchPlugin.header)
            .append(Component.text("Unable to submit to source"))
            .build();
    private final Component msgUsage = Component.text()
            .append(NetWatchPlugin.header)
            .append(Component.text("Usage: netwatch-submit [player|uuid] (reason)"))
            .build();
    private final Component msgNotFound = Component.text()
            .append(NetWatchPlugin.header)
            .append(Component.text("Target not found"))
            .build();

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();

        if (args.length < 1) {
            sender.sendMessage(this.msgUsage);
        } else {
            // find target player
            UUID target;
            Optional<Player> player = NetWatchPlugin.getInstance().getServer().getPlayer(args[0]);
            if (player.isEmpty()) {   // parse UUID
                try {
                    target = UUID.fromString(args[0]);
                } catch (Exception e) { // invalid UUID
                    sender.sendMessage(this.msgNotFound);
                    return;
                }
            } else {
                target = player.get().getUniqueId();  // use UUID from player
            }

            // build reason
            String reason;
            if (args.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (!sb.isEmpty())
                        sb.append(' ');
                    sb.append(args[i]);
                }
                reason = sb.toString();
            } else {
                reason = null;
            }

            // submit
            NetWatchPlugin.getInstance().getApi().submitAsync(target, reason,
                    (r, c) -> {
                        if (c == 200) {
                            sender.sendMessage(Component.text().append(this.msgCompleted).append(Component.text(" " + r.getBase())).build());
                        } else {
                            sender.sendMessage(Component.text().append(this.msgError).append(Component.text(String.format(" %s (%s)", r.getBase(), c))));
                        }
                    },
                    (r, e) -> sender.sendMessage(Component.text().append(this.msgError).append(Component.text(String.format(" %s (%s)", r.getBase(), e.toString())))));
        }
    }
}
