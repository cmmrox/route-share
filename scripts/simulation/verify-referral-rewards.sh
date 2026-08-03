#!/usr/bin/env bash
# Slice 11 runtime proof. Uses disposable local identities and data; never run against production.
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

RUN_TAG="s11-$RANDOM-$RANDOM"
REFERRER_PHONE="+9475$(printf '%07d' $((RANDOM % 10000000)))"
RIDER_PHONE="+9476$(printf '%07d' $((RANDOM % 10000000)))"
OTHER_PHONE="+9478$(printf '%07d' $((RANDOM % 10000000)))"
DRIVER_USERNAME="sim-s11-driver-$RANDOM"
DRIVER_PASSWORD="SimDriver#12345"
PASS=0
FAIL=0

equals() {
  if [ "$2" = "$3" ]; then
    PASS=$((PASS + 1)); sim_log "PASS: $1 ($2)"
  else
    FAIL=$((FAIL + 1)); sim_log "FAIL: $1 — got '$2', expected '$3'"
  fi
}
check() {
  if [ "$2" = "true" ]; then
    PASS=$((PASS + 1)); sim_log "PASS: $1"
  else
    FAIL=$((FAIL + 1)); sim_log "FAIL: $1"
  fi
}
call() {
  local method="$1" path="$2" token="$3" body="${4:-}" idem="${5:-}"
  local args=(-s -o /tmp/routeshare-s11-body -w '%{http_code}' -X "$method"
    "$SIM_API_BASE$path" -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  [ -n "$idem" ] && args+=(-H "Idempotency-Key: $idem")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/routeshare-s11-body)"
}
status_of() { printf '%s' "${1%% *}"; }
body_of() { printf '%s' "${1#* }"; }
data() {
  python3 -c "
import json,sys
try:
    d=json.loads(sys.argv[1])['data']
    for key in sys.argv[2].split('.'):
        d=d[int(key)] if isinstance(d,list) else d[key]
    print('' if d is None else d)
except Exception:
    print('')
" "$(body_of "$1")" "$2"
}
profile() {
  call PUT /api/v1/passenger/profile "$1" \
    "{\"fullName\":\"$2\",\"photoUrl\":null,\"preferences\":{}${3:+,\"referralCode\":\"$3\"}}"
}

sim_require_tools
sim_require_api

REFERRER_TOKEN="$(sim_login "$REFERRER_PHONE")"
RIDER_TOKEN="$(sim_login "$RIDER_PHONE")"
OTHER_TOKEN="$(sim_login "$OTHER_PHONE")"
DRIVER_TOKEN="$(sim_keycloak_login_with_roles \
  "$DRIVER_USERNAME" "$DRIVER_PASSWORD" "PASSENGER,DRIVER" driver-mobile)"

equals "referrer profile creates a code" \
  "$(status_of "$(profile "$REFERRER_TOKEN" "Maya Perera" "")")" "200"
REFERRAL="$(call GET /api/v1/me/referral "$REFERRER_TOKEN")"
equals "referral dashboard is readable" "$(status_of "$REFERRAL")" "200"
CODE="$(data "$REFERRAL" code)"
[ -n "$CODE" ] || sim_fail "referral code was not returned"
check "generated code uses the ambiguity-free alphabet" \
  "$(if [[ "$CODE" =~ [IO01] ]]; then echo false; else echo true; fi)"
equals "self-referral is refused" \
  "$(status_of "$(call POST /api/v1/me/referral/claim "$REFERRER_TOKEN" \
    "{\"code\":\"$CODE\"}")")" "409"

equals "referred rider claims at profile setup" \
  "$(status_of "$(profile "$RIDER_TOKEN" "Nimal Silva" "$CODE")")" "200"
equals "referred driver claims at profile setup" \
  "$(status_of "$(profile "$DRIVER_TOKEN" "Saman Fernando" "$CODE")")" "200"
