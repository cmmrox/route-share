import { create } from 'zustand';

import type { AuthMeStatus, TokenStatus } from './startup-state';

export interface AuthState {
  readonly status: 'anonymous' | 'authenticated';
  readonly accessToken?: string;
  readonly accessTokenExpiresAt?: number;
  readonly authMeStatus: AuthMeStatus;
  readonly profileComplete: boolean;
  readonly setAccessToken: (token: string, expiresAt?: number) => void;
  readonly setAuthMeAccepted: (profileComplete: boolean) => void;
  readonly setAuthMeRejected: () => void;
  readonly clearSession: () => void;
}

export function resolveTokenStatus(state: Pick<AuthState, 'accessToken' | 'accessTokenExpiresAt'>, now = Date.now()): TokenStatus {
  if (!state.accessToken) return 'missing';
  if (state.accessTokenExpiresAt !== undefined && state.accessTokenExpiresAt <= now) return 'expired';
  return 'present';
}

export const useAuthStore = create<AuthState>((set) => ({
  status: 'anonymous',
  authMeStatus: 'unknown',
  profileComplete: false,
  setAccessToken: (accessToken, accessTokenExpiresAt) => set({ status: 'authenticated', accessToken, accessTokenExpiresAt, authMeStatus: 'unknown' }),
  setAuthMeAccepted: (profileComplete) => set({ status: 'authenticated', authMeStatus: 'accepted', profileComplete }),
  setAuthMeRejected: () => set({ status: 'anonymous', accessToken: undefined, accessTokenExpiresAt: undefined, authMeStatus: 'rejected', profileComplete: false }),
  clearSession: () => set({ status: 'anonymous', accessToken: undefined, accessTokenExpiresAt: undefined, authMeStatus: 'unknown', profileComplete: false }),
}));
