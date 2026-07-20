#!/usr/bin/env bash
# Shared helpers for RouteShare local QA simulation scripts.
# These scripts target the LOCAL dev stack only (demo OTP enabled); never run against production.

SIM_API_BASE="${ROUTESHARE_API_BASE:-http://localhost:8080}"
SIM_PSQL_CONTAINER="${ROUTESHARE_POSTGRES_CONTAINER:-routeshare-postgres}"
SIM_DB_USER="${ROUTESHARE_DB_USER:-routeshare}"
SIM_DB_NAME="${ROUTESHARE_DB_NAME:-routeshare}"
SIM_DEMO_OTP="${ROUTESHARE_DEMO_OTP:-000000}"

sim_log() { printf '\033[1;36m[sim]\033[0m %s\n' "$*" >&2; }
sim_fail() { printf '\033[1;31m[sim] ERROR:\033[0m %s\n' "$*" >&2; exit 1; }

sim_require_tools() {
  command -v curl >/dev/null || sim_fail "curl is required"
  command -v python3 >/dev/null || sim_fail "python3 is required"
}

sim_require_api() {
  curl -sf "$SIM_API_BASE/actuator/health" >/dev/null \
    || sim_fail "API is not reachable at $SIM_API_BASE (start the backend first)"
}

sim_psql() {
  docker exec -i "$SIM_PSQL_CONTAINER" psql -U "$SIM_DB_USER" -d "$SIM_DB_NAME" -tA -c "$1" \
    || sim_fail "psql failed: $1"
}

sim_json_get() { # sim_json_get '<json>' '<python expression over d>'
  python3 -c "import json,sys; d=json.loads(sys.argv[1]); print(eval(sys.argv[2]))" "$1" "$2"
}

# Authenticates a phone number through the demo-OTP flow and prints the access token.
sim_login() { # sim_login <phoneNumber>
  local phone="$1" response verification_id token
  response="$(curl -sf -X POST "$SIM_API_BASE/api/v1/auth/otp/request" \
    -H 'Content-Type: application/json' \
    -d "{\"phoneNumber\":\"$phone\"}")" || sim_fail "OTP request failed for $phone"
  verification_id="$(sim_json_get "$response" "d['data']['verificationId']")"
  response="$(curl -sf -X POST "$SIM_API_BASE/api/v1/auth/otp/verify" \
    -H 'Content-Type: application/json' \
    -d "{\"verificationId\":\"$verification_id\",\"phoneNumber\":\"$phone\",\"code\":\"$SIM_DEMO_OTP\"}")" \
    || sim_fail "OTP verify failed for $phone (is NOTIFY_LK_ALLOW_DEMO_SENDER_FOR_OTP=true?)"
  token="$(sim_json_get "$response" "d['data']['accessToken']")"
  [ -n "$token" ] || sim_fail "no access token returned for $phone"
  printf '%s' "$token"
}

SIM_KEYCLOAK_BASE="${ROUTESHARE_KEYCLOAK_BASE:-http://localhost:8081}"
SIM_KEYCLOAK_REALM="${ROUTESHARE_KEYCLOAK_REALM:-routeshare}"

sim_env_value() { # sim_env_value <VAR> — from environment, else repo .env
  local from_env="${!1:-}"
  if [ -n "$from_env" ]; then printf '%s' "$from_env"; return; fi
  sed -n "s/^$1=//p" "$(git rev-parse --show-toplevel 2>/dev/null || echo ../..)/.env" 2>/dev/null | head -1
}

sim_keycloak_admin_token() {
  local user pass
  user="$(sim_env_value KEYCLOAK_ADMIN)"; pass="$(sim_env_value KEYCLOAK_ADMIN_PASSWORD)"
  [ -n "$user" ] && [ -n "$pass" ] || sim_fail "KEYCLOAK_ADMIN(_PASSWORD) not set (env or .env)"
  curl -sf -X POST "$SIM_KEYCLOAK_BASE/realms/master/protocol/openid-connect/token" \
    -d "grant_type=password&client_id=admin-cli&username=$user&password=$pass" \
    | python3 -c 'import json,sys;print(json.load(sys.stdin)["access_token"])' \
    || sim_fail "Keycloak admin login failed at $SIM_KEYCLOAK_BASE"
}

