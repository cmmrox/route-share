import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';

import {
  AppText,
  Avatar,
  Card,
  Chip,
  EmptyState,
  MapBackdrop,
  MapOverlayCard,
  MatchRing,
  Screen,
  routeShareTokens as t,
} from '../design-system';
import {
  applyRideFilters,
  defaultRideFilters,
  groupRideResults,
  toRideResultModels,
  type RideFilters,
  type RideResultModel,
  type RideSort,
} from '../features/ride-results';
import type { PassengerRootStackParamList } from '../application/navigation';

type Props = NativeStackScreenProps<PassengerRootStackParamList, 'SearchResults'>;
type ViewMode = 'list' | 'map' | 'grouped';

const SORT_OPTIONS: readonly { readonly id: RideSort; readonly label: string }[] = [
  { id: 'best-match', label: 'Best match' },
  { id: 'price-low', label: 'Lowest price' },
  { id: 'departure-early', label: 'Departs soon' },
  { id: 'seats-most', label: 'Most seats' },
];

const MATCH_THRESHOLDS: readonly { readonly id: number; readonly label: string }[] = [
  { id: 0, label: 'All matches' },
  { id: 45, label: '45%+' },
  { id: 75, label: '75%+' },
  { id: 95, label: 'Full only' },
];

export function SearchResultsScreen({ navigation, route }: Props) {
  const models = useMemo(() => toRideResultModels(route.params?.results ?? []), [route.params?.results]);
  const [mode, setMode] = useState<ViewMode>('list');
  const [filters, setFilters] = useState<RideFilters>(defaultRideFilters);

  const visible = useMemo(() => applyRideFilters(models, filters), [models, filters]);
  const groups = useMemo(() => groupRideResults(visible), [visible]);

  const openDetail = (model: RideResultModel) => {
    navigation.navigate('RideDetail', {
      searchId: route.params?.searchId ?? 'latest',
      resultId: model.resultId,
      result: model.source,
      pickup: route.params?.pickup,
      dropoff: route.params?.dropoff,
      seats: route.params?.seats,
    });
  };

  const setSort = (sort: RideSort) => setFilters((f) => ({ ...f, sort }));
  const setMinMatch = (minMatchPercent: number) => setFilters((f) => ({ ...f, minMatchPercent }));

  return (
    <Screen accessibilityLabel="Available rides">
      <View style={styles.headerRow}>
        <Pressable accessibilityRole="button" accessibilityLabel="Back to search" onPress={() => navigation.goBack()} style={styles.backCircle}>
          <AppText variant="title">‹</AppText>
        </Pressable>
        <View style={styles.flex}>
          <AppText variant="label" color={t.colors.accent}>AVAILABLE RIDES</AppText>
          <AppText variant="title" numberOfLines={1}>
            {(route.params?.results?.[0]?.originLabel ?? 'Your route')} → {route.params?.results?.[0]?.destinationLabel ?? 'destination'}
          </AppText>
          <AppText color={t.colors.ink3}>{visible.length} of {models.length} rides match your filters</AppText>
        </View>
      </View>

      <View accessibilityRole="tablist" style={styles.toggleRow}>
        {(['list', 'map', 'grouped'] as const).map((m) => (
          <Chip key={m} selected={mode === m} accessibilityLabel={`${m} view`} accessibilityHint={`Show rides as ${m}`} onPress={() => setMode(m)}>
            {m === 'list' ? 'List' : m === 'map' ? 'Map' : 'Grouped'}
          </Chip>
        ))}
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterRow} accessibilityLabel="Sort rides">
        {SORT_OPTIONS.map((s) => (
          <Chip key={s.id} selected={filters.sort === s.id} accessibilityLabel={`Sort by ${s.label}`} accessibilityHint="Reorder ride results" onPress={() => setSort(s.id)}>{s.label}</Chip>
        ))}
      </ScrollView>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterRow} accessibilityLabel="Filter by match quality">
        {MATCH_THRESHOLDS.map((m) => (
          <Chip key={m.id} selected={filters.minMatchPercent === m.id} accessibilityLabel={`Minimum match ${m.label}`} accessibilityHint="Filter ride results by route match" onPress={() => setMinMatch(m.id)}>{m.label}</Chip>
        ))}
      </ScrollView>

      {visible.length === 0 ? (
        <EmptyState title="No rides match yet" message="Try widening your filters, adjusting departure time, or searching a nearby pickup." />
      ) : mode === 'map' ? (
        <ResultsMap models={visible} onSelect={openDetail} />
      ) : mode === 'grouped' ? (
        <View>
          {groups.map((group) => (
            <View key={group.tier} style={styles.group}>
              <AppText variant="label" color={t.colors.ink3}>{group.title.toUpperCase()} · {group.items.length}</AppText>
              {group.items.map((model) => (
                <CompactRow key={model.resultId} model={model} onPress={() => openDetail(model)} />
              ))}
            </View>
          ))}
        </View>
      ) : (
        <View>
          {visible.map((model) => (
            <RideCard key={model.resultId} model={model} onPress={() => openDetail(model)} />
          ))}
        </View>
      )}
    </Screen>
  );
}

