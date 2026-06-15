import { useMemo, useState } from 'react';
import { View } from 'react-native';

import { usePassengerPreferencesStore } from '../application/preferences';
import { AppText, Button, Card, Chip, MapBackdrop, ProgressBar, Screen } from '../design-system';

const slides = [
  { title: 'Rides already heading your way', body: "Drivers publish trips they are already making. You hop on for the part that matches you.", tag: 'Route matching' },
  { title: 'Pay only for your stretch', body: 'Fare is calculated on the actual kilometres you travel, not the full driver route.', tag: 'Fair pricing' },
  { title: 'Track every turn safely', body: 'Live trip status, trusted contacts, verified drivers and SOS are part of the passenger journey.', tag: 'Safety first' },
] as const;

interface OnboardingNavigation { readonly replace?: (screen: string) => void; readonly navigate?: (screen: string) => void }

export function OnboardingScreen({ navigation }: { readonly navigation?: OnboardingNavigation }) {
  const [index, setIndex] = useState(0);
  const setOnboardingComplete = usePassengerPreferencesStore((state) => state.setOnboardingComplete);
  const slide = slides[index];
  const isLast = index === slides.length - 1;
  const progress = useMemo(() => ((index + 1) / slides.length) * 100, [index]);

  const complete = () => {
    setOnboardingComplete(true);
    navigation?.replace?.('Login') ?? navigation?.navigate?.('Login');
  };

  return (
    <Screen accessibilityLabel="RouteShare onboarding" contentStyle={{ gap: 18 }}>
      <MapBackdrop showRoute={index !== 1} />
      <Card accessibilityLabel={`Onboarding slide ${index + 1} of ${slides.length}`}>
        <Chip selected>{slide.tag}</Chip>
        <AppText variant="display" style={{ marginTop: 16 }}>{slide.title}</AppText>
        <AppText color="#6f6258" style={{ marginTop: 8 }}>{slide.body}</AppText>
      </Card>
      <ProgressBar accessibilityLabel="Onboarding progress" value={progress} />
      <View style={{ flexDirection: 'row', gap: 8 }}>
        {slides.map((item, slideIndex) => <View key={item.title} accessibilityLabel={`Page ${slideIndex + 1}${slideIndex === index ? ' selected' : ''}`} style={{ backgroundColor: slideIndex === index ? '#d66a3b' : '#ecdfce', borderRadius: 4, flex: 1, height: 8 }} />)}
      </View>
      <View style={{ flexDirection: 'row', gap: 12 }}>
        <Button variant="ghost" accessibilityLabel="Skip onboarding" accessibilityHint="Go directly to sign in" onPress={complete}>Skip</Button>
        <Button accessibilityLabel={isLast ? 'Get started' : 'Next onboarding slide'} accessibilityHint={isLast ? 'Finish onboarding and sign in' : 'Show the next onboarding slide'} onPress={() => isLast ? complete() : setIndex(index + 1)} style={{ flex: 1 }}>{isLast ? 'Get Started' : 'Next'}</Button>
      </View>
    </Screen>
  );
}
