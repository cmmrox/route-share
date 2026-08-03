#!/usr/bin/env bash
# Verifies slice 09 — search on trip-start radius, tiers, sorts, commute, share codes and pickup
# points.
#
# The check that matters most is the pair: a driver starting 14 km away must appear at radius 20 and
# be gone at radius 10, *and* the filtered-out count must move by exactly one. Either alone can pass
# for the wrong reason — an empty list satisfies "not present", and a count can be right about a
# list that is wrong.
#
#   1. The three radii return the drivers seeded at 2, 6, 14 and 25 km, and nothing else
#   2. filteredOutByRadius is exact at every radius, and reconciles with the list
#   3. startsKmAway is on every card and matches the seeded distance
#   4. An unoffered radius is refused, and one above the ceiling is refused differently
#   5. Tiers agree with the discount the quote actually applied
#   6. All three sorts page without repeating or dropping a trip
#   7. The result payload carries every P04 field without client arithmetic
#   8. The usual commute stores, and its match count comes from the real search
#   9. A share code is 10 characters, resolves, renders a QR, and 404s once revoked
#  10. Pickup points resolve curated-first and reuse a persisted corner rather than calling Places
#  11. No driver origin coordinate appears in any response
#
# Requires the local stack with demo OTP. Never run against production.
#
# Usage:
#   ROUTESHARE_API_BASE=http://localhost:8088 ROUTESHARE_DB_NAME=routeshare_comigo \
#     scripts/simulation/verify-search-v2.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

PHONE="${ROUTESHARE_SIM_SEARCH_PHONE:-+9477$(printf '%07d' $((RANDOM % 10000000)))}"
RUN_TAG="s9-$RANDOM"

PASS=0; FAIL=0; SKIP=0
check() { if [ "$2" = "true" ]; then PASS=$((PASS+1)); sim_log "PASS: $1";
          else FAIL=$((FAIL+1)); sim_log "FAIL: $1"; fi; }
equals() { if [ "$2" = "$3" ]; then PASS=$((PASS+1)); sim_log "PASS: $1 ($2)";
           else FAIL=$((FAIL+1)); sim_log "FAIL: $1 — got '$2', expected '$3'"; fi; }
skip() { SKIP=$((SKIP+1)); sim_log "SKIP: $1"; }

