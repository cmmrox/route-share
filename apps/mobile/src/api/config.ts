export interface PassengerApiEnvironment {
  EXPO_PUBLIC_API_BASE_URL?: string;
  ROUTESHARE_API_BASE_URL?: string;
}

const DEFAULT_LOCAL_API_BASE_URL = 'http://localhost:8080';

const readGlobalEnvironment = (): PassengerApiEnvironment => {
  const processLike = (globalThis as { process?: { env?: PassengerApiEnvironment } }).process;
  return processLike?.env ?? {};
};

export const resolvePassengerApiBaseUrl = (
  environment: PassengerApiEnvironment = readGlobalEnvironment()
): string => {
  const configured = environment.EXPO_PUBLIC_API_BASE_URL ?? environment.ROUTESHARE_API_BASE_URL;
  const baseUrl = configured?.trim() || DEFAULT_LOCAL_API_BASE_URL;
  return baseUrl.replace(/\/+$/, '');
};
