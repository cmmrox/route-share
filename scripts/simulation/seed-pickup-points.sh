#!/usr/bin/env bash
# Seeds the curated pickup-point tier for the launch corridors.
#
# This script is a cost control as much as a data load. Every corner it covers is a corner that
# never reaches Google Places: resolved naively, pickup points are the plan's single largest new
# Google line item — roughly 30,000 Place Details calls a month at 500 trips a day, about $150,
# enough on its own to break the monthly credit.
#
# It is also the only tier that carries a real landmark *name*. A derived point can only be
# labelled by its address, because a Places `displayName` is a Pro-tier field and one Pro field
# re-prices the entire request. So "Rajagiriya junction bus halt" can only ever come from here.
#
# Idempotent: a point whose label and position already exist is left alone, so this can be re-run
# after every deploy without duplicating a junction.
#
# Usage:
#   ROUTESHARE_DB_NAME=routeshare_comigo scripts/simulation/seed-pickup-points.sh
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck source=lib.sh
source ./lib.sh

sim_require_tools

# label | description | side hint | latitude | longitude
#
# Chosen for the launch corridors — the Colombo–Kotte/Kaduwela and Galle Road axes — and biased
# toward things a driver can see from the road at 40 km/h: junctions, bus halts, stations and
# buildings nobody needs directions to. A landmark you have to look for is not a landmark.
LANDMARKS=$(cat <<'POINTS'
Fort Railway Station|Main entrance on Olcott Mawatha.|Station side, by the clock|6.93365|79.84980
Pettah Bus Stand|Central bus stand, Bodhiraja Mawatha side.|Kerb outside gate 2|6.93760|79.85560
Slave Island Station|Small station off Sir James Peiris Mawatha.|Station side|6.92330|79.84900
Galle Face Green (north)|Car park end, nearest the Old Parliament.|Sea side of the road|6.92700|79.84260
Kollupitiya Junction|Galle Road at R A de Mel Mawatha.|Liberty Plaza side|6.91170|79.84930
Bambalapitiya Junction|Galle Road at Bauddhaloka Mawatha.|Majestic City side|6.89390|79.85560
Wellawatte Bridge|Galle Road at the canal bridge.|Dehiwala side of the bridge|6.87330|79.86020
Dehiwala Junction|Galle Road at Hill Street.|Zoo side|6.85110|79.86560
Mount Lavinia Station|Station Road, by the level crossing.|Beach side|6.83800|79.86360
Town Hall|Opposite the Viharamahadevi Park entrance.|Park side|6.91690|79.86380
Borella Junction|Baseline Road at Cotta Road.|Cemetery side|6.91500|79.87720
Rajagiriya Junction|Sri Jayawardenepura Mawatha at the flyover.|Bus halt, not the roundabout|6.90890|79.89400
Battaramulla Junction|Pannipitiya Road at the main junction.|Supermarket side|6.89870|79.91830
Koswatte Junction|Parliament Road at Koswatte.|Petrol shed side|6.90650|79.91060
Malabe Junction|Athurugiriya Road at the main junction.|Clock tower side|6.90600|79.95770
Kaduwela Junction|Low Level Road at the town centre.|Bus stand side|6.93330|79.98330
Nugegoda Junction|High Level Road at Stanley Thilakaratne Mawatha.|Supermarket side|6.87280|79.89890
Maharagama Junction|High Level Road at the town centre.|Bus stand side|6.84800|79.92800
Kirulapone Junction|High Level Road at Kirulapone.|Canal side|6.87730|79.87940
Kohuwala Junction|Dutugemunu Street at Kohuwala.|Pharmacy side|6.86420|79.88370
Thimbirigasyaya Junction|Thimbirigasyaya Road at Havelock Road.|Havelock City side|6.89680|79.86720
Havelock Town|Havelock Road near the park.|Park side|6.89050|79.86300
Narahenpita Junction|Kirula Road at Elvitigala Mawatha.|Economic Centre side|6.89170|79.87830
Rajagiriya Cocoon Junction|Nawala Road at the Cocoon building.|Nawala side|6.90370|79.89710
Nawala Junction|Nawala Road at Koswatte Road.|Bank side|6.89020|79.89210
Welikada Junction|Sri Jayawardenepura Mawatha at Welikada.|Market side|6.91120|79.88700
Borella Cemetery Road|Elvitigala Mawatha entrance.|Cemetery wall side|6.91290|79.87540
Kelaniya Temple|Temple Road entrance.|Temple side|6.95530|79.92200
Peliyagoda Junction|Kandy Road at the expressway entrance.|Fish market side|6.96500|79.88500
Wattala Junction|Negombo Road at the town centre.|Church side|6.98940|79.89170
Ja-Ela Junction|Negombo Road at the town centre.|Bus stand side|7.07440|79.89170
Katunayake Airport Arrivals|Terminal 1 arrivals kerb.|Column 4|7.18080|79.88410
Moratuwa Station|Station Road at the level crossing.|Beach side|6.77330|79.88220
Panadura Junction|Galle Road at the town centre.|Clock tower side|6.71330|79.90720
Homagama Junction|High Level Road at the town centre.|Bus stand side|6.84420|80.00220
Kottawa Junction|High Level Road at the expressway entrance.|Bus stand side|6.84120|79.96560
Piliyandala Junction|Horana Road at the town centre.|Market side|6.80180|79.92220
Ratmalana Airport|Domestic terminal entrance.|Terminal side|6.82190|79.88610
Angoda Junction|Kaduwela Road at Angoda.|Hospital side|6.93330|79.91670
Orugodawatta Junction|Baseline Road at the flyover.|Flyover side|6.94720|79.88060
POINTS
)

created=0
skipped=0

# Read on fd 3, not stdin. `sim_psql` shells out to `docker exec -i`, which consumes stdin — so a
# loop reading landmarks from stdin seeds exactly one of them and silently stops.
while IFS='|' read -r label description side_hint lat lng <&3; do
  [ -z "${label:-}" ] && continue
  # Position is part of the identity, not just the label: two different junctions can share a name,
  # and the same junction re-seeded at a slightly better coordinate should update rather than
  # duplicate. Ten metres is well inside GPS noise and well outside "a different corner".
  existing="$(sim_psql "SELECT pickup_point_id FROM routing.pickup_point
                         WHERE source = 'CURATED'
                           AND label = '$(printf '%s' "$label" | sed "s/'/''/g")'
                           AND ST_DWithin(position::geography,
                                          ST_SetSRID(ST_MakePoint($lng, $lat), 4326)::geography, 10)
                         LIMIT 1" | head -1)"
  if [ -n "$existing" ]; then
    skipped=$((skipped + 1))
    continue
  fi
  sim_psql "INSERT INTO routing.pickup_point(label, description, side_hint, position, source)
            VALUES ('$(printf '%s' "$label" | sed "s/'/''/g")',
                    '$(printf '%s' "$description" | sed "s/'/''/g")',
                    '$(printf '%s' "$side_hint" | sed "s/'/''/g")',
                    ST_SetSRID(ST_MakePoint($lng, $lat), 4326), 'CURATED')" >/dev/null
  created=$((created + 1))
done 3<<<"$LANDMARKS"

total="$(sim_psql "SELECT count(*) FROM routing.pickup_point WHERE source = 'CURATED'" | head -1)"
sim_log "curated pickup points: $created created, $skipped already present, $total in total"
