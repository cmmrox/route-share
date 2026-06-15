import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { createPassengerRuntimeApi } from '../application/providers';
import { useToast } from '../application/toast';
import { AppText, Button, Card, EmptyState, ErrorState, ListRow, LoadingState, MapBackdrop, Screen, TextField } from '../design-system';
import { toSavedPlaceBody, validateSavedPlace, withDefaultSavedPlace } from '../features/profile';

export function SavedPlacesScreen() {
  const api = useMemo(() => createPassengerRuntimeApi(), []);
  const qc = useQueryClient(); const { showToast } = useToast();
  const places = useQuery({ queryKey: ['saved-places'], queryFn: api.savedPlaces.list });
  const profile = useQuery({ queryKey: ['passenger-profile'], queryFn: api.profile.get });
  const [label,setLabel]=useState(''); const [address,setAddress]=useState(''); const [lat,setLat]=useState('6.9271'); const [lng,setLng]=useState('79.8612');
  const draft = { label, address, location: { latitude: Number(lat), longitude: Number(lng) } };
  const errors = validateSavedPlace(draft);
  const create = useMutation({ mutationFn: () => api.savedPlaces.create(toSavedPlaceBody(draft)), onSuccess: (p) => { qc.setQueryData(['saved-places'], [...(places.data ?? []), p]); setLabel(''); showToast('Saved place added.'); }, onError: () => showToast('Could not save place; retry when online.') });
  const remove = useMutation({ mutationFn: api.savedPlaces.delete, onMutate: async (id: string) => { const prev = places.data ?? []; qc.setQueryData(['saved-places'], prev.filter(p => p.savedPlaceId !== id)); return prev; }, onError: (_e,_id,prev) => qc.setQueryData(['saved-places'], prev) });
  const setDefault = useMutation({ mutationFn: (id: string) => api.profile.update(withDefaultSavedPlace(profile.data, id)), onSuccess: (next) => { qc.setQueryData(['passenger-profile'], next); showToast('Default saved place updated.'); } });
  if (places.isLoading) return <LoadingState label="Loading saved places" />;
  if (places.isError) return <ErrorState title="Saved places unavailable" message="You appear offline or signed out. Manual address entry still works after retry." onRetry={() => void places.refetch()} />;
  return <Screen accessibilityLabel="Saved places"><MapBackdrop /><Card><AppText variant="title">Add a place</AppText><AppText color="#6f6258">Use the map/location picker when permissions are allowed, or enter an address and coordinates manually.</AppText><TextField label="Label" value={label} onChangeText={setLabel} error={errors.label} /><TextField label="Address" value={address} onChangeText={setAddress} error={errors.address} /><TextField label="Latitude" value={lat} onChangeText={setLat} error={errors.location} /><TextField label="Longitude" value={lng} onChangeText={setLng} /><Button accessibilityLabel="Add saved place" disabled={Object.keys(errors).length > 0 || create.isPending} onPress={() => create.mutate()}>Add place</Button></Card>{(places.data ?? []).length === 0 ? <EmptyState title="No saved places yet" message="Add home, work, or a frequent pickup. Offline users can type an address manually." /> : (places.data ?? []).map(place => <ListRow key={place.savedPlaceId} title={place.label} subtitle={`${place.address ?? 'Manual coordinate'} · ${place.location.latitude}, ${place.location.longitude}`} trailing={<Button variant="ghost" accessibilityLabel={`Delete ${place.label}`} onPress={() => remove.mutate(place.savedPlaceId)}>Delete</Button>} onPress={() => setDefault.mutate(place.savedPlaceId)} accessibilityHint="Set as default saved place" />)}</Screen>;
}
