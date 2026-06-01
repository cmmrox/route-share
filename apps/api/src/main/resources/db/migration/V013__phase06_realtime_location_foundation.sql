CREATE TABLE IF NOT EXISTS location.location_event_outbox (
  location_event_id BIGSERIAL PRIMARY KEY,
  trip_id BIGINT NOT NULL REFERENCES trip.trip(trip_id),
  driver_profile_id BIGINT NOT NULL REFERENCES driver.driver_profile(driver_profile_id),
  event_type TEXT NOT NULL,
  payload JSONB NOT NULL,
  published_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_location_event_outbox_trip_created
  ON location.location_event_outbox(trip_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_location_event_outbox_unpublished
  ON location.location_event_outbox(created_at)
  WHERE published_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_location_sample_trip_device_time
  ON location.location_sample(trip_id, device_recorded_at DESC);
