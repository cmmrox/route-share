# Stage 11 — Keycloak Authentication, Authorization, and User Management Decision

## Final decision

RouteShareApp will use **Keycloak** for authentication, authorization, session management, user management, realm roles, client roles, groups, and token issuance.

The backend modular monolith will not implement its own username/password/OTP identity store. It will validate Keycloak-issued JWTs using Spring Security OAuth2 Resource Server.

## Why Keycloak is required

The same person can be:

- only a passenger,
- only a driver,
- both passenger and driver,
- an admin/support/finance/verification user.

Therefore, RouteShareApp needs one central identity per person, with multiple roles and application profiles. Keycloak is the right place to manage that identity and role assignment.

## Ownership boundary

### Keycloak owns

- User account identity
- Login/authentication
- OTP/social login/federation configuration
- Passwords/credentials if used
- Sessions
- Access/refresh tokens
- Realm roles and client roles
- Groups
- Admin user management

### RouteShare backend owns

- Passenger profile
- Driver profile
- Driver KYC status
- Vehicle records
- Driver approval workflow
- Passenger saved places
- Trusted contacts
- Booking/trip/payment/fare state
- Local suspension/business status
- App-specific audit logs

## Keycloak realm

Realm name:

```text
routeshare
```

## Clients

```text
passenger-mobile   public OIDC client for Passenger App
driver-mobile      public OIDC client for Driver App
admin-web          confidential or public+PKCE client for Admin Web
api-monolith       backend resource server / audience
```

Mobile apps should use Authorization Code Flow with PKCE.

## Roles

Realm roles:

```text
PASSENGER
DRIVER
ADMIN
SUPPORT_AGENT
VERIFICATION_AGENT
FINANCE_ADMIN
OPS_ADMIN
SUPER_ADMIN
```

Important: one Keycloak user may have both `PASSENGER` and `DRIVER`.

## Groups

Suggested groups:

```text
Passengers
Drivers
Admins
Support
Verification
Finance
Operations
```

Groups may assign roles, but backend authorization should check resolved roles in JWT.

## Backend local user mapping

Create `identity.app_user` table:

```text
id
keycloak_subject unique not null
phone
email
display_name
local_status: ACTIVE | SUSPENDED | DELETED
created_at
updated_at
```

Use Keycloak `sub` claim as the stable foreign identity key.

Passenger and driver tables reference local `app_user.id` or `keycloak_subject`. Prefer local `app_user.id` internally while keeping `keycloak_subject` unique and immutable.

## App mode behavior

After login, apps call:

```text
GET /api/v1/auth/me
```

The response tells the frontend which modes are available:

```json
{
  "subject": "keycloak-user-sub",
  "roles": ["PASSENGER", "DRIVER"],
  "hasPassengerProfile": true,
  "hasDriverProfile": true,
  "driverVerificationStatus": "APPROVED",
  "availableModes": ["PASSENGER", "DRIVER"]
}
```

If a passenger applies as a driver, use the same Keycloak account and create a driver profile/KYC application. When approved, assign/confirm the `DRIVER` role in Keycloak.

## Mobile authentication

Use OIDC Authorization Code Flow with PKCE.

Recommended library:

```text
expo-auth-session
```

Token storage:

```text
expo-secure-store
```

## Admin authentication

Admin Web also authenticates through Keycloak. Admin APIs require admin/support/finance/verification roles.

Every admin mutation must create backend audit logs even though authentication is handled by Keycloak.

## Backend security stack

```text
Spring Security
OAuth2 Resource Server
JWT validation using Keycloak JWKS
Custom JWT role converter for realm/client roles
Method-level authorization with @PreAuthorize
```

## Final rule

Do not create separate passenger and driver accounts for the same real person. Create one Keycloak user and attach multiple roles/profiles.
