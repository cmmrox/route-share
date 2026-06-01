CREATE TABLE IF NOT EXISTS trip.pre_trip_checklist (
  pre_trip_checklist_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  trip_id BIGINT NOT NULL REFERENCES trip.trip(trip_id),
  driver_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
  vehicle_checked BOOLEAN NOT NULL,
  documents_ready BOOLEAN NOT NULL,
  route_reviewed BOOLEAN NOT NULL,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (trip_id)
);

CREATE TABLE IF NOT EXISTS trip.trip_operational_event (
  trip_operational_event_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  trip_id BIGINT NOT NULL REFERENCES trip.trip(trip_id),
  event_type TEXT NOT NULL CHECK (event_type IN ('ARRIVED_PICKUP')),
  actor_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (trip_id, event_type)
);

CREATE TABLE IF NOT EXISTS routing.route_share_link (
  route_share_link_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  route_plan_id BIGINT NOT NULL REFERENCES routing.route_plan(route_plan_id),
  share_token TEXT NOT NULL UNIQUE,
  share_url TEXT NOT NULL,
  qr_payload TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (route_plan_id)
);

ALTER TABLE payment.fare_ledger_entry
  DROP CONSTRAINT IF EXISTS fare_ledger_entry_entry_type_check;

ALTER TABLE payment.fare_ledger_entry
  ADD CONSTRAINT fare_ledger_entry_entry_type_check
    CHECK (entry_type IN (
      'BOOKING_FARE_ESTIMATE',
      'PAYMENT_CAPTURED',
      'PAYMENT_VOIDED',
      'PAYMENT_REFUNDED',
      'CASH_COLLECTED',
      'DRIVER_EARNING',
      'PLATFORM_COMMISSION',
      'SETTLEMENT_ADJUSTMENT',
      'FARE_ADJUSTMENT_REQUESTED'
    ));
