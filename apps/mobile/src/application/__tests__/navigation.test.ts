import { describe, expect, it } from 'vitest';

import { PASSENGER_LINKING_PREFIXES, PROTECTED_ROUTE_NAMES, PUBLIC_ROUTE_NAMES, passengerRouteNames, type PassengerRootStackParamList } from '../navigation';

describe('passenger navigation contract', () => {
  it('defines all Phase 07 Task 03 app shell routes', () => {
    expect(passengerRouteNames).toEqual([
      'Splash', 'Onboarding', 'Login', 'Otp', 'ProfileSetup', 'Home', 'Search', 'SearchResults', 'RideDetail', 'SeatSelection', 'Payment', 'BookedWaiting', 'InTrip', 'ExitEarly', 'Receipt', 'RateDriver', 'TripHistory', 'SavedPlaces', 'TrustedContacts', 'Verification', 'Account', 'Safety', 'ShareTrip', 'Notifications', 'Support',
    ]);
  });

  it('keeps auth-only routes separate from public routes', () => {
    expect(PUBLIC_ROUTE_NAMES).toEqual(['Splash', 'Onboarding', 'Login', 'Otp']);
    expect(PROTECTED_ROUTE_NAMES).toContain('Home');
    expect(PROTECTED_ROUTE_NAMES).toContain('Payment');
    expect(PROTECTED_ROUTE_NAMES).not.toContain('Login');
  });

  it('types required params for search, result, booking, trip, payment, share, pickup, and dropoff', () => {
    const params: PassengerRootStackParamList = {
      Splash: undefined,
      Onboarding: undefined,
      Login: { redirectTo: 'Home' },
      Otp: { phoneNumber: '+94770000000' },
      ProfileSetup: undefined,
      Home: undefined,
      Search: { pickup: { latitude: 6.9271, longitude: 79.8612 }, dropoff: { latitude: 6.9, longitude: 79.9 } },
      SearchResults: { searchId: 's1', pickup: { latitude: 6.9271, longitude: 79.8612 }, dropoff: { latitude: 6.9, longitude: 79.9 } },
      RideDetail: { searchId: 's1', resultId: 'r1' },
      SeatSelection: { searchId: 's1', resultId: 'r1' },
      Payment: { bookingId: 'b1', paymentIntentId: 'pi1' },
      BookedWaiting: { bookingId: 'b1' },
      InTrip: { tripId: 't1', bookingId: 'b1' },
      ExitEarly: { tripId: 't1', bookingId: 'b1' },
      Receipt: { bookingId: 'b1' },
      RateDriver: { bookingId: 'b1', tripId: 't1' },
      TripHistory: undefined,
      SavedPlaces: undefined,
      TrustedContacts: undefined,
      Verification: undefined,
      Account: undefined,
      Safety: { tripId: 't1', bookingId: 'b1' },
      ShareTrip: { shareLinkId: 'share1', tripId: 't1' },
      Notifications: undefined,
      Support: { ticketId: 'ticket1' },
    };
    expect(params.Payment?.paymentIntentId).toBe('pi1');
  });

  it('registers deep link prefixes without making protected routes public', () => {
    expect(PASSENGER_LINKING_PREFIXES).toEqual(expect.arrayContaining(['routeshare://', 'https://routeshare.app']));
  });
});
