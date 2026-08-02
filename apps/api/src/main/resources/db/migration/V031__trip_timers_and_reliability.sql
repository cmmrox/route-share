-- Slice 05 — trip timers and reliability.
--
-- Sixteen screens count down. Until now nothing counted.
--
-- Four clocks, and the counters they feed. The clocks are rows, not timers held in memory: a
-- restart must not lose a deadline, and any instance must be able to resolve any expiry. The
-- scheduler only *finds* expired rows — the transition itself is the same service method a manual
-- action calls, so an automatic no-show and a driver-tapped one cannot drift apart.

-- ── scheduler infrastructure ─────────────────────────────────────────────────────────────────────
-- Eleven time-driven behaviours across the plan need this. Without leader election a two-instance
-- deploy auto-cancels each trip twice and charges each no-show fee twice.
CREATE SCHEMA IF NOT EXISTS scheduling;

CREATE TABLE scheduling.shedlock (
    name       TEXT PRIMARY KEY,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_at  TIMESTAMPTZ NOT NULL,
    locked_by  TEXT NOT NULL
);

-- Job observability without grepping logs: a silently dead sweeper strands riders and money, and
-- the only way to notice is to record every run, including the ones that did nothing.
CREATE TABLE scheduling.job_run (
    job_run_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_name        TEXT NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    status          TEXT NOT NULL DEFAULT 'RUNNING'
                        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    processed_count INT NOT NULL DEFAULT 0,
    error           TEXT
);

CREATE INDEX job_run_name_started_idx ON scheduling.job_run(job_name, started_at DESC);

