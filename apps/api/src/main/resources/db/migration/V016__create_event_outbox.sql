-- Shared transactional outbox for domain events. Rows are written in the same transaction as the
-- state change they describe and relayed to Kafka/Redpanda by OutboxRelayScheduler.
CREATE TABLE common.event_outbox (
    event_outbox_id BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(120) NOT NULL,
    aggregate_type  VARCHAR(80)  NOT NULL,
    aggregate_id    VARCHAR(120) NOT NULL,
    payload_json    TEXT         NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts        INT          NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,
    CONSTRAINT uq_event_outbox_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_event_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- Relay claim query filters on status and orders by id; this partial index keeps it cheap as the
-- table grows and SENT rows accumulate.
CREATE INDEX idx_event_outbox_dispatchable
    ON common.event_outbox (event_outbox_id)
    WHERE status IN ('PENDING', 'FAILED');
