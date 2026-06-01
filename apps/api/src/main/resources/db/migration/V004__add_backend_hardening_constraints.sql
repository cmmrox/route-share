CREATE UNIQUE INDEX IF NOT EXISTS payment_intent_one_active_per_booking_idx
  ON payment.payment_intent (booking_id)
  WHERE status IN ('REQUIRES_CAPTURE', 'CAPTURED');

ALTER TABLE booking.booking
  ADD CONSTRAINT booking_fare_estimate_positive CHECK (fare_estimate > 0);
