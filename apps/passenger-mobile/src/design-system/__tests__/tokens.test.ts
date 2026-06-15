import { describe, expect, it } from 'vitest';

import { darkPassengerTokens, getMatchTier, lightPassengerTokens, routeShareTokens } from '../tokens';

describe('RouteShare design tokens', () => {
  it('matches the passenger source asset color palette', () => {
    expect(lightPassengerTokens.colors.bg).toBe('#faf7f2');
    expect(lightPassengerTokens.colors.bgSoft).toBe('#f2ede4');
    expect(lightPassengerTokens.colors.surface).toBe('#ffffff');
    expect(lightPassengerTokens.colors.ink).toBe('#1b1410');
    expect(lightPassengerTokens.colors.line).toBe('#ecdfce');
    expect(lightPassengerTokens.colors.accent).toBe('#d66a3b');
    expect(lightPassengerTokens.colors.accentPressed).toBe('#c55a2d');
    expect(lightPassengerTokens.colors.teal).toBe('#0f6e66');
    expect(lightPassengerTokens.colors.tealSoft).toBe('#d2ebe6');
    expect(routeShareTokens.match.full).toBe('#2e7d5b');
    expect(routeShareTokens.semantic.danger).toBe('#c0392b');
  });

  it('includes spacing, radius, shadow and typography scales for reusable components', () => {
    expect(routeShareTokens.spacing.touchTarget).toBeGreaterThanOrEqual(44);
    expect(routeShareTokens.radius.xl).toBe(28);
    expect(routeShareTokens.shadows.md.shadowRadius).toBe(12);
    expect(routeShareTokens.typography.display.fontFamily).toContain('Fraunces');
    expect(routeShareTokens.typography.body.lineHeight).toBeGreaterThan(routeShareTokens.typography.body.fontSize);
  });

  it('provides dark mode colors with readable warm contrast', () => {
    expect(darkPassengerTokens.colors.bg).toBe('#161310');
    expect(darkPassengerTokens.colors.surface).toBe('#221d18');
    expect(darkPassengerTokens.colors.ink).toBe('#f4ece0');
    expect(darkPassengerTokens.colors.accentSoft).toBe('#3a1f12');
  });

  it('maps numeric match scores to source asset tiers', () => {
    expect(getMatchTier(100)).toEqual({ name: 'full', color: '#2e7d5b', label: 'Full route match' });
    expect(getMatchTier(84).name).toBe('high');
    expect(getMatchTier(55).name).toBe('mid');
    expect(getMatchTier(20).name).toBe('low');
  });
});
