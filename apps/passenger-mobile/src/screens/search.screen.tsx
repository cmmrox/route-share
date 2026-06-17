import * as Location from 'expo-location';
import { useEffect, useMemo, useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, StyleSheet, TextInput, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';

import { createPassengerRuntimeApi } from '../application/providers';
import { passengerRecentSearchRepository } from '../application/passenger-recent-searches';
import type { SavedPlace } from '../api/types';
import { AppText, Button, LoadingState, Screen } from '../design-system';
import {
  buildRideSearchRequest,
  resolveLocationState,
  swapSearchLocations,
  validateRideSearchDraft,
} from '../features/ride-search';
import type { LocationPermissionState, SearchPlace } from '../features/ride-search';
import type { PassengerRootStackParamList } from '../application/navigation';

type SearchScreenProps = NativeStackScreenProps<PassengerRootStackParamList, 'Search'>;
type FieldTarget = 'pickup' | 'dropoff';

const toSearchPlace = (place: { readonly placeId: string; readonly label: string; readonly address?: string; readonly coordinate?: SearchPlace['coordinate'] }): SearchPlace => ({
  label: place.label,
  address: place.address,
  coordinate: place.coordinate,
  placeProviderId: place.placeId,
});

const savedToSearchPlace = (place: SavedPlace): SearchPlace => ({ label: place.label, address: place.address, coordinate: place.location });

async function resolvePlaceCoordinates(place: SearchPlace | undefined): Promise<SearchPlace | undefined> {
  if (!place?.placeProviderId || place.coordinate) return place;
  const details = await createPassengerRuntimeApi().places.details(place.placeProviderId);
  return toSearchPlace(details);
}

export function SearchScreen({ navigation, route }: SearchScreenProps) {
  const [pickupText, setPickupText] = useState('');
  const [dropoffText, setDropoffText] = useState('');
  const [pickup, setPickup] = useState<SearchPlace | undefined>(route.params?.pickup ? { label: 'Selected pickup', address: 'Selected pickup', coordinate: route.params.pickup } : undefined);
  const [dropoff, setDropoff] = useState<SearchPlace | undefined>(route.params?.dropoff ? { label: 'Selected destination', address: 'Selected destination', coordinate: route.params.dropoff } : undefined);
  const [activeField, setActiveField] = useState<FieldTarget>('dropoff');
  const [permission, setPermission] = useState<LocationPermissionState>('unknown');
  const [seats, setSeats] = useState(1);
  const [baseDepartureTime] = useState(() => Date.now());
  const [timeOffsetMinutes, setTimeOffsetMinutes] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [placeLoading, setPlaceLoading] = useState(false);
  const [suggestions, setSuggestions] = useState<SearchPlace[]>([]);
  const [suggestionsOpen, setSuggestionsOpen] = useState(false);
  const [savedPlaces, setSavedPlaces] = useState<SavedPlace[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | undefined>();

  const requestedDepartureTime = useMemo(() => new Date(baseDepartureTime + timeOffsetMinutes * 60_000), [baseDepartureTime, timeOffsetMinutes]);
  const locationState = resolveLocationState({ permission, coordinate: pickup?.label === 'Current location' ? pickup.coordinate : undefined });
  const activeText = activeField === 'pickup' ? pickupText : dropoffText;
  const isPlaceChosen = (place: SearchPlace | undefined): boolean => Boolean(place?.coordinate || place?.placeProviderId);
  const canSearch = isPlaceChosen(pickup) && isPlaceChosen(dropoff);

  useEffect(() => {
    let active = true;
    createPassengerRuntimeApi().savedPlaces.list().then((places) => { if (active) setSavedPlaces(places); }).catch(() => undefined);
    return () => { active = false; };
  }, []);

  // Type-to-search Google Places autocomplete (debounced) for the active field.
  useEffect(() => {
    const query = activeText.trim();
    const handle = setTimeout(async () => {
      if (!suggestionsOpen || query.length < 2) { setSuggestions([]); setPlaceLoading(false); return; }
      setPlaceLoading(true);
      try {
        const center = pickup?.coordinate;
        const places = await createPassengerRuntimeApi().places.autocomplete({ query, latitude: center?.latitude, longitude: center?.longitude });
        setSuggestions(places.map(toSearchPlace));
        setErrorMessage(places.length === 0 ? 'No places found. Try a more specific address.' : undefined);
      } catch {
        setErrorMessage('Google Places search failed. Retry or type a more specific address.');
      } finally {
        setPlaceLoading(false);
      }
    }, 350);
    return () => clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeText, activeField, suggestionsOpen]);

  const requestCurrentLocation = async () => {
    setPermission('requesting');
    try {
      const permissionResult = await Location.requestForegroundPermissionsAsync();
      if (permissionResult.status !== Location.PermissionStatus.GRANTED) { setPermission('denied'); return; }
      const current = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      const currentPlace = { label: 'Current location', address: 'Current GPS location', coordinate: { latitude: current.coords.latitude, longitude: current.coords.longitude } };
      setPickup(currentPlace);
      setPickupText(currentPlace.label);
      setPermission('granted');
    } catch {
      setPermission('unavailable');
    }
  };

  const applyPlace = (target: FieldTarget, place: SearchPlace) => {
    if (target === 'pickup') { setPickup(place); setPickupText(place.label); } else { setDropoff(place); setDropoffText(place.label); }
    setSuggestionsOpen(false);
    setSuggestions([]);
  };

  const swapLocations = () => {
    const swapped = swapSearchLocations({ pickup, dropoff });
    setPickup(swapped.pickup);
    setDropoff(swapped.dropoff);
    setPickupText(swapped.pickup?.label ?? '');
    setDropoffText(swapped.dropoff?.label ?? '');
  };

  const submitSearch = async () => {
    setErrorMessage(undefined);
    setSubmitting(true);
    try {
      const nextPickup = await resolvePlaceCoordinates(pickup ?? (pickupText.trim() ? { label: pickupText.trim(), address: pickupText.trim() } : undefined));
      const nextDropoff = await resolvePlaceCoordinates(dropoff ?? (dropoffText.trim() ? { label: dropoffText.trim(), address: dropoffText.trim() } : undefined));
      // "Now" and short offsets must always resolve to a future departure for backend validation.
      const departAt = new Date(Math.max(requestedDepartureTime.getTime(), Date.now() + 60_000));
      const nextDraft = { pickup: nextPickup, dropoff: nextDropoff, requestedDepartureTime: departAt, seats };
      const nextValidation = validateRideSearchDraft(nextDraft);
      setPickup(nextPickup);
      setDropoff(nextDropoff);
      if (!nextValidation.valid) { setErrorMessage('Choose pickup and destination places with map coordinates before searching.'); return; }
      const results = await createPassengerRuntimeApi().rideSearch.search(buildRideSearchRequest(nextDraft));
      if (nextPickup && nextDropoff) await passengerRecentSearchRepository.save({ pickup: nextPickup, dropoff: nextDropoff, requestedDepartureTime: departAt, seats });
      navigation.navigate('SearchResults', { searchId: 'latest', pickup: nextPickup?.coordinate, dropoff: nextDropoff?.coordinate, results });
    } catch {
      setErrorMessage('Route discovery failed. Retry or edit your search.');
    } finally {
      setSubmitting(false);
    }
  };

  const showSuggestions = suggestionsOpen && activeText.trim().length >= 2;
  const timeChips: { readonly label: string; readonly minutes: number }[] = [
    { label: '◷ Now', minutes: 0 },
    { label: '+30 min', minutes: 30 },
    { label: '+60 min', minutes: 60 },
  ];

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
      <Screen accessibilityLabel="Search shared rides" contentStyle={styles.content}>
        {/* Field card with pickup/dropoff markers + swap */}
        <View style={styles.fieldRow}>
          <Pressable accessibilityRole="button" accessibilityLabel="Back to home" onPress={() => navigation.goBack()} style={styles.backCircle}><AppText variant="title">‹</AppText></Pressable>
          <View style={styles.fieldCard}>
            <View style={styles.fieldLine}>
              <View style={styles.dotTeal} />
              <TextInput
                accessibilityLabel="Pickup location"
                value={pickupText || pickup?.label || ''}
                onFocus={() => setActiveField('pickup')}
                onChangeText={(value) => { setPickupText(value); setPickup(undefined); setActiveField('pickup'); setSuggestionsOpen(true); }}
                placeholder="Pickup — current location or address"
                placeholderTextColor="#9a8d82"
                style={styles.fieldInput}
              />
            </View>
            <View style={styles.fieldDivider} />
            <View style={styles.fieldLine}>
              <View style={styles.squareOrange} />
              <TextInput
                accessibilityLabel="Destination"
                value={dropoffText || dropoff?.label || ''}
                onFocus={() => setActiveField('dropoff')}
                onChangeText={(value) => { setDropoffText(value); setDropoff(undefined); setActiveField('dropoff'); setSuggestionsOpen(true); }}
                placeholder="Where to?"
                placeholderTextColor="#9a8d82"
                style={styles.fieldInput}
              />
            </View>
          </View>
          <Pressable accessibilityRole="button" accessibilityLabel="Swap pickup and destination" accessibilityHint="Swap selected pickup and dropoff locations" onPress={swapLocations} style={styles.swapBtn}><AppText variant="title">⇅</AppText></Pressable>
        </View>

        {/* Time + seat pills */}
        <View style={styles.pillRow}>
          {timeChips.map((chip) => (
            <Pressable key={chip.label} accessibilityRole="button" accessibilityLabel={chip.label} accessibilityState={{ selected: timeOffsetMinutes === chip.minutes }} onPress={() => setTimeOffsetMinutes(chip.minutes)} style={[styles.pill, timeOffsetMinutes === chip.minutes ? styles.pillSelected : null]}>
              <AppText variant="label" color={timeOffsetMinutes === chip.minutes ? '#ffffff' : '#1b1410'}>{chip.label}</AppText>
            </Pressable>
          ))}
          <View style={styles.seatStepper}>
            <Pressable accessibilityRole="button" accessibilityLabel="Decrease seats" onPress={() => setSeats((s) => Math.max(1, s - 1))} style={styles.stepBtn}><AppText variant="title">−</AppText></Pressable>
            <AppText variant="label">{seats} seat{seats === 1 ? '' : 's'}</AppText>
            <Pressable accessibilityRole="button" accessibilityLabel="Increase seats" onPress={() => setSeats((s) => Math.min(4, s + 1))} style={styles.stepBtn}><AppText variant="title">+</AppText></Pressable>
          </View>
        </View>

        {locationState.manualPickupRequired ? <AppText variant="label" color="#b54708">{locationState.message}</AppText> : null}

        {/* SUGGESTIONS (while typing) or SAVED (default) */}
        {showSuggestions ? (
          <View style={styles.section}>
            <AppText variant="label" color="#9a8d82">SUGGESTIONS</AppText>
            {placeLoading ? <LoadingState label="Searching Google Places" /> : null}
            {suggestions.map((suggestion, suggestionIndex) => (
              <Pressable key={suggestion.placeProviderId ?? suggestion.label} nativeID={`placeSuggestion${suggestionIndex}`} testID={`placeSuggestion${suggestionIndex}`} accessibilityRole="button" accessibilityLabel={`Select place ${suggestion.label}`} onPress={() => applyPlace(activeField, suggestion)} style={styles.placeRow}>
                <AppText variant="title" color="#c8612f">📍</AppText>
                <View style={styles.flex}><AppText variant="label">{suggestion.label}</AppText>{suggestion.address ? <AppText color="#9a8d82" numberOfLines={1}>{suggestion.address}</AppText> : null}</View>
              </Pressable>
            ))}
          </View>
        ) : (
          <View style={styles.section}>
            <Pressable accessibilityRole="button" accessibilityLabel="Use current location" accessibilityHint="Set pickup from device GPS" disabled={permission === 'requesting'} onPress={requestCurrentLocation} style={styles.placeRow}>
              <AppText variant="title" color="#0f6e66">◎</AppText>
              <View style={styles.flex}><AppText variant="label" color="#0f6e66">{permission === 'requesting' ? 'Locating…' : 'Use current location'}</AppText></View>
            </Pressable>
            <AppText variant="label" color="#9a8d82">SAVED</AppText>
            {savedPlaces.length === 0 ? <AppText color="#9a8d82">Add Home and Office in Saved Places for one-tap pickup.</AppText> : null}
            {savedPlaces.map((place) => (
              <Pressable key={place.savedPlaceId} accessibilityRole="button" accessibilityLabel={place.label} onPress={() => applyPlace(activeField, savedToSearchPlace(place))} style={styles.placeRow}>
                <AppText variant="title">{place.label === 'Home' ? '🏠' : place.label === 'Office' ? '💼' : '⭐'}</AppText>
                <View style={styles.flex}><AppText variant="label">{place.label}</AppText>{place.address ? <AppText color="#9a8d82" numberOfLines={1}>{place.address}</AppText> : null}</View>
              </Pressable>
            ))}
          </View>
        )}

        {errorMessage ? <AppText variant="label" color="#b3261e">{errorMessage}</AppText> : null}
      </Screen>
      <View style={styles.footer}>
        <Button accessibilityLabel="Search for shared rides" accessibilityHint="Create a route discovery search" disabled={!canSearch || submitting || placeLoading} onPress={submitSearch}>{submitting ? 'Searching…' : 'Search shared rides'}</Button>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  content: { gap: 16 },
  fieldRow: { alignItems: 'center', flexDirection: 'row', gap: 8 },
  backCircle: { alignItems: 'center', backgroundColor: '#f4ece2', borderRadius: 18, height: 36, justifyContent: 'center', width: 36 },
  fieldCard: { backgroundColor: '#ffffff', borderColor: '#eadfce', borderRadius: 14, borderWidth: 1, flex: 1, paddingHorizontal: 12 },
  fieldLine: { alignItems: 'center', flexDirection: 'row', gap: 12, minHeight: 48 },
  fieldDivider: { backgroundColor: '#f0e7da', height: 1, marginLeft: 22 },
  fieldInput: { color: '#1b1410', flex: 1, fontSize: 15, paddingVertical: 8 },
  dotTeal: { backgroundColor: '#0f6e66', borderRadius: 5, height: 10, width: 10 },
  squareOrange: { backgroundColor: '#c8612f', borderRadius: 2, height: 10, width: 10 },
  swapBtn: { alignItems: 'center', backgroundColor: '#f4ece2', borderRadius: 18, height: 36, justifyContent: 'center', width: 36 },
  pillRow: { alignItems: 'center', flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  pill: { backgroundColor: '#f4ece2', borderRadius: 999, minHeight: 36, justifyContent: 'center', paddingHorizontal: 14 },
  pillSelected: { backgroundColor: '#1b1410' },
  seatStepper: { alignItems: 'center', backgroundColor: '#f4ece2', borderRadius: 999, flexDirection: 'row', gap: 10, minHeight: 36, paddingHorizontal: 8 },
  stepBtn: { alignItems: 'center', height: 32, justifyContent: 'center', width: 32 },
  section: { gap: 8 },
  placeRow: { alignItems: 'center', flexDirection: 'row', gap: 12, minHeight: 48, paddingVertical: 6 },
  footer: { backgroundColor: '#faf7f2', borderTopColor: '#eee3d4', borderTopWidth: 1, padding: 16 },
});
