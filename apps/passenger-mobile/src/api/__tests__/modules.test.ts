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
      'places',
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

  it('maps create ride search runtime response list to route result DTOs', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ success: true, data: [{ routePlanId: 10, routeOccurrenceId: 20, originLabel: 'Rajagiriya', destinationLabel: 'Colombo', score: 0.91 }] }), { status: 200 }));
    const api = createPassengerApi(new ApiClient({ baseUrl: 'https://api.example.test', fetch: fetchMock }));

    await expect(api.rideSearch.search({ pickup: { latitude: 6.9, longitude: 79.9 }, dropoff: { latitude: 6.92, longitude: 79.86 }, requestedDepartureTime: '2026-06-15T12:30:00.000Z', seats: 1 })).resolves.toEqual([
      expect.objectContaining({ resultId: '20', rideId: '10', originLabel: 'Rajagiriya', destinationLabel: 'Colombo', matchScore: 0.91 }),
    ]);
  });

  it('maps backend place autocomplete and details into searchable places', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ success: true, data: [
        { placeId: 'places/codegen', label: 'Codegen', address: 'Trace Expert City', coordinate: null },
      ] }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ success: true, data: {
        placeId: 'places/codegen',
        label: 'Codegen',
        address: 'Trace Expert City, Colombo',
        coordinate: { latitude: 6.9271, longitude: 79.8612 },
      } }), { status: 200 }));
    const api = createPassengerApi(new ApiClient({ baseUrl: 'https://api.example.test', fetch: fetchMock }));

    await expect(api.places.autocomplete({ query: 'Codegen', latitude: 6.9, longitude: 79.9 })).resolves.toEqual([
      { placeId: 'places/codegen', label: 'Codegen', address: 'Trace Expert City' },
    ]);
    await expect(api.places.details('places/codegen')).resolves.toEqual({
      placeId: 'places/codegen',
      label: 'Codegen',
      address: 'Trace Expert City, Colombo',
      coordinate: { latitude: 6.9271, longitude: 79.8612 },
    });
    expect(fetchMock.mock.calls[0][0]).toContain('/api/v1/passenger/places/autocomplete?query=Codegen&latitude=6.9&longitude=79.9');
  });

  it('keeps generated-compatible passenger contract paths in sync with api-contracts inventory', () => {
    expect(passengerContractPaths).toContain('GET /api/v1/app/config');
    expect(passengerContractPaths).toContain('POST /api/v1/auth/otp/request');
    expect(passengerContractPaths).toContain('POST /api/v1/auth/otp/verify');
    expect(passengerContractPaths).toContain('POST /api/v1/passenger/bookings');
    expect(passengerContractPaths).toContain('POST /api/v1/passenger/sos-events');
    expect(passengerContractPaths).toContain('GET /api/v1/passenger/saved-places/{savedPlaceId}');
    expect(passengerContractPaths).toContain('GET /api/v1/passenger/trusted-contacts/{contactId}');
    // Ride search is stateless: the two GET-by-searchId result paths were removed (06.6-K cleanup).
    // +1 for the road-following directions endpoint used to draw the real route on the map.
    expect(passengerContractPaths).toHaveLength(50);
  });
});
