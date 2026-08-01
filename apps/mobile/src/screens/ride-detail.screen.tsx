import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useMemo, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { createPassengerRuntimeApi } from '../application/providers';
import type { Coordinate } from '../api/types';
import {
  AppText,
  Avatar,
  Button,
  Card,
  EmptyState,
  FareRow,
  MapBackdrop,
  MapOverlayCard,
  MatchRing,
  RouteTimeline,
  Screen,
  routeShareTokens as t,
} from '../design-system';
import { toRideResultModel } from '../features/ride-results';
import type { PassengerRootStackParamList } from '../application/navigation';

type Props = NativeStackScreenProps<PassengerRootStackParamList, 'RideDetail'>;

export function RideDetailScreen({ navigation, route }: Props) {
  const result = route.params?.result;
  const model = useMemo(() => (result ? toRideResultModel(result) : undefined), [result]);
  const pickupLat = route.params?.pickup?.latitude;
  const pickupLng = route.params?.pickup?.longitude;
  const dropLat = route.params?.dropoff?.latitude;
  const dropLng = route.params?.dropoff?.longitude;
  const [routeCoordinates, setRouteCoordinates] = useState<Coordinate[] | undefined>(undefined);

  const occurrenceId = result?.routeOccurrenceId;
  const pickupFraction = result?.pickupRouteFraction;
  const dropoffFraction = result?.dropoffRouteFraction;

  useEffect(() => {
    let active = true;
    const api = createPassengerRuntimeApi().places;
    const loadFallbackDirections = async (): Promise<Coordinate[]> => {
      if (pickupLat == null || pickupLng == null || dropLat == null || dropLng == null) return [];
      return api.directions({ originLat: pickupLat, originLng: pickupLng, destLat: dropLat, destLng: dropLng });
    };
    const load = async (): Promise<Coordinate[]> => {
      // Prefer the stored driver route segment: it is the road the driver actually drives and it
      // costs nothing, unlike the Google-billed directions fallback.
      if (occurrenceId && pickupFraction != null && dropoffFraction != null && pickupFraction < dropoffFraction) {
        try {
          const coords = await api.routeGeometry({ routeOccurrenceId: occurrenceId, pickupFraction, dropoffFraction });
          if (coords.length >= 2) return coords;
        } catch {
          // fall through to the directions fallback below
        }
      }
      return loadFallbackDirections();
    };
    load()
      .then((coords) => { if (active && coords.length >= 2) setRouteCoordinates(coords); })
      .catch(() => { /* map still renders without the route overlay */ });
    return () => { active = false; };
  }, [occurrenceId, pickupFraction, dropoffFraction, pickupLat, pickupLng, dropLat, dropLng]);

  if (!model) {
    return (
      <Screen accessibilityLabel="Ride details">
        <EmptyState title="Ride unavailable" message="This ride could not be loaded. Return to the results and pick a ride again." />
        <Button accessibilityLabel="Back to results" accessibilityHint="Return to the ride list" onPress={() => navigation.goBack()}>Back to results</Button>
      </Screen>
    );
  }

  const continueToBooking = () => {
    navigation.navigate('SeatSelection', {
      searchId: route.params?.searchId ?? 'latest',
      resultId: model.resultId,
      result: model.source,
      pickup: route.params?.pickup,
      dropoff: route.params?.dropoff,
      seats: route.params?.seats,
    });
  };

  return (
    <Screen accessibilityLabel="Ride details">
      <View style={styles.headerRow}>
        <Pressable accessibilityRole="button" accessibilityLabel="Back to results" onPress={() => navigation.goBack()} style={styles.backCircle}>
          <AppText variant="title">‹</AppText>
        </Pressable>
        <View style={styles.flex}>
          <AppText variant="label" color={t.colors.accent}>RIDE DETAILS</AppText>
          <AppText variant="title" numberOfLines={1}>{model.originLabel} → {model.destinationLabel}</AppText>
        </View>
      </View>

      <MapBackdrop showRoute routeCoordinates={routeCoordinates}>
        <MapOverlayCard>
          <AppText variant="label">{model.departureLabel}</AppText>
          <AppText color={t.colors.ink3}>{model.walkLabel}</AppText>
        </MapOverlayCard>
      </MapBackdrop>

      <Card style={styles.section}>
        <View style={styles.driverRow}>
          <Avatar name={model.driverName} size={56} />
          <View style={styles.flex}>
            <AppText variant="title">{model.driverName}</AppText>
            <AppText color={t.colors.ink3}>{model.vehicleLabel}{model.vehicleRegistration ? ` · ${model.vehicleRegistration}` : ''}</AppText>
          </View>
          <MatchRing value={model.matchPercent} />
        </View>
      </Card>

      <Card style={styles.section}>
        <AppText variant="label" color={t.colors.ink3}>TRIP</AppText>
        <RouteTimeline
          stops={[
            { label: model.originLabel, detail: 'Pickup', tone: 'pickup' },
            { label: model.destinationLabel, detail: 'Drop-off', tone: 'dropoff' },
          ]}
        />
        <View style={styles.metaRow}>
          <AppText variant="label">{model.seatsLabel}</AppText>
          <AppText variant="label" color={t.colors.teal}>{model.matchTierLabel}</AppText>
        </View>
      </Card>

      <Card style={styles.section}>
        <AppText variant="label" color={t.colors.ink3}>FARE ESTIMATE</AppText>
        <FareRow label="Estimated fare" amount={model.fareLabel} emphasized />
        <AppText color={t.colors.ink3}>Final fare is confirmed at drop-off based on the actual distance you travel.</AppText>
      </Card>

      {model.explanation ? (
        <Card style={styles.section}>
          <AppText variant="label" color={t.colors.ink3}>WHY THIS IS A GOOD MATCH</AppText>
          <AppText>{model.explanation}</AppText>
        </Card>
      ) : null}

      <Card style={styles.section}>
        <AppText variant="label" color={t.colors.ink3}>SAFETY & POLICY</AppText>
        <AppText color={t.colors.ink3}>Verified driver and vehicle. Share your trip with a trusted contact and use SOS any time during the ride. Free cancellation before the driver starts the trip.</AppText>
      </Card>

      <Button accessibilityLabel="Continue to seat selection" accessibilityHint="Choose seats and book this ride" onPress={continueToBooking}>
        Continue · {model.fareLabel}
      </Button>
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  headerRow: { flexDirection: 'row', alignItems: 'center', gap: t.spacing.md, marginBottom: t.spacing.md },
  backCircle: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center', backgroundColor: t.colors.surface2 },
  section: { gap: t.spacing.sm, marginTop: t.spacing.md },
  driverRow: { flexDirection: 'row', alignItems: 'center', gap: t.spacing.md },
  metaRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: t.spacing.sm },
});