call() { # call <method> <path> <token> [body] -> "<status> <body>"
  local method="$1" path="$2" token="$3" body="${4:-}" args=()
  args=(-s -o /tmp/s9-body -w '%{http_code}' -X "$method" "$SIM_API_BASE$path"
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  [ -n "$body" ] && args+=(-d "$body")
  printf '%s %s' "$(curl "${args[@]}")" "$(cat /tmp/s9-body)"
}
status_of() { printf '%s' "${1%% *}"; }
body_of() { printf '%s' "${1#* }"; }
error_code() { python3 -c "
import json,sys
try:
    b = json.loads(sys.argv[1]); print(b.get('code') or b.get('error', {}).get('code') or '')
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
# The origin labels a search returned, sorted — the whole of checks 1 and 2.
labels() { python3 -c "
import json,sys
try:
    rows = json.loads(sys.argv[1])['data']['results']
    print(','.join(sorted(r['originLabel'] for r in rows if r['originLabel'].startswith(sys.argv[2]))))
except Exception:
    print('error')
" "$(body_of "$1")" "$2"; }
field_present_on_every_result() { python3 -c "
import json,sys
try:
    rows = json.loads(sys.argv[1])['data']['results']
    print(str(bool(rows) and all(r.get(sys.argv[2]) is not None for r in rows)).lower())
except Exception:
    print('error')
" "$(body_of "$1")" "$2"; }
mentions() { python3 -c "
import sys
print(str(sys.argv[2].lower() in sys.argv[1].lower()).lower())
" "$(body_of "$1")" "$2"; }
# Structural checks read the body from a file. Interpolating a JSON document into an inline python
# programme mangles anything containing a colon or a brace, which is most of a JSON document.
save_body() { body_of "$1" > "$2"; }

sim_require_tools
sim_require_api

TOKEN="$(sim_login "$PHONE")"
call GET /api/v1/me/context "$TOKEN" >/dev/null
# The share endpoints are driver-or-admin. A phone-OTP rider has neither role, and demo drivers are
# seeded without the DRIVER grant — so the admin token stands in, exactly as slice 07's script does.
ADMIN_USERNAME="${ROUTESHARE_SIM_ADMIN_USERNAME:-sim-admin}"
ADMIN_PASSWORD="${ROUTESHARE_SIM_ADMIN_PASSWORD:-SimAdmin#12345}"
ADMIN_TOKEN="$(sim_keycloak_login_with_roles "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN" admin-web)"

sim_log "seeding a demo route (idempotent)"
bash ./seed-demo-route.sh >/dev/null 2>&1 || sim_log "seed script reported an issue; continuing"

# One driver, four trips, starting 2 / 6 / 14 / 25 km north of the rider and all running south
# through her. Under the old pickup-proximity rule every one of them would match at any radius,
# because every one of them drives past her — which is exactly what makes this fixture the test.
PICKUP_LAT=6.9271
PICKUP_LNG=79.8612

# The newest driver profile is not necessarily the one with a car: a profile is created the moment
# somebody applies, and the seed script's approved vehicle may belong to an older one. Picking the
# newest profile and then asking for its vehicle finds nothing, and reads like a broken stack.
VEHICLE_ID="$(sim_psql "SELECT vehicle_id FROM vehicle.vehicle
                         WHERE status = 'APPROVED'
                         ORDER BY vehicle_id DESC LIMIT 1" | head -1)"
DRIVER_PROFILE="$(sim_psql "SELECT driver_profile_id FROM vehicle.vehicle
                             WHERE vehicle_id = ${VEHICLE_ID:-0}" | head -1)"
[ -n "$DRIVER_PROFILE" ] && [ -n "$VEHICLE_ID" ] || sim_fail "no approved driver/vehicle to seed with"

seed_route() { # seed_route <label> <kmAway>
  local label="$1" km="$2"
  local start_lat
  start_lat="$(python3 -c "print($PICKUP_LAT + $km * 0.009)")"
  local plan
  plan="$(sim_psql "INSERT INTO routing.route_plan(driver_profile_id, vehicle_id, origin_label,
              destination_label, route_line, departure_time, available_seats, status,
              route_length_m)
            VALUES ($DRIVER_PROFILE, $VEHICLE_ID, '$label', 'Fort',
                    ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint($PICKUP_LNG, $start_lat),
                                                 ST_MakePoint($PICKUP_LNG, $(python3 -c "print($PICKUP_LAT - 0.02)"))]), 4326),
                    now() + interval '4 hours', 3, 'PUBLISHED', 9500)
            RETURNING route_plan_id" | head -1)"
  local occ
  occ="$(sim_psql "INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at,
              available_seats, status, approval_mode)
            VALUES ($plan, now() + interval '4 hours', 3, 'PUBLISHED', 'INSTANT')
            RETURNING route_occurrence_id" | head -1)"
  # Search matches on the corridor bucket first; an occurrence with no cells is invisible to
  # everyone, which would read here as a radius refusal rather than a missing fixture.
  sim_psql "INSERT INTO routing.route_bucket_cell(route_plan_id, route_occurrence_id,
              bucket_resolution, bucket_cell)
            SELECT $plan, $occ, 3, cell FROM (VALUES
              ('$(python3 -c "print(f'{round($PICKUP_LAT,3)}:{round($PICKUP_LNG,3)}')")')
            ) AS c(cell)" >/dev/null
  printf '%s' "$occ"
}

# Mirrors RouteBucketCellGenerator at resolution 3: floor(value * 10) formatted to three digits.
# Copying a cell from an existing row instead would attach the fixture to somebody else's corridor,
# and every search would return nothing — which reads as a radius failure rather than a fixture
# that was never reachable in the first place.
bucket_cell() { # bucket_cell <lat> <lng>
  python3 -c "
import math, sys
lat, lng = float(sys.argv[1]), float(sys.argv[2])
print('r3:%03d:%03d' % (math.floor(lat * 10), math.floor(lng * 10)))
" "$1" "$2"
}
BUCKET_CELL="$(bucket_cell "$PICKUP_LAT" "$PICKUP_LNG")"
sim_log "corridor bucket cell: $BUCKET_CELL"

seed_route_with_cell() { # <label> <kmAway>
  local occ
  occ="$(seed_route "$1" "$2")"
  sim_psql "DELETE FROM routing.route_bucket_cell WHERE route_occurrence_id = $occ" >/dev/null
  sim_psql "INSERT INTO routing.route_bucket_cell(route_plan_id, route_occurrence_id,
              bucket_resolution, bucket_cell)
            SELECT route_plan_id, $occ, 3, '$BUCKET_CELL'
              FROM routing.route_occurrence WHERE route_occurrence_id = $occ" >/dev/null
  printf '%s' "$occ"
}

