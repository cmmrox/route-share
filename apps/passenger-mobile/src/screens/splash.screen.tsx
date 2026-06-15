import { View } from 'react-native';

import { AppText, LoadingState, MapBackdrop, Screen } from '../design-system';

export function SplashScreen() {
  return (
    <Screen scroll={false} accessibilityLabel="RouteShare splash screen" contentStyle={{ flex: 1, justifyContent: 'center' }}>
      <MapBackdrop showRoute />
      <View style={{ alignItems: 'center', gap: 14 }}>
        <View style={{ alignItems: 'center', backgroundColor: '#d66a3b', borderRadius: 28, height: 88, justifyContent: 'center', width: 88 }}>
          <AppText variant="display" color="#ffffff">RS</AppText>
        </View>
        <AppText variant="display">RouteShare</AppText>
        <AppText color="#6f6258">Share the ride. Share the cost.</AppText>
        <LoadingState label="Preparing secure passenger experience" />
      </View>
    </Screen>
  );
}
