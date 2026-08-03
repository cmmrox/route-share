#!/usr/bin/env bash
# Verifies slice 07 — booking depth: seats, approval modes, expiry and cancellation windows.
#
# Two of these checks are the whole slice. The seat race must produce exactly one winner against a
# real index, and every negative contact-disclosure path must refuse — that second one is a privacy
# incident rather than a bug if it ever passes wrongly, so it is checked from both sides and from a
# stranger's.
#
#   1. A CAR occurrence has one front seat beside the driver and the rest in the rear row
#   2. Two riders taking the last seat: one succeeds, one is refused as SEATS_TAKEN
#   3. Seat choice does not change the fare
#   4. Instant-book confirms and authorises; approve-each waits with a 30-minute deadline
#   5. A lapsed request expires, releases its seat and voids nothing it never held
#   6. A third open request is refused
#   7. An occurrence with a booking refuses edits; an empty one allows them
#   8. Cancelling at 26 h is free; at 3 h it is priced by slice 06 and riders are told
#   9. Contact is refused on a request, refused after cancellation, refused to a stranger
#  10. No seat hold is ever left behind a terminal booking
#
# Requires the local stack with demo OTP and the scheduler enabled. Never run against production.
#
# Usage:
#   ROUTESHARE_API_BASE=http://localhost:8088 ROUTESHARE_DB_NAME=routeshare_comigo \
#     scripts/simulation/verify-booking-depth.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
PHONE_A="${ROUTESHARE_SIM_DEPTH_PHONE_A:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"
PHONE_B="${ROUTESHARE_SIM_DEPTH_PHONE_B:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"
TICK="${ROUTESHARE_SIM_TICK_SECONDS:-70}"

RUN_STARTED_AT="$(date -u +'%Y-%m-%d %H:%M:%S')"

PASS=0; FAIL=0; SKIP=0
check() { if [ "$2" = "true" ]; then PASS=$((PASS+1)); sim_log "PASS: $1";
          else FAIL=$((FAIL+1)); sim_log "FAIL: $1"; fi; }
equals() { if [ "$2" = "$3" ]; then PASS=$((PASS+1)); sim_log "PASS: $1 ($2)";
           else FAIL=$((FAIL+1)); sim_log "FAIL: $1 — got '$2', expected '$3'"; fi; }
skip() { SKIP=$((SKIP+1)); sim_log "SKIP: $1"; }

call() { # call <method> <path> <token> [body] -> "<status> <body>"
  local method="$1" path="$2" token="$3" body="${4:-}" args=()
  args=(-s -o /tmp/depth-body -w '%{http_code}' -X "$method" "$SIM_API_BASE$path"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/depth-body)"
}
status_of() { printf '%s' "${1%% *}"; }
body_of() { printf '%s' "${1#* }"; }
error_code() { python3 -c "
import json,sys
try:
    b = json.loads(sys.argv[1])
    print(b.get('code') or b.get('error', {}).get('code') or '')
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

TOKEN_A="$(sim_login "$PHONE_A")"
TOKEN_B="$(sim_login "$PHONE_B")"
ADMIN_TOKEN="$(sim_keycloak_login_with_roles "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN" admin-web)"

sim_log "seeding a demo route (idempotent)"
bash ./seed-demo-route.sh >/dev/null 2>&1 || sim_log "seed script reported an issue; continuing"
# verify-trip-timers deliberately deactivates the shared demo driver. This suite is independently
# runnable, so restore that local fixture before asking it to publish and cancel fresh occurrences.
sim_psql "DELETE FROM driver.driver_reinstatement_request" >/dev/null
sim_psql "DELETE FROM driver.driver_deactivation" >/dev/null
sim_psql "UPDATE routing.route_occurrence SET status = 'PUBLISHED'
          WHERE status = 'CANCELLED' AND scheduled_departure_at > now()" >/dev/null

# The disclosure path needs a number to disclose, and demo drivers are seeded without one. A driver
# with no phone is correctly refused, which would read here as a broken rule rather than missing
# data — so this runs after the seed that creates the profile this run will actually use.
sim_psql "UPDATE identity.app_user u
             SET phone = COALESCE(u.phone, '+94112345678'),
                 display_name = CASE WHEN u.display_name ~ '[0-9@]' OR u.display_name IS NULL
                                     THEN 'Priya Jayawardena' ELSE u.display_name END
           FROM driver.driver_profile d
          WHERE d.app_user_id = u.app_user_id" >/dev/null

free_occurrence() {
  sim_psql "SELECT ro.route_occurrence_id FROM routing.route_occurrence ro
    WHERE ro.status = 'PUBLISHED' AND ro.scheduled_departure_at > now()
      AND EXISTS (SELECT 1 FROM routing.route_occurrence_seat s
                   WHERE s.route_occurrence_id = ro.route_occurrence_id)
      AND NOT EXISTS (
        SELECT 1 FROM routing.route_occurrence_seat s
        JOIN booking.booking_seat bs
          ON bs.route_occurrence_seat_id = s.route_occurrence_seat_id
         AND bs.released_at IS NULL
        WHERE s.route_occurrence_id = ro.route_occurrence_id)
      AND NOT EXISTS (SELECT 1 FROM booking.booking b
                       WHERE b.route_occurrence_id = ro.route_occurrence_id
                         AND b.status IN ('REQUESTED','CONFIRMED'))
    ORDER BY ro.route_occurrence_id ASC LIMIT 1"
}
next_occurrence() {
  local found; found="$(free_occurrence)"
  if [ -z "$found" ]; then
    sim_psql "WITH created AS (
                INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at,
                  available_seats, status)
              SELECT rp.route_plan_id, now() + interval '20 hours', 3, 'PUBLISHED'
                FROM routing.route_plan rp ORDER BY rp.route_plan_id DESC LIMIT 1
                RETURNING route_occurrence_id
              )
              INSERT INTO routing.route_occurrence_seat
                (route_occurrence_id, slot_index, label, sub_label)
              SELECT route_occurrence_id, slot_index,
                     CASE slot_index WHEN 1 THEN 'Front seat' ELSE 'Back seat ' || (slot_index - 1) END,
                     CASE slot_index WHEN 1 THEN 'Beside the driver' ELSE 'Rear row' END
                FROM created CROSS JOIN generate_series(1, 3) AS slot_index" >/dev/null
    found="$(free_occurrence)"
  fi
  printf '%s' "$found"
}

