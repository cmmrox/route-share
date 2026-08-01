-- Slice 03 — the fare engine rewrite.
--
-- Out: 250 base + 90/km + 5/min with a 10% fee added on top.
-- In:  gross = onRouteKm × the vehicle's own chosen rate, less a route-match discount, with the
--      commission taken OUT of what the passenger pays rather than added to it.
--
-- The passenger sees one price. The driver sees the same price and what they keep. There is no base
-- fare and no time component: a rider pays for the distance they actually ride on a route the
-- driver was making anyway.

-- ── the policy surface (decision D1) ─────────────────────────────────────────────────────────────
-- Every figure the product states as a rule lives here, so a number can only be wrong in one place
-- and can be corrected without a deploy.
CREATE TABLE platform.policy_setting (
    policy_key             TEXT PRIMARY KEY,
    value                  TEXT NOT NULL,
    value_type             TEXT NOT NULL
                               CHECK (value_type IN ('INT', 'DECIMAL', 'BOOLEAN', 'STRING')),
    description            TEXT,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by_app_user_id BIGINT REFERENCES identity.app_user(app_user_id)
);

-- Price rules need a paper trail: "what was the commission on 14 March" must be answerable.
CREATE TABLE platform.policy_setting_history (
    policy_setting_history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    policy_key                TEXT NOT NULL,
    old_value                 TEXT,
    new_value                 TEXT NOT NULL,
    changed_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    changed_by_app_user_id    BIGINT REFERENCES identity.app_user(app_user_id)
);
CREATE INDEX policy_setting_history_key_idx
    ON platform.policy_setting_history(policy_key, policy_setting_history_id DESC);

INSERT INTO platform.policy_setting (policy_key, value, value_type, description) VALUES
    ('COMMISSION_PCT',              '10',        'DECIMAL', 'Taken out of what the passenger pays, never added on top.'),
    ('MATCH_DISCOUNT_TIER_95_PCT',  '10',        'DECIMAL', 'Discount when the rider overlaps 95% or more of their own trip with the driver''s route.'),
    ('MATCH_DISCOUNT_TIER_75_PCT',  '8',         'DECIMAL', 'Discount at 75% overlap or more.'),
    ('MATCH_DISCOUNT_TIER_45_PCT',  '5',         'DECIMAL', 'Discount at 45% overlap or more.'),
    ('MATCH_DISCOUNT_TIER_BASE_PCT','2.5',       'DECIMAL', 'Discount below 45% overlap.'),
    ('MATCH_DISCOUNT_THRESHOLD_HIGH','95',       'DECIMAL', 'Overlap percent at which the top discount tier starts.'),
    ('MATCH_DISCOUNT_THRESHOLD_MID','75',        'DECIMAL', 'Overlap percent at which the middle tier starts.'),
    ('MATCH_DISCOUNT_THRESHOLD_LOW','45',        'DECIMAL', 'Overlap percent at which the low tier starts.'),
    ('CURRENCY',                    'LKR',       'STRING',  'The only currency ComiGo prices in.'),
    ('EARLY_DROP_ADJUSTED_PER_MONTH','2',        'INT',     'Early drop-offs repriced on distance travelled, per calendar month.'),
    ('DRIVER_CANCEL_FREE_HOURS',    '12',        'INT',     'Cancelling a published trip is free outside this window.'),
    ('LATE_CANCEL_PENALTY_PCT',     '20',        'DECIMAL', 'Penalty for cancelling inside the free window.'),
    ('PAX_CANCEL_AFTER_START_PCT',  '20',        'DECIMAL', 'Passenger cancels after the trip has started.'),
    ('NO_SHOW_PENALTY_PCT',         '25',        'DECIMAL', 'Passenger never turns up — costs more than telling us early.'),
    ('DRIVER_LATE_GRACE_MIN',       '10',        'INT',     'Minutes before a passenger may cancel a late driver free of charge.'),
    ('DRIVER_LATE_PENALTY_PCT',     '20',        'DECIMAL', 'Charged to a driver who is late past the grace period.'),
    ('PENALTY_VICTIM_PCT',          '50',        'DECIMAL', 'Half of every penalty goes to the person who was let down.'),
    ('PENALTY_PLATFORM_PCT',        '50',        'DECIMAL', 'The remainder. Rounded so the two halves always add back exactly.'),
    ('PICKUP_WAIT_MIN',             '5',         'INT',     'Waiting time at a pickup, from arrival.'),
    ('PICKUP_WAIT_EXTEND_MIN',      '5',         'INT',     'One extension of the pickup wait.'),
    ('PICKUP_WAIT_EXTEND_LIMIT',    '1',         'INT',     'How many times the pickup wait may be extended.'),
    ('START_BUFFER_MIN',            '10',        'INT',     'Grace on the driver''s own departure before the trip auto-cancels.'),
    ('START_EXTEND_MIN',            '10',        'INT',     'One extension of the start buffer.'),
    ('START_EXTEND_LIMIT',          '1',         'INT',     'How many times the start buffer may be extended.'),
    ('MISSED_START_LIMIT',          '3',         'INT',     'Missed starts in a month before the driver profile is deactivated.'),
    ('REVIEW_REPLY_LIMIT',          '1',         'INT',     'One reply per review, both directions.'),
    ('PAYOUT_MINIMUM',              '1000',      'DECIMAL', 'Floor below which a payout is not processed.'),
    ('PAYOUT_DAY',                  'FRIDAY',    'STRING',  'Weekly payout batch day.'),
    ('SEARCH_RADIUS_KM',            '20',        'INT',     'Trips must start within this radius of the rider''s pickup.'),
    ('REFERRAL_PAX_PCT',            '1',         'DECIMAL', 'Share of a referred passenger''s fare, paid out of commission.'),
    ('REFERRAL_DRIVER_PCT',         '2',         'DECIMAL', 'Share of a referred driver''s net earnings, paid out of commission.'),
    ('REFERRAL_WINDOW_MONTHS',      '12',        'INT',     'How long a referral keeps paying.'),
    ('REFERRAL_MAX_TRIPS',          '50',        'INT',     'Whichever ends first, this or the window.'),
    ('REFEREE_FIRST_RIDE_DISCOUNT', '150',       'DECIMAL', 'Flat credit on a referred rider''s first trip.'),
    ('REWARDS_BANK_MINIMUM',        '1000',      'DECIMAL', 'Floor for moving a rewards balance to a bank account.');

