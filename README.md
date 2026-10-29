# collateral-service (CLT-COLLATERAL-SVC)

Collateral management service for the Securities Division: CSA agreements, haircut-adjusted
exposure and the daily margin-call cycle.

| | |
|---|---|
| Runtime | Java 17, Spring Boot 3.5 |
| Build | Maven (`mvn clean verify`) |
| Pipeline | GS Managed Pipelines **v2** (`.gs/pipeline.yml`) |
| Auth | Legacy desk tokens (`X-Desk-Token`, HMAC shared secret per desk) |
| CLM | Latest scan report in `.gs/clm-report.json` |

## Run locally

```bash
mvn spring-boot:run
# health
curl localhost:8080/collateral/actuator/health
# issue a desk token for the COLL-OPS desk (dev secret) and run the margin cycle
TOKEN=$(./scripts/desk-token.sh COLL-OPS)
curl -X POST -H "X-Desk-Token: $TOKEN" localhost:8080/collateral/api/margin-calls/run
```

Swagger UI: `http://localhost:8080/collateral/swagger-ui.html`

## Branching

`main` is protected; feature work happens on `feature/CLT-<ticket>-<slug>` branches and lands via PR.
In the COG-GTM demo copy this service lives on the `gs/main` integration branch of
`COG-GTM/spring-boot-realworld-example-app` (alongside unrelated projects), so PRs target `gs/main`
once the managed pipeline is green. Platform changes (JDK, framework, pipeline, auth) are owned by
the collateral-platform team and are sequenced so feature branches are not disrupted.