-- ── clock 1: the start buffer (D32, D32c, D32b, P24, P35) ────────────────────────────────────────
-- Runs from the trip's departure time and protects the DRIVER from auto-cancellation.
CREATE TABLE trip.trip_start_window (
    trip_id             BIGINT PRIMARY KEY REFERENCES trip.trip(trip_id),
    departs_at          TIMESTAMPTZ NOT NULL,
    buffer_expires_at   TIMESTAMPTZ NOT NULL,
    extension_used      BOOLEAN NOT NULL DEFAULT false,
    extended_expires_at TIMESTAMPTZ,
    resolved_at         TIMESTAMPTZ,
    resolution          TEXT CHECK (resolution IN ('STARTED', 'AUTO_CANCELLED', 'CANCELLED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- An unresolved window has no resolution and a resolved one must say what happened; the pair
    -- is what the sweeper filters on, so letting them disagree would strand rows forever.
    CONSTRAINT trip_start_window_resolution_pair
        CHECK ((resolved_at IS NULL) = (resolution IS NULL)),
    -- The extension is spendable exactly once, and taking it must move the deadline.
    CONSTRAINT trip_start_window_extension_pair
        CHECK (extension_used = (extended_expires_at IS NOT NULL))
);

-- The sweeper reads exactly this: unresolved windows whose effective deadline has passed.
CREATE INDEX trip_start_window_sweep_idx
    ON trip.trip_start_window (COALESCE(extended_expires_at, buffer_expires_at))
    WHERE resolved_at IS NULL;

-- ── clock 2: the pickup wait (D19, D19b, P38, P38b, D21, P27) ────────────────────────────────────
-- Starts on DETECTED GPS ARRIVAL, never on a tap: a driver-triggered clock lets a no-show be
-- manufactured two streets away.
CREATE TABLE trip.pickup_wait (
    pickup_wait_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id             BIGINT NOT NULL REFERENCES trip.trip(trip_id),
    booking_id          BIGINT NOT NULL UNIQUE REFERENCES booking.booking(booking_id),
    arrived_at          TIMESTAMPTZ NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    extension_used      BOOLEAN NOT NULL DEFAULT false,
    extended_expires_at TIMESTAMPTZ,
    resolved_at         TIMESTAMPTZ,
    resolution          TEXT CHECK (resolution IN ('BOARDED', 'NO_SHOW', 'CANCELLED')),
    -- The samples that triggered arrival, so a disputed no-show is investigable rather than a
    -- matter of opinion. Location is evidence here, not telemetry.
    triggered_by_samples JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pickup_wait_resolution_pair
        CHECK ((resolved_at IS NULL) = (resolution IS NULL)),
    CONSTRAINT pickup_wait_extension_pair
        CHECK (extension_used = (extended_expires_at IS NOT NULL))
);

CREATE INDEX pickup_wait_sweep_idx
    ON trip.pickup_wait (COALESCE(extended_expires_at, expires_at))
    WHERE resolved_at IS NULL;
CREATE INDEX pickup_wait_trip_idx ON trip.pickup_wait(trip_id);

-- ── clock 3: the driver-late grace (P34, D41, P35) ───────────────────────────────────────────────
-- Runs from THIS PASSENGER's promised pickup time and protects HER. P35 exists to say these are
-- two different clocks: a trip that left on time can still be twenty minutes from her corner, and
-- his extension is his protection, not an obligation on her.
CREATE TABLE trip.driver_late_grace (
    driver_late_grace_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id           BIGINT NOT NULL UNIQUE REFERENCES booking.booking(booking_id),
    promised_pickup_at   TIMESTAMPTZ NOT NULL,
    grace_expires_at     TIMESTAMPTZ NOT NULL,
    unlocked_at          TIMESTAMPTZ,
    resolved_at          TIMESTAMPTZ,
    resolution           TEXT CHECK (resolution IN ('PICKED_UP', 'FREE_CANCELLED', 'EXPIRED')),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT driver_late_grace_resolution_pair
        CHECK ((resolved_at IS NULL) = (resolution IS NULL))
);

CREATE INDEX driver_late_grace_sweep_idx
    ON trip.driver_late_grace (grace_expires_at)
    WHERE resolved_at IS NULL AND unlocked_at IS NULL;

-- Per-passenger, and deliberately distinct from the trip's departure time (see P35 above).
ALTER TABLE booking.booking ADD COLUMN promised_pickup_at TIMESTAMPTZ;

-- ── reliability ──────────────────────────────────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS reliability;

-- Append-only. Counters are projections of this, never incremented in place: a correction must be
-- possible, and D28/P39 must be able to show what happened rather than only a number.
CREATE TABLE reliability.reliability_event (
    reliability_event_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id          BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    role                 TEXT NOT NULL CHECK (role IN ('DRIVER', 'PASSENGER')),
    event_type           TEXT NOT NULL
                             CHECK (event_type IN ('MISSED_START', 'LATE_CANCELLATION',
                                                   'START_EXTENSION_USED', 'NO_SHOW', 'LATE_CANCEL',
                                                   'EARLY_DROP_ADJUSTED', 'TRIP_COMPLETED',
                                                   'TRIP_BOOKED', 'ON_TIME', 'ON_TIME_OPPORTUNITY',
                                                   'CORRECTION')),
    occurred_at          TIMESTAMPTZ NOT NULL,
    booking_id           BIGINT REFERENCES booking.booking(booking_id),
    trip_id              BIGINT REFERENCES trip.trip(trip_id),
    metadata             JSONB,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX reliability_event_user_month_idx
    ON reliability.reliability_event(app_user_id, role, occurred_at DESC);

-- The projection. period_month is the first day of the calendar month it counts.
CREATE TABLE reliability.monthly_counter (
    monthly_counter_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_user_id           BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    role                  TEXT NOT NULL CHECK (role IN ('DRIVER', 'PASSENGER')),
    period_month          DATE NOT NULL,
    missed_starts         INT NOT NULL DEFAULT 0,
    late_cancellations    INT NOT NULL DEFAULT 0,
    start_extensions_used INT NOT NULL DEFAULT 0,
    no_shows              INT NOT NULL DEFAULT 0,
    late_cancels          INT NOT NULL DEFAULT 0,
    early_drops_adjusted  INT NOT NULL DEFAULT 0,
    trips_completed       INT NOT NULL DEFAULT 0,
    trips_booked          INT NOT NULL DEFAULT 0,
    on_time_events        INT NOT NULL DEFAULT 0,
    on_time_opportunities INT NOT NULL DEFAULT 0,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (app_user_id, role, period_month),
    CONSTRAINT monthly_counter_period_is_first_of_month
        CHECK (date_trunc('month', period_month::timestamp)::date = period_month)
);

-- ── policy ───────────────────────────────────────────────────────────────────────────────────────
-- The prepay threshold is the only figure this slice needs that slice 03 did not already seed.
-- Every other duration and limit it uses is already in platform.policy_setting.
INSERT INTO platform.policy_setting (policy_key, value, value_type, description)
VALUES ('PAX_PREPAY_NO_SHOW_THRESHOLD', '2', 'INT',
        'No-shows in a calendar month after which a passenger must prepay')
ON CONFLICT (policy_key) DO NOTHING;
