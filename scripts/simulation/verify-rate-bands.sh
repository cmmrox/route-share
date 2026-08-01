#!/usr/bin/env bash
# Verifies slice 02 — vehicle classes and rate bands — end to end against the LOCAL stack.
#
# The rule under test is the product's central pricing rule: a driver never types a price. ComiGo
# assesses a band, the driver picks a point inside it, and an approved car with no band cannot
# publish at all — which is a screen (D40), not an error.
#
#   1. Vehicle classes are seeded with caps and default bands
#   2. A seat count above the class cap is refused
#   3. A newly approved vehicle is PENDING_ASSESSMENT and blocks publishing (RATE_BAND_NOT_SET)
#   4. A band outside the class range is refused by the DATABASE, not only the service
#   5. An admin band activates it, defaults the rate to the midpoint, and unblocks publishing
#   6. A rate outside the band is refused; a rate inside it is accepted
#   7. A driver cannot set their own band (the escalation that would let them price themselves)
#   8. A second open review request is refused
#
# Requires the local stack (docker compose) with demo OTP enabled. Never run against production.
#
# Usage: scripts/simulation/verify-rate-bands.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
DRIVER_PHONE="${ROUTESHARE_SIM_BAND_PHONE:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"
PLATE="QA-$RANDOM"

PASS=0; FAIL=0
check() { # check <name> <ok-boolean>
  if [ "$2" = "true" ]; then PASS=$((PASS+1)); sim_log "PASS: $1"; else FAIL=$((FAIL+1)); sim_log "FAIL: $1"; fi
}

