-- Slice 09 — search on where the driver's trip *starts*, and give the rider a landmark to stand at.
--
-- The radius is not a tuning change, it is a different predicate. Until now a candidate was kept if
-- the rider's pickup point was within 1 km of the driver's *route line*. The product asks for 5, 10
-- or 20 km measured from the driver's **trip origin**, with 20 km as a hard ceiling and a stated
-- reason: further than that and a driver is making a trip for you rather than sharing one.
--
-- Pickup proximity does not disappear — it stays a scoring input. It stops being the filter.

-- ── the radius, re-expressed ─────────────────────────────────────────────────────────────────────
-- Changed in place per decision D6: these are pre-launch columns whose meaning is changing, and
-- keeping the old pair alongside the new one would leave two radii in the same row with nothing to
-- say which the query honours.
ALTER TABLE routing.matching_settings DROP COLUMN default_search_radius_meters;
ALTER TABLE routing.matching_settings DROP COLUMN max_search_radius_meters;

ALTER TABLE routing.matching_settings
    ADD COLUMN default_trip_start_radius_m INT NOT NULL DEFAULT 20000;
ALTER TABLE routing.matching_settings
    ADD COLUMN max_trip_start_radius_m INT NOT NULL DEFAULT 20000;
-- The offered set, not a range: the product offers three chips, and a rider typing 7 km is asking
-- for something no screen can render.
ALTER TABLE routing.matching_settings
    ADD COLUMN allowed_trip_start_radii_m INT[] NOT NULL DEFAULT '{5000,10000,20000}';
ALTER TABLE routing.matching_settings
    ADD CONSTRAINT matching_settings_radius_ceiling
    CHECK (default_trip_start_radius_m <= max_trip_start_radius_m);

-- ── the column the whole search now turns on ─────────────────────────────────────────────────────
-- Stored rather than computed. `ST_StartPoint(route_line)` per row per query is the difference
-- between an index seek and a sequential scan over every published route in the country, and this
-- is the hottest query in the product.
ALTER TABLE routing.route_plan ADD COLUMN origin_point geometry(Point, 4326);
UPDATE routing.route_plan SET origin_point = ST_StartPoint(route_line) WHERE origin_point IS NULL;
ALTER TABLE routing.route_plan ALTER COLUMN origin_point SET NOT NULL;

-- Indexed as *geography*, not as geometry, because that is how the search filters: a radius in
-- metres means ST_DWithin over geography, and a plain geometry index is silently ineligible for
-- it. The planner falls back to a sequential scan over every published route in the country and
-- nothing fails — the query just gets slower as the table grows. `TripStartRadiusIT` asserts the
-- plan against five thousand rows so this cannot regress quietly.
CREATE INDEX idx_route_plan_origin ON routing.route_plan USING GIST ((origin_point::geography));

-- A route line and an origin point that disagree would silently mis-file every trip a driver
-- publishes, and nothing downstream could detect it. Kept true by the database rather than by every
-- caller remembering.
CREATE OR REPLACE FUNCTION routing.route_plan_sync_origin_point() RETURNS trigger AS $$
BEGIN
    NEW.origin_point := ST_StartPoint(NEW.route_line);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER route_plan_origin_point_sync
    BEFORE INSERT OR UPDATE OF route_line ON routing.route_plan
    FOR EACH ROW EXECUTE FUNCTION routing.route_plan_sync_origin_point();

-- ── named pickup points ──────────────────────────────────────────────────────────────────────────
-- A raw coordinate is not an instruction. In Colombo a 50 m GPS error puts the pin on the wrong
-- side of Galle Road or outside a different shop, and no amount of filtering helps because the
-- error is in the pin, not in the match. The prototype already knew this: its chat fixture reads
-- "I'll be at the Rajagiriya junction bus halt, not the roundabout."
--
-- Three tiers, layered. CURATED overrides everything and is where real landmark names come from;
-- DERIVED is resolved once per corner and then reused for ever; LEARNED is promoted later from
-- `success_count`, once there is usage data to promote on.
CREATE TABLE routing.pickup_point (
    pickup_point_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label                 TEXT NOT NULL,
    description           TEXT,
    -- "Kerb side, opposite the pharmacy" — the half of the instruction a coordinate cannot carry.
    side_hint             TEXT,
    position              geometry(Point, 4326) NOT NULL,
    source                TEXT NOT NULL
                              CHECK (source IN ('CURATED', 'DERIVED', 'LEARNED')),
    google_place_id       TEXT,
    success_count         INT NOT NULL DEFAULT 0,
    active                BOOLEAN NOT NULL DEFAULT true,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by_app_user_id BIGINT REFERENCES identity.app_user(app_user_id)
);

