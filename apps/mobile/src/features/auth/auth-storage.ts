import type { QueryClient } from '@tanstack/react-query';

import type { AuthTokens } from './auth-session';

export interface AuthSecureStorage {
  readonly getItemAsync: (key: string) => Promise<string | null>;
  readonly setItemAsync: (key: string, value: string) => Promise<void>;
  readonly deleteItemAsync: (key: string) => Promise<void>;
}

const AUTH_TOKENS_KEY = 'routeshare.passenger.auth.tokens.v1';

export function createAuthMemoryStorage(initial?: Record<string, string>): AuthSecureStorage {
  const values = new Map(Object.entries(initial ?? {}));
  return {
    getItemAsync: async (key) => values.get(key) ?? null,
    setItemAsync: async (key, value) => { values.set(key, value); },
    deleteItemAsync: async (key) => { values.delete(key); },
  };
}

function isAuthTokens(value: unknown): value is AuthTokens {
  return Boolean(value && typeof value === 'object' && !Array.isArray(value) && typeof (value as { accessToken?: unknown }).accessToken === 'string' && typeof (value as { accessTokenExpiresAt?: unknown }).accessTokenExpiresAt === 'number');
}

export async function persistTokens(storage: AuthSecureStorage, tokens: AuthTokens): Promise<void> {
  await storage.setItemAsync(AUTH_TOKENS_KEY, JSON.stringify(tokens));
}

export async function restoreTokens(storage: AuthSecureStorage): Promise<AuthTokens | undefined> {
  const raw = await storage.getItemAsync(AUTH_TOKENS_KEY);
  if (!raw) return undefined;
  try {
    const parsed = JSON.parse(raw) as unknown;
    return isAuthTokens(parsed) ? parsed : undefined;
  } catch {
    await storage.deleteItemAsync(AUTH_TOKENS_KEY);
    return undefined;
  }
}

export async function secureLogout(storage: AuthSecureStorage, queryClient?: Pick<QueryClient, 'clear'>): Promise<void> {
  await storage.deleteItemAsync(AUTH_TOKENS_KEY);
  queryClient?.clear();
}