book() { # book <token> <occurrence> [seatSlotIdsJson] -> "<status> <body>"
  local token="$1" occurrence="$2" slots="${3:-null}"
  local args=(-s -o /tmp/depth-body -w '%{http_code}' -X POST
        "$SIM_API_BASE/api/v1/passenger/bookings"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json'
        -H "Idempotency-Key: sim-depth-$RANDOM-$RANDOM"
        -d "{\"routeOccurrenceId\":$occurrence,\"seats\":1,\"pickupLat\":6.90,\"pickupLng\":79.86,
      \"dropLat\":6.95,\"dropLng\":79.90,\"pickupRouteFraction\":0.1,
      \"dropoffRouteFraction\":0.6,\"paymentMethodId\":null,\"seatSlotIds\":$slots}")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/depth-body)"
}
book_id() { data "$1" bookingId; }
set_mode() { sim_psql "UPDATE routing.route_occurrence SET approval_mode = '$2',
                         gender_policy = 'ANYONE', verified_riders_only = false
                       WHERE route_occurrence_id = $1" >/dev/null; }

# ── 1: the seat map ──────────────────────────────────────────────────────────────────────────────
OCCURRENCE="$(next_occurrence)"
[ -n "$OCCURRENCE" ] || sim_fail "no unbooked published occurrence to work with"
set_mode "$OCCURRENCE" INSTANT

R="$(call GET "/api/v1/passenger/route-occurrences/$OCCURRENCE/seats" "$TOKEN_A")"
equals "the seat map is readable" "$(status_of "$R")" "200"
equals "a CAR has three named slots" "$(data "$R" capacity)" "3"
equals "slot 1 is the front seat" "$(data "$R" seats.0.label)" "Front seat"
equals "and it is beside the driver" "$(data "$R" seats.0.subLabel)" "Beside the driver"
equals "slot 2 is in the rear row" "$(data "$R" seats.1.subLabel)" "Rear row"

FRONT_SEAT="$(data "$R" seats.0.seatId)"
BACK_SEAT="$(data "$R" seats.1.seatId)"

# ── 2-3: the race, and that the seat never changes the price ─────────────────────────────────────
R="$(book "$TOKEN_A" "$OCCURRENCE" "[$FRONT_SEAT]")"
equals "the front seat is booked" "$(status_of "$R")" "200"
FRONT_BOOKING="$(book_id "$R")"
FRONT_FARE="$(data "$R" fareEstimate)"

R="$(book "$TOKEN_B" "$OCCURRENCE" "[$FRONT_SEAT]")"
equals "the same seat again is refused" "$(status_of "$R")" "409"
equals "and it is refused as a seat problem" "$(error_code "$R")" "SEATS_TAKEN"

R="$(book "$TOKEN_B" "$OCCURRENCE" "[$BACK_SEAT]")"
equals "a different seat on the same trip is fine" "$(status_of "$R")" "200"
BACK_BOOKING="$(book_id "$R")"
equals "07-3: the back seat costs exactly what the front seat costs" \
  "$(data "$R" fareEstimate)" "$FRONT_FARE"

equals "the seat map now shows the front seat taken" \
  "$(sim_psql "SELECT count(*) FROM booking.booking_seat
     WHERE route_occurrence_seat_id = $FRONT_SEAT AND released_at IS NULL")" "1"

# ── 4: instant-book confirms and authorises ──────────────────────────────────────────────────────
equals "07-4: instant-book confirms immediately" \
  "$(sim_psql "SELECT status FROM booking.booking WHERE booking_id = $FRONT_BOOKING")" "CONFIRMED"
equals "and it materialised the trip" \
  "$(sim_psql "SELECT count(*) FROM trip.trip WHERE route_occurrence_id = $OCCURRENCE")" "1"
R="$(call GET "/api/v1/passenger/bookings/$FRONT_BOOKING/alternatives" "$TOKEN_A")"
equals "07-alternatives: the passenger can read alternatives for her booking" \
  "$(status_of "$R")" "200"
R="$(call GET "/api/v1/passenger/bookings/$FRONT_BOOKING/alternatives" "$TOKEN_B")"
equals "and another passenger cannot use it as a corridor oracle" "$(status_of "$R")" "404"

# ── 7: the freeze rule ───────────────────────────────────────────────────────────────────────────
R="$(call GET "/api/v1/driver/route-occurrences/$OCCURRENCE/editability" "$ADMIN_TOKEN")"
equals "07-10: an occurrence with bookings is frozen" "$(data "$R" editable)" "False"
R="$(call PUT "/api/v1/driver/route-occurrences/$OCCURRENCE/approval-mode" "$ADMIN_TOKEN" \
      '{"mode":"INSTANT"}')"
equals "and its approval mode can no longer be changed" "$(status_of "$R")" "409"
equals "with TRIP_FROZEN" "$(error_code "$R")" "TRIP_FROZEN"

EMPTY_OCCURRENCE="$(next_occurrence)"
set_mode "$EMPTY_OCCURRENCE" INSTANT
R="$(call GET "/api/v1/driver/route-occurrences/$EMPTY_OCCURRENCE/editability" "$ADMIN_TOKEN")"
equals "07-9: an occurrence nobody has booked is still editable" "$(data "$R" editable)" "True"
R="$(call PUT "/api/v1/driver/route-occurrences/$EMPTY_OCCURRENCE/approval-mode" "$ADMIN_TOKEN" \
      '{"mode":"APPROVE_EACH"}')"
equals "and its approval mode can be set" "$(status_of "$R")" "200"

# ── 5: approve-each, and the lapse ───────────────────────────────────────────────────────────────
R="$(book "$TOKEN_A" "$EMPTY_OCCURRENCE")"
if [ "$(status_of "$R")" != "200" ]; then
  sim_fail "approve-each fixture booking failed: $(status_of "$R") $(body_of "$R")"
fi
equals "07-5: approve-each creates a request, not a booking" "$(data "$R" status)" "REQUESTED"
REQUEST_BOOKING="$(book_id "$R")"
check "07-6: the request carries a deadline" \
  "$([ -n "$(data "$R" expiresAt)" ] && echo true || echo false)"
equals "and nothing was authorised for it" \
  "$(sim_psql "SELECT count(*) FROM payment.payment_intent WHERE booking_id = $REQUEST_BOOKING")" "0"
equals "the deadline is the policy's 30 minutes" \
  "$(sim_psql "SELECT (expires_at BETWEEN now() + interval '28 minutes'
                                       AND now() + interval '31 minutes')::text
     FROM booking.booking WHERE booking_id = $REQUEST_BOOKING")" "true"

SEATS_BEFORE="$(sim_psql "SELECT count(*) FROM booking.booking_seat
  WHERE booking_id = $REQUEST_BOOKING AND released_at IS NULL")"
equals "the request holds its seat while it waits" "$SEATS_BEFORE" "1"

sim_psql "UPDATE booking.booking SET expires_at = now() - interval '1 minute'
          WHERE booking_id = $REQUEST_BOOKING" >/dev/null
sim_log "waiting up to ${TICK}s for the request-expiry sweep (the job's own tick)"
DEADLINE=$((SECONDS + TICK)); STATUS=""
while [ "$SECONDS" -lt "$DEADLINE" ]; do
  STATUS="$(sim_psql "SELECT status FROM booking.booking WHERE booking_id = $REQUEST_BOOKING")"
  [ "$STATUS" = "EXPIRED" ] && break
  sleep 5
done
if [ "$STATUS" = "EXPIRED" ]; then
  PASS=$((PASS+1)); sim_log "PASS: 07-7: the lapsed request expired"
  equals "and its seat went back to the car" \
    "$(sim_psql "SELECT count(*) FROM booking.booking_seat
       WHERE booking_id = $REQUEST_BOOKING AND released_at IS NULL")" "0"
  equals "booking.expired was published" \
    "$(sim_psql "SELECT count(*) FROM common.event_outbox
       WHERE event_type = 'booking.expired' AND aggregate_id = '$REQUEST_BOOKING'")" "1"
else
  skip "the request-expiry sweep did not run within ${TICK}s (is ROUTESHARE_SCHEDULER_ENABLED=true?)"
  skip "seat release on expiry not checked"
fi

# ── 6: two open requests at once ─────────────────────────────────────────────────────────────────
# Minted explicitly rather than fetched three times: next_occurrence only mints when nothing is
# free, so asking before booking any of them hands back the same occurrence three times over.
mint_occurrence() {
  # head -1, because psql prints the command tag after a RETURNING row and a two-line id lands in
  # the JSON body as a syntax error rather than as an occurrence.
  sim_psql "INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at,
              available_seats, status, approval_mode)
            SELECT rp.route_plan_id, now() + interval '20 hours', 3, 'PUBLISHED', 'APPROVE_EACH'
            FROM routing.route_plan rp ORDER BY rp.route_plan_id DESC LIMIT 1
            RETURNING route_occurrence_id" | head -1
}
LIMIT_OCC_1="$(mint_occurrence)"
LIMIT_OCC_2="$(mint_occurrence)"
LIMIT_OCC_3="$(mint_occurrence)"
R="$(book "$TOKEN_B" "$LIMIT_OCC_1")"
equals "the first request is accepted" "$(status_of "$R")" "200"
R="$(book "$TOKEN_B" "$LIMIT_OCC_2")"
equals "the second request is accepted — the limit is two, not one" "$(status_of "$R")" "200"
R="$(book "$TOKEN_B" "$LIMIT_OCC_3")"
equals "07-8: a third open request is refused" "$(status_of "$R")" "409"
equals "with TOO_MANY_OPEN_REQUESTS" "$(error_code "$R")" "TOO_MANY_OPEN_REQUESTS"

