import { useRef } from 'react';
import type { PropsWithChildren, ReactNode } from 'react';
import MapView, { Marker, Polyline } from 'react-native-maps';
import { ActivityIndicator, Image, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import type { AccessibilityRole, StyleProp, TextStyle, TextInput as TextInputInstance, ViewStyle } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getMatchTier, lightPassengerTokens, routeShareTokens } from './tokens';

export const MIN_TOUCH_TARGET = routeShareTokens.spacing.touchTarget;
const t = lightPassengerTokens;

type BaseProps = { readonly style?: StyleProp<ViewStyle>; readonly accessibilityLabel?: string; readonly accessibilityHint?: string };

type AppTextVariant = 'display' | 'title' | 'body' | 'label' | 'mono';
export function AppText({ children, variant = 'body', color, style, numberOfLines }: PropsWithChildren<{ readonly variant?: AppTextVariant; readonly color?: string; readonly style?: StyleProp<TextStyle>; readonly numberOfLines?: number }>) {
  return <Text numberOfLines={numberOfLines} style={[t.typography[variant], { color: color ?? t.colors.ink }, style]}>{children}</Text>;
}

export function Screen({ children, scroll = true, style, contentStyle, accessibilityLabel }: PropsWithChildren<BaseProps & { readonly scroll?: boolean; readonly contentStyle?: StyleProp<ViewStyle> }>) {
  const content = <View style={[styles.screenContent, contentStyle]}>{children}</View>;
  return <SafeAreaView accessibilityLabel={accessibilityLabel} style={[styles.screen, style]}>{scroll ? <ScrollView contentInsetAdjustmentBehavior="automatic">{content}</ScrollView> : content}</SafeAreaView>;
}

type PressableProps = BaseProps & { readonly onPress?: () => void; readonly disabled?: boolean; readonly selected?: boolean };
export function Button({ children, onPress, disabled = false, selected = false, accessibilityLabel, accessibilityHint, style, variant = 'primary' }: PropsWithChildren<PressableProps & { readonly variant?: 'primary' | 'secondary' | 'ghost' | 'danger' }>) {
  return <Pressable accessibilityRole="button" accessibilityLabel={accessibilityLabel} accessibilityHint={accessibilityHint} accessibilityState={{ disabled, selected }} disabled={disabled} onPress={onPress} style={[styles.touchTarget, styles.button, buttonStyle(variant, disabled, selected), style]}><AppText variant="label" color={variant === 'primary' || variant === 'danger' ? '#ffffff' : t.colors.ink}>{children}</AppText></Pressable>;
}

export function IconButton({ icon, onPress, disabled = false, selected = false, accessibilityLabel, accessibilityHint, style }: PressableProps & { readonly icon: ReactNode }) {
  return <Pressable accessibilityRole="button" accessibilityLabel={accessibilityLabel} accessibilityHint={accessibilityHint} accessibilityState={{ disabled, selected }} disabled={disabled} onPress={onPress} style={[styles.touchTarget, styles.iconButton, disabled && styles.disabled, selected && styles.selected, style]}>{icon}</Pressable>;
}

export function TextField({ label, value, onChangeText, placeholder, error, accessibilityLabel, accessibilityHint, secureTextEntry = false }: { readonly label: string; readonly value: string; readonly onChangeText?: (value: string) => void; readonly placeholder?: string; readonly error?: string; readonly accessibilityLabel?: string; readonly accessibilityHint?: string; readonly secureTextEntry?: boolean }) {
  return <View style={styles.fieldWrap}><AppText variant="label">{label}</AppText><TextInput accessibilityLabel={accessibilityLabel ?? label} accessibilityHint={accessibilityHint} accessibilityState={{ disabled: !onChangeText }} secureTextEntry={secureTextEntry} value={value} onChangeText={onChangeText} placeholder={placeholder} placeholderTextColor={t.colors.ink4} style={[styles.input, error && styles.inputError]} />{error ? <AppText variant="label" color={t.semantic.danger}>{error}</AppText> : null}</View>;
}

