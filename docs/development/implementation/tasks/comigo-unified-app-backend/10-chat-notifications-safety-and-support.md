---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 10 — Chat, Notifications, Safety and Support

**Goal:** Give a booking a conversation, give the inbox real categories and channels, make SOS carry context, and make the app shell's badges truthful.

**Depends on:** 01, 07.
**Blocks:** nothing. Can run in parallel with 11–15 once 07 lands.

## Objective

Chat is referenced as a call-to-action from roughly fifteen screens and does not exist. The notification
domain exists but not the per-category × per-channel matrix S23 renders, nor the broadcast kind S22 shows,
nor the badge rules S14 specifies. SOS exists but does not carry the trip, vehicle and live location the
prototype promises to send. Support tickets exist without attachments.

This slice closes all four, plus the user settings screen (S18) that has no backend at all.

## Scope

In scope:

- Booking-scoped chat: opens on confirmation, closes 24 h after drop-off, quick replies, support-readable.
- Notification categories × channels (push / SMS / in-app), matching S23 exactly.
- Broadcast as an inbox kind with filters, mark-all-read and unread counts.
- Badge summary endpoint per the S14 board rules.
- Driving-mode notification suppression (S11).
- SOS context: trip, vehicle, live location, trusted-contact alerting.
- Support ticket attachments.
- User settings: theme, language, privacy toggles, receipts by email, data export request, account deletion request.

Out of scope:

- Number masking — cut by decision D5.
- Push transport itself — the FCM adapter already exists.
- Rating notifications — slice 15 wires its own.

## Source material / references

- `docs/source-assets/comigo-prototype/shared-chat.jsx` — P23 / D36, the one-component two-mode thread, and `CHAT.closesIn`.
- `docs/source-assets/comigo-prototype/shared-support.jsx` — S22 inbox + broadcast, S23 preference matrix, S24 help centre, S25/S26 tickets, S27 SOS, S28 safety centre.
- `docs/source-assets/comigo-prototype/shell-modes.jsx` — S11 queued passenger alerts while driving, S14 tab badge rules.
- `docs/source-assets/comigo-prototype/shared-account.jsx` — S18 settings.
- `docs/source-assets/comigo-prototype/data.jsx` — `CHAT`.
- Current code: `notification/**`, `support/**`, `safety/**`, `passenger/entity/TrustedContactEntity.java`, `location/**`.

## Architecture and design notes

**Chat is scoped, which is the whole safety design.** There is no profile-to-profile messaging, so there
is no directory to mine and no thread to moderate outside a transaction. A thread exists only for a
confirmed booking, opens on confirmation, and closes 24 h after drop-off — enforced by a scheduler job,
not by a client check.

**Transport is polling plus push, not WebSockets.** Message volume per thread is tiny and bursty around
pickup. `GET /threads/{id}/messages?since=` with a push notification on each inbound message gives the
same felt latency without a socket layer, connection state, or a second scaling axis. Revisit only if
usage proves otherwise.

**Support can read a thread, and both parties are told so.** P23's banner says it. So messages are stored
in plaintext, and an admin read is audited.

**The badge rules are specific and asymmetric** (S14): Home and Account are dots only, never counts;
Trips and Inbox are counts; the action tab is never badged. One endpoint returns all of them so the shell
does not derive badges from five list calls.

**Suppression while driving is a delivery rule, not a storage rule** (S11). Passenger-side notifications
raised while the user has a live trip in driver mode are stored, marked `deferred`, delivered without
sound and without an interrupt, and counted into the Inbox badge. Only SOS and trip-critical driver
alerts bypass. The rule lives in `NotificationService.deliver`, keyed off the caller's active trip.

**SOS assembles context at raise time**, not at read time — the trip, vehicle, plate, and the last known
location sample are snapshotted onto the event, because the value of the record is what was true when the
button was pressed.

**Settings are user rows, not app state.** Language and theme are returned by `/me/context` so a
reinstall restores them.

## API contracts involved

Chat:

```
GET  /api/v1/bookings/{bookingId}/chat            -> thread meta: state, closesAt, participants, quickReplies
GET  /api/v1/bookings/{bookingId}/chat/messages?since=&limit=
POST /api/v1/bookings/{bookingId}/chat/messages   { body }   (Idempotency-Key)
POST /api/v1/bookings/{bookingId}/chat/read       { upToMessageId }
```

Notifications:

```
GET  /api/v1/notifications?filter=ALL|TRIPS|MONEY|ACCOUNT&page=&size=
POST /api/v1/notifications/read-all
GET  /api/v1/notification-preferences              -> category × channel matrix
PUT  /api/v1/notification-preferences              -> { categories: [{key, enabled, channels[]}] }
GET  /api/v1/badges                                -> { home: bool, trips: int, inbox: int, account: bool }
```

