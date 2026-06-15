CREATE TABLE identity.phone_otp_challenge (
    id BIGSERIAL PRIMARY KEY,
    verification_id UUID NOT NULL UNIQUE,
    phone_e164 VARCHAR(16) NOT NULL,
    otp_hash VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    resend_available_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    CONSTRAINT chk_phone_otp_status CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'LOCKED')),
    CONSTRAINT chk_phone_otp_attempts CHECK (attempts >= 0 AND attempts <= 5),
    CONSTRAINT chk_phone_otp_format CHECK (phone_e164 ~ '^\+947[0-9]{8}$')
);

CREATE INDEX idx_phone_otp_phone_status_expires
    ON identity.phone_otp_challenge (phone_e164, status, expires_at DESC);
