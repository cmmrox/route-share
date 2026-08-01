import { describe, expect, it } from 'vitest';

import appConfig from '../../app.config';

describe('Expo app config', () => {
  const config = appConfig({ config: {} });

  it('presents the app as ComiGo', () => {
    expect(config.name).toBe('ComiGo');
  });

  // Native identity (slug, scheme, bundle identifier, android package) is deliberately
  // unchanged by the unified-app move. Renaming it requires regenerating the committed
  // native projects, which is blocked on the prebuild-vs-native-owned decision in
  // Blocker 009. Tracked as Phase 09 (ComiGo unified mobile app) work; these assertions
  // pin the current values so the change is deliberate when it happens.
  it('keeps the existing native identity until the prebuild decision is made', () => {
    expect(config.slug).toBe('routeshare-passenger');
    expect(config.scheme).toBe('routeshare');
    expect(config.ios?.bundleIdentifier).toBe('app.routeshare.passenger');
    expect(config.android?.package).toBe('app.routeshare.passenger');
  });

  it('declares the permissions and usage strings the product needs', () => {
    expect(config.ios?.infoPlist?.NSLocationWhenInUseUsageDescription).toContain('nearby shared rides');
    expect(config.ios?.infoPlist?.NSCameraUsageDescription).toContain('identity verification');
    expect(config.android?.permissions).toEqual(
      expect.arrayContaining(['ACCESS_COARSE_LOCATION', 'ACCESS_FINE_LOCATION', 'POST_NOTIFICATIONS']),
    );
  });
});
