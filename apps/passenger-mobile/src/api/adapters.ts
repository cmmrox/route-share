import type { Booking, Coordinate, PassengerProfile, PaymentIntent, RideSearchResult, SavedPlace, TrustedContact } from './types';

const asRecord = (value: unknown): Record<string, unknown> => (value && typeof value === 'object' ? (value as Record<string, unknown>) : {});
const stringFrom = (...values: unknown[]) => String(values.find((value) => typeof value === 'string' || typeof value === 'number') ?? '');
const numberFrom = (...values: unknown[]) => {
  const value = values.find((candidate) => typeof candidate === 'number' || (typeof candidate === 'string' && candidate.trim() !== ''));
  return value === undefined ? undefined : Number(value);
};

export const adaptCoordinate = (input: unknown): Coordinate => {
  const source = asRecord(input);
  const nested = asRecord(source.location ?? source.coordinate ?? source.coordinates);
  const latitude = numberFrom(source.latitude, source.lat, nested.latitude, nested.lat);
  const longitude = numberFrom(source.longitude, source.lng, source.lon, nested.longitude, nested.lng, nested.lon);
  if (latitude === undefined || longitude === undefined || Number.isNaN(latitude) || Number.isNaN(longitude)) {
    throw new Error('Invalid coordinate DTO');
  }
  return { latitude, longitude };
};

export const adaptPassengerProfile = (input: unknown): PassengerProfile => {
  const source = asRecord(input);
  const preferences = asRecord(source.preferences);
  const fullName = stringFrom(source.fullName, source.displayName, source.name) || undefined;
  return {
    passengerId: stringFrom(source.passengerId, source.id),
    displayName: fullName,
    fullName,
    phoneNumber: typeof source.phoneNumber === 'string' ? source.phoneNumber : undefined,
    email: typeof source.email === 'string' ? source.email : typeof preferences.email === 'string' ? preferences.email : undefined,
    photoUrl: typeof source.photoUrl === 'string' ? source.photoUrl : undefined,
    preferences,
    verificationStatus: typeof source.verificationStatus === 'string' ? source.verificationStatus : 'readiness_only'
  };
};

export const adaptSavedPlace = (input: unknown): SavedPlace => {
  const source = asRecord(input);
  return {
    savedPlaceId: stringFrom(source.savedPlaceId, source.id),
    label: stringFrom(source.label, source.name),
    address: typeof source.address === 'string' ? source.address : undefined,
    location: adaptCoordinate(source.location ? source : { location: source }),
    isDefault: source.isDefault === true
  };
};

export const adaptTrustedContact = (input: unknown): TrustedContact => {
  const source = asRecord(input);
  const contact: TrustedContact = {
    contactId: stringFrom(source.contactId, source.id),
    name: stringFrom(source.name, source.contactName, source.fullName),
    phoneNumber: stringFrom(source.phoneNumber, source.phone),
    relationship: typeof source.relationship === "string" ? source.relationship : undefined,
  };
  if (source.isPrimary === true) contact.isPrimary = true;
  return contact;
};

export const adaptRideSearchResult = (input: unknown): RideSearchResult => {
  const source = asRecord(input);
  return {
    resultId: stringFrom(source.resultId, source.searchResultId, source.id),
    rideId: stringFrom(source.rideId, source.routeId, source.tripId),
    fareEstimateLkr: numberFrom(source.fareEstimateLkr, source.estimatedFare, source.fareEstimate),
    pickupEtaMinutes: numberFrom(source.pickupEtaMinutes, source.etaMinutes),
    raw: input
  };
};

export const adaptBooking = (input: unknown): Booking => {
  const source = asRecord(input);
  return {
    bookingId: stringFrom(source.bookingId, source.id),
    rideId: stringFrom(source.rideId, source.routeId, source.tripId),
    resultId: stringFrom(source.resultId, source.searchResultId),
    status: typeof source.status === 'string' ? source.status : undefined,
    fareEstimateLkr: numberFrom(source.fareEstimateLkr, source.fareEstimate, source.estimatedFare),
    raw: input
  };
};

export const adaptPaymentIntent = (input: unknown): PaymentIntent => {
  const source = asRecord(input);
  return {
    paymentIntentId: stringFrom(source.paymentIntentId, source.id),
    clientSecret: typeof source.clientSecret === 'string' ? source.clientSecret : undefined,
    amountLkr: numberFrom(source.amountLkr, source.amount) ?? 0,
    currency: stringFrom(source.currency) || 'LKR',
    status: typeof source.status === 'string' ? source.status : undefined
  };
};
