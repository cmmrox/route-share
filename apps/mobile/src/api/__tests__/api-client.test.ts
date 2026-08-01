import { describe, expect, it, vi } from 'vitest';
import {
  ApiClient,
  ConflictError,
  ForbiddenError,
  HttpError,
  MalformedJsonError,
  TimeoutError,
  UnauthorizedError,
  unwrapApiResponse,
  redactLogPayload
} from '../api-client';
import { resolvePassengerApiBaseUrl } from '../config';

const jsonResponse = (status: number, body: unknown, headers: Record<string, string> = {}) =>
  new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json', ...headers } });

describe('unwrapApiResponse', () => {
  it('unwraps the backend ApiResponse envelope centrally', () => {
    expect(unwrapApiResponse<{ id: string }>({ success: true, data: { id: 'p1' }, timestamp: 'now' })).toEqual({ id: 'p1' });
  });

  it('passes through plain OpenAPI-compatible payloads when backend omits the envelope', () => {
    expect(unwrapApiResponse({ id: 'plain' })).toEqual({ id: 'plain' });
  });

  it('raises a typed HttpError when an envelope reports failure', () => {
    expect(() => unwrapApiResponse({ success: false, error: { code: 'BAD_REQUEST', message: 'Invalid' } })).toThrow(HttpError);
  });
});


describe('resolvePassengerApiBaseUrl', () => {
  it('uses explicit Expo public API URL and removes trailing slash', () => {
    expect(resolvePassengerApiBaseUrl({ EXPO_PUBLIC_API_BASE_URL: 'https://api.example.test/' })).toBe('https://api.example.test');
  });

  it('falls back to the local backend URL for development', () => {
    expect(resolvePassengerApiBaseUrl({})).toBe('http://localhost:8080');
  });
});

describe('ApiClient', () => {
  it('injects bearer tokens for protected calls and omits them for public calls', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(jsonResponse(200, { success: true, data: { ok: true } })));
    const client = new ApiClient({ baseUrl: 'https://api.example.test', getAccessToken: () => 'secret-token', fetch: fetchMock });

    await client.request('/api/v1/passenger/profile');
    await client.request('/api/v1/app/config', { auth: false });

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer secret-token');
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBeUndefined();
  });

  it.each([
    [401, UnauthorizedError],
    [403, ForbiddenError],
    [409, ConflictError],
    [500, HttpError]
  ])('maps HTTP %s to a typed error', async (status, ErrorType) => {
    const client = new ApiClient({ baseUrl: 'https://api.example.test', fetch: vi.fn().mockResolvedValue(jsonResponse(status, { error: { message: 'Nope' } })) });
    await expect(client.request('/api/v1/passenger/profile')).rejects.toBeInstanceOf(ErrorType);
  });

  it('raises TimeoutError when the request exceeds the configured timeout', async () => {
    const fetchMock = vi.fn((_url: RequestInfo | URL, init?: RequestInit) =>
      new Promise<Response>((_resolve, reject) => init?.signal?.addEventListener('abort', () => reject(Object.assign(new Error('aborted'), { name: 'AbortError' }))))
    );
    const client = new ApiClient({ baseUrl: 'https://api.example.test', fetch: fetchMock, timeoutMs: 1 });

    await expect(client.request('/api/v1/passenger/profile')).rejects.toBeInstanceOf(TimeoutError);
  });

  it('retries transient network failures when retry attempts are configured', async () => {
    const fetchMock = vi
      .fn()
      .mockRejectedValueOnce(new TypeError('Network request failed'))
      .mockResolvedValueOnce(jsonResponse(200, { success: true, data: { ok: true } }));
    const client = new ApiClient({ baseUrl: 'https://api.example.test', fetch: fetchMock });

    await expect(client.request('/api/v1/passenger/profile', { retry: { attempts: 2, delayMs: 0 } })).resolves.toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('raises MalformedJsonError for invalid JSON responses', async () => {
    const client = new ApiClient({ baseUrl: 'https://api.example.test', fetch: vi.fn().mockResolvedValue(new Response('{bad', { status: 200, headers: { 'content-type': 'application/json' } })) });
    await expect(client.request('/api/v1/passenger/profile')).rejects.toBeInstanceOf(MalformedJsonError);
  });

  it('redacts card number embedded in ordinary strings', () => {
    const redacted = redactLogPayload({ message: 'charged test card 4242424242424242 for QA' });

    expect(JSON.stringify(redacted)).not.toContain('4242424242424242');
    expect(redacted.message).toContain('[REDACTED_CARD]');
  });

  it('redacts secrets, OTPs, full phone/card data, and precise coordinates in logs', () => {
    const redacted = redactLogPayload({
      Authorization: 'Bearer abc.def.ghi',
      accessToken: 'secret',
      refresh_token: 'secret2',
      otp: '123456',
      phoneNumber: '+94771234567',
      cardNumber: '4242424242424242',
      pickup: { latitude: 6.927079, longitude: 79.861244 }
    });

    expect(JSON.stringify(redacted)).not.toContain('abc.def.ghi');
    expect(JSON.stringify(redacted)).not.toContain('123456');
    expect(JSON.stringify(redacted)).not.toContain('771234567');
    expect(JSON.stringify(redacted)).not.toContain('4242424242424242');
    expect(JSON.stringify(redacted)).not.toContain('6.927079');
    expect(redacted.pickup).toEqual({ latitude: '[REDACTED_COORDINATE]', longitude: '[REDACTED_COORDINATE]' });
  });
});
