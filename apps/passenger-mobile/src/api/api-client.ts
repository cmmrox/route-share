import type { ApiResponse } from './types';

export type ApiClientOptions = {
  baseUrl: string;
  getAccessToken?: () => string | undefined | Promise<string | undefined>;
  timeoutMs?: number;
  fetch?: typeof fetch;
  logger?: Pick<Console, 'debug' | 'warn' | 'error'>;
};

export type RequestOptions = {
  method?: string;
  pathParams?: Record<string, string | number>;
  query?: Record<string, string | number | boolean | undefined>;
  body?: unknown;
  headers?: Record<string, string>;
  auth?: boolean;
  timeoutMs?: number;
  retry?: { attempts?: number; delayMs?: number; retryOnStatuses?: number[] };
};

export class HttpError extends Error {
  constructor(public readonly status: number, message: string, public readonly body?: unknown) {
    super(message);
    this.name = 'HttpError';
  }
}
export class UnauthorizedError extends HttpError { constructor(message = 'Unauthorized', body?: unknown) { super(401, message, body); this.name = 'UnauthorizedError'; } }
export class ForbiddenError extends HttpError { constructor(message = 'Forbidden', body?: unknown) { super(403, message, body); this.name = 'ForbiddenError'; } }
export class ConflictError extends HttpError { constructor(message = 'Conflict', body?: unknown) { super(409, message, body); this.name = 'ConflictError'; } }
export class TimeoutError extends Error { constructor(message = 'Request timed out') { super(message); this.name = 'TimeoutError'; } }
export class MalformedJsonError extends Error { constructor(message = 'Malformed JSON response') { super(message); this.name = 'MalformedJsonError'; } }

const isRecord = (value: unknown): value is Record<string, unknown> => Boolean(value && typeof value === 'object' && !Array.isArray(value));
const isEnvelope = <T>(value: unknown): value is ApiResponse<T> => isRecord(value) && typeof value.success === 'boolean' && ('data' in value || 'error' in value);
const errorMessage = (body: unknown, fallback: string) => {
  if (isRecord(body)) {
    if (typeof body.message === 'string') return body.message;
    if (isRecord(body.error) && typeof body.error.message === 'string') return body.error.message;
  }
  return fallback;
};

export function unwrapApiResponse<T>(payload: unknown): T {
  if (isEnvelope<T>(payload)) {
    if (payload.success) return payload.data as T;
    throw new HttpError(400, errorMessage(payload, 'API response reported failure'), payload);
  }
  return payload as T;
}

const secretKey = /(authorization|access.?token|refresh.?token|id.?token|otp|one.?time.?password|card.?number|pan|phone.?number|mobile)/i;
const coordinateKey = /^(lat|lng|lon|latitude|longitude)$/i;

export function redactLogPayload<T>(payload: T): T {
  const visit = (value: unknown, key = ''): unknown => {
    if (secretKey.test(key)) return '[REDACTED]';
    if (coordinateKey.test(key) && (typeof value === 'number' || typeof value === 'string')) return '[REDACTED_COORDINATE]';
    if (typeof value === 'string') {
      return value
        .replace(/Bearer\s+[A-Za-z0-9._~+\-/]+=*/gi, 'Bearer [REDACTED]')
        .replace(/\b\d{4}[ -]?\d{4}[ -]?\d{4}[ -]?\d{4}\b/g, '[REDACTED_CARD]')
        .replace(/\+?\d[\d\s-]{7,}\d/g, '[REDACTED_NUMBER]');
    }
    if (Array.isArray(value)) return value.map((item) => visit(item));
    if (isRecord(value)) return Object.fromEntries(Object.entries(value).map(([entryKey, entryValue]) => [entryKey, visit(entryValue, entryKey)]));
    return value;
  };
  return visit(payload) as T;
}

export class ApiClient {
  private readonly fetchImpl: typeof fetch;
  private readonly timeoutMs: number;

  constructor(private readonly options: ApiClientOptions) {
    this.fetchImpl = options.fetch ?? fetch;
    this.timeoutMs = options.timeoutMs ?? 15000;
  }

  async request<T = unknown>(pathTemplate: string, options: RequestOptions = {}): Promise<T> {
    const maxAttempts = Math.max(1, options.retry?.attempts ?? 1);
    let lastError: unknown;

    for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
      try {
        return await this.requestOnce<T>(pathTemplate, options, attempt);
      } catch (error) {
        lastError = error;
        if (attempt >= maxAttempts || !this.shouldRetry(error, options.retry?.retryOnStatuses)) throw error;
        await this.delay(options.retry?.delayMs ?? 250);
      }
    }

    throw lastError;
  }

  private async requestOnce<T>(pathTemplate: string, options: RequestOptions, attempt: number): Promise<T> {
    const url = this.buildUrl(pathTemplate, options.pathParams, options.query);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), options.timeoutMs ?? this.timeoutMs);
    const headers: Record<string, string> = { Accept: 'application/json', ...options.headers };

    if (options.body !== undefined) headers['Content-Type'] = 'application/json';
    if (options.auth !== false) {
      const token = await this.options.getAccessToken?.();
      if (token) headers.Authorization = `Bearer ${token}`;
    }

    const init: RequestInit = {
      method: options.method ?? (options.body === undefined ? 'GET' : 'POST'),
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: controller.signal
    };

    this.options.logger?.debug?.('api.request', redactLogPayload({ url, method: init.method, attempt, headers, body: options.body }));

    try {
      const response = await this.fetchImpl(url, init);
      const payload = await this.parseJson(response);
      if (!response.ok) throw this.toHttpError(response.status, payload);
      return unwrapApiResponse<T>(payload);
    } catch (error) {
      if (isRecord(error) && error.name === 'AbortError') throw new TimeoutError();
      throw error;
    } finally {
      clearTimeout(timeout);
    }
  }

  private shouldRetry(error: unknown, retryOnStatuses: number[] = [500, 502, 503, 504]): boolean {
    if (error instanceof TimeoutError || error instanceof TypeError) return true;
    if (error instanceof HttpError) return retryOnStatuses.includes(error.status);
    return false;
  }

  private delay(delayMs: number): Promise<void> {
    if (delayMs <= 0) return Promise.resolve();
    return new Promise((resolve) => setTimeout(resolve, delayMs));
  }

  private buildUrl(pathTemplate: string, pathParams: RequestOptions['pathParams'], query: RequestOptions['query']) {
    const path = Object.entries(pathParams ?? {}).reduce((current, [key, value]) => current.replace(`{${key}}`, encodeURIComponent(String(value))), pathTemplate);
    const url = new URL(path, this.options.baseUrl.endsWith('/') ? this.options.baseUrl : `${this.options.baseUrl}/`);
    for (const [key, value] of Object.entries(query ?? {})) if (value !== undefined) url.searchParams.set(key, String(value));
    return url.toString();
  }

  private async parseJson(response: Response): Promise<unknown> {
    if (response.status === 204) return undefined;
    const text = await response.text();
    if (!text) return undefined;
    try { return JSON.parse(text); } catch { throw new MalformedJsonError(); }
  }

  private toHttpError(status: number, body: unknown): HttpError {
    if (status === 401) return new UnauthorizedError(errorMessage(body, 'Unauthorized'), body);
    if (status === 403) return new ForbiddenError(errorMessage(body, 'Forbidden'), body);
    if (status === 409) return new ConflictError(errorMessage(body, 'Conflict'), body);
    return new HttpError(status, errorMessage(body, `HTTP ${status}`), body);
  }
}
