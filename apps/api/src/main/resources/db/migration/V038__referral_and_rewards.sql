-- Slice 11: immutable referral attribution and one rewards balance shared by both app modes.
CREATE SCHEMA IF NOT EXISTS rewards;

CREATE TABLE rewards.referral_code (
    app_user_id BIGINT PRIMARY KEY REFERENCES identity.app_user(app_user_id),
    code TEXT NOT NULL UNIQUE CHECK (code ~ '^[A-Z2-9]{6,20}$'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE rewards.referral_device (
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    device_hash TEXT NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (app_user_id, device_hash)
);
CREATE INDEX idx_referral_device_hash ON rewards.referral_device(device_hash);

CREATE TABLE rewards.referral_edge (
    referral_edge_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    referrer_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    referee_app_user_id BIGINT NOT NULL UNIQUE REFERENCES identity.app_user(app_user_id),
    code TEXT NOT NULL,
    attributed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    window_expires_at TIMESTAMPTZ NOT NULL,
    max_trips INTEGER NOT NULL CHECK (max_trips > 0),
    trips_counted INTEGER NOT NULL DEFAULT 0 CHECK (trips_counted BETWEEN 0 AND max_trips),
    status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'EXPIRED_WINDOW', 'EXPIRED_TRIPS', 'REVOKED')),
    CHECK (referrer_app_user_id <> referee_app_user_id)
);
CREATE INDEX idx_referral_edge_referrer
    ON rewards.referral_edge(referrer_app_user_id, attributed_at DESC);
CREATE INDEX idx_referral_edge_active
    ON rewards.referral_edge(referee_app_user_id) WHERE status = 'ACTIVE';

CREATE TABLE rewards.withdrawal (
    withdrawal_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status TEXT NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'BATCHED', 'PAID', 'FAILED', 'CANCELLED')),
    payout_batch_id BIGINT REFERENCES finance.payout_batch(payout_batch_id),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    batched_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    failure_reason TEXT
);
CREATE UNIQUE INDEX idx_rewards_withdrawal_open
    ON rewards.withdrawal(app_user_id) WHERE status IN ('QUEUED', 'BATCHED');
CREATE INDEX idx_rewards_withdrawal_user
    ON rewards.withdrawal(app_user_id, requested_at DESC);

CREATE TABLE rewards.rewards_ledger (
    rewards_ledger_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    kind TEXT NOT NULL
        CHECK (kind IN ('REFERRAL', 'COMPENSATION', 'SPEND', 'WITHDRAWAL', 'ADJUSTMENT')),
    amount NUMERIC(12,2) NOT NULL CHECK (amount <> 0),
    label TEXT NOT NULL,
    sublabel TEXT,
    source_booking_id BIGINT REFERENCES booking.booking(booking_id),
    source_penalty_id BIGINT REFERENCES penalty.penalty_assessment(penalty_id),
    referral_edge_id BIGINT REFERENCES rewards.referral_edge(referral_edge_id),
    withdrawal_id BIGINT REFERENCES rewards.withdrawal(withdrawal_id),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    idempotency_key TEXT NOT NULL UNIQUE
);
CREATE INDEX idx_rewards_ledger_user
    ON rewards.rewards_ledger(app_user_id, occurred_at DESC, rewards_ledger_id DESC);

ALTER TABLE passenger.passenger_profile
    ADD COLUMN rewards_auto_apply BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE booking.booking
    ADD COLUMN applied_credit_amount NUMERIC(12,2) NOT NULL DEFAULT 0
        CHECK (applied_credit_amount >= 0 AND applied_credit_amount <= fare_estimate),
    ADD COLUMN use_rewards_credit BOOLEAN;

ALTER TABLE payment.fare_ledger_entry
    DROP CONSTRAINT IF EXISTS fare_ledger_entry_entry_type_check;
ALTER TABLE payment.fare_ledger_entry
    ADD COLUMN source_key TEXT NOT NULL DEFAULT 'PRIMARY';
ALTER TABLE payment.fare_ledger_entry
    DROP CONSTRAINT IF EXISTS fare_ledger_entry_booking_id_entry_type_key;
ALTER TABLE payment.fare_ledger_entry
    ADD CONSTRAINT fare_ledger_entry_booking_type_source_uk
        UNIQUE (booking_id, entry_type, source_key);
ALTER TABLE payment.fare_ledger_entry
    ADD CONSTRAINT fare_ledger_entry_entry_type_check
    CHECK (entry_type IN (
        'BOOKING_FARE_ESTIMATE', 'PAYMENT_AUTHORIZED', 'PAYMENT_CAPTURED', 'PAYMENT_VOIDED',
        'PAYMENT_REFUNDED', 'PAYMENT_FAILED', 'CASH_COLLECTED', 'COMMISSION_OWED_CASH',
        'DRIVER_EARNING', 'PLATFORM_COMMISSION', 'SETTLEMENT_ADJUSTMENT',
        'FARE_ADJUSTMENT_REQUESTED', 'FARE_FINALIZED', 'PENALTY_CHARGE',
        'PENALTY_DEDUCTION', 'PENALTY_REVERSAL', 'COMPENSATION', 'DUES_SETTLEMENT',
        'REFERRAL_PAYOUT'
    ));

INSERT INTO platform.policy_setting (policy_key, value, value_type, description) VALUES
    ('REFERRAL_PAX_PCT', '1', 'DECIMAL',
     'Share of a referred passenger fare paid from platform commission.'),
    ('REFERRAL_DRIVER_PCT', '2', 'DECIMAL',
     'Share of referred driver net earnings paid from platform commission.'),
    ('REFERRAL_WINDOW_MONTHS', '12', 'INT', 'Referral earning window.'),
    ('REFERRAL_MAX_TRIPS', '50', 'INT', 'Maximum completed trips per referral edge.'),
    ('REFEREE_FIRST_RIDE_DISCOUNT', '150', 'DECIMAL',
     'One-time rewards credit granted to a referred rider.'),
    ('REWARDS_BANK_MINIMUM', '1000', 'DECIMAL', 'Minimum bank withdrawal.')
ON CONFLICT (policy_key) DO NOTHING;

-- Compensation recorded by slice 06 before the real balance existed is replayed once.
INSERT INTO rewards.rewards_ledger
    (app_user_id, kind, amount, label, sublabel, source_penalty_id, occurred_at, idempotency_key)
SELECT pb.beneficiary_app_user_id, 'COMPENSATION', pb.amount, 'Penalty compensation',
       'Credit preserved from before rewards launched', pb.penalty_id,
       COALESCE(pb.credited_at, pa.assessed_at), 'penalty:' || pb.penalty_id || ':' || pb.penalty_beneficiary_id
  FROM penalty.penalty_beneficiary pb
  JOIN penalty.penalty_assessment pa ON pa.penalty_id = pb.penalty_id
 WHERE pb.credited_at IS NOT NULL AND pb.amount > 0
ON CONFLICT (idempotency_key) DO NOTHING;
