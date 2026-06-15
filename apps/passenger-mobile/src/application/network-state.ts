import { useEffect, useMemo, useState } from 'react';
import { AppState } from 'react-native';

export { canRunMutation, createOfflineAwareQueryOptions, type MutationSafety } from './network-policy';

export interface NetworkSnapshot {
  readonly online: boolean;
  readonly checkedAt: number;
}

export function useNetworkState(): NetworkSnapshot {
  const [online, setOnline] = useState(true);
  const [checkedAt, setCheckedAt] = useState(() => Date.now());

  useEffect(() => {
    const check = async () => {
      try {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 2500);
        await fetch('https://clients3.google.com/generate_204', { method: 'GET', signal: controller.signal });
        clearTimeout(timeout);
        setOnline(true);
      } catch {
        setOnline(false);
      } finally {
        setCheckedAt(Date.now());
      }
    };

    void check();
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') void check();
    });
    return () => subscription.remove();
  }, []);

  return useMemo(() => ({ online, checkedAt }), [online, checkedAt]);
}
