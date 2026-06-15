import { describe, expect, it } from 'vitest';

import { ENVIRONMENT_PROFILES, getAppEnvironment, getEnvironmentProfile } from '../environment';

describe('passenger mobile environment profiles', () => {
  it('defines release-safe profiles for local, simulator, device, staging, and production', () => {
    expect(Object.keys(ENVIRONMENT_PROFILES).sort()).toEqual([
      'device',
      'local',
      'production',
      'simulator',
      'staging',
    ]);
    expect(ENVIRONMENT_PROFILES.production.apiBaseUrl).toBe('https://api.routeshare.app');
    expect(ENVIRONMENT_PROFILES.production.enableDebugTools).toBe(false);
    expect(ENVIRONMENT_PROFILES.local.apiBaseUrl).toContain('localhost');
    expect(ENVIRONMENT_PROFILES.device.apiBaseUrl).toContain('100.');
  });

  it('selects explicit profile names and defaults to local for unknown values', () => {
    expect(getEnvironmentProfile('staging').name).toBe('staging');
    expect(getEnvironmentProfile('unexpected').name).toBe('local');
  });

  it('allows env var overrides without enabling production debug tools', () => {
    const env = getAppEnvironment({
      EXPO_PUBLIC_APP_ENV: 'production',
      EXPO_PUBLIC_API_BASE_URL: 'https://override.example',
      EXPO_PUBLIC_SENTRY_DSN: 'https://dsn.example',
    });

    expect(env.apiBaseUrl).toBe('https://override.example');
    expect(env.sentryDsn).toBe('https://dsn.example');
    expect(env.enableDebugTools).toBe(false);
  });
});
