import { StyleSheet, View } from 'react-native';

import { useToast } from '../application/toast';
import { AppText, BottomSheet, Button, Chip, IconButton, ListRow, MapBackdrop, SosButton } from '../design-system';

export function HomeScreen() {
  const { showToast } = useToast();

  return (
    <View style={styles.root}>
      <MapBackdrop showRoute={false}>
        <View style={styles.topBar}>
          <IconButton accessibilityLabel="Open menu" accessibilityHint="Shows account, trips, payments and saved places" icon={<AppText variant="title">☰</AppText>} onPress={() => showToast('Menu ready')} />
          <Button variant="secondary" accessibilityLabel="Safety" accessibilityHint="Open the safety toolkit" onPress={() => showToast('Safety toolkit ready')}>Safety</Button>
        </View>
        <View style={styles.currentPin}>
          <AppText variant="label" color="#ffffff">●</AppText>
        </View>
        <View style={styles.sosFloating}><SosButton onPress={() => showToast('SOS ready')} /></View>
      </MapBackdrop>

      <BottomSheet accessibilityLabel="Find a shared ride" style={styles.sheet}>
        <AppText variant="display">Where to, Nimali?</AppText>
        <Button accessibilityLabel="Enter destination" accessibilityHint="Open destination and route search" variant="secondary" onPress={() => showToast('Search flow coming next')}>🔎  Enter destination     Now</Button>
        <View style={styles.quickRow}>
          <Chip accessibilityLabel="Go home" accessibilityHint="Use saved home destination">Home</Chip>
          <Chip accessibilityLabel="Go to office" accessibilityHint="Use saved office destination">Office</Chip>
        </View>
        <View style={styles.sectionHeader}>
          <AppText variant="label" color="#9a8d82">FREQUENT ROUTES</AppText>
          <Button variant="ghost" accessibilityLabel="See all frequent routes" onPress={() => showToast('Frequent routes ready')}>See all</Button>
        </View>
        <ListRow title="Rajagiriya → Colombo Fort" subtitle="Shared route · 12 min pickup · Rs 520" leading={<AppText variant="title">🏠</AppText>} trailing={<AppText color="#d66a3b">96%</AppText>} onPress={() => showToast('Route detail coming next')} />
        <ListRow title="Office → Nugegoda" subtitle="Evening ride · 2 seats · Rs 680" leading={<AppText variant="title">🏢</AppText>} trailing={<AppText color="#d66a3b">91%</AppText>} onPress={() => showToast('Route detail coming next')} />
      </BottomSheet>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { backgroundColor: '#faf7f2', flex: 1 },
  topBar: { flexDirection: 'row', justifyContent: 'space-between', left: 16, position: 'absolute', right: 16, top: 16 },
  currentPin: { alignItems: 'center', backgroundColor: '#0f6e66', borderColor: '#ffffff', borderRadius: 16, borderWidth: 4, height: 32, justifyContent: 'center', left: '49%', position: 'absolute', top: '38%', width: 32 },
  sosFloating: { bottom: 18, position: 'absolute', right: 18 },
  sheet: { flex: 1, gap: 14, marginTop: -34 },
  quickRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  sectionHeader: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' },
});
