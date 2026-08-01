import { View } from 'react-native';

import { AppText, Button, Card, EmptyState, LoadingState, MapBackdrop, RouteTimeline, Screen, ToastView } from '../design-system';
import type { PassengerRouteName } from './navigation';

type ShellScreenProps = {
  readonly route?: { readonly name?: PassengerRouteName; readonly params?: unknown };
  readonly navigation?: { readonly goBack?: () => void; readonly canGoBack?: () => boolean };
};

const titles: Record<PassengerRouteName, string> = {
  Splash: 'Preparing RouteShare',
  Onboarding: 'Welcome to RouteShare',
  Login: 'Sign in',
  Otp: 'Verify OTP',
  ProfileSetup: 'Complete your profile',
  Home: 'RouteShare Passenger',
  Search: 'Search rides',
  SearchResults: 'Available rides',
  RideDetail: 'Ride details',
  SeatSelection: 'Select seats',
  Payment: 'Payment',
  BookedWaiting: 'Booking confirmed',
  InTrip: 'In trip',
  ExitEarly: 'Exit early',
  Receipt: 'Receipt',
  RateDriver: 'Rate driver',
  TripHistory: 'Trip history',
  SavedPlaces: 'Saved places',
  TrustedContacts: 'Trusted contacts',
  Verification: 'Verification',
  Account: 'Account',
  Safety: 'Safety',
  ShareTrip: 'Share trip',
  Notifications: 'Notifications',
  Support: 'Support',
};

export function AppShellPlaceholderScreen({ route, navigation }: ShellScreenProps) {
  const routeName = route?.name ?? 'Home';
  const canGoBack = navigation?.canGoBack?.() ?? false;
  const params = route?.params ? JSON.stringify(route.params) : undefined;

  return (
    <Screen accessibilityLabel={`${titles[routeName]} RouteShare screen`}>
      <MapBackdrop showRoute={routeName === 'RideDetail' || routeName === 'InTrip'} />
      <Card accessibilityLabel="RouteShare screen status">
        <AppText variant="label" color="#c55a2d">PASSENGER EXPERIENCE</AppText>
        <AppText variant="display">{titles[routeName]}</AppText>
        <AppText color="#6f6258">Reusable warm RouteShare components, source-asset tokens, accessible states, and safe touch targets are now wired into the app shell while feature-specific flows are completed.</AppText>
        {params ? <AppText variant="mono" color="#6f6258">{params}</AppText> : null}
      </Card>
      <RouteTimeline stops={[{ label: 'Search your route', detail: 'Choose pickup and drop off', tone: 'pickup' }, { label: 'Compare trusted matches', detail: 'Review overlap, seats and fare', tone: 'stop' }, { label: 'Book and share trip', detail: 'Payment, SOS and live status components available', tone: 'dropoff' }]} />
      {canGoBack ? <Button accessibilityLabel="Go back" accessibilityHint="Return to the previous RouteShare screen" onPress={() => navigation?.goBack?.()}>Back</Button> : <EmptyState title="Ready for screen slice" message="This route uses the production design system instead of the plain scaffold." />}
    </Screen>
  );
}

export function OfflineBanner({ online }: { readonly online: boolean }) {
  if (online) return null;
  return (
    <View style={{ left: 0, position: 'absolute', right: 0, top: 0, zIndex: 10 }}>
      <ToastView tone="warn" message="You are offline. Cached safe actions remain available; bookings and payments need connection." />
    </View>
  );
}

export { LoadingState };
