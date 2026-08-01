import { StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppText } from '../design-system';

const ACCENT = '#c8612f';

export function SplashScreen() {
  return (
    <SafeAreaView accessibilityLabel="RouteShare splash screen" style={styles.root}>
      <View style={styles.center}>
        <View style={styles.logoMark}>
          <AppText variant="display" color={ACCENT}>a</AppText>
        </View>
        <AppText variant="display" color="#ffffff" style={styles.wordmark}>RouteShare</AppText>
        <AppText color="#fce4d6">Share the ride. Share the cost.</AppText>
      </View>
      <AppText variant="label" color="#f6cdb6" style={styles.footer}>COLOMBO · SRI LANKA</AppText>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { alignItems: 'center', backgroundColor: ACCENT, flex: 1, justifyContent: 'center' },
  center: { alignItems: 'center', flex: 1, gap: 14, justifyContent: 'center' },
  logoMark: { alignItems: 'center', backgroundColor: '#ffffff', borderRadius: 22, height: 78, justifyContent: 'center', marginBottom: 8, width: 78 },
  wordmark: { fontSize: 34 },
  footer: { letterSpacing: 2, marginBottom: 28 },
});
