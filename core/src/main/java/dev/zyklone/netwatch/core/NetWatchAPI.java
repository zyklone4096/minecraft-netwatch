package dev.zyklone.netwatch.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class NetWatchAPI implements Closeable {
    private final ExecutorService exec;
    private final Gson gson = new Gson();
    private final HttpClient hc = HttpClient.newHttpClient();
    private final Cache<@NotNull UUID, List<NetWatchResponse>> cache;
    private final List<NetWatchSource> sources;

    /**
     * Create new API instance
     * @param executor thread pool
     * @param cacheMax max cache size
     * @param cacheExpire cache expiration (seconds)
     * @param sources sources list
     */
    public NetWatchAPI(
            ExecutorService executor,
            int cacheMax, long cacheExpire,
            List<NetWatchSource> sources
    ) {
        this.exec = executor;
        this.cache = Caffeine.newBuilder()
                .maximumSize(cacheMax)
                .expireAfterWrite(cacheExpire, TimeUnit.SECONDS)
                .build();
        this.sources = sources;
    }

    /**
     * Get ExecutorService object passed in constructor
     * @return executor
     */
    public ExecutorService getExecutor() {
        return exec;
    }

    /**
     * returns false if no source configured
     * @return result
     */
    public boolean available() {
        return !this.sources.isEmpty();
    }

    /**
     * Check sources
     * @param executor thread pool
     * @param sources sources list
     * @param errorHandler error handler, internal error occurred if source is null
     * @param timeout timeout millis
     * @return passed sources
     */
    public static List<NetWatchSource> check(ExecutorService executor, List<NetWatchSource> sources, BiConsumer<NetWatchSource, Exception> errorHandler, long timeout) {
        HttpClient hc = HttpClient.newHttpClient();
        List<CompletableFuture<NetWatchSource>> futures = sources.parallelStream()  // check all sources async
                .map(it -> CompletableFuture.supplyAsync(() -> {
                    try {
                        if (!it.isCheck())
                            return it;    // no check needed

                        HttpResponse<String> resp = hc.send(it.check(), HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() == 200)
                            return it;
                        else {
                            errorHandler.accept(it, new RuntimeException("Remote returns " + resp.statusCode()));   // bad return code
                            return null;
                        }
                    } catch (Exception e) {
                        errorHandler.accept(it, e);
                        return null;
                    }
                }, executor))
                .toList();

        // wait for results
        try {
            CompletableFuture<Void> f = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            if (timeout > 0)
                f.get(timeout, TimeUnit.MILLISECONDS);
            else f.get();
        } catch (Exception e) {
            errorHandler.accept(null, e);
        }
        hc.close(); // shutdown http client

        return futures.parallelStream()
                .filter(it -> it.isDone() && !it.isCompletedExceptionally())    // filter completed
                .map(it -> it.getNow(null)) // map results
                .filter(Objects::nonNull)   // exclude nulls
                .toList();
    }

    /**
     * Query ban records with blocking
     * @param uuid target player UUID
     * @param timeout timeout millis
     * @return results list
     */
    public List<NetWatchResponse> query(UUID uuid, long timeout) {
        List<CompletableFuture<NetWatchResponse>> comp = queryAsync(uuid);
        try {   // wait for all futures
            CompletableFuture<Void> f = CompletableFuture.allOf(comp.toArray(new CompletableFuture[0]));
            if (timeout > 0) {
                f.get(timeout, TimeUnit.MILLISECONDS);
            } else f.get();
        } catch (Exception ignored) {}

        List<NetWatchResponse> results = comp.parallelStream()
                .filter(it -> !it.isCompletedExceptionally())    // filter completed
                .map(it -> it.getNow(null)) // map results
                .filter(Objects::nonNull)   // exclude nulls
                .toList();
        this.cache.put(uuid, results);
        return results;
    }

    /**
     * Query valid bans
     * @param uuid target player UUID
     * @param timeout timeout millis
     * @return true if banned
     */
    public boolean isBanned(UUID uuid, long timeout) {
        return this.query(uuid, timeout)
                .parallelStream()
                .anyMatch(NetWatchResponse::valid);
    }

    /**
     * Query all sources async
     * @param uuid target player UUID
     * @return futures list
     */
    public List<CompletableFuture<NetWatchResponse>> queryAsync(UUID uuid)  {
        List<NetWatchResponse> resp = cache.getIfPresent(uuid);
        if (resp == null) {
            return sources.parallelStream()
                    .map(src ->
                                    CompletableFuture.supplyAsync(() -> {
                                        try {
                                            return query0(src, uuid);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }, exec)
                            ).toList();
        }
        return resp.parallelStream().map(CompletableFuture::completedFuture).toList();  // map cache
    }

    private NetWatchResponse query0(NetWatchSource source, UUID uuid) throws IOException, URISyntaxException, InterruptedException {
        URI uri = source.queryUri(uuid);
        HttpRequest.Builder req = HttpRequest.newBuilder(uri);
        String auth = source.getAuthorization();
        if (auth != null)
            req.header("Authorization", auth);
        HttpResponse<String> resp = this.hc.send(req.build(), HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200)
            return null;    // failed or not found

        // decode response
        JsonObject json = this.gson.fromJson(resp.body(), JsonObject.class);
        try {
            int count = json.get("count").getAsInt();
            return new NetWatchResponse(uuid, count, source);
        } catch (NullPointerException npe) {    // invalid response
            return null;
        }
    }

    /**
     * Submit ban records
     * @param uuid target player
     * @param reason reason
     * @param resultHandler result handler, only used if request sent
     * @param errorHandler error handler, only used if internal error occurred
     */
    public void submitAsync(UUID uuid, String reason, BiConsumer<NetWatchSource, Integer> resultHandler, BiConsumer<NetWatchSource, Exception> errorHandler) {
        for (NetWatchSource source : this.sources) {
            if (source.isSubmit()) {    // filter sources to submit
                this.exec.execute(() -> {
                    try {
                        int result = this.submit0(source, uuid, reason);
                        if (result != 0)
                            resultHandler.accept(source, result);
                    } catch (Exception e) {
                        errorHandler.accept(source, e);
                    }
                });
            }
        }
    }

    /**
     * Submit ban records
     * @param uuid target player
     * @param reason reason
     * @return futures for all sources
     */
    public List<CompletableFuture<Integer>> submitAsync(UUID uuid, String reason) {
        List<CompletableFuture<Integer>> results = this.sources.parallelStream()
                .filter(NetWatchSource::isSubmit)   // filter source to submit
                .map(it -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return submit0(it, uuid, reason);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, this.exec))
                .toList();
        this.cache.invalidate(uuid);    // invalidate cache after submit
        return results;
    }

    private int submit0(NetWatchSource source, UUID uuid, String reason) throws IOException, InterruptedException, URISyntaxException {
        HttpRequest req = source.submit(this.gson, uuid, reason);
        if (req == null) {
            return 0;
        }

        return this.hc.send(req, HttpResponse.BodyHandlers.ofByteArray()).statusCode();
    }

    /**
     * Closes thread pool and HTTP client
     */
    @Override
    public void close() {
        this.exec.shutdown();
        this.hc.close();
    }
}
