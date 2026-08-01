import { describe, expect, it } from 'vitest';

import { createOfflineAwareQueryOptions, canRunMutation } from '../network-policy';
import { DEFAULT_PASSENGER_PREFERENCES, mergeStoredPreferences } from '../preferences';

describe('offline-aware app foundation', () => {
  it('persists release-safe preference defaults and merges stored values defensively', () => {
    expect(DEFAULT_PASSENGER_PREFERENCES).toEqual({ onboardingComplete: false, homeVariant: 'map', themePreference: 'system', devEnvironmentName: undefined });
    expect(mergeStoredPreferences({ onboardingComplete: true, homeVariant: 'dashboard', themePreference: 'dark', devEnvironmentName: 'simulator' })).toEqual({ onboardingComplete: true, homeVariant: 'dashboard', themePreference: 'dark', devEnvironmentName: 'simulator' });
    expect(mergeStoredPreferences({ onboardingComplete: true, homeVariant: 'unsafe', themePreference: 'neon', devEnvironmentName: 'production' })).toEqual({ ...DEFAULT_PASSENGER_PREFERENCES, onboardingComplete: true });
  });

  it('disables unsafe mutations while offline but allows cached reads', () => {
    expect(canRunMutation({ online: false, mutationSafety: 'unsafe' })).toBe(false);
    expect(canRunMutation({ online: false, mutationSafety: 'safe' })).toBe(true);
    expect(canRunMutation({ online: true, mutationSafety: 'unsafe' })).toBe(true);
  });

  it('uses conservative retry and cache rules for mobile startup', () => {
    expect(createOfflineAwareQueryOptions(false)).toMatchObject({ networkMode: 'offlineFirst', retry: 0, staleTime: 300000 });
    expect(createOfflineAwareQueryOptions(true)).toMatchObject({ networkMode: 'online', retry: 1, staleTime: 30000 });
  });
});
