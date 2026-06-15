# Stage 06 — Realtime Location Tracking Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Implement scalable live tracking for passenger and driver apps without writing every GPS update directly to PostgreSQL.

**Architecture:** Mobile sends GPS to backend. Backend validates, publishes events, stores latest location in Redis, stores selected audit samples in Postgres, and pushes matched updates via WebSocket.

**Tech Stack:** Spring WebSocket/WebFlux, Redis, Redpanda/Kafka-compatible event flow, PostGIS, Expo location APIs.

---

## Acceptance criteria

- Driver app sends active-trip location updates.
- Backend rejects stale, inaccurate, impossible-jump updates.
- Latest trip/driver location is available in Redis with TTL.
- Passenger receives live updates over WebSocket.
- Fare distance is based on backend route progress, not UI animation.

## Correct pipeline

```text
Mobile GPS
→ Location Ingestion API
→ location.raw event
→ Map Matching / Route Progress
→ location.matched event
→ Redis latest location
→ Realtime Gateway WebSocket
→ Passenger / Driver / Admin apps
```

## Tasks

### Task 1: Location payload contract

Payload fields:
- tripId
- actorType: DRIVER or PASSENGER
- actorId from token, not trusted from body
- lat/lng
- accuracyMeters
- speedMps
- bearingDegrees
- deviceTimestamp
- batteryLevel
- networkType

### Task 2: Location ingestion endpoint

Endpoint:
- `POST /api/v1/location/updates`

Validation:
- Authenticated actor belongs to trip.
- Trip is active.
- Timestamp is fresh.
- Accuracy is within threshold.
- Speed/jump is plausible.

### Task 3: Redis latest-location cache

Keys:
- `trip:{tripId}:driver:latest`
- `trip:{tripId}:passenger:{bookingId}:latest`

TTL: short, e.g. 30–90 seconds.

### Task 4: Raw event publishing

Event:
- `LocationRawReceivedV1`

For MVP, can use in-process event publisher. Keep interface compatible with Kafka/Redpanda.

### Task 5: Map matching / route progress

Initial MVP:
- Project point onto planned route.
- Compute fraction along route.
- Compute distance travelled between accepted matched points.

Later:
- Google Roads, Mapbox, OSRM, or Valhalla snapping.

### Task 6: Persist selected samples only

Persist:
- trip start point
- passenger boarded point
- passenger drop-off point
- trip completion point
- periodic audit sample, e.g. every 30–60 seconds
- suspicious/deviation events

Do not persist every 1–3 second GPS update.

### Task 7: WebSocket subscriptions

Channels:
- `/topic/trips/{tripId}/location`
- `/topic/trips/{tripId}/state`
- `/topic/admin/live-trips`

Authorization:
- Passenger can subscribe only to their active trip.
- Driver can subscribe only to own active trip.
- Admin requires permission.

### Task 8: Mobile location behavior

Driver app:
- Active trip updates every 1–3 seconds or 5–15 meters.
- Increase frequency near pickup/drop-off.
- Reduce strongly outside active trip.

Passenger app:
- Track driver and own pickup/drop if needed.
- Smooth marker animation locally.
- Never use animation as billing source.
