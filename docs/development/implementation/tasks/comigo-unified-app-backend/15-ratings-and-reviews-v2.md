---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 15 — Ratings and Reviews v2

**Goal:** Make rating mutual and symmetric — both sides rate, both publish together, both are named, and each may reply exactly once.

**Depends on:** 05.
**Blocks:** nothing. Final slice.

## Objective

`rating.rating` stores a star, a comment and a rater. The prototype needs considerably more, and the extra
parts are what make the system fair rather than a one-way scoreboard:

- **Both go live together**, so neither side can read the other's score before writing their own (P18: "Both go live together, so neither of you can answer the other's score").
- **Named**, signed with a first name and initial, on both sides.
- **One reply each** — an unfair review sits on a public profile, so the person it is about gets exactly one answer, and after that it stands as written.
- **Tags** with counts ("Safe driving · 142").
- **A star histogram**, from which the headline average is derived rather than typed — arithmetic a reader can check on screen.
- **A passenger aggregate**, because `driver_profile.rating_average` has no counterpart today and P39 is the rider's mirror of D28.

## Scope

In scope:

- Mutual rating per booking, with a publish gate: released when both sides submit, or when the rating window closes.
- Tags per role with aggregated counts.
- One reply per review, enforced.
- Star distribution and derived averages for both roles.
- Passenger rating aggregate on the profile.
- The `showRatingPublicly` setting from slice 10 honoured on read.

Out of scope:

- Reliability counters — slice 05 owns them; D28 and P39 compose both.
- Moderation and takedown — an admin concern, deferred to an operations slice.

## Source material / references

- `docs/source-assets/comigo-prototype/passenger-trip.jsx` — P18 rate driver, tags, the mutual-publish banner and the signing convention.
- `docs/source-assets/comigo-prototype/driver-live.jsx` — D24 rate your passengers, "they're rating you too".
- `docs/source-assets/comigo-prototype/driver-money.jsx` — D28 histogram, tag chips with counts, reviews with a single reply.
- `docs/source-assets/comigo-prototype/passenger-status.jsx` — P39 the rider's own record, `REVIEWS.asRider`.
- `docs/source-assets/comigo-prototype/data.jsx` — `TRUST`, `DRIVER_STARS`, `PAX_STARS`, `ratingFromDist`, `ratingDist`, `REVIEWS`, `POLICY.reviewsNamed`, `reviewReplyLimit`.
- Current code: `rating/**`, `driver/entity/DriverProfileEntity.java`.

## Architecture and design notes

**The average is derived from the histogram, not stored independently.** `data.jsx` computes
`ratingFromDist(distribution)` precisely so the number beside the bars can always be reproduced from
them. Storing an average that a histogram cannot produce is a bug a user can see. So the aggregate table
holds the five counts, and the average is computed — cached, but never authoritative on its own.

**Publish gating has two triggers.** Released when the second rating arrives, or when the window closes
(so a one-sided rating still eventually counts). Until release, neither party may read the other's stars
or comment — enforced in the query, not by hiding fields client-side.

**Reply limit is structural.** One reply per review, enforced by a unique constraint on `review_id`
rather than a counter, so a race cannot produce two.

**Tags are per role and fixed.** Rider-to-driver: Punctual, Safe driving, Friendly, Clean car, Good route.
Driver-to-rider: On time, Clear directions, and the equivalents visible in `REVIEWS.asRider`. A free tag
vocabulary would make the counted chips meaningless.

**Signing convention: first name plus surname initial** ("Nimali P."), on both sides. Never a full name.

**`showRatingPublicly` (S18) affects display, not collection.** Ratings are still recorded and still count
toward reliability; the profile simply does not show the score to others. The setting must be honoured in
every read path, including search results.

## API contracts involved

