-- POLICY.penaltyRecipient = "SPLIT" is the rule that makes half the product's screens coherent: a
-- penalty is never a fee the platform pockets. It produces a negative line for the person who caused
-- it and a positive one for the person it cost (D26 shows both kinds in the same ledger).
--
-- Nothing in the codebase had a penalty concept before this migration. Slice 05 fires the triggers —
-- a released no-show, an unlocked free cancel, an auto-cancelled start — and this slice prices them.

CREATE SCHEMA IF NOT EXISTS penalty;

-- ── assessment ───────────────────────────────────────────────────────────────────────────────────
-- One row per broken promise, carrying the whole computation rather than just its result. A support
-- agent who cannot say "25% of LKR 197, half to your driver" is looking at a refund, so the base,
-- the percentage and both halves are all stored beside the fee.
CREATE TABLE penalty.penalty_assessment (
    penalty_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind               TEXT NOT NULL CHECK (kind IN (
                           'PASSENGER_CANCEL_AFTER_START',
                           'PASSENGER_NO_SHOW',
                           'DRIVER_LATE',
                           'DRIVER_LATE_CANCELLATION',
                           'DRIVER_MISSED_START')),
    booking_id         BIGINT REFERENCES booking.booking(booking_id),
    trip_id            BIGINT REFERENCES trip.trip(trip_id),
    payer_app_user_id  BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    payer_role         TEXT NOT NULL CHECK (payer_role IN ('PASSENGER', 'DRIVER')),
    victim_role        TEXT NOT NULL CHECK (victim_role IN ('PASSENGER', 'DRIVER', 'NONE')),
    fare_base          NUMERIC(12, 2) NOT NULL CHECK (fare_base >= 0),
    percent            NUMERIC(5, 2) NOT NULL CHECK (percent >= 0),
    fee_amount         NUMERIC(12, 2) NOT NULL CHECK (fee_amount >= 0),
    victim_share       NUMERIC(12, 2) NOT NULL CHECK (victim_share >= 0),
    platform_share     NUMERIC(12, 2) NOT NULL CHECK (platform_share >= 0),
    status             TEXT NOT NULL CHECK (status IN ('ASSESSED', 'SETTLED', 'WAIVED', 'REVERSED')),
    collection_method  TEXT CHECK (collection_method IN
                           ('NETTED', 'CARD_CHARGE', 'DUES', 'EARNINGS_DEDUCTION', 'NONE')),
    explanation        TEXT NOT NULL,
    assessed_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    settled_at         TIMESTAMPTZ,
    policy_version     TEXT NOT NULL,
    -- The halves are produced by subtraction, never by two independent roundings, so they re-add to
    -- the fee by construction. This says so in the one place a bad migration or a direct SQL edit
    -- would also have to pass.
    CONSTRAINT penalty_assessment_split_adds_up
        CHECK (victim_share + platform_share = fee_amount),
    -- Every kind but the trip-wide driver cancellation is assessed against one booking.
    CONSTRAINT penalty_assessment_has_subject
        CHECK (booking_id IS NOT NULL OR trip_id IS NOT NULL)
);

-- The idempotency guard, and the reason a repeated trigger costs nobody twice. Two sweeps racing on
-- the same released seat are two transactions, and an application check cannot make that safe.
CREATE UNIQUE INDEX penalty_assessment_booking_kind_uk
    ON penalty.penalty_assessment (kind, booking_id)
    WHERE booking_id IS NOT NULL;

-- The trip-wide kinds have no booking to key on; the trip is their subject instead.
CREATE UNIQUE INDEX penalty_assessment_trip_kind_uk
    ON penalty.penalty_assessment (kind, trip_id)
    WHERE booking_id IS NULL AND trip_id IS NOT NULL;

CREATE INDEX penalty_assessment_payer_idx
    ON penalty.penalty_assessment (payer_app_user_id, assessed_at DESC);

-- ── beneficiaries ────────────────────────────────────────────────────────────────────────────────
-- D31 says a driver's penalty is shared "between them as ride credit", so one penalty can reach
-- several people. Their amounts must total the victim half exactly: a rounding remainder dropped
-- here is money destroyed, and one added is money created.
CREATE TABLE penalty.penalty_beneficiary (
    penalty_beneficiary_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    penalty_id                 BIGINT NOT NULL REFERENCES penalty.penalty_assessment(penalty_id),
    beneficiary_app_user_id    BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    booking_id                 BIGINT REFERENCES booking.booking(booking_id),
    amount                     NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    credited_at                TIMESTAMPTZ,
    credit_reference           TEXT,
    UNIQUE (penalty_id, beneficiary_app_user_id, booking_id)
);

CREATE INDEX penalty_beneficiary_user_idx
    ON penalty.penalty_beneficiary (beneficiary_app_user_id);

-- Deferred, because the rows are written one at a time inside the transaction that also writes the
-- assessment: checking per statement would fail on the first beneficiary of two.
CREATE OR REPLACE FUNCTION penalty.assert_beneficiary_total() RETURNS TRIGGER AS $$
DECLARE
    v_penalty_id BIGINT := COALESCE(NEW.penalty_id, OLD.penalty_id);
    v_expected   NUMERIC(12, 2);
    v_actual     NUMERIC(12, 2);
