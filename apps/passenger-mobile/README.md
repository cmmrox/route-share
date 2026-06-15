# RouteShare Passenger App

Expo React Native app for passengers: search routes, compare full/partial matches, book seats, pay, track trips, receive receipts, use SOS/support, and rate drivers.

## Dev commands

```bash
pnpm --filter @routeshare/passenger-mobile start
pnpm --filter @routeshare/passenger-mobile start:web
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile run doctor
```

## Environment profiles

- `local`: Mac backend at `http://localhost:8080`.
- `simulator`: simulator loopback at `http://10.0.2.2:8080`.
- `device`: physical device via Tailscale at `http://100.93.64.101:8080`.
- `staging`: `https://staging-api.routeshare.app`.
- `production`: `https://api.routeshare.app`, debug tools disabled.

Set `EXPO_PUBLIC_APP_ENV`, `EXPO_PUBLIC_API_BASE_URL`, and `EXPO_PUBLIC_SENTRY_DSN` to override defaults.
