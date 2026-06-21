export type JsonPrimitive = string | number | boolean | null;
export type JsonValue = JsonPrimitive | JsonValue[] | { [key: string]: JsonValue };
export type JsonRecord = Record<string, unknown>;

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: { code?: string; message?: string; details?: unknown };
  message?: string;
  timestamp?: string;
};

export type Coordinate = { latitude: number; longitude: number };

export type PassengerProfile = {
  passengerId: string;
  displayName?: string;
  fullName?: string;
  phoneNumber?: string;
  email?: string;
  photoUrl?: string;
  preferences?: Record<string, unknown>;
  verificationStatus?: string;
};

export type SavedPlace = { savedPlaceId: string; label: string; location: Coordinate; address?: string; isDefault?: boolean };
export type PlaceSuggestion = { placeId: string; label: string; address?: string; coordinate?: Coordinate };
export type TrustedContact = { contactId: string; name: string; phoneNumber: string; relationship?: string; isPrimary?: boolean };
export type RideSearch = { searchId: string; status?: string; requestedDepartureAt?: string; seatsRequested?: number; raw?: unknown };
export type RideSearchResult = { resultId: string; rideId: string; routeOccurrenceId?: string; originLabel?: string; destinationLabel?: string; departureTime?: string; availableSeats?: number; matchScore?: number; overlapPercent?: number; pickupDistanceMeters?: number; dropoffDistanceMeters?: number; matchedDistanceMeters?: number; pickupRouteFraction?: number; dropoffRouteFraction?: number; explanation?: string; fareEstimateLkr?: number; currency?: string; pickupEtaMinutes?: number; driverName?: string; vehicleMake?: string; vehicleModel?: string; vehicleRegistration?: string; vehicleSeatCount?: number; raw?: unknown };
export type Booking = { bookingId: string; rideId?: string; resultId?: string; status?: string; fareEstimateLkr?: number; raw?: unknown };
export type PaymentIntent = { paymentIntentId: string; clientSecret?: string; amountLkr: number; currency: 'LKR' | string; status?: string };
