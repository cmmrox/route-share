# Stage 08 — Driver Mobile App Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Build the driver React Native app for onboarding/KYC, vehicle management, route publishing, trip operation, earnings, payout, safety, and support.

**Architecture:** Feature-based Expo app using shared contracts. Live trip and location state must be battery-aware and isolated from low-frequency UI state.

**Tech Stack:** Expo Dev Build, React Native, TypeScript strict, React Navigation, TanStack Query, background location APIs, map provider SDK.

---

## Acceptance criteria

- Driver can apply, submit KYC, add vehicle, publish route, handle booking requests, operate live trip, mark boarding/drop-off, see earnings.
- Background/foreground location behavior is explicit and permission-aware.
- Driver cannot publish/operate trips before verification rules allow it.

## Feature folders

```text
src/features/auth
src/features/kyc
src/features/vehicles
src/features/route-create
src/features/schedule
src/features/booking-requests
src/features/trip-operation
src/features/live-location
src/features/earnings
src/features/payout
src/features/ratings
src/features/safety
src/features/notifications
src/features/support
```

## Tasks

### Task 1: Driver app shell

Create navigation:
- Auth/KYC stack
- Main driver tabs
- Route creation stack
- Live trip stack

### Task 2: Driver auth and onboarding

Screens:
- Splash
- Onboarding
- Login
- Driver application start

### Task 3: KYC flow

Screens:
- Identity details
- NIC/passport upload
- Selfie upload
- Driving licence details/upload
- KYC pending review

### Task 4: Vehicle management

Screens:
- Add vehicle
- Vehicle document upload
- Vehicle list
- Make primary
- Verification status

### Task 5: Home/dashboard

Show:
- Today earnings
- Next trip
- Weekly schedule
- Rating
- Quick actions

### Task 6: Route creation wizard

Steps:
- Draw/select route from/to
- Schedule one-time or recurring
- Seats and price/km
- Booking mode
- Publish confirmation/share link

### Task 7: Trip list and booking requests

Screens:
- Upcoming/live/past trips
- Booking request approvals
- Passenger details and pickup point

### Task 8: Pre-trip checklist

Require before start:
- driver alert
- vehicle ok
- seatbelts ok
- phone mounted/charged

### Task 9: Live trip operation

Features:
- Navigation instruction
- next pickup/drop
- passenger strip
- seats occupied
- earning so far
- location updates to backend

### Task 10: Boarding and drop-off

Actions:
- Mark boarded
- Mark no-show
- Mark dropped off
- Show card/cash payment status

### Task 11: Trip completion

Show:
- passenger fare breakdown
- platform commission
- net earnings
- trip summary
- rating prompt

### Task 12: Earnings/payout/ratings/account

Screens:
- Earnings dashboard
- Payout account
- Ratings/reviews
- Account
- Notifications
- SOS
- Help center
- Leaderboard
