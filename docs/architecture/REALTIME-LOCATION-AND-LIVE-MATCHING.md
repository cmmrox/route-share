# Real-Time Location and Live Matching Architecture

Date: 2026-08-01
Status: Accepted
Applies to: ComiGo unified app — slices 09 (pickup points), 12 (location pipeline), 13 (live en-route booking)
Related decisions: 016 (route-constrained matching), 017 (right-sized to actual scale)

## 0. Target scale, and what it settles

| Metric | Target |
| --- | --- |
| Trips per day | **500** |
| Typical concurrent trips | **200** |
| Design ceiling | **300** |

That arithmetic decides most of this document:

| | Calculation | Result |
| --- | --- | --- |
| Samples/sec at 4 s cadence | 300 ÷ 4 | **75/sec** |
| HTTP requests/sec (batched 4) | 75 ÷ 4 | **~19/sec** |
| Samples/day | 500 × ~30 min × 15/min | **~225,000** |
| Storage/day | 225k × ~100 bytes | **~22 MB** |
| Storage/year | | **~8 GB** |

**Nineteen requests per second against a table of at most 300 live rows.** There is no scale problem
here. One PostgreSQL instance with the PostGIS indexes already in the schema carries this without
noticing, and the existing search and booking paths are heavier per request.

Everything below therefore spends its complexity budget on **accuracy**, not throughput. Where an
industry technique exists only to survive scale ComiGo will not reach, it is documented and declined,
with the threshold that would justify revisiting it.

## 1. The two problems, which are not the same

The product requirement is that a rider and a driver actually meet. That decomposes into two problems
with different solutions, and conflating them is the classic mistake.

### Problem A — matching

> *Is this driver still behind her pickup point, on his own route?*

A comparison of two fractions along a known line. It **tolerates ~50 m of GPS error**, because a safety
margin absorbs it. Solved by geometry, server-side, in §3.

### Problem B — rendezvous

> *Can these two people physically find each other at the kerb?*

Here 50 m is fatal. In Colombo it puts the pin on the wrong side of Galle Road, past a junction, or
outside a different shop. **No amount of filtering fixes this, because the error is in the map pin, not
in the filter.**

Uber, Grab and PickMe all solve Problem B the same way, and it is barely a GPS problem: named pickup
points, live two-way position in the final approach, the vehicle's plate and colour, and a phone call as
the backstop. The ComiGo prototype already knows this — the chat fixture in `data.jsx` reads:

> *"Hi Nimali — I'll be at the Rajagiriya junction bus halt, not the roundabout. Silver Alto."*

A landmark and a description, not a coordinate. §4 makes that a system feature rather than something a
driver has to think to type.

## 2. What the industry does, and what ComiGo takes

### Uber

- **Adaptive sampling** — ~4–6 s during an active trip, 10–30 s idle, tuned by speed, trip state and battery. **Adopted**, with an addition (§4.3).
- **Persistent connections** — WebSocket streaming, sub-200 ms delivery. **Adopted in hybrid form** (§3.6).
- **Map matching (CatchME)** — Hidden Markov Model snapping traces to the road network. **Not needed** (§3.1).
- **Shadow matching** — 3D city model, probabilistic position from satellite visibility, 20–100 ms per fix. **Declined.**
- **H3 hexagonal index** — 64-bit cell IDs, 16 resolutions, `kRing` neighbour traversal. **Declined at this scale** (§5.1).
- **DISCO dispatch** — offer to the top candidate with an ~8 s timeout, then the next. **Not applicable** — ComiGo is rider-initiated.
- **Ringpop consistent hashing** — cell ID as shard key across nodes. **Declined.**

### UberX Share / Uber Pool — the true product analogue

Uber's shared product caps added detour at **8 minutes**, and reports matched shared trips running only
~3.6 minutes longer on average than unshared ones. **Adopted** as a candidate filter (§4.4).

### PickMe (Sri Lanka)

**PickMe Hitch**, launched May 2022, is ComiGo's model almost exactly: *"a driver sets their starting
point and final destination, and anyone else who needs a ride to a location between these two points can
use the app to request the driver for a ride."* Drivers receive requests in the direction they are
already driving.

What PickMe has published technically: Apache **Kafka + Golang**, event-driven microservices, a
centralised schema registry, ~1M events/sec across 5,000+ CPUs by 2019, and **Google Directions + Places**
— with Directions explicitly cited as informing which driver is assigned.

