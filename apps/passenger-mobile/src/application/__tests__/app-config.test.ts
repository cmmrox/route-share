import { describe, expect, it } from 'vitest';

import appConfig from '../../app.config';

describe('Expo app config', () => {
  const config = appConfig({ config: {} });

  it('configures production app identity, scheme, bundle IDs, and permissions', () => {
    expect(config.name).toBe('RouteShare Passenger');
    expect(config.slug).toBe('routeshare-passenger');
    expect(config.scheme).toBe('routeshare');
    expect(config.ios?.bundleIdentifier).toBe('app.routeshare.passenger');
    expect(config.android?.package).toBe('app.routeshare.passenger');
    expect(config.ios?.infoPlist?.NSLocationWhenInUseUsageDescription).toContain('nearby shared rides');
    expect(config.android?.permissions).toEqual(
      expect.arrayContaining(['ACCESS_COARSE_LOCATION', 'ACCESS_FINE_LOCATION', 'POST_NOTIFICATIONS']),
    );
  });
});