export function OtpField({ value, length = 6, onChangeText, accessibilityLabel = 'One-time passcode' }: { readonly value: string; readonly length?: number; readonly onChangeText?: (value: string) => void; readonly accessibilityLabel?: string }) {
  const inputRef = useRef<TextInputInstance>(null);
  const chars = Array.from({ length }, (_, i) => value[i] ?? '');
  const selectedIndex = Math.min(value.length, length - 1);
  const changeCode = (nextValue: string) => onChangeText?.(nextValue.replace(/\D/g, '').slice(0, length));

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
      accessibilityHint="Enter the verification code sent to your phone"
      onPress={() => inputRef.current?.focus()}
      style={styles.otpWrap}
    >
      <View pointerEvents="none" style={styles.otpRow}>
        {chars.map((char, i) => (
          <View key={i} style={[styles.otpCell, i === selectedIndex && styles.selected]}>
            <AppText variant="title">{char || ' '}</AppText>
          </View>
        ))}
      </View>
      <TextInput
        ref={inputRef}
        accessibilityLabel={accessibilityLabel}
        accessibilityHint="Enter the verification code sent to your phone"
        autoComplete="sms-otp"
        autoFocus
        blurOnSubmit
        caretHidden
        inputMode="numeric"
        onSubmitEditing={() => inputRef.current?.blur()}
        returnKeyType="done"
        keyboardType="number-pad"
        maxLength={length}
        onChangeText={changeCode}
        textContentType="oneTimeCode"
        value={value}
        style={styles.otpInputOverlay}
      />
    </Pressable>
  );
}

export function Chip({ children, selected = false, disabled = false, onPress, accessibilityLabel, accessibilityHint, style }: PropsWithChildren<PressableProps>) {
  return <Pressable accessibilityRole="button" accessibilityLabel={accessibilityLabel} accessibilityHint={accessibilityHint} accessibilityState={{ disabled, selected }} disabled={disabled} onPress={onPress} style={[styles.touchTarget, styles.chip, selected && styles.chipSelected, disabled && styles.disabled, style]}><AppText variant="label" color={selected ? '#ffffff' : t.colors.ink2}>{children}</AppText></Pressable>;
}

export function Card({ children, style, accessibilityLabel }: PropsWithChildren<BaseProps>) { return <View accessibilityLabel={accessibilityLabel} style={[styles.card, style]}>{children}</View>; }
export function BottomSheet({ children, style, accessibilityLabel }: PropsWithChildren<BaseProps>) { return <View accessibilityLabel={accessibilityLabel} style={[styles.sheet, style]}><View style={styles.grabber} />{children}</View>; }

export function ListRow({ title, subtitle, leading, trailing, onPress, accessibilityLabel, accessibilityHint }: { readonly title: string; readonly subtitle?: string; readonly leading?: ReactNode; readonly trailing?: ReactNode; readonly onPress?: () => void; readonly accessibilityLabel?: string; readonly accessibilityHint?: string }) {
  const Comp = onPress ? Pressable : View;
  const access = onPress ? { accessibilityRole: 'button' as AccessibilityRole, accessibilityLabel: accessibilityLabel ?? title, accessibilityHint, accessibilityState: { disabled: false } } : { accessibilityLabel: accessibilityLabel ?? title };
  return <Comp {...access} onPress={onPress} style={[styles.row, onPress && styles.touchTarget]}>{leading}<View style={styles.flex}><AppText variant="label">{title}</AppText>{subtitle ? <AppText color={t.colors.ink3}>{subtitle}</AppText> : null}</View>{trailing}</Comp>;
}

