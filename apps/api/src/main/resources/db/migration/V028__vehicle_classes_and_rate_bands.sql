-- Slice 02 — vehicle classes and admin-assessed per-km rate bands.
--
-- The product's central pricing rule is that a driver never types a price. ComiGo assesses a min–max
-- per-km band for each vehicle and the driver picks a rate inside it, which is why two cars on the
-- same road are not the same price and why search can explain "why Priya's rate is LKR 46".
--
-- Nothing here prices anything — slice 03 owns the fare engine. This migration stores and governs
-- the number.

CREATE TABLE vehicle.vehicle_class (
    class_key           TEXT PRIMARY KEY,
    label               TEXT NOT NULL,
    max_passenger_seats INTEGER NOT NULL CHECK (max_passenger_seats BETWEEN 1 AND 12),
    default_rate_min    NUMERIC(6, 2) NOT NULL CHECK (default_rate_min > 0),
    default_rate_max    NUMERIC(6, 2) NOT NULL,
    sort_order          INTEGER NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (default_rate_max >= default_rate_min)
);

INSERT INTO vehicle.vehicle_class
    (class_key, label, max_passenger_seats, default_rate_min, default_rate_max, sort_order)
VALUES
    ('CAR',           'Car',          3, 38, 62, 1),
    ('SUV',           'SUV / Wagon',  4, 46, 74, 2),
    ('VAN',           'Van',          6, 40, 68, 3),
    ('THREE_WHEELER', 'Three-wheeler', 2, 26, 42, 4);

-- Existing rows are dev data only (decision D6), so a blanket backfill to CAR is honest and the
-- column can go NOT NULL immediately.
ALTER TABLE vehicle.vehicle
    ADD COLUMN class_key TEXT REFERENCES vehicle.vehicle_class(class_key);
UPDATE vehicle.vehicle SET class_key = 'CAR' WHERE class_key IS NULL;
ALTER TABLE vehicle.vehicle ALTER COLUMN class_key SET NOT NULL;

-- A seat count above the class cap is a capacity lie sold to riders, so it is refused by the
-- database and not only by the service. A CHECK cannot read another table; a trigger can.
CREATE OR REPLACE FUNCTION vehicle.vehicle_seats_within_class() RETURNS TRIGGER AS $$
DECLARE
    cap INTEGER;
BEGIN
    SELECT max_passenger_seats INTO cap
      FROM vehicle.vehicle_class WHERE class_key = NEW.class_key;
    IF cap IS NULL THEN
        RAISE EXCEPTION 'SEATS_EXCEED_CLASS_CAP: unknown vehicle class %', NEW.class_key;
    END IF;
    IF NEW.seat_count > cap THEN
        RAISE EXCEPTION 'SEATS_EXCEED_CLASS_CAP: % seats exceeds the % cap of %',
            NEW.seat_count, NEW.class_key, cap;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER vehicle_seats_within_class
    BEFORE INSERT OR UPDATE OF seat_count, class_key ON vehicle.vehicle
    FOR EACH ROW EXECUTE FUNCTION vehicle.vehicle_seats_within_class();

CREATE TABLE vehicle.vehicle_rate_band (
    vehicle_rate_band_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vehicle_id           BIGINT NOT NULL UNIQUE REFERENCES vehicle.vehicle(vehicle_id),
    rate_min             NUMERIC(6, 2) NOT NULL,
    rate_max             NUMERIC(6, 2) NOT NULL,
    chosen_rate          NUMERIC(6, 2),
    status               TEXT NOT NULL DEFAULT 'PENDING_ASSESSMENT'
                             CHECK (status IN ('NOT_SET', 'PENDING_ASSESSMENT', 'ACTIVE',
                                               'UNDER_REVIEW')),
    set_by_app_user_id   BIGINT REFERENCES identity.app_user(app_user_id),
    set_at               TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (rate_min > 0 AND rate_max >= rate_min),
    CHECK (chosen_rate IS NULL OR (chosen_rate >= rate_min AND chosen_rate <= rate_max))
);

