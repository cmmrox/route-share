import type { LocationStateInput, ResolvedLocationState } from './types';

export function resolveLocationState(input: LocationStateInput): ResolvedLocationState {
  if (input.permission === 'granted' && input.coordinate) {
    return {
      status: 'granted',
      currentLocation: { label: 'Current location', address: 'Current GPS location', coordinate: input.coordinate },
      manualPickupRequired: false,
      message: 'Using your current location for pickup.',
    };
  }

  if (input.permission === 'requesting') {
    return { status: 'requesting', manualPickupRequired: false, message: 'Requesting location permission…' };
  }

  if (input.permission === 'denied') {
    return {
      status: 'denied',
      manualPickupRequired: true,
      message: 'Location permission is off. You can still search by entering pickup manually.',
    };
  }

  if (input.permission === 'unavailable') {
    return {
      status: 'unavailable',
      manualPickupRequired: true,
      message: input.errorMessage ?? 'Current location is unavailable. Enter pickup manually to continue.',
    };
  }

  return { status: 'unknown', manualPickupRequired: false, message: 'Choose current location or enter pickup manually.' };
}