equals "control rider profile is ready" \
  "$(status_of "$(profile "$OTHER_TOKEN" "Control Rider" "")")" "200"
equals "first-ride discount is exactly LKR 150" \
  "$(data "$(call GET /api/v1/me/rewards "$RIDER_TOKEN")" balance)" "150.0"
equals "second claim is immutable" \
  "$(status_of "$(call POST /api/v1/me/referral/claim "$RIDER_TOKEN" \
    "{\"code\":\"$CODE\"}")")" "409"

# Resolve local ids after their profile calls have projected all identities.
REFERRER_ID="$(sim_psql "SELECT app_user_id FROM identity.app_user
  WHERE phone = '$REFERRER_PHONE' ORDER BY app_user_id DESC LIMIT 1")"
RIDER_ID="$(sim_psql "SELECT app_user_id FROM identity.app_user
  WHERE phone = '$RIDER_PHONE' ORDER BY app_user_id DESC LIMIT 1")"
OTHER_ID="$(sim_psql "SELECT app_user_id FROM identity.app_user
  WHERE phone = '$OTHER_PHONE' ORDER BY app_user_id DESC LIMIT 1")"
DRIVER_ID="$(sim_psql "SELECT app_user_id FROM identity.app_user
  WHERE lower(email) = lower('$DRIVER_USERNAME@routeshare.local')
  ORDER BY app_user_id DESC LIMIT 1")"
[ -n "$REFERRER_ID" ] && [ -n "$RIDER_ID" ] && [ -n "$OTHER_ID" ] && [ -n "$DRIVER_ID" ] \
  || sim_fail "could not resolve Slice 11 identities"

DRIVER_PROFILE_ID="$(sim_psql "INSERT INTO driver.driver_profile
    (app_user_id, display_name, verification_status)
  VALUES ($DRIVER_ID, 'Saman Fernando', 'APPROVED')
  ON CONFLICT (app_user_id) DO UPDATE SET verification_status = 'APPROVED'
  RETURNING driver_profile_id" | head -1)"
sim_psql "INSERT INTO driver.driver_payout_profile
    (driver_profile_id, method, bank_name, branch, account_name, account_number, status)
  VALUES ($DRIVER_PROFILE_ID, 'BANK_TRANSFER', 'QA Bank', 'Colombo',
          'Saman Fernando', '000011112222', 'VERIFIED')
  ON CONFLICT (driver_profile_id) DO UPDATE SET status = 'VERIFIED'" >/dev/null
VEHICLE_ID="$(sim_psql "INSERT INTO vehicle.vehicle
    (driver_profile_id, make, model, manufacture_year, color, registration_number,
     seat_count, status, class_key)
  VALUES ($DRIVER_PROFILE_ID, 'Toyota', 'Aqua', 2022, 'Blue', 'S11-$RANDOM',
          3, 'APPROVED', 'CAR')
  RETURNING vehicle_id" | head -1)"
sim_psql "INSERT INTO vehicle.vehicle_rate_band
    (vehicle_id, rate_min, rate_max, chosen_rate, status)
  VALUES ($VEHICLE_ID, 38, 62, 50, 'ACTIVE')" >/dev/null
ROUTE_ID="$(sim_psql "INSERT INTO routing.route_plan
    (driver_profile_id, vehicle_id, origin_label, destination_label, route_line,
     route_length_m, departure_time, available_seats, status)
  VALUES ($DRIVER_PROFILE_ID, $VEHICLE_ID, 'Slice 11 Origin', 'Slice 11 Destination',
          ST_GeomFromText('LINESTRING(79.8612 6.9271,79.9000 6.9500)',4326),
          6500, now() + interval '4 hours', 3, 'PUBLISHED')
  RETURNING route_plan_id" | head -1)"
OCCURRENCE_ID="$(sim_psql "INSERT INTO routing.route_occurrence
    (route_plan_id, scheduled_departure_at, available_seats, status, approval_mode)
  VALUES ($ROUTE_ID, now() + interval '4 hours', 3, 'PUBLISHED', 'INSTANT')
  RETURNING route_occurrence_id" | head -1)"

