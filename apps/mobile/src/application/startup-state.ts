import type { PassengerRouteName } from './navigation';

export type AppConfigStatus = 'loading' | 'ready' | 'failed';
export type TokenStatus = 'unknown' | 'missing' | 'present' | 'expired';
export type AuthMeStatus = 'unknown' | 'accepted' | 'rejected';

export interface StartupRouteInput {
  readonly appConfig: AppConfigStatus;
  readonly onboardingComplete: boolean;
  readonly token: TokenStatus;
  readonly authMe: AuthMeStatus;
  readonly profileComplete: boolean;
  readonly online: boolean;
}

export function resolveStartupRoute(input: StartupRouteInput): PassengerRouteName {
  if (input.appConfig === 'loading') return 'Splash';
  if (!input.onboardingComplete) return 'Onboarding';
  if (input.token === 'missing' || input.token === 'expired') return 'Login';
  if (!input.online && input.token === 'present') return input.profileComplete ? 'Home' : 'ProfileSetup';
  if (input.authMe === 'unknown') return 'Splash';
  if (input.authMe === 'rejected') return 'Login';
  if (!input.profileComplete) return 'ProfileSetup';
  return 'Home';
}
