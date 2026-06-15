import { useMemo, useState } from 'react';
import { View } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthStore } from '../application/auth-store';
import { createPassengerRuntimeApi } from '../application/providers';
import { useToast } from '../application/toast';
import { AppText, Avatar, Button, Card, ErrorState, LoadingState, ProgressBar, Screen, TextField } from '../design-system';
import { prepareAvatarForUpload, simulateAvatarUpload, toProfileUpdateBody, validateEmail, validateFullName, type AvatarAsset } from '../features/profile';

export function ProfileSetupScreen() {
  const api = useMemo(() => createPassengerRuntimeApi(), []);
  const qc = useQueryClient();
  const { showToast } = useToast();
  const setAuthMeAccepted = useAuthStore((auth) => auth.setAuthMeAccepted);
  const profile = useQuery({ queryKey: ['passenger-profile'], queryFn: api.profile.get, retry: false });
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [referralCode, setReferralCode] = useState('');
  const [photoUrl, setPhotoUrl] = useState<string | undefined>();
  const [avatarError, setAvatarError] = useState<string>();
  const [progress, setProgress] = useState(0);
  const source = profile.data;
  const sourceName = source?.fullName || source?.displayName || '';
  const [sourceFirstName, ...sourceLastParts] = sourceName.split(' ').filter(Boolean);
  const first = firstName || sourceFirstName || '';
  const last = lastName || sourceLastParts.join(' ');
  const fullName = `${first} ${last}`.trim().replace(/\s+/g, ' ');
  const mail = email || source?.email || '';
  const referral = referralCode || (typeof source?.preferences?.referralCode === 'string' ? source.preferences.referralCode : '');
  const nameValidation = validateFullName(fullName);
  const emailValidation = validateEmail(mail);
  const save = useMutation({
    mutationFn: () => api.profile.update(toProfileUpdateBody({ fullName, email: mail, photoUrl: photoUrl ?? source?.photoUrl, referralCode: referral }, source)),
    onSuccess: (next) => { qc.setQueryData(['passenger-profile'], next); setAuthMeAccepted(true); showToast('Profile saved.'); },
    onError: () => showToast('Profile could not be saved. Try again.'),
  });
  async function chooseAvatar() {
    setAvatarError(undefined);
    setProgress(0);
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      setAvatarError('Photo library permission is required to add a profile image.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      allowsEditing: true,
      aspect: [1, 1],
      mediaTypes: ['images'],
      quality: 0.85,
    });
    if (result.canceled || !result.assets[0]) return;
    const selected = result.assets[0];
    const asset: AvatarAsset = {
      uri: selected.uri,
      fileName: selected.fileName ?? undefined,
      mimeType: selected.mimeType ?? 'image/jpeg',
      fileSize: selected.fileSize ?? undefined,
      width: selected.width,
      height: selected.height,
    };
    try {
      const prepared = await prepareAvatarForUpload(asset);
      setPhotoUrl(await simulateAvatarUpload(prepared, setProgress));
    }
    catch (error) { setAvatarError(error instanceof Error ? error.message : 'Avatar upload failed.'); }
  }
  if (profile.isLoading) return <LoadingState label="Loading profile" />;
  if (profile.isError) return <ErrorState message="Profile is unavailable. You can retry when online." onRetry={() => void profile.refetch()} />;
  return (
    <Screen accessibilityLabel="Profile setup" contentStyle={{ flexGrow: 1 }}>
      <AppText variant="display">Tell us about you</AppText>
      <AppText color="#6f6258">Drivers will see your name and photo.</AppText>
      <Card style={{ alignItems: 'center', gap: 12 }}>
        <View style={{ alignItems: 'center' }}>
          <Avatar name={fullName || 'Passenger'} size={96} imageUri={photoUrl ?? source?.photoUrl} />
          <View style={{ marginTop: -28, marginLeft: 68, backgroundColor: '#d66a3b', borderRadius: 18, minWidth: 36, minHeight: 36, alignItems: 'center', justifyContent: 'center', borderWidth: 3, borderColor: '#ffffff' }}>
            <AppText variant="title" color="#ffffff">+</AppText>
          </View>
        </View>
        {progress > 0 && progress < 100 ? <ProgressBar value={progress} accessibilityLabel="Avatar upload progress" /> : null}
        {avatarError ? <AppText color="#b42318">{avatarError}</AppText> : null}
        <Button variant="secondary" accessibilityLabel="Add profile photo" accessibilityHint="Open photo picker or camera with manual fallback" onPress={() => void chooseAvatar()}>Add photo</Button>
      </Card>
      <TextField label="First name" value={first} onChangeText={setFirstName} error={!first.trim() && !nameValidation.ok ? nameValidation.error : undefined} />
      <TextField label="Last name" value={last} onChangeText={setLastName} />
      <TextField label="Email" value={mail} onChangeText={setEmail} error={emailValidation.ok ? undefined : emailValidation.error} />
      <TextField label="Referral code" value={referral} onChangeText={setReferralCode} placeholder="Optional" />
      <Card style={{ backgroundColor: '#e6f1ef', borderColor: '#b7d8d2' }}>
        <AppText variant="title" color="#0f6e66">Verified passenger profiles unlock smoother rides</AppText>
        <AppText color="#34524e">RouteShare keeps your phone-linked Keycloak identity as the source of truth and stores only passenger profile details here.</AppText>
      </Card>
      <View style={{ flex: 1 }} />
      <Button accessibilityLabel="Continue" disabled={!nameValidation.ok || !emailValidation.ok || save.isPending} onPress={() => save.mutate()}>{save.isPending ? 'Saving…' : 'Continue'}</Button>
    </Screen>
  );
}
