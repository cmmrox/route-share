import type { RideSearchDraft, RideSearchRequestDto, SearchPlace } from './types';
import { validateRideSearchDraft } from './validation';

function toCoordinateRequest(place: SearchPlace) {
  if (!place.coordinate) throw new Error('Cannot build ride search request without coordinates.');
  return { latitude: place.coordinate.latitude, longitude: place.coordinate.longitude };
}

export function buildRideSearchRequest(draft: RideSearchDraft, now = new Date()): RideSearchRequestDto {
  const validation = validateRideSearchDraft(draft, now);
  if (!validation.valid || !draft.pickup || !draft.dropoff || !draft.requestedDepartureTime) {
    throw new Error('Ride search draft is not ready to submit.');
  }

  return {
    pickup: toCoordinateRequest(draft.pickup),
    dropoff: toCoordinateRequest(draft.dropoff),
    requestedDepartureTime: draft.requestedDepartureTime.toISOString(),
    seats: draft.seats,
    pickupRadiusMeters: 1000,
    dropoffRadiusMeters: 1000,
    departureWindowMinutes: 60,
    limit: 20,
  };
}
