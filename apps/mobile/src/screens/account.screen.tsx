import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useMemo } from 'react';
import { StyleSheet, View } from 'react-native';
import { useQuery } from '@tanstack/react-query';

import type { PassengerRootStackParamList } from '../application/navigation';
import { createPassengerRuntimeApi } from '../application/providers';
import { AppText, Avatar, Button, Card, ListRow, LoadingState, Screen } from '../design-system';

const chevron = <AppText color="#c2b3a3">›</AppText>;

type Props = NativeStackScreenProps<PassengerRootStackParamList, 'Account'>;
export function AccountScreen({ navigation }: Props) {
  const api = useMemo(() => createPassengerRuntimeApi(), []);
  const profile = useQuery({ queryKey: ['passenger-profile'], queryFn: api.profile.get, retry: false });
  if (profile.isLoading) return <LoadingState label="Loading account" />;
  const name = profile.data?.fullName ?? profile.data?.displayName ?? 'Passenger';
  return (
    <Screen accessibilityLabel="Account">
      <Card style={styles.header}>
        <View style={styles.headerTop}>
          <Button variant="ghost" accessibilityLabel="Back" onPress={() => navigation.goBack()}>‹</Button>
          <Button variant="ghost" accessibilityLabel="Settings" onPress={() => navigation.navigate('ProfileSetup')}>☀</Button>
        </View>
        <View style={styles.identity}>
          <Avatar name={name} size={56} imageUri={profile.data?.photoUrl} />
          <View style={{ flex: 1 }}>
            <AppText variant="title" color="#ffffff">{name}</AppText>
            <AppText color="#fff1e6">★ 4.92 · 38 trips</AppText>
          </View>
        </View>
        <View style={styles.walletRow}>
          <View style={styles.walletTile}><AppText variant="label" color="#ffe2cf">WALLET</AppText><AppText variant="title" color="#ffffff">LKR 1,250</AppText></View>
          <View style={styles.walletTile}><AppText variant="label" color="#ffe2cf">SAVED</AppText><AppText variant="title" color="#ffffff">LKR 3,440</AppText></View>
        </View>
      </Card>
      <Card>
        <ListRow title="Saved places" subtitle="Home, Office +2" leading={<AppText variant="title">📍</AppText>} trailing={chevron} onPress={() => navigation.navigate('SavedPlaces')} />
        <ListRow title="Payment methods" subtitle="Visa •••• 4429" leading={<AppText variant="title">💳</AppText>} trailing={chevron} onPress={() => navigation.navigate('Payment', { bookingId: 'preview' })} />
        <ListRow title="Trip history" subtitle="14 this month" leading={<AppText variant="title">🧾</AppText>} trailing={chevron} onPress={() => navigation.navigate('TripHistory')} />
        <ListRow title="Receipts & invoices" subtitle="Download past receipts" leading={<AppText variant="title">📄</AppText>} trailing={chevron} onPress={() => navigation.navigate('TripHistory')} />
        <ListRow title="Notifications" subtitle="SMS, Push" leading={<AppText variant="title">🔔</AppText>} trailing={chevron} onPress={() => navigation.navigate('Notifications')} />
      </Card>
      <Card>
        <AppText variant="label" color="#9a8d82">SAFETY</AppText>
        <ListRow title="Trusted contacts" subtitle="3 added" leading={<AppText variant="title">👥</AppText>} trailing={chevron} onPress={() => navigation.navigate('TrustedContacts')} />
        <ListRow title="Ride preferences" subtitle="Female drivers preferred" leading={<AppText variant="title">🛡️</AppText>} trailing={chevron} onPress={() => navigation.navigate('Safety')} />
        <ListRow title="Verification" subtitle="Passenger document readiness" leading={<AppText variant="title">✅</AppText>} trailing={chevron} onPress={() => navigation.navigate('Verification')} />
      </Card>
      <Button variant="secondary" accessibilityLabel="Sign out">Sign out</Button>
      <AppText color="#9a8d82" style={{ textAlign: 'center' }}>RouteShare Passenger v0.1.0</AppText>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { backgroundColor: '#c8612f', borderColor: '#c8612f', gap: 14 },
  headerTop: { flexDirection: 'row', justifyContent: 'space-between' },
  identity: { alignItems: 'center', flexDirection: 'row', gap: 14 },
  walletRow: { flexDirection: 'row', gap: 12 },
  walletTile: { backgroundColor: 'rgba(0,0,0,0.16)', borderRadius: 14, flex: 1, gap: 2, padding: 14 },
});
