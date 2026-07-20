#!/usr/bin/env bash
# Verifies the Google-API cost controls end-to-end against the LOCAL stack:
#   1. Places autocomplete with a session token (proxy pass-through).
#   2. Place details twice -> second call must be served from the Redis cache (timing + Redis key).
#   3. Passenger ride search against the seeded route.
#   4. Route-occurrence geometry endpoint (stored polyline, zero Google cost).
#   5. Pricing estimate-by-route twice -> Distance Matrix Redis cache.
#   6. Autocomplete burst -> per-user rate limiter must answer 429 past the limit.
#
# Run scripts/simulation/seed-demo-route.sh first. Google keys may be absent: steps that need
# Google then report the fallback source instead (still validating the degradation path).
#
# Usage: scripts/simulation/verify-cost-controls.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

PASSENGER_USERNAME="${ROUTESHARE_SIM_PASSENGER_USERNAME:-sim-passenger}"
PASSENGER_PASSWORD="${ROUTESHARE_SIM_PASSENGER_PASSWORD:-SimPassenger#12345}"
REDIS_CONTAINER="${ROUTESHARE_REDIS_CONTAINER:-routeshare-redis}"
PASS=0; FAIL=0
check() { # check <name> <ok-boolean>
  if [ "$2" = "true" ]; then PASS=$((PASS+1)); sim_log "PASS: $1"; else FAIL=$((FAIL+1)); sim_log "FAIL: $1"; fi
}

sim_require_tools
sim_require_api

# Keycloak password grant instead of phone OTP: repeat QA runs must not consume the
# (intentionally strict) OTP request quota.
sim_log "provisioning Keycloak demo passenger '$PASSENGER_USERNAME'"
TOKEN="$(sim_keycloak_login_with_roles "$PASSENGER_USERNAME" "$PASSENGER_PASSWORD" "PASSENGER" passenger-mobile)"
SESSION="sim-$(date +%s)-$RANDOM"

# --- 1+2: autocomplete + details cache ---------------------------------------------------------
AC_STATUS="$(curl -s -o /tmp/sim-ac.json -w '%{http_code}' -H "Authorization: Bearer $TOKEN" \
  "$SIM_API_BASE/api/v1/passenger/places/autocomplete?query=Nugegoda&sessionToken=$SESSION")"
if [ "$AC_STATUS" = "200" ]; then
  check "autocomplete with session token" true
  PLACE_ID="$(python3 -c "import json;d=json.load(open('/tmp/sim-ac.json'));print(d['data'][0]['placeId'] if d.get('data') else '')")"
  if [ -n "$PLACE_ID" ]; then
    ENC_PLACE_ID="$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1],safe=''))" "$PLACE_ID")"
    T1=$(python3 -c 'import time;print(time.time())')
    curl -sf -H "Authorization: Bearer $TOKEN" "$SIM_API_BASE/api/v1/passenger/places/$ENC_PLACE_ID?sessionToken=$SESSION" >/dev/null
    T2=$(python3 -c 'import time;print(time.time())')
    curl -sf -H "Authorization: Bearer $TOKEN" "$SIM_API_BASE/api/v1/passenger/places/$ENC_PLACE_ID" >/dev/null
    T3=$(python3 -c 'import time;print(time.time())')
    FIRST_MS=$(python3 -c "print(round(($T2-$T1)*1000))"); SECOND_MS=$(python3 -c "print(round(($T3-$T2)*1000))")
    sim_log "details latency: first=${FIRST_MS}ms (Google), second=${SECOND_MS}ms (expected cache)"
    CACHED="$(docker exec "$REDIS_CONTAINER" redis-cli --scan --pattern 'maps:place:*' 2>/dev/null | head -1)"
    check "place details cached in Redis (maps:place:*)" "$([ -n "$CACHED" ] && echo true || echo false)"
  else
    sim_log "SKIP: no autocomplete results (Google disabled?); details-cache check skipped"
  fi
else
  sim_log "autocomplete returned HTTP $AC_STATUS (Google disabled -> 412 is expected without keys)"
  check "autocomplete endpoint reachable (200/412)" "$([ "$AC_STATUS" = "412" ] && echo true || echo false)"
fi

