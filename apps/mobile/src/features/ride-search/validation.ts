import type { RideSearchDraft, RideSearchValidationErrors, RideSearchValidationResult, SearchPlace } from './types';

export const MIN_SEARCH_SEATS = 1;
export const MAX_SEARCH_SEATS = 4;

const hasCoordinate = (place: SearchPlace | undefined): boolean =>
  typeof place?.coordinate?.latitude === 'number' &&
  typeof place.coordinate.longitude === 'number' &&
  place.coordinate.latitude >= -90 &&
  place.coordinate.latitude <= 90 &&
  place.coordinate.longitude >= -180 &&
  place.coordinate.longitude <= 180;

const hasUsableAddress = (place: SearchPlace | undefined): boolean => Boolean(place?.address?.trim() || place?.label.trim());

export function validateRideSearchDraft(draft: RideSearchDraft, now = new Date()): RideSearchValidationResult {
  const errors: RideSearchValidationErrors = {};

  if (!hasUsableAddress(draft.pickup) || !hasCoordinate(draft.pickup)) errors.pickup = 'Choose a pickup location.';
  if (!hasUsableAddress(draft.dropoff) || !hasCoordinate(draft.dropoff)) errors.dropoff = 'Choose a destination.';
  if (!draft.requestedDepartureTime || Number.isNaN(draft.requestedDepartureTime.getTime())) {
    errors.requestedDepartureTime = 'Choose a pickup time.';
  } else if (draft.requestedDepartureTime.getTime() <= now.getTime()) {
    errors.requestedDepartureTime = 'Choose a future pickup time.';
  }
  if (!Number.isInteger(draft.seats) || draft.seats < MIN_SEARCH_SEATS || draft.seats > MAX_SEARCH_SEATS) {
    errors.seats = 'Shared rides support 1-4 passenger seats.';
  }

  return Object.keys(errors).length === 0 ? { valid: true, errors } : { valid: false, errors };
}

export function swapSearchLocations({ pickup, dropoff }: Pick<RideSearchDraft, 'pickup' | 'dropoff'>): Pick<RideSearchDraft, 'pickup' | 'dropoff'> {
  return { pickup: dropoff, dropoff: pickup };
}
