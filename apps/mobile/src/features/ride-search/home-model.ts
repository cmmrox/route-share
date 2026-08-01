import type { SavedPlace } from '../../api/types';
import type { HomeDashboardInput, HomeDashboardModel, HomeRouteSuggestion, SearchPlace } from './types';

const DEFAULT_QUICK_PLACES: readonly SearchPlace[] = [
  { label: 'Home', address: 'Add home in Saved Places' },
  { label: 'Office', address: 'Add office in Saved Places' },
];

function toSearchPlace(place: SavedPlace): SearchPlace {
  return { label: place.label, address: place.address, coordinate: place.location };
}

function firstName(displayName: string | undefined): string {
  const trimmed = displayName?.trim();
  return trimmed ? trimmed.split(/\s+/)[0] : 'there';
}

function routeTitle(pickupLabel: string, dropoffLabel: string): string {
  return `${pickupLabel} → ${dropoffLabel}`;
}

export function buildHomeDashboardModel(input: HomeDashboardInput): HomeDashboardModel {
  const quickPlaces = input.savedPlaces.length > 0 ? input.savedPlaces.slice(0, 4).map(toSearchPlace) : DEFAULT_QUICK_PLACES;
  const frequentRoutes: HomeRouteSuggestion[] = input.recentSearches.slice(0, 3).map((recent) => ({
    title: routeTitle(recent.pickup.label, recent.dropoff.label),
    subtitle: `${recent.seats} seat${recent.seats === 1 ? '' : 's'} · recent search`,
    pickup: recent.pickup,
    dropoff: recent.dropoff,
  }));

  if (frequentRoutes.length === 0 && quickPlaces.length >= 2) {
    frequentRoutes.push({
      title: routeTitle(quickPlaces[0].label, quickPlaces[1].label),
      subtitle: 'Quick route suggestion · edit before searching',
      pickup: quickPlaces[0],
      dropoff: quickPlaces[1],
    });
  }

  return {
    greeting: `Where to, ${firstName(input.displayName)}?`,
    quickPlaces,
    frequentRoutes,
    stats: [
      { label: 'Avg match', value: '92%' },
      { label: 'Saved routes', value: String(frequentRoutes.length) },
      { label: 'CO₂ shared', value: '12kg' },
    ],
  };
}
