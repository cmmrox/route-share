-- Real support, safety (SOS), and ratings domains (replace the app_backend.workflow_item shells).
CREATE SCHEMA IF NOT EXISTS support;
CREATE SCHEMA IF NOT EXISTS safety;
CREATE SCHEMA IF NOT EXISTS rating;

CREATE TABLE support.support_ticket (
    support_ticket_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    owner_role  TEXT   NOT NULL,
    subject     TEXT   NOT NULL,
    category    TEXT,
    status      TEXT   NOT NULL DEFAULT 'OPEN'
                  CHECK (status IN ('OPEN','PENDING','RESOLVED','CLOSED')),
    priority    TEXT   NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW','NORMAL','HIGH','URGENT')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX support_ticket_owner_idx ON support.support_ticket(app_user_id, support_ticket_id DESC);
CREATE INDEX support_ticket_status_idx ON support.support_ticket(status, support_ticket_id DESC);

CREATE TABLE support.support_message (
    support_message_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    support_ticket_id BIGINT NOT NULL REFERENCES support.support_ticket(support_ticket_id),
    sender_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    sender_role TEXT NOT NULL,
    body        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX support_message_ticket_idx ON support.support_message(support_ticket_id, support_message_id);

CREATE TABLE safety.sos_event (
    sos_event_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    owner_role  TEXT   NOT NULL,
    trip_id     BIGINT,
    booking_id  BIGINT,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    note        TEXT,
    status      TEXT   NOT NULL DEFAULT 'RAISED'
                  CHECK (status IN ('RAISED','ACKNOWLEDGED','RESOLVED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    resolved_by BIGINT REFERENCES identity.app_user(app_user_id),
    resolution_note TEXT
);
CREATE INDEX sos_event_status_idx ON safety.sos_event(status, sos_event_id DESC);
CREATE INDEX sos_event_user_idx ON safety.sos_event(app_user_id, sos_event_id DESC);

CREATE TABLE rating.rating (
    rating_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES booking.booking(booking_id),
    rater_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    ratee_app_user_id BIGINT REFERENCES identity.app_user(app_user_id),
    rater_role TEXT NOT NULL,
    stars   INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_rating_booking_rater UNIQUE (booking_id, rater_app_user_id)
);
CREATE INDEX rating_ratee_idx ON rating.rating(ratee_app_user_id);
