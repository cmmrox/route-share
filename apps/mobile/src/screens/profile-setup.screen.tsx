import { useMemo, useState } from 'react';
import { Pressable, View } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { useNavigation, type NavigationProp } from '@react-navigation/native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthStore } from '../application/auth-store';
import type { PassengerRootStackParamList } from '../application/navigation';
import { createPassengerRuntimeApi } from '../application/providers';
import { useToast } from '../application/toast';
import { AppText, Avatar, Button, Card, ErrorState, LoadingState, ProgressBar, Screen, TextField } from '../design-system';
import { prepareAvatarForUpload, simulateAvatarUpload, toProfileUpdateBody, validateEmail, validateFullName, type AvatarAsset } from '../features/profile';

export function ProfileSetupScreen() {
  const api = useMemo(() => createPassengerRuntimeApi(), []);
  const qc = useQueryClient();
  const { showToast } = useToast();
  const navigation = useNavigation<NavigationProp<PassengerRootStackParamList>>();
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
    onSuccess: (next) => {
      qc.setQueryData(['passenger-profile'], next);
      setAuthMeAccepted(true);
      showToast('Profile saved.');
      navigation.reset({ index: 0, routes: [{ name: 'Home' }] });
    },
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
      <Button variant="ghost" accessibilityLabel="Back" accessibilityHint="Return to the previous screen" onPress={() => navigation.canGoBack() && navigation.goBack()} style={{ alignSelf: 'flex-start' }}>‹</Button>
      <AppText variant="display">Tell us about you</AppText>
      <AppText color="#6f6258">Drivers will see your name and photo.</AppText>
      <View style={{ alignItems: 'center', marginVertical: 8 }}>
        <Pressable accessibilityRole="button" accessibilityLabel="Add profile photo" accessibilityHint="Open photo picker or camera with manual fallback" onPress={() => void chooseAvatar()} style={{ position: 'relative' }}>
          <Avatar name={fullName || 'Passenger'} size={96} imageUri={photoUrl ?? source?.photoUrl} />
          <View style={{ position: 'absolute', right: -2, bottom: -2, backgroundColor: '#1b1410', borderRadius: 16, width: 32, height: 32, alignItems: 'center', justifyContent: 'center', borderWidth: 3, borderColor: '#ffffff' }}>
            <AppText variant="label" color="#ffffff">+</AppText>
          </View>
        </Pressable>
        {progress > 0 && progress < 100 ? <ProgressBar value={progress} accessibilityLabel="Avatar upload progress" style={{ marginTop: 12, width: '100%' }} /> : null}
        {avatarError ? <AppText color="#b42318" style={{ marginTop: 8 }}>{avatarError}</AppText> : null}
      </View>
      <TextField label="FIRST NAME" value={first} onChangeText={setFirstName} error={!first.trim() && !nameValidation.ok ? nameValidation.error : undefined} />
      <TextField label="LAST NAME" value={last} onChangeText={setLastName} />
      <TextField label="EMAIL" value={mail} onChangeText={setEmail} error={emailValidation.ok ? undefined : emailValidation.error} />
      <TextField label="REFERRAL CODE (OPTIONAL)" value={referral} onChangeText={setReferralCode} placeholder="Optional" />
      <Card style={{ backgroundColor: '#e6f1ef', borderColor: '#b7d8d2' }}>
        <AppText variant="label" color="#0f6e66">✓ Verified passenger</AppText>
        <AppText color="#34524e">{"We'll ask for a photo ID later to unlock all rides."}</AppText>
      </Card>
      <Button accessibilityLabel="Continue" disabled={!nameValidation.ok || !emailValidation.ok || save.isPending} onPress={() => save.mutate()}>{save.isPending ? 'Saving…' : 'Continue'}</Button>
    </Screen>
  );
}
