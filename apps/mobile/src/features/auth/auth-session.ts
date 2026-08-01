export interface KeycloakProviderConfig {
  readonly issuer: string;
  readonly clientId: string;
  readonly redirectUri: string;
  readonly scope?: string;
}

export interface AuthTokens {
  readonly accessToken: string;
  readonly refreshToken?: string;
  readonly idToken?: string;
  readonly tokenType: string;
  readonly accessTokenExpiresAt: number;
  readonly refreshTokenExpiresAt?: number;
}

interface TokenPayload {
  readonly access_token?: unknown;
  readonly refresh_token?: unknown;
  readonly id_token?: unknown;
  readonly token_type?: unknown;
  readonly expires_in?: unknown;
  readonly refresh_expires_in?: unknown;
}

const tokenEndpoint = (issuer: string) => `${issuer.replace(/\/$/, '')}/protocol/openid-connect/token`;
const authEndpoint = (issuer: string) => `${issuer.replace(/\/$/, '')}/protocol/openid-connect/auth`;

export function buildKeycloakAuthorizationUrl(options: KeycloakProviderConfig & { readonly codeChallenge: string; readonly state: string; readonly scope?: string }): string {
  const url = new URL(authEndpoint(options.issuer));
  url.searchParams.set('client_id', options.clientId);
  url.searchParams.set('redirect_uri', options.redirectUri);
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('scope', options.scope ?? 'openid profile email offline_access');
  url.searchParams.set('code_challenge', options.codeChallenge);
  url.searchParams.set('code_challenge_method', 'S256');
  url.searchParams.set('state', options.state);
  return url.toString();
}

export function toAuthTokens(payload: TokenPayload, now = Date.now()): AuthTokens {
  if (typeof payload.access_token !== 'string') throw new Error('Missing access token');
  const expiresIn = typeof payload.expires_in === 'number' ? payload.expires_in : 300;
  const refreshExpiresIn = typeof payload.refresh_expires_in === 'number' ? payload.refresh_expires_in : undefined;
  return {
    accessToken: payload.access_token,
    refreshToken: typeof payload.refresh_token === 'string' ? payload.refresh_token : undefined,
    idToken: typeof payload.id_token === 'string' ? payload.id_token : undefined,
    tokenType: typeof payload.token_type === 'string' ? payload.token_type : 'Bearer',
    accessTokenExpiresAt: now + expiresIn * 1000,
    refreshTokenExpiresAt: refreshExpiresIn === undefined ? undefined : now + refreshExpiresIn * 1000,
  };
}

async function postTokenForm(options: { readonly issuer: string; readonly body: URLSearchParams; readonly fetch?: typeof fetch; readonly now?: number }): Promise<AuthTokens> {
  const fetchImpl = options.fetch ?? fetch;
  const response = await fetchImpl(tokenEndpoint(options.issuer), { method: 'POST', headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded' }, body: options.body.toString() });
  let payload: unknown;
  try { payload = await response.json(); } catch { throw new Error('Authentication failed. Please try again.'); }
  if (!response.ok) throw new Error(sanitizeAuthError(payload));
  return toAuthTokens(payload as TokenPayload, options.now ?? Date.now());
}

export function exchangeAuthorizationCode(options: KeycloakProviderConfig & { readonly code: string; readonly codeVerifier: string; readonly fetch?: typeof fetch; readonly now?: number }): Promise<AuthTokens> {
  const body = new URLSearchParams({ grant_type: 'authorization_code', client_id: options.clientId, redirect_uri: options.redirectUri, code: options.code, code_verifier: options.codeVerifier });
  return postTokenForm({ issuer: options.issuer, body, fetch: options.fetch, now: options.now });
}

export function refreshAccessToken(options: Pick<KeycloakProviderConfig, 'issuer' | 'clientId'> & { readonly refreshToken: string; readonly fetch?: typeof fetch; readonly now?: number }): Promise<AuthTokens> {
  const body = new URLSearchParams({ grant_type: 'refresh_token', client_id: options.clientId, refresh_token: options.refreshToken });
  return postTokenForm({ issuer: options.issuer, body, fetch: options.fetch, now: options.now });
}

export function sanitizeAuthError(error: unknown): string {
  if (error instanceof Error && error.message && !/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]|\+?\d[\d\s-]{7,}\d|\b\d{4,8}\b/.test(error.message)) return error.message;
  return 'Authentication failed. Please try again.';
}
