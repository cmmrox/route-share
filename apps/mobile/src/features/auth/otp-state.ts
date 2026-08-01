export type OtpStatus = 'empty' | 'focused' | 'submitting' | 'invalid' | 'verified';
export type OtpResendStatus = 'countdown' | 'ready' | 'throttled' | 'networkFailure' | 'sending';

export interface OtpState {
  readonly code: string;
  readonly length: number;
  readonly status: OtpStatus;
  readonly errorMessage?: string;
  readonly attempts: number;
  readonly resend: { readonly status: OtpResendStatus; readonly remainingMs: number };
}

export type OtpEvent =
  | { readonly type: 'change'; readonly value: string }
  | { readonly type: 'submit_started' }
  | { readonly type: 'submit_failed'; readonly message: string }
  | { readonly type: 'submit_succeeded' }
  | { readonly type: 'tick'; readonly remainingMs: number }
  | { readonly type: 'resend_started' }
  | { readonly type: 'resend_succeeded'; readonly countdownMs: number }
  | { readonly type: 'resend_failed'; readonly reason: 'throttled'; readonly retryAfterMs: number }
  | { readonly type: 'resend_failed'; readonly reason: 'network' };

export function sanitizeOtpInput(input: string, length = 6): string {
  return input.replace(/\D/g, '').slice(0, length);
}

export function createInitialOtpState(countdownMs = 30_000, length = 6): OtpState {
  return { code: '', length, status: 'empty', attempts: 0, resend: { status: countdownMs > 0 ? 'countdown' : 'ready', remainingMs: Math.max(0, countdownMs) } };
}

export function isOtpComplete(state: Pick<OtpState, 'code' | 'length'>): boolean {
  return state.code.length === state.length;
}

export function reduceOtpState(state: OtpState, event: OtpEvent): OtpState {
  switch (event.type) {
    case 'change': {
      const code = sanitizeOtpInput(event.value, state.length);
      return { ...state, code, status: code ? 'focused' : 'empty', errorMessage: undefined };
    }
    case 'submit_started':
      return { ...state, status: 'submitting', errorMessage: undefined };
    case 'submit_failed':
      return { ...state, status: 'invalid', errorMessage: event.message, attempts: state.attempts + 1 };
    case 'submit_succeeded':
      return { ...state, status: 'verified', errorMessage: undefined };
    case 'tick': {
      const remainingMs = Math.max(0, event.remainingMs);
      return { ...state, resend: { status: remainingMs === 0 ? 'ready' : 'countdown', remainingMs } };
    }
    case 'resend_started':
      return { ...state, resend: { ...state.resend, status: 'sending' } };
    case 'resend_succeeded':
      return { ...state, code: '', status: 'empty', errorMessage: undefined, resend: { status: 'countdown', remainingMs: Math.max(0, event.countdownMs) } };
    case 'resend_failed':
      if (event.reason === 'throttled') return { ...state, resend: { status: 'throttled', remainingMs: Math.max(0, event.retryAfterMs) } };
      return { ...state, resend: { status: 'networkFailure', remainingMs: 0 } };
  }
}
