import { describe, expect, it } from 'vitest';

import { createInitialOtpState, isOtpComplete, reduceOtpState, sanitizeOtpInput } from '../otp-state';

describe('OTP state machine', () => {
  it('handles empty, typed, pasted, invalid and successful OTP states', () => {
    let state = createInitialOtpState(10_000);
    expect(state.status).toBe('empty');
    state = reduceOtpState(state, { type: 'change', value: '12a 3456' });
    expect(state.code).toBe('123456');
    expect(state.status).toBe('focused');
    expect(isOtpComplete(state)).toBe(true);
    state = reduceOtpState(state, { type: 'submit_failed', message: 'Invalid code' });
    expect(state).toMatchObject({ status: 'invalid', errorMessage: 'Invalid code', attempts: 1 });
    state = reduceOtpState(state, { type: 'submit_succeeded' });
    expect(state.status).toBe('verified');
  });

  it('supports resend countdown, ready, throttled, and network failure states', () => {
    let state = createInitialOtpState(30_000);
    expect(state.resend.status).toBe('countdown');
    state = reduceOtpState(state, { type: 'tick', remainingMs: 0 });
    expect(state.resend.status).toBe('ready');
    state = reduceOtpState(state, { type: 'resend_failed', reason: 'throttled', retryAfterMs: 60_000 });
    expect(state.resend).toEqual({ status: 'throttled', remainingMs: 60000 });
    state = reduceOtpState(state, { type: 'resend_failed', reason: 'network' });
    expect(state.resend.status).toBe('networkFailure');
    expect(sanitizeOtpInput('Your code is 987-654')).toBe('987654');
  });
});
