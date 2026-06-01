CREATE SCHEMA IF NOT EXISTS app_backend;

CREATE TABLE app_backend.workflow_item (
    workflow_item_id BIGSERIAL PRIMARY KEY,
    item_type VARCHAR(80) NOT NULL,
    owner_role VARCHAR(40),
    owner_app_user_id BIGINT,
    target_type VARCHAR(80),
    target_id VARCHAR(120),
    status VARCHAR(60) NOT NULL DEFAULT 'ACTIVE',
    title VARCHAR(200),
    payload_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workflow_item_type_owner ON app_backend.workflow_item(item_type, owner_app_user_id, workflow_item_id DESC);
CREATE INDEX idx_workflow_item_type_status ON app_backend.workflow_item(item_type, status, workflow_item_id DESC);
CREATE INDEX idx_workflow_item_target ON app_backend.workflow_item(target_type, target_id);