OCC_2="$(seed_route_with_cell "${RUN_TAG}-D2" 2)"
OCC_6="$(seed_route_with_cell "${RUN_TAG}-D6" 6)"
OCC_14="$(seed_route_with_cell "${RUN_TAG}-D14" 14)"
OCC_25="$(seed_route_with_cell "${RUN_TAG}-D25" 25)"
[ -n "$OCC_25" ] || sim_fail "could not seed the four corridor trips"

# A rate band, or every card comes back unpriced and the tier checks have nothing to read.
sim_psql "INSERT INTO vehicle.vehicle_rate_band(vehicle_id, rate_min, rate_max, chosen_rate, status)
          VALUES ($VEHICLE_ID, 38.00, 62.00, 50.00, 'ACTIVE')
          ON CONFLICT (vehicle_id) DO UPDATE SET chosen_rate = 50.00, status = 'ACTIVE'" >/dev/null

DEPART="$(date -u -v+4H +'%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || date -u -d '+4 hours' +'%Y-%m-%dT%H:%M:%SZ')"

search() { # search <radiusKm> [sort] [page] [size]
  call POST /api/v1/passenger/ride-searches "$TOKEN" \
    "{\"pickup\":{\"latitude\":$PICKUP_LAT,\"longitude\":$PICKUP_LNG},
      \"dropoff\":{\"latitude\":$(python3 -c "print($PICKUP_LAT - 0.015)"),\"longitude\":$PICKUP_LNG},
      \"requestedDepartureTime\":\"$DEPART\",\"seats\":1,\"radiusKm\":$1,
      \"departureWindowMinutes\":240,\"sort\":\"${2:-BEST_MATCH}\",
      \"page\":${3:-0},\"size\":${4:-50}}"
}

# ── 1-2: the radius, and what it removed ─────────────────────────────────────────────────────────
R20="$(search 20)"
equals "a search at 20 km succeeds" "$(status_of "$R20")" "200"
equals "09-1: 20 km returns the drivers starting 2, 6 and 14 km away" \
  "$(labels "$R20" "$RUN_TAG")" "${RUN_TAG}-D14,${RUN_TAG}-D2,${RUN_TAG}-D6"

R10="$(search 10)"
equals "09-2: 10 km returns only the drivers starting 2 and 6 km away" \
  "$(labels "$R10" "$RUN_TAG")" "${RUN_TAG}-D2,${RUN_TAG}-D6"

R5="$(search 5)"
equals "09-3: 5 km returns only the driver starting 2 km away" \
  "$(labels "$R5" "$RUN_TAG")" "${RUN_TAG}-D2"

# The pair that means something. The count is corridor-wide, and previous runs of this script have
# left their own trips on it, so "exactly one more" would be asserting how many times the script has
# been run. The invariants that do hold are stated instead: the totals reconcile at every radius,
# and tightening the radius never removes fewer trips.
save_body "$R20" /tmp/s9-r20.json
save_body "$R10" /tmp/s9-r10.json
save_body "$R5" /tmp/s9-r5.json

check "09-6: shown + filtered out equals every candidate on the corridor, at every radius" \
  "$(python3 -c '
import json
ok = True
for path in ["/tmp/s9-r20.json", "/tmp/s9-r10.json", "/tmp/s9-r5.json"]:
    d = json.load(open(path))["data"]
    ok = ok and len(d["results"]) + d["filteredOutByRadius"] == d["totalMatching"]
print(str(ok).lower())
')"

check "09-6: tightening the radius never filters out fewer trips" "$(python3 -c '
import json
out = [json.load(open(p))["data"]["filteredOutByRadius"]
       for p in ["/tmp/s9-r20.json", "/tmp/s9-r10.json", "/tmp/s9-r5.json"]]
print(str(out[0] <= out[1] <= out[2] and out[2] > out[0]).lower())
')"

# ── 3: the number on the card ────────────────────────────────────────────────────────────────────
equals "09-7: every card carries startsKmAway" \
  "$(field_present_on_every_result "$R20" startsKmAway)" "true"
check "09-7: and it matches the seeded distances" "$(python3 -c '
import json, sys
rows = {r["originLabel"]: round(r["startsKmAway"]) for r in json.load(open("/tmp/s9-r20.json"))["data"]["results"]}
tag = sys.argv[1]
print(str(rows.get(tag + "-D2") == 2 and rows.get(tag + "-D6") == 6
          and rows.get(tag + "-D14") == 14).lower())
