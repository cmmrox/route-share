import type { TextStyle, ViewStyle } from 'react-native';

export const routeShareTokens = {
  colors: {
    bg: '#faf7f2', bgSoft: '#f2ede4', surface: '#ffffff', surface2: '#faf6f0',
    ink: '#1b1410', ink2: '#3a2f29', ink3: '#6f6258', ink4: '#9e948a',
    line: '#ecdfce', line2: '#e5d4bc', accent: '#d66a3b', accentPressed: '#c55a2d',
    accentSoft: '#fce9dc', teal: '#0f6e66', tealSoft: '#d2ebe6',
  },
  darkColors: {
    bg: '#161310', bgSoft: '#1f1a15', surface: '#221d18', surface2: '#2a241e',
    ink: '#f4ece0', ink2: '#e1d4c1', ink3: '#a89d8f', ink4: '#766c62',
    line: '#3a3128', line2: '#483d32', accent: '#d66a3b', accentPressed: '#e07a4d',
    accentSoft: '#3a1f12', teal: '#48a89f', tealSoft: '#153632',
  },
  semantic: { success: '#3a8a5a', successSoft: '#d9eadb', warn: '#c98a1a', danger: '#c0392b', dangerSoft: '#f7dcd8' },
  darkSemantic: { success: '#7bc89b', successSoft: '#18302a', warn: '#f0b957', danger: '#f08a7d', dangerSoft: '#3a1a17' },
  match: { full: '#2e7d5b', high: '#3a8a5a', mid: '#c98a1a', low: '#9e948a' },
  spacing: { xxs: 4, xs: 8, sm: 12, md: 16, lg: 20, xl: 24, xxl: 32, touchTarget: 44 },
  radius: { sm: 10, md: 16, lg: 20, xl: 28, pill: 999 },
  shadows: {
    sm: { shadowColor: '#1e140a', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.06, shadowRadius: 2, elevation: 1 } satisfies ViewStyle,
    md: { shadowColor: '#1e140a', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.08, shadowRadius: 12, elevation: 3 } satisfies ViewStyle,
    lg: { shadowColor: '#1e140a', shadowOffset: { width: 0, height: 12 }, shadowOpacity: 0.12, shadowRadius: 28, elevation: 6 } satisfies ViewStyle,
  },
  typography: {
    display: { fontFamily: 'Fraunces, Plus Jakarta Sans', fontSize: 28, lineHeight: 34, fontWeight: '800' } satisfies TextStyle,
    title: { fontFamily: 'Plus Jakarta Sans', fontSize: 22, lineHeight: 28, fontWeight: '800' } satisfies TextStyle,
    body: { fontFamily: 'Plus Jakarta Sans', fontSize: 16, lineHeight: 24, fontWeight: '400' } satisfies TextStyle,
    label: { fontFamily: 'Plus Jakarta Sans', fontSize: 13, lineHeight: 18, fontWeight: '700' } satisfies TextStyle,
    mono: { fontFamily: 'JetBrains Mono', fontSize: 12, lineHeight: 18, fontWeight: '600' } satisfies TextStyle,
  },
} as const;

type ColorName = keyof typeof routeShareTokens.colors;
type SemanticName = keyof typeof routeShareTokens.semantic;
type Palette = Readonly<Record<ColorName, string>>;
type SemanticPalette = Readonly<Record<SemanticName, string>>;
export type PassengerTokens = Omit<typeof routeShareTokens, 'colors' | 'semantic'> & { readonly colors: Palette; readonly semantic: SemanticPalette };

export const lightPassengerTokens: PassengerTokens = { ...routeShareTokens, colors: routeShareTokens.colors, semantic: routeShareTokens.semantic };
export const darkPassengerTokens: PassengerTokens = { ...routeShareTokens, colors: routeShareTokens.darkColors, semantic: routeShareTokens.darkSemantic };

export type MatchTierName = keyof typeof routeShareTokens.match;
export function getMatchTier(value: number): { readonly name: MatchTierName; readonly color: string; readonly label: string } {
  const score = Math.max(0, Math.min(100, Math.round(value)));
  if (score >= 95) return { name: 'full', color: routeShareTokens.match.full, label: 'Full route match' };
  if (score >= 75) return { name: 'high', color: routeShareTokens.match.high, label: 'High route match' };
  if (score >= 45) return { name: 'mid', color: routeShareTokens.match.mid, label: 'Partial route match' };
  return { name: 'low', color: routeShareTokens.match.low, label: 'Low route match' };
}
