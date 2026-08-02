-- Slice 08 — who may ride with whom, decided by the server.
--
-- Two of the driving preferences narrow who may book, and both are safety features rather than
-- filters: women-only trips and verified-riders-only trips. The prototype is emphatic that a rider
-- who cannot book a trip must never be shown it — "Riders see this on your trip before they book,
-- so nobody wastes a request" — so eligibility is a column on the occurrence that the search query
-- filters on, not a flag the client is trusted to honour.
--
-- Verification is the other half, and it is deliberately *not* a booking gate. It is a badge, a
-- ranking signal and the key that unlocks verified-only trips. P31a's promise — "Book, pay and ride
-- as normal" — is a product commitment, and the only thing verification may ever refuse is a trip
-- whose driver asked for it.

-- ── driving preferences ──────────────────────────────────────────────────────────────────────────
-- One row per driver, created on first read. The account-level default that slice 07's
-- `route_occurrence.approval_mode` was stubbed to 'APPROVE_EACH' waiting for.
CREATE TABLE driver.driving_preference (
    driver_profile_id    BIGINT PRIMARY KEY REFERENCES driver.driver_profile(driver_profile_id),
    gender_policy        TEXT NOT NULL DEFAULT 'ANYONE'
                             CHECK (gender_policy IN ('ANYONE', 'WOMEN_ONLY')),
    verified_riders_only BOOLEAN NOT NULL DEFAULT false,
    approve_each_request BOOLEAN NOT NULL DEFAULT true,
    mid_trip_bookings    BOOLEAN NOT NULL DEFAULT true,
    early_drop_requests  BOOLEAN NOT NULL DEFAULT true,
    chat_enabled         BOOLEAN NOT NULL DEFAULT true,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── the eligibility inputs, on the trip ──────────────────────────────────────────────────────────
-- Copied from preferences when the occurrence is generated and overridable until the trip freezes
-- (D09). Stored on the occurrence rather than read through to the driver's preferences at query
-- time, because changing a preference must not silently change the terms of a trip somebody has
-- already booked.
ALTER TABLE routing.route_occurrence
    ADD COLUMN gender_policy TEXT NOT NULL DEFAULT 'ANYONE'
        CHECK (gender_policy IN ('ANYONE', 'WOMEN_ONLY'));
ALTER TABLE routing.route_occurrence
    ADD COLUMN verified_riders_only BOOLEAN NOT NULL DEFAULT false;

-- The search query filters on these two columns on every published trip, so they carry their own
-- index rather than relying on the corridor predicates to narrow the set first.
CREATE INDEX idx_occurrence_eligibility
    ON routing.route_occurrence (gender_policy, verified_riders_only)
    WHERE status = 'PUBLISHED';

-- Every refusal, kept. D35 shows the driver what "verified riders only" actually cost him — "that
-- cost you 3 requests last week" — and there is no other trace of it: a rider filtered out of search
-- never made a request, so nothing else in the system knows she wanted the seat.
CREATE TABLE routing.eligibility_denial (
    eligibility_denial_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    route_occurrence_id   BIGINT NOT NULL REFERENCES routing.route_occurrence(route_occurrence_id),
    app_user_id           BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    reason                TEXT NOT NULL
                              CHECK (reason IN ('NOT_ELIGIBLE_WOMEN_ONLY',
                                                'NOT_ELIGIBLE_VERIFIED_ONLY')),
    surface               TEXT NOT NULL CHECK (surface IN ('SEARCH', 'BOOKING')),
    denied_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX eligibility_denial_occurrence_idx
    ON routing.eligibility_denial (route_occurrence_id, denied_at DESC);

-- ── the rider side ───────────────────────────────────────────────────────────────────────────────
-- Gender is written by the verification decision and is not user-editable. It is an eligibility
-- input and nothing else: it appears in no public profile, no search result and no booking payload.
ALTER TABLE passenger.passenger_profile
    ADD COLUMN verification_level TEXT NOT NULL DEFAULT 'NONE'
        CHECK (verification_level IN ('NONE', 'PENDING', 'VERIFIED', 'REJECTED'));
ALTER TABLE passenger.passenger_profile ADD COLUMN verified_at TIMESTAMPTZ;
ALTER TABLE passenger.passenger_profile
    ADD COLUMN gender TEXT NOT NULL DEFAULT 'UNSPECIFIED'
        CHECK (gender IN ('FEMALE', 'MALE', 'UNSPECIFIED'));
-- MATCHED is the default rather than PUBLIC: a photo shown to everyone who searches is a decision a
-- rider should make deliberately, not one she discovers she made.
ALTER TABLE passenger.passenger_profile
    ADD COLUMN photo_visibility TEXT NOT NULL DEFAULT 'MATCHED'
        CHECK (photo_visibility IN ('PUBLIC', 'MATCHED', 'HIDDEN'));

-- Written by driver KYC review. Needed for the women-only *set* gate: D35 offers the toggle only to
-- a driver whose own NIC verifies her as female.
ALTER TABLE driver.driver_profile ADD COLUMN gender TEXT
    CHECK (gender IS NULL OR gender IN ('FEMALE', 'MALE', 'UNSPECIFIED'));

-- ── verification sessions ────────────────────────────────────────────────────────────────────────
-- A session exists so the four captures are one submission a reviewer decides on together, and so
-- each upload can be bound to a short-lived, server-issued id. Without it, "camera-only" is a claim
-- attached to nothing.
CREATE TABLE passenger.verification_session (
    verification_session_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id             BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    status                  TEXT NOT NULL DEFAULT 'OPEN'
                                CHECK (status IN ('OPEN', 'SUBMITTED', 'APPROVED', 'REJECTED',
                                                  'EXPIRED')),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at              TIMESTAMPTZ NOT NULL,
    submitted_at            TIMESTAMPTZ,
    decided_at              TIMESTAMPTZ,
    decided_by_app_user_id  BIGINT REFERENCES identity.app_user(app_user_id),
    decision_note           TEXT
);

CREATE INDEX verification_session_user_idx
    ON passenger.verification_session (app_user_id, verification_session_id DESC);

-- One live attempt per rider. A second OPEN session would let a rider start again mid-capture and
-- leave a reviewer two half-finished sets with no way to tell which one she meant.
CREATE UNIQUE INDEX verification_session_one_live_uk
    ON passenger.verification_session (app_user_id)
    WHERE status IN ('OPEN', 'SUBMITTED');

CREATE TABLE passenger.verification_step (
    verification_step_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id           BIGINT NOT NULL
                             REFERENCES passenger.verification_session(verification_session_id),
    step_key             TEXT NOT NULL
                             CHECK (step_key IN ('NIC_FRONT', 'NIC_BACK', 'SELFIE_WITH_NIC',
                                                 'PROFILE_PHOTO')),
    document_id          BIGINT REFERENCES passenger.passenger_document(passenger_document_id),
    -- POLICY.verifyCameraOnly, expressed where it cannot be forgotten. The whole value of a
    -- selfie-with-NIC is that it could not have been assembled beforehand; a gallery upload is a
    -- different piece of evidence wearing the same name. This is deterrence rather than proof — a
    -- determined client can lie — which is why the review step stays human.
    capture_source       TEXT CHECK (capture_source IS NULL OR capture_source IN ('CAMERA')),
    captured_at          TIMESTAMPTZ,
    status               TEXT NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    rejection_reason     TEXT,
    UNIQUE (session_id, step_key)
);

CREATE INDEX verification_step_session_idx ON passenger.verification_step (session_id);

-- ── policy ───────────────────────────────────────────────────────────────────────────────────────
-- A switch rather than a constant, because a support-assisted path may be needed later for a rider
-- whose camera will not work. Turning it off does not remove the CHECK; it removes the requirement
-- that the client declare a source at all.
INSERT INTO platform.policy_setting (policy_key, value, value_type, description) VALUES
    ('VERIFY_CAMERA_ONLY', 'true', 'BOOLEAN',
     'Identity captures must come from the in-app camera, never the gallery.'),
    ('VERIFICATION_SESSION_TTL_MINUTES', '30', 'INT',
     'How long a verification capture session stays open before it must be restarted.'),
    ('VERIFIED_RIDES_SHARE_PCT', '34', 'INT',
     'How many more requests a verified rider gets accepted. Stated once, in one place (P28).')
ON CONFLICT (policy_key) DO NOTHING;
