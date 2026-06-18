-- Real notifications domain (replaces the generic app_backend.workflow_item shell for
-- notifications/preferences/push registrations) plus a delivery audit log.
CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.notification (
    notification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    type        TEXT   NOT NULL,
    title       TEXT   NOT NULL,
    body        TEXT,
    data_json   TEXT,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX notification_user_idx ON notification.notification(app_user_id, notification_id DESC);
CREATE INDEX notification_user_unread_idx
    ON notification.notification(app_user_id) WHERE read_at IS NULL;

CREATE TABLE notification.notification_preference (
    app_user_id     BIGINT PRIMARY KEY REFERENCES identity.app_user(app_user_id),
    push_enabled    BOOLEAN NOT NULL DEFAULT true,
    email_enabled   BOOLEAN NOT NULL DEFAULT true,
    booking_updates BOOLEAN NOT NULL DEFAULT true,
    trip_updates    BOOLEAN NOT NULL DEFAULT true,
    payment_updates BOOLEAN NOT NULL DEFAULT true,
    marketing       BOOLEAN NOT NULL DEFAULT false,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notification.push_registration (
    push_registration_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id  BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    platform     TEXT   NOT NULL CHECK (platform IN ('ANDROID','IOS','WEB')),
    token        TEXT   NOT NULL UNIQUE,
    enabled      BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX push_registration_user_idx ON notification.push_registration(app_user_id, enabled);

CREATE TABLE notification.notification_delivery_log (
    notification_delivery_log_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    notification_id BIGINT REFERENCES notification.notification(notification_id),
    channel  TEXT NOT NULL,
    status   TEXT NOT NULL,
    provider_message_id TEXT,
    error    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX notification_delivery_log_notif_idx
    ON notification.notification_delivery_log(notification_id);
