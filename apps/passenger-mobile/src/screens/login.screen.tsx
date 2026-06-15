import { useMemo, useState } from 'react';
import { Keyboard, Pressable, StyleSheet, View } from 'react-native';

import { createPassengerRuntimeApi } from '../application/providers';
import { useToast } from '../application/toast';
import { getPassengerAuthProviderConfig, validateSriLankanPhone } from '../features/auth';
import { AppText, Button, Card, Chip, Screen, TextField } from '../design-system';

interface LoginNavigation { readonly navigate?: (screen: string, params?: Record<string, unknown>) => void; readonly goBack?: () => void; readonly canGoBack?: () => boolean }

function userFriendlyOtpError(error: unknown): string {
  if (error instanceof Error) {
    const message = error.message.toLowerCase();
    if (message.includes('failed to connect') || message.includes('network request failed') || message.includes('connection refused')) {
      return 'Cannot reach the RouteShare server. Please check that the local backend is running and try again.';
    }
    if (message.includes('timed out')) return 'The server took too long to respond. Please try again.';
    if (error.message.trim()) return error.message;
  }
  return 'Could not send the verification code. Please try again shortly.';
}

export function LoginScreen({ navigation }: { readonly navigation?: LoginNavigation }) {
  const provider = useMemo(() => getPassengerAuthProviderConfig(), []);
  const [phone, setPhone] = useState('');
  const [error, setError] = useState<string>();
  const [sending, setSending] = useState(false);
  const { showToast } = useToast();

  const sendCode = async () => {
    const result = validateSriLankanPhone(phone);
    if (!result.ok) { setError(result.reason); return; }
    setError(undefined);
    if (!provider.phoneOtpSupported) {
      showToast('Phone OTP is disabled for this environment until the backend Notify.lk sender/credentials are enabled.');
      return;
    }
    try {
      Keyboard.dismiss();
      setSending(true);
      const response = await createPassengerRuntimeApi().auth.requestOtp({ phoneNumber: result.e164 });
      navigation?.navigate?.('Otp', { phoneNumber: response.phoneNumber, verificationId: response.verificationId });
    } catch (error) {
      setError(userFriendlyOtpError(error));
    } finally {
      setSending(false);
    }
  };

  const providerLogin = () => {
    showToast('Keycloak PKCE sign-in is configured. Provider launch is enabled in the auth session module.');
  };

  return (
    <Screen accessibilityLabel="Passenger sign in" contentStyle={styles.content}>
      <Pressable accessibilityRole="button" accessibilityLabel="Back" accessibilityHint="Return to the previous screen" onPress={() => navigation?.goBack?.()} disabled={!navigation?.canGoBack?.()} style={styles.backButton}>
        <AppText variant="title">‹</AppText>
      </Pressable>
      <View style={styles.heading}>
        <AppText variant="display" style={styles.displayTitle}>Welcome back</AppText>
        <AppText color="#6f6258">Enter your mobile number to continue</AppText>
      </View>
      <View style={{ gap: 8 }}>
        <AppText variant="label">MOBILE NUMBER</AppText>
        <View style={styles.phoneRow}>
          <Chip selected accessibilityLabel="Sri Lanka country code selected" style={styles.countryCode}>+94</Chip>
          <View style={styles.phoneInput}>
            <TextField label="" value={phone} onChangeText={(value) => { setPhone(value); setError(undefined); }} placeholder="77 123 4567" error={error} accessibilityLabel="Sri Lankan mobile number" accessibilityHint="Enter your mobile number starting with seven or zero seven" />
          </View>
        </View>
        <AppText color="#6f6258">We will text you a 6-digit verification code.</AppText>
      </View>
      <View style={styles.authActions}>
        <Button accessibilityLabel="Send verification code" accessibilityHint="Validate the phone number and request an OTP" onPress={sendCode}>{sending ? 'Sending…' : 'Send Code'}</Button>
      </View>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
        <View style={{ flex: 1, height: 1, backgroundColor: '#e3d6c8' }} />
        <AppText variant="label" color="#9a8d82">OR</AppText>
        <View style={{ flex: 1, height: 1, backgroundColor: '#e3d6c8' }} />
      </View>
      <View style={{ gap: 10 }}>
        <Button variant="secondary" disabled={!provider.googleEnabled} accessibilityLabel="Continue with Google" accessibilityHint="Use Google through Keycloak" onPress={providerLogin}>Continue with Google</Button>
        <Button variant="secondary" disabled={!provider.emailEnabled} accessibilityLabel="Continue with email" accessibilityHint="Use email sign in through Keycloak" onPress={providerLogin}>Continue with Email</Button>
      </View>
      {!provider.phoneOtpSupported ? <Card accessibilityLabel="Phone OTP unavailable"><AppText color="#b42318">{provider.dependencyNote ?? 'Phone OTP is not enabled for this environment.'}</AppText></Card> : null}
      <View style={{ flex: 1 }} />
      <AppText color="#9a8d82" style={styles.terms}>By continuing, you agree to RouteShare Terms and Privacy Policy.</AppText>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { flexGrow: 1, gap: 20, paddingHorizontal: 24, paddingTop: 8, paddingBottom: 24 },
  heading: { gap: 6, marginTop: 12, marginBottom: 8 },
  displayTitle: { fontSize: 30, lineHeight: 36 },
  authActions: { marginTop: 4 },
  backButton: { alignItems: 'flex-start', alignSelf: 'flex-start', justifyContent: 'center', minHeight: 44, minWidth: 44 },
  phoneRow: { alignItems: 'flex-start', flexDirection: 'row', gap: 10 },
  countryCode: { height: 56, marginTop: 26, paddingHorizontal: 14 },
  phoneInput: { flex: 1, minWidth: 0 },
  terms: { textAlign: 'center' },
});