What PickMe has **not** published: their matching algorithm, position handling, GPS filtering, or whether
Hitch supports mid-trip joining at all. There is no PickMe engineering blog. Nothing here is
reverse-engineered or inferred; the model is validated in-market, the implementation is not public.

Their one visible technical choice — leaning on Google Directions for assignment — is one ComiGo
deliberately went the other way on, serving polylines from stored PostGIS geometry (July 2026 cost work).
Their Kafka + Go stack answers a scale question ComiGo does not have.

## 3. Problem A — the matching pipeline

### 3.1 Route-constrained matching, not road-network matching

**An Uber driver has no published route**, so Uber must map-match against the whole road network. **A
ComiGo driver publishes his route before departure**, already stored as
`routing.route_plan.route_line geometry(LineString, 4326)`, validated and already used for booking
fractions.

That turns *"which road is he on"* into *"how far along this one line is he"*:

```sql
ST_LineLocatePoint(route_line, point)  -- fraction 0…1, directly
ST_Distance(route_line, point)         -- perpendicular offset, for the corridor test
```

One call, no external API, and **more accurate for this question** than general map matching, because the
search space is a single line rather than thousands of candidate segments.

### 3.2 Sampling (device)

| State | Interval | Priority |
| --- | --- | --- |
| **Final approach** — within 500 m of a pickup | **1–2 s** | High accuracy |
| Trip running | 4 s | High accuracy |
| Trip running, battery < 15% | 10 s | Balanced |
| Published, not started | 30 s | Balanced |
| Idle | none | — |

The approach tier is where ComiGo's small scale buys something Uber cannot afford. Even if 10% of trips
are approaching simultaneously, the load is ~25 req/sec — still trivial — and it concentrates precision on
the ninety seconds that decide whether the pickup works.

Battery remains the real constraint, which is why it is a short burst rather than a global rate. Android
needs a foreground service with a persistent notification; `FusedLocationProvider` fuses GPS, Wi-Fi, cell
and inertial sensors — the same class of sensor fusion Uber uses, supplied free by the OS. iOS uses
`kCLLocationAccuracyBestForNavigation` while a trip is active.

**Reject bad fixes on the device**, before transmission. A fix the phone already knows is poor should not
consume a request, a row, or server CPU.

### 3.3 The four filters

Applied in order to every accepted sample.

| Filter | Rule | Removes |
| --- | --- | --- |
| Accuracy gate | drop `accuracyMeters > 50` | fixes whose own error exceeds a city block |
| Speed gate | drop if implied speed > 40 m/s from the last accepted sample | the classic jump-across-the-city-and-back |
| Route projection | flag `OFF_ROUTE` if perpendicular offset > 80 m | points not on this route at all |
| Monotonic clamp | reject a lower fraction unless two consecutive samples confirm it | residual jitter around the true position |

The monotonic clamp is the highest-value rule and **exists only because the route is known**. It also
resolves self-intersecting routes: of the candidate fractions a loop could produce, take the one nearest
to — and not behind — the previous accepted fraction.

### 3.4 Confidence, and failing closed

Between samples the position is **extrapolated** along the route from the last fraction, speed and
elapsed time. Without this a rider's joinable list flickers whenever a ping is late on a weak network.

| Confidence | Meaning | Offerable? |
| --- | --- | --- |
| `MATCHED` | from a sample accepted this tick | yes |
| `EXTRAPOLATED` | dead-reckoned, within the 20 s cap | yes |
| `STALE` | past the cap | **no** |
| `OFF_ROUTE` | outside the corridor past the grace window | **no** |

A position that cannot be **proven** behind a pickup is never offered. Guessing in the rider's favour is
precisely the failure the prototype forbids.

### 3.5 Candidate lookup — PostGIS, not H3

At a ceiling of 300 live trips, joinable search filters **at most 300 rows**. A GiST index on the driver's
last known position with `ST_DWithin` resolves that in well under a millisecond — faster than the H3 path,
because there is no cell arithmetic, no `kRing` expansion and no join.

```sql
SELECT ... FROM location.trip_progress p
 WHERE p.confidence IN ('MATCHED','EXTRAPOLATED')
   AND ST_DWithin(p.last_position::geography, :pickup::geography, :radiusMeters)
```

with `CREATE INDEX ... USING GIST (last_position)`.

See §5.1 for why H3 was declined and the threshold that would reverse it.

### 3.6 Real-time delivery

The 45-second driver prompt is the one place where delivery latency is a product requirement.

