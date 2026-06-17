import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { usePassengerPreferencesStore } from '../application/preferences';
import { AppText, Button, Screen } from '../design-system';

const slides = [
  {
    title: 'Rides, already heading your way',
    body: "Drivers publish trips they're already making. You just hop on.",
  },
  {
    title: 'Pay only for your stretch',
    body: 'Fare is calculated on the actual kilometres you travel — not the whole route.',
  },
  {
    title: 'Track every turn',
    body: 'Live GPS, seat count, driver rating and an SOS button. Always.',
  },
] as const;

interface OnboardingNavigation { readonly replace?: (screen: string) => void; readonly navigate?: (screen: string) => void }

function SlideIllustration({ index }: { readonly index: number }) {
  return (
    <View accessibilityLabel="Onboarding illustration" style={styles.illustration}>
      {index === 0 ? (
        <View style={styles.routeWrap}>
          <View style={[styles.endpoint, styles.endpointTeal]} />
          <View style={styles.carDot} />
          <View style={[styles.endpoint, styles.endpointOrange]} />
        </View>
      ) : null}
      {index === 1 ? (
        <View style={{ alignItems: 'center', gap: 18 }}>
          <View style={styles.farePill}>
            <AppText variant="title" color="#1b1410">LKR 600</AppText>
            <AppText variant="label" color="#8a7a6c">12 KM · YOUR STRETCH</AppText>
          </View>
          <View style={styles.stretchRow}>
            <View style={[styles.endpoint, styles.endpointTeal]} />
            <View style={styles.stretchLine} />
            <View style={[styles.endpoint, styles.endpointHollow]} />
          </View>
        </View>
      ) : null}
      {index === 2 ? (
        <View style={styles.routeWrap}>
          <View style={[styles.endpoint, styles.endpointOrange]} />
          <View style={styles.pulse}><View style={styles.pulseCore} /></View>
        </View>
      ) : null}
    </View>
  );
}

export function OnboardingScreen({ navigation }: { readonly navigation?: OnboardingNavigation }) {
  const [index, setIndex] = useState(0);
  const setOnboardingComplete = usePassengerPreferencesStore((state) => state.setOnboardingComplete);
  const slide = slides[index];
  const isLast = index === slides.length - 1;

  const complete = () => {
    setOnboardingComplete(true);
    navigation?.replace?.('Login') ?? navigation?.navigate?.('Login');
  };

  return (
    <Screen accessibilityLabel="RouteShare onboarding" contentStyle={styles.content}>
      <View style={styles.skipRow}>
        <Button variant="ghost" accessibilityLabel="Skip onboarding" accessibilityHint="Go directly to sign in" onPress={complete}>Skip</Button>
      </View>
      <SlideIllustration index={index} />
      <View style={styles.copy}>
        <AppText variant="display" style={styles.title}>{slide.title}</AppText>
        <AppText color="#6f6258" style={styles.body}>{slide.body}</AppText>
      </View>
      <View style={styles.dots}>
        {slides.map((item, slideIndex) => (
          <View
            key={item.title}
            accessibilityLabel={`Page ${slideIndex + 1}${slideIndex === index ? ' selected' : ''}`}
            style={[styles.dot, slideIndex === index ? styles.dotActive : null]}
          />
        ))}
      </View>
      <Button
        accessibilityLabel={isLast ? 'Get started' : 'Next onboarding slide'}
        accessibilityHint={isLast ? 'Finish onboarding and sign in' : 'Show the next onboarding slide'}
        onPress={() => (isLast ? complete() : setIndex(index + 1))}
      >
        {isLast ? 'Get started' : 'Next'}
      </Button>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { flexGrow: 1, gap: 24, paddingHorizontal: 24, paddingTop: 8, paddingBottom: 28 },
  skipRow: { alignItems: 'flex-end' },
  illustration: { alignItems: 'center', backgroundColor: '#f7e2d3', borderRadius: 24, height: 280, justifyContent: 'center', overflow: 'hidden' },
  routeWrap: { alignItems: 'center', flexDirection: 'row', gap: 36, justifyContent: 'center' },
  endpoint: { borderRadius: 9, height: 18, width: 18 },
  endpointTeal: { backgroundColor: '#0f6e66' },
  endpointOrange: { backgroundColor: '#c8612f' },
  endpointHollow: { backgroundColor: '#ffffff', borderColor: '#c9b8a6', borderWidth: 2 },
  carDot: { backgroundColor: '#1b1410', borderRadius: 8, height: 28, width: 40 },
  pulse: { alignItems: 'center', backgroundColor: 'rgba(200,97,47,0.18)', borderRadius: 36, height: 72, justifyContent: 'center', width: 72 },
  pulseCore: { backgroundColor: '#c8612f', borderRadius: 14, height: 28, width: 28 },
  farePill: { alignItems: 'center', backgroundColor: '#ffffff', borderRadius: 16, gap: 4, paddingHorizontal: 22, paddingVertical: 14 },
  stretchRow: { alignItems: 'center', flexDirection: 'row', gap: 10 },
  stretchLine: { backgroundColor: '#c8612f', borderRadius: 2, height: 4, width: 120 },
  copy: { alignItems: 'center', gap: 10 },
  title: { textAlign: 'center' },
  body: { textAlign: 'center' },
  dots: { alignItems: 'center', flexDirection: 'row', gap: 8, justifyContent: 'center' },
  dot: { backgroundColor: '#e3d2bf', borderRadius: 4, height: 8, width: 8 },
  dotActive: { backgroundColor: '#c8612f', width: 22 },
});
