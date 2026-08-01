import { describe, expect, it, vi } from 'vitest';

import { buildKeycloakAuthorizationUrl, exchangeAuthorizationCode, refreshAccessToken, sanitizeAuthError, toAuthTokens } from '../auth-session';
import { createAuthMemoryStorage, persistTokens, restoreTokens, secureLogout } from '../auth-storage';

describe('Keycloak PKCE auth session helpers', () => {
  it('builds authorization-code PKCE URLs without leaking secrets', () => {
    const url = buildKeycloakAuthorizationUrl({ issuer: 'https://id.example/realms/routeshare', clientId: 'passenger-mobile', redirectUri: 'routeshare://auth/callback', codeChallenge: 'challenge', state: 'state-1', scope: 'openid profile offline_access' });
    expect(url).toContain('/protocol/openid-connect/auth');
    expect(url).toContain('response_type=code');
    expect(url).toContain('code_challenge_method=S256');
    expect(url).not.toContain('client_secret');
  });

  it('exchanges and refreshes tokens through Keycloak token endpoint', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ access_token: 'access', refresh_token: 'refresh', id_token: 'id', expires_in: 300, refresh_expires_in: 3600, token_type: 'Bearer' }), { status: 200 })));
    const exchanged = await exchangeAuthorizationCode({ issuer: 'https://id.example/realms/routeshare', clientId: 'passenger-mobile', redirectUri: 'routeshare://auth/callback', code: 'code', codeVerifier: 'verifier', fetch: fetchMock, now: 1_000 });
    expect(exchanged.accessTokenExpiresAt).toBe(301_000);
    await refreshAccessToken({ issuer: 'https://id.example/realms/routeshare', clientId: 'passenger-mobile', refreshToken: 'refresh', fetch: fetchMock, now: 2_000 });
    expect(fetchMock.mock.calls[0][0]).toBe('https://id.example/realms/routeshare/protocol/openid-connect/token');
    expect(String(fetchMock.mock.calls[0][1].body)).toContain('grant_type=authorization_code');
    expect(String(fetchMock.mock.calls[1][1].body)).toContain('grant_type=refresh_token');
  });

  it('persists, restores, and securely clears tokens without unsafe logs', async () => {
    const storage = createAuthMemoryStorage();
    const tokens = toAuthTokens({ access_token: 'access-secret', refresh_token: 'refresh-secret', expires_in: 60 }, 10_000);
    await persistTokens(storage, tokens);
    expect(await restoreTokens(storage)).toEqual(tokens);
    await secureLogout(storage, { clear: vi.fn() });
    expect(await restoreTokens(storage)).toBeUndefined();
    expect(sanitizeAuthError({ error_description: 'invalid_grant: code 123456 for +94771234567' })).toBe('Authentication failed. Please try again.');
  });
});
