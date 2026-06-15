import { create } from 'zustand';

import type { EnvironmentName } from './environment';

export type HomeVariant = 'map' | 'dashboard';
export type ThemePreference = 'system' | 'light' | 'dark';
export type DevEnvironmentPreference = Exclude<EnvironmentName, 'production'>;

export interface PassengerPreferences {
  readonly onboardingComplete: boolean;
  readonly homeVariant: HomeVariant;
  readonly themePreference: ThemePreference;
  readonly devEnvironmentName?: DevEnvironmentPreference;
}

export const DEFAULT_PASSENGER_PREFERENCES: PassengerPreferences = {
  onboardingComplete: false,
  homeVariant: 'map',
  themePreference: 'system',
  devEnvironmentName: undefined,
};

const HOME_VARIANTS = new Set<HomeVariant>(['map', 'dashboard']);
const THEME_PREFERENCES = new Set<ThemePreference>(['system', 'light', 'dark']);
const DEV_ENVIRONMENTS = new Set<DevEnvironmentPreference>(['local', 'simulator', 'device', 'staging']);

const isRecord = (value: unknown): value is Record<string, unknown> => Boolean(value && typeof value === 'object' && !Array.isArray(value));

export function mergeStoredPreferences(stored: unknown): PassengerPreferences {
  if (!isRecord(stored)) return DEFAULT_PASSENGER_PREFERENCES;
  const onboardingComplete = typeof stored.onboardingComplete === 'boolean' ? stored.onboardingComplete : DEFAULT_PASSENGER_PREFERENCES.onboardingComplete;
  const homeVariant = HOME_VARIANTS.has(stored.homeVariant as HomeVariant) ? (stored.homeVariant as HomeVariant) : DEFAULT_PASSENGER_PREFERENCES.homeVariant;
  const themePreference = THEME_PREFERENCES.has(stored.themePreference as ThemePreference) ? (stored.themePreference as ThemePreference) : DEFAULT_PASSENGER_PREFERENCES.themePreference;
  const devEnvironmentName = DEV_ENVIRONMENTS.has(stored.devEnvironmentName as DevEnvironmentPreference) ? (stored.devEnvironmentName as DevEnvironmentPreference) : undefined;
  return { onboardingComplete, homeVariant, themePreference, devEnvironmentName };
}

export interface PassengerPreferencesState extends PassengerPreferences {
  readonly setOnboardingComplete: (onboardingComplete: boolean) => void;
  readonly setHomeVariant: (homeVariant: HomeVariant) => void;
  readonly setThemePreference: (themePreference: ThemePreference) => void;
  readonly setDevEnvironmentName: (devEnvironmentName: DevEnvironmentPreference | undefined) => void;
  readonly hydratePreferences: (stored: unknown) => void;
}

export const usePassengerPreferencesStore = create<PassengerPreferencesState>((set) => ({
  ...DEFAULT_PASSENGER_PREFERENCES,
  setOnboardingComplete: (onboardingComplete) => set({ onboardingComplete }),
  setHomeVariant: (homeVariant) => set({ homeVariant }),
  setThemePreference: (themePreference) => set({ themePreference }),
  setDevEnvironmentName: (devEnvironmentName) => set({ devEnvironmentName }),
  hydratePreferences: (stored) => set(mergeStoredPreferences(stored)),
}));