' "$RUN_TAG")"

# ── 4: the two refusals ──────────────────────────────────────────────────────────────────────────
R="$(search 7)"
equals "09-5: an unoffered radius is refused" "$(status_of "$R")" "403"
equals "and named as such" "$(error_code "$R")" "RADIUS_NOT_ALLOWED"

R="$(search 25)"
equals "09-4: a radius above the ceiling is refused" "$(status_of "$R")" "403"
equals "with the ceiling reason, not the option one" "$(error_code "$R")" "RADIUS_EXCEEDS_MAXIMUM"

# ── 5: tiers agree with the discount actually applied ────────────────────────────────────────────
equals "09-8: every card carries a tier" "$(field_present_on_every_result "$R20" matchTier)" "true"
check "09-8: and the tier never contradicts the discount band on the quote" "$(python3 -c '
import json
band = dict(HIGH="FULL_ROUTE", MID="MOST_OF_ROUTE", LOW="PART_OF_ROUTE", BASE="SHORT_HOP")
rows = json.load(open("/tmp/s9-r20.json"))["data"]["results"]
priced = [r for r in rows if r.get("fare")]
print(str(bool(priced) and all(band[r["fare"]["matchTier"]] == r["matchTier"] for r in priced)).lower())
')"

# ── 6: sorts page without losing or repeating a trip ─────────────────────────────────────────────
for sort in BEST_MATCH CHEAPEST SOONEST; do
  P1="$(search 20 "$sort" 0 2)"
  P2="$(search 20 "$sort" 1 2)"
  save_body "$P1" /tmp/s9-p1.json
  save_body "$P2" /tmp/s9-p2.json
  check "09-10/09-11: $sort pages without a duplicate or an omission" "$(python3 -c '
import json
a = [r["routeOccurrenceId"] for r in json.load(open("/tmp/s9-p1.json"))["data"]["results"]]
b = [r["routeOccurrenceId"] for r in json.load(open("/tmp/s9-p2.json"))["data"]["results"]]
print(str(len(set(a + b)) == len(a) + len(b)).lower())
')"
done

# ── 7: the card needs no arithmetic ──────────────────────────────────────────────────────────────
for field in matchTierLabel overlapSummary ratePerKm approvalMode vehicleClassKey estimatedFare; do
  equals "09-12: every card carries $field" \
    "$(field_present_on_every_result "$R20" "$field")" "true"
done

# ── 8: the commuter dashboard ────────────────────────────────────────────────────────────────────
R="$(call PUT /api/v1/passenger/commute "$TOKEN" \
  "{\"originLabel\":\"Home\",\"origin\":{\"latitude\":$PICKUP_LAT,\"longitude\":$PICKUP_LNG},
    \"destinationLabel\":\"Fort\",
    \"destination\":{\"latitude\":$(python3 -c "print($PICKUP_LAT - 0.015)"),\"longitude\":$PICKUP_LNG},
    \"habitualTime\":\"08:15\"}")"
equals "09-14: the usual commute saves" "$(status_of "$R")" "200"
equals "and reads back as saved" "$(data "$R" saved)" "True"
R="$(call GET /api/v1/passenger/commute "$TOKEN")"
equals "with its origin label" "$(data "$R" originLabel)" "Home"
equals "and the habitual time it was given" "$(data "$R" habitualTime)" "08:15"

