import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';


const root = process.cwd();

describe('onboarding and auth screen architecture', () => {
  it('exports real Splash, Onboarding, Login and OTP screens', () => {
    const screens = readFileSync(join(root, 'src/screens/index.ts'), 'utf8');
    for (const name of ['SplashScreen', 'OnboardingScreen', 'LoginScreen', 'OtpScreen']) expect(screens).toContain(name);
  });

  it('wires auth routes to real screens instead of the generic shell placeholder', () => {
    const rootShell = readFileSync(join(root, 'src/application/root-shell.tsx'), 'utf8');
    expect(rootShell).toContain('AUTH_ROUTE_COMPONENTS');
    expect(rootShell).toContain('Splash: SplashScreen');
    expect(rootShell).toContain('Onboarding: OnboardingScreen');
    expect(rootShell).toContain('Login: LoginScreen');
    expect(rootShell).toContain('Otp: OtpScreen');
  });

  it('supports local/dev phone OTP while keeping explicit production provider gates', () => {
    const config = readFileSync(join(root, 'src/features/auth/provider-config.ts'), 'utf8');
    expect(config).toContain('DEV_ENVS');
    expect(config).toContain('phoneOtpSupported');
    expect(config).toContain('EXPO_PUBLIC_AUTH_PHONE_OTP_SUPPORTED');
  });

  it('keeps OTP demo and bypass behavior out of the mobile application', () => {
    const providerConfig = readFileSync(join(root, 'src/features/auth/provider-config.ts'), 'utf8');
    const environment = readFileSync(join(root, 'src/application/environment.ts'), 'utf8');
    const otpScreen = readFileSync(join(root, 'src/screens/otp.screen.tsx'), 'utf8');

    for (const source of [providerConfig, environment, otpScreen]) {
      expect(source).not.toContain('devOtpBypass');
      expect(source).not.toContain('EXPO_PUBLIC_AUTH_DEV_OTP_BYPASS_CODE');
      expect(source).not.toContain('000000');
    }
  });
});
