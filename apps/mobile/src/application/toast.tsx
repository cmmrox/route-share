import { createContext, useContext, useMemo, useState, type PropsWithChildren } from 'react';
import { StyleSheet, Text, View } from 'react-native';

interface ToastContextValue {
  readonly showToast: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue>({ showToast: () => undefined });

export function ToastProvider({ children }: PropsWithChildren) {
  const [message, setMessage] = useState<string | null>(null);
  const value = useMemo(
    () => ({
      showToast: (nextMessage: string) => {
        setMessage(nextMessage);
        setTimeout(() => setMessage(null), 2500);
      },
    }),
    [],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      {message ? (
        <View style={styles.toast} accessibilityRole="alert">
          <Text style={styles.text}>{message}</Text>
        </View>
      ) : null}
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  return useContext(ToastContext);
}

const styles = StyleSheet.create({
  toast: {
    position: 'absolute',
    left: 16,
    right: 16,
    bottom: 40,
    borderRadius: 16,
    backgroundColor: '#0F172A',
    padding: 14,
  },
  text: {
    color: '#FFFFFF',
    textAlign: 'center',
    fontWeight: '600',
  },
});