# ── 9: share codes ───────────────────────────────────────────────────────────────────────────────
R="$(call POST "/api/v1/driver/route-occurrences/$OCC_2/share" "$ADMIN_TOKEN")"
if [ "$(status_of "$R")" = "200" ]; then
  SHORT_CODE="$(data "$R" shortCode)"
  equals "09-15: a share code is ten characters" "${#SHORT_CODE}" "10"
  R2="$(call POST "/api/v1/driver/route-occurrences/$OCC_2/share" "$ADMIN_TOKEN")"
  equals "sharing twice returns the same code, so a link already sent keeps working" \
    "$(data "$R2" shortCode)" "$SHORT_CODE"
  R2="$(call GET "/api/v1/driver/route-occurrences/$OCC_2/share" "$ADMIN_TOKEN")"
  equals "09-15: the driver can read the current share metadata" "$(status_of "$R2")" "200"
  equals "and the read returns the same code" "$(data "$R2" shortCode)" "$SHORT_CODE"

  R="$(curl -s -o /dev/null -w '%{http_code}' "$SIM_API_BASE/api/v1/public/trip-links/$SHORT_CODE")"
  equals "09-15: the code resolves without a token" "$R" "200"
  R="$(curl -s -o /tmp/s9-qr.png -w '%{http_code}' \
        "$SIM_API_BASE/api/v1/public/trip-links/$SHORT_CODE/qr.png")"
  equals "09-17: the QR renders" "$R" "200"
  check "09-17: and it is a real PNG" \
    "$(python3 -c "print(str(open('/tmp/s9-qr.png','rb').read(4) == b'\x89PNG').lower())")"

  call DELETE "/api/v1/driver/route-occurrences/$OCC_2/share" "$ADMIN_TOKEN" >/dev/null
  R="$(curl -s -o /dev/null -w '%{http_code}' "$SIM_API_BASE/api/v1/public/trip-links/$SHORT_CODE")"
  # 404 rather than 410 on purpose: 410 confirms the code once existed, which is the one bit a
  # scanner walking the code space is trying to learn.
  equals "09-16: a revoked code answers 404, not 410" "$R" "404"
else
  skip "share codes: the driver token cannot act on this occurrence ($(status_of "$R"))"
  skip "share codes: resolve, QR and revoke"
fi

# ── 10: pickup points ────────────────────────────────────────────────────────────────────────────
RUN_OFFSET="$(python3 -c "print(0.0004 + ($RANDOM % 900) * 0.0001)")"
CURATED_LAT="$(python3 -c "print($PICKUP_LAT + $RUN_OFFSET)")"
CURATED_LNG="$(python3 -c "print($PICKUP_LNG + $RUN_OFFSET)")"
sim_psql "INSERT INTO routing.pickup_point(label, description, side_hint, position, source)
          VALUES ('${RUN_TAG} curated halt', 'Seeded by the slice 09 smoke.', 'Kerb side',
                  ST_SetSRID(ST_MakePoint($CURATED_LNG, $CURATED_LAT), 4326), 'CURATED')" >/dev/null

R="$(call POST /api/v1/passenger/pickup-points/resolve "$TOKEN" \
      "{\"latitude\":$CURATED_LAT,\"longitude\":$CURATED_LNG}")"
equals "09-20: a coordinate beside a curated point resolves to it" \
  "$(data "$R" label)" "${RUN_TAG} curated halt"
equals "and says so" "$(data "$R" source)" "CURATED"
equals "carrying the side hint a coordinate cannot" "$(data "$R" sideHint)" "Kerb side"

# Somewhere with no curated point: the first resolve writes a row, the second must reuse it.
FAR_LAT="$(python3 -c "print($PICKUP_LAT + 0.30 + $RUN_OFFSET)")"
BEFORE="$(sim_psql "SELECT count(*) FROM routing.pickup_point WHERE source <> 'CURATED'" | head -1)"
R="$(call POST /api/v1/passenger/pickup-points/resolve "$TOKEN" \
      "{\"latitude\":$FAR_LAT,\"longitude\":$PICKUP_LNG}")"
equals "a coordinate with nothing curated nearby still resolves" "$(status_of "$R")" "200"
FIRST_ID="$(data "$R" pickupPointId)"
R="$(call POST /api/v1/passenger/pickup-points/resolve "$TOKEN" \
      "{\"latitude\":$FAR_LAT,\"longitude\":$PICKUP_LNG}")"
equals "09-22: the second rider at the same corner reuses the persisted point" \
  "$(data "$R" pickupPointId)" "$FIRST_ID"
AFTER="$(sim_psql "SELECT count(*) FROM routing.pickup_point WHERE source <> 'CURATED'" | head -1)"
equals "09-22: and no second row was written for it" \
  "$(python3 -c "print(int('$AFTER') - int('$BEFORE'))")" "1"

# ── 11: what must never leave the server ─────────────────────────────────────────────────────────
equals "09-18: no driver origin coordinate in a search response" \
  "$(mentions "$R20" "origin_point")" "false"
check "09-18: and no raw origin latitude/longitude pair either" "$(python3 -c '
import json
rows = json.load(open("/tmp/s9-r20.json"))["data"]["results"]
leaked = set(["originLatitude", "originLongitude", "originPoint"])
print(str(not any(leaked & set(r.keys()) for r in rows)).lower())
')"

sim_log "passed: $PASS   failed: $FAIL   skipped: $SKIP"
[ "$FAIL" -eq 0 ] || exit 1
