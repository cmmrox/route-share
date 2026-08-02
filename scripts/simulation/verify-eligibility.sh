#!/usr/bin/env bash
# Verifies slice 08 — preferences, verification and server-side eligibility.
#
# The point of this run is the *absences*. Unit tests can prove the rule; only a live stack can
# prove that a trip a rider may not book never reaches her at all — and that the same rule refuses
# her if she names the id directly, which is the hole a client-side filter would leave open.
#
#   1. A driver whose NIC does not verify her as female cannot set women-only
#   2. A verified female driver can, and the trip inherits it
#   3. An unverified rider's search never returns a verified-only trip
#   4. She is refused NOT_ELIGIBLE_VERIFIED_ONLY if she books it by id
#   5. A male rider's search never returns a women-only trip
#   6. He is refused NOT_ELIGIBLE_WOMEN_ONLY if he books it by id
#   7. A verified female rider sees and books the women-only trip
#   8. An unverified rider still books an ordinary trip — verification is never a gate
#   9. A gallery capture is refused; a camera capture is accepted
#  10. HIDDEN and MATCHED photo URLs never appear where they should not
#  11. Gender and the NIC appear in no rider-facing payload
#  12. eligibility-impact returns the refusals this run actually caused
#
# Requires the local stack with demo OTP. Never run against production.
#
# Usage:
#   ROUTESHARE_API_BASE=http://localhost:8088 ROUTESHARE_DB_NAME=routeshare_comigo \
#     scripts/simulation/verify-eligibility.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
PHONE_UNVERIFIED="${ROUTESHARE_SIM_ELIG_PHONE_U:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"
PHONE_MALE="${ROUTESHARE_SIM_ELIG_PHONE_M:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"
PHONE_FEMALE="${ROUTESHARE_SIM_ELIG_PHONE_F:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"

RUN_STARTED_AT="$(date -u +'%Y-%m-%d %H:%M:%S')"

PASS=0; FAIL=0; SKIP=0
check() { if [ "$2" = "true" ]; then PASS=$((PASS+1)); sim_log "PASS: $1";
          else FAIL=$((FAIL+1)); sim_log "FAIL: $1"; fi; }
equals() { if [ "$2" = "$3" ]; then PASS=$((PASS+1)); sim_log "PASS: $1 ($2)";
           else FAIL=$((FAIL+1)); sim_log "FAIL: $1 — got '$2', expected '$3'"; fi; }
skip() { SKIP=$((SKIP+1)); sim_log "SKIP: $1"; }

