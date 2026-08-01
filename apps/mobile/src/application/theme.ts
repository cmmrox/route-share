import { useColorScheme } from 'react-native';

import { darkPassengerTokens, lightPassengerTokens } from '../design-system';

export interface PassengerTheme {
  readonly background: string;
  readonly foreground: string;
  readonly muted: string;
  readonly card: string;
  readonly primary: string;
  readonly primaryText: string;
  readonly danger: string;
}

export const lightTheme: PassengerTheme = {
  background: lightPassengerTokens.colors.bg,
  foreground: lightPassengerTokens.colors.ink,
  muted: lightPassengerTokens.colors.ink3,
  card: lightPassengerTokens.colors.surface,
  primary: lightPassengerTokens.colors.accent,
  primaryText: '#FFFFFF',
  danger: lightPassengerTokens.semantic.danger,
};

export const darkTheme: PassengerTheme = {
  background: darkPassengerTokens.colors.bg,
  foreground: darkPassengerTokens.colors.ink,
  muted: darkPassengerTokens.colors.ink3,
  card: darkPassengerTokens.colors.surface,
  primary: darkPassengerTokens.colors.accent,
  primaryText: '#FFFFFF',
  danger: darkPassengerTokens.semantic.danger,
};

export function usePassengerTheme(): PassengerTheme {
  return useColorScheme() === 'dark' ? darkTheme : lightTheme;
}
