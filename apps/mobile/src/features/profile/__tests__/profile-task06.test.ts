import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it, vi } from 'vitest';

import {
  avatarInitials,
  prepareAvatarForUpload,
  simulateAvatarUpload,
  toProfileUpdateBody,
  toSavedPlaceBody,
  toTrustedContactBody,
  validateAvatarAsset,
  validateEmail,
  validateFullName,
  validateSavedPlace,
  validateTrustedContact,
  verificationCopy,
} from '../index';

describe('Task 06 profile validation and adapters', () => {
  it('validates profile setup names and optional email', () => {
    expect(validateFullName('A').ok).toBe(false);
    expect(validateFullName('  Benadee   Passenger  ')).toEqual({ ok: true });
    expect(validateEmail('').ok).toBe(true);
    expect(validateEmail('bad-email').ok).toBe(false);
  });

  it('maps backend profile update body without exposing unsupported fields', () => {
    expect(toProfileUpdateBody({ fullName: '  CMMROX  User ', email: 'me@example.com', photoUrl: 'file:///a.jpg', referralCode: 'REF1' })).toEqual({
      fullName: 'CMMROX User',
      photoUrl: 'file:///a.jpg',
      preferences: { email: 'me@example.com', referralCode: 'REF1' },
    });
  });

  it('wires profile image selection to Expo image picker instead of a hard-coded demo asset', () => {
    const packageJson = JSON.parse(readFileSync(join(process.cwd(), 'package.json'), 'utf8'));
    const screenSource = readFileSync(join(process.cwd(), 'src/screens/profile-setup.screen.tsx'), 'utf8');

    expect(packageJson.dependencies['expo-image-picker']).toBeTruthy();
    expect(screenSource).toContain('launchImageLibraryAsync');
    expect(screenSource).not.toContain("file:///avatar.jpg");
  });

  it('returns to Home immediately after a successful profile save', () => {
    const screenSource = readFileSync(join(process.cwd(), 'src/screens/profile-setup.screen.tsx'), 'utf8');

    expect(screenSource).toContain('setAuthMeAccepted(true)');
    expect(screenSource).toMatch(/navigation\.reset\(\{[\s\S]*name: 'Home'/);
  });

  it('validates avatar type and size before upload simulation', async () => {
    expect(avatarInitials('CMMROX User')).toBe('CU');
    expect(validateAvatarAsset({ uri: 'file:///a.gif', mimeType: 'image/gif', fileSize: 100 }).ok).toBe(false);
    await expect(prepareAvatarForUpload({ uri: 'file:///a.jpg', mimeType: 'image/jpeg', fileSize: 100, width: 1024, height: 1024 })).resolves.toMatchObject({ width: 512, height: 512 });
    const progress = vi.fn();
    await expect(simulateAvatarUpload({ uri: 'file:///a.jpg', mimeType: 'image/jpeg', fileSize: 100 }, progress)).resolves.toBe('file:///a.jpg');
    expect(progress).toHaveBeenCalledWith(100);
  });

  it('validates saved place coordinates and converts to backend body', () => {
    expect(validateSavedPlace({ label: '', location: { latitude: 6.9, longitude: 79.8 } }).label).toBeTruthy();
    expect(validateSavedPlace({ label: 'Home', location: { latitude: 91, longitude: 79.8 } }).location).toBeTruthy();
    expect(toSavedPlaceBody({ label: ' Home ', address: ' Rajagiriya ', location: { latitude: 6.9271, longitude: 79.8612 } })).toEqual({
      label: 'Home',
      address: 'Rajagiriya',
      latitude: 6.9271,
      longitude: 79.8612,
    });
  });

  it('normalizes trusted contacts and verification readiness copy honestly', () => {
    expect(validateTrustedContact({ name: '', phoneNumber: '+947****4567' }).name).toBeTruthy();
    expect(validateTrustedContact({ name: 'Lakshani', phoneNumber: '0771234567', relationship: 'Wife' })).toEqual({});
    expect(toTrustedContactBody({ name: ' Lakshani ', phoneNumber: '+947****4567', relationship: ' Wife ' })).toEqual({ name: 'Lakshani', phone: '+947****4567', relationship: 'Wife' });
    expect(verificationCopy('readiness_only').message).toContain('not enabled');
  });
});
