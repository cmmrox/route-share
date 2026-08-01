export type VerificationStatus = 'not_started' | 'readiness_only' | 'submitted' | 'verified' | 'rejected';

export function verificationCopy(status: string | undefined) {
  const normalized = (status ?? 'readiness_only') as VerificationStatus;
  if (normalized === 'verified') return { title: 'Verification ready', message: 'Your passenger account is marked verified by RouteShare.' };
  if (normalized === 'submitted') return { title: 'Documents saved for review', message: 'We saved your submission, but the current backend only exposes readiness status until document review is enabled.' };
  if (normalized === 'rejected') return { title: 'Verification needs attention', message: 'Please prepare a clearer ID image. Live review is not enabled in this backend slice yet.' };
  return { title: 'Verification shell', message: 'RouteShare can collect readiness details, but live ID document review is not enabled by the current backend.' };
}