BEGIN
    SELECT victim_share INTO v_expected
    FROM penalty.penalty_assessment WHERE penalty_id = v_penalty_id;

    IF v_expected IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT COALESCE(SUM(amount), 0) INTO v_actual
    FROM penalty.penalty_beneficiary WHERE penalty_id = v_penalty_id;

    IF v_actual <> v_expected THEN
        RAISE EXCEPTION
            'penalty % beneficiaries total % but the victim share is %',
            v_penalty_id, v_actual, v_expected;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER penalty_beneficiary_total_trg
    AFTER INSERT OR UPDATE OR DELETE ON penalty.penalty_beneficiary
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION penalty.assert_beneficiary_total();

-- ── passenger dues ───────────────────────────────────────────────────────────────────────────────
-- A cash passenger has no card to take a fee from. P25 carries the amount to her next booking rather
-- than blocking her: dues are added to a checkout total, never a gate on making one.
CREATE TABLE penalty.passenger_due (
    passenger_due_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id        BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    penalty_id         BIGINT NOT NULL REFERENCES penalty.penalty_assessment(penalty_id),
    amount             NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    reason             TEXT NOT NULL,
    origin_booking_id  BIGINT REFERENCES booking.booking(booking_id),
    status             TEXT NOT NULL CHECK (status IN ('OUTSTANDING', 'SETTLED', 'WAIVED')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    settled_at         TIMESTAMPTZ,
    settled_booking_id BIGINT REFERENCES booking.booking(booking_id),
    CONSTRAINT passenger_due_settlement_pair
        CHECK ((status = 'OUTSTANDING') = (settled_at IS NULL)),
    -- One due per penalty. A penalty that fell to dues twice would bill the same no-show twice.
    UNIQUE (penalty_id)
);

CREATE INDEX passenger_due_outstanding_idx
    ON penalty.passenger_due (app_user_id)
    WHERE status = 'OUTSTANDING';

-- ── disputes ─────────────────────────────────────────────────────────────────────────────────────
CREATE TABLE penalty.penalty_dispute (
    penalty_dispute_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    penalty_id             BIGINT NOT NULL REFERENCES penalty.penalty_assessment(penalty_id),
    raised_by_app_user_id  BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    reason                 TEXT NOT NULL,
    note                   TEXT,
    status                 TEXT NOT NULL CHECK (status IN ('OPEN', 'UPHELD', 'REVERSED')),
    raised_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at             TIMESTAMPTZ,
    decided_by_app_user_id BIGINT REFERENCES identity.app_user(app_user_id),
    decision_note          TEXT,
    reversed_amount        NUMERIC(12, 2) CHECK (reversed_amount IS NULL OR reversed_amount >= 0),
    CONSTRAINT penalty_dispute_decision_pair
        CHECK ((status = 'OPEN') = (decided_at IS NULL))
);

-- One open dispute per penalty: re-raising is not a second case, it is the same argument.
CREATE UNIQUE INDEX penalty_dispute_open_uk
    ON penalty.penalty_dispute (penalty_id)
    WHERE status = 'OPEN';

CREATE INDEX penalty_dispute_status_idx
    ON penalty.penalty_dispute (status, raised_at DESC);

-- ── ledger ───────────────────────────────────────────────────────────────────────────────────────
-- Compensation is not trip income. Folding it into fares would overstate what a driver earned from
-- driving, and D26 gives it its own icon precisely because it is a different kind of money.
ALTER TABLE payment.fare_ledger_entry DROP CONSTRAINT IF EXISTS fare_ledger_entry_entry_type_check;
ALTER TABLE payment.fare_ledger_entry
    ADD CONSTRAINT fare_ledger_entry_entry_type_check
    CHECK (entry_type IN (
        'BOOKING_FARE_ESTIMATE',
        'PAYMENT_AUTHORIZED',
        'PAYMENT_CAPTURED',
        'PAYMENT_VOIDED',
        'PAYMENT_REFUNDED',
        'PAYMENT_FAILED',
        'CASH_COLLECTED',
        'COMMISSION_OWED_CASH',
        'DRIVER_EARNING',
        'PLATFORM_COMMISSION',
        'SETTLEMENT_ADJUSTMENT',
        'FARE_ADJUSTMENT_REQUESTED',
        'FARE_FINALIZED',
        'PENALTY_CHARGE',
        'PENALTY_DEDUCTION',
        'PENALTY_REVERSAL',
        'COMPENSATION',
        'DUES_SETTLEMENT'
    ));

-- ── booking ──────────────────────────────────────────────────────────────────────────────────────
-- What this checkout carried over from an earlier trip (P09d). Stored on the booking so a receipt
-- read months later still shows the line the passenger actually paid.
ALTER TABLE booking.booking
    ADD COLUMN applied_dues_amount NUMERIC(12, 2) NOT NULL DEFAULT 0
    CHECK (applied_dues_amount >= 0);

-- ── policy ───────────────────────────────────────────────────────────────────────────────────────
INSERT INTO platform.policy_setting (policy_key, value, value_type, description) VALUES
    ('PENALTY_DISPUTE_WINDOW_HOURS', '48', 'INT',
     'How long after a penalty is assessed it may still be disputed.')
ON CONFLICT (policy_key) DO NOTHING;
