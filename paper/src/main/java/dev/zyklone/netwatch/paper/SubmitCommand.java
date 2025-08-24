package dev.zyklone.netwatch.paper;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

class SubmitCommand extends Command {
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
    private final Component notJoined = Component.text()
            .append(NetWatchPlugin.header)
            .append(Component.text("You cannot submit a player who has not logged in before"))
            .build();


    SubmitCommand() {
        super("netwatch-submit");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
        if (args.length < 1) {
            sender.sendMessage(this.msgUsage);
        } else {
            // find target player
            UUID target;
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {   // parse UUID
                try {
                    target = UUID.fromString(args[0]);

                    // check if joined before
                    OfflinePlayer op = Bukkit.getOfflinePlayer(target);
                    if (!op.hasPlayedBefore() && !op.isOnline()) {
                        sender.sendMessage(this.notJoined);
                        return true;
                    }
                } catch (Exception e) { // invalid UUID
                    sender.sendMessage(this.msgNotFound);
                    return true;
                }
            } else {
                target = player.getUniqueId();  // use UUID from player
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
            NetWatchPlugin.getApi().submitAsync(target, reason,
                    (r, c) -> {
                if (c == 200) {
                    sender.sendMessage(Component.text().append(this.msgCompleted).append(Component.text(" " + r.getBase())).build());
                } else {
                    sender.sendMessage(Component.text().append(this.msgError).append(Component.text(String.format(" %s (%s)", r.getBase(), c))));
                }
                    },
                    (r, e) -> sender.sendMessage(Component.text().append(this.msgError).append(Component.text(String.format(" %s (%s)", r.getBase(), e.toString())))));
        }

        return true;
    }
}
