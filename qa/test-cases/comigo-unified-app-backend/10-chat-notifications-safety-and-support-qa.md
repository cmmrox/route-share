# QA — Task 10: Chat, Notifications, Safety and Support

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/10-chat-notifications-safety-and-support.md`

## Scope

Booking-scoped chat with its open/close window, the notification category × channel matrix,
broadcasts and badges, driving-mode suppression, SOS context and trusted-contact alerting, support
attachments, and user settings.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V037`.
- At least one confirmed booking with two distinct participants, plus a third unrelated account.
- A user with a live trip in driver mode, for the suppression cases.

## Automated test coverage

- `ChatLifecycleIT` — opens on confirmation, closes 24 h after drop-off.
- `ChatAuthorizationTest` — the full negative matrix including post-close.
- `ChatAutoCloseJobIT` — the hourly job closes exactly the due threads.
- `NotificationMatrixTest` — twelve rows, three channels, safety categories non-disableable.
- `BadgeRulesTest` — dots vs counts per the S14 board.
- `DrivingSuppressionTest` — passenger alerts deferred during a live drive; SOS exempt.
- `SosContextSnapshotTest` — trip, vehicle, plate, role and location captured at raise time.
- `SupportAttachmentValidationTest` — type sniffing and size limits.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 10-1 | Message before confirmation | `CHAT_NOT_AVAILABLE` |
| 10-2 | Booking confirmed | Thread opens; both participants can post |
| 10-3 | Third party reads the thread | 403 |
| 10-4 | Another passenger on the same trip reads it | 403 |
| 10-5 | 23 h after drop-off | Still open |
| 10-6 | 25 h after drop-off | `CHAT_CLOSED` for both sides |
| 10-7 | Admin reads without a reason | Refused |
| 10-8 | Admin reads with a reason | Allowed; audit row written |
| 10-9 | Message over 2000 characters | 400 |
| 10-10 | 25 messages in a minute | Rate limited |
| 10-11 | Duplicate send with the same idempotency key | One message stored |
| 10-12 | Disable the "Offers and news" category | No push sent for it |
| 10-13 | Attempt to disable a safety category | Refused; alert still delivered |
| 10-14 | Broadcast raised by admin | Appears in the inbox as kind `BROADCAST` |
| 10-15 | Inbox filter `MONEY` | Only money-category items |
| 10-16 | Mark all read | Unread count zero; badge updates |
| 10-17 | Badge payload | Home and account are booleans; trips and inbox are integers; no action badge |
| 10-18 | Passenger alert raised during a live drive | Stored, `deferred=true`, no interrupt, counted in the inbox badge |
| 10-19 | SOS raised during the same live drive | Delivered immediately, not deferred |
| 10-20 | SOS raised as a rider | Snapshot carries driver, vehicle, plate, destination, place label |
| 10-21 | SOS with two trusted contacts | Both alerted by SMS; `contactsAlerted=2` |
| 10-22 | SOS payload | Contains no counterparty phone number |
| 10-23 | Attachment renamed `.pdf` but actually an executable | Refused by content sniffing |
| 10-24 | Attachment over the size limit | `ATTACHMENT_TOO_LARGE` |
| 10-25 | Settings saved then re-read | Theme and language persist and appear on `/me/context` |
| 10-26 | Deletion request | Queued; response states the 7-year receipt retention |

## Manual checks

- Read a closed thread as each participant and confirm the history is still visible but posting is refused.
- Confirm the admin chat-read audit is surfaced in the admin UI, not only in the database.
- Trigger an SOS with a trusted contact whose SMS delivery fails; confirm the failure alert fires.

## Evidence to collect

- `scripts/simulation/verify-chat-and-notifications.sh` output.
- `chat.chat_admin_read_audit` extract.
- SMS gateway log for the trusted-contact alert (numbers redacted).

## Pass/fail criteria

Pass when: chat is reachable only by the two participants within its window; safety categories cannot be
disabled; badges follow S14 exactly; passenger alerts defer during a live drive while SOS does not; and
SOS carries full context and alerts configured contacts.

## Execution evidence — 2026-08-03

- Focused test command: all eight named Slice 10 classes passed.
- Full backend gate: `spotless:apply spotless:check verify` passed 605 tests with JaCoCo at 84%.
- Runtime: `verify-chat-and-notifications.sh` passed 33, failed 0, skipped 0 against API 8088,
  PostgreSQL 5434 and fresh `routeshare_slice10_final` schema V037.
- Local Notify.lk was intentionally disabled: the runtime proved delivery-failure accounting and
  alerting; `SosContextSnapshotTest` proved two configured contacts are sent through `SmsGateway`.

Fail on: any third-party chat access, any disableable safety alert, an SOS that interrupts nothing or
alerts nobody, or an attachment accepted on a client-declared content type.
