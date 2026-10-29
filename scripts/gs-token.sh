#!/usr/bin/env bash
# Obtains a GS SSO JWT for local testing against collateral-service (app id CLT-COLLATERAL-SVC).
#   gs-token.sh [ENTITLEMENT ...]   e.g. gs-token.sh CLT_OPS
#
# Inside the GS network the token comes from GS SSO via the gs-sso CLI; the entitlements on the
# token are whatever your SSO profile carries, and the service maps them to roles with
# gs.auth.entitlement-roles (CLT_OPS -> OPS, CLT_RISK_RO -> RISK, TSY_RO -> TREASURY).
#
# Outside the GS network there is no issuer to call. Use the MockMvc tests (GsAuthApiTest), which
# provide a mocked JwtDecoder and build tokens with SecurityMockMvcRequestPostProcessors.jwt(), or
# flip the kill-switch collateral.auth.legacy-desk-token.enabled=true to use desk tokens for one
# more release.
set -euo pipefail
app_id="${GS_APP_ID:-CLT-COLLATERAL-SVC}"
if command -v gs-sso >/dev/null 2>&1; then
  exec gs-sso login --app "$app_id" --print-token
fi
cat >&2 <<MSG
gs-sso CLI not found. Inside GS run:
  gs-sso login --app $app_id --print-token
and send the result as 'Authorization: Bearer <jwt>'. Requested entitlements: ${*:-<none>}
Outside GS: run 'mvn test -Dtest=GsAuthApiTest' (mocked JwtDecoder) or enable
collateral.auth.legacy-desk-token.enabled=true for the legacy X-Desk-Token path.
MSG
exit 1
