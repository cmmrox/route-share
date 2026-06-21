import type { MatchTierName } from '../../design-system/tokens';
import type { RideResultModel } from './model';

export type RideSort = 'best-match' | 'price-low' | 'departure-early' | 'seats-most';

export type RideFilters = {
  readonly minMatchPercent: number;
  readonly maxFareLkr?: number;
  readonly minSeats: number;
  readonly sort: RideSort;
};

export const defaultRideFilters: RideFilters = {
  minMatchPercent: 0,
  maxFareLkr: undefined,
  minSeats: 1,
  sort: 'best-match',
};

const sorters: Record<RideSort, (a: RideResultModel, b: RideResultModel) => number> = {
  'best-match': (a, b) => b.matchPercent - a.matchPercent,
  'price-low': (a, b) => (a.fareLkr ?? Number.POSITIVE_INFINITY) - (b.fareLkr ?? Number.POSITIVE_INFINITY),
  'departure-early': (a, b) => departureMillis(a) - departureMillis(b),
  'seats-most': (a, b) => b.availableSeats - a.availableSeats,
};

const departureMillis = (model: RideResultModel): number => {
  if (!model.departureTime) return Number.POSITIVE_INFINITY;
  const t = new Date(model.departureTime).getTime();
  return Number.isNaN(t) ? Number.POSITIVE_INFINITY : t;
};

export const applyRideFilters = (
  models: readonly RideResultModel[],
  filters: RideFilters,
): RideResultModel[] => {
  const filtered = models.filter((m) => {
    if (m.matchPercent < filters.minMatchPercent) return false;
    if (filters.minSeats > 0 && m.availableSeats < filters.minSeats) return false;
    if (filters.maxFareLkr != null && m.fareLkr != null && m.fareLkr > filters.maxFareLkr) return false;
    return true;
  });
  // Stable, tie-broken by departure then match so equal keys keep a deterministic order.
  return [...filtered].sort((a, b) => sorters[filters.sort](a, b) || departureMillis(a) - departureMillis(b) || b.matchPercent - a.matchPercent);
};

const TIER_ORDER: readonly MatchTierName[] = ['full', 'high', 'mid', 'low'];
const TIER_TITLES: Record<MatchTierName, string> = {
  full: 'Full route match',
  high: 'High route match',
  mid: 'Partial route match',
  low: 'Low route match',
};

export type RideResultGroup = {
  readonly tier: MatchTierName;
  readonly title: string;
  readonly items: readonly RideResultModel[];
};

export const groupRideResults = (models: readonly RideResultModel[]): RideResultGroup[] =>
  TIER_ORDER.map((tier) => ({
    tier,
    title: TIER_TITLES[tier],
    items: models.filter((m) => m.matchTier === tier),
  })).filter((group) => group.items.length > 0);
