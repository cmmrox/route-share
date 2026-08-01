import { useEffect, useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { createPassengerRuntimeApi } from '../application/providers';
import { passengerRecentSearchRepository } from '../application/passenger-recent-searches';
import { useToast } from '../application/toast';
import type { RecentSearch } from '../features/ride-search';
import { AppText, Button, Card, EmptyState, ErrorState, ListRow, LoadingState, Screen, TextField } from '../design-system';
import { toSavedPlaceBody, validateSavedPlace, withDefaultSavedPlace } from '../features/profile';

const ICONS: Record<string, string> = { Home: '🏠', Office: '💼', Gym: '⭐' };

export function SavedPlacesScreen() {
  const api = useMemo(() => createPassengerRuntimeApi(), []);
  const qc = useQueryClient();
  const { showToast } = useToast();
  const places = useQuery({ queryKey: ['saved-places'], queryFn: api.savedPlaces.list });
  const profile = useQuery({ queryKey: ['passenger-profile'], queryFn: api.profile.get });
  const [showForm, setShowForm] = useState(false);
  const [recents, setRecents] = useState<RecentSearch[]>([]);
  const [label, setLabel] = useState('');
  const [address, setAddress] = useState('');
  const [lat, setLat] = useState('6.9271');
  const [lng, setLng] = useState('79.8612');
  const draft = { label, address, location: { latitude: Number(lat), longitude: Number(lng) } };
  const errors = validateSavedPlace(draft);

  useEffect(() => {
    let active = true;
    passengerRecentSearchRepository.list().then((r) => { if (active) setRecents(r); }).catch(() => undefined);
    return () => { active = false; };
  }, []);

  const create = useMutation({
    mutationFn: () => api.savedPlaces.create(toSavedPlaceBody(draft)),
    onSuccess: (p) => { qc.setQueryData(['saved-places'], [...(places.data ?? []), p]); setLabel(''); setAddress(''); setShowForm(false); showToast('Saved place added.'); },
    onError: () => showToast('Could not save place; retry when online.'),
  });
  const remove = useMutation({
    mutationFn: api.savedPlaces.delete,
    onMutate: async (id: string) => { const prev = places.data ?? []; qc.setQueryData(['saved-places'], prev.filter((p) => p.savedPlaceId !== id)); return prev; },
    onError: (_e, _id, prev) => qc.setQueryData(['saved-places'], prev),
  });
  const setDefault = useMutation({
    mutationFn: (id: string) => api.profile.update(withDefaultSavedPlace(profile.data, id)),
    onSuccess: (next) => { qc.setQueryData(['passenger-profile'], next); showToast('Default saved place updated.'); },
  });

  const clearRecents = async () => { await passengerRecentSearchRepository.clear(); setRecents([]); showToast('Recent searches cleared'); };

  if (places.isLoading) return <LoadingState label="Loading saved places" />;
  if (places.isError) return <ErrorState title="Saved places unavailable" message="You appear offline or signed out. Manual address entry still works after retry." onRetry={() => void places.refetch()} />;

  const list = places.data ?? [];
  return (
    <Screen accessibilityLabel="Saved places">
      <AppText variant="display">Saved places</AppText>

      {list.length === 0 ? (
        <EmptyState title="No saved places yet" message="Add home, work, or a frequent pickup. Offline users can type an address manually." />
      ) : (
        <Card>
          {list.map((place) => (
            <ListRow
              key={place.savedPlaceId}
              title={place.label}
              subtitle={place.address ?? `${place.location.latitude}, ${place.location.longitude}`}
              leading={<AppText variant="title">{ICONS[place.label] ?? '📍'}</AppText>}
              trailing={<Button variant="ghost" accessibilityLabel={`Delete ${place.label}`} onPress={() => remove.mutate(place.savedPlaceId)}>⋯</Button>}
              onPress={() => setDefault.mutate(place.savedPlaceId)}
              accessibilityHint="Set as default saved place"
            />
          ))}
        </Card>
      )}

      {showForm ? (
        <Card style={styles.formGap}>
          <AppText variant="title">Add a place</AppText>
          <AppText color="#6f6258">Enter an address and coordinates. Map picker is used when location permission is granted.</AppText>
          <TextField label="LABEL" value={label} onChangeText={setLabel} error={errors.label} />
          <TextField label="ADDRESS" value={address} onChangeText={setAddress} error={errors.address} />
          <TextField label="LATITUDE" value={lat} onChangeText={setLat} error={errors.location} />
          <TextField label="LONGITUDE" value={lng} onChangeText={setLng} />
          <Button accessibilityLabel="Save place" disabled={Object.keys(errors).length > 0 || create.isPending} onPress={() => create.mutate()}>{create.isPending ? 'Saving…' : 'Save place'}</Button>
        </Card>
      ) : (
        <Button variant="secondary" accessibilityLabel="Add new place" accessibilityHint="Show the add place form" onPress={() => setShowForm(true)}>+ Add new place</Button>
      )}

      {recents.length > 0 ? (
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <AppText variant="label" color="#9a8d82">RECENT SEARCHES</AppText>
            <Button variant="ghost" accessibilityLabel="Clear recent searches" onPress={() => void clearRecents()} style={styles.clearBtn}>Clear</Button>
          </View>
          {recents.map((recent) => (
            <ListRow key={recent.id} title={`${recent.pickup.label} → ${recent.dropoff.label}`} subtitle={`${recent.seats} seat${recent.seats === 1 ? '' : 's'}`} leading={<AppText variant="title" color="#9a8d82">🕘</AppText>} />
          ))}
        </View>
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  formGap: { gap: 12 },
  section: { gap: 4 },
  sectionHeader: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' },
  clearBtn: { paddingHorizontal: 0, paddingVertical: 0 },
});
