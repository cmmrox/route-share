#!/usr/bin/env bash
# Verifies Slice 10 against the running local stack: confirmed-booking chat, audited access,
# scheduled closure, the preference matrix, badges, driving suppression, contextual SOS and
# persisted settings. Never run against production.
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

RUN_TAG="s10-$RANDOM-$RANDOM"
DRIVER_USERNAME="${ROUTESHARE_SIM_S10_DRIVER:-sim-s10-driver}"
DRIVER_PASSWORD="${ROUTESHARE_SIM_S10_DRIVER_PASSWORD:-SimDriver#12345}"
ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
PASSENGER_PHONE="${ROUTESHARE_SIM_S10_PASSENGER_PHONE:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"
STRANGER_PHONE="${ROUTESHARE_SIM_S10_STRANGER_PHONE:-+9476$(printf '%07d' $((RANDOM % 10000000)))}"
TICK="${ROUTESHARE_SIM_TICK_SECONDS:-70}"

PASS=0
FAIL=0
check() {
  if [ "$2" = "true" ]; then
    PASS=$((PASS + 1)); sim_log "PASS: $1"
  else
    FAIL=$((FAIL + 1)); sim_log "FAIL: $1"
  fi
}
equals() {
  if [ "$2" = "$3" ]; then
    PASS=$((PASS + 1)); sim_log "PASS: $1 ($2)"
  else
    FAIL=$((FAIL + 1)); sim_log "FAIL: $1 — got '$2', expected '$3'"
  fi
}
call() {
  local method="$1" path="$2" token="$3" body="${4:-}" idempotency="${5:-}"
  local args=(-s -o /tmp/routeshare-s10-body -w '%{http_code}' -X "$method"
    "$SIM_API_BASE$path" -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  [ -n "$idempotency" ] && args+=(-H "Idempotency-Key: $idempotency")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/routeshare-s10-body)"
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

sim_require_tools
sim_require_api

PASSENGER_TOKEN="$(sim_login "$PASSENGER_PHONE")"
STRANGER_TOKEN="$(sim_login "$STRANGER_PHONE")"
DRIVER_TOKEN="$(sim_keycloak_login_with_roles \
  "$DRIVER_USERNAME" "$DRIVER_PASSWORD" "PASSENGER,DRIVER" driver-mobile)"
ADMIN_TOKEN="$(sim_keycloak_login_with_roles \
  "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN" admin-web)"

# Force identity upserts before resolving their database ids.
call GET /api/v1/me/context "$PASSENGER_TOKEN" >/dev/null
call GET /api/v1/me/context "$STRANGER_TOKEN" >/dev/null
call GET /api/v1/me/context "$DRIVER_TOKEN" >/dev/null

PASSENGER_ID="$(sim_psql "SELECT app_user_id FROM identity.app_user
  WHERE phone = '$PASSENGER_PHONE' ORDER BY app_user_id DESC LIMIT 1")"
STRANGER_ID="$(sim_psql "SELECT app_user_id FROM identity.app_user
  WHERE phone = '$STRANGER_PHONE' ORDER BY app_user_id DESC LIMIT 1")"
DRIVER_ID="$(sim_psql "SELECT app_user_id FROM identity.app_user
  WHERE lower(email) = lower('$DRIVER_USERNAME@routeshare.local')
  ORDER BY app_user_id DESC LIMIT 1")"
[ -n "$PASSENGER_ID" ] && [ -n "$STRANGER_ID" ] && [ -n "$DRIVER_ID" ] \
  || sim_fail "could not resolve the three simulation users"

DRIVER_PROFILE_ID="$(sim_psql "INSERT INTO driver.driver_profile
    (app_user_id, display_name, verification_status)
  VALUES ($DRIVER_ID, 'Slice Ten Driver', 'APPROVED')
  ON CONFLICT (app_user_id) DO UPDATE
    SET display_name = EXCLUDED.display_name, verification_status = 'APPROVED'
  RETURNING driver_profile_id" | head -1)"
REGISTRATION="S10-$(printf '%05d' $((RANDOM % 100000)))"
VEHICLE_ID="$(sim_psql "INSERT INTO vehicle.vehicle
    (driver_profile_id, make, model, manufacture_year, color, registration_number,
     seat_count, status, class_key)
  VALUES ($DRIVER_PROFILE_ID, 'Toyota', 'Aqua', 2022, 'Blue', '$REGISTRATION',
          3, 'APPROVED', 'CAR')
  RETURNING vehicle_id" | head -1)"
sim_psql "INSERT INTO vehicle.vehicle_rate_band
    (vehicle_id, rate_min, rate_max, chosen_rate, status)
  VALUES ($VEHICLE_ID, 38, 62, 50, 'ACTIVE')" >/dev/null
ROUTE_ID="$(sim_psql "INSERT INTO routing.route_plan
    (driver_profile_id, vehicle_id, origin_label, destination_label, route_line,
     route_length_m, departure_time, available_seats, status)
  VALUES ($DRIVER_PROFILE_ID, $VEHICLE_ID, 'Slice 10 Origin', 'Slice 10 Destination',
          ST_GeomFromText('LINESTRING(79.8612 6.9271,79.9000 6.9500)',4326),
          6500, now() + interval '4 hours', 3, 'PUBLISHED')
  RETURNING route_plan_id" | head -1)"
OCCURRENCE_ID="$(sim_psql "INSERT INTO routing.route_occurrence
    (route_plan_id, scheduled_departure_at, available_seats, status, approval_mode)
  VALUES ($ROUTE_ID, now() + interval '4 hours', 3, 'PUBLISHED', 'APPROVE_EACH')
  RETURNING route_occurrence_id" | head -1)"
[ -n "$DRIVER_PROFILE_ID" ] && [ -n "$VEHICLE_ID" ] \
  && [ -n "$ROUTE_ID" ] && [ -n "$OCCURRENCE_ID" ] \
  || sim_fail "could not build the Slice 10 route fixture"

BOOKING_RESPONSE="$(call POST /api/v1/passenger/bookings "$PASSENGER_TOKEN" \
  "{\"routeOccurrenceId\":$OCCURRENCE_ID,\"seats\":1,
    \"pickupLat\":6.9271,\"pickupLng\":79.8612,
    \"dropLat\":6.9500,\"dropLng\":79.9000,
    \"pickupRouteFraction\":0.1,\"dropoffRouteFraction\":0.9,
    \"paymentMethodId\":null,\"seatSlotIds\":null}" "s10-book-$RUN_TAG")"
equals "booking request is accepted" "$(status_of "$BOOKING_RESPONSE")" "200"
BOOKING_ID="$(data "$BOOKING_RESPONSE" bookingId)"
[ -n "$BOOKING_ID" ] || sim_fail "booking fixture was not created"
equals "chat is absent before confirmation" \
  "$(sim_psql "SELECT count(*) FROM chat.chat_thread WHERE booking_id = $BOOKING_ID")" "0"

APPROVE_RESPONSE="$(call POST "/api/v1/driver/bookings/$BOOKING_ID/approve" "$DRIVER_TOKEN")"
equals "driver confirmation succeeds" "$(status_of "$APPROVE_RESPONSE")" "200"
equals "confirmation opens exactly one chat thread" \
  "$(sim_psql "SELECT count(*) FROM chat.chat_thread
               WHERE booking_id = $BOOKING_ID AND state = 'OPEN'")" "1"

THREAD_RESPONSE="$(call GET "/api/v1/bookings/$BOOKING_ID/chat" "$PASSENGER_TOKEN")"
equals "passenger reads booking-scoped chat" "$(status_of "$THREAD_RESPONSE")" "200"
equals "thread discloses audited support readability" \
  "$(data "$THREAD_RESPONSE" supportReadable)" "True"

MESSAGE_RESPONSE="$(call POST "/api/v1/bookings/$BOOKING_ID/chat/messages" "$PASSENGER_TOKEN" \
  '{"body":"I am at the pickup point"}' "s10-message-$RUN_TAG")"
equals "participant sends a message" "$(status_of "$MESSAGE_RESPONSE")" "200"
MESSAGE_ID="$(data "$MESSAGE_RESPONSE" id)"
equals "same idempotency key stores one message" \
  "$(status_of "$(call POST "/api/v1/bookings/$BOOKING_ID/chat/messages" "$PASSENGER_TOKEN" \
    '{"body":"I am at the pickup point"}' "s10-message-$RUN_TAG")")" "200"
equals "idempotent send has one database row" \
  "$(sim_psql "SELECT count(*) FROM chat.chat_message
               WHERE chat_message_id = ${MESSAGE_ID:-0}")" "1"
equals "unrelated user cannot read the thread" \
  "$(status_of "$(call GET "/api/v1/bookings/$BOOKING_ID/chat" "$STRANGER_TOKEN")")" "403"

ADMIN_READ="$(call GET \
  "/api/v1/admin/bookings/$BOOKING_ID/chat/messages?reason=Safety%20case%20review" "$ADMIN_TOKEN")"
equals "support reads only through the reasoned admin path" "$(status_of "$ADMIN_READ")" "200"
equals "admin read is audited" \
  "$(sim_psql "SELECT count(*) FROM chat.chat_admin_read_audit a
    JOIN chat.chat_thread t ON t.chat_thread_id = a.chat_thread_id
    WHERE t.booking_id = $BOOKING_ID AND a.reason = 'Safety case review'")" "1"

TRIP_ID="$(sim_psql "SELECT trip_id FROM trip.trip
  WHERE route_occurrence_id = $OCCURRENCE_ID ORDER BY trip_id DESC LIMIT 1")"
[ -n "$TRIP_ID" ] || sim_fail "approval did not materialise a trip"
sim_psql "UPDATE trip.passenger_trip_state
  SET status = 'BOARDED', boarded_at = now(), updated_at = now()
  WHERE trip_id = $TRIP_ID AND booking_id = $BOOKING_ID;
  INSERT INTO trip.passenger_trip_state
    (trip_id, booking_id, route_occurrence_id, passenger_app_user_id,
     status, boarded_at)
  SELECT $TRIP_ID, $BOOKING_ID, $OCCURRENCE_ID, $PASSENGER_ID, 'BOARDED', now()
  WHERE NOT EXISTS (
    SELECT 1 FROM trip.passenger_trip_state
    WHERE trip_id = $TRIP_ID AND booking_id = $BOOKING_ID)" >/dev/null
DROP_RESPONSE="$(call POST \
  "/api/v1/driver/trips/$TRIP_ID/passengers/$BOOKING_ID/drop-off" "$DRIVER_TOKEN")"
equals "driver drop-off transition succeeds" "$(status_of "$DROP_RESPONSE")" "200"
check "drop-off schedules closure 24 hours later" \
  "$(sim_psql "SELECT (closes_at BETWEEN now() + interval '23 hours 59 minutes'
    AND now() + interval '24 hours 1 minute')::text
    FROM chat.chat_thread WHERE booking_id = $BOOKING_ID")"

# Make the already-proven deadline due, then let the real registered scheduler close it.
sim_psql "UPDATE chat.chat_thread SET closes_at = now() - interval '1 second'
  WHERE booking_id = $BOOKING_ID" >/dev/null
DEADLINE=$((SECONDS + TICK))
CHAT_STATE="OPEN"
while [ "$SECONDS" -lt "$DEADLINE" ]; do
  CHAT_STATE="$(sim_psql "SELECT state FROM chat.chat_thread WHERE booking_id = $BOOKING_ID")"
  [ "$CHAT_STATE" = "CLOSED" ] && break
  sleep 5
done
equals "chat-auto-close closes the due thread" "$CHAT_STATE" "CLOSED"
equals "passenger cannot post after closure" \
  "$(status_of "$(call POST "/api/v1/bookings/$BOOKING_ID/chat/messages" "$PASSENGER_TOKEN" \
    '{"body":"Too late"}' "s10-after-close-p")")" "409"
equals "driver cannot post after closure" \
  "$(status_of "$(call POST "/api/v1/bookings/$BOOKING_ID/chat/messages" "$DRIVER_TOKEN" \
    '{"body":"Too late"}' "s10-after-close-d")")" "409"

PREFERENCES="$(call GET /api/v1/notification-preferences "$DRIVER_TOKEN")"
equals "notification preference matrix is readable" "$(status_of "$PREFERENCES")" "200"
equals "preference matrix has the twelve S23 rows" \
  "$(python3 -c "import json,sys; print(len(json.loads(sys.argv[1])['data']['categories']))" \
    "$(body_of "$PREFERENCES")")" "12"
SAFETY_UPDATE="$(call PUT /api/v1/notification-preferences "$DRIVER_TOKEN" \
  '{"categories":[{"key":"SAFETY_AND_EMERGENCY","enabled":false,
    "push":false,"sms":false,"inApp":false}]}')"
