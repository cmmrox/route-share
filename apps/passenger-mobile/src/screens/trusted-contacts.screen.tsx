import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { createPassengerRuntimeApi } from '../application/providers';
import { useToast } from '../application/toast';
import { AppText, Button, Card, EmptyState, ErrorState, ListRow, LoadingState, Screen, TextField } from '../design-system';
import { normalizeSriLankanPhone, toTrustedContactBody, validateTrustedContact, withPrimaryTrustedContact } from '../features/profile';

export function TrustedContactsScreen() {
  const api = useMemo(() => createPassengerRuntimeApi(), []); const qc = useQueryClient(); const { showToast } = useToast();
  const contacts = useQuery({ queryKey: ['trusted-contacts'], queryFn: api.trustedContacts.list });
  const profile = useQuery({ queryKey: ['passenger-profile'], queryFn: api.profile.get });
  const [name,setName]=useState(''); const [phoneNumber,setPhone]=useState(''); const [relationship,setRelationship]=useState('');
  const draft = { name, phoneNumber: normalizeSriLankanPhone(phoneNumber), relationship };
  const errors = validateTrustedContact(draft);
  const create = useMutation({ mutationFn: () => api.trustedContacts.create(toTrustedContactBody(draft)), onSuccess: (c) => { qc.setQueryData(['trusted-contacts'], [...(contacts.data ?? []), c]); setName(''); setPhone(''); showToast('Trusted contact added.'); }, onError: () => showToast('Could not save contact; retry when online.') });
  const remove = useMutation({ mutationFn: api.trustedContacts.delete, onMutate: async (id: string) => { const prev = contacts.data ?? []; qc.setQueryData(['trusted-contacts'], prev.filter(c => c.contactId !== id)); return prev; }, onError: (_e,_id,prev) => qc.setQueryData(['trusted-contacts'], prev) });
  const setPrimary = useMutation({ mutationFn: (id: string) => api.profile.update(withPrimaryTrustedContact(profile.data, id)), onSuccess: (next) => { qc.setQueryData(['passenger-profile'], next); showToast('Primary trusted contact updated.'); } });
  if (contacts.isLoading) return <LoadingState label="Loading trusted contacts" />;
  if (contacts.isError) return <ErrorState title="Trusted contacts unavailable" message="Contact import needs permission and network; add a contact manually after retry." onRetry={() => void contacts.refetch()} />;
  return <Screen accessibilityLabel="Trusted contacts"><Card><AppText variant="title">Emergency contacts</AppText><AppText color="#6f6258">These contacts are offered in SOS and Share Trip flows. RouteShare will not contact them except for emergency or trip-share actions you start.</AppText><Button variant="secondary" accessibilityLabel="Import contacts" onPress={() => showToast('Contact import requires permission; manual entry is available.')}>Import from contacts</Button><TextField label="Name" value={name} onChangeText={setName} error={errors.name} /><TextField label="Mobile number" value={phoneNumber} onChangeText={setPhone} error={errors.phoneNumber} /><TextField label="Relationship" value={relationship} onChangeText={setRelationship} error={errors.relationship} /><Button accessibilityLabel="Add trusted contact" disabled={Object.keys(errors).length > 0 || create.isPending} onPress={() => create.mutate()}>Add contact</Button></Card>{(contacts.data ?? []).length === 0 ? <EmptyState title="No trusted contacts yet" message="Add at least one person before using SOS or Share Trip." /> : (contacts.data ?? []).map(contact => <ListRow key={contact.contactId} title={contact.name} subtitle={`${contact.phoneNumber}${contact.relationship ? ` · ${contact.relationship}` : ''}`} trailing={<Button variant="ghost" accessibilityLabel={`Delete ${contact.name}`} onPress={() => remove.mutate(contact.contactId)}>Delete</Button>} onPress={() => setPrimary.mutate(contact.contactId)} accessibilityHint="Set as primary trusted contact" />)}</Screen>;
}
