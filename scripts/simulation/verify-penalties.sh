#!/usr/bin/env bash
# Verifies slice 06 — penalties, dues and compensation — against the LOCAL stack.
#
# Money is the subject, so every check reads the ledger and the assessment rows rather than a
# response body that could agree with itself and disagree with the database. Fares are rewritten to
# the prototype's fixtures before a trigger fires, for the same reason the timer script rewrites
# deadlines: a figure that only matches because the seed happened to price it that way proves
# nothing about the arithmetic.
#
#   1. A no-show on a LKR 197 fare is a 49 fee, 25 to the driver and 24 to ComiGo
#   2. The halves always re-add to the fee, in the database
#   3. Compensation is its own ledger kind, not folded into fares
#   4. A cash passenger is never charged; the fee becomes a due
#   5. A repeated trigger assesses nothing further
#   6. Dues appear on the next checkout and never block it
#   7. A driver late-cancellation on LKR 429 expected net is 86, 43 shared across two riders
#   8. A driver is deducted from earnings and never billed
#   9. Beneficiaries are disclosed by first name only
#  10. A dispute is accepted inside 48 hours and refused outside it
#  11. An upheld reversal returns the money and clears the due
#  12. A missed start is recorded at zero — D32b charges no fee
#
# Requires the local stack (docker compose) with demo OTP and the scheduler enabled.
# Never run against production.
#
# Usage:
#   ROUTESHARE_API_BASE=http://localhost:8088 ROUTESHARE_DB_NAME=routeshare_comigo \
#     scripts/simulation/verify-penalties.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
PASSENGER_PHONE="${ROUTESHARE_SIM_PENALTY_PHONE:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"
TICK="${ROUTESHARE_SIM_TICK_SECONDS:-70}"

PASS=0; FAIL=0; SKIP=0
check() { if [ "$2" = "true" ]; then PASS=$((PASS+1)); sim_log "PASS: $1";
          else FAIL=$((FAIL+1)); sim_log "FAIL: $1"; fi; }
equals() { if [ "$2" = "$3" ]; then PASS=$((PASS+1)); sim_log "PASS: $1 ($2)";
           else FAIL=$((FAIL+1)); sim_log "FAIL: $1 — got '$2', expected '$3'"; fi; }
skip() { SKIP=$((SKIP+1)); sim_log "SKIP: $1"; }