The existing `/passenger/notifications` and `/driver/notifications` pairs collapse into these unified
paths; the old ones are marked deprecated in the contract for one release.

Safety and support:

```
POST /api/v1/sos-events                { kind, note? }   -> snapshots context, alerts trusted contacts
GET  /api/v1/sos-events/{id}
POST /api/v1/support/tickets/{id}/attachments/upload-url
POST /api/v1/support/tickets/{id}/attachments/{attachmentId}/submit
```

Settings:

```
GET  /api/v1/me/settings
PUT  /api/v1/me/settings   { theme, language, shareLiveLocation, showRatingPublicly, receiptsByEmail }
POST /api/v1/me/data-export        -> queued request
POST /api/v1/me/deletion-request   -> queued request, states the 7-year receipt retention
```

New errors: `CHAT_CLOSED`, `CHAT_NOT_AVAILABLE`, `ATTACHMENT_TOO_LARGE`, `ATTACHMENT_TYPE_NOT_ALLOWED`.

## Database / migration changes

**`V036__chat_notifications_safety_support.sql`**

- New `chat.chat_thread`:
  `id`, `booking_id FK UNIQUE`, `state TEXT CHECK (state IN ('OPEN','CLOSED'))`,
  `opened_at`, `closes_at`, `closed_at`.
- New `chat.chat_message`:
  `id`, `thread_id FK`, `sender_app_user_id FK`, `body TEXT NOT NULL CHECK (char_length(body) <= 2000)`,
  `sent_at`, `read_by_counterparty_at`, index on `(thread_id, sent_at)`.
- New `chat.chat_admin_read_audit` — `id`, `thread_id FK`, `admin_app_user_id`, `read_at`, `reason`.
- `notification.notification` — add `category TEXT CHECK (category IN ('RIDE','DRIVE','MONEY','ACCOUNT','BROADCAST'))`,
  `deferred BOOLEAN NOT NULL DEFAULT false`, `action_path TEXT`.
- Rewrite `notification.notification_preference` to the matrix:
  `id`, `app_user_id FK`, `category_key TEXT`, `enabled BOOLEAN`,
  `push BOOLEAN`, `sms BOOLEAN`, `in_app BOOLEAN`, `UNIQUE (app_user_id, category_key)`.
  Seeded per S23's twelve rows on first read.
- `safety.sos_event` — add `trip_id FK NULL`, `booking_id FK NULL`, `vehicle_registration TEXT`,
  `snapshot_location geometry(Point,4326)`, `snapshot_place_label TEXT`, `role TEXT CHECK (role IN ('RIDER','DRIVER'))`,
  `contacts_alerted INT DEFAULT 0`.
- New `support.support_attachment`:
  `id`, `ticket_id FK`, `message_id FK NULL`, `object_key TEXT`, `filename TEXT`,
  `content_type TEXT`, `size_bytes BIGINT`, `uploaded_by_app_user_id`, `uploaded_at`.
- New `platform.user_setting`:
  `app_user_id PK FK`, `theme TEXT DEFAULT 'SYSTEM'`, `language TEXT DEFAULT 'en' CHECK (language IN ('en','si','ta'))`,
  `share_live_location BOOLEAN DEFAULT true`, `show_rating_publicly BOOLEAN DEFAULT true`,
  `receipts_by_email BOOLEAN DEFAULT true`, `updated_at`.
- New `platform.account_request`:
  `id`, `app_user_id FK`, `kind TEXT CHECK (kind IN ('DATA_EXPORT','DELETION'))`,
  `status TEXT CHECK (status IN ('QUEUED','IN_PROGRESS','COMPLETED','REJECTED'))`,
  `requested_at`, `completed_at`, `note`.
- Index `idx_chat_thread_closing ON chat.chat_thread(closes_at) WHERE state = 'OPEN'`.

## Configuration / environment changes

- Policy setting `CHAT_CLOSE_HOURS_AFTER_DROPOFF` (24).
- `ROUTESHARE_CHAT_MESSAGE_RATE_LIMIT_PER_MINUTE` (default `20`) via the existing `RedisRateLimiter`.
- `ROUTESHARE_SUPPORT_ATTACHMENT_MAX_BYTES` (default `10485760`), allowed types `image/jpeg,image/png,application/pdf`.
- New scheduler job on slice 05's infrastructure: `chat-auto-close`, hourly.

## UI / UX requirements

Backend slice. The contract must supply:

- P23 / D36 — thread state, close time, counterparty trust summary, mode-appropriate quick replies, and the "support can read this" disclosure.
- S22 / S22b — items with category, broadcast styling, unread flags, filters, and mark-all-read.
- S23 — the twelve rows in three groups, each with its enabled state and its three channel toggles.
- S11 — the deferred flag so the app can render the quiet queued card.
- S14 — all four badges in one call.
- S27 — the assembled context line: role, driver/vehicle/plate, destination, and the place label.
- S28 — trusted contacts with their auto-share flag.
- S18 — every setting, and the deletion copy's retention statement.