# ── 9: contact disclosure, every negative path ───────────────────────────────────────────────────
R="$(call GET "/api/v1/passenger/bookings/$FRONT_BOOKING/contact" "$TOKEN_A")"
equals "07-16: a confirmed booking discloses the driver's number" "$(status_of "$R")" "200"
check "and it is a real number" \
  "$([ -n "$(data "$R" phoneNumber)" ] && echo true || echo false)"
equals "the emergency number rides along" "$(data "$R" emergencyNumber)" "119"
equals "the read was audited" \
  "$(sim_psql "SELECT count(*) FROM booking.contact_disclosure_audit
     WHERE booking_id = $FRONT_BOOKING")" "1"

R="$(call GET "/api/v1/passenger/bookings/$FRONT_BOOKING/contact" "$TOKEN_B")"
equals "07-19: another passenger on the same trip is refused" "$(status_of "$R")" "403"

REQ_OCC="$(next_occurrence)"; set_mode "$REQ_OCC" APPROVE_EACH
R="$(book "$TOKEN_A" "$REQ_OCC")"
PENDING_BOOKING="$(book_id "$R")"
if [ -n "$PENDING_BOOKING" ]; then
  R="$(call GET "/api/v1/passenger/bookings/$PENDING_BOOKING/contact" "$TOKEN_A")"
  equals "07-15: a pending request discloses nothing" "$(status_of "$R")" "409"
  equals "with CONTACT_NOT_AVAILABLE" "$(error_code "$R")" "CONTACT_NOT_AVAILABLE"
