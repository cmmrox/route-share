import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useMemo } from 'react';
import { View } from 'react-native';
import { useQuery } from '@tanstack/react-query';

import type { PassengerRootStackParamList } from '../application/navigation';
import { createPassengerRuntimeApi } from '../application/providers';
import { AppText, Avatar, Button, Card, ListRow, LoadingState, Screen, StatCard } from '../design-system';

type Props = NativeStackScreenProps<PassengerRootStackParamList, 'Account'>;
export function AccountScreen({ navigation }: Props) {
  const api = useMemo(() => createPassengerRuntimeApi(), []);
  const profile = useQuery({ queryKey: ['passenger-profile'], queryFn: api.profile.get, retry: false });
  if (profile.isLoading) return <LoadingState label="Loading account" />;
  const name = profile.data?.fullName ?? profile.data?.displayName ?? 'Passenger';
  return (
    <Screen accessibilityLabel="Account">
      <Card style={{ backgroundColor: '#d66a3b', borderColor: '#d66a3b', gap: 14 }}>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Button variant="ghost" accessibilityLabel="Back" onPress={() => navigation.goBack()}>←</Button>
          <Button variant="ghost" accessibilityLabel="Settings" onPress={() => navigation.navigate('ProfileSetup')}>⚙</Button>
        </View>
        <View style={{ alignItems: 'center', gap: 8 }}>
          <Avatar name={name} size={72} />
          <AppText variant="display" color="#ffffff">{name}</AppText>
          <AppText color="#fff7f0">★ 4.9 · 28 trips</AppText>
          <AppText color="#fff7f0">{profile.data?.email ?? 'Add email in profile setup'}</AppText>
        </View>
      </Card>
      <View style={{ flexDirection: 'row', gap: 12 }}>
        <StatCard label="Wallet" value="Rs 0" tone="accent" />
        <StatCard label="Saved" value="12%" tone="success" />
      </View>
      <Card>
        <ListRow title="Saved places" subtitle="Home, office and favorite stops" leading={<AppText variant="title">📍</AppText>} onPress={() => navigation.navigate('SavedPlaces')} />
        <ListRow title="Payment methods" subtitle="Cards, wallets and cash preferences" leading={<AppText variant="title">💳</AppText>} onPress={() => navigation.navigate('Payment', { bookingId: 'preview' })} />
        <ListRow title="Trip history" subtitle="Past rides and receipts" leading={<AppText variant="title">🧾</AppText>} onPress={() => navigation.navigate('TripHistory')} />
        <ListRow title="Notifications" subtitle="Ride alerts and promotions" leading={<AppText variant="title">🔔</AppText>} onPress={() => navigation.navigate('Notifications')} />
      </Card>
      <Card>
        <AppText variant="label" color="#9a8d82">SAFETY</AppText>
        <ListRow title="Trusted contacts" subtitle="People shown in SOS and trip sharing" leading={<AppText variant="title">👥</AppText>} onPress={() => navigation.navigate('TrustedContacts')} />
        <ListRow title="Ride preferences" subtitle="Seats, pickup notes and sharing options" leading={<AppText variant="title">🛡️</AppText>} onPress={() => navigation.navigate('Safety')} />
        <ListRow title="Verification" subtitle="Passenger document readiness" leading={<AppText variant="title">✅</AppText>} onPress={() => navigation.navigate('Verification')} />
      </Card>
      <Card>
        <AppText variant="label" color="#9a8d82">SUPPORT</AppText>
        <ListRow title="Help center" subtitle="FAQs and support articles" leading={<AppText variant="title">❔</AppText>} onPress={() => navigation.navigate('Support')} />
        <ListRow title="Report an issue" subtitle="Tell RouteShare support what happened" leading={<AppText variant="title">✉️</AppText>} onPress={() => navigation.navigate('Support', { ticketId: 'new' })} />
        <ListRow title="Rate the app" subtitle="Share feedback" leading={<AppText variant="title">⭐</AppText>} />
      </Card>
      <Button variant="secondary" accessibilityLabel="Sign out">Sign out</Button>
      <AppText color="#9a8d82" style={{ textAlign: 'center' }}>RouteShare Passenger v0.1.0</AppText>
    </Screen>
  );
}
