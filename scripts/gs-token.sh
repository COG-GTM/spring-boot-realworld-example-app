#!/usr/bin/env bash
# Explains how to obtain a GS Auth (GS SSO OIDC) bearer JWT for collateral-service (app id CLT-COLLATERAL-SVC).
# Legacy desk tokens are disabled (collateral.auth.legacy-desk-token.enabled=false).
set -euo pipefail
app_id="${1:-CLT-COLLATERAL-SVC}"
cat <<MSG
GS Auth dev token for ${app_id}

Inside the GS network:
  gs-sso login --app ${app_id}
  TOKEN=\$(gs-sso token --app ${app_id})
  curl -H "Authorization: Bearer \$TOKEN" localhost:8080/collateral/api/agreements/CSA-NORTHWIND-2021-07

The JWT must carry iss=\${GS_SSO_ISSUER:-https://sso.gs.com/realms/firmwide}, aud=${app_id} and a
gs_entitlements claim; entitlements map to roles via gs.auth.entitlement-roles:
  CLT_OPS      -> OPS       (/api/margin-calls/**, /api/agreements/**)
  CLT_RISK_RO  -> RISK      (/api/agreements/**)
  TSY_RO       -> TREASURY  (/api/agreements/**)

Outside the GS network there is no issuer to call. Tests mock the decoder instead: see
src/test/java/com/gs/collateral/api/GsAuthApiTest.java (a mocked JwtDecoder bean plus
SecurityMockMvcRequestPostProcessors.jwt() with .claim("gs_entitlements", List.of("CLT_OPS"))).
To exercise the real bearer path locally, point GS_SSO_ISSUER at a local OIDC provider
(e.g. a Keycloak realm) and mint a token there with the claims above.
MSG
