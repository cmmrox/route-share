export type MutationSafety = 'safe' | 'unsafe';

export function canRunMutation(input: { readonly online: boolean; readonly mutationSafety: MutationSafety }): boolean {
  return input.online || input.mutationSafety === 'safe';
}

export function createOfflineAwareQueryOptions(online: boolean) {
  return online
    ? { networkMode: 'online' as const, retry: 1, staleTime: 30_000 }
    : { networkMode: 'offlineFirst' as const, retry: 0, staleTime: 300_000 };
}
