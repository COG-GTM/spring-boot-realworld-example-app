package io.spring.application.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Caffeine cache whose puts are rejected if any invalidation happened while the value was being
 * loaded. A loader that read a row just before a concurrent write committed therefore can never
 * re-populate the cache with that stale row after the write's invalidation ran.
 */
public class GuardedCache<K, V> {
  private final Cache<K, V> cache;
  private volatile long generation;

  public GuardedCache(boolean enabled, long maximumSize, Duration ttl) {
    this.cache =
        enabled
            ? Caffeine.newBuilder().maximumSize(maximumSize).expireAfterWrite(ttl).build()
            : null;
  }

  public V get(K key, Supplier<V> loader) {
    V cached = getIfPresent(key);
    if (cached != null) {
      return cached;
    }
    long loadGeneration = generation();
    V loaded = loader.get();
    if (loaded != null) {
      putIfUnchanged(loadGeneration, key, loaded);
    }
    return loaded;
  }

  public V getIfPresent(K key) {
    return cache == null ? null : cache.getIfPresent(key);
  }

  public Map<K, V> getAllPresent(Iterable<K> keys) {
    return cache == null ? Collections.emptyMap() : cache.getAllPresent(keys);
  }

  public long generation() {
    return generation;
  }

  public synchronized void putIfUnchanged(long loadGeneration, K key, V value) {
    if (cache != null && loadGeneration == generation) {
      cache.put(key, value);
    }
  }

  public synchronized void putAllIfUnchanged(long loadGeneration, Map<K, V> values) {
    if (cache != null && loadGeneration == generation) {
      cache.putAll(values);
    }
  }

  public synchronized void invalidate(Collection<K> keys) {
    generation++;
    if (cache != null) {
      cache.invalidateAll(keys);
    }
  }

  public synchronized void invalidateIf(Predicate<V> predicate) {
    generation++;
    if (cache != null) {
      cache.asMap().values().removeIf(predicate);
    }
  }

  public synchronized void invalidateKeysIf(Predicate<K> predicate) {
    generation++;
    if (cache != null) {
      cache.asMap().keySet().removeIf(predicate);
    }
  }

  public synchronized void invalidateAll() {
    generation++;
    if (cache != null) {
      cache.invalidateAll();
    }
  }
}
