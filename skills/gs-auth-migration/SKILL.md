---
name: gs-auth-migration
description: Replace a service's legacy shared-secret / desk-token authentication with GS Auth (GS SSO OIDC JWTs validated as a Spring Security OAuth2 resource server, entitlements claim mapped to roles), keeping the legacy path behind a kill-switch for one release. Requires Spring Boot 3.x / Spring Security 6+.
---

# Move to GS Auth

GS Auth = bearer JWTs issued by GS SSO (OIDC). Services validate them as an OAuth2 **resource
server** and derive roles from the `gs_entitlements` claim. Inside GS this comes from
`com.gs.auth:gs-auth-spring-boot-starter`; outside the GS network use the equivalent open-source
configuration below (`spring-boot-starter-oauth2-resource-server`).

Precondition: Spring Boot 3.x (Security 6). On Boot 2.x stop and report `blocked`.

## Footprint
`pom.xml` (add resource-server starter), `src/main/java/**/security/**`, `SecurityConfig`,
`application*.yml`, `src/test/**` for auth tests, `scripts/` token helper, `README.md` auth row.

## Steps
1. Inventory the legacy auth: filter class, header name, secret property, role mapping. Note which
   endpoints need which roles — the new config must keep the exact same authorization matrix.
2. Create branch `platform/05-gs-auth`.
3. `pom.xml`: add `spring-boot-starter-oauth2-resource-server`.
4. Properties (`application.yml`):
   ```yaml
   gs:
     auth:
       issuer-uri: ${GS_SSO_ISSUER:https://sso.gs.com/realms/firmwide}
       audience: <appId, e.g. CLT-COLLATERAL-SVC>
       entitlements-claim: gs_entitlements
       entitlement-roles:            # entitlement -> role
         CLT_OPS: OPS
         CLT_RISK_RO: RISK
         TSY_RO: TREASURY
   spring.security.oauth2.resourceserver.jwt.issuer-uri: ${gs.auth.issuer-uri}
   collateral.auth.legacy-desk-token.enabled: false   # kill-switch, remove next release
   ```
5. Code:
   - `GsAuthProperties` (`@ConfigurationProperties("gs.auth")`).
   - `GsEntitlementsConverter implements Converter<Jwt, AbstractAuthenticationToken>`: maps each
     entitlement in the claim to `ROLE_<role>` via `entitlement-roles`; unknown entitlements ignored;
     principal name = `sub`.
   - `SecurityConfig`: `oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))`,
     add an audience validator (`JwtValidators.createDefaultWithIssuer` + `JwtClaimValidator<List<String>>("aud", ...)`).
   - Keep the legacy filter registered **only** when `collateral.auth.legacy-desk-token.enabled=true`
     (`@ConditionalOnProperty`). Default false. Delete nothing yet — the removal is a follow-up PR.
6. Tests: use `spring-security-test` `SecurityMockMvcRequestPostProcessors.jwt()` with
   `.claim("gs_entitlements", List.of("CLT_OPS"))` for each role; assert the authorization matrix
   (200/403/401) is identical to the legacy tests. For the resource server bean in tests, provide a
   `JwtDecoder` mock/`@MockBean` so no network call to the issuer is made. Keep legacy tests but run
   them with the kill-switch on via `@TestPropertySource`.
7. Replace `scripts/desk-token.sh` with `scripts/gs-token.sh` that prints how to obtain a dev JWT
   (`gs-sso login --app <appId>` inside GS; outside, document the mock decoder).
8. Validate: `mvn -B -ntp clean verify`; boot the app with a dummy issuer and check
   `/actuator/health` is 200 and `/api/...` without a token is 401.
9. README auth row -> `GS Auth (GS SSO JWT, entitlements -> roles); legacy desk tokens disabled`.
   Commit `PLAT-UPG feature: migrate to GS Auth` and open PR `[platform 5/N] GS Auth`, listing the
   entitlement -> role table and the kill-switch property.

## Done when
Resource-server auth works with identical authorization matrix, legacy path is flag-guarded and off,
tests cover both, build green, PR open.
