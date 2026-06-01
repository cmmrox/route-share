# Stage 05 — Booking, Trip, Fare, Payment, and Settlement Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Implement booking seats, trip operation, fare calculation by actual distance, cash/card payment flow, commission, and driver earnings.

**Architecture:** `booking`, `trip`, `pricing`, `payment`, and `settlement` remain separate internal modules with explicit application services and events.

**Tech Stack:** Spring transactions, PostgreSQL locks/constraints, idempotency keys, payment gateway abstraction, immutable ledger tables.

---

## Acceptance criteria

- Passenger can book available seat(s) without overbooking.
- Driver can approve/decline manual requests.
- Driver can start trip, mark boarded/drop-off, complete trip.
- Fare estimate exists before booking; final fare uses actual matched distance.
- Cash and card payment states are auditable.
- Driver earnings and platform commission are recorded.

## State machines

### Booking states

```text
REQUESTED -> CONFIRMED -> BOARDED -> COMPLETED
REQUESTED -> DECLINED
REQUESTED/CONFIRMED -> CANCELLED
CONFIRMED -> NO_SHOW
```

### Trip states

```text
DRAFT -> PUBLISHED -> SCHEDULED -> STARTED -> COMPLETED
PUBLISHED/SCHEDULED -> CANCELLED
STARTED -> INTERRUPTED only with admin/support reason
```

### Payment states

```text
NOT_REQUIRED
PREAUTH_PENDING -> PREAUTHORIZED -> CAPTURE_PENDING -> CAPTURED
PREAUTHORIZED -> VOIDED
CAPTURED -> REFUNDED/PARTIALLY_REFUNDED
CASH_CONFIRMED -> COMMISSION_RECEIVABLE
FAILED
```

## Tasks

### Task 1: Booking schema and domain

Tables:
- `booking.booking`
- `booking.seat_reservation`
- `booking.booking_status_history`

Rules:
- Seat reservation update must be transactional.
- Use unique/idempotency key for booking request.

### Task 2: Booking APIs

Passenger:
- `POST /api/v1/passenger/bookings`
- `GET /api/v1/passenger/bookings/{id}`
- `POST /api/v1/passenger/bookings/{id}/cancel`

Driver:
- `GET /api/v1/driver/trips/{tripId}/booking-requests`
- `POST /api/v1/driver/bookings/{id}/approve`
- `POST /api/v1/driver/bookings/{id}/decline`

### Task 3: Trip operation domain

Tables:
- `trip.trip_state`
- `trip.passenger_trip_state`
- `trip.trip_stop_event`

Driver actions:
- start trip
- mark passenger boarded
- mark no-show
- mark passenger dropped off
- complete trip

### Task 4: Trip operation APIs

Endpoints:
- `POST /api/v1/driver/trips/{id}/start`
- `POST /api/v1/driver/trips/{id}/passengers/{bookingId}/board`
- `POST /api/v1/driver/trips/{id}/passengers/{bookingId}/no-show`
- `POST /api/v1/driver/trips/{id}/passengers/{bookingId}/drop-off`
- `POST /api/v1/driver/trips/{id}/complete`

### Task 5: Pricing estimate

Before booking:
```text
estimated_fare = planned_overlap_km * route_price_per_km
estimated_commission = estimated_fare * commission_rate
```

Store estimate snapshot so passenger sees what was expected at booking time.

### Task 6: Final fare ledger

Final fare uses backend matched distance while passenger is onboard.

Tables:
- `pricing.fare_estimate`
- `pricing.fare_ledger`
- `pricing.commission_rule`

Ledger must be immutable. Corrections use reversal/adjustment rows.

### Task 7: Payment abstraction

Create `PaymentGatewayPort` with:
- preAuthorize
- capture
- voidAuthorization
- refund

Implement fake sandbox adapter first.

### Task 8: Cash payment handling

When cash:
- mark `CASH_CONFIRMED`
- create settlement commission receivable
- add to driver cash-collected total

### Task 9: Settlement and payout

Tables:
- `settlement.driver_balance`
- `settlement.driver_earning_ledger`
- `settlement.platform_commission_ledger`
- `settlement.payout_batch`

### Task 10: Mobile screens

Passenger:
- seat select, payment, booked, in-trip, exit early, receipt, rating.

Driver:
- trip detail, booking requests, pre-trip checklist, live trip, boarding, drop-off, trip complete, earnings.
