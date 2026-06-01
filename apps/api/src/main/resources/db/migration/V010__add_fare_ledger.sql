CREATE TABLE payment.fare_ledger_entry (
  fare_ledger_entry_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  booking_id BIGINT NOT NULL REFERENCES booking.booking(booking_id),
  entry_type TEXT NOT NULL CHECK (entry_type IN ('BOOKING_FARE_ESTIMATE')),
  amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
  currency TEXT NOT NULL DEFAULT 'LKR',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (booking_id, entry_type)
);

CREATE INDEX fare_ledger_entry_booking_idx ON payment.fare_ledger_entry(booking_id, created_at);
