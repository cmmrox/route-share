-- Slice 10: booking-scoped chat, the notification category/channel matrix, contextual SOS,
-- support attachments and durable user settings.
CREATE SCHEMA IF NOT EXISTS chat;

CREATE TABLE chat.chat_thread (
    chat_thread_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE REFERENCES booking.booking(booking_id),
    state TEXT NOT NULL DEFAULT 'OPEN' CHECK (state IN ('OPEN', 'CLOSED')),
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closes_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ
);
CREATE INDEX idx_chat_thread_closing
    ON chat.chat_thread(closes_at) WHERE state = 'OPEN' AND closes_at IS NOT NULL;

CREATE TABLE chat.chat_message (
    chat_message_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_thread_id BIGINT NOT NULL REFERENCES chat.chat_thread(chat_thread_id),
    sender_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    body TEXT NOT NULL CHECK (char_length(body) BETWEEN 1 AND 2000),
    idempotency_key TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_by_counterparty_at TIMESTAMPTZ,
    UNIQUE (chat_thread_id, sender_app_user_id, idempotency_key)
);
CREATE INDEX idx_chat_message_thread_cursor
    ON chat.chat_message(chat_thread_id, chat_message_id);

CREATE TABLE chat.chat_admin_read_audit (
    chat_admin_read_audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_thread_id BIGINT NOT NULL REFERENCES chat.chat_thread(chat_thread_id),
    admin_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    reason TEXT NOT NULL CHECK (char_length(trim(reason)) BETWEEN 3 AND 500),
    read_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_admin_read_audit_thread
    ON chat.chat_admin_read_audit(chat_thread_id, read_at DESC);

ALTER TABLE notification.notification
    ADD COLUMN category TEXT NOT NULL DEFAULT 'ACCOUNT'
        CHECK (category IN ('RIDE', 'DRIVE', 'MONEY', 'ACCOUNT', 'BROADCAST', 'SAFETY')),
    ADD COLUMN deferred BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN action_path TEXT;
UPDATE notification.notification
SET category = CASE
    WHEN type = 'BROADCAST' THEN 'BROADCAST'
    WHEN type LIKE 'BOOKING_%' OR type LIKE 'TRIP_%' OR type = 'DRIVER_ARRIVED' THEN 'RIDE'
    WHEN type LIKE 'PAYMENT_%' OR type LIKE 'PAYOUT_%' OR type LIKE 'PENALTY_%' THEN 'MONEY'
    WHEN type LIKE 'SOS_%' THEN 'SAFETY'
    ELSE 'ACCOUNT'
END;
CREATE INDEX idx_notification_user_category
    ON notification.notification(app_user_id, category, notification_id DESC);

ALTER TABLE notification.notification_preference
    RENAME TO notification_preference_legacy;
CREATE TABLE notification.notification_preference (
    notification_preference_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    category_key TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    push BOOLEAN NOT NULL DEFAULT true,
    sms BOOLEAN NOT NULL DEFAULT false,
    in_app BOOLEAN NOT NULL DEFAULT true,
    mandatory BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (app_user_id, category_key)
);
CREATE INDEX idx_notification_preference_user
    ON notification.notification_preference(app_user_id, notification_preference_id);

-- Preserve existing opt-outs while expanding the old global switches into the S23 matrix.
INSERT INTO notification.notification_preference
    (app_user_id, category_key, enabled, push, sms, in_app, mandatory)
SELECT l.app_user_id, d.category_key,
       d.default_enabled AND CASE d.group_key
           WHEN 'MARKETING' THEN l.marketing
           WHEN 'MONEY' THEN l.payment_updates
           WHEN 'RIDE' THEN l.booking_updates AND l.trip_updates
           ELSE true
       END,
       l.push_enabled AND d.default_push,
       d.default_sms,
       true,
       d.mandatory
FROM notification.notification_preference_legacy l
CROSS JOIN (VALUES
    ('BOOKING_DECISIONS', 'RIDE', true, true, true, true),
    ('DRIVER_ARRIVING', 'RIDE', true, true, false, true),
    ('TRIP_CHANGES', 'RIDE', true, true, true, true),
    ('FEES_AND_DUES', 'MONEY', true, true, false, false),
    ('RECEIPTS', 'MONEY', false, false, false, false),
    ('NEW_BOOKING_REQUESTS', 'RIDE', true, true, true, true),
    ('PASSENGER_CHANGES', 'RIDE', true, true, false, true),
    ('PAYOUTS_AND_PENALTIES', 'MONEY', true, true, false, false),
    ('DOCUMENT_EXPIRY', 'ACCOUNT', true, true, true, true),
    ('SERVICE_UPDATES', 'ACCOUNT', true, true, false, false),
    ('OFFERS_AND_NEWS', 'MARKETING', false, false, false, false),
    ('SAFETY_AND_EMERGENCY', 'SAFETY', true, true, true, true)
) AS d(category_key, group_key, default_enabled, default_push, default_sms, mandatory);
DROP TABLE notification.notification_preference_legacy;

ALTER TABLE safety.sos_event
    ADD COLUMN vehicle_registration TEXT,
    ADD COLUMN snapshot_location geometry(Point, 4326),
    ADD COLUMN snapshot_place_label TEXT,
    ADD COLUMN role TEXT CHECK (role IN ('RIDER', 'DRIVER')),
    ADD COLUMN destination_label TEXT,
    ADD COLUMN contacts_alerted INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN contact_alert_failures INTEGER NOT NULL DEFAULT 0;
ALTER TABLE safety.sos_event
    ADD CONSTRAINT sos_trip_fk FOREIGN KEY (trip_id) REFERENCES trip.trip(trip_id),
    ADD CONSTRAINT sos_booking_fk FOREIGN KEY (booking_id) REFERENCES booking.booking(booking_id);

ALTER TABLE passenger.trusted_contact
    ADD COLUMN auto_share_sos BOOLEAN NOT NULL DEFAULT true;

CREATE TABLE support.support_attachment (
    support_attachment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    support_ticket_id BIGINT NOT NULL REFERENCES support.support_ticket(support_ticket_id),
    support_message_id BIGINT REFERENCES support.support_message(support_message_id),
    object_key TEXT NOT NULL UNIQUE,
    filename TEXT NOT NULL,
    content_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    uploaded_by_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_support_attachment_ticket
    ON support.support_attachment(support_ticket_id, support_attachment_id);

CREATE TABLE platform.user_setting (
    app_user_id BIGINT PRIMARY KEY REFERENCES identity.app_user(app_user_id),
    theme TEXT NOT NULL DEFAULT 'SYSTEM' CHECK (theme IN ('SYSTEM', 'LIGHT', 'DARK')),
    language TEXT NOT NULL DEFAULT 'en' CHECK (language IN ('en', 'si', 'ta')),
    share_live_location BOOLEAN NOT NULL DEFAULT true,
    show_rating_publicly BOOLEAN NOT NULL DEFAULT true,
    receipts_by_email BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE platform.account_request (
    account_request_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    kind TEXT NOT NULL CHECK (kind IN ('DATA_EXPORT', 'DELETION')),
    status TEXT NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED')),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    note TEXT
);
CREATE UNIQUE INDEX idx_account_request_open
    ON platform.account_request(app_user_id, kind)
    WHERE status IN ('QUEUED', 'IN_PROGRESS');

INSERT INTO platform.policy_setting (policy_key, value, value_type, description) VALUES
    ('CHAT_CLOSE_HOURS_AFTER_DROPOFF', '24', 'INT',
     'Hours a booking chat stays open after the passenger is dropped off.')
ON CONFLICT (policy_key) DO NOTHING;
