import { useEffect, useReducer, useState } from 'react';
import { Keyboard, StyleSheet, View } from 'react-native';

import { useAuthStore } from '../application/auth-store';
import { createPassengerRuntimeApi } from '../application/providers';
import { createInitialOtpState, formatSriLankanPhoneForDisplay, isOtpComplete, reduceOtpState } from '../features/auth';
import { AppText, Button, Card, OtpField, Screen } from '../design-system';

interface OtpNavigation { readonly replace?: (screen: string) => void; readonly goBack?: () => void }
interface OtpRoute { readonly params?: { readonly phoneNumber?: string; readonly verificationId?: string } }

export function OtpScreen({ route, navigation }: { readonly route?: OtpRoute; readonly navigation?: OtpNavigation }) {
  const [state, dispatch] = useReducer(reduceOtpState, createInitialOtpState(30_000));
  const [submitting, setSubmitting] = useState(false);
  const phone = route?.params?.phoneNumber;
  const verificationId = route?.params?.verificationId;
  const [currentVerificationId, setCurrentVerificationId] = useState(verificationId);
  const setAccessToken = useAuthStore((auth) => auth.setAccessToken);
  const setAuthMeAccepted = useAuthStore((auth) => auth.setAuthMeAccepted);


  useEffect(() => {
    if (state.resend.status !== 'countdown' && state.resend.status !== 'throttled') return undefined;
    if (state.resend.remainingMs <= 0) return undefined;
    const startedAt = Date.now();
    const initialRemaining = state.resend.remainingMs;
    const timer = setInterval(() => {
      dispatch({ type: 'tick', remainingMs: Math.max(0, initialRemaining - (Date.now() - startedAt)) });
    }, 1000);
    return () => clearInterval(timer);
  }, [state.resend.remainingMs, state.resend.status]);

  const verify = async () => {
    if (!isOtpComplete(state)) { dispatch({ type: 'submit_failed', message: 'Enter the 6 digit verification code.' }); return; }
    if (!phone || !currentVerificationId) { dispatch({ type: 'submit_failed', message: 'Verification session expired. Please request a new code.' }); return; }
    try {
      Keyboard.dismiss();
      setSubmitting(true);
      dispatch({ type: 'submit_started' });
      const response = await createPassengerRuntimeApi().auth.verifyOtp({ verificationId: currentVerificationId, phoneNumber: phone, code: state.code });
      setAccessToken(response.accessToken, Date.now() + response.expiresInSeconds * 1000);
      setAuthMeAccepted(false);
      dispatch({ type: 'submit_succeeded' });
      navigation?.replace?.('ProfileSetup');
    } catch {
      dispatch({ type: 'submit_failed', message: 'Invalid or expired verification code. Please try again.' });
    } finally {
      setSubmitting(false);
    }
  };

  const resend = async () => {
    if (!phone) { dispatch({ type: 'resend_failed', reason: 'network' }); return; }
    if (state.resend.status !== 'ready' && state.resend.status !== 'networkFailure') {
      dispatch({ type: 'resend_failed', reason: 'throttled', retryAfterMs: state.resend.remainingMs || 30_000 });
      return;
    }
    try {
      dispatch({ type: 'resend_started' });
      const response = await createPassengerRuntimeApi().auth.requestOtp({ phoneNumber: phone });
      setCurrentVerificationId(response.verificationId);
      dispatch({ type: 'resend_succeeded', countdownMs: response.resendAfterSeconds * 1000 });
    } catch {
      dispatch({ type: 'resend_failed', reason: 'network' });
    }
  };

  const resendCopy = state.resend.status === 'ready' || state.resend.status === 'networkFailure'
    ? 'Resend'
    : `Resend in 0:${Math.ceil(state.resend.remainingMs / 1000).toString().padStart(2, '0')}`;

  return (
    <Screen accessibilityLabel="OTP verification" contentStyle={styles.content}>
      <Button variant="ghost" accessibilityLabel="Back" accessibilityHint="Go back to phone number entry" onPress={() => navigation?.goBack?.()}>←</Button>
      <View style={styles.heading}>
        <AppText variant="display" style={styles.displayTitle}>Enter the code</AppText>
        <AppText color="#6f6258">Sent to {phone ? formatSriLankanPhoneForDisplay(phone) : 'your phone'}</AppText>
      </View>
      <View style={styles.otpArea}>
        <OtpField value={state.code} onChangeText={(value) => dispatch({ type: 'change', value })} />
      </View>
      {state.errorMessage ? <Card accessibilityLabel="Verification error"><AppText color="#c0392b">{state.errorMessage}</AppText></Card> : null}
      <View style={styles.resendRow}>
        <AppText color="#6f6258">{"Didn't receive it? "}</AppText>
        <Button variant="ghost" accessibilityLabel="Resend code" accessibilityHint="Request another code when countdown is ready" onPress={resend} style={styles.resendButton}>{resendCopy}</Button>
      </View>
      <View style={{ flex: 1 }} />
      <Button accessibilityLabel="Verify OTP" accessibilityHint="Submit the one-time code" disabled={!isOtpComplete(state) || submitting} onPress={verify}>{submitting ? 'Verifying…' : 'Verify'}</Button>
    </Screen>
  );
}


const styles = StyleSheet.create({
  content: { flexGrow: 1, gap: 20, paddingHorizontal: 24, paddingTop: 8, paddingBottom: 24 },
  heading: { gap: 6, marginTop: 12 },
  displayTitle: { fontSize: 30, lineHeight: 36 },
  otpArea: { gap: 12, marginTop: 10 },
  resendRow: { alignItems: 'center', flexDirection: 'row', justifyContent: 'center' },
  resendButton: { paddingHorizontal: 4, paddingVertical: 4 },
});
