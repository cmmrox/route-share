import { describe, expect, it, vi } from 'vitest';
import { ApiClient } from '../api-client';
import { createPassengerApi } from '../index';
import { passengerContractPaths } from '../contracts';

describe('passenger endpoint modules', () => {
  it('exposes modules for every Task 01 API area', () => {
    const api = createPassengerApi(new ApiClient({ baseUrl: 'https://api.example.test', fetch: vi.fn() }));
    expect(Object.keys(api).sort()).toEqual([
      'appConfig',
      'auth',
      'bookings',
      'notifications',
      'payments',
      'profile',
      'rideSearch',
      'safety',
      'savedPlaces',
      'support',
      'trips',
      'trustedContacts'
    ]);
  });

  it('sends the required Idempotency-Key header when creating a booking', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ success: true, data: { bookingId: 'b1' } }), { status: 200 }));
    const api = createPassengerApi(new ApiClient({ baseUrl: 'https://api.example.test', fetch: fetchMock }));

    await api.bookings.create({ routeOccurrenceId: 10, seats: 1 }, 'idem-123');

    expect(fetchMock.mock.calls[0][1].headers['Idempotency-Key']).toBe('idem-123');
  });

  it('keeps generated-compatible passenger contract paths in sync with api-contracts inventory', () => {
    expect(passengerContractPaths).toContain('GET /api/v1/app/config');
    expect(passengerContractPaths).toContain('POST /api/v1/auth/otp/request');
    expect(passengerContractPaths).toContain('POST /api/v1/auth/otp/verify');
    expect(passengerContractPaths).toContain('POST /api/v1/passenger/bookings');
    expect(passengerContractPaths).toContain('POST /api/v1/passenger/sos-events');
    expect(passengerContractPaths).toContain('GET /api/v1/passenger/saved-places/{savedPlaceId}');
    expect(passengerContractPaths).toContain('GET /api/v1/passenger/trusted-contacts/{contactId}');
    expect(passengerContractPaths).toHaveLength(49);
  });
});
