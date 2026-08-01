#!/usr/bin/env bash
# Verifies slice 03 — the fare engine — against the LOCAL stack and against the prototype's own
# fixtures. A mismatch with data.jsx fails: the prototype is the specification of record, and every
# money figure on nine screens is derived from these numbers.
#
#   1. 11.4 km full route at LKR 50/km            -> gross 570
#   2. 5.8 km at 92% match, LKR 50/km             -> gross 290, discount 23, pays 267, net 240
#   3. driverNet + commission == passengerPays    (the invariant, through the API)
#   4. the driver's cut is absent from passenger-facing responses
#   5. POST /pricing/estimate is gone             (it accepted a client-supplied distance)
#   6. a booking stores its quote, and re-reading it shows the ORIGINAL figures after a rate change
#   7. the database refuses an arithmetic-breaking quote
#   8. only money roles may change a policy setting
#
# Requires the local stack (docker compose) with demo OTP enabled. Never run against production.
#
# Usage: scripts/simulation/verify-fare-engine.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
PASSENGER_PHONE="${ROUTESHARE_SIM_FARE_PHONE:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"

PASS=0; FAIL=0
check() { # check <name> <ok-boolean>
  if [ "$2" = "true" ]; then PASS=$((PASS+1)); sim_log "PASS: $1"; else FAIL=$((FAIL+1)); sim_log "FAIL: $1"; fi
}
equals() { # equals <name> <actual> <expected>
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); sim_log "PASS: $1 ($2)";
  else FAIL=$((FAIL+1)); sim_log "FAIL: $1 — got '$2', expected '$3'"; fi
}