Android **Doze** defers WebSocket traffic and normal-priority messages until a maintenance window.
**FCM high-priority messages wake the radio and are delivered immediately even in Doze** — which is why
they exist. They are rate-limited (~240/device/minute) and drain battery when abused.

| App state | Channel |
| --- | --- |
| Foregrounded, trip running | WebSocket / SSE — instant, no FCM quota consumed |
| Backgrounded, screen off, Doze | FCM **high priority**, prompt payload inline |

High priority is reserved for live offers, SOS and trip-critical alerts. The offer is authoritative on the
server either way: the countdown is a database `expires_at` swept by the scheduler, and the client timer is
cosmetic.

## 4. Problem B — making the rendezvous work

### 4.1 Named pickup points

A raw coordinate is not an instruction. Every pickup and drop-off resolves to a **named point** with a
human description, which is what both sides actually see and what the driver navigates to.

Three sources, layered:

| Tier | Source | When |
| --- | --- | --- |
| **1 — Curated** | operator-maintained landmark list per corridor | overrides everything; built via an admin screen |
| **2 — Derived** | nearest transit stop / notable POI from Google Places, resolved at booking | the launch default — zero curation, works on day one |
| **3 — Learned** | points where pickups actually succeed, promoted after repeated use | added once real usage data exists |

Launch with tier 2, schema ready for tier 1, tier 3 later. That gives something usable immediately, an
override where Google picks something odd, and a path that improves by itself.

The resolved point carries `label`, `description`, `side_hint` (which side of the road), and coordinates.
It is what appears on the driver's navigation card, in the rider's booking, and in the chat quick-replies.

### 4.2 Two-way position during the final approach

Within 500 m of a pickup, **both sides see each other**: she sees his car moving, he sees her pin. Outside
that window, the rider's position is never shared with the driver.

This is deliberately the narrowest possible privacy window — it is the only moment the information is
needed, and permanently sharing a rider's position with a driver would be indefensible.

### 4.3 Approach mode

Entering the 500 m window raises sampling to 1–2 s on both devices, opens two-way position, and switches
the driver's card to the named pickup point with its description. Leaving it, or completing boarding,
closes both.

### 4.4 Detour cap

UberX Share limits added detour to **8 minutes**. ComiGo's prototype shows `+2 min · +0.4 km` on the driver
prompt but states no ceiling — so nothing currently prevents a live request adding fifteen minutes to a
driver already carrying three passengers with their own promised arrival times.

Candidates exceeding `LIVE_MAX_ADDED_MINUTES` (default 8) or `LIVE_MAX_ADDED_KM` are filtered **before the
prompt reaches the driver** — the same principle as the behind-pickup rule: never make him decline
something he should not have been offered.

### 4.5 Off-route fallback

If the driver leaves the corridor by more than 80 m for longer than `OFF_ROUTE_GRACE_SECONDS` (60 s),
route-constrained matching no longer describes reality:

1. Mark `OFF_ROUTE`; **stop offering live seats immediately** (fail closed).
2. Keep storing samples for the trail and for fare-adjustment review.
3. On rejoining, resume from the **projected** fraction, never the stale one.
4. Optionally snap through Google Roads API for recovery — **off-route only**, which is a small fraction of samples. On-route snapping is never worth it, because route projection is both better and free.
5. A self-hosted map-matching engine (Valhalla Meili) stays on the shelf until production evidence shows sustained off-route driving is common.

## 5. Declined, with thresholds

### 5.1 H3 hexagonal indexing

| | H3 | GiST + `ST_DWithin` |
| --- | --- | --- |
| Query time at 300 live trips | sub-ms | sub-ms |
| Postgres extension (`h3-pg`) | required | none |
| Java dependency (`h3-java`) | required | none |
| Managed-Postgres compatibility | at risk | fine |
| Code and tests to maintain | meaningful | none — PostGIS already does it |
| Pays off from | ~50,000 concurrent trips | — |

**Declined.** H3 exists to avoid scanning millions of rows; ComiGo scans at most 300. Adding it would cost
a Postgres extension, a JVM dependency and a body of tests, in exchange for no measurable gain.

**Revisit threshold: sustained concurrent trips above 5,000**, or a measured p95 on the joinable query
above 50 ms. Recorded so the decision is deliberate rather than forgotten.

### 5.2 Everything else

