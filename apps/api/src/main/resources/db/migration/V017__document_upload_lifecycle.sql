-- Real document upload lifecycle: AWAITING_UPLOAD -> SUBMITTED -> APPROVED/REJECTED.
-- Adds upload metadata and replaces the legacy PENDING-only status set across document tables.

-- ---- driver.driver_document ----
ALTER TABLE driver.driver_document
    ADD COLUMN content_type      TEXT,
    ADD COLUMN file_size_bytes   BIGINT,
    ADD COLUMN original_filename TEXT,
    ADD COLUMN submitted_at      TIMESTAMPTZ;

ALTER TABLE driver.driver_document DROP CONSTRAINT IF EXISTS driver_document_status_check;
-- Legacy rows already carried a storage key, so treat them as submitted for review.
UPDATE driver.driver_document SET status = 'SUBMITTED' WHERE status = 'PENDING';
ALTER TABLE driver.driver_document ALTER COLUMN status SET DEFAULT 'AWAITING_UPLOAD';
ALTER TABLE driver.driver_document
    ADD CONSTRAINT driver_document_status_check
    CHECK (status IN ('AWAITING_UPLOAD','SUBMITTED','APPROVED','REJECTED'));

-- ---- vehicle.vehicle_document ----
ALTER TABLE vehicle.vehicle_document
    ADD COLUMN content_type      TEXT,
    ADD COLUMN file_size_bytes   BIGINT,
    ADD COLUMN original_filename TEXT,
    ADD COLUMN submitted_at      TIMESTAMPTZ;

ALTER TABLE vehicle.vehicle_document DROP CONSTRAINT IF EXISTS vehicle_document_status_check;
UPDATE vehicle.vehicle_document SET status = 'SUBMITTED' WHERE status = 'PENDING';
ALTER TABLE vehicle.vehicle_document ALTER COLUMN status SET DEFAULT 'AWAITING_UPLOAD';
ALTER TABLE vehicle.vehicle_document
    ADD CONSTRAINT vehicle_document_status_check
    CHECK (status IN ('AWAITING_UPLOAD','SUBMITTED','APPROVED','REJECTED'));

-- ---- passenger.passenger_document (new) ----
-- Passenger avatar + optional identity verification documents.
CREATE TABLE passenger.passenger_document (
    passenger_document_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id           BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    document_type         TEXT   NOT NULL,
    storage_key           TEXT   NOT NULL,
    content_type          TEXT,
    file_size_bytes       BIGINT,
    original_filename     TEXT,
    status                TEXT   NOT NULL DEFAULT 'AWAITING_UPLOAD'
                            CHECK (status IN ('AWAITING_UPLOAD','SUBMITTED','APPROVED','REJECTED')),
    reviewed_by           BIGINT REFERENCES identity.app_user(app_user_id),
    reviewed_at           TIMESTAMPTZ,
    rejection_reason      TEXT,
    submitted_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX passenger_document_user_idx ON passenger.passenger_document(app_user_id);
