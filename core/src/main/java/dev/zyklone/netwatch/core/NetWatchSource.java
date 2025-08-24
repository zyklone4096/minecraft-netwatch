package dev.zyklone.netwatch.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@SuppressWarnings("ClassCanBeRecord")
public class NetWatchSource {
    private final String base;
    private final int threshold;
    private final String authorization;

    private NetWatchSource(String base, int threshold, String api) {
        this.base = base;
        this.threshold = threshold;
        this.authorization = api == null ? null : "Bearer " + api;
    }

    public int getThreshold() {
        return threshold;
    }

    public String getAuthorization() {
        return authorization;
    }

    public HttpRequest check() throws URISyntaxException {
        HttpRequest.Builder req = HttpRequest.newBuilder(new URI("%s/check".formatted(this.base)))
                .HEAD();
        if (this.authorization != null)
            req.header("Authorization", this.authorization);
        return req
                .build();
    }

    /**
     * Get URI for query if banned
     * @param target target player UUID
     * @return URI result
     * @throws URISyntaxException invalid URI
     */
    public URI queryUri(UUID target) throws URISyntaxException {
        return new URI("%s/query?uuid=%s".formatted(base, URLEncoder.encode(target.toString(), StandardCharsets.UTF_8)));
    }

    public static NetWatchSource create(String base, int threshold, String api) throws URISyntaxException {
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        new URI(base);  // URI syntax check
        return new NetWatchSource(
                base, threshold, api
        );
    }
}