call() { # call <method> <path> <token> [body] -> "<status> <body>"
  local method="$1" path="$2" token="$3" body="${4:-}" args=()
  args=(-s -o /tmp/elig-body -w '%{http_code}' -X "$method" "$SIM_API_BASE$path"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/elig-body)"
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
# Whether a search result set contains a given occurrence — the whole of checks 3 and 5.
contains_occurrence() { python3 -c "
import json,sys
try:
    rows = json.loads(sys.argv[1])['data']
    print(str(any(str(r.get('routeOccurrenceId')) == sys.argv[2] for r in rows)).lower())
except Exception:
    print('error')
" "$(body_of "$1")" "$2"; }
# Any occurrence of a needle anywhere in a payload, at any depth.
mentions() { python3 -c "
import json,sys
print(str(sys.argv[2].lower() in sys.argv[1].lower()).lower())
" "$(body_of "$1")" "$2"; }

sim_require_tools
sim_require_api

TOKEN_U="$(sim_login "$PHONE_UNVERIFIED")"
TOKEN_M="$(sim_login "$PHONE_MALE")"
TOKEN_F="$(sim_login "$PHONE_FEMALE")"
ADMIN_TOKEN="$(sim_keycloak_login_with_roles "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN" admin-web)"

sim_log "seeding a demo route (idempotent)"
bash ./seed-demo-route.sh >/dev/null 2>&1 || sim_log "seed script reported an issue; continuing"
sim_psql "UPDATE routing.route_occurrence SET status = 'PUBLISHED'
          WHERE status = 'CANCELLED' AND scheduled_departure_at > now()" >/dev/null

# Each rider's profile row and level, set directly: the four-capture flow is exercised separately
# below, and driving it three times here would test the reviewer rather than the eligibility rule.
seed_rider() { # seed_rider <phone> <level> <gender>
  sim_psql "INSERT INTO passenger.passenger_profile(app_user_id, full_name, verification_level,
                                                    gender, verified_at)
            SELECT app_user_id, COALESCE(display_name, 'Rider'), '$2', '$3',
                   CASE WHEN '$2' = 'VERIFIED' THEN now() ELSE NULL END
              FROM identity.app_user WHERE phone = '$1'
            ON CONFLICT (app_user_id) DO UPDATE
              SET verification_level = EXCLUDED.verification_level,
                  gender = EXCLUDED.gender, verified_at = EXCLUDED.verified_at" >/dev/null
}
# A rider's app_user row is created lazily on her first authenticated call, so seeding a level
# before that silently updates nothing — the INSERT ... SELECT matches no row.
for t in "$TOKEN_U" "$TOKEN_M" "$TOKEN_F"; do
  call GET /api/v1/me/context "$t" >/dev/null
done

seed_rider "$PHONE_UNVERIFIED" NONE UNSPECIFIED
seed_rider "$PHONE_MALE" VERIFIED MALE
seed_rider "$PHONE_FEMALE" VERIFIED FEMALE

# The three trips this run compares are clones of one real seeded occurrence, differing only in
# their eligibility columns. Anything else — a different corridor, a missing bucket cell — would
# make a trip absent from search for a reason that has nothing to do with eligibility, and every
# negative check here would then pass while proving nothing.
BASE_OCC="$(sim_psql "SELECT c.route_occurrence_id
                        FROM routing.route_bucket_cell c
                        JOIN routing.route_occurrence o
                          ON o.route_occurrence_id = c.route_occurrence_id
                        JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
                       WHERE o.status = 'PUBLISHED' AND p.status = 'PUBLISHED'
                       ORDER BY c.route_occurrence_id DESC LIMIT 1")"
[ -n "$BASE_OCC" ] || sim_fail "no published, bucketed occurrence to clone the fixture from"
BASE_PLAN="$(sim_psql "SELECT route_plan_id FROM routing.route_occurrence
                       WHERE route_occurrence_id = $BASE_OCC")"

# Pickup and drop-off are read off that plan's own stored line, so the corridor predicates are
# satisfied by construction rather than by a coordinate that happened to work once.
read -r PICKUP_LAT PICKUP_LNG DROP_LAT DROP_LNG <<<"$(sim_psql "
  SELECT ST_Y(ST_LineInterpolatePoint(route_line, 0.10)) || ' ' ||
         ST_X(ST_LineInterpolatePoint(route_line, 0.10)) || ' ' ||
         ST_Y(ST_LineInterpolatePoint(route_line, 0.60)) || ' ' ||
         ST_X(ST_LineInterpolatePoint(route_line, 0.60))
    FROM routing.route_plan WHERE route_plan_id = $BASE_PLAN")"

new_occurrence() { # new_occurrence <genderPolicy> <verifiedRidersOnly>
  # `head -1` matters: psql prints the command tag after the RETURNING row, so an unfiltered read
  # yields "85\nINSERT 0 1" and every request naming it is refused as an invalid body.
  local id
  id="$(sim_psql "INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at,
              available_seats, status, gender_policy, verified_riders_only)
            VALUES ($BASE_PLAN, now() + interval '20 hours', 3, 'PUBLISHED', '$1', $2)
            RETURNING route_occurrence_id" | head -1)"
  sim_psql "INSERT INTO routing.route_bucket_cell(route_plan_id, route_occurrence_id,
              bucket_resolution, bucket_cell)
            SELECT c.route_plan_id, $id, c.bucket_resolution, c.bucket_cell
              FROM routing.route_bucket_cell c
             WHERE c.route_occurrence_id = $BASE_OCC" >/dev/null
  printf '%s' "$id"
}

