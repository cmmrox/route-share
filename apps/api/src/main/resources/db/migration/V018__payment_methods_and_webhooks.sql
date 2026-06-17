-- Stored card references (provider tokens only; never PAN/CVV) and processed webhook events.

CREATE TABLE payment.payment_method (
    payment_method_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id  BIGINT      NOT NULL REFERENCES identity.app_user(app_user_id),
    provider     TEXT        NOT NULL,
    token        TEXT        NOT NULL,
    brand        TEXT,
    last4        TEXT,
    exp_month    INTEGER,
    exp_year     INTEGER,
    is_default   BOOLEAN     NOT NULL DEFAULT false,
    status       TEXT        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','REMOVED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX payment_method_user_idx ON payment.payment_method(app_user_id, status);
-- At most one default active card per user.
CREATE UNIQUE INDEX payment_method_one_default_idx
    ON payment.payment_method(app_user_id)
    WHERE is_default AND status = 'ACTIVE';

CREATE TABLE payment.payment_webhook_event (
    payment_webhook_event_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider    TEXT        NOT NULL,
    event_id    TEXT        NOT NULL,
    event_type  TEXT,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payment_webhook_event UNIQUE (provider, event_id)
);