export function StatCard({ label, value, tone = 'teal' }: { readonly label: string; readonly value: string; readonly tone?: 'teal' | 'accent' | 'success' }) { const color = tone === 'accent' ? t.colors.accent : tone === 'success' ? t.semantic.success : t.colors.teal; return <Card style={styles.stat}><AppText variant="display" color={color}>{value}</AppText><AppText variant="label" color={t.colors.ink3}>{label}</AppText></Card>; }
export function ProgressBar({ value, accessibilityLabel = 'Progress', style }: { readonly value: number; readonly accessibilityLabel?: string; readonly style?: StyleProp<ViewStyle> }) { const pct = Math.max(0, Math.min(100, value)); return <View accessibilityRole="progressbar" accessibilityLabel={accessibilityLabel} accessibilityValue={{ min: 0, max: 100, now: Math.round(value) }} style={[styles.progress, style]}><View style={[styles.progressFill, { width: `${pct}%` as `${number}%` }]} /></View>; }
export function ToastView({ message, tone = 'success' }: { readonly message: string; readonly tone?: 'success' | 'danger' | 'warn' }) { const color = tone === 'danger' ? t.semantic.danger : tone === 'warn' ? t.semantic.warn : t.semantic.success; return <View accessibilityRole="alert" style={[styles.toast, { borderColor: color }]}><AppText variant="label">{message}</AppText></View>; }
export function ConfirmDialog({ visible, title, message, onCancel, onConfirm }: { readonly visible: boolean; readonly title: string; readonly message: string; readonly onCancel: () => void; readonly onConfirm: () => void }) { return <Modal transparent visible={visible}><View style={styles.dialogBackdrop}><Card style={styles.dialog}><AppText variant="title">{title}</AppText><AppText>{message}</AppText><View style={styles.actions}><Button variant="secondary" accessibilityLabel="Cancel" accessibilityHint="Dismiss confirmation" onPress={onCancel}>Cancel</Button><Button accessibilityLabel="Confirm" accessibilityHint="Confirm this action" onPress={onConfirm}>Confirm</Button></View></Card></View></Modal>; }
export function LoadingState({ label = 'Loading' }: { readonly label?: string }) { return <View accessibilityRole="progressbar" accessibilityLabel={label} style={styles.state}><ActivityIndicator color={t.colors.accent} /><AppText>{label}</AppText></View>; }
export function EmptyState({ title, message }: { readonly title: string; readonly message: string }) { return <View accessibilityLabel={title} style={styles.state}><AppText variant="title">{title}</AppText><AppText color={t.colors.ink3}>{message}</AppText></View>; }
export function ErrorState({ title = 'Something went wrong', message, onRetry }: { readonly title?: string; readonly message: string; readonly onRetry?: () => void }) { return <View accessibilityRole="alert" style={styles.state}><AppText variant="title" color={t.semantic.danger}>{title}</AppText><AppText color={t.colors.ink3}>{message}</AppText>{onRetry ? <Button variant="secondary" accessibilityLabel="Retry" accessibilityHint="Try loading this content again" onPress={onRetry}>Retry</Button> : null}</View>; }

