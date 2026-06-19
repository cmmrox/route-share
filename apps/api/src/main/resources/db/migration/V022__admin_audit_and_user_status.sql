-- Admin audit trail + user status history (real backing for the admin suite; replaces the
-- app_backend.workflow_item shells for audit/support/SOS/user management).
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.audit_action (
    audit_action_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_app_user_id BIGINT REFERENCES identity.app_user(app_user_id),
    actor_role  TEXT,
    action      TEXT NOT NULL,
    target_type TEXT,
    target_id   TEXT,
    detail_json TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX audit_action_created_idx ON audit.audit_action(audit_action_id DESC);
CREATE INDEX audit_action_target_idx ON audit.audit_action(target_type, target_id);

CREATE TABLE identity.app_user_status_history (
    app_user_status_history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    from_status TEXT,
    to_status   TEXT NOT NULL,
    reason      TEXT,
    changed_by  BIGINT REFERENCES identity.app_user(app_user_id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX app_user_status_history_user_idx
    ON identity.app_user_status_history(app_user_id, app_user_status_history_id DESC);
