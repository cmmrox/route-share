export interface PassengerAuthProviderConfig {
  readonly issuer: string;
  readonly clientId: string;
  readonly redirectScheme: string;
  readonly redirectPath: string;
  readonly googleEnabled: boolean;
  readonly emailEnabled: boolean;
  readonly phoneOtpSupported: boolean;
  readonly dependencyNote?: string;
}

const DEV_ENVS = new Set(['local', 'simulator', 'device']);

function enabled(value: string | undefined, defaultValue: boolean): boolean {
  if (value === undefined) return defaultValue;
  return value === 'true';
}

export function getPassengerAuthProviderConfig(env: Partial<Record<string, string | undefined>> = process.env): PassengerAuthProviderConfig {
  const appEnv = env.EXPO_PUBLIC_APP_ENV ?? 'local';
  const devDefaultPhoneOtp = DEV_ENVS.has(appEnv);
  const phoneOtpSupported = enabled(env.EXPO_PUBLIC_AUTH_PHONE_OTP_SUPPORTED, devDefaultPhoneOtp);
  return {
    issuer: env.EXPO_PUBLIC_KEYCLOAK_ISSUER ?? 'https://id.routeshare.app/realms/routeshare',
    clientId: env.EXPO_PUBLIC_KEYCLOAK_CLIENT_ID ?? 'comigo-mobile',
    redirectScheme: 'routeshare',
    redirectPath: 'auth/callback',
    googleEnabled: enabled(env.EXPO_PUBLIC_AUTH_GOOGLE_ENABLED, false),
    emailEnabled: enabled(env.EXPO_PUBLIC_AUTH_EMAIL_ENABLED, false),
    phoneOtpSupported,
    dependencyNote: phoneOtpSupported ? undefined : 'Phone OTP is disabled for this environment until provider credentials are enabled.',
  };
}
