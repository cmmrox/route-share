#!/usr/bin/env bash
# Verifies slice 01 — auth unification and mode gates — end to end against the LOCAL stack.
#
# The claim under test is the one that blocked the whole unified app: ONE phone-OTP account, ONE
# token, both namespaces. Everything else here is the refusal path — each state must answer with
# its own code, not a bare 403, and suspension must outrank every driver gate.
#
#   1. Fresh OTP account, no driver profile     -> driver endpoint 403 DRIVER_PROFILE_MISSING
#   2. Same account applies to drive            -> 403 DRIVER_REVIEW_PENDING (status is data, not an error)
#   3. Admin approves                           -> driver endpoint 200 on the SAME token
#   4. Same token, same run                     -> passenger endpoint 200
#   5. Admin deactivates                        -> driver 403 DRIVER_DEACTIVATED, earnings still 200
#   6. Reinstatement request                    -> 200, second one refused
#   7. Admin reinstates                         -> driver endpoint 200 again
#   8. Admin suspends the account               -> both namespaces 403 ACCOUNT_SUSPENDED
#
# Requires the local stack (docker compose) with demo OTP enabled. Never run against production.
#
# Usage: scripts/simulation/verify-mode-gates.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
DRIVER_PHONE="${ROUTESHARE_SIM_GATE_PHONE:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"

PASS=0; FAIL=0
check() { # check <name> <ok-boolean>
  if [ "$2" = "true" ]; then PASS=$((PASS+1)); sim_log "PASS: $1"; else FAIL=$((FAIL+1)); sim_log "FAIL: $1"; fi
}

