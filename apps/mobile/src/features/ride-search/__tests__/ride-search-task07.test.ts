import { describe, expect, it } from 'vitest';

import {
  buildHomeDashboardModel,
  buildRideSearchRequest,
  createMemoryRecentSearchStorage,
  createRecentSearchRepository,
  resolveLocationState,
  swapSearchLocations,
  validateRideSearchDraft,
} from '../index';

const pickup = { label: 'Current location', address: 'Buthgamuwa Road, Rajagiriya', coordinate: { latitude: 6.909, longitude: 79.909 } };
const dropoff = { label: 'Office', address: 'Codegen, Colombo', coordinate: { latitude: 6.927, longitude: 79.861 } };

describe('Task 07 ride search feature logic', () => {
  it('validates pickup, dropoff, future time and seat limits before route discovery', () => {
    const past = validateRideSearchDraft({ pickup, dropoff, requestedDepartureTime: new Date('2026-01-01T08:00:00.000Z'), seats: 5 }, new Date('2026-01-01T08:05:00.000Z'));
    expect(past.valid).toBe(false);
    expect(past.errors.requestedDepartureTime).toBe('Choose a future pickup time.');
    expect(past.errors.seats).toBe('Shared rides support 1-4 passenger seats.');

    const missing = validateRideSearchDraft({ pickup: undefined, dropoff: undefined, requestedDepartureTime: new Date('2026-01-01T08:10:00.000Z'), seats: 1 }, new Date('2026-01-01T08:00:00.000Z'));
    expect(missing.errors.pickup).toBe('Choose a pickup location.');
    expect(missing.errors.dropoff).toBe('Choose a destination.');
  });

  it("builds the current backend RouteSearchRequest DTO with coordinates and no stale context", () => {
    const request = buildRideSearchRequest({ pickup, dropoff, requestedDepartureTime: new Date("2026-06-15T12:30:00.000Z"), seats: 2 }, new Date("2026-06-15T12:00:00.000Z"));

    expect(request).toEqual({
      pickup: pickup.coordinate,
      dropoff: dropoff.coordinate,
      requestedDepartureTime: "2026-06-15T12:30:00.000Z",
      seats: 2,
      pickupRadiusMeters: 1000,
      dropoffRadiusMeters: 1000,
      departureWindowMinutes: 60,
      limit: 20,
    });
    expect(JSON.stringify(request)).not.toContain("routeId");
    expect(JSON.stringify(request)).not.toContain("resultId");
  });

  it('supports swap and location permission fallback states', () => {
    expect(swapSearchLocations({ pickup, dropoff })).toEqual({ pickup: dropoff, dropoff: pickup });
    expect(resolveLocationState({ permission: 'denied' }).manualPickupRequired).toBe(true);
    expect(resolveLocationState({ permission: 'granted', coordinate: pickup.coordinate }).currentLocation?.coordinate).toEqual(pickup.coordinate);
  });

  it('persists recent searches with capped privacy-safe retention and clear/delete controls', async () => {
    const storage = createMemoryRecentSearchStorage();
    const repo = createRecentSearchRepository(storage, { limit: 2 });

    await repo.save({ pickup, dropoff, requestedDepartureTime: new Date('2026-06-15T12:30:00.000Z'), seats: 2 });
    await repo.save({ pickup: dropoff, dropoff: pickup, requestedDepartureTime: new Date('2026-06-16T12:30:00.000Z'), seats: 1 });
    await repo.save({ pickup, dropoff: { ...dropoff, label: 'Nugegoda' }, requestedDepartureTime: new Date('2026-06-17T12:30:00.000Z'), seats: 3 });

    const recents = await repo.list();
    expect(recents).toHaveLength(2);
    expect(recents[0].dropoff.label).toBe('Nugegoda');
    expect(recents[0].pickup.coordinate).toBeUndefined();
    expect(recents[0].dropoff.coordinate).toBeUndefined();

    await repo.delete(recents[0].id);
    expect(await repo.list()).toHaveLength(1);
    await repo.clear();
    expect(await repo.list()).toEqual([]);
  });

  it('builds dashboard content from saved and recent places without screen business logic', () => {
    const model = buildHomeDashboardModel({ displayName: 'Nimali', savedPlaces: [{ savedPlaceId: '1', label: 'Home', address: 'Rajagiriya', location: pickup.coordinate, isDefault: true }], recentSearches: [{ id: 'r1', pickup: { label: 'Home' }, dropoff: { label: 'Office' }, requestedDepartureTimeIso: '2026-06-15T12:30:00.000Z', seats: 2, createdAtIso: '2026-06-14T12:30:00.000Z' }] });

    expect(model.greeting).toBe('Where to, Nimali?');
    expect(model.quickPlaces[0].label).toBe('Home');
    expect(model.frequentRoutes[0].title).toBe('Home → Office');
  });
});
