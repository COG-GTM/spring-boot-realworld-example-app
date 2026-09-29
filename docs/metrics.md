# Metrics

The application exposes [Micrometer](https://micrometer.io) metrics in the Prometheus text format via Spring Boot Actuator.

| Endpoint | Auth | Purpose |
| --- | --- | --- |
| `GET /actuator/prometheus` | none | Prometheus scrape endpoint |
| `GET /actuator/health` | none | Liveness/readiness check |

No other actuator endpoints are exposed over HTTP (`management.endpoints.web.exposure.include=health,prometheus`).

Every metric carries a common tag `application="realworld"`.

Quick check:

    ./gradlew bootRun
    curl http://localhost:8080/tags
    curl -s http://localhost:8080/actuator/prometheus | grep -E '^(http_server_requests|mybatis_)'

## Request latency: `http_server_requests_seconds`

Provided by Spring Boot's built-in Spring MVC instrumentation. One timer sample is recorded per HTTP request handled by the `DispatcherServlet` (REST endpoints and `/graphql`), measuring wall-clock time from the request entering Spring MVC until the response is completed, including controller, service and database work.

| Series | Type | Meaning |
| --- | --- | --- |
| `http_server_requests_seconds_count` | counter | Number of requests handled |
| `http_server_requests_seconds_sum` | counter | Total time spent handling those requests (seconds) |
| `http_server_requests_seconds_max` | gauge | Slowest request observed in the current decay window (~2 min) |
| `http_server_requests_seconds_bucket{le=...}` | counter | Cumulative histogram buckets for latency quantiles/SLOs |

Tags:

- `method` – HTTP method (`GET`, `POST`, ...).
- `uri` – the matched route template, e.g. `/articles/{slug}` (not the raw path, so cardinality stays bounded). Requests that never reach a handler are reported as `root`, `NOT_FOUND` or `REDIRECTION`. All GraphQL operations share `uri="/graphql"`.
- `status` – HTTP status code.
- `outcome` – `SUCCESS`, `CLIENT_ERROR`, `SERVER_ERROR`, `REDIRECTION`, `INFORMATIONAL` or `UNKNOWN`.
- `exception` – simple class name of an unhandled exception, or `None`.

Configuration (`application.properties`):

- `management.metrics.distribution.percentiles-histogram.http.server.requests=true` publishes histogram buckets so quantiles can be aggregated across instances with `histogram_quantile()`.
- `management.metrics.distribution.slo.http.server.requests=50ms,100ms,250ms,500ms,1s` guarantees exact buckets at those thresholds for SLO alerting.

Note: requests rejected by Spring Security before reaching a handler (e.g. `401` on a protected route) are still timed, but are tagged `uri="root"`.

Example queries:

    # p95 latency per route over 5 minutes
    histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket[5m])))

    # fraction of requests served within 250 ms
    sum(rate(http_server_requests_seconds_bucket{le="0.25"}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))

## MyBatis query counts: `mybatis_queries_total`

Recorded by `io.spring.infrastructure.metrics.MyBatisMetricsInterceptor`, a MyBatis plugin on `Executor.query` / `Executor.update`. It increments once per mapped-statement invocation (every call to a mapper method such as `ArticleMapper.findById` or `ArticleReadService.queryArticles`).

| Series | Type | Meaning |
| --- | --- | --- |
| `mybatis_queries_total` | counter | Number of mapped statements executed |

Tags:

- `statement` – fully qualified mapped statement id (`<mapper interface>.<method>`), e.g. `io.spring.infrastructure.mybatis.readservice.TagReadService.all`. Cardinality is bounded by the number of mapper methods.
- `command` – `SELECT`, `INSERT`, `UPDATE`, `DELETE` (or `FLUSH`/`UNKNOWN`).
- `outcome` – `SUCCESS`, or `ERROR` if the statement threw.

The count is taken at the executor boundary, so it counts statements *issued by the application*. A `SELECT` answered from MyBatis' session-local (first-level) cache inside a single transaction is still counted. That makes the metric suitable for spotting N+1 patterns, but it can slightly exceed the number of JDBC round trips. No mapper declares a second-level `<cache/>`, so there is no cross-session caching to account for.

## MyBatis query latency: `mybatis_query_duration_seconds`

Recorded by the same interceptor with the same tags, measuring the time spent inside the MyBatis executor for each statement (SQL execution plus result mapping).

| Series | Type | Meaning |
| --- | --- | --- |
| `mybatis_query_duration_seconds_count` | counter | Number of timed statements (equals `mybatis_queries_total`) |
| `mybatis_query_duration_seconds_sum` | counter | Total time spent executing statements (seconds) |
| `mybatis_query_duration_seconds_max` | gauge | Slowest statement in the current decay window |
| `mybatis_query_duration_seconds_bucket{le=...}` | counter | Buckets at 5, 10, 25, 50, 100, 250 ms (`management.metrics.distribution.slo.mybatis.query.duration`) |

Example queries:

    # statements per second by statement id
    sum by (statement) (rate(mybatis_queries_total[5m]))

    # average DB statements per HTTP request (N+1 indicator)
    sum(rate(mybatis_queries_total[5m])) / sum(rate(http_server_requests_seconds_count{uri!~"/actuator.*"}[5m]))

    # mean statement latency
    sum by (statement) (rate(mybatis_query_duration_seconds_sum[5m])) / sum by (statement) (rate(mybatis_query_duration_seconds_count[5m]))

## Other metrics

Adding Actuator also enables Spring Boot's default binders (JVM memory/GC/threads, process CPU, Hikari connection pool `hikaricp_*`, Tomcat sessions, logback events). They are exposed on the same endpoint but not covered here.