| Technique | Decision | Threshold to revisit |
| --- | --- | --- |
| Kafka / event streaming platform | Declined — the existing transactional outbox is correct here | > 10,000 events/sec sustained |
| Ringpop / consistent hashing / sharding | Declined | multi-region, or a single Postgres saturating |
| 3D shadow matching | Declined — needs a 3D model of Colombo, 20–100 ms per fix | never, at this product scale |
| HMM road-network map matching | Deferred to §4.5 | sustained off-route driving proven common |
| Google Roads snap on every ping | Declined — ~450,000 calls/hour at 500 concurrent, and it would reverse the July 2026 cost work | never |
| DISCO-style offer rotation | Not applicable — ComiGo is rider-initiated; she picks the driver, so there is no candidate queue and no fallback, which is also why the window is 45 s rather than 8 s | — |

## 5.5 Google API cost model

Cost control is a first-class constraint, not an afterthought. The rule for this architecture:

> **Never call Google for something the database already knows, and never call it per-ping.**

### Current SKU rates (verify against your billing console — tiers vary by volume)

| SKU | Rate |
| --- | --- |
| Places Autocomplete, per-request | $2.83 / 1,000 |
| Places Autocomplete **inside a session** | **$0** |
| Place Details — **Essentials** | $5 / 1,000 |
| Place Details — Pro | $17 / 1,000 |
| Place Details — Enterprise | $20 / 1,000 |
| Distance Matrix — Essentials | ~$2.04 / 1,000 elements |
| Monthly credit | $200 |

**One Pro-tier field upgrades the entire request to Pro price.** This is why the July 2026 work reduced
the Place Details field mask to `id,formattedAddress,location` and dropped `displayName` — a single Pro
field would have tripled the cost of every call. That constraint holds for anything added later.

### Where calls are allowed, and where they are forbidden

| Operation | Google call? | Why |
| --- | --- | --- |
| Rider types a destination | **Yes** — Autocomplete in a session + one Place Details (Essentials) | Unavoidable; session token makes autocomplete free |
| Driver publishes a route | **Yes** — one Directions call | Once per route plan; recurring routes amortise it across every generated occurrence |
| Fare estimate | **Cached** Distance Matrix, ~110 m rounding, 7-day TTL | Mostly hits |
| Ride-detail polyline | **No** — `ST_LineSubstring` on the stored route line | July 2026 work |
| **Driver position → route fraction** | **No** — `ST_LineLocatePoint` | §3.1 |
| **ETA to pickup / destination** | **No** — remaining route distance ÷ the trip's own observed speed | §5.6 |
| **Live-request detour minutes** | **No** — route geometry + observed speed | §5.6 |
| **Named pickup point** | **Rarely** — route labels first, persisted rows second, Places last | §5.7 |
| Any per-location-ping operation | **Never** | ~450,000 calls/hour at 500 concurrent — the design's hard line |

### Estimated steady state at 500 rides/day

| Item | Calls/day | Cost/day | Cost/month |
| --- | --- | --- | --- |
| Autocomplete (in session) | ~4,000 | **$0.00** | **$0** |
| Place Details (Essentials, ~1.5 sessions/ride) | 750 | $3.75 | ~$112 |
| Directions (new route plans only) | ~30 | $0.15 | ~$5 |
| Distance Matrix (cache misses) | ~200 | $0.41 | ~$12 |
| Pickup points (after warm-up) | ~20 | $0.10 | ~$3 |
| **Location pipeline** — all GPS, ETA, detour, matching | **0** | **$0.00** | **$0** |
| **Total** | | **≈ $4.41/day** | **≈ $132/month** |

**Inside the $200 monthly credit, so actual payable is $0.** Per ride: **$0.0088** — under one US cent,
about LKR 3.

Directions stays negligible because of **recurring routes**: one route plan generates twelve occurrences,
so the single Directions call amortises across every trip that comes from it.

### What the optimisations are worth

| Path | Naive | Optimised |
| --- | --- | --- |
| Autocomplete without session tokens | $11.32/day | **$0** |
| Place Details on Pro mask instead of Essentials | $12.75/day | **$3.75** |
| Ride-detail polylines via Directions | $2.50/day | **$0** — stored geometry |
| ETA via Directions | ~$12.50/day | **$0** — derived (§5.6) |
| Detour via Directions per candidate | ~$2.50/day, rising with live usage | **$0** — derived (§5.6) |
| Pickup points uncached | $5.00/day | **$0.10** — §5.7 |
| **Subtotal** | **~$46/day ≈ $1,380/month** | **~$4.41/day ≈ $132/month** |