const avatarColors = ['#0f6e66', '#d66a3b', '#8a5a2b', '#5c7c3a', '#8c4a6b', '#4a6c8a'];
export function Avatar({ name, size = 44, imageUri }: { readonly name: string; readonly size?: number; readonly imageUri?: string }) { const initials = name.split(' ').filter(Boolean).map(p => p[0]).slice(0, 2).join('').toUpperCase() || '?'; const hash = [...name].reduce((a, c) => a + c.charCodeAt(0), 0); const frameStyle = [styles.avatar, { width: size, height: size, borderRadius: size / 2, backgroundColor: avatarColors[hash % avatarColors.length] }]; if (imageUri) return <Image accessibilityLabel={`${name} avatar photo`} source={{ uri: imageUri }} style={frameStyle} />; return <View accessibilityLabel={`${name} avatar`} style={frameStyle}><AppText variant="label" color="#ffffff" style={{ fontSize: size * 0.38 }}>{initials}</AppText></View>; }
export function MatchRing({ value, size = 56 }: { readonly value: number; readonly size?: number }) { const tier = getMatchTier(value); return <View accessibilityLabel={`${Math.round(value)} percent ${tier.label}`} style={[styles.matchRing, { width: size, height: size, borderRadius: size / 2, borderColor: tier.color }]}><AppText variant="label" color={tier.color}>{Math.round(value)}%</AppText></View>; }
export function RouteTimeline({ stops }: { readonly stops: readonly { readonly label: string; readonly detail?: string; readonly tone?: 'pickup' | 'dropoff' | 'stop' }[] }) { return <View>{stops.map((s, i) => <View key={`${s.label}-${i}`} style={styles.timelineRow}><View style={[styles.timelineDot, { backgroundColor: s.tone === 'dropoff' ? t.colors.accent : t.colors.teal }]} /><View style={styles.flex}><AppText variant="label">{s.label}</AppText>{s.detail ? <AppText color={t.colors.ink3}>{s.detail}</AppText> : null}</View></View>)}</View>; }
export function FareRow({ label, amount, emphasized = false }: { readonly label: string; readonly amount: string; readonly emphasized?: boolean }) { return <View style={styles.between}><AppText variant={emphasized ? 'label' : 'body'}>{label}</AppText><AppText variant={emphasized ? 'title' : 'label'}>{amount}</AppText></View>; }
export function PaymentRow({ brand, last4, selected, onPress }: { readonly brand: string; readonly last4: string; readonly selected?: boolean; readonly onPress?: () => void }) { return <ListRow title={brand} subtitle={`•••• ${last4}`} onPress={onPress} accessibilityLabel={`${brand} ending ${last4}`} accessibilityHint="Select payment method" trailing={<Chip selected={!!selected}>{selected ? 'Selected' : 'Use'}</Chip>} />; }
export function SeatPlan({ seats, onToggle }: { readonly seats: readonly { readonly id: string; readonly label: string; readonly state: 'driver' | 'taken' | 'free' | 'selected' | 'disabled' }[]; readonly onToggle?: (id: string) => void }) { return <View accessibilityLabel="Seat plan" style={styles.seatGrid}>{seats.map(seat => { const disabled = seat.state === 'driver' || seat.state === 'taken' || seat.state === 'disabled'; return <Pressable key={seat.id} accessibilityRole="button" accessibilityLabel={`${seat.label} ${seat.state}`} accessibilityHint={disabled ? 'Seat is not available' : 'Toggle seat selection'} accessibilityState={{ disabled, selected: seat.state === 'selected' }} disabled={disabled} onPress={() => onToggle?.(seat.id)} style={[styles.touchTarget, styles.seat, seatStyle(seat.state)]}><AppText variant="label">{seat.label}</AppText></Pressable>; })}</View>; }
export function SosButton({ onPress }: { readonly onPress?: () => void }) { return <Pressable accessibilityRole="button" accessibilityLabel="SOS emergency help" accessibilityHint="Call emergency support and share your trip" accessibilityState={{ disabled: !onPress }} disabled={!onPress} onPress={onPress} style={[styles.touchTarget, styles.sos]}><AppText variant="label" color="#ffffff">SOS</AppText></Pressable>; }
export function MapBackdrop({ showRoute = true, children }: PropsWithChildren<{ readonly showRoute?: boolean }>) {
  const center = { latitude: 6.9271, longitude: 79.8612 };
  const route = [{ latitude: 6.909, longitude: 79.909 }, center];
  return (
    <View accessibilityLabel="Google route map preview" style={styles.map}>
      <MapView
        accessibilityLabel="Google map"
        initialRegion={{ ...center, latitudeDelta: 0.08, longitudeDelta: 0.08 }}
        provider="google"
        style={StyleSheet.absoluteFill}
      >
        {showRoute ? <Polyline coordinates={route} strokeColor={t.colors.accent} strokeWidth={5} /> : null}
        <Marker coordinate={center} title="RouteShare map center" />
      </MapView>
      {children}
    </View>
  );
}
export function MapOverlayCard({ children, style }: PropsWithChildren<{ readonly style?: StyleProp<ViewStyle> }>) { return <Card style={[styles.overlay, style]}>{children}</Card>; }

function buttonStyle(variant: 'primary' | 'secondary' | 'ghost' | 'danger', disabled: boolean, selected: boolean): ViewStyle { if (disabled) return styles.disabled; if (selected) return styles.selected; if (variant === 'secondary') return styles.secondaryButton; if (variant === 'ghost') return styles.ghostButton; if (variant === 'danger') return styles.dangerButton; return styles.primaryButton; }
function seatStyle(state: 'driver' | 'taken' | 'free' | 'selected' | 'disabled'): ViewStyle { if (state === 'selected') return styles.seatSelected; if (state === 'free') return styles.seatFree; if (state === 'driver') return styles.seatDriver; return styles.seatDisabled; }

