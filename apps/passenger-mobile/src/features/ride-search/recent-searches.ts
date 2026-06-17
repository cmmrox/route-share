import type { RecentSearch, RecentSearchStorage, RideSearchDraft, SearchPlace } from './types';

const RECENT_SEARCHES_KEY = 'routeshare.passenger.recent-searches.v1';
const DEFAULT_LIMIT = 8;

type RecentSearchRepositoryOptions = { readonly limit?: number; readonly now?: () => Date; readonly idFactory?: () => string };

export function createMemoryRecentSearchStorage(initial?: Record<string, string>): RecentSearchStorage {
  const values = new Map(Object.entries(initial ?? {}));
  return {
    getItemAsync: async (key) => values.get(key) ?? null,
    setItemAsync: async (key, value) => {
      values.set(key, value);
    },
    deleteItemAsync: async (key) => {
      values.delete(key);
    },
  };
}

function safePlace(place: SearchPlace): SearchPlace {
  return {
    label: place.label,
    address: place.address,
    placeProviderId: place.placeProviderId,
  };
}

function parseStoredRecentSearches(raw: string | null): RecentSearch[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(isRecentSearch);
  } catch {
    return [];
  }
}

function isRecentSearch(value: unknown): value is RecentSearch {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  const candidate = value as Partial<RecentSearch>;
  return (
    typeof candidate.id === 'string' &&
    typeof candidate.pickup?.label === 'string' &&
    typeof candidate.dropoff?.label === 'string' &&
    typeof candidate.requestedDepartureTimeIso === 'string' &&
    typeof candidate.seats === 'number' &&
    typeof candidate.createdAtIso === 'string'
  );
}

export function createRecentSearchRepository(storage: RecentSearchStorage, options: RecentSearchRepositoryOptions = {}) {
  const limit = Math.max(1, options.limit ?? DEFAULT_LIMIT);
  const now = options.now ?? (() => new Date());
  const idFactory = options.idFactory ?? (() => `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`);

  const write = async (items: readonly RecentSearch[]) => storage.setItemAsync(RECENT_SEARCHES_KEY, JSON.stringify(items.slice(0, limit)));

  return {
    list: async (): Promise<RecentSearch[]> => parseStoredRecentSearches(await storage.getItemAsync(RECENT_SEARCHES_KEY)).slice(0, limit),
    save: async (draft: Required<Pick<RideSearchDraft, 'pickup' | 'dropoff' | 'requestedDepartureTime' | 'seats'>>): Promise<RecentSearch> => {
      const recent: RecentSearch = {
        id: idFactory(),
        pickup: safePlace(draft.pickup),
        dropoff: safePlace(draft.dropoff),
        requestedDepartureTimeIso: draft.requestedDepartureTime.toISOString(),
        seats: draft.seats,
        createdAtIso: now().toISOString(),
      };
      const current = parseStoredRecentSearches(await storage.getItemAsync(RECENT_SEARCHES_KEY));
      const deduped = current.filter((item) => `${item.pickup.label}|${item.dropoff.label}` !== `${recent.pickup.label}|${recent.dropoff.label}`);
      await write([recent, ...deduped]);
      return recent;
    },
    delete: async (id: string): Promise<void> => {
      const current = parseStoredRecentSearches(await storage.getItemAsync(RECENT_SEARCHES_KEY));
      await write(current.filter((item) => item.id !== id));
    },
    clear: async (): Promise<void> => storage.deleteItemAsync(RECENT_SEARCHES_KEY),
  };
}
