import dev.zyklone.netwatch.core.NetWatchAPI;
import dev.zyklone.netwatch.core.NetWatchSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestAPI {
    @Test
    public void test() throws Exception {
        NetWatchSource src = NetWatchSource.create("http://127.0.0.1:8080/", 1, "114514", false, true);
        ExecutorService exec = Executors.newCachedThreadPool();
        NetWatchAPI api = new NetWatchAPI(
                exec,
                10, 600,
                NetWatchAPI.check(exec, List.of(src), (s, e) -> e.printStackTrace(), 0)
        );
        System.out.println(api.isBanned(UUID.fromString("d25ef921-b014-4170-9bce-90839c943a99"), 0));
    }
}
