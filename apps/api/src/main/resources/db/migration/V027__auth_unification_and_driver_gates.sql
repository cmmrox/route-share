-- Slice 01 — auth unification and mode gates.
--
-- One account now acts as both passenger and driver on a single token, so the two states that stop a
-- driver have to be told apart in data: an account-level SUSPENSION stops everything, while a driver
-- DEACTIVATION stops driving only and deliberately leaves riding and pending payouts untouched
-- (prototype board D34). Conflating them would either strand earnings or over-punish.
--
-- No backfill: the app is unreleased (decision D6).

-- The mode the app reopens in. Nullable: an account that has never chosen keeps the shell's default.
ALTER TABLE identity.app_user
    ADD COLUMN last_active_mode TEXT
        CHECK (last_active_mode IN ('PASSENGER', 'DRIVER'));

-- S13 shows a case reference next to the suspension reason ("Case #SL-40912"); the reason column
-- already exists.
ALTER TABLE identity.app_user_status_history
    ADD COLUMN case_ref TEXT;

-- Document expiry is a publish blocker in its own right (S12 lists an expired licence separately
-- from a rejected one), and until now there was nowhere to record it.
ALTER TABLE driver.driver_document
    ADD COLUMN expires_at TIMESTAMPTZ;

CREATE TABLE driver.driver_deactivation (
    driver_deactivation_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    driver_profile_id           BIGINT NOT NULL REFERENCES driver.driver_profile(driver_profile_id),
    reason                      TEXT NOT NULL,
    case_ref                    TEXT NOT NULL,
    deactivated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    deactivated_by_app_user_id  BIGINT REFERENCES identity.app_user(app_user_id),
    reinstated_at               TIMESTAMPTZ,
    reinstated_by_app_user_id   BIGINT REFERENCES identity.app_user(app_user_id)
);

-- A driver can only be under one open deactivation at a time; a second one is a bug, not a state.
CREATE UNIQUE INDEX idx_driver_deactivation_active
    ON driver.driver_deactivation(driver_profile_id)
    WHERE reinstated_at IS NULL;

CREATE TABLE driver.driver_reinstatement_request (
    driver_reinstatement_request_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    driver_profile_id       BIGINT NOT NULL REFERENCES driver.driver_profile(driver_profile_id),
    deactivation_id         BIGINT NOT NULL
                                REFERENCES driver.driver_deactivation(driver_deactivation_id),
    support_ticket_id       BIGINT,
    message                 TEXT,
    status                  TEXT NOT NULL DEFAULT 'OPEN'
                                CHECK (status IN ('OPEN', 'APPROVED', 'REJECTED')),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at              TIMESTAMPTZ,
    decided_by_app_user_id  BIGINT REFERENCES identity.app_user(app_user_id),
    decision_note           TEXT
);

CREATE INDEX driver_reinstatement_request_profile_idx
    ON driver.driver_reinstatement_request(driver_profile_id,
                                           driver_reinstatement_request_id DESC);

-- One open request per deactivation: D34's primary action is idempotent from the driver's side.
CREATE UNIQUE INDEX idx_driver_reinstatement_request_open
    ON driver.driver_reinstatement_request(deactivation_id)
    WHERE status = 'OPEN';
