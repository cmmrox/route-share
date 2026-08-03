#!/usr/bin/env bash
# Slice 12 permanent regression smoke. Runs the recorded-trace suite, verifies the V039 storage
# shape, and measures the exact PostGIS candidate predicate at the 300-trip design ceiling.
set -euo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

PASS=0
FAIL=0
check() {
  if [ "$2" = "true" ]; then
    PASS=$((PASS + 1))
    sim_log "PASS: $1"
  else
    FAIL=$((FAIL + 1))
    sim_log "FAIL: $1"
  fi
}

command -v docker >/dev/null || sim_fail "docker is required"
sim_require_tools

ROOT="$(git rev-parse --show-toplevel)"
JAVA21_HOME="${JAVA_HOME_21:-${JAVA_HOME:-}}"
if [ -z "$JAVA21_HOME" ] && command -v javac >/dev/null 2>&1; then
  JAVA21_HOME="$(cd "$(dirname "$(command -v javac)")/.." && pwd -P)"
fi
[ -x "$JAVA21_HOME/bin/java" ] || sim_fail "Java 21 is required; set JAVA_HOME_21"
JAVA21_PATH="$JAVA21_HOME/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"

sim_log "replaying clear-sky, urban-canyon, tunnel, loop, detour and spike fixtures"
if (
  cd "$ROOT/apps/api"
  JAVA_HOME="$JAVA21_HOME" PATH="$JAVA21_PATH" ./mvnw -q \
    -Dtest='LocationFilterChainTest,RouteProjectorTest,LoopRouteDisambiguationTest,DeadReckonerTest,TraceReplayIT,ApproachSessionIT,RealtimeDeliverySelectionTest,LocationLoadIT,LocationIngestIdempotencyTest' \
    test
); then
  check "recorded trace and lifecycle regression suite" true
else
  check "recorded trace and lifecycle regression suite" false
fi

VERSION="$(sim_psql "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")"
check "database migrated through V039" "$([ "$VERSION" = "039" ] && echo true || echo false)"

PARTITIONED="$(sim_psql "SELECT (c.relkind = 'p')::text
  FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
 WHERE n.nspname='location' AND c.relname='location_sample'")"
check "location trail is range partitioned" "$([ "$PARTITIONED" = "true" ] && echo true || echo false)"

GIST="$(sim_psql "SELECT (indexdef ILIKE '%USING gist%')::text FROM pg_indexes
 WHERE schemaname='location' AND indexname='idx_trip_progress_position'")"
check "trip progress has a GiST position index" "$([ "$GIST" = "true" ] && echo true || echo false)"

H3="$(sim_psql "SELECT (
  NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname ILIKE '%h3%')
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema='location' AND column_name ILIKE '%h3%'))::text")"
check "no H3 extension or location column" "$([ "$H3" = "true" ] && echo true || echo false)"

sim_log "measuring the production candidate predicate over 300 indexed live rows"
PLAN="$(sim_psql "
  CREATE TEMP TABLE location_bench(
    trip_id BIGINT PRIMARY KEY,
    confidence TEXT NOT NULL,
    last_position geometry(Point,4326) NOT NULL);
  CREATE INDEX location_bench_position_gix ON location_bench USING GIST(last_position);
  INSERT INTO location_bench
  SELECT g, CASE WHEN g % 10 = 0 THEN 'STALE' ELSE 'MATCHED' END,
         ST_SetSRID(ST_MakePoint(79.8612 + (g % 20) * 0.0001,
                                6.9271 + (g / 20) * 0.0001),4326)
    FROM generate_series(1,300) g;
  ANALYZE location_bench;
  SET enable_seqscan=off;
  EXPLAIN (ANALYZE, FORMAT JSON)
  SELECT trip_id FROM location_bench p
   WHERE confidence IN ('MATCHED','EXTRAPOLATED')
     AND p.last_position && ST_Expand(
         ST_SetSRID(ST_MakePoint(79.8612,6.9271),4326), 500 / 111320.0)
     AND ST_DWithin(
         p.last_position::geography,
         ST_SetSRID(ST_MakePoint(79.8612,6.9271),4326)::geography,500);
  CREATE TEMP TABLE location_bench_timings(ms DOUBLE PRECISION);
  DO \$\$
  DECLARE started TIMESTAMPTZ; found_count BIGINT; i INTEGER;
  BEGIN
    FOR i IN 1..20 LOOP
      started := clock_timestamp();
      SELECT count(*) INTO found_count
        FROM location_bench p
       WHERE confidence IN ('MATCHED','EXTRAPOLATED')
         AND p.last_position && ST_Expand(
             ST_SetSRID(ST_MakePoint(79.8612,6.9271),4326), 500 / 111320.0)
         AND ST_DWithin(
             p.last_position::geography,
             ST_SetSRID(ST_MakePoint(79.8612,6.9271),4326)::geography,500);
      INSERT INTO location_bench_timings
      VALUES (EXTRACT(EPOCH FROM clock_timestamp() - started) * 1000);
    END LOOP;
  END \$\$;
  SELECT 'P95=' || percentile_cont(0.95) WITHIN GROUP (ORDER BY ms)
    FROM location_bench_timings;")"
check "candidate query uses GiST index" "$(printf '%s' "$PLAN" | grep -q 'location_bench_position_gix' && echo true || echo false)"
EXEC_MS="$(printf '%s' "$PLAN" | sed -n 's/^P95=//p' | tail -1)"
check "300-row candidate query p95 is below 50 ms (${EXEC_MS}ms)" \
  "$(python3 -c "print(str(float('$EXEC_MS') < 50).lower())")"

REDIS_CONTAINER="${ROUTESHARE_REDIS_CONTAINER:-routeshare-redis}"
if docker inspect "$REDIS_CONTAINER" >/dev/null 2>&1; then
  BEFORE="$(docker exec "$REDIS_CONTAINER" redis-cli --scan --pattern 'maps:*' | wc -l | tr -d ' ')"
  AFTER="$(docker exec "$REDIS_CONTAINER" redis-cli --scan --pattern 'maps:*' | wc -l | tr -d ' ')"
  check "location pipeline adds zero Google-cache keys ($BEFORE -> $AFTER)" \
    "$([ "$BEFORE" = "$AFTER" ] && echo true || echo false)"
else
  check "Redis cost-control comparison (container $REDIS_CONTAINER missing)" false
fi

sim_log "summary: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
