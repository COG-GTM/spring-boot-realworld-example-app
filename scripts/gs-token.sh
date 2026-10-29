#!/usr/bin/env bash
# How to obtain a GS Auth (GS SSO OIDC) bearer JWT for CLT-COLLATERAL-SVC: gs-token.sh [appId]
# Legacy desk tokens (desk-token.sh) are disabled; the filter only runs with
# collateral.auth.legacy-desk-token.enabled=true (kill-switch, removed next release).
set -euo pipefail
app="${1:-CLT-COLLATERAL-SVC}"
if command -v gs-sso >/dev/null 2>&1; then
  # Inside GS: log in against GS SSO and print the access token for this app.
  exec gs-sso login --app "$app"
fi
cat <<MSG
gs-sso CLI not found (outside the GS network).

Inside GS:
  gs-sso login --app $app          # prints a bearer JWT; use it as
  curl -H "Authorization: Bearer \$TOKEN" localhost:8080/collateral/api/margin-calls

Outside GS there is no issuer to mint tokens from. Use the test setup instead:
  - MarginCallGsAuthApiTest mocks the JwtDecoder (@MockitoBean) and drives the API with
    SecurityMockMvcRequestPostProcessors.jwt().claim("gs_entitlements", List.of("CLT_OPS"))
  - entitlement -> role mapping lives under gs.auth.entitlement-roles in application.yml
    (CLT_OPS -> OPS, CLT_RISK_RO -> RISK, TSY_RO -> TREASURY)
  - point GS_SSO_ISSUER at a local OIDC provider (e.g. Keycloak) if you need a real token.
MSG
