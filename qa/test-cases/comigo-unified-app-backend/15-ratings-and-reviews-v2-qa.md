# QA — Task 15: Ratings and Reviews v2

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/15-ratings-and-reviews-v2.md`

## Scope

Mutual rating with paired release, the fixed tag vocabularies, one reply per review, star
histograms with derived averages, the passenger aggregate, and the `showRatingPublicly` setting.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V041`.
- A completed trip with one driver and two passengers, and an injected `Clock` for window-close cases.

## Automated test coverage

- `RatingReleaseGateTest` — no disclosure before release, asserted at the repository query level.
- `AverageFromHistogramPropertyTest` — the headline average always reproduces from the five counts.
- `ReviewReplyUniquenessIT` — concurrent replies produce one.
- `RatingTagValidationTest` — tags outside the role's vocabulary refused.
- `RatingVisibilityTest` — `showRatingPublicly=false` hides the score everywhere including search.
- `RatingAggregateReconciliationTest` — `driver_profile` mirrors `rating_aggregate`.
- `RatingReleaseJobIT` — window close releases one-sided ratings.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 15-1 | Passenger rates; driver has not | Passenger cannot see the driver's rating |
| 15-2 | Driver then rates | Both release together |
| 15-3 | Direct query for an unreleased counterpart rating | Returns nothing — enforced in the query |
| 15-4 | Only one side rates; window closes | The submitted rating releases |
| 15-5 | Neither side rates; window closes | Nothing released; no aggregate change |
| 15-6 | Aggregate before release | Unchanged — unreleased ratings never move a public average |
| 15-7 | Duplicate submission | `RATING_ALREADY_SUBMITTED` |
| 15-8 | Idempotent resubmission with the same key | One rating stored |
| 15-9 | Tag outside the rider→driver vocabulary | 400 |
| 15-10 | Driver→rider tags | Only the driver vocabulary accepted |
| 15-11 | Histogram and average | Average reproduces exactly from the five counts |
| 15-12 | Headline average with no ratings | Zero state, not a divide-by-zero |
| 15-13 | Reply to a review about oneself | Accepted |
| 15-14 | Second reply | `REPLY_ALREADY_GIVEN` |
| 15-15 | Concurrent replies | Exactly one stored |
| 15-16 | Third party replies | 403 `REPLY_NOT_PERMITTED` |
| 15-17 | Author label on a review | First name + surname initial only |
| 15-18 | Full name, phone or email in a review payload | Never |
| 15-19 | Passenger aggregate | Present and shown to a driver evaluating a request |
| 15-20 | `showRatingPublicly=false` | No score in profile, booking detail or search results |
| 15-21 | `driver_profile.rating_average` vs `rating_aggregate` | Identical after reconciliation |
| 15-22 | Comment over the length cap | 400 |

## Manual checks

- Attempt to read a counterpart's unreleased rating through every available path — booking detail, review list, user profile, search enrichment.
- Confirm the derived average matches the histogram on screen for a driver with a skewed distribution.
- Confirm the unreleased-past-window gauge returns to zero after the release job runs.

## Evidence to collect

- `scripts/simulation/verify-ratings.sh` output.
- Histogram and derived average for the seeded driver and passenger.
- Reply-uniqueness concurrency test report.

## Pass/fail criteria

Pass when: no path discloses a rating before release; both sides release together or on window close;
the average always reproduces from the histogram; exactly one reply survives a concurrent race; and
`showRatingPublicly` is honoured on every read path.

Fail on: any pre-release disclosure, any average that cannot be derived from its histogram, two replies on
one review, or a hidden rating appearing in a search payload.
