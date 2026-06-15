import type { PassengerProfile, SavedPlace, TrustedContact } from '../../api/types';

const record = (value: unknown): Record<string, unknown> => value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {};
const stringFrom = (...values: unknown[]) => String(values.find((v) => typeof v === 'string' || typeof v === 'number') ?? '');

export type ProfileForm = { fullName: string; email: string; photoUrl?: string; referralCode?: string };
export type ProfilePreferences = { email?: string; defaultSavedPlaceId?: string; primaryTrustedContactId?: string; referralCode?: string } & Record<string, unknown>;

export function adaptPassengerProfile(input: unknown): PassengerProfile {
  const source = record(input);
  const preferences = record(source.preferences) as ProfilePreferences;
  const fullName = stringFrom(source.fullName, source.displayName, source.name) || undefined;
  return {
    passengerId: stringFrom(source.passengerId, source.id),
    displayName: fullName,
    fullName,
    phoneNumber: typeof source.phoneNumber === 'string' ? source.phoneNumber : undefined,
    email: typeof source.email === 'string' ? source.email : typeof preferences.email === 'string' ? preferences.email : undefined,
    photoUrl: typeof source.photoUrl === 'string' ? source.photoUrl : undefined,
    preferences,
    verificationStatus: typeof source.verificationStatus === 'string' ? source.verificationStatus : 'readiness_only',
  };
}

export function toProfileUpdateBody(form: ProfileForm, current?: PassengerProfile) {
  const preferences = { ...(current?.preferences ?? {}) } as ProfilePreferences;
  if (form.email.trim()) preferences.email = form.email.trim(); else delete preferences.email;
  if (form.referralCode?.trim()) preferences.referralCode = form.referralCode.trim();
  return { fullName: form.fullName.trim().replace(/\s+/g, ' '), photoUrl: form.photoUrl?.trim() || undefined, preferences };
}

export function toSavedPlaceBody(place: Pick<SavedPlace, 'label' | 'address' | 'location'>) {
  return { label: place.label.trim(), address: place.address?.trim() || undefined, latitude: place.location.latitude, longitude: place.location.longitude };
}

export function toTrustedContactBody(contact: Pick<TrustedContact, 'name' | 'phoneNumber' | 'relationship'>) {
  return { name: contact.name.trim(), phone: contact.phoneNumber.trim(), relationship: contact.relationship?.trim() || undefined };
}

export function withDefaultSavedPlace(profile: PassengerProfile | undefined, savedPlaceId: string) {
  const preferences = { ...(profile?.preferences ?? {}), defaultSavedPlaceId: savedPlaceId };
  return { fullName: profile?.fullName ?? profile?.displayName ?? 'Passenger', photoUrl: profile?.photoUrl, preferences };
}

export function withPrimaryTrustedContact(profile: PassengerProfile | undefined, contactId: string) {
  const preferences = { ...(profile?.preferences ?? {}), primaryTrustedContactId: contactId };
  return { fullName: profile?.fullName ?? profile?.displayName ?? 'Passenger', photoUrl: profile?.photoUrl, preferences };
}
