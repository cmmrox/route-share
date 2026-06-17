import type { LinkingOptions } from '@react-navigation/native';

import type { Coordinate, RideSearchResult } from '../api/types';

export type PassengerPublicRouteName = 'Splash' | 'Onboarding' | 'Login' | 'Otp';
export type PassengerProtectedRouteName =
  | 'ProfileSetup'
  | 'Home'
  | 'Search'
  | 'SearchResults'
  | 'RideDetail'
  | 'SeatSelection'
  | 'Payment'
  | 'BookedWaiting'
  | 'InTrip'
  | 'ExitEarly'
  | 'Receipt'
  | 'RateDriver'
  | 'TripHistory'
  | 'SavedPlaces'
  | 'TrustedContacts'
  | 'Verification'
  | 'Account'
  | 'Safety'
  | 'ShareTrip'
  | 'Notifications'
  | 'Support';

export type PassengerRouteName = PassengerPublicRouteName | PassengerProtectedRouteName;

export type PassengerRootStackParamList = {
  Splash: undefined;
  Onboarding: undefined;
  Login: { redirectTo?: PassengerProtectedRouteName } | undefined;
  Otp: { phoneNumber?: string; verificationId?: string; redirectTo?: PassengerProtectedRouteName } | undefined;
  ProfileSetup: undefined;
  Home: undefined;
  Search: { pickup?: Coordinate; dropoff?: Coordinate } | undefined;
  SearchResults: { searchId: string; pickup?: Coordinate; dropoff?: Coordinate; results?: RideSearchResult[] };
  RideDetail: { searchId: string; resultId: string };
  SeatSelection: { searchId: string; resultId: string };
  Payment: { bookingId: string; paymentIntentId?: string };
  BookedWaiting: { bookingId: string };
  InTrip: { tripId: string; bookingId: string };
  ExitEarly: { tripId: string; bookingId: string };
  Receipt: { bookingId: string };
  RateDriver: { bookingId: string; tripId?: string };
  TripHistory: undefined;
  SavedPlaces: undefined;
  TrustedContacts: undefined;
  Verification: undefined;
  Account: undefined;
  Safety: { tripId?: string; bookingId?: string } | undefined;
  ShareTrip: { shareLinkId?: string; tripId?: string } | undefined;
  Notifications: undefined;
  Support: { ticketId?: string } | undefined;
};

export const PUBLIC_ROUTE_NAMES = ['Splash', 'Onboarding', 'Login', 'Otp'] as const satisfies readonly PassengerPublicRouteName[];
export const PROTECTED_ROUTE_NAMES = [
  'ProfileSetup',
  'Home',
  'Search',
  'SearchResults',
  'RideDetail',
  'SeatSelection',
  'Payment',
  'BookedWaiting',
  'InTrip',
  'ExitEarly',
  'Receipt',
  'RateDriver',
  'TripHistory',
  'SavedPlaces',
  'TrustedContacts',
  'Verification',
  'Account',
  'Safety',
  'ShareTrip',
  'Notifications',
  'Support',
] as const satisfies readonly PassengerProtectedRouteName[];

export const passengerRouteNames = [...PUBLIC_ROUTE_NAMES, ...PROTECTED_ROUTE_NAMES] as const satisfies readonly PassengerRouteName[];
export const PASSENGER_LINKING_PREFIXES = ['routeshare://', 'https://routeshare.app'] as const;

export function isProtectedRoute(routeName: PassengerRouteName): routeName is PassengerProtectedRouteName {
  return (PROTECTED_ROUTE_NAMES as readonly string[]).includes(routeName);
}

export const passengerLinking: LinkingOptions<PassengerRootStackParamList> = {
  prefixes: [...PASSENGER_LINKING_PREFIXES],
  config: {
    initialRouteName: 'Splash',
    screens: {
      Splash: 'splash',
      Onboarding: 'onboarding',
      Login: 'auth/login',
      Otp: 'auth/otp',
      ProfileSetup: 'profile/setup',
      Home: 'home',
      Search: 'rides/search',
      SearchResults: 'rides/search/:searchId/results',
      RideDetail: 'rides/search/:searchId/results/:resultId',
      SeatSelection: 'rides/search/:searchId/results/:resultId/seats',
      Payment: 'bookings/:bookingId/payment/:paymentIntentId?',
      BookedWaiting: 'bookings/:bookingId/waiting',
      InTrip: 'trips/:tripId/bookings/:bookingId',
      ExitEarly: 'trips/:tripId/bookings/:bookingId/exit-early',
      Receipt: 'bookings/:bookingId/receipt',
      RateDriver: 'bookings/:bookingId/rate',
      TripHistory: 'trips/history',
      SavedPlaces: 'account/saved-places',
      TrustedContacts: 'account/trusted-contacts',
      Verification: 'account/verification',
      Account: 'account',
      Safety: 'safety',
      ShareTrip: 'share/:shareLinkId?',
      Notifications: 'notifications',
      Support: 'support/:ticketId?',
    },
  },
};
