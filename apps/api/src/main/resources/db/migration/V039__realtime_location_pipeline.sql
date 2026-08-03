-- Slice 12 — route-constrained real-time location, approach privacy and realtime channels.
--
-- PostgreSQL requires every UNIQUE constraint on a range-partitioned table to contain the
-- partition key. Global (trip_id, sample_id) idempotency therefore lives in the small unpartitioned
-- location_sample_dedupe table while the high-volume trail is partitioned by receive month.

ALTER TABLE location.location_sample RENAME TO location_sample_pre_v039;

CREATE TABLE location.location_sample (
    location_sample_id BIGINT GENERATED ALWAYS AS IDENTITY,
    trip_id BIGINT NOT NULL REFERENCES trip.trip(trip_id),
    driver_profile_id BIGINT NOT NULL REFERENCES driver.driver_profile(driver_profile_id),
    sample_id TEXT,
    point geometry(Point, 4326) NOT NULL,
    accuracy_m NUMERIC(8,2),
    speed_mps NUMERIC(8,2),
    bearing_degrees NUMERIC(6,2),
    battery_pct SMALLINT CHECK (battery_pct BETWEEN 0 AND 100),
    device_recorded_at TIMESTAMPTZ NOT NULL,
    server_received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    source TEXT NOT NULL DEFAULT 'DRIVER_APP',
    accepted BOOLEAN NOT NULL DEFAULT true,
    rejection_reason TEXT,
    route_fraction NUMERIC(9,8),
    route_offset_meters NUMERIC(10,2),
    PRIMARY KEY (location_sample_id, server_received_at),
    UNIQUE (trip_id, sample_id, server_received_at)
) PARTITION BY RANGE (server_received_at);

DO $$
DECLARE
    month_start DATE := date_trunc('month', CURRENT_DATE)::date;
    next_month DATE := (month_start + INTERVAL '1 month')::date;
    following_month DATE := (month_start + INTERVAL '2 months')::date;
BEGIN
    EXECUTE format(
        'CREATE TABLE location.location_sample_%s PARTITION OF location.location_sample
           FOR VALUES FROM (%L) TO (%L)',
        to_char(month_start, 'YYYY_MM'), month_start, next_month);
    EXECUTE format(
        'CREATE TABLE location.location_sample_%s PARTITION OF location.location_sample
           FOR VALUES FROM (%L) TO (%L)',
        to_char(next_month, 'YYYY_MM'), next_month, following_month);
END $$;

CREATE TABLE location.location_sample_default
    PARTITION OF location.location_sample DEFAULT;

INSERT INTO location.location_sample (
    trip_id, driver_profile_id, point, accuracy_m, speed_mps, bearing_degrees,
    device_recorded_at, server_received_at, source)
SELECT trip_id, driver_profile_id, point, accuracy_m, speed_mps, bearing_degrees,
       device_recorded_at, server_received_at, source
  FROM location.location_sample_pre_v039;

DROP TABLE location.location_sample_pre_v039;

CREATE INDEX location_sample_trip_idx
    ON location.location_sample(trip_id, server_received_at DESC);
CREATE INDEX idx_location_sample_trip_device_time
    ON location.location_sample(trip_id, device_recorded_at DESC);
CREATE INDEX location_sample_point_gix
    ON location.location_sample USING GIST(point);
CREATE INDEX idx_location_sample_retention
    ON location.location_sample(server_received_at);

CREATE TABLE location.location_sample_dedupe (
    trip_id BIGINT NOT NULL REFERENCES trip.trip(trip_id) ON DELETE CASCADE,
    sample_id TEXT NOT NULL,
    server_received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (trip_id, sample_id)
);

CREATE TABLE location.trip_progress (
    trip_id BIGINT PRIMARY KEY REFERENCES trip.trip(trip_id) ON DELETE CASCADE,
    route_fraction NUMERIC(9,8) NOT NULL CHECK (route_fraction BETWEEN 0 AND 1),
    confidence TEXT NOT NULL
        CHECK (confidence IN ('MATCHED', 'EXTRAPOLATED', 'STALE', 'OFF_ROUTE')),
    matched_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    speed_mps NUMERIC(6,2),
    bearing_degrees NUMERIC(6,2),
    off_route_since TIMESTAMPTZ,
    last_position geometry(Point, 4326) NOT NULL,
    reversal_candidate_fraction NUMERIC(9,8),
    reversal_candidate_count SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT trip_progress_reversal_count_ck
        CHECK (reversal_candidate_count BETWEEN 0 AND 2)
);

CREATE INDEX idx_trip_progress_position
    ON location.trip_progress USING GIST(last_position);
CREATE INDEX idx_trip_progress_confidence
    ON location.trip_progress(confidence, route_fraction);
CREATE INDEX idx_trip_progress_updated
    ON location.trip_progress(updated_at);

CREATE TABLE location.approach_session (
    approach_session_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trip.trip(trip_id) ON DELETE CASCADE,
    booking_id BIGINT NOT NULL REFERENCES booking.booking(booking_id) ON DELETE CASCADE,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    rider_position geometry(Point, 4326),
    rider_position_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX approach_session_open_booking_uk
    ON location.approach_session(booking_id) WHERE closed_at IS NULL;
CREATE INDEX idx_approach_open
    ON location.approach_session(trip_id) WHERE closed_at IS NULL;

CREATE TABLE location.realtime_token (
    token_hash TEXT PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX realtime_token_expiry_idx ON location.realtime_token(expires_at);

CREATE TABLE location.realtime_channel (
    realtime_channel_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id) ON DELETE CASCADE,
    connection_id TEXT NOT NULL UNIQUE,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    transport TEXT NOT NULL CHECK (transport IN ('WS', 'SSE'))
);
CREATE INDEX realtime_channel_user_idx
    ON location.realtime_channel(app_user_id, last_seen_at DESC);

CREATE OR REPLACE FUNCTION location.ensure_location_sample_partitions()
RETURNS INTEGER LANGUAGE plpgsql AS $$
DECLARE
    month_start DATE;
    next_month DATE;
    partition_name TEXT;
    created_count INTEGER := 0;
BEGIN
    FOR month_offset IN 0..2 LOOP
        month_start := (date_trunc('month', CURRENT_DATE) + (month_offset || ' months')::interval)::date;
        next_month := (month_start + INTERVAL '1 month')::date;
        partition_name := 'location_sample_' || to_char(month_start, 'YYYY_MM');
        IF to_regclass('location.' || partition_name) IS NULL THEN
            EXECUTE format(
                'CREATE TABLE location.%I PARTITION OF location.location_sample
                   FOR VALUES FROM (%L) TO (%L)',
                partition_name, month_start, next_month);
            created_count := created_count + 1;
        END IF;
    END LOOP;
    RETURN created_count;
END $$;