CREATE INDEX pickup_point_position_gix ON routing.pickup_point USING GIST (position);
-- Curated points are probed first on every resolve, so they get their own narrow index.
CREATE INDEX pickup_point_curated_gix ON routing.pickup_point USING GIST (position)
    WHERE source = 'CURATED' AND active;

-- The row that makes a derived point cost nothing the second time. Without it the same corner is
-- re-resolved through Places for every rider, which is the plan's single largest Google line item.
CREATE UNIQUE INDEX pickup_point_place_uk ON routing.pickup_point (google_place_id)
    WHERE google_place_id IS NOT NULL;

ALTER TABLE booking.booking ADD COLUMN pickup_point_id BIGINT
    REFERENCES routing.pickup_point(pickup_point_id);
ALTER TABLE booking.booking ADD COLUMN dropoff_point_id BIGINT
    REFERENCES routing.pickup_point(pickup_point_id);

-- ── the commuter dashboard ───────────────────────────────────────────────────────────────────────
-- A saved search, not a new domain. The match count is the ordinary search run with a small window,
-- so there is one search path and nothing to fall out of step with it.
CREATE TABLE passenger.usual_commute (
    app_user_id       BIGINT PRIMARY KEY REFERENCES identity.app_user(app_user_id),
    origin_label      TEXT NOT NULL,
    origin            geometry(Point, 4326) NOT NULL,
    destination_label TEXT NOT NULL,
    destination       geometry(Point, 4326) NOT NULL,
    habitual_time     TIME,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── share codes ──────────────────────────────────────────────────────────────────────────────────
-- Ten base32 characters, ~50 bits. A short code is handed round in a WhatsApp group, so it must be
-- unguessable rather than merely unique — an enumerable code would let anyone walk the trip table.
-- Revocation is a timestamp rather than a delete, because "who did I share this with" survives it.
CREATE TABLE routing.route_occurrence_share (
    route_occurrence_share_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    route_occurrence_id       BIGINT NOT NULL UNIQUE
                                  REFERENCES routing.route_occurrence(route_occurrence_id),
    short_code                TEXT NOT NULL UNIQUE CHECK (char_length(short_code) = 10),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at                TIMESTAMPTZ
);

CREATE INDEX route_occurrence_share_code_idx
    ON routing.route_occurrence_share (short_code) WHERE revoked_at IS NULL;

-- ── two de-duplications, both deliberate ─────────────────────────────────────────────────────────
--
-- 1. The radius had three candidate homes: `matching_settings` (which has an admin screen and
--    validation), the `SEARCH_RADIUS_KM` policy row seeded by V029, and the three new policy keys
--    this slice was specified to add. Three copies of one number is how a search screen and an
--    admin screen end up disagreeing about what the product offers. `matching_settings` wins — it
--    is the only one with an operator surface — and the orphaned policy row goes.
--
--    `SEARCH_RADIUS_KM` was never read by anything: it was seeded in V029 describing exactly this
--    slice's rule ("Trips must start within this radius of the rider's pickup") and then waited
--    here unused. Removing it now is cheaper than removing it after something starts reading it.
DELETE FROM platform.policy_setting WHERE policy_key = 'SEARCH_RADIUS_KM';
DELETE FROM platform.policy_setting_history WHERE policy_key = 'SEARCH_RADIUS_KM';

-- 2. The match-tier thresholds are deliberately *not* added. FULL/MOST/PART are the same three
--    numbers as MATCH_DISCOUNT_THRESHOLD_HIGH/MID/LOW, and a second copy is precisely how a rider
--    ends up seeing "Full route" beside an 8% discount. One thresholds table, two consumers —
--    which is what this slice's own design note asks for, even though its config section listed
--    new keys.
