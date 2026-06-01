ALTER TABLE booking.booking
  ADD COLUMN route_occurrence_id BIGINT REFERENCES routing.route_occurrence(route_occurrence_id),
  ADD COLUMN pickup_route_fraction NUMERIC(9,8) NOT NULL DEFAULT 0,
  ADD COLUMN dropoff_route_fraction NUMERIC(9,8) NOT NULL DEFAULT 1,
  ADD CONSTRAINT booking_route_fraction_order_chk CHECK (pickup_route_fraction >= 0 AND dropoff_route_fraction <= 1 AND pickup_route_fraction < dropoff_route_fraction);

CREATE INDEX booking_route_occurrence_idx ON booking.booking(route_occurrence_id);
