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
export type TrustedContact = { contactId: string; name: string; phoneNumber: string; relationship?: string; isPrimary?: boolean };
export type RideSearchResult = { resultId: string; rideId: string; fareEstimateLkr?: number; pickupEtaMinutes?: number; raw?: unknown };
export type Booking = { bookingId: string; rideId?: string; resultId?: string; status?: string; fareEstimateLkr?: number; raw?: unknown };
export type PaymentIntent = { paymentIntentId: string; clientSecret?: string; amountLkr: number; currency: 'LKR' | string; status?: string };
