import { describe, expect, it } from 'vitest';
import { adaptBooking, adaptPaymentIntent, adaptRideSearchResult, adaptSavedPlace, adaptTrustedContact } from '../adapters';

describe('DTO adapters for known runtime/OpenAPI mismatches', () => {
  it('normalizes runtime routeId/searchResultId booking fields to mobile booking DTO ids', () => {
    expect(adaptBooking({ id: 'b1', routeId: 'r1', searchResultId: 'sr1', status: 'PENDING', fareEstimate: 1200 })).toMatchObject({
      bookingId: 'b1',
      rideId: 'r1',
      resultId: 'sr1',
      status: 'PENDING',
      fareEstimateLkr: 1200
    });
  });

  it('normalizes saved-place runtime coordinate object and OpenAPI flat lat/lng shape', () => {
    expect(adaptSavedPlace({ id: 'sp1', label: 'Home', latitude: 6.9, longitude: 79.8 }).location).toEqual({ latitude: 6.9, longitude: 79.8 });
    expect(adaptSavedPlace({ savedPlaceId: 'sp2', name: 'Work', location: { latitude: 7.1, longitude: 80.1 } }).label).toBe('Work');
  });

  it('normalizes trusted contact id/name/phone fields', () => {
    expect(adaptTrustedContact({ id: 'c1', contactName: 'A', phone: '+94', relationship: 'Friend' })).toEqual({
      contactId: 'c1',
      name: 'A',
      phoneNumber: '+94',
      relationship: 'Friend'
    });
  });

  it('normalizes ride-search result fare/time naming differences', () => {
    expect(adaptRideSearchResult({ id: 'res1', routeId: 'route1', estimatedFare: 900, pickupEtaMinutes: 5 })).toMatchObject({
      resultId: 'res1',
      rideId: 'route1',
      fareEstimateLkr: 900,
      pickupEtaMinutes: 5
    });
  });

  it('normalizes payment intent runtime amount/currency/status fields', () => {
    expect(adaptPaymentIntent({ id: 'pi1', clientSecret: 'cs', amount: 1000, currency: 'LKR', status: 'REQUIRES_PAYMENT_METHOD' })).toEqual({
      paymentIntentId: 'pi1',
      clientSecret: 'cs',
      amountLkr: 1000,
      currency: 'LKR',
      status: 'REQUIRES_PAYMENT_METHOD'
    });
  });
});