# Ensures a local Keycloak user with the given realm roles and prints a password-grant token.
sim_keycloak_login_with_roles() { # <username> <password> <role1,role2> <client>
  local username="$1" password="$2" roles="$3" client="$4" admin_token user_id
  admin_token="$(sim_keycloak_admin_token)"
  # Profile fields are required: without email/first/last the direct grant fails with
  # "Account is not fully set up" under Keycloak's default user profile.
  local user_body="{\"username\":\"$username\",\"enabled\":true,\"emailVerified\":true,
       \"email\":\"$username@routeshare.local\",\"firstName\":\"Sim\",\"lastName\":\"Driver\"}"
  curl -sf -X POST "$SIM_KEYCLOAK_BASE/admin/realms/$SIM_KEYCLOAK_REALM/users" \
    -H "Authorization: Bearer $admin_token" -H 'Content-Type: application/json' \
    -d "$user_body" >/dev/null 2>&1 || true # already exists is fine
  user_id="$(curl -sf -H "Authorization: Bearer $admin_token" \
    "$SIM_KEYCLOAK_BASE/admin/realms/$SIM_KEYCLOAK_REALM/users?username=$username&exact=true" \
    | python3 -c 'import json,sys;u=json.load(sys.stdin);print(u[0]["id"] if u else "")')"
  [ -n "$user_id" ] || sim_fail "could not resolve Keycloak user $username"
  curl -sf -X PUT "$SIM_KEYCLOAK_BASE/admin/realms/$SIM_KEYCLOAK_REALM/users/$user_id" \
    -H "Authorization: Bearer $admin_token" -H 'Content-Type: application/json' \
    -d "$user_body" >/dev/null || sim_fail "failed to update Keycloak user profile"
  curl -sf -X PUT "$SIM_KEYCLOAK_BASE/admin/realms/$SIM_KEYCLOAK_REALM/users/$user_id/reset-password" \
    -H "Authorization: Bearer $admin_token" -H 'Content-Type: application/json' \
    -d "{\"type\":\"password\",\"value\":\"$password\",\"temporary\":false}" >/dev/null \
    || sim_fail "failed to set Keycloak user password"
  local IFS=','
  for role in $roles; do
    local role_json
    role_json="$(curl -sf -H "Authorization: Bearer $admin_token" \
      "$SIM_KEYCLOAK_BASE/admin/realms/$SIM_KEYCLOAK_REALM/roles/$role")" \
      || sim_fail "realm role $role not found"
    curl -sf -X POST "$SIM_KEYCLOAK_BASE/admin/realms/$SIM_KEYCLOAK_REALM/users/$user_id/role-mappings/realm" \
      -H "Authorization: Bearer $admin_token" -H 'Content-Type: application/json' \
      -d "[$role_json]" >/dev/null || sim_fail "failed to assign role $role"
  done
  curl -sf -X POST "$SIM_KEYCLOAK_BASE/realms/$SIM_KEYCLOAK_REALM/protocol/openid-connect/token" \
    -d "grant_type=password&client_id=$client&username=$username&password=$password" \
    | python3 -c 'import json,sys;print(json.load(sys.stdin)["access_token"])' \
    || sim_fail "password-grant login failed for $username"
}

sim_api() { # sim_api <token> <method> <path> [json-body]
  local token="$1" method="$2" path="$3" body="${4:-}"
  if [ -n "$body" ]; then
    curl -sf -X "$method" "$SIM_API_BASE$path" \
      -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$body"
  else
    curl -sf -X "$method" "$SIM_API_BASE$path" -H "Authorization: Bearer $token"
  fi
}