Roughly a **10× reduction** — and that excludes the one that would genuinely hurt: snapping every GPS ping
through Roads API would be ~**225,000 calls/day**, dwarfing everything above combined.

### Sensitivity — the number that actually moves the bill

Place Details is ~85% of spend, and search sessions per ride is the least certain assumption in this
model. Everything else is bounded by trip count; this one is bounded by rider behaviour.

| Search sessions per ride | Place Details/day | Cost/day | Cost/month |
| --- | --- | --- | --- |
| 1.5 — modelled | 750 | $3.75 | ~$132 |
| 3 — heavier browsing | 1,500 | $7.50 | ~$245 |
| 5 — very heavy browsing | 2,500 | $12.50 | ~$395 |

Even at five sessions per ride the bill stays manageable, and the billing dashboard surfaces it long before
it becomes a surprise.

Two guardrails protect this line specifically:

- The **Essentials field mask** is enforced by a build-failing contract test. One Pro-tier field takes Place Details from $5 to $17 per 1,000 — instantly tripling the largest line item.
- **Saved places match locally** (July 2026 work), so a commuter travelling Home → Work generates zero Places calls.

### Estimate confidence

Three inputs are estimates rather than measurements, and should be re-checked against real usage:

1. **~1.5 search sessions per booked ride** — the dominant variable; see the sensitivity table.
2. **~30 new route plans/day** — assumes most trips come from recurring commuter templates. A small line either way.
3. **Warm-up period** — pickup points cost more in the first weeks (perhaps $1/day) while the library fills, falling toward zero as Colombo's finite set of sensible stopping places gets covered.

SKU rates and volume tiers change. **Re-verify against the billing console before launch**; the figures
above are published rates at the time of writing, not a contract.

## 5.6 Deriving ETA and detour without Google

Two numbers the product needs looked like Google calls and are not.

**ETA.** `remainingRouteMeters ÷ observedSpeed`, where `remainingRouteMeters` comes from
`ST_Length(ST_LineSubstring(route_line, currentFraction, 1.0))` and `observedSpeed` is an
exponentially-smoothed average of `trip_progress.speed_mps` over the last few minutes.

**Live-request detour.** `addedMeters` is the difference between the driver's current route length and the
length including the rider's pickup and drop-off — pure geometry on a line already in the database.
`addedMinutes = addedMeters ÷ observedSpeed`.

Both are **free, and more accurate than a Google estimate**, because they use the traffic that specific
driver is actually sitting in right now rather than a generic model of that road. A driver crawling
through Borella at 8 km/h gets an ETA reflecting 8 km/h.

Fallback: with no observed speed yet (trip just started), use the corridor's historical median speed, and
only if that is also missing fall back to a cached Distance Matrix lookup. In practice the third tier is
almost never reached.

## 5.7 Keeping pickup points nearly free

Named pickup points (§4.1) would be the plan's largest new Google cost if resolved naively — 500 trips ×
2 points/day ≈ 30,000 Place Details calls/month, roughly **$150** and enough on its own to break the
$200 credit. Four measures reduce steady state to near zero:

1. **Route labels first.** Every published route already carries `origin_label` and `destination_label`, resolved through Places when the driver created it. Those are landmark names already paid for.
2. **Seed the curated tier.** Load the top ~200 Colombo landmarks — junctions, bus halts, stations, well-known buildings — once, up front. One-time effort that covers most launch-corridor pickups permanently.
3. **Persist every derived point and reuse it.** Colombo corridors have a finite number of sensible stopping places. The second rider at the same corner costs nothing, and the hit rate climbs toward 100% as the library fills.
4. **Resolve once per unique location at booking time.** Never per search keystroke, never per location ping.

Steady-state expectation after warm-up: **a few hundred calls per month**, and falling.

## 6. Cost and load

**Zero external API cost is added by the location pipeline.** Every step is PostGIS and an index — see
§5.5 for the full cost model and §5.6 for how ETA and detour avoid Google entirely.

| | Value |
| --- | --- |
| Peak request rate | ~19/sec (~25/sec with approach mode) |
| Peak rows scanned per joinable query | ≤ 300 |
| Google calls added | **0** |
| Google calls if every ping were snapped | ~450,000/hour at 500 concurrent |
| Storage | ~22 MB/day, ~8 GB/year before retention |

Named pickup points consume Google Places at **booking** time, not per ping, reuse the existing
session-token and Redis-cache machinery, and fall to near-zero after warm-up (§5.7).

