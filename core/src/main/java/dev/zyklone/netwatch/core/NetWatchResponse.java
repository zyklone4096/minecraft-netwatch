package dev.zyklone.netwatch.core;

import java.util.UUID;

public record NetWatchResponse(
        UUID uuid,
        String name,
        int count,
        NetWatchSource source
) {
    public boolean valid() {
        return this.count >= this.source.getThreshold();
    }
}
