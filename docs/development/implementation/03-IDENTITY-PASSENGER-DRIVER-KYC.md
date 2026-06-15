# Stage 03 — Identity, Passenger, Driver, Vehicle, and KYC Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Integrate Keycloak user management with passenger profile, driver onboarding/KYC, and vehicle management.

**Architecture:** Keycloak owns authentication, authorization, users, sessions, roles, and tokens. The backend owns application profiles, passenger/driver business state, KYC, vehicles, and domain permissions. One Keycloak account may be both passenger and driver.

**Tech Stack:** Keycloak, Spring Security OAuth2 Resource Server, Spring Boot, PostgreSQL, Flyway, React Native forms, object storage abstraction for documents.

---

## Acceptance criteria

- Passenger can register/login through Keycloak and create a passenger profile.
- Same Keycloak user can apply as a driver, receive `DRIVER` role when approved, submit KYC steps, add vehicle, and reach pending review.
- Admin can later approve/reject driver/vehicle documents.
- Document metadata is stored; binary files go to object storage adapter, not DB.

## Backend modules

- `identity` — Keycloak integration, local user reference, role/profile status projection
- `passenger`
- `driver`
- `vehicle`
- `admin` partial review endpoints

## Key entities

- `AppUser`: local reference to Keycloak user, `keycloak_subject`, phone, email, display name, account status projection.
- `PassengerProfile`: app user id / Keycloak subject, name, photo, preferences.
- `SavedPlace`: label, location, lat/lng.
- `TrustedContact`: name, phone.
- `DriverProfile`: app user id / Keycloak subject, KYC status, driver status, rating aggregate.
- `DriverDocument`: type, status, storage key, reviewed by/time.
- `Vehicle`: driver id, make, model, year, colour, registration, seats, status.
- `VehicleDocument`: CR, revenue licence, insurance, vehicle photos.

## Tasks

### Task 1: Keycloak realm and client setup

Create Keycloak realm configuration for:
- Realm: `routeshare`
- Clients: `passenger-mobile`, `driver-mobile`, `admin-web`, `api-monolith`
- Realm roles: `PASSENGER`, `DRIVER`, `ADMIN`, `SUPPORT_AGENT`, `VERIFICATION_AGENT`, `FINANCE_ADMIN`, `OPS_ADMIN`, `SUPER_ADMIN`
- Groups: `Passengers`, `Drivers`, `Admins`, `Support`, `Verification`, `Finance`, `Operations`

**Important rule:** One Keycloak user can have both `PASSENGER` and `DRIVER`. Do not create separate accounts for the same person.

### Task 2: Identity database and Keycloak subject mapping

Create local identity tables for application-owned data only:
- `identity.app_user` with `keycloak_subject` unique
- `identity.user_role_projection` optional cache of Keycloak roles
- `identity.auth_audit` for app-side security events

**Tests:** backend creates/fetches local app user from valid Keycloak token; disabled/suspended local app status blocks app actions even if Keycloak token is valid.

### Task 3: Auth/me and profile status endpoint

Create endpoint:
- `GET /api/v1/auth/me`

Response must include:
- Keycloak subject
- phone/email/name claims
- roles from Keycloak
- `hasPassengerProfile`
- `hasDriverProfile`
- `driverVerificationStatus`
- available app modes: `PASSENGER`, `DRIVER`, `ADMIN` where applicable

**No custom OTP endpoints in backend MVP:** login/OTP/social login flows are handled by Keycloak or its configured identity/SMS providers.

### Task 4: Passenger profile APIs

Create endpoints:
- `GET /api/v1/passenger/profile`
- `PUT /api/v1/passenger/profile`
- `GET/POST/DELETE /api/v1/passenger/saved-places`
- `GET/POST/DELETE /api/v1/passenger/trusted-contacts`

### Task 5: Driver profile and KYC APIs

Create endpoints:
- `POST /api/v1/driver/application`
- `PUT /api/v1/driver/kyc/identity`
- `PUT /api/v1/driver/kyc/licence`
- `POST /api/v1/driver/documents`
- `GET /api/v1/driver/verification-status`

### Task 6: Vehicle APIs

Create endpoints:
- `POST /api/v1/driver/vehicles`
- `GET /api/v1/driver/vehicles`
- `PUT /api/v1/driver/vehicles/{vehicleId}`
- `POST /api/v1/driver/vehicles/{vehicleId}/documents`
- `POST /api/v1/driver/vehicles/{vehicleId}/make-primary`

### Task 7: Admin review shell

Create endpoints:
- `GET /api/v1/admin/driver-applications?status=PENDING`
- `POST /api/v1/admin/driver-applications/{id}/approve` — approves backend driver profile and assigns/keeps Keycloak `DRIVER` role
- `POST /api/v1/admin/driver-applications/{id}/reject`
- `POST /api/v1/admin/vehicles/{id}/approve`
- `POST /api/v1/admin/vehicles/{id}/reject`

### Task 8: Mobile screens wiring

Passenger:
- Login, OTP, profile setup, saved places, account.

Driver:
- Login, KYC identity, KYC licence, vehicle docs, pending review, add vehicle, vehicle list.
