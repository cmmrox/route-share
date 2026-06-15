# Passenger Mobile App Release Readiness Checklist

## Build artifacts
- [ ] iOS preview build URL/path:
- [ ] Android preview build URL/path:
- [ ] iOS production candidate build number:
- [ ] Android production candidate versionCode:

## Smoke evidence
- [ ] First install → onboarding → auth → profile.
- [ ] Saved place and trusted contact creation.
- [ ] Ride search → results list/map → detail.
- [ ] Seat selection → idempotent booking.
- [ ] Payment/cash flow.
- [ ] Booked waiting → live trip resume.
- [ ] Share trip.
- [ ] SOS on physical device or documented limitation.
- [ ] Early drop-off.
- [ ] Receipt → rating.
- [ ] Notifications.
- [ ] Support ticket/message.
- [ ] Sign out.

## Privacy/security
- [ ] No secrets in bundle.
- [ ] Tokens only in SecureStore/keychain-backed storage.
- [ ] No OTP/token/full card/precise location leaks in logs/crash reports.

## Accessibility
- [ ] Screen reader core booking flow.
- [ ] Screen reader SOS flow.
- [ ] Touch targets >= 44px.
- [ ] Dynamic text and contrast pass.
