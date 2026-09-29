# Read-path cache benchmark

`read-cache-bench.sh` starts the app jar on a fresh SQLite db, seeds 10 users / 500 articles
(10 tags) through the REST API, runs HTTP smoke checks that writes are visible through the
cache (update, create, delete), then drives each read endpoint with `wrk`.

```
./gradlew bootJar
bench/read-cache-bench.sh build/libs/*.jar cache-off --realworld.cache.enabled=false
bench/read-cache-bench.sh build/libs/*.jar cache-on
```

## Results

8 vCPU VM, OpenJDK 17, `wrk -t4 -c32 -d20s`, app and load generator on the same host.
`master` is the jar built from the default branch; `cache-off` is this branch with
`realworld.cache.enabled=false`. Numbers are requests/sec (p50 / p99 latency).

| endpoint | master | cache-off | cache-on | cache-on vs master |
|---|---|---|---|---|
| `GET /tags` | 60,644 (441us / 2.91ms) | 61,171 (430us / 3.04ms) | 87,472 (293us / 2.40ms) | 1.4x |
| `GET /articles/{slug}` anonymous | 28,159 (1.08ms / 4.73ms) | 27,080 (1.11ms / 5.22ms) | 77,407 (341us / 2.57ms) | 2.7x |
| `GET /articles/{slug}` authenticated | 9,950 (2.66ms / 19.99ms) | 10,134 (2.58ms / 18.67ms) | 12,760 (2.06ms / 15.81ms) | 1.3x |
| `GET /articles` anonymous | 1,159 (26.43ms / 60.64ms) | 1,144 (26.59ms / 62.49ms) | 23,000 (1.30ms / 6.04ms) | 19.9x |
| `GET /articles` authenticated | 1,079 (28.62ms / 57.83ms) | 1,009 (30.44ms / 62.51ms) | 8,239 (3.24ms / 21.82ms) | 7.6x |
| `GET /articles?tag=` anonymous | 1,350 (22.59ms / 52.52ms) | 1,171 (25.94ms / 63.01ms) | 23,155 (1.29ms / 5.82ms) | 17.2x |

Authenticated requests still pay for the JWT user lookup and the per-viewer
`favorited` / `favoritesCount` / `following` queries, which are intentionally not cached.