function RideCard({ model, onPress }: { readonly model: RideResultModel; readonly onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityLabel={`${model.driverName}, ${model.matchPercent} percent match, ${model.fareLabel}, ${model.seatsLabel}`} accessibilityHint="Open ride details" onPress={onPress}>
      <Card style={styles.card}>
        <View style={styles.cardTop}>
          <MatchRing value={model.matchPercent} />
          <View style={styles.flex}>
            <AppText variant="title" numberOfLines={1}>{model.driverName}</AppText>
            <AppText color={t.colors.ink3} numberOfLines={1}>{model.vehicleLabel}{model.vehicleRegistration ? ` · ${model.vehicleRegistration}` : ''}</AppText>
          </View>
          <Avatar name={model.driverName} />
        </View>
        <View style={styles.cardMeta}>
          <AppText variant="label">{model.departureLabel}</AppText>
          <AppText variant="label" color={t.colors.teal}>{model.seatsLabel}</AppText>
        </View>
        <AppText color={t.colors.ink3} numberOfLines={1}>{model.originLabel} → {model.destinationLabel}</AppText>
        <AppText color={t.colors.ink3} numberOfLines={1}>{model.walkLabel}</AppText>
        <View style={styles.cardMeta}>
          <AppText variant="label" color={t.colors.ink3}>{model.matchTierLabel}</AppText>
          <AppText variant="title" color={t.colors.accent}>{model.fareLabel}</AppText>
        </View>
      </Card>
    </Pressable>
  );
}

function CompactRow({ model, onPress }: { readonly model: RideResultModel; readonly onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityLabel={`${model.driverName}, ${model.fareLabel}, departs ${model.departureLabel}`} accessibilityHint="Open ride details" onPress={onPress} style={styles.compact}>
      <MatchRing value={model.matchPercent} size={44} />
      <View style={styles.flex}>
        <AppText variant="label" numberOfLines={1}>{model.driverName} · {model.departureLabel}</AppText>
        <AppText color={t.colors.ink3} numberOfLines={1}>{model.seatsLabel} · {model.vehicleLabel}</AppText>
      </View>
      <AppText variant="label" color={t.colors.accent}>{model.fareLabel}</AppText>
    </Pressable>
  );
}

function ResultsMap({ models, onSelect }: { readonly models: readonly RideResultModel[]; readonly onSelect: (model: RideResultModel) => void }) {
  return (
    <View>
      <MapBackdrop showRoute>
        <MapOverlayCard>
          <AppText variant="label" color={t.colors.ink3}>{models.length} rides on the map</AppText>
          <AppText color={t.colors.ink3}>Swipe the cards below to preview each ride; tap to open details.</AppText>
        </MapOverlayCard>
      </MapBackdrop>
      <ScrollView horizontal pagingEnabled showsHorizontalScrollIndicator={false} contentContainerStyle={styles.mapCards} accessibilityLabel="Ride cards (accessible list fallback)">
        {models.map((model) => (
          <View key={model.resultId} style={styles.mapCard}>
            <RideCard model={model} onPress={() => onSelect(model)} />
          </View>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  headerRow: { flexDirection: 'row', alignItems: 'center', gap: t.spacing.md, marginBottom: t.spacing.md },
  backCircle: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center', backgroundColor: t.colors.surface2 },
  toggleRow: { flexDirection: 'row', gap: t.spacing.sm, marginBottom: t.spacing.sm },
  filterRow: { flexDirection: 'row', gap: t.spacing.sm, paddingVertical: t.spacing.xs },
  group: { gap: t.spacing.sm, marginBottom: t.spacing.md },
  card: { gap: t.spacing.sm, marginBottom: t.spacing.md },
  cardTop: { flexDirection: 'row', alignItems: 'center', gap: t.spacing.md },
  cardMeta: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  compact: { flexDirection: 'row', alignItems: 'center', gap: t.spacing.md, paddingVertical: t.spacing.sm },
  mapCards: { gap: t.spacing.md, paddingVertical: t.spacing.md },
  mapCard: { width: 300 },
});
