import * as Sentry from '@sentry/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useMemo, type PropsWithChildren } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { ApiClient } from '../api/api-client';
import { createPassengerApi } from '../api';
import { getAppEnvironment } from './environment';
import { PassengerErrorBoundary } from './error-boundary';
import { createOfflineAwareQueryOptions } from './network-state';
import { ToastProvider } from './toast';
import { useAuthStore } from './auth-store';

let sentryInitialized = false;

function initializeSentry(): void {
  const env = getAppEnvironment();
  if (!env.sentryDsn || sentryInitialized) return;

  Sentry.init({
    dsn: env.sentryDsn,
    environment: env.name,
    enableNative: true,
    tracesSampleRate: env.name === 'production' ? 0.1 : 1,
  });
  sentryInitialized = true;
}

export function createPassengerQueryClient(online = true): QueryClient {
  const queryOptions = createOfflineAwareQueryOptions(online);
  return new QueryClient({
    defaultOptions: {
      queries: queryOptions,
      mutations: {
        retry: false,
        networkMode: online ? 'online' : 'offlineFirst',
      },
    },
  });
}

export function createPassengerRuntimeApi() {
  const env = getAppEnvironment();
  return createPassengerApi(
    new ApiClient({
      baseUrl: env.apiBaseUrl,
      getAccessToken: () => useAuthStore.getState().accessToken,
      timeoutMs: 15000,
      logger: env.enableDebugTools ? console : undefined,
    }),
  );
}

export function PassengerAppProviders({ children }: PropsWithChildren) {
  initializeSentry();
  const queryClient = useMemo(() => createPassengerQueryClient(true), []);

  return (
    <PassengerErrorBoundary>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <ToastProvider>{children}</ToastProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </PassengerErrorBoundary>
  );
}
