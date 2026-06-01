CREATE TABLE booking.booking_status_history (
  booking_status_history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  booking_id BIGINT NOT NULL REFERENCES booking.booking(booking_id),
  from_status TEXT,
  to_status TEXT NOT NULL CHECK (to_status IN ('REQUESTED','CONFIRMED','CANCELLED','REJECTED','COMPLETED')),
  changed_by_app_user_id BIGINT REFERENCES identity.app_user(app_user_id),
  reason TEXT,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (from_status IS NULL OR from_status IN ('REQUESTED','CONFIRMED','CANCELLED','REJECTED','COMPLETED'))
);

CREATE INDEX booking_status_history_booking_idx
  ON booking.booking_status_history(booking_id, changed_at);
CREATE INDEX booking_status_history_changed_by_idx
  ON booking.booking_status_history(changed_by_app_user_id);
