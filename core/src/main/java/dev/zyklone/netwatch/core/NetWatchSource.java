package dev.zyklone.netwatch.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class NetWatchSource {
    private final String base;
    private final int threshold;
    private final String authorization;
    private final boolean submit;
    private final boolean check;

    private NetWatchSource(String base, int threshold, String api, boolean submit, boolean check) {
        this.base = base;
        this.threshold = threshold;
        this.authorization = api == null ? null : "Bearer " + api;
        this.submit = submit;
        this.check = check;
    }

    public String getBase() {
        return base;
    }

    public int getThreshold() {
        return threshold;
    }

    public String getAuthorization() {
        return authorization;
    }

    public boolean isCheck() {
        return check;
    }

    public boolean isSubmit() {
        return submit;
    }

    /**
     * Create a request for check if this source is available
     * @return request
     * @throws URISyntaxException URI constructor error
     */
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

    /**
     * Create a request for submit ban record
     * @param gson json serializer
     * @param uuid target player
     * @param reason reason
     * @return request, null if disable for submit or no credential configured
     * @throws URISyntaxException URI constructor error
     */
    public HttpRequest submit(Gson gson, UUID uuid, String reason) throws URISyntaxException {
        if (!this.submit || this.authorization == null) {
            return null;
        }

        JsonObject body = new JsonObject();
        body.addProperty("id", uuid.toString());
        body.addProperty("reason", reason);

        return HttpRequest.newBuilder(new URI("%s/submit".formatted(this.base)))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .header("Authorization", this.authorization)
                .header("Content-Type", "application/json")
                .build();

    }

    public static NetWatchSource create(String base, int threshold, String api, boolean submit, boolean check) throws URISyntaxException {
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        new URI(base);  // URI syntax check
        return new NetWatchSource(
                base, threshold, api, submit, check
        );
    }

    public static NetWatchSource create(NetWatchConfig.Source config) throws URISyntaxException {
        return create(config.base(), config.threshold(), config.apiKey(), config.submit(), config.check());
    }
}
