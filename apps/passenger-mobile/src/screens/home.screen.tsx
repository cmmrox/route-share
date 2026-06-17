import { useEffect, useMemo, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';

import { createPassengerRuntimeApi } from '../application/providers';
import { passengerRecentSearchRepository } from '../application/passenger-recent-searches';
import { usePassengerPreferencesStore } from '../application/preferences';
import { useToast } from '../application/toast';
import type { SavedPlace } from '../api/types';
import { AppText, BottomSheet, Button, IconButton, LoadingState, MapBackdrop, SosButton, StatCard } from '../design-system';
import { buildHomeDashboardModel } from '../features/ride-search';
import type { RecentSearch, SearchPlace } from '../features/ride-search';
import type { PassengerRootStackParamList } from '../application/navigation';

type HomeScreenProps = NativeStackScreenProps<PassengerRootStackParamList, 'Home'>;

const QUICK_ICONS: Record<string, string> = { Home: '🏠', Office: '💼', Gym: '⭐', 'Mum\'s place': '📍' };

export function HomeScreen({ navigation }: HomeScreenProps) {
  const { showToast } = useToast();
  const homeVariant = usePassengerPreferencesStore((state) => state.homeVariant);
  const [savedPlaces, setSavedPlaces] = useState<SavedPlace[]>([]);
  const [recentSearches, setRecentSearches] = useState<RecentSearch[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | undefined>();

  useEffect(() => {
    let active = true;
    async function loadHomeData() {
      setLoading(true);
      setLoadError(undefined);
      try {
        const [places, recents] = await Promise.all([createPassengerRuntimeApi().savedPlaces.list(), passengerRecentSearchRepository.list()]);
        if (!active) return;
        setSavedPlaces(places);
        setRecentSearches(recents);
      } catch {
        if (!active) return;
        setLoadError('Saved places could not be loaded. You can still search manually.');
        setRecentSearches(await passengerRecentSearchRepository.list());
      } finally {
        if (active) setLoading(false);
      }
    }
    void loadHomeData();
    return () => {
      active = false;
    };
  }, []);

  const model = useMemo(() => buildHomeDashboardModel({ displayName: 'Nimali', savedPlaces, recentSearches }), [recentSearches, savedPlaces]);
  const openSearch = (pickup?: SearchPlace, dropoff?: SearchPlace) => navigation.navigate('Search', { pickup: pickup?.coordinate, dropoff: dropoff?.coordinate });

  return (
    <View style={styles.root}>
      <MapBackdrop showRoute={false}>
        <View style={styles.topBar}>
          <IconButton accessibilityLabel="Open menu" accessibilityHint="Shows account, trips, payments and saved places" icon={<AppText variant="title">☰</AppText>} onPress={() => navigation.navigate('Account')} />
          <Button variant="secondary" accessibilityLabel="Safety" accessibilityHint="Open the safety toolkit" onPress={() => navigation.navigate('Safety')}>◇ Safety</Button>
        </View>
        <View style={styles.currentPin}>
          <View style={styles.currentPinCore} />
        </View>
        <View style={styles.sosFloating}><SosButton onPress={() => navigation.navigate('Safety')} /></View>
      </MapBackdrop>

      <BottomSheet accessibilityLabel="Where to - Find a shared ride" style={styles.sheet}>
        <AppText variant="display">{model.greeting}</AppText>

        <Pressable accessibilityRole="button" accessibilityLabel="Enter destination" accessibilityHint="Open destination and route search" onPress={() => openSearch()} style={styles.searchBar}>
          <AppText color="#9a8d82">🔎  Enter destination</AppText>
          <View style={styles.nowPill}><AppText variant="label" color="#1b1410">◷ Now</AppText></View>
        </Pressable>

        {loading ? <LoadingState label="Loading saved places" /> : null}
        {loadError ? <AppText variant="label" color="#b54708">{loadError}</AppText> : null}

        {homeVariant === 'dashboard' ? (
          <View style={styles.statsRow}>
            {model.stats.map((stat) => <StatCard key={stat.label} label={stat.label} value={stat.value} />)}
          </View>
        ) : null}

        <View style={styles.quickRow}>
          {model.quickPlaces.slice(0, 2).map((place) => (
            <Pressable key={place.label} accessibilityRole="button" accessibilityLabel={`Use ${place.label}`} accessibilityHint="Use this saved place in route search" onPress={() => openSearch(place)} style={styles.quickCard}>
              <AppText variant="title">{QUICK_ICONS[place.label] ?? '📍'}</AppText>
              <View style={styles.flex}>
                <AppText variant="label">{place.label}</AppText>
                {place.address ? <AppText color="#9a8d82" numberOfLines={1}>{place.address}</AppText> : null}
              </View>
            </Pressable>
          ))}
        </View>

        <View style={styles.sectionHeader}>
          <AppText variant="label" color="#9a8d82">FREQUENT ROUTES</AppText>
          <Button variant="ghost" accessibilityLabel="See all frequent routes" onPress={() => showToast('Full route history is scheduled for Trip History')} style={styles.seeAll}>See all</Button>
        </View>
        {model.frequentRoutes.map((route) => (
          <Pressable key={route.title} accessibilityRole="button" accessibilityLabel={route.title} accessibilityHint="Reuse this route in search" onPress={() => openSearch(route.pickup, route.dropoff)} style={styles.routeRow}>
            <AppText variant="title" color="#9a8d82">🕘</AppText>
            <View style={styles.flex}>
              <AppText variant="label">{route.title}</AppText>
              <AppText color="#9a8d82">{route.subtitle}</AppText>
            </View>
          </Pressable>
        ))}
      </BottomSheet>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  root: { backgroundColor: '#faf7f2', flex: 1 },
  topBar: { flexDirection: 'row', justifyContent: 'space-between', left: 16, position: 'absolute', right: 16, top: 16 },
  currentPin: { alignItems: 'center', backgroundColor: 'rgba(15,110,102,0.18)', borderRadius: 22, height: 44, justifyContent: 'center', left: '47%', position: 'absolute', top: '36%', width: 44 },
  currentPinCore: { backgroundColor: '#0f6e66', borderColor: '#ffffff', borderRadius: 10, borderWidth: 3, height: 20, width: 20 },
  sosFloating: { bottom: 18, position: 'absolute', right: 18 },
  sheet: { flex: 1, gap: 14, marginTop: -34 },
  searchBar: { alignItems: 'center', backgroundColor: '#f4ece2', borderRadius: 14, flexDirection: 'row', justifyContent: 'space-between', minHeight: 52, paddingHorizontal: 16, paddingVertical: 12 },
  nowPill: { backgroundColor: '#ffffff', borderRadius: 999, paddingHorizontal: 12, paddingVertical: 6 },
  quickRow: { flexDirection: 'row', gap: 10 },
  quickCard: { alignItems: 'center', backgroundColor: '#ffffff', borderColor: '#eadfce', borderRadius: 14, borderWidth: 1, flex: 1, flexDirection: 'row', gap: 10, padding: 12 },
  sectionHeader: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' },
  seeAll: { paddingHorizontal: 0, paddingVertical: 0 },
  routeRow: { alignItems: 'center', flexDirection: 'row', gap: 12, paddingVertical: 8 },
  statsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
});