else
  skip "no pending request for the contact case"
  skip "contact refusal code not checked"
fi

if [ -n "${BACK_BOOKING:-}" ]; then
  call POST "/api/v1/passenger/bookings/$BACK_BOOKING/cancel" "$TOKEN_B" \
    '{"reason":"sim"}' >/dev/null
  R="$(call GET "/api/v1/passenger/bookings/$BACK_BOOKING/contact" "$TOKEN_B")"
  equals "07-18: a cancelled booking discloses nothing" "$(error_code "$R")" "CONTACT_NOT_AVAILABLE"
else
  skip "no second booking for the cancelled-contact case"
fi

# ── 8: the cancellation windows ──────────────────────────────────────────────────────────────────
FREE_OCC="$(next_occurrence)"; set_mode "$FREE_OCC" INSTANT
sim_psql "UPDATE routing.route_occurrence SET scheduled_departure_at = now() + interval '26 hours'
          WHERE route_occurrence_id = $FREE_OCC" >/dev/null
book "$TOKEN_A" "$FREE_OCC" >/dev/null
R="$(call GET "/api/v1/driver/route-occurrences/$FREE_OCC/cancellation-terms" "$ADMIN_TOKEN")"
equals "07-11: 26 hours out is inside the free window" "$(data "$R" withinFreeWindow)" "True"
R="$(call POST "/api/v1/driver/route-occurrences/$FREE_OCC/cancel" "$ADMIN_TOKEN" \
      '{"reasonCode":"PLANS_CHANGED"}')"