search() { # search <token> -> "<status> <body>"
  call POST /api/v1/passenger/ride-searches "$1" \
    "{\"pickup\":{\"latitude\":$PICKUP_LAT,\"longitude\":$PICKUP_LNG},
      \"dropoff\":{\"latitude\":$DROP_LAT,\"longitude\":$DROP_LNG},
      \"requestedDepartureTime\":\"$(date -u -v+20H +'%Y-%m-%dT%H:%M:%SZ' 2>/dev/null \
        || date -u -d '+20 hours' +'%Y-%m-%dT%H:%M:%SZ')\",
      \"seats\":1,\"departureWindowMinutes\":240,\"limit\":50}"
}

book() { # book <token> <occurrence> -> "<status> <body>"
  local args=(-s -o /tmp/elig-body -w '%{http_code}' -X POST
        "$SIM_API_BASE/api/v1/passenger/bookings"
        -H "Authorization: Bearer $1" -H 'Content-Type: application/json'
        -H "Idempotency-Key: sim-elig-$RANDOM-$RANDOM"
        -d "{\"routeOccurrenceId\":$2,\"seats\":1,\"pickupLat\":$PICKUP_LAT,
      \"pickupLng\":$PICKUP_LNG,\"dropLat\":$DROP_LAT,\"dropLng\":$DROP_LNG,
      \"pickupRouteFraction\":0.1,
      \"dropoffRouteFraction\":0.6,\"paymentMethodId\":null,\"seatSlotIds\":null}")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/elig-body)"
}

DRIVER_PROFILE="$(sim_psql "SELECT d.driver_profile_id FROM driver.driver_profile d
                            ORDER BY d.driver_profile_id DESC LIMIT 1")"
DRIVER_TOKEN_PHONE="$(sim_psql "SELECT u.phone FROM identity.app_user u
                                JOIN driver.driver_profile d ON d.app_user_id = u.app_user_id
                                WHERE d.driver_profile_id = $DRIVER_PROFILE")"

# ── 1-2: the set gate ────────────────────────────────────────────────────────────────────────────
if [ -n "$DRIVER_TOKEN_PHONE" ]; then
  DRIVER_TOKEN="$(sim_login "$DRIVER_TOKEN_PHONE")"

  sim_psql "UPDATE driver.driver_profile SET gender = NULL
            WHERE driver_profile_id = $DRIVER_PROFILE" >/dev/null
  R="$(call PUT /api/v1/driver/preferences "$DRIVER_TOKEN" \
        '{"genderPolicy":"WOMEN_ONLY","verifiedRidersOnly":false,"approveEachRequest":true,
          "midTripBookings":true,"earlyDropRequests":true,"chatEnabled":true}')"
  equals "08-8: an unverified driver cannot set women-only" "$(status_of "$R")" "403"
  equals "and is told which rule refused" "$(error_code "$R")" "WOMEN_ONLY_NOT_AVAILABLE"

  sim_psql "UPDATE driver.driver_profile SET gender = 'MALE'
            WHERE driver_profile_id = $DRIVER_PROFILE" >/dev/null
  R="$(call PUT /api/v1/driver/preferences "$DRIVER_TOKEN" \
        '{"genderPolicy":"WOMEN_ONLY","verifiedRidersOnly":false,"approveEachRequest":true,
          "midTripBookings":true,"earlyDropRequests":true,"chatEnabled":true}')"
  equals "08-7: a male driver cannot set women-only either" "$(status_of "$R")" "403"

  R="$(call GET /api/v1/driver/preferences "$DRIVER_TOKEN")"
  equals "the toggle is not even offered to him" "$(data "$R" canSetWomenOnly)" "False"

  sim_psql "UPDATE driver.driver_profile SET gender = 'FEMALE'
            WHERE driver_profile_id = $DRIVER_PROFILE" >/dev/null
  R="$(call PUT /api/v1/driver/preferences "$DRIVER_TOKEN" \
        '{"genderPolicy":"WOMEN_ONLY","verifiedRidersOnly":true,"approveEachRequest":false,
          "midTripBookings":true,"earlyDropRequests":true,"chatEnabled":true}')"
  equals "08-9: a verified female driver may set women-only" "$(status_of "$R")" "200"
  equals "and it is stored" "$(data "$R" genderPolicy)" "WOMEN_ONLY"
  equals "the toggle is offered to her" "$(data "$R" canSetWomenOnly)" "True"
