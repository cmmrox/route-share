-- `booking.seats` is an integer, and the product books a *seat*. P08 offers the front seat beside the
-- driver or a place in the rear row, because that is the only distinction that changes the ride —
-- and it is the distinction a counter cannot express.
--
-- Naming the seats also fixes the race. A counter decrement can be made safe, but two riders taking
-- "the last seat" is then arbitrated by arithmetic rather than by which seat each one holds. One row
-- per slot and a partial unique index turns that into a constraint: whoever inserts first holds
-- slot 3, and the loser is told so rather than being silently short-changed.

-- ── named seat inventory ─────────────────────────────────────────────────────────────────────────
CREATE TABLE routing.route_occurrence_seat (
    route_occurrence_seat_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    route_occurrence_id      BIGINT NOT NULL REFERENCES routing.route_occurrence(route_occurrence_id),
    slot_index               INTEGER NOT NULL CHECK (slot_index >= 1),
    label                    TEXT NOT NULL,
    sub_label                TEXT NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (route_occurrence_id, slot_index)
);

CREATE INDEX route_occurrence_seat_occurrence_idx
    ON routing.route_occurrence_seat (route_occurrence_id, slot_index);

CREATE TABLE booking.booking_seat (
    booking_seat_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id               BIGINT NOT NULL REFERENCES booking.booking(booking_id),
    route_occurrence_seat_id BIGINT NOT NULL
                                 REFERENCES routing.route_occurrence_seat(route_occurrence_seat_id),
    held_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    released_at              TIMESTAMPTZ
);

-- The race guard. Partial, because a released hold must not block the seat being sold again — the
-- history of who held it stays, and only the live hold is exclusive.
CREATE UNIQUE INDEX booking_seat_live_hold_uk
    ON booking.booking_seat (route_occurrence_seat_id)
    WHERE released_at IS NULL;

CREATE INDEX booking_seat_booking_idx ON booking.booking_seat (booking_id);

-- ── approval mode ────────────────────────────────────────────────────────────────────────────────
-- D13: the driver decides per trip whether seats sell instantly or each request is approved. The
-- default is the cautious one; slice 08's driving preferences will supply the account-level default.
ALTER TABLE routing.route_occurrence
    ADD COLUMN approval_mode TEXT NOT NULL DEFAULT 'APPROVE_EACH'
        CHECK (approval_mode IN ('INSTANT', 'APPROVE_EACH'));

-- ── deferred authorisation ───────────────────────────────────────────────────────────────────────
-- An approve-each booking authorises when the driver accepts, not when the rider asks — so the card
-- she chose has to survive the wait. Before this, the payment method was passed straight into the
-- authorisation and never stored, which was fine only because every booking authorised immediately.
ALTER TABLE booking.booking ADD COLUMN payment_method_id BIGINT
    REFERENCES payment.payment_method(payment_method_id);

-- ── request expiry ───────────────────────────────────────────────────────────────────────────────
-- D16: a request the driver never answers must not sit against a rider's two-request allowance for
-- ever, and must not hold a seat nobody is going to sell.
ALTER TABLE booking.booking ADD COLUMN expires_at TIMESTAMPTZ;
ALTER TABLE booking.booking ADD COLUMN expired_at TIMESTAMPTZ;

ALTER TABLE booking.booking DROP CONSTRAINT IF EXISTS booking_status_check;
ALTER TABLE booking.booking
    ADD CONSTRAINT booking_status_check
    CHECK (status IN ('REQUESTED', 'CONFIRMED', 'CANCELLED', 'REJECTED', 'COMPLETED', 'EXPIRED'));

ALTER TABLE booking.booking_status_history
    DROP CONSTRAINT IF EXISTS booking_status_history_to_status_check;
ALTER TABLE booking.booking_status_history
    ADD CONSTRAINT booking_status_history_to_status_check
    CHECK (to_status IN
        ('REQUESTED', 'CONFIRMED', 'CANCELLED', 'REJECTED', 'COMPLETED', 'EXPIRED'));

-- The unnamed table-level check on from_status, which V008 left Postgres to name. Nothing ever
-- transitions *out* of EXPIRED, but leaving the two lists different is the kind of asymmetry that
-- fails a year later on a path nobody predicted.
ALTER TABLE booking.booking_status_history
    DROP CONSTRAINT IF EXISTS booking_status_history_check;
ALTER TABLE booking.booking_status_history
    ADD CONSTRAINT booking_status_history_check
    CHECK (from_status IS NULL OR from_status IN
        ('REQUESTED', 'CONFIRMED', 'CANCELLED', 'REJECTED', 'COMPLETED', 'EXPIRED'));

-- Exactly what the sweep reads: open requests whose deadline has passed.
CREATE INDEX idx_booking_expiring
    ON booking.booking (expires_at)
    WHERE status = 'REQUESTED';