## Implementation steps

1. Create the `chat` module; open a thread on `booking.confirmed` (slice 07's event); set `closes_at` on drop-off; register the `chat-auto-close` job.
2. Implement message send with idempotency, rate limiting, length cap, and a push notification to the counterparty carrying the booking reference.
3. Implement `since`-based polling with a stable cursor and read receipts.
4. Add admin thread read with a mandatory reason and an audit row.
5. Migrate notification preferences to the matrix; seed S23's rows lazily per user; enforce that safety and trip-critical categories cannot be disabled (S23: "Safety and trip-critical alerts always arrive").
6. Add `category` and `action_path` to every notification emitted anywhere in the system; sweep existing call sites.
7. Unify the passenger/driver notification controllers into `/api/v1/notifications`; deprecate the old pairs in the contract.
8. Implement the badge endpoint per S14's rules — dots for home/account, counts for trips/inbox, action never badged.
9. Implement driving-mode suppression in `NotificationService.deliver`, exempting SOS and trip-critical driver alerts.
10. Extend SOS raise to snapshot trip, vehicle, plate, role and last location; alert trusted contacts by SMS through the existing `SmsGateway` and count them.
11. Add support attachments on the existing presigned lifecycle, with type and size validation.
12. Add `platform.user_setting` and `platform.account_request`; surface theme and language on `/me/context`.

## Files expected to change

- `apps/api/.../chat/**` — new module.
- `apps/api/.../notification/**` — categories, matrix preferences, unified controllers, badges, suppression.
- `apps/api/.../safety/**` — SOS context snapshot, trusted-contact alerting.
- `apps/api/.../support/**` — attachments.
- `apps/api/.../platform/**` — settings, account requests, `/me/context` additions.
- `apps/api/.../scheduling/**` — `chat-auto-close`.
- `apps/api/src/main/resources/db/migration/V036__chat_notifications_safety_support.sql`.
- `apps/api/src/test/java/**` — chat lifecycle and authorization tests, close-job test, preference matrix tests, badge rule tests, suppression tests, SOS snapshot test, attachment validation tests.
- `docs/api/mobile-app.openapi.json`, `docs/api/admin-web.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/10-chat-notifications-safety-and-support-qa.md`

Maestro: not applicable — no mobile surface in this slice.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='ChatLifecycleIT,ChatAuthorizationTest,ChatAutoCloseJobIT,NotificationMatrixTest,BadgeRulesTest,DrivingSuppressionTest,SosContextSnapshotTest,SupportAttachmentValidationTest' test
```

```bash
bash scripts/simulation/verify-chat-and-notifications.sh
```

The smoke must prove: a thread opens on confirmation and refuses messages before it; it closes exactly 24
hours after drop-off and then refuses both sides; a third party cannot read it; disabling a category stops
push but never stops a safety alert; a passenger notification raised during a live drive arrives deferred.

## Security, privacy, and observability checks

- **Chat authorization is the sharp edge.** Only the two booking participants may read or write. Test: another passenger on the same trip, the driver of a different trip, an admin without the audit path, and a participant after close.
- Message bodies are user content: length-capped, stored as text, never interpolated into HTML or SMS templates without escaping.
- Admin reads require a reason and are audited; surface the audit in the admin UI so the power is visible.
- Trusted-contact SMS reveals a live location to a third party. Only contacts the user configured are alerted, the count is recorded, and the SOS payload never includes the counterparty's phone number.
- Safety categories must be non-disableable; assert by test that a crafted preference payload cannot switch them off.
- Attachments: validate content type by sniffing, not by the client-declared header; store outside the web root in the private bucket; presigned reads only.
- Metrics: `routeshare_chat_messages_total`, `routeshare_chat_threads_open` gauge, `routeshare_notifications_delivered_total{category,channel,deferred}`, `routeshare_sos_events_total{role}`, `routeshare_trusted_contact_alerts_total`.
- Alert on any SOS event that failed to alert a configured trusted contact.

## Done criteria

- [ ] Chat opens on confirmation, closes 24 h after drop-off by job, and is readable only by the two participants plus audited admins.
- [ ] Notification preferences match S23's twelve rows and three channels; safety categories cannot be disabled.
- [ ] Broadcasts appear as an inbox kind with working filters and mark-all-read.
- [ ] Badge endpoint implements S14's rules exactly, including which slots are dots and which are counts.
- [ ] Passenger alerts raised during a live drive are deferred, silent and counted.
- [ ] SOS snapshots trip, vehicle, plate, role and location, and alerts trusted contacts.
- [ ] Support tickets accept validated attachments.
- [ ] Settings persist and surface on `/me/context`; export and deletion requests are queued and visible to admin.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): add booking chat, notification matrix, badges, SOS context and user settings"
```