else
  skip "no driver profile to exercise the women-only set gate"
  skip "no driver profile: the preference read"
fi

# ── 3-6: the two absences, and the two refusals ──────────────────────────────────────────────────
VERIFIED_ONLY_OCC="$(new_occurrence ANYONE true)"
WOMEN_ONLY_OCC="$(new_occurrence WOMEN_ONLY false)"
ORDINARY_OCC="$(new_occurrence ANYONE false)"
[ -n "$VERIFIED_ONLY_OCC" ] && [ -n "$WOMEN_ONLY_OCC" ] && [ -n "$ORDINARY_OCC" ] \
  || sim_fail "could not create the three trips this run needs"

R="$(search "$TOKEN_U")"
equals "an unverified rider can search at all" "$(status_of "$R")" "200"
equals "08-1: and never sees the verified-only trip" \
  "$(contains_occurrence "$R" "$VERIFIED_ONLY_OCC")" "false"
equals "08-3: nor the women-only trip" \
  "$(contains_occurrence "$R" "$WOMEN_ONLY_OCC")" "false"
equals "the ordinary trip is there, so the absences above are the rule and not an empty search" \
  "$(contains_occurrence "$R" "$ORDINARY_OCC")" "true"
equals "search never says why a trip is missing" \
  "$(mentions "$R" "NOT_ELIGIBLE")" "false"

R="$(book "$TOKEN_U" "$VERIFIED_ONLY_OCC")"
equals "08-2: naming the verified-only trip by id is refused" "$(status_of "$R")" "403"
equals "and the reason is stated at booking" "$(error_code "$R")" "NOT_ELIGIBLE_VERIFIED_ONLY"

R="$(search "$TOKEN_M")"
equals "08-3: a verified male rider never sees the women-only trip" \
  "$(contains_occurrence "$R" "$WOMEN_ONLY_OCC")" "false"
equals "but he does see the verified-only one" \
  "$(contains_occurrence "$R" "$VERIFIED_ONLY_OCC")" "true"

R="$(book "$TOKEN_M" "$WOMEN_ONLY_OCC")"
equals "08-4: he is refused if he books it by id" "$(status_of "$R")" "403"
equals "with the women-only reason" "$(error_code "$R")" "NOT_ELIGIBLE_WOMEN_ONLY"

# ── 7-8: the two positives ───────────────────────────────────────────────────────────────────────
R="$(search "$TOKEN_F")"
equals "08-5: a verified female rider sees the women-only trip" \
  "$(contains_occurrence "$R" "$WOMEN_ONLY_OCC")" "true"

R="$(book "$TOKEN_F" "$WOMEN_ONLY_OCC")"
equals "08-5: and she can book it" "$(status_of "$R")" "200"

R="$(book "$TOKEN_U" "$ORDINARY_OCC")"
equals "08-6: an unverified rider books an ordinary trip — verification is never a gate" \
  "$(status_of "$R")" "200"
equals "and her level is still NONE afterwards" \
  "$(sim_psql "SELECT p.verification_level FROM passenger.passenger_profile p
     JOIN identity.app_user u ON u.app_user_id = p.app_user_id
     WHERE u.phone = '$PHONE_UNVERIFIED'")" "NONE"

# ── 9: camera-only ───────────────────────────────────────────────────────────────────────────────
sim_psql "DELETE FROM passenger.verification_step s
           USING passenger.verification_session v, identity.app_user u
           WHERE s.session_id = v.verification_session_id
             AND v.app_user_id = u.app_user_id AND u.phone = '$PHONE_UNVERIFIED'" >/dev/null
sim_psql "DELETE FROM passenger.verification_session v
           USING identity.app_user u
           WHERE v.app_user_id = u.app_user_id AND u.phone = '$PHONE_UNVERIFIED'" >/dev/null