```
POST /api/v1/bookings/{bookingId}/rating
     { stars, comment?, tags[] }                     (Idempotency-Key)
GET  /api/v1/bookings/{bookingId}/rating             -> mine, and theirs only once released
POST /api/v1/reviews/{reviewId}/reply   { body }     -> 409 REPLY_ALREADY_GIVEN on the second
GET  /api/v1/users/{appUserId}/reviews?role=DRIVER|PASSENGER&page=&size=
GET  /api/v1/me/rating-summary?role=DRIVER|PASSENGER
GET  /api/v1/rating-tags?role=DRIVER|PASSENGER       -> the fixed vocabulary
```

`RatingSummaryResponse`: `average`, `total`, `distribution[{stars, count}]`, `tags[{key,label,count}]`,
`since`, `tripCount`, `visibleToOthers`.

`ReviewResponse`: `id`, `author{displayLabel}` (first name + initial), `stars`, `comment`, `tags[]`,
`createdAt`, `reply{body, repliedAt} | null`, `canReply`.

Changed: search results and booking detail carry `driver.ratingAverage` / `ratingCount` only when
`showRatingPublicly` is true for that user.

New errors: `RATING_ALREADY_SUBMITTED`, `RATING_WINDOW_CLOSED`, `REPLY_ALREADY_GIVEN`,
`REPLY_NOT_PERMITTED`, `RATING_NOT_RELEASED`.

## Database / migration changes

**`V042__ratings_and_reviews_v2.sql`**

- `rating.rating` — add `tags TEXT[] NOT NULL DEFAULT '{}'`,
  `released_at TIMESTAMPTZ NULL`, `window_closes_at TIMESTAMPTZ NOT NULL`,
  `ratee_role TEXT CHECK (ratee_role IN ('DRIVER','PASSENGER'))`.
  The existing `UNIQUE (booking_id, rater_app_user_id)` already prevents double-rating.
- New `rating.review_reply`:
  `id`, `rating_id FK UNIQUE`, `author_app_user_id FK`, `body TEXT NOT NULL CHECK (char_length(body) <= 1000)`,
  `replied_at`. The `UNIQUE` on `rating_id` is the one-reply rule.
- New `rating.rating_aggregate`:
  `id`, `app_user_id FK`, `role TEXT CHECK (role IN ('DRIVER','PASSENGER'))`,
  `stars_1 INT DEFAULT 0`, `stars_2 INT`, `stars_3 INT`, `stars_4 INT`, `stars_5 INT`,
  `total INT GENERATED ALWAYS AS (stars_1+stars_2+stars_3+stars_4+stars_5) STORED`,
  `updated_at`, `UNIQUE (app_user_id, role)`.
  No average column — it is derived, per the design note.
- New `rating.rating_tag` (reference, seeded):
  `tag_key PK`, `role TEXT`, `label TEXT`, `sort_order INT`, `active BOOLEAN`.
- New `rating.rating_tag_count`:
  `id`, `app_user_id FK`, `role TEXT`, `tag_key FK`, `count INT DEFAULT 0`,
  `UNIQUE (app_user_id, role, tag_key)`.
- `driver.driver_profile` — `rating_average` and `rating_count` become derived read-through columns
  maintained from `rating_aggregate`, kept for existing query compatibility.
- Index `idx_rating_unreleased ON rating.rating(window_closes_at) WHERE released_at IS NULL`.

## Configuration / environment changes

- Policy settings: `RATING_WINDOW_HOURS` (default `168`), `REVIEW_REPLY_LIMIT` (1).
- New scheduler job on slice 05's infrastructure: `rating-release`, hourly — releases ratings whose window has closed.

## UI / UX requirements

Backend slice. The contract must supply:

- P18 — the tag vocabulary for rating a driver, the mutual-publish explanation inputs, and the signing label the comment will carry.
- D24 — every passenger on the trip with whether they have already rated, and the driver's rating targets.
- D28 — the histogram, the derived average, the total, the tag chips with counts, the reviews with any single reply, and whether a reply is still available.
- P39 — the same shape for the rider, with her tags and the reviews drivers wrote.
- Search / booking detail — a driver's average and count only when they permit it.

## Implementation steps

