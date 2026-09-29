package io.spring.application.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class GuardedCacheTest {
  private final GuardedCache<String, String> cache =
      new GuardedCache<>(true, 100, Duration.ofMinutes(1));

  @Test
  public void should_load_once_and_serve_from_cache() {
    AtomicInteger loads = new AtomicInteger();
    Supplier<String> loader =
        () -> {
          loads.incrementAndGet();
          return "v";
        };
    Assertions.assertEquals("v", cache.get("k", loader));
    Assertions.assertEquals("v", cache.get("k", loader));
    Assertions.assertEquals(1, loads.get());
  }

  @Test
  public void should_drop_value_loaded_across_an_invalidation() {
    String loaded =
        cache.get(
            "k",
            () -> {
              cache.invalidate(Collections.singleton("other"));
              return "stale";
            });
    Assertions.assertEquals("stale", loaded);
    Assertions.assertNull(cache.getIfPresent("k"));
  }

  @Test
  public void should_not_cache_null_or_when_disabled() {
    Assertions.assertNull(cache.get("missing", () -> null));
    Assertions.assertNull(cache.getIfPresent("missing"));

    GuardedCache<String, String> disabled = new GuardedCache<>(false, 100, Duration.ofMinutes(1));
    Assertions.assertEquals("v", disabled.get("k", () -> "v"));
    Assertions.assertNull(disabled.getIfPresent("k"));
  }

  @Test
  public void should_evict_again_when_transaction_completes() {
    AtomicInteger evictions = new AtomicInteger();
    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionalEviction.evict(evictions::incrementAndGet);
      Assertions.assertEquals(1, evictions.get());
      for (TransactionSynchronization sync :
          TransactionSynchronizationManager.getSynchronizations()) {
        sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
      }
      Assertions.assertEquals(2, evictions.get());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}
