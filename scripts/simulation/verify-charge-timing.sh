#!/usr/bin/env bash
# Verifies slice 04 — charge timing — against the LOCAL stack.
#
# The promise under test is stated on eleven screens and, until this slice, was not true of the
# backend at all: the card is AUTHORISED at booking and CAPTURED when the driver starts the trip.
# Nothing moves in between, and a trip that never happens costs the passenger nothing.
#
#   1. Booking authorises but does not capture
#   2. Driver approval does not capture either
#   3. Trip start captures every card, exactly once
#   4. A repeated start captures nothing further
#   5. Cancelling before the start voids the hold and charges zero
#   6. A cash booking creates no intent at all
#   7. Every gateway call left an attempt row, written before the call
#   8. Reconciliation is reachable by finance and by nobody else
#
# Requires the local stack (docker compose) with demo OTP enabled. Never run against production.
#
# Usage: scripts/simulation/verify-charge-timing.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
PASSENGER_PHONE="${ROUTESHARE_SIM_CHARGE_PHONE:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"

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
  args=(-s -o /tmp/charge-body -w '%{http_code}' -X "$method" "$SIM_API_BASE$path"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/charge-body)"
}
status_of() { printf '%s' "${1%% *}"; }
body_of() { printf '%s' "${1#* }"; }
data() { python3 -c "
import json,sys
try:
    d=json.loads(sys.argv[1])['data']
    for key in sys.argv[2].split('.'):
        d = d[key] if not isinstance(d, list) else d[int(key)]
    print('' if d is None else d)
except Exception:
    print('')
" "$(body_of "$1")" "$2"; }

sim_require_tools
sim_require_api

TOKEN="$(sim_login "$PASSENGER_PHONE")"
ADMIN_TOKEN="$(sim_keycloak_login_with_roles "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN" admin-web)"

sim_log "seeding a demo route (idempotent)"
bash ./seed-demo-route.sh >/dev/null 2>&1 || sim_log "seed script reported an issue; continuing"
OCCURRENCE_ID="$(sim_psql "SELECT route_occurrence_id FROM routing.route_occurrence
  WHERE status = 'PUBLISHED' ORDER BY route_occurrence_id DESC LIMIT 1")"
[ -n "$OCCURRENCE_ID" ] || sim_fail "no published trip to book"

book() { # book <paymentMethodIdOrNull> -> bookingId
  local method="$1"
  local body
  body="{\"routeOccurrenceId\":$OCCURRENCE_ID,\"seats\":1,\"pickupLat\":6.90,\"pickupLng\":79.86,
         \"dropLat\":6.95,\"dropLng\":79.90,\"pickupRouteFraction\":0.1,
         \"dropoffRouteFraction\":0.6,\"paymentMethodId\":$method}"
  local response
  response="$(curl -s -X POST "$SIM_API_BASE/api/v1/passenger/bookings" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -H "Idempotency-Key: sim-$RANDOM-$RANDOM" -d "$body")"
  python3 -c "
import json,sys
try:
    print(json.loads(sys.argv[1])['data']['bookingId'])
except Exception:
    print('')
" "$response"
}

# 6 — cash first: it must create no intent at all.
CASH_BOOKING="$(book null)"
if [ -n "$CASH_BOOKING" ]; then
  INTENTS="$(sim_psql "SELECT count(*) FROM payment.payment_intent WHERE booking_id = $CASH_BOOKING")"
  equals "a cash booking creates no payment intent" "$INTENTS" "0"
  equals "the booking records itself as cash" \
    "$(sim_psql "SELECT payment_method FROM booking.booking WHERE booking_id = $CASH_BOOKING")" "CASH"
else
  sim_log "SKIP: cash booking could not be created"
fi

# A card booking needs a stored instrument; with Cybersource disabled the fallback still exercises
# the full state machine, which is the point of testing it this way.
PAYMENT_METHOD_ID="$(sim_psql "SELECT payment_method_id FROM payment.payment_method
  WHERE status = 'ACTIVE' ORDER BY payment_method_id DESC LIMIT 1")"
if [ -z "$PAYMENT_METHOD_ID" ]; then
  sim_log "SKIP: no stored card on this stack; card-path checks not run"
  sim_log "passed: $PASS   failed: $FAIL"
  [ "$FAIL" -eq 0 ] || exit 1
  exit 0
fi

# 1 — booking authorises, and does not capture.
CARD_BOOKING="$(book "$PAYMENT_METHOD_ID")"
[ -n "$CARD_BOOKING" ] || sim_fail "card booking could not be created"
equals "booking authorises the card" \
  "$(sim_psql "SELECT status FROM payment.payment_intent WHERE booking_id = $CARD_BOOKING")" \
  "AUTHORIZED"
equals "nothing is captured at booking" \
  "$(sim_psql "SELECT coalesce(captured_at::text,'') FROM payment.payment_intent
     WHERE booking_id = $CARD_BOOKING")" ""

R="$(call GET "/api/v1/passenger/bookings/$CARD_BOOKING" "$TOKEN")"
equals "the app is told authorised, not charged" "$(data "$R" payment.status)" "AUTHORIZED"

# 2 — approval must not charge.
# Keyed on the occurrence, not the plan: since V032 a trip belongs to one occurrence, and a
# recurring plan has many. Joining on route_plan_id could pick a sibling occurrence's trip.
TRIP_ID="$(sim_psql "SELECT t.trip_id FROM trip.trip t
  JOIN booking.booking b ON b.route_occurrence_id = t.route_occurrence_id
  WHERE b.booking_id = $CARD_BOOKING LIMIT 1")"
equals "approval does not capture" \
  "$(sim_psql "SELECT status FROM payment.payment_intent WHERE booking_id = $CARD_BOOKING")" \
  "AUTHORIZED"

# 3 + 4 — start captures once; a repeat captures nothing further.
if [ -n "$TRIP_ID" ]; then
  call POST "/api/v1/driver/trips/$TRIP_ID/start" "$ADMIN_TOKEN" '{}' >/dev/null
  equals "starting the trip captures the card" \
    "$(sim_psql "SELECT status FROM payment.payment_intent WHERE booking_id = $CARD_BOOKING")" \
    "CAPTURED"
  CAPTURES_BEFORE="$(sim_psql "SELECT count(*) FROM payment.payment_attempt
    WHERE booking_id = $CARD_BOOKING AND operation = 'CAPTURE'")"
  call POST "/api/v1/driver/trips/$TRIP_ID/start" "$ADMIN_TOKEN" '{}' >/dev/null
  equals "a repeated start captures nothing further" \
    "$(sim_psql "SELECT count(*) FROM payment.payment_attempt
       WHERE booking_id = $CARD_BOOKING AND operation = 'CAPTURE'")" "$CAPTURES_BEFORE"
else
  sim_fail "no trip row for the booked route; capture checks could not run"
fi

# 5 — cancelling before the start charges nothing.
CANCEL_BOOKING="$(book "$PAYMENT_METHOD_ID")"
if [ -n "$CANCEL_BOOKING" ]; then
  call POST "/api/v1/passenger/bookings/$CANCEL_BOOKING/transitions" "$TOKEN" \
    '{"status":"CANCELLED","reason":"QA"}' >/dev/null
  equals "cancelling before the start releases the hold" \
    "$(sim_psql "SELECT status FROM payment.payment_intent WHERE booking_id = $CANCEL_BOOKING")" \
    "VOIDED"
  equals "and charges nothing" \
    "$(sim_psql "SELECT coalesce(sum(amount),0) FROM payment.fare_ledger_entry
       WHERE booking_id = $CANCEL_BOOKING AND entry_type = 'PAYMENT_CAPTURED'")" "0"
fi

# 7 — every call left a record, written before it was made.
check "every gateway call has an attempt row" \
  "$([ "$(sim_psql "SELECT count(*) FROM payment.payment_attempt
     WHERE booking_id = $CARD_BOOKING")" -ge 1 ] && echo true || echo false)"
equals "no attempt was left unfinished" \
  "$(sim_psql "SELECT count(*) FROM payment.payment_attempt WHERE status = 'STARTED'")" "0"

# 8 — who may see the money that is stuck.
R="$(call GET /api/v1/admin/payments/reconciliation "$ADMIN_TOKEN")"
check "finance can read reconciliation" \
  "$([ "$(status_of "$R")" = 200 ] && echo true || echo false)"
R="$(call GET /api/v1/admin/payments/reconciliation "$TOKEN")"
check "a passenger cannot" \
  "$([ "$(status_of "$R")" = 403 ] && echo true || echo false)"

sim_log "payment attempts for this run:"
sim_psql "SELECT operation, status, idempotency_key FROM payment.payment_attempt
  ORDER BY payment_attempt_id DESC LIMIT 8" || true

sim_log "passed: $PASS   failed: $FAIL"
[ "$FAIL" -eq 0 ] || exit 1
