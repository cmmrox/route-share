import { describe, expect, it } from 'vitest';

import { formatSriLankanPhoneForDisplay, normalizeSriLankanPhone, validateSriLankanPhone } from '../phone-validation';

describe('Sri Lankan phone validation', () => {
  it('normalizes local and E.164 mobile numbers to +94 format', () => {
    expect(normalizeSriLankanPhone('077 123 4567')).toEqual({ ok: true, e164: '+94771234567', national: '0771234567' });
    expect(normalizeSriLankanPhone('+94 71 234 5678')).toEqual({ ok: true, e164: '+94712345678', national: '0712345678' });
    expect(normalizeSriLankanPhone('76 831 0905')).toEqual({ ok: true, e164: '+94768310905', national: '0768310905' });
    expect(formatSriLankanPhoneForDisplay('+94771234567')).toBe('+94 77 123 4567');
  });

  it('blocks invalid Sri Lankan numbers before sending OTP', () => {
    expect(validateSriLankanPhone('011 123 4567')).toEqual({ ok: false, reason: 'Use a Sri Lankan mobile number starting with 07.' });
    expect(validateSriLankanPhone('077123')).toEqual({ ok: false, reason: 'Enter a 10 digit Sri Lankan mobile number.' });
    expect(validateSriLankanPhone('07ABCD5678')).toEqual({ ok: false, reason: 'Enter a valid Sri Lankan mobile number.' });
  });
});