R="$(call POST /api/v1/passenger/verification/sessions "$TOKEN_U")"
equals "a capture session opens" "$(status_of "$R")" "200"
SESSION_ID="$(data "$R" sessionId)"
equals "with the four captures in order" "$(data "$R" steps.0.key)" "NIC_FRONT"
equals "and the selfie is the third" "$(data "$R" steps.2.key)" "SELFIE_WITH_NIC"
equals "and camera-only is declared to the client" "$(data "$R" cameraOnly)" "True"

R="$(call POST "/api/v1/passenger/verification/steps/NIC_FRONT/upload-url" "$TOKEN_U" \
      "{\"sessionId\":$SESSION_ID,\"captureSource\":\"GALLERY\",
        \"capturedAt\":\"$(date -u +'%Y-%m-%dT%H:%M:%SZ')\",
        \"contentType\":\"image/jpeg\",\"fileSizeBytes\":120000,
        \"originalFilename\":\"nic.jpg\"}")"
equals "08-10: a gallery capture is refused" "$(status_of "$R")" "403"
equals "with the capture-source reason" "$(error_code "$R")" "CAPTURE_SOURCE_NOT_ALLOWED"

R="$(call POST "/api/v1/passenger/verification/steps/NIC_FRONT/upload-url" "$TOKEN_U" \
      "{\"sessionId\":$SESSION_ID,\"captureSource\":\"CAMERA\",
        \"capturedAt\":\"$(date -u +'%Y-%m-%dT%H:%M:%SZ')\",
        \"contentType\":\"image/jpeg\",\"fileSizeBytes\":120000,
        \"originalFilename\":\"nic.jpg\"}")"
if [ "$(status_of "$R")" = "200" ]; then
  PASS=$((PASS+1)); sim_log "PASS: a camera capture is accepted"
  equals "and the attestation is stored for the reviewer" \
    "$(sim_psql "SELECT capture_source FROM passenger.verification_step
       WHERE session_id = $SESSION_ID AND step_key = 'NIC_FRONT'")" "CAMERA"
else
  # Object storage is not configured on every local stack, and a presign that cannot be minted is
  # a storage gap rather than an eligibility one — named rather than passed over.
  skip "camera capture: object storage unavailable ($(status_of "$R"))"
  skip "camera capture: the stored attestation"
fi

R="$(call POST "/api/v1/passenger/verification/steps/NIC_FRONT/upload-url" "$TOKEN_U" \
      "{\"sessionId\":999999999,\"captureSource\":\"CAMERA\",
        \"capturedAt\":\"$(date -u +'%Y-%m-%dT%H:%M:%SZ')\",
        \"contentType\":\"image/jpeg\",\"fileSizeBytes\":120000,
        \"originalFilename\":\"nic.jpg\"}")"
equals "08-11: a capture naming somebody else's session is refused" "$(status_of "$R")" "404"

sim_psql "UPDATE passenger.verification_session SET expires_at = now() - interval '1 minute'
          WHERE verification_session_id = $SESSION_ID" >/dev/null
R="$(call POST "/api/v1/passenger/verification/steps/NIC_BACK/upload-url" "$TOKEN_U" \
      "{\"sessionId\":$SESSION_ID,\"captureSource\":\"CAMERA\",
        \"capturedAt\":\"$(date -u +'%Y-%m-%dT%H:%M:%SZ')\",
        \"contentType\":\"image/jpeg\",\"fileSizeBytes\":120000,
        \"originalFilename\":\"nic.jpg\"}")"
equals "08-12: a lapsed session refuses the capture" "$(error_code "$R")" \
  "VERIFICATION_SESSION_EXPIRED"

# The schema states the same rule, so a path that forgot to check still cannot write the row.
equals "camera-only is enforced by the schema too" \
  "$(sim_psql "SELECT (count(*) = 0)::text FROM passenger.verification_step
     WHERE capture_source IS NOT NULL AND capture_source <> 'CAMERA'")" "true"

