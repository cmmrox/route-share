import type { RideSearchResult } from '../../api/types';
import { getMatchTier, type MatchTierName } from '../../design-system/tokens';

export type RideResultModel = {
  readonly resultId: string;
  readonly rideId: string;
  readonly routeOccurrenceId?: string;
  readonly originLabel: string;
  readonly destinationLabel: string;
  readonly departureTime?: string;
  readonly departureLabel: string;
  readonly availableSeats: number;
  readonly matchPercent: number;
  readonly matchTier: MatchTierName;
  readonly matchTierLabel: string;
  readonly overlapPercent: number;
  readonly fareLkr?: number;
  readonly fareLabel: string;
  readonly driverName: string;
  readonly vehicleLabel: string;
  readonly vehicleRegistration?: string;
  readonly seatsLabel: string;
  readonly walkLabel: string;
  readonly explanation?: string;
  readonly source: RideSearchResult;
};

const clampPercent = (value: number | undefined): number => {
  if (value == null || Number.isNaN(value)) return 0;
  return Math.max(0, Math.min(100, Math.round(value)));
};

export const formatLkr = (amount: number | undefined): string => {
  if (amount == null || Number.isNaN(amount)) return 'Fare on request';
  return `LKR ${Math.round(amount).toLocaleString('en-LK')}`;
};

export const formatDeparture = (iso: string | undefined, now: Date = new Date()): string => {
  if (!iso) return 'Departure pending';
  const when = new Date(iso);
  if (Number.isNaN(when.getTime())) return 'Departure pending';
  const time = when.toLocaleTimeString('en-LK', { hour: '2-digit', minute: '2-digit' });
  const sameDay = when.toDateString() === now.toDateString();
  if (sameDay) return `Today ${time}`;
  const day = when.toLocaleDateString('en-LK', { weekday: 'short', day: 'numeric', month: 'short' });
  return `${day} ${time}`;
};

const formatWalk = (pickupMeters?: number, dropoffMeters?: number): string => {
  const parts: string[] = [];
  if (pickupMeters != null) parts.push(`${Math.round(pickupMeters)} m to pickup`);
  if (dropoffMeters != null) parts.push(`${Math.round(dropoffMeters)} m from drop-off`);
  return parts.length ? parts.join(' · ') : 'Walking distance unavailable';
};

const vehicleLabelOf = (result: RideSearchResult): string => {
  const make = result.vehicleMake?.trim();
  const model = result.vehicleModel?.trim();
  const combined = [make, model].filter(Boolean).join(' ');
  return combined || 'Vehicle details on confirmation';
};

export const toRideResultModel = (
  result: RideSearchResult,
  now: Date = new Date(),
): RideResultModel => {
  const matchPercent = clampPercent(result.matchScore);
  const tier = getMatchTier(matchPercent);
  const seats = result.availableSeats ?? 0;
  return {
    resultId: result.resultId,
    rideId: result.rideId,
    routeOccurrenceId: result.routeOccurrenceId,
    originLabel: result.originLabel?.trim() || 'Pickup area',
    destinationLabel: result.destinationLabel?.trim() || 'Drop-off area',
    departureTime: result.departureTime,
    departureLabel: formatDeparture(result.departureTime, now),
    availableSeats: seats,
    matchPercent,
    matchTier: tier.name,
    matchTierLabel: tier.label,
    overlapPercent: clampPercent(result.overlapPercent),
    fareLkr: result.fareEstimateLkr,
    fareLabel: formatLkr(result.fareEstimateLkr),
    driverName: result.driverName?.trim() || 'RouteShare driver',
    vehicleLabel: vehicleLabelOf(result),
    vehicleRegistration: result.vehicleRegistration?.trim() || undefined,
    seatsLabel: seats === 1 ? '1 seat left' : `${seats} seats left`,
    walkLabel: formatWalk(result.pickupDistanceMeters, result.dropoffDistanceMeters),
    explanation: result.explanation?.trim() || undefined,
    source: result,
  };
};

export const toRideResultModels = (
  results: readonly RideSearchResult[],
  now: Date = new Date(),
): RideResultModel[] => results.map((r) => toRideResultModel(r, now));
