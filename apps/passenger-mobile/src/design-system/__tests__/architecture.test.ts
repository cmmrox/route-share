import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();

describe('design system architecture', () => {
  it('exports the required reusable primitives and RouteShare components', () => {
    const index = readFileSync(join(root, 'src/design-system/index.ts'), 'utf8');
    const components = readFileSync(join(root, 'src/design-system/components.tsx'), 'utf8');
    expect(index).toContain("export * from './components'");
    for (const name of [
      'Screen', 'AppText', 'Button', 'IconButton', 'TextField', 'OtpField', 'Chip', 'Card',
      'BottomSheet', 'ListRow', 'StatCard', 'ProgressBar', 'ToastView', 'ConfirmDialog',
      'EmptyState', 'ErrorState', 'LoadingState', 'Avatar', 'MatchRing', 'RouteTimeline',
      'FareRow', 'PaymentRow', 'SeatPlan', 'SosButton', 'MapBackdrop', 'MapOverlayCard',
    ]) {
      expect(components).toContain(`export function ${name}`);
    }
  });

  it('keeps touchable primitives accessible with roles, labels, hints and 44px targets', () => {
    const components = readFileSync(join(root, 'src/design-system/components.tsx'), 'utf8');
    expect(components).toContain('MIN_TOUCH_TARGET');
    expect(components).toMatch(/accessibilityRole=\"button\"/);
    expect(components).toMatch(/accessibilityLabel=\{/);
    expect(components).toMatch(/accessibilityHint=\{/);
    expect(components).toMatch(/accessibilityState=\{/);
    expect(components).toContain('minHeight: MIN_TOUCH_TARGET');
    expect(components).toContain('minWidth: MIN_TOUCH_TARGET');
  });

  it('replaces the plain Task 03 shell with RouteShare components and map-backed home UI', () => {
    const shell = readFileSync(join(root, 'src/application/app-shell-screens.tsx'), 'utf8');
    const home = readFileSync(join(root, 'src/screens/home.screen.tsx'), 'utf8');
    expect(shell).not.toContain('Phase 07 shell');
    expect(home).not.toContain('Passenger app scaffold');
    expect(home).toContain('MapBackdrop');
    expect(home).toContain('BottomSheet');
    expect(home).toContain('Where to');
  });
});
