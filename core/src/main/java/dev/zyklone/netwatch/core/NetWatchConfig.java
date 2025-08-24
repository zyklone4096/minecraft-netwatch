package dev.zyklone.netwatch.core;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public final class NetWatchConfig {
    private List<Source> sources = new ArrayList<>();
    private Cache cache = new Cache();
    private Message message = new Message();

    private boolean async = true;
    private long timeout = 1000*10;

    public List<Source> sources() {
        return sources;
    }

    public Cache cache() {
        return cache;
    }

    /**
     * true if query without blocking server thread
     * <br>
     * for Paper, it registers a PlayerJoinEvent listener and kicks player after join, otherwise registers an AsyncPlayerPreLoginEvent and kicks player before join
     * @return async
     */
    public boolean async() {
        return async;
    }

    /**
     * wait timeout for all query/check requests
     * @return timeouts
     */
    public long timeout() {
        return timeout;
    }

    public Message message() {
        return message;
    }

    public static final class Source {
        private String base;
        private int threshold;
        private String apiKey;
        private boolean check = true;
        private boolean submit = false;

        /**
         * Check source before actual use
         * @return check
         */
        public boolean check() {
            return check;
        }

        /**
         * Base URL for this source
         * @return url
         */
        public String base() {
            return base;
        }

        /**
         * Minimum ban records from this source for kick
         * @return threshold
         */
        public int threshold() {
            return threshold;
        }

        /**
         * API key for requests
         * @return key
         */
        public String apiKey() {
            return apiKey;
        }

        /**
         * Enable this source for submitting
         * @return submit
         */
        public boolean submit() {
            return submit;
        }

        @Override
        public String toString() {
            return "Source[" +
                    "base=" + base + ", " +
                    "threshold=" + threshold + ", " +
                    "apiKey=" + apiKey + ']';
        }
    }

    public static class Cache {
        /**
         * Max cache size
         */
        private int cacheMax = 50;

        /**
         * Cache expiration time (seconds)
         */
        private long cacheExpire = 60*10;

        public int getCacheMax() {
            return cacheMax;
        }

        public long getCacheExpire() {
            return cacheExpire;
        }
    }

    public static class Message {
        /**
         * Kick messages
         */
        private List<String> kick = List.of("<red>Banned by NetWatch</red>");

        public List<String> getKick() {
            return kick;
        }
    }
}
