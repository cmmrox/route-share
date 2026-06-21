import { describe, expect, it } from 'vitest';

import type { RideSearchResult } from '../../../api/types';
import { applyRideFilters, defaultRideFilters, groupRideResults } from '../filters';
import { formatDeparture, formatLkr, toRideResultModel, toRideResultModels } from '../model';

const result = (over: Partial<RideSearchResult>): RideSearchResult => ({
  resultId: over.resultId ?? 'r1',
  rideId: over.rideId ?? 'ride1',
  ...over,
});

describe('ride-results model', () => {
  it('normalizes a backend result into a UI model', () => {
    const model = toRideResultModel(
      result({
        resultId: '20',
        rideId: '10',
        routeOccurrenceId: '20',
        originLabel: 'Rajagiriya',
        destinationLabel: 'Nugegoda',
        departureTime: '2026-06-21T09:30:00.000Z',
        availableSeats: 3,
        matchScore: 88,
        overlapPercent: 70,
        fareEstimateLkr: 980,
        driverName: 'Nimal Perera',
        vehicleMake: 'Toyota',
        vehicleModel: 'Aqua',
        vehicleRegistration: 'CAB-1234',
        pickupDistanceMeters: 120,
        dropoffDistanceMeters: 80,
        explanation: 'Strong overlap',
      }),
      new Date('2026-06-21T06:00:00.000Z'),
    );
    expect(model.matchPercent).toBe(88);
    expect(model.matchTier).toBe('high');
    expect(model.fareLabel).toBe('LKR 980');
    expect(model.vehicleLabel).toBe('Toyota Aqua');
    expect(model.seatsLabel).toBe('3 seats left');
    expect(model.walkLabel).toContain('120 m to pickup');
  });

  it('falls back gracefully when optional fields are missing', () => {
    const model = toRideResultModel(result({ availableSeats: 1 }));
    expect(model.driverName).toBe('RouteShare driver');
    expect(model.vehicleLabel).toBe('Vehicle details on confirmation');
    expect(model.fareLabel).toBe('Fare on request');
    expect(model.seatsLabel).toBe('1 seat left');
  });

  it('formats fare and departure', () => {
    expect(formatLkr(1234)).toBe('LKR 1,234');
    expect(formatLkr(undefined)).toBe('Fare on request');
    expect(formatDeparture(undefined)).toBe('Departure pending');
  });
});

describe('ride-results filters', () => {
  const models = toRideResultModels([
    result({ resultId: 'a', matchScore: 96, fareEstimateLkr: 1200, availableSeats: 2, departureTime: '2026-06-21T10:00:00.000Z' }),
    result({ resultId: 'b', matchScore: 80, fareEstimateLkr: 600, availableSeats: 4, departureTime: '2026-06-21T09:00:00.000Z' }),
    result({ resultId: 'c', matchScore: 40, fareEstimateLkr: 400, availableSeats: 1, departureTime: '2026-06-21T11:00:00.000Z' }),
  ]);

  it('sorts by best match by default', () => {
    const out = applyRideFilters(models, defaultRideFilters);
    expect(out.map((m) => m.resultId)).toEqual(['a', 'b', 'c']);
  });

  it('sorts by lowest price', () => {
    const out = applyRideFilters(models, { ...defaultRideFilters, sort: 'price-low' });
    expect(out.map((m) => m.resultId)).toEqual(['c', 'b', 'a']);
  });

  it('filters by min match and min seats', () => {
    const out = applyRideFilters(models, { ...defaultRideFilters, minMatchPercent: 75, minSeats: 3 });
    expect(out.map((m) => m.resultId)).toEqual(['b']);
  });

  it('filters by max fare', () => {
    const out = applyRideFilters(models, { ...defaultRideFilters, maxFareLkr: 600 });
    expect(out.map((m) => m.resultId).sort()).toEqual(['b', 'c']);
  });

  it('groups by overlap tier in priority order', () => {
    const groups = groupRideResults(models);
    expect(groups.map((g) => g.tier)).toEqual(['full', 'high', 'low']);
    expect(groups[0].items[0].resultId).toBe('a');
  });
});