equals "crafted safety opt-out is refused" "$(status_of "$SAFETY_UPDATE")" "409"

sim_psql "UPDATE identity.app_user SET last_active_mode = 'DRIVER'
  WHERE app_user_id = $DRIVER_ID;
  UPDATE trip.trip SET status = 'STARTED', started_at = now() WHERE trip_id = $TRIP_ID" >/dev/null
BROADCAST="$(call POST /api/v1/admin/notifications/broadcasts "$ADMIN_TOKEN" \
  "{\"title\":\"Slice 10 quiet alert\",\"body\":\"$RUN_TAG\"}")"
equals "admin broadcast is accepted" "$(status_of "$BROADCAST")" "200"
equals "passenger-side alert is stored deferred during a live drive" \
  "$(sim_psql "SELECT deferred::text FROM notification.notification
    WHERE app_user_id = $DRIVER_ID AND type = 'BROADCAST' AND body = '$RUN_TAG'
    ORDER BY notification_id DESC LIMIT 1")" "true"

BADGES="$(call GET /api/v1/badges "$DRIVER_TOKEN")"
equals "badge summary is readable" "$(status_of "$BADGES")" "200"
check "badge summary uses dot booleans and numeric counts without an action badge" \
  "$(python3 -c "
import json,sys
d=json.loads(sys.argv[1])['data']
print(str(type(d.get('home')) is bool and type(d.get('account')) is bool
          and type(d.get('trips')) is int and type(d.get('inbox')) is int
          and 'action' not in d).lower())