-- ── driver cancellation ──────────────────────────────────────────────────────────────────────────
-- D30 asks for a reason, and D31 reports on the consequence. Both are stored: "he cancelled" without
-- the window it fell in cannot explain a penalty to the driver who was charged for it.
CREATE TABLE routing.route_occurrence_cancellation (
    route_occurrence_cancellation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    route_occurrence_id     BIGINT NOT NULL UNIQUE
                                REFERENCES routing.route_occurrence(route_occurrence_id),
    cancelled_by_app_user_id BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    reason_code             TEXT NOT NULL CHECK (reason_code IN
                                ('VEHICLE_PROBLEM', 'UNWELL', 'PLANS_CHANGED', 'WRONG_DETAILS',
                                 'OTHER')),
    note                    TEXT CHECK (note IS NULL OR length(note) <= 2000),
    hours_before_departure  NUMERIC(6, 2) NOT NULL,
    within_free_window      BOOLEAN NOT NULL,
    penalty_id              BIGINT REFERENCES penalty.penalty_assessment(penalty_id),
    cancelled_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── contact disclosure ───────────────────────────────────────────────────────────────────────────
-- Calls are direct dial (D5), so the app must be handed a real mobile number. This is the trail a
-- harassment report is investigated from, and it is written on every read — including repeats,
-- because "he called me eleven times" is a pattern only repeated reads can show.
CREATE TABLE booking.contact_disclosure_audit (
    contact_disclosure_audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id           BIGINT NOT NULL REFERENCES booking.booking(booking_id),
    reader_app_user_id   BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    subject_app_user_id  BIGINT NOT NULL REFERENCES identity.app_user(app_user_id),
    read_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX contact_disclosure_reader_idx
    ON booking.contact_disclosure_audit (reader_app_user_id, read_at DESC);
CREATE INDEX contact_disclosure_booking_idx
    ON booking.contact_disclosure_audit (booking_id);

-- ── policy ───────────────────────────────────────────────────────────────────────────────────────
INSERT INTO platform.policy_setting (policy_key, value, value_type, description) VALUES
    ('SCHEDULED_REQUEST_EXPIRY_MINUTES', '30', 'INT',
     'How long a scheduled booking request waits for the driver before it lapses (D16).'),
    ('MAX_OPEN_PASSENGER_REQUESTS', '2', 'INT',
     'How many unanswered requests one passenger may hold at once (P11).'),
    ('CONTACT_DISCLOSURE_HOURS_AFTER_DROPOFF', '24', 'INT',
     'How long a counterparty phone number stays readable after the ride ends.')
ON CONFLICT (policy_key) DO NOTHING;

-- ── backfill ─────────────────────────────────────────────────────────────────────────────────────
-- Existing occurrences predate named seats. Without slots they would be unbookable the moment
-- booking starts holding them, so every occurrence gets a seat map sized from its own plan.
INSERT INTO routing.route_occurrence_seat (route_occurrence_id, slot_index, label, sub_label)
SELECT o.route_occurrence_id,
       s.slot_index,
       CASE WHEN s.slot_index = 1 THEN 'Front seat' ELSE 'Back seat' END,
       CASE WHEN s.slot_index = 1 THEN 'Beside the driver' ELSE 'Rear row' END
FROM routing.route_occurrence o
JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
CROSS JOIN LATERAL generate_series(1, GREATEST(p.available_seats, 1)) AS s(slot_index)
ON CONFLICT (route_occurrence_id, slot_index) DO NOTHING;

-- Bookings made before this migration hold seats by count alone. Each takes the lowest free slots it
-- is entitled to — all of them, not one, or a two-seat booking would silently give a seat back.
-- Looped rather than written as one statement so each booking sees the holds the previous one took.
DO $$
DECLARE
    b RECORD;
BEGIN
    FOR b IN
        SELECT booking_id, route_occurrence_id, GREATEST(COALESCE(seats, 1), 1) AS seats
        FROM booking.booking
        WHERE status IN ('REQUESTED', 'CONFIRMED')
          AND route_occurrence_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM booking.booking_seat bs WHERE bs.booking_id = booking.booking.booking_id)
        ORDER BY booking_id
    LOOP
        INSERT INTO booking.booking_seat (booking_id, route_occurrence_seat_id)
        SELECT b.booking_id, s.route_occurrence_seat_id
        FROM routing.route_occurrence_seat s
        WHERE s.route_occurrence_id = b.route_occurrence_id
          AND NOT EXISTS (
              SELECT 1 FROM booking.booking_seat bs
              WHERE bs.route_occurrence_seat_id = s.route_occurrence_seat_id
                AND bs.released_at IS NULL)
        ORDER BY s.slot_index
        LIMIT b.seats;
    END LOOP;
END $$;