call() { # call <method> <path> <token> [body] -> "<status> <body>"
  local method="$1" path="$2" token="$3" body="${4:-}" args=()
  args=(-s -o /tmp/penalty-body -w '%{http_code}' -X "$method" "$SIM_API_BASE$path"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/penalty-body)"
}
status_of() { printf '%s' "${1%% *}"; }
money() { python3 -c "import sys; print('%.2f' % float(sys.argv[1] or 0))" "$1"; }
body_of() { printf '%s' "${1#* }"; }
error_code() { python3 -c "
import json,sys
try:
    b = json.loads(sys.argv[1])
    print(b.get('code') or b.get('error', {}).get('code') or b.get('data', {}).get('code') or '')
except Exception:
    print('')
" "$(body_of "$1")"; }
data() { python3 -c "
import json,sys
try:
    d=json.loads(sys.argv[1])['data']
    for key in sys.argv[2].split('.'):
        d = d[int(key)] if isinstance(d, list) else d[key]
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
sim_psql "UPDATE routing.route_occurrence SET status = 'PUBLISHED'
          WHERE status = 'CANCELLED' AND scheduled_departure_at > now()" >/dev/null

free_occurrence() { # an occurrence with no trip behind it yet — one trip per occurrence (V032)
  sim_psql "SELECT ro.route_occurrence_id FROM routing.route_occurrence ro
    WHERE ro.status = 'PUBLISHED' AND ro.scheduled_departure_at > now()
      AND NOT EXISTS (SELECT 1 FROM trip.trip t
                       WHERE t.route_occurrence_id = ro.route_occurrence_id)
    ORDER BY ro.route_occurrence_id ASC LIMIT 1"
}

# Each case consumes one occurrence, and repeated runs exhaust what the seed publishes. Minting a
# fresh one off the same plan keeps a re-run from silently degrading into a page of skips.
next_occurrence() {
  local found
  found="$(free_occurrence)"
  if [ -z "$found" ]; then
    sim_psql "INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at,
                available_seats, status)
              SELECT rp.route_plan_id, now() + interval '6 hours', 3, 'PUBLISHED'
              FROM routing.route_plan rp
              ORDER BY rp.route_plan_id DESC LIMIT 1" >/dev/null
    found="$(free_occurrence)"
  fi
  # Penalty scenarios require an immediately confirmed booking so a trip exists. The shared demo
  # seed may inherit APPROVE_EACH from preference tests, which would leave this scenario at
  # REQUESTED and make the missing trip look like a penalty implementation failure.
  sim_psql "UPDATE routing.route_occurrence SET approval_mode = 'INSTANT'
            WHERE route_occurrence_id = $found" >/dev/null
  printf '%s' "$found"
}

book_on() { # book_on <routeOccurrenceId> -> bookingId
  curl -s -X POST "$SIM_API_BASE/api/v1/passenger/bookings" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -H "Idempotency-Key: sim-penalty-$RANDOM-$RANDOM" \
    -d "{\"routeOccurrenceId\":$1,\"seats\":1,\"pickupLat\":6.90,\"pickupLng\":79.86,
         \"dropLat\":6.95,\"dropLng\":79.90,\"pickupRouteFraction\":0.1,
         \"dropoffRouteFraction\":0.6,\"paymentMethodId\":null}" \
    | python3 -c "
import json,sys
try: print(json.loads(sys.argv[1])['data']['bookingId'])
except Exception: print('')
" "$(cat)" 2>/dev/null
}

trip_of() { sim_psql "SELECT t.trip_id FROM trip.trip t
  JOIN booking.booking b ON b.route_occurrence_id = t.route_occurrence_id
  WHERE b.booking_id = $1"; }

price_at() { # price_at <bookingId> <passengerPays> — rewrite the stored quote to a fixture
  sim_psql "UPDATE booking.booking SET fare_estimate = $2 WHERE booking_id = $1" >/dev/null
  sim_psql "UPDATE pricing.fare_quote SET passenger_pays = $2,
                   gross_fare = $2 + discount_amount,
                   commission_amount = round($2 * commission_percent / 100),
                   driver_net = $2 - round($2 * commission_percent / 100)
            WHERE booking_id = $1" >/dev/null
}

net_at() { # net_at <bookingId> <driverNet> — set the driver's net for a seat directly
  sim_psql "UPDATE pricing.fare_quote SET driver_net = $2,
                   passenger_pays = $2 + commission_amount,
                   gross_fare = $2 + commission_amount + discount_amount
            WHERE booking_id = $1" >/dev/null
}

penalty_field() { # penalty_field <bookingId> <kind> <column>
  sim_psql "SELECT coalesce($3::text,'') FROM penalty.penalty_assessment
            WHERE booking_id = $1 AND kind = '$2'"; }

# ── 1-5: a no-show, priced ───────────────────────────────────────────────────────────────────────
OCCURRENCE="$(next_occurrence)"
[ -n "$OCCURRENCE" ] || sim_fail "no unbooked published occurrence to work with"
BOOKING="$(book_on "$OCCURRENCE")"
[ -n "$BOOKING" ] || sim_fail "booking could not be created"
TRIP="$(trip_of "$BOOKING")"
[ -n "$TRIP" ] || sim_fail "no trip materialised for the booking"
# Read the caller's id off the booking rather than looking it up by phone: the stored format is
# the auth module's business, and guessing it wrong reads as a product failure.
PASSENGER_APP_USER="$(sim_psql "SELECT passenger_app_user_id FROM booking.booking
  WHERE booking_id = $BOOKING")"

# P27's fare, exactly. RIDES[2] in the prototype: LKR 197.
price_at "$BOOKING" 197.00

DRIVER_PROFILE="$(sim_psql "SELECT dp.driver_profile_id FROM trip.trip t
  JOIN routing.route_plan rp ON rp.route_plan_id = t.route_plan_id
  JOIN driver.driver_profile dp ON dp.driver_profile_id = rp.driver_profile_id
  WHERE t.trip_id = $TRIP")"
DRIVER_APP_USER="$(sim_psql "SELECT app_user_id FROM driver.driver_profile
  WHERE driver_profile_id = $DRIVER_PROFILE")"

# The wait exists only once the driver has been detected at the pickup; slice 05 owns that
# detection, so the row is seeded here exactly as verify-trip-timers.sh does.
WAIT_MIN="$(sim_psql "SELECT value FROM platform.policy_setting WHERE policy_key = 'PICKUP_WAIT_MIN'")"
sim_psql "INSERT INTO trip.pickup_wait(trip_id, booking_id, arrived_at, expires_at)
          VALUES ($TRIP, $BOOKING, now() - interval '10 minutes', now() - interval '1 minute')
          ON CONFLICT (booking_id) DO NOTHING" >/dev/null

sim_log "waiting up to ${TICK}s for the pickup-wait sweep (the job's own tick)"
DEADLINE=$((SECONDS + TICK))
RESOLUTION=""
while [ "$SECONDS" -lt "$DEADLINE" ]; do
  RESOLUTION="$(sim_psql "SELECT coalesce(resolution,'') FROM trip.pickup_wait
    WHERE booking_id = $BOOKING")"
  [ "$RESOLUTION" = "NO_SHOW" ] && break
  sleep 5
done

if [ "$RESOLUTION" != "NO_SHOW" ]; then
  sim_fail "the pickup-wait sweep did not run within ${TICK}s (is ROUTESHARE_SCHEDULER_ENABLED=true?)"
fi

equals "the no-show was assessed exactly once" \
  "$(sim_psql "SELECT count(*) FROM penalty.penalty_assessment
     WHERE booking_id = $BOOKING AND kind = 'PASSENGER_NO_SHOW'")" "1"
equals "25% of LKR 197 is a 49 fee" "$(penalty_field "$BOOKING" PASSENGER_NO_SHOW fee_amount)" "49.00"
equals "the driver's half is 25" "$(penalty_field "$BOOKING" PASSENGER_NO_SHOW victim_share)" "25.00"
equals "ComiGo's half is 24" "$(penalty_field "$BOOKING" PASSENGER_NO_SHOW platform_share)" "24.00"
equals "the base and rate are stored beside the fee, so support can explain it" \
  "$(penalty_field "$BOOKING" PASSENGER_NO_SHOW fare_base)/$(penalty_field "$BOOKING" PASSENGER_NO_SHOW percent)" \
  "197.00/25.00"
equals "the policy version that priced it is recorded" \
  "$(sim_psql "SELECT (policy_version IS NOT NULL)::text FROM penalty.penalty_assessment
     WHERE booking_id = $BOOKING AND kind = 'PASSENGER_NO_SHOW'")" "true"

# 2 — the split, as a database fact rather than a response field.
equals "every split in the database re-adds to its fee" \
  "$(sim_psql "SELECT count(*) FROM penalty.penalty_assessment
     WHERE victim_share + platform_share <> fee_amount")" "0"
equals "the beneficiary rows total the victim half exactly" \
  "$(sim_psql "SELECT count(*) FROM penalty.penalty_assessment p
     WHERE p.victim_share <> (SELECT coalesce(sum(b.amount), 0)
       FROM penalty.penalty_beneficiary b WHERE b.penalty_id = p.penalty_id)
       AND p.victim_share > 0")" "0"

# 3 — compensation is a distinct rewards-ledger kind. Folding it into fares would overstate what
# he earned driving; Slice 11 moved role-neutral compensation into the shared rewards balance.
equals "the driver's half reached him as COMPENSATION, not as a fare" \
  "$(sim_psql "SELECT coalesce(amount::text,'') FROM rewards.rewards_ledger
     WHERE source_booking_id = $BOOKING AND kind = 'COMPENSATION'")" "25.00"

# 4 — a cash passenger has no card, so the fee rides along (P25).
equals "a cash passenger's fee is recorded as dues" \
  "$(penalty_field "$BOOKING" PASSENGER_NO_SHOW collection_method)" "DUES"
equals "no card charge was attempted" \
  "$(sim_psql "SELECT count(*) FROM payment.payment_attempt WHERE booking_id = $BOOKING")" "0"
equals "the due is outstanding at the fee amount" \
  "$(sim_psql "SELECT amount::text FROM penalty.passenger_due
     WHERE app_user_id = $PASSENGER_APP_USER AND status = 'OUTSTANDING'
     ORDER BY passenger_due_id DESC LIMIT 1")" "49.00"

R="$(call GET "/api/v1/passenger/dues" "$TOKEN")"
equals "P25 reads back the due" "$(status_of "$R")" "200"
# JSON carries 49.0 where the column holds 49.00, so the comparison is on the number, not the text.
equals "P25's total is the outstanding fee" "$(money "$(data "$R" total)")" "49.00"
equals "P25 names the trip the fee came from" \
  "$([ -n "$(data "$R" items.0.trip)" ] && echo true || echo false)" "true"

# 5 — the same trigger again must cost her nothing further.
sim_psql "UPDATE trip.pickup_wait SET resolved_at = NULL, resolution = NULL,
                 expires_at = now() - interval '1 minute'
          WHERE booking_id = $BOOKING" >/dev/null
sleep 20
equals "a repeated trigger assesses nothing further" \
  "$(sim_psql "SELECT count(*) FROM penalty.penalty_assessment
     WHERE booking_id = $BOOKING AND kind = 'PASSENGER_NO_SHOW'")" "1"
equals "and creates no second due" \
  "$(sim_psql "SELECT count(*) FROM penalty.passenger_due
     WHERE app_user_id = $PASSENGER_APP_USER")" "1"

# ── 6: dues at the next checkout, and that they never block one ──────────────────────────────────
NEXT_OCCURRENCE="$(next_occurrence)"
if [ -n "$NEXT_OCCURRENCE" ]; then
  NEXT_BOOKING="$(book_on "$NEXT_OCCURRENCE")"
  check "a passenger with an outstanding fee can still book" \
    "$([ -n "$NEXT_BOOKING" ] && echo true || echo false)"
  if [ -n "$NEXT_BOOKING" ]; then
    equals "the due is carried onto the new booking" \
      "$(sim_psql "SELECT applied_dues_amount::text FROM booking.booking
         WHERE booking_id = $NEXT_BOOKING")" "49.00"
    equals "carrying it does not settle it — capture does" \
      "$(sim_psql "SELECT status FROM penalty.passenger_due
         WHERE settled_booking_id = $NEXT_BOOKING")" "OUTSTANDING"
  fi
else
  skip "no further unbooked occurrence for the dues-at-checkout case"
fi

# ── 7-9: a driver cancels inside the free window, stranding two riders ───────────────────────────
CANCEL_OCCURRENCE="$(next_occurrence)"
CANCEL_TRIP=""
if [ -n "$CANCEL_OCCURRENCE" ]; then
  RIDER_ONE="$(book_on "$CANCEL_OCCURRENCE")"
  RIDER_TWO="$(book_on "$CANCEL_OCCURRENCE")"
  CANCEL_TRIP="$(trip_of "$RIDER_ONE")"
fi

if [ -n "$CANCEL_TRIP" ] && [ -n "${RIDER_TWO:-}" ]; then
  # D31's two riders: 251 + 178 = 429 expected net.
  net_at "$RIDER_ONE" 251.00
  net_at "$RIDER_TWO" 178.00
  # Inside the 12-hour window. Outside it he has done nothing wrong and the seats resell.
  sim_psql "UPDATE routing.route_occurrence SET scheduled_departure_at = now() + interval '3 hours'
            WHERE route_occurrence_id = $CANCEL_OCCURRENCE" >/dev/null
  sim_psql "UPDATE trip.trip_start_window SET departs_at = now() + interval '3 hours',
                   buffer_expires_at = now() + interval '4 hours'
            WHERE trip_id = $CANCEL_TRIP" >/dev/null

  call POST "/api/v1/trips/$CANCEL_TRIP/transition" "$ADMIN_TOKEN" '{"status":"CANCELLED"}' >/dev/null

  PENALTY_ID="$(sim_psql "SELECT coalesce(max(penalty_id)::text,'') FROM penalty.penalty_assessment
    WHERE trip_id = $CANCEL_TRIP AND kind = 'DRIVER_LATE_CANCELLATION'")"
  if [ -n "$PENALTY_ID" ]; then
    equals "20% of LKR 429 expected net is an 86 fee" \
      "$(sim_psql "SELECT fee_amount::text FROM penalty.penalty_assessment
         WHERE penalty_id = $PENALTY_ID")" "86.00"
    equals "43 is shared between the riders and 43 goes to ComiGo" \
      "$(sim_psql "SELECT victim_share::text || '/' || platform_share::text
         FROM penalty.penalty_assessment WHERE penalty_id = $PENALTY_ID")" "43.00/43.00"
    equals "the riders' shares are 22 and 21, the remainder to the first" \
      "$(sim_psql "SELECT string_agg(amount::text, ',' ORDER BY booking_id)
         FROM penalty.penalty_beneficiary WHERE penalty_id = $PENALTY_ID")" "22.00,21.00"
    # 8 — he is never billed. The fee is a negative line against what he earns next.
    equals "the driver's fee is a ledger deduction" \
      "$(sim_psql "SELECT amount::text FROM payment.fare_ledger_entry
         WHERE booking_id = $RIDER_ONE AND entry_type = 'PENALTY_DEDUCTION'")" "-86.00"
    equals "the collection method says so" \
      "$(sim_psql "SELECT collection_method FROM penalty.penalty_assessment
         WHERE penalty_id = $PENALTY_ID")" "EARNINGS_DEDUCTION"
    equals "no card was charged for a driver's penalty" \
      "$(sim_psql "SELECT count(*) FROM payment.payment_attempt
         WHERE booking_id IN ($RIDER_ONE, $RIDER_TWO) AND operation = 'CAPTURE'")" "0"

    # 9 — D31 names the riders. A surname or a contact detail would be a disclosure.
    # The driver surface answers with the caller's own penalties and nobody else's — this admin
    # holds none, so an empty list is the correct answer rather than another driver's ledger.
    R="$(call GET "/api/v1/driver/penalties" "$ADMIN_TOKEN")"
    equals "the driver surface answers only with the caller's own penalties" \
      "$(status_of "$R") $(python3 -c "
import json,sys
try: print(len(json.loads(sys.argv[1])['data']))
except Exception: print('?')
" "$(body_of "$R")")" "200 0"

    # The payload shape is checked where the multi-victim penalty is actually visible.
    R="$(call GET "/api/v1/admin/penalties?kind=DRIVER_LATE_CANCELLATION" "$ADMIN_TOKEN")"
    equals "an admin can read the assessed penalties" "$(status_of "$R")" "200"
    # The property is what the payload cannot contain, not what a particular name looks like: a
    # surname, phone or email on a fee notice would be a disclosure whatever the fee was for.
    BENEFICIARY_SHAPE="$(python3 -c "
import json,sys
try:
    rows = json.loads(sys.argv[1])['data']
    people = [b for r in rows for b in r.get('beneficiaries', [])]
    ok = people and all(set(b) == {'firstName', 'amount'} for b in people)
    # A phone-OTP account carries its number as its display name, so this also asserts that no
    # contact detail reached the payload dressed as a first name.
    ok = ok and not any(c in b['firstName'] for b in people for c in '0123456789@')
    print('true' if ok else 'false')
except Exception:
    print('false')
" "$(body_of "$R")")"
    check "a beneficiary carries a first name and an amount, and nothing else" "$BENEFICIARY_SHAPE"
  else
    skip "no late-cancellation penalty was assessed; the remaining driver checks cannot run"
    skip "driver deduction not checked"
  fi
else
  skip "no occurrence with two riders for the late-cancellation case"
fi

# ── 10-11: disputes ──────────────────────────────────────────────────────────────────────────────
NOSHOW_PENALTY="$(sim_psql "SELECT penalty_id FROM penalty.penalty_assessment
  WHERE booking_id = $BOOKING AND kind = 'PASSENGER_NO_SHOW'")"

R="$(call POST "/api/v1/passenger/penalties/$NOSHOW_PENALTY/dispute" "$ADMIN_TOKEN" \
      '{"reason":"NOT_MINE"}')"
equals "nobody may dispute a penalty that is not theirs" "$(status_of "$R")" "403"

sim_psql "UPDATE penalty.penalty_assessment SET assessed_at = now() - interval '47 hours'
          WHERE penalty_id = $NOSHOW_PENALTY" >/dev/null
R="$(call POST "/api/v1/passenger/penalties/$NOSHOW_PENALTY/dispute" "$TOKEN" \
      '{"reason":"I_WAS_THERE","note":"He drove past the halt."}')"
equals "a dispute at 47 hours is accepted" "$(status_of "$R")" "200"

R="$(call POST "/api/v1/passenger/penalties/$NOSHOW_PENALTY/dispute" "$TOKEN" '{"reason":"AGAIN"}')"
equals "the same argument twice is one dispute" "$(status_of "$R")" "409"
equals "and it says why" "$(error_code "$R")" "PENALTY_ALREADY_DISPUTED"

DISPUTE_ID="$(sim_psql "SELECT penalty_dispute_id FROM penalty.penalty_dispute
  WHERE penalty_id = $NOSHOW_PENALTY ORDER BY penalty_dispute_id DESC LIMIT 1")"

R="$(call GET "/api/v1/admin/penalty-disputes?status=OPEN" "$ADMIN_TOKEN")"
equals "the open dispute reaches the admin queue" "$(status_of "$R")" "200"

R="$(call POST "/api/v1/admin/penalty-disputes/$DISPUTE_ID/decide" "$ADMIN_TOKEN" \
      '{"decision":"REVERSED","note":"Location trail supports the rider."}')"
equals "an admin may reverse it" "$(status_of "$R")" "200"
equals "the penalty is reversed" \
  "$(sim_psql "SELECT status FROM penalty.penalty_assessment WHERE penalty_id = $NOSHOW_PENALTY")" \
  "REVERSED"
equals "a reversed cash fee is waived rather than refunded — nothing was ever taken" \
  "$(sim_psql "SELECT status FROM penalty.passenger_due WHERE penalty_id = $NOSHOW_PENALTY")" \
  "WAIVED"
equals "the decision is audited" \
  "$(sim_psql "SELECT count(*) FROM audit.audit_action
     WHERE action = 'PENALTY_DISPUTE_DECIDED' AND target_id = '$NOSHOW_PENALTY'")" "1"

# The other side of the same window. Aged past 48 hours, the same fee can no longer be argued —
# a fee disputed three months later cannot be checked against a location trail that has been pruned.
sim_psql "UPDATE penalty.penalty_assessment SET assessed_at = now() - interval '49 hours'
          WHERE penalty_id = $NOSHOW_PENALTY" >/dev/null
R="$(call POST "/api/v1/passenger/penalties/$NOSHOW_PENALTY/dispute" "$TOKEN" '{"reason":"TOO_LATE"}')"
equals "a dispute at 49 hours is refused" "$(status_of "$R")" "409"
equals "with DISPUTE_WINDOW_CLOSED" "$(error_code "$R")" "DISPUTE_WINDOW_CLOSED"

# And the driver surface refuses somebody else's penalty before it ever reaches the window check.
AGED="$(sim_psql "SELECT coalesce(max(penalty_id)::text,'') FROM penalty.penalty_assessment
  WHERE kind = 'DRIVER_LATE_CANCELLATION'")"
if [ -n "$AGED" ]; then
  R="$(call POST "/api/v1/driver/penalties/$AGED/dispute" "$ADMIN_TOKEN" '{"reason":"NOT_MINE"}')"
  equals "the driver surface refuses another driver's penalty" "$(status_of "$R")" "403"
else
  skip "no driver penalty for the ownership case on the driver surface"
fi

# ── 12: a missed start costs nothing (D32b) ──────────────────────────────────────────────────────
MISSED="$(sim_psql "SELECT count(*) FROM penalty.penalty_assessment
  WHERE kind = 'DRIVER_MISSED_START' AND fee_amount <> 0")"
equals "no missed start ever carries a fee" "$MISSED" "0"

# ── the properties that must hold over everything this run produced ──────────────────────────────
equals "no beneficiary was paid more than the victim half" \
  "$(sim_psql "SELECT count(*) FROM penalty.penalty_beneficiary b
     JOIN penalty.penalty_assessment p ON p.penalty_id = b.penalty_id
     WHERE b.amount > p.victim_share")" "0"
equals "no penalty was collected from a driver's card" \
  "$(sim_psql "SELECT count(*) FROM penalty.penalty_assessment
     WHERE payer_role = 'DRIVER' AND collection_method IN ('NETTED','CARD_CHARGE')")" "0"

# These provider-backed penalty scenarios require separate card bookings and remain an explicit
# external-runtime matrix. PaymentFacadeImplTest covers all three state branches, while
# verify-charge-timing.sh proves the selected local/real adapter can authorize, capture and void.
skip "netted collection — covered by facade regression; real-provider runtime certification pending"
skip "card-charge collection — covered by facade regression; real-provider runtime certification pending"
skip "dues settled on capture — covered by facade regression; real-provider runtime certification pending"

sim_log "passed: $PASS   failed: $FAIL   skipped: $SKIP"
[ "$FAIL" -eq 0 ] || exit 1