Total estimated Google spend at target scale: **~$132/month, inside the $200 monthly credit** — with the
location pipeline, ETA, detour calculation and steady-state pickup points all contributing nothing.

## 7. Configuration

All in `platform.policy_setting` (Decision 012) except device-side sampling, which is served from
`GET /api/v1/driver/location-policy` so cadence is tuned server-side without an app release.

| Key | Default |
| --- | --- |
| `LOCATION_ACCURACY_MAX_METERS` | 50 |
| `LOCATION_MAX_SPEED_MPS` | 40 |
| `ROUTE_CORRIDOR_METERS` | 80 |
| `ROUTE_REVERSAL_TOLERANCE_FRACTION` | 0.005 |
| `EXTRAPOLATION_MAX_SECONDS` | 20 |
| `LIVE_FRACTION_STALENESS_SECONDS` | 60 |
| `OFF_ROUTE_GRACE_SECONDS` | 60 |
| `LIVE_PICKUP_SAFETY_MARGIN_FRACTION` | 0.01 |
| `APPROACH_RADIUS_METERS` | 500 |
| `APPROACH_SAMPLE_INTERVAL_SECONDS` | 2 |
| `LIVE_MAX_ADDED_MINUTES` | 8 |
| `LIVE_MAX_ADDED_KM` | 3 |
| `SPEED_SMOOTHING_WINDOW_SECONDS` | 180 |
| `CORRIDOR_FALLBACK_SPEED_KMH` | 22 |

## 8. Sources

- [H3: Uber's Hexagonal Hierarchical Spatial Index — Uber Engineering](https://www.uber.com/us/en/blog/h3/)
- [H3 resolution table — h3geo.org](https://h3geo.org/docs/core-library/restable/)
- [Improving Uber's Mapping Accuracy with CatchME — Uber Engineering](https://www.uber.com/en-GB/blog/mapping-accuracy-with-catchme/)
- [How Uber's Real-Time Location Tracking System Works at Scale](https://www.bnxt.ai/blog/how-ubers-real-time-location-tracking-system-works-at-scale)
- [How Uber Built Their Dispatch System](https://archon-eight.vercel.app/company-architecture/uber-dispatch)
- [Uber Engineering's Ringpop](https://www.uber.com/blog/ringpop-open-source-nodejs-library/)
- [UberX Share — how it works for riders](https://www.ridester.com/uberx-share/)
- [Uber Pool cost optimisation at scale](https://www.markhub24.com/post/uber-pool-s-cost-optimization-strategy-engineering-shared-mobility-at-scale)
- [Detour penalty analysis: UberPool vs UberX, Toronto](https://www.sciencedirect.com/science/article/abs/pii/S1361920920307276)
- [PickMe launches carpooling with PickMe Hitch — Arteculate](https://arteculate.asia/pickme-hitch-announced/)
- [Persistent: PickMe on Google Maps Platform](https://www.persistent.com/client-success/pickme-hails-google-maps-and-google-workspace-with-persistent-for-increased-agility-availability-and-scalability/)
- [PickMe — Wikipedia](https://en.wikipedia.org/wiki/PickMe)
- [Snap to Roads — Google Roads API](https://developers.google.com/maps/documentation/roads/snap)
- [Roads API advanced concepts (100-point limit, request stitching)](https://developers.google.com/maps/documentation/roads/advanced)
- [Map Matching with Valhalla's Meili](https://towardsdatascience.com/map-matching-done-right-using-valhallas-meili-f635ebd17053/)
- [Set and manage Android message priority — Firebase](https://firebase.google.com/docs/cloud-messaging/android/message-priority)
- [Ensure your FCM notifications reach your users on Android — Firebase Blog](https://firebase.blog/posts/2025/04/fcm-on-android/)
- [Optimize location use for real-world scenarios — Android Developers](https://developer.android.com/develop/sensors-and-location/location/battery/scenarios)
- [Google Maps Platform core services pricing](https://developers.google.com/maps/billing-and-pricing/pricing)
- [Google Maps Platform SKU details](https://developers.google.com/maps/billing-and-pricing/sku-details)
- [Places API usage and billing](https://developers.google.com/maps/documentation/places/web-service/usage-and-billing)
- [Google Places API pricing: real cost per session](https://www.woosmap.com/blog/google-places-api-pricing)
- [Google Maps API pricing 2026: exact cost per 1,000 calls](https://mapatlas.eu/blog/google-maps-api-pricing-2026)