const styles = StyleSheet.create({
  flex: { flex: 1 }, screen: { flex: 1, backgroundColor: t.colors.bg }, screenContent: { flexGrow: 1, gap: 16, padding: 20 }, touchTarget: { minHeight: MIN_TOUCH_TARGET, minWidth: MIN_TOUCH_TARGET }, button: { alignItems: 'center', borderRadius: t.radius.pill, justifyContent: 'center', paddingHorizontal: 20, paddingVertical: 12 }, primaryButton: { backgroundColor: t.colors.accent }, secondaryButton: { backgroundColor: t.colors.accentSoft }, ghostButton: { backgroundColor: 'transparent' }, dangerButton: { backgroundColor: t.semantic.danger }, disabled: { opacity: 0.45, backgroundColor: t.colors.line }, selected: { backgroundColor: t.colors.teal }, iconButton: { alignItems: 'center', backgroundColor: t.colors.surface, borderRadius: 22, justifyContent: 'center', ...t.shadows.md }, fieldWrap: { gap: 8 }, input: { backgroundColor: t.colors.surface, borderColor: t.colors.line, borderRadius: t.radius.md, borderWidth: 1.5, color: t.colors.ink, fontSize: 16, minHeight: 52, padding: 14 }, inputError: { borderColor: t.semantic.danger }, otpWrap: { minHeight: 52, position: 'relative' }, otpInputOverlay: { ...StyleSheet.absoluteFill, color: 'transparent', opacity: 0.01, zIndex: 2 }, otpRow: { flexDirection: 'row', gap: 8, justifyContent: 'space-between', width: '100%' }, otpCell: { alignItems: 'center', aspectRatio: 0.85, backgroundColor: t.colors.surface, borderColor: t.colors.line, borderRadius: 14, borderWidth: 1.5, flex: 1, justifyContent: 'center', minHeight: 60 }, chip: { alignItems: 'center', backgroundColor: t.colors.bgSoft, borderRadius: t.radius.pill, justifyContent: 'center', paddingHorizontal: 14 }, chipSelected: { backgroundColor: t.colors.teal }, card: { backgroundColor: t.colors.surface, borderColor: t.colors.line, borderRadius: t.radius.lg, borderWidth: 1, padding: 16, ...t.shadows.sm }, sheet: { backgroundColor: t.colors.surface, borderTopLeftRadius: t.radius.xl, borderTopRightRadius: t.radius.xl, padding: 20, ...t.shadows.lg }, grabber: { alignSelf: 'center', backgroundColor: t.colors.line2, borderRadius: 2, height: 4, marginBottom: 12, width: 42 }, row: { alignItems: 'center', flexDirection: 'row', gap: 12, paddingVertical: 12 }, stat: { minWidth: 120 }, progress: { backgroundColor: t.colors.line, borderRadius: 999, height: 8, overflow: 'hidden' }, progressFill: { backgroundColor: t.colors.accent, height: '100%' }, toast: { backgroundColor: t.colors.surface, borderLeftWidth: 4, borderRadius: t.radius.md, padding: 14 }, dialogBackdrop: { alignItems: 'center', backgroundColor: 'rgba(27,20,16,0.45)', flex: 1, justifyContent: 'center', padding: 24 }, dialog: { gap: 12, width: '100%' }, actions: { flexDirection: 'row', gap: 12, justifyContent: 'flex-end' }, state: { alignItems: 'center', gap: 12, justifyContent: 'center', padding: 24 }, avatar: { alignItems: 'center', justifyContent: 'center' }, matchRing: { alignItems: 'center', backgroundColor: t.colors.surface, borderWidth: 4, justifyContent: 'center' }, timelineRow: { flexDirection: 'row', gap: 12, paddingVertical: 8 }, timelineDot: { borderColor: t.colors.surface, borderRadius: 7, borderWidth: 3, height: 18, marginTop: 3, width: 18 }, between: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 8 }, seatGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 }, seat: { alignItems: 'center', borderRadius: t.radius.md, justifyContent: 'center', width: 72 }, seatFree: { backgroundColor: t.colors.surface, borderColor: t.colors.line, borderWidth: 1 }, seatSelected: { backgroundColor: t.colors.tealSoft, borderColor: t.colors.teal, borderWidth: 2 }, seatDriver: { backgroundColor: t.colors.bgSoft }, seatDisabled: { backgroundColor: t.colors.line, opacity: 0.7 }, sos: { alignItems: 'center', backgroundColor: t.semantic.danger, borderRadius: t.radius.pill, justifyContent: 'center', paddingHorizontal: 18 }, map: { backgroundColor: t.colors.bgSoft, minHeight: 260, overflow: 'hidden', position: 'relative' }, mapGrid: { ...StyleSheet.absoluteFill, backgroundColor: '#efe5d5' }, mapRoute: { backgroundColor: t.colors.accent, borderRadius: 4, height: 180, left: '48%', opacity: 0.9, position: 'absolute', top: 42, transform: [{ rotate: '-28deg' }], width: 8 }, mapPin: { backgroundColor: t.colors.teal, borderColor: '#ffffff', borderRadius: 12, borderWidth: 5, height: 24, left: '50%', position: 'absolute', top: '38%', width: 24 }, overlay: { position: 'absolute' },
});
