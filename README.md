# collateral-service (CLT-COLLATERAL-SVC)

Collateral management service for the Securities Division: CSA agreements, haircut-adjusted
exposure and the daily margin-call cycle.

| | |
|---|---|
| Runtime | Java 17, Spring Boot 4.0 |
| Build | Maven (`mvn clean verify`) |
| Pipeline | GS Managed Pipelines **v2** (`.gs/pipeline.yml`) |
| Auth | GS Auth (GS SSO JWT, entitlements -> roles); legacy desk tokens disabled (`collateral.auth.legacy-desk-token.enabled`, removed next release) |
| CLM | Latest scan report in `.gs/clm-report.json` |

## Run locally

```bash
mvn spring-boot:run
# health
curl localhost:8080/collateral/actuator/health
# obtain a GS SSO JWT with the CLT_OPS entitlement (see ./scripts/gs-token.sh) and run the margin cycle
TOKEN=$(./scripts/gs-token.sh CLT_OPS)
curl -X POST -H "Authorization: Bearer $TOKEN" localhost:8080/collateral/api/margin-calls/run
```

Entitlement -> role mapping (`gs.auth.entitlement-roles`): `CLT_OPS` -> OPS, `CLT_RISK_RO` -> RISK,
`TSY_RO` -> TREASURY. Outside the GS network there is no issuer to call; run the tests, which mock the
`JwtDecoder`, or set `collateral.auth.legacy-desk-token.enabled=true` to fall back to desk tokens for
one more release.

Swagger UI: `http://localhost:8080/collateral/swagger-ui.html`

## Branching

`main` is protected; feature work happens on `feature/CLT-<ticket>-<slug>` branches and lands via PR.
In the COG-GTM demo copy this service lives on the `gs/main` integration branch of
`COG-GTM/spring-boot-realworld-example-app` (alongside unrelated projects), so PRs target `gs/main`
once the managed pipeline is green. Platform changes (JDK, framework, pipeline, auth) are owned by
the collateral-platform team and are sequenced so feature branches are not disrupted.