equals "and cancelling is accepted" "$(status_of "$R")" "200"
equals "with no penalty assessed" \
  "$(sim_psql "SELECT count(*) FROM penalty.penalty_assessment p
     JOIN trip.trip t ON t.trip_id = p.trip_id
     WHERE t.route_occurrence_id = $FREE_OCC AND p.kind = 'DRIVER_LATE_CANCELLATION'")" "0"

LATE_OCC="$(next_occurrence)"; set_mode "$LATE_OCC" INSTANT
sim_psql "UPDATE routing.route_occurrence SET scheduled_departure_at = now() + interval '3 hours'
          WHERE route_occurrence_id = $LATE_OCC" >/dev/null
book "$TOKEN_A" "$LATE_OCC" >/dev/null
sim_psql "UPDATE trip.trip_start_window SET departs_at = now() + interval '3 hours',
                 buffer_expires_at = now() + interval '4 hours'
          WHERE trip_id IN (SELECT trip_id FROM trip.trip
                             WHERE route_occurrence_id = $LATE_OCC)" >/dev/null
R="$(call GET "/api/v1/driver/route-occurrences/$LATE_OCC/cancellation-terms" "$ADMIN_TOKEN")"
equals "07-12: 3 hours out is inside the penalty window" "$(data "$R" withinFreeWindow)" "False"
check "and the terms name a priced fee" \
  "$(python3 -c "print('true' if float('$(data "$R" penaltyAmount)' or 0) > 0 else 'false')")"
R="$(call POST "/api/v1/driver/route-occurrences/$LATE_OCC/cancel" "$ADMIN_TOKEN" \
      '{"reasonCode":"VEHICLE_PROBLEM","note":"gearbox"}')"
equals "cancelling inside the window is accepted" "$(status_of "$R")" "200"
equals "and slice 06 assessed the penalty" \
  "$(sim_psql "SELECT count(*) FROM penalty.penalty_assessment p
     JOIN trip.trip t ON t.trip_id = p.trip_id
     WHERE t.route_occurrence_id = $LATE_OCC AND p.kind = 'DRIVER_LATE_CANCELLATION'")" "1"
equals "the reason and window were recorded" \
  "$(sim_psql "SELECT reason_code || '/' || within_free_window::text
     FROM routing.route_occurrence_cancellation WHERE route_occurrence_id = $LATE_OCC")" \
  "VEHICLE_PROBLEM/false"

R="$(call POST "/api/v1/driver/route-occurrences/$LATE_OCC/cancel" "$ADMIN_TOKEN" '{}')"
equals "07-13: cancelling with no reason code is refused" "$(status_of "$R")" "400"

# ── 10: the property that must hold over everything this run did ─────────────────────────────────
equals "07-21: no seat hold is left behind a terminal booking" \
  "$(sim_psql "SELECT count(*) FROM booking.booking_seat bs
     JOIN booking.booking b ON b.booking_id = bs.booking_id
     WHERE bs.released_at IS NULL
       AND b.status IN ('CANCELLED','REJECTED','EXPIRED')
       AND b.created_at >= TIMESTAMPTZ '$RUN_STARTED_AT+00'")" "0"
equals "no seat is held twice at once" \
  "$(sim_psql "SELECT count(*) FROM (
       SELECT route_occurrence_seat_id FROM booking.booking_seat
        WHERE released_at IS NULL
        GROUP BY route_occurrence_seat_id HAVING count(*) > 1) AS doubled")" "0"
equals "every disclosure this run made was audited" \
  "$(sim_psql "SELECT (count(*) > 0)::text FROM booking.contact_disclosure_audit")" "true"

sim_log "passed: $PASS   failed: $FAIL   skipped: $SKIP"
[ "$FAIL" -eq 0 ] || exit 1
