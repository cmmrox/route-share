ALTER TABLE trip.trip
  ADD COLUMN route_occurrence_id BIGINT REFERENCES routing.route_occurrence(route_occurrence_id);

CREATE INDEX trip_route_occurrence_idx ON trip.trip(route_occurrence_id);

CREATE TABLE trip.passenger_trip_state (
  passenger_trip_state_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  trip_id BIGINT NOT NULL REFERENCES trip.trip(trip_id),
  booking_id BIGINT NOT NULL REFERENCES booking.booking(booking_id),
  route_occurrence_id BIGINT NOT NULL REFERENCES routing.route_occurrence(route_occurrence_id),
  passenger_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
  status TEXT NOT NULL DEFAULT 'WAITING_PICKUP' CHECK (status IN ('WAITING_PICKUP','BOARDED','NO_SHOW','DROPPED_OFF')),
  boarded_at TIMESTAMPTZ,
  no_show_at TIMESTAMPTZ,
  dropped_off_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (trip_id, booking_id),
  CHECK (
    (status = 'BOARDED' AND boarded_at IS NOT NULL AND no_show_at IS NULL AND dropped_off_at IS NULL)
    OR (status = 'NO_SHOW' AND no_show_at IS NOT NULL AND dropped_off_at IS NULL)
    OR (status = 'DROPPED_OFF' AND boarded_at IS NOT NULL AND dropped_off_at IS NOT NULL AND no_show_at IS NULL)
    OR (status = 'WAITING_PICKUP' AND boarded_at IS NULL AND no_show_at IS NULL AND dropped_off_at IS NULL)
  )
);

CREATE INDEX passenger_trip_state_trip_idx ON trip.passenger_trip_state(trip_id, status);
CREATE INDEX passenger_trip_state_booking_idx ON trip.passenger_trip_state(booking_id);
CREATE INDEX passenger_trip_state_occurrence_idx ON trip.passenger_trip_state(route_occurrence_id);
