export type PhoneValidationResult = { readonly ok: true; readonly e164: string; readonly national: string } | { readonly ok: false; readonly reason: string };

const sriLankanMobilePrefix = /^7[0-8]\d{7}$/;
const digitOnly = /^\d+$/;

export function normalizeSriLankanPhone(input: string): PhoneValidationResult {
  const trimmed = input.trim();
  if (!trimmed) return { ok: false, reason: 'Enter your Sri Lankan mobile number.' };
  if (/[*•]/.test(trimmed)) return { ok: false, reason: 'Enter a valid Sri Lankan mobile number.' };
  const compact = trimmed.replace(/[\s()-]/g, '');
  if (!/^\+?\d+$/.test(compact)) return { ok: false, reason: 'Enter a valid Sri Lankan mobile number.' };
  const local = compact.startsWith('+94')
    ? `0${compact.slice(3)}`
    : compact.startsWith('94') && compact.length === 11
      ? `0${compact.slice(2)}`
      : compact.length === 9 && compact.startsWith('7')
        ? `0${compact}`
        : compact;
  if (!digitOnly.test(local)) return { ok: false, reason: 'Enter a valid Sri Lankan mobile number.' };
  if (local.length !== 10) return { ok: false, reason: 'Enter a 10 digit Sri Lankan mobile number.' };
  if (!local.startsWith('07')) return { ok: false, reason: 'Use a Sri Lankan mobile number starting with 07.' };
  const withoutZero = local.slice(1);
  if (!sriLankanMobilePrefix.test(withoutZero)) return { ok: false, reason: 'Enter a valid Sri Lankan mobile number.' };
  return { ok: true, e164: `+94${withoutZero}`, national: local };
}

export function validateSriLankanPhone(input: string): PhoneValidationResult {
  return normalizeSriLankanPhone(input);
}

export function formatSriLankanPhoneForDisplay(e164: string): string {
  const normalized = normalizeSriLankanPhone(e164);
  if (!normalized.ok) return e164;
  const n = normalized.national;
  return `+94 ${n.slice(1, 3)} ${n.slice(3, 6)} ${n.slice(6)}`;
}
