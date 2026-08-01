-- Slice 04 — charge timing and capture correctness.
--
-- The product's central promise, stated on eleven screens: the card is AUTHORISED at booking and
-- CAPTURED when the driver starts the trip. "Accepting does not charge you." "Decline, cancel or a
-- no-start all cost you nothing."
--
-- Until now the backend did none of it: there was no capture call anywhere in the trip lifecycle,
-- so a trip could run to completion without money moving, and a cancelled booking could leave an
-- authorisation hanging on someone's card for a week.

-- ── the state machine gains the state it was missing ─────────────────────────────────────────────
-- REQUIRES_CAPTURE conflated "not yet authorised" with "authorised, awaiting capture". Those are
-- different facts about someone's money and the app has to tell them apart:
--
--   PENDING → AUTHORIZED → CAPTURED → REFUNDED
--      ↓          ↓
--    FAILED     VOIDED
ALTER TABLE payment.payment_intent DROP CONSTRAINT IF EXISTS payment_intent_status_check;
ALTER TABLE payment.payment_intent
    ADD CONSTRAINT payment_intent_status_check
    CHECK (status IN ('PENDING', 'AUTHORIZED', 'REQUIRES_CAPTURE', 'CAPTURED', 'VOIDED',
                      'REFUNDED', 'FAILED'));

ALTER TABLE payment.payment_intent ADD COLUMN authorized_at TIMESTAMPTZ;
ALTER TABLE payment.payment_intent ADD COLUMN captured_at TIMESTAMPTZ;
ALTER TABLE payment.payment_intent ADD COLUMN voided_at TIMESTAMPTZ;
ALTER TABLE payment.payment_intent ADD COLUMN failure_code TEXT;
ALTER TABLE payment.payment_intent ADD COLUMN failure_message TEXT;
ALTER TABLE payment.payment_intent
    ADD COLUMN payment_method_id BIGINT REFERENCES payment.payment_method(payment_method_id);
ALTER TABLE payment.payment_intent ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;

-- Existing rows were authorised under the old vocabulary; say so rather than leaving them ambiguous.
UPDATE payment.payment_intent
   SET status = 'AUTHORIZED', authorized_at = created_at
 WHERE status = 'REQUIRES_CAPTURE';

-- An authorisation that is never captured and never voided is money held on a stranger's card.
-- This index is what lets the reconciliation job find them cheaply.
CREATE INDEX idx_payment_intent_stuck
    ON payment.payment_intent(status, created_at)
    WHERE status IN ('PENDING', 'AUTHORIZED');

-- ── every gateway call leaves a record before it is made ─────────────────────────────────────────
-- A capture that times out has either happened or not, and the difference is a double charge. The
-- attempt row is written first, so a timeout leaves something to reconcile against instead of a
-- blind retry.
CREATE TABLE payment.payment_attempt (
    payment_attempt_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_intent_id  BIGINT NOT NULL
                           REFERENCES payment.payment_intent(payment_intent_id),
    booking_id         BIGINT REFERENCES booking.booking(booking_id),
    operation          TEXT NOT NULL
                           CHECK (operation IN ('AUTHORIZE', 'CAPTURE', 'VOID', 'REFUND')),
    idempotency_key    TEXT NOT NULL UNIQUE,
    provider_reference TEXT,
    amount             NUMERIC(12, 2),
    currency           TEXT NOT NULL DEFAULT 'LKR',
    status             TEXT NOT NULL DEFAULT 'STARTED'
                           CHECK (status IN ('STARTED', 'SUCCEEDED', 'FAILED')),
    failure_code       TEXT,
    started_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at        TIMESTAMPTZ
);

CREATE INDEX payment_attempt_intent_idx
    ON payment.payment_attempt(payment_intent_id, payment_attempt_id DESC);
CREATE INDEX payment_attempt_unfinished_idx
    ON payment.payment_attempt(status, started_at)
    WHERE status = 'STARTED';

-- ── what the booking screens read ────────────────────────────────────────────────────────────────
-- P11 says "authorised, not charged"; P12 states the exact capture time; P22 and P24 say "never
-- charged". All three read these columns.
ALTER TABLE booking.booking
    ADD COLUMN payment_method TEXT CHECK (payment_method IN ('CARD', 'CASH'));
ALTER TABLE booking.booking ADD COLUMN payment_status TEXT;
ALTER TABLE booking.booking ADD COLUMN captured_at TIMESTAMPTZ;

-- Cash never authorises: there is no card to hold. The platform's cut on a cash fare is owed by the
-- driver and netted from the next payout (boards D23 and D27).
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
        'FARE_FINALIZED'
    ));
