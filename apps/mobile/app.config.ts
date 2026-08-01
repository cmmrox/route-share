import type { ExpoConfig } from 'expo/config';

type AppEnv = 'local' | 'simulator' | 'device' | 'staging' | 'production';

const DEFAULT_API_URLS: Record<AppEnv, string> = {
  local: 'http://localhost:8080',
  simulator: 'http://10.0.2.2:8080',
  device: 'http://100.93.64.101:8080',
  staging: 'https://staging-api.routeshare.app',
  production: 'https://api.routeshare.app',
};

function resolveAppEnv(value: string | undefined): AppEnv {
  if (value === 'simulator' || value === 'device' || value === 'staging' || value === 'production') {
    return value;
  }
  return 'local';
}

export default function appConfig({ config }: { readonly config: Partial<ExpoConfig> }): ExpoConfig {
  const appEnv = resolveAppEnv(process.env.EXPO_PUBLIC_APP_ENV);
  const apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL ?? DEFAULT_API_URLS[appEnv];

  return {
    ...config,
    name: 'ComiGo',
    slug: 'routeshare-passenger',
    scheme: 'routeshare',
    version: '0.1.0',
    orientation: 'portrait',
    userInterfaceStyle: 'automatic',
    runtimeVersion: { policy: 'appVersion' },
    assetBundlePatterns: ['**/*'],
    ios: {
      supportsTablet: false,
      bundleIdentifier: 'app.routeshare.passenger',
      googleServicesFile: process.env.GOOGLE_SERVICE_INFO_PLIST,
      config: {
        googleMapsApiKey: process.env.EXPO_PUBLIC_GOOGLE_MAPS_IOS_API_KEY,
      },
      infoPlist: {
        NSLocationWhenInUseUsageDescription:
          'ComiGo uses your location to find nearby shared rides, pickup points, and safe trip progress.',
        NSLocationAlwaysAndWhenInUseUsageDescription:
          'ComiGo uses background location only during active trips, for pickup accuracy and safety.',
        NSCameraUsageDescription:
          'ComiGo uses the camera for identity verification, your profile photo, and support attachments.',
        NSPhotoLibraryUsageDescription: 'ComiGo may access selected photos for support attachments.',
        NSPhotoLibraryAddUsageDescription: 'ComiGo may save photos you choose to keep.',
      },
    },
    android: {
      package: 'app.routeshare.passenger',
      googleServicesFile: process.env.GOOGLE_SERVICES_JSON,
      config: {
        googleMaps: {
          apiKey: process.env.EXPO_PUBLIC_GOOGLE_MAPS_ANDROID_API_KEY,
        },
      },
      permissions: [
        'ACCESS_COARSE_LOCATION',
        'ACCESS_FINE_LOCATION',
        'POST_NOTIFICATIONS',
        'CAMERA',
        'READ_MEDIA_IMAGES',
      ],
      adaptiveIcon: {
        backgroundColor: '#0F172A',
      },
    },
    plugins: [
      'expo-secure-store',
      'expo-location',
      'expo-notifications',
      'expo-image-picker',
      'expo-splash-screen',
      [
        '@sentry/react-native/expo',
        {
          organization: 'routeshare',
          project: 'comigo-mobile',
        },
      ],
    ],
    extra: {
      appEnv,
      apiBaseUrl,
      sentryDsn: process.env.EXPO_PUBLIC_SENTRY_DSN,
      googleMapsEnabled: process.env.GOOGLE_MAPS_ENABLED === 'true',
      firebaseProjectId: process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID,
      pushNotificationsEnabled: process.env.PUSH_NOTIFICATIONS_ENABLED === 'true',
      enableDebugTools: appEnv !== 'production',
      eas: {
        projectId: '00000000-0000-0000-0000-000000000000',
      },
    },
    updates: {
      url: 'https://u.expo.dev/00000000-0000-0000-0000-000000000000',
    },
  };
}
