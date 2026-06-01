# Stage 04 — Route Publishing and Route Matching Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Let drivers publish one-time/recurring planned routes and let passengers search for full/partial route matches.

**Architecture:** `routing` owns published route data. `matching` owns search/ranking logic. Both are modules inside the monolith with no direct UI logic.

**Tech Stack:** PostgreSQL/PostGIS, optional H3 indexing, map provider adapter, Spring Boot, React Native maps.

---

## Acceptance criteria

- Driver can create a route with origin, destination, schedule, seats, price/km, booking mode.
- Recurring templates generate route occurrences.
- Passenger search returns ranked matches with match percentage, estimated price, ETA/departure, available seats.
- Matching supports full, partial, high-overlap, and nearby route cases.

## Backend modules

- `routing`
- `matching`
- `driver`
- `vehicle`

## Core data

- `route_template`
- `route_occurrence`
- `route_geometry`
- `route_h3_cell`
- `route_schedule_rule`
- `route_price_rule`

## Tasks

### Task 1: Map provider abstraction

Create interface:
- `GeocodingPort`
- `DirectionsPort`
- `RouteGeometryNormalizer`

Implement local fake adapter first for tests, then Google/Mapbox later.

### Task 2: Route template domain

Rules:
- Only verified drivers can publish.
- Vehicle must be verified.
- Seats offered cannot exceed passenger seat capacity.
- Price per km must be positive and within configured bounds.
- Booking mode is `INSTANT` or `MANUAL_APPROVAL`.

### Task 3: One-time route APIs

Endpoints:
- `POST /api/v1/driver/routes`
- `GET /api/v1/driver/routes`
- `GET /api/v1/driver/routes/{id}`
- `POST /api/v1/driver/routes/{id}/publish`
- `POST /api/v1/driver/routes/{id}/cancel`

### Task 4: Recurring route APIs

Endpoints:
- `POST /api/v1/driver/recurring-routes`
- `GET /api/v1/driver/recurring-routes`
- `POST /api/v1/driver/recurring-routes/{id}/generate-occurrences`

Rules:
- Generate finite future horizon, e.g. 14 or 30 days.
- Avoid duplicate occurrence generation.

### Task 5: Candidate filtering

Use:
- region/city
- departure time window
- available seats
- route status
- pickup/drop proximity with PostGIS `ST_DWithin`
- H3 cells later for broad filtering

### Task 6: Exact overlap validation

Use PostGIS concepts:
- `ST_LineLocatePoint` for pickup/drop along driver route.
- Direction check: pickup fraction < drop fraction.
- `ST_LineSubstring` for passenger segment.
- Distance/overlap percentage.

### Task 7: Ranking model

Initial score:
```text
score = 0.30 overlap + 0.20 pickup proximity + 0.15 drop proximity + 0.15 time compatibility + 0.10 price + 0.10 driver quality
```

API response must explain match:
- `90% route match`
- `Pickup 300m from route`
- `LKR 292 estimated`

### Task 8: Passenger search APIs

Endpoints:
- `POST /api/v1/passenger/ride-searches`
- `GET /api/v1/passenger/ride-searches/{searchId}/results`

### Task 9: Mobile route screens

Driver:
- Create route, create schedule, seats/price, publish confirmation.

Passenger:
- Home, search, results list, results map, ride detail.
