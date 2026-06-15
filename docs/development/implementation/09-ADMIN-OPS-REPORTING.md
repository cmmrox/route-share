# Stage 09 — Admin, Operations, Support, and Reporting Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Build the admin webapp and backend admin APIs for verification, user management, live trips, fare/commission rules, disputes, payments, settlements, and reports.

**Architecture:** Admin UI is Next.js. Admin backend APIs live in `admin`, but must call module application services instead of editing module data directly.

**Tech Stack:** Next.js, TypeScript, Spring Boot admin APIs, RBAC, audit logs, charts, map view.

---

## Acceptance criteria

- Admin can approve/reject driver and vehicle documents.
- Admin can view live/planned/completed/cancelled trips.
- Admin can inspect bookings, fares, payments, disputes, and settlements.
- Every admin action is audited.

## Tasks

### Task 1: Admin auth/RBAC shell

Roles:
- SUPER_ADMIN
- VERIFICATION_AGENT
- SUPPORT_AGENT
- FINANCE_ADMIN
- OPS_ADMIN

### Task 2: Driver/vehicle verification dashboard

Features:
- Pending driver applications
- Document preview metadata
- Approve/reject with reason
- Status history

### Task 3: User management

Features:
- Passenger list
- Driver list
- Account status
- Fraud/support flags

### Task 4: Trip operations dashboard

Views:
- Planned trips
- Live trips
- Completed/cancelled trips
- Trip detail with route, passengers, events

### Task 5: Live map

Show active trips from Redis/WebSocket or backend live API. Do not query raw GPS firehose from PostgreSQL.

### Task 6: Fare and commission settings

Manage:
- price bounds
- default commission rate
- route deviation tolerance
- pickup/drop radius policy

### Task 7: Disputes/support

Features:
- Support tickets
- SOS events
- Payment disputes
- Refund/adjustment workflow

### Task 8: Payment and settlement dashboard

Features:
- Card captures/refunds
- Cash commission receivables
- Driver balances
- Payout batches

### Task 9: Reports

Reports:
- trips created/completed
- average route overlap
- top recurring routes
- revenue/commission
- driver earnings
- passenger booking trends
- cancellation/fraud reports

### Task 10: Admin audit log

Every admin mutation records:
- admin id
- action
- target type/id
- previous status
- new status
- reason
- timestamp
- IP/user agent if available