-- A band outside its class band is a pricing incident, not a validation slip, so it is refused at
-- the lowest level that can see both rows.
CREATE OR REPLACE FUNCTION vehicle.vehicle_rate_band_within_class() RETURNS TRIGGER AS $$
DECLARE
    class_min NUMERIC(6, 2);
    class_max NUMERIC(6, 2);
BEGIN
    SELECT c.default_rate_min, c.default_rate_max INTO class_min, class_max
      FROM vehicle.vehicle v
      JOIN vehicle.vehicle_class c ON c.class_key = v.class_key
     WHERE v.vehicle_id = NEW.vehicle_id;
    IF class_min IS NULL THEN
        RAISE EXCEPTION 'BAND_OUTSIDE_CLASS: vehicle % has no class', NEW.vehicle_id;
    END IF;
    IF NEW.rate_min < class_min OR NEW.rate_max > class_max THEN
        RAISE EXCEPTION 'BAND_OUTSIDE_CLASS: band %-% is outside the class range %-%',
            NEW.rate_min, NEW.rate_max, class_min, class_max;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER vehicle_rate_band_within_class
    BEFORE INSERT OR UPDATE OF rate_min, rate_max, vehicle_id ON vehicle.vehicle_rate_band
    FOR EACH ROW EXECUTE FUNCTION vehicle.vehicle_rate_band_within_class();

-- The four factors are displayed justification (decision D2): typed by the admin, never computed.
-- They explain a band to the driver; they do not derive it.
CREATE TABLE vehicle.vehicle_rate_band_factor (
    vehicle_rate_band_factor_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vehicle_rate_band_id BIGINT NOT NULL
                             REFERENCES vehicle.vehicle_rate_band(vehicle_rate_band_id)
                             ON DELETE CASCADE,
    factor_key           TEXT NOT NULL
                             CHECK (factor_key IN ('AGE', 'INSURANCE', 'FUEL', 'SERVICE')),
    label                TEXT NOT NULL,
    detail               TEXT,
    delta                NUMERIC(6, 2) NOT NULL,
    sort_order           INTEGER NOT NULL DEFAULT 0,
    UNIQUE (vehicle_rate_band_id, factor_key)
);

CREATE TABLE vehicle.rate_band_review_request (
    rate_band_review_request_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vehicle_id                  BIGINT NOT NULL REFERENCES vehicle.vehicle(vehicle_id),
    requested_by_app_user_id    BIGINT REFERENCES identity.app_user(app_user_id),
    reason                      TEXT NOT NULL,
    note                        TEXT,
    status                      TEXT NOT NULL DEFAULT 'OPEN'
                                    CHECK (status IN ('OPEN', 'APPROVED', 'REJECTED')),
    requested_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at                  TIMESTAMPTZ,
    decided_by_app_user_id      BIGINT REFERENCES identity.app_user(app_user_id),
    decision_note               TEXT
);

CREATE INDEX rate_band_review_request_vehicle_idx
    ON vehicle.rate_band_review_request(vehicle_id, rate_band_review_request_id DESC);
CREATE INDEX rate_band_review_request_open_idx
    ON vehicle.rate_band_review_request(status, rate_band_review_request_id DESC);

-- "One re-assessment" from board D39, enforced rather than described.
CREATE UNIQUE INDEX idx_rate_band_review_request_open
    ON vehicle.rate_band_review_request(vehicle_id)
    WHERE status = 'OPEN';

-- Every approved vehicle already in the system needs somewhere for D40 to point at.
INSERT INTO vehicle.vehicle_rate_band (vehicle_id, rate_min, rate_max, status)
SELECT v.vehicle_id, c.default_rate_min, c.default_rate_max, 'PENDING_ASSESSMENT'
  FROM vehicle.vehicle v
  JOIN vehicle.vehicle_class c ON c.class_key = v.class_key
 WHERE v.status = 'APPROVED';