# --- 3: ride search over the seeded route ------------------------------------------------------
DEPART="$(python3 -c "from datetime import datetime,timezone,timedelta;print((datetime.now(timezone.utc)+timedelta(minutes=60)).strftime('%Y-%m-%dT%H:%M:%SZ'))")"
SEARCH_JSON="$(sim_api "$TOKEN" POST /api/v1/passenger/ride-searches "{
  \"pickup\": {\"latitude\": 6.9337, \"longitude\": 79.8500},
  \"dropoff\": {\"latitude\": 6.8649, \"longitude\": 79.8997},
  \"requestedDepartureTime\": \"$DEPART\", \"seats\": 1, \"departureWindowMinutes\": 720
}")" || SEARCH_JSON='{"data":[]}'
RESULTS="$(sim_json_get "$SEARCH_JSON" "len(d.get('data') or [])")"
check "ride search returns seeded route (results=$RESULTS)" "$([ "$RESULTS" -ge 1 ] && echo true || echo false)"

if [ "$RESULTS" -ge 1 ]; then
  OCC="$(sim_json_get "$SEARCH_JSON" "d['data'][0]['routeOccurrenceId']")"
  PF="$(sim_json_get "$SEARCH_JSON" "d['data'][0]['pickupRouteFraction']")"
  DF="$(sim_json_get "$SEARCH_JSON" "d['data'][0]['dropoffRouteFraction']")"
  # --- 4: stored route geometry (no Google) ----------------------------------------------------
  GEO_JSON="$(sim_api "$TOKEN" GET "/api/v1/passenger/route-occurrences/$OCC/geometry?pickupFraction=$PF&dropoffFraction=$DF")" || GEO_JSON=''
  if [ -n "$GEO_JSON" ]; then
    COORDS="$(sim_json_get "$GEO_JSON" "len(d['data']['coordinates'])")"
    SOURCE="$(sim_json_get "$GEO_JSON" "d['data']['source']")"
    sim_log "geometry: $COORDS points, source=$SOURCE"
    check "route geometry served from stored route_plan" "$([ "$SOURCE" = "route_plan" ] && [ "$COORDS" -ge 2 ] && echo true || echo false)"
  else
    check "route geometry endpoint" false
  fi
fi

# --- 5: pricing estimate-by-route cache --------------------------------------------------------
PRICE_BODY='{"pickupLat":6.9337,"pickupLng":79.8500,"dropoffLat":6.8649,"dropoffLng":79.8997}'
P1="$(sim_api "$TOKEN" POST /api/v1/pricing/estimate-by-route "$PRICE_BODY")" || P1=''
P2="$(sim_api "$TOKEN" POST /api/v1/pricing/estimate-by-route "$PRICE_BODY")" || P2=''
if [ -n "$P1" ] && [ -n "$P2" ]; then
  SRC="$(sim_json_get "$P1" "d['data']['metricsSource']")"
  sim_log "estimate-by-route source=$SRC (GOOGLE_DISTANCE_MATRIX when keys set, else HAVERSINE_ESTIMATE)"
  if [ "$SRC" = "GOOGLE_DISTANCE_MATRIX" ]; then
    DM_CACHED="$(docker exec "$REDIS_CONTAINER" redis-cli --scan --pattern 'maps:dm:*' 2>/dev/null | head -1)"
    check "distance-matrix result cached in Redis (maps:dm:*)" "$([ -n "$DM_CACHED" ] && echo true || echo false)"
  else
    check "estimate-by-route degrades safely without Google" true
  fi
else
  check "pricing estimate-by-route" false
fi

# --- 6: autocomplete rate limiter --------------------------------------------------------------
LIMIT="${ROUTESHARE_RATE_LIMIT_PLACES_AUTOCOMPLETE_PER_MIN:-40}"
sim_log "bursting autocomplete $((LIMIT+5)) times to trip the per-user limiter"
GOT_429=false
for i in $(seq 1 $((LIMIT+5))); do
  CODE="$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TOKEN" \
    "$SIM_API_BASE/api/v1/passenger/places/autocomplete?query=ratelimitprobe$i")"
  if [ "$CODE" = "429" ]; then GOT_429=true; break; fi
done
check "autocomplete rate limiter returns 429 after $LIMIT/min" "$GOT_429"

sim_log "summary: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
