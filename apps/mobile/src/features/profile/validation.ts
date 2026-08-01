import type { Coordinate } from '../../api/types';

export type ValidationResult = { ok: true } | { ok: false; error: string };

export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const LK_PHONE_PATTERN = /^(?:\+94|0)?7\d{8}$/;

export function validateFullName(value: string): ValidationResult {
  const trimmed = value.trim().replace(/\s+/g, ' ');
  if (trimmed.length < 2) return { ok: false, error: 'Enter your full name.' };
  if (trimmed.length > 120) return { ok: false, error: 'Name must be 120 characters or less.' };
  return { ok: true };
}

export function validateEmail(value: string): ValidationResult {
  const trimmed = value.trim();
  if (!trimmed) return { ok: true };
  if (trimmed.length > 160 || !EMAIL_PATTERN.test(trimmed)) return { ok: false, error: 'Enter a valid email address.' };
  return { ok: true };
}

export function validateCoordinate(location: Coordinate): ValidationResult {
  if (!Number.isFinite(location.latitude) || location.latitude < -90 || location.latitude > 90) return { ok: false, error: 'Latitude must be between -90 and 90.' };
  if (!Number.isFinite(location.longitude) || location.longitude < -180 || location.longitude > 180) return { ok: false, error: 'Longitude must be between -180 and 180.' };
  return { ok: true };
}

export function validateSavedPlace(input: { label: string; location: Coordinate; address?: string }): Record<string, string> {
  const errors: Record<string, string> = {};
  const label = input.label.trim();
  if (!label) errors.label = 'Add a label such as Home or Work.';
  else if (label.length > 80) errors.label = 'Label must be 80 characters or less.';
  if ((input.address ?? '').length > 500) errors.address = 'Address must be 500 characters or less.';
  const coordinate = validateCoordinate(input.location);
  if (!coordinate.ok) errors.location = coordinate.error;
  return errors;
}

export function normalizeSriLankanPhone(value: string): string {
  const digits = value.replace(/[^\d+]/g, '');
  if (digits.startsWith('+94')) return digits;
  if (digits.startsWith('94') && digits.length === 11) return `+${digits}`;
  if (digits.startsWith('0') && digits.length === 10) return `+94${digits.slice(1)}`;
  return digits;
}

export function validateTrustedContact(input: { name: string; phoneNumber: string; relationship?: string }): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.name.trim()) errors.name = 'Enter a contact name.';
  else if (input.name.trim().length > 120) errors.name = 'Name must be 120 characters or less.';
  const normalized = normalizeSriLankanPhone(input.phoneNumber);
  if (!LK_PHONE_PATTERN.test(normalized)) errors.phoneNumber = 'Enter a valid Sri Lankan mobile number.';
  if ((input.relationship ?? '').length > 80) errors.relationship = 'Relationship must be 80 characters or less.';
  return errors;
}
