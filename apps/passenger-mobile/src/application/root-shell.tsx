import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { StatusBar } from 'expo-status-bar';
import type { ComponentType } from 'react';
import { useMemo } from 'react';

import { AppShellPlaceholderScreen, OfflineBanner } from './app-shell-screens';
import { AccountScreen, HomeScreen, LoginScreen, OnboardingScreen, OtpScreen, ProfileSetupScreen, RideDetailScreen, SavedPlacesScreen, SearchResultsScreen, SearchScreen, SplashScreen, TrustedContactsScreen, VerificationScreen } from '../screens';
import { useAuthStore, resolveTokenStatus } from './auth-store';
import { useNetworkState } from './network-state';
import { passengerLinking, passengerRouteNames, type PassengerRootStackParamList, type PassengerRouteName } from './navigation';
import { usePassengerPreferencesStore } from './preferences';
import { resolveStartupRoute } from './startup-state';

const Stack = createNativeStackNavigator<PassengerRootStackParamList>();

export const AUTH_ROUTE_COMPONENTS = {
  Splash: SplashScreen,
  Onboarding: OnboardingScreen,
  Login: LoginScreen,
  Otp: OtpScreen,
} as const;

// React Navigation screen components receive route-specific props that cannot be represented by one shared prop object.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type RouteScreenComponent = ComponentType<any>;

const ROUTE_COMPONENTS: Partial<Record<PassengerRouteName, RouteScreenComponent>> = {
  ...AUTH_ROUTE_COMPONENTS,
  Home: HomeScreen,
  Search: SearchScreen,
  SearchResults: SearchResultsScreen,
  RideDetail: RideDetailScreen,
  ProfileSetup: ProfileSetupScreen,
  Account: AccountScreen,
  SavedPlaces: SavedPlacesScreen,
  TrustedContacts: TrustedContactsScreen,
  Verification: VerificationScreen,
} as const;

export function RootShell() {
  const network = useNetworkState();
  const onboardingComplete = usePassengerPreferencesStore((state) => state.onboardingComplete);
  const authMeStatus = useAuthStore((state) => state.authMeStatus);
  const profileComplete = useAuthStore((state) => state.profileComplete);
  const tokenStatus = useAuthStore((state) => resolveTokenStatus(state));

  const initialRouteName = useMemo(
    () => resolveStartupRoute({ appConfig: 'ready', onboardingComplete, token: tokenStatus, authMe: authMeStatus, profileComplete, online: network.online }),
    [authMeStatus, network.online, onboardingComplete, profileComplete, tokenStatus],
  );

  return (
    <NavigationContainer linking={passengerLinking}>
      <StatusBar style="auto" />
      <OfflineBanner online={network.online} />
      <Stack.Navigator key={initialRouteName} initialRouteName={initialRouteName} screenOptions={{ headerShown: false }}>
        {passengerRouteNames.map((routeName) => (
          <Stack.Screen key={routeName} name={routeName} component={ROUTE_COMPONENTS[routeName] ?? AppShellPlaceholderScreen} />
        ))}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
