-- Real trip share-links: tokenized, time-boxed, revocable public views of a booking's live status.
CREATE TABLE booking.trip_share (
  trip_share_id          BIGSERIAL PRIMARY KEY,
  booking_id             BIGINT NOT NULL REFERENCES booking.booking(booking_id),
  passenger_app_user_id  BIGINT NOT NULL,
  token                  TEXT NOT NULL UNIQUE,
  expires_at             TIMESTAMPTZ NOT NULL,
  revoked                BOOLEAN NOT NULL DEFAULT FALSE,
  created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trip_share_booking ON booking.trip_share(booking_id);
CREATE INDEX idx_trip_share_passenger ON booking.trip_share(passenger_app_user_id);
