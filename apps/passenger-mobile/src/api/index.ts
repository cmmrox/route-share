import type { ApiClient } from './api-client';
import {
  appConfigApi,
  authApi,
  bookingsApi,
  notificationsApi,
  paymentsApi,
  placesApi,
  profileApi,
  rideSearchApi,
  safetyApi,
  savedPlacesApi,
  supportApi,
  tripsApi,
  trustedContactsApi,
} from './modules';

export * from './api-client';
export * from './adapters';
export * from './contracts';
export * from './config';
export * from './types';
export * from './modules';

export const createPassengerApi = (client: ApiClient) => ({
  appConfig: appConfigApi(client),
  auth: authApi(client),
  profile: profileApi(client),
  savedPlaces: savedPlacesApi(client),
  trustedContacts: trustedContactsApi(client),
  rideSearch: rideSearchApi(client),
  bookings: bookingsApi(client),
  payments: paymentsApi(client),
  places: placesApi(client),
  trips: tripsApi(client),
  notifications: notificationsApi(client),
  support: supportApi(client),
  safety: safetyApi(client),
});