1. Extend `rating.rating` with tags, window and release columns; set `window_closes_at` from policy at trip completion.
2. Seed `rating.rating_tag` with the two fixed vocabularies; expose them.
3. Implement submit with idempotency, tag validation against the vocabulary for that role, and the existing per-booking uniqueness.
4. Implement release: on the second submission for a booking, release both; register `rating-release` to release the rest when the window closes.
5. Enforce non-disclosure before release in the read query — a rater must never see the counterpart's stars or comment early.
6. Maintain `rating_aggregate` and `rating_tag_count` on release, not on submit, so unreleased ratings cannot move a public average.
7. Implement one-reply-per-review with the unique constraint and a typed 409.
8. Implement the summary endpoint with the derived average, and assert by test that the average always reproduces from the histogram.
9. Add the passenger aggregate and expose it wherever a driver evaluates a request (slice 07's request list already sorts by verification; the rating is displayed alongside).
10. Honour `showRatingPublicly` in every read path including search enrichment.
11. Keep `driver_profile.rating_average/count` in sync for compatibility, and add a reconciliation test proving the two agree.

## Files expected to change

- `apps/api/.../rating/**` — entities, replies, aggregates, tags, release logic, summary endpoints.
- `apps/api/.../driver/**` — profile aggregate sync.
- `apps/api/.../routing/**` — search enrichment honouring the visibility setting.
- `apps/api/.../booking/**` — request-list rating display.
- `apps/api/.../scheduling/**` — the release job.
- `apps/api/src/main/resources/db/migration/V042__ratings_and_reviews_v2.sql`.
- `apps/api/src/test/java/**` — release gating tests, average-from-histogram property test, reply uniqueness concurrency test, tag validation tests, visibility tests, aggregate reconciliation test.
- `docs/api/mobile-app.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/15-ratings-and-reviews-v2-qa.md`

Maestro: not applicable — no mobile surface in this slice.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='RatingReleaseGateTest,AverageFromHistogramPropertyTest,ReviewReplyUniquenessIT,RatingTagValidationTest,RatingVisibilityTest,RatingAggregateReconciliationTest,RatingReleaseJobIT' test
```

```bash
bash scripts/simulation/verify-ratings.sh
```

The smoke must prove: neither side can read the other's rating before both submit; both release together
on the second submission; an unrated counterpart releases when the window closes; the headline average
always reproduces from the histogram; a second reply is refused; and a driver with
`showRatingPublicly=false` shows no score in search.

## Security, privacy, and observability checks

- Pre-release disclosure is the integrity risk. Test it at the repository query level, not just the controller — a leaked counterpart rating destroys the mutual-publish guarantee the product promises on P18.
- Comments and replies are user content: length-capped, stored as text, escaped in every rendering surface including admin.
- Author labels expose a first name and initial only. Assert by contract test that no full name, phone or email reaches a review payload.
- A user may reply only to a review written about them; a third party replying is a straightforward authorization test.
- `showRatingPublicly` must be enforced server-side on every path; a hidden rating that still ships in a search payload is a silent privacy failure.
- Metrics: `routeshare_ratings_submitted_total{role}`, `routeshare_ratings_released_total{trigger}`, `routeshare_review_replies_total`, gauge of unreleased ratings past their window.
- Alert if the release job leaves ratings unreleased past their window — a stalled job silently freezes both profiles.

## Done criteria

- [ ] Rating is mutual and per booking, with idempotent submission.
- [ ] Both sides release together on the second submission, or on window close.
- [ ] No path discloses a counterpart's rating before release.
- [ ] Tags validated against a fixed per-role vocabulary and counted on release.
- [ ] Exactly one reply per review, enforced by constraint under concurrency.
- [ ] Histogram stored; average derived and provably reproducible from it.
- [ ] Passenger aggregate exists and is shown to drivers evaluating requests.
- [ ] `showRatingPublicly` honoured everywhere including search.
- [ ] `driver_profile` aggregates stay in sync; reconciliation test passes.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): make ratings mutual with paired release, tags, single replies and histograms"
```