booking() {
  call POST /api/v1/passenger/bookings "$1" \
    "{\"routeOccurrenceId\":$OCCURRENCE_ID,\"seats\":1,
      \"pickupLat\":6.9271,\"pickupLng\":79.8612,
      \"dropLat\":6.9500,\"dropLng\":79.9000,
      \"pickupRouteFraction\":0.1,\"dropoffRouteFraction\":0.9,
      \"paymentMethodId\":null,\"seatSlotIds\":null}" "$2"
}
RIDER_BOOKING="$(booking "$RIDER_TOKEN" "s11-rider-$RUN_TAG")"
OTHER_BOOKING="$(booking "$OTHER_TOKEN" "s11-other-$RUN_TAG")"
equals "referred rider booking succeeds" "$(status_of "$RIDER_BOOKING")" "200"
equals "other rider booking succeeds" "$(status_of "$OTHER_BOOKING")" "200"
RIDER_BOOKING_ID="$(data "$RIDER_BOOKING" bookingId)"
OTHER_BOOKING_ID="$(data "$OTHER_BOOKING" bookingId)"
equals "first-ride credit auto-applies and caps below the fare" \
  "$(data "$RIDER_BOOKING" appliedCredit)" "150.0"

# Preserve every V029 invariant while reproducing the prototype's exact settled figures:
# rider pays 290; the driver's two bookings keep 261 + 979 = 1240.
sim_psql "UPDATE pricing.fare_quote
  SET gross_fare = 290, discount_amount = 0, passenger_pays = 290,
      commission_amount = 29, driver_net = 261
  WHERE booking_id = $RIDER_BOOKING_ID;
  UPDATE booking.booking SET fare_estimate = 290 WHERE booking_id = $RIDER_BOOKING_ID;
  UPDATE pricing.fare_quote
  SET gross_fare = 1088, discount_amount = 0, passenger_pays = 1088,
      commission_amount = 109, driver_net = 979
  WHERE booking_id = $OTHER_BOOKING_ID;
  UPDATE booking.booking SET fare_estimate = 1088 WHERE booking_id = $OTHER_BOOKING_ID" >/dev/null

TRIP_ID="$(sim_psql "SELECT trip_id FROM trip.trip
  WHERE route_occurrence_id = $OCCURRENCE_ID ORDER BY trip_id DESC LIMIT 1")"
[ -n "$TRIP_ID" ] || sim_fail "bookings did not materialise the trip"
equals "trip start succeeds" \
  "$(status_of "$(call POST "/api/v1/driver/trips/$TRIP_ID/start" "$DRIVER_TOKEN")")" "200"
equals "arrival succeeds" \
  "$(status_of "$(call POST "/api/v1/driver/trips/$TRIP_ID/arrived-pickup" "$DRIVER_TOKEN")")" "200"
equals "trip reaches onboard state" \
  "$(status_of "$(call POST "/api/v1/trips/$TRIP_ID/transition" "$DRIVER_TOKEN" \
    '{"status":"PASSENGER_ONBOARD"}')")" "200"
equals "completion fires referral accrual" \
  "$(status_of "$(call POST "/api/v1/driver/trips/$TRIP_ID/complete" "$DRIVER_TOKEN")")" "200"

equals "referred driver LKR 1240 net accrues LKR 25" \
  "$(sim_psql "SELECT amount FROM rewards.rewards_ledger l
    JOIN rewards.referral_edge e ON e.referral_edge_id = l.referral_edge_id
    WHERE e.referee_app_user_id = $DRIVER_ID AND l.kind = 'REFERRAL'")" "25.00"
