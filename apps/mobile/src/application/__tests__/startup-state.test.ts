import { describe, expect, it } from 'vitest';

import { resolveStartupRoute } from '../startup-state';

describe('passenger startup route guard', () => {
  it('keeps the splash screen while app configuration is still loading', () => {
    expect(resolveStartupRoute({ appConfig: 'loading', onboardingComplete: false, token: 'missing', authMe: 'unknown', profileComplete: false, online: true })).toBe('Splash');
  });

  it('sends fresh installs to onboarding before auth', () => {
    expect(resolveStartupRoute({ appConfig: 'ready', onboardingComplete: false, token: 'missing', authMe: 'unknown', profileComplete: false, online: true })).toBe('Onboarding');
  });

  it('routes users with no usable token to login after onboarding', () => {
    expect(resolveStartupRoute({ appConfig: 'ready', onboardingComplete: true, token: 'missing', authMe: 'unknown', profileComplete: false, online: true })).toBe('Login');
    expect(resolveStartupRoute({ appConfig: 'ready', onboardingComplete: true, token: 'expired', authMe: 'unknown', profileComplete: false, online: true })).toBe('Login');
  });

  it('uses cached home safely while offline with a previously valid session', () => {
    expect(resolveStartupRoute({ appConfig: 'ready', onboardingComplete: true, token: 'present', authMe: 'unknown', profileComplete: true, online: false })).toBe('Home');
  });

  it('forces login on rejected auth/me and profile setup for incomplete profiles', () => {
    expect(resolveStartupRoute({ appConfig: 'ready', onboardingComplete: true, token: 'present', authMe: 'rejected', profileComplete: true, online: true })).toBe('Login');
    expect(resolveStartupRoute({ appConfig: 'ready', onboardingComplete: true, token: 'present', authMe: 'accepted', profileComplete: false, online: true })).toBe('ProfileSetup');
  });
});