" "$(body_of "$BADGES")")"

sim_psql "DELETE FROM passenger.trusted_contact
  WHERE app_user_id = $DRIVER_ID AND name = 'Emergency Contact';
  INSERT INTO passenger.trusted_contact
  (app_user_id, name, phone, relationship, auto_share_sos)
  VALUES ($DRIVER_ID, 'Emergency Contact', '+94770000001', 'Family', true)" >/dev/null
SOS_RESPONSE="$(call POST /api/v1/sos-events "$DRIVER_TOKEN" \
  "{\"kind\":\"EMERGENCY\",\"tripId\":$TRIP_ID,\"bookingId\":$BOOKING_ID,
    \"latitude\":6.9271,\"longitude\":79.8612,\"note\":\"$RUN_TAG\"}")"
equals "contextual SOS is accepted during the live drive" "$(status_of "$SOS_RESPONSE")" "200"
SOS_ID="$(data "$SOS_RESPONSE" id)"
equals "SOS alert itself bypasses driving suppression" \
  "$(sim_psql "SELECT deferred::text FROM notification.notification
    WHERE app_user_id = $DRIVER_ID AND type = 'SOS_RECEIVED'
    ORDER BY notification_id DESC LIMIT 1")" "false"
check "SOS snapshots role, vehicle and place and records the contact delivery attempt" \
  "$(sim_psql "SELECT (role = 'DRIVER' AND vehicle_registration = '$REGISTRATION'
    AND snapshot_location IS NOT NULL AND snapshot_place_label IS NOT NULL
    AND contacts_alerted + contact_alert_failures = 1)::text
    FROM safety.sos_event WHERE sos_event_id = ${SOS_ID:-0}")"

SETTINGS="$(call PUT /api/v1/me/settings "$PASSENGER_TOKEN" \
  '{"theme":"DARK","language":"si","shareLiveLocation":false,
    "showRatingPublicly":false,"receiptsByEmail":true}')"
equals "settings persist" "$(status_of "$SETTINGS")" "200"
CONTEXT="$(call GET /api/v1/me/context "$PASSENGER_TOKEN")"
equals "saved theme surfaces in app context" "$(data "$CONTEXT" settings.theme)" "DARK"
equals "saved language surfaces in app context" "$(data "$CONTEXT" settings.language)" "si"
equals "data export request queues" \
  "$(data "$(call POST /api/v1/me/data-export "$PASSENGER_TOKEN")" status)" "QUEUED"
DELETION="$(call POST /api/v1/me/deletion-request "$PASSENGER_TOKEN")"
equals "deletion request queues" "$(data "$DELETION" status)" "QUEUED"
equals "deletion response states seven-year receipt retention" \
  "$(data "$DELETION" receiptRetentionYears)" "7"

sim_log "Slice 10 result: PASS=$PASS FAIL=$FAIL SKIP=0"
[ "$FAIL" -eq 0 ] || exit 1
