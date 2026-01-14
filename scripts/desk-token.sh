#!/usr/bin/env bash
# Issues a legacy desk token for local testing: desk-token.sh <DESK> [secret]
set -euo pipefail
desk="${1:?desk id required}"
secret="${2:-${DESK_TOKEN_SECRET:-local-dev-only-secret}}"
now=$(date +%s)
sig=$(printf '%s' "$desk:$now" | openssl dgst -sha256 -hmac "$secret" | sed 's/^.* //')
echo "$desk:$now:$sig"
