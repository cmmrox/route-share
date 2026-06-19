-- Real admin finance domain (replaces workflow_item shells for commission rules, fare policies,
-- finance adjustments, and settlement payout batches).
CREATE SCHEMA IF NOT EXISTS finance;

CREATE TABLE finance.commission_rule (
    commission_rule_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scope     TEXT NOT NULL DEFAULT 'GLOBAL' CHECK (scope IN ('GLOBAL','ROUTE','DRIVER')),
    scope_ref TEXT,
    rate      NUMERIC(5,4) NOT NULL CHECK (rate >= 0 AND rate <= 1),
    active    BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX commission_rule_active_idx ON finance.commission_rule(active, commission_rule_id DESC);

CREATE TABLE finance.fare_policy (
    fare_policy_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       TEXT NOT NULL,
    base_fare  NUMERIC(12,2) NOT NULL CHECK (base_fare >= 0),
    per_km     NUMERIC(12,2) NOT NULL CHECK (per_km >= 0),
    per_min    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (per_min >= 0),
    min_fare   NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (min_fare >= 0),
    currency   TEXT NOT NULL DEFAULT 'LKR',
    active     BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX fare_policy_active_idx ON finance.fare_policy(active, fare_policy_id DESC);

CREATE TABLE finance.payout_batch (
    payout_batch_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status     TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','PAID','CANCELLED')),
    total_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    currency   TEXT NOT NULL DEFAULT 'LKR',
    note       TEXT,
    created_by BIGINT REFERENCES identity.app_user(app_user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at    TIMESTAMPTZ
);
CREATE INDEX payout_batch_status_idx ON finance.payout_batch(status, payout_batch_id DESC);

CREATE TABLE finance.payout_batch_item (
    payout_batch_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payout_batch_id BIGINT NOT NULL REFERENCES finance.payout_batch(payout_batch_id),
    driver_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    amount   NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    currency TEXT NOT NULL DEFAULT 'LKR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX payout_batch_item_batch_idx ON finance.payout_batch_item(payout_batch_id);
CREATE INDEX payout_batch_item_driver_idx ON finance.payout_batch_item(driver_app_user_id);

CREATE TABLE finance.finance_adjustment (
    finance_adjustment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id BIGINT,
    driver_app_user_id BIGINT REFERENCES identity.app_user(app_user_id),
    amount   NUMERIC(14,2) NOT NULL CHECK (amount <> 0),
    currency TEXT NOT NULL DEFAULT 'LKR',
    reason   TEXT NOT NULL,
    created_by BIGINT REFERENCES identity.app_user(app_user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX finance_adjustment_created_idx ON finance.finance_adjustment(finance_adjustment_id DESC);
