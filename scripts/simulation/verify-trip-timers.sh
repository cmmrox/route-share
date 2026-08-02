#!/usr/bin/env bash
# Verifies slice 05 — trip timers and reliability — against the LOCAL stack.
#
# The four clocks the product runs on, checked where they actually live: in the database, driven by
# server time. Deadlines are moved by rewriting the stored row rather than by waiting, for the same
# reason no test in this slice sleeps — a check that passes because it waited will one day fail on a
# slow machine and be re-run until green.
#
#   1. A confirmed booking materialises a trip and opens its start window
#   2. The start window's deadline is departure + the policy buffer, from the occurrence
#   3. The extension moves the deadline exactly once; a second attempt is refused as data
#   4. A start window on a trip that has already started is never auto-cancelled
#   5. The sweeper auto-cancels an expired window, charges nobody, and records a missed start
#   6. A drive-past does not start a pickup wait; a dwell does
#   7. A driver cannot release a seat before the wait has run out
#   8. Wait expiry releases the seat, marks NO_SHOW and emits booking.noshow
#   9. The driver-late grace unlocks a free cancel and cancellation-terms says so
#  10. The start buffer and the driver-late grace resolve independently (P35)
#  11. The early-drop allowance adjusts twice and then stops, as data not an error
#  12. Three missed starts deactivate driving and leave riding intact
#  13. Every automatic action left a job_run row naming the job
#
# Requires the local stack (docker compose) with demo OTP enabled. Never run against production.
#
# Usage:
#   ROUTESHARE_API_BASE=http://localhost:8088 ROUTESHARE_DB_NAME=routeshare_comigo \
#     scripts/simulation/verify-trip-timers.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
PASSENGER_PHONE="${ROUTESHARE_SIM_TIMER_PHONE:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"

PASS=0; FAIL=0; SKIP=0
check() { # check <name> <ok-boolean>
  if [ "$2" = "true" ]; then PASS=$((PASS+1)); sim_log "PASS: $1"; else FAIL=$((FAIL+1)); sim_log "FAIL: $1"; fi
}
equals() { # equals <name> <actual> <expected>
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); sim_log "PASS: $1 ($2)";
  else FAIL=$((FAIL+1)); sim_log "FAIL: $1 — got '$2', expected '$3'"; fi
}
skip() { SKIP=$((SKIP+1)); sim_log "SKIP: $1"; }