# Prints "<http-status> <body>" for an authenticated call.
call() { # call <method> <path> <token> [body]
  local method="$1" path="$2" token="$3" body="${4:-}" args=()
  args=(-s -o /tmp/mode-gates-body -w '%{http_code}' -X "$method" "$SIM_API_BASE$path"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  local status
  status="$(curl "${args[@]}")"
  printf '%s %s' "$status" "$(cat /tmp/mode-gates-body)"
}

status_of() { printf '%s' "${1%% *}"; }
body_of() { printf '%s' "${1#* }"; }
code_of() { # the gate code inside an error body
  python3 -c "
import json,sys
try:
    print(json.loads(sys.argv[1]).get('code',''))
except Exception:
    print('')
" "$(body_of "$1")"
}

sim_require_tools
sim_require_api

sim_log "authenticating a fresh phone-OTP account: $DRIVER_PHONE"
TOKEN="$(sim_login "$DRIVER_PHONE")"
[ -n "$TOKEN" ] || sim_fail "no OTP token"

sim_log "provisioning the admin actor"
ADMIN_TOKEN="$(sim_keycloak_login_with_roles "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN" admin-web)"

# 1 — no driver profile at all.
R="$(call GET /api/v1/driver/documents "$TOKEN")"
check "no driver profile -> 403 DRIVER_PROFILE_MISSING" \
  "$([ "$(status_of "$R")" = 403 ] && [ "$(code_of "$R")" = DRIVER_PROFILE_MISSING ] && echo true || echo false)"

CTX="$(call GET /api/v1/me/context "$TOKEN")"
check "the same reason is on /me/context before anything is tapped" \
  "$(python3 -c "
import json,sys
d=json.loads(sys.argv[1])['data']
print('true' if any(g['code']=='DRIVER_PROFILE_MISSING' for g in d['driver']['gates']) else 'false')
" "$(body_of "$CTX")")"

# 2 — applied, under review.
call POST /api/v1/driver/application "$TOKEN" '{"displayName":"Sim Driver"}' >/dev/null
R="$(call GET /api/v1/driver/documents "$TOKEN")"
check "under review -> 403 DRIVER_REVIEW_PENDING" \
  "$([ "$(code_of "$R")" = DRIVER_REVIEW_PENDING ] && echo true || echo false)"

DRIVER_PROFILE_ID="$(sim_psql "SELECT dp.driver_profile_id FROM driver.driver_profile dp
  JOIN identity.app_user u ON u.app_user_id = dp.app_user_id
  WHERE u.phone = '$DRIVER_PHONE'")"
[ -n "$DRIVER_PROFILE_ID" ] || sim_fail "driver profile was not created"

# 3 + 4 — the core fix: approval, then both namespaces on one unchanged token.
call POST "/api/v1/admin/drivers/$DRIVER_PROFILE_ID/review?status=APPROVED" "$ADMIN_TOKEN" >/dev/null
R="$(call GET /api/v1/driver/documents "$TOKEN")"
check "approved -> driver endpoint 200 on the ORIGINAL token" \
  "$([ "$(status_of "$R")" = 200 ] && echo true || echo false)"

R="$(call GET /api/v1/passenger/profile "$TOKEN")"
check "the same token still reaches a passenger endpoint" \
  "$([ "$(status_of "$R")" = 200 ] && echo true || echo false)"

# 5 — deactivation stops driving only.
call POST "/api/v1/admin/drivers/$DRIVER_PROFILE_ID/deactivate" "$ADMIN_TOKEN" \
  '{"reason":"Three missed starts","caseRef":"SL-40912"}' >/dev/null
R="$(call GET /api/v1/driver/documents "$TOKEN")"
check "deactivated -> 403 DRIVER_DEACTIVATED" \
  "$([ "$(code_of "$R")" = DRIVER_DEACTIVATED ] && echo true || echo false)"

R="$(call GET /api/v1/driver/payout-profile "$TOKEN")"
check "deactivated driver still reaches payout details (D34 promises the money)" \
  "$([ "$(status_of "$R")" != 403 ] && echo true || echo false)"

# 6 — the way back.
R="$(call POST /api/v1/driver/reinstatement-requests "$TOKEN" '{"message":"I was in hospital"}')"
check "reinstatement request accepted" \
  "$([ "$(status_of "$R")" = 200 ] && echo true || echo false)"
R="$(call POST /api/v1/driver/reinstatement-requests "$TOKEN" '{"message":"again"}')"
check "a second request while one is open is refused" \
  "$([ "$(status_of "$R")" = 409 ] && echo true || echo false)"

# 7 — reinstated.
call POST "/api/v1/admin/drivers/$DRIVER_PROFILE_ID/reinstate?note=Appeal%20upheld" "$ADMIN_TOKEN" >/dev/null
R="$(call GET /api/v1/driver/documents "$TOKEN")"
check "reinstated -> driver endpoint 200 again" \
  "$([ "$(status_of "$R")" = 200 ] && echo true || echo false)"

# 8 — suspension outranks everything, on the token already in hand.
APP_USER_ID="$(sim_psql "SELECT app_user_id FROM identity.app_user WHERE phone = '$DRIVER_PHONE'")"
call POST "/api/v1/admin/users/$APP_USER_ID/suspend" "$ADMIN_TOKEN" '{"reason":"QA run"}' >/dev/null
R="$(call GET /api/v1/driver/documents "$TOKEN")"
check "suspended -> driver namespace 403 ACCOUNT_SUSPENDED" \
  "$([ "$(code_of "$R")" = ACCOUNT_SUSPENDED ] && echo true || echo false)"
R="$(call GET /api/v1/passenger/profile "$TOKEN")"
check "suspended -> passenger namespace 403 ACCOUNT_SUSPENDED (token minted before the suspension)" \
  "$([ "$(code_of "$R")" = ACCOUNT_SUSPENDED ] && echo true || echo false)"

call POST "/api/v1/admin/users/$APP_USER_ID/activate" "$ADMIN_TOKEN" '{"reason":"QA cleanup"}' >/dev/null

sim_log "audit rows for this run:"
sim_psql "SELECT action, target_id FROM audit.audit_action
  WHERE action IN ('DRIVER_DEACTIVATED','DRIVER_REINSTATED','USER_STATUS_CHANGED')
  ORDER BY audit_action_id DESC LIMIT 5" || true

sim_log "passed: $PASS   failed: $FAIL"
[ "$FAIL" -eq 0 ] || exit 1
