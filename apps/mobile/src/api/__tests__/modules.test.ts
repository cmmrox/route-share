import { describe, expect, it, vi } from 'vitest';
import { ApiClient } from '../api-client';
import { createPassengerApi } from '../index';
import { mobileContractPaths, mobileLiveContractPaths } from '../contracts';

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

  it('passes the Google Places session token through autocomplete and details requests', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ success: true, data: [] }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ success: true, data: { placeId: 'p1', label: 'X', address: 'Y' } }), { status: 200 }));
    const api = createPassengerApi(new ApiClient({ baseUrl: 'https://api.example.test', fetch: fetchMock }));

    await api.places.autocomplete({ query: 'Colombo Fort', sessionToken: 'tok-1' });
    await api.places.details('p1', 'tok-1');

    expect(fetchMock.mock.calls[0][0]).toContain('sessionToken=tok-1');
    expect(fetchMock.mock.calls[1][0]).toContain('/api/v1/passenger/places/p1?sessionToken=tok-1');
  });

  it('loads the stored driver route segment without any directions call', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ success: true, data: {
      coordinates: [
        { latitude: 6.9337, longitude: 79.85 },
        { latitude: 6.9, longitude: 79.87 },
        { latitude: 6.8649, longitude: 79.8997 },
      ],
      distanceMeters: 5321,
      source: 'route_plan',
    } }), { status: 200 }));
    const api = createPassengerApi(new ApiClient({ baseUrl: 'https://api.example.test', fetch: fetchMock }));

    await expect(api.places.routeGeometry({ routeOccurrenceId: '20', pickupFraction: 0.2, dropoffFraction: 0.8 })).resolves.toEqual([
      { latitude: 6.9337, longitude: 79.85 },
      { latitude: 6.9, longitude: 79.87 },
      { latitude: 6.8649, longitude: 79.8997 },
    ]);
    expect(fetchMock.mock.calls[0][0]).toContain('/api/v1/passenger/route-occurrences/20/geometry?pickupFraction=0.2&dropoffFraction=0.8');
  });

  it('keeps the unified mobile contract inventory in sync with the OpenAPI document', () => {
    // One app, one contract (Decision 011): both role namespaces live in the same inventory.
    expect(mobileContractPaths).toContain('GET /api/v1/app/config');
    expect(mobileContractPaths).toContain('POST /api/v1/auth/otp/request');
    expect(mobileContractPaths).toContain('POST /api/v1/passenger/bookings');
    expect(mobileContractPaths).toContain('POST /api/v1/passenger/sos-events');
    expect(mobileContractPaths).toContain('GET /api/v1/passenger/saved-places/{savedPlaceId}');
    expect(mobileContractPaths).toContain('GET /api/v1/passenger/trusted-contacts/{contactId}');
    // Billing-free matched-ride polyline, served from stored PostGIS geometry.
    expect(mobileContractPaths).toContain('GET /api/v1/passenger/route-occurrences/{routeOccurrenceId}/geometry');
    // Driver endpoints are now reachable from the same app.
    expect(mobileContractPaths).toContain('GET /api/v1/driver/trips');
    expect(mobileContractPaths).toContain('POST /api/v1/driver/vehicles');
  });

  it('exposes the app shell context endpoint as live', () => {
    expect(mobileLiveContractPaths).toContain('GET /api/v1/me/context');
  });

  it('separates live endpoints from those a later slice will build', () => {
    // Planned paths are in the contract so the shape is agreed, but calling one today is a bug.
    expect(mobileContractPaths).toContain('POST /api/v1/passenger/live-requests');
    expect(mobileLiveContractPaths).not.toContain('POST /api/v1/passenger/live-requests');
    expect(mobileLiveContractPaths.length).toBeLessThan(mobileContractPaths.length);
  });
});