-- ── fare quote v2 ────────────────────────────────────────────────────────────────────────────────
-- The old table priced base + distance + fee, a model that no longer exists. Its rows are dev-only
-- (decision D6) and are recreated by scripts/simulation/seed-demo-route.sh.
DROP TABLE IF EXISTS pricing.fare_quote;

CREATE TABLE pricing.fare_quote (
    fare_quote_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id           BIGINT REFERENCES booking.booking(booking_id),
    route_occurrence_id  BIGINT REFERENCES routing.route_occurrence(route_occurrence_id),
    vehicle_id           BIGINT REFERENCES vehicle.vehicle(vehicle_id),
    passenger_app_user_id BIGINT REFERENCES identity.app_user(app_user_id),
    on_route_distance_m  NUMERIC(12, 2) NOT NULL CHECK (on_route_distance_m >= 0),
    rate_per_km          NUMERIC(6, 2) NOT NULL CHECK (rate_per_km > 0),
    seats                INTEGER NOT NULL CHECK (seats >= 1),
    gross_fare           NUMERIC(12, 2) NOT NULL CHECK (gross_fare >= 0),
    match_percent        NUMERIC(5, 2) NOT NULL,
    match_tier           TEXT NOT NULL,
    discount_percent     NUMERIC(5, 2) NOT NULL,
    discount_amount      NUMERIC(12, 2) NOT NULL CHECK (discount_amount >= 0),
    passenger_pays       NUMERIC(12, 2) NOT NULL CHECK (passenger_pays >= 0),
    commission_percent   NUMERIC(5, 2) NOT NULL,
    commission_amount    NUMERIC(12, 2) NOT NULL CHECK (commission_amount >= 0),
    driver_net           NUMERIC(12, 2) NOT NULL CHECK (driver_net >= 0),
    min_fare_applied     BOOLEAN NOT NULL DEFAULT FALSE,
    currency             TEXT NOT NULL DEFAULT 'LKR',
    quoted_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    policy_version       TEXT,
    -- The two invariants of the whole engine, as database guarantees rather than as comments.
    -- An arithmetic bug reaching this table fails the insert instead of quietly mispaying someone.
    CONSTRAINT fare_quote_commission_splits_the_fare
        CHECK (driver_net + commission_amount = passenger_pays),
    CONSTRAINT fare_quote_discount_comes_off_gross
        CHECK (passenger_pays = gross_fare - discount_amount)
);

CREATE INDEX fare_quote_booking_idx ON pricing.fare_quote(booking_id);
CREATE INDEX fare_quote_occurrence_idx ON pricing.fare_quote(route_occurrence_id, fare_quote_id DESC);

-- A booking points at the quote that priced it. The quote is never rewritten, so a receipt read
-- three months later shows the rate and tier that actually applied, not today's.
ALTER TABLE booking.booking
    ADD COLUMN fare_quote_id BIGINT REFERENCES pricing.fare_quote(fare_quote_id);

-- ── retire the old fare model ────────────────────────────────────────────────────────────────────
-- min_fare is the one field worth keeping: a very short overlap must not price below it.
ALTER TABLE finance.fare_policy DROP COLUMN base_fare;
ALTER TABLE finance.fare_policy DROP COLUMN per_km;
ALTER TABLE finance.fare_policy DROP COLUMN per_min;

INSERT INTO finance.fare_policy (name, min_fare, currency, active)
SELECT 'ComiGo default', 100, 'LKR', TRUE
 WHERE NOT EXISTS (SELECT 1 FROM finance.fare_policy WHERE active);