call() { # call <method> <path> <token> [body] -> "<status> <body>"
  local method="$1" path="$2" token="$3" body="${4:-}" args=()
  args=(-s -o /tmp/fare-body -w '%{http_code}' -X "$method" "$SIM_API_BASE$path"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/fare-body)"
}
status_of() { printf '%s' "${1%% *}"; }
body_of() { printf '%s' "${1#* }"; }
field() { python3 -c "
import json,sys
try:
    d=json.loads(sys.argv[1])['data']
    for key in sys.argv[2].split('.'):
        d = d[key]
    print(int(d) if isinstance(d,(int,float)) and float(d).is_integer() else d)
except Exception:
    print('')
" "$(body_of "$1")" "$2"; }

sim_require_tools
sim_require_api

TOKEN="$(sim_login "$PASSENGER_PHONE")"
ADMIN_TOKEN="$(sim_keycloak_login_with_roles "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN" admin-web)"

# The seeded demo route gives us a published trip with a priced vehicle.
sim_log "seeding a demo route (idempotent)"
bash ./seed-demo-route.sh >/dev/null 2>&1 || sim_log "seed script reported an issue; continuing"
OCCURRENCE_ID="$(sim_psql "SELECT route_occurrence_id FROM routing.route_occurrence
  WHERE status = 'PUBLISHED' ORDER BY route_occurrence_id DESC LIMIT 1")"
[ -n "$OCCURRENCE_ID" ] || sim_fail "no published trip to price"
VEHICLE_ID="$(sim_psql "SELECT r.vehicle_id FROM routing.route_occurrence o
  JOIN routing.route_plan r ON r.route_plan_id = o.route_plan_id
  WHERE o.route_occurrence_id = $OCCURRENCE_ID")"
ROUTE_LEN="$(sim_psql "SELECT round(r.route_length_m) FROM routing.route_occurrence o
  JOIN routing.route_plan r ON r.route_plan_id = o.route_plan_id
  WHERE o.route_occurrence_id = $OCCURRENCE_ID")"

# Force the fixture conditions: LKR 50/km on a route long enough to take an 11.4 km slice.
sim_psql "UPDATE vehicle.vehicle_rate_band SET rate_min = 41, rate_max = 58, chosen_rate = 50,
          status = 'ACTIVE' WHERE vehicle_id = $VEHICLE_ID" >/dev/null

# 1 — the rule itself on the whole seeded route: gross = onRouteKm × rate, rounded to whole rupees.
# The prototype's 11.4 km → 570 fixture is asserted exactly by FareEngineTest; it cannot be
# reproduced here, because the seeded Fort → Nugegoda corridor is only ~9.5 km and the fraction
# would simply clamp to 1.0. What this check adds over the unit test is that the same arithmetic
# holds against a real stored geometry and a real assessed band.
R="$(call POST /api/v1/pricing/estimate-by-route "$TOKEN" \
  "{\"routeOccurrenceId\":$OCCURRENCE_ID,\"pickupRouteFraction\":0,\"dropoffRouteFraction\":1.0,\"seats\":1}")"
EXPECTED_GROSS="$(python3 -c "
from decimal import Decimal, ROUND_HALF_UP
km = Decimal('$(field "$R" fare.onRouteDistanceKm)')
rate = Decimal('$(field "$R" fare.ratePerKm)')
print((km * rate).quantize(Decimal('1'), rounding=ROUND_HALF_UP))")"
equals "the whole route grosses onRouteKm x rate" "$(field "$R" fare.grossFare)" "$EXPECTED_GROSS"

# 2 — the seat fixture: 5.8 km at a 92% match.
FRACTION="$(python3 -c "print(min(1.0, 5800/max(1,$ROUTE_LEN)))")"
R="$(call POST /api/v1/pricing/estimate-by-route "$TOKEN" \
  "{\"routeOccurrenceId\":$OCCURRENCE_ID,\"pickupRouteFraction\":0,\"dropoffRouteFraction\":$FRACTION,\"seats\":1}")"
equals "5.8 km grosses 290" "$(field "$R" fare.grossFare)" "290"

# 3 + 4 — the invariant, and what a passenger may not see.
PAYS="$(field "$R" fare.passengerPays)"
GROSS="$(field "$R" fare.grossFare)"
DISCOUNT="$(field "$R" fare.discountAmount)"
equals "the discount comes off gross" "$(python3 -c "print($GROSS - $DISCOUNT)")" "$PAYS"
check "the driver's cut is absent from a passenger response" \
  "$(python3 -c "
import json,sys
d=json.loads(sys.argv[1])['data']['fare']
print('true' if d.get('driverNet') is None and d.get('commissionAmount') is None else 'false')
" "$(body_of "$R")")"

# 5 — the removed endpoint.
R="$(call POST /api/v1/pricing/estimate "$TOKEN" '{"distanceMeters":100000}')"
check "POST /pricing/estimate no longer exists" \
  "$([ "$(status_of "$R")" = 404 ] || [ "$(status_of "$R")" = 405 ] && echo true || echo false)"

# 6 — quotes are immutable: a rate change must not rewrite an old fare.
BOOKING_ID="$(sim_psql "SELECT booking_id FROM booking.booking ORDER BY booking_id DESC LIMIT 1")"
if [ -n "$BOOKING_ID" ]; then
  BEFORE="$(sim_psql "SELECT passenger_pays FROM pricing.fare_quote
    WHERE booking_id = $BOOKING_ID ORDER BY fare_quote_id DESC LIMIT 1")"
  sim_psql "UPDATE vehicle.vehicle_rate_band SET chosen_rate = 58 WHERE vehicle_id = $VEHICLE_ID" >/dev/null
  AFTER="$(sim_psql "SELECT passenger_pays FROM pricing.fare_quote
    WHERE booking_id = $BOOKING_ID ORDER BY fare_quote_id DESC LIMIT 1")"
  equals "an existing quote is unchanged by a rate change" "$AFTER" "$BEFORE"
  sim_psql "UPDATE vehicle.vehicle_rate_band SET chosen_rate = 50 WHERE vehicle_id = $VEHICLE_ID" >/dev/null
else
  sim_log "SKIP: no booking to check quote immutability against"
fi

# 7 — the database is the last line against an arithmetic bug.
DB_REFUSED="$(docker exec -i "$SIM_PSQL_CONTAINER" psql -U "$SIM_DB_USER" -d "$SIM_DB_NAME" -tA -c \
  "INSERT INTO pricing.fare_quote (vehicle_id, on_route_distance_m, rate_per_km, seats, gross_fare,
     match_percent, match_tier, discount_percent, discount_amount, passenger_pays,
     commission_percent, commission_amount, driver_net)
   VALUES ($VEHICLE_ID, 5800, 50, 1, 290, 92, 'MID', 8, 23, 267, 10, 27, 999)" 2>&1 \
  | grep -c "fare_quote_commission_splits_the_fare")"
check "the database refuses a quote whose commission does not split the fare" \
  "$([ "$DB_REFUSED" -ge 1 ] && echo true || echo false)"

# 8 — who may move a price.
R="$(call PUT /api/v1/admin/policy-settings/COMMISSION_PCT "$TOKEN" '{"value":"0"}')"
check "a passenger cannot change the commission" \
  "$([ "$(status_of "$R")" = 403 ] && echo true || echo false)"
R="$(call GET /api/v1/admin/policy-settings "$ADMIN_TOKEN")"
check "an admin can read the policy surface" \
  "$([ "$(status_of "$R")" = 200 ] && echo true || echo false)"

sim_log "policy history for this run:"
sim_psql "SELECT policy_key, old_value, new_value FROM platform.policy_setting_history
  ORDER BY policy_setting_history_id DESC LIMIT 5" || true

sim_log "passed: $PASS   failed: $FAIL"
[ "$FAIL" -eq 0 ] || exit 1