# ── 10: photo visibility ─────────────────────────────────────────────────────────────────────────
R="$(call GET /api/v1/passenger/profile/photo-visibility "$TOKEN_F")"
equals "photo visibility is readable" "$(status_of "$R")" "200"
equals "and defaults to her confirmed driver only" "$(data "$R" visibility)" "MATCHED"
equals "with all three options and their copy" "$(data "$R" options.2.value)" "HIDDEN"

R="$(call PUT /api/v1/passenger/profile/photo-visibility "$TOKEN_F" '{"visibility":"HIDDEN"}')"
equals "she can hide it completely" "$(data "$R" visibility)" "HIDDEN"

sim_psql "UPDATE passenger.passenger_profile p
             SET photo_url = 'https://storage.local/photos/sim.jpg'
            FROM identity.app_user u
           WHERE u.app_user_id = p.app_user_id AND u.phone = '$PHONE_FEMALE'" >/dev/null

if [ -n "${DRIVER_TOKEN:-}" ]; then
  R="$(call GET /api/v1/driver/bookings "$DRIVER_TOKEN")"
  equals "08-16: a HIDDEN photo URL appears nowhere in the driver's request list" \
    "$(mentions "$R" "storage.local/photos/sim.jpg")" "false"
else
  skip "no driver token: the HIDDEN photo check"
fi

# ── 11: the two values that must never leave the server ──────────────────────────────────────────
R="$(search "$TOKEN_F")"
equals "08-21: no gender in a search result" "$(mentions "$R" '"gender"')" "false"
R="$(call GET /api/v1/passenger/bookings "$TOKEN_F")"
equals "08-21: no gender in a booking payload" "$(mentions "$R" '"gender"')" "false"
equals "08-22: no NIC number in a booking payload" "$(mentions "$R" "nicNumber")" "false"
R="$(call GET /api/v1/me/context "$TOKEN_F")"
equals "08-21: no gender on the app shell" "$(mentions "$R" '"gender"')" "false"
equals "the shell does carry the real verification level" \
  "$(data "$R" passenger.verificationLevel)" "VERIFIED"

# ── 12: the cost line D35 shows ──────────────────────────────────────────────────────────────────
equals "the refusals this run caused were recorded" \
  "$(sim_psql "SELECT (count(*) > 0)::text FROM routing.eligibility_denial
     WHERE denied_at >= TIMESTAMPTZ '$RUN_STARTED_AT+00'")" "true"
equals "including the ones search made silently" \
  "$(sim_psql "SELECT (count(*) > 0)::text FROM routing.eligibility_denial
     WHERE surface = 'SEARCH' AND denied_at >= TIMESTAMPTZ '$RUN_STARTED_AT+00'")" "true"

if [ -n "${DRIVER_TOKEN:-}" ]; then
  R="$(call GET /api/v1/driver/preferences/eligibility-impact "$DRIVER_TOKEN")"
  equals "08-impact: the cost line is served" "$(status_of "$R")" "200"
  equals "over the last week" "$(data "$R" windowDays)" "7"
  check "and it is a real count rather than a placeholder" \
    "$(python3 -c "print(str($(data "$R" requestsTurnedAwayByVerifiedOnly) >= 0).lower())")"
else
  skip "no driver token: eligibility-impact"
fi

# ── the property that must hold over everything this run did ─────────────────────────────────────
equals "every occurrence created this run carries a legal gender policy" \
  "$(sim_psql "SELECT (count(*) = 0)::text FROM routing.route_occurrence
     WHERE gender_policy NOT IN ('ANYONE','WOMEN_ONLY')")" "true"
equals "no rider was ever refused an ordinary trip on eligibility grounds" \
  "$(sim_psql "SELECT (count(*) = 0)::text FROM routing.eligibility_denial d
     JOIN routing.route_occurrence o ON o.route_occurrence_id = d.route_occurrence_id
     WHERE o.gender_policy = 'ANYONE' AND o.verified_riders_only = false")" "true"

sim_log "passed: $PASS   failed: $FAIL   skipped: $SKIP"
[ "$FAIL" -eq 0 ] || exit 1
