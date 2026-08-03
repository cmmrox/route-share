#!/usr/bin/env bash
# Seeds a complete, searchable demo route on the LOCAL stack:
#   demo driver (phone OTP) -> driver application -> vehicle -> local approvals (SQL)
#   -> published Colombo Fort -> Nugegoda route with a road-shaped polyline.
# The route then appears in passenger ride search and drives the results/ride-detail/geometry QA.
#
# Usage: scripts/simulation/seed-demo-route.sh [departure-offset-minutes] (default 90)
set -euo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

OFFSET_MINUTES="${1:-90}"
DRIVER_USERNAME="${ROUTESHARE_SIM_DRIVER_USERNAME:-sim-driver}"
DRIVER_PASSWORD="${ROUTESHARE_SIM_DRIVER_PASSWORD:-SimDriver#12345}"
SEATS="${ROUTESHARE_SIM_SEATS:-3}"

sim_require_tools
sim_require_api

sim_log "provisioning Keycloak demo driver '$DRIVER_USERNAME' with DRIVER role (local realm)"
DRIVER_TOKEN="$(sim_keycloak_login_with_roles "$DRIVER_USERNAME" "$DRIVER_PASSWORD" "PASSENGER,DRIVER" comigo-mobile)"

sim_log "submitting driver application"
sim_api "$DRIVER_TOKEN" POST /api/v1/driver/application '{"displayName":"Saman Fernando"}' >/dev/null || true

DRIVER_SUBJECT="$(python3 -c "
import base64,json,sys
payload=sys.argv[1].split('.')[1]
payload+='='*(-len(payload)%4)
print(json.loads(base64.urlsafe_b64decode(payload))['sub'])" "$DRIVER_TOKEN")"

sim_log "approving driver profile locally (SQL, local QA only; subject=$DRIVER_SUBJECT)"
sim_psql "UPDATE driver.driver_profile SET verification_status='APPROVED', updated_at=now()
          WHERE app_user_id=(SELECT app_user_id FROM identity.app_user
                             WHERE keycloak_subject='$DRIVER_SUBJECT')" >/dev/null

REGISTRATION="CAB-$(( RANDOM % 9000 + 1000 ))"
sim_log "creating vehicle $REGISTRATION"
VEHICLE_JSON="$(sim_api "$DRIVER_TOKEN" POST /api/v1/driver/vehicles \
  "{\"make\":\"Toyota\",\"model\":\"Aqua\",\"manufactureYear\":2019,\"color\":\"Blue\",\"registrationNumber\":\"$REGISTRATION\",\"seatCount\":3,\"vehicleClass\":\"CAR\"}")"
VEHICLE_ID="$(sim_json_get "$VEHICLE_JSON" "d['data']['id']")"

sim_log "approving vehicle $VEHICLE_ID locally (SQL, local QA only)"
sim_psql "UPDATE vehicle.vehicle SET status='APPROVED', updated_at=now() WHERE vehicle_id=$VEHICLE_ID" >/dev/null

# Since slice 02 an approved car with no assessed rate band cannot publish at all (D40). The real
# flow is an admin assessment; this is the same local-QA SQL shortcut used for the approvals above.
sim_log "assessing the rate band for vehicle $VEHICLE_ID (SQL, local QA only)"
sim_psql "INSERT INTO vehicle.vehicle_rate_band (vehicle_id, rate_min, rate_max, chosen_rate, status, set_at)
          VALUES ($VEHICLE_ID, 41.00, 58.00, 50.00, 'ACTIVE', now())
          ON CONFLICT (vehicle_id) DO UPDATE
            SET rate_min=41.00, rate_max=58.00, chosen_rate=50.00, status='ACTIVE', set_at=now()" >/dev/null

DEPARTURE="$(python3 -c "from datetime import datetime,timezone,timedelta;print((datetime.now(timezone.utc)+timedelta(minutes=$OFFSET_MINUTES)).strftime('%Y-%m-%dT%H:%M:%SZ'))")"
sim_log "publishing Colombo Fort -> Nugegoda route departing $DEPARTURE ($SEATS seats)"
# Road-shaped waypoints: Fort -> Slave Island -> Union Place -> Thummulla -> Kirulapone -> Nugegoda.
ROUTE_JSON="$(sim_api "$DRIVER_TOKEN" POST /api/v1/driver/routes "{
  \"vehicleId\": $VEHICLE_ID,
  \"originLabel\": \"Colombo Fort\",
  \"destinationLabel\": \"Nugegoda\",
  \"availableSeats\": $SEATS,
  \"departureTime\": \"$DEPARTURE\",
  \"coordinates\": [
    {\"latitude\": 6.9337, \"longitude\": 79.8500},
    {\"latitude\": 6.9270, \"longitude\": 79.8520},
    {\"latitude\": 6.9218, \"longitude\": 79.8562},
    {\"latitude\": 6.9143, \"longitude\": 79.8607},
    {\"latitude\": 6.9061, \"longitude\": 79.8648},
    {\"latitude\": 6.8964, \"longitude\": 79.8690},
    {\"latitude\": 6.8880, \"longitude\": 79.8770},
    {\"latitude\": 6.8794, \"longitude\": 79.8851},
    {\"latitude\": 6.8722, \"longitude\": 79.8920},
    {\"latitude\": 6.8649, \"longitude\": 79.8997}
  ]
}")"
ROUTE_PLAN_ID="$(sim_json_get "$ROUTE_JSON" "d['data']['routePlanId']")"
OCCURRENCE_ID="$(sim_json_get "$ROUTE_JSON" "d['data']['routeOccurrenceId']")"

sim_log "seed complete"
echo "routePlanId=$ROUTE_PLAN_ID"
echo "routeOccurrenceId=$OCCURRENCE_ID"
echo "vehicleId=$VEHICLE_ID"
echo "departure=$DEPARTURE"
