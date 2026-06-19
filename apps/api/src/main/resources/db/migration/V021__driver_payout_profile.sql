-- Real driver payout profile (replaces the app_backend.workflow_item shell). One per driver profile.
CREATE TABLE driver.driver_payout_profile (
    driver_profile_id BIGINT PRIMARY KEY REFERENCES driver.driver_profile(driver_profile_id),
    method          TEXT NOT NULL DEFAULT 'BANK_TRANSFER'
                      CHECK (method IN ('BANK_TRANSFER','MOBILE_WALLET')),
    bank_name       TEXT,
    branch          TEXT,
    account_name    TEXT,
    account_number  TEXT,
    wallet_provider TEXT,
    wallet_number   TEXT,
    status          TEXT NOT NULL DEFAULT 'PENDING_VERIFICATION'
                      CHECK (status IN ('PENDING_VERIFICATION','VERIFIED','REJECTED')),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
