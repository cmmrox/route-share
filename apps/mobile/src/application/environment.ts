export type EnvironmentName = 'local' | 'simulator' | 'device' | 'staging' | 'production';

export interface AppEnvironment {
  readonly name: EnvironmentName;
  readonly apiBaseUrl: string;
  readonly sentryDsn?: string;
  readonly enableDebugTools: boolean;
}

export const ENVIRONMENT_PROFILES: Record<EnvironmentName, AppEnvironment> = {
  local: {
    name: 'local',
    apiBaseUrl: 'http://localhost:8080',
    enableDebugTools: true,
  },
  simulator: {
    name: 'simulator',
    apiBaseUrl: 'http://10.0.2.2:8080',
    enableDebugTools: true,
  },
  device: {
    name: 'device',
    apiBaseUrl: 'http://100.93.64.101:8080',
    enableDebugTools: true,
  },
  staging: {
    name: 'staging',
    apiBaseUrl: 'https://staging-api.routeshare.app',
    enableDebugTools: false,
  },
  production: {
    name: 'production',
    apiBaseUrl: 'https://api.routeshare.app',
    enableDebugTools: false,
  },
};

function isEnvironmentName(value: string | undefined): value is EnvironmentName {
  return value === 'local' || value === 'simulator' || value === 'device' || value === 'staging' || value === 'production';
}

export function getEnvironmentProfile(profileName: string | undefined): AppEnvironment {
  if (!isEnvironmentName(profileName)) return ENVIRONMENT_PROFILES.local;
  return ENVIRONMENT_PROFILES[profileName];
}

export function getAppEnvironment(env: Partial<Record<string, string | undefined>> = process.env): AppEnvironment {
  const profile = getEnvironmentProfile(env.EXPO_PUBLIC_APP_ENV);
  const apiBaseUrl = env.EXPO_PUBLIC_API_BASE_URL ?? profile.apiBaseUrl;
  const sentryDsn = env.EXPO_PUBLIC_SENTRY_DSN ?? profile.sentryDsn;
  return {
    ...profile,
    apiBaseUrl,
    sentryDsn,
    enableDebugTools: profile.name === 'production' ? false : profile.enableDebugTools,
  };
}
