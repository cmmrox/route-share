# Stage 07 — Passenger Mobile App Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Build the passenger React Native app from onboarding through search, booking, live trip, receipt, ratings, safety, and support.

**Architecture:** Feature-based Expo app using shared API client/types. High-frequency realtime state stays isolated from global state to avoid rerender storms.

**Tech Stack:** Expo Dev Build, React Native, TypeScript strict, React Navigation, TanStack Query, Zustand or lightweight store, React Hook Form/Zod, map provider SDK.

---

## Acceptance criteria

- Passenger can login, create profile, search, view matches, book, pay/select cash, track trip, exit early, see receipt, rate driver.
- UI follows existing design assets.
- API models come from shared contracts.
- App handles loading/error/offline states.

## Feature folders

```text
src/features/auth
src/features/profile
src/features/saved-places
src/features/ride-search
src/features/ride-detail
src/features/seat-selection
src/features/booking
src/features/payment
src/features/in-trip
src/features/trip-history
src/features/ratings
src/features/safety
src/features/notifications
src/features/support
```

## Tasks

### Task 1: App shell and navigation

Create navigators:
- Auth stack
- Passenger tabs
- Ride booking stack
- In-trip modal/stack

### Task 2: Design system integration

Create shared components:
- Button
- TextField
- Screen
- Card
- BottomSheet
- RouteTimeline
- FareBreakdown
- SeatMap
- Loading/Error states

### Task 3: Auth screens

Screens:
- Splash
- Onboarding
- Login
- OTP
- Profile setup

### Task 4: Home and search

Screens:
- Home with saved/frequent routes
- Search pickup/drop/time/seats
- Saved places

### Task 5: Results list and map

Requirements:
- Filters: match threshold, price, depart time, rating.
- Show list/map toggle.
- Show match percentage and reason text.

### Task 6: Ride detail

Show:
- Driver/vehicle
- Timeline
- Pickup/drop
- Match explanation
- Fare estimate
- Seat availability
- Actual-distance billing note

### Task 7: Seat selection

Show free/taken/selected seats. Backend confirms final availability.

### Task 8: Payment selection

Support:
- Cash
- Card placeholder/preauth
- Wallet later disabled/coming soon

### Task 9: Booked and live trip

Show:
- Driver arrival
- pickup point
- cancel/share trip
- live driver marker
- ETA
- fare so far
- distance travelled

### Task 10: Exit early

Flow:
- Ask confirmation
- Show original vs adjusted fare
- Call drop-off/exit endpoint when backend supports passenger-initiated early exit.

### Task 11: Receipt and rating

Show actual distance, fare detail, payment/refund, driver/vehicle, trip id, help/rate actions.

### Task 12: Safety/support/account

Screens:
- SOS
- Share trip
- Trusted contacts
- Notifications
- Help center
- Account
- Trip history