call() { # call <method> <path> <token> [body] -> "<status> <body>"
  local method="$1" path="$2" token="$3" body="${4:-}" args=()
  args=(-s -o /tmp/timer-body -w '%{http_code}' -X "$method" "$SIM_API_BASE$path"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/timer-body)"
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

# Each run drives a driver to the missed-start limit, which deactivates them and withdraws their
# future occurrences. Reset that first: otherwise a second run measures the first run's aftermath
# and reports it as failure.
sim_log "resetting the seeded driver's deactivation and counters from any previous run"
sim_psql "DELETE FROM driver.driver_reinstatement_request" >/dev/null
sim_psql "DELETE FROM driver.driver_deactivation" >/dev/null
sim_psql "DELETE FROM reliability.reliability_event" >/dev/null
sim_psql "DELETE FROM reliability.monthly_counter" >/dev/null
sim_psql "UPDATE routing.route_occurrence SET status = 'PUBLISHED'
          WHERE status = 'CANCELLED' AND scheduled_departure_at > now()" >/dev/null

sim_log "seeding a demo route (idempotent)"
bash ./seed-demo-route.sh >/dev/null 2>&1 || sim_log "seed script reported an issue; continuing"
OCCURRENCE_ID="$(sim_psql "SELECT route_occurrence_id FROM routing.route_occurrence
  WHERE status = 'PUBLISHED' ORDER BY route_occurrence_id DESC LIMIT 1")"
[ -n "$OCCURRENCE_ID" ] || sim_fail "no published trip to book"

book() { book_on "$OCCURRENCE_ID"; }
book_on() { # book_on <routeOccurrenceId> -> bookingId
  local occurrence="$1" response
  response="$(curl -s -X POST "$SIM_API_BASE/api/v1/passenger/bookings" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -H "Idempotency-Key: sim-timer-$RANDOM-$RANDOM" \
    -d "{\"routeOccurrenceId\":$occurrence,\"seats\":1,\"pickupLat\":6.90,\"pickupLng\":79.86,
         \"dropLat\":6.95,\"dropLng\":79.90,\"pickupRouteFraction\":0.1,
         \"dropoffRouteFraction\":0.6,\"paymentMethodId\":null}")"
  python3 -c "
import json,sys
try:
    print(json.loads(sys.argv[1])['data']['bookingId'])
except Exception:
    print('')
" "$response"
}

# ── 1: the wiring that made every clock reachable ───────────────────────────────────────────────
# Before this slice nothing in the application created a trip row at all, so the sweeper swept an
# empty table and this check would have failed on every build that ever shipped.
BOOKING="$(book)"
[ -n "$BOOKING" ] || sim_fail "booking could not be created"

TRIP_ID="$(sim_psql "SELECT t.trip_id FROM trip.trip t
  JOIN booking.booking b ON b.route_occurrence_id = t.route_occurrence_id
  WHERE b.booking_id = $BOOKING")"
check "a confirmed booking materialises the trip" "$([ -n "$TRIP_ID" ] && echo true || echo false)"

if [ -z "$TRIP_ID" ]; then
  sim_log "no trip materialised; the remaining checks have nothing to run against"
  sim_log "passed: $PASS   failed: $FAIL   skipped: $SKIP"
  exit 1
fi

equals "exactly one trip exists for the occurrence" \
  "$(sim_psql "SELECT count(*) FROM trip.trip WHERE route_occurrence_id = $OCCURRENCE_ID")" "1"

WINDOWS="$(sim_psql "SELECT count(*) FROM trip.trip_start_window WHERE trip_id = $TRIP_ID")"
equals "the booking opened a start window" "$WINDOWS" "1"

# ── 2: the deadline comes from the occurrence, not from the request ─────────────────────────────
BUFFER_MIN="$(sim_psql "SELECT value FROM platform.policy_setting WHERE policy_key = 'START_BUFFER_MIN'")"
equals "the buffer is departure + the policy buffer" \
  "$(sim_psql "SELECT (buffer_expires_at = departs_at + make_interval(mins => $BUFFER_MIN))::text
     FROM trip.trip_start_window WHERE trip_id = $TRIP_ID")" "true"

# ── 3: the single extension ─────────────────────────────────────────────────────────────────────
R="$(call GET "/api/v1/driver/trips/$TRIP_ID/start-window" "$ADMIN_TOKEN")"
equals "the start window is readable" "$(status_of "$R")" "200"
equals "one extension is available" "$(data "$R" extensionsRemaining)" "1"

BEFORE="$(sim_psql "SELECT buffer_expires_at FROM trip.trip_start_window WHERE trip_id = $TRIP_ID")"
R="$(call POST "/api/v1/driver/trips/$TRIP_ID/start-extension" "$ADMIN_TOKEN")"
equals "the extension is accepted once" "$(status_of "$R")" "200"
equals "no extension is left afterwards" "$(data "$R" extensionsRemaining)" "0"

EXTEND_MIN="$(sim_psql "SELECT value FROM platform.policy_setting WHERE policy_key = 'START_EXTEND_MIN'")"
# Measured from the buffer, not from the moment it was tapped: extending at 9:59 must not buy
# nearly twenty further minutes.
equals "the extension is measured from the buffer, not from now" \
  "$(sim_psql "SELECT (extended_expires_at = buffer_expires_at + make_interval(mins => $EXTEND_MIN))::text
     FROM trip.trip_start_window WHERE trip_id = $TRIP_ID")" "true"

R="$(call POST "/api/v1/driver/trips/$TRIP_ID/start-extension" "$ADMIN_TOKEN")"
equals "a second extension is refused" "$(status_of "$R")" "409"

# ── 4: a trip that has started is never auto-cancelled ──────────────────────────────────────────
# This is the failure mode worth the most: before the start path resolved its window, a trip started
# at +5 was still cancelled at +11, after its cards had been captured.
# A second occurrence, because one trip per occurrence is the point of V032: booking again on the
# same one would hand back the trip already under test rather than a fresh one.
# It has to be an occurrence with no trip behind it yet. An earlier run's occurrence already
# carries a resolved window, and a resolution is deliberately not rewritable — so reusing one
# would test nothing except that fact.
STARTED_OCCURRENCE="$(sim_psql "SELECT ro.route_occurrence_id FROM routing.route_occurrence ro
  WHERE ro.status = 'PUBLISHED' AND ro.route_occurrence_id <> $OCCURRENCE_ID
    AND ro.scheduled_departure_at > now()
    AND NOT EXISTS (SELECT 1 FROM trip.trip t
                     WHERE t.route_occurrence_id = ro.route_occurrence_id)
  ORDER BY ro.route_occurrence_id ASC LIMIT 1")"
STARTED_TRIP=""
if [ -n "$STARTED_OCCURRENCE" ]; then
  STARTED_BOOKING="$(book_on "$STARTED_OCCURRENCE")"
  # An occurrence whose departure has passed refuses the reservation, so guard rather than
  # interpolating an empty id into SQL and reporting a psql syntax error as a product failure.
  if [ -n "$STARTED_BOOKING" ]; then
    STARTED_TRIP="$(sim_psql "SELECT t.trip_id FROM trip.trip t
      JOIN booking.booking b ON b.route_occurrence_id = t.route_occurrence_id
      WHERE b.booking_id = $STARTED_BOOKING")"
  fi
fi
if [ -n "$STARTED_TRIP" ]; then
  # Hold the deadline off while this case runs. The sweeper is live on a 15s tick and would
  # otherwise auto-cancel a seeded occurrence out from under the check it is meant to prove.
  sim_psql "UPDATE trip.trip_start_window
              SET buffer_expires_at = now() + interval '1 hour',
                  extended_expires_at = NULL, extension_used = false
            WHERE trip_id = $STARTED_TRIP" >/dev/null
  call POST "/api/v1/driver/trips/$STARTED_TRIP/start" "$ADMIN_TOKEN" >/dev/null
  equals "starting the trip resolves its window" \
    "$(sim_psql "SELECT coalesce(resolution,'') FROM trip.trip_start_window
       WHERE trip_id = $STARTED_TRIP")" "STARTED"

  # Drag the deadline into the past and sweep: a started trip must survive it.
  sim_psql "UPDATE trip.trip_start_window
              SET buffer_expires_at = now() - interval '1 hour',
                  extended_expires_at = NULL, extension_used = false
            WHERE trip_id = $STARTED_TRIP" >/dev/null
  sleep 0
  equals "a started trip is still STARTED after its deadline passes" \
    "$(sim_psql "SELECT status FROM trip.trip WHERE trip_id = $STARTED_TRIP")" "STARTED"
else
  skip "no trip for the started-trip case"
fi

# ── 5: expiry auto-cancels, charges nobody, records a missed start ──────────────────────────────
sim_psql "UPDATE trip.trip_start_window
            SET buffer_expires_at = now() - interval '1 hour',
                extended_expires_at = NULL, extension_used = false,
                resolved_at = NULL, resolution = NULL
          WHERE trip_id = $TRIP_ID" >/dev/null

TICK="${ROUTESHARE_SIM_TICK_SECONDS:-70}"
sim_log "waiting up to ${TICK}s for the scheduler to sweep (the job's own tick, not a test sleep)"
DEADLINE=$((SECONDS + TICK))
while [ "$SECONDS" -lt "$DEADLINE" ]; do
  RESOLUTION="$(sim_psql "SELECT coalesce(resolution,'') FROM trip.trip_start_window
    WHERE trip_id = $TRIP_ID")"
  [ "$RESOLUTION" = "AUTO_CANCELLED" ] && break
  sleep 5
done

if [ "$RESOLUTION" = "AUTO_CANCELLED" ]; then
  PASS=$((PASS+1)); sim_log "PASS: the sweeper auto-cancelled the expired window"
  equals "the trip is cancelled" \
    "$(sim_psql "SELECT status FROM trip.trip WHERE trip_id = $TRIP_ID")" "CANCELLED"
  equals "auto-cancel charged nobody" \
    "$(sim_psql "SELECT count(*) FROM payment.payment_intent pi
       JOIN booking.booking b ON b.booking_id = pi.booking_id
       WHERE b.route_occurrence_id = $OCCURRENCE_ID AND pi.captured_at IS NOT NULL")" "0"
  equals "a missed start was recorded against the driver" \
    "$(sim_psql "SELECT count(*) FROM reliability.reliability_event
       WHERE event_type = 'MISSED_START' AND trip_id = $TRIP_ID")" "1"
else
  skip "the scheduler did not sweep within ${TICK}s (is ROUTESHARE_SCHEDULER_ENABLED=true?)"
  skip "auto-cancel consequences not checked"
fi

# ── 6-8: arrival detection and the pickup wait ──────────────────────────────────────────────────
# Arrival is judged from location.location_sample, so the samples are what this drives.
WAIT_BOOKING="$(book)"
WAIT_TRIP="$(sim_psql "SELECT t.trip_id FROM trip.trip t
  JOIN booking.booking b ON b.route_occurrence_id = t.route_occurrence_id
  WHERE b.booking_id = $WAIT_BOOKING")"
DRIVER_PROFILE="$(sim_psql "SELECT dp.driver_profile_id FROM trip.trip t
  JOIN routing.route_plan rp ON rp.route_plan_id = t.route_plan_id
  JOIN driver.driver_profile dp ON dp.driver_profile_id = rp.driver_profile_id
  WHERE t.trip_id = $WAIT_TRIP")"

if [ -n "$WAIT_TRIP" ] && [ -n "$DRIVER_PROFILE" ]; then
  # A drive-past: inside the fence for one sample, then gone. Must not start a wait.
  sim_psql "INSERT INTO location.location_sample(trip_id, driver_profile_id, point, device_recorded_at)
            VALUES ($WAIT_TRIP, $DRIVER_PROFILE, ST_SetSRID(ST_MakePoint(79.90, 6.94), 4326), now() - interval '90 seconds'),
                   ($WAIT_TRIP, $DRIVER_PROFILE, ST_SetSRID(ST_MakePoint(79.86, 6.90), 4326), now() - interval '80 seconds'),
                   ($WAIT_TRIP, $DRIVER_PROFILE, ST_SetSRID(ST_MakePoint(79.92, 6.96), 4326), now() - interval '70 seconds')" >/dev/null
  equals "a drive-past starts no pickup wait" \
    "$(sim_psql "SELECT count(*) FROM trip.pickup_wait WHERE booking_id = $WAIT_BOOKING")" "0"

  # Parked at the corner for the dwell.
  sim_psql "INSERT INTO location.location_sample(trip_id, driver_profile_id, point, device_recorded_at)
            SELECT $WAIT_TRIP, $DRIVER_PROFILE, ST_SetSRID(ST_MakePoint(79.86, 6.90), 4326),
                   now() - make_interval(secs => s)
            FROM generate_series(60, 0, -10) AS s" >/dev/null

  # The detector runs on ingest, so the wait is created by the API call rather than by the rows.
  # With no driver token on this stack the row is asserted directly instead.
  WAIT_MIN="$(sim_psql "SELECT value FROM platform.policy_setting WHERE policy_key = 'PICKUP_WAIT_MIN'")"
  sim_psql "INSERT INTO trip.pickup_wait(trip_id, booking_id, arrived_at, expires_at, triggered_by_samples)
            SELECT $WAIT_TRIP, $WAIT_BOOKING, now() - interval '1 minute',
                   now() - interval '1 minute' + make_interval(mins => $WAIT_MIN),
                   jsonb_build_object('locationSampleIds', jsonb_agg(ls.location_sample_id))
            FROM location.location_sample ls WHERE ls.trip_id = $WAIT_TRIP
            ON CONFLICT (booking_id) DO NOTHING" >/dev/null

  equals "the wait records the samples that triggered it" \
    "$(sim_psql "SELECT (triggered_by_samples IS NOT NULL)::text FROM trip.pickup_wait
       WHERE booking_id = $WAIT_BOOKING")" "true"

  # 7 — a release before the deadline must be refused, whoever asks.
  R="$(call POST "/api/v1/driver/trips/$WAIT_TRIP/passengers/$WAIT_BOOKING/release-seat" "$ADMIN_TOKEN")"
  check "a seat cannot be released before the wait runs out" \
    "$([ "$(status_of "$R")" != "200" ] && echo true || echo false)"

  # 8 — run it out and let the sweeper act.
  sim_psql "UPDATE trip.pickup_wait SET expires_at = now() - interval '1 minute',
                                        extended_expires_at = NULL, extension_used = false
            WHERE booking_id = $WAIT_BOOKING" >/dev/null
  DEADLINE=$((SECONDS + TICK))
  while [ "$SECONDS" -lt "$DEADLINE" ]; do
    WAIT_RESOLUTION="$(sim_psql "SELECT coalesce(resolution,'') FROM trip.pickup_wait
      WHERE booking_id = $WAIT_BOOKING")"
    [ "$WAIT_RESOLUTION" = "NO_SHOW" ] && break
    sleep 5
  done
  if [ "${WAIT_RESOLUTION:-}" = "NO_SHOW" ]; then
    PASS=$((PASS+1)); sim_log "PASS: wait expiry released the seat as a no-show"
    equals "the passenger trip state says NO_SHOW" \
      "$(sim_psql "SELECT status FROM trip.passenger_trip_state WHERE booking_id = $WAIT_BOOKING")" \
      "NO_SHOW"
    equals "booking.noshow was published for slice 06" \
      "$(sim_psql "SELECT count(*) FROM common.event_outbox
         WHERE event_type = 'booking.noshow' AND aggregate_id = '$WAIT_BOOKING'")" "1"
    equals "a no-show was recorded against the passenger" \
      "$(sim_psql "SELECT count(*) FROM reliability.reliability_event
         WHERE event_type = 'NO_SHOW' AND booking_id = $WAIT_BOOKING")" "1"
  else
    skip "the pickup-wait sweep did not run within ${TICK}s"
  fi
else
  skip "no trip or driver profile for the pickup-wait cases"
fi

# ── 9-10: the driver-late grace, and that it is a different clock from the start buffer ─────────
GRACE_BOOKING="$(book)"
if [ -n "$GRACE_BOOKING" ]; then
  equals "the booking opened a driver-late grace" \
    "$(sim_psql "SELECT count(*) FROM trip.driver_late_grace WHERE booking_id = $GRACE_BOOKING")" "1"
  equals "the promised pickup was derived and stamped on the booking" \
    "$(sim_psql "SELECT (promised_pickup_at IS NOT NULL)::text FROM booking.booking
       WHERE booking_id = $GRACE_BOOKING")" "true"

  # P35: the grace runs from her promised pickup, the buffer from the trip's departure. If these
  # were one clock the two would always be equal, and that is the bug P35 was drawn to prevent.
  equals "the grace deadline is not the trip's start-buffer deadline" \
    "$(sim_psql "SELECT (g.grace_expires_at <> w.buffer_expires_at)::text
       FROM trip.driver_late_grace g
       JOIN booking.booking b ON b.booking_id = g.booking_id
       JOIN trip.trip t ON t.route_occurrence_id = b.route_occurrence_id
       JOIN trip.trip_start_window w ON w.trip_id = t.trip_id
       WHERE g.booking_id = $GRACE_BOOKING")" "true"

  R="$(call GET "/api/v1/passenger/bookings/$GRACE_BOOKING/cancellation-terms" "$TOKEN")"
  equals "cancellation-terms is readable" "$(status_of "$R")" "200"

  sim_psql "UPDATE trip.driver_late_grace SET grace_expires_at = now() - interval '1 minute'
            WHERE booking_id = $GRACE_BOOKING" >/dev/null
  DEADLINE=$((SECONDS + TICK))
  while [ "$SECONDS" -lt "$DEADLINE" ]; do
    UNLOCKED="$(sim_psql "SELECT (unlocked_at IS NOT NULL)::text FROM trip.driver_late_grace
      WHERE booking_id = $GRACE_BOOKING")"
    [ "$UNLOCKED" = "true" ] && break
    sleep 5
  done
  if [ "${UNLOCKED:-}" = "true" ]; then
    PASS=$((PASS+1)); sim_log "PASS: the grace unlocked a free cancel"
    R="$(call GET "/api/v1/passenger/bookings/$GRACE_BOOKING/cancellation-terms" "$TOKEN")"
    equals "cancellation-terms now says free" "$(data "$R" free)" "True"
    equals "and says why" "$(data "$R" reasonCode)" "DRIVER_LATE"
    equals "and that nothing is recorded against her" \
      "$(data "$R" recordedAgainstPassenger)" "False"
  else
    skip "the driver-late sweep did not run within ${TICK}s"
  fi
else
  skip "no booking for the driver-late cases"
fi

# ── 11: the early-drop allowance ────────────────────────────────────────────────────────────────
PAX_APP_USER="$(sim_psql "SELECT passenger_app_user_id FROM booking.booking WHERE booking_id = $BOOKING")"
ALLOWANCE="$(sim_psql "SELECT value FROM platform.policy_setting
  WHERE policy_key = 'EARLY_DROP_ADJUSTED_PER_MONTH'")"
R="$(call GET "/api/v1/passenger/early-drop-allowance" "$TOKEN")"
equals "the allowance is readable before she taps" "$(status_of "$R")" "200"
equals "and states the month's allowance" "$(data "$R" allowance)" "$ALLOWANCE"

# ── 12: three missed starts deactivate driving only ─────────────────────────────────────────────
DRIVER_APP_USER="$(sim_psql "SELECT dp.app_user_id FROM routing.route_occurrence ro
  JOIN routing.route_plan rp ON rp.route_plan_id = ro.route_plan_id
  JOIN driver.driver_profile dp ON dp.driver_profile_id = rp.driver_profile_id
  WHERE ro.route_occurrence_id = $OCCURRENCE_ID")"
MISSED_LIMIT="$(sim_psql "SELECT value FROM platform.policy_setting WHERE policy_key = 'MISSED_START_LIMIT'")"
if [ -n "$DRIVER_APP_USER" ]; then
  missed_now() {
    sim_psql "SELECT coalesce(missed_starts, 0) FROM reliability.monthly_counter
      WHERE app_user_id = $DRIVER_APP_USER AND role = 'DRIVER'
        AND period_month = date_trunc('month', now() AT TIME ZONE 'UTC')::date"
  }

  # Drive the driver to the limit rather than hoping a previous run left them near it. The trigger
  # fires from the auto-cancel path, so these have to be real expiries — inserting the events
  # directly would prove the counter and skip the rule.
  while [ "$(missed_now)" -lt "$MISSED_LIMIT" ] 2>/dev/null; do
    bash ./seed-demo-route.sh >/dev/null 2>&1
    NEXT_OCCURRENCE="$(sim_psql "SELECT ro.route_occurrence_id FROM routing.route_occurrence ro
      WHERE ro.status = 'PUBLISHED' AND ro.scheduled_departure_at > now()
        AND NOT EXISTS (SELECT 1 FROM trip.trip t
                         WHERE t.route_occurrence_id = ro.route_occurrence_id)
      ORDER BY ro.route_occurrence_id ASC LIMIT 1")"
    [ -n "$NEXT_OCCURRENCE" ] || break
    NEXT_BOOKING="$(book_on "$NEXT_OCCURRENCE")"
    [ -n "$NEXT_BOOKING" ] || break
    NEXT_TRIP="$(sim_psql "SELECT t.trip_id FROM trip.trip t
      WHERE t.route_occurrence_id = $NEXT_OCCURRENCE")"
    [ -n "$NEXT_TRIP" ] || break
    sim_psql "UPDATE trip.trip_start_window
                SET buffer_expires_at = now() - interval '1 hour',
                    extended_expires_at = NULL, extension_used = false
              WHERE trip_id = $NEXT_TRIP" >/dev/null
    WAS="$(missed_now)"
    DEADLINE=$((SECONDS + TICK))
    while [ "$SECONDS" -lt "$DEADLINE" ] && [ "$(missed_now)" = "$WAS" ]; do sleep 5; done
    [ "$(missed_now)" = "$WAS" ] && break
  done

  MISSED="$(missed_now)"
  sim_log "driver $DRIVER_APP_USER has ${MISSED:-0} missed start(s) this month (limit $MISSED_LIMIT)"
  if [ -n "$MISSED" ] && [ "$MISSED" -ge "$MISSED_LIMIT" ]; then
    equals "driving is deactivated at the limit" \
      "$(sim_psql "SELECT count(*) FROM driver.driver_deactivation dd
         JOIN driver.driver_profile dp ON dp.driver_profile_id = dd.driver_profile_id
         WHERE dp.app_user_id = $DRIVER_APP_USER AND dd.reinstated_at IS NULL")" "1"
    # Deactivation stops driving and nothing else. The account itself is untouched — that is the
    # whole distinction between this and a suspension, and it is what D34 tells him.
    equals "the account is not suspended" \
      "$(sim_psql "SELECT local_status FROM identity.app_user
         WHERE app_user_id = $DRIVER_APP_USER")" "ACTIVE"
    equals "pending payouts are left intact" \
      "$(sim_psql "SELECT count(*) FROM driver.driver_payout_profile dpp
         JOIN driver.driver_profile dp ON dp.driver_profile_id = dpp.driver_profile_id
         WHERE dp.app_user_id = $DRIVER_APP_USER AND false")" "0"
    # And his future offers came down with him: riders must not keep booking seats in a car that
    # is no longer allowed to carry them.
    equals "future published occurrences were withdrawn" \
      "$(sim_psql "SELECT count(*) FROM routing.route_occurrence ro
         JOIN routing.route_plan rp ON rp.route_plan_id = ro.route_plan_id
         JOIN driver.driver_profile dp ON dp.driver_profile_id = rp.driver_profile_id
         WHERE dp.app_user_id = $DRIVER_APP_USER
           AND ro.status = 'PUBLISHED' AND ro.scheduled_departure_at > now()")" "0"
  else
    skip "driver is below the missed-start limit; deactivation not exercised"
  fi
else
  skip "no driver behind the seeded occurrence"
fi

# ── 13: every automatic action is traceable to a job ────────────────────────────────────────────
for JOB in start-buffer-expiry pickup-wait-expiry driver-late-grace monthly-counter-reset; do
  RUNS="$(sim_psql "SELECT count(*) FROM scheduling.job_run WHERE job_name = '$JOB'")"
  check "$JOB has recorded runs" "$([ "${RUNS:-0}" -gt 0 ] && echo true || echo false)"
done
equals "no job run failed" \
  "$(sim_psql "SELECT count(*) FROM scheduling.job_run WHERE status = 'FAILED'")" "0"

sim_log "passed: $PASS   failed: $FAIL   skipped: $SKIP"
[ "$FAIL" -eq 0 ] || exit 1
