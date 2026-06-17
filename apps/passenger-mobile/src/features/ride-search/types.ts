import type { Coordinate, SavedPlace } from '../../api/types';

export type SearchPlace = {
  readonly label: string;
  readonly address?: string;
  readonly coordinate?: Coordinate;
  readonly placeProviderId?: string;
};

export type RideSearchDraft = {
  readonly pickup?: SearchPlace;
  readonly dropoff?: SearchPlace;
  readonly requestedDepartureTime?: Date;
  readonly seats: number;
};

export type RideSearchValidationErrors = Partial<Record<'pickup' | 'dropoff' | 'requestedDepartureTime' | 'seats', string>>;

export type RideSearchValidationResult =
  | { readonly valid: true; readonly errors: RideSearchValidationErrors }
  | { readonly valid: false; readonly errors: RideSearchValidationErrors };

export type CoordinateRequestDto = {
  readonly latitude: number;
  readonly longitude: number;
};

export type RideSearchRequestDto = {
  readonly pickup: CoordinateRequestDto;
  readonly dropoff: CoordinateRequestDto;
  readonly requestedDepartureTime: string;
  readonly seats: number;
  readonly pickupRadiusMeters?: number;
  readonly dropoffRadiusMeters?: number;
  readonly departureWindowMinutes?: number;
  readonly limit?: number;
};

export type RecentSearch = {
  readonly id: string;
  readonly pickup: SearchPlace;
  readonly dropoff: SearchPlace;
  readonly requestedDepartureTimeIso: string;
  readonly seats: number;
  readonly createdAtIso: string;
};

export type RecentSearchStorage = {
  readonly getItemAsync: (key: string) => Promise<string | null>;
  readonly setItemAsync: (key: string, value: string) => Promise<void>;
  readonly deleteItemAsync: (key: string) => Promise<void>;
};

export type LocationPermissionState = 'unknown' | 'requesting' | 'granted' | 'denied' | 'unavailable';

export type LocationStateInput = {
  readonly permission: LocationPermissionState;
  readonly coordinate?: Coordinate;
  readonly errorMessage?: string;
};

export type ResolvedLocationState = {
  readonly status: LocationPermissionState;
  readonly currentLocation?: SearchPlace;
  readonly manualPickupRequired: boolean;
  readonly message: string;
};

export type HomeDashboardInput = {
  readonly displayName?: string;
  readonly savedPlaces: readonly SavedPlace[];
  readonly recentSearches: readonly RecentSearch[];
};

export type HomeRouteSuggestion = {
  readonly title: string;
  readonly subtitle: string;
  readonly pickup?: SearchPlace;
  readonly dropoff?: SearchPlace;
};

export type HomeDashboardModel = {
  readonly greeting: string;
  readonly quickPlaces: readonly SearchPlace[];
  readonly frequentRoutes: readonly HomeRouteSuggestion[];
  readonly stats: readonly { readonly label: string; readonly value: string }[];
};
