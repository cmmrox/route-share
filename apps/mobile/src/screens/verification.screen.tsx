import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import { createPassengerRuntimeApi } from '../application/providers';
import { useToast } from '../application/toast';
import { AppText, Button, Card, LoadingState, Screen } from '../design-system';
import { verificationCopy } from '../features/profile';

export function VerificationScreen() {
  const api = useMemo(() => createPassengerRuntimeApi(), []); const { showToast } = useToast();
  const status = useQuery({ queryKey: ['verification-status'], queryFn: api.profile.getVerificationStatus });
  if (status.isLoading) return <LoadingState label="Loading verification status" />;
  const copy = verificationCopy(status.data?.status);
  return <Screen accessibilityLabel="Passenger verification"><Card><AppText variant="display">{copy.title}</AppText><AppText color="#6f6258">{copy.message}</AppText><AppText color="#6f6258">You may prepare an ID image now. Production review, approval, and rejection callbacks are blocked until backend document endpoints exist.</AppText><Button accessibilityLabel="Submit verification document" onPress={() => showToast('Document stored locally for readiness only; backend upload is blocked.')}>Submit document shell</Button></Card></Screen>;
}
