CREATE TABLE identity.app_user (
  app_user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
  keycloak_subject TEXT NOT NULL UNIQUE,
  email TEXT,
  phone TEXT,
  display_name TEXT,
  local_status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (local_status IN ('ACTIVE','SUSPENDED','DELETED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX app_user_email_idx ON identity.app_user (lower(email));

CREATE TABLE common.idempotency_key (
  idempotency_key TEXT PRIMARY KEY,
  keycloak_subject TEXT NOT NULL,
  operation TEXT NOT NULL,
  request_hash TEXT NOT NULL,
  response_body JSONB,
  status_code INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE passenger.passenger_profile (
  passenger_profile_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  app_user_id BIGINT NOT NULL UNIQUE REFERENCES identity.app_user(app_user_id),
  full_name TEXT NOT NULL,
  photo_url TEXT,
  preferences JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX passenger_profile_app_user_idx ON passenger.passenger_profile(app_user_id);

CREATE TABLE passenger.saved_place (
  saved_place_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
  label TEXT NOT NULL,
  address TEXT,
  location geometry(Point, 4326) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (ST_X(location) BETWEEN -180 AND 180),
  CHECK (ST_Y(location) BETWEEN -90 AND 90)
);
CREATE INDEX saved_place_user_idx ON passenger.saved_place(app_user_id);
CREATE INDEX saved_place_location_gix ON passenger.saved_place USING gist(location);

CREATE TABLE passenger.trusted_contact (
  trusted_contact_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
  name TEXT NOT NULL,
  phone TEXT NOT NULL,
  relationship TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX trusted_contact_user_idx ON passenger.trusted_contact(app_user_id);

CREATE TABLE driver.driver_profile (
  driver_profile_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  app_user_id BIGINT NOT NULL UNIQUE REFERENCES identity.app_user(app_user_id),
  display_name TEXT NOT NULL,
  verification_status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (verification_status IN ('DRAFT','SUBMITTED','PENDING_REVIEW','APPROVED','REJECTED','SUSPENDED')),
  rating_average NUMERIC(3,2) NOT NULL DEFAULT 0,
  rating_count BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX driver_profile_user_idx ON driver.driver_profile(app_user_id);

CREATE TABLE driver.driver_document (
  driver_document_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  driver_profile_id BIGINT NOT NULL REFERENCES driver.driver_profile(driver_profile_id),
  document_type TEXT NOT NULL,
  storage_key TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
  reviewed_by BIGINT REFERENCES identity.app_user(app_user_id),
  reviewed_at TIMESTAMPTZ,
  rejection_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX driver_document_profile_idx ON driver.driver_document(driver_profile_id);

CREATE TABLE vehicle.vehicle (
  vehicle_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  driver_profile_id BIGINT NOT NULL REFERENCES driver.driver_profile(driver_profile_id),
  make TEXT NOT NULL,
  model TEXT NOT NULL,
  manufacture_year INTEGER NOT NULL CHECK (manufacture_year BETWEEN 1980 AND 2100),
  color TEXT NOT NULL,
  registration_number TEXT NOT NULL UNIQUE,
  seat_count INTEGER NOT NULL CHECK (seat_count BETWEEN 1 AND 12),
  status TEXT NOT NULL DEFAULT 'PENDING_REVIEW' CHECK (status IN ('PENDING_REVIEW','APPROVED','REJECTED','SUSPENDED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX vehicle_driver_idx ON vehicle.vehicle(driver_profile_id);

CREATE TABLE routing.route_plan (
  route_plan_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  driver_profile_id BIGINT NOT NULL REFERENCES driver.driver_profile(driver_profile_id),
  vehicle_id BIGINT NOT NULL REFERENCES vehicle.vehicle(vehicle_id),
  origin_label TEXT NOT NULL,
  destination_label TEXT NOT NULL,
  route_line geometry(LineString, 4326) NOT NULL,
  encoded_polyline TEXT,
  route_length_m NUMERIC(12,2) NOT NULL CHECK (route_length_m > 0),
  departure_time TIMESTAMPTZ NOT NULL,
  available_seats INTEGER NOT NULL CHECK (available_seats >= 0),
  status TEXT NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED','COMPLETED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (ST_NPoints(route_line) >= 2),
  CHECK (ST_IsValid(route_line))
);
CREATE INDEX route_plan_driver_idx ON routing.route_plan(driver_profile_id);
CREATE INDEX route_plan_status_time_idx ON routing.route_plan(status, departure_time);
CREATE INDEX route_plan_line_gix ON routing.route_plan USING gist(route_line);

CREATE TABLE booking.booking (
  booking_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  route_plan_id BIGINT NOT NULL REFERENCES routing.route_plan(route_plan_id),
  passenger_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
  seats INTEGER NOT NULL CHECK (seats > 0),
  pickup geometry(Point,4326) NOT NULL,
  dropoff geometry(Point,4326) NOT NULL,
  status TEXT NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('REQUESTED','CONFIRMED','CANCELLED','REJECTED','COMPLETED')),
  fare_estimate NUMERIC(12,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX booking_route_idx ON booking.booking(route_plan_id);
CREATE INDEX booking_passenger_idx ON booking.booking(passenger_app_user_id);

CREATE TABLE trip.trip (
  trip_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  route_plan_id BIGINT NOT NULL REFERENCES routing.route_plan(route_plan_id),
  status TEXT NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','STARTED','ARRIVED_PICKUP','PASSENGER_ONBOARD','COMPLETED','CANCELLED')),
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX trip_route_idx ON trip.trip(route_plan_id);

CREATE TABLE location.location_sample (
  location_sample_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  trip_id BIGINT REFERENCES trip.trip(trip_id),
  driver_profile_id BIGINT REFERENCES driver.driver_profile(driver_profile_id),
  point geometry(Point,4326) NOT NULL,
  accuracy_m NUMERIC(8,2),
  speed_mps NUMERIC(8,2),
  bearing_degrees NUMERIC(6,2),
  device_recorded_at TIMESTAMPTZ NOT NULL,
  server_received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  source TEXT NOT NULL DEFAULT 'DRIVER_APP'
);
CREATE INDEX location_sample_trip_idx ON location.location_sample(trip_id, server_received_at DESC);
CREATE INDEX location_sample_point_gix ON location.location_sample USING gist(point);

CREATE TABLE pricing.fare_quote (
  fare_quote_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  passenger_app_user_id BIGINT REFERENCES identity.app_user(app_user_id),
  distance_m NUMERIC(12,2) NOT NULL CHECK (distance_m >= 0),
  base_fare NUMERIC(12,2) NOT NULL,
  distance_fare NUMERIC(12,2) NOT NULL,
  platform_fee NUMERIC(12,2) NOT NULL,
  total_fare NUMERIC(12,2) NOT NULL,
  currency TEXT NOT NULL DEFAULT 'LKR',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payment.payment_intent (
  payment_intent_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  booking_id BIGINT REFERENCES booking.booking(booking_id),
  provider TEXT NOT NULL DEFAULT 'MOCK',
  provider_reference TEXT NOT NULL UNIQUE,
  amount NUMERIC(12,2) NOT NULL,
  currency TEXT NOT NULL DEFAULT 'LKR',
  status TEXT NOT NULL DEFAULT 'REQUIRES_CAPTURE' CHECK (status IN ('REQUIRES_CAPTURE','CAPTURED','VOIDED','REFUNDED','FAILED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