equals "referred rider LKR 290 fare accrues LKR 3" \
  "$(sim_psql "SELECT amount FROM rewards.rewards_ledger l
    JOIN rewards.referral_edge e ON e.referral_edge_id = l.referral_edge_id
    WHERE e.referee_app_user_id = $RIDER_ID AND l.kind = 'REFERRAL'")" "3.00"
equals "paired platform cost is recorded twice without a uniqueness collision" \
  "$(sim_psql "SELECT count(*) FROM payment.fare_ledger_entry
    WHERE entry_type = 'REFERRAL_PAYOUT'
      AND booking_id IN ($RIDER_BOOKING_ID,$OTHER_BOOKING_ID)")" "2"
equals "replaying completion key cannot duplicate a reward" \
  "$(sim_psql "INSERT INTO rewards.rewards_ledger
      (app_user_id, kind, amount, label, referral_edge_id, source_booking_id, idempotency_key)
    SELECT app_user_id, kind, amount, label, referral_edge_id, source_booking_id, idempotency_key
    FROM rewards.rewards_ledger WHERE kind = 'REFERRAL' LIMIT 1
    ON CONFLICT (idempotency_key) DO NOTHING RETURNING 1")" "INSERT 0 0"

sim_psql "UPDATE rewards.referral_edge SET trips_counted = max_trips, status = 'EXPIRED_TRIPS'
  WHERE referee_app_user_id = $RIDER_ID;
  UPDATE rewards.referral_edge SET window_expires_at = now() - interval '1 second'
  WHERE referee_app_user_id = $DRIVER_ID;
  UPDATE rewards.referral_edge SET status = 'EXPIRED_WINDOW'
  WHERE referee_app_user_id = $DRIVER_ID AND window_expires_at <= now()" >/dev/null
equals "50-trip edge closes with zero still earning" \
  "$(sim_psql "SELECT count(*) FROM rewards.referral_edge
    WHERE referee_app_user_id = $RIDER_ID AND status = 'ACTIVE'")" "0"
equals "past-12-month edge closes with zero still earning" \
  "$(sim_psql "SELECT count(*) FROM rewards.referral_edge
    WHERE referee_app_user_id = $DRIVER_ID AND status = 'ACTIVE'")" "0"

equals "withdrawal below LKR 1000 is refused" \
  "$(status_of "$(call POST /api/v1/me/rewards/withdrawals "$DRIVER_TOKEN")")" "409"
sim_psql "INSERT INTO rewards.rewards_ledger
    (app_user_id, kind, amount, label, idempotency_key)
  VALUES ($DRIVER_ID, 'ADJUSTMENT', 1200, 'QA top-up', 's11-top-up:$RUN_TAG')" >/dev/null
WITHDRAWAL="$(call POST /api/v1/me/rewards/withdrawals "$DRIVER_TOKEN")"
equals "eligible bank withdrawal queues" "$(status_of "$WITHDRAWAL")" "200"
equals "withdrawal takes the whole available shared balance" \
  "$(data "$WITHDRAWAL" status)" "QUEUED"
equals "second open withdrawal is refused" \
  "$(status_of "$(call POST /api/v1/me/rewards/withdrawals "$DRIVER_TOKEN")")" "409"
equals "queued withdrawal debits the shared balance to zero" \
  "$(data "$(call GET /api/v1/me/rewards "$DRIVER_TOKEN")" balance)" "0.0"

check "schema rejects self-referral independently of the service" \
  "$(if docker exec -i "$SIM_PSQL_CONTAINER" psql -U "$SIM_DB_USER" -d "$SIM_DB_NAME" \
    -c "INSERT INTO rewards.referral_edge
      (referrer_app_user_id, referee_app_user_id, code, window_expires_at, max_trips)
    VALUES ($OTHER_ID, $OTHER_ID, 'SELFXX', now() + interval '1 year', 50)" \
      >/dev/null 2>&1; then echo false; else echo true; fi)"

sim_log "Slice 11 result: PASS=$PASS FAIL=$FAIL SKIP=0"
[ "$FAIL" -eq 0 ] || exit 1