call() { # call <method> <path> <token> [body] -> "<status> <body>"
  local method="$1" path="$2" token="$3" body="${4:-}" args=()
  args=(-s -o /tmp/rate-bands-body -w '%{http_code}' -X "$method" "$SIM_API_BASE$path"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/rate-bands-body)"
}
status_of() { printf '%s' "${1%% *}"; }
body_of() { printf '%s' "${1#* }"; }
json_of() { python3 -c "
import json,sys
try:
    print(json.loads(sys.argv[1]).get(sys.argv[2],''))
except Exception:
    print('')
" "$(body_of "$1")" "$2"; }
# parse_float=Decimal keeps money at the scale the API sent it: json.loads would turn 49.50 into
# the float 49.5, and every scale-2 comparison below would fail against a correct response.
data_of() { python3 -c "
import json,sys
from decimal import Decimal
try:
    d=json.loads(sys.argv[1], parse_float=Decimal)['data']
    for key in sys.argv[2].split('.'):
        d = d[key]
    print(d)
except Exception:
    print('')
" "$(body_of "$1")" "$2"; }

sim_require_tools
sim_require_api

sim_log "authenticating a fresh phone-OTP driver: $DRIVER_PHONE"
TOKEN="$(sim_login "$DRIVER_PHONE")"
ADMIN_TOKEN="$(sim_keycloak_login_with_roles "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN" admin-web)"

call POST /api/v1/driver/application "$TOKEN" '{"displayName":"Band QA"}' >/dev/null
DRIVER_PROFILE_ID="$(sim_psql "SELECT dp.driver_profile_id FROM driver.driver_profile dp
  JOIN identity.app_user u ON u.app_user_id = dp.app_user_id WHERE u.phone = '$DRIVER_PHONE'")"
call POST "/api/v1/admin/drivers/$DRIVER_PROFILE_ID/review?status=APPROVED" "$ADMIN_TOKEN" >/dev/null

# 1 — the reference data the class picker reads.
R="$(call GET /api/v1/driver/vehicle-classes "$TOKEN")"
check "four vehicle classes are seeded" \
  "$([ "$(python3 -c "
import json,sys
print(len(json.loads(sys.argv[1])['data']))" "$(body_of "$R")")" = 4 ] && echo true || echo false)"

# 2 — the cap is a capacity promise to riders, so it is enforced.
R="$(call POST /api/v1/driver/vehicles "$TOKEN" \
  "{\"make\":\"Toyota\",\"model\":\"Aqua\",\"manufactureYear\":2018,\"color\":\"White\",\"registrationNumber\":\"$PLATE-X\",\"seatCount\":6,\"vehicleClass\":\"CAR\"}")"
check "six seats in a CAR is refused with SEATS_EXCEED_CLASS_CAP" \
  "$([ "$(json_of "$R" code)" = SEATS_EXCEED_CLASS_CAP ] && echo true || echo false)"

R="$(call POST /api/v1/driver/vehicles "$TOKEN" \
  "{\"make\":\"Toyota\",\"model\":\"Aqua\",\"manufactureYear\":2018,\"color\":\"White\",\"registrationNumber\":\"$PLATE\",\"seatCount\":3,\"vehicleClass\":\"CAR\"}")"
VEHICLE_ID="$(data_of "$R" id)"
check "a vehicle within the cap is created" "$([ -n "$VEHICLE_ID" ] && echo true || echo false)"

# 3 — approved papers are not a price.
call POST "/api/v1/admin/vehicles/$VEHICLE_ID/review" "$ADMIN_TOKEN" '{"status":"APPROVED"}' >/dev/null
R="$(call GET "/api/v1/driver/vehicles/$VEHICLE_ID/rate-band" "$TOKEN")"
check "a newly approved vehicle is PENDING_ASSESSMENT" \
  "$([ "$(data_of "$R" status)" = PENDING_ASSESSMENT ] && echo true || echo false)"

CTX="$(call GET /api/v1/me/context "$TOKEN")"
check "RATE_BAND_NOT_SET appears on /me/context before the driver tries to publish" \
  "$(python3 -c "
import json,sys
d=json.loads(sys.argv[1])['data']
print('true' if any(g['code']=='RATE_BAND_NOT_SET' for g in d['driver']['gates']) else 'false')
" "$(body_of "$CTX")")"

R="$(call POST /api/v1/routes "$TOKEN" '{"originLabel":"A","destinationLabel":"B"}')"
check "publishing is blocked while the band is unset" \
  "$([ "$(status_of "$R")" != 200 ] && echo true || echo false)"

# 4 — the database is the last line, not the service.
DB_REFUSED="$(docker exec -i "$SIM_PSQL_CONTAINER" psql -U "$SIM_DB_USER" -d "$SIM_DB_NAME" -tA \
  -c "UPDATE vehicle.vehicle_rate_band SET rate_min = 10, rate_max = 20 WHERE vehicle_id = $VEHICLE_ID" 2>&1 \
  | grep -c BAND_OUTSIDE_CLASS)"
check "the database itself refuses a band outside its class" \
  "$([ "$DB_REFUSED" -ge 1 ] && echo true || echo false)"

R="$(call PUT "/api/v1/admin/vehicles/$VEHICLE_ID/rate-band" "$ADMIN_TOKEN" \
  '{"rateMin":30,"rateMax":58,"factors":[],"note":"below the class floor"}')"
check "the service refuses a band outside its class with BAND_OUTSIDE_CLASS" \
  "$([ "$(json_of "$R" code)" = BAND_OUTSIDE_CLASS ] && echo true || echo false)"

# 5 — a real assessment.
R="$(call PUT "/api/v1/admin/vehicles/$VEHICLE_ID/rate-band" "$ADMIN_TOKEN" \
  '{"rateMin":41,"rateMax":58,"factors":[{"key":"AGE","label":"Wear and tyres","detail":"2018","delta":-2}],"note":"initial"}')"
check "the band becomes ACTIVE" "$([ "$(data_of "$R" status)" = ACTIVE ] && echo true || echo false)"
check "the rate defaults to the midpoint so the car is publishable at once" \
  "$([ "$(data_of "$R" chosenRate)" = "49.50" ] && echo true || echo false)"

CTX="$(call GET /api/v1/me/context "$TOKEN")"
check "the publish gate clears once the band is set" \
  "$(python3 -c "
import json,sys
d=json.loads(sys.argv[1])['data']
print('false' if any(g['code']=='RATE_BAND_NOT_SET' for g in d['driver']['gates']) else 'true')
" "$(body_of "$CTX")")"

# 6 — the driver picks inside the band, and only inside it.
R="$(call PUT "/api/v1/driver/vehicles/$VEHICLE_ID/rate-band/chosen-rate" "$TOKEN" '{"ratePerKm":70}')"
check "a rate above the band is refused with RATE_OUTSIDE_BAND" \
  "$([ "$(json_of "$R" code)" = RATE_OUTSIDE_BAND ] && echo true || echo false)"
R="$(call PUT "/api/v1/driver/vehicles/$VEHICLE_ID/rate-band/chosen-rate" "$TOKEN" '{"ratePerKm":46}')"
check "a rate inside the band is accepted" \
  "$([ "$(status_of "$R")" = 200 ] && echo true || echo false)"
check "the position is derived server-side so D39 and P07 agree" \
  "$([ -n "$(data_of "$R" position.key)" ] && echo true || echo false)"

# 7 — the escalation that matters most in this slice.
R="$(call PUT "/api/v1/admin/vehicles/$VEHICLE_ID/rate-band" "$TOKEN" '{"rateMin":55,"rateMax":62}')"
check "a driver cannot set their own band" \
  "$([ "$(status_of "$R")" = 403 ] && echo true || echo false)"

# 8 — one re-assessment.
R="$(call POST "/api/v1/driver/vehicles/$VEHICLE_ID/rate-band/review-requests" "$TOKEN" \
  '{"reason":"NEW_TYRES","note":"New tyres fitted"}')"
check "a review request is accepted" "$([ "$(status_of "$R")" = 200 ] && echo true || echo false)"
R="$(call GET "/api/v1/driver/vehicles/$VEHICLE_ID/rate-band" "$TOKEN")"
check "the band under review is still live" \
  "$([ "$(data_of "$R" status)" = UNDER_REVIEW ] && echo true || echo false)"
R="$(call POST "/api/v1/driver/vehicles/$VEHICLE_ID/rate-band/review-requests" "$TOKEN" \
  '{"reason":"AGAIN","note":"again"}')"
check "a second open review request is refused" \
  "$([ "$(json_of "$R" code)" = RATE_REVIEW_ALREADY_OPEN ] && echo true || echo false)"

sim_log "audit rows for this run:"
sim_psql "SELECT action, target_id FROM audit.audit_action
  WHERE action LIKE 'VEHICLE_RATE_BAND%' ORDER BY audit_action_id DESC LIMIT 5" || true

sim_log "passed: $PASS   failed: $FAIL"
[ "$FAIL" -eq 0 ] || exit 1
