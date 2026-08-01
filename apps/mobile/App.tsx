import { Fraunces_600SemiBold, Fraunces_700Bold } from '@expo-google-fonts/fraunces';
import { PlusJakartaSans_400Regular, PlusJakartaSans_500Medium, PlusJakartaSans_700Bold, PlusJakartaSans_800ExtraBold } from '@expo-google-fonts/plus-jakarta-sans';
import { useFonts } from 'expo-font';

import { PassengerAppProviders } from './src/application/providers';
import { RootShell } from './src/application/root-shell';
import { SplashScreen } from './src/screens';

export default function App() {
  const [fontsLoaded] = useFonts({
    Fraunces_600SemiBold,
    Fraunces_700Bold,
    PlusJakartaSans_400Regular,
    PlusJakartaSans_500Medium,
    PlusJakartaSans_700Bold,
    PlusJakartaSans_800ExtraBold,
  });

  if (!fontsLoaded) return <SplashScreen />;

  return (
    <PassengerAppProviders>
      <RootShell />
    </PassengerAppProviders>
  );
}
